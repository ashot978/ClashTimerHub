package nl.clashtimerhub.v2;

import org.junit.Test;
import static org.junit.Assert.*;

/** Regression coverage for newer documented Home Village IDs. */
public class KnownUpgradeNameCoverageTest {
 @Test public void allDocumentedNewUpgradeIdsHaveReadableNames(){
  long[] ids={1000064L,28000007L,93000000L,93000001L,93000002L,107000000L,107000001L,103000033L,4000095L,4000097L,4000109L,4000110L,4000123L,4000132L,4000135L,4000150L,4000177L,4000188L,26000098L,26000120L,26000123L};
  String[] expected={"B.O.B's Hut","Dragon Duke","Builder's Apprentice","Lab Assistant","Prospector","Longshot","Smasher","Hot Candle - Hitpoints","Electro Titan","Apprentice Warden","Ruin Witch","Root Rider","Druid","Thrower","Troop Launcher","Furnace","Meteor Golem","Sky Wagon","Revive Spell","Totem Spell","Angry Spell"};
  assertEquals(ids.length,expected.length);
  for(int i=0;i<ids.length;i++){
   assertEquals("Wrong name for exported ID "+ids[i],expected[i],ClashNames.name(ids[i]));
   assertEquals("Saved fallback should self-heal for "+ids[i],expected[i],
    ClashNames.resolvedName("Upgrade #"+ids[i],ids[i]));
   assertEquals("Fallback should resolve without sourceId "+ids[i],expected[i],
    ClashNames.resolvedName("Upgrade #"+ids[i],-1L));
  }
 }
 @Test public void previouslyImportedTroopLauncherNotifiesWithItsRealName(){
  String name=ClashNames.resolvedName("Upgrade #4000135",4000135L);
  assertEquals("Troop Launcher",name);
  assertEquals("Troop Launcher · Check in Clash",NotificationCopy.body(name,false,true,0L));
  assertEquals("Troop Launcher · 1 min remaining",NotificationCopy.body(name,true,true,60000L));
 }
 @Test public void userEditedNamesRemainAndUnknownIdsAreTraceable(){
  assertEquals("Custom Siege",ClashNames.resolvedName("Custom Siege",4000135L));
  assertEquals("Upgrade #4999999",ClashNames.resolvedName("Upgrade #4999999",4999999L));
 }
}
