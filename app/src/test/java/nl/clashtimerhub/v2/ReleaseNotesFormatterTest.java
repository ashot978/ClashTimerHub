package nl.clashtimerhub.v2;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReleaseNotesFormatterTest {
 @Test public void allBulletsSurviveIncludingMoreThanSix(){
  StringBuilder input=new StringBuilder("### NL\n");
  for(int i=1;i<=10;i++)input.append("- Punt ").append(i).append("\n");
  input.append("### EN\n- Other change\n");
  String nl=ReleaseNotesFormatter.format(input.toString(),false);
  assertTrue(nl.contains("• Punt 1"));
  assertTrue(nl.contains("• Punt 10"));
  assertFalse(nl.contains("Other change"));
  assertEquals(10,nl.split("\n").length);
 }
 @Test public void longBulletsAreNotCutOff(){
  StringBuilder longText=new StringBuilder();
  for(int i=0;i<210;i++)longText.append('x');
  String full=ReleaseNotesFormatter.format("### NL\n- "+longText+"\n### EN\n- Hello",false);
  assertEquals("• "+longText,full);
  assertFalse(full.endsWith("…"));
 }
 @Test public void languageSelectionAndBuildFooterAreCorrect(){
  String input="## Build #541\n### NL\n- Een update\n### EN\n- A short update\n## Build information\nBuild #541";
  assertEquals("• Een update",ReleaseNotesFormatter.format(input,false));
  assertEquals("• A short update",ReleaseNotesFormatter.format(input,true));
 }
 @Test public void formattedMarkdownBulletAndMissingNotes(){
  assertEquals("• Bold fix",ReleaseNotesFormatter.format("### EN\n* **Bold** fix",true));
  assertEquals("See the full list of changes on GitHub.",ReleaseNotesFormatter.format("",true));
 }
}
