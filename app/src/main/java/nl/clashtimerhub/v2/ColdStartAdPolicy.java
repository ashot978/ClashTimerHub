package nl.clashtimerhub.v2;

/** Pure rules shared with unit tests. One opportunity per process, no wraparound on resume. */
final class ColdStartAdPolicy {
 private boolean claimed;
 synchronized boolean claim(boolean newInstance,boolean fromLauncher){
  if(claimed)return false;
  claimed=true;
  return newInstance&&fromLauncher;
 }
 static boolean eligible(int launcherStarts,boolean cachedPremium){
  return launcherStarts>=2&&!cachedPremium;
 }
 static boolean mayShow(boolean loading,boolean resumed,boolean consent,boolean ownershipKnown,
   boolean premium,boolean pendingPurchase,boolean displayed){
  return loading&&resumed&&consent&&ownershipKnown&&!premium&&!pendingPurchase&&!displayed;
 }
}
