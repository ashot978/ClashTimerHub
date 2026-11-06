package nl.clashtimerhub.v2;
import android.content.Context;

/** Same effective, realtime upgrade finish for widget and notification engine. */
final class UpgradeForecast {
 private UpgradeForecast(){}
 static long finish(Context ctx,String account,String category,long sourceFinish){
  if("Bouwers".equals(category))return BuilderPotionTracker.estimateFinish(
   BuilderPotionTracker.load(ctx,account),sourceFinish);
  if("Laboratorium".equals(category))return ResearchPotionTracker.estimateFinish(
   ResearchPotionTracker.load(ctx,account),sourceFinish);
  return sourceFinish;
 }
}
