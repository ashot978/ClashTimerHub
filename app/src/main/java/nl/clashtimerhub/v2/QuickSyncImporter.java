package nl.clashtimerhub.v2;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.util.*;

/** Atomic, account-scoped clipboard import for the floating shortcut. */
public final class QuickSyncImporter {
 private QuickSyncImporter(){}
 public static String importJson(Context context,String raw)throws Exception{
  if(raw==null||raw.length()>10000000)throw new IllegalArgumentException("Klembord is leeg of te groot");
  int start=raw.indexOf('{'),end=raw.lastIndexOf('}');
  if(start<0||end<=start)throw new IllegalArgumentException("Geen JSON gevonden");
  JSONObject json=new JSONObject(raw.substring(start,end+1));
  // Reject truncated or malformed snapshots before touching previously stored timers.
  int availableSections=0;
  for(String section:new String[]{"buildings","heroes","units","spells","pets","traps"}){
   if(!json.has(section))continue;
   if(!(json.opt(section) instanceof JSONArray))
    throw new IllegalArgumentException("Ongeldige sectie: "+section+". Bestaande timers blijven behouden.");
   availableSections++;
  }
  if(availableSections==0)throw new IllegalArgumentException("Geen Clash-spelerdata");
  if(!json.has("buildings")||!json.has("heroes")||!json.has("units"))
   throw new IllegalArgumentException("Export is onvolledig (buildings/heroes/units ontbreken). Maak een volledige Clash-export.");
  JSONObject player=json.optJSONObject("player");
  String tag=json.optString("tag",json.optString("playerTag",player==null?"":player.optString("tag",""))).trim().toUpperCase(Locale.ROOT);
  if(tag.isEmpty())throw new IllegalArgumentException("Player tag ontbreekt: importeer via de app");
  AppData.migrate(context);
  SharedPreferences prefs=AppData.prefs(context);
  JSONArray oldAccounts=new JSONArray(prefs.getString("accounts","[]"));
  JSONArray oldUpgrades=new JSONArray(prefs.getString("upgrades","[]"));
  JSONObject tags=new JSONObject(prefs.getString("playerTags","{}"));
  ArrayList<String> accounts=new ArrayList<>();
  for(int i=0;i<oldAccounts.length();i++)accounts.add(oldAccounts.optString(i));
  String account=null;
  for(String name:accounts)if(tag.equalsIgnoreCase(tags.optString(name,""))){account=name;break;}
  String playerName=json.optString("name",player==null?"":player.optString("name","")).trim();
  if(account==null&&!playerName.isEmpty())for(String existing:accounts){
   if(existing.equalsIgnoreCase(playerName)&&tags.optString(existing,"").isEmpty()){account=existing;break;}
  }
  // Preserve stable account keys, but save the real player name as a display label.
  // Never overwrite a name that the user chose manually.
  if(account!=null&&!playerName.isEmpty()&&!playerName.equalsIgnoreCase(account)){
   JSONObject names=new JSONObject(prefs.getString("accountNames","{}"));
   if(names.optString(account,"").trim().isEmpty()){
    names.put(account,playerName);
    prefs.edit().putString("accountNames",names.toString()).apply();
   }
  }
  if(account==null){
   String base=playerName.isEmpty()?tag:playerName;
   account=base;int suffix=2;
   while(accounts.contains(account))account=base+" ("+(suffix++)+")";
   accounts.add(account);
  }
  long now=System.currentTimeMillis();
  long timestamp=json.optLong("timestamp",now/1000L)*1000L;
  if(timestamp>now+3600000L||timestamp<now-30L*86400000L)throw new IllegalArgumentException("Exporttijd is ongeldig of te oud");
  // A missing section must never silently clear active timers of that category.
  HashSet<String> previousCategories=new HashSet<>();
  for(int i=0;i<oldUpgrades.length();i++){
   JSONObject old=oldUpgrades.optJSONObject(i);
   if(old!=null&&account.equals(old.optString("account"))&&old.optBoolean("exported",false)&&old.optLong("finish")>now)
    previousCategories.add(old.optString("category"));
  }
  for(String cat:previousCategories){
   boolean present="Pets".equals(cat)?json.has("pets"):
    "Laboratorium".equals(cat)?json.has("units")&&json.has("spells"):
    json.has("buildings")&&json.has("heroes")&&json.has("traps");
   if(!present)throw new IllegalArgumentException("Export mist "+cat+"; eerdere timers zijn niet overschreven.");
  }
  JSONArray parsed=new JSONArray();
  String[] sections={"buildings","heroes","units","spells","pets","traps"};
  for(String section:sections){
   JSONArray arr=json.optJSONArray(section);if(arr==null)continue;
   for(int i=0;i<arr.length();i++){
    JSONObject entry=arr.optJSONObject(i);if(entry==null)continue;
    long id=entry.optLong("data",-1);if(id<0)continue;
    String category="pets".equals(section)?"Pets":("units".equals(section)||"spells".equals(section))?"Laboratorium":"Bouwers";
    JSONArray types=entry.optJSONArray("types");
    if(types!=null)for(int t=0;t<types.length();t++){
     JSONObject type=types.optJSONObject(t);if(type==null)continue;
     JSONArray modules=type.optJSONArray("modules");if(modules==null)continue;
     for(int m=0;m<modules.length();m++){
      JSONObject module=modules.optJSONObject(m);if(module!=null)add(parsed,account,category,module.optLong("data",-1),module.optLong("timer"),timestamp,tag,section+"/module/"+id+"/"+t+"/"+m);
     }
    }
    JSONArray timers=entry.optJSONArray("timers");
    if(timers!=null){
     for(int j=0;j<timers.length();j++){
      Object value=timers.opt(j);
      long seconds=value instanceof JSONObject?((JSONObject)value).optLong("timer"):timers.optLong(j);
      add(parsed,account,category,id,seconds,timestamp,tag,section+"/timer/"+id+"/"+j);
     }
    }else add(parsed,account,category,id,entry.optLong("timer"),timestamp,tag,section+"/item/"+id+"/"+i);
   }
  }
  // Count actual differences against the previous export. Do not infer
  // whether a changed end time represents a new upgrade or an edit.
  ArrayList<JSONObject> previous=new ArrayList<>();
  for(int i=0;i<oldUpgrades.length();i++){
   JSONObject old=oldUpgrades.optJSONObject(i);
   if(old!=null&&account.equals(old.optString("account"))&&old.optBoolean("exported",false))previous.add(old);
  }
  HashSet<Integer> matched=new HashSet<>();
  int newOrChanged=0;
  for(int i=0;i<parsed.length();i++){
   JSONObject incoming=parsed.optJSONObject(i);
   if(incoming==null)continue;
   long closest=Long.MAX_VALUE;int found=-1;
   for(int j=0;j<previous.size();j++){
    if(matched.contains(j))continue;
    JSONObject old=previous.get(j);
    if(!incoming.optString("category").equals(old.optString("category")))continue;
    long oldId=old.optLong("sourceId",-1),newId=incoming.optLong("sourceId",-1);
    if(oldId>=0&&newId>=0?oldId!=newId:!incoming.optString("name").equals(old.optString("name")))continue;
    long delta=Math.abs(old.optLong("finish")-incoming.optLong("finish"));
    if(delta<closest){closest=delta;found=j;}
   }
   // Timer finish times can drift slightly between exports.
   if(found>=0&&closest<=120000L)matched.add(found);
   else newOrChanged++;
  }
  int noLongerActive=0;
  for(int i=0;i<previous.size();i++)
   if(!matched.contains(i)&&previous.get(i).optLong("finish")>now)noLongerActive++;
  int differences=Math.max(newOrChanged,noLongerActive);
  JSONArray merged=new JSONArray();
  for(int i=0;i<oldUpgrades.length();i++){
   JSONObject upgrade=oldUpgrades.optJSONObject(i);
   if(upgrade!=null&&!(account.equals(upgrade.optString("account"))&&upgrade.optBoolean("exported",false)))merged.put(upgrade);
  }
  for(int i=0;i<parsed.length();i++)merged.put(parsed.getJSONObject(i));
  if(!playerName.isEmpty()&&!playerName.equalsIgnoreCase(account)){
   JSONObject names=new JSONObject(prefs.getString("accountNames","{}"));
   if(names.optString(account,"").trim().isEmpty()){
    names.put(account,playerName);
    prefs.edit().putString("accountNames",names.toString()).apply();
   }
  }
  tags.put(account,tag);
  int builders=0,laboratory=0,pets=0;
  for(int i=0;i<parsed.length();i++){
   String cat=parsed.getJSONObject(i).optString("category");
   if("Bouwers".equals(cat))builders++;
   else if("Laboratorium".equals(cat))laboratory++;
   else if("Pets".equals(cat))pets++;
  }
  String status=AppData.displayName(context,account)+" ✓ "+parsed.length()+" timers (⚒ "+builders+" · ⚗ "+laboratory+" · 🐾 "+pets+")";
  if(previous.isEmpty())status+="\nEerste volledige synchronisatie";
  else if(differences==0)status+="\nGeen verschillen met vorige sync";
  else status+="\nWijzigingen sinds vorige sync gevonden";
  long revision=prefs.getLong("syncRevision",0L)+1L;
  SharedPreferences.Editor editor=prefs.edit().putString("accounts",new JSONArray(accounts).toString())
   .putString("upgrades",merged.toString()).putString("playerTags",tags.toString())
   .putLong("ack_"+account,now).putLong("syncRevision",revision)
   .putLong("lastSyncAt_"+account,now).putString("lastSyncSummary",status);
  if(!editor.commit())throw new IllegalStateException("Opslaan mislukt");
  TimerWidget.schedule(context);
  // Run Drive backup independently: it must never make a successful Clash sync fail.
  GoogleDriveUi.afterSuccessfulSync(context);
  return status;
 }

 private static void add(JSONArray out,String account,String category,long id,long seconds,long timestamp,String tag,String key)throws JSONException{
  if(id<0||seconds<=0)return;
  long finish=timestamp+seconds*1000L;
  if(finish<=System.currentTimeMillis())return;
  JSONObject timer=new JSONObject();
  String stable=java.util.UUID.nameUUIDFromBytes((tag+"|"+key).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
  timer.put("id",stable);timer.put("syncKey",key);timer.put("account",account);
  timer.put("category",category);timer.put("name",ClashNames.name(id));
  timer.put("finish",finish);timer.put("exported",true);timer.put("sourceId",id);
  out.put(timer);
 }
}
