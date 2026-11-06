package nl.clashtimerhub.v2;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import java.util.ArrayList;

/** Scrollable widget collection: unlike adding child views, this can show ALL timers. */
public final class AllTimersTestService extends RemoteViewsService {
 @Override public RemoteViewsFactory onGetViewFactory(Intent intent){
  return new Factory(getApplicationContext());
 }
 private static final class Factory implements RemoteViewsFactory {
  private final Context ctx;
  private final ArrayList<AllTimersTestData.Item> entries=new ArrayList<>();
  Factory(Context ctx){this.ctx=ctx;}
  @Override public void onCreate(){onDataSetChanged();}
  @Override public void onDataSetChanged(){
   entries.clear();
   entries.addAll(AllTimersTestData.load(ctx).items);
  }
  @Override public void onDestroy(){entries.clear();}
  @Override public int getCount(){return entries.size();}
  @Override public RemoteViews getViewAt(int position){
   if(position<0||position>=entries.size())return null;
   AllTimersTestData.Item item=entries.get(position);
   long now=System.currentTimeMillis();
   RemoteViews rv=new RemoteViews(ctx.getPackageName(),R.layout.widget_all_timers_test_row);
   rv.setTextViewText(R.id.all_timer_account,AppData.displayName(ctx,item.account));
   rv.setTextViewText(R.id.all_timer_name,item.name);
   rv.setTextViewText(R.id.all_timer_category,AllTimersTestData.category(ctx,item));
   rv.setTextViewText(R.id.all_timer_end,AllTimersTestData.endTime(ctx,item.finish,now));
   rv.setViewVisibility(R.id.all_timer_clock,item.finish>now?View.VISIBLE:View.GONE);
   rv.setViewVisibility(R.id.all_timer_ready,item.finish>now?View.GONE:View.VISIBLE);
   if(item.finish>now){
    rv.setChronometer(R.id.all_timer_clock,SystemClock.elapsedRealtime()+(item.finish-now),null,true);
    rv.setChronometerCountDown(R.id.all_timer_clock,true);
   }
   rv.setTextViewText(R.id.all_timer_ready,I18n.english(ctx)?"✓ Ready":"✓ Klaar");
   String potion=item.boosted?("Bouwers".equals(item.category)?"10× ":"24× "):"";
   rv.setTextViewText(R.id.all_timer_boost,potion+(item.boosted?AllTimersTestData.saved(ctx,item.saved):""));
   rv.setTextColor(R.id.all_timer_end,item.boosted?0xff8be3ad:0xffffd783);
   rv.setTextColor(R.id.all_timer_boost,item.boosted?0xff8be3ad:0xffcdbbdc);
   // The host uses the widget's one PendingIntent template for all rows.
   Intent click=new Intent();
   click.putExtra("account",item.account);
   rv.setOnClickFillInIntent(R.id.all_timer_row,click);
   return rv;
  }
  @Override public RemoteViews getLoadingView(){return null;}
  @Override public int getViewTypeCount(){return 1;}
  @Override public long getItemId(int position){
   if(position<0||position>=entries.size())return position;
   AllTimersTestData.Item i=entries.get(position);
   return (i.account+":"+i.category+":"+i.id).hashCode();
  }
  @Override public boolean hasStableIds(){return true;}
 }
}
