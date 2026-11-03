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
 private String lastLoadedSnapshot="";
 private final Runnable tick=new Runnable(){public void run(){if(activeTab==0&&dashboard!=null)renderDashboard();else if(activeTab==1||activeTab==2)refreshVisibleTimers();ticker.postDelayed(this,1000);}};
 @Override protected void onResume(){super.onResume();refreshExternalImport();ticker.removeCallbacks(tick);ticker.post(tick);}
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
 private String snapshot(){android.content.SharedPreferences p=getPreferences(0);return p.getString("accounts","[]")+"|"+p.getString("upgrades","[]");}
 private void refreshExternalImport(){String current=snapshot();if(!current.equals(lastLoadedSnapshot)){accounts.clear();upgrades.clear();load();if(!accounts.contains(selectedAccount)&&!accounts.isEmpty())selectedAccount=accounts.get(0);draw();}}
 private void load(){
  try{JSONArray a=new JSONArray(getPreferences(0).getString("accounts","[]"));for(int i=0;i<a.length();i++)accounts.add(a.getString(i));}catch(Exception ignored){}
  try{JSONArray a=new JSONArray(getPreferences(0).getString("upgrades","[]"));for(int i=0;i<a.length();i++)upgrades.add(a.getJSONObject(i));}catch(Exception ignored){}
  lastLoadedSnapshot=snapshot();
 }
 private void persist(){getPreferences(0).edit().putString("accounts",new JSONArray(accounts).toString()).putString("upgrades",new JSONArray(upgrades).toString()).apply();lastLoadedSnapshot=snapshot();TimerWidget.schedule(this);}
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
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(56));lp.setMargins(0,dp(8),0,dp(8));(screenContent!=null?screenContent:root).addView(b,lp);
  b.setOnClickListener(v->run.run());
 }
 private int activeTab=0;
 private LinearLayout screenContent;
 private String selectedAccount="";
 private void draw(){
  getWindow().setStatusBarColor(0xff0e1020);getWindow().setNavigationBarColor(0xff0e1020);
  LinearLayout shell=new LinearLayout(this);shell.setOrientation(1);shell.setBackgroundColor(0xff160f25);setContentView(shell);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
  root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(12),dp(22),dp(12),dp(64));scroll.addView(root);

  TextView heading=label("⚔  CLASH TIMER HUB",24);heading.setTypeface(null,android.graphics.Typeface.BOLD);heading.setTextColor(0xffffd88a);root.addView(heading);
  TextView subtitle=label("COMMAND CENTER / V4",11);subtitle.setTextColor(0xffb5a5ff);root.addView(subtitle);
  screenContent=new LinearLayout(this);screenContent.setOrientation(1);root.addView(screenContent);
  LinearLayout nav=new LinearLayout(this);nav.setBackgroundColor(0xff261a36);nav.setPadding(dp(4),dp(20),dp(4),dp(12));shell.addView(nav);
  String[] names={"Dashboard","Tijdlijn","Accounts","Instellingen"};
  for(int i=0;i<names.length;i++){
   final int tab=i;
   Button button=new Button(this);button.setText(names[i]);button.setAllCaps(false);button.setTextSize(14);button.setTypeface(null,android.graphics.Typeface.BOLD);button.setSingleLine(false);
   button.setTextColor(i==activeTab?0xffffd88a:0xffc6b9d6);
   button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(i==activeTab?0xff49335f:0xff261a36));
   button.setOnClickListener(v->{activeTab=tab;draw();});
   nav.addView(button,new LinearLayout.LayoutParams(0,dp(64),1));
  }
  shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  if(activeTab==0){
   screenContent.addView(label("ALLE ACCOUNTS · EERSTVOLGENDE BOUWER",17));
   dashboard=new LinearLayout(this);dashboard.setOrientation(1);screenContent.addView(dashboard);
   renderDashboard();
  }else if(activeTab==1){
   dashboard=null;showTimeline();
  }else if(activeTab==2){
   dashboard=null;showAccountScreen();
  }else{
   dashboard=null;showSettingsScreen();
  }
 }
 private void showSettingsScreen(){
  screenContent.addView(label("INSTELLINGEN EN SYNCHRONISATIE",19));
  button("⚔ Zwevende plakknop aan/uit",()->toggleFloatingSync());
  button("🔔 Meldingen instellen",()->notificationSettings());
  button("🔔 Testmelding versturen",()->sendTestNotification());
  button("⏰ Alarmtest over 1 minuut",()->{String status=TimerWidget.scheduleDiagnostic(this);new android.app.AlertDialog.Builder(this).setTitle("Alarmtest").setMessage(status).setPositiveButton("OK",null).show();});
  button("📋 Synchroniseer vanaf klembord",()->importClipboard());
  button("Back-up kopiëren",()->copyBackup());
  button("Back-up delen",()->backup());
  button("📋 Back-up herstellen vanaf klembord",()->restoreBackupClipboard());

 }
 private void showAccountScreen(){
  screenContent.addView(label("ACCOUNTS · ALLE UPGRADES",19));
  if(accounts.isEmpty()){screenContent.addView(label("Nog geen accounts. Gebruik de zwevende plakknop of importeer JSON.",14));return;}
  if(!accounts.contains(selectedAccount))selectedAccount=accounts.get(0);
  LinearLayout chips=new LinearLayout(this);chips.setOrientation(1);screenContent.addView(chips);
  for(String ac:accounts){
   Button choice=new Button(this);choice.setText((ac.equals(selectedAccount)?"● ":"○ ")+ac);choice.setAllCaps(false);
   choice.setOnClickListener(v->{selectedAccount=ac;draw();});chips.addView(choice);
  }
  if(selectedAccount.startsWith("#")){
   Button link=new Button(this);link.setText("Koppel deze player tag aan bestaand account");link.setAllCaps(false);
   link.setOnClickListener(v->linkImportedAccount(selectedAccount));screenContent.addView(link);
  }
  account=new Spinner(this);account.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,accounts));
  account.setSelection(Math.max(0,accounts.indexOf(selectedAccount)));account.setVisibility(View.GONE);screenContent.addView(account);
  category=new Spinner(this);category.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,categories));
  category.setVisibility(View.GONE);screenContent.addView(category);
  for(String cat:categories){
   TextView header=label(cat.toUpperCase(java.util.Locale.ROOT),16);header.setTextColor(0xffbba7f3);screenContent.addView(header);
   ArrayList<JSONObject> list=new ArrayList<>();
   for(JSONObject o:upgrades)if(selectedAccount.equals(o.optString("account"))&&cat.equals(o.optString("category"))&&o.optLong("finish")>System.currentTimeMillis())list.add(o);
   Collections.sort(list,(x,y)->Long.compare(x.optLong("finish"),y.optLong("finish")));
   if(list.isEmpty())screenContent.addView(label("Geen actieve upgrades",12));
   for(JSONObject o:list)addTimerRow(screenContent,o,true);
  }
 }
 private void linkImportedAccount(String source){
  ArrayList<String> targets=new ArrayList<>();
  for(String ac:accounts)if(!ac.equals(source))targets.add(ac);
  if(targets.isEmpty()){toast("Geen ander account om te koppelen");return;}
  new android.app.AlertDialog.Builder(this).setTitle("Koppel "+source+" aan account")
   .setItems(targets.toArray(new String[0]),(d,which)->{
    String target=targets.get(which);
    try{
     JSONObject tags=new JSONObject(getPreferences(0).getString("playerTags","{}"));
     String existing=tags.optString(target,"");
     if(!existing.isEmpty()&&!existing.equalsIgnoreCase(source)){toast("Doelaccount is al gekoppeld aan een andere player tag");return;}
     for(int i=upgrades.size()-1;i>=0;i--){
      JSONObject o=upgrades.get(i);
      if(target.equals(o.optString("account"))&&o.optBoolean("exported",false))upgrades.remove(i);
     }
     for(JSONObject o:upgrades)if(source.equals(o.optString("account")))o.put("account",target);
     accounts.remove(source);tags.remove(source);tags.put(target,source);
     getPreferences(0).edit().putString("playerTags",tags.toString()).apply();
     selectedAccount=target;persist();draw();toast(source+" gekoppeld aan "+target);
    }catch(Exception e){toast("Koppelen mislukt: "+e.getMessage());}
   }).setNegativeButton("Annuleren",null).show();
 }
 private void showTimeline(){
  screenContent.addView(label("TIJDLIJN · ALLE ACCOUNTS",19));
  final String[] filters={"Alles","Bouwers","Laboratorium","Pets"};
  LinearLayout filterRow=new LinearLayout(this);screenContent.addView(filterRow);
  for(String filter:filters){
   Button b=new Button(this);b.setText(filter);b.setTextSize(10);b.setAllCaps(false);
   b.setOnClickListener(v->{timelineFilter=filter;draw();});
   b.setTextColor(filter.equals(timelineFilter)?0xffffd88a:0xffd5c9e2);
   filterRow.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));
  }
  ArrayList<JSONObject> list=new ArrayList<>();
  for(JSONObject o:upgrades)if(o.optLong("finish")>System.currentTimeMillis()&&("Alles".equals(timelineFilter)||timelineFilter.equals(o.optString("category"))))list.add(o);
  Collections.sort(list,(x,y)->Long.compare(x.optLong("finish"),y.optLong("finish")));
  if(list.isEmpty())screenContent.addView(label("Geen actieve upgrades voor dit filter.",14));
  for(JSONObject o:list)addTimerRow(screenContent,o,false);
 }
 private void refreshVisibleTimers(){
  if(screenContent==null)return;
  for(int i=0;i<screenContent.getChildCount();i++){
   View v=screenContent.getChildAt(i);
   if(v instanceof LinearLayout){
    LinearLayout group=(LinearLayout)v;
    if(group.getTag() instanceof Long){
     long finish=(Long)group.getTag();
     if(group.getChildCount()>1&&group.getChildAt(1) instanceof TextView)
      ((TextView)group.getChildAt(1)).setText(format(Math.max(0,(finish-System.currentTimeMillis()+999)/1000))+"  ·  "+finishAt(finish));
    }
   }
  }
 }
 private String timelineFilter="Alles";
 private String finishAt(long timestamp){
  java.text.SimpleDateFormat fmt=new java.text.SimpleDateFormat("HH:mm",java.util.Locale.forLanguageTag("nl-NL"));
  java.util.Calendar now=java.util.Calendar.getInstance(),finish=java.util.Calendar.getInstance();finish.setTimeInMillis(timestamp);
  boolean today=now.get(java.util.Calendar.YEAR)==finish.get(java.util.Calendar.YEAR)&&now.get(java.util.Calendar.DAY_OF_YEAR)==finish.get(java.util.Calendar.DAY_OF_YEAR);
  now.add(java.util.Calendar.DAY_OF_YEAR,1);
  boolean tomorrow=now.get(java.util.Calendar.YEAR)==finish.get(java.util.Calendar.YEAR)&&now.get(java.util.Calendar.DAY_OF_YEAR)==finish.get(java.util.Calendar.DAY_OF_YEAR);
  if(today)return "Vandaag om "+fmt.format(new java.util.Date(timestamp));
  if(tomorrow)return "Morgen om "+fmt.format(new java.util.Date(timestamp));
  return new java.text.SimpleDateFormat("EEE d MMM · HH:mm",java.util.Locale.forLanguageTag("nl-NL")).format(new java.util.Date(timestamp));
 }
 private void addTimerRow(LinearLayout parent,JSONObject o,boolean editable){
  long finish=o.optLong("finish"),remain=Math.max(0,(finish-System.currentTimeMillis()+999)/1000);
  LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(10),dp(5),dp(10),dp(5));card.setBackground(shape(remain<=1800?0xff46351f:0xff30243e,12));card.setTag(finish);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(dp(2),dp(3),dp(2),dp(3));parent.addView(card,lp);
  card.addView(label((editable?"":o.optString("account")+" · ")+cleanName(o.optString("name")),14));
  TextView time=label(format(remain)+"  ·  "+finishAt(finish),13);time.setTextColor(0xffffd88a);card.addView(time);
  if(editable)card.setOnClickListener(v->editTimer(o));
  else card.setOnClickListener(v->{selectedAccount=o.optString("account");activeTab=2;draw();});
 }
 private void toggleFloatingSync(){
  android.content.SharedPreferences prefs=getPreferences(0);
  boolean active=prefs.getBoolean("floatingEnabled",false);
  Intent service=new Intent(this,FloatingSyncService.class);
  if(active){
   stopService(service);
   prefs.edit().putBoolean("floatingEnabled",false).apply();
   toast("Zwevende plakknop uitgeschakeld");
   return;
  }
  if(!android.provider.Settings.canDrawOverlays(this)){
   new android.app.AlertDialog.Builder(this)
    .setTitle("Zwevende plakknop")
    .setMessage("Geef Clash Timer Hub toestemming om over andere apps te worden weergegeven. Kom daarna terug en tik nogmaals op de knop.")
    .setPositiveButton("Toestemming geven",(d,w)->{
     Intent i=new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,android.net.Uri.parse("package:"+getPackageName()));
     startActivity(i);
    }).setNegativeButton("Annuleren",null).show();
   return;
  }
  try{
   if(android.os.Build.VERSION.SDK_INT>=26)startForegroundService(service);
   else startService(service);
   prefs.edit().putBoolean("floatingEnabled",true).apply();
   toast("Zwevende plakknop ingeschakeld");
  }catch(Exception e){toast("Kan zwevende knop niet starten: "+e.getMessage());}
 }
 private void sendTestNotification(){
  if(android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED){
   requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},55);
   toast("Sta meldingen toe en druk daarna opnieuw op Testmelding");return;
  }
  BuilderAlarm.sendTest(this);
  toast("Testmelding verzonden. Controleer je meldingenpaneel.");
 }
 private void notificationSettings(){
  android.content.SharedPreferences prefs=getPreferences(0);
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(16),dp(8),dp(16),dp(8));
  CheckBox enabled=new CheckBox(this);enabled.setText("Meldingen inschakelen");enabled.setChecked(prefs.getBoolean("notifyEnabled",true));panel.addView(enabled);
  CheckBox completed=new CheckBox(this);completed.setText("Melding zodra upgrade klaar is");completed.setChecked(prefs.getBoolean("notifyOnFinish",true));panel.addView(completed);
  panel.addView(label("Waarschuw mij zoveel minuten vooraf (0 = uit)",13));
  EditText lead=new EditText(this);lead.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);lead.setSingleLine(true);
  lead.setText(String.valueOf(prefs.getInt("notifyLeadMinutes",15)));lead.setSelectAllOnFocus(true);panel.addView(lead);
  panel.addView(label("Bijvoorbeeld 5, 10, 30, 45 of 120 minuten. Maximaal 10080 (7 dagen).",12));
  if(android.os.Build.VERSION.SDK_INT>=31){
   android.app.AlarmManager alarm=(android.app.AlarmManager)getSystemService(ALARM_SERVICE);
   if(alarm!=null&&!alarm.canScheduleExactAlarms()){
    Button precise=new Button(this);precise.setText("Sta exacte alarmen toe");
    precise.setOnClickListener(v->{
     try{startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:"+getPackageName())));}
     catch(Exception e){toast("Open de alarmtoestemming via Android-instellingen");}
    });
    panel.addView(precise);
   }
  }
  new android.app.AlertDialog.Builder(this).setTitle("Meldingen instellen").setView(panel)
   .setPositiveButton("Opslaan",(d,w)->{
    int minutes=15;
    try{minutes=Integer.parseInt(lead.getText().toString().trim());}catch(Exception ignored){}
    minutes=Math.max(0,Math.min(10080,minutes));
    prefs.edit().putBoolean("notifyEnabled",enabled.isChecked())
     .putBoolean("notifyOnFinish",completed.isChecked()).putInt("notifyLeadMinutes",minutes).apply();
    TimerWidget.schedule(this);toast("Meldingen opgeslagen");
   })
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
   return Long.compare(nextBuilder(a,now),nextBuilder(b,now));
  });
  int active=0,totalReady=0;
  for(JSONObject o:upgrades)if("Bouwers".equals(o.optString("category"))&&o.optLong("finish")>now)active++;
  for(String ac:accounts)totalReady+=completedCount(ac,now);
  LinearLayout stats=new LinearLayout(this);dashboard.addView(stats);
  String[] metrics={accounts.size()+"\\nACCOUNTS",active+"\\nBOUWERS BEZIG",totalReady+"\\nKLAAR"};
  for(String metric:metrics){
   TextView view=label(metric.replace("\\n","\n"),13);view.setGravity(android.view.Gravity.CENTER);
   view.setTextColor(0xffffd88a);view.setBackground(shape(0xff292139,12));
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(70),1);lp.setMargins(dp(2),dp(3),dp(2),dp(3));stats.addView(view,lp);
  }
  LinearLayout gridRow=null;int index=0;
  for(String ac:ordered){
   if(index%2==0){gridRow=new LinearLayout(this);dashboard.addView(gridRow);}
   index++;
   long soonest=nextBuilder(ac,now);
   JSONObject first=null;int busy=0;
   for(JSONObject o:upgrades)if(ac.equals(o.optString("account"))&&"Bouwers".equals(o.optString("category"))&&o.optLong("finish")>now){
    busy++;if(o.optLong("finish")==soonest)first=o;
   }
   int completed=completedCount(ac,now);
   boolean ready=completed>0,urgent=!ready&&soonest!=Long.MAX_VALUE&&soonest-now<=1800000L;
   int color=ready?0xff213a30:urgent?0xff443420:0xff30233e;
   LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(8),dp(6),dp(8),dp(8));card.setBackground(shape(color,14));
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(2),dp(3),dp(2),dp(3));gridRow.addView(card,lp);
   TextView name=label(ac,15);name.setTypeface(null,android.graphics.Typeface.BOLD);card.addView(name);
   if(ready){
    TextView done=label(completed+" BOUWER"+(completed==1?"":"S")+" KLAAR",14);done.setTextColor(0xff8be3ad);done.setTypeface(null,android.graphics.Typeface.BOLD);card.addView(done);
   }
   TextView timer=label(soonest==Long.MAX_VALUE?"Geen actieve builder":format(Math.max(0,(soonest-now+999)/1000)),18);
   timer.setTextColor(ready?0xff8be3ad:0xffffd783);timer.setTypeface(null,android.graphics.Typeface.BOLD);card.addView(timer);
   if(soonest!=Long.MAX_VALUE){
    TextView end=label(finishAt(soonest),12);end.setTextColor(0xffc9b9df);card.addView(end);
   }
   TextView detail=label((first==null?"Geen actieve builder":cleanName(first.optString("name")))+"\n"+busy+" bouwers bezig",12);
   detail.setTextColor(0xffd4c5e4);card.addView(detail);
   if(ready){
    Button checked=new Button(this);checked.setText("Gecontroleerd");checked.setTextSize(11);checked.setAllCaps(false);
    checked.setOnClickListener(v->acknowledgeAccount(ac));card.addView(checked);
   }
   card.setOnClickListener(v->{selectedAccount=ac;activeTab=2;draw();});
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
  String selected=selectedAccount.isEmpty()?"automatisch herkend account":selectedAccount;
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
   String ac=account!=null&&account.getSelectedItem()!=null?account.getSelectedItem().toString():"";
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
   upgrades.addAll(parsed);getPreferences(0).edit().putLong("ack_"+ac,System.currentTimeMillis()).apply();if(!tag.isEmpty()){tags.put(ac,tag);getPreferences(0).edit().putString("playerTags",tags.toString()).apply();}persist();selectedAccount=ac;draw();toast(parsed.size()+" upgrades geïmporteerd voor "+ac);
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
 private void editAccount(){EditText e=new EditText(this);e.setSingleLine(true);new android.app.AlertDialog.Builder(this).setTitle("Nieuw account").setView(e).setPositiveButton("Toevoegen",(d,w)->{String s=e.getText().toString().trim();if(!s.isEmpty()&&!accounts.contains(s)){accounts.add(s);selectedAccount=s;persist();draw();}}).setNegativeButton("Annuleren",null).show();}
 private void manual(){LinearLayout form=new LinearLayout(this);form.setOrientation(1);EditText n=new EditText(this),t=new EditText(this);n.setHint("Gebouwnaam");t.setHint("Tijd, bijvoorbeeld 3h 15m");form.addView(n);form.addView(t);new android.app.AlertDialog.Builder(this).setTitle("Timer toevoegen").setView(form).setPositiveButton("Opslaan",(d,w)->{long seconds=parse(t.getText().toString());if(seconds>0&&!accounts.isEmpty()){try{JSONObject o=new JSONObject();o.put("id",UUID.randomUUID().toString());o.put("account",selectedAccount.isEmpty()?accounts.get(0):selectedAccount);o.put("category","Bouwers");o.put("name",n.getText().toString());o.put("finish",System.currentTimeMillis()+seconds*1000);upgrades.add(o);persist();draw();}catch(Exception ignored){}}}).setNegativeButton("Annuleren",null).show();}
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
