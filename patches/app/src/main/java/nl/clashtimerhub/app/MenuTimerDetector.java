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
        public final List<TimerParser.Hit> hits;
        public final List<String> names;
        public final int freeBuilders;
        Result(boolean menuOpen, TimerParser.Hit hit) {
            this(menuOpen, hit, -1);
        }
        Result(boolean menuOpen, TimerParser.Hit hit, int freeBuilders) {
            this.menuOpen = menuOpen;
            this.hit = hit;
            this.freeBuilders = freeBuilders;
            this.hits = new ArrayList<>();
            this.names = new ArrayList<>();
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
        int freeBuilders = -1;
        if (category == 0) {
            for (Token t : tokens) {
                if (t.centerY() > height * .35f) continue;
                int count = BuilderAvailability.parse(t.text);
                if (count >= 0) {
                    if (freeBuilders >= 0 && freeBuilders != count) {
                        freeBuilders = -1;
                        break;
                    }
                    freeBuilders = count;
                }
            }
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

        // The active upgrade list is a fixed vertical stack in the right-hand panel.
        // Use row centers from the green progress bars, not OCR line centers:
        // a split "5d 10h" must never turn into an unrelated "10h".
        ArrayList<TimerParser.Hit> activeTimers = new ArrayList<>();
        ArrayList<String> activeNames = new ArrayList<>();
        TimerParser.Hit shortest = null;
        if (category == 0) {
            int start = Math.round(height * .072f);
            int step = Math.round(height * .053f);
            int rowHeight = Math.round(height * .045f);
            for (int row = 0; row < 12; row++) {
                int top = start + row * step;
                int center = top + rowHeight / 2;
                if (center >= endOfActiveList || center >= height * .80f) break;
                if (validator == null || !validator.isActiveRow(center)) continue;
                ArrayList<Token> rowTokens = new ArrayList<>();
                for (Token t : tokens) {
                    if (t.centerY() >= top && t.centerY() < top + rowHeight
                            && t.left > width * .58f && t.right < width * .99f)
                        rowTokens.add(t);
                }
                rowTokens.sort(Comparator.comparingInt(t -> t.left));
                StringBuilder name = new StringBuilder();
                StringBuilder time = new StringBuilder();
                for (Token t : rowTokens) {
                    if (t.left < width * .84f) name.append(t.text).append(' ');
                    else time.append(t.text).append(' ');
                }
                List<TimerParser.Hit> durations = TimerParser.findDurations(time.toString());
                TimerParser.Hit complete = null;
                for (TimerParser.Hit candidate : durations)
                    if (complete == null || unitCount(candidate.text) > unitCount(complete.text)
                        || (unitCount(candidate.text) == unitCount(complete.text)
                            && candidate.text.length() > complete.text.length()))
                        complete = candidate;
                if (complete == null) continue;
                activeTimers.add(complete);
                activeNames.add(name.toString().trim());
                if (shortest == null || complete.seconds < shortest.seconds) shortest = complete;
            }
        } else {
            ArrayList<Token> eligible = new ArrayList<>();
            for (Token t : tokens)
                if (t.centerY() > height * .065f && t.centerY() < endOfActiveList
                        && t.left > width * .70f) eligible.add(t);
            for (Token t : eligible) {
                TimerParser.Hit hit = TimerParser.shortestInText(t.text);
                if (hit != null) {
                    activeTimers.add(hit);
                    activeNames.add("");
                    if (shortest == null || hit.seconds < shortest.seconds) shortest = hit;
                }
            }
        }
        Result result = new Result(true, shortest, freeBuilders);
        result.hits.addAll(activeTimers);
        result.names.addAll(activeNames);
        return result;
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
