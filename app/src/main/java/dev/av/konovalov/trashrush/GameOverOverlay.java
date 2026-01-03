package dev.av.konovalov.trashrush;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

public class GameOverOverlay extends View {

    private final Paint paint;
    private final Paint textPaint;
    int screenWidth = 0;
    int screenHeight = 0;
    private GameOverListener gameOverListener;
    private Button restartButton;
    private Button menuButton;
    private int itemsSortedTotal;
    private float totalTreesSaved;
    private float totalWaterSaved;
    private float totalCo2Saved;

    public GameOverOverlay(Context context) {
        super(context);

        paint = new Paint();
        paint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setGameOverListener(GameOverListener listener) {
        this.gameOverListener = listener;
    }

    public void updateStats(int itemsSorted, float treesSaved, float waterSaved, float co2Saved) {
        this.itemsSortedTotal = itemsSorted;
        this.totalTreesSaved = treesSaved;
        this.totalWaterSaved = waterSaved;
        this.totalCo2Saved = co2Saved;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        screenWidth = w;
        screenHeight = h;

        float buttonWidth = w * 0.35f;
        float buttonHeight = h * 0.1f;
        float buttonY = h * 0.75f;
        float spacing = w * 0.02f;

        restartButton = new Button(w / 2 - buttonWidth - spacing / 2, buttonY, buttonWidth, buttonHeight, getContext().getString(R.string.gameOverMenuPlayAgain), Color.parseColor("#4CAF50"));
        menuButton = new Button(w / 2 + spacing / 2, buttonY, buttonWidth, buttonHeight, getContext().getString(R.string.gameOverMenuBackToMenu), Color.parseColor("#2196F3"));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paint.setColor(Color.argb(180, 0, 0, 0));
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);

        // title
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(80);
        canvas.drawText("GAME OVER", screenWidth / 2, screenHeight / 2 - 200, textPaint);

        // statistics
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40);
        float y = screenHeight / 2 - 100;
        canvas.drawText(String.format(getContext().getString(R.string.msgNSorted), itemsSortedTotal), screenWidth / 2, y, textPaint);
        y += 70;
        canvas.drawText(getContext().getString(R.string.statYouSaved), screenWidth / 2, y, textPaint);

        textPaint.setTextAlign(Paint.Align.LEFT);
        {
            final float x = screenWidth / 2 - 130;
            y += 50;
            canvas.drawText(String.format(getContext().getString(R.string.statNTrees), (int) totalTreesSaved), x, y, textPaint);
            y += 50;
            canvas.drawText(String.format(getContext().getString(R.string.statLitersOfWater), (int) totalWaterSaved), x, y, textPaint);
            y += 50;
            canvas.drawText(String.format(getContext().getString(R.string.statCO2), (int) totalCo2Saved), x, y, textPaint);
            textPaint.setTextAlign(Paint.Align.CENTER);
        }

        if (restartButton != null) {
            restartButton.draw(canvas, paint, textPaint);
        }
        if (menuButton != null) {
            menuButton.draw(canvas, paint, textPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && gameOverListener != null) {
            float x = event.getX();
            float y = event.getY();

            if (restartButton != null && restartButton.contains(x, y)) {
                gameOverListener.onRestartClicked();
                return true;
            }

            if (menuButton != null && menuButton.contains(x, y)) {
                gameOverListener.onMenuClicked();
                return true;
            }
        }
        return true;
    }

    public interface GameOverListener {
        void onRestartClicked();

        void onMenuClicked();
    }

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
            return touchX >= x && touchX <= x + width && touchY >= y && touchY <= y + height;
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
            textPaint.setTextSize(height * 0.35f);
            canvas.drawText(text, x + width / 2, y + height / 2 + textPaint.getTextSize() / 3, textPaint);
        }
    }
}