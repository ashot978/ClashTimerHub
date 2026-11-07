package nl.clashtimerhub.v2;
import android.content.Context;

/** Same effective, realtime upgrade finish for widget and notification engine. */
final class UpgradeForecast {
 private UpgradeForecast(){}
 static long finish(Context ctx,String account,String category,long sourceFinish){
  if("Bouwers".equals(category)||"Laboratorium".equals(category)||"Pets".equals(category))
   return MagicSnackTracker.estimateFinish(ctx,account,category,sourceFinish);
  return sourceFinish;
 }
}
