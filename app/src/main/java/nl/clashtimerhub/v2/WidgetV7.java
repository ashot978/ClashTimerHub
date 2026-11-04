package nl.clashtimerhub.v2;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.*;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;
import org.json.*;
import java.util.*;

/** Shared presentation engine for V7 multi, account, and mini home widgets. */
final class WidgetV7 {
 static final int MULTI=0, SINGLE=1, MINI=2, SYNC=3;
 private WidgetV7(){}
 static final class Status {
  String account;
  int builderActive,builderReady,labActive,petActive;
  long builderNext=Long.MAX_VALUE,labNext=Long.MAX_VALUE,petNext=Long.MAX_VALUE;
  Status(String account){this.account=account;}
 }
 static int kind(Context ctx,int id){
  AppWidgetProviderInfo p=AppWidgetManager.getInstance(ctx).getAppWidgetInfo(id);
  if(p==null||p.provider==null)return MULTI;
  String name=p.provider.getClassName();
  if(name.equals(MiniWidget.class.getName()))return MINI;
  if(name.equals(SyncWidget.class.getName()))return SYNC;
  if(name.equals(AccountWidget.class.getName()))return SINGLE;
  return MULTI;
 }
 static int[] allIds(Context ctx){
  AppWidgetManager manager=AppWidgetManager.getInstance(ctx);
  ArrayList<Integer> ids=new ArrayList<>();
  Class<?>[] providers={TimerWidget.class,MiniWidget.class,AccountWidget.class,SyncWidget.class};
  for(Class<?> provider:providers)for(int id:manager.getAppWidgetIds(new android.content.ComponentName(ctx,provider)))ids.add(id);
  int[] result=new int[ids.size()];for(int i=0;i<result.length;i++)result[i]=ids.get(i);
  return result;
 }
 private static ArrayList<Status> read(Context ctx,long now){
  AppData.migrate(ctx);
  android.content.SharedPreferences prefs=AppData.prefs(ctx);
  LinkedHashMap<String,Status> map=new LinkedHashMap<>();
  try{
   JSONArray accounts=new JSONArray(prefs.getString("accounts","[]"));
   for(int i=0;i<accounts.length();i++){
    String account=accounts.optString(i,"");if(!account.isEmpty())map.put(account,new Status(account));
   }
   JSONArray upgrades=new JSONArray(prefs.getString("upgrades","[]"));
   for(int i=0;i<upgrades.length();i++){
    JSONObject o=upgrades.optJSONObject(i);if(o==null)continue;
    Status s=map.get(o.optString("account"));if(s==null)continue;
    String category=o.optString("category");
    long finish=o.optLong("finish");
    if("Bouwers".equals(category)){
     if(finish<=now){
      if(finish>prefs.getLong("ack_"+s.account,0))s.builderReady++;
     }else{
      s.builderActive++;s.builderNext=Math.min(s.builderNext,finish);
     }
    }else if(finish>now&&"Laboratorium".equals(category)){
     s.labActive++;s.labNext=Math.min(s.labNext,finish);
    }else if(finish>now&&"Pets".equals(category)){
     s.petActive++;s.petNext=Math.min(s.petNext,finish);
    }
   }
  }catch(Exception e){android.util.Log.e("ClashTimerHub","Widget account data error",e);}
  ArrayList<Status> list=new ArrayList<>(map.values());
  Collections.sort(list,(a,b)->{
   if(a.builderReady!=b.builderReady)return Integer.compare(b.builderReady,a.builderReady);
   return Long.compare(a.builderNext,b.builderNext);
  });
  return list;
 }
 private static String finish(Context ctx,long when,boolean shortForm){
  if(when==Long.MAX_VALUE)return "—";
  Calendar now=Calendar.getInstance(),date=Calendar.getInstance();
  date.setTimeInMillis(when);
  Locale loc=I18n.english(ctx)?Locale.UK:Locale.forLanguageTag("nl-NL");
  String clock=new java.text.SimpleDateFormat("HH:mm",loc).format(new Date(when));
  boolean today=now.get(Calendar.YEAR)==date.get(Calendar.YEAR)&&now.get(Calendar.DAY_OF_YEAR)==date.get(Calendar.DAY_OF_YEAR);
  now.add(Calendar.DAY_OF_YEAR,1);
  boolean tomorrow=now.get(Calendar.YEAR)==date.get(Calendar.YEAR)&&now.get(Calendar.DAY_OF_YEAR)==date.get(Calendar.DAY_OF_YEAR);
  if(shortForm)return (today?"":tomorrow?I18n.english(ctx)?"Tmr ":"Mrg ":new java.text.SimpleDateFormat("d/M ",loc).format(new Date(when)))+clock;
  if(today)return I18n.english(ctx)?"Today "+clock:"Vandaag "+clock;
  if(tomorrow)return I18n.english(ctx)?"Tomorrow "+clock:"Morgen "+clock;
  return new java.text.SimpleDateFormat("d MMM HH:mm",loc).format(new Date(when));
 }
 private static void timer(RemoteViews rv,int chrono,int idle,long finish,long now){
  boolean present=finish!=Long.MAX_VALUE;
  rv.setViewVisibility(chrono,present?View.VISIBLE:View.GONE);
  rv.setViewVisibility(idle,present?View.GONE:View.VISIBLE);
  if(present){
   rv.setChronometer(chrono,SystemClock.elapsedRealtime()+Math.max(0,finish-now),null,true);
   rv.setChronometerCountDown(chrono,true);
  }else rv.setTextViewText(idle,"—");
 }
 private static void showMax(RemoteViews view,Context ctx,Status status,String category,int idleId,long next){
  if(next==Long.MAX_VALUE&&AppData.isMaxed(ctx,status.account,category)){
   view.setTextViewText(idleId,"✓");
   view.setTextColor(idleId,0xff8BE3AD);
  }
 }
 private static PendingIntent accountClick(Context ctx,String account,int id){
  Intent intent=new Intent(ctx,MainActivity.class);
  intent.setAction("nl.clashtimerhub.v2.OPEN_WIDGET_ACCOUNT."+id+"."+account);
  intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
  intent.putExtra("openAccount",account);
  return PendingIntent.getActivity(ctx,Math.abs((id+":"+account).hashCode()),intent,
    PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 private static PendingIntent settingsClick(Context ctx,int id){
  Intent intent=new Intent(ctx,WidgetConfigActivity.class);
  intent.setAction("nl.clashtimerhub.v2.CONFIG_"+id);
  intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
  return PendingIntent.getActivity(ctx,120000+id,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 private static PendingIntent syncClick(Context ctx,int id){
  Intent intent=new Intent(ctx,WidgetSyncReceiver.class);
  intent.setAction(WidgetSyncReceiver.TOGGLE);
  intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
  return PendingIntent.getBroadcast(ctx,130000+id,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 private static RemoteViews multiRow(Context ctx,Status s,int mode,long now,int id){
  if(mode==2||mode==3){
   RemoteViews row=new RemoteViews(ctx.getPackageName(),mode==3?R.layout.widget_v7_wide_row_dense:R.layout.widget_v7_wide_row);
   row.setTextViewText(R.id.v7_wide_name,AppData.displayName(ctx,s.account));
   row.setTextViewText(R.id.v7_wide_builders,s.builderReady+"✓ "+s.builderActive+"⚒");
   timer(row,R.id.v7_wide_lab_timer,R.id.v7_wide_lab_idle,s.labNext,now);
   timer(row,R.id.v7_wide_pet_timer,R.id.v7_wide_pet_idle,s.petNext,now);
   timer(row,R.id.v7_wide_builder_timer,R.id.v7_wide_builder_idle,s.builderNext,now);
   showMax(row,ctx,s,"Laboratorium",R.id.v7_wide_lab_idle,s.labNext);
   showMax(row,ctx,s,"Pets",R.id.v7_wide_pet_idle,s.petNext);
   showMax(row,ctx,s,"Bouwers",R.id.v7_wide_builder_idle,s.builderNext);
   row.setTextViewText(R.id.v7_wide_finish,finish(ctx,s.builderNext,true));
   row.setOnClickPendingIntent(R.id.widget_account_root,accountClick(ctx,s.account,id));
   return row;
  }
  boolean medium=mode==1;
  RemoteViews row=new RemoteViews(ctx.getPackageName(),medium?R.layout.widget_account_compact:R.layout.widget_account_mini);
  row.setTextViewText(R.id.widget_account_name,AppData.displayName(ctx,s.account));
  row.setViewVisibility(R.id.widget_ready,s.builderReady>0?View.VISIBLE:View.GONE);
  if(s.builderReady>0)row.setTextViewText(R.id.widget_ready,"●"+s.builderReady);
  timer(row,R.id.widget_timer,R.id.widget_idle,s.builderNext,now);
  if(medium){
   String lab=s.labActive>0?String.valueOf(s.labActive):AppData.isMaxed(ctx,s.account,"Laboratorium")?"✓":"0";
   String pets=s.petActive>0?String.valueOf(s.petActive):AppData.isMaxed(ctx,s.account,"Pets")?"✓":"0";
   row.setTextViewText(R.id.widget_subtitle,"⚗ "+lab+"  🐾 "+pets+"  ·  "+finish(ctx,s.builderNext,true));
  }
  row.setOnClickPendingIntent(R.id.widget_account_root,accountClick(ctx,s.account,id));
  return row;
 }
 private static RemoteViews multi(Context ctx,int id,List<Status> visible,long now){
  AppWidgetManager manager=AppWidgetManager.getInstance(ctx);
  android.os.Bundle opts=manager.getAppWidgetOptions(id);
  int width=190,height=240;
  if(opts!=null){
   boolean landscape=ctx.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
   width=opts.getInt(landscape?AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH:AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,190);
   height=opts.getInt(landscape?AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT:AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,240);
   if(width<=0)width=190;if(height<=0)height=240;
  }
  // Use dense rows when needed, then fit as many accounts as the launcher allows.
  // A one-cell widget has room for its header and a timer, not the sync footer.
  int rows=Math.max(1,visible.size());
  int mode;
  if(width>=510)mode=height<44+rows*24?3:2;
  else if(width>=225&&height>=44+rows*34)mode=1;
  else mode=0;
  int rowHeight=mode==3?16:mode==2?24:mode==1?34:19;
  boolean showSync=height>=44+rowHeight;
  int available=Math.max(0,height-24-(showSync?20:0));
  int count=Math.min(visible.size(),Math.max(1,available/rowHeight));
  int hidden=visible.size()-count;
  RemoteViews w=new RemoteViews(ctx.getPackageName(),R.layout.widget_v7_multi);
  boolean narrow=width<230;
  int ready=0;for(Status s:visible)ready+=s.builderReady;
  w.setTextViewText(R.id.v7_multi_title,narrow?("⚔ TIMERS"+(hidden>0?" +"+hidden:"")):"⚔ CLASH TIMER HUB");
  w.setViewVisibility(R.id.v7_multi_summary,narrow?View.GONE:View.VISIBLE);
  w.setTextViewText(R.id.v7_multi_summary,(hidden>0?count+"/"+visible.size():String.valueOf(visible.size()))+" · "+ready+"✓");
  w.removeAllViews(R.id.v7_multi_rows);
  if(visible.isEmpty())w.addView(R.id.v7_multi_rows,new RemoteViews(ctx.getPackageName(),R.layout.widget_empty));
  else for(int i=0;i<count;i++)w.addView(R.id.v7_multi_rows,multiRow(ctx,visible.get(i),mode,now,id));
  w.setViewVisibility(R.id.v7_multi_sync,showSync?View.VISIBLE:View.GONE);
  boolean active=AppData.prefs(ctx).getBoolean("floatingEnabled",false);
  w.setTextViewText(R.id.v7_multi_sync,I18n.english(ctx)?("⚔ Sync "+(active?"ON":"OFF")):("⚔ Sync "+(active?"AAN":"UIT")));
  w.setOnClickPendingIntent(R.id.v7_multi_sync,syncClick(ctx,id));
  w.setOnClickPendingIntent(R.id.v7_multi_settings,settingsClick(ctx,id));
  Intent launch=new Intent(ctx,MainActivity.class);
  w.setOnClickPendingIntent(R.id.v7_multi_title,PendingIntent.getActivity(ctx,140000+id,launch,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  return w;
 }
 private static RemoteViews mini(Context ctx,int id,Status s,long now){
  RemoteViews view=new RemoteViews(ctx.getPackageName(),R.layout.widget_v7_mini);
  view.setTextViewText(R.id.v7_mini_title,s==null?"Clash":AppData.displayName(ctx,s.account));
  if(s==null){
   timer(view,R.id.v7_mini_timer,R.id.v7_mini_idle,Long.MAX_VALUE,now);
   view.setTextViewText(R.id.v7_mini_finish,"");
   view.setTextViewText(R.id.v7_mini_status,I18n.english(ctx)?"No data":"Geen data");
  }else{
   timer(view,R.id.v7_mini_timer,R.id.v7_mini_idle,s.builderNext,now);
   view.setTextViewText(R.id.v7_mini_finish,finish(ctx,s.builderNext,true));
   view.setTextViewText(R.id.v7_mini_status,s.builderReady+" ✓  ·  "+s.builderActive+" ⚒");
   view.setTextColor(R.id.v7_mini_status,s.builderReady>0?0xff8BE3AD:0xffCBB5D9);
   view.setOnClickPendingIntent(R.id.v7_mini_body,accountClick(ctx,s.account,id));
   view.setOnClickPendingIntent(R.id.v7_mini_title,accountClick(ctx,s.account,id));
  }
  view.setOnClickPendingIntent(R.id.v7_mini_settings,settingsClick(ctx,id));
  return view;
 }
 private static RemoteViews sync(Context ctx,int id){
  RemoteViews view=new RemoteViews(ctx.getPackageName(),R.layout.widget_v7_sync);
  boolean active=AppData.prefs(ctx).getBoolean("floatingEnabled",false);
  view.setTextViewText(R.id.v7_sync_status,active?"SYNC ON":"SYNC OFF");
  view.setTextColor(R.id.v7_sync_status,active?0xff8BE3AD:0xffCBB5D9);
  view.setTextViewText(R.id.v7_sync_hint,
   I18n.english(ctx)?(active?"Tap to turn off":"Tap to turn on"):(active?"Tik om uit te zetten":"Tik om aan te zetten"));
  view.setInt(R.id.v7_sync_symbol,"setBackgroundResource",active?R.drawable.widget_sync_ring_on:R.drawable.widget_sync_ring_off);
  view.setOnClickPendingIntent(R.id.v7_sync_root,syncClick(ctx,id));
  return view;
 }
 private static RemoteViews single(Context ctx,int id,Status s,long now){
  RemoteViews w=new RemoteViews(ctx.getPackageName(),R.layout.widget_v7_account);
  w.setTextViewText(R.id.v7_single_name,s==null?"Clash Timer Hub":AppData.displayName(ctx,s.account));
  w.setOnClickPendingIntent(R.id.v7_single_settings,settingsClick(ctx,id));
  if(s==null){
   timer(w,R.id.v7_single_builder_timer,R.id.v7_single_builder_idle,Long.MAX_VALUE,now);
   timer(w,R.id.v7_single_lab_timer,R.id.v7_single_lab_idle,Long.MAX_VALUE,now);
   timer(w,R.id.v7_single_pet_timer,R.id.v7_single_pet_idle,Long.MAX_VALUE,now);
  }
  if(s!=null){
   w.setTextViewText(R.id.v7_single_builders,s.builderReady+"✓  "+s.builderActive+"⚒");
   w.setTextViewText(R.id.v7_single_lab_count,"⚗ "+s.labActive);
   w.setTextViewText(R.id.v7_single_pet_count,"🐾 "+s.petActive);
   timer(w,R.id.v7_single_builder_timer,R.id.v7_single_builder_idle,s.builderNext,now);
   timer(w,R.id.v7_single_lab_timer,R.id.v7_single_lab_idle,s.labNext,now);
   timer(w,R.id.v7_single_pet_timer,R.id.v7_single_pet_idle,s.petNext,now);
   showMax(w,ctx,s,"Bouwers",R.id.v7_single_builder_idle,s.builderNext);
   showMax(w,ctx,s,"Laboratorium",R.id.v7_single_lab_idle,s.labNext);
   showMax(w,ctx,s,"Pets",R.id.v7_single_pet_idle,s.petNext);
   w.setTextViewText(R.id.v7_single_builder_finish,finish(ctx,s.builderNext,true));
   w.setTextViewText(R.id.v7_single_lab_finish,finish(ctx,s.labNext,true));
   w.setTextViewText(R.id.v7_single_pet_finish,finish(ctx,s.petNext,true));
   w.setOnClickPendingIntent(R.id.v7_single_name,accountClick(ctx,s.account,id));
  }
  boolean active=AppData.prefs(ctx).getBoolean("floatingEnabled",false);
  w.setTextViewText(R.id.v7_single_sync,I18n.english(ctx)?"⚔ Sync "+(active?"ON":"OFF"):"⚔ Sync "+(active?"AAN":"UIT"));
  w.setOnClickPendingIntent(R.id.v7_single_sync,syncClick(ctx,id));
  return w;
 }
 static void updateAll(Context ctx){
  long now=System.currentTimeMillis();
  ArrayList<Status> data=read(ctx,now);
  AppWidgetManager manager=AppWidgetManager.getInstance(ctx);
  android.content.SharedPreferences preferences=ctx.getSharedPreferences("WidgetPreferences",Context.MODE_PRIVATE);
  Class<?>[] providers={TimerWidget.class,AccountWidget.class,MiniWidget.class,SyncWidget.class};
  for(int kind=0;kind<providers.length;kind++){
   for(int id:manager.getAppWidgetIds(new android.content.ComponentName(ctx,providers[kind]))){
    try{
     Status selected=null;
     String saved=preferences.getString("account_"+id,"");
     for(Status s:data)if(s.account.equals(saved))selected=s;
     if(selected==null&&!data.isEmpty())selected=data.get(0);
     RemoteViews view;
     if(kind==SINGLE)view=single(ctx,id,selected,now);
     else if(kind==MINI)view=mini(ctx,id,selected,now);
     else if(kind==SYNC)view=sync(ctx,id);
     else{
      boolean all=preferences.getBoolean("all_"+id,true);
      HashSet<String> selectedNames=new HashSet<>();
      try{
       JSONArray arr=new JSONArray(preferences.getString("selected_"+id,"[]"));
       for(int j=0;j<arr.length();j++)selectedNames.add(arr.optString(j));
      }catch(Exception ignored){}
      ArrayList<Status> visible=new ArrayList<>();
      for(Status s:data)if(all||selectedNames.contains(s.account))visible.add(s);
      view=multi(ctx,id,visible,now);
     }
     manager.updateAppWidget(id,view);
    }catch(RuntimeException e){android.util.Log.e("ClashTimerHub","V7 widget "+id+" update error",e);}
   }
  }
 }
}
