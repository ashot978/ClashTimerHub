package nl.clashtimerhub.v2;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;

/** Explicit, account-specific early completion notifications for potion forecasts.
 * Never modifies or cancels the original imported upgrade timers and notifications.
 */
public final class PotionCompletionAlarm extends BroadcastReceiver {
 private static final String ACTION="nl.clashtimerhub.v2.POTION_COMPLETION";
 private static final String CHANNEL="potion_completion_v1";
 private static final String KEY="scheduledPotionCompletions";

 private static PendingIntent intent(Context ctx,int code,String account,String id,String category,long source,long predicted){
  Intent i=new Intent(ctx,PotionCompletionAlarm.class).setAction(ACTION);
  i.putExtra("account",account).putExtra("id",id).putExtra("category",category)
   .putExtra("original",source).putExtra("predicted",predicted);
  return PendingIntent.getBroadcast(ctx,code,i,
   PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 private static PendingIntent lookup(Context ctx,int code){
  return PendingIntent.getBroadcast(ctx,code,
   new Intent(ctx,PotionCompletionAlarm.class).setAction(ACTION),
   PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);
 }
 static synchronized void schedule(Context ctx){
  AlarmManager alarms=(AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
  if(alarms==null)return;
  android.content.SharedPreferences prefs=AppData.prefs(ctx);
  // Cancel old forecast alarms before rebuilding from the latest import/boosts.
  try{
   JSONArray last=new JSONArray(prefs.getString(KEY,"[]"));
   for(int n=0;n<last.length();n++){
    PendingIntent old=lookup(ctx,last.optInt(n));
    if(old!=null){alarms.cancel(old);old.cancel();}
   }
  }catch(Exception ex){Log.w("ClashTimerHub","Old potion alarms unavailable",ex);}
  JSONArray scheduled=new JSONArray();
  if(!prefs.getBoolean("notifyEnabled",true)||!prefs.getBoolean("notifyOnFinish",true)){
   prefs.edit().putString(KEY,scheduled.toString()).apply();return;
  }
  long now=System.currentTimeMillis();
  HashSet<Integer> seen=new HashSet<>();
  try{
   JSONArray upgrades=new JSONArray(prefs.getString("upgrades","[]"));
   for(int n=0;n<upgrades.length();n++){
    JSONObject upgrade=upgrades.optJSONObject(n);
    if(upgrade==null)continue;
    String account=upgrade.optString("account","");
    String category=upgrade.optString("category","");
    if(account.isEmpty()||(!"Bouwers".equals(category)&&!"Laboratorium".equals(category)))continue;
    long original=upgrade.optLong("finish",0L);
    if(original<=now)continue;
    boolean builder="Bouwers".equals(category);
    long predicted=builder?BuilderPotionTracker.estimateFinish(BuilderPotionTracker.load(ctx,account),original):
      ResearchPotionTracker.estimateFinish(ResearchPotionTracker.load(ctx,account),original);
    if(predicted>=original||predicted<=now)continue;
    String id=upgrade.optString("id",account+":"+category+":"+n);
    int code=("potion:"+category+":"+account+":"+id).hashCode();
    if(!seen.add(code))continue;
    PendingIntent op=intent(ctx,code,account,id,category,original,predicted);
    try{
     if(Build.VERSION.SDK_INT>=23){
      if(Build.VERSION.SDK_INT<31||alarms.canScheduleExactAlarms())
       alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,predicted,op);
      else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,predicted,op);
     }else alarms.set(AlarmManager.RTC_WAKEUP,predicted,op);
     scheduled.put(code);
    }catch(Exception error){Log.w("ClashTimerHub","Could not schedule potion completion",error);}
   }
  }catch(Exception error){Log.e("ClashTimerHub","Potion schedule failed",error);}
  prefs.edit().putString(KEY,scheduled.toString()).apply();
 }
 @Override public void onReceive(Context ctx,Intent intent){
  if(intent==null||!ACTION.equals(intent.getAction()))return;
  android.content.SharedPreferences prefs=AppData.prefs(ctx);
  if(!prefs.getBoolean("notifyEnabled",true)||!prefs.getBoolean("notifyOnFinish",true))return;
  String account=intent.getStringExtra("account"),id=intent.getStringExtra("id"),category=intent.getStringExtra("category");
  long original=intent.getLongExtra("original",0L),predicted=intent.getLongExtra("predicted",0L);
  if(account==null||id==null||category==null||predicted<=0)return;
  long now=System.currentTimeMillis();
  if(now+60000L<predicted)return;
  boolean matched=false;
  try{
   JSONArray upgrades=new JSONArray(prefs.getString("upgrades","[]"));
   for(int i=0;i<upgrades.length();i++){
    JSONObject entry=upgrades.optJSONObject(i);
    if(entry!=null&&account.equals(entry.optString("account"))
      &&id.equals(entry.optString("id"))
      &&category.equals(entry.optString("category"))
      &&original==entry.optLong("finish",0L)){matched=true;break;}
   }
  }catch(Exception ignored){}
  if(!matched)return;
  long expected="Bouwers".equals(category)?
    BuilderPotionTracker.estimateFinish(BuilderPotionTracker.load(ctx,account),original):
    "Laboratorium".equals(category)?
     ResearchPotionTracker.estimateFinish(ResearchPotionTracker.load(ctx,account),original):0L;
  if(Math.abs(expected-predicted)>2000L||expected>=original)return;
  if(Build.VERSION.SDK_INT>=33&&ctx.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
     !=android.content.pm.PackageManager.PERMISSION_GRANTED)return;
  NotificationManager manager=(NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
  if(manager==null)return;
  if(Build.VERSION.SDK_INT>=26)
   manager.createNotificationChannel(new NotificationChannel(CHANNEL,
    I18n.english(ctx)?"Potion completions":"Potion-upgrades gereed",
    NotificationManager.IMPORTANCE_HIGH));
  String name=id;
  try{
   JSONArray values=new JSONArray(prefs.getString("upgrades","[]"));
   for(int i=0;i<values.length();i++){
    JSONObject item=values.optJSONObject(i);
    if(item!=null&&id.equals(item.optString("id"))&&account.equals(item.optString("account"))){
     name=item.optString("name",id);break;
    }
   }
  }catch(Exception ignored){}
  String display=AppData.displayName(ctx,account);
  String type="Bouwers".equals(category)?"Builder Potion":"Research Potion";
  Intent open=new Intent(ctx,MainActivity.class);
  open.setAction("nl.clashtimerhub.v2.OPEN_POTION_ALARM_"+id);
  open.putExtra("openAccount",account);
  PendingIntent click=PendingIntent.getActivity(ctx,("potion:"+account+":"+id).hashCode(),open,
   PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder message=Build.VERSION.SDK_INT>=26?new Notification.Builder(ctx,CHANNEL):new Notification.Builder(ctx);
  message.setSmallIcon(android.R.drawable.ic_dialog_info)
   .setContentTitle(display+(I18n.english(ctx)?" · Boosted upgrade complete":" · Versnelde upgrade klaar"))
   .setContentText(name+" · "+type)
   .setAutoCancel(true).setContentIntent(click);
  manager.notify(("potion-ready:"+account+":"+id).hashCode(),message.build());
  TimerWidget.update(ctx);
 }
}
