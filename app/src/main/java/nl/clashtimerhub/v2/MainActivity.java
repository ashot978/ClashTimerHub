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
 private LinearLayout root,items,dashboard;
 private Spinner account,category;
 private final ArrayList<String> accounts=new ArrayList<>();
 private final ArrayList<JSONObject> upgrades=new ArrayList<>();
 private final ArrayList<JSONObject> pending=new ArrayList<>();
 private static final int PICK=12,IMPORT=13,RESTORE=14;
 private Bitmap lastScanBitmap;
 private final android.os.Handler ticker=new android.os.Handler(android.os.Looper.getMainLooper());
 private final Runnable tick=new Runnable(){public void run(){if(items!=null)showTimers();if(dashboard!=null)renderDashboard();ticker.postDelayed(this,5000);}};
 @Override protected void onResume(){super.onResume();ticker.removeCallbacks(tick);ticker.post(tick);}
 @Override protected void onPause(){ticker.removeCallbacks(tick);super.onPause();}
 private static final Pattern TIME=Pattern.compile("(?i)(?:(\\d+)\\s*d\\s*)?(?:(\\d+)\\s*h\\s*)?(?:(\\d+)\\s*m\\s*)?(?:(\\d+)\\s*s\\s*)?");
 @Override public void onCreate(Bundle b){super.onCreate(b);load();draw();if(android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},55);TimerWidget.schedule(this);handleSharedJson(getIntent());}
 @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleSharedJson(intent);}
 private void handleSharedJson(Intent intent){
  if(intent==null||!Intent.ACTION_SEND.equals(intent.getAction()))return;
  String payload=intent.getStringExtra(Intent.EXTRA_TEXT);
  if(payload==null||payload.length()>10000000||!payload.contains("buildings")){toast("Geen geldige Clash JSON gedeeld");return;}
  new android.app.AlertDialog.Builder(this).setTitle("Clash-data snel importeren?")
   .setMessage("De player tag bepaalt welk account wordt bijgewerkt.")
   .setPositiveButton("Importeren",(d,w)->importClashData(payload))
   .setNegativeButton("Annuleren",null).show();
 }
 private void load(){
  try{JSONArray a=new JSONArray(getPreferences(0).getString("accounts","[]"));for(int i=0;i<a.length();i++)accounts.add(a.getString(i));}catch(Exception ignored){}
  try{JSONArray a=new JSONArray(getPreferences(0).getString("upgrades","[]"));for(int i=0;i<a.length();i++)upgrades.add(a.getJSONObject(i));}catch(Exception ignored){}
 }
 private void persist(){getPreferences(0).edit().putString("accounts",new JSONArray(accounts).toString()).putString("upgrades",new JSONArray(upgrades).toString()).apply();TimerWidget.schedule(this);}
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
  root.addView(label("ALLE ACCOUNTS · EERSTVOLGENDE BOUWER",19));
  if(accounts.isEmpty())root.addView(label("Welkom! Kopieer je Clash-spelerdata en synchroniseer om je eerste account toe te voegen.",15));
  dashboard=new LinearLayout(this);dashboard.setOrientation(1);root.addView(dashboard);renderDashboard();
  root.addView(label("ACCOUNT",12));
  account=new Spinner(this);account.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,accounts));
  account.setBackground(shape(0xff38294d,14));root.addView(account,new LinearLayout.LayoutParams(-1,dp(52)));
  root.addView(label("UPGRADE TYPE",12));
  category=new Spinner(this);category.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,categories));
  category.setBackground(shape(0xff262941,14));root.addView(category,new LinearLayout.LayoutParams(-1,dp(52)));
  android.widget.AdapterView.OnItemSelectedListener refresh=new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){showTimers();}};
  account.setOnItemSelectedListener(refresh);category.setOnItemSelectedListener(refresh);
  TextView section=label("GESELECTEERD ACCOUNT  ·  LIVE",18);section.setTypeface(null,android.graphics.Typeface.BOLD);root.addView(section);
  items=new LinearLayout(this);items.setOrientation(1);root.addView(items);showTimers();
  root.addView(label("BEHEER EN SYNCHRONISATIE",15));
  button("🔔 Meldingen instellen",()->notificationSettings());
  button("📋 Synchroniseer vanaf klembord",()->importClipboard());
  button("Back-up kopiëren",()->copyBackup());
  button("Back-up delen",()->backup());
  button("📋 Back-up herstellen vanaf klembord",()->restoreBackupClipboard());
  button("Back-up herstellen uit bestand",()->{Intent i=new Intent(Intent.ACTION_GET_CONTENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,RESTORE);});
  button("Importeer Clash-data (JSON)",()->{Intent i=new Intent(Intent.ACTION_GET_CONTENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,IMPORT);});
  LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);root.addView(actions);
  Button manual=new Button(this);manual.setText("＋ Timer");manual.setAllCaps(false);manual.setOnClickListener(v->manual());
  manual.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff49345c));
  actions.addView(manual,new LinearLayout.LayoutParams(0,dp(52),1));
  Button add=new Button(this);add.setText("＋ Account");add.setAllCaps(false);add.setOnClickListener(v->editAccount());
  add.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff353853));
  LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,dp(52),1);ap.setMargins(dp(8),0,0,0);actions.addView(add,ap);
  button("Details per account",()->overview());


 }
 private void notificationSettings(){
  boolean enabled=getPreferences(0).getBoolean("notifyEnabled",true);
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(16),dp(8),dp(16),dp(8));
  CheckBox toggle=new CheckBox(this);toggle.setText("Meldingen bij voltooide upgrades");toggle.setChecked(enabled);panel.addView(toggle);
  TextView hint=label("Android moet meldingen toestaan. Meldingen zijn lokaal en gebruiken de laatst geïmporteerde timers.",13);panel.addView(hint);
  new android.app.AlertDialog.Builder(this).setTitle("Meldingen").setView(panel)
   .setPositiveButton("Opslaan",(d,w)->{getPreferences(0).edit().putBoolean("notifyEnabled",toggle.isChecked()).apply();TimerWidget.schedule(this);})
   .setNeutralButton("Android-instellingen",(d,w)->{
    Intent i=new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
    i.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName());startActivity(i);
   }).setNegativeButton("Annuleren",null).show();
 }
 private void copyBackup(){
  try{
   JSONObject data=new JSONObject();data.put("format","clash-timer-hub-backup-v1");
   data.put("accounts",new JSONArray(accounts));data.put("upgrades",new JSONArray(upgrades));
   data.put("playerTags",new JSONObject(getPreferences(0).getString("playerTags","{}")));
   android.content.ClipboardManager manager=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
   if(manager==null){toast("Klembord niet beschikbaar");return;}
   manager.setPrimaryClip(android.content.ClipData.newPlainText("Clash Timer Hub back-up",data.toString()));
   toast("Back-up van "+accounts.size()+" accounts gekopieerd");
  }catch(Exception e){toast("Back-up kopiëren mislukt");}
 }
 private void backup(){
  try{
   JSONObject data=new JSONObject();data.put("format","clash-timer-hub-backup-v1");
   data.put("accounts",new JSONArray(accounts));data.put("upgrades",new JSONArray(upgrades));
   data.put("playerTags",new JSONObject(getPreferences(0).getString("playerTags","{}")));
   Intent send=new Intent(Intent.ACTION_SEND);send.setType("application/json");
   send.putExtra(Intent.EXTRA_TEXT,data.toString(2));
   startActivity(Intent.createChooser(send,"Deel Clash Timer Hub back-up"));
  }catch(Exception e){toast("Back-up maken mislukt");}
 }
 private void restoreBackupClipboard(){
  android.content.ClipboardManager manager=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
  if(manager==null||!manager.hasPrimaryClip()){toast("Klembord is leeg");return;}
  android.content.ClipData clip=manager.getPrimaryClip();
  if(clip==null||clip.getItemCount()==0){toast("Geen back-up op klembord");return;}
  CharSequence text=clip.getItemAt(0).coerceToText(this);
  if(text==null||text.length()==0){toast("Geen tekst op klembord");return;}
  if(text.length()>2000000){toast("Back-up te groot");return;}
  restoreBackup(text.toString());
 }
 private void restoreBackup(String input){
  try{
   JSONObject data=new JSONObject(input.trim());
   if(!"clash-timer-hub-backup-v1".equals(data.optString("format")))throw new JSONException("Onbekend back-upformaat");
   JSONArray a=data.getJSONArray("accounts"),u=data.getJSONArray("upgrades");
   if(a.length()>100||u.length()>10000)throw new JSONException("Back-up is te groot");
   ArrayList<String> names=new ArrayList<>();ArrayList<JSONObject> timers=new ArrayList<>();
   for(int i=0;i<a.length();i++){String n=a.getString(i).trim();if(n.isEmpty()||names.contains(n))throw new JSONException("Ongeldig account");names.add(n);}
   for(int i=0;i<u.length();i++){JSONObject o=u.getJSONObject(i);if(!names.contains(o.optString("account"))||o.optLong("finish")<=0)throw new JSONException("Ongeldige timer");timers.add(o);}
   new android.app.AlertDialog.Builder(this).setTitle("Back-up herstellen?")
    .setMessage("Dit vervangt alle bestaande accounts en timers door "+names.size()+" accounts en "+timers.size()+" timers.")
    .setPositiveButton("Herstellen",(d,w)->{
     accounts.clear();accounts.addAll(names);upgrades.clear();upgrades.addAll(timers);
     getPreferences(0).edit().putString("playerTags",data.optJSONObject("playerTags")==null?"{}":data.optJSONObject("playerTags").toString()).apply();
     persist();draw();toast("Back-up hersteld");
    }).setNegativeButton("Annuleren",null).show();
  }catch(Exception e){toast("Ongeldige back-up: "+e.getMessage());}
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
 private void renderDashboard(){
  if(dashboard==null)return;
  dashboard.removeAllViews();
  long now=System.currentTimeMillis();
  ArrayList<String> ordered=new ArrayList<>(accounts);
  Collections.sort(ordered,(a,b)->{
   int ca=completedCount(a,now),cb=completedCount(b,now);
   if(ca!=cb)return Integer.compare(cb,ca);
   long x=nextBuilder(a,now),y=nextBuilder(b,now);
   if(x==Long.MAX_VALUE)return y==Long.MAX_VALUE?0:1;
   if(y==Long.MAX_VALUE)return -1;
   return Long.compare(x,y);
  });
  JSONObject tags;
  try{tags=new JSONObject(getPreferences(0).getString("playerTags","{}"));}catch(Exception e){tags=new JSONObject();}
  int totalActive=0;
  for(JSONObject o:upgrades)if("Bouwers".equals(o.optString("category"))&&o.optLong("finish")>now)totalActive++;
  TextView summary=label(accounts.size()+" accounts  ·  "+totalActive+" actieve bouwertimers",13);
  summary.setTextColor(0xffb5a5ff);dashboard.addView(summary);
  LinearLayout gridRow=null;
  int gridIndex=0;
  for(String ac:ordered){
   if(gridIndex%2==0){gridRow=new LinearLayout(this);gridRow.setOrientation(LinearLayout.HORIZONTAL);dashboard.addView(gridRow);}
   gridIndex++;
   long soonest=nextBuilder(ac,now);
   JSONObject first=null;int active=0;
   for(JSONObject o:upgrades)if(ac.equals(o.optString("account"))&&"Bouwers".equals(o.optString("category"))&&o.optLong("finish")>now){
    active++;if(o.optLong("finish")==soonest)first=o;
   }
   LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
   card.setPadding(dp(10),dp(2),dp(10),dp(2));
   card.setBackground(shape(first==null?0xff244735:0xff30243e,14));
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(2),dp(3),dp(2),dp(3));gridRow.addView(card,lp);
   LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.VERTICAL);card.addView(top);
   TextView title=label(ac,15);title.setTypeface(null,android.graphics.Typeface.BOLD);
   top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
   TextView countdown=label(first==null?"Geen timer":format((soonest-now+999)/1000),16);
   countdown.setTypeface(null,android.graphics.Typeface.BOLD);countdown.setTextColor(first==null?0xff82e8a7:0xffffd88a);top.addView(countdown);
   String tag=tags.optString(ac,"");
   String name=first==null?"Geen actieve buildertimer":cleanName(first.optString("name"));
   int completed=completedCount(ac,now);
   long oldest=oldestCompleted(ac,now);
   String waiting=completed==0?"":(" · "+completed+" klaar · oudste "+format((now-oldest)/1000)+" geleden");
   TextView detail=label(name+"\n"+active+" bezig · vrij: ?"+waiting,12);
   detail.setTextColor(0xffc5b7d6);card.addView(detail);
   if(completed>0){
    Button checked=new Button(this);checked.setText("Gecontroleerd");checked.setAllCaps(false);
    checked.setOnClickListener(v->acknowledgeAccount(ac));card.addView(checked);
   }
   card.setOnClickListener(v->{int i=accounts.indexOf(ac);if(i>=0)account.setSelection(i);});
  }
 }
 private int completedCount(String ac,long now){
  long acknowledged=getPreferences(0).getLong("ack_"+ac,0L);
  int count=0;
  for(JSONObject o:upgrades){
   long finish=o.optLong("finish");
   if(ac.equals(o.optString("account"))&&finish>acknowledged&&finish<=now)count++;
  }
  return count;
 }
 private long oldestCompleted(String ac,long now){
  long acknowledged=getPreferences(0).getLong("ack_"+ac,0L);
  long oldest=Long.MAX_VALUE;
  for(JSONObject o:upgrades){
   long finish=o.optLong("finish");
   if(ac.equals(o.optString("account"))&&finish>acknowledged&&finish<=now)oldest=Math.min(oldest,finish);
  }
  return oldest;
 }
 private void acknowledgeAccount(String ac){
  getPreferences(0).edit().putLong("ack_"+ac,System.currentTimeMillis()).apply();
  renderDashboard();showTimers();
 }
 private long nextBuilder(String ac,long now){
  long min=Long.MAX_VALUE;
  for(JSONObject o:upgrades)if(ac.equals(o.optString("account"))&&"Bouwers".equals(o.optString("category"))&&o.optLong("finish")>now)min=Math.min(min,o.optLong("finish"));
  return min;
 }
 private void showAllNextBuilders(){
  LinearLayout list=new LinearLayout(this);list.setOrientation(1);list.setPadding(dp(12),dp(8),dp(12),dp(8));
  long now=System.currentTimeMillis();
  for(String ac:accounts){
   JSONObject earliest=null;long soonest=Long.MAX_VALUE;
   for(JSONObject o:upgrades){
    if(!ac.equals(o.optString("account"))||!"Bouwers".equals(o.optString("category")))continue;
    long finish=o.optLong("finish");if(finish<soonest){soonest=finish;earliest=o;}
   }
   TextView line=label(ac+"  ·  "+(earliest==null?"Geen builder bekend":cleanName(earliest.optString("name"))+"  ·  "+(soonest<=now?"Gereed":format((soonest-now+999)/1000))),16);
   line.setBackground(shape(0xff30243e,12));list.addView(line);
  }
  ScrollView scroll=new ScrollView(this);scroll.addView(list);
  new android.app.AlertDialog.Builder(this).setTitle("Alle accounts · Eerstvolgende builder")
   .setView(scroll).setPositiveButton("Sluiten",null).show();
 }
 private void importClipboard(){
  android.content.ClipboardManager manager=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
  if(manager==null||!manager.hasPrimaryClip()){toast("Klembord is leeg. Kopieer eerst je spelergegevens in Clash of Clans.");return;}
  android.content.ClipData clip=manager.getPrimaryClip();
  if(clip==null||clip.getItemCount()==0){toast("Geen tekst op het klembord gevonden");return;}
  CharSequence text=clip.getItemAt(0).coerceToText(this);
  if(text==null||text.length()==0){toast("Geen Clash-data gevonden op het klembord");return;}
  if(text.length()>10000000){toast("Gekopieerde data is te groot");return;}
  String payload=text.toString().trim();
  if(!payload.contains("\"buildings\"")&&!payload.contains("\"heroes\"")&&!payload.contains("\"units\"")){
   toast("Dit lijkt geen Clash of Clans-spelerdata. Kopieer eerst Copy Player Data.");return;
  }
  String selected=account.getSelectedItem()==null?"nieuw account":account.getSelectedItem().toString();
  new android.app.AlertDialog.Builder(this).setTitle("Synchroniseren met "+selected+"?")
   .setMessage("De actieve upgrades uit de gekopieerde Clash-data worden aan dit account gekoppeld. Handmatige timers blijven behouden.")
   .setPositiveButton("Synchroniseren",(d,w)->importClashData(payload))
   .setNegativeButton("Annuleren",null).show();
 }
 private void importClashData(String input){
  try{
   int start=input.indexOf('{'),end=input.lastIndexOf('}');
   if(start<0||end<=start)throw new JSONException("Geen JSON gevonden");
   JSONObject json=new JSONObject(input.substring(start,end+1));
   if(!json.has("buildings")&&!json.has("heroes")&&!json.has("units"))throw new JSONException("Geen Clash-export");
   String ac=account.getSelectedItem()==null?"":account.getSelectedItem().toString();
   JSONObject player=json.optJSONObject("player");
   String tag=json.optString("tag",json.optString("playerTag",player==null?"":player.optString("tag",""))).trim().toUpperCase(java.util.Locale.ROOT);
   JSONObject tags=new JSONObject(getPreferences(0).getString("playerTags","{}"));
   String playerName=json.optString("name",player==null?"":player.optString("name","")).trim();
   if(ac.isEmpty()&&!tag.isEmpty()&&!playerName.isEmpty())ac=playerName;
   String linked=tags.optString(ac,"");
   if(!tag.isEmpty()){
    String matched=null;
    for(String candidate:accounts)if(tag.equalsIgnoreCase(tags.optString(candidate,""))){matched=candidate;break;}
    if(matched!=null){
     ac=matched;
    }else if(false){
     final String payload=input,detected=tag;
     new android.app.AlertDialog.Builder(this).setTitle("Onbekende speler: "+tag)
      .setMessage("Deze player tag hoort niet bij "+ac+". Kies het juiste account om verkeerd overschrijven te voorkomen.")
      .setItems(accounts.toArray(new String[0]),(d,which)->{
       String chosen=accounts.get(which);
       String existing=tags.optString(chosen,"");
       if(!existing.isEmpty()&&!existing.equalsIgnoreCase(detected)){
        toast("Dit account is al gekoppeld aan "+existing);return;
       }
       account.setSelection(which);
       importClashData(payload);
      }).setNegativeButton("Annuleren",null).show();
     return;
    }
   }
   if(tag.isEmpty()&&ac.isEmpty()){if(playerName.isEmpty()){toast("Export bevat geen spelernaam of player tag. Voeg eerst een account toe.");return;}ac=playerName;}
   if(!tag.isEmpty()&&!tags.optString(ac,"").equalsIgnoreCase(tag)){
    String base=playerName.isEmpty()?tag:playerName;
    String candidate=base;int suffix=2;
    while(accounts.contains(candidate)){candidate=base+" ("+suffix+")";suffix++;}
    ac=candidate;
   }
   if(!accounts.contains(ac))accounts.add(ac);
   long now=System.currentTimeMillis();
   long timestamp=json.optLong("timestamp",now/1000)*1000L;
   // Timestamp is the moment at which the exported remaining seconds were measured.
   if(timestamp>now+3600000||timestamp<now-30L*86400000L)timestamp=now;
   ArrayList<JSONObject> parsed=new ArrayList<>();
   String[] sections={"buildings","heroes","units","spells","pets","traps"};
   for(String section:sections){
    JSONArray arr=json.optJSONArray(section);if(arr==null)continue;
    for(int i=0;i<arr.length();i++){
     JSONObject entry=arr.optJSONObject(i);if(entry==null)continue;
     long id=entry.optLong("data",-1);if(id<0)continue;
     String cat="pets".equals(section)?"Pets":("units".equals(section)||"spells".equals(section))?"Laboratorium":"Bouwers";
     JSONArray types=entry.optJSONArray("types");
     if(types!=null)for(int t=0;t<types.length();t++){
      JSONObject type=types.optJSONObject(t);if(type==null)continue;
      JSONArray modules=type.optJSONArray("modules");if(modules==null)continue;
      for(int m=0;m<modules.length();m++){
       JSONObject module=modules.optJSONObject(m);if(module==null)continue;
       long moduleId=module.optLong("data",-1);
       if(moduleId>=0)addExportTimer(parsed,ac,cat,moduleId,module.optLong("timer"),timestamp,m);
      }
     }
     JSONArray timers=entry.optJSONArray("timers");
     if(timers!=null){
      for(int j=0;j<timers.length();j++){
       Object obj=timers.opt(j);long sec=obj instanceof JSONObject?((JSONObject)obj).optLong("timer"):timers.optLong(j);
       addExportTimer(parsed,ac,cat,id,sec,timestamp,j);
      }
     }else addExportTimer(parsed,ac,cat,id,entry.optLong("timer"),timestamp,0);
    }
   }
   // An empty valid snapshot is still a successful account refresh.
   // Preserve manual timers and other accounts. Only replace earlier imports.
   for(int i=upgrades.size()-1;i>=0;i--){
    JSONObject old=upgrades.get(i);
    if(ac.equals(old.optString("account"))&&old.optBoolean("exported",false))upgrades.remove(i);
   }
   upgrades.addAll(parsed);getPreferences(0).edit().putLong("ack_"+ac,System.currentTimeMillis()).apply();if(!tag.isEmpty()){tags.put(ac,tag);getPreferences(0).edit().putString("playerTags",tags.toString()).apply();}persist();((ArrayAdapter)account.getAdapter()).notifyDataSetChanged();int selectedIndex=accounts.indexOf(ac);if(selectedIndex>=0)account.setSelection(selectedIndex);showTimers();renderDashboard();toast(parsed.size()+" upgrades geïmporteerd voor "+ac);
  }catch(Exception e){toast("Ongeldige Clash-export: "+e.getMessage());}
 }
 private void addExportTimer(ArrayList<JSONObject> out,String ac,String cat,long id,long sec,long ts,int index)throws JSONException{
  if(sec<=0)return;
  long finish=ts+sec*1000L;
  if(finish<=System.currentTimeMillis())return;
  JSONObject o=new JSONObject();
  o.put("id",UUID.randomUUID().toString());o.put("account",ac);o.put("category",cat);
  o.put("name",exportName(id));o.put("finish",finish);o.put("exported",true);o.put("sourceId",id);
  out.add(o);
 }
 private String exportName(long id){
  if(id==1000089)return "Crafting Station";
  if(id==1000002)return "Elixir Collector";
  if(id==1000015)return "Builder's Hut";
  if(id==1000019)return "Hidden Tesla";
  if(id==1000001)return "Town Hall";
  if(id==1000007)return "Laboratory";
  if(id==28000000)return "Barbarian King";
  if(id==28000001)return "Archer Queen";
  if(id==28000002)return "Grand Warden";
  if(id==28000004)return "Royal Champion";
  return ClashNames.name(id);
 }
 private void overview(){
  long now=System.currentTimeMillis();
  LinearLayout list=new LinearLayout(this);list.setOrientation(1);list.setPadding(dp(12),dp(8),dp(12),dp(8));
  for(String ac:accounts){
   int builders=0,lab=0,pets=0;long soonest=Long.MAX_VALUE;String name="";
   for(JSONObject o:upgrades){
    if(!ac.equals(o.optString("account"))||o.optLong("finish")<=now)continue;
    String type=o.optString("category");
    if("Bouwers".equals(type)){builders++;if(o.optLong("finish")<soonest){soonest=o.optLong("finish");name=o.optString("name");}}
    else if("Laboratorium".equals(type))lab++;
    else if("Pets".equals(type))pets++;
   }
   String next=soonest==Long.MAX_VALUE?"Geen actieve builder":cleanName(name)+" · "+format((soonest-now+999)/1000);
   TextView line=label(ac+"\n"+next+"\n"+builders+" bouwers bezig · "+lab+" lab · "+pets+" pets",15);
   line.setBackground(shape(0xff30243e,12));
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(4),0,dp(4));list.addView(line,lp);
  }
  ScrollView scroll=new ScrollView(this);scroll.addView(list);
  new android.app.AlertDialog.Builder(this).setTitle("Alle accounts · Details")
   .setView(scroll).setPositiveButton("Sluiten",null).show();
 }
 private void editTimer(JSONObject o){
  LinearLayout form=new LinearLayout(this);form.setOrientation(1);form.setPadding(dp(16),dp(8),dp(16),0);
  EditText n=new EditText(this),t=new EditText(this);
  n.setSingleLine(true);n.setText(cleanName(o.optString("name")));n.setHint("Naam");
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
  int count=0;long nextFinish=Long.MAX_VALUE;
  if("Bouwers".equals(chosenCategory))for(JSONObject candidate:upgrades){
   if(chosenAccount.equals(candidate.optString("account"))&&"Bouwers".equals(candidate.optString("category"))&&candidate.optLong("finish")>now)
    nextFinish=Math.min(nextFinish,candidate.optLong("finish"));
  }
  for(JSONObject o:upgrades){
   String name=cleanName(o.optString("name")),ac=o.optString("account"),cat=o.optString("category");
   if(!ac.equals(chosenAccount)||!cat.equals(chosenCategory))continue;
   if(o.optLong("finish")<=now)continue;
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
  if(count==0){TextView empty=label("Geen actieve timers. Importeer Clash-data via het klembord.",15);empty.setTextColor(0xffa5a9c0);items.addView(empty);}
 }
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null)return;
  if(req==RESTORE){try(InputStream stream=getContentResolver().openInputStream(data.getData())){java.io.ByteArrayOutputStream buffer=new java.io.ByteArrayOutputStream();byte[] bytes=new byte[8192];int n;while((n=stream.read(bytes))!=-1){buffer.write(bytes,0,n);if(buffer.size()>2000000)throw new java.io.IOException("Back-up te groot");}restoreBackup(buffer.toString("UTF-8"));}catch(Exception e){toast("Herstellen mislukt: "+e.getMessage());}return;}
  if(req==IMPORT){try(InputStream stream=getContentResolver().openInputStream(data.getData())){
   java.io.ByteArrayOutputStream buffer=new java.io.ByteArrayOutputStream();byte[] bytes=new byte[8192];int n;
   while((n=stream.read(bytes))!=-1){buffer.write(bytes,0,n);if(buffer.size()>10_000_000)throw new java.io.IOException("Export te groot");}
   importClashData(buffer.toString("UTF-8"));
  }catch(Exception e){toast("Import mislukt: "+e.getMessage());}return;}
  if(req!=PICK)return;try(InputStream stream=getContentResolver().openInputStream(data.getData())){Bitmap bmp=BitmapFactory.decodeStream(stream);if(bmp!=null)scan(bmp);}catch(Exception e){toast("Foto kon niet worden geopend");}}
 private void scan(Bitmap bmp){
  // Only the FIRST active upgrade and the builder availability counter matter.
  // The bottom-right builder badge (0/7 in the supplied screenshot) is below the list.
  int w=bmp.getWidth(),h=bmp.getHeight();
  int left=(int)(w*.587),right=Math.min(w,(int)(w*.998));
  int top=(int)(h*.072),height=(int)(h*.046);
  if(right<=left||top+height>h){toast("Screenshotformaat niet ondersteund");return;}
  ArrayList<Rect> rows=new ArrayList<>();
  rows.add(new Rect(left,top,right,top+height));
  pending.clear();
  // Read builder count independently so one failed OCR region does not lose the other.
  int badgeLeft=(int)(w*.585),badgeTop=(int)(h*.915);
  int badgeRight=(int)(w*.69),badgeBottom=Math.min(h,(int)(h*.995));
  if(badgeRight<=badgeLeft||badgeBottom<=badgeTop){scanRow(bmp,rows,0);return;}
  Bitmap badge=Bitmap.createBitmap(bmp,badgeLeft,badgeTop,badgeRight-badgeLeft,badgeBottom-badgeTop);
  Bitmap enlarged=Bitmap.createScaledBitmap(badge,badge.getWidth()*3,badge.getHeight()*3,true);
  badge.recycle();
  com.google.mlkit.vision.text.TextRecognizer recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
  recognizer.process(InputImage.fromBitmap(enlarged,0)).addOnSuccessListener(result->{
   String raw=result.getText().replaceAll("\\s+","").replace('O','0').replace('o','0');
   java.util.regex.Matcher match=java.util.regex.Pattern.compile("(\\d+)\\s*[/|]\\s*(\\d+)").matcher(raw);
   if(match.find()){
    try{
     int available=Integer.parseInt(match.group(1)),total=Integer.parseInt(match.group(2));
     if(total>0&&total<=20&&available<=total){
      getPreferences(0).edit().putInt("builderAvailable",available).putInt("builderTotal",total).apply();
     }
    }catch(NumberFormatException ignored){}
   }else{
    getPreferences(0).edit().remove("builderAvailable").remove("builderTotal").apply();
   }
   enlarged.recycle();recognizer.close();scanRow(bmp,rows,0);
  }).addOnFailureListener(e->{enlarged.recycle();recognizer.close();scanRow(bmp,rows,0);});
 }
 private static String cleanName(String raw){
  String s=raw.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z ]"," ").replaceAll("\\s+"," ").trim();
  if(s.contains("craft")&&(s.contains("station")||s.contains("tation")))return "Crafting Station";
  if(s.contains("builder")&&s.contains("hut"))return "Builder's Hut";
  if((s.contains("hidden")||s.contains("hidde")||s.contains("hidd"))&&s.contains("tesla"))return "Hidden Tesla";
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
  int free=getPreferences(0).getInt("builderAvailable",-1),total=getPreferences(0).getInt("builderTotal",-1);
  String counter=free>=0&&total>0?" · Vrije bouwers: "+free+"/"+total:" · Bouwersstatus niet herkend";
  android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("Controleer eerste upgrade"+counter).setView(scroll).setPositiveButton("Opslaan",null).setNegativeButton("Annuleren",null).create();
  dialog.setOnShowListener(showEvent->dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   String ac=accounts.get(account.getSelectedItemPosition()),cat=categories[category.getSelectedItemPosition()];long now=System.currentTimeMillis();
   ArrayList<JSONObject> replacement=new ArrayList<>();ArrayList<String> invalid=new ArrayList<>();
   for(int i=0;i<names.size();i++){
    long sec=parse(times.get(i).getText().toString());String name=cleanName(names.get(i).getText().toString().trim());
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
