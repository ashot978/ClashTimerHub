package nl.clashtimerhub.v2;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.*;
import org.json.JSONArray;
import java.util.*;

/** Configure each widget ID independently; scrollable controls with a fixed save bar. */
public final class WidgetConfigActivity extends Activity {
 private static final int BG=0xff171025;
 private static final int GOLD=0xffffd783;
 private static final int MUTED=0xffcdbbdc;
 private int widgetId=AppWidgetManager.INVALID_APPWIDGET_ID,kind=WidgetV7.MULTI;
 private final LinkedHashMap<String,CheckBox> boxes=new LinkedHashMap<>();
 private final ArrayList<String> accounts=new ArrayList<>(),manualOrder=new ArrayList<>();
 private RadioButton all,custom;
 private Spinner accountPicker,sortPicker,sizePicker,backgroundPicker;
 private CheckBox labCheck,petCheck,helpersCheck;
 private LinearLayout orderRows;
 private SharedPreferences prefs;
 private boolean en;

 private String tr(String nl,String english){return en?english:nl;}
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  setResult(RESULT_CANCELED);
  widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
  if(widgetId==AppWidgetManager.INVALID_APPWIDGET_ID){finish();return;}
  AppData.migrate(this);
  kind=WidgetV7.kind(this,widgetId);
  en=I18n.english(this);
  prefs=getSharedPreferences("WidgetPreferences",MODE_PRIVATE);
  try{
   JSONArray saved=new JSONArray(AppData.prefs(this).getString("accounts","[]"));
   for(int i=0;i<saved.length();i++){
    String account=saved.optString(i,"");
    if(!account.isEmpty()&&!accounts.contains(account))accounts.add(account);
   }
  }catch(Exception e){android.util.Log.e("ClashTimerHub","Widget accounts unavailable",e);}
  getWindow().setStatusBarColor(BG);
  getWindow().setNavigationBarColor(BG);
  getWindow().getDecorView().setSystemUiVisibility(0);
  render();
 }

 private void render(){
  LinearLayout outer=new LinearLayout(this);
  outer.setOrientation(LinearLayout.VERTICAL);
  outer.setBackgroundColor(BG);
  outer.setLayoutParams(new ViewGroup.LayoutParams(-1,-1));
  if(Build.VERSION.SDK_INT>=30){
   outer.setOnApplyWindowInsetsListener((v,insets)->{
    android.graphics.Insets bars=insets.getInsets(
     WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
    v.setPadding(bars.left,bars.top,bars.right,bars.bottom);
    return insets;
   });
  }else outer.setFitsSystemWindows(true);
  setContentView(outer);
  if(Build.VERSION.SDK_INT>=30)outer.requestApplyInsets();

  LinearLayout header=new LinearLayout(this);
  header.setOrientation(LinearLayout.VERTICAL);
  header.setPadding(dp(18),dp(10),dp(18),dp(9));
  header.setBackgroundColor(0xff21162e);
  TextView title=label(tr("Widget instellen","Configure widget"),21,GOLD);
  title.setTypeface(null,android.graphics.Typeface.BOLD);
  title.setPadding(0,0,0,dp(2));
  header.addView(title);
  String type=kind==WidgetV7.SYNC?tr("Sync-knop","Sync button"):
   kind==WidgetV7.MINI?tr("Miniwidget","Mini widget"):
   kind==WidgetV7.SINGLE?tr("Accountwidget","Account widget"):
   tr("Multi-accountwidget","Multi-account widget");
  TextView subtitle=label(type+"  •  "+tr("Widget ","Widget ")+widgetId,13,MUTED);
  subtitle.setPadding(0,0,0,0);header.addView(subtitle);
  outer.addView(header,new LinearLayout.LayoutParams(-1,-2));

  ScrollView scroll=new ScrollView(this);
  scroll.setFillViewport(false);
  scroll.setClipToPadding(false);
  LinearLayout content=new LinearLayout(this);
  content.setOrientation(LinearLayout.VERTICAL);
  content.setPadding(dp(18),dp(12),dp(18),dp(24));
  scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
  outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

  if(kind==WidgetV7.MULTI)buildMulti(content);
  else if(kind==WidgetV7.SINGLE||kind==WidgetV7.MINI)buildAccount(content);
  else{
   section(content,tr("Zwevende synchronisatie","Floating synchronization"),
    tr("Deze widget schakelt de algemene zwevende Sync-knop in of uit. De status geldt voor de hele app.",
       "This widget toggles the app-wide floating Sync button. Its state is shared by all widgets."));
  }
  buildAppearance(content);

  LinearLayout footer=new LinearLayout(this);
  footer.setOrientation(LinearLayout.VERTICAL);
  footer.setPadding(dp(16),dp(8),dp(16),dp(10));
  footer.setBackgroundColor(0xff21162e);
  Button save=new Button(this);
  save.setAllCaps(false);
  save.setText(tr("✓ Widget opslaan","✓ Save widget"));
  save.setTextSize(16);
  save.setTextColor(Color.WHITE);
  save.setBackgroundTintList(ColorStateList.valueOf(0xff7652a3));
  save.setMinHeight(dp(48));
  footer.addView(save,new LinearLayout.LayoutParams(-1,dp(52)));
  outer.addView(footer,new LinearLayout.LayoutParams(-1,-2));
  save.setOnClickListener(v->saveWidget());
 }

 private void section(LinearLayout parent,String title,String details){
  TextView heading=label(title,17,GOLD);
  heading.setTypeface(null,android.graphics.Typeface.BOLD);
  heading.setPadding(0,dp(13),0,dp(5));
  parent.addView(heading);
  if(details!=null&&!details.isEmpty()){
   TextView sub=label(details,13,MUTED);
   sub.setPadding(0,0,0,dp(8));
   parent.addView(sub);
  }
 }

 private void buildMulti(LinearLayout parent){
  section(parent,tr("Accounts","Accounts"),
   tr("Kies de accounts voor deze widget. Iedere widget bewaart zijn eigen selectie.",
      "Choose accounts for this widget. Each widget keeps its own selection."));
  if(accounts.isEmpty()){
   parent.addView(label(tr("Synchroniseer eerst een account in de app.",
     "Sync an account in the app first."),14,MUTED));
   return;
  }
  RadioGroup mode=new RadioGroup(this);
  mode.setOrientation(RadioGroup.VERTICAL);
  all=new RadioButton(this);
  all.setText(tr("Alle accounts","All accounts"));all.setTextColor(Color.WHITE);
  custom=new RadioButton(this);
  custom.setText(tr("Geselecteerde accounts","Selected accounts"));custom.setTextColor(Color.WHITE);
  mode.addView(all);mode.addView(custom);parent.addView(mode);
  boolean showAll=prefs.getBoolean("all_"+widgetId,true);
  Set<String> chosen=new HashSet<>();
  try{
   JSONArray arr=new JSONArray(prefs.getString("selected_"+widgetId,"[]"));
   for(int i=0;i<arr.length();i++)chosen.add(arr.optString(i));
  }catch(Exception ignored){}
  for(String name:accounts){
   CheckBox box=new CheckBox(this);
   box.setText(AppData.displayName(this,name));
   box.setTextColor(Color.WHITE);
   box.setChecked(showAll||chosen.contains(name));
   boxes.put(name,box);manualOrder.add(name);
  }
  try{
   JSONArray arr=new JSONArray(prefs.getString("order_"+widgetId,"[]"));
   ArrayList<String> sorted=new ArrayList<>();
   for(int i=0;i<arr.length();i++){
    String value=arr.optString(i);
    if(manualOrder.contains(value)&&!sorted.contains(value))sorted.add(value);
   }
   for(String key:manualOrder)if(!sorted.contains(key))sorted.add(key);
   manualOrder.clear();manualOrder.addAll(sorted);
  }catch(Exception ignored){}
  all.setChecked(showAll);custom.setChecked(!showAll);
  mode.setOnCheckedChangeListener((group,checked)->updateSelection());
  parent.addView(label(tr("Gebruik pijlen om de eigen accountvolgorde te wijzigen.",
   "Use the arrows to change your custom account order."),12,MUTED));
  orderRows=new LinearLayout(this);
  orderRows.setOrientation(LinearLayout.VERTICAL);
  parent.addView(orderRows);
  renderOrderRows();
  updateSelection();
 }

 private void renderOrderRows(){
  if(orderRows==null)return;
  orderRows.removeAllViews();
  for(int i=0;i<manualOrder.size();i++){
   int index=i;String name=manualOrder.get(i);
   LinearLayout row=new LinearLayout(this);
   row.setOrientation(LinearLayout.HORIZONTAL);
   row.setGravity(Gravity.CENTER_VERTICAL);
   CheckBox box=boxes.get(name);
   if(box.getParent() instanceof ViewGroup)((ViewGroup)box.getParent()).removeView(box);
   row.addView(box,new LinearLayout.LayoutParams(0,dp(46),1));
   Button up=new Button(this),down=new Button(this);
   up.setText("↑");down.setText("↓");
   up.setAllCaps(false);down.setAllCaps(false);
   up.setTextSize(16);down.setTextSize(16);
   up.setEnabled(index>0);down.setEnabled(index<manualOrder.size()-1);
   up.setOnClickListener(v->{Collections.swap(manualOrder,index,index-1);renderOrderRows();});
   down.setOnClickListener(v->{Collections.swap(manualOrder,index,index+1);renderOrderRows();});
   row.addView(up,new LinearLayout.LayoutParams(dp(48),dp(46)));
   row.addView(down,new LinearLayout.LayoutParams(dp(48),dp(46)));
   orderRows.addView(row);
  }
  updateSelection();
 }
 private void updateSelection(){
  if(all==null||custom==null)return;
  boolean editable=custom.isChecked();
  for(CheckBox box:boxes.values())box.setEnabled(editable);
 }

 private void buildAccount(LinearLayout parent){
  section(parent,tr("Account kiezen","Choose account"),
   tr("Dit account geldt alleen voor deze widget.","This account is selected only for this widget."));
  if(accounts.isEmpty()){
   parent.addView(label(tr("Synchroniseer eerst een account in de app.",
    "Sync an account in the app first."),14,MUTED));
   return;
  }
  accountPicker=picker(new String[0]);
  ArrayList<String> names=new ArrayList<>();
  for(String account:accounts)names.add(AppData.displayName(this,account));
  accountPicker.setAdapter(new ArrayAdapter<String>(this,
   android.R.layout.simple_spinner_dropdown_item,names));
  int saved=accounts.indexOf(prefs.getString("account_"+widgetId,""));
  accountPicker.setSelection(Math.max(0,saved));
  parent.addView(accountPicker,new LinearLayout.LayoutParams(-1,dp(52)));
 }

 private void buildAppearance(LinearLayout parent){
  section(parent,tr("Weergave","Appearance"),
   tr("Deze instellingen beïnvloeden alleen deze widget.","These settings affect only this widget."));
  if(kind==WidgetV7.MULTI){
   addField(parent,tr("Accountvolgorde","Account order"));
   sortPicker=picker(en?new String[]{"Next builder","Account name","Custom order"}:
    new String[]{"Volgende bouwer","Accountnaam","Eigen volgorde"});
   sortPicker.setSelection(clamp(prefs.getInt("sort_"+widgetId,0),0,2));
   parent.addView(sortPicker,new LinearLayout.LayoutParams(-1,dp(52)));
  }
  if(kind!=WidgetV7.SYNC){
   addField(parent,tr("Tekstgrootte","Text size"));
   sizePicker=picker(en?new String[]{"Small","Normal","Comfort"}:
    new String[]{"Klein","Normaal","Comfort"});
   sizePicker.setSelection(clamp(prefs.getInt("size_"+widgetId,1),0,2));
   parent.addView(sizePicker,new LinearLayout.LayoutParams(-1,dp(52)));
  }
  if(kind==WidgetV7.MULTI||kind==WidgetV7.SINGLE){
   labCheck=new CheckBox(this);
   labCheck.setText(tr("Laboratorium tonen","Show laboratory"));
   labCheck.setTextColor(Color.WHITE);
   labCheck.setChecked(prefs.getBoolean("lab_"+widgetId,true));
   parent.addView(labCheck);
   petCheck=new CheckBox(this);
   petCheck.setText(tr("Pets tonen","Show pets"));
   petCheck.setTextColor(Color.WHITE);
   petCheck.setChecked(prefs.getBoolean("pet_"+widgetId,true));
   parent.addView(petCheck);
   if(kind==WidgetV7.MULTI){
    helpersCheck=new CheckBox(this);
    helpersCheck.setText(tr("Helpers tonen onder elk account","Show helpers below each account"));
    helpersCheck.setTextColor(Color.WHITE);
    helpersCheck.setChecked(prefs.getBoolean("helpers_"+widgetId,true));
    parent.addView(helpersCheck);
   }
  }
  addField(parent,tr("Achtergrond","Background"));
  backgroundPicker=picker(en?new String[]{"Classic purple","Dark","Transparent"}:
   new String[]{"Klassiek paars","Donker","Transparant"});
  backgroundPicker.setSelection(clamp(prefs.getInt("bg_"+widgetId,0),0,2));
  parent.addView(backgroundPicker,new LinearLayout.LayoutParams(-1,dp(52)));
 }

 private void addField(LinearLayout parent,String name){
  TextView text=label(name,14,MUTED);
  text.setPadding(0,dp(10),0,dp(3));
  parent.addView(text);
 }
 private Spinner picker(String[] items){
  Spinner spinner=new Spinner(this);
  if(items.length>0)spinner.setAdapter(new ArrayAdapter<String>(this,
   android.R.layout.simple_spinner_dropdown_item,items));
  return spinner;
 }
 private int clamp(int value,int min,int max){return Math.max(min,Math.min(max,value));}

 private void saveWidget(){
  if(kind!=WidgetV7.SYNC&&accounts.isEmpty()){
   Toast.makeText(this,tr("Synchroniseer eerst een account","Sync an account first"),Toast.LENGTH_LONG).show();
   return;
  }
  SharedPreferences.Editor edit=prefs.edit();
  if(kind==WidgetV7.MULTI){
   JSONArray selected=new JSONArray();
   for(Map.Entry<String,CheckBox> entry:boxes.entrySet())
    if(entry.getValue().isChecked())selected.put(entry.getKey());
   if(custom.isChecked()&&selected.length()==0){
    Toast.makeText(this,tr("Selecteer minstens één account","Select at least one account"),Toast.LENGTH_LONG).show();
    return;
   }
   edit.putBoolean("all_"+widgetId,all.isChecked())
    .putString("selected_"+widgetId,selected.toString())
    .putString("order_"+widgetId,new JSONArray(manualOrder).toString())
    .putInt("sort_"+widgetId,sortPicker.getSelectedItemPosition());
  }else if(kind==WidgetV7.SINGLE||kind==WidgetV7.MINI){
   edit.putString("account_"+widgetId,accounts.get(accountPicker.getSelectedItemPosition()));
  }
  if(sizePicker!=null)edit.putInt("size_"+widgetId,sizePicker.getSelectedItemPosition());
  if(backgroundPicker!=null)edit.putInt("bg_"+widgetId,backgroundPicker.getSelectedItemPosition());
  if(labCheck!=null)edit.putBoolean("lab_"+widgetId,labCheck.isChecked());
  if(petCheck!=null)edit.putBoolean("pet_"+widgetId,petCheck.isChecked());
  if(helpersCheck!=null)edit.putBoolean("helpers_"+widgetId,helpersCheck.isChecked());
  edit.apply();
  TimerWidget.update(this);
  Intent result=new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId);
  setResult(RESULT_OK,result);
  finish();
 }
 private TextView label(String value,int size,int color){
  TextView text=new TextView(this);
  text.setText(value);text.setTextSize(size);text.setTextColor(color);
  return text;
 }
 private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
}
