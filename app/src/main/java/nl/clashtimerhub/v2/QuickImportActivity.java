package nl.clashtimerhub.v2;
import android.app.*;
import android.content.*;
import android.os.Bundle;

public final class QuickImportActivity extends Activity {
 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
  CharSequence value=null;
  if(clipboard!=null&&clipboard.hasPrimaryClip()){
   android.content.ClipData clip=clipboard.getPrimaryClip();
   if(clip!=null&&clip.getItemCount()>0)value=clip.getItemAt(0).coerceToText(this);
  }
  final String json=value==null?"":value.toString();
  if(json.length()>10000000||!json.contains("buildings")){
   new AlertDialog.Builder(this).setMessage("Geen Clash JSON op het klembord").setPositiveButton("Sluiten",(d,w)->finish()).show();return;
  }
  new AlertDialog.Builder(this).setTitle("Clash JSON synchroniseren?")
   .setPositiveButton("Verder",(d,w)->{
    Intent i=new Intent(this,MainActivity.class);
    i.setAction(Intent.ACTION_SEND);i.setType("text/plain");
    i.putExtra(Intent.EXTRA_TEXT,json);
    i.putExtra("quickImport",true);
    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
    startActivity(i);finish();
   }).setNegativeButton("Annuleren",(d,w)->finish()).show();
 }
}
