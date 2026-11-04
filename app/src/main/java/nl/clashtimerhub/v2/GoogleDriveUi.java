package nl.clashtimerhub.v2;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.RevokeAccessRequest;
import com.google.android.gms.common.api.Scope;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Consumer;

/** Direct Google Drive (My Drive) backup UI, no Android Downloads picker. */
final class GoogleDriveUi {
 static final int REQUEST_GOOGLE_AUTH=73;
 private final MainActivity activity;
 private final Runnable refresh;
 private final Consumer<String> restore;
 private Consumer<String> pending;
 private boolean busy=false;
 private static final int MAX_BACKUP=2_000_000;

 GoogleDriveUi(MainActivity activity,Runnable refresh,Consumer<String> restore){
  this.activity=activity;this.refresh=refresh;this.restore=restore;
 }
 static AuthorizationRequest authorizationRequest(){
  return AuthorizationRequest.builder().setRequestedScopes(
   Collections.singletonList(new Scope(GoogleDriveFiles.SCOPE))).build();
 }

 private void authorize(Consumer<String> action){
  if(busy){toast("Een Google Drive-actie is al bezig");return;}
  busy=true;pending=action;
  Identity.getAuthorizationClient(activity).authorize(authorizationRequest())
   .addOnSuccessListener(result->{
    if(result.hasResolution()){
     PendingIntent consent=result.getPendingIntent();
     if(consent==null){fail("Google heeft geen aanmeldscherm teruggegeven.");return;}
     try{
      activity.startIntentSenderForResult(consent.getIntentSender(),REQUEST_GOOGLE_AUTH,
       null,0,0,0);
     }catch(IntentSender.SendIntentException e){fail("Google-aanmelding kon niet worden gestart: "+e.getMessage());}
    }else consume(result);
   }).addOnFailureListener(e->fail("Kan Google Drive niet verbinden: "+e.getMessage()+
    "\nControleer of Google Drive API, OAuth-toestemming en de SHA-1 van deze APK zijn ingesteld."));
 }
 void onAuthorizationResult(int resultCode,Intent data){
  if(!busy)return;
  if(resultCode!=Activity.RESULT_OK||data==null){
   pending=null;busy=false;toast("Google Drive-aanmelding geannuleerd");return;
  }
  try{
   consume(Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(data));
  }catch(Exception e){fail("Google-aanmelding mislukt: "+e.getMessage());}
 }
 private void consume(AuthorizationResult result){
  Consumer<String> next=pending;
  pending=null;busy=false;
  String token=result.getAccessToken();
  if(token==null||token.isEmpty()){fail("Google heeft geen toegangstoken gegeven");return;}
  AppData.prefs(activity).edit().putBoolean("googleDriveAuthorized",true).apply();
  refresh.run();
  if(next!=null)next.accept(token);
 }
 private void fail(String msg){pending=null;busy=false;toast(msg);}
 private void toast(String msg){Toast.makeText(activity,msg,Toast.LENGTH_LONG).show();}
 private void io(DriveTask task){
  GoogleDriveFiles.IO.execute(()->{
   try{task.work();}
   catch(Exception e){GoogleDriveFiles.error(activity.getApplicationContext(),e);
    activity.runOnUiThread(()->{
     if(activity.isFinishing()||activity.isDestroyed())return;
     new AlertDialog.Builder(activity).setTitle("Google Drive")
      .setMessage(e.getMessage()==null?"Er is iets misgegaan":e.getMessage())
      .setPositiveButton("OK",null).show();refresh.run();
    });}
  });
 }
 interface DriveTask{void work()throws Exception;}
 private void done(String msg){
  activity.runOnUiThread(()->{
   if(activity.isFinishing()||activity.isDestroyed())return;
   refresh.run();
   new AlertDialog.Builder(activity).setTitle("Google Drive")
    .setMessage(msg).setPositiveButton("OK",null).show();
  });
 }

 void connect(){
  authorize(token->io(()->{
   ArrayList<GoogleDriveFiles.FileItem> list=GoogleDriveFiles.list(token);
   activity.runOnUiThread(()->{
    if(activity.isFinishing()||activity.isDestroyed())return;
    refresh.run();
    new AlertDialog.Builder(activity).setTitle("Google Drive verbonden")
     .setMessage("ClashTimerHub heeft toegang tot zijn eigen JSON-bestanden in Mijn Drive. "+
      list.size()+" back-upbestand(en) gevonden.\n\nMaak een nieuwe back-up of kies een bestaand bestand.")
     .setPositiveButton("Nieuwe back-up",(d,w)->create())
     .setNeutralButton("Bestand kiezen",(d,w)->chooseForAuto())
     .setNegativeButton("Sluiten",null).show();
   });
  }));
 }
 void create(){
  final EditText field=new EditText(activity);
  field.setSingleLine(true);
  field.setText(GoogleDriveFiles.FILENAME);
  field.setSelectAllOnFocus(true);
  int pad=(int)(18*activity.getResources().getDisplayMetrics().density);
  android.widget.LinearLayout wrapper=new android.widget.LinearLayout(activity);
  wrapper.setPadding(pad,0,pad,0);wrapper.addView(field);
  new AlertDialog.Builder(activity).setTitle("Nieuwe back-up in Mijn Drive")
   .setMessage("Maak een gewoon JSON-bestand. Dit is zichtbaar in je Google Drive.")
   .setView(wrapper)
   .setPositiveButton("Opslaan",(d,w)->{
    final String name=field.getText().toString().trim();
    authorize(token->io(()->{
     JSONObject payload=DriveBackupManager.buildPayload(activity.getApplicationContext());
     byte[] bytes=payload.toString(2).getBytes(StandardCharsets.UTF_8);
     if(bytes.length>MAX_BACKUP)throw new IOException("Back-up groter dan 2 MB");
     GoogleDriveFiles.FileItem file=GoogleDriveFiles.create(token,name,bytes);
     GoogleDriveFiles.select(activity,file);
     GoogleDriveFiles.success(activity,file);
     done(file.name+" is opgeslagen in Mijn Drive.\n\nJe kunt dit bestand bij volgende back-ups vervangen.");
    }));
   }).setNegativeButton("Annuleren",null).show();
 }

 void overwriteSelected(){
  String fileId=GoogleDriveFiles.selectedId(activity);
  if(fileId.isEmpty()){chooseToOverwrite();return;}
  String fileName=GoogleDriveFiles.selectedName(activity);
  new AlertDialog.Builder(activity).setTitle("Bestaande back-up vervangen?")
   .setMessage("Het huidige bestand '"+fileName+"' in Mijn Drive wordt vervangen door de nieuwste gegevens. "+
    "Je bestaande ClashTimerHub-app en timers blijven staan.")
   .setPositiveButton("Vervangen",(d,w)->overwrite(fileId,fileName))
   .setNeutralButton("Ander bestand",(d,w)->chooseToOverwrite())
   .setNegativeButton("Annuleren",null).show();
 }
 private void overwrite(String id,String name){
  authorize(token->io(()->{
   JSONObject payload=DriveBackupManager.buildPayload(activity.getApplicationContext());
   byte[] bytes=payload.toString(2).getBytes(StandardCharsets.UTF_8);
   GoogleDriveFiles.FileItem item=GoogleDriveFiles.overwrite(token,
    new GoogleDriveFiles.FileItem(new JSONObject().put("id",id).put("name",name)),bytes);
   GoogleDriveFiles.success(activity,item);
   done(item.name+" is bijgewerkt in Mijn Drive.");
  }));
 }
 void chooseToOverwrite(){choose(1);}
 void chooseForRestore(){choose(2);}
 void chooseForAuto(){choose(3);}
 private void choose(int operation){
  authorize(token->io(()->{
   ArrayList<GoogleDriveFiles.FileItem> files=GoogleDriveFiles.list(token);
   activity.runOnUiThread(()->{
    if(activity.isFinishing()||activity.isDestroyed())return;
    if(files.isEmpty()){
     new AlertDialog.Builder(activity).setTitle("Geen Drive-back-ups gevonden")
      .setMessage("Er zijn nog geen JSON-bestanden gevonden die ClashTimerHub via Google Drive mag beheren. "+
       "Maak eerst een nieuwe back-up in Mijn Drive.")
      .setPositiveButton("Nieuwe back-up",(d,w)->create())
      .setNegativeButton("Sluiten",null).show();return;
    }
    String[] choices=new String[files.size()];
    for(int i=0;i<choices.length;i++)choices[i]=files.get(i).toString();
    String title=operation==1?"Welke back-up vervangen?":
     operation==2?"Welke back-up herstellen?":"Automatisch back-upbestand kiezen";
    new AlertDialog.Builder(activity).setTitle(title).setItems(choices,(d,index)->{
     GoogleDriveFiles.FileItem file=files.get(index);
     if(operation==1){
      new AlertDialog.Builder(activity).setTitle("Back-up vervangen?")
       .setMessage(file.name+" wordt in Google Drive overschreven. Dit kan niet vanuit de app ongedaan worden gemaakt.")
       .setPositiveButton("Vervangen",(dialog,which)->overwrite(file.id,file.name))
       .setNegativeButton("Annuleren",null).show();
     }else if(operation==2){
      io(()->{
       String json=GoogleDriveFiles.download(token,file);
       activity.runOnUiThread(()->{
        if(!activity.isFinishing()&&!activity.isDestroyed())restore.accept(json);
       });
      });
     }else{
      GoogleDriveFiles.select(activity,file);
      refresh.run();
      new AlertDialog.Builder(activity).setTitle("Back-upbestand geselecteerd")
       .setMessage(file.name+" is geselecteerd. Wil je na iedere succesvolle Clash-synchronisatie "+
        "automatisch naar dit bestand back-uppen?")
       .setPositiveButton("Automatisch aan",(dialog,which)->{
        GoogleDriveFiles.auto(activity,true);refresh.run();
       }).setNegativeButton("Handmatig",null).show();
     }
    }).setNegativeButton("Annuleren",null).show();
   });
  }));
 }
 void toggleAuto(){
  if(GoogleDriveFiles.selectedId(activity).isEmpty()){
   chooseForAuto();return;
  }
  boolean next=!GoogleDriveFiles.auto(activity);
  GoogleDriveFiles.auto(activity,next);refresh.run();
  toast(next?"Automatische Google Drive-back-up ingeschakeld":"Automatische Drive-back-up uitgeschakeld");
 }
 void disconnect(){
  new AlertDialog.Builder(activity).setTitle("Google Drive ontkoppelen?")
   .setMessage("ClashTimerHub verliest zijn Google Drive-toegang. De JSON-back-ups blijven in Mijn Drive staan.")
   .setPositiveButton("Ontkoppelen",(d,w)->{
    GoogleDriveFiles.clear(activity);
    AppData.prefs(activity).edit().putBoolean("googleDriveAuthorized",false).apply();
    try{
     RevokeAccessRequest req=RevokeAccessRequest.builder()
      .setScopes(Collections.singletonList(new Scope(GoogleDriveFiles.SCOPE))).build();
     Identity.getAuthorizationClient(activity).revokeAccess(req)
      .addOnFailureListener(e->toast("Toegang in Google-account mogelijk nog actief: "+e.getMessage()));
    }catch(Exception e){toast("Google-toegang intrekken via je Google-account: "+e.getMessage());}
    refresh.run();
   }).setNegativeButton("Annuleren",null).show();
 }
 /** Best-effort background snapshot. If Google needs a consent screen, never interrupt Clash Sync. */
 static void afterSuccessfulSync(Context context){
  if(!GoogleDriveFiles.auto(context))return;
  Context app=context.getApplicationContext();
  Identity.getAuthorizationClient(app).authorize(authorizationRequest())
   .addOnSuccessListener(result->{
    if(result.hasResolution()||result.getAccessToken()==null){
     GoogleDriveFiles.error(app,new IOException("Open ClashTimerHub om Google Drive-toegang te vernieuwen."));
     return;
    }
    final String token=result.getAccessToken(),id=GoogleDriveFiles.selectedId(app),
     name=GoogleDriveFiles.selectedName(app);
    if(id.isEmpty())return;
    GoogleDriveFiles.IO.execute(()->{
     try{
      JSONObject payload=DriveBackupManager.buildPayload(app);
      byte[] bytes=payload.toString(2).getBytes(StandardCharsets.UTF_8);
      GoogleDriveFiles.FileItem file=GoogleDriveFiles.overwrite(token,
       new GoogleDriveFiles.FileItem(new JSONObject().put("id",id).put("name",name)),bytes);
      GoogleDriveFiles.success(app,file);
     }catch(Exception e){GoogleDriveFiles.error(app,e);}
    });
   }).addOnFailureListener(e->GoogleDriveFiles.error(app,new IOException(
    "Automatische Drive-back-up mislukt: "+e.getMessage())));
 }
}
