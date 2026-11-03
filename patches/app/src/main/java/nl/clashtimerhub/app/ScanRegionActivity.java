package nl.clashtimerhub.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Choose a screenshot, drag a rectangle around the builder count and first
 * running upgrade, and save the selected region as normalized coordinates.
 */
public final class ScanRegionActivity extends Activity {
    private static final int PICK_IMAGE = 41;
    private Bitmap screenshot;
    private Preview preview;
    private int category;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        category = getIntent().getIntExtra("category", 0);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16,16,16,16);
        TextView help = new TextView(this);
        help.setText("Kies een Clash-screenshot met het upgrademenu open. Sleep daarna een kader rond de builderstand en de bovenste actieve timer. Tik Opslaan.");
        layout.addView(help);
        Button pick = new Button(this);
        pick.setText("Kies screenshot");
        pick.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE);
        });
        layout.addView(pick);
        preview = new Preview();
        layout.addView(preview, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        Button save = new Button(this);
        save.setText("Scangebied opslaan");
        save.setOnClickListener(v -> {
            if (screenshot == null || preview.selection == null) {
                android.widget.Toast.makeText(this, "Selecteer eerst een screenshot en een gebied", 1).show();
                return;
            }
            try {
                ScanRegions.save(this, category, preview.selection,
                    screenshot.getWidth(), screenshot.getHeight());
                android.widget.Toast.makeText(this, "Scangebied opgeslagen",
                    android.widget.Toast.LENGTH_SHORT).show();
                finish();
            } catch (RuntimeException error) {
                android.util.Log.e("ClashTimerHub", "Saving scan region failed", error);
                android.widget.Toast.makeText(this,
                    "Opslaan mislukt: " + error.getClass().getSimpleName(),
                    android.widget.Toast.LENGTH_LONG).show();
            }
        });
        layout.addView(save);
        Button reset = new Button(this);
        reset.setText("Standaardgebied herstellen");
        reset.setOnClickListener(v -> {
            ScanRegions.reset(this, category);
            finish();
        });
        layout.addView(reset);
        setContentView(layout);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK_IMAGE || result != RESULT_OK || data == null) return;
        try (java.io.InputStream stream =
                getContentResolver().openInputStream(data.getData())) {
            Bitmap next = BitmapFactory.decodeStream(stream);
            if (next == null) throw new IllegalArgumentException("Geen afbeelding");
            if (screenshot != null) screenshot.recycle();
            screenshot = next;
            preview.selection = ScanRegions.get(this, category,
                next.getWidth(), next.getHeight());
            preview.invalidate();
        } catch (Exception error) {
            android.widget.Toast.makeText(this, "Screenshot niet leesbaar", 1).show();
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (screenshot != null) screenshot.recycle();
    }

    private final class Preview extends View {
        Rect selection;
        final Paint paint = new Paint(3);
        float scale, offsetX, offsetY;
        float downX, downY;
        Preview() { super(ScanRegionActivity.this); }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(Color.rgb(28, 32, 40));
            if (screenshot == null) return;
            scale = Math.min((float)getWidth()/screenshot.getWidth(),
                (float)getHeight()/screenshot.getHeight());
            offsetX = (getWidth()-screenshot.getWidth()*scale)/2f;
            offsetY = (getHeight()-screenshot.getHeight()*scale)/2f;
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawBitmap(screenshot, null,
                new android.graphics.RectF(offsetX,offsetY,
                    offsetX+screenshot.getWidth()*scale,
                    offsetY+screenshot.getHeight()*scale), paint);
            if (selection != null) {
                paint.setColor(Color.rgb(0,230,140));
                paint.setStrokeWidth(5);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawRect(offsetX+selection.left*scale,
                    offsetY+selection.top*scale,
                    offsetX+selection.right*scale,
                    offsetY+selection.bottom*scale,paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            if (screenshot == null || scale <= 0) return false;
            float x = Math.max(0,Math.min(screenshot.getWidth(),
                (event.getX()-offsetX)/scale));
            float y = Math.max(0,Math.min(screenshot.getHeight(),
                (event.getY()-offsetY)/scale));
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX=x; downY=y; return true;
                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_UP:
                    int left=Math.round(Math.min(downX,x));
                    int top=Math.round(Math.min(downY,y));
                    int right=Math.round(Math.max(downX,x));
                    int bottom=Math.round(Math.max(downY,y));
                    if (right-left >= 2 && bottom-top >= 2)
                        selection=new Rect(left,top,right,bottom);
                    invalidate();
                    return true;
                default:return true;
            }
        }
    }
}
