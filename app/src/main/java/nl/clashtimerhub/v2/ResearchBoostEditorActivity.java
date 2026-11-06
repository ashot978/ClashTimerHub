package nl.clashtimerhub.v2;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/** Multi-account boost overview and per-account exact-duration editor. */
public final class ResearchBoostEditorActivity extends Activity {
 private static final int BG=0xff171025,WHITE=Color.WHITE,MUTED=0xffcdbbdc;
 private static final int GOLD=0xffffd783,GREEN=0xff8be3ad,PURPLE=0xff7551b1;
 private String account;
 private boolean english;
 private EditText hourField,minuteField;
 private String tr(String nl,String en){return english?en:nl;}
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 private TextView text(String v,int size,int color){
  TextView t=new TextView(this);t.setText(v);t.setTextColor(color);t.setTextSize(size);return t;
 }
 private Button button(String label,int color){
  Button b=new Button(this);
  b.setText(label);b.setAllCaps(false);b.setTextSize(15);
  b.setTextColor(WHITE);b.setTypeface(null,1);
  b.setBackgroundTintList(ColorStateList.valueOf(color));
  b.setMinHeight(dp(48));return b;
 }
 private boolean exists(String value){
  if(value==null||value.isEmpty())return false;
  try{
   JSONArray values=new JSONArray(AppData.prefs(this).getString("accounts","[]"));
   for(int i=0;i<values.length();i++)if(value.equals(values.optString(i)))return true;
  }catch(Exception ignored){}
  return false;
 }
 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);
  english=I18n.english(this);
  account=getIntent().getBooleanExtra("overview",false)?null:getIntent().getStringExtra("account");
  if(account!=null&&!exists(account)){finish();return;}
  render();
 }
 @Override protected void onNewIntent(Intent intent){
  super.onNewIntent(intent);setIntent(intent);
  if(intent.getBooleanExtra("overview",false)){account=null;render();return;}
  String target=intent.getStringExtra("account");
  if(exists(target)){account=target;render();}
 }
 @Override public void onBackPressed(){
  if(account!=null){account=null;render();}
  else super.onBackPressed();
 }
 private LinearLayout screen(){
  LinearLayout body=new LinearLayout(this);
  body.setOrientation(LinearLayout.VERTICAL);
  body.setPadding(dp(15),dp(12),dp(15),dp(32));
  body.setBackgroundColor(BG);
  ScrollView scroll=new ScrollView(this);
  scroll.setFillViewport(true);
  scroll.addView(body);
  setContentView(scroll);
  // Android 15+ forces edge-to-edge. Keep all controls clear of status/nav bars.
  if(Build.VERSION.SDK_INT>=30){
   scroll.setOnApplyWindowInsetsListener((v,insets)->{
    android.graphics.Insets bars=insets.getInsets(
     WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
    scroll.setPadding(bars.left,bars.top,bars.right,bars.bottom);
    return insets;
   });
   scroll.requestApplyInsets();
  }else scroll.setFitsSystemWindows(true);
  return body;
 }
 private String date(long ms){
  return new SimpleDateFormat("EEE d MMM HH:mm",
   english?Locale.UK:Locale.forLanguageTag("nl-NL")).format(new Date(ms));
 }
 private String compact(long ms){
  long min=Math.max(1,(ms+30000L)/60000L);
  return min>=60?(min/60)+"h"+String.format(Locale.ROOT,"%02d",min%60):min+"m";
 }
 private String boostTime(ResearchPotionTracker.Snapshot snapshot,long now){
  if(!snapshot.valid)return tr("⚠ Controle nodig","⚠ Check needed");
  long minutes=(snapshot.remaining(now)+59999L)/60000L;
  if(minutes==0)return tr("Geen boost","No boost");
  return (minutes/60)+"h "+String.format(Locale.ROOT,"%02d",minutes%60)+"m";
 }
 private ArrayList<JSONObject> builders(String account,long now){
  ArrayList<JSONObject> result=new ArrayList<>();
  try{
   JSONArray all=new JSONArray(AppData.prefs(this).getString("upgrades","[]"));
   for(int i=0;i<all.length();i++){
    JSONObject item=all.optJSONObject(i);
    if(item!=null&&account.equals(item.optString("account"))&&
     "Laboratorium".equals(item.optString("category"))&&item.optLong("finish",0L)>now)
     result.add(item);
   }
  }catch(Exception e){android.util.Log.e("ClashTimerHub","Laboratory forecast unavailable",e);}
  return result;
 }
 private void addHour(String target){
  try{
   ResearchPotionTracker.addHour(this,target);
   TimerWidget.update(this);
   android.widget.Toast.makeText(this,AppData.displayName(this,target)+
    tr(": +1u Research Potion",": +1h Research Potion"),
    android.widget.Toast.LENGTH_SHORT).show();
   render();
  }catch(Exception e){
   android.widget.Toast.makeText(this,e.getMessage(),android.widget.Toast.LENGTH_LONG).show();
  }
 }
 private void action(int type){
  try{
   if(type==0){addHour(account);return;}
   if(type==1)ResearchPotionTracker.reset(this,account);
   else{
    int h=Integer.parseInt(hourField.getText().toString().trim().isEmpty()?"0":hourField.getText().toString().trim());
    int m=Integer.parseInt(minuteField.getText().toString().trim().isEmpty()?"0":minuteField.getText().toString().trim());
    ResearchPotionTracker.setRemaining(this,account,h,m);
   }
   TimerWidget.update(this);render();
  }catch(Exception e){
   android.widget.Toast.makeText(this,
    e.getMessage()==null?tr("Voer een geldige tijd in","Enter a valid time"):e.getMessage(),
    android.widget.Toast.LENGTH_LONG).show();
  }
 }
 private void render(){
  if(account==null)renderOverview();
  else renderAccount();
 }
 private void renderOverview(){
  long now=System.currentTimeMillis();
  LinearLayout body=screen();
  TextView heading=text(tr("⚡ Research Potions · alle accounts","⚡ Research Potions · all accounts"),21,GOLD);
  heading.setTypeface(null,1);
  body.addView(heading);
  TextView hint=text(tr("Zelfde volgorde als je oorspronkelijke multi-accountwidget. "+
    "Tik op +1u zodra je in Clash een Research Potion activeert. Tik op een account om de precieze tijd en alle onderzoeksupgrades te bekijken.",
    "Matches your original multi-account widget order. "+
    "Tap +1h when you activate a Research Potion in Clash. Select an account for exact timing and all Laboratory upgrades."),13,MUTED);
  hint.setPadding(0,dp(9),0,dp(15));body.addView(hint);
  ArrayList<String> accounts=WidgetV7.boostAccountOrder(this);
  if(accounts.isEmpty())body.addView(text(tr("Geen accounts beschikbaar","No accounts available"),15,MUTED));
  for(String key:accounts){
   ResearchPotionTracker.Snapshot snapshot=ResearchPotionTracker.load(this,key);
   long firstOriginal=Long.MAX_VALUE,firstForecast=Long.MAX_VALUE;
   for(JSONObject item:builders(key,now)){
    long original=item.optLong("finish");
    firstOriginal=Math.min(firstOriginal,original);
    firstForecast=Math.min(firstForecast,ResearchPotionTracker.estimateFinish(snapshot,original));
   }
   LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);
   line.setGravity(Gravity.CENTER_VERTICAL);
   line.setPadding(dp(12),dp(6),dp(6),dp(6));
   line.setBackgroundColor(0xff2e2040);
   LinearLayout.LayoutParams lineParams=new LinearLayout.LayoutParams(-1,dp(76));
   lineParams.bottomMargin=dp(5);body.addView(line,lineParams);
   LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);
   line.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
   TextView name=text(AppData.displayName(this,key),15,WHITE);name.setTypeface(null,1);
   labels.addView(name);
   String when=firstForecast==Long.MAX_VALUE?"—":
     new SimpleDateFormat("HH:mm",Locale.ROOT).format(new Date(firstForecast));
   long saved=firstOriginal!=Long.MAX_VALUE&&firstForecast!=Long.MAX_VALUE?
    Math.max(0,firstOriginal-firstForecast):0;
   String status=boostTime(snapshot,now)+"  ·  "+
    tr("Lab ","Laboratory ")+when+(saved>0?"  −"+compact(saved):"");
   TextView sub=text(status,12,saved>0?GREEN:MUTED);
   sub.setPadding(0,dp(4),0,0);labels.addView(sub);
   labels.setOnClickListener(v->{account=key;render();});
   Button add=button(english?"+1h":"+1u",0xff27774f);
   line.addView(add,new LinearLayout.LayoutParams(dp(78),dp(54)));
   add.setOnClickListener(v->addHour(key));
   Button open=button("›",PURPLE);
   LinearLayout.LayoutParams openParams=new LinearLayout.LayoutParams(dp(48),dp(54));
   openParams.leftMargin=dp(4);line.addView(open,openParams);
   open.setContentDescription(tr("Bewerk boost van ","Edit boost for ")+AppData.displayName(this,key));
   open.setOnClickListener(v->{account=key;render();});
  }
  TextView foot=text(tr("⚡ Eindtijden zijn schattingen. Nieuwe JSON-exports stoppen oude voorspellingen om dubbele versnelling te voorkomen.",
   "⚡ End times are estimates. A new JSON import invalidates old forecasts to prevent double boost calculations."),12,MUTED);
  foot.setPadding(0,dp(13),0,0);body.addView(foot);
 }
 private void renderAccount(){
  long now=System.currentTimeMillis();
  ResearchPotionTracker.Snapshot snapshot=ResearchPotionTracker.load(this,account);
  long remainingMinutes=(snapshot.remaining(now)+59999L)/60000L;
  LinearLayout body=screen();
  Button back=button(tr("← Alle accounts","← All accounts"),0xff423151);
  LinearLayout.LayoutParams backParams=new LinearLayout.LayoutParams(dp(180),dp(44));
  body.addView(back,backParams);
  back.setOnClickListener(v->{account=null;render();});
  TextView name=text("⚡ "+AppData.displayName(this,account),22,GOLD);
  name.setTypeface(null,1);name.setPadding(0,dp(12),0,dp(2));body.addView(name);
  TextView hint=text(tr(
   "Research Potion · 24×. Eén potion = 1 uur boost en maximaal 23 uur tijdwinst per actieve Laboratory-upgrade.",
   "Research Potion · 24×. One potion = one hour of boost and up to 23 hours saved per active Laboratory upgrade."),13,MUTED);
  hint.setPadding(0,dp(3),0,dp(10));body.addView(hint);
  TextView current=text(tr("Resterende boosttijd: ","Remaining boost: ")+boostTime(snapshot,now),16,GREEN);
  current.setTypeface(null,1);body.addView(current);
  TextView inputLabel=text(tr("Exacte resterende tijd instellen","Set exact remaining time"),13,MUTED);
  inputLabel.setPadding(0,dp(11),0,dp(3));body.addView(inputLabel);
  LinearLayout inputs=new LinearLayout(this);
  inputs.setGravity(Gravity.CENTER_VERTICAL);
  inputs.setOrientation(LinearLayout.HORIZONTAL);
  hourField=new EditText(this);hourField.setInputType(InputType.TYPE_CLASS_NUMBER);
  hourField.setSingleLine(true);hourField.setText(String.valueOf(remainingMinutes/60));
  hourField.setTextColor(WHITE);hourField.setSelectAllOnFocus(true);
  minuteField=new EditText(this);minuteField.setInputType(InputType.TYPE_CLASS_NUMBER);
  minuteField.setSingleLine(true);minuteField.setText(String.valueOf(remainingMinutes%60));
  minuteField.setTextColor(WHITE);minuteField.setSelectAllOnFocus(true);
  inputs.addView(hourField,new LinearLayout.LayoutParams(dp(75),dp(48)));
  inputs.addView(text(tr(" uur  "," h  "),13,MUTED));
  inputs.addView(minuteField,new LinearLayout.LayoutParams(dp(75),dp(48)));
  inputs.addView(text(" min",13,MUTED));
  body.addView(inputs);
  Button save=button(tr("✓ Tijd opslaan","✓ Save duration"),PURPLE);
  LinearLayout.LayoutParams saveParams=new LinearLayout.LayoutParams(-1,dp(54));
  saveParams.bottomMargin=dp(7);body.addView(save,saveParams);
  save.setOnClickListener(v->action(2));
  LinearLayout quick=new LinearLayout(this);
  quick.setOrientation(LinearLayout.HORIZONTAL);
  Button add=button(tr("⚡ +1u potion","⚡ +1h potion"),0xff27774f);
  Button clear=button(tr("Boost wissen","Clear boost"),0xff604151);
  quick.addView(add,new LinearLayout.LayoutParams(0,dp(54),1));
  LinearLayout.LayoutParams clearParams=new LinearLayout.LayoutParams(0,dp(54),1);
  clearParams.leftMargin=dp(7);quick.addView(clear,clearParams);
  add.setOnClickListener(v->action(0));
  clear.setOnClickListener(v->action(1));
  body.addView(quick);
  if(!snapshot.valid){
   TextView warn=text(tr("⚠ Nieuwe JSON geïmporteerd na deze boost. De oude voorspelling staat uit om dubbel rekenen te voorkomen. Controleer de resterende boosttijd in Clash.",
    "⚠ New JSON imported after this boost. The old forecast is disabled to avoid double counting. Verify remaining boost time in Clash."),13,0xffffb1a8);
   warn.setPadding(0,dp(10),0,0);body.addView(warn);
  }
  TextView section=text(tr("Geschatte eindtijden van onderzoeksupgrades",
   "Estimated research completion times"),17,WHITE);
  section.setTypeface(null,1);section.setPadding(0,dp(20),0,dp(4));body.addView(section);
  TextView explanation=text(tr(
   "23 uur besparing als er minstens 24 uur onderzoek over is bij 1 uur boost. Korter onderzoek is al binnen de potion klaar.",
   "23 hours saved when at least 24 hours of research remain for a one-hour boost. Shorter research finishes during the potion."),12,MUTED);
  explanation.setPadding(0,0,0,dp(8));body.addView(explanation);
  ArrayList<JSONObject> list=builders(account,now);
  Collections.sort(list,(a,b)->Long.compare(
   ResearchPotionTracker.estimateFinish(snapshot,a.optLong("finish")),
   ResearchPotionTracker.estimateFinish(snapshot,b.optLong("finish"))));
  if(list.isEmpty())body.addView(text(tr("Geen actief Laboratory-onderzoek. Synchroniseer dit account.",
   "No active research upgrades. Sync this account."),14,MUTED));
  HashMap<String,Integer> repeated=new HashMap<>();
  for(JSONObject item:list){
   String title=item.optString("name",tr("Upgrade","Upgrade"));
   repeated.put(title,repeated.getOrDefault(title,0)+1);
  }
  HashMap<String,Integer> seen=new HashMap<>();
  for(JSONObject item:list){
   long original=item.optLong("finish");
   long predicted=ResearchPotionTracker.estimateFinish(snapshot,original);
   long saved=Math.max(0,original-predicted);
   String title=item.optString("name",tr("Upgrade","Upgrade"));
   int index=seen.getOrDefault(title,0)+1;seen.put(title,index);
   if(repeated.getOrDefault(title,0)>1)title+="  #"+index;
   LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
   card.setBackgroundColor(0xff2e2040);
   card.setPadding(dp(12),dp(9),dp(12),dp(9));
   LinearLayout.LayoutParams cardParams=new LinearLayout.LayoutParams(-1,-2);
   cardParams.bottomMargin=dp(5);body.addView(card,cardParams);
   TextView building=text("⚒ "+title,14,WHITE);building.setTypeface(null,1);
   card.addView(building);
   TextView next=text(tr("Met boost: ","With boost: ")+date(predicted)+
    (saved>0?"   −"+compact(saved):""),14,saved>0?GREEN:GOLD);
   next.setPadding(0,dp(5),0,dp(2));card.addView(next);
   card.addView(text(tr("Zonder nieuwe boost: ","Without new boost: ")+date(original),12,MUTED));
  }
  TextView foot=text(tr(
   "Voorspellingen zijn geen live Clash-data. Oorspronkelijke timers en meldingen blijven ongewijzigd. Is een boost al verwerkt in je JSON-export, synchroniseer dan eerst opnieuw.",
   "Forecasts are not live Clash data. Original timers and alerts remain unchanged. If a boost is already reflected in the JSON export, sync before forecasting again."),12,MUTED);
  foot.setPadding(0,dp(17),0,dp(15));body.addView(foot);
 }
}
