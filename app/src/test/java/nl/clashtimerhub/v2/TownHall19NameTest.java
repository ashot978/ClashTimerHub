package nl.clashtimerhub.v2;

import org.junit.Test;
import static org.junit.Assert.*;

/** TH19 buildings from the 16 Nov 2026 game-data update.
 * Spiky Tower level 1 is 15d; Firecracker Tower level 1 is 14d 12h.
 */
public class TownHall19NameTest {
 private static final long[] IDS={1000105L,1000106L,1000107L,1000108L};
 private static final String[] NAMES={"Guardian Shrine","Squad Barracks","Spiky Tower","Firecracker Tower"};

 @Test public void townHall19BuildingsHaveTheirOwnNames(){
  for(int i=0;i<IDS.length;i++)
   assertEquals("TH19 building ID "+IDS[i],NAMES[i],ClashNames.name(IDS[i]));
 }

 @Test public void previouslySavedNumberOnlyTimersSelfHealWithoutImport(){
  for(int i=0;i<IDS.length;i++){
   assertEquals(NAMES[i],ClashNames.resolvedName("Upgrade #"+IDS[i],IDS[i]));
   assertEquals(NAMES[i],ClashNames.resolvedName("Upgrade #"+IDS[i],-1L));
  }
 }

 @Test public void customNamesArePreservedAndUnknownIdsStayTraceable(){
  assertEquals("My custom tower",ClashNames.resolvedName("My custom tower",1000108L));
  assertEquals("Upgrade #1000109",ClashNames.resolvedName("Upgrade #1000109",-1L));
 }
}
