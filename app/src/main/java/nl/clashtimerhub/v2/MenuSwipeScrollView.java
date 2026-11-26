package nl.clashtimerhub.v2;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.ScrollView;

/** Adds horizontal menu gestures without stealing vertical scrolling or taps. */
final class MenuSwipeScrollView extends ScrollView {
 interface Listener { void onSwipe(int direction); }
 private final Listener listener;
 private final float slop,minimum,edge;
 private float downX,downY;
 private boolean tracking,captured,childDisallows;

 MenuSwipeScrollView(Context context,Listener listener){
  super(context);
  this.listener=listener;
  float density=getResources().getDisplayMetrics().density;
  slop=Math.max(ViewConfiguration.get(context).getScaledTouchSlop(),12*density);
  minimum=72*density;
  edge=24*density;
 }

 @Override public void requestDisallowInterceptTouchEvent(boolean disallowIntercept){
  childDisallows=disallowIntercept;
  super.requestDisallowInterceptTouchEvent(disallowIntercept);
 }

 @Override public boolean dispatchTouchEvent(MotionEvent event){
  int action=event.getActionMasked();
  if(action==MotionEvent.ACTION_DOWN){
   downX=event.getX();downY=event.getY();
   captured=false;childDisallows=false;
   // Leave the Android back gesture at both screen edges alone.
   tracking=downX>=edge&&downX<=getWidth()-edge;
   boolean handled=super.dispatchTouchEvent(event);
   return handled||tracking;
  }
  if(action==MotionEvent.ACTION_POINTER_DOWN||event.getPointerCount()>1){
   // A two-finger gesture is not a navigation swipe.
   tracking=false;
   return captured||super.dispatchTouchEvent(event);
  }
  if(action==MotionEvent.ACTION_MOVE&&tracking&&!captured){
   if(childDisallows){
    tracking=false;
   }else{
    int axis=MenuSwipe.axis(event.getX()-downX,event.getY()-downY,slop);
    if(axis==MenuSwipe.VERTICAL){
     tracking=false;
    }else if(axis==MenuSwipe.HORIZONTAL){
     // Cancel the original child gesture so a swipe never clicks a timer/card.
     MotionEvent cancel=MotionEvent.obtain(event);
     cancel.setAction(MotionEvent.ACTION_CANCEL);
     super.dispatchTouchEvent(cancel);
     cancel.recycle();
     captured=true;
    }
   }
  }
  if(captured){
   if(action==MotionEvent.ACTION_UP){
    int direction=tracking?MenuSwipe.direction(event.getX()-downX,event.getY()-downY,minimum):0;
    captured=false;tracking=false;
    if(direction!=0)post(()->{if(isAttachedToWindow())listener.onSwipe(direction);});
   }else if(action==MotionEvent.ACTION_CANCEL){
    captured=false;tracking=false;
   }
   return true;
  }
  if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)tracking=false;
  return super.dispatchTouchEvent(event);
 }
}
