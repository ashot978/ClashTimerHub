package nl.clashtimerhub.v2;

import android.app.*;
import android.content.*;
import android.graphics.PixelFormat;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

/** User-controlled one-tap clipboard overlay. Never opens the main activity. */
public final class FloatingSyncService extends Service {
 private WindowManager manager;
 private View bubble;
 private WindowManager.LayoutParams params;
 private static final int CHANNEL_ID=11;
 private View dropTarget;
 private WindowManager.LayoutParams dropParams;
 private final Handler holdHandler=new Handler(Looper.getMainLooper());
 private Runnable holdAction;
 private View feedback;
 private Runnable feedbackDismiss;
 private boolean shuttingDown=false;
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 private void cancelHold(){
  if(holdAction!=null){holdHandler.removeCallbacks(holdAction);holdAction=null;}
 }
 @Override public void onCreate(){
  super.onCreate();
  if(!Settings.canDrawOverlays(this)){stopSelf();return;}
  if(Build.VERSION.SDK_INT>=26){
   NotificationChannel channel=new NotificationChannel("floating_sync",I18n.tr(this,"Zwevende synchronisatie"),NotificationManager.IMPORTANCE_LOW);
   NotificationManager nm=getSystemService(NotificationManager.class);if(nm!=null)nm.createNotificationChannel(channel);
  }
  Intent open=new Intent(this,MainActivity.class);
  PendingIntent pi=PendingIntent.getActivity(this,12,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  Notification.Builder nb=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"floating_sync"):new Notification.Builder(this);
  nb.setSmallIcon(android.R.drawable.ic_menu_upload).setContentTitle("Clash Timer Hub").setContentText(I18n.tr(this,"Tik: sync · Sleep naar X of houd ingedrukt: uitschakelen")).setContentIntent(pi).setOngoing(true);
  if(Build.VERSION.SDK_INT>=29)startForeground(CHANNEL_ID,nb.build(),android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
  else startForeground(CHANNEL_ID,nb.build());
  manager=(WindowManager)getSystemService(WINDOW_SERVICE);
  TextView view=new TextView(this);view.setText("⚔");view.setTextSize(23);view.setTextColor(Color.WHITE);view.setGravity(Gravity.CENTER);
  GradientDrawable bg=new GradientDrawable();bg.setColor(0xff6840a0);bg.setCornerRadius(100);view.setBackground(bg);view.setAlpha(.88f);
  int size=(int)(52*getResources().getDisplayMetrics().density);
  params=new WindowManager.LayoutParams(size,size,Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE,WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
  params.gravity=Gravity.TOP|Gravity.START;params.x=getResources().getDisplayMetrics().widthPixels-size;params.y=getResources().getDisplayMetrics().heightPixels/3;
  android.content.SharedPreferences preferences=AppData.prefs(this);
  params.x=Math.max(0,preferences.getInt("floatingX",params.x));
  params.y=Math.max(0,preferences.getInt("floatingY",params.y));
  view.setOnTouchListener(new View.OnTouchListener(){
   float startX,startY;int originalX,originalY;boolean moved;
   @Override public boolean onTouch(View v,MotionEvent e){
    switch(e.getActionMasked()){
     case MotionEvent.ACTION_DOWN:
      startX=e.getRawX();startY=e.getRawY();originalX=params.x;originalY=params.y;moved=false;
      cancelHold();
      holdAction=()->{
       if(!moved&&!shuttingDown){
        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        disableBubble();
       }
      };
      holdHandler.postDelayed(holdAction,1350);
      return true;
     case MotionEvent.ACTION_MOVE:
      int dx=(int)(e.getRawX()-startX),dy=(int)(e.getRawY()-startY);
      if(Math.abs(dx)>dp(10)||Math.abs(dy)>dp(10)){
       if(!moved){moved=true;cancelHold();showDropTarget();}
      }
      if(moved&&!shuttingDown){
       android.util.DisplayMetrics metrics=getResources().getDisplayMetrics();
       params.x=Math.max(0,Math.min(metrics.widthPixels-params.width,originalX+dx));
       params.y=Math.max(0,Math.min(metrics.heightPixels-params.height,originalY+dy));
       if(manager!=null)manager.updateViewLayout(v,params);
       highlightDropTarget();
      }
      return true;
     case MotionEvent.ACTION_UP:
      cancelHold();
      if(shuttingDown)return true;
      if(moved){
       boolean discard=isNearDropTarget();
       hideDropTarget();
       if(discard)disableBubble();
       else AppData.prefs(FloatingSyncService.this).edit().putInt("floatingX",params.x).putInt("floatingY",params.y).apply();
      }else pasteNow();
      return true;
     case MotionEvent.ACTION_CANCEL:
      cancelHold();hideDropTarget();return true;
    }
    return false;
   }
  });
  bubble=view;manager.addView(bubble,params);
  AppData.prefs(this).edit().putBoolean("floatingEnabled",true).apply();
  TimerWidget.update(this);
 }
 private void showDropTarget(){
  if(dropTarget!=null||manager==null)return;
  TextView target=new TextView(this);
  target.setText(I18n.tr(this,"✕  Sleep hierheen om te sluiten"));target.setTextColor(Color.WHITE);
  target.setTextSize(14);target.setGravity(Gravity.CENTER);target.setTypeface(null,android.graphics.Typeface.BOLD);
  GradientDrawable bg=new GradientDrawable();bg.setColor(0xff873449);bg.setCornerRadius(dp(28));target.setBackground(bg);
  android.util.DisplayMetrics m=getResources().getDisplayMetrics();
  int w=Math.min(m.widthPixels-dp(24),dp(240)),h=dp(55);
  dropParams=new WindowManager.LayoutParams(w,h,params.type,
   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
   PixelFormat.TRANSLUCENT);
  dropParams.gravity=Gravity.TOP|Gravity.START;
  dropParams.x=Math.max(0,(m.widthPixels-w)/2);dropParams.y=Math.max(0,m.heightPixels-dp(140));
  try{manager.addView(target,dropParams);dropTarget=target;}
  catch(RuntimeException e){android.util.Log.w("ClashTimerHub","Cannot show discard target",e);}
 }
 private boolean isNearDropTarget(){
  if(dropTarget==null||dropParams==null||params==null)return false;
  int dx=params.x+params.width/2-(dropParams.x+dropParams.width/2);
  int dy=params.y+params.height/2-(dropParams.y+dropParams.height/2);
  return (long)dx*dx+(long)dy*dy<(long)dp(145)*dp(145);
 }
 private void highlightDropTarget(){
  if(dropTarget instanceof TextView){
   boolean near=isNearDropTarget();
   TextView text=(TextView)dropTarget;
   text.setText(I18n.tr(this,near?"✕  Laat los om te sluiten":"✕  Sleep hierheen om te sluiten"));
   ((GradientDrawable)text.getBackground()).setColor(near?0xffcc304f:0xff873449);
  }
 }
 private void hideDropTarget(){
  if(manager!=null&&dropTarget!=null){
   try{manager.removeView(dropTarget);}catch(RuntimeException ignored){}
  }
  dropTarget=null;dropParams=null;
 }
 private void disableBubble(){
  if(shuttingDown)return;
  shuttingDown=true;cancelHold();hideDropTarget();
  AppData.prefs(this).edit().putBoolean("floatingEnabled",false).apply();
  TimerWidget.update(this);
  Toast.makeText(this,I18n.tr(this,"Zwevende plakknop uitgeschakeld"),Toast.LENGTH_SHORT).show();
  stopSelf();
 }
 private void pasteNow(){
  try{
   android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
   if(clipboard==null||!clipboard.hasPrimaryClip())throw new IllegalArgumentException("Klembord is leeg");
   android.content.ClipData clip=clipboard.getPrimaryClip();
   if(clip==null||clip.getItemCount()==0)throw new IllegalArgumentException("Klembord is leeg");
   CharSequence value=clip.getItemAt(0).coerceToText(this);
   String result=QuickSyncImporter.importJson(this,value==null?"":value.toString());
   flashBubble(true);
   showFeedback(I18n.tr(this,result),true);
  }catch(Exception e){
   flashBubble(false);
   showFeedback(I18n.tr(this,"Synchronisatie mislukt\n"+e.getMessage()),false);
  }
 }
 private void showFeedback(String message,boolean success){
  clearFeedback();
  // Accept both real and literal newlines from older exports; never print \\n on screen.
  String normalized=(message==null?"":message).replace("\\n","\n").trim();
  String[] parts=normalized.split("\n",3);
  String first=parts.length>0?parts[0]:"",last=parts.length>1?parts[1]:"";
  int marker=first.indexOf(" ✓ ");
  String title=success&&marker>0?"✓ "+first.substring(0,marker).trim():
   success?"✓ Synchronisatie gelukt":"⚠ Synchronisatie mislukt";
  String details=success&&marker>0?first.substring(marker+3).trim():first;
  if(details.endsWith(")")){
   int open=details.indexOf(" (");
   if(open>0)details=details.substring(0,open)+"  ·  "+details.substring(open+2,details.length()-1);
  }
  if(last.isEmpty()&&parts.length>2)last=parts[2];
  if(manager==null||params==null){
   Toast.makeText(this,normalized,Toast.LENGTH_LONG).show();return;
  }
  LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
  card.setPadding(dp(12),dp(8),dp(12),dp(8));
  TextView heading=new TextView(this);heading.setText(title);heading.setTextColor(Color.WHITE);
  heading.setTextSize(13);heading.setTypeface(null,android.graphics.Typeface.BOLD);
  heading.setSingleLine(true);heading.setEllipsize(android.text.TextUtils.TruncateAt.END);
  card.addView(heading);
  if(!details.isEmpty()){
   TextView meta=new TextView(this);meta.setText(details);meta.setTextColor(0xffe7ddeb);
   meta.setTextSize(11);meta.setSingleLine(true);meta.setEllipsize(android.text.TextUtils.TruncateAt.END);
   card.addView(meta);
  }
  if(!last.isEmpty()){
   TextView subtitle=new TextView(this);subtitle.setText(last);subtitle.setTextSize(10);
   subtitle.setTextColor(0xffc5e3d6);subtitle.setMaxLines(2);
   subtitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
   card.addView(subtitle);
  }
  GradientDrawable bg=new GradientDrawable();
  bg.setColor(success?0xe0204135:0xe0442435);
  bg.setStroke(dp(1),success?0xff65ba94:0xffd66b84);
  bg.setCornerRadius(dp(12));card.setBackground(bg);
  android.util.DisplayMetrics m=getResources().getDisplayMetrics();
  int width=Math.min(dp(295),m.widthPixels-dp(24));
  WindowManager.LayoutParams popup=new WindowManager.LayoutParams(width,WindowManager.LayoutParams.WRAP_CONTENT,
   params.type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|
   WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|
   WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
  popup.gravity=Gravity.TOP|Gravity.START;
  popup.x=Math.max(dp(8),Math.min(m.widthPixels-width-dp(8),params.x+params.width/2-width/2));
  popup.y=params.y+params.height+dp(8);
  if(popup.y>m.heightPixels-dp(125))popup.y=Math.max(dp(8),params.y-dp(105));
  try{
   manager.addView(card,popup);feedback=card;
   feedbackDismiss=this::clearFeedback;
   holdHandler.postDelayed(feedbackDismiss,3200);
  }catch(RuntimeException e){
   Toast.makeText(this,normalized,Toast.LENGTH_LONG).show();
  }
 }
 private void clearFeedback(){
  if(feedbackDismiss!=null)holdHandler.removeCallbacks(feedbackDismiss);
  feedbackDismiss=null;
  if(feedback!=null&&manager!=null){
   try{manager.removeView(feedback);}catch(RuntimeException ignored){}
  }
  feedback=null;
 }
 private void flashBubble(boolean success){
  if(!(bubble instanceof TextView))return;
  TextView text=(TextView)bubble;
  text.setText(success?"✓":"!");
  GradientDrawable shape=(GradientDrawable)text.getBackground();
  shape.setColor(success?0xff267c57:0xffa33b53);
  text.postDelayed(()->{
   if(bubble==text){
    text.setText("⚔");
    shape.setColor(0xff6840a0);
   }
  },1200);
 }
 @Override public int onStartCommand(Intent intent,int flags,int startId){return START_STICKY;}
 @Override public void onDestroy(){
  cancelHold();hideDropTarget();clearFeedback();
  if(manager!=null&&bubble!=null){
   try{manager.removeView(bubble);}catch(RuntimeException ignored){}
  }
  AppData.prefs(this).edit().putBoolean("floatingEnabled",false).apply();
  TimerWidget.update(this);
  super.onDestroy();
 }
 @Override public IBinder onBind(Intent intent){return null;}
}
