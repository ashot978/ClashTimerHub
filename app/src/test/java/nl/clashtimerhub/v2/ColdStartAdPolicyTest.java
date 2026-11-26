package nl.clashtimerhub.v2;

import org.junit.Test;
import static org.junit.Assert.*;

public class ColdStartAdPolicyTest {
 @Test public void directColdLauncherGetsOnlyOneOpportunity(){
  ColdStartAdPolicy p=new ColdStartAdPolicy();
  assertTrue(p.claim(true,true));
  assertFalse(p.claim(true,true));
  assertFalse(p.claim(false,true));
  assertTrue(new ColdStartAdPolicy().claim(true,true));
 }
 @Test public void widgetAndRecreatedActivitiesNeverStartAds(){
  ColdStartAdPolicy widget=new ColdStartAdPolicy();
  assertFalse(widget.claim(true,false));
  assertFalse(widget.claim(true,true));
  assertFalse(new ColdStartAdPolicy().claim(false,true));
 }
 @Test public void firstLaunchAndPremiumAreExempt(){
  assertFalse(ColdStartAdPolicy.eligible(1,false));
  assertTrue(ColdStartAdPolicy.eligible(2,false));
  assertFalse(ColdStartAdPolicy.eligible(10,true));
 }
 @Test public void displayOnlyDuringLoadingWithConsentAndVerifiedUnownedState(){
  assertTrue(ColdStartAdPolicy.mayShow(true,true,true,true,false,false,false));
  assertFalse(ColdStartAdPolicy.mayShow(false,true,true,true,false,false,false));
  assertFalse(ColdStartAdPolicy.mayShow(true,false,true,true,false,false,false));
  assertFalse(ColdStartAdPolicy.mayShow(true,true,false,true,false,false,false));
  assertFalse(ColdStartAdPolicy.mayShow(true,true,true,false,false,false,false));
  assertFalse(ColdStartAdPolicy.mayShow(true,true,true,true,true,false,false));
  assertFalse(ColdStartAdPolicy.mayShow(true,true,true,true,false,true,false));
  assertFalse(ColdStartAdPolicy.mayShow(true,true,true,true,false,false,true));
 }
}
