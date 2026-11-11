package nl.clashtimerhub.v2;

import java.util.List;

/** Pick exactly one earliest active timer across visible accounts and categories. */
final class NextTimerHighlight {
 static final String HELPERS="helpers";
 private NextTimerHighlight(){}

 static final class Selection {
  final String account,category;
  final long finish;
  Selection(String account,String category,long finish){
   this.account=account;this.category=category;this.finish=finish;
  }
  boolean matches(String account,String category,long finish){
   return this.account.equals(account)&&this.category.equals(category)&&this.finish==finish;
  }
 }

 /** The same effective finish timestamps used by Timeline and the countdowns.
  * Finished/ready/MAX entries are not eligible. Helpers count only when shown.
  * A deterministic account/category tie-break prevents row sorting from changing
  * which of two simultaneous timers is highlighted.
  */
 static Selection find(List<AllTimersTestData.Account> shown,boolean showHelpers,long now){
  Selection earliest=null;
  for(AllTimersTestData.Account account:shown){
   if(showHelpers)
    earliest=choose(earliest,account.key,HELPERS,account.helperNext,now);
   earliest=choose(earliest,account.key,AllTimersWidgetSettings.LAB,account.labAt,now);
   earliest=choose(earliest,account.key,AllTimersWidgetSettings.PETS,account.petAt,now);
   earliest=choose(earliest,account.key,AllTimersWidgetSettings.BUILDERS,account.builderAt,now);
  }
  return earliest;
 }

 private static Selection choose(Selection current,String account,String category,long finish,long now){
  if(finish<=now||finish==Long.MAX_VALUE)return current;
  if(current==null||finish<current.finish)return new Selection(account,category,finish);
  if(finish>current.finish)return current;
  int byAccount=account.compareToIgnoreCase(current.account);
  if(byAccount<0||(byAccount==0&&category.compareTo(current.category)<0))
   return new Selection(account,category,finish);
  return current;
 }
}
