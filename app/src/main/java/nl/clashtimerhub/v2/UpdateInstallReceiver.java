package nl.clashtimerhub.v2;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.util.Log;

/** Handles PackageInstaller results and launches Android's user confirmation. */
public final class UpdateInstallReceiver extends BroadcastReceiver {
 static final String ACTION_STATUS="nl.clashtimerhub.v2.ACTION_INSTALL_STATUS";
 @Override public void onReceive(Context context,Intent intent){
  if(intent==null||!ACTION_STATUS.equals(intent.getAction()))return;
  int status=intent.getIntExtra(PackageInstaller.EXTRA_STATUS,PackageInstaller.STATUS_FAILURE);
  String details=intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
  if(status==PackageInstaller.STATUS_PENDING_USER_ACTION){
   Intent confirm;
   if(Build.VERSION.SDK_INT>=33)
    confirm=intent.getParcelableExtra(Intent.EXTRA_INTENT,Intent.class);
   else confirm=(Intent)intent.getParcelableExtra(Intent.EXTRA_INTENT);
   if(confirm==null){
    record(context,"Android leverde geen installatiebevestiging terug.");
    return;
   }
   try{
    confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startActivity(confirm);
   }catch(Exception e){record(context,"Installatiebevestiging kon niet worden geopend: "+e.getMessage());}
   return;
  }
  if(status==PackageInstaller.STATUS_SUCCESS){
   context.getSharedPreferences(MainActivity.class.getName(),Context.MODE_PRIVATE)
    .edit().remove("lastInstallError").remove("lastVerifiedApk")
    .remove("lastVerifiedBuild").remove("lastVerifiedHash").apply();
   return;
  }
  String cause;
  switch(status){
   case PackageInstaller.STATUS_FAILURE_CONFLICT:
    cause="Conflict met de huidige installatie of handtekening";break;
   case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
    cause="APK is niet compatibel met dit toestel";break;
   case PackageInstaller.STATUS_FAILURE_INVALID:
    cause="APK is beschadigd of ongeldig";break;
   case PackageInstaller.STATUS_FAILURE_STORAGE:
    cause="Onvoldoende opslagruimte";break;
   case PackageInstaller.STATUS_FAILURE_BLOCKED:
    cause="Android of toestelbeleid blokkeert de installatie";break;
   case PackageInstaller.STATUS_FAILURE_ABORTED:
    cause="Installatie is geannuleerd";break;
   default:cause="Android heeft de installatie geweigerd (code "+status+")";
  }
  if(details!=null&&!details.isEmpty())cause+=" — "+details;
  record(context,cause);
 }
 private static void record(Context context,String message){
  Log.e("ClashTimerHub","APK install: "+message);
  context.getSharedPreferences(MainActivity.class.getName(),Context.MODE_PRIVATE)
   .edit().putString("lastInstallError",message).apply();
  android.widget.Toast.makeText(context,"ClashTimerHub: "+I18n.tr(context,message),android.widget.Toast.LENGTH_LONG).show();
 }
}
