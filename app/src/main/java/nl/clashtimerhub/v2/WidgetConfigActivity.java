package nl.clashtimerhub.v2;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import java.util.*;

/** Explicit per-widget configuration; show all accounts by default. */
public final class WidgetConfigActivity extends Activity {
 private int widgetId=AppWidgetManager.INVALID_APPWIDGET_ID;
 private final LinkedHashMap<String,CheckBox> choices=new LinkedHashMap<>();
 private RadioButton all,custom;
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  setResult(RESULT_CANCELED);
  AppData.migrate(this);
  widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
  if(widgetId==AppWidgetManager.INVALID_APPWIDGET_ID){finish();return;}
  getWindow().setStatusBarColor(0xff171025);
  getWindow().setNavigationBarColor(0xff171025);
  ScrollView scroll=new ScrollView(this);
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(20),dp(24),dp(20),dp(28));root.setBackgroundColor(0xff171025);
  scroll.addView(root);setContentView(scroll);
  TextView title=text("⚔ Widget instellen",24,0xffffd783);root.addView(title);
  root.addView(text("Je ziet altijd alle geselecteerde accounts in één lijst. Standaard tonen we ze allemaal.",14,0xffcdbbdc));
  RadioGroup mode=new RadioGroup(this);mode.setOrientation(1);root.addView(mode);
  all=new RadioButton(this);all.setText(I18n.tr(this,"Alle accounts tonen"));all.setTextColor(Color.WHITE);mode.addView(all);
  custom=new RadioButton(this);custom.setText(I18n.tr(this,"Alleen geselecteerde accounts"));custom.setTextColor(Color.WHITE);mode.addView(custom);
  root.addView(text("ACCOUNTS",15,0xffffd783));
  LinearLayout list=new LinearLayout(this);list.setOrientation(1);root.addView(list);
  try{
   JSONArray accounts=new JSONArray(AppData.prefs(this).getString("accounts","[]"));
   for(int i=0;i<accounts.length();i++){
    String ac=accounts.optString(i);if(ac.isEmpty())continue;
    CheckBox box=new CheckBox(this);box.setText(ac);box.setTextColor(Color.WHITE);box.setChecked(true);
    choices.put(ac,box);list.addView(box);
   }
  }catch(Exception ex){android.util.Log.e("ClashTimerHub","Widget config accounts failed",ex);}
  if(choices.isEmpty())root.addView(text("Nog geen accounts gevonden. Synchroniseer eerst je Clash-data in de app.",13,0xffa9a0b7));
  android.content.SharedPreferences prefs=getSharedPreferences("WidgetPreferences",MODE_PRIVATE);
  boolean showAll=prefs.getBoolean("all_"+widgetId,true);
  Set<String> selected=new HashSet<>();
  try{JSONArray values=new JSONArray(prefs.getString("selected_"+widgetId,"[]"));for(int i=0;i<values.length();i++)selected.add(values.optString(i));}catch(Exception ignored){}
  for(Map.Entry<String,CheckBox> entry:choices.entrySet())entry.getValue().setChecked(showAll||selected.isEmpty()||selected.contains(entry.getKey()));
  all.setChecked(showAll);custom.setChecked(!showAll);
  Runnable toggle=()->{for(CheckBox cb:choices.values())cb.setEnabled(custom.isChecked());};
  mode.setOnCheckedChangeListener((g,id)->toggle.run());toggle.run();
  Button save=new Button(this);save.setAllCaps(false);save.setText(I18n.tr(this,"✓ Widget opslaan"));
  save.setTextColor(Color.WHITE);save.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff7652a3));root.addView(save);
  save.setOnClickListener(v->{
   JSONArray arr=new JSONArray();
   for(Map.Entry<String,CheckBox> item:choices.entrySet())if(item.getValue().isChecked())arr.put(item.getKey());
   if(custom.isChecked()&&arr.length()==0){Toast.makeText(this,I18n.tr(this,"Selecteer minstens één account"),Toast.LENGTH_LONG).show();return;}
   prefs.edit().putBoolean("all_"+widgetId,all.isChecked()).putString("selected_"+widgetId,arr.toString()).apply();
   TimerWidget.update(this);
   Intent result=new Intent();result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId);
   setResult(RESULT_OK,result);finish();
  });
 }
 private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(I18n.tr(this,value));t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(9),0,dp(9));return t;}
 private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
}
