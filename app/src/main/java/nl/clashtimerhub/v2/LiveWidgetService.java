package nl.clashtimerhub.v2;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.os.*;
import android.widget.RemoteViews;

/**
 * User-enabled foreground companion for All Timers launcher widgets.
 * It cannot force a third-party launcher to render every second, but it
 * actively rebinds countdown rows while the screen is interactive and
 * refreshes immediately when the screen wakes.
 */
public final class LiveWidgetService extends Service {
 static final String ACTION_RECONCILE="nl.clashtimerhub.v2.LIVE_WIDGET_RECONCILE";
 private static final String CHANNEL="widget_live_countdowns";
 private static final int NOTIFICATION_ID=22;
 private static final long INTERVAL_MS=15_000L;
 private final Handler handler=new Handler(Looper.getMainLooper());
 private boolean registered;
 private boolean interactive=true;
 private final Runnable tick=new Runnable(){
  @Override public void run(){
   if(!hasLiveWidgets(LiveWidgetService.this)){stopSelf();return;}
   if(interactive)AllTimersTestWidget.refreshCountdownRows(LiveWidgetService.this);
   handler.postDelayed(this,INTERVAL_MS);
  }
 };
 private final BroadcastReceiver screenReceiver=new BroadcastReceiver(){
  @Override public void onReceive(Context context,Intent intent){
   String action=intent==null?"":intent.getAction();
   if(Intent.ACTION_SCREEN_OFF.equals(action)){interactive=false;return;}
   if(Intent.ACTION_SCREEN_ON.equals(action)||Intent.ACTION_USER_PRESENT.equals(action)){
    interactive=true;
    AllTimersTestWidget.refreshCountdownRows(context);
    scheduleTick();
   }
  }
 };
 static boolean hasLiveWidgets(Context ctx){
  AppWidgetManager manager=AppWidgetManager.getInstance(ctx);
  int[] ids=manager.getAppWidgetIds(new ComponentName(ctx,AllTimersTestWidget.class));
  for(int id:ids)if(AllTimersWidgetSettings.liveCountdowns(ctx,id))return true;
  return false;
 }
 /** A foreground activity, widget configuration or user interaction should
  * call reconcile. Background starts are best effort on Android 12+.
  */
 static void reconcile(Context ctx){
  Intent service=new Intent(ctx,LiveWidgetService.class).setAction(ACTION_RECONCILE);
  if(!hasLiveWidgets(ctx)){ctx.stopService(service);return;}
  try{
   if(Build.VERSION.SDK_INT>=26)ctx.startForegroundService(service);
   else ctx.startService(service);
  }catch(RuntimeException blocked){
   android.util.Log.w("ClashTimerHub","Android deferred live widget service start",blocked);
  }
 }
 @Override public void onCreate(){
  super.onCreate();
  if(Build.VERSION.SDK_INT>=26){
   NotificationChannel channel=new NotificationChannel(CHANNEL,
    I18n.english(this)?"Live widget countdowns":"Live widget-aftellers",
    NotificationManager.IMPORTANCE_LOW);
   channel.setDescription("Keeps launcher widget countdowns refreshed");
   NotificationManager manager=getSystemService(NotificationManager.class);
   if(manager!=null)manager.createNotificationChannel(channel);
  }
  Intent open=new Intent(this,MainActivity.class);
  PendingIntent launch=PendingIntent.getActivity(this,NOTIFICATION_ID,open,
   PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder notification=Build.VERSION.SDK_INT>=26?
   new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
  notification.setSmallIcon(android.R.drawable.ic_popup_sync)
   .setContentTitle("Clash Timer Hub")
   .setContentText(I18n.english(this)?"Live widget timers enabled":"Live widget-timers actief")
   .setOngoing(true).setContentIntent(launch);
  if(Build.VERSION.SDK_INT>=29)
   startForeground(NOTIFICATION_ID,notification.build(),
    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
  else startForeground(NOTIFICATION_ID,notification.build());
  PowerManager power=(PowerManager)getSystemService(POWER_SERVICE);
  interactive=power==null||power.isInteractive();
  IntentFilter filter=new IntentFilter();
  filter.addAction(Intent.ACTION_SCREEN_ON);
  filter.addAction(Intent.ACTION_SCREEN_OFF);
  filter.addAction(Intent.ACTION_USER_PRESENT);
  if(Build.VERSION.SDK_INT>=33)registerReceiver(screenReceiver,filter,Context.RECEIVER_NOT_EXPORTED);
  else registerReceiver(screenReceiver,filter);
  registered=true;
  scheduleTick();
 }
 private void scheduleTick(){
  handler.removeCallbacks(tick);
  handler.post(tick);
 }
 @Override public int onStartCommand(Intent intent,int flags,int startId){
  if(!hasLiveWidgets(this)){stopSelf();return START_NOT_STICKY;}
  scheduleTick();
  return START_STICKY;
 }
 @Override public void onDestroy(){
  handler.removeCallbacks(tick);
  if(registered){unregisterReceiver(screenReceiver);registered=false;}
  super.onDestroy();
 }
 @Override public IBinder onBind(Intent intent){return null;}
}
