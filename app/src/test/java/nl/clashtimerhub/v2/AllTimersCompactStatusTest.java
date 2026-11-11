package nl.clashtimerhub.v2;

import org.junit.Test;
import static org.junit.Assert.*;

public class AllTimersCompactStatusTest {
 @Test public void maxedStatusStaysCompact(){
  assertEquals("✓ MAX",AllTimersTestService.compactIdleLabel(true,false,false));
 }
 @Test public void readyStatusStaysCompactInBothLanguages(){
  assertEquals("! Klaar",AllTimersTestService.compactIdleLabel(false,true,false));
  assertEquals("! Ready",AllTimersTestService.compactIdleLabel(false,true,true));
 }
 @Test public void idleStatusIsUnchanged(){
  assertEquals("—",AllTimersTestService.compactIdleLabel(false,false,false));
 }
}
