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
public final class TimerWidget extends AppWidgetProvider {
 static final String CHANNEL="builder_ready_v1";
 private static final String REFRESH="nl.clashtimerhub.v2.WIDGET_REFRESH";
 private static final int REFRESH_ID=90118;

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
 private static String endAt(long timestamp){
  java.util.Calendar today=java.util.Calendar.getInstance();
  java.util.Calendar end=java.util.Calendar.getInstance();end.setTimeInMillis(timestamp);
  String hhmm=new java.text.SimpleDateFormat("HH:mm",Locale.forLanguageTag("nl-NL")).format(new Date(timestamp));
  if(today.get(java.util.Calendar.YEAR)==end.get(java.util.Calendar.YEAR)&&today.get(java.util.Calendar.DAY_OF_YEAR)==end.get(java.util.Calendar.DAY_OF_YEAR))
   return "vandaag "+hhmm;
  today.add(java.util.Calendar.DAY_OF_YEAR,1);
  if(today.get(java.util.Calendar.YEAR)==end.get(java.util.Calendar.YEAR)&&today.get(java.util.Calendar.DAY_OF_YEAR)==end.get(java.util.Calendar.DAY_OF_YEAR))
   return "morgen "+hhmm;
  return new java.text.SimpleDateFormat("d MMM HH:mm",Locale.forLanguageTag("nl-NL")).format(new Date(timestamp));
 }
 private static RemoteViews makeAccount(Context context,Status s,boolean expanded,long now){
  RemoteViews row=new RemoteViews(context.getPackageName(),expanded?R.layout.widget_account_expanded:R.layout.widget_account_compact);
  row.setTextViewText(R.id.widget_account_name,s.account);
  row.setViewVisibility(R.id.widget_ready,s.ready>0?View.VISIBLE:View.GONE);
  if(s.ready>0)row.setTextViewText(R.id.widget_ready,"● "+s.ready+" klaar");
  boolean active=s.next!=Long.MAX_VALUE;
  row.setViewVisibility(R.id.widget_timer,active?View.VISIBLE:View.GONE);
  row.setViewVisibility(R.id.widget_idle,active?View.GONE:View.VISIBLE);
  if(active){
   row.setChronometer(R.id.widget_timer,SystemClock.elapsedRealtime()+Math.max(0,s.next-now),null,true);
   row.setChronometerCountDown(R.id.widget_timer,true);
  }else row.setTextViewText(R.id.widget_idle,s.ready>0?"GEREED":"—");
  if(expanded)row.setTextViewText(R.id.widget_subtitle,active?s.name+" · "+endAt(s.next):"Geen actieve builder");
  Intent open=new Intent(context,MainActivity.class);
  open.setAction("nl.clashtimerhub.v2.OPEN_ACCOUNT."+s.account);
  open.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
  open.putExtra("openAccount",s.account);
  PendingIntent click=PendingIntent.getActivity(context,s.account.hashCode(),open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  row.setOnClickPendingIntent(R.id.widget_account_root,click);
  return row;
 }
 static void update(Context context){
  AppWidgetManager manager=AppWidgetManager.getInstance(context);
  int[] ids=manager.getAppWidgetIds(new android.content.ComponentName(context,TimerWidget.class));
  long now=System.currentTimeMillis();
  ArrayList<Status> data=states(context,now);
  int totalReady=0;for(Status s:data)totalReady+=s.ready;
  for(int widgetId:ids){
   android.os.Bundle options=manager.getAppWidgetOptions(widgetId);
   if(options==null)options=new android.os.Bundle();
   boolean landscape=context.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
   int width=options.getInt(landscape?AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH:AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,250);
   int height=options.getInt(landscape?AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT:AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,180);
   if(width<=0)width=250;if(height<=0)height=180;
   boolean wide=width>=420;
   boolean expanded=height>=205;
   int rowHeight=expanded?56:39;
   int displayRows=Math.max(1,(height-50)/rowHeight);
   int slots=Math.min(data.size(),Math.max(1,displayRows*(wide?2:1)));
   RemoteViews widget=new RemoteViews(context.getPackageName(),R.layout.builder_widget);
   widget.setTextViewText(R.id.widget_title,"⚔ CLASH TIMER HUB");
   widget.setTextViewText(R.id.widget_summary,data.size()+" accounts · "+totalReady+" klaar");
   widget.removeAllViews(R.id.widget_rows);
   for(int i=0;i<slots;){
    if(wide){
     RemoteViews pair=new RemoteViews(context.getPackageName(),R.layout.widget_pair);
     pair.addView(R.id.widget_left,makeAccount(context,data.get(i++),expanded,now));
     if(i<slots)pair.addView(R.id.widget_right,makeAccount(context,data.get(i++),expanded,now));
     widget.addView(R.id.widget_rows,pair);
    }else widget.addView(R.id.widget_rows,makeAccount(context,data.get(i++),expanded,now));
   }
   int hidden=Math.max(0,data.size()-slots);
   widget.setViewVisibility(R.id.widget_more,hidden>0?View.VISIBLE:View.GONE);
   if(hidden>0)widget.setTextViewText(R.id.widget_more,"+"+hidden+" accounts · vergroot voor meer");
   Intent home=new Intent(context,MainActivity.class);
   PendingIntent open=PendingIntent.getActivity(context,90119,home,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   widget.setOnClickPendingIntent(R.id.widget_title,open);
   widget.setOnClickPendingIntent(R.id.widget_more,open);
   try{manager.updateAppWidget(widgetId,widget);}
   catch(RuntimeException e){android.util.Log.e("ClashTimerHub","Widget "+widgetId+" update failed",e);}
  }
 }
 static String summary(Context context){
  ArrayList<Status> s=states(context,System.currentTimeMillis());
  StringBuilder b=new StringBuilder();
  for(Status a:s){if(b.length()>0)b.append("\n");b.append(a.account).append(": ");
   b.append(a.ready>0?a.ready+" klaar":a.next!=Long.MAX_VALUE?endAt(a.next):"geen timer");
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
 private static void scheduleWidgetRefresh(Context context,AlarmManager alarms,JSONArray upgrades,long now){
  PendingIntent operation=PendingIntent.getBroadcast(context,REFRESH_ID,
   new Intent(context,TimerWidget.class).setAction(REFRESH),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  alarms.cancel(operation);
  long earliest=Long.MAX_VALUE;
  for(int i=0;i<upgrades.length();i++){
   JSONObject o=upgrades.optJSONObject(i);
   if(o==null||!"Bouwers".equals(o.optString("category")))continue;
   long finish=o.optLong("finish");
   if(finish>now&&finish<earliest)earliest=finish;
  }
  if(earliest!=Long.MAX_VALUE)try{scheduleOne(alarms,earliest+1000,operation);}catch(Exception e){
   android.util.Log.w("ClashTimerHub","Widget refresh alarm unavailable",e);
  }
 }
 static void schedule(Context context){
  android.content.SharedPreferences prefs=AppData.prefs(context);
  try{
   JSONArray upgrades=new JSONArray(prefs.getString("upgrades","[]"));
   long now=System.currentTimeMillis();
   AlarmManager alarms=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
   if(alarms==null){update(context);return;}
   boolean enabled=prefs.getBoolean("notifyEnabled",true);
   boolean onFinish=prefs.getBoolean("notifyOnFinish",true);
   int lead=Math.max(0,Math.min(10080,prefs.getInt("notifyLeadMinutes",15)));
   for(int i=0;i<upgrades.length();i++){
    JSONObject o=upgrades.getJSONObject(i);
    long finish=o.optLong("finish");
    String key=o.optString("id",o.optString("account")+i);
    for(int kind=0;kind<2;kind++){
     int request=(key+":"+kind).hashCode();
     Intent intent=new Intent(context,BuilderAlarm.class);
     intent.putExtra("account",o.optString("account"));
     intent.putExtra("name",o.optString("name"));
     intent.putExtra("finish",finish);
     intent.putExtra("early",kind==1);
     PendingIntent operation=PendingIntent.getBroadcast(context,request,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
     alarms.cancel(operation);
     long trigger=kind==1?finish-lead*60000L:finish;
     if(enabled&&(kind==0?onFinish:lead>0)&&trigger>now&&finish>now){
      try{scheduleOne(alarms,trigger,operation);}
      catch(Exception e){android.util.Log.e("ClashTimerHub","Timer alarm schedule failed for "+key,e);}
     }
    }
   }
   scheduleWidgetRefresh(context,alarms,upgrades,now);
  }catch(Exception e){android.util.Log.e("ClashTimerHub","Scheduling alarms failed",e);}
  update(context);
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
