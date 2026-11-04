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
 private Spinner accountPicker;
 private ArrayList<String> accounts=new ArrayList<>();
 private int kind=WidgetV7.MULTI;
 @Override public void onCreate(Bundle state){
  super.onCreate(state);setResult(RESULT_CANCELED);
  AppData.migrate(this);
  widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
  if(widgetId==AppWidgetManager.INVALID_APPWIDGET_ID){finish();return;}
  kind=WidgetV7.kind(this,widgetId);
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
    CheckBox box=new CheckBox(this);box.setText(name);box.setTextColor(Color.WHITE);
    box.setChecked(showAll||chosen.contains(name));boxes.put(name,box);root.addView(box);
   }
   all.setChecked(showAll);custom.setChecked(!showAll);
   Runnable toggle=()->{for(CheckBox box:boxes.values())box.setEnabled(custom.isChecked());};
   mode.setOnCheckedChangeListener((g,id)->toggle.run());toggle.run();
  }else{
   root.addView(label(I18n.english(this)?"Choose one account for this widget.":"Kies één account voor deze widget.",14,0xffcdbbdc));
   accountPicker=new Spinner(this);
   ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,accounts);
   accountPicker.setAdapter(adapter);
   String saved=prefs.getString("account_"+widgetId,"");
   int chosen=accounts.indexOf(saved);
   accountPicker.setSelection(Math.max(0,chosen));root.addView(accountPicker);
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
   }else editor.putString("account_"+widgetId,accounts.get(accountPicker.getSelectedItemPosition()));
   editor.apply();TimerWidget.update(this);
   Intent result=new Intent();result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId);
   setResult(RESULT_OK,result);finish();
  });
 }
 private TextView label(String value,int size,int color){
  TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);
  t.setPadding(0,dp(8),0,dp(8));return t;
 }
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
}
