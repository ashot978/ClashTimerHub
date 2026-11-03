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
  try{JSONArray a=new JSONArray(getPreferences(0).getString("accounts",new JSONArray(Arrays.asList("Ashot","Ashotgun","AshotRush","Rushot","Shotty","Ashotty","RushShot")).toString()));for(int i=0;i<a.length();i++)accounts.add(a.getString(i));}catch(Exception e){accounts.add("Ashot");}
  try{JSONArray a=new JSONArray(getPreferences(0).getString("upgrades","[]"));for(int i=0;i<a.length();i++)upgrades.add(a.getJSONObject(i));}catch(Exception ignored){}
 }
 private void persist(){getPreferences(0).edit().putString("accounts",new JSONArray(accounts).toString()).putString("upgrades",new JSONArray(upgrades).toString()).apply();}
 private TextView label(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextColor(Color.WHITE);v.setTextSize(size);v.setPadding(14,14,14,14);return v;}
 private void button(String title,Runnable run){Button b=new Button(this);b.setText(title);root.addView(b);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff6834b9));b.setTextColor(Color.WHITE);b.setOnClickListener(v->run.run());}
 private void draw(){
  ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(1);root.setPadding(16,16,16,24);root.setBackgroundColor(0xff101024);scroll.addView(root);setContentView(scroll);
  root.addView(label("⚔  CLASH TIMER HUB  •  V3",26));
  root.addView(label("Jouw upgrades. Altijd op tijd.",14));
  account=new Spinner(this);account.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,accounts));root.addView(account);
  category=new Spinner(this);category.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,categories));root.addView(category);
  button("Screenshot kiezen en scannen",()->{Intent i=new Intent(Intent.ACTION_GET_CONTENT);i.setType("image/*");startActivityForResult(i,PICK);});
  button("Timer handmatig toevoegen",()->manual());
  button("Account toevoegen",()->editAccount());
  button("Overzicht vernieuwen",()->showTimers());
  root.addView(label("ACTIEVE UPGRADES",20));
  items=new LinearLayout(this);items.setOrientation(1);root.addView(items);showTimers();
 }
 private void editAccount(){EditText e=new EditText(this);e.setSingleLine(true);new android.app.AlertDialog.Builder(this).setTitle("Nieuw account").setView(e).setPositiveButton("Toevoegen",(d,w)->{String s=e.getText().toString().trim();if(!s.isEmpty()){accounts.add(s);persist();draw();}}).setNegativeButton("Annuleren",null).show();}
 private void manual(){LinearLayout form=new LinearLayout(this);form.setOrientation(1);EditText n=new EditText(this),t=new EditText(this);n.setHint("Gebouwnaam");t.setHint("Tijd, bijvoorbeeld 3h 15m");form.addView(n);form.addView(t);new android.app.AlertDialog.Builder(this).setTitle("Timer toevoegen").setView(form).setPositiveButton("Opslaan",(d,w)->{long seconds=parse(t.getText().toString());if(seconds>0){try{JSONObject o=new JSONObject();o.put("id",UUID.randomUUID().toString());o.put("account",accounts.get(account.getSelectedItemPosition()));o.put("category",categories[category.getSelectedItemPosition()]);o.put("name",n.getText().toString());o.put("finish",System.currentTimeMillis()+seconds*1000);upgrades.add(o);persist();showTimers();}catch(Exception ignored){}}}).setNegativeButton("Annuleren",null).show();}
 private static long parse(String raw){
  String s=raw.trim().toLowerCase(java.util.Locale.ROOT).replace('ı','1');
  s=s.replaceAll("(?i)(?<![a-z0-9])[il](?=\\s*[dhms])","1");
  s=s.replaceAll("(?i)(?<![a-z0-9])s(?=\\s*h)","5");
  s=s.replaceAll("(?i)(?<=\\d)o(?=\\s*[dhms])","0");
  java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d+)\\s*([dhms])").matcher(s);
  long total=0;int consumed=0;boolean found=false;java.util.HashSet<String> seen=new java.util.HashSet<>();
  while(m.find()){
   if(!s.substring(consumed,m.start()).trim().isEmpty())return 0;
   String unit=m.group(2);if(!seen.add(unit))return 0;
   long value;try{value=Long.parseLong(m.group(1));}catch(Exception e){return 0;}
   if(value>9999)return 0;
   total+=value*(unit.equals("d")?86400L:unit.equals("h")?3600L:unit.equals("m")?60L:1L);
   consumed=m.end();found=true;
  }
  return found&&s.substring(consumed).trim().isEmpty()?total:0;
 }
 private static String format(long seconds){long d=seconds/86400,h=seconds%86400/3600,m=seconds%3600/60;return(d>0?d+"d ":"")+(h>0?h+"h ":"")+(m>0?m+"m ":"")+(seconds<3600?seconds%60+"s":"");}
 private void showTimers(){if(items==null)return;items.removeAllViews();long now=System.currentTimeMillis();for(JSONObject o:upgrades){String name=o.optString("name"),ac=o.optString("account"),cat=o.optString("category");long remain=Math.max(0,(o.optLong("finish")-now)/1000);TextView v=label("⚒  "+name+"    •    "+format(remain)+"\n"+ac+"  /  "+cat,16);v.setBackgroundColor(0xff29253e);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,5,0,5);items.addView(v,lp);v.setOnLongClickListener(x->{new android.app.AlertDialog.Builder(this).setMessage("Timer verwijderen?").setPositiveButton("Verwijderen",(d,w)->{upgrades.remove(o);persist();showTimers();}).setNegativeButton("Annuleren",null).show();return true;});}}
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req!=PICK||result!=RESULT_OK||data==null)return;try(InputStream stream=getContentResolver().openInputStream(data.getData())){Bitmap bmp=BitmapFactory.decodeStream(stream);if(bmp!=null)scan(bmp);}catch(Exception e){toast("Foto kon niet worden geopend");}}
 private void scan(Bitmap bmp){
  // Seven builder slots are a maximum, not a target. Read all active rows, including brown hero/pet rows.
  // The screenshot layout is 1312x1182: active rows end before the Suggested upgrades heading.
  int w=bmp.getWidth(),h=bmp.getHeight();
  int left=(int)(w*.587),right=(int)(w*.998);
  int top=(int)(h*.071),bottom=(int)(h*.44);
  if(right<=left||bottom<=top){toast("Ongeldig screenshotformaat");return;}
  Bitmap panel=Bitmap.createBitmap(bmp,left,top,right-left,bottom-top);
  TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(InputImage.fromBitmap(panel,0))
   .addOnSuccessListener(text->{
    int cutoff=bottom;
    for(Text.TextBlock block:text.getTextBlocks())for(Text.Line line:block.getLines()){
     String s=line.getText().toLowerCase(java.util.Locale.ROOT);
     if(s.contains("suggested")||s.contains("other upgrades")){
      Rect rect=line.getBoundingBox();if(rect!=null)cutoff=Math.min(cutoff,top+rect.top);
     }
    }
    panel.recycle();
    // Fixed row bands follow the menu's own repeated row layout, not green pixels.
    // Include the Elixir Collector, whose progress bar is brown.
    ArrayList<Rect> rows=new ArrayList<>();
    int first=(int)(h*.072),stride=(int)(h*.053),height=(int)(h*.046);
    for(int i=0;i<7;i++){
     int y=first+i*stride;
     if(y+height>cutoff)break;
     rows.add(new Rect(left,y,right,Math.min(h,y+height)));
    }
    if(rows.isEmpty()){toast("Geen actieve rijen gevonden");return;}
    pending.clear();scanRow(bmp,rows,0);
   }).addOnFailureListener(e->{panel.recycle();toast("Menudetectie mislukt");});
 }
 private static String cleanName(String raw){
  String s=raw.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z ]"," ").replaceAll("\\s+"," ").trim();
  if(s.contains("craft")&&s.contains("station"))return "Crafting Station";
  if(s.contains("builder")&&s.contains("hut"))return "Builder's Hut";
  if(s.contains("hidden")&&s.contains("tesla"))return "Hidden Tesla";
  if(s.contains("elixir")&&s.contains("collector"))return "Elixir Collector";
  return raw.trim();
 }
 private void scanRow(Bitmap bmp,ArrayList<Rect> rows,int index){
  if(index>=rows.size()){review();return;}
  Rect r=rows.get(index);
  int split=r.left+(int)(r.width()*.70);
  Bitmap name=Bitmap.createBitmap(bmp,r.left,r.top,split-r.left,r.height());
  Bitmap timer=Bitmap.createBitmap(bmp,split,r.top,r.right-split,r.height());
  Bitmap n2=Bitmap.createScaledBitmap(name,name.getWidth()*3,name.getHeight()*3,true);
  Bitmap t2=Bitmap.createScaledBitmap(timer,timer.getWidth()*4,timer.getHeight()*4,true);
  name.recycle();timer.recycle();
  com.google.mlkit.vision.text.TextRecognizer recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
  recognizer.process(InputImage.fromBitmap(n2,0)).addOnSuccessListener(n->{
   recognizer.process(InputImage.fromBitmap(t2,0)).addOnSuccessListener(t->{
    String raw=t.getText().replace("\\n"," ").trim();
    try{JSONObject o=new JSONObject();o.put("name",cleanName(n.getText().replace("\\n"," ")));o.put("raw",raw);o.put("seconds",parse(raw));o.put("row",index);pending.add(o);}catch(Exception ignored){}
    n2.recycle();t2.recycle();scanRow(bmp,rows,index+1);
   }).addOnFailureListener(e->{n2.recycle();t2.recycle();scanRow(bmp,rows,index+1);});
  }).addOnFailureListener(e->{n2.recycle();t2.recycle();scanRow(bmp,rows,index+1);});
 }
 private void review(){if(pending.isEmpty()){toast("Geen OCR-resultaten");return;}LinearLayout form=new LinearLayout(this);form.setOrientation(1);ScrollView scroll=new ScrollView(this);scroll.addView(form);ArrayList<EditText> names=new ArrayList<>(),times=new ArrayList<>();
  for(JSONObject o:pending){form.addView(label("Rij "+(o.optInt("row")+1),16));EditText n=new EditText(this),t=new EditText(this);n.setSingleLine(true);t.setSingleLine(true);n.setText(o.optString("name"));t.setText(o.optString("raw"));n.setHint("Naam");t.setHint("Bijv. 3h 15m");form.addView(n);form.addView(t);names.add(n);times.add(t);}
  new android.app.AlertDialog.Builder(this).setTitle("Controleer "+pending.size()+" rijen").setView(scroll).setPositiveButton("Opslaan",(d,w)->{
   String ac=accounts.get(account.getSelectedItemPosition()),cat=categories[category.getSelectedItemPosition()];long now=System.currentTimeMillis();
   ArrayList<JSONObject> replacement=new ArrayList<>();ArrayList<String> invalid=new ArrayList<>();
   for(int i=0;i<names.size();i++){
    long sec=parse(times.get(i).getText().toString());String name=names.get(i).getText().toString().trim();
    if(sec<=0||name.isEmpty()){invalid.add("Rij "+(i+1));continue;}
    try{JSONObject o=new JSONObject();o.put("id",UUID.randomUUID().toString());o.put("account",ac);o.put("category",cat);o.put("name",name);o.put("finish",now+sec*1000);replacement.add(o);}catch(Exception ignored){}
   }
   if(!invalid.isEmpty()){
    new android.app.AlertDialog.Builder(this).setTitle("Scan niet opgeslagen")
     .setMessage("Ongeldige timer(s): "+android.text.TextUtils.join(", ",invalid)+". Corrigeer deze rijen en scan opnieuw. Bestaande timers blijven behouden.")
     .setPositiveButton("OK",null).show();
    return;
   }
   for(int i=upgrades.size()-1;i>=0;i--){JSONObject old=upgrades.get(i);if(ac.equals(old.optString("account"))&&cat.equals(old.optString("category")))upgrades.remove(i);}
   upgrades.addAll(replacement);persist();showTimers();toast(replacement.size()+" timers opgeslagen");
  }).setNegativeButton("Annuleren",null).show();
 }
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
