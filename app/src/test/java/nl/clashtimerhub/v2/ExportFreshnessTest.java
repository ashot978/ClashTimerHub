package nl.clashtimerhub.v2;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class ExportFreshnessTest {
 @Test public void anOlderExportCannotReplaceNewerAccountTimers(){
  long fresh=1794309600000L;
  assertTrue(QuickSyncImporter.olderThanStoredExport(fresh-39L*60_000L,fresh));
 }
 @Test public void repeatAndFreshExportsRemainAccepted(){
  long previous=1794309600000L;
  assertFalse(QuickSyncImporter.olderThanStoredExport(previous,previous));
  assertFalse(QuickSyncImporter.olderThanStoredExport(previous+1_000L,previous));
  assertFalse(QuickSyncImporter.olderThanStoredExport(previous,0L));
 }
 @Test public void archerQueenStillUsesHomeVillageBuilderImport(){
  assertTrue(Arrays.asList(QuickSyncImporter.TIMER_SECTIONS).contains("heroes"));
  assertEquals("Archer Queen",ClashNames.name(28000001L));
 }
}
