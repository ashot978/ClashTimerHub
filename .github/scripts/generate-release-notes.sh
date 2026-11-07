#!/usr/bin/env bash
# Generate notes for this exact Android build, never the static CHANGELOG head.
set -euo pipefail

: "$GITHUB_REPOSITORY"
: "$GITHUB_SHA"
: "$GITHUB_RUN_NUMBER"

previous_sha="$(gh api "repos/$GITHUB_REPOSITORY/releases/latest" --jq '.target_commitish' 2>/dev/null || true)"
previous_tag="$(gh api "repos/$GITHUB_REPOSITORY/releases/latest" --jq '.tag_name' 2>/dev/null || true)"
valid_previous=false
if [[ "$previous_sha" =~ ^[0-9a-f]{40}$ ]] &&
   git cat-file -e "$previous_sha^{commit}" 2>/dev/null &&
   git merge-base --is-ancestor "$previous_sha" "$GITHUB_SHA"; then
  valid_previous=true
fi

# Use curated bilingual notes only if the file changed since the last release.
# An unchanged file MUST NEVER be reused in a subsequent build.
curated=false
if [[ -f RELEASE_NOTES.md ]] &&
   grep -q '^### NL$' RELEASE_NOTES.md &&
   grep -q '^### EN$' RELEASE_NOTES.md &&
   grep -q '^- ' RELEASE_NOTES.md; then
  if [[ "$valid_previous" == true ]]; then
    if ! git diff --quiet "$previous_sha" "$GITHUB_SHA" -- RELEASE_NOTES.md; then
      curated=true
    fi
  else
    curated=true
  fi
fi

printf '## Build #%s\n\n' "$GITHUB_RUN_NUMBER" > release-notes.md
if [[ "$curated" == true ]]; then
  cat RELEASE_NOTES.md >> release-notes.md
  printf '\n' >> release-notes.md
  echo "Using changed, bilingual RELEASE_NOTES.md for this build."
else
  # Rebuilding the same commit is not a new feature release.
  if [[ "$valid_previous" == true ]]; then
    subjects="$(git log --no-merges --reverse --format=%s "$previous_sha..$GITHUB_SHA")"
  else
    subjects="$(git log -1 --format=%s "$GITHUB_SHA")"
  fi
  echo "Generating fresh notes from commit subjects (no updated curated notes)."
  printf '### NL\n' >> release-notes.md
  if [[ -z "$subjects" ]]; then
    printf '%s\n' '- Onderhoudsbuild van dezelfde broncode; geen nieuwe functies sinds de vorige build.' >> release-notes.md
  else
    count=0
    while IFS= read -r subject; do
      [[ -z "$subject" ]] && continue
      subject="$(printf '%s' "$subject" | sed -E 's/[[:space:]]+\(#[0-9]+\)$//')"
      printf '%s\n' "- Wijziging: $subject" >> release-notes.md
      count=$((count+1))
      [[ "$count" -ge 6 ]] && break
    done <<< "$subjects"
  fi
  printf '\n### EN\n' >> release-notes.md
  if [[ -z "$subjects" ]]; then
    printf '%s\n' '- Maintenance rebuild from the same source; no new features since the previous build.' >> release-notes.md
  else
    count=0
    while IFS= read -r subject; do
      [[ -z "$subject" ]] && continue
      subject="$(printf '%s' "$subject" | sed -E 's/[[:space:]]+\(#[0-9]+\)$//')"
      printf '%s\n' "- $subject" >> release-notes.md
      count=$((count+1))
      [[ "$count" -ge 6 ]] && break
    done <<< "$subjects"
  fi
  printf '\n' >> release-notes.md
fi

if [[ -z "$previous_tag" ]]; then previous_tag=none; fi
printf '## Build information\n\n' >> release-notes.md
printf 'Build #%s · Android versionCode %s.\n' "$GITHUB_RUN_NUMBER" "$((1000 + GITHUB_RUN_NUMBER))" >> release-notes.md
printf 'Source commit: %s\n' "$GITHUB_SHA" >> release-notes.md
printf 'Previous published release: %s\n' "$previous_tag" >> release-notes.md
printf 'Install over the existing app; do not uninstall without a verified backup.\n' >> release-notes.md

# Reject invalid build-specific release metadata.
grep -Fxq "## Build #$GITHUB_RUN_NUMBER" release-notes.md
grep -Fxq '### NL' release-notes.md
grep -Fxq '### EN' release-notes.md
grep -Fq "Source commit: $GITHUB_SHA" release-notes.md
echo 'Verified fresh release notes:'
cat release-notes.md
