package nl.clashtimerhub.v2;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import java.util.ArrayList;

/**
 * Single account-overview collection. Every category is one compact
 * icon + realtime countdown + wall-clock finish pair, with a visible
 * gap BETWEEN pairs. Per-widget order and sorting are independent.
 */
public final class AllTimersTestService extends RemoteViewsService {
 @Override public RemoteViewsFactory onGetViewFactory(Intent intent){
  int id=intent==null?AppWidgetManager.INVALID_APPWIDGET_ID:
   intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
  return new Factory(getApplicationContext(),id);
 }
 private static final class Factory implements RemoteViewsFactory {
  private static final int GOLD=0xffffd783,GREEN=0xff8be3ad,
   LAVENDER=0xffcbb5d9,ICON=0xffd6bcea;
  private static final int[] GROUPS={
   R.id.all_slot_0_group,R.id.all_slot_1_group,R.id.all_slot_2_group};
  private static final int[] ICONS={
   R.id.all_slot_0_icon,R.id.all_slot_1_icon,R.id.all_slot_2_icon};
  private static final int[] CLOCKS={
   R.id.all_slot_0_clock,R.id.all_slot_1_clock,R.id.all_slot_2_clock};
  private static final int[] IDLES={
   R.id.all_slot_0_idle,R.id.all_slot_1_idle,R.id.all_slot_2_idle};
  private static final int[] ENDS={
   R.id.all_slot_0_end,R.id.all_slot_1_end,R.id.all_slot_2_end};
  private final Context ctx;
  private final int widgetId;
  private final ArrayList<AllTimersTestData.Account> accounts=new ArrayList<>();
  private String[] columns={AllTimersWidgetSettings.LAB,AllTimersWidgetSettings.PETS,AllTimersWidgetSettings.BUILDERS};
  private int layoutMode=1; // 0 narrow, 1 compact, 2 wide.
  Factory(Context c,int id){ctx=c;widgetId=id;}
  @Override public void onCreate(){onDataSetChanged();}
  @Override public void onDataSetChanged(){
   AllTimersTestData.Snapshot snapshot=AllTimersTestData.load(ctx);
   accounts.clear();
   accounts.addAll(AllTimersWidgetSettings.visibleAccounts(ctx,widgetId,snapshot));
   columns=AllTimersWidgetSettings.order(ctx,widgetId);
   layoutMode=AllTimersTestWidget.layoutMode(ctx,widgetId);
  }
  @Override public void onDestroy(){accounts.clear();}
  @Override public int getCount(){return accounts.size();}
  @Override public RemoteViews getViewAt(int position){
   if(position<0||position>=accounts.size())return null;
   AllTimersTestData.Account a=accounts.get(position);
   long now=System.currentTimeMillis();
   int layout=layoutMode==2?R.layout.widget_all_timers_account_wide:
    layoutMode==1?R.layout.widget_all_timers_account_compact:
     R.layout.widget_all_timers_account_narrow;
   RemoteViews rv=new RemoteViews(ctx.getPackageName(),layout);
   rv.setTextViewText(R.id.all_account_name,AppData.displayName(ctx,a.key));
   rv.setTextViewText(R.id.all_account_builders,a.ready+"✓ "+a.builders+"⚒");
   boolean helpers=AllTimersWidgetSettings.preferences(ctx).getBoolean(
    AllTimersWidgetSettings.key("helpers",widgetId),true);
   rv.setTextViewText(R.id.all_account_helpers,
    helpers&&!a.helpers.isEmpty()?"🤝 "+a.helpers:"");
   for(int slot=0;slot<3;slot++){
    renderSlot(rv,slot,columns[slot],a,now);
    Intent intent=new Intent().putExtra("account",a.key);
    if(AllTimersWidgetSettings.BUILDERS.equals(columns[slot]))
     intent.putExtra("potionType","builders");
    else if(AllTimersWidgetSettings.LAB.equals(columns[slot]))
     intent.putExtra("potionType","research");
    rv.setOnClickFillInIntent(GROUPS[slot],intent);
   }
   rv.setOnClickFillInIntent(R.id.all_account_row,new Intent().putExtra("account",a.key));
   return rv;
  }
  private void renderSlot(RemoteViews rv,int slot,String category,AllTimersTestData.Account a,long now){
   long finish;
   int ready;
   boolean maxed,boosted;
   String icon;
   if(AllTimersWidgetSettings.BUILDERS.equals(category)){
    finish=a.builderAt;ready=a.ready;maxed=a.builderMax;boosted=a.builderBoost;icon="⚒";
   }else if(AllTimersWidgetSettings.PETS.equals(category)){
    finish=a.petAt;ready=a.petReady;maxed=a.petMax;boosted=false;icon="🐾";
   }else{
    finish=a.labAt;ready=a.labReady;maxed=a.labMax;boosted=a.labBoost;icon="🧙";
   }
   int clock=CLOCKS[slot],idle=IDLES[slot],end=ENDS[slot],marker=ICONS[slot];
   boolean active=finish!=Long.MAX_VALUE&&finish>now;
   boolean checkedMax=!active&&maxed;
   boolean pendingSync=!active&&!maxed&&ready>0;
   rv.setViewVisibility(clock,active?View.VISIBLE:View.GONE);
   rv.setViewVisibility(idle,active?View.GONE:View.VISIBLE);
   rv.setTextViewText(idle,checkedMax?"✓":pendingSync?"!":"—");
   rv.setTextViewText(end,active?(layoutMode==2?
    AllTimersTestData.endTime(ctx,finish,now):AllTimersTestData.compactEnd(ctx,finish,now)):
    checkedMax?"MAX":pendingSync?(I18n.english(ctx)?"Ready":"Klaar"):"—");
   if(active){
    // This is the predicted REAL completion time. Never speed the Chronometer
    // by 10x/24x a second time: that multiplier is already in estimateFinish.
    rv.setChronometer(clock,SystemClock.elapsedRealtime()+finish-now,null,true);
    rv.setChronometerCountDown(clock,true);
   }
   rv.setTextViewText(marker,icon); // Original category icon; no lightning glyph.
   rv.setTextColor(marker,ICON);
   rv.setTextColor(clock,active&&boosted?GREEN:GOLD);
   rv.setTextColor(idle,checkedMax?GREEN:pendingSync?GOLD:LAVENDER);
   rv.setTextColor(end,checkedMax?GREEN:pendingSync?GOLD:
    active&&boosted?GREEN:LAVENDER);
  }
  @Override public RemoteViews getLoadingView(){return null;}
  @Override public int getViewTypeCount(){return 3;}
  @Override public long getItemId(int position){
   return position<0||position>=accounts.size()?position:
    ("account:"+accounts.get(position).key).hashCode();
  }
  @Override public boolean hasStableIds(){return true;}
 }
}