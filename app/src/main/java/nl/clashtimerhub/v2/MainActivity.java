package nl.clashtimerhub.v2;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.Context;
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
 private static final int PICK=12,IMPORT=13,RESTORE=14,BACKUP_SAVE=76;
 private Bitmap lastScanBitmap;
 private PermissionWizard permissionWizard;
 private GoogleDriveUi googleDriveUi;
 private boolean updateDownloadListening=false;
 private final android.content.BroadcastReceiver updateDownloadListener=new android.content.BroadcastReceiver(){
  @Override public void onReceive(Context context,Intent intent){
   if(intent!=null&&android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())
      &&intent.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID,-1)
        ==getPreferences(0).getLong("activeUpdateDownloadId",-2)){
    processPendingUpdateDownload();
   }
  }
 };
 private void startListeningForUpdateDownload(){
  if(updateDownloadListening)return;
  android.content.IntentFilter filter=new android.content.IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE);
  if(android.os.Build.VERSION.SDK_INT>=33)
   registerReceiver(updateDownloadListener,filter,Context.RECEIVER_EXPORTED);
  else registerReceiver(updateDownloadListener,filter);
  updateDownloadListening=true;
 }
 private void stopListeningForUpdateDownload(){
  if(!updateDownloadListening)return;
  try{unregisterReceiver(updateDownloadListener);}catch(Exception ignored){}
  updateDownloadListening=false;
 }
 private void clearPendingUpdateDownload(){
  getPreferences(0).edit().remove("activeUpdateDownloadId")
   .remove("activeUpdateDownloadBuild").remove("activeUpdateDownloadHash")
   .remove("activeUpdateDownloadPath").apply();
 }
 private void processPendingUpdateDownload(){
  long id=getPreferences(0).getLong("activeUpdateDownloadId",-1);
  if(id<=0)return;
  android.app.DownloadManager manager=(android.app.DownloadManager)getSystemService(DOWNLOAD_SERVICE);
  if(manager==null)return;
  try(android.database.Cursor cursor=manager.query(new android.app.DownloadManager.Query().setFilterById(id))){
   if(cursor==null||!cursor.moveToFirst()){clearPendingUpdateDownload();showUpdateInstallError(
    "Android heeft de download niet meer. Controleer je downloads en probeer opnieuw.");return;}
   int status=cursor.getInt(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_STATUS));
   if(status==android.app.DownloadManager.STATUS_PENDING||
      status==android.app.DownloadManager.STATUS_RUNNING||
      status==android.app.DownloadManager.STATUS_PAUSED)return;
   long build=getPreferences(0).getLong("activeUpdateDownloadBuild",0);
   String hash=getPreferences(0).getString("activeUpdateDownloadHash","");
   String path=getPreferences(0).getString("activeUpdateDownloadPath","");
   if(status!=android.app.DownloadManager.STATUS_SUCCESSFUL){
    int reason=cursor.getInt(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_REASON));
    clearPendingUpdateDownload();
    showUpdateInstallError("Download mislukt (Android-code "+reason+"). Controleer netwerk en vrije opslagruimte.");
    return;
   }
   clearPendingUpdateDownload();
   UpdateInstaller.install(this,new java.io.File(path),build,hash);
  }catch(Exception e){
   clearPendingUpdateDownload();
   showUpdateInstallError("Kan de APK-download niet controleren: "+e.getMessage());
  }
 }

 private final android.os.Handler ticker=new android.os.Handler(android.os.Looper.getMainLooper());
 private String lastLoadedSnapshot="";
 private boolean foldExpanded=false;
 private LinearLayout detailPanel;
 private final ArrayList<TextView> dashboardClocks=new ArrayList<>();
 private final ArrayList<Long> dashboardFinishes=new ArrayList<>();
 private final android.content.SharedPreferences.OnSharedPreferenceChangeListener syncWatcher=(prefs,key)->{
  if("syncRevision".equals(key))ticker.post(()->refreshExternalImport());
 };
 private final Runnable tick=new Runnable(){public void run(){if(activeTab==0&&dashboard!=null){refreshDashboardClocks();refreshVisibleTimers();}else if(activeTab==1||activeTab==2)refreshVisibleTimers();ticker.postDelayed(this,1000);}};
 @Override protected void onResume(){
  super.onResume();
  if(permissionWizard!=null)permissionWizard.onResume();
  getPreferences(0).registerOnSharedPreferenceChangeListener(syncWatcher);
  refreshExternalImport();ticker.removeCallbacks(tick);ticker.post(tick);
  startListeningForUpdateDownload();
  processPendingUpdateDownload();
  String priorError=getPreferences(0).getString("lastInstallError","");
  if(!priorError.isEmpty()){
   getPreferences(0).edit().remove("lastInstallError").apply();
   showUpdateInstallError(priorError);
  }
  String pending=getPreferences(0).getString("pendingVerifiedApk","");
  if(!pending.isEmpty()&&getPackageManager().canRequestPackageInstalls()){
   long build=getPreferences(0).getLong("pendingVerifiedBuild",0);
   String hash=getPreferences(0).getString("pendingVerifiedSha256","");
   getPreferences(0).edit().remove("pendingVerifiedApk").remove("pendingVerifiedBuild")
    .remove("pendingVerifiedSha256").apply();
   UpdateInstaller.install(this,new java.io.File(pending),build,hash);
  }
  long last=getPreferences(0).getLong("lastUpdateCheck",0);
  if(System.currentTimeMillis()-last>6L*3600000L)checkUpdates(true);
 }
 @Override protected void onPause(){stopListeningForUpdateDownload();getPreferences(0).unregisterOnSharedPreferenceChangeListener(syncWatcher);ticker.removeCallbacks(tick);super.onPause();}
 private static final Pattern TIME=Pattern.compile("(?i)(?:(\\d+)\\s*d\\s*)?(?:(\\d+)\\s*h\\s*)?(?:(\\d+)\\s*m\\s*)?(?:(\\d+)\\s*s\\s*)?");
 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  AppData.migrate(this);load();
  permissionWizard=new PermissionWizard(this);
  googleDriveUi=new GoogleDriveUi(this,()->draw(),this::restoreBackup);
  draw();
  TimerWidget.schedule(this);
  handleWidgetNavigation(getIntent());
  handleSharedJson(getIntent());
  // Display after the activity is visible, not during an external JSON import.
  if(b==null&&getIntent()!=null&&!Intent.ACTION_SEND.equals(getIntent().getAction()))
   getWindow().getDecorView().post(()->{
    if(permissionWizard!=null&&!isFinishing()&&!isDestroyed())
     permissionWizard.start(false);
   });
 }
 @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
  super.onRequestPermissionsResult(requestCode,permissions,grantResults);
  if(permissionWizard!=null)permissionWizard.onRequestPermissionsResult(requestCode);
 }
 @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleWidgetNavigation(intent);handleSharedJson(intent);}
 private void handleWidgetNavigation(Intent intent){
  if(intent==null)return;
  if(intent.getBooleanExtra("widgetEnableOverlay",false)&&!getPreferences(0).getBoolean("floatingEnabled",false))toggleFloatingSync();
  String open=intent.getStringExtra("openAccount");
  if(open!=null&&accounts.contains(open)){selectedAccount=open;activeTab=0;draw();}
 }
 @Override public void onBackPressed(){
  if(activeTab==0&&!selectedAccount.isEmpty()){selectedAccount="";draw();return;}
  if(activeTab!=0){activeTab=0;selectedAccount="";draw();return;}
  super.onBackPressed();
 }
 private void handleSharedJson(Intent intent){
  if(intent==null||!Intent.ACTION_SEND.equals(intent.getAction()))return;
  String payload=intent.getStringExtra(Intent.EXTRA_TEXT);
  if(payload==null||payload.length()>10000000||!payload.contains("buildings")){toast("Geen geldige Clash JSON gedeeld");return;}
  new android.app.AlertDialog.Builder(this).setTitle(t("Clash-data snel importeren?"))
   .setMessage(t("De player tag bepaalt welk account wordt bijgewerkt."))
   .setPositiveButton(t("Importeren"),(d,w)->importClashData(payload))
   .setNegativeButton(t("Annuleren"),null).show();
 }
 private String snapshot(){android.content.SharedPreferences p=getPreferences(0);return p.getString("accounts","[]")+"|"+p.getString("upgrades","[]");}
 private void refreshExternalImport(){String current=snapshot();if(!current.equals(lastLoadedSnapshot)){accounts.clear();upgrades.clear();load();if(!accounts.contains(selectedAccount)&&!accounts.isEmpty())selectedAccount=accounts.get(0);draw();}}
 private void load(){
  try{JSONArray a=new JSONArray(getPreferences(0).getString("accounts","[]"));for(int i=0;i<a.length();i++)accounts.add(a.getString(i));}catch(Exception ignored){}
  try{JSONArray a=new JSONArray(getPreferences(0).getString("upgrades","[]"));for(int i=0;i<a.length();i++)upgrades.add(a.getJSONObject(i));}catch(Exception ignored){}
  lastLoadedSnapshot=snapshot();
 }
 private void persist(){getPreferences(0).edit().putString("accounts",new JSONArray(accounts).toString()).putString("upgrades",new JSONArray(upgrades).toString()).apply();lastLoadedSnapshot=snapshot();TimerWidget.schedule(this);}
 private String t(String dutch){return I18n.tr(this,dutch);}
 private String displayAccount(String key){return AppData.displayName(this,key);}
 private void chooseLanguage(){
  String[] languages={"🇬🇧  English","🇳🇱  Nederlands"};
  int selected=I18n.english(this)?0:1;
  new android.app.AlertDialog.Builder(this).setTitle(t("Taal"))
   .setSingleChoiceItems(languages,selected,(dialog,choice)->{
    I18n.choose(this,choice==0?"en":"nl");
    dialog.dismiss();
    draw();
    toast(choice==0?"Language saved: English":"Taal opgeslagen: Nederlands");
   }).setNegativeButton(t("Annuleren"),null).show();
 }
 private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
 private android.graphics.drawable.GradientDrawable shape(int color,int radius){
  android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();
  d.setColor(color);d.setCornerRadius(dp(radius));return d;
 }
 private android.graphics.drawable.GradientDrawable clashSurface(int fill,int border,int radius){
  int dark=android.graphics.Color.rgb((int)(Color.red(fill)*.82f),(int)(Color.green(fill)*.82f),(int)(Color.blue(fill)*.82f));
  android.graphics.drawable.GradientDrawable drawable=new android.graphics.drawable.GradientDrawable(
   android.graphics.drawable.GradientDrawable.Orientation.TL_BR,new int[]{fill,dark});
  drawable.setCornerRadius(dp(radius));drawable.setStroke(dp(1),border);
  return drawable;
 }
 private TextView label(String s,int size){
  TextView v=new TextView(this);v.setText(t(s));v.setTextColor(0xfff5f4ff);v.setTextSize(size);
  v.setPadding(dp(12),dp(12),dp(12),dp(12));return v;
 }
 private void button(String title,Runnable run){
  Button b=new Button(this);b.setText(t(title));b.setAllCaps(false);b.setTextSize(15);
  b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x55ffffff),clashSurface(0xff49315e,0xff76548e,12),null));b.setTextColor(Color.WHITE);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(56));lp.setMargins(0,dp(8),0,dp(8));(screenContent!=null?screenContent:root).addView(b,lp);
  b.setOnClickListener(v->run.run());
 }
 private int activeTab=0;
 private LinearLayout screenContent;
 private String selectedAccount="";
 private void draw(){
  foldExpanded=getResources().getConfiguration().screenWidthDp>=600;
  detailPanel=null;
  getWindow().setStatusBarColor(0xff0e1020);getWindow().setNavigationBarColor(0xff0e1020);
  LinearLayout shell=new LinearLayout(this);shell.setOrientation(1);shell.setBackgroundColor(0xff160f25);
  if(android.os.Build.VERSION.SDK_INT>=35)shell.setOnApplyWindowInsetsListener((v,insets)->{
   android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars());
   v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;
  });
  setContentView(shell);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
  root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(12),dp(22),dp(12),dp(64));scroll.addView(root);

  LinearLayout headingRow=new LinearLayout(this);headingRow.setOrientation(0);headingRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
  root.addView(headingRow,new LinearLayout.LayoutParams(-1,-2));
  TextView heading=label("⚔  CLASH TIMER HUB",24);heading.setTypeface(null,android.graphics.Typeface.BOLD);heading.setTextColor(0xffffd88a);
  headingRow.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
  TextView languageButton=label(I18n.flag(this)+" ▾",20);languageButton.setGravity(android.view.Gravity.CENTER);
  languageButton.setContentDescription(t("Taal wijzigen"));
  languageButton.setBackground(clashSurface(0xff332342,0xff75518f,12));
  LinearLayout.LayoutParams langParams=new LinearLayout.LayoutParams(dp(62),dp(48));langParams.setMargins(dp(4),0,dp(2),0);
  headingRow.addView(languageButton,langParams);languageButton.setOnClickListener(v->chooseLanguage());
  TextView subtitle=label("COMMAND CENTER / V7",11);subtitle.setTextColor(0xffb5a5ff);root.addView(subtitle);
  View accent=new View(this);accent.setBackgroundColor(0xffd2a96e);LinearLayout.LayoutParams accentLp=new LinearLayout.LayoutParams(dp(56),dp(2));accentLp.setMargins(dp(12),0,0,dp(8));root.addView(accent,accentLp);
  screenContent=new LinearLayout(this);screenContent.setOrientation(1);root.addView(screenContent);
  LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);
  nav.setBackgroundColor(0xff261a36);nav.setPadding(dp(6),dp(8),dp(6),dp(8));
  shell.addView(nav,new LinearLayout.LayoutParams(-1,dp(72)));
  String[] names={"Overzicht","Tijdlijn","Instellingen"};
  int[] navTabs={0,1,3};
  for(int i=0;i<names.length;i++){
   final int tab=navTabs[i];
   Button button=new Button(this);button.setText(t(names[i]));button.setAllCaps(false);
   button.setTextSize(14);button.setTypeface(null,android.graphics.Typeface.BOLD);
   button.setSingleLine(true);button.setEllipsize(android.text.TextUtils.TruncateAt.END);
   button.setGravity(android.view.Gravity.CENTER);
   button.setPadding(dp(2),0,dp(2),0);button.setMinHeight(0);button.setMinimumHeight(0);
   button.setTextColor(tab==activeTab?0xffffd88a:0xffc6b9d6);
   button.setBackground(clashSurface(tab==activeTab?0xff49335f:0xff261a36,tab==activeTab?0xffd8ae69:0xff352742,10));
   button.setOnClickListener(v->{activeTab=tab;draw();});
   LinearLayout.LayoutParams tabParams=new LinearLayout.LayoutParams(0,-1,1f);
   tabParams.setMargins(dp(3),0,dp(3),0);nav.addView(button,tabParams);
  }
  shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  if(activeTab==0){
   boolean open=selectedAccount!=null&&!selectedAccount.isEmpty()&&accounts.contains(selectedAccount);
   if(foldExpanded&&open){
    LinearLayout columns=new LinearLayout(this);columns.setOrientation(0);screenContent.addView(columns);
    LinearLayout left=new LinearLayout(this);left.setOrientation(1);
    LinearLayout right=new LinearLayout(this);right.setOrientation(1);
    LinearLayout.LayoutParams leftParams=new LinearLayout.LayoutParams(0,-2,45);
    LinearLayout.LayoutParams rightParams=new LinearLayout.LayoutParams(0,-2,55);rightParams.setMargins(dp(10),0,0,0);
    columns.addView(left,leftParams);columns.addView(right,rightParams);
    left.addView(label("⚒  ALLE ACCOUNTS",16));
    dashboard=new LinearLayout(this);dashboard.setOrientation(1);left.addView(dashboard);
    detailPanel=new LinearLayout(this);detailPanel.setOrientation(1);detailPanel.setPadding(dp(8),dp(8),dp(8),dp(8));
    detailPanel.setBackground(shape(0xff261c35,16));right.addView(detailPanel);
    renderDashboard();renderSelectedDetail();
   }else if(open){
    dashboard=null;
    detailPanel=new LinearLayout(this);detailPanel.setOrientation(1);detailPanel.setPadding(dp(8),dp(8),dp(8),dp(8));
    detailPanel.setBackground(shape(0xff261c35,16));screenContent.addView(detailPanel);
    renderSelectedDetail();
   }else{
    selectedAccount="";
    screenContent.addView(label("⚔  ALLE ACCOUNTS",17));
    dashboard=new LinearLayout(this);dashboard.setOrientation(1);screenContent.addView(dashboard);
    renderDashboard();
   }
  }else if(activeTab==1){
   dashboard=null;showTimeline();
  }else if(activeTab==2){
   dashboard=null;showAccountScreen();
  }else{
   dashboard=null;showSettingsScreen();
  }
 }
 private LinearLayout settingsSection(String title,String subtitle){
  TextView header=label(title,16);header.setTypeface(null,android.graphics.Typeface.BOLD);header.setTextColor(0xffffd783);screenContent.addView(header);
  if(!subtitle.isEmpty()){TextView sub=label(subtitle,12);sub.setTextColor(0xffb6a9c8);screenContent.addView(sub);}
  LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(6),dp(5),dp(6),dp(5));
  card.setBackground(clashSurface(0xff2c203b,0xff654875,14));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(dp(2),dp(3),dp(2),dp(12));screenContent.addView(card,lp);
  return card;
 }
 private void settingsItem(LinearLayout section,String iconText,String title,String description,Runnable task){
  LinearLayout item=new LinearLayout(this);item.setOrientation(0);item.setGravity(android.view.Gravity.CENTER_VERTICAL);item.setPadding(dp(10),dp(4),dp(8),dp(4));item.setMinimumHeight(dp(62));
  item.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x44ffffff),shape(0xff352748,9),null));
  TextView icon=label(iconText,20);icon.setTextColor(0xffffd783);icon.setPadding(dp(3),0,dp(10),0);
  item.addView(icon,new LinearLayout.LayoutParams(dp(44),-2));
  LinearLayout copy=new LinearLayout(this);copy.setOrientation(1);
  TextView name=label(title,15);name.setTypeface(null,android.graphics.Typeface.BOLD);name.setPadding(0,dp(3),0,dp(2));copy.addView(name);
  TextView helper=label(description,12);helper.setTextColor(0xffbdadce);helper.setPadding(0,0,0,dp(3));copy.addView(helper);
  item.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
  TextView chevron=label("›",22);chevron.setTextColor(0xffd8ae69);chevron.setPadding(dp(8),0,dp(5),0);item.addView(chevron);
  LinearLayout.LayoutParams row=new LinearLayout.LayoutParams(-1,-2);row.setMargins(0,dp(2),0,dp(2));section.addView(item,row);
  item.setOnClickListener(v->task.run());
 }
 private void showSettingsScreen(){
  screenContent.addView(label("INSTELLINGEN",20));
  LinearLayout language=settingsSection("🌐  TAAL","Kies je voorkeurstaal");
  settingsItem(language,I18n.flag(this),"Taal",I18n.flag(this)+"  "+I18n.label(this),()->chooseLanguage());
  LinearLayout sync=settingsSection("↻  SYNCHRONISATIE","Accounts bijwerken zonder Clash te verlaten");
  settingsItem(sync,"⚔","Zwevende plakknop",getPreferences(0).getBoolean("floatingEnabled",false)?"Actief · tik om uit te schakelen":"Uit · tik om in te schakelen",()->{toggleFloatingSync();draw();});
  settingsItem(sync,"▤","Synchroniseer vanaf klembord","Importeer een gekopieerde Clash-export",()->importClipboard());
  LinearLayout notifications=settingsSection("♧  MELDINGEN","Upgrade-alarmen en tests");
  settingsItem(notifications,"♧","Meldingen instellen","Gereedmelding en waarschuwingen",()->notificationSettings());
  settingsItem(notifications,"✓","Toestemmingen controleren",
   PermissionWizard.grantedCount(this)+"/4 actief · meldingen, zwevende knop, alarmen en app-updates",
   ()->{if(permissionWizard==null)permissionWizard=new PermissionWizard(this);permissionWizard.start(true);});
  settingsItem(notifications,"✓","Testmelding","Controleer of notificaties aankomen",()->sendTestNotification());
  settingsItem(notifications,"◷","Alarmtest over 1 minuut","Controleer of Android een alarm uitvoert",()->{
   String status=TimerWidget.scheduleDiagnostic(this);
   new android.app.AlertDialog.Builder(this).setTitle(t("Alarmtest")).setMessage(status).setPositiveButton(t("OK"),null).show();
  });
  LinearLayout widget=settingsSection("▦  HOMESCREENWIDGET","Toon standaard al je accounts");
  settingsItem(widget,"⚙","Widget instellen","Alle accounts of een eigen selectie",()->showWidgetSettings());
  LinearLayout players=settingsSection("♙  ACCOUNTS","Geef je accounts herkenbare namen");
  settingsItem(players,"✎","Accountnamen aanpassen","Verander de naam zonder de player tag te wijzigen",()->renameAccount());
  LinearLayout releases=settingsSection("↓  APP-UPDATES","Versie en GitHub-builds");
  TextView currentBuild=label(installedBuildDescription(),13);
  currentBuild.setTextColor(0xff8be3ad);
  currentBuild.setPadding(dp(12),dp(8),dp(12),dp(4));
  releases.addView(currentBuild);
  long known=getPreferences(0).getLong("latestFeedBuild",0);
  long installedCode=UpdateChecker.installedVersionCode(this);
  long installedBuild=installedCode>=1000?installedCode-1000:installedCode;
  TextView latestBuild=label(known>0?"Nieuwste GitHub-build: #"+known+(known>installedBuild?" · UPDATE BESCHIKBAAR":" · Up-to-date"):"Nieuwste GitHub-build: nog niet gecontroleerd",12);
  latestBuild.setTextColor(0xffbba7d1);
  latestBuild.setPadding(dp(12),dp(2),dp(12),dp(8));
  releases.addView(latestBuild);
  settingsItem(releases,"↻","Controleer op updates","Vergelijk met de nieuwste gepubliceerde APK",()->checkUpdates(false));
  settingsItem(releases,"↓","GitHub-builds bekijken","Open de builds in je browser",()->openUpdates());
  settingsItem(releases,"ⓘ","Update-diagnose",
   "Bekijk installatiecontrole, downloadstatus en laatste fout",()->showUpdateDiagnostics());
  LinearLayout backups=settingsSection("▣  BACK-UP","Je opgeslagen accounts veilig bewaren");
  settingsItem(backups,"▣","Back-up kopiëren","Accounts, timers, taal, meldingen en widgetinstellingen",()->copyBackup());
  settingsItem(backups,"↶","Back-up herstellen","Herstel timers en opgeslagen voorkeuren",()->restoreBackupClipboard());
  settingsItem(backups,"☁","Opslaan in Google Drive",
   "Deel een echt JSON-bestand rechtstreeks met de Google Drive-app",()->shareBackupToDrive());
  settingsItem(backups,"↓","Opslaan als bestand",
   "Kies een map en bestandsnaam voor een losse .json-back-up",()->saveBackupAsFile());
  settingsItem(backups,"↑","Back-upbestand importeren",
   "Kies een eerder opgeslagen .json-bestand om te herstellen",()->openBackupFile());
  TextView tip=label("Drive-back-ups via Delen zijn handmatig. Voor automatisch vervangen is een aparte Drive-koppeling nodig.",12);
  tip.setPadding(dp(12),dp(6),dp(12),dp(10));tip.setTextColor(0xffbca7d5);backups.addView(tip);
 }
 private void renameAccount(){
  if(accounts.isEmpty()){toast("Geen accounts beschikbaar");return;}
  String[] labels=new String[accounts.size()];
  for(int i=0;i<labels.length;i++)labels[i]=displayAccount(accounts.get(i))+" · "+accounts.get(i);
  new android.app.AlertDialog.Builder(this).setTitle(t("Account kiezen")).setItems(labels,(dialog,index)->{
   String key=accounts.get(index);
   EditText edit=new EditText(this);edit.setSingleLine(true);edit.setText(displayAccount(key));edit.selectAll();
   LinearLayout frame=new LinearLayout(this);frame.setPadding(dp(16),0,dp(16),0);frame.addView(edit);
   new android.app.AlertDialog.Builder(this).setTitle(t("Naam wijzigen"))
    .setMessage(t("De player tag en timers veranderen niet."))
    .setView(frame).setPositiveButton(t("Opslaan"),(d,w)->{
     String display=edit.getText().toString().trim();
     if(display.isEmpty()){toast("Naam mag niet leeg zijn");return;}
     AppData.saveAccountName(this,key,display);TimerWidget.update(this);draw();
    }).setNegativeButton(t("Annuleren"),null).show();
  }).setNegativeButton(t("Annuleren"),null).show();
 }
 /** The APK has GitHub run_number + 1000 as its Android versionCode. */
 private String installedBuildDescription(){
  try{
   android.content.pm.PackageInfo info=getPackageManager().getPackageInfo(getPackageName(),0);
   long code=android.os.Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;
   String name=info.versionName==null?"Onbekende versie":info.versionName;
   return "Geïnstalleerd: "+name+(code>=1001?" · build #"+(code-1000):" · versiecode "+code);
  }catch(Exception e){
   return "Geïnstalleerde build niet beschikbaar";
  }
 }
 private void configureUpdateFeed(){
  EditText url=new EditText(this);url.setSingleLine(true);
  url.setHint("https://example.com/latest.json");
  url.setText(getPreferences(0).getString("updateFeedUrl",""));
  LinearLayout wrapper=new LinearLayout(this);wrapper.setPadding(dp(16),dp(8),dp(16),dp(8));wrapper.addView(url);
  new android.app.AlertDialog.Builder(this)
   .setTitle(t("Updatefeed instellen"))
   .setMessage(t("Vul een openbare HTTPS-link naar latest.json in. GitHub Actions in een private repository is niet anoniem toegankelijk."))
   .setView(wrapper).setPositiveButton(t("Opslaan"),(d,w)->{
    String input=url.getText().toString().trim();
    if(!input.isEmpty()&&!UpdateChecker.validHttps(input)){toast("Gebruik een HTTPS-link");return;}
    getPreferences(0).edit().putString("updateFeedUrl",input).remove("latestFeedBuild").remove("lastUpdateCheck").apply();
    draw();if(!input.isEmpty())checkUpdates(false);
   }).setNegativeButton(t("Annuleren"),null).show();
 }
 private void checkUpdates(boolean silent){
  UpdateChecker.check(this,(data,error)->{
   if(isFinishing()||isDestroyed())return;
   if(error!=null){
    if(!silent)new android.app.AlertDialog.Builder(this).setTitle(t("Updatecontrole"))
     .setMessage(error+"\nControleer of GitHub de nieuwste APK als Release heeft gepubliceerd.")
     .setPositiveButton(t("OK"),null).show();
    return;
   }
   long versionCode=data.optLong("versionCode",0);
   long build=data.optLong("build",versionCode>=1000?versionCode-1000:versionCode);
   long current=UpdateChecker.installedVersionCode(this);
   getPreferences(0).edit().putLong("latestFeedBuild",build)
    .putLong("lastUpdateCheck",System.currentTimeMillis()).apply();
   boolean newer=versionCode>current;
   if(silent){
    if(!newer||getPreferences(0).getLong("lastNotifiedVersionCode",0)>=versionCode)return;
    getPreferences(0).edit().putLong("lastNotifiedVersionCode",versionCode).apply();
   }
   StringBuilder message=new StringBuilder();
   boolean english=I18n.english(this);
   message.append(english?"Installed build: #":"Geïnstalleerde build: #")
    .append(current>=1000?current-1000:current).append('\n');
   message.append(english?"Latest build: #":"Nieuwste build: #").append(build).append("\n\n");
   message.append(newer?(english?"Update available":"Nieuwe update beschikbaar"):
    (english?"You are up to date":"Je hebt de nieuwste versie"));
   if(newer){
    String changes=cleanReleaseNotes(data.optString("notes",""));
    if(!changes.isEmpty())message.append("\n\n").append(english?"What's new:":"Wat is er nieuw:").append("\n").append(changes);
   }
   android.app.AlertDialog.Builder dialog=new android.app.AlertDialog.Builder(this)
    .setTitle(english?"App updates":"App-updates")
    .setMessage(message.toString()).setNegativeButton(t("Later"),null);
   if(newer)dialog.setPositiveButton(t("Update installeren"),(d,w)->{
    String apk=data.optString("apkUrl","");
    if(UpdateChecker.validHttps(apk))downloadUpdate(apk,build,data.optString("sha256",""));
   });
   dialog.show();
   if(activeTab==3&&!silent)draw();
  });
 }
 /** Convert GitHub release Markdown into a short, readable Android dialog. */
 private String cleanReleaseNotes(String input){
  if(input==null||input.isEmpty())return "";
  String raw=input.replace("\\n","\n");
  String[] lines=raw.split("\n");
  StringBuilder out=new StringBuilder();
  int bullets=0;
  for(String original:lines){
   String line=original.trim();
   if(line.startsWith("### Safety")||line.startsWith("### Important")||
      line.startsWith("### Verification"))break;
   if(line.isEmpty()||line.startsWith("#"))continue;
   line=line.replace("**","").replace("`","").replaceAll("^[\\-*]\\s*","");
   if(line.startsWith("Build #")||line.startsWith("Android versionCode"))continue;
   if(line.length()>160)line=line.substring(0,157)+"…";
   if(out.length()>0)out.append('\n');
   out.append("• ").append(line);
   if(++bullets>=5)break;
  }
  return out.toString();
 }
 void showUpdateInstallError(String reason){
  runOnUiThread(()->{
   if(isFinishing()||isDestroyed())return;
   String path=getPreferences(0).getString("lastVerifiedApk","");
   long build=getPreferences(0).getLong("lastVerifiedBuild",0);
   String sha=getPreferences(0).getString("lastVerifiedHash","");
   java.io.File apk=path.isEmpty()?null:new java.io.File(path);
   boolean fallback=apk!=null&&apk.isFile()&&apk.length()>1024*1024;
   android.app.AlertDialog.Builder dialog=new android.app.AlertDialog.Builder(this)
    .setTitle(t("Update niet geïnstalleerd"))
    .setMessage(reason+"\n\nDe bestaande installatie is niet gewijzigd. Verwijder de app niet.")
    .setPositiveButton(t("OK"),null);
   if(fallback)dialog.setNeutralButton(
    I18n.english(this)?"Try Android installer":"Via Android installeren",
    (d,w)->UpdateInstaller.installWithAndroidInstaller(this,apk,build,sha));
   dialog.show();
  });
 }
 private void downloadUpdate(String apkUrl,long build,String sha256){
  try{
   if(!UpdateChecker.validHttps(apkUrl))throw new IllegalArgumentException("Ongeldige APK-link");
   android.app.DownloadManager manager=(android.app.DownloadManager)getSystemService(DOWNLOAD_SERVICE);
   if(manager==null)throw new IllegalStateException("Downloadmanager niet beschikbaar");
   if(getPreferences(0).getLong("activeUpdateDownloadId",-1)>0){
    processPendingUpdateDownload();
    if(getPreferences(0).getLong("activeUpdateDownloadId",-1)>0){
     toast("Er is al een update-download bezig. Deze wordt automatisch opgepakt.");
     return;
    }
   }
   java.io.File folder=getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS);
   if(folder==null)throw new java.io.IOException("Downloadmap is niet beschikbaar");
   String filename="ClashTimerHub-"+build+".apk";
   java.io.File file=new java.io.File(folder,filename);
   if(file.exists()&&!file.delete())throw new java.io.IOException("Oude APK kon niet worden vervangen");
   getPreferences(0).edit().remove("lastVerifiedApk").remove("lastVerifiedBuild")
    .remove("lastVerifiedHash").apply();
   android.app.DownloadManager.Request request=new android.app.DownloadManager.Request(android.net.Uri.parse(apkUrl));
   request.setTitle("ClashTimerHub build #"+build);
   request.setDescription("APK veilig downloaden en controleren");
   request.setMimeType("application/vnd.android.package-archive");
   request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
   request.setDestinationInExternalFilesDir(this,android.os.Environment.DIRECTORY_DOWNLOADS,filename);
   long id=manager.enqueue(request);
   getPreferences(0).edit().putLong("activeUpdateDownloadId",id)
    .putLong("activeUpdateDownloadBuild",build)
    .putString("activeUpdateDownloadHash",sha256==null?"":sha256)
    .putString("activeUpdateDownloadPath",file.getAbsolutePath()).apply();
   startListeningForUpdateDownload();
   toast("APK wordt gedownload. Bij terugkeer naar de app wordt de installatie hervat.");
  }catch(Exception e){showUpdateInstallError("Download starten mislukt: "+e.getMessage());}
 }
 private void showUpdateDiagnostics(){
  long installed=UpdateChecker.installedVersionCode(this);
  long latest=getPreferences(0).getLong("latestFeedBuild",0);
  long pending=getPreferences(0).getLong("activeUpdateDownloadId",-1);
  String lastError=getPreferences(0).getString("lastInstallError","");
  String verified=getPreferences(0).getString("lastVerifiedApk","");
  boolean permitted=getPackageManager().canRequestPackageInstalls();
  String text="Geïnstalleerde build: "+(installed>=1000?"#"+(installed-1000):String.valueOf(installed))+
   "\nNieuwste gepubliceerde build: "+(latest>0?"#"+latest:"niet gecontroleerd")+
   "\nAPK-installatietoestemming: "+(permitted?"toegestaan":"ontbreekt")+
   "\nDownload: "+(pending>0?"bezig (#"+pending+")":"geen actieve download")+
   "\nGeverifieerde APK: "+(!verified.isEmpty()&&new java.io.File(verified).isFile()?"beschikbaar":"geen")+
   "\nLaatste fout: "+(lastError.isEmpty()?"geen opgeslagen fout":lastError);
  android.app.AlertDialog.Builder dialog=new android.app.AlertDialog.Builder(this)
   .setTitle("APK update-diagnose").setMessage(text).setPositiveButton("Sluiten",null);
  if(!verified.isEmpty()&&new java.io.File(verified).isFile()){
   java.io.File file=new java.io.File(verified);
   long build=getPreferences(0).getLong("lastVerifiedBuild",0);
   String hash=getPreferences(0).getString("lastVerifiedHash","");
   dialog.setNeutralButton("Via Android installeren",
    (d,w)->UpdateInstaller.installWithAndroidInstaller(this,file,build,hash));
  }
  dialog.show();
 }
 private void openUpdates(){
  new android.app.AlertDialog.Builder(this).setTitle(t("Nieuwe versie installeren"))
   .setMessage(t("Open de nieuwste GitHub Release. De APK is direct te downloaden zonder ZIP-bestand. Verwijder je bestaande app niet."))
   .setPositiveButton(t("Open GitHub"),(d,w)->startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://github.com/ashot978/ClashTimerHub/releases/latest"))))
   .setNegativeButton(t("Annuleren"),null).show();
 }
 private void showWidgetSettings(){
  android.appwidget.AppWidgetManager manager=android.appwidget.AppWidgetManager.getInstance(this);
  int[] ids=WidgetV7.allIds(this);
  if(ids.length==0){
   new android.app.AlertDialog.Builder(this).setTitle(t("Widget instellen"))
    .setMessage(t("Voeg eerst de Clash Timer Hub-widget toe via je Samsung-startscherm. Tijdens het toevoegen kun je meteen 'Alle accounts tonen' kiezen."))
    .setPositiveButton(t("Begrepen"),null).show();
   return;
  }
  if(ids.length==1){openWidgetSettings(ids[0]);return;}
  String[] options=new String[ids.length];
  android.content.SharedPreferences widgetPreferences=getSharedPreferences("WidgetPreferences",MODE_PRIVATE);
  for(int i=0;i<ids.length;i++){
   int widgetId=ids[i],kind=WidgetV7.kind(this,widgetId);
   String type=kind==WidgetV7.SYNC?"Sync":
    kind==WidgetV7.MINI?"Mini":kind==WidgetV7.SINGLE?"Account":"Multi-account";
   String account=widgetPreferences.getString("account_"+widgetId,"");
   String details=kind==WidgetV7.SYNC?"Floating Sync":
    kind==WidgetV7.MULTI?(widgetPreferences.getBoolean("all_"+widgetId,true)?
     "Alle accounts":"Accountselectie"):
     (account.isEmpty()?"Niet ingesteld":AppData.displayName(this,account));
   options[i]=type+" — "+details+"  (#"+widgetId+")";
  }
  new android.app.AlertDialog.Builder(this).setTitle(t("Welke widget wil je instellen?"))
   .setItems(options,(dialog,index)->openWidgetSettings(ids[index]))
   .setNegativeButton(t("Annuleren"),null).show();
 }
 private void openWidgetSettings(int id){
  Intent intent=new Intent(this,WidgetConfigActivity.class);
  intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,id);
  startActivity(intent);
 }
 private void renderSelectedDetail(){
  if(detailPanel==null)return;
  detailPanel.removeAllViews();
  if(accounts.isEmpty()){detailPanel.addView(label("Synchroniseer een account met de zwevende knop.",14));return;}
  String ac=selectedAccount;
  Button back=new Button(this);back.setAllCaps(false);back.setText(t("‹  Terug naar alle accounts"));back.setTextSize(13);
  back.setOnClickListener(v->{selectedAccount="";activeTab=0;draw();});detailPanel.addView(back);
  TextView title=label("⚔  "+displayAccount(ac),20);title.setTextColor(0xffffd783);
  title.setTypeface(null,android.graphics.Typeface.BOLD);detailPanel.addView(title);
  int ready=completedCount(ac,System.currentTimeMillis());
  if(ready>0){
   TextView done=label("✓  "+ready+" builders klaar",14);done.setTextColor(0xff8be3ad);detailPanel.addView(done);
   Button checked=new Button(this);checked.setText(t("Markeer als gecontroleerd"));checked.setTextSize(12);checked.setAllCaps(false);
   checked.setOnClickListener(v->{acknowledgeAccount(ac);renderSelectedDetail();});detailPanel.addView(checked);
  }
  long last=getPreferences(0).getLong("lastSyncAt_"+ac,0);
  if(last>0){TextView synced=label("↻ Gesynchroniseerd: "+finishAt(last),11);synced.setTextColor(0xffbba7d1);detailPanel.addView(synced);}
  if(ac.startsWith("#")){Button link=new Button(this);link.setAllCaps(false);link.setText(t("Koppel deze player tag aan bestaand account"));link.setOnClickListener(v->linkImportedAccount(ac));detailPanel.addView(link);}
  showAccountHelpers(detailPanel,ac);
  CheckBox alchemist=new CheckBox(this);
  alchemist.setText(I18n.english(this)?"Alchemist available":"Alchemist beschikbaar");
  alchemist.setTextColor(0xff8be3ad);
  alchemist.setChecked(HelperTracker.isAlchemistAvailable(this,ac));
  detailPanel.addView(alchemist);
  alchemist.setOnCheckedChangeListener((button,available)->{
   HelperTracker.setAlchemistAvailable(this,ac,available);
   TimerWidget.update(this);
   renderSelectedDetail();
  });
  TextView maxTitle=label("✓  MAX STATUS",14);
   maxTitle.setTextColor(0xff8be3ad);detailPanel.addView(maxTitle);
   for(String categoryName:categories){
    CheckBox box=new CheckBox(this);
    box.setText(t(categoryName)+" MAX");
    box.setTextColor(0xff8be3ad);
    box.setChecked(AppData.isMaxed(this,ac,categoryName));
    detailPanel.addView(box);
    box.setOnCheckedChangeListener((button,checked)->{
     AppData.setMaxed(this,ac,categoryName,checked);
     TimerWidget.update(this);renderDashboard();
    });
   }
   renderTimelinePreview(detailPanel);
  for(String cat:categories){
   TextView section=label(("Bouwers".equals(cat)?"⚒  ":"Laboratorium".equals(cat)?"⚗  ":"🐾  ")+cat.toUpperCase(java.util.Locale.ROOT),14);
   section.setTextColor(0xffd9b8ff);section.setTypeface(null,android.graphics.Typeface.BOLD);detailPanel.addView(section);
   ArrayList<JSONObject> list=new ArrayList<>();
   for(JSONObject o:upgrades)if(ac.equals(o.optString("account"))&&cat.equals(o.optString("category"))&&o.optLong("finish")>System.currentTimeMillis())list.add(o);
   Collections.sort(list,(a,b)->Long.compare(a.optLong("finish"),b.optLong("finish")));
   if(list.isEmpty()){TextView none=label("Geen actieve upgrades",11);none.setTextColor(0xffa99bb8);detailPanel.addView(none);}
   else for(JSONObject o:list)addTimerRow(detailPanel,o,true);
  }
 }
 private void showAccountHelpers(LinearLayout parent,String account){
  HelperTracker.Snapshot snap=HelperTracker.forAccount(this,account);
  TextView heading=label(I18n.english(this)?"🤝  HELPERS":"🤝  HELPERS",14);
  heading.setTextColor(0xff8be3ad);
  heading.setTypeface(null,android.graphics.Typeface.BOLD);
  parent.addView(heading);
  if(snap==null||snap.items.isEmpty()){
   TextView empty=label(I18n.english(this)?
    "No helper information yet. Synchronize a full Clash export.":
    "Nog geen helpergegevens. Synchroniseer een volledige Clash-export.",12);
   empty.setTextColor(0xffa99bb8);parent.addView(empty);
   return;
  }
  long now=System.currentTimeMillis();
  boolean en=I18n.english(this);
  for(HelperTracker.Helper helper:snap.items){
   String level=helper.level>0?(en?" · Level ":" · Niveau ")+helper.level:"";
   String remaining=helper.readyAt<0?(en?"Cooldown unknown":"Cooldown onbekend"):
      HelperTracker.remaining(helper.readyAt,now,en);
   String work=helper.workingUntil>now?
      (en?" · Working ":" · Bezig ")+HelperTracker.remaining(helper.workingUntil,now,en):"";
   TextView label=label(helper.name(en)+level+"\n"+remaining+work,12);
   label.setPadding(dp(8),dp(5),dp(8),dp(5));
   label.setTextColor(helper.readyAt>=0&&helper.readyAt<=now?0xff8be3ad:0xffd4c5e4);
   parent.addView(label);
  }
  String updated=new java.text.SimpleDateFormat("dd-MM HH:mm",java.util.Locale.getDefault())
   .format(new java.util.Date(snap.importedAt));
  TextView last=label((en?"Last export: ":"Laatste export: ")+updated,11);
  last.setTextColor(0xffa99bb8);parent.addView(last);
 }
 private void renderTimelinePreview(LinearLayout parent){
  TextView header=label("◷  TIJDLIJN · ALLE ACCOUNTS",15);header.setTextColor(0xffffd783);parent.addView(header);
  ArrayList<JSONObject> pending=new ArrayList<>();
  long now=System.currentTimeMillis();
  for(JSONObject o:upgrades)if(o.optLong("finish")>now)pending.add(o);
  Collections.sort(pending,(x,y)->Long.compare(x.optLong("finish"),y.optLong("finish")));
  int shown=Math.min(6,pending.size());
  if(shown==0)parent.addView(label("Geen aankomende upgrades",12));
  for(int i=0;i<shown;i++){
   JSONObject o=pending.get(i);
   long finish=o.optLong("finish");
   String name=cleanName(o.optString("name")),who=o.optString("account");
   TextView row=label(finishAt(finish)+"  ·  "+displayAccount(who)+"\n"+name,12);
   row.setTextColor(0xfff1debd);row.setBackground(clashSurface(0xff2e2340,0xff53416a,10));
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(2),0,dp(2));parent.addView(row,lp);
   row.setOnClickListener(v->{selectedAccount=who;activeTab=0;draw();});
  }
  Button full=new Button(this);full.setText(t("Bekijk volledige tijdlijn ›"));full.setTextSize(12);full.setAllCaps(false);
  full.setOnClickListener(v->{activeTab=1;draw();});parent.addView(full);
 }
 private void showAccountScreen(){
  screenContent.addView(label("ACCOUNTS · ALLE UPGRADES",19));
  if(accounts.isEmpty()){screenContent.addView(label("Nog geen accounts. Gebruik de zwevende plakknop of importeer JSON.",14));return;}
  if(!accounts.contains(selectedAccount))selectedAccount=accounts.get(0);
  LinearLayout chips=new LinearLayout(this);chips.setOrientation(1);screenContent.addView(chips);
  for(String ac:accounts){
   Button choice=new Button(this);choice.setText((ac.equals(selectedAccount)?"● ":"○ ")+displayAccount(ac));choice.setAllCaps(false);
   choice.setOnClickListener(v->{selectedAccount=ac;draw();});chips.addView(choice);
  }
  if(selectedAccount.startsWith("#")){
   Button link=new Button(this);link.setText(t("Koppel deze player tag aan bestaand account"));link.setAllCaps(false);
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
  new android.app.AlertDialog.Builder(this).setTitle(t("Koppel "+source+" aan account"))
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
     AppData.mergeMaxedAccount(this,source,target);
     HelperTracker.mergeAlchemistAccount(this,source,target);
     selectedAccount=target;persist();draw();toast(source+" gekoppeld aan "+target);
    }catch(Exception e){toast("Koppelen mislukt: "+e.getMessage());}
   }).setNegativeButton(t("Annuleren"),null).show();
 }
 private void showTimeline(){
  screenContent.addView(label("TIJDLIJN · ALLE ACCOUNTS",19));
  final String[] filters={"Alles","Bouwers","Laboratorium","Pets"};
  LinearLayout filterColumn=new LinearLayout(this);filterColumn.setOrientation(1);screenContent.addView(filterColumn);
  boolean narrow=getResources().getConfiguration().screenWidthDp<420;
  LinearLayout filterRow=null;
  for(int index=0;index<filters.length;index++){
   if(index%(narrow?2:4)==0){filterRow=new LinearLayout(this);filterRow.setOrientation(0);filterColumn.addView(filterRow,new LinearLayout.LayoutParams(-1,dp(48)));}
   String filter=filters[index];
   Button b=new Button(this);b.setText(t(filter));b.setTextSize(10);b.setAllCaps(false);
   b.setOnClickListener(v->{timelineFilter=filter;draw();});
   b.setTextColor(filter.equals(timelineFilter)?0xffffd88a:0xffd5c9e2);
   filterRow.addView(b,new LinearLayout.LayoutParams(0,dp(44),1));
  }
  ArrayList<JSONObject> list=new ArrayList<>();
  for(JSONObject o:upgrades)if(o.optLong("finish")>System.currentTimeMillis()&&("Alles".equals(timelineFilter)||timelineFilter.equals(o.optString("category"))))list.add(o);
  Collections.sort(list,(x,y)->Long.compare(x.optLong("finish"),y.optLong("finish")));
  if(list.isEmpty())screenContent.addView(label("Geen actieve upgrades voor dit filter.",14));
  for(JSONObject o:list)addTimerRow(screenContent,o,false);
 }
 private void refreshDashboardClocks(){
  long now=System.currentTimeMillis();
  boolean finished=false;
  for(int i=0;i<dashboardClocks.size();i++){
   long end=dashboardFinishes.get(i);
   TextView view=dashboardClocks.get(i);
   if(end<=now){finished=true;break;}
   view.setText(format(Math.max(0,(end-now+999)/1000)));
  }
  if(finished){renderDashboard();if(detailPanel!=null)renderSelectedDetail();}
 }
 private void refreshVisibleTimers(){
  if(screenContent==null)return;
  boolean expired=refreshTimeTree(screenContent,System.currentTimeMillis());
  if(expired){
   if(activeTab==0&&detailPanel!=null)renderSelectedDetail();
   else if(activeTab!=0)draw();
  }
 }
 private boolean refreshTimeTree(View view,long now){
  boolean expired=false;
  if(view instanceof LinearLayout&&view.getTag() instanceof Long){
   LinearLayout group=(LinearLayout)view;
   long finish=(Long)view.getTag();
   if(group.getChildCount()>1&&group.getChildAt(1) instanceof TextView)
    ((TextView)group.getChildAt(1)).setText(format(Math.max(0,(finish-now+999)/1000))+"  ·  "+finishAt(finish));
   if(finish<=now)expired=true;
  }
  if(view instanceof android.view.ViewGroup){
   android.view.ViewGroup group=(android.view.ViewGroup)view;
   for(int i=0;i<group.getChildCount();i++)expired=refreshTimeTree(group.getChildAt(i),now)||expired;
  }
  return expired;
 }
 private String timelineFilter="Alles";
 private String finishAt(long timestamp){
  java.util.Locale displayLocale=I18n.english(this)?java.util.Locale.UK:java.util.Locale.forLanguageTag("nl-NL");
  java.text.SimpleDateFormat fmt=new java.text.SimpleDateFormat("HH:mm",displayLocale);
  java.util.Calendar now=java.util.Calendar.getInstance(),finish=java.util.Calendar.getInstance();finish.setTimeInMillis(timestamp);
  boolean today=now.get(java.util.Calendar.YEAR)==finish.get(java.util.Calendar.YEAR)&&now.get(java.util.Calendar.DAY_OF_YEAR)==finish.get(java.util.Calendar.DAY_OF_YEAR);
  now.add(java.util.Calendar.DAY_OF_YEAR,1);
  boolean tomorrow=now.get(java.util.Calendar.YEAR)==finish.get(java.util.Calendar.YEAR)&&now.get(java.util.Calendar.DAY_OF_YEAR)==finish.get(java.util.Calendar.DAY_OF_YEAR);
  if(today)return t("Vandaag om ")+fmt.format(new java.util.Date(timestamp));
  if(tomorrow)return t("Morgen om ")+fmt.format(new java.util.Date(timestamp));
  return new java.text.SimpleDateFormat("EEE d MMM · HH:mm",displayLocale).format(new java.util.Date(timestamp));
 }
 private void addTimerRow(LinearLayout parent,JSONObject o,boolean editable){
  long finish=o.optLong("finish"),remain=Math.max(0,(finish-System.currentTimeMillis()+999)/1000);
  LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(10),dp(5),dp(10),dp(5));card.setBackground(shape(remain<=1800?0xff46351f:0xff30243e,12));card.setTag(finish);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(dp(2),dp(3),dp(2),dp(3));parent.addView(card,lp);
  card.addView(label((editable?"":displayAccount(o.optString("account"))+" · ")+cleanName(o.optString("name")),14));
  TextView time=label(format(remain)+"  ·  "+finishAt(finish),13);time.setTextColor(0xffffd88a);card.addView(time);
  if(editable)card.setOnClickListener(v->editTimer(o));
  else card.setOnClickListener(v->{selectedAccount=o.optString("account");activeTab=0;draw();});
 }
 private void toggleFloatingSync(){
  android.content.SharedPreferences prefs=getPreferences(0);
  boolean active=prefs.getBoolean("floatingEnabled",false);
  Intent service=new Intent(this,FloatingSyncService.class);
  if(active){
   stopService(service);
   prefs.edit().putBoolean("floatingEnabled",false).apply();
   TimerWidget.update(this);
   toast("Zwevende plakknop uitgeschakeld");
   return;
  }
  if(!android.provider.Settings.canDrawOverlays(this)){
   new android.app.AlertDialog.Builder(this)
    .setTitle(t("Zwevende plakknop"))
    .setMessage(t("Geef Clash Timer Hub toestemming om over andere apps te worden weergegeven. Kom daarna terug en tik nogmaals op de knop."))
    .setPositiveButton(t("Toestemming geven"),(d,w)->{
     Intent i=new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,android.net.Uri.parse("package:"+getPackageName()));
     startActivity(i);
    }).setNegativeButton(t("Annuleren"),null).show();
   return;
  }
  try{
   if(android.os.Build.VERSION.SDK_INT>=26)startForegroundService(service);
   else startService(service);
   prefs.edit().putBoolean("floatingEnabled",true).apply();
   TimerWidget.update(this);
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
  CheckBox enabled=new CheckBox(this);enabled.setText(t("Meldingen inschakelen"));enabled.setChecked(prefs.getBoolean("notifyEnabled",true));panel.addView(enabled);
  CheckBox completed=new CheckBox(this);completed.setText(t("Melding zodra upgrade klaar is"));completed.setChecked(prefs.getBoolean("notifyOnFinish",true));panel.addView(completed);
  panel.addView(label("Waarschuw mij zoveel minuten vooraf (0 = uit)",13));
  EditText lead=new EditText(this);lead.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);lead.setSingleLine(true);
  lead.setText(String.valueOf(prefs.getInt("notifyLeadMinutes",15)));lead.setSelectAllOnFocus(true);panel.addView(lead);
  panel.addView(label("Bijvoorbeeld 5, 10, 30, 45 of 120 minuten. Maximaal 10080 (7 dagen).",12));
  if(android.os.Build.VERSION.SDK_INT>=31){
   android.app.AlarmManager alarm=(android.app.AlarmManager)getSystemService(ALARM_SERVICE);
   if(alarm!=null&&!alarm.canScheduleExactAlarms()){
    Button precise=new Button(this);precise.setText(t("Sta exacte alarmen toe"));
    precise.setOnClickListener(v->{
     try{startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:"+getPackageName())));}
     catch(Exception e){toast("Open de alarmtoestemming via Android-instellingen");}
    });
    panel.addView(precise);
   }
  }
  new android.app.AlertDialog.Builder(this).setTitle(t("Meldingen instellen")).setView(panel)
   .setPositiveButton(t("Opslaan"),(d,w)->{
    int minutes=15;
    try{minutes=Integer.parseInt(lead.getText().toString().trim());}catch(Exception ignored){}
    minutes=Math.max(0,Math.min(10080,minutes));
    prefs.edit().putBoolean("notifyEnabled",enabled.isChecked())
     .putBoolean("notifyOnFinish",completed.isChecked()).putInt("notifyLeadMinutes",minutes).apply();
    TimerWidget.schedule(this);toast("Meldingen opgeslagen");
   })
   .setNeutralButton(t("Android-instellingen"),(d,w)->{
    Intent i=new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
    i.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName());startActivity(i);
   }).setNegativeButton(t("Annuleren"),null).show();
 }
 private JSONObject backupPayload()throws JSONException{
  return DriveBackupManager.buildPayload(this);
 }
 private void copyBackup(){
  try{
   JSONObject data=backupPayload();
   android.content.ClipboardManager manager=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
   if(manager==null){toast("Klembord niet beschikbaar");return;}
   manager.setPrimaryClip(android.content.ClipData.newPlainText("Clash Timer Hub back-up",data.toString()));
   toast("Back-up van "+accounts.size()+" accounts en instellingen gekopieerd");
  }catch(Exception e){toast("Back-up kopiëren mislukt");}
 }
 private void backup(){
  try{
   JSONObject data=backupPayload();
   Intent send=new Intent(Intent.ACTION_SEND);send.setType("application/json");
   send.putExtra(Intent.EXTRA_TEXT,data.toString(2));
   startActivity(Intent.createChooser(send,"Deel Clash Timer Hub back-up"));
  }catch(Exception e){toast("Back-up maken mislukt");}
 }
 /** Date-stamped, portable filename for every exported JSON backup. */
 private String backupFilename(){
  return "ClashTimerHub-backup-"+
   new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS",java.util.Locale.ROOT)
    .format(new java.util.Date())+".json";
 }
 private void shareBackupToDrive(){
  try{
   java.io.File folder=new java.io.File(getCacheDir(),"backup_exports");
   if(!folder.exists()&&!folder.mkdirs())throw new java.io.IOException("Kan back-upmap niet maken");
   java.io.File file=new java.io.File(folder,backupFilename());
   byte[] bytes=backupPayload().toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8);
   if(bytes.length>2_000_000)throw new java.io.IOException("Back-up te groot (max. 2 MB)");
   try(java.io.FileOutputStream out=new java.io.FileOutputStream(file,false)){out.write(bytes);out.getFD().sync();}
   android.net.Uri uri=androidx.core.content.FileProvider.getUriForFile(
    this,getPackageName()+".backups",file);
   Intent share=new Intent(Intent.ACTION_SEND);
   share.setType("application/json");
   share.putExtra(Intent.EXTRA_STREAM,uri);
   share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
   share.setClipData(android.content.ClipData.newUri(getContentResolver(),file.getName(),uri));
   startActivity(Intent.createChooser(share,"Kies Google Drive om je back-up op te slaan"));
  }catch(Exception e){toast("Back-up delen mislukt: "+e.getMessage());}
 }
 private void saveBackupAsFile(){
  Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
  intent.addCategory(Intent.CATEGORY_OPENABLE);
  intent.setType("application/json");
  intent.putExtra(Intent.EXTRA_TITLE,backupFilename());
  try{startActivityForResult(intent,BACKUP_SAVE);}
  catch(Exception e){toast("Kan bestand opslaan niet openen: "+e.getMessage());}
 }
 private void openBackupFile(){
  Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
  intent.addCategory(Intent.CATEGORY_OPENABLE);
  intent.setType("*/*");
  intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
  try{startActivityForResult(intent,RESTORE);}
  catch(Exception e){toast("Kan bestanden niet openen: "+e.getMessage());}
 }
 private void writeBackupToFile(android.net.Uri uri){
  try{
   byte[] bytes=backupPayload().toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8);
   if(bytes.length>2_000_000)throw new java.io.IOException("Back-up te groot (max. 2 MB)");
   try(java.io.OutputStream out=getContentResolver().openOutputStream(uri,"wt")){
    if(out==null)throw new java.io.IOException("Geen toegang tot gekozen bestand");
    out.write(bytes);out.flush();
   }
   toast("Back-upbestand opgeslagen");
  }catch(Exception e){toast("Back-up opslaan mislukt: "+e.getMessage());}
 }
 private void restorePreferences(JSONObject preferences){
  if(preferences==null)return; // Old backups retain the current device's settings.
  android.content.SharedPreferences.Editor edit=getPreferences(0).edit();
  String language=preferences.optString("language",I18n.code(this));
  if("en".equals(language)||"nl".equals(language))edit.putString("language",language);
  int start=preferences.optInt("timerStart",70),end=preferences.optInt("timerEnd",94);
  if(start>=0&&start<=96&&end>=start+4&&end<=100){
   edit.putInt("timerStart",start).putInt("timerEnd",end);
  }
  edit.putBoolean("notifyEnabled",preferences.optBoolean("notifyEnabled",true));
  edit.putBoolean("notifyOnFinish",preferences.optBoolean("notifyOnFinish",true));
  edit.putInt("notifyLeadMinutes",Math.max(0,Math.min(10080,preferences.optInt("notifyLeadMinutes",15))));
  edit.apply();
 }
 private void restoreWidgetSettings(JSONArray widgets){
  if(widgets==null)return;
  android.content.SharedPreferences.Editor edit=getSharedPreferences("WidgetPreferences",MODE_PRIVATE).edit();
  HashMap<Integer,Integer> sequence=new HashMap<>();
  for(int widgetId:WidgetV7.allIds(this)){
   int type=WidgetV7.kind(this,widgetId);
   int index=sequence.getOrDefault(type,0);sequence.put(type,index+1);
   JSONObject entry=null;
   for(int i=0;i<widgets.length();i++){
    JSONObject candidate=widgets.optJSONObject(i);
    if(candidate!=null&&candidate.optInt("type",-1)==type&&candidate.optInt("index",-1)==index){entry=candidate;break;}
   }
   if(entry==null)continue;
   edit.putBoolean("all_"+widgetId,entry.optBoolean("all",true));
   edit.putString("selected_"+widgetId,entry.optJSONArray("selected")==null?"[]":entry.optJSONArray("selected").toString());
   edit.putString("order_"+widgetId,entry.optJSONArray("order")==null?"[]":entry.optJSONArray("order").toString());
   edit.putInt("sort_"+widgetId,Math.max(0,Math.min(2,entry.optInt("sort",0))));
   edit.putInt("size_"+widgetId,Math.max(0,Math.min(2,entry.optInt("size",1))));
   edit.putInt("bg_"+widgetId,Math.max(0,Math.min(2,entry.optInt("bg",0))));
   edit.putBoolean("lab_"+widgetId,entry.optBoolean("lab",true));
   edit.putBoolean("pet_"+widgetId,entry.optBoolean("pet",true));
   edit.putBoolean("helpers_"+widgetId,entry.optBoolean("helpers",true));
   edit.putString("account_"+widgetId,entry.optString("account",""));
  }
  edit.apply();
  TimerWidget.update(this);
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
   new android.app.AlertDialog.Builder(this).setTitle(t("Back-up herstellen?"))
    .setMessage(t("Dit vervangt alle bestaande accounts en timers door "+names.size()+" accounts en "+timers.size()+" timers."))
    .setPositiveButton(t("Herstellen"),(d,w)->{
     accounts.clear();accounts.addAll(names);upgrades.clear();upgrades.addAll(timers);
     getPreferences(0).edit()
       .putString("playerTags",data.optJSONObject("playerTags")==null?"{}":data.optJSONObject("playerTags").toString())
       .putString("accountNames",data.optJSONObject("accountNames")==null?"{}":data.optJSONObject("accountNames").toString())
       .putString("maxedCategories",data.optJSONObject("maxedCategories")==null?"{}":data.optJSONObject("maxedCategories").toString())
       .putString(HelperTracker.KEY,data.optJSONObject("helpersByAccount")==null?"{}":data.optJSONObject("helpersByAccount").toString())
       .putString(HelperTracker.ALCHEMIST_AVAILABLE_KEY,
        data.optJSONObject("alchemistAvailableByAccount")==null?"{}":
         data.optJSONObject("alchemistAvailableByAccount").toString()).apply();
     restorePreferences(data.optJSONObject("preferences"));
     restoreWidgetSettings(data.optJSONArray("widgets"));
     persist();draw();toast("Back-up hersteld, inclusief beschikbare instellingen");
    }).setNegativeButton(t("Annuleren"),null).show();
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
   public void onProgressChanged(SeekBar bar,int progress,boolean user){vals[0]=progress;a.setText(t("Timer begint: "+progress+"%"));}
   public void onStartTrackingTouch(SeekBar bar){}public void onStopTrackingTouch(SeekBar bar){}
  });
  end.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){
   public void onProgressChanged(SeekBar bar,int progress,boolean user){vals[1]=progress;b.setText(t("Timer eindigt: "+progress+"%"));}
   public void onStartTrackingTouch(SeekBar bar){}public void onStopTrackingTouch(SeekBar bar){}
  });
  form.addView(a);form.addView(start);form.addView(b);form.addView(end);
  new android.app.AlertDialog.Builder(this).setTitle(t("Timergebied kalibreren")).setView(form)
   .setPositiveButton(t("Opslaan"),(d,w)->{
    if(vals[1]<=vals[0]+3){toast("Eindpositie moet minimaal 4% na de beginpositie liggen");return;}
    getPreferences(0).edit().putInt("timerStart",vals[0]).putInt("timerEnd",vals[1]).apply();
    toast("Scannerkalibratie opgeslagen");
   }).setNegativeButton(t("Annuleren"),null).show();
 }
 private void renderDashboard(){
  if(dashboard==null)return;
  dashboard.removeAllViews();dashboardClocks.clear();dashboardFinishes.clear();
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
  String last=getPreferences(0).getString("lastSyncSummary","");
  if(!last.isEmpty()){
   TextView recent=label("↻  "+last,11);recent.setTextColor(0xffbba7d1);dashboard.addView(recent);
  }
  if(foldExpanded){
   TextView compact=label(accounts.size()+" accounts  ·  "+active+" bezig  ·  "+totalReady+" klaar",12);
   compact.setTextColor(0xffffd783);compact.setBackground(clashSurface(0xff292139,0xff644b76,12));
   dashboard.addView(compact);
  }else{
   LinearLayout stats=new LinearLayout(this);
   stats.setOrientation(LinearLayout.HORIZONTAL);
   stats.setBaselineAligned(false);
   stats.setGravity(android.view.Gravity.TOP);
   dashboard.addView(stats,new LinearLayout.LayoutParams(-1,dp(76)));
   String[] metrics={accounts.size()+"\\nACCOUNTS",active+"\\nBOUWERS BEZIG",totalReady+"\\nKLAAR"};
   for(String metric:metrics){
    TextView view=label(metric.replace("\\n","\n"),13);view.setGravity(android.view.Gravity.CENTER);
    view.setTextColor(0xffffd88a);view.setBackground(clashSurface(0xff292139,0xff644b76,12));
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(70),1);lp.setMargins(dp(2),dp(3),dp(2),dp(3));stats.addView(view,lp);
   }
  }
  LinearLayout gridRow=null;int index=0;
  int columns=foldExpanded&&!selectedAccount.isEmpty()?1:2;
  for(String ac:ordered){
   if(index%columns==0){
    gridRow=new LinearLayout(this);gridRow.setOrientation(LinearLayout.HORIZONTAL);
    gridRow.setGravity(android.view.Gravity.TOP);dashboard.addView(gridRow);
   }
   index++;
   long soonest=nextBuilder(ac,now);
   JSONObject first=null;int busy=0;
   for(JSONObject o:upgrades)if(ac.equals(o.optString("account"))&&"Bouwers".equals(o.optString("category"))&&o.optLong("finish")>now){
    busy++;if(o.optLong("finish")==soonest)first=o;
   }
   int completed=completedCount(ac,now);
   boolean ready=completed>0,urgent=!ready&&soonest!=Long.MAX_VALUE&&soonest-now<=1800000L;
   int color=ready?0xff213a30:urgent?0xff443420:0xff30233e;
   LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
   card.setPadding(dp(8),dp(6),dp(8),dp(6));
   card.setBackground(clashSurface(color,ready?0xff4c9b77:urgent?0xffc6a266:0xff57416b,14));
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(ready?220:190),1f);
   lp.setMargins(dp(3),dp(4),dp(3),dp(4));gridRow.addView(card,lp);

   TextView name=label((ac.equals(selectedAccount)&&foldExpanded?"◆  ":"⚔  ")+displayAccount(ac),15);
   name.setTypeface(null,android.graphics.Typeface.BOLD);
   name.setGravity(android.view.Gravity.CENTER_VERTICAL);name.setSingleLine(true);
   name.setEllipsize(android.text.TextUtils.TruncateAt.END);name.setPadding(dp(6),0,dp(3),0);
   card.addView(name,new LinearLayout.LayoutParams(-1,dp(34)));

   String statusText=ready?(I18n.english(this)?completed+" BUILDERS READY":completed+" BOUWERS KLAAR"):"";
   TextView status=label(statusText,10);status.setTextColor(0xff8be3ad);
   status.setSingleLine(true);status.setEllipsize(android.text.TextUtils.TruncateAt.END);
   status.setGravity(android.view.Gravity.CENTER_VERTICAL);status.setPadding(dp(6),0,0,0);
   card.addView(status,new LinearLayout.LayoutParams(-1,dp(18)));

   TextView timer=label(soonest==Long.MAX_VALUE?"Geen actieve builder":format(Math.max(0,(soonest-now+999)/1000)),18);
   timer.setTextColor(ready?0xff8be3ad:0xffffd783);
   timer.setTypeface(null,android.graphics.Typeface.BOLD);timer.setGravity(android.view.Gravity.CENTER_VERTICAL);
   timer.setSingleLine(true);timer.setEllipsize(android.text.TextUtils.TruncateAt.END);
   timer.setPadding(dp(6),0,dp(3),0);
   card.addView(timer,new LinearLayout.LayoutParams(-1,dp(36)));
   if(soonest!=Long.MAX_VALUE){dashboardClocks.add(timer);dashboardFinishes.add(soonest);}

   TextView end=label(soonest==Long.MAX_VALUE?"":finishAt(soonest),12);
   end.setTextColor(0xffc9b9df);end.setSingleLine(true);
   end.setEllipsize(android.text.TextUtils.TruncateAt.END);
   end.setGravity(android.view.Gravity.CENTER_VERTICAL);end.setPadding(dp(6),0,dp(3),0);
   card.addView(end,new LinearLayout.LayoutParams(-1,dp(26)));

   String itemName=first==null?"Geen actieve builder":"⚒  "+cleanName(first.optString("name"));
   int lab=0,pets=0;
    for(JSONObject upgrade:upgrades)if(ac.equals(upgrade.optString("account"))&&upgrade.optLong("finish")>now){
     if("Laboratorium".equals(upgrade.optString("category")))lab++;
     else if("Pets".equals(upgrade.optString("category")))pets++;
    }
    String labLabel=lab>0?lab+" lab":AppData.isMaxed(this,ac,"Laboratorium")?"✓ lab MAX":"0 lab";
    String petLabel=pets>0?pets+" pets":AppData.isMaxed(this,ac,"Pets")?"✓ pets MAX":"0 pets";
    TextView detail=label(itemName+"\n"+busy+" bouwers · ⚗ "+labLabel+" · 🐾 "+petLabel,12);
   detail.setTextColor(0xffd4c5e4);detail.setMaxLines(2);
   detail.setEllipsize(android.text.TextUtils.TruncateAt.END);
   detail.setGravity(android.view.Gravity.TOP);detail.setPadding(dp(6),dp(3),dp(3),0);
   card.addView(detail,new LinearLayout.LayoutParams(-1,dp(48)));

   if(ready){
    Button checked=new Button(this);checked.setText(t("Gecontroleerd"));
    checked.setTextSize(11);checked.setAllCaps(false);
    checked.setPadding(dp(4),0,dp(4),0);checked.setMinHeight(0);checked.setMinimumHeight(0);
    checked.setOnClickListener(v->acknowledgeAccount(ac));
    card.addView(checked,new LinearLayout.LayoutParams(-1,dp(42)));
   }
   card.setOnClickListener(v->{selectedAccount=ac.equals(selectedAccount)?"":ac;activeTab=0;draw();});
  }
 }
 private int completedCount(String ac,long now){
  long acknowledged=getPreferences(0).getLong("ack_"+ac,0L);
  int count=0;
  for(JSONObject o:upgrades){
   long finish=o.optLong("finish");
   if(ac.equals(o.optString("account"))&&"Bouwers".equals(o.optString("category"))&&finish>acknowledged&&finish<=now)count++;
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
   TextView line=label(displayAccount(ac)+"  ·  "+(earliest==null?"Geen builder bekend":cleanName(earliest.optString("name"))+"  ·  "+(soonest<=now?"Gereed":format((soonest-now+999)/1000))),16);
   line.setBackground(shape(0xff30243e,12));list.addView(line);
  }
  ScrollView scroll=new ScrollView(this);scroll.addView(list);
  new android.app.AlertDialog.Builder(this).setTitle(t("Alle accounts · Eerstvolgende builder"))
   .setView(scroll).setPositiveButton(t("Sluiten"),null).show();
 }
 private void importClipboard(){
  try{
   android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
   if(clipboard==null||!clipboard.hasPrimaryClip())throw new IllegalArgumentException("Klembord is leeg");
   android.content.ClipData clip=clipboard.getPrimaryClip();
   if(clip==null||clip.getItemCount()==0)throw new IllegalArgumentException("Geen Clash-data gevonden");
   CharSequence raw=clip.getItemAt(0).coerceToText(this);
   String result=QuickSyncImporter.importJson(this,raw==null?"":raw.toString());
   refreshExternalImport();
   toast(result);
  }catch(Exception e){toast("Synchroniseren mislukt: "+e.getMessage());}
 }
 private void importClashData(String input){
  // All import paths use the same atomic, player-tag-scoped parser as the floating sync.
  // Never fall back to a selected account if the source identity cannot be verified.
  try{
   String result=QuickSyncImporter.importJson(this,input);
   refreshExternalImport();
   toast(result);
  }catch(Exception e){
   new android.app.AlertDialog.Builder(this).setTitle(t("Synchronisatie mislukt"))
    .setMessage(t(e.getMessage()==null?"Controleer de Clash-export":e.getMessage())+
     "\\n\\nBestaande accounttimers zijn niet overschreven.")
    .setPositiveButton(t("OK"),null).show();
  }
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
   TextView line=label(displayAccount(ac)+"\n"+next+"\n"+builders+" bouwers bezig · "+lab+" lab · "+pets+" pets",15);
   line.setBackground(shape(0xff30243e,12));
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(4),0,dp(4));list.addView(line,lp);
  }
  ScrollView scroll=new ScrollView(this);scroll.addView(list);
  new android.app.AlertDialog.Builder(this).setTitle(t("Alle accounts · Details"))
   .setView(scroll).setPositiveButton(t("Sluiten"),null).show();
 }
 private void editTimer(JSONObject o){
  LinearLayout form=new LinearLayout(this);form.setOrientation(1);form.setPadding(dp(16),dp(8),dp(16),0);
  EditText n=new EditText(this),t=new EditText(this);
  n.setSingleLine(true);n.setText(cleanName(o.optString("name")));n.setHint(t("Naam"));
  t.setSingleLine(true);t.setHint(t("Bijv. 2d 5h"));long remaining=Math.max(0,(o.optLong("finish")-System.currentTimeMillis()+999)/1000);
  t.setText(format(remaining));form.addView(n);form.addView(t);
  android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle(t("Timer bewerken")).setView(form)
   .setPositiveButton(t("Opslaan"),null).setNeutralButton(t("Verwijderen"),(d,w)->confirmDelete(o))
   .setNegativeButton(t("Annuleren"),null).create();
  dialog.setOnShowListener(ev->dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   long seconds=parse(t.getText().toString());String name=n.getText().toString().trim();
   if(seconds<=0||name.isEmpty()){t.setError("Vul een geldige duur in, bijvoorbeeld 2d 5h");return;}
   try{o.put("name",name);o.put("finish",System.currentTimeMillis()+seconds*1000);}catch(Exception ignored){}
   persist();showTimers();dialog.dismiss();
  }));dialog.show();
 }
 private void confirmDelete(JSONObject o){
  new android.app.AlertDialog.Builder(this).setTitle(t("Timer verwijderen?"))
   .setMessage(o.optString("name")).setPositiveButton(t("Verwijderen"),(d,w)->{upgrades.remove(o);persist();showTimers();})
   .setNegativeButton(t("Annuleren"),null).show();
 }
 private void editAccount(){EditText e=new EditText(this);e.setSingleLine(true);new android.app.AlertDialog.Builder(this).setTitle(t("Nieuw account")).setView(e).setPositiveButton(t("Toevoegen"),(d,w)->{String s=e.getText().toString().trim();if(!s.isEmpty()&&!accounts.contains(s)){accounts.add(s);selectedAccount=s;persist();draw();}}).setNegativeButton(t("Annuleren"),null).show();}
 private void manual(){LinearLayout form=new LinearLayout(this);form.setOrientation(1);EditText n=new EditText(this),t=new EditText(this);n.setHint(t("Gebouwnaam"));t.setHint(t("Tijd, bijvoorbeeld 3h 15m"));form.addView(n);form.addView(t);new android.app.AlertDialog.Builder(this).setTitle(t("Timer toevoegen")).setView(form).setPositiveButton(t("Opslaan"),(d,w)->{long seconds=parse(t.getText().toString());if(seconds>0&&!accounts.isEmpty()){try{JSONObject o=new JSONObject();o.put("id",UUID.randomUUID().toString());o.put("account",selectedAccount.isEmpty()?accounts.get(0):selectedAccount);o.put("category","Bouwers");o.put("name",n.getText().toString());o.put("finish",System.currentTimeMillis()+seconds*1000);upgrades.add(o);persist();draw();}catch(Exception ignored){}}}).setNegativeButton(t("Annuleren"),null).show();}
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
 @Override protected void onActivityResult(int req,int result,Intent data){
  super.onActivityResult(req,result,data);
  if(req==GoogleDriveUi.REQUEST_GOOGLE_AUTH){
   if(googleDriveUi!=null)googleDriveUi.onAuthorizationResult(result,data);
   return;
  }
  if(result!=RESULT_OK||data==null)return;
  if(req==BACKUP_SAVE){if(data.getData()!=null)writeBackupToFile(data.getData());return;}
  if(req==RESTORE){try(InputStream stream=getContentResolver().openInputStream(data.getData())){java.io.ByteArrayOutputStream buffer=new java.io.ByteArrayOutputStream();byte[] bytes=new byte[8192];int n;while((n=stream.read(bytes))!=-1){buffer.write(bytes,0,n);if(buffer.size()>2000000)throw new java.io.IOException("Back-up te groot");}restoreBackup(buffer.toString("UTF-8"));}catch(Exception e){toast("Herstellen mislukt: "+e.getMessage());}return;}
  if(req==IMPORT){try(InputStream stream=getContentResolver().openInputStream(data.getData())){
   java.io.ByteArrayOutputStream buffer=new java.io.ByteArrayOutputStream();byte[] bytes=new byte[8192];int n;
   while((n=stream.read(bytes))!=-1){buffer.write(bytes,0,n);if(buffer.size()>10_000_000)throw new java.io.IOException("Export te groot");}
   importClashData(buffer.toString("UTF-8"));
  }catch(Exception e){toast("Import mislukt: "+e.getMessage());}return;}
  if(req!=PICK)return;try(InputStream stream=getContentResolver().openInputStream(data.getData())){Bitmap bmp=BitmapFactory.decodeStream(stream);if(bmp!=null)guardedScan(bmp);}catch(Exception e){toast("Foto kon niet worden geopend");}}
 /** OCR is screenshot-assisted only: reject ambiguous menus unless the user confirms. */
 private void guardedScan(Bitmap bmp){
  if(bmp.getWidth()<300||bmp.getHeight()<400){toast("Screenshotformaat niet ondersteund");return;}
  int cropHeight=Math.max(1,Math.min(bmp.getHeight(),(int)(bmp.getHeight()*.32)));
  Bitmap heading=Bitmap.createBitmap(bmp,0,0,bmp.getWidth(),cropHeight);
  com.google.mlkit.vision.text.TextRecognizer recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
  recognizer.process(InputImage.fromBitmap(heading,0))
   .addOnSuccessListener(result->{
    String title=result.getText().toLowerCase(java.util.Locale.ROOT);
    heading.recycle();recognizer.close();
    if(title.contains("upgrade")||title.contains("upgrades")||title.contains("bouw")||
       title.contains("builder")||title.contains("laboratory")||title.contains("laboratorium")||
       title.contains("pets")||title.contains("troops")||title.contains("research")){
     scan(bmp);
    }else{
     new android.app.AlertDialog.Builder(this).setTitle(t("Clash-menu niet herkend"))
      .setMessage(t("De scanner kan het upgrade-menu niet betrouwbaar herkennen. Controleer of het juiste Clash-menu zichtbaar is. Scan alleen verder als dit klopt."))
      .setPositiveButton(t("Toch scannen"),(d,w)->scan(bmp))
      .setNegativeButton(t("Annuleren"),null).show();
    }
   }).addOnFailureListener(e->{
    heading.recycle();recognizer.close();
    new android.app.AlertDialog.Builder(this).setTitle(t("Menudetectie mislukt"))
     .setMessage(t("Kan het Clash-menu niet herkennen. Probeer een scherpere screenshot."))
     .setPositiveButton(t("OK"),null).show();
   });
 }
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
  for(JSONObject o:pending){form.addView(label("Rij "+(o.optInt("row")+1),16));EditText n=new EditText(this),t=new EditText(this);n.setSingleLine(true);t.setSingleLine(true);n.setText(o.optString("name"));String recognized=o.optString("raw");t.setText(recognized);if(parse(recognized)>0)t.setCompoundDrawablesWithIntrinsicBounds(0,0,android.R.drawable.checkbox_on_background,0);else t.setError("Controleer deze tijd");n.setHint(t("Naam"));t.setHint(t("Bijv. 3h 15m"));form.addView(n);form.addView(t);names.add(n);times.add(t);}
  int free=getPreferences(0).getInt("builderAvailable",-1),total=getPreferences(0).getInt("builderTotal",-1);
  String counter=free>=0&&total>0?" · Vrije bouwers: "+free+"/"+total:" · Bouwersstatus niet herkend";
  android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("Controleer eerste upgrade"+counter).setView(scroll).setPositiveButton(t("Opslaan"),null).setNegativeButton(t("Annuleren"),null).create();
  dialog.setOnShowListener(showEvent->dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   String ac=accounts.get(account.getSelectedItemPosition()),cat=categories[category.getSelectedItemPosition()];long now=System.currentTimeMillis();
   ArrayList<JSONObject> replacement=new ArrayList<>();ArrayList<String> invalid=new ArrayList<>();
   for(int i=0;i<names.size();i++){
    long sec=parse(times.get(i).getText().toString());String name=cleanName(names.get(i).getText().toString().trim());
    if(sec<=0||name.isEmpty()){invalid.add("Rij "+(i+1));continue;}
    try{JSONObject o=new JSONObject();o.put("id",UUID.randomUUID().toString());o.put("account",ac);o.put("category",cat);o.put("name",name);o.put("finish",now+sec*1000);o.put("scanned",true);replacement.add(o);}catch(Exception ignored){}
   }
   if(!invalid.isEmpty()){
    new android.app.AlertDialog.Builder(this).setTitle(t("Scan niet opgeslagen"))
     .setMessage(t("Ongeldige timer(s): "+android.text.TextUtils.join(", ",invalid)+". Corrigeer deze rijen en druk opnieuw op Opslaan. Bestaande timers blijven behouden."))
     .setPositiveButton(t("OK"),null).show();
    return;
   }
   // Only replace previous screenshot-scanned entries; never destroy manually created timers.
   for(int i=upgrades.size()-1;i>=0;i--){JSONObject old=upgrades.get(i);if(ac.equals(old.optString("account"))&&cat.equals(old.optString("category"))&&old.optBoolean("scanned",false))upgrades.remove(i);}
   upgrades.addAll(replacement);persist();
   
   showTimers();toast(replacement.size()+" timers opgeslagen");dialog.dismiss();
  }));dialog.show();
 }
 private void toast(String s){Toast.makeText(this,t(s),Toast.LENGTH_LONG).show();}
}
