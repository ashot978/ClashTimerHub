package nl.clashtimerhub.app;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses a builder availability indicator such as 2/6 or 1/7. */
public final class BuilderAvailability {
    private BuilderAvailability() {}
    private static final Pattern PATTERN =
        Pattern.compile("([0-7])\\s*/\\s*([67])");

    public static int parse(String text) {
        if (text == null) return -1;
        Matcher match = PATTERN.matcher(text);
        if (!match.find()) return -1;
        int free = Integer.parseInt(match.group(1));
        int total = Integer.parseInt(match.group(2));
        return free <= total ? free : -1;
    }
}
