package nl.clashtimerhub.v2;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Google Drive-compatible backups using Android's Storage Access Framework.
 * The user selects and authorizes a document through the system picker;
 * the app never requests Google login credentials or broad Drive access.
 */
final class DriveBackupManager {
 static final String KEY_URI="driveBackupDocumentUri";
 static final String KEY_NAME="driveBackupDocumentName";
 static final String KEY_AUTO="driveBackupAfterSync";
 static final String KEY_TIME="driveBackupLastSuccess";
 static final String KEY_ERROR="driveBackupLastError";
 private static final int MAX_BYTES=2_000_000;
 private static final ExecutorService WORK=Executors.newSingleThreadExecutor();
 private static final Handler MAIN=new Handler(Looper.getMainLooper());

 interface Completion { void done(boolean ok,String message); }

 private DriveBackupManager(){}

 static boolean isLinked(Context ctx){
  return !AppData.prefs(ctx).getString(KEY_URI,"").isEmpty();
 }
 static boolean isAutomatic(Context ctx){
  SharedPreferences p=AppData.prefs(ctx);
  return isLinked(ctx)&&p.getBoolean(KEY_AUTO,false);
 }
 static Uri linkedUri(Context ctx){
  String str=AppData.prefs(ctx).getString(KEY_URI,"");
  return str.isEmpty()?null:Uri.parse(str);
 }

 /** Same v1 schema as the existing clipboard backup, including extended settings. */
 static JSONObject buildPayload(Context context)throws JSONException{
  SharedPreferences p=AppData.prefs(context);
  JSONObject data=new JSONObject();
  data.put("format","clash-timer-hub-backup-v1");
  data.put("accounts",new JSONArray(p.getString("accounts","[]")));
  data.put("upgrades",new JSONArray(p.getString("upgrades","[]")));
  data.put("playerTags",new JSONObject(p.getString("playerTags","{}")));
  data.put("accountNames",new JSONObject(p.getString("accountNames","{}")));
  data.put("helpersByAccount",new JSONObject(p.getString(HelperTracker.KEY,"{}")));
  data.put("alchemistAvailableByAccount",new JSONObject(p.getString(HelperTracker.ALCHEMIST_AVAILABLE_KEY,"{}")));
  data.put("maxedCategories",new JSONObject(p.getString("maxedCategories","{}")));
  JSONObject preferences=new JSONObject();
  preferences.put("language",I18n.code(context));
  preferences.put("timerStart",p.getInt("timerStart",70));
  preferences.put("timerEnd",p.getInt("timerEnd",94));
  preferences.put("notifyEnabled",p.getBoolean("notifyEnabled",true));
  preferences.put("notifyOnFinish",p.getBoolean("notifyOnFinish",true));
  preferences.put("notifyLeadMinutes",p.getInt("notifyLeadMinutes",15));
   preferences.put("potionNotifyEnabled",p.getBoolean("potionNotifyEnabled",true));
   preferences.put("potionNotifyBuilders",p.getBoolean("potionNotifyBuilders",true));
   preferences.put("potionNotifyResearch",p.getBoolean("potionNotifyResearch",true));
   preferences.put("potionNotifyAccounts",new JSONObject(p.getString("potionNotifyAccounts","{}")));
  data.put("preferences",preferences);
  JSONArray widgets=new JSONArray();
  SharedPreferences wp=context.getSharedPreferences("WidgetPreferences",Context.MODE_PRIVATE);
  HashMap<Integer,Integer> ordinal=new HashMap<>();
  for(int id:WidgetV7.allIds(context)){
   int kind=WidgetV7.kind(context,id);
   int index=ordinal.getOrDefault(kind,0);ordinal.put(kind,index+1);
   JSONObject entry=new JSONObject();
   entry.put("type",kind).put("index",index);
   entry.put("all",wp.getBoolean("all_"+id,true));
   entry.put("selected",new JSONArray(wp.getString("selected_"+id,"[]")));
   entry.put("order",new JSONArray(wp.getString("order_"+id,"[]")));
   entry.put("sort",wp.getInt("sort_"+id,0));
   entry.put("size",wp.getInt("size_"+id,1));
   entry.put("lab",wp.getBoolean("lab_"+id,true));
   entry.put("pet",wp.getBoolean("pet_"+id,true));
   entry.put("helpers",wp.getBoolean("helpers_"+id,true));
   entry.put("bg",wp.getInt("bg_"+id,0));
   entry.put("account",wp.getString("account_"+id,""));
   widgets.put(entry);
  }
  data.put("widgets",widgets);
  return data;
 }

 static void link(Context context,Uri uri,String name,boolean automatic){
  AppData.prefs(context).edit()
   .putString(KEY_URI,uri.toString())
   .putString(KEY_NAME,name==null?"ClashTimerHub-backup.json":name)
   .putBoolean(KEY_AUTO,automatic)
   .remove(KEY_ERROR).apply();
 }
 static void enableAutomatic(Context context,boolean on){
  AppData.prefs(context).edit().putBoolean(KEY_AUTO,on&&isLinked(context)).apply();
 }
 static void unlink(Context context){
  Uri previous=linkedUri(context);
  if(previous!=null){
   try{context.getContentResolver().releasePersistableUriPermission(previous,
    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION|android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
   }catch(Exception ignored){}
  }
  AppData.prefs(context).edit().remove(KEY_URI).remove(KEY_NAME)
   .putBoolean(KEY_AUTO,false).apply();
 }

 /** Runs after the importer has durably saved its new account data. */
 static void afterSuccessfulSync(Context context){
  if(!isAutomatic(context))return;
  Uri dest=linkedUri(context);
  if(dest!=null)writeAsync(context,dest,null);
 }

 static void writeAsync(Context context,Uri destination,Completion callback){
  Context app=context.getApplicationContext();
  WORK.execute(()->{
   boolean ok=false;String message;
   try{
    byte[] bytes=buildPayload(app).toString(2).getBytes(StandardCharsets.UTF_8);
    if(bytes.length>MAX_BYTES)throw new java.io.IOException("Back-up is te groot");
    // Preserve a local copy first in case the Drive document provider fails
    // mid-write or the device is offline.
    File local=new File(app.getFilesDir(),"clashtimerhub-last-backup.json");
    try(FileOutputStream file=new FileOutputStream(local,false)){file.write(bytes);file.getFD().sync();}
    try(OutputStream out=app.getContentResolver().openOutputStream(destination,"wt")){
     if(out==null)throw new java.io.IOException("Bestand kan niet worden geopend");
     out.write(bytes);out.flush();
    }
    long now=System.currentTimeMillis();
    app.getSharedPreferences(MainActivity.class.getName(),Context.MODE_PRIVATE).edit()
     .putLong(KEY_TIME,now).remove(KEY_ERROR).apply();
    ok=true;message="Back-up opgeslagen ("+bytes.length+" bytes)";
   }catch(Exception e){
    message=e.getMessage()==null?"Geen toegang tot het gekozen Drive-bestand":e.getMessage();
    AppData.prefs(app).edit().putString(KEY_ERROR,message).apply();
    Log.e("ClashTimerHub","Drive backup failed",e);
   }
   boolean success=ok;String result=message;
   if(callback!=null)MAIN.post(()->callback.done(success,result));
  });
 }

 static void readAsync(Context context,Uri uri,CompletionWithData callback){
  Context app=context.getApplicationContext();
  WORK.execute(()->{
   String json=null,error=null;
   try(InputStream in=app.getContentResolver().openInputStream(uri)){
    if(in==null)throw new java.io.IOException("Back-upbestand kan niet worden geopend");
    ByteArrayOutputStream bytes=new ByteArrayOutputStream();
    byte[] chunk=new byte[8192];int n;
    while((n=in.read(chunk))!=-1){
     if(bytes.size()+n>MAX_BYTES)throw new java.io.IOException("Back-up is groter dan 2 MB");
     bytes.write(chunk,0,n);
    }
    json=new String(bytes.toByteArray(),StandardCharsets.UTF_8);
    JSONObject obj=new JSONObject(json);
    if(!"clash-timer-hub-backup-v1".equals(obj.optString("format")))
     throw new java.io.IOException("Onbekend back-upformaat");
   }catch(Exception e){error=e.getMessage()==null?"Kon back-up niet lezen":e.getMessage();}
   String finalJson=json,finalError=error;
   MAIN.post(()->callback.done(finalJson,finalError));
  });
 }
 interface CompletionWithData{void done(String json,String error);}

 static String status(Context ctx){
  SharedPreferences p=AppData.prefs(ctx);
  if(!isLinked(ctx))return "Nog geen Drive-bestand gekozen";
  String name=p.getString(KEY_NAME,"ClashTimerHub-backup.json");
  long timestamp=p.getLong(KEY_TIME,0);
  String when=timestamp==0?"nog geen succesvolle back-up":
   new SimpleDateFormat("dd-MM-yyyy HH:mm",Locale.getDefault()).format(new Date(timestamp));
  String issue=p.getString(KEY_ERROR,"");
  return name+" • "+(isAutomatic(ctx)?"automatisch":"handmatig")+" • "+when+
   (issue.isEmpty()?"":"\nFout bij laatste poging: "+issue);
 }
}
