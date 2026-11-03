package nl.clashtimerhub.app;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.content.Intent;

/** Small movable one-tap scan trigger displayed over Clash of Clans. */
public final class ScanOverlay {
    private final WindowManager manager;
    private final TextView button;
    private final LinearLayout panel;
    private boolean expanded;
    private final WindowManager.LayoutParams params;
    private boolean visible;
    private final Context context;
    private final int category;

    public ScanOverlay(Context context, int category, Runnable onScan) {
        this.context=context;
        this.category=category;
        manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        button = new TextView(context);
        button.setText("☰ Timers");
        button.setTextColor(0xffffffff);
        button.setTextSize(14);
        button.setGravity(Gravity.CENTER);
        int padding = (int) (12 * context.getResources().getDisplayMetrics().density);
        button.setPadding(padding, padding, padding, padding);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xee177d57);
        bg.setCornerRadius(padding);
        button.setBackground(bg);
        panel=new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(8,8,8,8);
        GradientDrawable panelBg=new GradientDrawable();
        panelBg.setColor(0xf5223028);
        panelBg.setCornerRadius(18);
        panel.setBackground(panelBg);
        panel.addView(button);
        addAction("Scan upgrademenu",()->{ collapse(); onScan.run(); });
        addAction("Screenshot verwerken",()->openApp());
        addAction("Scangebied instellen",()->{
            collapse();
            Intent i=new Intent(context,ScanRegionActivity.class);
            i.putExtra("category",category);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        });
        addAction("Timers bekijken",()->openApp());
        collapse();
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= 26
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 20;
        params.y = 170;
        button.setOnTouchListener(new android.view.View.OnTouchListener() {
            float startX, startY;
            int initialX, initialY;
            boolean dragged;
            long pressedAt;
            @Override public boolean onTouch(android.view.View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        pressedAt=System.currentTimeMillis();
                        startX = event.getRawX();
                        startY = event.getRawY();
                        initialX = params.x;
                        initialY = params.y;
                        dragged = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = Math.round(event.getRawX() - startX);
                        int dy = Math.round(event.getRawY() - startY);
                        if (Math.abs(dx) + Math.abs(dy) > 12) dragged = true;
                        if (dragged && visible) {
                            params.x = Math.max(0, initialX + dx);
                            params.y = Math.max(0, initialY + dy);
                            manager.updateViewLayout(panel, params);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!dragged) {
                            if(expanded)collapse();else expand();
                        }
                        return true;
                    default: return true;
                }
            }
        });
    }

    private void addAction(String label,Runnable action){
        TextView item=new TextView(context);
        item.setText(label);
        item.setTextColor(0xffffffff);
        item.setTextSize(15);
        int pad=(int)(12*context.getResources().getDisplayMetrics().density);
        item.setPadding(pad,pad,pad,pad);
        item.setOnClickListener(v->action.run());
        panel.addView(item);
    }
    private void expand(){
        expanded=true;
        for(int i=1;i<panel.getChildCount();i++)panel.getChildAt(i).setVisibility(android.view.View.VISIBLE);
        button.setText("Sluiten");
        if(visible)manager.updateViewLayout(panel,params);
    }
    private void collapse(){
        expanded=false;
        for(int i=1;i<panel.getChildCount();i++)panel.getChildAt(i).setVisibility(android.view.View.GONE);
        button.setText("☰ Timers");
    }
    private void openApp(){
        collapse();
        Intent intent=new Intent(context,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }
    public void show() {
        if (!visible) {
            manager.addView(panel, params);
            visible = true;
        }
    }

    public void hide() {
        if (visible) {
            manager.removeView(panel);
            visible = false;
        }
    }

    public static boolean allowed(Context context) {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(context);
    }
}
