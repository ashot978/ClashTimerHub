package nl.clashtimerhub.v2;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertEquals;

/** Protects snack/potion overlap math from regressions. Milliseconds used throughout. */
public class MagicSnackTrackerTest {
 private static final long HOUR=3_600_000L;
 @Test public void studySoupFourTimesOneHour(){
  assertEquals(HOUR,MagicSnackTracker.integrate(0L,4L*HOUR,0L,HOUR,
   false,Collections.emptyList()));
 }
 @Test public void expiredStudySoupFallsBackToNormal(){
  assertEquals(3L*HOUR,MagicSnackTracker.integrate(0L,6L*HOUR,0L,HOUR,
   false,Collections.emptyList()));
 }
 @Test public void builderBiteTwiceAsFast(){
  assertEquals(HOUR,MagicSnackTracker.integrate(0L,2L*HOUR,0L,HOUR,
   true,Collections.emptyList()));
 }
 @Test public void builderBiteAndPotionAddUpToElevenTimes(){
  // 30m at 11x completes 5h30m of work; final 30m of work takes 15m at 2x.
  assertEquals(3L*HOUR/4L,MagicSnackTracker.integrate(0L,6L*HOUR,0L,HOUR,
   true,Arrays.asList(new long[]{0L,HOUR/2L})));
 }
 @Test public void studySoupAndResearchPotionAreAdditive(){
  // First 30m at 27x = 13.5 hours work, next 30m at 4x = 2h.
  assertEquals(HOUR,MagicSnackTracker.integrate(0L,15L*HOUR+HOUR/2L,0L,HOUR,
   false,Arrays.asList(new long[]{0L,HOUR/2L})));
 }
 @Test public void exportTimeAnchorPreventsPotionDelayOvercount(){
  // The export says 10h38m normal work is left. The potion runs 58m25s
  // from export; the clipboard is imported 90s later.
  long minute=60_000L, export=1_000_000_000L;
  long potionEnd=export+58L*minute+25_000L;
  long actual=MagicSnackTracker.integrate(export,638L*minute,
   0L,0L,true,Arrays.asList(new long[]{export,potionEnd}));
  assertEquals(export+112L*minute+15_000L,actual);
  long importTime=export+90_000L;
  long incorrectlyAnchored=MagicSnackTracker.integrate(importTime,
   638L*minute-90_000L,0L,0L,true,
   Arrays.asList(new long[]{export,potionEnd}));
  assertEquals(13L*minute+30_000L,incorrectlyAnchored-actual);
 }
 @Test public void exportTimeAnchorHandlesPotionAndBuilderBite(){
  long minute=60_000L, export=2_000_000_000L;
  long potionEnd=export+58L*minute+25_000L;
  long snackEnd=export+58L*minute+6_000L;
  long predicted=MagicSnackTracker.integrate(export,638L*minute,
   export,snackEnd,true,Arrays.asList(new long[]{export,potionEnd}));
  // Concurrent 10x potion + 2x Bite equals 11x and finishes 638m of work in 58m.
  assertEquals(export+58L*minute,predicted);
 }
 @Test public void builderBiteCarriesOnAfterPotionEnds(){
  // 30m at 11x completes 330m of work; 30m at 2x completes the last 60m.
  assertEquals(HOUR,MagicSnackTracker.integrate(0L,390L*60_000L,0L,HOUR,
   true,Arrays.asList(new long[]{0L,HOUR/2L})));
 }
 @Test public void futureSnackStartsOnlyWhenActivated(){
  assertEquals(HOUR+HOUR/2L,MagicSnackTracker.integrate(0L,2L*HOUR,
   HOUR,HOUR+HOUR, true,Collections.emptyList()));
 }
 @Test public void nineHoursTwentyOneMinutesWithAnHourOfBuilderPotionFinishesInFiftySixMinutes(){
  // Screenshot: shortest Bomb has 9h21m of regular work, with 59m of 10x boost.
  // At 10x, 9h21m of work finishes in 56m06s, before the boost ends.
  long minute=60_000L;
  long work=9L*HOUR+21L*minute;
  long activeBoost=59L*minute;
  assertEquals(56L*minute+6_000L,MagicSnackTracker.integrate(
   0L,work,0L,0L,true,Arrays.asList(new long[]{0L,activeBoost})));
 }
 @Test public void builderPotionExpiryReturnsToNormalSpeed(){
  // Once a 10x potion ends, the unprocessed work runs at normal speed.
  long minute=60_000L;
  long work=9L*HOUR+21L*minute;
  long activeBoost=50L*minute;
  assertEquals(111L*minute,MagicSnackTracker.integrate(
   0L,work,0L,0L,true,Arrays.asList(new long[]{0L,activeBoost})));
 }
}
