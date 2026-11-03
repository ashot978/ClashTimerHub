package nl.clashtimerhub.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Spatial OCR gate for Clash upgrade screens. Nothing is parsed unless the
 * heading for the selected category is actually visible in the game menu.
 * Values from the village, status bar and Suggested/Other upgrades are ignored.
 */
public final class MenuTimerDetector {
    private MenuTimerDetector() {}

    public interface RowValidator {
        boolean isActiveRow(int y);
    }

    public static final class Token {
        public final String text;
        public final int left, top, right, bottom;
        public Token(String text, int left, int top, int right, int bottom) {
            this.text = text == null ? "" : text;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
        public int centerY() { return (top + bottom) / 2; }
    }

    public static final class Result {
        public final boolean menuOpen;
        public final TimerParser.Hit hit;
        Result(boolean menuOpen, TimerParser.Hit hit) {
            this.menuOpen = menuOpen;
            this.hit = hit;
        }
    }

    public static Result detect(List<Token> tokens, int width, int height,
                                int category, RowValidator validator) {
        if (tokens == null || width <= 0 || height <= 0) return new Result(false, null);
        // In Clash's landscape UI the menu is on the right. The heading is
        // fixed above the scrolling list, so it is safe to use as a hard gate.
        StringBuilder header = new StringBuilder();
        for (Token t : tokens) {
            if (t.centerY() < height * .16f && t.right > width * .55f) {
                header.append(normalize(t.text));
            }
        }
        String heading = header.toString();
        boolean found;
        if (category == 0) {
            found = heading.contains("building") && heading.contains("hero");
        } else if (category == 1) {
            found = heading.contains("laborator") || heading.contains("research");
        } else if (category == 2) {
            found = heading.contains("pethouse") || heading.contains("pets");
        } else {
            return new Result(false, null);
        }
        if (!found) return new Result(false, null);

        // Recommendations have their own durations and MUST NOT be counted.
        int endOfActiveList = height;
        for (Token t : tokens) {
            String label = normalize(t.text);
            if (t.centerY() > height * .14f && t.right > width * .55f &&
                    (label.contains("suggested") || label.contains("otherupgrades"))) {
                endOfActiveList = Math.min(endOfActiveList, t.top);
            }
        }

        ArrayList<Token> eligible = new ArrayList<>();
        for (Token t : tokens) {
            int y = t.centerY();
            if (y > height * .065f && y < endOfActiveList &&
                    t.right > width * .75f && t.left < width && t.top >= 0) {
                eligible.add(t);
            }
        }
        Collections.sort(eligible, (a,b) -> {
            int diff = Integer.compare(a.centerY(), b.centerY());
            return diff != 0 ? diff : Integer.compare(a.left, b.left);
        });

        // OCR can return "5H" and "14M" as separate elements. Join elements
        // from the SAME visual row before parsing, not from arbitrary lines.
        int rowTolerance = Math.max(11, Math.round(height * .022f));
        ArrayList<ArrayList<Token>> rows = new ArrayList<>();
        ArrayList<Integer> rowCenters = new ArrayList<>();
        for (Token token : eligible) {
            int bestRow = -1;
            int minDistance = Integer.MAX_VALUE;
            for (int i = 0; i < rows.size(); i++) {
                int distance = Math.abs(token.centerY() - rowCenters.get(i));
                if (distance <= rowTolerance && distance < minDistance) {
                    bestRow = i;
                    minDistance = distance;
                }
            }
            if (bestRow == -1) {
                ArrayList<Token> row = new ArrayList<>();
                row.add(token);
                rows.add(row);
                rowCenters.add(token.centerY());
            } else {
                rows.get(bestRow).add(token);
            }
        }

        TimerParser.Hit shortest = null;
        for (int i = 0; i < rows.size(); i++) {
            int y = rowCenters.get(i);
            // Builders & Heroes active rows have green progress bars. This
            // extra visual check excludes suggested upgrades even if OCR
            // failed to read the "Suggested upgrades" divider.
            if (category == 0 && (validator == null || !validator.isActiveRow(y))) continue;
            ArrayList<Token> row = rows.get(i);
            Collections.sort(row, Comparator.comparingInt(t -> t.left));
            StringBuilder merged = new StringBuilder();
            for (Token t : row) merged.append(t.text).append(' ');
            List<TimerParser.Hit> matches = TimerParser.findDurations(merged.toString());
            if (matches.isEmpty()) continue;
            // Prefer a complete multi-unit expression (5h 14m) over an OCR
            // fragment (14m) that appears on the same visual row.
            TimerParser.Hit mostComplete = null;
            for (TimerParser.Hit hit : matches) {
                if (mostComplete == null || unitCount(hit.text) > unitCount(mostComplete.text) ||
                        (unitCount(hit.text) == unitCount(mostComplete.text) &&
                         hit.text.length() > mostComplete.text.length())) {
                    mostComplete = hit;
                }
            }
            if (mostComplete != null &&
                    (shortest == null || mostComplete.seconds < shortest.seconds)) {
                shortest = mostComplete;
            }
        }
        return new Result(true, shortest);
    }

    private static int unitCount(String text) {
        String value = text.toLowerCase(Locale.ROOT);
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == 'd' || c == 'h' || c == 'u' || c == 'm' || c == 's') count++;
        }
        return count;
    }

    private static String normalize(String source) {
        if (source == null) return "";
        return source.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }
}
