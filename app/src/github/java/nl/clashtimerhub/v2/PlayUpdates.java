package nl.clashtimerhub.v2;

import android.app.Activity;
import java.util.function.BooleanSupplier;

/** The GitHub distribution uses its existing updater. */
final class PlayUpdates {
 PlayUpdates(Activity activity) {}
 void onResume(BooleanSupplier canPrompt) {}
 void onPause() {}
}
