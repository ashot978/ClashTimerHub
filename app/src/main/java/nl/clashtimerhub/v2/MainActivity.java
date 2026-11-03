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
 private Bitmap lastScanBitmap;
 private final android.os.Handler ticker=new android.os.Handler(android.os.Looper.getMainLooper());
 private final Runnable tick=new Runnable(){public void run(){if(items!=null)showTimers();ticker.postDelayed(this,1000);}};
 @Override protected void onResume(){super.onResume();ticker.removeCallbacks(tick);ticker.post(tick);}
 @Override protected void onPause(){ticker.removeCallbacks(tick);super.onPause();}
 private static final Pattern TIME=Pattern.compile("(?i)(?:(\\d+)\\s*d\\s*)?(?:(\\d+)\\s*h\\s*)?(?:(\\d+)\\s*m\\s*)?(?:(\\d+)\\s*s\\s*)?");
 @Override public void onCreate(Bundle b){super.onCreate(b);load();draw();}
 private void load(){
  try{JSONArray a=new JSONArray(getPreferences(0).getString("accounts",new JSONArray(Arrays.asList("Ashot","Ashotgun","AshotRush","Rushot","Shotty","Ashotty","RushShot")).toString()));for(int i=0;i<a.length();i++)accounts.add(a.getString(i));}catch(Exception e){accounts.add("Ashot");}
  try{JSONArray a=new JSONArray(getPreferences(0).getString("upgrades","[]"));for(int i=0;i<a.length();i++)upgrades.add(a.getJSONObject(i));}catch(Exception ignored){}
 }
 private void persist(){getPreferences(0).edit().putString("accounts",new JSONArray(accounts).toString()).putString("upgrades",new JSONArray(upgrades).toString()).apply();}
 private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
 private android.graphics.drawable.GradientDrawable shape(int color,int radius){
  android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();
  d.setColor(color);d.setCornerRadius(dp(radius));return d;
 }
 private TextView label(String s,int size){
  TextView v=new TextView(this);v.setText(s);v.setTextColor(0xfff5f4ff);v.setTextSize(size);
  v.setPadding(dp(12),dp(12),dp(12),dp(12));return v;
 }
 private void button(String title,Runnable run){
  Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextSize(15);
  b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff8950c7));b.setTextColor(Color.WHITE);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(56));lp.setMargins(0,dp(6),0,dp(6));root.addView(b,lp);
  b.setOnClickListener(v->run.run());
 }
 private void draw(){
  getWindow().setStatusBarColor(0xff0e1020);getWindow().setNavigationBarColor(0xff0e1020);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
  root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(16),dp(20),dp(16),dp(28));
  root.setBackgroundColor(0xff160f25);scroll.addView(root);setContentView(scroll);
  TextView heading=label("⚔  CLASH TIMER HUB",27);heading.setTypeface(null,android.graphics.Typeface.BOLD);heading.setTextColor(0xffffd88a);root.addView(heading);
  TextView subtitle=label("COMMAND CENTER   /   V3",12);subtitle.setTextColor(0xffb5a5ff);root.addView(subtitle);
  root.addView(label("ACCOUNT",12));
  account=new Spinner(this);account.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,accounts));
  account.setBackground(shape(0xff38294d,14));root.addView(account,new LinearLayout.LayoutParams(-1,dp(52)));
  root.addView(label("UPGRADE TYPE",12));
  category=new Spinner(this);category.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,categories));
  category.setBackground(shape(0xff262941,14));root.addView(category,new LinearLayout.LayoutParams(-1,dp(52)));
  android.widget.AdapterView.OnItemSelectedListener refresh=new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){showTimers();}};
  account.setOnItemSelectedListener(refresh);category.setOnItemSelectedListener(refresh);
  button("Scan screenshot",()->{Intent i=new Intent(Intent.ACTION_GET_CONTENT);i.setType("image/*");startActivityForResult(i,PICK);});
  LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);root.addView(actions);
  Button manual=new Button(this);manual.setText("＋ Timer");manual.setAllCaps(false);manual.setOnClickListener(v->manual());
  manual.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff49345c));
  actions.addView(manual,new LinearLayout.LayoutParams(0,dp(52),1));
  Button add=new Button(this);add.setText("＋ Account");add.setAllCaps(false);add.setOnClickListener(v->editAccount());
  add.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff353853));
  LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,dp(52),1);ap.setMargins(dp(8),0,0,0);actions.addView(add,ap);
  button("Deel timer-overzicht",()->shareTimers());
  button("Scanner kalibreren",()->calibrate());
  button("Alle accounts · Overzicht",()->overview());
  TextView section=label("ACTIVE UPGRADES  •  LIVE",18);section.setTypeface(null,android.graphics.Typeface.BOLD);root.addView(section);
  items=new LinearLayout(this);items.setOrientation(1);root.addView(items);showTimers();
 }
 private void shareTimers(){
  StringBuilder out=new StringBuilder("Clash Timer Hub · Upgrade overzicht\n");
  long now=System.currentTimeMillis();int count=0;
  for(String ac:accounts){
   StringBuilder section=new StringBuilder();
   for(JSONObject o:upgrades){
    if(!ac.equals(o.optString("account")))continue;
    long remain=Math.max(0,(o.optLong("finish")-now+999)/1000);
    section.append("• ").append(o.optString("name")).append(" [").append(o.optString("category")).append("] — ").append(remain==0?"Gereed":format(remain)).append("\n");
    count++;
   }
   if(section.length()>0)out.append("\n").append(ac).append("\n").append(section);
  }
  if(count==0){toast("Er zijn nog geen timers om te delen");return;}
  Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/plain");send.putExtra(Intent.EXTRA_TEXT,out.toString());
  startActivity(Intent.createChooser(send,"Deel je timers"));
 }
 private void calibrate(){
  LinearLayout form=new LinearLayout(this);form.setOrientation(1);form.setPadding(dp(18),dp(12),dp(18),dp(12));
  TextView caption=label("Stel in waar de timertekst begint en eindigt binnen iedere upgrade-rij. De instelling wordt onthouden voor volgende scans.",14);form.addView(caption);
  int[] vals={getPreferences(0).getInt("timerStart",70),getPreferences(0).getInt("timerEnd",94)};
  TextView a=label("Timer begint: "+vals[0]+"%",15),b=label("Timer eindigt: "+vals[1]+"%",15);
  SeekBar start=new SeekBar(this),end=new SeekBar(this);start.setMax(90);end.setMax(100);
  start.setProgress(vals[0]);end.setProgress(vals[1]);
  start.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){
   public void onProgressChanged(SeekBar bar,int progress,boolean user){vals[0]=progress;a.setText("Timer begint: "+progress+"%");}
   public void onStartTrackingTouch(SeekBar bar){}public void onStopTrackingTouch(SeekBar bar){}
  });
  end.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){
   public void onProgressChanged(SeekBar bar,int progress,boolean user){vals[1]=progress;b.setText("Timer eindigt: "+progress+"%");}
   public void onStartTrackingTouch(SeekBar bar){}public void onStopTrackingTouch(SeekBar bar){}
  });
  form.addView(a);form.addView(start);form.addView(b);form.addView(end);
  new android.app.AlertDialog.Builder(this).setTitle("Timergebied kalibreren").setView(form)
   .setPositiveButton("Opslaan",(d,w)->{
    if(vals[1]<=vals[0]+3){toast("Eindpositie moet minimaal 4% na de beginpositie liggen");return;}
    getPreferences(0).edit().putInt("timerStart",vals[0]).putInt("timerEnd",vals[1]).apply();
    toast("Scannerkalibratie opgeslagen");
   }).setNegativeButton("Annuleren",null).show();
 }
 private void overview(){
  long now=System.currentTimeMillis();StringBuilder result=new StringBuilder();
  int active=0,done=0;
  for(String ac:accounts){
   int a=0,d=0;long next=Long.MAX_VALUE;
   for(JSONObject o:upgrades)if(ac.equals(o.optString("account"))){
    long finish=o.optLong("finish");if(finish<=now)d++;else{a++;next=Math.min(next,finish);}
   }
   active+=a;done+=d;
   result.append(ac).append(": ").append(a).append(" bezig · ").append(d).append(" gereed");
   if(next!=Long.MAX_VALUE)result.append(" · volgende over ").append(format((next-now+999)/1000));
   result.append("\\n");
  }
  new android.app.AlertDialog.Builder(this).setTitle("Command Center · "+active+" bezig / "+done+" gereed")
   .setMessage(result.toString()).setPositiveButton("Sluiten",null).show();
 }
 private void editTimer(JSONObject o){
  LinearLayout form=new LinearLayout(this);form.setOrientation(1);form.setPadding(dp(16),dp(8),dp(16),0);
  EditText n=new EditText(this),t=new EditText(this);
  n.setSingleLine(true);n.setText(o.optString("name"));n.setHint("Naam");
  t.setSingleLine(true);t.setHint("Bijv. 2d 5h");long remaining=Math.max(0,(o.optLong("finish")-System.currentTimeMillis()+999)/1000);
  t.setText(format(remaining));form.addView(n);form.addView(t);
  android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("Timer bewerken").setView(form)
   .setPositiveButton("Opslaan",null).setNeutralButton("Verwijderen",(d,w)->confirmDelete(o))
   .setNegativeButton("Annuleren",null).create();
  dialog.setOnShowListener(ev->dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   long seconds=parse(t.getText().toString());String name=n.getText().toString().trim();
   if(seconds<=0||name.isEmpty()){t.setError("Vul een geldige duur in, bijvoorbeeld 2d 5h");return;}
   try{o.put("name",name);o.put("finish",System.currentTimeMillis()+seconds*1000);}catch(Exception ignored){}
   persist();showTimers();dialog.dismiss();
  }));dialog.show();
 }
 private void confirmDelete(JSONObject o){
  new android.app.AlertDialog.Builder(this).setTitle("Timer verwijderen?")
   .setMessage(o.optString("name")).setPositiveButton("Verwijderen",(d,w)->{upgrades.remove(o);persist();showTimers();})
   .setNegativeButton("Annuleren",null).show();
 }
 private void editAccount(){EditText e=new EditText(this);e.setSingleLine(true);new android.app.AlertDialog.Builder(this).setTitle("Nieuw account").setView(e).setPositiveButton("Toevoegen",(d,w)->{String s=e.getText().toString().trim();if(!s.isEmpty()){accounts.add(s);persist();draw();}}).setNegativeButton("Annuleren",null).show();}
 private void manual(){LinearLayout form=new LinearLayout(this);form.setOrientation(1);EditText n=new EditText(this),t=new EditText(this);n.setHint("Gebouwnaam");t.setHint("Tijd, bijvoorbeeld 3h 15m");form.addView(n);form.addView(t);new android.app.AlertDialog.Builder(this).setTitle("Timer toevoegen").setView(form).setPositiveButton("Opslaan",(d,w)->{long seconds=parse(t.getText().toString());if(seconds>0){try{JSONObject o=new JSONObject();o.put("id",UUID.randomUUID().toString());o.put("account",accounts.get(account.getSelectedItemPosition()));o.put("category",categories[category.getSelectedItemPosition()]);o.put("name",n.getText().toString());o.put("finish",System.currentTimeMillis()+seconds*1000);upgrades.add(o);persist();showTimers();}catch(Exception ignored){}}}).setNegativeButton("Annuleren",null).show();}
 private static long parse(String raw){
  if(raw==null)return 0;
  String s=raw.toLowerCase(java.util.Locale.ROOT).replace('ı','1');
  s=s.replaceAll("(?<![a-z0-9])[il](?=\\s*[dhms])","1");
  s=s.replaceAll("(?<![a-z0-9])s(?=\\s*h)","5");
  s=s.replaceAll("(?<=\\d)o(?=\\s*[dhms])","0");
  // OCR frequently appends E/6 after the final minute or hour unit.
  s=s.replaceAll("(?<=[dhms])\\s*[e€]+\\s*$","");
  s=s.replaceAll("(?<=m)6\\s*$","");
  java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d+)\\s*([dhms])").matcher(s);
  long total=0;int consumed=0;boolean found=false;
  java.util.HashSet<String> seen=new java.util.HashSet<>();
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
 private void showTimers(){
  if(items==null)return;items.removeAllViews();long now=System.currentTimeMillis();
  String chosenAccount=account!=null&&account.getSelectedItem()!=null?account.getSelectedItem().toString():"";
  String chosenCategory=category!=null&&category.getSelectedItem()!=null?category.getSelectedItem().toString():"";
  int count=0;
  for(JSONObject o:upgrades){
   String name=o.optString("name"),ac=o.optString("account"),cat=o.optString("category");
   if(!ac.equals(chosenAccount)||!cat.equals(chosenCategory))continue;
   count++;long remain=Math.max(0,(o.optLong("finish")-now+999)/1000);
   LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
   card.setPadding(dp(16),dp(10),dp(16),dp(12));card.setBackground(shape(0xff30243e,16));
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(5),0,dp(5));items.addView(card,lp);
   TextView title=label("⚒  "+name,18);title.setTypeface(null,android.graphics.Typeface.BOLD);card.addView(title);
   TextView time=label(remain==0?"✓ Gereed":format(remain),24);time.setTextColor(remain==0?0xff72e6b0:0xffffd88a);time.setTypeface(null,android.graphics.Typeface.BOLD);card.addView(time);
   card.setOnClickListener(x->editTimer(o));
   card.setOnLongClickListener(x->{confirmDelete(o);return true;});
  }
  if(count>0){TextView hint=label("Tik op een timer om deze te bewerken · Houd ingedrukt om te verwijderen",12);hint.setTextColor(0xffa5a9c0);items.addView(hint);}
  if(count==0){TextView empty=label("Nog geen actieve upgrades. Scan een screenshot om te beginnen.",15);empty.setTextColor(0xffa5a9c0);items.addView(empty);}
 }
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
  if(s.contains("craft")&&(s.contains("station")||s.contains("tation")))return "Crafting Station";
  if(s.contains("builder")&&s.contains("hut"))return "Builder's Hut";
  if(s.contains("hidden")&&s.contains("tesla"))return "Hidden Tesla";
  if(s.contains("elixir")&&s.contains("collector"))return "Elixir Collector";
  return raw.trim();
 }
 private void scanRow(Bitmap bmp,ArrayList<Rect> rows,int index){
  if(index>=rows.size()){review();return;}
  Rect r=rows.get(index);
  int startPct=getPreferences(0).getInt("timerStart",70),endPct=getPreferences(0).getInt("timerEnd",94);
  if(endPct<=startPct+3){startPct=70;endPct=94;}
  int split=r.left+(int)(r.width()*startPct/100.0);
  Bitmap name=Bitmap.createBitmap(bmp,r.left,r.top,split-r.left,r.height());
  int timerRight=r.left+(int)(r.width()*endPct/100.0); // Exclude stopwatch icon
  Bitmap timer=Bitmap.createBitmap(bmp,split,r.top,timerRight-split,r.height());
  Bitmap n2=Bitmap.createScaledBitmap(name,name.getWidth()*3,name.getHeight()*3,true);
  Bitmap t2=Bitmap.createScaledBitmap(timer,timer.getWidth()*3,timer.getHeight()*3,true);
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
  for(JSONObject o:pending){form.addView(label("Rij "+(o.optInt("row")+1),16));EditText n=new EditText(this),t=new EditText(this);n.setSingleLine(true);t.setSingleLine(true);n.setText(o.optString("name"));String recognized=o.optString("raw");t.setText(recognized);if(parse(recognized)>0)t.setCompoundDrawablesWithIntrinsicBounds(0,0,android.R.drawable.checkbox_on_background,0);else t.setError("Controleer deze tijd");n.setHint("Naam");t.setHint("Bijv. 3h 15m");form.addView(n);form.addView(t);names.add(n);times.add(t);}
  android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("Controleer "+pending.size()+" rijen").setView(scroll).setPositiveButton("Opslaan",null).setNegativeButton("Annuleren",null).create();
  dialog.setOnShowListener(showEvent->dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   String ac=accounts.get(account.getSelectedItemPosition()),cat=categories[category.getSelectedItemPosition()];long now=System.currentTimeMillis();
   ArrayList<JSONObject> replacement=new ArrayList<>();ArrayList<String> invalid=new ArrayList<>();
   for(int i=0;i<names.size();i++){
    long sec=parse(times.get(i).getText().toString());String name=names.get(i).getText().toString().trim();
    if(sec<=0||name.isEmpty()){invalid.add("Rij "+(i+1));continue;}
    try{JSONObject o=new JSONObject();o.put("id",UUID.randomUUID().toString());o.put("account",ac);o.put("category",cat);o.put("name",name);o.put("finish",now+sec*1000);o.put("scanned",true);replacement.add(o);}catch(Exception ignored){}
   }
   if(!invalid.isEmpty()){
    new android.app.AlertDialog.Builder(this).setTitle("Scan niet opgeslagen")
     .setMessage("Ongeldige timer(s): "+android.text.TextUtils.join(", ",invalid)+". Corrigeer deze rijen en druk opnieuw op Opslaan. Bestaande timers blijven behouden.")
     .setPositiveButton("OK",null).show();
    return;
   }
   // Only replace previous screenshot-scanned entries; never destroy manually created timers.
   for(int i=upgrades.size()-1;i>=0;i--){JSONObject old=upgrades.get(i);if(ac.equals(old.optString("account"))&&cat.equals(old.optString("category"))&&old.optBoolean("scanned",false))upgrades.remove(i);}
   upgrades.addAll(replacement);persist();showTimers();toast(replacement.size()+" timers opgeslagen");dialog.dismiss();
  }));dialog.show();
 }
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
