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

/** Small movable one-tap scan trigger displayed over Clash of Clans. */
public final class ScanOverlay {
    private final WindowManager manager;
    private final TextView button;
    private final WindowManager.LayoutParams params;
    private boolean visible;

    public ScanOverlay(Context context, Runnable onScan) {
        manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        button = new TextView(context);
        button.setText("Scan nu");
        button.setTextColor(0xffffffff);
        button.setTextSize(14);
        button.setGravity(Gravity.CENTER);
        int padding = (int) (12 * context.getResources().getDisplayMetrics().density);
        button.setPadding(padding, padding, padding, padding);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xee177d57);
        bg.setCornerRadius(padding);
        button.setBackground(bg);
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
            @Override public boolean onTouch(android.view.View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
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
                            manager.updateViewLayout(button, params);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!dragged) onScan.run();
                        return true;
                    default: return true;
                }
            }
        });
    }

    public void show() {
        if (!visible) {
            manager.addView(button, params);
            visible = true;
        }
    }

    public void hide() {
        if (visible) {
            manager.removeView(button);
            visible = false;
        }
    }

    public static boolean allowed(Context context) {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(context);
    }
}
