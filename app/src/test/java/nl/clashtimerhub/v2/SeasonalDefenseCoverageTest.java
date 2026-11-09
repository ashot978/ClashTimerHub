package nl.clashtimerhub.v2;

import org.junit.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

/** Regression coverage for the three 3-module seasonal defenses in Clash exports. */
public class SeasonalDefenseCoverageTest {
 private static final long[][] MODULE_IDS={
  {102000033L,102000034L,102000035L}, // Hot Candle
  {102000036L,102000037L,102000038L}, // Hero Hunter
  {102000039L,102000040L,102000041L}  // Cake-a-Pult
 };
 private static final String[][] LABELS={
  {"Hot Candle - Hitpoints","Hot Candle - DPS","Hot Candle - TSA"},
  {"Hero Hunter - Hitpoints","Hero Hunter - DPS","Hero Hunter - PSL"},
  {"Cake-A-Pult - Hitpoints","Cake-A-Pult - DPH","Cake-A-Pult - ED"}
 };

 @Test public void allNineModuleIdsHaveTheExpectedReadableNames(){
  Set<Long> unique=new HashSet<>();
  int checked=0;
  for(int defense=0;defense<MODULE_IDS.length;defense++){
   assertEquals("Each defense must have three module upgrades",3,MODULE_IDS[defense].length);
   for(int module=0;module<3;module++){
    long id=MODULE_IDS[defense][module];
    assertTrue("Duplicate module id "+id,unique.add(id));
    assertEquals(LABELS[defense][module],ClashNames.name(id));
    checked++;
   }
  }
  assertEquals(9,checked);
  assertEquals(9,unique.size());
 }

 @Test public void existingImportedFallbacksResolveForAllNineWithoutResync(){
  for(int defense=0;defense<3;defense++)
   for(int module=0;module<3;module++){
    long id=MODULE_IDS[defense][module];
    assertEquals(LABELS[defense][module],
     ClashNames.resolvedName("Upgrade #"+id,id));
   }
 }

 @Test public void parentDefenseIdsAndPriorUpgradesStillResolve(){
  assertEquals("Hot Candle",ClashNames.name(103000011L));
  assertEquals("Hero Hunter",ClashNames.name(103000012L));
  assertEquals("Cake-A-Pult",ClashNames.name(103000013L));
  assertEquals("Logger",ClashNames.name(107000008L));
 }
}
