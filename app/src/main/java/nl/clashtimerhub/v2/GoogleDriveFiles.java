package nl.clashtimerhub.v2;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Visible My Drive JSON files managed by ClashTimerHub using drive.file scope.
 * Only app-created or user-approved files are accessible. No hidden appDataFolder.
 */
final class GoogleDriveFiles {
 static final String SCOPE="https://www.googleapis.com/auth/drive.file";
 static final String FILENAME="ClashTimerHub-backup.json";
 static final String FILE_ID="googleDriveBackupId";
 static final String FILE_NAME="googleDriveBackupName";
 static final String AUTO="googleDriveBackupAuto";
 static final String LAST_TIME="googleDriveBackupLastTime";
 static final String LAST_ERROR="googleDriveBackupLastError";
 private static final String API="https://www.googleapis.com/drive/v3/files";
 private static final String UPLOAD="https://www.googleapis.com/upload/drive/v3/files";
 private static final int MAX_BYTES=2_000_000;
 static final ExecutorService IO=Executors.newSingleThreadExecutor();
 private static final OkHttpClient HTTP=new OkHttpClient.Builder()
  .connectTimeout(10,TimeUnit.SECONDS).readTimeout(25,TimeUnit.SECONDS)
  .writeTimeout(25,TimeUnit.SECONDS).build();
 private static final MediaType JSON=MediaType.parse("application/json; charset=utf-8");

 static final class FileItem{
  final String id,name,modified;
  FileItem(JSONObject obj){
   id=obj.optString("id","");
   name=obj.optString("name","");
   modified=obj.optString("modifiedTime","");
  }
  @Override public String toString(){
   return name+"   ·   "+(modified.length()>=10?modified.substring(0,10):modified);
  }
 }
 static String selectedId(Context ctx){return AppData.prefs(ctx).getString(FILE_ID,"");}
 static String selectedName(Context ctx){return AppData.prefs(ctx).getString(FILE_NAME,"");}
 static boolean auto(Context ctx){return !selectedId(ctx).isEmpty()&&AppData.prefs(ctx).getBoolean(AUTO,false);}
 static void select(Context ctx,FileItem item){
  AppData.prefs(ctx).edit().putString(FILE_ID,item.id)
   .putString(FILE_NAME,item.name).putBoolean(AUTO,false)
   .remove(LAST_ERROR).apply();
 }
 static void auto(Context ctx,boolean enabled){
  AppData.prefs(ctx).edit().putBoolean(AUTO,enabled&&!selectedId(ctx).isEmpty()).apply();
 }
 static void clear(Context ctx){
  AppData.prefs(ctx).edit().remove(FILE_ID).remove(FILE_NAME)
   .putBoolean(AUTO,false).remove(LAST_ERROR).apply();
 }
 static void success(Context ctx,FileItem file){
  AppData.prefs(ctx).edit().putString(FILE_ID,file.id)
   .putString(FILE_NAME,file.name).putLong(LAST_TIME,System.currentTimeMillis())
   .remove(LAST_ERROR).apply();
 }
 static void error(Context ctx,Exception e){
  String message=e.getMessage()==null?"Google Drive-fout":e.getMessage();
  AppData.prefs(ctx).edit().putString(LAST_ERROR,message).apply();
  Log.e("ClashTimerHub","Google Drive operation failed",e);
 }
 static String status(Context ctx){
  SharedPreferences p=AppData.prefs(ctx);
  String id=p.getString(FILE_ID,"");
  if(id.isEmpty())return "Nog geen bestand in Mijn Drive geselecteerd";
  String name=p.getString(FILE_NAME,FILENAME);
  long last=p.getLong(LAST_TIME,0);
  String when=last==0?"nog niet opgeslagen":
   new java.text.SimpleDateFormat("dd-MM HH:mm",java.util.Locale.getDefault())
    .format(new java.util.Date(last));
  String issue=p.getString(LAST_ERROR,"");
  return name+" · "+when+(auto(ctx)?" · Auto-back-up aan":"")+
   (issue.isEmpty()?"":"\nFout: "+issue);
 }

 static ArrayList<FileItem> list(String token)throws Exception{
  // drive.file deliberately lists only files this app created/has been granted access to.
  // Some previous versions may lack the appProperty; include JSON files with the
  // conventional filename as well, but never unrelated Drive content.
  String query="trashed = false and mimeType = 'application/json'";
  String url=API+"?fields="+enc("nextPageToken,files(id,name,modifiedTime,mimeType)")+
   "&q="+enc(query)+"&pageSize=100&orderBy="+enc("modifiedTime desc");
  ArrayList<FileItem> files=new ArrayList<>();
  for(int page=0;page<5;page++){
   JSONObject result=request(token,new Request.Builder().url(url).get());
   JSONArray entries=result.optJSONArray("files");
   if(entries!=null)for(int i=0;i<entries.length();i++){
    JSONObject obj=entries.optJSONObject(i);
    if(obj!=null&&obj.optString("name").toLowerCase(java.util.Locale.ROOT)
        .endsWith(".json")&&!obj.optString("id","").isEmpty())
     files.add(new FileItem(obj));
   }
   String next=result.optString("nextPageToken","");
   if(next.isEmpty())break;
   url=API+"?fields="+enc("nextPageToken,files(id,name,modifiedTime,mimeType)")+
    "&q="+enc(query)+"&pageSize=100&orderBy="+enc("modifiedTime desc")+
    "&pageToken="+enc(next);
  }
  return files;
 }
 static FileItem create(String token,String filename,byte[] json)throws Exception{
  checkSize(json);
  String safe=filename.trim();
  if(safe.length()==0||safe.length()>100||safe.contains("/")||safe.contains("\\"))
   throw new IOException("Ongeldige naam voor back-upbestand");
  if(!safe.toLowerCase(java.util.Locale.ROOT).endsWith(".json"))safe+=".json";
  JSONObject meta=new JSONObject().put("name",safe)
   .put("mimeType","application/json")
   .put("parents",new JSONArray().put("root"))
   .put("appProperties",new JSONObject().put("clashtimerhub","backup-v1"));
  String boundary="clashtimerhub-"+System.currentTimeMillis();
  ByteArrayOutputStream buffer=new ByteArrayOutputStream();
  buffer.write(("--"+boundary+"\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n"+
   meta.toString()+"\r\n").getBytes(StandardCharsets.UTF_8));
  buffer.write(("--"+boundary+"\r\nContent-Type: application/json\r\n\r\n")
   .getBytes(StandardCharsets.UTF_8));
  buffer.write(json);
  buffer.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
  RequestBody body=RequestBody.create(MediaType.parse("multipart/related; boundary="+boundary),
   buffer.toByteArray());
  JSONObject out=request(token,new Request.Builder().url(UPLOAD+
   "?uploadType=multipart&fields=id,name,modifiedTime").post(body));
  FileItem item=new FileItem(out);
  if(item.id.isEmpty())throw new IOException("Drive heeft geen bestand-ID teruggegeven");
  return item;
 }
 static FileItem overwrite(String token,FileItem file,byte[] json)throws Exception{
  checkId(file.id);checkSize(json);
  RequestBody body=RequestBody.create(JSON,json);
  JSONObject out=request(token,new Request.Builder().url(UPLOAD+"/"+enc(file.id)+
   "?uploadType=media&fields=id,name,modifiedTime").patch(body));
  if(out.optString("id","").isEmpty())throw new IOException("Drive kon vervanging niet bevestigen");
  return new FileItem(out);
 }
 static String download(String token,FileItem file)throws Exception{
  checkId(file.id);
  Request req=new Request.Builder().url(API+"/"+enc(file.id)+"?alt=media")
   .header("Authorization","Bearer "+token).get().build();
  try(Response response=HTTP.newCall(req).execute()){
   if(!response.isSuccessful())throw httpError(response);
   ResponseBody body=response.body();
   if(body==null)throw new IOException("Leeg Drive-back-upbestand");
   ByteArrayOutputStream data=new ByteArrayOutputStream();
   try(InputStream in=body.byteStream()){
    byte[] chunk=new byte[8192];int n;
    while((n=in.read(chunk))!=-1){
     if(data.size()+n>MAX_BYTES)throw new IOException("Back-up te groot (maximaal 2 MB)");
     data.write(chunk,0,n);
    }
   }
   String text=new String(data.toByteArray(),StandardCharsets.UTF_8);
   JSONObject obj=new JSONObject(text);
   if(!"clash-timer-hub-backup-v1".equals(obj.optString("format"))||
      obj.optJSONArray("accounts")==null||obj.optJSONArray("upgrades")==null)
    throw new IOException("Dit is geen ClashTimerHub-back-up");
   return text;
  }
 }
 private static JSONObject request(String token,Request.Builder builder)throws Exception{
  Request req=builder.header("Authorization","Bearer "+token)
   .header("Accept","application/json").build();
  try(Response response=HTTP.newCall(req).execute()){
   if(!response.isSuccessful())throw httpError(response);
   ResponseBody body=response.body();
   if(body==null)throw new IOException("Geen antwoord van Google Drive");
   String raw=body.string();
   if(raw.length()>300_000)throw new IOException("Te veel Drive-bestandsgegevens");
   return new JSONObject(raw);
  }
 }
 private static IOException httpError(Response response)throws IOException{
  String detail="";
  if(response.body()!=null){
   String raw=response.body().string();
   if(raw.length()>300)raw=raw.substring(0,300);
   detail=raw;
  }
  if(response.code()==401)return new IOException(
   "Google-accounttoestemming verlopen. Koppel Google Drive opnieuw.");
  if(response.code()==403)return new IOException(
   "Drive-toegang geweigerd. Controleer Google Drive API en OAuth-instellingen.");
  if(response.code()==404)return new IOException(
   "Back-upbestand bestaat niet meer of is niet toegankelijk voor ClashTimerHub.");
  return new IOException("Google Drive HTTP "+response.code()+": "+detail);
 }
 private static void checkId(String id)throws IOException{
  if(id==null||id.isEmpty()||!id.matches("[a-zA-Z0-9_-]{8,256}"))
   throw new IOException("Ongeldig Google Drive-bestand-ID");
 }
 private static void checkSize(byte[] data)throws IOException{
  if(data==null||data.length==0||data.length>MAX_BYTES)
   throw new IOException("Back-up mag niet leeg of groter dan 2 MB zijn");
 }
 private static String enc(String s)throws Exception{
  return URLEncoder.encode(s,"UTF-8");
 }
 private GoogleDriveFiles(){}
}
