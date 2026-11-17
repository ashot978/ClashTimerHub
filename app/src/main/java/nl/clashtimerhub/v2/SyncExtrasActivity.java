package nl.clashtimerhub.v2;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

/** Actual export-backed equipment and collection view. No speculative roadmap. */
public final class SyncExtrasActivity extends Activity {
 private int category=0;
 private boolean expanded=false;
 private String account="";
 private boolean en=false;
 private static final int GOLD=0xffffd783,PALE=0xffe8dff2,SUB=0xffbdadce;
 private static final String[][] GROUPS={
  {"equipment"},{"decos","decos2"},{"obstacles","obstacles2"},
  {"skins","skins2"},{"sceneries","sceneries2"}
 };
 private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private GradientDrawable shape(int bg,int border){
  GradientDrawable d=new GradientDrawable();
  d.setColor(bg);d.setCornerRadius(dp(13));d.setStroke(dp(1),border);return d;
 }
 private TextView label(String text,int size,int color){
  TextView t=new TextView(this);t.setText(text);t.setTextSize(size);
  t.setTextColor(color);t.setLineSpacing(dp(3),1.02f);
  return t;
 }
 private void attach(LinearLayout parent,View child,int margin,int padding){
  child.setPadding(dp(padding),dp(padding),dp(padding),dp(padding));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
  lp.setMargins(dp(margin),dp(3),dp(margin),dp(5));parent.addView(child,lp);
 }
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  account=getIntent().getStringExtra("account");
  if(account==null||account.trim().isEmpty()){finish();return;}
  en=I18n.english(this);
  render();
 }
 private void render(){
  LinearLayout whole=new LinearLayout(this);whole.setOrientation(LinearLayout.VERTICAL);
  whole.setBackgroundColor(0xff160f25);
  if(android.os.Build.VERSION.SDK_INT>=35)whole.setOnApplyWindowInsetsListener((v,insets)->{
   android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars());
   v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;
  });
  setContentView(whole);
  ScrollView page=new ScrollView(this);
  page.setFillViewport(true);
  LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
  content.setPadding(dp(11),dp(14),dp(11),dp(22));
  page.addView(content);whole.addView(page,new LinearLayout.LayoutParams(-1,-1));
  Button goBack=new Button(this);
  goBack.setText(en?"‹ Back to account":"‹ Terug naar account");
  goBack.setTextColor(GOLD);goBack.setTextSize(14);goBack.setAllCaps(false);
  goBack.setBackground(shape(0xff332342,0xff654875));
  goBack.setOnClickListener(v->finish());
  content.addView(goBack,new LinearLayout.LayoutParams(-1,dp(50)));
  TextView title=label("⚔  "+AppData.displayName(this,account),22,GOLD);
  title.setTypeface(null,Typeface.BOLD);attach(content,title,1,11);
  TextView subtitle=label(en?"Equipment and collectibles from your Clash export":
   "Uitrusting en verzamelingen uit je Clash-export",13,SUB);
  attach(content,subtitle,1,7);
  JSONObject saved=SyncExtras.account(this,account);
  long timestamp=saved.optLong("exportedAt",0L);
  String when=timestamp==0?(en?"Not synced yet":"Nog niet gesynchroniseerd"):
   new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm",Locale.ROOT)
    .format(new java.util.Date(timestamp));
  TextView status=label((en?"Last collection export: ":"Laatste collectie-export: ")+when,12,0xff8be3ad);
  attach(content,status,1,8);
  HorizontalScrollView horizontal=new HorizontalScrollView(this);
  horizontal.setHorizontalScrollBarEnabled(false);
  LinearLayout chipRow=new LinearLayout(this);
  chipRow.setOrientation(LinearLayout.HORIZONTAL);
  horizontal.addView(chipRow);
  String[] tabs=en?new String[]{"Equipment","Decorations","Obstacles","Skins","Sceneries"}:
    new String[]{"Uitrusting","Decoraties","Obstakels","Skins","Sceneries"};
  for(int i=0;i<tabs.length;i++){
   final int idx=i;
   Button chip=new Button(this);
   chip.setAllCaps(false);chip.setTextSize(12);chip.setText(tabs[i]);
   chip.setTextColor(category==i?GOLD:PALE);
   chip.setBackground(shape(category==i?0xff49335f:0xff30243e,
    category==i?0xffd8ae69:0xff654875));
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(46));
   lp.setMargins(dp(3),0,dp(3),dp(4));chipRow.addView(chip,lp);
   chip.setOnClickListener(v->{category=idx;expanded=false;render();});
  }
  content.addView(horizontal);
  for(String section:GROUPS[category])renderSection(content,saved,section);
  TextView warning=label(en?
   "An item missing from the export is not necessarily unowned. Ore estimates may change with game updates.":
   "Niet vermeld in de export betekent niet automatisch niet in bezit. Ore-kosten zijn indicatief.",11,SUB);
  attach(content,warning,1,12);
 }
 private void renderSection(LinearLayout panel,JSONObject saved,String section){
  String region=section.equals("equipment")?(en?"Hero Equipment":"Heldenuitrusting"):
    section.endsWith("2")?(en?"Builder Base":"Bouwersbasis"):(en?"Home Village":"Thuisdorp");
  TextView heading=label(region.toUpperCase(Locale.ROOT),15,0xffdab8ff);
  heading.setTypeface(null,Typeface.BOLD);attach(panel,heading,1,9);
  JSONArray source=saved.optJSONArray(section);
  if(saved.optBoolean(section+"_unreadable",false)){
   TextView shape=label(en?"Export format unrecognized; previously synced items kept.":
     "Exportformaat niet herkend; eerdere items behouden.",12,0xffffc277);
   attach(panel,shape,1,5);
  }
  if(source==null||source.length()==0){
   TextView empty=label(source==null?
    (en?"Not included in this export.":"Niet aanwezig in deze export."):
    (en?"No items listed in this export.":"Geen items vermeld in deze export."),13,SUB);
   attach(panel,empty,1,10);return;
  }
  ArrayList<JSONObject> items=new ArrayList<>();
  for(int i=0;i<source.length();i++){
   JSONObject item=source.optJSONObject(i);
   if(item!=null)items.add(item);
  }
  Collections.sort(items,(one,two)->SyncCatalog.name(this,section,one.optLong("data",-1))
   .compareToIgnoreCase(SyncCatalog.name(this,section,two.optLong("data",-1))));
  TextView count=label(items.size()+(en?" item types found":" soorten gevonden"),12,SUB);
  attach(panel,count,1,4);
  int show=expanded?items.size():Math.min(30,items.size());
  for(int i=0;i<show;i++)renderItem(panel,section,items.get(i));
  if(show<items.size()){
   Button more=new Button(this);
   more.setText(en?"Show "+(items.size()-show)+" more":"Toon nog "+(items.size()-show)+" items");
   more.setAllCaps(false);more.setTextColor(GOLD);
   more.setOnClickListener(v->{expanded=true;render();});
   panel.addView(more,new LinearLayout.LayoutParams(-1,dp(50)));
  }
 }
 private void renderItem(LinearLayout panel,String section,JSONObject item){
  long id=item.optLong("data",-1);
  JSONObject info=SyncCatalog.entry(this,section,id);
  String title=info.optString("n","ID "+id);
  String subtitle;
  if(section.equals("equipment")){
   int level=item.optInt("lvl",0),max=info.optInt("max",0);
   String rarity=info.optString("r","");
   if(rarity.equalsIgnoreCase("Common"))rarity=en?"Common":"Gewoon";
   if(rarity.equalsIgnoreCase("Epic"))rarity=en?"Epic":"Episch";
   subtitle=(en?"Level ":"Level ")+level+(max>0?"/"+max:"");
   if(!rarity.isEmpty())subtitle+=" · "+rarity;
   String hero=info.optString("hero","");
   if(!hero.isEmpty())subtitle+=" · "+hero;
   String next=SyncCatalog.oreText(info,level,false);
   String rest=SyncCatalog.oreText(info,level,true);
   if(!next.isEmpty())subtitle+="\n"+(en?"Next level: ":"Volgend level: ")+next;
   if(!rest.isEmpty())subtitle+="\n"+(en?"To max (estimate): ":"Tot max (indicatie): ")+rest;
  }else{
   subtitle=(en?"Count in export: ":"Aantal in export: ")+item.optInt("cnt",1);
  }
  LinearLayout card=new LinearLayout(this);
  card.setOrientation(LinearLayout.VERTICAL);
  card.setBackground(shape(0xff30243e,0xff654875));
  TextView name=label(title,15,PALE);name.setTypeface(null,Typeface.BOLD);
  name.setPadding(dp(12),dp(10),dp(12),dp(3));card.addView(name);
  TextView detail=label(subtitle,12,SUB);
  detail.setPadding(dp(12),0,dp(12),dp(12));card.addView(detail);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
  lp.setMargins(dp(3),dp(3),dp(3),dp(5));panel.addView(card,lp);
 }
}
