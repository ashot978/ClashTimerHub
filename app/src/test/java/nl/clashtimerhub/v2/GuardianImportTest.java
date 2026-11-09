package nl.clashtimerhub.v2;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class GuardianImportTest {
 @Test public void guardiansAreAnImportedTimerSection(){
  assertTrue(Arrays.asList(QuickSyncImporter.TIMER_SECTIONS).contains("guardians"));
 }
 @Test public void existingTimerSectionsRemainEnabled(){
  for(String section:new String[]{"buildings","heroes","units","spells","siege_machines","pets","traps"})
   assertTrue(Arrays.asList(QuickSyncImporter.TIMER_SECTIONS).contains(section));
 }
 @Test public void loggerUsesItsReadableName(){
  assertEquals("Logger",ClashNames.name(107000008L));
 }
 @Test public void ordinaryBuilderNamesRemainUnchanged(){
  assertEquals("Wizard Tower",ClashNames.name(1000011L));
  assertEquals("Revenge Tower",ClashNames.name(1000086L));
 }
}
