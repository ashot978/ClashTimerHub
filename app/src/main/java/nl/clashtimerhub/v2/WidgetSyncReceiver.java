package nl.clashtimerhub.v2;
import android.content.*;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

/** User-triggered widget action toggling the existing overlay, not clipboard access. */
public final class WidgetSyncReceiver extends BroadcastReceiver {
 static final String TOGGLE="nl.clashtimerhub.v2.WIDGET_TOGGLE_FLOATING";
 @Override public void onReceive(Context context,Intent intent){
  if(intent==null||!TOGGLE.equals(intent.getAction()))return;
  android.content.SharedPreferences prefs=AppData.prefs(context);
  boolean enabled=prefs.getBoolean("floatingEnabled",false);
  Intent service=new Intent(context,FloatingSyncService.class);
  if(enabled){
   context.stopService(service);
   prefs.edit().putBoolean("floatingEnabled",false).apply();
  }else if(!Settings.canDrawOverlays(context)){
   Intent open=new Intent(context,MainActivity.class);
   open.putExtra("widgetEnableOverlay",true);
   open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
   context.startActivity(open);
  }else{
   try{
    if(Build.VERSION.SDK_INT>=26)context.startForegroundService(service);
    else context.startService(service);
    prefs.edit().putBoolean("floatingEnabled",true).apply();
   }catch(RuntimeException e){
    prefs.edit().putBoolean("floatingEnabled",false).apply();
    Toast.makeText(context,I18n.english(context)?"Could not start floating sync":"Kan zwevende synchronisatie niet starten",Toast.LENGTH_LONG).show();
   }
  }
  TimerWidget.update(context);
 }
}
