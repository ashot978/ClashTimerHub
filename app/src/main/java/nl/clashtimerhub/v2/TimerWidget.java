package nl.clashtimerhub.v2;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.*;
import android.view.View;
import android.widget.RemoteViews;
import org.json.*;
import java.util.*;

/** V5 responsive, live-counting home-screen widget. */
public class TimerWidget extends AppWidgetProvider {
 static final String CHANNEL="builder_ready_v1";
 private static final String REFRESH="nl.clashtimerhub.v2.WIDGET_REFRESH";
 private static final int REFRESH_ID=90118;
 static final String UPGRADE_ACTION="nl.clashtimerhub.v2.UNIFIED_UPGRADE_COMPLETE";
 private static final String UPGRADE_CODES="unifiedUpgradeAlarmCodes";

 private static final class Status {
  final String account;
  String name="";
  long next=Long.MAX_VALUE;
  int ready;
  Status(String name){account=name;}
 }
 private static ArrayList<Status> states(Context context,long now){
  AppData.migrate(context);
  android.content.SharedPreferences prefs=AppData.prefs(context);
  ArrayList<Status> result=new ArrayList<>();
  try{
   JSONArray accounts=new JSONArray(prefs.getString("accounts","[]"));
   JSONArray upgrades=new JSONArray(prefs.getString("upgrades","[]"));
   for(int a=0;a<accounts.length();a++){
    String ac=accounts.getString(a);Status s=new Status(ac);
    long ack=prefs.getLong("ack_"+ac,0);
    for(int i=0;i<upgrades.length();i++){
     JSONObject o=upgrades.getJSONObject(i);
     if(!ac.equals(o.optString("account"))||!"Bouwers".equals(o.optString("category")))continue;
     long finish=o.optLong("finish");
     if(finish<=now&&finish>ack)s.ready++;
     if(finish>now&&finish<s.next){s.next=finish;s.name=o.optString("name");}
    }
    result.add(s);
   }
   Collections.sort(result,(a,b)->{
    if(a.ready!=b.ready)return Integer.compare(b.ready,a.ready);
    return Long.compare(a.next,b.next);
   });
  }catch(Exception e){android.util.Log.e("ClashTimerHub","Widget state failed",e);}
  return result;
 }
 private static String endAt(Context context,long timestamp){
  java.util.Calendar today=java.util.Calendar.getInstance();
  java.util.Calendar end=java.util.Calendar.getInstance();end.setTimeInMillis(timestamp);
  Locale userLocale=I18n.english(context)?Locale.UK:Locale.forLanguageTag("nl-NL");
  String hhmm=new java.text.SimpleDateFormat("HH:mm",userLocale).format(new Date(timestamp));
  if(today.get(java.util.Calendar.YEAR)==end.get(java.util.Calendar.YEAR)&&today.get(java.util.Calendar.DAY_OF_YEAR)==end.get(java.util.Calendar.DAY_OF_YEAR))
   return I18n.tr(context,"vandaag ")+hhmm;
  today.add(java.util.Calendar.DAY_OF_YEAR,1);
  if(today.get(java.util.Calendar.YEAR)==end.get(java.util.Calendar.YEAR)&&today.get(java.util.Calendar.DAY_OF_YEAR)==end.get(java.util.Calendar.DAY_OF_YEAR))
   return I18n.tr(context,"morgen ")+hhmm;
  return new java.text.SimpleDateFormat("d MMM HH:mm",userLocale).format(new Date(timestamp));
 }
 private static RemoteViews makeAccount(Context context,Status s,boolean expanded,long now){
  RemoteViews row=new RemoteViews(context.getPackageName(),expanded?R.layout.widget_account_compact:R.layout.widget_account_mini);
  row.setTextViewText(R.id.widget_account_name,s.account);
  row.setViewVisibility(R.id.widget_ready,s.ready>0?View.VISIBLE:View.GONE);
  if(s.ready>0)row.setTextViewText(R.id.widget_ready,"●"+s.ready);
  boolean active=s.next!=Long.MAX_VALUE;
  row.setViewVisibility(R.id.widget_timer,active?View.VISIBLE:View.GONE);
  row.setViewVisibility(R.id.widget_idle,active?View.GONE:View.VISIBLE);
  if(active){
   row.setChronometer(R.id.widget_timer,SystemClock.elapsedRealtime()+Math.max(0,s.next-now),null,true);
   row.setChronometerCountDown(R.id.widget_timer,true);
  }else row.setTextViewText(R.id.widget_idle,s.ready>0?I18n.tr(context,"Gereed"):"—");
  if(expanded)row.setTextViewText(R.id.widget_subtitle,active?I18n.tr(context,"Klaar ")+endAt(context,s.next):I18n.tr(context,s.ready>0?"Builder beschikbaar":"Geen actieve builder"));
  Intent open=new Intent(context,MainActivity.class);
  open.setAction("nl.clashtimerhub.v2.OPEN_ACCOUNT."+s.account);
  open.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
  open.putExtra("openAccount",s.account);
  PendingIntent click=PendingIntent.getActivity(context,s.account.hashCode(),open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  row.setOnClickPendingIntent(R.id.widget_account_root,click);
  return row;
 }
 static void update(Context context){
  WidgetV7.updateAll(context);
  AllTimersTestWidget.update(context);
  // Never schedule a second, Potion-specific notification during redraw.
 }
 @Override public void onDeleted(Context context,int[] widgetIds){
  android.content.SharedPreferences.Editor editor=context.getSharedPreferences("WidgetPreferences",Context.MODE_PRIVATE).edit();
  for(int id:widgetIds){editor.remove("all_"+id);editor.remove("selected_"+id);editor.remove("account_"+id);}
  editor.apply();super.onDeleted(context,widgetIds);
 }
 static String summary(Context context){
  ArrayList<Status> s=states(context,System.currentTimeMillis());
  StringBuilder b=new StringBuilder();
  for(Status a:s){if(b.length()>0)b.append("\n");b.append(a.account).append(": ");
   b.append(a.ready>0?I18n.tr(context,a.ready+" klaar"):a.next!=Long.MAX_VALUE?endAt(context,a.next):I18n.tr(context,"geen timer"));
  }return b.toString();
 }
 @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){update(context);}
 @Override public void onAppWidgetOptionsChanged(Context context,AppWidgetManager manager,int id,android.os.Bundle options){super.onAppWidgetOptionsChanged(context,manager,id,options);update(context);}
 @Override public void onReceive(Context context,Intent intent){
  super.onReceive(context,intent);
  if(intent==null)return;
  String action=intent.getAction();
  if(REFRESH.equals(action))schedule(context);
  else if(AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action))update(context);
 }
 private static PendingIntent alarmIntent(Context ctx,int code,Intent extras,int flags){
  Intent action=new Intent(ctx,BuilderAlarm.class).setAction(UPGRADE_ACTION);
  if(extras!=null)action.putExtras(extras);
  return PendingIntent.getBroadcast(ctx,code,action,flags|PendingIntent.FLAG_IMMUTABLE);
 }
 private static void clearPrevious(Context ctx,AlarmManager mgr,android.content.SharedPreferences prefs){
  try{
   JSONArray codes=new JSONArray(prefs.getString(UPGRADE_CODES,"[]"));
   for(int i=0;i<codes.length();i++){
    PendingIntent previous=alarmIntent(ctx,codes.optInt(i),null,PendingIntent.FLAG_NO_CREATE);
    if(previous!=null){mgr.cancel(previous);previous.cancel();}
   }
  }catch(Exception e){android.util.Log.w("ClashTimerHub","Cannot cancel previous upgrade alarms",e);}
  PotionCompletionAlarm.cancelAll(ctx);
 }
 private static void cancelLegacy(Context ctx,AlarmManager mgr,String id,int kind){
  int request=(id+":"+kind).hashCode();
  PendingIntent old=PendingIntent.getBroadcast(ctx,request,new Intent(ctx,BuilderAlarm.class),
   PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);
  if(old!=null){mgr.cancel(old);old.cancel();}
 }
 private static void scheduleWidgetRefresh(Context ctx,AlarmManager mgr,JSONArray upgrades,long now){
  PendingIntent operation=PendingIntent.getBroadcast(ctx,REFRESH_ID,
   new Intent(ctx,TimerWidget.class).setAction(REFRESH),
   PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  mgr.cancel(operation);
  long earliest=Long.MAX_VALUE;
  for(int i=0;i<upgrades.length();i++){
   JSONObject row=upgrades.optJSONObject(i);if(row==null)continue;
   long source=row.optLong("finish",0L);if(source<=0)continue;
   long finish=UpgradeForecast.finish(ctx,row.optString("account"),row.optString("category"),source);
   if(finish>now&&finish<earliest)earliest=finish;
  }
  if(earliest!=Long.MAX_VALUE)try{scheduleOne(mgr,earliest+1000,operation);}
  catch(Exception e){android.util.Log.w("ClashTimerHub","Widget refresh unavailable",e);}
 }
 static synchronized void schedule(Context ctx){
  android.content.SharedPreferences prefs=AppData.prefs(ctx);
  AlarmManager mgr=(AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
  if(mgr==null){update(ctx);return;}
  clearPrevious(ctx,mgr,prefs);
  JSONArray scheduled=new JSONArray();
  try{
   JSONArray upgrades=new JSONArray(prefs.getString("upgrades","[]"));
   long now=System.currentTimeMillis();
   boolean enabled=prefs.getBoolean("notifyEnabled",true);
   boolean completion=prefs.getBoolean("notifyOnFinish",true);
   int lead=Math.max(0,Math.min(10080,prefs.getInt("notifyLeadMinutes",15)));
   for(int i=0;i<upgrades.length();i++){
    JSONObject row=upgrades.optJSONObject(i);if(row==null)continue;
    String ac=row.optString("account",""),cat=row.optString("category","");
    String id=row.optString("id","row-"+i);
    long original=row.optLong("finish",0L);
    if(ac.isEmpty()||original<=0)continue;
    long finish=UpgradeForecast.finish(ctx,ac,cat,original);
    for(int kind=0;kind<2;kind++){
     cancelLegacy(ctx,mgr,id,kind);
     boolean early=kind==1;
     if(!enabled||(early?lead<=0:!completion)||finish<=now)continue;
     long when=early?finish-lead*60000L:finish;
     if(when<=now)continue;
     int code=("unified:"+ac+":"+cat+":"+id+":"+i+":"+kind).hashCode();
     Intent extras=new Intent().putExtra("account",ac).putExtra("category",cat)
      .putExtra("id",id).putExtra("row",i).putExtra("name",row.optString("name",""))
      .putExtra("sourceFinish",original).putExtra("finish",finish).putExtra("early",early);
     PendingIntent operation=alarmIntent(ctx,code,extras,PendingIntent.FLAG_UPDATE_CURRENT);
     try{scheduleOne(mgr,when,operation);scheduled.put(code);}
     catch(Exception e){android.util.Log.e("ClashTimerHub","Upgrade alert scheduling failed: "+id,e);}
    }
   }
   scheduleWidgetRefresh(ctx,mgr,upgrades,now);
  }catch(Exception e){android.util.Log.e("ClashTimerHub","Scheduling alarms failed",e);}
  prefs.edit().putString(UPGRADE_CODES,scheduled.toString()).apply();
  update(ctx);
 }
 private static boolean scheduleOne(AlarmManager alarms,long time,PendingIntent op){
  if(Build.VERSION.SDK_INT>=23){
   if(Build.VERSION.SDK_INT<31||alarms.canScheduleExactAlarms()){
    try{alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time,op);return true;}
    catch(SecurityException e){android.util.Log.w("ClashTimerHub","Exact alarm denied",e);}
   }
   alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time,op);
   return false;
  }
  alarms.set(AlarmManager.RTC_WAKEUP,time,op);
  return false;
 }
 static String scheduleDiagnostic(Context context){
  try{
   AlarmManager alarms=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
   if(alarms==null)return "Alarmservice niet beschikbaar";
   Intent intent=new Intent(context,BuilderAlarm.class).setAction("nl.clashtimerhub.v2.TEST_SCHEDULED_ALARM");
   PendingIntent operation=PendingIntent.getBroadcast(context,90117,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   alarms.cancel(operation);
   boolean exact=scheduleOne(alarms,System.currentTimeMillis()+60000L,operation);
   return exact?"Alarmtest gepland over 1 minuut. Je kunt de app verlaten.":"Alarmtest gepland, maar exacte alarmtoestemming ontbreekt. De melding kan later komen. Zet 'Exacte alarmen' aan via Meldingen instellen.";
  }catch(Exception e){android.util.Log.e("ClashTimerHub","Alarmtest plannen mislukt",e);
   return "Alarmtest mislukt: "+e.getClass().getSimpleName()+". Controleer de Android-alarmrechten.";
  }
 }
}
