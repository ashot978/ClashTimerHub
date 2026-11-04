package nl.clashtimerhub.v2;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;

/** One persistent data source for Activity, overlay service, alarms and widgets. */
final class AppData {
 private AppData(){}
 static SharedPreferences prefs(Context context){
  // Activity.getPreferences() uses getLocalClassName(). Since applicationId
  // differs from the Java package name, that is the fully qualified class name.
  return context.getSharedPreferences(MainActivity.class.getName(),Context.MODE_PRIVATE);
 }
 /** Friendly labels are presentation-only; player tags and timer keys remain unchanged. */
 static String displayName(Context context,String account){
  if(account==null)return "";
  try{
   JSONObject labels=new JSONObject(prefs(context).getString("accountNames","{}"));
   String label=labels.optString(account,"").trim();
   return label.isEmpty()?account:label;
  }catch(Exception ignored){return account;}
 }
 static void saveAccountName(Context context,String account,String display){
  try{
   SharedPreferences p=prefs(context);
   JSONObject labels=new JSONObject(p.getString("accountNames","{}"));
   String value=display==null?"":display.trim();
   if(value.isEmpty()||value.equals(account))labels.remove(account);
   else labels.put(account,value);
   p.edit().putString("accountNames",labels.toString()).apply();
  }catch(Exception e){android.util.Log.e("ClashTimerHub","Could not save account display name",e);}
 }
 /** A checked MAX flag is manually maintained, per account and upgrade category. */
 static boolean isMaxed(Context context,String account,String category){
  try{
   JSONObject map=new JSONObject(prefs(context).getString("maxedCategories","{}"));
   JSONObject entry=map.optJSONObject(account);
   return entry!=null&&entry.optBoolean(category,false);
  }catch(Exception e){return false;}
 }
 static void setMaxed(Context context,String account,String category,boolean maxed){
  if(!("Bouwers".equals(category)||"Laboratorium".equals(category)||"Pets".equals(category)))return;
  try{
   SharedPreferences p=prefs(context);
   JSONObject map=new JSONObject(p.getString("maxedCategories","{}"));
   JSONObject entry=map.optJSONObject(account);
   if(entry==null)entry=new JSONObject();
   if(maxed)entry.put(category,true);
   else entry.remove(category);
   if(entry.length()==0)map.remove(account);
   else map.put(account,entry);
   p.edit().putString("maxedCategories",map.toString()).apply();
  }catch(Exception e){android.util.Log.e("ClashTimerHub","Could not store MAX category",e);}
 }
 /** Keep MAX settings attached when a tag-only account is linked to a named one. */
 static void mergeMaxedAccount(Context context,String source,String target){
  try{
   SharedPreferences p=prefs(context);
   JSONObject map=new JSONObject(p.getString("maxedCategories","{}"));
   JSONObject from=map.optJSONObject(source);
   if(from==null)return;
   JSONObject to=map.optJSONObject(target);
   if(to==null)to=new JSONObject();
   for(String category:new String[]{"Bouwers","Laboratorium","Pets"})
    if(from.optBoolean(category,false)&&!to.has(category))to.put(category,true);
   map.put(target,to);map.remove(source);
   p.edit().putString("maxedCategories",map.toString()).apply();
  }catch(Exception e){android.util.Log.e("ClashTimerHub","Could not merge MAX categories",e);}
 }
 static synchronized void migrate(Context context){
  SharedPreferences current=prefs(context);
  if(current.getBoolean("legacyOverlayMigratedV6",false))return;
  SharedPreferences legacy=context.getSharedPreferences("MainActivity",Context.MODE_PRIVATE);
  try{
   JSONArray baseAccounts=new JSONArray(current.getString("accounts","[]"));
   JSONArray baseTimers=new JSONArray(current.getString("upgrades","[]"));
   JSONArray otherAccounts=new JSONArray(legacy.getString("accounts","[]"));
   JSONArray otherTimers=new JSONArray(legacy.getString("upgrades","[]"));
   JSONObject baseTags=new JSONObject(current.getString("playerTags","{}"));
   JSONObject otherTags=new JSONObject(legacy.getString("playerTags","{}"));
   HashSet<String> names=new HashSet<>();
   for(int i=0;i<baseAccounts.length();i++)names.add(baseAccounts.optString(i));
   boolean modified=false;
   for(int i=0;i<otherAccounts.length();i++){
    String name=otherAccounts.optString(i,"");
    if(name.isEmpty())continue;
    String incomingTag=otherTags.optString(name,"");
    if(names.contains(name)){
     if(incomingTag.isEmpty())continue;
     String existingTag=baseTags.optString(name,"");
     if(!existingTag.isEmpty()&&!existingTag.equalsIgnoreCase(incomingTag))continue;
     long otherTime=legacy.getLong("lastSyncAt_"+name,0L);
     long localTime=current.getLong("lastSyncAt_"+name,0L);
     if(otherTime<=localTime)continue;
     JSONArray clean=new JSONArray();
     for(int j=0;j<baseTimers.length();j++){
      JSONObject timer=baseTimers.optJSONObject(j);
      if(timer!=null&&!(name.equals(timer.optString("account"))&&timer.optBoolean("exported",false)))clean.put(timer);
     }
     baseTimers=clean;
    }else{
     baseAccounts.put(name);names.add(name);
    }
    if(!incomingTag.isEmpty())baseTags.put(name,incomingTag);
    for(int j=0;j<otherTimers.length();j++){
     JSONObject timer=otherTimers.optJSONObject(j);
     if(timer!=null&&name.equals(timer.optString("account")))baseTimers.put(timer);
    }
    modified=true;
   }
   SharedPreferences.Editor e=current.edit().putBoolean("legacyOverlayMigratedV6",true);
   if(modified)e.putString("accounts",baseAccounts.toString())
     .putString("upgrades",baseTimers.toString()).putString("playerTags",baseTags.toString())
     .putLong("syncRevision",current.getLong("syncRevision",0)+1);
   e.commit();
  }catch(Exception ex){
   android.util.Log.e("ClashTimerHub","Legacy preferences migration failed",ex);
  }
 }
}
