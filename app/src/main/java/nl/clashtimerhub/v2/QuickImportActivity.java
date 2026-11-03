package nl.clashtimerhub.v2;
import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.widget.Toast;

/** Visible, short-lived activity: Android allows clipboard reads while foreground. */
public final class QuickImportActivity extends Activity {
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  String result;
  try{
   android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
   if(clipboard==null||!clipboard.hasPrimaryClip())throw new IllegalArgumentException("Klembord is leeg");
   android.content.ClipData clip=clipboard.getPrimaryClip();
   if(clip==null||clip.getItemCount()==0)throw new IllegalArgumentException("Klembord is leeg");
   CharSequence value=clip.getItemAt(0).coerceToText(this);
   result=QuickSyncImporter.importJson(this,value==null?"":value.toString());
  }catch(Exception error){
   result="Niet geïmporteerd: "+error.getMessage();
  }
  Toast.makeText(this,result,Toast.LENGTH_LONG).show();
  finish();
 }
}
