package nl.clashtimerhub.v2;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONArray;

/** Migration-only: suppress and cancel the obsolete parallel Potion alarms. */
public final class PotionCompletionAlarm extends BroadcastReceiver {
 private static final String ACTION="nl.clashtimerhub.v2.POTION_COMPLETION";
 private static final String KEY="scheduledPotionCompletions";
 static synchronized void cancelAll(Context ctx){
  AlarmManager mgr=(AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
  SharedPreferences prefs=AppData.prefs(ctx);
  try{
   JSONArray history=new JSONArray(prefs.getString(KEY,"[]"));
   for(int i=0;i<history.length();i++){
    int code=history.optInt(i);
    PendingIntent pi=PendingIntent.getBroadcast(ctx,code,
     new Intent(ctx,PotionCompletionAlarm.class).setAction(ACTION),
     PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);
    if(pi!=null){if(mgr!=null)mgr.cancel(pi);pi.cancel();}
   }
  }catch(Exception e){Log.w("ClashTimerHub","Cannot cancel prior Potion alarms",e);}
  prefs.edit().putString(KEY,"[]").apply();
 }
 static void schedule(Context ctx){cancelAll(ctx);}
 @Override public void onReceive(Context ctx,Intent intent){
  // Ignore any alarm still held by Android from an older APK.
 }
}
