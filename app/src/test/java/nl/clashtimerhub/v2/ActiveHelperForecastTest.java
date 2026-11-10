package nl.clashtimerhub.v2;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

/** Replay Ashotty's Queen with a level-eight Builder's Apprentice. */
public class ActiveHelperForecastTest {
 private static final long MINUTE=60_000L, HOUR=60L*MINUTE;

 @Test public void queenLevelEightHelperFinishesInAboutEightyOneMinutes(){
  long work=7L*HOUR+52L*MINUTE;
  long helper=48L*MINUTE+53_000L;
  assertEquals(80L*MINUTE+56_000L,MagicSnackTracker.integrate(
   0L,work,0L,0L,true,Collections.emptyList(),0L,helper,8));
 }

 @Test public void onlyAnAssignedUpgradeGetsTheHelperDiscount(){
  long work=7L*HOUR+52L*MINUTE,until=48L*MINUTE+53_000L;
  long faster=MagicSnackTracker.integrate(
   0L,work,0L,0L,true,Collections.emptyList(),0L,until,8);
  long normal=MagicSnackTracker.integrate(
   0L,work,0L,0L,true,Collections.emptyList(),0L,0L,0);
  assertEquals(work,normal);
  assertTrue(faster<normal);
 }

 @Test public void helperSpeedAddsToBuilderPotionWithoutMultiplyingIt(){
  assertEquals(30L*MINUTE,MagicSnackTracker.integrate(
   0L,9L*HOUR,0L,0L,true,Arrays.asList(new long[]{0L,HOUR}),
   0L,HOUR,8));
 }

 @Test public void remainderRunsAtNormalSpeedAfterHelperEnds(){
  assertEquals(3L*HOUR,MagicSnackTracker.integrate(
   0L,7L*HOUR,0L,0L,true,Collections.emptyList(),0L,HOUR/2L,8));
 }

 @Test public void labAssistantAlsoAddsItsLevelToResearchWork(){
  assertEquals(HOUR,MagicSnackTracker.integrate(
   0L,13L*HOUR,0L,0L,false,Collections.emptyList(),0L,HOUR,12));
 }
}
