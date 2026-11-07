package nl.clashtimerhub.v2;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;
import org.json.JSONArray;

/** Compact multi-account timers without a timeline or potion lightning icons. */
public final class AllTimersTestWidget extends AppWidgetProvider {
 private static final String REFRESH="nl.clashtimerhub.v2.ALL_TIMERS_TEST_REFRESH";
 // Some launcher implementations suspend RemoteViews Chronometer updates.
 // Rebind only the collection periodically so each row receives a fresh
 // elapsedRealtime-based countdown, even when the app has not been opened.
 private static final String COUNTDOWN_HEARTBEAT="nl.clashtimerhub.v2.ALL_TIMERS_COUNTDOWN_HEARTBEAT";
 private static final int REQUEST_HEARTBEAT=949002;
 private static final long HEARTBEAT_INTERVAL_MS=10L*60L*1000L;
 private static final String OPEN="nl.clashtimerhub.v2.ALL_TIMERS_TEST_OPEN";
 private static final int REQUEST_REFRESH=949001;

 static void update(Context ctx){
  AppWidgetManager manager=AppWidgetManager.getInstance(ctx);
  int[] ids=manager.getAppWidgetIds(new ComponentName(ctx,AllTimersTestWidget.class));
  if(ids.length==0){plan(ctx,Long.MAX_VALUE);planHeartbeat(ctx,Long.MAX_VALUE);return;}
  AllTimersTestData.Snapshot snapshot=AllTimersTestData.load(ctx);
  for(int id:ids){
   try{
    RemoteViews rv=new RemoteViews(ctx.getPackageName(),R.layout.widget_all_timers_test);
    rv.setTextViewText(R.id.all_timers_title,
     I18n.english(ctx)?"All timers":"Alle timers");
    java.util.ArrayList<AllTimersTestData.Account> shown=
     AllTimersWidgetSettings.visibleAccounts(ctx,id,snapshot);
    rv.setTextViewText(R.id.all_timers_summary,
     shown.size()+" accounts · "+(I18n.english(ctx)?"green = boosted":"groen = versneld"));
    Intent service=new Intent(ctx,AllTimersTestService.class);
    service.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
    service.setData(Uri.parse("clashtimerhub://all-timers/"+id));
    rv.setRemoteAdapter(R.id.all_timers_list,service);
    rv.setEmptyView(R.id.all_timers_list,R.id.all_timers_empty);
    // On modern Android, make the ListView only as tall as the data instead
    // of stretching it to fill the launcher allocation. On smaller widget
    // sizes it remains scrollable, with the Sync and Settings buttons visible.
    if(Build.VERSION.SDK_INT>=31){
     int mode=layoutMode(ctx,id);
     int rowDp=AllTimersWidgetSettings.twoLines(ctx,id)?49:
      mode==2?31:mode==1?49:71;
     int requestedHeight=widgetHeight(ctx,id);
     int available=Math.max(56,requestedHeight-99);
     int target=Math.max(56,Math.min(shown.size()*rowDp+2,available));
     rv.setViewLayoutHeight(R.id.all_timers_list,(float)target,
      android.util.TypedValue.COMPLEX_UNIT_DIP);
    }
    Intent template=new Intent(ctx,AllTimersTestWidget.class).setAction(OPEN);
    template.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
    int flags=PendingIntent.FLAG_UPDATE_CURRENT |
     (Build.VERSION.SDK_INT>=31?PendingIntent.FLAG_MUTABLE:0);
    rv.setPendingIntentTemplate(R.id.all_timers_list,
     PendingIntent.getBroadcast(ctx,949100+id,template,flags));
    Intent config=new Intent(ctx,AllTimersConfigActivity.class)
     .setAction("nl.clashtimerhub.v2.CONFIG_ALL_TIMERS_"+id)
     .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
    config.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    rv.setOnClickPendingIntent(R.id.all_timers_settings,
     PendingIntent.getActivity(ctx,949200+id,config,
      PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
    boolean floating=AppData.prefs(ctx).getBoolean("floatingEnabled",false);
    rv.setTextViewText(R.id.all_timers_sync,
     "Sync "+(floating?"ON":"OFF"));
    Intent toggle=new Intent(ctx,WidgetSyncReceiver.class).setAction(WidgetSyncReceiver.TOGGLE);
    rv.setOnClickPendingIntent(R.id.all_timers_sync,PendingIntent.getBroadcast(ctx,
     949300+id,toggle,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
    manager.updateAppWidget(id,rv);
   }catch(Exception error){android.util.Log.e("ClashTimerHub","All Timers widget error",error);}
  }
  manager.notifyAppWidgetViewDataChanged(ids,R.id.all_timers_list);
  plan(ctx,snapshot.next);
  planHeartbeat(ctx,snapshot.next);
 }

 /** Change only the Sync button. Do not rebind the collection, which shows
  * a launcher loading placeholder when the toggle is tapped repeatedly. */
 static void updateSyncStatus(Context ctx){
  AppWidgetManager manager=AppWidgetManager.getInstance(ctx);
  int[] ids=manager.getAppWidgetIds(new ComponentName(ctx,AllTimersTestWidget.class));
  boolean enabled=AppData.prefs(ctx).getBoolean("floatingEnabled",false);
  for(int id:ids){
   RemoteViews update=new RemoteViews(ctx.getPackageName(),R.layout.widget_all_timers_test);
   update.setTextViewText(R.id.all_timers_sync,"Sync "+(enabled?"ON":"OFF"));
   update.setTextColor(R.id.all_timers_sync,enabled?0xff8be3ad:0xffcbb5d9);
   manager.partiallyUpdateAppWidget(id,update);
  }
 }
 /** Read launcher sizing from the same options as the account RemoteViews service. */
 static int layoutMode(Context ctx,int id){
  int width=400;
  if(id!=AppWidgetManager.INVALID_APPWIDGET_ID){
   android.os.Bundle options=AppWidgetManager.getInstance(ctx).getAppWidgetOptions(id);
   if(options!=null){
    boolean landscape=ctx.getResources().getConfiguration().orientation==
     android.content.res.Configuration.ORIENTATION_LANDSCAPE;
    width=options.getInt(landscape?AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH:
     AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,400);
   }
  }
  return width>=670?2:width>=420?1:0;
 }
 private static int widgetHeight(Context ctx,int id){
  int height=320;
  android.os.Bundle options=AppWidgetManager.getInstance(ctx).getAppWidgetOptions(id);
  if(options!=null){
   boolean landscape=ctx.getResources().getConfiguration().orientation==
    android.content.res.Configuration.ORIENTATION_LANDSCAPE;
   height=options.getInt(landscape?AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT:
    AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,320);
  }
  return height>0?height:320;
 }
 private static PendingIntent refresh(Context ctx){
  return PendingIntent.getBroadcast(ctx,REQUEST_REFRESH,
   new Intent(ctx,AllTimersTestWidget.class).setAction(REFRESH),
   PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 /** Wake the collection on a conservative interval to restore launcher-
  * suspended Chronometers. Avoid a full widget rebind (and its loading UI).
  * Android may defer alarms in Doze; a per-second update is not guaranteed.
  */
 private static PendingIntent heartbeatIntent(Context ctx){
  return PendingIntent.getBroadcast(ctx,REQUEST_HEARTBEAT,
   new Intent(ctx,AllTimersTestWidget.class).setAction(COUNTDOWN_HEARTBEAT),
   PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 private static void planHeartbeat(Context ctx,long next){
  AlarmManager alarm=(AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
  if(alarm==null)return;
  PendingIntent operation=heartbeatIntent(ctx);
  alarm.cancel(operation);
  if(next==Long.MAX_VALUE||next<=System.currentTimeMillis())return;
  long trigger=android.os.SystemClock.elapsedRealtime()+HEARTBEAT_INTERVAL_MS;
  try{
   if(Build.VERSION.SDK_INT>=23)
    alarm.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,trigger,operation);
   else alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,trigger,operation);
  }catch(Exception e){android.util.Log.w("ClashTimerHub","Countdown heartbeat scheduling failed",e);}
 }
 private static void refreshCountdowns(Context ctx){
  AppWidgetManager manager=AppWidgetManager.getInstance(ctx);
  int[] ids=manager.getAppWidgetIds(new ComponentName(ctx,AllTimersTestWidget.class));
  if(ids.length==0){planHeartbeat(ctx,Long.MAX_VALUE);return;}
  // Chronometer bases are recomputed in AllTimersTestService.getViewAt().
  // Only invalidate the list, not the entire widget or its buttons.
  manager.notifyAppWidgetViewDataChanged(ids,R.id.all_timers_list);
  planHeartbeat(ctx,AllTimersTestData.load(ctx).next);
 }
 private static void plan(Context ctx,long next){
  AlarmManager alarm=(AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
  if(alarm==null)return;
  PendingIntent operation=refresh(ctx);
  alarm.cancel(operation);
  if(next==Long.MAX_VALUE||next<=System.currentTimeMillis())return;
  try{
   long at=next+1200L;
   if(Build.VERSION.SDK_INT>=23){
    if(Build.VERSION.SDK_INT<31||alarm.canScheduleExactAlarms())
     alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,operation);
    else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,operation);
   }else alarm.set(AlarmManager.RTC_WAKEUP,at,operation);
  }catch(Exception error){android.util.Log.w("ClashTimerHub","All Timers refresh failed",error);}
 }
 private static boolean registered(Context ctx,String account){
  try{
   JSONArray names=new JSONArray(AppData.prefs(ctx).getString("accounts","[]"));
   for(int i=0;i<names.length();i++)if(account.equals(names.optString(i)))return true;
  }catch(Exception ignored){}
  return false;
 }
 @Override public void onUpdate(Context ctx,AppWidgetManager manager,int[] ids){update(ctx);}
 @Override public void onAppWidgetOptionsChanged(Context ctx,AppWidgetManager manager,int id,android.os.Bundle options){
  super.onAppWidgetOptionsChanged(ctx,manager,id,options);update(ctx);
 }
 @Override public void onDeleted(Context ctx,int[] ids){
  for(int id:ids)AllTimersWidgetSettings.delete(ctx,id);
  super.onDeleted(ctx,ids);update(ctx);
 }
 @Override public void onReceive(Context ctx,Intent intent){
  super.onReceive(ctx,intent);
  if(intent==null)return;
  if(REFRESH.equals(intent.getAction())){update(ctx);return;}
  if(COUNTDOWN_HEARTBEAT.equals(intent.getAction())){refreshCountdowns(ctx);return;}
  if(OPEN.equals(intent.getAction())){
   String account=intent.getStringExtra("account");
   if(account==null||!registered(ctx,account))return;
   Intent open=new Intent(ctx,MainActivity.class);
   open.setAction("nl.clashtimerhub.v2.OPEN_ALL_TIMERS_ACCOUNT_"+account);
   open.putExtra("openAccount",account);
   open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
   ctx.startActivity(open);
  }
 }
}
