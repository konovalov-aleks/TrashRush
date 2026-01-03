package dev.av.konovalov.trashrush;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

public class MainMenuView extends View {

    public interface MenuListener {
        void onContinueClicked();
        void onPlayClicked();
        void onExitClicked();
    }

    private MenuListener menuListener;
    private final Paint paint;
    private final Paint textPaint;
    private Bitmap backgroundBitmap;
    private Bitmap titleBitmap;
    private boolean showContinueButton = false;

    private Button continueButton;
    private Button playButton;
    private Button exitButton;

    private class Button {
        float x, y, width, height;
        String text;
        int color;

        Button(float x, float y, float width, float height, String text, int color) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.text = text;
            this.color = color;
        }

        boolean contains(float touchX, float touchY) {
            return touchX >= x && touchX <= x + width &&
                    touchY >= y && touchY <= y + height;
        }

        void draw(Canvas canvas, Paint paint, Paint textPaint) {
            paint.setColor(color);
            canvas.drawRoundRect(x, y, x + width, y + height, 20, 20, paint);

            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            canvas.drawRoundRect(x, y, x + width, y + height, 20, 20, paint);
            paint.setStyle(Paint.Style.FILL);

            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(height * 0.4f);
            canvas.drawText(text, x + width/2, y + height/2 + textPaint.getTextSize()/3, textPaint);
        }
    }

    public MainMenuView(Context context) {
        super(context);

        paint = new Paint();
        paint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(80);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setMenuListener(MenuListener listener) {
        this.menuListener = listener;
    }

    public void setContinueButtonVisibility(boolean show) {
        this.showContinueButton = show;
        if (getWidth() > 0 && getHeight() > 0) {
            onSizeChanged(getWidth(), getHeight(), getWidth(), getHeight());
        }
        invalidate();
    }

    private void loadGraphics(int screenWidth, int screenHeight) {
        titleBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.title);
        titleBitmap = Utility.fitBitmap(titleBitmap, screenWidth, screenHeight / 2);

        backgroundBitmap = createGradientBackground(screenWidth, screenHeight);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        float buttonWidth = w * 0.6f;
        float buttonHeight = h * 0.12f;

        if (showContinueButton) {
            float centerY = h * 0.5f;
            continueButton = new Button(
                    w/2 - buttonWidth/2,
                    centerY,
                    buttonWidth,
                    buttonHeight,
                    getContext().getString(R.string.menuContinue),
                    Color.parseColor("#2196F3")
            );
            playButton = new Button(
                    w/2 - buttonWidth/2,
                    centerY + buttonHeight * 1.3f,
                    buttonWidth,
                    buttonHeight,
                    getContext().getString(R.string.menuNewGame),
                    Color.parseColor("#4CAF50")
            );
            exitButton = new Button(
                    w/2 - buttonWidth/2,
                    centerY + buttonHeight * 2.6f,
                    buttonWidth,
                    buttonHeight,
                    getContext().getString(R.string.menuExit),
                    Color.parseColor("#F44336")
            );
        } else {
            float centerY = h * 0.6f;
            continueButton = null;
            playButton = new Button(
                    w/2 - buttonWidth/2,
                    centerY,
                    buttonWidth,
                    buttonHeight,
                    getContext().getString(R.string.menuPlay),
                    Color.parseColor("#4CAF50")
            );
            exitButton = new Button(
                    w/2 - buttonWidth/2,
                    centerY + buttonHeight * 1.3f,
                    buttonWidth,
                    buttonHeight,
                    getContext().getString(R.string.menuExit),
                    Color.parseColor("#F44336")
            );
        }

        loadGraphics(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (backgroundBitmap != null) {
            canvas.drawBitmap(backgroundBitmap, 0, 0, paint);
        }
        if (titleBitmap != null) {
            int x = (canvas.getWidth() - titleBitmap.getWidth()) / 2;
            canvas.drawBitmap(titleBitmap, x, 0, paint);
        }

        if (continueButton != null) {
            continueButton.draw(canvas, paint, textPaint);
        }
        if (playButton != null) {
            playButton.draw(canvas, paint, textPaint);
        }
        if (exitButton != null) {
            exitButton.draw(canvas, paint, textPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && menuListener != null) {
            float x = event.getX();
            float y = event.getY();

            if (continueButton != null && continueButton.contains(x, y)) {
                menuListener.onContinueClicked();
                return true;
            }

            if (playButton != null && playButton.contains(x, y)) {
                menuListener.onPlayClicked();
                return true;
            }

            if (exitButton != null && exitButton.contains(x, y)) {
                menuListener.onExitClicked();
                return true;
            }
        }
        return true;
    }

    private Bitmap createGradientBackground(int width, int height) {
        if (width <= 0 || height <= 0) {
            width = getResources().getDisplayMetrics().widthPixels;
            height = getResources().getDisplayMetrics().heightPixels;
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint p = new Paint();

        for (int i = 0; i < height; i++) {
            float ratio = (float) i / height;
            int blue = 100 + (int) (155 * ratio);
            int green = 50 + (int) (100 * ratio);
            int red = 30 + (int) (80 * ratio);

            p.setColor(Color.rgb(red, green, blue));
            canvas.drawRect(0, i, width, i + 1, p);
        }

        return bitmap;
    }
}