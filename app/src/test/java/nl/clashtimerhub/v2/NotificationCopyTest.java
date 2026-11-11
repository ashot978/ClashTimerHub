package nl.clashtimerhub.v2;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** Regression tests for both language variants of each alert type. */
public class NotificationCopyTest {
 @Test public void dutchAdvanceWarningIsShort(){
  assertEquals("AshotRush · Bijna klaar",NotificationCopy.title("AshotRush",true,false));
  assertEquals("Ricochet Cannon · nog 5 minuten",
   NotificationCopy.body("Ricochet Cannon",true,false,5L*60_000L));
  assertEquals("Barbarian King · nog 1 minuut",
   NotificationCopy.body("Barbarian King",true,false,60_000L));
 }
 @Test public void englishAdvanceWarningIsShort(){
  assertEquals("AshotRush · Almost ready",NotificationCopy.title("AshotRush",true,true));
  assertEquals("Multi-Archer Tower · 10 min remaining",
   NotificationCopy.body("Multi-Archer Tower",true,true,10L*60_000L));
 }
 @Test public void dutchCompletionSuggestsVerifyingInGame(){
  assertEquals("AshotRush · Upgrade klaar!",NotificationCopy.title("AshotRush",false,false));
  assertEquals("Ricochet Cannon · Controleer in Clash",
   NotificationCopy.body("Ricochet Cannon",false,false,0L));
 }
 @Test public void englishCompletionSuggestsVerifyingInGame(){
  assertEquals("AshotRush · Upgrade complete!",NotificationCopy.title("AshotRush",false,true));
  assertEquals("Ricochet Cannon · Check in Clash",
   NotificationCopy.body("Ricochet Cannon",false,true,0L));
 }
}
