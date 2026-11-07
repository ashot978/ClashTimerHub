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
 @Test public void builderBiteDoesNotStackWithBuilderPotion(){
  assertEquals(HOUR,MagicSnackTracker.integrate(0L,6L*HOUR,0L,HOUR,
   true,Arrays.asList(new long[]{0L,HOUR/2L})));
 }
 @Test public void studySoupAndResearchPotionAreAdditive(){
  // First 30m at 27x = 13.5 hours work, next 30m at 4x = 2h.
  assertEquals(HOUR,MagicSnackTracker.integrate(0L,15L*HOUR,0L,HOUR,
   false,Arrays.asList(new long[]{0L,HOUR/2L})));
 }
 @Test public void futureSnackStartsOnlyWhenActivated(){
  assertEquals(HOUR+HOUR/2L,MagicSnackTracker.integrate(0L,2L*HOUR,
   HOUR,HOUR+HOUR, true,Collections.emptyList()));
 }
}
