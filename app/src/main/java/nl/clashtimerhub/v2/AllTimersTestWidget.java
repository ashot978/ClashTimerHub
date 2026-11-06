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
 private static final String OPEN="nl.clashtimerhub.v2.ALL_TIMERS_TEST_OPEN";
 private static final int REQUEST_REFRESH=949001;

 static void update(Context ctx){
  AppWidgetManager manager=AppWidgetManager.getInstance(ctx);
  int[] ids=manager.getAppWidgetIds(new ComponentName(ctx,AllTimersTestWidget.class));
  if(ids.length==0){plan(ctx,Long.MAX_VALUE);return;}
  AllTimersTestData.Snapshot snapshot=AllTimersTestData.load(ctx);
  for(int id:ids){
   try{
    RemoteViews rv=new RemoteViews(ctx.getPackageName(),R.layout.widget_all_timers_test);
    rv.setTextViewText(R.id.all_timers_title,
     I18n.english(ctx)?"All timers":"Alle timers");
    java.util.ArrayList<AllTimersTestData.Account> shown=
     AllTimersWidgetSettings.visibleAccounts(ctx,id,snapshot);
    rv.setTextViewText(R.id.all_timers_summary,
     shown.size()+" "+(I18n.english(ctx)?"accounts":"accounts")+
     " · "+AllTimersWidgetSettings.boostedCount(snapshot,shown)+" "+
     (I18n.english(ctx)?"boosted":"versneld")+
     " · "+(I18n.english(ctx)?"green = potion":"groen = potion"));
    Intent service=new Intent(ctx,AllTimersTestService.class);
    service.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
    service.setData(Uri.parse("clashtimerhub://all-timers/"+id));
    rv.setRemoteAdapter(R.id.all_timers_list,service);
    rv.setEmptyView(R.id.all_timers_list,R.id.all_timers_empty);
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
    rv.setOnClickPendingIntent(R.id.all_timers_open,
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
 }
 private static PendingIntent refresh(Context ctx){
  return PendingIntent.getBroadcast(ctx,REQUEST_REFRESH,
   new Intent(ctx,AllTimersTestWidget.class).setAction(REFRESH),
   PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
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
