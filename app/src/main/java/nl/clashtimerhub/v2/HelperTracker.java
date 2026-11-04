package nl.clashtimerhub.v2;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistent, account-specific helper levels and cooldowns from the in-game
 * village export. This class NEVER modifies building/research finish times:
 * exported timers may already include helper effects.
 */
final class HelperTracker {
 static final String KEY="helpersByAccount";
 static final String ALCHEMIST_AVAILABLE_KEY="alchemistAvailableByAccount";
 static final long ALCHEMIST_ID=93000002L;

 /** An explicit per-account opt-in, off for all existing and new accounts. */
 static boolean isAlchemistAvailable(Context context,String account){
  try{
   JSONObject flags=new JSONObject(AppData.prefs(context).getString(ALCHEMIST_AVAILABLE_KEY,"{}"));
   return flags.optBoolean(account,false);
  }catch(Exception e){return false;}
 }
 static void setAlchemistAvailable(Context context,String account,boolean available){
  try{
   SharedPreferences prefs=AppData.prefs(context);
   JSONObject flags=new JSONObject(prefs.getString(ALCHEMIST_AVAILABLE_KEY,"{}"));
   if(available)flags.put(account,true);
   else flags.remove(account);
   prefs.edit().putString(ALCHEMIST_AVAILABLE_KEY,flags.toString()).apply();
  }catch(Exception e){android.util.Log.e("ClashTimerHub","Unable to save Alchemist availability",e);}
 }
 /** Keep the setting when a player-tag account is linked to a named account. */
 static void mergeAlchemistAccount(Context context,String source,String target){
  try{
   SharedPreferences prefs=AppData.prefs(context);
   JSONObject flags=new JSONObject(prefs.getString(ALCHEMIST_AVAILABLE_KEY,"{}"));
   if(flags.optBoolean(source,false)&&!flags.has(target))flags.put(target,true);
   flags.remove(source);
   prefs.edit().putString(ALCHEMIST_AVAILABLE_KEY,flags.toString()).apply();
  }catch(Exception e){android.util.Log.e("ClashTimerHub","Unable to merge Alchemist availability",e);}
 }

 private static final long MAX_COOLDOWN_SECONDS=172800L;
 private HelperTracker(){}

 static final class Helper {
  final long id,readyAt,workingUntil;
  final int level;
  Helper(long id,int level,long readyAt,long workingUntil){
   this.id=id;this.level=level;this.readyAt=readyAt;this.workingUntil=workingUntil;
  }
  String name(boolean en){
   if(id==93000000L)return en?"Builder's Apprentice":"Bouwleerling";
   if(id==93000001L)return en?"Lab Assistant":"Labassistent";
   // Keep the ID in labels for newer/unknown helpers, rather than
   // incorrectly identifying a helper if Supercell changes game data.
   if(id==ALCHEMIST_ID)return "Alchemist";
   if(id==93000003L)return en?"Helper #4":"Helper #4";
   return "Helper "+id;
  }
 }
 static final class Snapshot {
  final long importedAt;
  final List<Helper> items;
  Snapshot(long at,List<Helper> values){importedAt=at;items=values;}
  int ready(long now){
   int n=0;for(Helper h:items)if(h.readyAt>=0&&h.readyAt<=now)n++;return n;
  }
  long soonest(long now){
   long next=Long.MAX_VALUE;
   for(Helper h:items)if(h.readyAt>now)next=Math.min(next,h.readyAt);
   return next;
  }
 }

 /** Returns null when export does not contain the helpers section. */
 static String merged(SharedPreferences prefs,JSONObject export,String account,long timestamp)throws JSONException{
  if(!export.has("helpers"))return null;
  JSONArray original=export.optJSONArray("helpers");
  if(original==null)throw new JSONException("Ongeldige helpers-sectie");
  if(original.length()>30)throw new JSONException("Te veel helpers in export");
  JSONArray normalized=new JSONArray();
  JSONObject boosts=export.optJSONObject("boosts");
  long global=export.has("helper_cooldown")?export.optLong("helper_cooldown",-1):
   boosts==null?-1:boosts.optLong("helper_cooldown",-1);
  for(int i=0;i<original.length();i++){
   JSONObject source=original.optJSONObject(i);
   if(source==null)continue;
   long id=source.optLong("data",-1);
   if(id<0)continue;
   int level=Math.max(0,Math.min(100,source.optInt("lvl",0)));
   long cd=source.has("helper_cooldown")?source.optLong("helper_cooldown",0):global;
   // A snapshot is authoritative only as of the export time, not a live game connection.
   if(cd>=0)cd=Math.min(MAX_COOLDOWN_SECONDS,cd);
   long work=Math.max(0,Math.min(86400,source.optLong("helper_timer",0)));
   normalized.put(new JSONObject()
    .put("id",id).put("level",level)
    .put("readyAt",cd<0?-1L:timestamp+cd*1000L)
    .put("workingUntil",timestamp+work*1000L));
  }
  JSONObject all=new JSONObject(prefs.getString(KEY,"{}"));
  all.put(account,new JSONObject().put("importedAt",timestamp).put("items",normalized));
  return all.toString();
 }

 static Snapshot forAccount(Context ctx,String account){
  try{
   JSONObject map=new JSONObject(AppData.prefs(ctx).getString(KEY,"{}"));
   JSONObject value=map.optJSONObject(account);
   if(value==null)return null;
   long timestamp=value.optLong("importedAt",0);
   JSONArray arr=value.optJSONArray("items");
   if(arr==null)return null;
   ArrayList<Helper> helpers=new ArrayList<>();
   boolean showAlchemist=isAlchemistAvailable(ctx,account);
   for(int i=0;i<arr.length();i++){
    JSONObject obj=arr.optJSONObject(i);if(obj==null)continue;
    long id=obj.optLong("id",-1);
    if(id==ALCHEMIST_ID&&!showAlchemist)continue;
    helpers.add(new Helper(id,obj.optInt("level",0),
      obj.optLong("readyAt",timestamp),obj.optLong("workingUntil",timestamp)));
   }
   return new Snapshot(timestamp,helpers);
  }catch(Exception ignored){return null;}
 }

 static String compact(Context ctx,String account,long now){
  Snapshot snapshot=forAccount(ctx,account);
  if(snapshot==null||snapshot.items.isEmpty())return "";
  int available=snapshot.ready(now);
  int total=snapshot.items.size();
  long next=snapshot.soonest(now);
  if(available==0&&next==Long.MAX_VALUE)return "🤝 "+total+" helpers";
  if(next==Long.MAX_VALUE)return "🤝 "+available+"/"+total+(available==total?" ✓":" · ?");
  long mins=Math.max(1,(next-now+59999)/60000);
  String remain=mins>=60?(mins/60)+"h"+String.format(java.util.Locale.ROOT,"%02d",mins%60):
    mins+"m";
  return "🤝 "+available+"/"+total+" · "+remain;
 }

 static String remaining(long timestamp,long now,boolean en){
  if(timestamp<=now)return en?"Ready":"Klaar";
  long mins=Math.max(1,(timestamp-now+59999)/60000);
  long hours=mins/60;
  if(hours>0)return hours+"h "+(mins%60)+"m";
  return mins+"m";
 }
}
