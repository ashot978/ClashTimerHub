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
 /**
  * Integrate the remaining unboosted work from the latest JSON export.
  * Builder Bite does NOT stack with a Builder Potion (max of 2x / 10x).
  * Study Soup combines its +3x with Research Potion's +23x for research.
  * Pet House uses the same +3x with any active Pet Potion's +23x.
  * Split at each actual boost transition; otherwise a snack may be wrongly
  * applied after expiry or a potion twice.
  */
 static long estimateFinish(Context ctx,String account,String category,long sourceFinish){
  if(!"Bouwers".equals(category)&&!"Laboratorium".equals(category)&&!"Pets".equals(category))
   return sourceFinish;
  Snapshot snacks=load(ctx,account);
  boolean builder="Bouwers".equals(category);
  boolean laboratory="Laboratorium".equals(category);
  long snackStart=builder?snacks.builderStart:snacks.labStart;
  long snackEnd=builder?snacks.builderEnd:snacks.labEnd;
  if(snackStart<=0L||snackEnd<=snackStart){
   return potionOnly(ctx,account,category,sourceFinish);
  }
  android.content.SharedPreferences prefs=AppData.prefs(ctx);
  long anchor=prefs.getLong("lastExportAt_"+account,prefs.getLong("lastSyncAt_"+account,0L));
  if(anchor<=0L||sourceFinish<=anchor||snackEnd<=anchor)
   return potionOnly(ctx,account,category,sourceFinish);

  java.util.ArrayList<long[]> potions=new java.util.ArrayList<>();
  if(builder){
   BuilderPotionTracker.Snapshot p=BuilderPotionTracker.load(ctx,account);
   if(!p.valid)return sourceFinish;
   for(BuilderPotionTracker.Period period:p.periods)
    potions.add(new long[]{period.start,period.end});
  }else if(laboratory){
   ResearchPotionTracker.Snapshot p=ResearchPotionTracker.load(ctx,account);
   if(!p.valid)return sourceFinish;
   for(ResearchPotionTracker.Period period:p.periods)
    potions.add(new long[]{period.start,period.end});
  }else{
   PetPotionTracker.Snapshot p=PetPotionTracker.load(ctx,account);
   if(!p.valid)return sourceFinish;
   for(PetPotionTracker.Period period:p.periods)
    potions.add(new long[]{period.start,period.end});
  }
  return integrate(anchor,sourceFinish-anchor,snackStart,snackEnd,
   builder, potions);
 }
 private static long potionOnly(Context ctx,String account,String category,long finish){
  if("Bouwers".equals(category))
   return BuilderPotionTracker.estimateFinish(BuilderPotionTracker.load(ctx,account),finish);
  if("Laboratorium".equals(category))
   return ResearchPotionTracker.estimateFinish(ResearchPotionTracker.load(ctx,account),finish);
  return PetPotionTracker.estimateFinish(PetPotionTracker.load(ctx,account),finish);
 }

 /** Package-private pure arithmetic: units are source-work milliseconds. */
 static long integrate(long anchor,long work,long snackStart,long snackEnd,
  boolean builder,java.util.List<long[]> potionPeriods){
  if(work<=0)return anchor;
  java.util.TreeSet<Long> boundaries=new java.util.TreeSet<>();
  boundaries.add(anchor);
  if(snackStart>anchor)boundaries.add(snackStart);
  if(snackEnd>anchor)boundaries.add(snackEnd);
  for(long[] p:potionPeriods){
   if(p[0]>anchor)boundaries.add(p[0]);
   if(p[1]>anchor)boundaries.add(p[1]);
  }
  long clock=anchor,outstanding=work;
  for(long transition:boundaries.tailSet(anchor,false)){
   if(transition<=clock)continue;
   long speed=speedAt(clock,snackStart,snackEnd,builder,potionPeriods);
   long millis=transition-clock;
   // Avoid overflow if a malformed export presents far-future transition.
   if(outstanding/speed<millis||outstanding/speed==millis&&outstanding%speed==0)
    return clock+(outstanding+speed-1)/speed;
   outstanding-=millis*speed;
   clock=transition;
  }
  long speed=speedAt(clock,snackStart,snackEnd,builder,potionPeriods);
  return clock+(outstanding+speed-1)/speed;
 }
 private static long speedAt(long when,long snackStart,long snackEnd,
  boolean builder,java.util.List<long[]> potionPeriods){
  boolean snack=snackStart<=when&&snackEnd>when;
  boolean potion=false;
  for(long[] p:potionPeriods)
   if(p[0]<=when&&p[1]>when){potion=true;break;}
  if(builder)return potion?10L:snack?2L:1L;
  return 1L+(potion?23L:0L)+(snack?3L:0L);
 }

}
