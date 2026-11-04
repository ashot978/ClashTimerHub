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
  if(!json.has("buildings")&&!json.has("heroes")&&!json.has("units"))throw new IllegalArgumentException("Geen Clash-spelerdata");
  JSONObject player=json.optJSONObject("player");
  String tag=json.optString("tag",json.optString("playerTag",player==null?"":player.optString("tag",""))).trim().toUpperCase(Locale.ROOT);
  if(tag.isEmpty())throw new IllegalArgumentException("Player tag ontbreekt: importeer via de app");
  SharedPreferences prefs=context.getSharedPreferences("MainActivity",Context.MODE_PRIVATE);
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
  if(account==null){
   String base=playerName.isEmpty()?tag:playerName;
   account=base;int suffix=2;
   while(accounts.contains(account))account=base+" ("+(suffix++)+")";
   accounts.add(account);
  }
  long now=System.currentTimeMillis();
  long timestamp=json.optLong("timestamp",now/1000L)*1000L;
  if(timestamp>now+3600000L||timestamp<now-30L*86400000L)throw new IllegalArgumentException("Exporttijd is ongeldig of te oud");
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
  // Reconcile only the matching player. A sync must never touch manual timers
  // or another account, and identical snapshots must not create new identities.
  Map<String,JSONObject> oldTimers=new LinkedHashMap<>();
  for(int i=0;i<oldUpgrades.length();i++){
   JSONObject old=oldUpgrades.optJSONObject(i);
   if(old!=null&&account.equals(old.optString("account"))&&old.optBoolean("exported",false)){
    String key=old.optString("syncKey","");
    if(key.isEmpty())key=old.optString("category")+"|"+old.optLong("sourceId",-1)+"|"+old.optString("name");
    oldTimers.put(key,old);
   }
  }
  int added=0,changed=0,finished=0;
  Set<String> seen=new HashSet<>();
  for(int i=0;i<parsed.length();i++){
   JSONObject timer=parsed.getJSONObject(i);
   String key=timer.optString("syncKey");
   JSONObject before=oldTimers.get(key);
   if(before==null){
    String legacy=timer.optString("category")+"|"+timer.optLong("sourceId",-1)+"|"+timer.optString("name");
    before=oldTimers.get(legacy);
    if(before!=null)seen.add(legacy);
   }
   if(before==null)added++;
   else{
    seen.add(key);
    if(Math.abs(before.optLong("finish")-timer.optLong("finish"))>60000L)changed++;
   }
  }
  for(Map.Entry<String,JSONObject> old:oldTimers.entrySet()){
   if(!seen.contains(old.getKey())&&old.getValue().optLong("finish")<=now)finished++;
  }
  JSONArray merged=new JSONArray();
  for(int i=0;i<oldUpgrades.length();i++){
   JSONObject upgrade=oldUpgrades.optJSONObject(i);
   if(upgrade!=null&&!(account.equals(upgrade.optString("account"))&&upgrade.optBoolean("exported",false)))merged.put(upgrade);
  }
  for(int i=0;i<parsed.length();i++)merged.put(parsed.getJSONObject(i));
  tags.put(account,tag);
  String status=account+": "+parsed.length()+" actief · "+added+" nieuw · "+finished+" klaar · "+changed+" gewijzigd";
  long revision=prefs.getLong("syncRevision",0L)+1L;
  SharedPreferences.Editor editor=prefs.edit().putString("accounts",new JSONArray(accounts).toString())
   .putString("upgrades",merged.toString()).putString("playerTags",tags.toString())
   .putLong("ack_"+account,now).putLong("syncRevision",revision)
   .putLong("lastSyncAt_"+account,now).putString("lastSyncSummary",status);
  if(!editor.commit())throw new IllegalStateException("Opslaan mislukt");
  TimerWidget.schedule(context);
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
