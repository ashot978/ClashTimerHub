package nl.clashtimerhub.v2;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

/**
 * Research Potion forecasts per account. The original JSON timers remain untouched.
 * Potion start and end timestamps belong to the account independently of JSON.
 * Each new import moves the forecast baseline to current per-account timers,
 * without resetting the actual Potion duration or applying elapsed boost twice.
 */
final class ResearchPotionTracker {
 static final String KEY="researchPotionForecasts";
 private static final long MINUTE=60000L;
 private static final long MAX_DURATION=72L*60L*MINUTE;
 private static final int MAX_PERIODS=32;
 private ResearchPotionTracker(){}

 static final class Period {
  long start,end;
  Period(long start,long end){this.start=start;this.end=end;}
 }
 static final class Snapshot {
  final long syncedAt;
  final boolean valid,exists;
  final ArrayList<Period> periods;
  Snapshot(long syncedAt,boolean valid,boolean exists,ArrayList<Period> periods){
   this.syncedAt=syncedAt;this.valid=valid;this.exists=exists;this.periods=periods;
  }
  long remaining(long now){
   if(!valid)return 0;
   for(int i=periods.size()-1;i>=0;i--){
    Period period=periods.get(i);
    if(period.start<=now&&period.end>now)return period.end-now;
   }
   return 0;
  }
  long boostEnd(long now){
   if(!valid)return Long.MAX_VALUE;
   for(Period period:periods)if(period.start<=now&&period.end>now)return period.end;
   return Long.MAX_VALUE;
  }
 }

 static Snapshot load(Context ctx,String account){
  SharedPreferences p=AppData.prefs(ctx);
  // JSON timers are anchored to the export timestamp, NOT clipboard import time.
  long current=p.getLong("lastExportAt_"+account,p.getLong("lastSyncAt_"+account,0L));
  try{
   JSONObject map=new JSONObject(p.getString(KEY,"{}"));
   JSONObject item=map.optJSONObject(account);
   if(item==null)return new Snapshot(current,true,false,new ArrayList<>());
   // The stored sync marker is legacy metadata, not an expiry condition.
   // A fresh JSON import must never invalidate an active Potion countdown.
   JSONArray values=item.optJSONArray("periods");
   ArrayList<Period> periods=new ArrayList<>();
   if(values!=null)for(int i=0;i<Math.min(MAX_PERIODS,values.length());i++){
    JSONObject v=values.optJSONObject(i);
    if(v==null)continue;
    long start=v.optLong("start",0L),end=v.optLong("end",0L);
    if(start>0&&end>start&&end-start<=MAX_DURATION)periods.add(new Period(start,end));
   }
   // Use current per-account import as forecast baseline. Existing real-world
   // boost periods stay intact; estimateFinish ignores periods before baseline.
   return new Snapshot(current,true,true,periods);
  }catch(Exception e){
   android.util.Log.w("ClashTimerHub","Research Potion state unavailable",e);
   return new Snapshot(current,false,true,new ArrayList<>());
  }
 }

 /**
  * Safely reconcile the exact active Research Potion remaining-time from
  * Clash's own Data Export: boosts.lab_boost is measured in seconds.
  *
  * Use the export timestamp (not clipboard paste time) to avoid adding
  * transfer delays to an active Potion. This never clears independent
  * manual boosts when the key is absent, zero or no longer active.
  * Returns null when no reliable active value is available.
  */
 static String mergedFromExport(SharedPreferences prefs,JSONObject export,
   String account,long exportedAt,long now){
  JSONObject boosts=export.optJSONObject("boosts");
  if(boosts==null||!boosts.has("lab_boost"))return null;
  long seconds=boosts.optLong("lab_boost",-1L);
  if(seconds<=0||seconds>MAX_DURATION/1000L)return null;
  long end=exportedAt+seconds*1000L;
  if(end<=now||end-now>MAX_DURATION)return null;
  try{
   JSONObject all=new JSONObject(prefs.getString(KEY,"{}"));
   JSONObject saved=all.optJSONObject(account);
   JSONArray previous=saved==null?null:saved.optJSONArray("periods");
   JSONArray updated=new JSONArray();
   boolean replaced=false;
   if(previous!=null){
    for(int i=0;i<previous.length()&&i<MAX_PERIODS;i++){
     JSONObject p=previous.optJSONObject(i);
     if(p==null)continue;
     long start=p.optLong("start",0L),oldEnd=p.optLong("end",0L);
     if(start<=0||oldEnd<=start||oldEnd-start>MAX_DURATION)continue;
     if(!replaced&&start<=now&&oldEnd>now){
      // The game supplies the authoritative expiry of the current Potion.
      // Preserve its start; never replay time before the latest JSON sync.
      if(end>start)updated.put(new JSONObject().put("start",start).put("end",end));
      replaced=true;
     }else updated.put(new JSONObject().put("start",start).put("end",oldEnd));
    }
   }
   if(!replaced){
    // Past boost periods may still be retained for display/history. The
    // predicted end is calculated only from the most recent JSON baseline.
    if(updated.length()>=MAX_PERIODS){
     JSONArray trimmed=new JSONArray();
     for(int i=1;i<updated.length();i++)trimmed.put(updated.get(i));
     updated=trimmed;
    }
    long start=Math.min(now,exportedAt);
    updated.put(new JSONObject().put("start",start).put("end",end));
   }
   all.put(account,new JSONObject().put("syncedAt",now).put("periods",updated));
   return all.toString();
  }catch(Exception error){
   android.util.Log.w("ClashTimerHub","Could not reconcile lab_boost; existing Potion duration retained",error);
   return null;
  }
 }

 private static synchronized void change(Context ctx,String account,long duration,int mode){
  // mode 0 add one research potion; 1 set exact remaining duration; 2 reset.
  SharedPreferences prefs=AppData.prefs(ctx);
  long now=System.currentTimeMillis(),sync=prefs.getLong("lastSyncAt_"+account,0L);
  Snapshot stored=load(ctx,account);
  ArrayList<Period> periods=stored.valid?stored.periods:new ArrayList<>();
  if(mode==2)periods.clear();
  else{
   Period current=null;
   if(!periods.isEmpty()){
    Period last=periods.get(periods.size()-1);
    if(last.start<=now&&last.end>now)current=last;
   }
   if(mode==0){
    if(current!=null)current.end=Math.min(current.start+MAX_DURATION,current.end+duration);
    else if(periods.size()<MAX_PERIODS)periods.add(new Period(now,now+duration));
    else throw new IllegalStateException("Te veel boostperiodes: synchroniseer opnieuw");
   }else if(mode==1){
    long end=now+duration;
    if(current!=null)current.end=end;
    else if(duration>0){
     if(periods.size()>=MAX_PERIODS)throw new IllegalStateException("Te veel boostperiodes: synchroniseer opnieuw");
     periods.add(new Period(now,end));
    }
   }
  }
  try{
   JSONObject map=new JSONObject(prefs.getString(KEY,"{}"));
   if(periods.isEmpty())map.remove(account);
   else{
    JSONArray list=new JSONArray();
    for(Period period:periods){
     if(period.end>period.start)list.put(new JSONObject().put("start",period.start).put("end",period.end));
    }
    if(list.length()==0)map.remove(account);
    else map.put(account,new JSONObject().put("syncedAt",sync).put("periods",list));
   }
   if(!prefs.edit().putString(KEY,map.toString()).commit())
    throw new IllegalStateException("Opslaan van boosttijd mislukt");
  }catch(org.json.JSONException e){throw new IllegalStateException("Boosttijd kon niet worden opgeslagen",e);}
 }
 static void addHour(Context ctx,String account){change(ctx,account,3600000L,0);}
 static void setRemaining(Context ctx,String account,int hours,int minutes){
  if(hours<0||hours>72||minutes<0||minutes>59||(hours==72&&minutes!=0))
   throw new IllegalArgumentException("Voer maximaal 72 uur in");
  change(ctx,account,(hours*60L+minutes)*MINUTE,1);
 }
 static void reset(Context ctx,String account){change(ctx,account,0L,2);}

 /** Integrate remaining boost over normal-work milliseconds. Newer JSON
  * finish times already account for work completed before the last sync.
  */
 static long estimateFinish(Snapshot snapshot,long originalFinish){
  if(!snapshot.valid||snapshot.periods.isEmpty())return originalFinish;
  long anchor=snapshot.syncedAt>0?snapshot.syncedAt:snapshot.periods.get(0).start;
  if(originalFinish<=anchor)return originalFinish;
  long outstanding=originalFinish-anchor,clock=anchor;
  for(Period period:snapshot.periods){
   long start=Math.max(clock,period.start);
   if(start>clock){
    long before=start-clock;
    if(outstanding<=before)return clock+outstanding;
    outstanding-=before;
    clock=start;
   }
   long end=Math.max(clock,period.end);
   if(end<=clock)continue;
   long fastWork=(end-clock)*24L;
   if(outstanding<=fastWork)return clock+(outstanding+9L)/24L;
   outstanding-=fastWork;clock=end;
  }
  return clock+outstanding;
 }
}