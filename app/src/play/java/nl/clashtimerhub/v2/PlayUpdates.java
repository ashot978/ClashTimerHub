package nl.clashtimerhub.v2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

import java.util.function.BooleanSupplier;

/** Checks for a Play update in the foreground and lets Play handle installation. */
final class PlayUpdates {
 private static final int REQUEST_CODE = 2086;
 private static final long PROMPT_INTERVAL_MS = 24L * 60 * 60 * 1000;

 private final Activity activity;
 private final SharedPreferences preferences;
 private final AppUpdateManager manager;
 private final InstallStateUpdatedListener installListener;
 private boolean resumed;
 private int resumeGeneration;
 private AlertDialog restartDialog;

 PlayUpdates(Activity activity) {
  this.activity = activity;
  preferences = activity.getSharedPreferences("playUpdates", Activity.MODE_PRIVATE);
  manager = AppUpdateManagerFactory.create(activity);
  installListener = state -> {
   if (state.installStatus() == InstallStatus.DOWNLOADED) {
    activity.runOnUiThread(this::offerRestart);
   }
  };
 }

 void onResume(BooleanSupplier canPrompt) {
  resumed = true;
  int generation = ++resumeGeneration;
  manager.registerListener(installListener);
  manager.getAppUpdateInfo().addOnSuccessListener(activity, info -> {
   if (!isVisible() || generation != resumeGeneration) return;
   if (info.installStatus() == InstallStatus.DOWNLOADED) {
    offerRestart();
    return;
   }
   if (info.installStatus() == InstallStatus.PENDING || info.installStatus() == InstallStatus.DOWNLOADING
     || info.installStatus() == InstallStatus.INSTALLING) return;
   if (!canPrompt.getAsBoolean() || info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE
     || !info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) return;

   int versionCode = info.availableVersionCode();
   long now = System.currentTimeMillis();
   long lastPrompt = preferences.getLong("promptTime", 0);
   if (preferences.getInt("promptVersion", -1) == versionCode && now >= lastPrompt
     && now - lastPrompt < PROMPT_INTERVAL_MS) return;

   try {
    if (manager.startUpdateFlowForResult(info, activity,
      AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(), REQUEST_CODE)) {
     preferences.edit().putInt("promptVersion", versionCode).putLong("promptTime", now).apply();
    }
   } catch (IntentSender.SendIntentException unavailable) {
    // Play can invalidate a pending update offer. A later resume can check again.
   }
  });
 }

 void onPause() {
  resumed = false;
  manager.unregisterListener(installListener);
  if (restartDialog != null) restartDialog.dismiss();
 }

 private boolean isVisible() {
  return resumed && !activity.isFinishing() && !activity.isDestroyed();
 }

 private void offerRestart() {
  if (!isVisible() || restartDialog != null) return;
  restartDialog = new AlertDialog.Builder(activity)
    .setTitle(text("Update gedownload", "Update downloaded"))
    .setMessage(text("Start Clash Timer Hub opnieuw om de update te installeren.",
      "Restart Clash Timer Hub to install the update."))
    .setPositiveButton(text("Opnieuw starten", "Restart"), (dialog, which) ->
      manager.completeUpdate().addOnFailureListener(activity, error ->
        Toast.makeText(activity, text("Update installeren mislukt. Probeer het opnieuw via Google Play.",
          "Couldn't install the update. Try again in Google Play."), Toast.LENGTH_LONG).show()))
    .setNegativeButton(text("Later", "Later"), null)
    .create();
  restartDialog.setOnDismissListener(dialog -> restartDialog = null);
  restartDialog.show();
 }

 private String text(String dutch, String english) {
  return I18n.english(activity) ? english : dutch;
 }
}
