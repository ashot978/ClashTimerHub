package nl.clashtimerhub.v2;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import java.util.ArrayList;

public final class AllTimersTestService extends RemoteViewsService {
 @Override public RemoteViewsFactory onGetViewFactory(Intent intent){
  int id=intent==null?AppWidgetManager.INVALID_APPWIDGET_ID:
   intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
  return new Factory(getApplicationContext(),id);
 }
 private static final class Factory implements RemoteViewsFactory {
  private static final int GOLD=0xffffd783,GREEN=0xff8be3ad,LAVENDER=0xffd6bcea;
  private final Context ctx;
  private final int widgetId;
  private final ArrayList<AllTimersTestData.Account> accounts=new ArrayList<>();
  private final ArrayList<AllTimersTestData.Item> entries=new ArrayList<>();
  private boolean wide=false,compact=true;
  Factory(Context c,int id){ctx=c;widgetId=id;}
  @Override public void onCreate(){onDataSetChanged();}
  @Override public void onDataSetChanged(){
   AllTimersTestData.Snapshot s=AllTimersTestData.load(ctx);
   accounts.clear();accounts.addAll(s.overview);
   entries.clear();entries.addAll(s.items);
   int width=380;
   if(widgetId!=AppWidgetManager.INVALID_APPWIDGET_ID){
    android.os.Bundle options=AppWidgetManager.getInstance(ctx).getAppWidgetOptions(widgetId);
    if(options!=null){
     boolean landscape=ctx.getResources().getConfiguration().orientation==
      android.content.res.Configuration.ORIENTATION_LANDSCAPE;
     width=options.getInt(landscape?AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH:
      AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,380);
    }
   }
   wide=width>=665;
   compact=width>=325;
  }
  @Override public void onDestroy(){accounts.clear();entries.clear();}
  @Override public int getCount(){return accounts.size()+1+entries.size();}
  @Override public RemoteViews getViewAt(int pos){
   if(pos<0||pos>=getCount())return null;
   if(pos<accounts.size())return accountView(accounts.get(pos));
   if(pos==accounts.size()){
    RemoteViews section=new RemoteViews(ctx.getPackageName(),R.layout.widget_all_timers_section);
    section.setTextViewText(R.id.all_timers_section_title,
     (I18n.english(ctx)?"ALL UPGRADES · ":"ALLE UPGRADES · ")+entries.size());
    return section;
   }
   return itemView(entries.get(pos-accounts.size()-1));
  }
  private void clock(RemoteViews rv,int clock,int idle,int end,int marker,
   long finish,int ready,boolean boosted,String normalIcon,int normalColor,long now){
   boolean active=finish!=Long.MAX_VALUE&&finish>now;
   rv.setViewVisibility(clock,active?View.VISIBLE:View.GONE);
   rv.setViewVisibility(idle,active?View.GONE:View.VISIBLE);
   rv.setTextViewText(idle,ready>0?"✓":"—");
   if(active){
    rv.setChronometer(clock,SystemClock.elapsedRealtime()+finish-now,null,true);
    rv.setChronometerCountDown(clock,true);
   }
   rv.setTextViewText(end,active?(wide?AllTimersTestData.endTime(ctx,finish,now):
    AllTimersTestData.compactEnd(ctx,finish,now)):ready>0?"✓":"—");
   rv.setTextViewText(marker,boosted?"⚡":normalIcon);
   int color=boosted?GREEN:normalColor;
   rv.setTextColor(clock,color);rv.setTextColor(end,ready>0?GREEN:color);
   rv.setTextColor(marker,color);rv.setTextColor(idle,ready>0?GREEN:color);
  }
  private RemoteViews accountView(AllTimersTestData.Account s){
   long now=System.currentTimeMillis();
   RemoteViews rv=new RemoteViews(ctx.getPackageName(),
    wide?R.layout.widget_all_timers_account_wide:
     compact?R.layout.widget_all_timers_account_compact:
      R.layout.widget_all_timers_account_narrow);
   rv.setTextViewText(R.id.all_account_name,AppData.displayName(ctx,s.key));
   rv.setTextViewText(R.id.all_account_builders,s.ready+"✓ "+s.builders+"⚒");
   rv.setTextViewText(R.id.all_account_helpers,s.helpers.isEmpty()?"":"🤝 "+s.helpers);
   clock(rv,R.id.all_account_builder_clock,R.id.all_account_builder_idle,
    R.id.all_account_builder_end,R.id.all_account_builder_icon,
    s.builderAt,s.ready,s.builderBoost,"⚒",GOLD,now);
   clock(rv,R.id.all_account_lab_clock,R.id.all_account_lab_idle,
    R.id.all_account_lab_end,R.id.all_account_lab_icon,
    s.labAt,s.labReady,s.labBoost,"🧙",LAVENDER,now);
   clock(rv,R.id.all_account_pet_clock,R.id.all_account_pet_idle,
    R.id.all_account_pet_end,R.id.all_account_pet_icon,
    s.petAt,s.petReady,false,"🐾",LAVENDER,now);
   rv.setOnClickFillInIntent(R.id.all_account_row,new Intent().putExtra("account",s.key));
   return rv;
  }
  private RemoteViews itemView(AllTimersTestData.Item item){
   long now=System.currentTimeMillis();
   RemoteViews rv=new RemoteViews(ctx.getPackageName(),R.layout.widget_all_timers_test_row);
   rv.setTextViewText(R.id.all_timer_account,AppData.displayName(ctx,item.account));
   rv.setTextViewText(R.id.all_timer_name,item.name);
   rv.setTextViewText(R.id.all_timer_category,AllTimersTestData.category(ctx,item));
   rv.setTextViewText(R.id.all_timer_end,AllTimersTestData.endTime(ctx,item.finish,now));
   rv.setViewVisibility(R.id.all_timer_clock,item.finish>now?View.VISIBLE:View.GONE);
   rv.setViewVisibility(R.id.all_timer_ready,item.finish>now?View.GONE:View.VISIBLE);
   if(item.finish>now){
    rv.setChronometer(R.id.all_timer_clock,SystemClock.elapsedRealtime()+item.finish-now,null,true);
    rv.setChronometerCountDown(R.id.all_timer_clock,true);
   }
   rv.setTextViewText(R.id.all_timer_ready,I18n.english(ctx)?"✓ Ready":"✓ Klaar");
   rv.setTextViewText(R.id.all_timer_boost,item.boosted?
    "⚡ "+("Bouwers".equals(item.category)?"10× ":"24× ")+AllTimersTestData.saved(ctx,item.saved):
    (I18n.english(ctx)?"Normal":"Normaal"));
   rv.setTextColor(R.id.all_timer_end,item.boosted?GREEN:GOLD);
   rv.setTextColor(R.id.all_timer_clock,item.boosted?GREEN:GOLD);
   rv.setTextColor(R.id.all_timer_boost,item.boosted?GREEN:LAVENDER);
   rv.setOnClickFillInIntent(R.id.all_timer_row,new Intent().putExtra("account",item.account));
   return rv;
  }
  @Override public RemoteViews getLoadingView(){return null;}
  @Override public int getViewTypeCount(){return 5;}
  @Override public long getItemId(int pos){
   if(pos<0||pos>=getCount())return pos;
   if(pos<accounts.size())return ("account:"+accounts.get(pos).key).hashCode();
   if(pos==accounts.size())return -1;
   AllTimersTestData.Item s=entries.get(pos-accounts.size()-1);
   return ("timer:"+s.account+":"+s.category+":"+s.id).hashCode();
  }
  @Override public boolean hasStableIds(){return true;}
 }
}
