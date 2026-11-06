package nl.clashtimerhub.v2;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Optional upgrade artwork in the selected account's upgrade list only. */
final class UpgradeIcons {
 private static final String BASE="https://raw.githubusercontent.com/ClashKingInc/ClashKingAssets/main/assets/";
 private static final android.util.LruCache<String,Bitmap> CACHE=new android.util.LruCache<>(80);
 private static final Set<String> BUILDINGS=new HashSet<>(Arrays.asList(
  "army_camp","town_hall","elixir_collector","elixir_storage","gold_mine","gold_storage",
  "barracks","laboratory","cannon","archer_tower","wall","wizard_tower","air_defense",
  "mortar","clan_castle","builder's_hut","hidden_tesla","spell_factory","x-bow",
  "dark_elixir_drill","dark_elixir_storage","dark_barracks","inferno_tower","air_sweeper",
  "dark_spell_factory","eagle_artillery","bomb_tower","workshop","scattershot",
  "pet_house","blacksmith","hero_hall","spell_tower","monolith","multi-archer_tower",
  "ricochet_cannon","firespitter","multi-gear_tower","crafting_station"));
 private static final Set<String> TRAPS=new HashSet<>(Arrays.asList(
  "bomb","spring_trap","giant_bomb","air_bomb","seeking_air_mine","skeleton_trap",
  "tornado_trap","giga_bomb"));
 private static final Set<String> HEROES=new HashSet<>(Arrays.asList(
  "barbarian_king","archer_queen","grand_warden","royal_champion","minion_prince"));
 private static final Set<String> PETS=new HashSet<>(Arrays.asList(
  "lassi","electro_owl","mighty_yak","unicorn","frosty","diggy",
  "poison_lizard","phoenix","spirit_fox","angry_jelly","sneezy"));
 private static final ExecutorService WORKERS=Executors.newFixedThreadPool(3);

 static FrameLayout view(Activity activity,JSONObject upgrade){
  String category=upgrade.optString("category","");
  String name=upgrade.optString("name","");
  String key=normalized(name);
  FrameLayout frame=new FrameLayout(activity);
  TextView placeholder=new TextView(activity);
  placeholder.setText("Pets".equals(category)?"🐾":"Laboratorium".equals(category)?"🧙":"⚒");
  placeholder.setTextSize(22);
  placeholder.setGravity(Gravity.CENTER);
  frame.addView(placeholder,new FrameLayout.LayoutParams(-1,-1));
  String path=path(key,category);
  if(path==null)return frame;
  ImageView icon=new ImageView(activity);
  icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
  icon.setVisibility(android.view.View.GONE);
  frame.addView(icon,new FrameLayout.LayoutParams(-1,-1));
  Bitmap cached;
  synchronized(CACHE){cached=CACHE.get(path);}
  if(cached!=null){icon.setImageBitmap(cached);icon.setVisibility(android.view.View.VISIBLE);return frame;}
  WORKERS.execute(()->{
   HttpURLConnection conn=null;
   try{
    conn=(HttpURLConnection)new URL(BASE+path).openConnection();
    conn.setConnectTimeout(4500);
    conn.setReadTimeout(5500);
    if(conn.getResponseCode()!=200)return;
    Bitmap bitmap;
    try(InputStream stream=conn.getInputStream()){bitmap=BitmapFactory.decodeStream(stream);}
    if(bitmap==null)return;
    synchronized(CACHE){CACHE.put(path,bitmap);}
    activity.runOnUiThread(()->{
     if(!activity.isFinishing()&&!activity.isDestroyed()){
      icon.setImageBitmap(bitmap);
      icon.setVisibility(android.view.View.VISIBLE);
     }
    });
   }catch(Exception ignored){
    // Keep the category symbol if artwork is unavailable/offline.
   }finally{if(conn!=null)conn.disconnect();}
  });
  return frame;
 }

 private static String normalized(String name){
  String key=name.trim().toLowerCase(Locale.ROOT)
   .replace("l.a.s.s.i","lassi").replace("p.e.k.k.a","pekka")
   .replace("x-bow","x-bow").replace("’","'")
   .replaceAll("[^a-z0-9' -]","").replace(' ','_');
  return key;
 }
 private static String path(String key,String category){
  if(BUILDINGS.contains(key))return "buildings/home-village/"+key.replace("'","%27")+"/level_1.webp";
  if(TRAPS.contains(key))return "traps/home-village/"+key+"/level_1.webp";
  if(HEROES.contains(key))return "heroes/"+key+"/icon.webp";
  if(PETS.contains(key))return "pets/"+key+"/icon.webp";
  if(key.endsWith("_spell"))return "spells/"+key+".webp";
  if("Laboratorium".equals(category))return "troops/"+key+"/icon.webp";
  return null;
 }
 private UpgradeIcons(){}
}
