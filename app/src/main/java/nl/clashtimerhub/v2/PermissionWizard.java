package nl.clashtimerhub.v2;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
import java.util.ArrayList;

/**
 * Consent-first, step-by-step Android permission onboarding.
 * No permission is silently enabled, and every optional step can be skipped.
 */
final class PermissionWizard {
 static final int NOTIFICATION_REQUEST=2083;
 private static final int NOTIFICATIONS=0,OVERLAY=1,EXACT_ALARMS=2,INSTALL_APKS=3;
 private final MainActivity activity;
 private boolean running=false,waitingForSettings=false;
 private int current=0;
 private AlertDialog displayed;

 PermissionWizard(MainActivity activity){this.activity=activity;}
 private boolean english(){return I18n.english(activity);}
 private String tr(String dutch,String englishText){return english()?englishText:dutch;}
 private android.content.SharedPreferences prefs(){return AppData.prefs(activity);}

 boolean isRunning(){return running;}
 void start(boolean forced){
  if(running)return;
  if(!forced&&prefs().getBoolean("permissionsWizardSeen",false))return;
  running=true;waitingForSettings=false;current=0;
  next();
 }
 void onResume(){
  if(!running||!waitingForSettings)return;
  waitingForSettings=false;
  // Resume after the system permission page has closed; the user may have declined.
  // Continue to the next step without looping them back into the same Settings screen.
  activity.getWindow().getDecorView().post(()->{
   if(!running||activity.isFinishing()||activity.isDestroyed())return;
   current++;
   next();
  });
 }
 void onRequestPermissionsResult(int requestCode){
  if(requestCode!=NOTIFICATION_REQUEST||!running)return;
  current++;next();
 }
 static int grantedCount(Context ctx){
  int granted=0;
  if(Build.VERSION.SDK_INT<33||ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)granted++;
  if(Settings.canDrawOverlays(ctx))granted++;
  AlarmManager alarm=(AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
  if(Build.VERSION.SDK_INT<31||alarm!=null&&alarm.canScheduleExactAlarms())granted++;
  if(Build.VERSION.SDK_INT<26||ctx.getPackageManager().canRequestPackageInstalls())granted++;
  return granted;
 }
 private boolean granted(int step){
  switch(step){
   case NOTIFICATIONS:
    return Build.VERSION.SDK_INT<33||
     activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
   case OVERLAY:return Settings.canDrawOverlays(activity);
   case EXACT_ALARMS:
    if(Build.VERSION.SDK_INT<31)return true;
    AlarmManager alarm=(AlarmManager)activity.getSystemService(Context.ALARM_SERVICE);
    return alarm!=null&&alarm.canScheduleExactAlarms();
   case INSTALL_APKS:
    return Build.VERSION.SDK_INT<26||activity.getPackageManager().canRequestPackageInstalls();
   default:return true;
  }
 }
 private String name(int step){
  switch(step){
   case NOTIFICATIONS:return tr("Meldingen","Notifications");
   case OVERLAY:return tr("Weergeven over andere apps","Display over other apps");
   case EXACT_ALARMS:return tr("Exacte alarmen","Exact alarms");
   default:return tr("Apps installeren","Install APK updates");
  }
 }
 private String reason(int step){
  switch(step){
   case NOTIFICATIONS:return tr(
    "Ontvang een melding wanneer een builder, laboratorium- of petupgrade klaar is.",
    "Get alerts when a builder, laboratory or pet upgrade finishes.");
   case OVERLAY:return tr(
    "Toont de optionele zwevende Sync-knop boven Clash of Clans. Je zet de knop zelf aan in Instellingen.",
    "Allows the optional floating Sync button over Clash of Clans. You still enable it manually in Settings.");
   case EXACT_ALARMS:return tr(
    "Maakt nauwkeurige waarschuwingen op het ingestelde eindtijdstip mogelijk.",
    "Allows accurate alerts at the scheduled upgrade finish time.");
   default:return tr(
    "Laat ClashTimerHub een nieuwe APK ter installatie aan Android aanbieden. Jij bevestigt elke installatie.",
    "Lets ClashTimerHub offer a downloaded APK to Android. You still confirm every installation.");
  }
 }
 private void next(){
  if(!running||activity.isFinishing()||activity.isDestroyed())return;
  while(current<4&&granted(current))current++;
  if(current>=4){finish();return;}
  int step=current;
  LinearLayout box=new LinearLayout(activity);
  box.setOrientation(LinearLayout.VERTICAL);
  int pad=(int)(18*activity.getResources().getDisplayMetrics().density);
  box.setPadding(pad,pad/2,pad,pad/2);
  TextView progress=new TextView(activity);
  progress.setText(tr("Stap ","Step ")+(step+1)+" / 4  •  "+grantedCount(activity)+"/4 "+
   tr("toegestaan","granted"));
  progress.setTextColor(0xffa98bd4);progress.setTextSize(13);box.addView(progress);
  TextView description=new TextView(activity);
  description.setText("\n"+reason(step)+"\n\n"+tr(
   "Android opent zijn eigen toestemmingsscherm. Ga daarna terug naar ClashTimerHub voor de volgende stap.",
   "Android opens its own permission screen. Return to ClashTimerHub afterward for the next step."));
  description.setTextSize(15);box.addView(description);
  displayed=new AlertDialog.Builder(activity)
   .setTitle(tr("Toestemmingen instellen","Set up permissions")+" — "+name(step))
   .setView(box)
   .setPositiveButton(tr("Toestemming geven","Grant permission"),(d,w)->request(step))
   .setNeutralButton(tr("Overslaan","Skip"),(d,w)->{current++;next();})
   .setNegativeButton(tr("Later","Later"),(d,w)->end(false))
   .setOnCancelListener(d->end(false))
   .create();
  displayed.show();
 }
 private void request(int step){
  try{
   if(step==NOTIFICATIONS){
    if(Build.VERSION.SDK_INT>=33){
     activity.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFICATION_REQUEST);
     return;
    }
   }else{
    Intent intent;
    if(step==OVERLAY)intent=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
     Uri.parse("package:"+activity.getPackageName()));
    else if(step==EXACT_ALARMS)intent=new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
     Uri.parse("package:"+activity.getPackageName()));
    else intent=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
     Uri.parse("package:"+activity.getPackageName()));
    waitingForSettings=true;
    activity.startActivity(intent);
    return;
   }
  }catch(Exception ex){
   waitingForSettings=false;
   android.widget.Toast.makeText(activity,tr("Kan instelling niet openen: ","Unable to open Settings: ")+
    ex.getMessage(),android.widget.Toast.LENGTH_LONG).show();
  }
  current++;next();
 }
 private void finish(){
  int count=grantedCount(activity);
  end(true);
  if(activity.isFinishing()||activity.isDestroyed())return;
  new AlertDialog.Builder(activity)
   .setTitle(count==4?tr("Alles ingesteld","All set"):
    tr("Toestemmingen gecontroleerd","Permissions reviewed"))
   .setMessage(count+"/4 "+tr("toestemmingen actief. Je kunt ontbrekende toestemmingen later via Instellingen geven.",
    "permissions enabled. You can grant missing ones later via Settings."))
   .setPositiveButton(tr("Gereed","Done"),(d,w)->{})
   .setNeutralButton(count<4?tr("Opnieuw proberen","Try again"):tr("Sluiten","Close"),
    (d,w)->{if(count<4)start(true);})
   .show();
 }
 private void end(boolean completed){
  running=false;waitingForSettings=false;displayed=null;
  prefs().edit().putBoolean("permissionsWizardSeen",true).apply();
 }
}
