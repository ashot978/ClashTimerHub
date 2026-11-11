package nl.clashtimerhub.v2;

/** Short localized copy shared by individual upgrade alerts. Pure for unit tests. */
final class NotificationCopy {
 private NotificationCopy(){}

 static String title(String account,boolean early,boolean english){
  return account+(english?
   (early?" · Almost ready":" · Upgrade complete!"):
   (early?" · Bijna klaar":" · Upgrade klaar!"));
 }

 static String body(String upgrade,boolean early,boolean english,long remainingMs){
  if(!early)return upgrade+(english?" · Check in Clash":" · Controleer in Clash");
  long minutes=Math.max(1L,remainingMs>0L?(remainingMs+59999L)/60000L:1L);
  if(english)return upgrade+" · "+minutes+" min remaining";
  return upgrade+" · nog "+minutes+(minutes==1L?" minuut":" minuten");
 }
}
