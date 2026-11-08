package nl.clashtimerhub.v2;
import android.app.*;
import android.content.*;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;

/** Only notification receiver for normal and Potion-affected upgrades. */
public final class BuilderAlarm extends android.content.BroadcastReceiver {
 public static void sendTest(Context ctx){
  NotificationManager nm=(NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
  if(nm==null)return;
  if(Build.VERSION.SDK_INT>=33&&ctx.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
    !=android.content.pm.PackageManager.PERMISSION_GRANTED)return;
  if(Build.VERSION.SDK_INT>=26)
   nm.createNotificationChannel(new NotificationChannel(TimerWidget.CHANNEL,
    I18n.tr(ctx,"Upgrade meldingen"),NotificationManager.IMPORTANCE_HIGH));
  PendingIntent open=PendingIntent.getActivity(ctx,99,new Intent(ctx,MainActivity.class),
   PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder n=Build.VERSION.SDK_INT>=26?new Notification.Builder(ctx,TimerWidget.CHANNEL):new Notification.Builder(ctx);
  n.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(I18n.tr(ctx,"Clash Timer Hub · Testmelding"))
   .setContentText(I18n.tr(ctx,"Meldingen werken! Dit is een test vanuit de app."))
   .setAutoCancel(true).setContentIntent(open);
  nm.notify(880099,n.build());
 }
 @Override public void onReceive(Context ctx,Intent intent){
  if(intent==null)return;
  if("nl.clashtimerhub.v2.TEST_SCHEDULED_ALARM".equals(intent.getAction())){sendTest(ctx);return;}
  // Older unboosted alarms have no action; after upgrade only V2 is valid.
  if(!TimerWidget.UPGRADE_ACTION.equals(intent.getAction()))return;
  android.content.SharedPreferences prefs=AppData.prefs(ctx);
  boolean early=intent.getBooleanExtra("early",false);
  if(!prefs.getBoolean("notifyEnabled",true)||(!early&&!prefs.getBoolean("notifyOnFinish",true)))return;
  int lead=Math.max(0,Math.min(10080,prefs.getInt("notifyLeadMinutes",15)));
  if(early&&lead<=0)return;
  String ac=intent.getStringExtra("account"),cat=intent.getStringExtra("category"),
   id=intent.getStringExtra("id"),name=intent.getStringExtra("name");
  long source=intent.getLongExtra("sourceFinish",0L),finish=intent.getLongExtra("finish",0L);
  int pos=intent.getIntExtra("row",-1);
  if(ac==null||cat==null||id==null||name==null||source<=0||finish<=0)return;
  long now=System.currentTimeMillis();
  if(!early&&now+60000L<finish)return;
  if(early&&(now+60000L<finish-lead*60000L||now>=finish))return;
  if(finish<=prefs.getLong("ack_"+ac,0L))return;
  boolean stillCurrent=false;
  String resolvedName=ClashNames.resolvedName(name,-1L);
  try{
   JSONArray upgrades=new JSONArray(prefs.getString("upgrades","[]"));
   for(int i=0;i<upgrades.length();i++){
    if(pos>=0&&i!=pos)continue;
    JSONObject row=upgrades.optJSONObject(i);if(row==null)continue;
    if(!ac.equals(row.optString("account"))||!cat.equals(row.optString("category"))
     ||!name.equals(row.optString("name"))||source!=row.optLong("finish",0L)
     ||!id.equals(row.optString("id","row-"+i)))continue;
    stillCurrent=Math.abs(UpgradeForecast.finish(ctx,ac,cat,source)-finish)<=2000L;
    if(stillCurrent)resolvedName=ClashNames.displayName(row);
    break;
   }
  }catch(Exception e){android.util.Log.w("ClashTimerHub","Upgrade alert validation failed",e);}
  if(!stillCurrent)return;
  if(Build.VERSION.SDK_INT>=33&&ctx.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
    !=android.content.pm.PackageManager.PERMISSION_GRANTED)return;
  NotificationManager nm=(NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
  if(nm==null)return;
  if(Build.VERSION.SDK_INT>=26)
   nm.createNotificationChannel(new NotificationChannel(TimerWidget.CHANNEL,
    I18n.tr(ctx,"Upgrade meldingen"),NotificationManager.IMPORTANCE_HIGH));
  String title=AppData.displayName(ctx,ac)+(I18n.english(ctx)?
   (early?" · Upgrade nearly ready":" · Upgrade complete"):
   (early?" · Upgrade bijna klaar":" · Upgrade voltooid"));
  String text=early?resolvedName+" · "+Math.max(1,(finish-now+59999L)/60000L)+
   (I18n.english(ctx)?" min remaining":" min resterend"):
   resolvedName+(I18n.english(ctx)?" is ready":" is klaar");
  Intent open=new Intent(ctx,MainActivity.class).setAction("nl.clashtimerhub.v2.OPEN_UPGRADE_"+id);
  open.putExtra("openAccount",ac);
  PendingIntent click=PendingIntent.getActivity(ctx,("click:"+ac+":"+cat+":"+id).hashCode(),open,
   PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder note=Build.VERSION.SDK_INT>=26?
   new Notification.Builder(ctx,TimerWidget.CHANNEL):new Notification.Builder(ctx);
  note.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title)
   .setContentText(text).setAutoCancel(true).setContentIntent(click);
  nm.notify(("unified:"+ac+":"+cat+":"+id+":"+pos+":"+early).hashCode(),note.build());
  TimerWidget.update(ctx);
 }
}
