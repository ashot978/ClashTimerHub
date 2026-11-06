package nl.clashtimerhub.v2;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

/** Read-only projection of every currently imported active upgrade across accounts.
 * A potion changes only predicted completion, never the source JSON finish.
 */
final class AllTimersTestData {
 static final class Item {
  String account,category,name,id;
  boolean goblin,boosted;
  long original,finish,saved;
 }
 static final class Snapshot {
  final ArrayList<Item> items=new ArrayList<>();
  int accounts,builders,research,pets,boosted;
  long next=Long.MAX_VALUE;
 }
 private AllTimersTestData(){}

 static Snapshot load(Context ctx){
  Snapshot result=new Snapshot();
  AppData.migrate(ctx);
  SharedPreferences prefs=AppData.prefs(ctx);
  long now=System.currentTimeMillis();
  try{
   JSONArray names=new JSONArray(prefs.getString("accounts","[]"));
   HashSet<String> accounts=new HashSet<>();
   for(int i=0;i<names.length();i++){
    String name=names.optString(i,"");
    if(!name.isEmpty())accounts.add(name);
   }
   result.accounts=accounts.size();
   HashMap<String,BuilderPotionTracker.Snapshot> builder=new HashMap<>();
   HashMap<String,ResearchPotionTracker.Snapshot> lab=new HashMap<>();
   JSONArray upgrades=new JSONArray(prefs.getString("upgrades","[]"));
   for(int i=0;i<upgrades.length();i++){
    JSONObject raw=upgrades.optJSONObject(i);
    if(raw==null)continue;
    String account=raw.optString("account","");
    long original=raw.optLong("finish",0L);
    if(original<=now||!accounts.contains(account))continue;
    String category=raw.optString("category","");
    long predicted=original;
    if("Bouwers".equals(category)){
     BuilderPotionTracker.Snapshot potion=builder.get(account);
     if(potion==null){potion=BuilderPotionTracker.load(ctx,account);builder.put(account,potion);}
     predicted=BuilderPotionTracker.estimateFinish(potion,original);
     result.builders++;
    }else if("Laboratorium".equals(category)){
     ResearchPotionTracker.Snapshot potion=lab.get(account);
     if(potion==null){potion=ResearchPotionTracker.load(ctx,account);lab.put(account,potion);}
     predicted=ResearchPotionTracker.estimateFinish(potion,original);
     result.research++;
    }else if("Pets".equals(category))result.pets++;
    Item item=new Item();
    item.account=account;item.category=category;
    item.name=raw.optString("name","Upgrade");
    item.id=raw.optString("id","timer-"+i);
    item.goblin=raw.optBoolean("extra",false)&&"Laboratorium".equals(category);
    item.original=original;item.finish=predicted;
    item.saved=Math.max(0,original-predicted);
    item.boosted=item.saved>1000L;
    if(item.boosted)result.boosted++;
    if(predicted>now&&predicted<result.next)result.next=predicted;
    result.items.add(item);
   }
   // Earliest predicted completion first; completed-by-boost but not re-synced
   // upgrades remain visible at the top until a fresh JSON import removes them.
   Collections.sort(result.items,(a,b)->{
    int time=Long.compare(a.finish,b.finish);
    if(time!=0)return time;
    int accountOrder=a.account.compareToIgnoreCase(b.account);
    return accountOrder!=0?accountOrder:a.name.compareToIgnoreCase(b.name);
   });
  }catch(Exception error){
   android.util.Log.e("ClashTimerHub","All Timers test snapshot error",error);
  }
  return result;
 }

 static String category(Context ctx,Item item){
  boolean en=I18n.english(ctx);
  if("Bouwers".equals(item.category))return en?"Builders":"Bouwers";
  if("Laboratorium".equals(item.category)){
   if(item.goblin)return en?"Goblin research":"Goblin-onderzoek";
   return en?"Research":"Onderzoek";
  }
  if("Pets".equals(item.category))return en?"Pets":"Huisdieren";
  return item.category;
 }
 static String endTime(Context ctx,long millis,long now){
  Locale loc=I18n.english(ctx)?Locale.UK:Locale.forLanguageTag("nl-NL");
  Calendar day=Calendar.getInstance(),end=Calendar.getInstance();
  day.setTimeInMillis(now);end.setTimeInMillis(millis);
  String hhmm=new SimpleDateFormat("HH:mm",loc).format(new Date(millis));
  if(day.get(Calendar.YEAR)==end.get(Calendar.YEAR)&&day.get(Calendar.DAY_OF_YEAR)==end.get(Calendar.DAY_OF_YEAR))
   return hhmm;
  day.add(Calendar.DAY_OF_YEAR,1);
  if(day.get(Calendar.YEAR)==end.get(Calendar.YEAR)&&day.get(Calendar.DAY_OF_YEAR)==end.get(Calendar.DAY_OF_YEAR))
   return (I18n.english(ctx)?"Tmr ":"Mrg ")+hhmm;
  return new SimpleDateFormat("d MMM HH:mm",loc).format(new Date(millis));
 }
 static String saved(Context ctx,long duration){
  long minutes=Math.max(1,(duration+30000L)/60000L);
  String hour=I18n.english(ctx)?"h":"u";
  return "−"+(minutes>=60?(minutes/60)+hour+String.format(Locale.ROOT,"%02d",minutes%60):minutes+"m");
 }
}
