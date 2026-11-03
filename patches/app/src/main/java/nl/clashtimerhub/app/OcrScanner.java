package nl.clashtimerhub.app;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.content.Context;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads ONLY an explicitly opened Clash upgrades menu. Villages and other
 * screens cannot contribute timer candidates.
 */
public final class OcrScanner {
    private OcrScanner() { }
    private static final TextRecognizer RECOGNIZER =
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    public interface Callback {
        void onResult(MenuTimerDetector.Result result);
        void onError(Exception error);
    }

    public static void scan(Bitmap bitmap, int category, Callback callback) {
        scan(bitmap, category, null, callback);
    }

    public static void scan(Bitmap bitmap, int category, Context context, Callback callback) {
        if (bitmap == null) {
            callback.onError(new IllegalArgumentException("Geen afbeelding"));
            return;
        }
        try {
            Bitmap panel = cropUpgradePanel(bitmap, context, category);
            RECOGNIZER.process(InputImage.fromBitmap(panel, 0))
                .addOnSuccessListener(text -> {
                    List<MenuTimerDetector.Token> tokens = new ArrayList<>();
                    for (Text.TextBlock block : text.getTextBlocks()) {
                        for (Text.Line line : block.getLines()) {
                            for (Text.Element element : line.getElements()) {
                            Rect rect = element.getBoundingBox();
                            if (rect != null) {
                                tokens.add(new MenuTimerDetector.Token(
                                    element.getText(), rect.left + cropLeft(bitmap, context, category), rect.top + cropTop(bitmap, context, category), rect.right + cropLeft(bitmap, context, category), rect.bottom + cropTop(bitmap, context, category)));
                            }
                            }
                        }
                    }
                    MenuTimerDetector.RowValidator validator = y ->
                            hasGreenProgressBar(bitmap, y);
                    callback.onResult(MenuTimerDetector.detect(
                        tokens, bitmap.getWidth(), bitmap.getHeight(), category, validator));
                })
                .addOnFailureListener(callback::onError)
                .addOnCompleteListener(task -> panel.recycle());
        } catch (RuntimeException error) {
            callback.onError(error);
        }
    }

    /**
     * Crop the right-side upgrade panel before OCR. Village timers are never
     * submitted to text recognition, even if the menu is visible.
     */
    private static int cropLeft(Bitmap bitmap, Context context, int category) {
        return ScanRegions.get(context, category, bitmap.getWidth(), bitmap.getHeight()).left;
    }

    private static int cropTop(Bitmap bitmap, Context context, int category) {
        return ScanRegions.get(context, category, bitmap.getWidth(), bitmap.getHeight()).top;
    }

    private static Bitmap cropUpgradePanel(Bitmap bitmap, Context context, int category) {
        android.graphics.Rect r = ScanRegions.get(context, category,
                bitmap.getWidth(), bitmap.getHeight());
        return Bitmap.createBitmap(bitmap, r.left, r.top, r.width(), r.height());
    }

    /** Visual confirmation of the green progress bar in a running builder row. */
    private static boolean hasGreenProgressBar(Bitmap bitmap, int rowY) {
        int width = bitmap.getWidth(), height = bitmap.getHeight();
        if (width <= 0 || height <= 0 || rowY < 0 || rowY >= height) return false;
        int start = (int)(width * .596);
        int stop = (int)(width * .84);
        int step = Math.max(4, width / 190);
        int hits = 0;
        int activeColumns = 0;
        int yStep = Math.max(3, height / 300);
        for (int x = start; x < stop; x += step) {
            boolean green = false;
            for (int y = Math.max(0, rowY - height/90);
                 y <= Math.min(height-1, rowY + height/90); y += yStep) {
                int c = bitmap.getPixel(x,y);
                int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
                if (g > 78 && g-r > 17 && g-b > 19 && g > (int)(r * 1.12f)) {
                    green = true; break;
                }
            }
            if (green) {
                activeColumns++;
                if (++hits >= 3) return true;
            }
        }
        return activeColumns >= 3;
    }
}
