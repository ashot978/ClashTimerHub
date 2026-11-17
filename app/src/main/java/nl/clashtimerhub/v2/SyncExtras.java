package nl.clashtimerhub.v2;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.LinkedHashMap;

/** Persist only confirmed in-game export fields; missing sections stay untouched. */
final class SyncExtras {
 static final String KEY="syncExtrasByAccount";
 static final String[] SECTIONS={"equipment","decos","decos2","obstacles","obstacles2",
  "skins","skins2","sceneries","sceneries2"};
 private SyncExtras(){}
 static JSONObject normalize(JSONObject previous,JSONObject export,long exportedAt)throws JSONException{
  JSONObject next=previous==null?new JSONObject():new JSONObject(previous.toString());
  boolean changed=false;
  for(String section:SECTIONS){
   if(!export.has(section))continue;
   JSONArray entries=export.optJSONArray(section);
   if(entries==null)throw new JSONException("Ongeldige collectie-sectie: "+section);
   if(entries.length()>12000)throw new JSONException("Te veel items in "+section);
   LinkedHashMap<Long,JSONObject> map=new LinkedHashMap<>();
   int unknownShape=0;
   for(int i=0;i<entries.length();i++){
    Object value=entries.opt(i);
    long id=-1L;int level=-1,count=1;
    if(value instanceof JSONObject){
     JSONObject obj=(JSONObject)value;
     id=obj.optLong("data",-1L);
     level=obj.has("lvl")?obj.optInt("lvl",-1):-1;
     count=obj.optInt("cnt",1);
    }else if(value instanceof Number)id=((Number)value).longValue();
    else{unknownShape++;continue;}
    if(id<=0||id>2000000000L||count<0||count>10000){unknownShape++;continue;}
    if(count==0)continue;
    JSONObject found=map.get(id);
    if(found==null){
     found=new JSONObject().put("data",id);
     if("equipment".equals(section))found.put("lvl",Math.max(0,level));
     else found.put("cnt",count);
     map.put(id,found);
    }else if("equipment".equals(section)){
     found.put("lvl",Math.max(found.optInt("lvl",0),level));
    }else{
     found.put("cnt",Math.min(100000,found.optInt("cnt",0)+count));
    }
   }
   // If all parsed rows are unknown shapes, previous ownership data is retained.
   if(entries.length()>0&&map.isEmpty()&&unknownShape>0){
    next.put(section+"_unreadable",true);
    continue;
   }
   JSONArray rows=new JSONArray();
   for(JSONObject item:map.values())rows.put(item);
   next.put(section,rows);
   next.remove(section+"_unreadable");
   changed=true;
  }
  if(changed)next.put("exportedAt",exportedAt);
  return next;
 }
 static String merged(SharedPreferences prefs,JSONObject export,String account,long exportedAt)throws JSONException{
  boolean hasAny=false;for(String key:SECTIONS)if(export.has(key)){hasAny=true;break;}
  if(!hasAny)return null;
  JSONObject all=new JSONObject(prefs.getString(KEY,"{}"));
  all.put(account,normalize(all.optJSONObject(account),export,exportedAt));
  return all.toString();
 }
 static JSONObject account(Context context,String name){
  try{
   JSONObject all=new JSONObject(AppData.prefs(context).getString(KEY,"{}"));
   JSONObject found=all.optJSONObject(name);
   return found==null?new JSONObject():found;
  }catch(Exception ex){return new JSONObject();}
 }
}
