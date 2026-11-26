package nl.clashtimerhub.v2;

/** Shared, Android-independent rules for the three visible navigation tabs. */
final class MenuSwipe {
 static final int UNDECIDED=0,HORIZONTAL=1,VERTICAL=-1;
 private static final int[] TABS={0,1,3};

 private MenuSwipe(){}

 static int axis(float dx,float dy,float slop){
  float x=Math.abs(dx),y=Math.abs(dy);
  if(x<slop&&y<slop)return UNDECIDED;
  return x>=slop&&x>=1.5f*y?HORIZONTAL:VERTICAL;
 }

 /** A left swipe advances, a right swipe goes back. */
 static int direction(float dx,float dy,float minimum){
  return axis(dx,dy,minimum)==HORIZONTAL?(dx<0?1:-1):0;
 }

 static int targetTab(int current,int direction){
  if(direction!=1&&direction!=-1)return current;
  for(int i=0;i<TABS.length;i++){
   if(TABS[i]!=current)continue;
   int target=i+direction;
   return target>=0&&target<TABS.length?TABS[target]:current;
  }
  return current;
 }
}
