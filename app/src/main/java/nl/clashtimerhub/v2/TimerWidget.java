package nl.clashtimerhub.v2;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.Build;
import android.widget.RemoteViews;
import org.json.*;
import java.util.*;

public final class TimerWidget extends AppWidgetProvider {
 static final String CHANNEL="builder_ready_v1";
 static void update(Context context) {
  AppWidgetManager mgr=AppWidgetManager.getInstance(context);
  android.content.ComponentName name=new android.content.ComponentName(context,TimerWidget.class);
  int[] ids=mgr.getAppWidgetIds(name);
  for(int id:ids) {
   RemoteViews views=new RemoteViews(context.getPackageName(),R.layout.builder_widget);
   views.setTextViewText(R.id.widget_title,"CLASH TIMER HUB");
   views.setTextViewText(R.id.widget_accounts,summary(context));
   Intent open=new Intent(context,MainActivity.class);
   views.setOnClickPendingIntent(R.id.widget_title,PendingIntent.getActivity(context,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
   mgr.updateAppWidget(id,views);
  }
 }
 static String summary(Context context) {
  android.content.SharedPreferences prefs=context.getSharedPreferences("MainActivity",Context.MODE_PRIVATE);
  try {
   JSONArray accounts=new JSONArray(prefs.getString("accounts","[]"));
   JSONArray upgrades=new JSONArray(prefs.getString("upgrades","[]"));
   long now=System.currentTimeMillis();
   StringBuilder out=new StringBuilder();
   for(int a=0;a<accounts.length();a++){
    String ac=accounts.getString(a);long soonest=Long.MAX_VALUE;String item="";
    for(int i=0;i<upgrades.length();i++){
     JSONObject o=upgrades.getJSONObject(i);
     long finish=o.optLong("finish");
     if(ac.equals(o.optString("account"))&&"Bouwers".equals(o.optString("category"))&&finish>now&&finish<soonest){
      soonest=finish;item=o.optString("name");
     }
    }
    if(out.length()>0)out.append("\n");
    out.append(ac).append(": ");
    if(soonest==Long.MAX_VALUE)out.append("geen actieve timer");
    else out.append(item).append(" · ").append(Math.max(1,(soonest-now+59999)/60000)).append(" min");
   }
   return out.toString();
  }catch(Exception e){return "Open de app om accounts te synchroniseren";}
 }
 @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){update(context);}
 @Override public void onReceive(Context context,Intent intent){super.onReceive(context,intent);if(AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction()))update(context);}
 static void schedule(Context context) {
  android.content.SharedPreferences prefs=context.getSharedPreferences("MainActivity",Context.MODE_PRIVATE);
  try {
   JSONArray upgrades=new JSONArray(prefs.getString("upgrades","[]"));
   long now=System.currentTimeMillis();
   AlarmManager alarms=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
   if(alarms==null)return;
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
     PendingIntent pi=PendingIntent.getBroadcast(context,request,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
     alarms.cancel(pi);
     long trigger=kind==1?finish-lead*60000L:finish;
     boolean active=enabled&&(kind==0?onFinish:lead>0);
     if(active&&trigger>now&&finish>now){
      if(Build.VERSION.SDK_INT>=31&&alarms.canScheduleExactAlarms()){
       alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,trigger,pi);
      }else if(Build.VERSION.SDK_INT>=23&&Build.VERSION.SDK_INT<31){
       alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,trigger,pi);
      }else if(Build.VERSION.SDK_INT>=31){
       // Exact-alarm permission denied: alarm-clock scheduling remains exact and user-visible.
       PendingIntent show=PendingIntent.getActivity(context,0,new Intent(context,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
       alarms.setAlarmClock(new AlarmManager.AlarmClockInfo(trigger,show),pi);
      }else{
       alarms.set(AlarmManager.RTC_WAKEUP,trigger,pi);
      }
     }
    }
   }
  }catch(Exception e){android.util.Log.e("ClashTimerHub","Scheduling builder alarms failed",e);}
  update(context);
 }
 static void scheduleDiagnostic(Context context){
  AlarmManager alarms=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
  if(alarms==null)return;
  Intent intent=new Intent(context,BuilderAlarm.class).setAction("nl.clashtimerhub.v2.TEST_SCHEDULED_ALARM");
  PendingIntent pi=PendingIntent.getBroadcast(context,90117,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  long when=System.currentTimeMillis()+60000L;
  if(Build.VERSION.SDK_INT>=31&&!alarms.canScheduleExactAlarms()){
   PendingIntent show=PendingIntent.getActivity(context,0,new Intent(context,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   alarms.setAlarmClock(new AlarmManager.AlarmClockInfo(when,show),pi);
  }else if(Build.VERSION.SDK_INT>=23)alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);
  else alarms.setExact(AlarmManager.RTC_WAKEUP,when,pi);
 }
}
