package nl.clashtimerhub.v2;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * One-tap clipboard import from a user-triggered overlay action.
 *
 * Android 10+ only permits clipboard reads from a focused application (or
 * the default keyboard). Reading in onCreate(), or in the non-focusable
 * overlay service, incorrectly returns null on modern Android / One UI.
 *
 * This focusable Activity has a tiny transparent window. We read only after
 * the system gives it focus, then immediately return to Clash. No keyboard
 * focus is held by the persistent overlay.
 */
public final class QuickImportActivity extends Activity {
 private final Handler handler=new Handler(Looper.getMainLooper());
 private boolean completed=false;
 private boolean importPending=false;
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}

 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  getWindow().setBackgroundDrawableResource(android.R.color.transparent);
  getWindow().setDimAmount(0f);
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
  getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
  getWindow().setGravity(Gravity.TOP|Gravity.END);
  setContentView(new android.view.View(this));
  getWindow().setLayout(dp(28),dp(28));
  // Do not silently leave an invisible focused window over Clash on failure.
  handler.postDelayed(()->{
   if(!completed)complete(false,I18n.english(this)?
    "Could not get clipboard focus. Tap Sync again.":"Geen klembordfocus gekregen. Tik opnieuw op Sync.");
  },2200L);
 }

 @Override public void onWindowFocusChanged(boolean hasFocus){
  super.onWindowFocusChanged(hasFocus);
  if(hasFocus&&!completed&&!importPending){
   importPending=true;
   // Wait for Android's focus transition before reading protected clipboard.
   handler.postDelayed(()->readClipboard(0),70L);
  }
 }

 private void readClipboard(int attempt){
  if(completed)return;
  if(!hasWindowFocus()){
   importPending=false;
   return;
  }
  try{
   ClipboardManager manager=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
   ClipData clip=manager==null?null:manager.getPrimaryClip();
   if(clip==null||clip.getItemCount()==0){
    if(attempt<3){
     handler.postDelayed(()->readClipboard(attempt+1),150L);
     return;
    }
    complete(false,I18n.english(this)?
     "Clipboard unavailable. Copy Clash JSON via Data Export → Copy and retry.":
     "Klembord niet beschikbaar. Kopieer Clash JSON via Data Export → Copy en probeer opnieuw.");
    return;
   }
   CharSequence raw=clip.getItemAt(0).coerceToText(this);
   if(raw==null||raw.length()==0){
    complete(false,I18n.english(this)?
     "Clipboard is empty. Copy Clash JSON first.":
     "Klembord is leeg. Kopieer eerst de Clash-JSON.");
    return;
   }
   String result=QuickSyncImporter.importJson(this,raw.toString());
   complete(true,result);
  }catch(Exception error){
   android.util.Log.e("ClashTimerHub","Foreground JSON import failed",error);
   String reason=error.getMessage();
   complete(false,reason==null?"Import failed":reason);
  }
 }

 private void complete(boolean success,String result){
  if(completed)return;
  completed=true;handler.removeCallbacksAndMessages(null);
  try{
   if(AppData.prefs(this).getBoolean("floatingEnabled",false)){
    Intent reply=new Intent(this,FloatingSyncService.class)
     .setAction(FloatingSyncService.ACTION_IMPORT_RESULT)
     .putExtra(FloatingSyncService.EXTRA_IMPORT_SUCCESS,success)
     .putExtra(FloatingSyncService.EXTRA_IMPORT_MESSAGE,result);
    startService(reply);
   }else Toast.makeText(this,result,Toast.LENGTH_LONG).show();
  }catch(Exception error){
   android.util.Log.w("ClashTimerHub","Could not show overlay import result",error);
   Toast.makeText(this,result,Toast.LENGTH_LONG).show();
  }
  finish();
  overridePendingTransition(0,0);
 }
 @Override protected void onDestroy(){
  handler.removeCallbacksAndMessages(null);
  super.onDestroy();
 }
}
