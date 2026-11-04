package nl.clashtimerhub.v2;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** Read-only update feed. Never embeds GitHub credentials or installs an APK silently. */
final class UpdateChecker {
 interface Result { void completed(JSONObject feed,String error); }
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
 static void check(Context ctx,Result result){
  String feedUrl=AppData.prefs(ctx).getString("updateFeedUrl","").trim();
  if(!validHttps(feedUrl)){
   result.completed(null,"Geen openbare HTTPS-updatefeed ingesteld.");
   return;
  }
  new Thread(()->{
   HttpURLConnection connection=null;
   JSONObject data=null;String error=null;
   try{
    connection=(HttpURLConnection)new URL(feedUrl).openConnection();
    connection.setConnectTimeout(7000);connection.setReadTimeout(7000);
    connection.setInstanceFollowRedirects(false);
    connection.setRequestProperty("Accept","application/json");
    if(connection.getResponseCode()!=200)throw new java.io.IOException("HTTP "+connection.getResponseCode());
    ByteArrayOutputStream buffer=new ByteArrayOutputStream();
    try(InputStream stream=connection.getInputStream()){
     byte[] chunk=new byte[1024];int n;
     while((n=stream.read(chunk))!=-1){
      if(buffer.size()+n>16384)throw new java.io.IOException("Versiebestand te groot");
      buffer.write(chunk,0,n);
     }
    }
    data=new JSONObject(new String(buffer.toByteArray(),StandardCharsets.UTF_8));
    if(data.optLong("versionCode",0)<=0||!validHttps(data.optString("apkUrl","")))
     throw new java.io.IOException("Ongeldige versie-informatie");
   }catch(Exception e){error=e.getMessage();}
   finally{if(connection!=null)connection.disconnect();}
   JSONObject finalData=data;String finalError=error;
   new Handler(Looper.getMainLooper()).post(()->result.completed(finalError==null?finalData:null,finalError));
  },"ClashTimerHub-update-check").start();
 }
}