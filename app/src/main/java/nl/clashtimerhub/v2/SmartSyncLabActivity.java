package nl.clashtimerhub.v2;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.util.*;
import org.json.*;
import okhttp3.*;

/** An isolated, opt-in vision experiment. Never captures screens in the background. */
public final class SmartSyncLabActivity extends Activity {
 private static final int PROFILE=601, UPGRADES=602;
 private static final String ENDPOINT="https://api.openai.com/v1/responses";
 private final OkHttpClient client=new OkHttpClient.Builder()
  .connectTimeout(20,java.util.concurrent.TimeUnit.SECONDS)
  .readTimeout(75,java.util.concurrent.TimeUnit.SECONDS).build();
 private byte[] profile,upgrade;
 private LinearLayout root,rows;
 private TextView message;
 private EditText secret;
 private Button analyze,save,chooseProfile,chooseUpgrade;
 private boolean working;
 private String verifiedAccount="",verifiedTag="";
 private final ArrayList<Proposal> proposals=new ArrayList<>();
 private final ArrayList<CheckBox> checks=new ArrayList<>();
 static final class Proposal {
  final String name,category;final long seconds;
  Proposal(String n,String c,long s){name=n;category=c;seconds=s;}
 }
 private String tr(String nl,String en){return I18n.english(this)?en:nl;}
 private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
 private TextView text(String value,int size){
  TextView t=new TextView(this);t.setText(value);t.setTextColor(0xffe9dff2);t.setTextSize(size);
  t.setPadding(0,dp(9),0,dp(9));root.addView(t);return t;
 }
 private Button button(String label,Runnable r){
  Button b=new Button(this);b.setAllCaps(false);b.setText(label);
  root.addView(b,new LinearLayout.LayoutParams(-1,dp(50)));b.setOnClickListener(v->r.run());return b;
 }
 @Override public void onCreate(Bundle state){
  super.onCreate(state);setTitle("Smart Sync Lab");
  getWindow().setStatusBarColor(0xff171025);
  ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(1);
  root.setPadding(dp(18),dp(20),dp(18),dp(28));root.setBackgroundColor(0xff171025);
  scroll.addView(root);setContentView(scroll);
  text("SMART SYNC LAB · BETA",20).setTextColor(0xffffd783);
  text(tr("Optioneel experiment: je bestaande synchronisatie en timers blijven ongewijzigd.",
   "Optional experiment: your existing sync and timers remain unchanged."),14);
  text(tr("Selecteer een Clash-profiel met zichtbare player tag en daarna het upgrademenu van hetzelfde account.",
   "Choose a Clash profile showing the player tag, then an upgrade menu from the same account."),14);
  chooseProfile=button(tr("Profielscreenshot kiezen","Choose profile screenshot"),()->pick(PROFILE));
  chooseUpgrade=button(tr("Upgradescreenshot kiezen","Choose upgrade screenshot"),()->pick(UPGRADES));
  text(tr("OpenAI API-sleutel (eenmalig, niet opgeslagen)","OpenAI API key (one-time, not saved)"),13);
  secret=new EditText(this);secret.setHint("sk-...");secret.setSingleLine(true);
  secret.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
  secret.setTextColor(0xffffffff);root.addView(secret);
  text(tr("Met toestemming worden de twee screenshots naar OpenAI verzonden. De API kan kosten veroorzaken. Er worden geen beelden bewaard.",
   "With consent, both screenshots are sent to OpenAI. API charges may apply. Images are not retained in the app."),12);
  analyze=button(tr("Analyse starten","Analyze screenshots"),()->confirm());
  message=text("",13);message.setTextColor(0xffffd783);
  rows=new LinearLayout(this);rows.setOrientation(1);root.addView(rows);
  save=button(tr("Geselecteerde timers toevoegen","Add selected timers"),()->confirmSave());
  save.setVisibility(View.GONE);
  buttons();
 }
 private void buttons(){
  chooseProfile.setEnabled(!working);chooseUpgrade.setEnabled(!working);
  analyze.setEnabled(!working&&profile!=null&&upgrade!=null);
  save.setEnabled(!working&&!verifiedAccount.isEmpty()&&!proposals.isEmpty());
 }
 private void reset(){
  verifiedAccount="";verifiedTag="";proposals.clear();checks.clear();
  rows.removeAllViews();save.setVisibility(View.GONE);
 }
 private void pick(int which){
  Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
  intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("image/*");
  intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(intent,which);
 }
 @Override protected void onActivityResult(int req,int result,Intent intent){
  super.onActivityResult(req,result,intent);
  if(result!=RESULT_OK||intent==null||intent.getData()==null)return;
  if(req!=PROFILE&&req!=UPGRADES)return;
  try{
   byte[] jpeg=compress(intent.getData());
   if(req==PROFILE){profile=jpeg;chooseProfile.setText("✓ "+tr("Profiel gekozen","Profile chosen"));}
   else {upgrade=jpeg;chooseUpgrade.setText("✓ "+tr("Upgrades gekozen","Upgrades chosen"));}
   reset();message.setText(tr("Klaar voor analyse.","Ready to analyze."));
  }catch(Exception ex){message.setText(tr("Afbeeldingsfout: ","Image error: ")+ex.getMessage());}
  buttons();
 }
 private byte[] compress(Uri uri)throws Exception{
  BitmapFactory.Options dimensions=new BitmapFactory.Options();dimensions.inJustDecodeBounds=true;
  try(InputStream stream=getContentResolver().openInputStream(uri)){
   BitmapFactory.decodeStream(stream,null,dimensions);
  }
  if(dimensions.outWidth<100||dimensions.outHeight<100)throw new IOException("Invalid image");
  int sample=1;
  while(dimensions.outWidth/sample>1400||dimensions.outHeight/sample>2400)sample*=2;
  BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=sample;
  Bitmap bitmap;
  try(InputStream stream=getContentResolver().openInputStream(uri)){
   bitmap=BitmapFactory.decodeStream(stream,null,options);
  }
  if(bitmap==null)throw new IOException("Unsupported image");
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();
  bitmap.compress(Bitmap.CompressFormat.JPEG,80,bytes);bitmap.recycle();
  if(bytes.size()>2000000)throw new IOException("Image exceeds 2 MB");
  return bytes.toByteArray();
 }
 private void confirm(){
  String key=secret.getText().toString().trim();
  if(!key.startsWith("sk-")){message.setText(tr("Vul een API-sleutel in.","Enter an API key."));return;}
  new AlertDialog.Builder(this)
   .setTitle(tr("AI-analyse toestaan?","Allow AI analysis?"))
   .setMessage(tr("Twee screenshots worden verzonden naar OpenAI. Controleer of er geen onnodige persoonlijke gegevens zichtbaar zijn. Er wordt niets automatisch opgeslagen.",
    "Two screenshots will be sent to OpenAI. Check for unrelated personal information. Nothing is saved automatically."))
   .setPositiveButton(tr("Analyseren","Analyze"),(d,w)->request(key))
   .setNegativeButton(tr("Annuleren","Cancel"),null).show();
 }
 private void request(String key){
  reset();working=true;buttons();
  message.setText(tr("Beelden worden geanalyseerd…","Analyzing screenshots…"));
  final byte[] p=profile,u=upgrade;
  new Thread(()->{
   try{
    JSONObject result=new JSONObject(vision(key,p,u));
    runOnUiThread(()->display(result));
   }catch(Exception e){
    runOnUiThread(()->{
     working=false;message.setText(tr("Analyse mislukt: ","Analysis failed: ")+e.getMessage());buttons();
    });
   }
  },"SmartSyncLab-vision").start();
 }
 private String vision(String key,byte[] p,byte[] u)throws Exception{
  String instruction="Analyze two separate Clash of Clans screenshots. The first is intended to be a PLAYER PROFILE "+
   "and the second an UPGRADE MENU. Return ONLY a JSON object with fields profileDetected (boolean), "+
   "upgradeDetected (boolean), playerTag (string), tagConfidence ('high','medium','low'), "+
   "items (array of {name:string, category:'Builders'|'Laboratory'|'Pets', remainingSeconds:integer, "+
   "confidence:'high'|'medium'|'low'}). Do not invent any names, times, digits or tags. "+
   "Omit items that are not clearly legible, including abbreviated or ambiguous timers. "+
   "The profile player tag must be clearly visible to return high confidence. "+
   "Do not assume the two screenshots belong to the same account. Return only the visible timers.";
  JSONArray contents=new JSONArray();
  contents.put(new JSONObject().put("type","input_text").put("text",instruction));
  contents.put(new JSONObject().put("type","input_image")
   .put("image_url","data:image/jpeg;base64,"+android.util.Base64.encodeToString(p,2)));
  contents.put(new JSONObject().put("type","input_image")
   .put("image_url","data:image/jpeg;base64,"+android.util.Base64.encodeToString(u,2)));
  JSONObject payload=new JSONObject().put("model","gpt-4.1-mini")
   .put("input",new JSONArray().put(new JSONObject().put("role","user").put("content",contents)));
  Request request=new Request.Builder().url(ENDPOINT)
   .header("Authorization","Bearer "+key)
   .post(RequestBody.create(MediaType.parse("application/json"),payload.toString())).build();
  try(Response response=client.newCall(request).execute()){
   if(!response.isSuccessful())throw new IOException("OpenAI HTTP "+response.code());
   if(response.body()==null)throw new IOException("Empty response");
   JSONObject root=new JSONObject(response.body().string());
   JSONArray output=root.optJSONArray("output");
   if(output!=null)for(int i=0;i<output.length();i++){
    JSONObject part=output.optJSONObject(i);if(part==null)continue;
    JSONArray content=part.optJSONArray("content");if(content==null)continue;
    for(int j=0;j<content.length();j++){
     JSONObject item=content.optJSONObject(j);
     if(item!=null&&"output_text".equals(item.optString("type"))){
      String text=item.optString("text","").trim();
      if(text.startsWith("json"))text=text.substring(4).trim();
      int start=text.indexOf('{'),end=text.lastIndexOf('}');
      if(start>=0&&end>start)return text.substring(start,end+1);
     }
    }
   }
   throw new IOException("No valid model JSON");
  }
 }
 private static String normalizeTag(String raw){
  String candidate=raw.trim().toUpperCase(Locale.ROOT).replace(" ","");
  return candidate.matches("#[0289PYLQGRJCUV]{5,14}")?candidate:"";
 }
 private void display(JSONObject data){
  working=false;reset();
  String tag=normalizeTag(data.optString("playerTag",""));
  if(!data.optBoolean("profileDetected")||!data.optBoolean("upgradeDetected")||
   !"high".equals(data.optString("tagConfidence"))||tag.isEmpty()){
   message.setText(tr("Geen betrouwbaar account of upgrademenu herkend. Niets opgeslagen.",
    "No reliable account or upgrade screen recognized. Nothing saved."));buttons();return;
  }
  SharedPreferences prefs=AppData.prefs(this);
  String found="";
  try{
   JSONObject tags=new JSONObject(prefs.getString("playerTags","{}"));
   Iterator<String> keys=tags.keys();
   while(keys.hasNext()){
    String name=keys.next();
    if(tag.equalsIgnoreCase(tags.optString(name))){found=name;break;}
   }
  }catch(Exception ignored){}
  if(found.isEmpty()){
   message.setText(tr("Player tag niet gekoppeld. Synchroniseer eerst via de bestaande Clash-export. Niets opgeslagen.",
    "Player tag is not linked. First sync using the existing Clash export. Nothing saved."));buttons();return;
  }
  verifiedAccount=found;verifiedTag=tag;
  JSONArray items=data.optJSONArray("items");
  HashSet<String> dedupe=new HashSet<>();
  if(items!=null)for(int i=0;i<items.length();i++){
   JSONObject o=items.optJSONObject(i);if(o==null)continue;
   String name=o.optString("name","").trim(),cat=o.optString("category","");
   long seconds=o.optLong("remainingSeconds",0);
   if(!"high".equals(o.optString("confidence"))||name.length()<2||name.length()>70||
    seconds<60||seconds>45L*86400L)continue;
   String stored=cat.equals("Builders")?"Bouwers":cat.equals("Laboratory")?"Laboratorium":
    cat.equals("Pets")?"Pets":"";
   if(stored.isEmpty()||!dedupe.add(stored+"|"+name.toLowerCase(Locale.ROOT)))continue;
   Proposal item=new Proposal(name,stored,seconds);proposals.add(item);
   CheckBox check=new CheckBox(this);check.setTextColor(0xffffffff);check.setChecked(true);
   check.setText(name+" · "+I18n.tr(this,stored)+" · "+format(seconds));
   rows.addView(check);checks.add(check);
  }
  message.setText(tr("Herkenning: ","Recognition: ")+AppData.displayName(this,found)+" ("+tag+")\n"+
   tr("Controleer dat beide beelden écht van hetzelfde account zijn. Alleen geselecteerde regels worden toegevoegd.",
    "Verify both images really belong to the same account. Only selected rows will be added."));
  if(proposals.isEmpty())message.append("\n"+tr("Geen timers met hoge zekerheid.","No high-confidence timers."));
  else save.setVisibility(View.VISIBLE);
  buttons();
 }
 private static String format(long seconds){
  long days=seconds/86400,hours=seconds%86400/3600,minutes=seconds%3600/60;
  return (days>0?days+"d ":"")+(hours>0?hours+"h ":"")+minutes+"m";
 }
 private void confirmSave(){
  int n=0;for(CheckBox check:checks)if(check.isChecked())n++;
  if(n==0){message.setText(tr("Selecteer minstens één timer.","Select at least one timer."));return;}
  new AlertDialog.Builder(this).setTitle(tr("Timers bevestigen?","Confirm timers?"))
   .setMessage(tr("Controleer dat het upgrademenu bij ","Verify the upgrade menu belongs to ")+
    AppData.displayName(this,verifiedAccount)+tr(" hoort. Bestaande timers blijven behouden.",
     ". Existing timers will be preserved."))
   .setPositiveButton(tr("Toevoegen","Add"),(d,w)->commit())
   .setNegativeButton(tr("Annuleren","Cancel"),null).show();
 }
 private void commit(){
  try{
   SharedPreferences prefs=AppData.prefs(this);
   JSONObject tags=new JSONObject(prefs.getString("playerTags","{}"));
   if(!verifiedTag.equalsIgnoreCase(tags.optString(verifiedAccount)))throw new IOException("Account changed");
   JSONArray saved=new JSONArray(prefs.getString("upgrades","[]"));
   JSONArray all=new JSONArray(saved.toString());
   long now=System.currentTimeMillis();int count=0,duplicates=0;
   for(int i=0;i<proposals.size();i++){
    if(!checks.get(i).isChecked())continue;
    Proposal p=proposals.get(i);long finish=now+p.seconds*1000L;boolean duplicate=false;
    for(int j=0;j<all.length();j++){
     JSONObject existing=all.optJSONObject(j);if(existing==null)continue;
     if(verifiedAccount.equals(existing.optString("account"))&&
      p.category.equals(existing.optString("category"))&&
      p.name.equalsIgnoreCase(existing.optString("name"))&&
      Math.abs(finish-existing.optLong("finish"))<180000L){duplicate=true;break;}
    }
    if(duplicate){duplicates++;continue;}
    all.put(new JSONObject().put("id",UUID.randomUUID().toString())
     .put("account",verifiedAccount).put("category",p.category).put("name",p.name)
     .put("finish",finish).put("smartSync",true));
    count++;
   }
   if(count>0){
    boolean success=prefs.edit().putString("upgrades",all.toString())
     .putLong("syncRevision",prefs.getLong("syncRevision",0)+1).commit();
    if(!success)throw new IOException("Could not save");
    TimerWidget.schedule(this);
   }
   reset();buttons();
   message.setText(tr("Timers toegevoegd: ","Timers added: ")+count+
    tr(" · Duplicaten overgeslagen: "," · Duplicates skipped: ")+duplicates);
  }catch(Exception e){message.setText(tr("Niets opgeslagen: ","Nothing saved: ")+e.getMessage());}
 }
}
