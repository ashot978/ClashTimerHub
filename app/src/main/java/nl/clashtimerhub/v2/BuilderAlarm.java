package nl.clashtimerhub.v2;
import android.app.*;
import android.content.*;
import android.os.Build;

public final class BuilderAlarm extends android.content.BroadcastReceiver {
 public static void sendTest(Context context){
  NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
  if(manager==null)return;
  if(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)return;
  if(Build.VERSION.SDK_INT>=26){
   NotificationChannel channel=new NotificationChannel(TimerWidget.CHANNEL,I18n.tr(context,"Upgrade meldingen"),NotificationManager.IMPORTANCE_HIGH);
   manager.createNotificationChannel(channel);
  }
  Intent open=new Intent(context,MainActivity.class);
  PendingIntent pi=PendingIntent.getActivity(context,99,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,TimerWidget.CHANNEL):new Notification.Builder(context);
  builder.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(I18n.tr(context,"Clash Timer Hub · Testmelding"))
   .setContentText(I18n.tr(context,"Meldingen werken! Dit is een test vanuit de app."))
   .setAutoCancel(true).setContentIntent(pi);
  manager.notify(880099,builder.build());
 }
 @Override public void onReceive(Context context,Intent intent){
  if("nl.clashtimerhub.v2.TEST_SCHEDULED_ALARM".equals(intent.getAction())){sendTest(context);return;}

  long finish=intent.getLongExtra("finish",0);
  boolean early=intent.getBooleanExtra("early",false);
  if(!early&&finish>System.currentTimeMillis()+60000)return;
  String ac=intent.getStringExtra("account"),name=intent.getStringExtra("name");
  if(ac==null||name==null)return;
  android.content.SharedPreferences prefs=AppData.prefs(context);
  if(!prefs.getBoolean("notifyEnabled",true))return;
  if(!early&&!prefs.getBoolean("notifyOnFinish",true))return;
  if(early){
   int lead=prefs.getInt("notifyLeadMinutes",15);
   if(lead<=0||finish<=System.currentTimeMillis()||finish-System.currentTimeMillis()>lead*60000L+60000L)return;
  }
  if(finish<=prefs.getLong("ack_"+ac,0L))return;
  boolean stillTracked=false;
  try{
   org.json.JSONArray upgrades=new org.json.JSONArray(prefs.getString("upgrades","[]"));
   for(int i=0;i<upgrades.length();i++){
    org.json.JSONObject o=upgrades.getJSONObject(i);
    if(ac.equals(o.optString("account"))&&finish==o.optLong("finish")&&name.equals(o.optString("name"))){stillTracked=true;break;}
   }
  }catch(Exception ignored){}
  if(!stillTracked)return;
  NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
  if(manager==null)return;
  if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(new NotificationChannel(TimerWidget.CHANNEL,I18n.tr(context,"Builder gereed"),NotificationManager.IMPORTANCE_DEFAULT));
  if(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)return;
  Intent open=new Intent(context,MainActivity.class);
  PendingIntent pi=PendingIntent.getActivity(context,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,TimerWidget.CHANNEL):new Notification.Builder(context);
  builder.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(ac+I18n.tr(context,early?" · Upgrade bijna klaar":" · Upgrade voltooid"))
   .setContentText(I18n.english(context)?(early?name+" · "+Math.max(1,(finish-System.currentTimeMillis()+59999)/60000)+" min remaining":name+" is ready"):(early?name+" · nog "+Math.max(1,(finish-System.currentTimeMillis()+59999)/60000)+" minuten":name+" is klaar")).setAutoCancel(true).setContentIntent(pi);
  manager.notify((ac+"-"+name+"-"+finish+"-"+early).hashCode(),builder.build());
  TimerWidget.update(context);
 }
}
