package nl.clashtimerhub.v2;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.View;
import android.widget.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import org.json.*;
import java.io.InputStream;
import java.util.*;
import java.util.regex.*;

public final class MainActivity extends Activity {
 private final String[] categories={"Bouwers","Laboratorium","Pets"};
 private LinearLayout root,items;
 private Spinner account,category;
 private final ArrayList<String> accounts=new ArrayList<>();
 private final ArrayList<JSONObject> upgrades=new ArrayList<>();
 private final ArrayList<JSONObject> pending=new ArrayList<>();
 private static final int PICK=12;
 private static final Pattern TIME=Pattern.compile("(?i)(?:(\\d+)\\s*d\\s*)?(?:(\\d+)\\s*h\\s*)?(?:(\\d+)\\s*m\\s*)?(?:(\\d+)\\s*s\\s*)?");
 @Override public void onCreate(Bundle b){super.onCreate(b);load();draw();}
 private void load(){
  try{JSONArray a=new JSONArray(getPreferences(0).getString("accounts","[\\"Ashot\\",\\"Ashotgun\\",\\"AshotRush\\",\\"Rushot\\",\\"Shotty\\",\\"Ashotty\\",\\"RushShot\\"]"));for(int i=0;i<a.length();i++)accounts.add(a.getString(i));}catch(Exception e){accounts.add("Ashot");}
  try{JSONArray a=new JSONArray(getPreferences(0).getString("upgrades","[]"));for(int i=0;i<a.length();i++)upgrades.add(a.getJSONObject(i));}catch(Exception ignored){}
 }
 private void persist(){getPreferences(0).edit().putString("accounts",new JSONArray(accounts).toString()).putString("upgrades",new JSONArray(upgrades).toString()).apply();}
 private TextView label(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextColor(Color.WHITE);v.setTextSize(size);v.setPadding(12,14,12,14);return v;}
 private void button(String title,Runnable run){Button b=new Button(this);b.setText(title);root.addView(b);b.setOnClickListener(v->run.run());}
 private void draw(){
  ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(1);root.setPadding(16,16,16,24);root.setBackgroundColor(0xff171c17);scroll.addView(root);setContentView(scroll);
  root.addView(label("Clash Timer Hub V2",25));
  account=new Spinner(this);account.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,accounts));root.addView(account);
  category=new Spinner(this);category.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,categories));root.addView(category);
  button("Screenshot kiezen en scannen",()->{Intent i=new Intent(Intent.ACTION_GET_CONTENT);i.setType("image/*");startActivityForResult(i,PICK);});
  button("Timer handmatig toevoegen",()->manual());
  button("Account toevoegen",()->editAccount());
  button("Overzicht vernieuwen",()->showTimers());
  items=new LinearLayout(this);items.setOrientation(1);root.addView(items);showTimers();
 }
 private void editAccount(){EditText e=new EditText(this);e.setSingleLine(true);new android.app.AlertDialog.Builder(this).setTitle("Nieuw account").setView(e).setPositiveButton("Toevoegen",(d,w)->{String s=e.getText().toString().trim();if(!s.isEmpty()){accounts.add(s);persist();draw();}}).setNegativeButton("Annuleren",null).show();}
 private void manual(){LinearLayout form=new LinearLayout(this);form.setOrientation(1);EditText n=new EditText(this),t=new EditText(this);n.setHint("Gebouwnaam");t.setHint("Tijd, bijvoorbeeld 3h 15m");form.addView(n);form.addView(t);new android.app.AlertDialog.Builder(this).setTitle("Timer toevoegen").setView(form).setPositiveButton("Opslaan",(d,w)->{long seconds=parse(t.getText().toString());if(seconds>0){try{JSONObject o=new JSONObject();o.put("id",UUID.randomUUID().toString());o.put("account",accounts.get(account.getSelectedItemPosition()));o.put("category",categories[category.getSelectedItemPosition()]);o.put("name",n.getText().toString());o.put("finish",System.currentTimeMillis()+seconds*1000);upgrades.add(o);persist();showTimers();}catch(Exception ignored){}}}).setNegativeButton("Annuleren",null).show();}
 private static long parse(String s){Matcher m=TIME.matcher(s.replace('O','0'));long best=0;while(m.find()){if(m.group().trim().isEmpty())continue;long v=0;for(int i=1;i<=4;i++)if(m.group(i)!=null){try{v+=Long.parseLong(m.group(i))*(i==1?86400:i==2?3600:i==3?60:1);}catch(Exception ignored){}}best=Math.max(best,v);}return best;}
 private static String format(long seconds){long d=seconds/86400,h=seconds%86400/3600,m=seconds%3600/60;return(d>0?d+"d ":"")+(h>0?h+"h ":"")+(m>0?m+"m ":"")+(seconds<3600?seconds%60+"s":"");}
 private void showTimers(){if(items==null)return;items.removeAllViews();long now=System.currentTimeMillis();for(JSONObject o:upgrades){String name=o.optString("name"),ac=o.optString("account"),cat=o.optString("category");long remain=Math.max(0,(o.optLong("finish")-now)/1000);TextView v=label(ac+" · "+cat+" · "+name+" — "+format(remain),16);items.addView(v);v.setOnLongClickListener(x->{new android.app.AlertDialog.Builder(this).setMessage("Timer verwijderen?").setPositiveButton("Verwijderen",(d,w)->{upgrades.remove(o);persist();showTimers();}).setNegativeButton("Annuleren",null).show();return true;});}}
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req!=PICK||result!=RESULT_OK||data==null)return;try(InputStream stream=getContentResolver().openInputStream(data.getData())){Bitmap bmp=BitmapFactory.decodeStream(stream);if(bmp!=null)scan(bmp);}catch(Exception e){toast("Foto kon niet worden geopend");}}
 private void scan(Bitmap bmp){
  // V2 uses independent row crops; never takes the shortest timer from the whole panel.
  ArrayList<Rect> rows=new ArrayList<>();int w=bmp.getWidth(),h=bmp.getHeight();
  int start=(int)(h*.065),end=(int)(h*.75),x=(int)(w*.61);
  boolean in=false;int first=0,last=0;
  for(int y=start;y<end;y+=2){int c=0;for(int xx=x;xx<x+w*.06;xx+=Math.max(2,w/500)){int pixel=bmp.getPixel(Math.min(w-1,xx),y);int red=(pixel>>16)&255,green=(pixel>>8)&255,blue=pixel&255;if(green>90&&green>red*1.15&&green>blue*1.17)c++;}
   boolean active=c>=2;if(active){if(!in){first=y;in=true;}last=y;}else if(in&&y-last>h/170){if(last-first>h*.014&&last-first<h*.07)rows.add(new Rect((int)(w*.59),Math.max(0,first-h/110),(int)(w*.99),Math.min(h,last+h/110)));in=false;}
  }
  if(rows.isEmpty()){toast("Geen groene upgraderijen gevonden. Gebruik een volledige screenshot.");return;}
  pending.clear();scanRow(bmp,rows,0);
 }
 private void scanRow(Bitmap bmp,ArrayList<Rect> rows,int index){
  if(index>=rows.size()){review();return;}
  Rect r=rows.get(index);Bitmap crop=Bitmap.createBitmap(bmp,r.left,r.top,r.width(),r.height());Bitmap enlarged=Bitmap.createScaledBitmap(crop,crop.getWidth()*2,crop.getHeight()*2,true);crop.recycle();
  TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(InputImage.fromBitmap(enlarged,0)).addOnSuccessListener(text->{
   StringBuilder name=new StringBuilder(),time=new StringBuilder();
   for(Text.TextBlock block:text.getTextBlocks())for(Text.Line line:block.getLines())for(Text.Element element:line.getElements()){Rect b=element.getBoundingBox();if(b==null)continue;if(b.centerX()>enlarged.getWidth()*.69)time.append(element.getText()).append(" ");else name.append(element.getText()).append(" ");}
   try{JSONObject o=new JSONObject();o.put("name",name.toString().trim());o.put("raw",time.toString().trim());o.put("seconds",parse(time.toString()));o.put("row",index);pending.add(o);}catch(Exception ignored){}
   scanRow(bmp,rows,index+1);
  }).addOnFailureListener(e->scanRow(bmp,rows,index+1)).addOnCompleteListener(t->enlarged.recycle());
 }
 private void review(){if(pending.isEmpty()){toast("Geen OCR-resultaten");return;}LinearLayout form=new LinearLayout(this);form.setOrientation(1);ScrollView scroll=new ScrollView(this);scroll.addView(form);ArrayList<EditText> names=new ArrayList<>(),times=new ArrayList<>();
  for(JSONObject o:pending){form.addView(label("Rij "+(o.optInt("row")+1),16));EditText n=new EditText(this),t=new EditText(this);n.setSingleLine(true);t.setSingleLine(true);n.setText(o.optString("name"));t.setText(o.optString("raw"));n.setHint("Naam");t.setHint("Bijv. 3h 15m");form.addView(n);form.addView(t);names.add(n);times.add(t);}
  new android.app.AlertDialog.Builder(this).setTitle("Controleer alle "+pending.size()+" rijen").setView(scroll).setPositiveButton("Gecorrigeerde timers opslaan",(d,w)->{
   String ac=accounts.get(account.getSelectedItemPosition()),cat=categories[category.getSelectedItemPosition()];long now=System.currentTimeMillis();int count=0;
   for(int i=0;i<names.size();i++){long sec=parse(times.get(i).getText().toString());String name=names.get(i).getText().toString().trim();if(sec<=0||name.isEmpty())continue;try{JSONObject o=new JSONObject();o.put("id",UUID.randomUUID().toString());o.put("account",ac);o.put("category",cat);o.put("name",name);o.put("finish",now+sec*1000);upgrades.add(o);count++;}catch(Exception ignored){}}
   persist();showTimers();toast(count+" timers opgeslagen");
  }).setNegativeButton("Annuleren",null).show();
 }
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
