package nl.clashtimerhub.v2;

import org.junit.Test;
import static org.junit.Assert.*;

public class RoadmapTextTest {
 private static final String TEXT="# Roadmap\n\n## NL\n### Volgende\n- Testen\n\n## EN\n### Next\n- Testing\n";
 @Test public void selectsOnlyDutch(){
  assertEquals("### Volgende\n- Testen",RoadmapText.localized(TEXT,false));
 }
 @Test public void selectsOnlyEnglish(){
  assertEquals("### Next\n- Testing",RoadmapText.localized(TEXT,true));
 }
 @Test public void supportsWindowsLineEndings(){
  assertEquals("### Next\n- Testing",RoadmapText.localized(TEXT.replace("\n","\r\n"),true));
 }
 @Test public void missingSectionReturnsEmpty(){
  assertEquals("",RoadmapText.localized("## NL\n- Testen",true));
  assertEquals("",RoadmapText.localized(null,false));
 }
}
