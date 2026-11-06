package nl.clashtimerhub.v2;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONArray;

/** Per-widget settings for the completed multi-account All Timers widget. */
public final class AllTimersConfigActivity extends Activity {
 private static final int BG=0xff171025,SECTION=0xff21162e,GOLD=0xffffd783,MUTED=0xffcbb5d9;
 private final ArrayList<String> accounts=new ArrayList<>(),manualAccounts=new ArrayList<>();
 private final ArrayList<String> columns=new ArrayList<>();
 private final LinkedHashMap<String,CheckBox> selected=new LinkedHashMap<>();
 private int widgetId=AppWidgetManager.INVALID_APPWIDGET_ID;
 private boolean english;
 private SharedPreferences prefs;
 private LinearLayout accountRows,columnRows;
 private RadioButton all,custom;
 private Spinner sorter;
 private CheckBox helpers;
 private String tr(String nl,String en){return english?en:nl;}
 private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
 private TextView text(String t,int size,int color){
  TextView view=new TextView(this);view.setText(t);view.setTextSize(size);view.setTextColor(color);return view;
 }
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  setResult(RESULT_CANCELED);
  widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
  if(widgetId==AppWidgetManager.INVALID_APPWIDGET_ID){finish();return;}
  AppData.migrate(this);english=I18n.english(this);
  prefs=AllTimersWidgetSettings.preferences(this);
  try{
   JSONArray raw=new JSONArray(AppData.prefs(this).getString("accounts","[]"));
   for(int i=0;i<raw.length();i++){
    String a=raw.optString(i);
    if(!a.isEmpty()&&!accounts.contains(a))accounts.add(a);
   }
  }catch(Exception e){android.util.Log.w("ClashTimerHub","All Timers account config",e);}
  manualAccounts.addAll(AllTimersWidgetSettings.manualAccounts(this,widgetId,accounts));
  for(String column:AllTimersWidgetSettings.order(this,widgetId))columns.add(column);
  build();
 }
 private void heading(LinearLayout parent,String heading,String detail){
  TextView title=text(heading,17,GOLD);title.setTypeface(null,1);
  title.setPadding(0,dp(15),0,dp(5));parent.addView(title);
  TextView message=text(detail,13,MUTED);message.setPadding(0,0,0,dp(8));parent.addView(message);
 }
 private String columnLabel(String name){
  if(AllTimersWidgetSettings.LAB.equals(name))return tr("🧙 Onderzoek","🧙 Research");
  if(AllTimersWidgetSettings.PETS.equals(name))return tr("🐾 Pets","🐾 Pets");
  return tr("⚒ Bouwers","⚒ Builders");
 }
 private Button arrow(String glyph){
  Button b=new Button(this);b.setText(glyph);b.setAllCaps(false);b.setTextSize(16);return b;
 }
 private void build(){
  getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
  root.setBackgroundColor(BG);setContentView(root);
  TextView title=text(tr("Alle timers instellen","Configure All Timers"),21,GOLD);
  title.setTypeface(null,1);title.setPadding(dp(18),dp(18),dp(18),dp(16));
  title.setBackgroundColor(SECTION);root.addView(title);
  ScrollView scroll=new ScrollView(this);
  LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
  content.setPadding(dp(18),dp(12),dp(18),dp(24));scroll.addView(content);
  root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  heading(content,tr("Volgorde van categorieën","Category order"),
   tr("Iedere categorie houdt zijn eigen timer en eindkloktijd bij elkaar.",
      "Each category keeps its countdown and finish time together."));
  columnRows=new LinearLayout(this);columnRows.setOrientation(LinearLayout.VERTICAL);
  content.addView(columnRows);drawColumns();
  heading(content,tr("Accountsortering","Account sorting"),
   tr("Volgende bouwer sorteert op de echte eindtijd, inclusief Builder Potion.",
      "Next builder uses actual completion including Builder Potion boosts."));
  sorter=new Spinner(this);
  sorter.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,
   english?new String[]{"Next builder (boost-adjusted)","Account name (A–Z)","Manual order"}:
    new String[]{"Volgende bouwer (incl. potion)","Accountnaam (A–Z)","Eigen volgorde"}));
  sorter.setSelection(AllTimersWidgetSettings.sort(this,widgetId));
  content.addView(sorter,new LinearLayout.LayoutParams(-1,dp(50)));
  heading(content,tr("Accounts","Accounts"),
   tr("Selecteer accounts of kies alle accounts. Iedere widget wordt apart bewaard.",
      "Select accounts or show all. Each widget saves settings independently."));
  RadioGroup mode=new RadioGroup(this);mode.setOrientation(RadioGroup.VERTICAL);
  all=new RadioButton(this);all.setText(tr("Alle accounts","All accounts"));all.setTextColor(Color.WHITE);
  custom=new RadioButton(this);custom.setText(tr("Geselecteerde accounts","Selected accounts"));custom.setTextColor(Color.WHITE);
  mode.addView(all);mode.addView(custom);content.addView(mode);
  boolean allEnabled=AllTimersWidgetSettings.all(this,widgetId);
  all.setChecked(allEnabled);custom.setChecked(!allEnabled);
  ArrayList<String> saved=AllTimersWidgetSettings.savedSelected(this,widgetId);
  for(String account:accounts){
   CheckBox check=new CheckBox(this);check.setText(AppData.displayName(this,account));
   check.setTextColor(Color.WHITE);check.setChecked(allEnabled||saved.contains(account));
   selected.put(account,check);
  }
  accountRows=new LinearLayout(this);accountRows.setOrientation(LinearLayout.VERTICAL);
  content.addView(accountRows);drawAccounts();
  mode.setOnCheckedChangeListener((group,which)->updateEnabled());
  heading(content,tr("Extra instellingen","More settings"),
   tr("Groen = Potion-verkort, goud = normaal, ✓ = handmatig MAX.",
      "Green = boosted, gold = normal, ✓ = manually marked MAX."));
  helpers=new CheckBox(this);helpers.setText(tr("Helpers tonen","Show helpers"));
  helpers.setTextColor(Color.WHITE);
  helpers.setChecked(prefs.getBoolean(AllTimersWidgetSettings.key("helpers",widgetId),true));
  content.addView(helpers);
  LinearLayout footer=new LinearLayout(this);footer.setBackgroundColor(SECTION);
  footer.setPadding(dp(16),dp(8),dp(16),dp(10));
  Button save=new Button(this);save.setAllCaps(false);
  save.setText(tr("✓ Widget opslaan","✓ Save widget"));save.setTextColor(Color.WHITE);
  save.setTextSize(16);save.setBackgroundTintList(ColorStateList.valueOf(0xff7652a3));
  footer.addView(save,new LinearLayout.LayoutParams(-1,dp(52)));root.addView(footer);
  save.setOnClickListener(v->save());
 }
 private void drawColumns(){
  columnRows.removeAllViews();
  for(int i=0;i<columns.size();i++){
   final int pos=i;
   LinearLayout row=new LinearLayout(this);
   row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
   TextView label=text(columnLabel(columns.get(i)),15,Color.WHITE);
   label.setGravity(Gravity.CENTER_VERTICAL);
   row.addView(label,new LinearLayout.LayoutParams(0,dp(46),1));
   Button up=arrow("↑"),down=arrow("↓");
   up.setEnabled(i>0);down.setEnabled(i<columns.size()-1);
   up.setOnClickListener(v->{java.util.Collections.swap(columns,pos,pos-1);drawColumns();});
   down.setOnClickListener(v->{java.util.Collections.swap(columns,pos,pos+1);drawColumns();});
   row.addView(up,new LinearLayout.LayoutParams(dp(48),dp(46)));
   row.addView(down,new LinearLayout.LayoutParams(dp(48),dp(46)));
   columnRows.addView(row);
  }
 }
 private void drawAccounts(){
  accountRows.removeAllViews();
  for(int i=0;i<manualAccounts.size();i++){
   final int pos=i;String key=manualAccounts.get(i);
   CheckBox check=selected.get(key);if(check==null)continue;
   if(check.getParent() instanceof ViewGroup)((ViewGroup)check.getParent()).removeView(check);
   LinearLayout row=new LinearLayout(this);
   row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
   row.addView(check,new LinearLayout.LayoutParams(0,dp(46),1));
   Button up=arrow("↑"),down=arrow("↓");
   up.setEnabled(i>0);down.setEnabled(i<manualAccounts.size()-1);
   up.setOnClickListener(v->{java.util.Collections.swap(manualAccounts,pos,pos-1);drawAccounts();});
   down.setOnClickListener(v->{java.util.Collections.swap(manualAccounts,pos,pos+1);drawAccounts();});
   row.addView(up,new LinearLayout.LayoutParams(dp(48),dp(46)));
   row.addView(down,new LinearLayout.LayoutParams(dp(48),dp(46)));
   accountRows.addView(row);
  }
  updateEnabled();
 }
 private void updateEnabled(){
  boolean enabled=custom!=null&&custom.isChecked();
  for(CheckBox c:selected.values())c.setEnabled(enabled);
 }
 private void save(){
  if(accounts.isEmpty()){
   Toast.makeText(this,tr("Synchroniseer eerst een account","Sync an account first"),Toast.LENGTH_LONG).show();
   return;
  }
  JSONArray selectedAccounts=new JSONArray();
  for(Map.Entry<String,CheckBox> e:selected.entrySet())
   if(e.getValue().isChecked())selectedAccounts.put(e.getKey());
  if(custom.isChecked()&&selectedAccounts.length()==0){
   Toast.makeText(this,tr("Selecteer minstens één account","Select at least one account"),Toast.LENGTH_LONG).show();
   return;
  }
  prefs.edit()
   .putString(AllTimersWidgetSettings.key("columns",widgetId),new JSONArray(columns).toString())
   .putInt(AllTimersWidgetSettings.key("sort",widgetId),sorter.getSelectedItemPosition())
   .putBoolean(AllTimersWidgetSettings.key("all",widgetId),all.isChecked())
   .putString(AllTimersWidgetSettings.key("selected",widgetId),selectedAccounts.toString())
   .putString(AllTimersWidgetSettings.key("accountsOrder",widgetId),new JSONArray(manualAccounts).toString())
   .putBoolean(AllTimersWidgetSettings.key("helpers",widgetId),helpers.isChecked()).apply();
  AllTimersTestWidget.update(this);
  setResult(RESULT_OK,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId));
  finish();
 }
}