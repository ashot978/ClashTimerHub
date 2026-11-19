#!/usr/bin/env bash
set -euo pipefail

bundle="${1:?Path to signed Play AAB required}"
bundletool="${2:?Path to bundletool JAR required}"
version_code="${3:?Expected version code required}"
if [[ ! "$version_code" =~ ^[1-9][0-9]{3,9}$ ]] ||
   (( version_code < 1575 || version_code > 2100000000 )); then
  echo 'Invalid Play version code.' >&2
  exit 1
fi
# The certificate on the Play-accepted builds 1570 and 1575.
expected_cert='8A:EE:DB:5B:7D:7A:47:5F:B9:D8:42:06:D4:4A:4F:C5:A6:CC:B2:D7:42:2D:7E:17:41:40:4B:08:27:B3:34:3E'
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

keytool -J-Duser.language=en -J-Duser.country=US -printcert -rfc -jarfile "$bundle" > "$work/signer.pem"
if [[ "$(grep -c '^-----BEGIN CERTIFICATE-----$' "$work/signer.pem" || true)" != 1 ]]; then
  echo 'Expected exactly one AshLab upload certificate in the bundle.' >&2
  exit 1
fi
keytool -J-Duser.language=en -J-Duser.country=US -printcert -file "$work/signer.pem" > "$work/cert.txt"
if ! grep -Fq "SHA256: $expected_cert" "$work/cert.txt"; then
  echo 'Play upload certificate does not match the accepted AshLab certificate.' >&2
  exit 1
fi
# Trust the pinned public certificate for strict JAR verification. The upload
# certificate is self-signed, so Java's default CA trust store cannot verify it.
keytool -importcert -noprompt -storetype PKCS12 -keystore "$work/trust.p12" \
  -storepass changeit -alias play-upload -file "$work/signer.pem"
jarsigner -verify -strict -keystore "$work/trust.p12" -storepass changeit "$bundle" play-upload

java -jar "$bundletool" validate --bundle="$bundle" > "$work/validation.txt"
java -jar "$bundletool" dump manifest --bundle="$bundle" --module=base > "$work/manifest.xml"
java -jar "$bundletool" dump config --bundle="$bundle" > "$work/config.json"

python3 - "$work" "$version_code" <<'PY'
from pathlib import Path
from xml.etree import ElementTree
import json
import sys

work = Path(sys.argv[1])
version_code = sys.argv[2]
manifest = ElementTree.parse(work / "manifest.xml").getroot()
android = "{http://schemas.android.com/apk/res/android}"

if manifest.get("package") != "nl.clashtimerhub.app":
    sys.exit("Unexpected Play application ID")
if manifest.get(android + "versionCode") != version_code:
    sys.exit("Unexpected Play version code")
if int(manifest.find("uses-sdk").get(android + "targetSdkVersion")) < 36:
    sys.exit("Google Play build must target Android API 36 or newer")

app = manifest.find("application")
if app.get(android + "debuggable", "false") != "false":
    sys.exit("Play bundle is debuggable")
for item in manifest:
    if item.tag.startswith("uses-permission") and item.get(android + "name") == "android.permission.REQUEST_INSTALL_PACKAGES":
        sys.exit("Play bundle includes APK installation permission")
if any(receiver.get(android + "name", "").endswith(".UpdateInstallReceiver") for receiver in app.findall("receiver")):
    sys.exit("Play bundle contains the GitHub APK installer receiver")

config = json.loads((work / "config.json").read_text())
alignment = config.get("optimizations", {}).get("uncompressNativeLibraries", {}).get("alignment")
if alignment != "PAGE_ALIGNMENT_16K":
    sys.exit("Play bundle is missing 16 KB native library alignment")
print(f"Verified AshLab Play release for nl.clashtimerhub.app, version code {version_code}.")
PY
