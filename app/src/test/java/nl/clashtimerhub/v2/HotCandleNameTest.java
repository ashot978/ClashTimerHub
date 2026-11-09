package nl.clashtimerhub.v2;

import org.junit.Test;
import static org.junit.Assert.*;

public class HotCandleNameTest {
 @Test public void hotCandleHitpointsUsesExportedModuleId(){
  assertEquals("Hot Candle - Hitpoints",ClashNames.name(102000033L));
 }
 @Test public void previouslyImportedFallbackResolvesWithoutResync(){
  assertEquals("Hot Candle - Hitpoints",
   ClashNames.resolvedName("Upgrade #102000033",102000033L));
 }
 @Test public void otherHotCandleModulesRemainUnchanged(){
  assertEquals("Hot Candle - DPS",ClashNames.name(102000034L));
  assertEquals("Hot Candle - TSA",ClashNames.name(102000035L));
 }
}
