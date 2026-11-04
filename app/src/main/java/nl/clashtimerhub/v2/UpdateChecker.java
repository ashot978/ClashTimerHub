package nl.clashtimerhub.v2;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import org.json.JSONArray;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** Checks the newest published GitHub Release (or an explicitly configured HTTPS feed). */
final class UpdateChecker {
 interface Result { void completed(JSONObject feed,String error); }
 private static final String RELEASE_URL="https://api.github.com/repos/ashot978/ClashTimerHub/releases/latest";
 private UpdateChecker(){}
 static long installedVersionCode(Context ctx){
  try{
   android.content.pm.PackageInfo p=ctx.getPackageManager().getPackageInfo(ctx.getPackageName(),0);
   return android.os.Build.VERSION.SDK_INT>=28?p.getLongVersionCode():p.versionCode;
  }catch(Exception e){return 0;}
 }
 static boolean validHttps(String raw){
  try{
   URL url=new URL(raw);
   return "https".equalsIgnoreCase(url.getProtocol())&&url.getHost()!=null&&!url.getHost().isEmpty()&&url.getUserInfo()==null;
  }catch(Exception e){return false;}
 }
 private static JSONObject getJSON(String url)throws Exception{
  HttpURLConnection connection=null;
  try{
   connection=(HttpURLConnection)new URL(url).openConnection();
   connection.setConnectTimeout(8000);connection.setReadTimeout(8000);
   connection.setInstanceFollowRedirects(false);
   connection.setRequestProperty("Accept","application/vnd.github+json");
   connection.setRequestProperty("User-Agent","ClashTimerHub-Android");
   if(connection.getResponseCode()!=200)
    throw new java.io.IOException("GitHub HTTP "+connection.getResponseCode());
   ByteArrayOutputStream buffer=new ByteArrayOutputStream();
   try(InputStream stream=connection.getInputStream()){
    byte[] chunk=new byte[2048];int n;
    while((n=stream.read(chunk))!=-1){
     if(buffer.size()+n>65536)throw new java.io.IOException("Versie-informatie te groot");
     buffer.write(chunk,0,n);
    }
   }
   return new JSONObject(new String(buffer.toByteArray(),StandardCharsets.UTF_8));
  }finally{if(connection!=null)connection.disconnect();}
 }
 private static JSONObject releaseInfo(JSONObject release)throws Exception{
  String tag=release.optString("tag_name","");
  if(!tag.matches("build-[0-9]+"))throw new java.io.IOException("Ongeldige release-tag");
  long build=Long.parseLong(tag.substring(6));
  if(build<1||build>1000000000L)throw new java.io.IOException("Ongeldig buildnummer");
  JSONArray assets=release.optJSONArray("assets");
  String apk="",sha256="";
  if(assets!=null)for(int i=0;i<assets.length();i++){
   JSONObject asset=assets.optJSONObject(i);
   if(asset==null)continue;
   if("ClashTimerHub.apk".equals(asset.optString("name"))){
    apk=asset.optString("browser_download_url","");
    sha256=asset.optString("digest","");
    break;
   }
  }
  if(!validHttps(apk)||!new URL(apk).getHost().equalsIgnoreCase("github.com"))
   throw new java.io.IOException("Geen veilige APK in de nieuwste release");
  JSONObject normalized=new JSONObject();
  normalized.put("build",build).put("versionCode",1000+build)
   .put("version",release.optString("name","Build #"+build))
   .put("apkUrl",apk).put("sha256",sha256).put("notes",release.optString("body",""));
  return normalized;
 }
 static void check(Context ctx,Result result){
  String url=RELEASE_URL; // Public repository: no external feed setup or credentials needed.
  new Thread(()->{
   JSONObject data=null;String error=null;
   try{
    JSONObject raw=getJSON(url);
    data=url.equals(RELEASE_URL)?releaseInfo(raw):raw;
    if(data.optLong("versionCode",0)<=0||!validHttps(data.optString("apkUrl","")))
     throw new java.io.IOException("Ongeldige versie-informatie");
   }catch(Exception e){error=e.getMessage();}
   JSONObject answer=data;String problem=error;
   new Handler(Looper.getMainLooper()).post(()->result.completed(problem==null?answer:null,problem));
  },"ClashTimerHub-update-check").start();
 }
}