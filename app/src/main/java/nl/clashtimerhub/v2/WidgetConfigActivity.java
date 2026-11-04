package nl.clashtimerhub.v2;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;
import org.json.JSONArray;
import java.util.*;

/** Each widget instance has its own account selection. */
public final class WidgetConfigActivity extends Activity {
 private int widgetId=AppWidgetManager.INVALID_APPWIDGET_ID;
 private final LinkedHashMap<String,CheckBox> boxes=new LinkedHashMap<>();
 private RadioButton all,custom;
 private Spinner accountPicker,sortPicker,sizePicker,backgroundPicker;
 private CheckBox labCheck,petCheck;
 private ArrayList<String> accounts=new ArrayList<>();
 private final ArrayList<String> manualOrder=new ArrayList<>();
 private LinearLayout orderRows;
 private int kind=WidgetV7.MULTI;
 @Override public void onCreate(Bundle state){
  super.onCreate(state);setResult(RESULT_CANCELED);
  AppData.migrate(this);
  widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
  if(widgetId==AppWidgetManager.INVALID_APPWIDGET_ID){finish();return;}
  kind=WidgetV7.kind(this,widgetId);
  if(kind==WidgetV7.SYNC){
   new android.app.AlertDialog.Builder(this)
    .setTitle("⚔ Sync")
    .setMessage(I18n.english(this)?"Tap the Sync widget on your home screen to turn the floating sync button on or off.":"Tik op de Sync-widget op je startscherm om de zwevende sync-knop aan of uit te zetten.")
    .setPositiveButton("OK",(d,w)->{setResult(RESULT_OK,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId));finish();})
    .setOnCancelListener(d->finish()).show();
   return;
  }
  android.content.SharedPreferences prefs=getSharedPreferences("WidgetPreferences",MODE_PRIVATE);
  getWindow().setStatusBarColor(0xff171025);getWindow().setNavigationBarColor(0xff171025);
  ScrollView scroll=new ScrollView(this);
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(20),dp(24),dp(20),dp(28));
  root.setBackgroundColor(0xff171025);scroll.addView(root);setContentView(scroll);
  root.addView(label(kind==WidgetV7.MINI?"⚒ Mini Widget":kind==WidgetV7.SINGLE?"⚔ Account Widget":"⚔ Multi-account Widget",23,0xffffd783));
  try{
   JSONArray arr=new JSONArray(AppData.prefs(this).getString("accounts","[]"));
   for(int i=0;i<arr.length();i++){String name=arr.optString(i,"");if(!name.isEmpty())accounts.add(name);}
  }catch(Exception e){android.util.Log.e("ClashTimerHub","Widget config could not load accounts",e);}
  if(accounts.isEmpty())root.addView(label("No accounts yet. Sync your Clash data in the app first.",13,0xffd6bdce));
  else if(kind==WidgetV7.MULTI){
   root.addView(label(I18n.english(this)?"Select the accounts this widget should show.":"Selecteer de accounts die deze widget moet tonen.",14,0xffcdbbdc));
   RadioGroup mode=new RadioGroup(this);root.addView(mode);
   all=new RadioButton(this);all.setText(I18n.english(this)?"All accounts":"Alle accounts");all.setTextColor(Color.WHITE);mode.addView(all);
   custom=new RadioButton(this);custom.setText(I18n.english(this)?"Selected accounts":"Geselecteerde accounts");custom.setTextColor(Color.WHITE);mode.addView(custom);
   boolean showAll=prefs.getBoolean("all_"+widgetId,true);
   Set<String> chosen=new HashSet<>();
   try{JSONArray arr=new JSONArray(prefs.getString("selected_"+widgetId,"[]"));
    for(int j=0;j<arr.length();j++)chosen.add(arr.optString(j));}catch(Exception ignored){}
   for(String name:accounts){
    CheckBox box=new CheckBox(this);box.setText(AppData.displayName(this,name));box.setTextColor(Color.WHITE);
    box.setChecked(showAll||chosen.contains(name));boxes.put(name,box);manualOrder.add(name);
   }
   try{
    JSONArray savedOrder=new JSONArray(prefs.getString("order_"+widgetId,"[]"));
    ArrayList<String> stored=new ArrayList<>();
    for(int i=0;i<savedOrder.length();i++){
     String key=savedOrder.optString(i);
     if(manualOrder.contains(key)&&!stored.contains(key))stored.add(key);
    }
    for(String key:manualOrder)if(!stored.contains(key))stored.add(key);
    manualOrder.clear();manualOrder.addAll(stored);
   }catch(Exception ignored){}
   orderRows=new LinearLayout(this);orderRows.setOrientation(1);root.addView(orderRows);
   renderOrderRows();
   all.setChecked(showAll);custom.setChecked(!showAll);
   Runnable toggle=()->{for(CheckBox box:boxes.values())box.setEnabled(custom.isChecked());};
   mode.setOnCheckedChangeListener((g,id)->toggle.run());toggle.run();
  }else{
   root.addView(label(I18n.english(this)?"Choose one account for this widget.":"Kies één account voor deze widget.",14,0xffcdbbdc));
   accountPicker=new Spinner(this);
   ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,displayLabels());
   accountPicker.setAdapter(adapter);
   String saved=prefs.getString("account_"+widgetId,"");
   int chosen=accounts.indexOf(saved);
   accountPicker.setSelection(Math.max(0,chosen));root.addView(accountPicker);
  }
  if(kind==WidgetV7.MULTI){
   boolean en=I18n.english(this);
   root.addView(label(en?"WIDGET STUDIO":"WIDGET STUDIO",18,0xffffd783));
   root.addView(label(en?"Account order":"Accountvolgorde",14,0xffcdbbdc));
   sortPicker=new Spinner(this);
   sortPicker.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,
    en?new String[]{"Next builder","Account name","Manual account order"}:new String[]{"Volgende bouwer","Accountnaam","Eigen accountvolgorde"}));
   sortPicker.setSelection(Math.max(0,Math.min(2,prefs.getInt("sort_"+widgetId,0))));root.addView(sortPicker);
   root.addView(label(en?"Text size":"Tekstgrootte",14,0xffcdbbdc));
   sizePicker=new Spinner(this);
   sizePicker.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,
    new String[]{"Small / Klein","Normal / Normaal","Large / Groot"}));
   sizePicker.setSelection(Math.max(0,Math.min(2,prefs.getInt("size_"+widgetId,1))));root.addView(sizePicker);
   labCheck=new CheckBox(this);labCheck.setText(en?"Show laboratory":"Laboratorium tonen");
   labCheck.setTextColor(Color.WHITE);labCheck.setChecked(prefs.getBoolean("lab_"+widgetId,true));root.addView(labCheck);
   petCheck=new CheckBox(this);petCheck.setText(en?"Show pets":"Pets tonen");
   petCheck.setTextColor(Color.WHITE);petCheck.setChecked(prefs.getBoolean("pet_"+widgetId,true));root.addView(petCheck);
   root.addView(label(en?"Background":"Achtergrond",14,0xffcdbbdc));
   backgroundPicker=new Spinner(this);
   backgroundPicker.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,
    en?new String[]{"Original purple","Dark","Transparent"}:new String[]{"Origineel paars","Donker","Transparant"}));
   backgroundPicker.setSelection(Math.max(0,Math.min(2,prefs.getInt("bg_"+widgetId,0))));root.addView(backgroundPicker);
  }
  Button save=new Button(this);
  save.setText(I18n.english(this)?"✓ Save widget":"✓ Widget opslaan");save.setAllCaps(false);
  save.setTextColor(Color.WHITE);save.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff7652a3));
  root.addView(save);
  save.setOnClickListener(v->{
   if(accounts.isEmpty()){Toast.makeText(this,I18n.english(this)?"Sync an account first":"Synchroniseer eerst een account",Toast.LENGTH_LONG).show();return;}
   android.content.SharedPreferences.Editor editor=prefs.edit();
   if(kind==WidgetV7.MULTI){
    JSONArray selected=new JSONArray();
    for(Map.Entry<String,CheckBox> entry:boxes.entrySet())if(entry.getValue().isChecked())selected.put(entry.getKey());
    if(custom.isChecked()&&selected.length()==0){
     Toast.makeText(this,I18n.english(this)?"Select at least one account":"Selecteer minstens één account",Toast.LENGTH_LONG).show();return;
    }
    editor.putBoolean("all_"+widgetId,all.isChecked()).putString("selected_"+widgetId,selected.toString());
    editor.putInt("sort_"+widgetId,sortPicker.getSelectedItemPosition()).putString("order_"+widgetId,new JSONArray(manualOrder).toString());
    editor.putInt("size_"+widgetId,sizePicker.getSelectedItemPosition());
    editor.putBoolean("lab_"+widgetId,labCheck.isChecked());
    editor.putBoolean("pet_"+widgetId,petCheck.isChecked());
    editor.putInt("bg_"+widgetId,backgroundPicker.getSelectedItemPosition());
   }else editor.putString("account_"+widgetId,accounts.get(accountPicker.getSelectedItemPosition()));
   editor.apply();TimerWidget.update(this);
   Intent result=new Intent();result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId);
   setResult(RESULT_OK,result);finish();
  });
 }
 private void renderOrderRows(){
  if(orderRows==null)return;
  orderRows.removeAllViews();
  for(int i=0;i<manualOrder.size();i++){
   final int index=i;
   String key=manualOrder.get(i);
   LinearLayout line=new LinearLayout(this);line.setOrientation(0);line.setGravity(android.view.Gravity.CENTER_VERTICAL);
   CheckBox box=boxes.get(key);
   if(box.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)box.getParent()).removeView(box);
   line.addView(box,new LinearLayout.LayoutParams(0,dp(44),1));
   Button up=new Button(this);up.setText("↑");up.setTextSize(15);up.setAllCaps(false);
   up.setEnabled(index>0);up.setOnClickListener(v->{
    Collections.swap(manualOrder,index,index-1);renderOrderRows();
   });
   line.addView(up,new LinearLayout.LayoutParams(dp(42),dp(44)));
   Button down=new Button(this);down.setText("↓");down.setTextSize(15);down.setAllCaps(false);
   down.setEnabled(index<manualOrder.size()-1);down.setOnClickListener(v->{
    Collections.swap(manualOrder,index,index+1);renderOrderRows();
   });
   line.addView(down,new LinearLayout.LayoutParams(dp(42),dp(44)));
   orderRows.addView(line);
  }
 }
 private ArrayList<String> displayLabels(){
  ArrayList<String> labels=new ArrayList<>();
  for(String account:accounts)labels.add(AppData.displayName(this,account));
  return labels;
 }
 private TextView label(String value,int size,int color){
  TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);
  t.setPadding(0,dp(8),0,dp(8));return t;
 }
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
}
