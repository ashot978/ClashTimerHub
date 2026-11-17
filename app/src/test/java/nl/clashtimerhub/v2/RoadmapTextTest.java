package nl.clashtimerhub.v2;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class RoadmapTextTest {
 @Test public void headingsAndBulletsReadable(){
  List<RoadmapText.Entry> result=RoadmapText.parse(
   "# Roadmap\n## Gekozen\n- **Hero Equipment**\n- [Verzamelingen](https://example.com)");
  assertEquals(4,result.size());
  assertEquals(RoadmapText.TITLE,result.get(0).kind);
  assertEquals(RoadmapText.HEADING,result.get(1).kind);
  assertEquals("• Hero Equipment",result.get(2).text);
  assertEquals("• Verzamelingen",result.get(3).text);
 }
 @Test public void tableTurnsIntoCards(){
  List<RoadmapText.Entry> result=RoadmapText.parse(
   "| Priority | Idea | Source | Critical validation |\n"
   +"|---|---|---|---|\n| P1 | Equipment | Levels | Needs real export |");
  assertEquals(1,result.size());
  assertEquals(RoadmapText.CARD,result.get(0).kind);
  assertTrue(result.get(0).text.contains("P1 · Equipment"));
  assertTrue(result.get(0).text.contains("Needs real export"));
 }
 @Test public void emptyAndDivider(){
  assertTrue(RoadmapText.parse("").isEmpty());
  assertEquals(RoadmapText.DIVIDER,RoadmapText.parse("---").get(0).kind);
 }
}
