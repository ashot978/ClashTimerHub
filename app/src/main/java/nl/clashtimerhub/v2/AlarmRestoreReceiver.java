package nl.clashtimerhub.v2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Restore local upgrade notifications after device restart or application update. */
public final class AlarmRestoreReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context, Intent intent) {
  if(intent==null)return;
  String action=intent.getAction();
  if(Intent.ACTION_BOOT_COMPLETED.equals(action)
    || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
    || Intent.ACTION_TIME_CHANGED.equals(action)
    || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
   TimerWidget.schedule(context);
   // Android sends this after an actual package replacement, including system-installer
   // updates. Record it for the next app launch, because the old installer process
   // may have been terminated before it could display a success screen.
   if(Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)){
    long version=UpdateChecker.installedVersionCode(context);
    if(version>1000)context.getSharedPreferences(MainActivity.class.getName(),Context.MODE_PRIVATE)
     .edit().putLong("pendingSuccessfulUpdateVersionCode",version)
     .remove("lastInstallError").apply();
   }
  }
 }
}
