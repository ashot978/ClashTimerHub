package nl.clashtimerhub.v2;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

/**
 * Active Magic Snack durations from the official in-game village export.
 * Snack speeds differ from the corresponding Potions:
 * builder_consumable = Builder Bite (2x builders)
 * lab_consumable = Study Soup (4x laboratory AND pet house)
 *
 * Source upgrade timer timestamps are never overwritten; this is additive
 * forecast data, scoped by account. Zero values clear an explicitly absent
 * snack, while missing fields leave the other snack untouched.
 */
final class MagicSnackTracker {
 static final String KEY="magicSnackForecasts";
 private static final long MAX_SECONDS=72L*3600L;
 private MagicSnackTracker(){}

 static final class Snapshot {
  final long builderStart,builderEnd,labStart,labEnd;
  Snapshot(long bs,long be,long ls,long le){
   builderStart=bs;builderEnd=be;labStart=ls;labEnd=le;
  }
  boolean builderActive(long at){return builderStart<=at&&builderEnd>at;}
  boolean labActive(long at){return labStart<=at&&labEnd>at;}
 }

 static Snapshot load(Context ctx,String account){
  try{
   JSONObject all=new JSONObject(AppData.prefs(ctx).getString(KEY,"{}"));
   JSONObject item=all.optJSONObject(account);
   if(item==null)return new Snapshot(0L,0L,0L,0L);
   return new Snapshot(item.optLong("builderStart",0L),
    item.optLong("builderEnd",0L),item.optLong("labStart",0L),
    item.optLong("labEnd",0L));
  }catch(Exception e){
   android.util.Log.w("ClashTimerHub","Magic Snack state unavailable",e);
   return new Snapshot(0L,0L,0L,0L);
  }
 }

 /** Returns null for exports without snack information, never a fabricated
  * one-hour boost. Remaining durations are in seconds at export timestamp.
  */
 static String mergedFromExport(SharedPreferences prefs,JSONObject export,
   String account,long exportedAt,long now){
  JSONObject boosts=export.optJSONObject("boosts");
  if(boosts==null)return null;
  boolean hasBuilder=boosts.has("builder_consumable");
  boolean hasLab=boosts.has("lab_consumable");
  if(!hasBuilder&&!hasLab)return null;
  try{
   JSONObject all=new JSONObject(prefs.getString(KEY,"{}"));
   JSONObject prior=all.optJSONObject(account);
   if(prior!=null&&exportedAt<prior.optLong("exportedAt",0L))return null;
   JSONObject next=prior==null?new JSONObject():new JSONObject(prior.toString());
   if(hasBuilder)copySnack(boosts,next,"builder_consumable",
    "builderStart","builderEnd",exportedAt,now);
   if(hasLab)copySnack(boosts,next,"lab_consumable",
    "labStart","labEnd",exportedAt,now);
   next.put("exportedAt",exportedAt);
   all.put(account,next);
   return all.toString();
  }catch(Exception e){
   android.util.Log.w("ClashTimerHub","Could not import Magic Snack durations",e);
   return null;
  }
 }

 private static void copySnack(JSONObject source,JSONObject target,String key,
   String startKey,String endKey,long exportedAt,long now)throws org.json.JSONException{
  long seconds=source.optLong(key,-1L);
  // Unknown/invalid data does not introduce an artificial speed boost.
  if(seconds<0||seconds>MAX_SECONDS)return;
  long end=seconds==0L?0L:exportedAt+seconds*1000L;
  if(end<=now){
   target.put(startKey,0L);target.put(endKey,0L);
  }else{
   target.put(startKey,exportedAt);target.put(endKey,end);
  }
 }
}
