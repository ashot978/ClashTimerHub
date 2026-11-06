package nl.clashtimerhub.v2;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;

/** An optional multi-account home-screen view of Research Potion forecasts. */
public final class ResearchBoostWidget extends AppWidgetProvider {
 private static final String ADD_HOUR="nl.clashtimerhub.v2.RESEARCH_ADD_HOUR";
 private static final String REFRESH="nl.clashtimerhub.v2.RESEARCH_REFRESH";
 private static final int REFRESH_REQUEST=92010;
 private static PendingIntent addHour(Context ctx,int id,String account){
  Intent intent=new Intent(ctx,ResearchBoostWidget.class);
  intent.setAction(ADD_HOUR).putExtra("account",account);
  return PendingIntent.getBroadcast(ctx,(id+":"+account+":potion").hashCode(),intent,
    PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 private static PendingIntent edit(Context ctx,int id,String account){
  Intent intent=new Intent(ctx,ResearchBoostEditorActivity.class);
  intent.setAction("nl.clashtimerhub.v2.RESEARCH_EDIT_"+id+"_"+account);
  intent.putExtra("account",account);
  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
  return PendingIntent.getActivity(ctx,(id+":"+account+":edit").hashCode(),intent,
    PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 private static boolean accountExists(Context ctx,String account){
  try{
   JSONArray values=new JSONArray(AppData.prefs(ctx).getString("accounts","[]"));
   for(int i=0;i<values.length();i++)if(account.equals(values.optString(i)))return true;
  }catch(Exception ignored){}
  return false;
 }
 private static String savedLabel(long ms){
  long m=Math.max(1,(ms+30000L)/60000L);
  return m>=60?(m/60)+"h"+String.format(Locale.ROOT,"%02d",m%60):m+"m";
 }
 private static String endTime(Context ctx,long when,long now){
  if(when==Long.MAX_VALUE)return "—";
  Calendar date=Calendar.getInstance(),today=Calendar.getInstance();
  date.setTimeInMillis(when);today.setTimeInMillis(now);
  Locale loc=I18n.english(ctx)?Locale.UK:Locale.forLanguageTag("nl-NL");
  String hm=new java.text.SimpleDateFormat("HH:mm",loc).format(new Date(when));
  if(today.get(Calendar.YEAR)==date.get(Calendar.YEAR)&&today.get(Calendar.DAY_OF_YEAR)==date.get(Calendar.DAY_OF_YEAR))return hm;
  today.add(Calendar.DAY_OF_YEAR,1);
  if(today.get(Calendar.YEAR)==date.get(Calendar.YEAR)&&today.get(Calendar.DAY_OF_YEAR)==date.get(Calendar.DAY_OF_YEAR))
   return (I18n.english(ctx)?"Tmr ":"Mrg ")+hm;
  return new java.text.SimpleDateFormat("d/M HH:mm",loc).format(new Date(when));
 }
 static void update(Context ctx){
  AppWidgetManager mgr=AppWidgetManager.getInstance(ctx);
  int[] ids=mgr.getAppWidgetIds(new ComponentName(ctx,ResearchBoostWidget.class));
  if(ids.length==0){schedule(ctx,Long.MAX_VALUE);return;}
  long now=System.currentTimeMillis(),next=Long.MAX_VALUE;
  try{
   ArrayList<String> accounts=WidgetV7.boostAccountOrder(ctx);
   JSONArray upgrades=new JSONArray(AppData.prefs(ctx).getString("upgrades","[]"));
   for(int id:ids){
    RemoteViews rv=new RemoteViews(ctx.getPackageName(),R.layout.widget_builder_boost_multi);
    rv.removeAllViews(R.id.boost_widget_rows);
    Bundle options=mgr.getAppWidgetOptions(id);
    int h=options==null?310:options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,310);
    if(h<=0)h=310;
    int maximum=Math.max(1,(h-62)/32);
    int shown=0;
    for(int i=0;i<accounts.size()&&shown<maximum;i++){
     String account=accounts.get(i);
     if(account.isEmpty())continue;
     shown++;
     ResearchPotionTracker.Snapshot snapshot=ResearchPotionTracker.load(ctx,account);
     long left=snapshot.remaining(now),soonest=Long.MAX_VALUE,earliestOriginal=Long.MAX_VALUE;
     int ready=0;
     for(int j=0;j<upgrades.length();j++){
      JSONObject item=upgrades.optJSONObject(j);
      if(item==null||!account.equals(item.optString("account"))
        ||!"Laboratorium".equals(item.optString("category")))continue;
      long sourceFinish=item.optLong("finish",0L);
      if(sourceFinish>now&&sourceFinish<earliestOriginal)earliestOriginal=sourceFinish;
      long predicted=ResearchPotionTracker.estimateFinish(snapshot,sourceFinish);
      if(predicted>now&&predicted<soonest)soonest=predicted;
      if(predicted<=now&&predicted>AppData.prefs(ctx).getLong("ack_"+account,0L))ready++;
     }
     if(soonest>now&&soonest<next)next=soonest;
     long boostEnd=snapshot.boostEnd(now);
     if(boostEnd<next)next=boostEnd;
     RemoteViews row=new RemoteViews(ctx.getPackageName(),R.layout.widget_builder_boost_row);
     row.setTextViewText(R.id.boost_name,AppData.displayName(ctx,account));
     row.setViewVisibility(R.id.boost_remaining,left>0?View.VISIBLE:View.GONE);
     row.setViewVisibility(R.id.boost_idle,left>0?View.GONE:View.VISIBLE);
     if(left>0){
      row.setChronometer(R.id.boost_remaining,SystemClock.elapsedRealtime()+left,null,true);
      row.setChronometerCountDown(R.id.boost_remaining,true);
     }
     row.setTextViewText(R.id.boost_idle,!snapshot.valid?"⚡ !":"⚡ —");
     row.setTextViewText(R.id.boost_plus60,I18n.english(ctx)?"+1h":"+1u");
     String end=endTime(ctx,soonest,now);
     if(soonest==Long.MAX_VALUE&&ready>0)end="✓ "+ready;
     if(soonest!=Long.MAX_VALUE&&earliestOriginal>soonest&&snapshot.valid)
      end+="  −"+savedLabel(earliestOriginal-soonest);
     row.setTextViewText(R.id.boost_finish,end);
     row.setOnClickPendingIntent(R.id.boost_name,edit(ctx,id,account));
     row.setOnClickPendingIntent(R.id.boost_remaining,edit(ctx,id,account));
     row.setOnClickPendingIntent(R.id.boost_idle,edit(ctx,id,account));
     row.setOnClickPendingIntent(R.id.boost_finish,edit(ctx,id,account));
     row.setOnClickPendingIntent(R.id.boost_plus60,addHour(ctx,id,account));
     rv.addView(R.id.boost_widget_rows,row);
    }
    if(shown==0)rv.addView(R.id.boost_widget_rows,new RemoteViews(ctx.getPackageName(),R.layout.widget_empty));
    rv.setTextViewText(R.id.boost_widget_title,I18n.english(ctx)?"⚡ Research Potion · 24×":"⚡ Research Potion · 24×");
    rv.setTextViewText(R.id.boost_widget_footer,I18n.english(ctx)?
     "≡ All accounts · tap account to edit boost":
     "≡ Alle accounts · tik om boost te bewerken");
    Intent showAll=new Intent(ctx,ResearchBoostEditorActivity.class);
    showAll.setAction("nl.clashtimerhub.v2.RESEARCH_OVERVIEW_"+id);
    showAll.putExtra("overview",true);
    PendingIntent overview=PendingIntent.getActivity(ctx,810000+id,showAll,
     PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    rv.setOnClickPendingIntent(R.id.boost_widget_open,overview);
    rv.setOnClickPendingIntent(R.id.boost_widget_title,overview);
    rv.setOnClickPendingIntent(R.id.boost_widget_footer,overview);
    mgr.updateAppWidget(id,rv);
   }
   schedule(ctx,next);
  }catch(Exception ex){android.util.Log.e("ClashTimerHub","Research Boost widget failed",ex);}
 }
 private static PendingIntent refreshIntent(Context ctx){
  Intent intent=new Intent(ctx,ResearchBoostWidget.class).setAction(REFRESH);
  return PendingIntent.getBroadcast(ctx,REFRESH_REQUEST,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 private static void schedule(Context ctx,long next){
  AlarmManager manager=(AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
  if(manager==null)return;
  PendingIntent operation=refreshIntent(ctx);
  manager.cancel(operation);
  if(next==Long.MAX_VALUE)return;
  try{
   long when=Math.max(System.currentTimeMillis()+1000,next+1000);
   if(Build.VERSION.SDK_INT>=23){
    if(Build.VERSION.SDK_INT<31||manager.canScheduleExactAlarms())
     manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,operation);
    else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,operation);
   }else manager.set(AlarmManager.RTC_WAKEUP,when,operation);
  }catch(Exception e){android.util.Log.w("ClashTimerHub","Boost refresh unavailable",e);}
 }
 @Override public void onUpdate(Context ctx,AppWidgetManager manager,int[] ids){update(ctx);}
 @Override public void onAppWidgetOptionsChanged(Context ctx,AppWidgetManager manager,int id,Bundle options){
  super.onAppWidgetOptionsChanged(ctx,manager,id,options);update(ctx);
 }
 @Override public void onDeleted(Context ctx,int[] ids){super.onDeleted(ctx,ids);update(ctx);}
 @Override public void onReceive(Context ctx,Intent intent){
  super.onReceive(ctx,intent);
  if(intent==null)return;
  if(REFRESH.equals(intent.getAction())){update(ctx);return;}
  if(ADD_HOUR.equals(intent.getAction())){
   String account=intent.getStringExtra("account");
   if(account==null||!accountExists(ctx,account))return;
   try{
    ResearchPotionTracker.addHour(ctx,account);
    TimerWidget.update(ctx);
    android.widget.Toast.makeText(ctx,AppData.displayName(ctx,account)+
     (I18n.english(ctx)?" · Research Potion +1h":" · Research Potion +1u"),
     android.widget.Toast.LENGTH_SHORT).show();
   }catch(Exception e){
    android.widget.Toast.makeText(ctx,e.getMessage(),android.widget.Toast.LENGTH_LONG).show();
   }
  }
 }
}