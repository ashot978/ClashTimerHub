package nl.clashtimerhub.v2;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** Regression tests for Next builder priority with ready helpers. */
public class AllTimersHelperPriorityTest {
 @Test public void oneFreeHelperIsActionable(){
  AllTimersTestData.Account account=new AllTimersTestData.Account("A");
  account.helperReady=1;
  assertEquals(1,AllTimersWidgetSettings.actionableCount(account,true));
 }
 @Test public void twoFreeHelpersOutrankOneFinishedUpgrade(){
  AllTimersTestData.Account helpers=new AllTimersTestData.Account("A");
  helpers.helperReady=2;
  AllTimersTestData.Account upgrade=new AllTimersTestData.Account("B");
  upgrade.ready=1;
  assertEquals(2,AllTimersWidgetSettings.actionableCount(helpers,true));
  assertEquals(1,AllTimersWidgetSettings.actionableCount(upgrade,true));
 }
 @Test public void bothUpgradeAndHelperAreAdded(){
  AllTimersTestData.Account account=new AllTimersTestData.Account("A");
  account.ready=1;
  account.labReady=1;
  account.petReady=1;
  account.helperReady=2;
  assertEquals(5,AllTimersWidgetSettings.actionableCount(account,true));
 }
 @Test public void hiddenHelpersDoNotAffectPriority(){
  AllTimersTestData.Account account=new AllTimersTestData.Account("A");
  account.helperReady=2;
  account.ready=1;
  assertEquals(1,AllTimersWidgetSettings.actionableCount(account,false));
 }
 @Test public void noReadyActionsRemainZero(){
  AllTimersTestData.Account account=new AllTimersTestData.Account("A");
  account.builders=7;
  account.helperNext=123456L;
  assertEquals(0,AllTimersWidgetSettings.actionableCount(account,true));
 }
}
