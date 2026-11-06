package nl.clashtimerhub.v2;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import java.util.ArrayList;

/** Multi-account summary only. The timeline is intentionally not part of this widget. */
public final class AllTimersTestService extends RemoteViewsService {
 @Override public RemoteViewsFactory onGetViewFactory(Intent intent){
  int id=intent==null?AppWidgetManager.INVALID_APPWIDGET_ID:
   intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
  return new Factory(getApplicationContext(),id);
 }
 private static final class Factory implements RemoteViewsFactory {
  private static final int GOLD=0xffffd783,GREEN=0xff8be3ad,
   LAVENDER=0xffcbb5d9,ICON=0xffd6bcea;
  private final Context ctx;
  private final int widgetId;
  private final ArrayList<AllTimersTestData.Account> accounts=new ArrayList<>();
  private int layoutMode=1; // 0: narrow, 1: two-line, 2: full-width single-line.
  Factory(Context ctx,int id){this.ctx=ctx;this.widgetId=id;}
  @Override public void onCreate(){onDataSetChanged();}
  @Override public void onDataSetChanged(){
   AllTimersTestData.Snapshot snapshot=AllTimersTestData.load(ctx);
   accounts.clear();accounts.addAll(snapshot.overview);
   int width=400;
   if(widgetId!=AppWidgetManager.INVALID_APPWIDGET_ID){
    android.os.Bundle options=AppWidgetManager.getInstance(ctx).getAppWidgetOptions(widgetId);
    if(options!=null){
     boolean landscape=ctx.getResources().getConfiguration().orientation==
      android.content.res.Configuration.ORIENTATION_LANDSCAPE;
     width=options.getInt(landscape?AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH:
      AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,400);
    }
   }
   layoutMode=width>=670?2:width>=420?1:0;
  }
  @Override public void onDestroy(){accounts.clear();}
  @Override public int getCount(){return accounts.size();}
  @Override public RemoteViews getViewAt(int position){
   if(position<0||position>=accounts.size())return null;
   AllTimersTestData.Account status=accounts.get(position);
   long now=System.currentTimeMillis();
   int layout=layoutMode==2?R.layout.widget_all_timers_account_wide:
    layoutMode==1?R.layout.widget_all_timers_account_compact:
     R.layout.widget_all_timers_account_narrow;
   RemoteViews rv=new RemoteViews(ctx.getPackageName(),layout);
   rv.setTextViewText(R.id.all_account_name,AppData.displayName(ctx,status.key));
   rv.setTextViewText(R.id.all_account_builders,status.ready+"✓ "+status.builders+"⚒");
   rv.setTextViewText(R.id.all_account_helpers,
    status.helpers.isEmpty()?"":"🤝 "+status.helpers);

   // The columns have one fixed order in ALL layouts:
   // three original icon+remaining-time columns to the LEFT;
   // three wall-clock finish columns together on the RIGHT.
   renderTimer(rv,R.id.all_account_builder_clock,R.id.all_account_builder_idle,
    R.id.all_account_builder_end,R.id.all_account_builder_icon,
    status.builderAt,status.ready,status.builderMax,status.builderBoost,"⚒",now);
   renderTimer(rv,R.id.all_account_lab_clock,R.id.all_account_lab_idle,
    R.id.all_account_lab_end,R.id.all_account_lab_icon,
    status.labAt,status.labReady,status.labMax,status.labBoost,"🧙",now);
   renderTimer(rv,R.id.all_account_pet_clock,R.id.all_account_pet_idle,
    R.id.all_account_pet_end,R.id.all_account_pet_icon,
    status.petAt,status.petReady,status.petMax,false,"🐾",now);
   rv.setOnClickFillInIntent(R.id.all_account_row,
    new Intent().putExtra("account",status.key));
   return rv;
  }
  private void renderTimer(RemoteViews rv,int clock,int idle,int finishView,int iconView,
   long finish,int ready,boolean maxed,boolean boosted,String icon,long now){
   boolean active=finish!=Long.MAX_VALUE&&finish>now;
   boolean checkedMax=!active&&maxed;
   boolean pendingSync=!active&&!maxed&&ready>0;
   rv.setViewVisibility(clock,active?View.VISIBLE:View.GONE);
   rv.setViewVisibility(idle,active?View.GONE:View.VISIBLE);
   rv.setTextViewText(idle,checkedMax?"✓":pendingSync?"!":"—");
   rv.setTextViewText(finishView,active?
    (layoutMode==2?AllTimersTestData.endTime(ctx,finish,now):
     AllTimersTestData.compactEnd(ctx,finish,now)):
    checkedMax?"MAX":pendingSync?(I18n.english(ctx)?"Ready":"Klaar"):"—");
   if(active){
    rv.setChronometer(clock,SystemClock.elapsedRealtime()+finish-now,null,true);
    rv.setChronometerCountDown(clock,true);
   }
   // Never replace the original category icon with a potion lightning symbol.
   rv.setTextViewText(iconView,icon);
   rv.setTextColor(iconView,ICON);
   rv.setTextColor(clock,active&&boosted?GREEN:GOLD);
   rv.setTextColor(idle,checkedMax?GREEN:pendingSync?GOLD:LAVENDER);
   rv.setTextColor(finishView,checkedMax?GREEN:pendingSync?GOLD:
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
