package nl.clashtimerhub.v2;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.*;
import android.os.Bundle;
import android.os.SystemClock;
import android.graphics.Color;
import android.view.View;
import android.widget.RemoteViews;
import org.json.*;
import java.util.*;

/** Independent customizable fifth widget. Never alters existing V7 widget rendering. */
public final class StudioWidget extends AppWidgetProvider {
 static final String PREFS="WidgetStudioPreferences";
 static final String[] FIELDS={"builders","next","lab","pets","helpers","finish"};
 static final String[] DEFAULT_FIELDS={"builders","next","lab","pets"};
 static final int DEF_BG=0xff241832,DEF_CARD=0xff38284a,DEF_TEXT=0xffffffff,DEF_ACCENT=0xffffd783;
 static final class Status {
  String key,name;
  int active,ready;
  long builder=Long.MAX_VALUE,lab=Long.MAX_VALUE,pet=Long.MAX_VALUE;
  Status(String key,String name){this.key=key;this.name=name;}
 }
 static SharedPreferences preferences(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
 static int[] ids(Context ctx){
  return AppWidgetManager.getInstance(ctx).getAppWidgetIds(new ComponentName(ctx,StudioWidget.class));
 }
 static int color(String text,int fallback){
  try{return Color.parseColor(text);}catch(Exception e){return fallback;}
 }
 static String hex(int value){return String.format(Locale.ROOT,"#%06X",value&0xffffff);}
 static int withOpacity(int color,int percent){
  return Color.argb(Math.max(0,Math.min(100,percent))*255/100,Color.red(color),Color.green(color),Color.blue(color));
 }
 static int percent(SharedPreferences p,String key,int id,int fallback){
  return Math.max(0,Math.min(100,p.getInt(key+"_"+id,fallback)));
 }
 static ArrayList<String> fieldOrder(SharedPreferences p,int id){
  ArrayList<String> out=new ArrayList<>();
  try{
   JSONArray a; String raw=p.getString("fields_"+id,null);
   if(raw==null){a=new JSONArray();for(String f:DEFAULT_FIELDS)a.put(f);}
   else a=new JSONArray(raw);
   for(int i=0;i<a.length();i++){
    String key=a.optString(i);
    if(Arrays.asList(FIELDS).contains(key)&&!out.contains(key))out.add(key);
   }
  }catch(Exception ignored){}
  if(out.isEmpty())out.add("next");
  return out;
 }
 static ArrayList<Status> read(Context ctx){
  AppData.migrate(ctx);
  SharedPreferences p=AppData.prefs(ctx);
  LinkedHashMap<String,Status> map=new LinkedHashMap<>();
  try{
   JSONArray accounts=new JSONArray(p.getString("accounts","[]"));
   for(int i=0;i<accounts.length();i++){
    String key=accounts.optString(i,"");
    if(!key.isEmpty())map.put(key,new Status(key,AppData.displayName(ctx,key)));
   }
   long now=System.currentTimeMillis();
   JSONArray upgrades=new JSONArray(p.getString("upgrades","[]"));
   for(int i=0;i<upgrades.length();i++){
    JSONObject u=upgrades.optJSONObject(i);if(u==null)continue;
    Status s=map.get(u.optString("account"));if(s==null)continue;
    long finish=u.optLong("finish");String cat=u.optString("category");
    if("Bouwers".equals(cat)){
     if(finish>now){s.active++;s.builder=Math.min(s.builder,finish);}
     else if(finish>p.getLong("ack_"+s.key,0))s.ready++;
    }else if("Laboratorium".equals(cat)&&finish>now)s.lab=Math.min(s.lab,finish);
    else if("Pets".equals(cat)&&finish>now)s.pet=Math.min(s.pet,finish);
   }
  }catch(Exception ex){android.util.Log.w("ClashTimerHub","Studio data error",ex);}
  return new ArrayList<>(map.values());
 }
 static ArrayList<Status> filterSort(Context ctx,int id,ArrayList<Status> input){
  SharedPreferences p=preferences(ctx);
  ArrayList<Status> out=new ArrayList<>();
  Set<String> chosen=new HashSet<>();
  try{
   JSONArray a=new JSONArray(p.getString("selected_"+id,"[]"));
   for(int i=0;i<a.length();i++)chosen.add(a.optString(i));
  }catch(Exception ignored){}
  boolean all=p.getBoolean("all_"+id,true);
  for(Status s:input)if(all||chosen.contains(s.key))out.add(s);
  int sort=p.getInt("sort_"+id,0);
  if(sort==1)Collections.sort(out,(a,b)->a.name.compareToIgnoreCase(b.name));
  else if(sort==2)Collections.sort(out,(a,b)->Long.compare(a.builder,b.builder));
  else {
   HashMap<String,Integer> order=new HashMap<>();
   try{
    JSONArray a=new JSONArray(p.getString("order_"+id,"[]"));
    for(int i=0;i<a.length();i++)order.put(a.optString(i),i);
   }catch(Exception ignored){}
   Collections.sort(out,(a,b)->Integer.compare(order.getOrDefault(a.key,999),order.getOrDefault(b.key,999)));
  }
  return out;
 }
 static String duration(long finish,long now){
  if(finish==Long.MAX_VALUE)return "—";
  long s=Math.max(0,(finish-now+999)/1000L),d=s/86400,h=s%86400/3600,m=s%3600/60;
  return (d>0?d+"d ":"")+(h>0?h+"h ":"")+m+"m";
 }
 static String fieldLabel(Context ctx,String key){
  boolean en=I18n.english(ctx);
  switch(key){
   case "builders":return en?"Builders":"Bouwers";
   case "next":return en?"Next":"Volgende";
   case "lab":return "Lab";
   case "pets":return "Pets";
   case "helpers":return "Helpers";
   case "finish":return en?"Finish":"Klaar";
   default:return key;
  }
 }
 static String fieldValue(Context ctx,Status s,String key,long now){
  switch(key){
   case "builders":return s.ready+"/"+(s.ready+s.active)+" ✓";
   case "next":return duration(s.builder,now);
   case "lab":return s.lab==Long.MAX_VALUE?(AppData.isMaxed(ctx,s.key,"Laboratorium")?"✓":"—"):duration(s.lab,now);
   case "pets":return s.pet==Long.MAX_VALUE?(AppData.isMaxed(ctx,s.key,"Pets")?"✓":"—"):duration(s.pet,now);
   case "helpers":{
    String value=HelperTracker.compact(ctx,s.key,now);
    return value.isEmpty()?"—":value.replace("🤝 ","").replace("🤝","");
   }
   case "finish":return s.builder==Long.MAX_VALUE?"—":
    new java.text.SimpleDateFormat("dd/MM HH:mm",Locale.ROOT).format(new Date(s.builder));
   default:return "—";
  }
 }
 private static PendingIntent openAccount(Context ctx,String account,int id){
  Intent intent=new Intent(ctx,MainActivity.class);
  intent.setAction("nl.clashtimerhub.v2.STUDIO_ACCOUNT."+id+"."+account);
  intent.putExtra("openAccount",account);
  intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
  return PendingIntent.getActivity(ctx,Math.abs((id+"#"+account).hashCode()),intent,
   PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 private static PendingIntent openSettings(Context ctx,int id){
  Intent i=new Intent(ctx,StudioEditorActivity.class);
  i.setAction("nl.clashtimerhub.v2.STUDIO_SETTINGS."+id);
  i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
  return PendingIntent.getActivity(ctx,700000+id,i,
   PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 private static int dpWidth(Bundle opts,boolean portrait){
  int w=opts==null?310:opts.getInt(portrait?AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH:AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,310);
  return w>0?w:310;
 }
 private static int dpHeight(Bundle opts,boolean portrait){
  int h=opts==null?240:opts.getInt(portrait?AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT:AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,240);
  return h>0?h:240;
 }
 /** Keep account names and complete values readable. On narrow widgets omit low-priority
  * columns rather than clipping the right edge or cramming values together. */
 static ArrayList<String> compactFieldsForWidth(ArrayList<String> chosen,int width,int size){
  ArrayList<String> shown=new ArrayList<>(chosen);
  int available=Math.max(0,width-114);
  while(shown.size()>1&&compactRequiredWidth(shown,size)>available){
   String drop=null;
   for(String candidate:new String[]{"finish","helpers","pets","lab","next","builders"})
    if(shown.contains(candidate)){drop=candidate;break;}
   if(drop==null)break;
   shown.remove(drop);
  }
  return shown;
 }
 private static int compactRequiredWidth(List<String> fields,int size){
  int extra=Math.max(0,size-10)*4,total=0;
  for(String field:fields){
   switch(field){
    case "builders":total+=62+extra;break;
    case "next":case "lab":case "pets":total+=72+extra;break;
    case "helpers":total+=103+extra;break;
    case "finish":total+=93+extra;break;
    default:total+=70+extra;
   }
  }
  return total;
 }
 static RemoteViews build(Context ctx,int id,ArrayList<Status> available){
  SharedPreferences p=preferences(ctx);
  ArrayList<Status> list=filterSort(ctx,id,available);
  AppWidgetManager manager=AppWidgetManager.getInstance(ctx);
  Bundle options=manager.getAppWidgetOptions(id);
  boolean portrait=ctx.getResources().getConfiguration().orientation!=android.content.res.Configuration.ORIENTATION_LANDSCAPE;
  int width=dpWidth(options,portrait),height=dpHeight(options,portrait);
  int size=Math.max(10,Math.min(18,p.getInt("font_"+id,12)));
  int space=Math.max(0,Math.min(10,p.getInt("space_"+id,3)));
  int requested=Math.max(0,Math.min(3,p.getInt("cols_"+id,0)));
  int cols=requested==0?(width>=570?3:width>=360?2:1):requested;
  cols=Math.max(1,Math.min(cols,Math.max(1,width/145)));
  ArrayList<String> fields=fieldOrder(p,id);
  int bg=withOpacity(color(p.getString("bg_"+id,hex(DEF_BG)),DEF_BG),percent(p,"bgOpacity",id,90));
  int card=withOpacity(color(p.getString("card_"+id,hex(DEF_CARD)),DEF_CARD),percent(p,"cardOpacity",id,76));
  int ink=color(p.getString("text_"+id,hex(DEF_TEXT)),DEF_TEXT);
  int accent=color(p.getString("accent_"+id,hex(DEF_ACCENT)),DEF_ACCENT);
  boolean labels=p.getBoolean("labels_"+id,true);
  boolean shortNames=p.getBoolean("short_"+id,false);
  boolean footer=p.getBoolean("footer_"+id,true);
  boolean gear=p.getBoolean("gear_"+id,true);
  boolean separators=p.getBoolean("separators_"+id,true);
  int layout=p.getInt("layout_"+id,1);
  boolean compact=layout==1||layout==2;
  if(compact)fields=compactFieldsForWidth(fields,width,size);
  int estimatedRow=compact?Math.max(17,size+8)+space:(size+6)*(fields.size()+1)+space*2+7;
  int availableH=Math.max(0,height-(footer?36:8));
  int rows=Math.max(1,availableH/Math.max(20,estimatedRow));
  if(compact){cols=1;rows=Math.max(1,availableH/Math.max(16,estimatedRow));}
  int capacity=Math.min(list.size(),Math.min(35,rows*cols));
  RemoteViews root=new RemoteViews(ctx.getPackageName(),R.layout.studio_widget);
  root.setInt(R.id.studio_root,"setBackgroundColor",bg);
  for(int i:new int[]{R.id.studio_column_1,R.id.studio_column_2,R.id.studio_column_3})root.removeAllViews(i);
  root.setViewVisibility(R.id.studio_column_2,cols>=2?View.VISIBLE:View.GONE);
  root.setViewVisibility(R.id.studio_column_3,cols>=3?View.VISIBLE:View.GONE);
  int[] columns={R.id.studio_column_1,R.id.studio_column_2,R.id.studio_column_3};
  long now=System.currentTimeMillis();
  for(int i=0;i<capacity;i++){
   Status s=list.get(i);
   if(compact){
    RemoteViews row=new RemoteViews(ctx.getPackageName(),R.layout.studio_compact_row);
    int rowShade=separators?(i%2==0?card:withOpacity(card,Math.max(0,Color.alpha(card)*100/255-16))):Color.TRANSPARENT;
    row.setInt(R.id.studio_compact_root,"setBackgroundColor",rowShade);
    row.setViewPadding(R.id.studio_compact_root,4,Math.max(0,space/2),4,Math.max(0,space/2));
    String shown=s.name;
    if(shortNames&&shown.length()>13)shown=shown.substring(0,12)+"…";
    row.setTextViewText(R.id.studio_compact_name,shown);
    row.setTextColor(R.id.studio_compact_name,accent);
    row.setTextViewTextSize(R.id.studio_compact_name,android.util.TypedValue.COMPLEX_UNIT_SP,Math.max(9,size-1));
    row.removeAllViews(R.id.studio_compact_fields);
    for(String field:fields){
     int cellLayout=field.equals("helpers")?R.layout.studio_compact_cell_xwide:
      field.equals("finish")?R.layout.studio_compact_cell_wide:R.layout.studio_compact_cell;
     RemoteViews cell=new RemoteViews(ctx.getPackageName(),cellLayout);
     cell.setTextColor(R.id.studio_compact_value,ink);
     cell.setTextColor(R.id.studio_compact_countdown,ink);
     cell.setTextViewTextSize(R.id.studio_compact_value,android.util.TypedValue.COMPLEX_UNIT_SP,Math.max(8,size-2));
     cell.setTextViewTextSize(R.id.studio_compact_countdown,android.util.TypedValue.COMPLEX_UNIT_SP,Math.max(8,size-2));
     long next=field.equals("next")?s.builder:field.equals("lab")?s.lab:field.equals("pets")?s.pet:Long.MAX_VALUE;
     if(next!=Long.MAX_VALUE&&next>now){
      cell.setViewVisibility(R.id.studio_compact_value,View.GONE);
      cell.setViewVisibility(R.id.studio_compact_countdown,View.VISIBLE);
      cell.setChronometer(R.id.studio_compact_countdown,SystemClock.elapsedRealtime()+Math.max(0,next-now),null,true);
      cell.setChronometerCountDown(R.id.studio_compact_countdown,true);
     }else{
      cell.setViewVisibility(R.id.studio_compact_countdown,View.GONE);
      String value=fieldValue(ctx,s,field,now);
      // Clean numeric table: no decorative emoji or labels in compact columns.
      cell.setTextViewText(R.id.studio_compact_value,value);
     }
     row.addView(R.id.studio_compact_fields,cell);
    }
    row.setOnClickPendingIntent(R.id.studio_compact_root,openAccount(ctx,s.key,id));
    root.addView(R.id.studio_column_1,row);
    continue;
   }
   RemoteViews cardView=new RemoteViews(ctx.getPackageName(),R.layout.studio_card);
   cardView.setInt(R.id.studio_card_root,"setBackgroundColor",card);
   cardView.setViewPadding(R.id.studio_card_root,6,space+2,6,space+2);
   String shown=s.name;
   if(shortNames&&shown.length()>13)shown=shown.substring(0,12)+"…";
   cardView.setTextViewText(R.id.studio_card_name,shown);
   cardView.setTextColor(R.id.studio_card_name,accent);
   cardView.setTextViewTextSize(R.id.studio_card_name,android.util.TypedValue.COMPLEX_UNIT_SP,size+1);
   cardView.removeAllViews(R.id.studio_card_fields);
   for(String field:fields){
    RemoteViews line=new RemoteViews(ctx.getPackageName(),R.layout.studio_field);
    String text=(labels?fieldLabel(ctx,field)+": ":"")+fieldValue(ctx,s,field,now);
    line.setTextViewText(R.id.studio_field_text,text);
    line.setTextColor(R.id.studio_field_text,ink);
    line.setTextViewTextSize(R.id.studio_field_text,android.util.TypedValue.COMPLEX_UNIT_SP,size);
    cardView.addView(R.id.studio_card_fields,line);
   }
   cardView.setOnClickPendingIntent(R.id.studio_card_root,openAccount(ctx,s.key,id));
   root.addView(columns[i%cols],cardView);
  }
  if(capacity==0){
   RemoteViews empty=new RemoteViews(ctx.getPackageName(),R.layout.studio_field);
   empty.setTextViewText(R.id.studio_field_text,I18n.english(ctx)?"No accounts selected":"Geen accounts geselecteerd");
   empty.setTextColor(R.id.studio_field_text,ink);
   root.addView(R.id.studio_column_1,empty);
  }
  root.setViewVisibility(R.id.studio_footer,footer?View.VISIBLE:View.GONE);
  root.setViewVisibility(R.id.studio_settings,gear?View.VISIBLE:View.GONE);
  root.setTextViewText(R.id.studio_footer,(capacity<list.size()?capacity+"/"+list.size()+"  ·  +"+(list.size()-capacity):capacity+" "+(I18n.english(ctx)?"accounts":"accounts")));
  root.setTextColor(R.id.studio_footer,ink);
  root.setOnClickPendingIntent(R.id.studio_settings,openSettings(ctx,id));
  return root;
 }
 static JSONArray backupSettings(Context ctx){
  JSONArray result=new JSONArray();
  SharedPreferences p=preferences(ctx);
  int[] ids=ids(ctx);
  for(int i=0;i<ids.length;i++){
   int id=ids[i];
   JSONObject entry=new JSONObject();
   try{
    entry.put("index",i);
    entry.put("all",p.getBoolean("all_"+id,true));
    entry.put("selected",new JSONArray(p.getString("selected_"+id,"[]")));
    entry.put("order",new JSONArray(p.getString("order_"+id,"[]")));
    entry.put("fields",new JSONArray(fieldOrder(p,id)));
    entry.put("cols",p.getInt("cols_"+id,0));
    entry.put("sort",p.getInt("sort_"+id,0));
    entry.put("font",p.getInt("font_"+id,12));
    entry.put("space",p.getInt("space_"+id,3));
    entry.put("labels",p.getBoolean("labels_"+id,true));
    entry.put("short",p.getBoolean("short_"+id,false));
    entry.put("layout",p.getInt("layout_"+id,1));
    entry.put("bgOpacity",percent(p,"bgOpacity",id,90));
    entry.put("cardOpacity",percent(p,"cardOpacity",id,76));
    entry.put("footer",p.getBoolean("footer_"+id,true));
    entry.put("gear",p.getBoolean("gear_"+id,true));
    entry.put("separators",p.getBoolean("separators_"+id,true));
    for(String key:new String[]{"bg","card","text","accent"})
     entry.put(key,p.getString(key+"_"+id,hex(key.equals("bg")?DEF_BG:key.equals("card")?DEF_CARD:key.equals("text")?DEF_TEXT:DEF_ACCENT)));
    result.put(entry);
   }catch(Exception ex){android.util.Log.w("ClashTimerHub","Studio backup item failed",ex);}
  }
  return result;
 }
 static void restoreSettings(Context ctx,JSONArray entries){
  if(entries==null)return;
  SharedPreferences.Editor e=preferences(ctx).edit();
  int[] ids=ids(ctx);
  for(int i=0;i<entries.length();i++){
   JSONObject item=entries.optJSONObject(i);if(item==null)continue;
   int ordinal=item.optInt("index",-1);if(ordinal<0||ordinal>=ids.length)continue;
   int id=ids[ordinal];
   e.putBoolean("all_"+id,item.optBoolean("all",true));
   e.putString("selected_"+id,item.optJSONArray("selected")==null?"[]":item.optJSONArray("selected").toString());
   e.putString("order_"+id,item.optJSONArray("order")==null?"[]":item.optJSONArray("order").toString());
   JSONArray fields=item.optJSONArray("fields");
   if(fields!=null&&fields.length()>0)e.putString("fields_"+id,fields.toString());
   e.putInt("cols_"+id,Math.max(0,Math.min(3,item.optInt("cols",0))));
   e.putInt("sort_"+id,Math.max(0,Math.min(2,item.optInt("sort",0))));
   e.putInt("font_"+id,Math.max(10,Math.min(18,item.optInt("font",12))));
   e.putInt("space_"+id,Math.max(0,Math.min(10,item.optInt("space",3))));
   e.putBoolean("labels_"+id,item.optBoolean("labels",true));
   e.putBoolean("short_"+id,item.optBoolean("short",false));
   e.putInt("layout_"+id,Math.max(0,Math.min(2,item.optInt("layout",1))));
   e.putInt("bgOpacity_"+id,Math.max(0,Math.min(100,item.optInt("bgOpacity",90))));
   e.putInt("cardOpacity_"+id,Math.max(0,Math.min(100,item.optInt("cardOpacity",76))));
   e.putBoolean("footer_"+id,item.optBoolean("footer",true));
   e.putBoolean("gear_"+id,item.optBoolean("gear",true));
   e.putBoolean("separators_"+id,item.optBoolean("separators",true));
   for(String key:new String[]{"bg","card","text","accent"}){
    String value=item.optString(key,"");
    if(value.matches("#[0-9A-Fa-f]{6}"))e.putString(key+"_"+id,value);
   }
  }
  e.apply();updateAll(ctx);
 }
 static void updateAll(Context ctx){
  AppWidgetManager m=AppWidgetManager.getInstance(ctx);
  ArrayList<Status> all=read(ctx);
  for(int id:ids(ctx)){
   try{m.updateAppWidget(id,build(ctx,id,all));}
   catch(Exception e){android.util.Log.e("ClashTimerHub","Studio widget render failed "+id,e);}
  }
 }
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){updateAll(c);}
 @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle opts){updateAll(c);}
 @Override public void onDeleted(Context c,int[] ids){
  SharedPreferences.Editor e=preferences(c).edit();
  for(int id:ids)for(String prefix:new String[]{"fields_","selected_","all_","order_","sort_","cols_","font_","space_","bg_","card_","text_","accent_","labels_","short_","layout_","bgOpacity_","cardOpacity_","footer_","gear_","separators_"})e.remove(prefix+id);
  e.apply();super.onDeleted(c,ids);
 }
}