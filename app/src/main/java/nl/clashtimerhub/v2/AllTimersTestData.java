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
 static final class Account {
  String key,helpers="";
  int builders,ready,lab,labReady,pets,petReady;
  long builderAt=Long.MAX_VALUE,labAt=Long.MAX_VALUE,petAt=Long.MAX_VALUE;
  boolean builderBoost,labBoost,petBoost,builderMax,labMax,petMax;
  Account(String key){this.key=key;}
 }
 static final class Snapshot {
  final ArrayList<Item> items=new ArrayList<>();
  final ArrayList<Account> overview=new ArrayList<>();
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
   HashMap<String,Account> byAccount=new HashMap<>();
   for(String key:WidgetV7.boostAccountOrder(ctx)){
    if(accounts.contains(key)&&!byAccount.containsKey(key)){
     Account a=new Account(key);a.helpers=HelperTracker.compact(ctx,key,now);
    a.builderMax=AppData.isMaxed(ctx,key,"Bouwers");
    a.labMax=AppData.isMaxed(ctx,key,"Laboratorium");
    a.petMax=AppData.isMaxed(ctx,key,"Pets");
     byAccount.put(key,a);result.overview.add(a);
    }
   }
   for(String key:accounts)if(!byAccount.containsKey(key)){
    Account a=new Account(key);a.helpers=HelperTracker.compact(ctx,key,now);
    a.builderMax=AppData.isMaxed(ctx,key,"Bouwers");
    a.labMax=AppData.isMaxed(ctx,key,"Laboratorium");
    a.petMax=AppData.isMaxed(ctx,key,"Pets");
    byAccount.put(key,a);result.overview.add(a);
   }
   HashMap<String,BuilderPotionTracker.Snapshot> builder=new HashMap<>();
   HashMap<String,ResearchPotionTracker.Snapshot> lab=new HashMap<>();
   JSONArray upgrades=new JSONArray(prefs.getString("upgrades","[]"));
   for(int i=0;i<upgrades.length();i++){
    JSONObject raw=upgrades.optJSONObject(i);
    if(raw==null)continue;
    String account=raw.optString("account","");
    long original=raw.optLong("finish",0L);
    if(original<=0||!accounts.contains(account))continue;
    // A timer whose source finish has elapsed must remain visible until the
    // player makes a fresh export/sync for THIS account. This is what "!" in
    // the widget represents; do not remove it simply because time passed.
    long lastAcknowledged=prefs.getLong("ack_"+account,0L);
    String category=raw.optString("category","");
    long predicted=original;
    if("Bouwers".equals(category)){
     BuilderPotionTracker.Snapshot potion=builder.get(account);
     if(potion==null){potion=BuilderPotionTracker.load(ctx,account);builder.put(account,potion);}
     predicted=UpgradeForecast.finish(ctx,account,category,original);
     result.builders++;
    }else if("Laboratorium".equals(category)){
     ResearchPotionTracker.Snapshot potion=lab.get(account);
     if(potion==null){potion=ResearchPotionTracker.load(ctx,account);lab.put(account,potion);}
     predicted=UpgradeForecast.finish(ctx,account,category,original);
     result.research++;
    }else if("Pets".equals(category)){
     predicted=UpgradeForecast.finish(ctx,account,category,original);
     result.pets++;
    }
    // A previous sync has already acknowledged this completion. On a fresh
    // JSON import stale upgrades disappear, but this guard also prevents
    // old/acknowledged alarms from resurfacing in a widget.
    if(predicted<=lastAcknowledged)continue;
    Item item=new Item();
    item.account=account;item.category=category;
    item.name=ClashNames.displayName(raw);
    item.id=raw.optString("id","timer-"+i);
    item.goblin=raw.optBoolean("extra",false)&&"Laboratorium".equals(category);
    item.original=original;item.finish=predicted;
    item.saved=Math.max(0,original-predicted);
    item.boosted=item.saved>1000L;
    if(item.boosted)result.boosted++;
    Account a=byAccount.get(account);
    if(a!=null){
     if("Bouwers".equals(category)){
      if(predicted>now){
       a.builders++;
       if(predicted<a.builderAt){a.builderAt=predicted;a.builderBoost=item.boosted;}
      }else a.ready++;
     }else if("Laboratorium".equals(category)){
      if(predicted>now){
       a.lab++;
       if(predicted<a.labAt){a.labAt=predicted;a.labBoost=item.boosted;}
      }else a.labReady++;
     }else if("Pets".equals(category)){
      if(predicted>now){
       a.pets++;
       if(predicted<a.petAt){a.petAt=predicted;a.petBoost=item.boosted;}
      }
      else a.petReady++;
     }
    }
    if(predicted>now&&predicted<result.next)result.next=predicted;
    result.items.add(item);
   }
   // Include both normal and boost-completed timers until their next sync.
   // Sort individual upgrades by effective finish; account priority is in
   // AllTimersWidgetSettings and uses a.ready + a.labReady + a.petReady.
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
 static String compactEnd(Context ctx,long millis,long now){
  Locale loc=I18n.english(ctx)?Locale.UK:Locale.forLanguageTag("nl-NL");
  Calendar current=Calendar.getInstance(),target=Calendar.getInstance();
  current.setTimeInMillis(now);target.setTimeInMillis(millis);
  String time=new SimpleDateFormat("HH:mm",loc).format(new Date(millis));
  if(current.get(Calendar.YEAR)==target.get(Calendar.YEAR)&&
   current.get(Calendar.DAY_OF_YEAR)==target.get(Calendar.DAY_OF_YEAR))return time;
  current.add(Calendar.DAY_OF_YEAR,1);
  if(current.get(Calendar.YEAR)==target.get(Calendar.YEAR)&&
   current.get(Calendar.DAY_OF_YEAR)==target.get(Calendar.DAY_OF_YEAR))
    return (I18n.english(ctx)?"Tmr ":"Mrg ")+time;
  return new SimpleDateFormat("d/M HH:mm",loc).format(new Date(millis));
 }
 static String saved(Context ctx,long duration){
  long minutes=Math.max(1,(duration+30000L)/60000L);
  String hour=I18n.english(ctx)?"h":"u";
  return "−"+(minutes>=60?(minutes/60)+hour+String.format(Locale.ROOT,"%02d",minutes%60):minutes+"m");
 }
}
