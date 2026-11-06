package nl.clashtimerhub.v2;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

/** Separate optional prewarnings and completion alerts for boosted Builder/Lab upgrades. */
public final class PotionCompletionAlarm extends BroadcastReceiver {
 private static final String ACTION="nl.clashtimerhub.v2.POTION_COMPLETION";
 private static final String CHANNEL="potion_completion_v1";
 private static final String KEY="scheduledPotionCompletions";

 private static int lead(SharedPreferences p){
  return Math.max(0,Math.min(10080,p.getInt("potionNotifyLeadMinutes",0)));
 }
 private static boolean enabled(SharedPreferences p,boolean early){
  if(!p.getBoolean("notifyEnabled",true))return false;
  return early?lead(p)>0:p.getBoolean("potionNotifyOnFinish",p.getBoolean("potionNotifyEnabled",true));
 }
 private static PendingIntent alarmIntent(Context ctx,int code,String account,String category,String id,
   int row,long original,long predicted,boolean early){
  Intent intent=new Intent(ctx,PotionCompletionAlarm.class).setAction(ACTION);
  intent.putExtra("account",account).putExtra("category",category).putExtra("id",id)
   .putExtra("row",row).putExtra("original",original).putExtra("predicted",predicted)
   .putExtra("early",early);
  return PendingIntent.getBroadcast(ctx,code,intent,
   PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 private static PendingIntent oldIntent(Context ctx,int code){
  return PendingIntent.getBroadcast(ctx,code,
   new Intent(ctx,PotionCompletionAlarm.class).setAction(ACTION),
   PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);
 }
 private static long estimate(Context ctx,String account,String category,long finish){
  if("Bouwers".equals(category))return BuilderPotionTracker.estimateFinish(BuilderPotionTracker.load(ctx,account),finish);
  if("Laboratorium".equals(category))return ResearchPotionTracker.estimateFinish(ResearchPotionTracker.load(ctx,account),finish);
  return finish;
 }
 private static void scheduleAlarm(AlarmManager mgr,long when,PendingIntent pi){
  if(Build.VERSION.SDK_INT>=23){
   if(Build.VERSION.SDK_INT<31||mgr.canScheduleExactAlarms()){
    try{mgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);return;}
    catch(SecurityException e){Log.w("ClashTimerHub","Exact Potion alarm denied",e);}
   }
   mgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);
  }else mgr.set(AlarmManager.RTC_WAKEUP,when,pi);
 }
 static synchronized void schedule(Context ctx){
  AlarmManager mgr=(AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
  if(mgr==null)return;
  SharedPreferences prefs=AppData.prefs(ctx);
  try{
   JSONArray previous=new JSONArray(prefs.getString(KEY,"[]"));
   for(int i=0;i<previous.length();i++){
    PendingIntent pi=oldIntent(ctx,previous.optInt(i));
    if(pi!=null){mgr.cancel(pi);pi.cancel();}
   }
  }catch(Exception e){Log.w("ClashTimerHub","Cannot cancel previous Potion alarms",e);}
  JSONArray scheduled=new JSONArray();
  if(!enabled(prefs,false)&&!enabled(prefs,true)){
   prefs.edit().putString(KEY,scheduled.toString()).apply();return;
  }
  long now=System.currentTimeMillis();
  try{
   JSONArray upgrades=new JSONArray(prefs.getString("upgrades","[]"));
   for(int i=0;i<upgrades.length();i++){
    JSONObject row=upgrades.optJSONObject(i);
    if(row==null)continue;
    String account=row.optString("account",""),category=row.optString("category","");
    if(account.isEmpty()||(!"Bouwers".equals(category)&&!"Laboratorium".equals(category)))continue;
    long original=row.optLong("finish",0L);
    if(original<=now)continue;
    long predicted=estimate(ctx,account,category,original);
    if(predicted>=original||predicted<=now)continue;
    String id=row.optString("id","row-"+i);
    for(int kind=0;kind<2;kind++){
     boolean early=kind==1;
     if(!enabled(prefs,early))continue;
     long when=early?predicted-lead(prefs)*60000L:predicted;
     if(when<=now||when>=original)continue;
     // Include index and original finish; otherwise identical upgrade IDs collide.
     int code=("potion:"+account+":"+category+":"+id+":"+i+":"+original+":"+kind).hashCode();
     PendingIntent pi=alarmIntent(ctx,code,account,category,id,i,original,predicted,early);
     try{scheduleAlarm(mgr,when,pi);scheduled.put(code);}
     catch(Exception e){Log.w("ClashTimerHub","Potion alarm scheduling failed",e);}
    }
   }
  }catch(Exception e){Log.e("ClashTimerHub","Potion alarms could not be rebuilt",e);}
  prefs.edit().putString(KEY,scheduled.toString()).apply();
 }
 @Override public void onReceive(Context ctx,Intent intent){
  if(intent==null||!ACTION.equals(intent.getAction()))return;
  SharedPreferences prefs=AppData.prefs(ctx);
  boolean early=intent.getBooleanExtra("early",false);
  if(!enabled(prefs,early))return;
  String account=intent.getStringExtra("account"),category=intent.getStringExtra("category"),
   id=intent.getStringExtra("id");
  int row=intent.getIntExtra("row",-1);
  long original=intent.getLongExtra("original",0L),predicted=intent.getLongExtra("predicted",0L);
  if(account==null||category==null||id==null||original<=0||predicted<=0)return;
  long now=System.currentTimeMillis();
  long when=early?predicted-lead(prefs)*60000L:predicted;
  if(now<when||(early&&now>=predicted))return;
  JSONObject matched=null;
  try{
   JSONArray current=new JSONArray(prefs.getString("upgrades","[]"));
   for(int i=0;i<current.length();i++){
    if(row>=0&&i!=row)continue;
    JSONObject upgrade=current.optJSONObject(i);
    if(upgrade!=null&&account.equals(upgrade.optString("account"))
      &&category.equals(upgrade.optString("category"))
      &&original==upgrade.optLong("finish",0L)
      &&id.equals(upgrade.optString("id",row>=0?"row-"+i:account+":"+category+":"+i))){
     matched=upgrade;break;
    }
   }
  }catch(Exception e){Log.w("ClashTimerHub","Potion alarm validation failed",e);}
  if(matched==null||Math.abs(estimate(ctx,account,category,original)-predicted)>2000L)return;
  if(Build.VERSION.SDK_INT>=33&&ctx.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
   !=android.content.pm.PackageManager.PERMISSION_GRANTED)return;
  NotificationManager manager=(NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
  if(manager==null)return;
  if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(new NotificationChannel(CHANNEL,
   I18n.english(ctx)?"Potion upgrade completions":"Potion-versnelde upgrades",
   NotificationManager.IMPORTANCE_HIGH));
  String name=matched.optString("name",id);
  boolean english=I18n.english(ctx);
  String title=AppData.displayName(ctx,account)+(english?
   (early?" · Boosted upgrade nearly ready":" · Boosted upgrade complete"):
   (early?" · Versnelde upgrade bijna klaar":" · Versnelde upgrade klaar"));
  String type="Bouwers".equals(category)?"Builder Potion":"Research Potion";
  String description=early?name+" · "+Math.max(1,(predicted-now+59999L)/60000L)+
   (english?" min remaining":" min resterend"):name+" · "+type;
  Intent open=new Intent(ctx,MainActivity.class)
   .setAction("nl.clashtimerhub.v2.OPEN_POTION_ALARM_"+category+"_"+id);
  open.putExtra("openAccount",account);
  PendingIntent clicked=PendingIntent.getActivity(ctx,
   ("potion:click:"+account+":"+category+":"+id+":"+row).hashCode(),open,
   PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder notification=Build.VERSION.SDK_INT>=26?
   new Notification.Builder(ctx,CHANNEL):new Notification.Builder(ctx);
  notification.setSmallIcon(android.R.drawable.ic_dialog_info)
   .setContentTitle(title).setContentText(description)
   .setAutoCancel(true).setContentIntent(clicked);
  manager.notify(("potion:ready:"+account+":"+category+":"+id+":"+row+":"+original+":"+early).hashCode(),
   notification.build());
  // Alarm schedule is stable: don't rebuild it in the receiver and risk
  // dropping another upgrade that is due at the same moment.
 }
}
