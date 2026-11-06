package nl.clashtimerhub.v2;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
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
import java.util.Locale;

/** Select one account to enter a precise potion duration and view every builder. */
public final class BuilderBoostEditorActivity extends Activity {
 private String account;
 private boolean english;
 private EditText hourField,minuteField;
 private String tr(String nl,String en){return english?en:nl;}
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 private TextView text(String v,int size,int color){
  TextView t=new TextView(this);t.setText(v);t.setTextColor(color);t.setTextSize(size);return t;
 }
 private Button button(String name){
  Button b=new Button(this);b.setText(name);b.setAllCaps(false);b.setTextSize(13);return b;
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
  super.onCreate(saved);english=I18n.english(this);
  account=getIntent().getStringExtra("account");
  if(!exists(account)){finish();return;}
  render();
 }
 @Override protected void onNewIntent(Intent intent){
  super.onNewIntent(intent);setIntent(intent);
  String target=intent.getStringExtra("account");
  if(exists(target)){account=target;render();}
 }
 private void action(int type){
  try{
   if(type==0)BuilderPotionTracker.addHour(this,account);
   else if(type==1)BuilderPotionTracker.reset(this,account);
   else{
    int hours=Integer.parseInt(hourField.getText().toString().trim().isEmpty()?"0":hourField.getText().toString().trim());
    int minutes=Integer.parseInt(minuteField.getText().toString().trim().isEmpty()?"0":minuteField.getText().toString().trim());
    BuilderPotionTracker.setRemaining(this,account,hours,minutes);
   }
   TimerWidget.update(this);
   render();
  }catch(Exception e){android.widget.Toast.makeText(this,
    e.getMessage()==null?tr("Voer een geldige tijd in","Enter a valid time"):e.getMessage(),
    android.widget.Toast.LENGTH_LONG).show();}
 }
 private String date(long value){
  return new SimpleDateFormat("EEE d MMM HH:mm",
   english?Locale.UK:Locale.forLanguageTag("nl-NL")).format(new Date(value));
 }
 private void render(){
  long now=System.currentTimeMillis();
  BuilderPotionTracker.Snapshot boost=BuilderPotionTracker.load(this,account);
  long mins=(boost.remaining(now)+59999)/60000;
  LinearLayout container=new LinearLayout(this);
  container.setOrientation(LinearLayout.VERTICAL);
  container.setPadding(dp(18),dp(16),dp(18),dp(20));
  container.setBackgroundColor(0xff171025);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(container);
  setContentView(scroll);

  TextView title=text("⚡ "+AppData.displayName(this,account),22,0xffffd783);
  title.setTypeface(null,1);container.addView(title);
  TextView sub=text(tr("Builder Potion · 10× bouwsnelheid\n"+
    "Tik in de widget op +1u wanneer je in Clash een potion activeert. Hier kun je een exacte resterende tijd invoeren.",
    "Builder Potion · 10× building speed\n"+
    "Tap +1h in the widget when you activate a potion in Clash. Enter an exact remaining duration here."),13,0xffcdbbdc);
  sub.setPadding(0,dp(8),0,dp(12));container.addView(sub);

  LinearLayout inputs=new LinearLayout(this);inputs.setGravity(Gravity.CENTER_VERTICAL);
  inputs.setOrientation(LinearLayout.HORIZONTAL);
  hourField=new EditText(this);hourField.setInputType(InputType.TYPE_CLASS_NUMBER);
  hourField.setSingleLine(true);hourField.setText(String.valueOf(mins/60));
  hourField.setSelectAllOnFocus(true);
  minuteField=new EditText(this);minuteField.setInputType(InputType.TYPE_CLASS_NUMBER);
  minuteField.setSingleLine(true);minuteField.setText(String.valueOf(mins%60));
  minuteField.setSelectAllOnFocus(true);
  inputs.addView(hourField,new LinearLayout.LayoutParams(0,dp(48),1));
  inputs.addView(text(tr(" uur "," h "),13,Color.WHITE));
  inputs.addView(minuteField,new LinearLayout.LayoutParams(0,dp(48),1));
  inputs.addView(text(" min",13,Color.WHITE));container.addView(inputs);
  Button save=button(tr("✓ Exacte resterende tijd opslaan","✓ Save exact remaining time"));
  container.addView(save,new LinearLayout.LayoutParams(-1,dp(52)));
  save.setOnClickListener(v->action(2));
  LinearLayout quick=new LinearLayout(this);quick.setOrientation(LinearLayout.HORIZONTAL);
  Button add=button("+1u / +1h"),reset=button(tr("Boost wissen","Clear boost"));
  quick.addView(add,new LinearLayout.LayoutParams(0,dp(48),1));
  quick.addView(reset,new LinearLayout.LayoutParams(0,dp(48),1));
  add.setOnClickListener(v->action(0));reset.setOnClickListener(v->action(1));
  container.addView(quick);

  if(!boost.valid){
   TextView warning=text(tr(
    "⚠ Na het instellen van de boost is een nieuwe JSON geïmporteerd. De eerdere voorspelling is uitgeschakeld, omdat de export de versnelling al kan bevatten. Controleer de resterende tijd in Clash voordat je opnieuw registreert.",
    "⚠ New JSON was imported after this boost was recorded. The old forecast is disabled because the export may already contain the speedup. Check the remaining duration in Clash before recording it again."),
    13,0xffffb1a8);
   warning.setPadding(0,dp(10),0,dp(6));container.addView(warning);
  }

  TextView heading=text(tr("\nGeschatte eindtijden per bouwer",
   "\nEstimated completion times for each builder"),16,Color.WHITE);
  heading.setTypeface(null,1);container.addView(heading);
  try{
   JSONArray upgrades=new JSONArray(AppData.prefs(this).getString("upgrades","[]"));
   ArrayList<JSONObject> builds=new ArrayList<>();
   for(int i=0;i<upgrades.length();i++){
    JSONObject item=upgrades.optJSONObject(i);
    if(item!=null&&account.equals(item.optString("account"))&&
      "Bouwers".equals(item.optString("category"))&&item.optLong("finish")>now)builds.add(item);
   }
   Collections.sort(builds,(a,b)->Long.compare(
    BuilderPotionTracker.estimateFinish(boost,a.optLong("finish")),
    BuilderPotionTracker.estimateFinish(boost,b.optLong("finish"))));
   if(builds.isEmpty())container.addView(text(tr("Geen actieve bouwertimers. Synchroniseer het account.",
     "No active builder timers. Sync this account."),13,0xffcdbbdc));
   for(JSONObject item:builds){
    long original=item.optLong("finish");
    long projected=BuilderPotionTracker.estimateFinish(boost,original);
    TextView name=text("⚒ "+item.optString("name",tr("Bouwer","Builder")),14,Color.WHITE);
    name.setPadding(0,dp(12),0,dp(3));container.addView(name);
    TextView result=text(tr("Origineel: ","Original: ")+date(original)+"\n"+
      tr("Geschat met boost: ","Estimated with boost: ")+date(projected),
      12,projected<original?0xff8be3ad:0xffcdbbdc);
    container.addView(result);
   }
  }catch(Exception e){
   container.addView(text(tr("Timers niet beschikbaar","Timers unavailable"),13,0xffffb1a8));
  }
  TextView note=text(tr(
   "\nDit zijn voorspellingen, geen live Clash-data. Oorspronkelijke JSON-timers en meldingen worden niet overschreven. Als de export of een Helper de versnelling al heeft verwerkt, kan de schatting afwijken.",
   "\nThese are estimates, not live Clash data. Original JSON timers and alerts remain unchanged. Predictions can differ if the export or a Helper already incorporated the speedup."),12,0xffcdbbdc);
  note.setPadding(0,dp(13),0,dp(5));container.addView(note);
 }
}