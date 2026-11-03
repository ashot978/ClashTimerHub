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

/** User-controlled overlay. Clipboard access happens only in a visible activity. */
public final class FloatingSyncService extends Service {
 private WindowManager manager;
 private View bubble;
 private WindowManager.LayoutParams params;
 private static final int CHANNEL_ID=11;
 @Override public void onCreate(){
  super.onCreate();
  if(!Settings.canDrawOverlays(this)){stopSelf();return;}
  if(Build.VERSION.SDK_INT>=26){
   NotificationChannel channel=new NotificationChannel("floating_sync","Zwevende synchronisatie",NotificationManager.IMPORTANCE_LOW);
   NotificationManager nm=getSystemService(NotificationManager.class);if(nm!=null)nm.createNotificationChannel(channel);
  }
  Intent open=new Intent(this,MainActivity.class);
  PendingIntent pi=PendingIntent.getActivity(this,12,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  Notification.Builder nb=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"floating_sync"):new Notification.Builder(this);
  nb.setSmallIcon(android.R.drawable.ic_menu_upload).setContentTitle("Clash Timer Hub").setContentText("Zwevende synchronisatie actief").setContentIntent(pi).setOngoing(true);
  if(Build.VERSION.SDK_INT>=29)startForeground(CHANNEL_ID,nb.build(),android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
  else startForeground(CHANNEL_ID,nb.build());
  manager=(WindowManager)getSystemService(WINDOW_SERVICE);
  TextView view=new TextView(this);view.setText("⚔");view.setTextSize(23);view.setTextColor(Color.WHITE);view.setGravity(Gravity.CENTER);
  GradientDrawable bg=new GradientDrawable();bg.setColor(0xff6840a0);bg.setCornerRadius(100);view.setBackground(bg);view.setAlpha(.88f);
  int size=(int)(52*getResources().getDisplayMetrics().density);
  params=new WindowManager.LayoutParams(size,size,Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
  params.gravity=Gravity.TOP|Gravity.START;params.x=getResources().getDisplayMetrics().widthPixels-size;params.y=getResources().getDisplayMetrics().heightPixels/3;
  view.setOnTouchListener(new View.OnTouchListener(){
   float startX,startY;int originalX,originalY;boolean moved;
   @Override public boolean onTouch(View v,android.view.MotionEvent e){
    switch(e.getAction()){
     case MotionEvent.ACTION_DOWN:startX=e.getRawX();startY=e.getRawY();originalX=params.x;originalY=params.y;moved=false;return true;
     case MotionEvent.ACTION_MOVE:
      int dx=(int)(e.getRawX()-startX),dy=(int)(e.getRawY()-startY);
      if(Math.abs(dx)>12||Math.abs(dy)>12)moved=true;
      params.x=Math.max(0,originalX+dx);params.y=Math.max(0,originalY+dy);manager.updateViewLayout(view,params);return true;
     case MotionEvent.ACTION_UP:
      if(!moved){
       Intent i=new Intent(FloatingSyncService.this,QuickImportActivity.class);
       i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);
      }return true;
    }
    return false;
   }
  });
  bubble=view;manager.addView(bubble,params);
 }
 @Override public int onStartCommand(Intent intent,int flags,int startId){return START_STICKY;}
 @Override public void onDestroy(){if(manager!=null&&bubble!=null)manager.removeView(bubble);super.onDestroy();}
 @Override public IBinder onBind(Intent intent){return null;}
}
