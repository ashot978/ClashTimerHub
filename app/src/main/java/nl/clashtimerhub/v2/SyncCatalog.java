package nl.clashtimerhub.v2;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

/** Local game data names and equipment ore costs. Unknown IDs stay unknown. */
final class SyncCatalog {
 private static JSONObject catalog;
 private SyncCatalog(){}
 private static synchronized JSONObject all(Context context){
  if(catalog!=null)return catalog;
  try(InputStream input=context.getAssets().open("sync_catalog.json");
      ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] buf=new byte[8192];int n;
   while((n=input.read(buf))!=-1)out.write(buf,0,n);
   catalog=new JSONObject(out.toString("UTF-8"));
  }catch(Exception ex){
   android.util.Log.e("ClashTimerHub","Could not read item catalog",ex);
   catalog=new JSONObject();
  }
  return catalog;
 }
 static JSONObject entry(Context ctx,String section,long id){
  String type=section.endsWith("2")?section.substring(0,section.length()-1):section;
  JSONObject category=all(ctx).optJSONObject(type);
  JSONObject found=category==null?null:category.optJSONObject(String.valueOf(id));
  return found==null?new JSONObject():found;
 }
 static String name(Context ctx,String section,long id){
  return entry(ctx,section,id).optString("n","ID "+id);
 }
 static String oreText(JSONObject equip,int level,boolean total){
  JSONArray values=equip.optJSONArray("ore");int max=equip.optInt("max",0);
  if(values==null||level<1||level>=max||max>60)return "";
  int last=total?Math.min(max-1,values.length()):Math.min(level,values.length());
  long[] result=new long[3];
  for(int i=level-1;i<last;i++){
   JSONArray next=values.optJSONArray(i);
   if(next==null)continue;
   for(int j=0;j<3;j++)result[j]+=Math.max(0,next.optLong(j));
  }
  return result[0]+" Shiny · "+result[1]+" Glowy · "+result[2]+" Starry";
 }
}
