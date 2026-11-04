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
  if(shortForm)return (today?"":tomorrow?I18n.english(ctx)?"Tomorrow ":"Morgen ":new java.text.SimpleDateFormat("d MMM ",loc).format(new Date(when)))+clock;
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

 /** Exact per-widget column widths, computed once from the longest value in each
  * column across EVERY visible account. No per-row shifting or abbreviated timers. */
 private static final class Columns {
  float name=0,builders=0,lab=0,pet=0,next=0,finish=0;
  float singleLine(){return name+builders+lab+pet+next+finish+12f;}
  float twoLine(){return Math.max(name+builders+next,lab+pet+finish)+12f;}
  float threeLine(){return Math.max(name+builders,Math.max(lab+pet,next+finish))+12f;}
  float fiveLine(){return Math.max(name+builders,Math.max(lab,Math.max(pet,Math.max(next,finish))))+12f;}
 }
 private static float textDp(Context ctx,String text,float sp,boolean bold){
  android.util.DisplayMetrics m=ctx.getResources().getDisplayMetrics();
  android.graphics.Paint p=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
  p.setTextSize(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,sp,m));
  if(bold)p.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD));
  return p.measureText(text)/m.density;
 }
 private static String remainingForColumn(long when,long now){
  if(when==Long.MAX_VALUE)return "✓";
  long hours=Math.max(0,(when-now+999)/1000/3600);
  // The currently running chronometer can require two extra seconds digits.
  return hours>0?hours+":59:59":"59:59";
 }
 private static Columns sharedColumns(Context ctx,List<Status> values,long now,int fontSize){
  Columns c=new Columns();
  float boost=fontSize==2?0.5f:fontSize==0?-0.5f:0f;
  for(Status item:values){
   c.name=Math.max(c.name,textDp(ctx,AppData.displayName(ctx,item.account),11f+boost,true)+3f);
   c.builders=Math.max(c.builders,textDp(ctx,item.builderReady+"✓ "+item.builderActive+"⚒",10f,false)+4f);
   String lab=remainingForColumn(item.labNext,now);
   String pet=remainingForColumn(item.petNext,now);
   c.lab=Math.max(c.lab,textDp(ctx,"🧙 "+lab,10f,false)+6f);
   c.pet=Math.max(c.pet,textDp(ctx,"🐾 "+pet,10f,false)+6f);
   c.next=Math.max(c.next,textDp(ctx,remainingForColumn(item.builderNext,now),11f+boost,false)+6f);
   c.finish=Math.max(c.finish,textDp(ctx,finish(ctx,item.builderNext,true),10f,false)+5f);
  }
  return c;
 }
 private static void setColumnWidth(RemoteViews row,int viewId,float dp){
  if(android.os.Build.VERSION.SDK_INT>=31)
   row.setViewLayoutWidth(viewId,(float)Math.ceil(dp),android.util.TypedValue.COMPLEX_UNIT_DIP);
 }
 private static void alignColumns(RemoteViews row,Columns c){
  setColumnWidth(row,R.id.v7_wide_name,c.name);
  setColumnWidth(row,R.id.v7_wide_builders,c.builders);
  setColumnWidth(row,R.id.v7_wide_lab_group,c.lab);
  setColumnWidth(row,R.id.v7_wide_pet_group,c.pet);
  setColumnWidth(row,R.id.v7_wide_builder_timer,c.next);
  setColumnWidth(row,R.id.v7_wide_builder_idle,c.next);
  setColumnWidth(row,R.id.v7_wide_finish,c.finish);
 }
 private static RemoteViews multiRow(Context ctx,Status s,int mode,long now,int id,boolean showLab,boolean showPet,int size,int background,Columns columns){
  if(mode>=2&&mode<=6){
   int layout=mode==6?R.layout.widget_v7_five_line_row:
    mode==5?R.layout.widget_v7_three_line_row:
    mode==4?R.layout.widget_v7_two_line_row:R.layout.widget_v7_wide_row;
   RemoteViews row=new RemoteViews(ctx.getPackageName(),layout);
   row.setInt(R.id.widget_account_root,"setBackgroundResource",rowTheme(background));
   row.setTextViewText(R.id.v7_wide_name,AppData.displayName(ctx,s.account));
   alignColumns(row,columns);
   row.setViewVisibility(R.id.v7_wide_lab_group,showLab?View.VISIBLE:View.GONE);
   row.setViewVisibility(R.id.v7_wide_pet_group,showPet?View.VISIBLE:View.GONE);
   // Comfort enlarges only the primary name and next-builder countdown.
   // Secondary countdowns stay at their readable base size to avoid clipping.
   // Keep font sizes stable when resizing 4 -> 3 -> 2 launcher columns.
   // Narrow layouts reflow information instead of reducing its readability.
   float small=10f;
   float nameBase=11f;
   float mainBase=11f;
   float mainBoost=size==2?0.5f:size==0?-0.5f:0f;
   row.setTextViewTextSize(R.id.v7_wide_name,android.util.TypedValue.COMPLEX_UNIT_SP,nameBase+mainBoost);
   row.setTextViewTextSize(R.id.v7_wide_builder_timer,android.util.TypedValue.COMPLEX_UNIT_SP,mainBase+mainBoost);
   row.setTextViewTextSize(R.id.v7_wide_builder_idle,android.util.TypedValue.COMPLEX_UNIT_SP,mainBase+mainBoost);
   for(int textId:new int[]{R.id.v7_wide_builders,R.id.v7_wide_lab_timer,R.id.v7_wide_lab_idle,
    R.id.v7_wide_pet_timer,R.id.v7_wide_pet_idle,R.id.v7_wide_finish})
     row.setTextViewTextSize(textId,android.util.TypedValue.COMPLEX_UNIT_SP,small);

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
  row.setInt(R.id.widget_account_root,"setBackgroundResource",rowTheme(background));
  row.setTextViewText(R.id.widget_account_name,AppData.displayName(ctx,s.account));
  row.setViewVisibility(R.id.widget_ready,s.builderReady>0?View.VISIBLE:View.GONE);
  if(s.builderReady>0)row.setTextViewText(R.id.widget_ready,"●"+s.builderReady);
  timer(row,R.id.widget_timer,R.id.widget_idle,s.builderNext,now);
  if(medium){
   String lab=s.labActive>0?String.valueOf(s.labActive):AppData.isMaxed(ctx,s.account,"Laboratorium")?"✓":"0";
   String pets=s.petActive>0?String.valueOf(s.petActive):AppData.isMaxed(ctx,s.account,"Pets")?"✓":"0";
   // In case a smaller launcher does use this fallback, tint only the MAX marks green.
   android.text.SpannableStringBuilder subtitle=new android.text.SpannableStringBuilder("🧙 ");
   int labStart=subtitle.length();subtitle.append(lab);
   if("✓".equals(lab))subtitle.setSpan(new android.text.style.ForegroundColorSpan(0xff8BE3AD),labStart,subtitle.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
   subtitle.append("  🐾 ");int petStart=subtitle.length();subtitle.append(pets);
   if("✓".equals(pets))subtitle.setSpan(new android.text.style.ForegroundColorSpan(0xff8BE3AD),petStart,subtitle.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
   subtitle.append("  ·  ").append(finish(ctx,s.builderNext,true));
   if(!showLab&&!showPet)subtitle=new android.text.SpannableStringBuilder(finish(ctx,s.builderNext,true));
   else if(!showLab)subtitle=new android.text.SpannableStringBuilder("🐾 "+pets+" · "+finish(ctx,s.builderNext,true));
   else if(!showPet)subtitle=new android.text.SpannableStringBuilder("🧙 "+lab+" · "+finish(ctx,s.builderNext,true));
   row.setTextViewText(R.id.widget_subtitle,subtitle);
  }
  row.setOnClickPendingIntent(R.id.widget_account_root,accountClick(ctx,s.account,id));
  return row;
 }
 private static int rowTheme(int style){
  return style==2?R.drawable.widget_row_background_transparent:
   style==1?R.drawable.widget_row_background_dark:R.drawable.widget_row_background;
 }
 private static RemoteViews multi(Context ctx,int id,List<Status> visible,long now){
  android.content.SharedPreferences studio=ctx.getSharedPreferences("WidgetPreferences",Context.MODE_PRIVATE);
  boolean showLab=studio.getBoolean("lab_"+id,true),showPet=studio.getBoolean("pet_"+id,true);
  int fontSize=studio.getInt("size_"+id,1),sortMode=studio.getInt("sort_"+id,0);
  int background=studio.getInt("bg_"+id,0);
  ArrayList<Status> ordered=new ArrayList<>(visible);
  if(sortMode==1)Collections.sort(ordered,(a,b)->AppData.displayName(ctx,a.account).compareToIgnoreCase(AppData.displayName(ctx,b.account)));
  else if(sortMode==2){
   LinkedHashMap<String,Integer> order=new LinkedHashMap<>();
   try{
    JSONArray arr=new JSONArray(studio.getString("order_"+id,"[]"));
    if(arr.length()==0)arr=new JSONArray(AppData.prefs(ctx).getString("accounts","[]"));
    for(int i=0;i<arr.length();i++)order.put(arr.optString(i),i);
   }catch(Exception ignored){}
   Collections.sort(ordered,(a,b)->Integer.compare(order.getOrDefault(a.account,9999),order.getOrDefault(b.account,9999)));
  }

  AppWidgetManager manager=AppWidgetManager.getInstance(ctx);
  android.os.Bundle opts=manager.getAppWidgetOptions(id);
  int width=190,height=240;
  if(opts!=null){
   boolean landscape=ctx.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
   width=opts.getInt(landscape?AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH:AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,190);
   height=opts.getInt(landscape?AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT:AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,240);
   if(width<=0)width=190;if(height<=0)height=240;
  }
  // Fixed-width timer cells cannot be sacrificed to a flexible account-name column.
  // Reflow rather than scale the timer text or show ellipses as the widget narrows.
  // Pixel/dp option width can differ by launcher; thresholds include breathing room.
  int rows=Math.max(1,ordered.size());
  int mode;
  Columns columns=sharedColumns(ctx,ordered,now,fontSize);
  // Choose the most compact row layout that fits the full shared column widths.
  // Values use the widest account in each column, so EVERY row has the same starts.
  if(width>=columns.singleLine())mode=2;
  else if(width>=columns.twoLine())mode=4;
  else if(width>=columns.threeLine())mode=5;
  else mode=6;
  int rowHeight=mode==2?24:mode==4?42:mode==5?62:102;
  boolean showSync=height>=44+rowHeight;
  int available=Math.max(0,height-24-(showSync?20:0));
  int count=Math.min(ordered.size(),Math.max(1,available/rowHeight));
  int hidden=ordered.size()-count;
  RemoteViews w=new RemoteViews(ctx.getPackageName(),R.layout.widget_v7_multi);
  w.setInt(R.id.v7_multi_root,"setBackgroundResource",background==1?R.drawable.widget_background_dark:
    background==2?R.drawable.widget_background_transparent:R.drawable.widget_background);
  boolean narrow=width<230;
  int ready=0;for(Status s:ordered)ready+=s.builderReady;
  w.setTextViewText(R.id.v7_multi_title,narrow?"TIMERS":"CLASH TIMER HUB");
  // No decorative crossed swords and no top-right account / ready counts.
  w.setViewVisibility(R.id.v7_multi_summary,View.GONE);
  w.removeAllViews(R.id.v7_multi_rows);
  if(ordered.isEmpty())w.addView(R.id.v7_multi_rows,new RemoteViews(ctx.getPackageName(),R.layout.widget_empty));
  else for(int i=0;i<count;i++)w.addView(R.id.v7_multi_rows,multiRow(ctx,ordered.get(i),mode,now,id,showLab,showPet,fontSize,background,columns));
  w.setViewVisibility(R.id.v7_multi_sync,showSync?View.VISIBLE:View.GONE);
  w.setInt(R.id.v7_multi_sync,"setBackgroundResource",rowTheme(background));
  boolean active=AppData.prefs(ctx).getBoolean("floatingEnabled",false);
  w.setTextViewText(R.id.v7_multi_sync,I18n.english(ctx)?("Sync "+(active?"ON":"OFF")):("Sync "+(active?"AAN":"UIT")));
  w.setOnClickPendingIntent(R.id.v7_multi_sync,syncClick(ctx,id));
  w.setOnClickPendingIntent(R.id.v7_multi_settings,settingsClick(ctx,id));
  Intent launch=new Intent(ctx,MainActivity.class);
  w.setOnClickPendingIntent(R.id.v7_multi_title,PendingIntent.getActivity(ctx,140000+id,launch,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  return w;
 }
 private static int backgroundFor(int style){
  return style==2?R.drawable.widget_background_transparent:
   style==1?R.drawable.widget_background_dark:R.drawable.widget_background;
 }
 private static android.content.SharedPreferences widgetPrefs(Context ctx){
  return ctx.getSharedPreferences("WidgetPreferences",Context.MODE_PRIVATE);
 }
 private static float textBoost(Context ctx,int id){
  int size=widgetPrefs(ctx).getInt("size_"+id,1);
  return size==2?1f:size==0?-1f:0f;
 }
 private static RemoteViews mini(Context ctx,int id,Status s,long now){
  RemoteViews view=new RemoteViews(ctx.getPackageName(),R.layout.widget_v7_mini);
  view.setInt(R.id.v7_mini_root,"setBackgroundResource",backgroundFor(widgetPrefs(ctx).getInt("bg_"+id,0)));
  float boost=textBoost(ctx,id);
  view.setTextViewTextSize(R.id.v7_mini_title,android.util.TypedValue.COMPLEX_UNIT_SP,10f+boost);
  view.setTextViewTextSize(R.id.v7_mini_timer,android.util.TypedValue.COMPLEX_UNIT_SP,16f+boost);
  view.setTextViewTextSize(R.id.v7_mini_idle,android.util.TypedValue.COMPLEX_UNIT_SP,16f+boost);
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
  view.setInt(R.id.v7_sync_root,"setBackgroundResource",backgroundFor(widgetPrefs(ctx).getInt("bg_"+id,0)));
  boolean active=AppData.prefs(ctx).getBoolean("floatingEnabled",false);
  view.setTextViewText(R.id.v7_sync_status,active?"SYNC ON":"SYNC OFF");
  view.setTextColor(R.id.v7_sync_status,active?0xff8BE3AD:0xffCBB5D9);
  view.setTextViewText(R.id.v7_sync_hint,
   I18n.english(ctx)?(active?"Tap to turn off":"Tap to turn on"):(active?"Tik om uit te zetten":"Tik om aan te zetten"));
  view.setInt(R.id.v7_sync_symbol,"setBackgroundResource",active?R.drawable.widget_sync_ring_on:R.drawable.widget_sync_ring_off);
  view.setOnClickPendingIntent(R.id.v7_sync_root,syncClick(ctx,id));
  view.setOnClickPendingIntent(R.id.v7_sync_settings,settingsClick(ctx,id));
  return view;
 }
 private static RemoteViews single(Context ctx,int id,Status s,long now){
  RemoteViews w=new RemoteViews(ctx.getPackageName(),R.layout.widget_v7_account);
  android.content.SharedPreferences config=widgetPrefs(ctx);
  int bg=config.getInt("bg_"+id,0);
  float boost=textBoost(ctx,id);
  w.setInt(R.id.v7_single_root,"setBackgroundResource",backgroundFor(bg));
  w.setInt(R.id.v7_single_sync,"setBackgroundResource",rowTheme(bg));
  w.setViewVisibility(R.id.v7_single_lab_row,config.getBoolean("lab_"+id,true)?View.VISIBLE:View.GONE);
  w.setViewVisibility(R.id.v7_single_pet_row,config.getBoolean("pet_"+id,true)?View.VISIBLE:View.GONE);
  w.setTextViewTextSize(R.id.v7_single_name,android.util.TypedValue.COMPLEX_UNIT_SP,13f+boost);
  for(int timerId:new int[]{R.id.v7_single_builder_timer,R.id.v7_single_lab_timer,R.id.v7_single_pet_timer,
    R.id.v7_single_builder_idle,R.id.v7_single_lab_idle,R.id.v7_single_pet_idle})
    w.setTextViewTextSize(timerId,android.util.TypedValue.COMPLEX_UNIT_SP,12f+boost);
  w.setTextViewText(R.id.v7_single_name,s==null?"Clash Timer Hub":AppData.displayName(ctx,s.account));
  w.setOnClickPendingIntent(R.id.v7_single_settings,settingsClick(ctx,id));
  if(s==null){
   timer(w,R.id.v7_single_builder_timer,R.id.v7_single_builder_idle,Long.MAX_VALUE,now);
   timer(w,R.id.v7_single_lab_timer,R.id.v7_single_lab_idle,Long.MAX_VALUE,now);
   timer(w,R.id.v7_single_pet_timer,R.id.v7_single_pet_idle,Long.MAX_VALUE,now);
  }
  if(s!=null){
   w.setTextViewText(R.id.v7_single_builders,s.builderReady+"✓  "+s.builderActive+"⚒");
   w.setTextViewText(R.id.v7_single_lab_count,"🧙 "+s.labActive);
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
  w.setTextViewText(R.id.v7_single_sync,I18n.english(ctx)?"Sync "+(active?"ON":"OFF"):"Sync "+(active?"AAN":"UIT"));
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
