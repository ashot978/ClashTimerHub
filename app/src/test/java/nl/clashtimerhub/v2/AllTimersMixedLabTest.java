package nl.clashtimerhub.v2;

import org.junit.Test;
import static org.junit.Assert.*;

public class AllTimersMixedLabTest {
 @Test public void completedResearchAndRunningResearchShowBoth(){
  assertTrue(AllTimersTestService.showMixedLabReady(
   AllTimersWidgetSettings.LAB,1,120000L,60000L));
 }
 @Test public void twoCompletedResearchAndRunningResearchShowCount(){
  assertTrue(AllTimersTestService.showMixedLabReady(
   AllTimersWidgetSettings.LAB,2,120000L,60000L));
 }
 @Test public void ordinaryActiveResearchRetainsExistingAppearance(){
  assertFalse(AllTimersTestService.showMixedLabReady(
   AllTimersWidgetSettings.LAB,0,120000L,60000L));
 }
 @Test public void finishedResearchWithoutRunningResearchRetainsReadyStatus(){
  assertFalse(AllTimersTestService.showMixedLabReady(
   AllTimersWidgetSettings.LAB,1,60000L,60000L));
 }
 @Test public void maxedResearchRetainsMaxStatus(){
  assertFalse(AllTimersTestService.showMixedLabReady(
   AllTimersWidgetSettings.LAB,0,Long.MAX_VALUE,60000L));
 }
 @Test public void otherCategoriesRemainUnchanged(){
  assertFalse(AllTimersTestService.showMixedLabReady(
   AllTimersWidgetSettings.BUILDERS,1,120000L,60000L));
  assertFalse(AllTimersTestService.showMixedLabReady(
   AllTimersWidgetSettings.PETS,1,120000L,60000L));
 }
}
