package nl.clashtimerhub.v2;

import org.junit.Test;
import static org.junit.Assert.*;

/** Regression coverage for the first Town Hall 19 building ID seen in exports. */
public class TownHall19NameTest {
 @Test public void squadBarracksHasReadableNameAcrossAllViews(){
  assertEquals("Squad Barracks",ClashNames.name(1000106L));
  assertEquals("Squad Barracks",
   ClashNames.resolvedName("Upgrade #1000106",1000106L));
  assertEquals("Squad Barracks",
   ClashNames.resolvedName("Upgrade #1000106",-1L));
 }
 @Test public void playerCustomizationsAndUnverifiedIdsAreNotGuessed(){
  assertEquals("My barracks",ClashNames.resolvedName("My barracks",1000106L));
  assertEquals("Upgrade #1000107",ClashNames.resolvedName("Upgrade #1000107",1000107L));
 }
}
