package nl.clashtimerhub.v2;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class MenuSwipeTest {
 @Test public void leftSwipeAdvancesThroughVisibleTabs(){
  assertEquals(1,MenuSwipe.direction(-100,10,72));
  assertEquals(1,MenuSwipe.targetTab(0,1));
  assertEquals(3,MenuSwipe.targetTab(1,1));
  assertEquals(3,MenuSwipe.targetTab(3,1));
 }

 @Test public void rightSwipeReturnsWithoutWrapping(){
  assertEquals(-1,MenuSwipe.direction(100,-10,72));
  assertEquals(1,MenuSwipe.targetTab(3,-1));
  assertEquals(0,MenuSwipe.targetTab(1,-1));
  assertEquals(0,MenuSwipe.targetTab(0,-1));
 }

 @Test public void tapsAndVerticalOrDiagonalGesturesDoNotNavigate(){
  assertEquals(MenuSwipe.UNDECIDED,MenuSwipe.axis(3,5,12));
  assertEquals(MenuSwipe.VERTICAL,MenuSwipe.axis(30,90,12));
  assertEquals(0,MenuSwipe.direction(100,100,72));
  assertEquals(0,MenuSwipe.direction(-50,5,72));
  assertEquals(0,MenuSwipe.direction(10,120,72));
 }

 @Test public void hiddenTabAndInvalidDirectionDoNotNavigate(){
  assertEquals(2,MenuSwipe.targetTab(2,1));
  assertEquals(1,MenuSwipe.targetTab(1,0));
  assertEquals(1,MenuSwipe.targetTab(1,2));
 }
}
