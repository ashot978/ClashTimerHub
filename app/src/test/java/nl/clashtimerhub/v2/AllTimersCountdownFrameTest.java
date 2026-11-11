package nl.clashtimerhub.v2;

import org.junit.Test;
import static org.junit.Assert.*;

public class AllTimersCountdownFrameTest {
 @Test public void activeTimerKeepsItsCountdownFrame(){
  assertTrue(AllTimersTestService.showCountdownFrame(21000L,1000L));
 }
 @Test public void completedAndMaxTimersRemoveEmptyFrame(){
  assertFalse(AllTimersTestService.showCountdownFrame(1000L,1000L));
  assertFalse(AllTimersTestService.showCountdownFrame(500L,1000L));
  assertFalse(AllTimersTestService.showCountdownFrame(Long.MAX_VALUE,1000L));
 }
}
