package nl.clashtimerhub.v2;

import org.junit.Test;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

/** Covers the eight Home Village sources that can contain active timers. */
public class VillageExportCategoryCoverageTest {
 private static final String[] HOME_VILLAGE_TIMER_SECTIONS={
  "buildings","heroes","units","spells","siege_machines","pets","traps","guardians"
 };
 private static final String[] BUILDER_BASE_SECTIONS={
  "buildings2","traps2","units2","heroes2"
 };

 @Test public void allHomeVillageTimerSourcesAreImportedOnce(){
  Set<String> actual=new HashSet<>(Arrays.asList(QuickSyncImporter.TIMER_SECTIONS));
  assertEquals("Unexpected duplicate timer sections",
   QuickSyncImporter.TIMER_SECTIONS.length,actual.size());
  assertEquals(new HashSet<>(Arrays.asList(HOME_VILLAGE_TIMER_SECTIONS)),actual);
 }

 @Test public void builderBaseIsNotAccidentallyCountedAsHomeVillageBuilders(){
  Set<String> imported=new HashSet<>(Arrays.asList(QuickSyncImporter.TIMER_SECTIONS));
  for(String base:BUILDER_BASE_SECTIONS)
   assertFalse("Builder Base should require independent categories and counts: "+base,
    imported.contains(base));
 }

 @Test public void guardiansAreImportedAndLoggerNameIsRecognized(){
  assertTrue(Arrays.asList(QuickSyncImporter.TIMER_SECTIONS).contains("guardians"));
  assertEquals("Logger",ClashNames.name(107000008L));
 }

 @Test public void allNineSeasonalDefenseModulesHaveReadableNames(){
  String[][] labels={
   {"Hot Candle - Hitpoints","Hot Candle - DPS","Hot Candle - TSA"},
   {"Hero Hunter - Hitpoints","Hero Hunter - DPS","Hero Hunter - PSL"},
   {"Cake-A-Pult - Hitpoints","Cake-A-Pult - DPH","Cake-A-Pult - ED"}
  };
  for(int defense=0;defense<3;defense++){
   for(int module=0;module<3;module++){
    long id=102000033L+defense*3+module;
    assertEquals(labels[defense][module],ClashNames.name(id));
    assertEquals(labels[defense][module],
     ClashNames.resolvedName("Upgrade #"+id,id));
   }
  }
 }
}
