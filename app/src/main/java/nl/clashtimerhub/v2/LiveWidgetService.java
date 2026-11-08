package nl.clashtimerhub.v2;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;

/**
 * Notification-free live widget ticker. Keep the existing class name so that
 * widget call sites and per-widget settings stay unchanged.
 *
 * The launcher owns the actual Chronometer countdowns; this best-effort
 * in-process ticker rebinds rows every second while the screen is interactive.
 * Android may pause a background process, so the existing non-wakeup widget
 * heartbeat and native Chronometers are retained as fallbacks.
 */
public final class LiveWidgetService {
 private static final long INTERVAL_MS=1_000L;
 private static final Handler handler=new Handler(Looper.getMainLooper());
 private static Context application;
 private static boolean registered;
 private static boolean interactive=true;
 private LiveWidgetService(){}

 private static final Runnable tick=new Runnable(){
  @Override public void run(){
   Context ctx=application;
   if(ctx==null||!hasLiveWidgets(ctx)){stop();return;}
   if(!interactive)return;
   AllTimersTestWidget.refreshCountdownRows(ctx);
   long uptime=SystemClock.uptimeMillis();
   handler.postAtTime(this,(uptime/INTERVAL_MS+1L)*INTERVAL_MS);
  }
 };
 private static final BroadcastReceiver screenReceiver=new BroadcastReceiver(){
  @Override public void onReceive(Context context,Intent intent){
   String action=intent==null?"":intent.getAction();
   if(Intent.ACTION_SCREEN_OFF.equals(action)){
    interactive=false;
    handler.removeCallbacks(tick);
   }else if(Intent.ACTION_SCREEN_ON.equals(action)||
            Intent.ACTION_USER_PRESENT.equals(action)){
    interactive=true;
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
 /**
  * Start or resume a best-effort second-by-second in-process refresh.
  * Unlike a foreground service this does not post an ongoing notification.
  */
 static synchronized void reconcile(Context ctx){
  Context app=ctx.getApplicationContext();
  if(!hasLiveWidgets(app)){stop();return;}
  application=app;
  if(!registered){
   IntentFilter filter=new IntentFilter();
   filter.addAction(Intent.ACTION_SCREEN_ON);
   filter.addAction(Intent.ACTION_SCREEN_OFF);
   filter.addAction(Intent.ACTION_USER_PRESENT);
   try{
    if(Build.VERSION.SDK_INT>=33)
     app.registerReceiver(screenReceiver,filter,Context.RECEIVER_NOT_EXPORTED);
    else app.registerReceiver(screenReceiver,filter);
    registered=true;
   }catch(RuntimeException e){
    android.util.Log.w("ClashTimerHub","Live widget screen receiver unavailable",e);
   }
  }
  PowerManager power=(PowerManager)app.getSystemService(Context.POWER_SERVICE);
  interactive=power==null||power.isInteractive();
  if(interactive)scheduleTick();
  else handler.removeCallbacks(tick);
 }
 private static void scheduleTick(){
  handler.removeCallbacks(tick);
  if(application!=null&&interactive)handler.post(tick);
 }
 private static synchronized void stop(){
  handler.removeCallbacks(tick);
  if(registered&&application!=null){
   try{application.unregisterReceiver(screenReceiver);}
   catch(IllegalArgumentException ignored){}
  }
  registered=false;
  application=null;
 }
}
