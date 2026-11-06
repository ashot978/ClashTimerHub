package nl.clashtimerhub.v2;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/** Per-widget settings. Never mutates the imported or potion-adjusted upgrade data. */
final class AllTimersWidgetSettings {
 static final String LAB="lab",PETS="pets",BUILDERS="builders";
 static final int NEXT_BUILDER=0,NAME=1,MANUAL=2;
 static final int SINGLE_LINE=0,TWO_LINES=1;
 private static final String PREFS="WidgetPreferences";
 private static final String[] DEFAULT={LAB,PETS,BUILDERS};
 private AllTimersWidgetSettings(){}
 static SharedPreferences preferences(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
 static String key(String field,int widgetId){return "allTimers_"+field+"_"+widgetId;}
 static boolean validColumn(String value){return LAB.equals(value)||PETS.equals(value)||BUILDERS.equals(value);}
 static String[] order(Context ctx,int id){
  ArrayList<String> saved=new ArrayList<>();
  try{
   JSONArray raw=new JSONArray(preferences(ctx).getString(key("columns",id),"[]"));
   for(int i=0;i<raw.length();i++){
    String value=raw.optString(i);
    if(validColumn(value)&&!saved.contains(value))saved.add(value);
   }
  }catch(Exception ignored){}
  for(String item:DEFAULT)if(!saved.contains(item))saved.add(item);
  return saved.toArray(new String[0]);
 }
 static int sort(Context ctx,int id){
  int mode=preferences(ctx).getInt(key("sort",id),NEXT_BUILDER);
  return mode>=NEXT_BUILDER&&mode<=MANUAL?mode:NEXT_BUILDER;
 }
 static boolean twoLines(Context ctx,int id){
  return preferences(ctx).getInt(key("rowLines",id),SINGLE_LINE)==TWO_LINES;
 }
 static boolean all(Context ctx,int id){return preferences(ctx).getBoolean(key("all",id),true);}
 static ArrayList<String> manualAccounts(Context ctx,int id,List<String> current){
  ArrayList<String> ordered=new ArrayList<>();
  try{
   JSONArray raw=new JSONArray(preferences(ctx).getString(key("accountsOrder",id),"[]"));
   for(int i=0;i<raw.length();i++){
    String name=raw.optString(i);
    if(current.contains(name)&&!ordered.contains(name))ordered.add(name);
   }
  }catch(Exception ignored){}
  for(String account:current)if(!ordered.contains(account))ordered.add(account);
  return ordered;
 }
 static ArrayList<String> savedSelected(Context ctx,int id){
  ArrayList<String> selected=new ArrayList<>();
  try{
   JSONArray raw=new JSONArray(preferences(ctx).getString(key("selected",id),"[]"));
   for(int i=0;i<raw.length();i++){
    String a=raw.optString(i);
    if(!a.isEmpty()&&!selected.contains(a))selected.add(a);
   }
  }catch(Exception ignored){}
  return selected;
 }
 static ArrayList<AllTimersTestData.Account> visibleAccounts(Context ctx,int widgetId,
    AllTimersTestData.Snapshot snapshot){
  ArrayList<AllTimersTestData.Account> out=new ArrayList<>();
  boolean showAll=all(ctx,widgetId);
  HashSet<String> selection=new HashSet<>(savedSelected(ctx,widgetId));
  ArrayList<String> current=new ArrayList<>();
  for(AllTimersTestData.Account a:snapshot.overview){
   current.add(a.key);
   if(showAll||selection.contains(a.key))out.add(a);
  }
  ArrayList<String> manual=manualAccounts(ctx,widgetId,current);
  HashMap<String,Integer> ranks=new HashMap<>();
  for(int i=0;i<manual.size();i++)ranks.put(manual.get(i),i);
  int mode=sort(ctx,widgetId);
  Collections.sort(out,(a,b)->{
   if(mode==NEXT_BUILDER){
    // A completed upgrade not yet acknowledged by a fresh account sync is
    // always above accounts with only running upgrades. This includes
    // builders, lab (also goblin research) and pets, with or without Potions.
    // When there are no pending completions, sort by the first effective
    // Builder finish, which already includes Builder Potion acceleration.
    int aPending=a.ready+a.labReady+a.petReady;
    int bPending=b.ready+b.labReady+b.petReady;
    if((aPending>0)!=(bPending>0))return aPending>0?-1:1;
    if(aPending>0&&bPending>0&&aPending!=bPending)
     return Integer.compare(bPending,aPending);
    int c=Long.compare(a.builderAt,b.builderAt);
    if(c!=0)return c;
   }else if(mode==NAME){
    int c=AppData.displayName(ctx,a.key).compareToIgnoreCase(AppData.displayName(ctx,b.key));
    if(c!=0)return c;
   }
   return Integer.compare(ranks.getOrDefault(a.key,9999),ranks.getOrDefault(b.key,9999));
  });
  return out;
 }
 static int boostedCount(AllTimersTestData.Snapshot data,List<AllTimersTestData.Account> rows){
  HashSet<String> keys=new HashSet<>();
  for(AllTimersTestData.Account a:rows)keys.add(a.key);
  int n=0;
  for(AllTimersTestData.Item item:data.items)if(item.boosted&&keys.contains(item.account))n++;
  return n;
 }
 static void delete(Context ctx,int id){
  SharedPreferences.Editor e=preferences(ctx).edit();
  for(String field:new String[]{"columns","sort","rowLines","all","selected","accountsOrder","helpers"})
   e.remove(key(field,id));
  e.apply();
 }
}