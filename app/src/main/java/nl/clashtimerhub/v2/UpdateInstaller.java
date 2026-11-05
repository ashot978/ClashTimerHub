package nl.clashtimerhub.v2;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.net.Uri;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Locale;

/** Verified APK update via the official PackageInstaller session API.
 * Android PackageManager, not archive metadata, enforces signing compatibility.
 * Android enforces compatibility for every update; CI pins a stable release certificate. */
final class UpdateInstaller {
 private UpdateInstaller(){}
 static void install(MainActivity activity,File apk,long expectedBuild,String expectedSha256){
  new Thread(()->{
   try {
    verify(activity,apk,expectedBuild,expectedSha256);
    // Save only a VERIFIED APK for an alternative Android installer retry.
    AppData.prefs(activity).edit()
     .putString("lastVerifiedApk",apk.getAbsolutePath())
     .putLong("lastVerifiedBuild",expectedBuild)
     .putString("lastVerifiedHash",expectedSha256==null?"":expectedSha256).apply();
    if(!activity.getPackageManager().canRequestPackageInstalls()){
     activity.runOnUiThread(()->new android.app.AlertDialog.Builder(activity)
      .setTitle("Toestemming voor app-updates")
      .setMessage("Sta 'Apps uit deze bron toestaan' toe voor Clash Timer Hub. Je APK is gecontroleerd; je bestaande gegevens blijven behouden.")
      .setPositiveButton("Toestaan",(d,w)->{
       try{
        AppData.prefs(activity).edit()
         .putString("pendingVerifiedApk",apk.getAbsolutePath())
         .putString("pendingVerifiedSha256",expectedSha256==null?"":expectedSha256)
         .putLong("pendingVerifiedBuild",expectedBuild).apply();
        Intent settings=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
         Uri.parse("package:"+activity.getPackageName()));
        activity.startActivity(settings);
       }catch(Exception e){activity.showUpdateInstallError("Kan installatietoestemming niet openen: "+e.getMessage());}
      }).setNegativeButton("Later",null).show());
     return;
    }
    PackageManager pm=activity.getPackageManager();
    PackageInstaller installer=pm.getPackageInstaller();
    PackageInstaller.SessionParams params=new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
    params.setAppPackageName(activity.getPackageName());
    params.setSize(apk.length());
    if(Build.VERSION.SDK_INT>=31)params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED);
    int sessionId=installer.createSession(params);
    boolean committed=false;
    try(PackageInstaller.Session session=installer.openSession(sessionId)){
     try(FileInputStream in=new FileInputStream(apk);
         OutputStream out=session.openWrite("base.apk",0,apk.length())){
      byte[] bytes=new byte[64*1024];int length;
      while((length=in.read(bytes))!=-1)out.write(bytes,0,length);
      session.fsync(out);
     }
     Intent status=new Intent(activity,UpdateInstallReceiver.class);
     status.setAction(UpdateInstallReceiver.ACTION_STATUS);
     PendingIntent pending=PendingIntent.getBroadcast(activity,sessionId,status,
      PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE);
     session.commit(pending.getIntentSender());
     committed=true;
    }finally{
     if(!committed){try{installer.abandonSession(sessionId);}catch(Exception ignored){}}
    }
   }catch(Exception error){
    android.util.Log.e("ClashTimerHub","Update verification or staging failed",error);
    activity.showUpdateInstallError(error.getMessage()==null?"APK-installatie mislukt":error.getMessage());
   }
  },"ClashTimerHub-install-update").start();
 }

 /** Fallback to Android's native installer UI when the session installer is rejected.
  * Verify again; Android itself checks the signing certificate during installation. */
 static void installWithAndroidInstaller(MainActivity activity,File apk,long build,String sha256){
  new Thread(()->{
   try{
    verify(activity,apk,build,sha256);
    if(!activity.getPackageManager().canRequestPackageInstalls())
     throw new java.io.IOException("Sta eerst Apps uit deze bron toestaan in Android-instellingen toe.");
    Uri uri=androidx.core.content.FileProvider.getUriForFile(
     activity,activity.getPackageName()+".backups",apk);
    Intent intent=new Intent(Intent.ACTION_VIEW);
    intent.setDataAndType(uri,"application/vnd.android.package-archive");
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    activity.runOnUiThread(()->{
     try{activity.startActivity(intent);}
     catch(Exception e){activity.showUpdateInstallError(
      "Android-installatieprogramma kan niet openen: "+e.getMessage());}
    });
   }catch(Exception error){
    activity.showUpdateInstallError(error.getMessage()==null?
      "APK kon niet veilig worden gecontroleerd":error.getMessage());
   }
  },"ClashTimerHub-system-installer").start();
 }
 private static void verify(MainActivity context,File file,long expectedBuild,String digest)throws Exception{
  if(file==null||!file.isFile()||file.length()<1024*1024)
   throw new java.io.IOException("Download is onvolledig of geen APK. Download opnieuw.");
  if(digest!=null&&!digest.trim().isEmpty()){
   String expected=digest.toLowerCase(Locale.ROOT).replace("sha256:","").trim();
   if(!expected.matches("[0-9a-f]{64}"))
    throw new java.io.IOException("GitHub leverde een ongeldige controlehash.");
   MessageDigest sha=MessageDigest.getInstance("SHA-256");
   try(FileInputStream in=new FileInputStream(file)){
    byte[] chunk=new byte[65536];int n;
    while((n=in.read(chunk))!=-1)sha.update(chunk,0,n);
   }
   StringBuilder actual=new StringBuilder();
   for(byte b:sha.digest())actual.append(String.format(Locale.ROOT,"%02x",b&0xff));
   if(!expected.equals(actual.toString()))
    throw new java.io.IOException("APK-controlehash komt niet overeen. Download opnieuw.");
  }

  PackageManager pm=context.getPackageManager();
  int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;
  PackageInfo candidate=pm.getPackageArchiveInfo(file.getAbsolutePath(),flags);
  if(candidate==null||!context.getPackageName().equals(candidate.packageName))
   throw new java.io.IOException("Geen geldige Clash Timer Hub-APK of onjuiste pakketnaam.");
  long currentVersion=UpdateChecker.installedVersionCode(context);
  long futureVersion=Build.VERSION.SDK_INT>=28?candidate.getLongVersionCode():candidate.versionCode;
  if(futureVersion<=currentVersion)
   throw new java.io.IOException("Deze APK is niet nieuwer (geïnstalleerd: "+currentVersion+", APK: "+futureVersion+").");
  if(expectedBuild>0&&futureVersion!=1000+expectedBuild)
   throw new java.io.IOException("APK-buildnummer komt niet overeen met GitHub. Download opnieuw.");

  // Do not compare signatures returned by getPackageArchiveInfo() with installed
  // signing history. Some Android versions do not populate the archive metadata
  // consistently, causing a false mismatch even for an APK the system accepts.
  // The Android PackageInstaller will independently enforce signature continuity;
  // an incompatible APK cannot replace the installed app.
 }
}
