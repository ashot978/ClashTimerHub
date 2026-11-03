package nl.clashtimerhub.app;

import android.content.Context;
import android.graphics.Rect;

/**
 * Percentage-based scan regions, persisted separately for each menu type.
 * Values survive screen resolution changes and application restarts.
 */
public final class ScanRegions {
    private ScanRegions() {}
    private static final String PREFS = "scan_regions";
    private static final float[] DEFAULT = { .58f, 0f, 1f, .58f };

    public static Rect get(Context context, int category, int width, int height) {
        float[] values = new float[4];
        for (int i = 0; i < 4; i++) {
            values[i] = context == null ? DEFAULT[i] :
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getFloat(category + "_" + i, DEFAULT[i]);
        }
        int left = clamp(Math.round(values[0] * width), 0, width - 2);
        int top = clamp(Math.round(values[1] * height), 0, height - 2);
        int right = clamp(Math.round(values[2] * width), left + 2, width);
        int bottom = clamp(Math.round(values[3] * height), top + 2, height);
        return new Rect(left, top, right, bottom);
    }

    public static void save(Context context, int category, Rect rect,
                            int width, int height) {
        if (context == null || rect == null || width <= 0 || height <= 0)
            throw new IllegalArgumentException("Invalid scan region");
        android.content.SharedPreferences.Editor editor =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        editor.putFloat(category + "_0", (float) rect.left / width);
        editor.putFloat(category + "_1", (float) rect.top / height);
        editor.putFloat(category + "_2", (float) rect.right / width);
        editor.putFloat(category + "_3", (float) rect.bottom / height);
        editor.apply();
    }

    public static void reset(Context context, int category) {
        android.content.SharedPreferences.Editor editor =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        for (int i = 0; i < 4; i++) editor.remove(category + "_" + i);
        editor.apply();
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
