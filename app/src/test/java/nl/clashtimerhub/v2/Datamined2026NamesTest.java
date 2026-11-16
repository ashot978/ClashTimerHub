package nl.clashtimerhub.v2;
import org.junit.Test;
import static org.junit.Assert.*;
/** The Clash of Clans 16 Nov 2026 game-file GlobalID dataset. */
public class Datamined2026NamesTest {
 @Test public void allTwelveSquadIdsHaveReadableNames(){
  String[] names={"Barch Squad","Giant Wizard Squad","Wall Breaker Squad","Minion Squad","Balloon Squad","Golem Witch Squad","Hog Rider Squad","Miner Squad","Healer Squad","P.E.K.K.A Titan Squad","Dragon Squad","Baby Dragon Squad"};
  for(int i=0;i<names.length;i++){
   long id=4000195L+i;
   assertEquals(names[i],ClashNames.name(id));
   assertEquals(names[i],ClashNames.resolvedName("Upgrade #"+id,-1L));
  }
 }
 @Test public void newBuildingsHeroicSpellAndSavedFallbacksResolve(){
  assertEquals("Guardian Shrine",ClashNames.name(1000105L));
  assertEquals("Squad Barracks",ClashNames.name(1000106L));
  assertEquals("Spiky Tower",ClashNames.name(1000107L));
  assertEquals("Firecracker Tower",ClashNames.name(1000108L));
  assertEquals("Heroic Spell",ClashNames.name(26000153L));
  assertEquals("Heroic Spell",ClashNames.resolvedName("Upgrade #26000153",26000153L));
 }
 @Test public void seasonalDefenseArchetypesAndModulesHaveNames(){
  for(long id=103000000L;id<=103000016L;id++)
   assertFalse("Unknown seasonal defense "+id,ClashNames.name(id).startsWith("Upgrade #"));
  for(long id=102000000L;id<=102000050L;id++){
   assertFalse("Unknown seasonal module "+id,ClashNames.name(id).startsWith("Upgrade #"));
   assertEquals(ClashNames.name(id),ClashNames.resolvedName("Upgrade #"+id,-1L));
  }
  assertEquals("Rocket Artillery Tower",ClashNames.name(103000014L));
  assertEquals("Minion Hive Launcher",ClashNames.name(103000015L));
  assertEquals("Goblin Thrower Tower",ClashNames.name(103000016L));
  assertEquals("Goblin Thrower Tower - Effect",ClashNames.name(102000050L));
 }
 @Test public void guardianIdsAndTheirExistingKnownNamesResolve(){
  for(long id=107000000L;id<=107000009L;id++)
   assertFalse("Unknown guardian "+id,ClashNames.name(id).startsWith("Upgrade #"));
  assertEquals("Longshot",ClashNames.name(107000000L));
  assertEquals("Smasher",ClashNames.name(107000001L));
  assertEquals("Logger",ClashNames.name(107000008L));
  assertEquals("Returner Guardian",ClashNames.name(107000009L));
 }
 @Test public void newEventCharacterAndSpellIdsAreMapped(){
  for(long id=4000207L;id<=4000212L;id++)
   assertFalse("Unknown event character "+id,ClashNames.name(id).startsWith("Upgrade #"));
  for(long id=26000149L;id<=26000154L;id++)
   assertFalse("Unknown spell "+id,ClashNames.name(id).startsWith("Upgrade #"));
 }
 @Test public void customNamesRemainAndUnverifiedIdsStayNumeric(){
  assertEquals("My squad",ClashNames.resolvedName("My squad",4000195L));
  assertEquals("Upgrade #4000213",ClashNames.resolvedName("Upgrade #4000213",-1L));
 }
}
