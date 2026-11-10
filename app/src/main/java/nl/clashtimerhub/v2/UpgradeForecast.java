package nl.clashtimerhub.v2;
import android.content.Context;
import org.json.JSONObject;

/** Same effective, realtime upgrade finish for widget and notification engine. */
final class UpgradeForecast {
 private UpgradeForecast(){}
 /** One forecast for the app, all widgets and completion notifications. */
 static long finish(Context ctx,JSONObject source){
  if(source==null)return 0L;
  long original=source.optLong("finish",0L);
  if(original<=0L)return original;
  return MagicSnackTracker.estimateFinish(ctx,source.optString("account",""),
   source.optString("category",""),original,
   source.optLong("helperUntil",0L),source.optInt("helperLevel",0));
 }
 static long finish(Context ctx,String account,String category,long sourceFinish){
  if("Bouwers".equals(category)||"Laboratorium".equals(category)||"Pets".equals(category))
   return MagicSnackTracker.estimateFinish(ctx,account,category,sourceFinish);
  return sourceFinish;
 }
}
