package nl.clashtimerhub.v2;
import android.app.*;
import android.content.*;
import android.os.Build;

public final class BuilderAlarm extends android.content.BroadcastReceiver {
 @Override public void onReceive(Context context,Intent intent){
  long finish=intent.getLongExtra("finish",0);
  if(finish>System.currentTimeMillis()+60000)return;
  String ac=intent.getStringExtra("account"),name=intent.getStringExtra("name");
  if(ac==null||name==null)return;
  NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
  if(manager==null)return;
  if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(new NotificationChannel(TimerWidget.CHANNEL,"Builder gereed",NotificationManager.IMPORTANCE_DEFAULT));
  if(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)return;
  Intent open=new Intent(context,MainActivity.class);
  PendingIntent pi=PendingIntent.getActivity(context,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,TimerWidget.CHANNEL):new Notification.Builder(context);
  builder.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(ac+" · Bouwer vrij")
   .setContentText(name+" is klaar").setAutoCancel(true).setContentIntent(pi);
  manager.notify((ac+"-"+name+"-"+finish).hashCode(),builder.build());
  TimerWidget.update(context);
 }
}
