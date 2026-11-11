package nl.clashtimerhub.v2;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Collections;
import static org.junit.Assert.*;

public class NextTimerHighlightTest {
 private static AllTimersTestData.Account account(String key,long lab,long pets,long builders,long helpers){
  AllTimersTestData.Account a=new AllTimersTestData.Account(key);
  a.labAt=lab;a.petAt=pets;a.builderAt=builders;a.helperNext=helpers;
  return a;
 }
 @Test public void picksGlobalShortestNotJustFirstRowOrColumn(){
  long now=1_000L;
  ArrayList<AllTimersTestData.Account> shown=new ArrayList<>();
  shown.add(account("Shotty",now+27*60_000L,now+60_000_000L,now+2*60_000L,now+4*3_600_000L));
  shown.add(account("Ashotty",now+21_000L,now+30_000L,now+45_000L,now+40_000L));
  NextTimerHighlight.Selection selected=NextTimerHighlight.find(shown,true,now);
  assertTrue(selected.matches("Ashotty",AllTimersWidgetSettings.LAB,now+21_000L));
  Collections.reverse(shown);
  assertTrue(NextTimerHighlight.find(shown,true,now).matches(
   "Ashotty",AllTimersWidgetSettings.LAB,now+21_000L));
 }
 @Test public void helpersCompeteOnlyWhenVisible(){
  long now=1_000L;
  ArrayList<AllTimersTestData.Account> shown=new ArrayList<>();
  shown.add(account("A",now+20_000L,Long.MAX_VALUE,Long.MAX_VALUE,now+10_000L));
  assertTrue(NextTimerHighlight.find(shown,true,now).matches(
   "A",NextTimerHighlight.HELPERS,now+10_000L));
  assertTrue(NextTimerHighlight.find(shown,false,now).matches(
   "A",AllTimersWidgetSettings.LAB,now+20_000L));
 }
 @Test public void movesToNextAfterFinishAndIgnoresInactive(){
  long now=1_000L;
  ArrayList<AllTimersTestData.Account> shown=new ArrayList<>();
  shown.add(account("A",now+21_000L,Long.MAX_VALUE,now+45_000L,Long.MAX_VALUE));
  shown.add(account("B",now-20_000L,now+30_000L,0L,Long.MAX_VALUE));
  assertTrue(NextTimerHighlight.find(shown,true,now).matches(
   "A",AllTimersWidgetSettings.LAB,now+21_000L));
  assertTrue(NextTimerHighlight.find(shown,true,now+21_000L).matches(
   "B",AllTimersWidgetSettings.PETS,now+30_000L));
  assertNull(NextTimerHighlight.find(shown,true,now+46_000L));
 }
 @Test public void simultaneousTimersHaveOneStableWinner(){
  long now=1_000L,finish=now+30_000L;
  ArrayList<AllTimersTestData.Account> shown=new ArrayList<>();
  shown.add(account("Zed",finish,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE));
  shown.add(account("Amy",Long.MAX_VALUE,finish,Long.MAX_VALUE,Long.MAX_VALUE));
  assertEquals("Amy",NextTimerHighlight.find(shown,false,now).account);
  Collections.reverse(shown);
  assertEquals("Amy",NextTimerHighlight.find(shown,false,now).account);
 }
}
