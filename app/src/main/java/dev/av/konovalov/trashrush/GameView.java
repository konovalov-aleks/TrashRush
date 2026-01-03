package dev.av.konovalov.trashrush;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "TrashRush";

    private static final int INITIAL_MONEY = 10;
    private static final float INITIAL_CONVEYOR_SPEED = 120.0f;
    private static final float INITIAL_SPAWN_INTERVAL = 2.0f;

    private static final int BELT_HEIGHT = 450;
    private final int MAX_TRASH_ITEMS = 100;

    private final SurfaceHolder holder;
    private final SoundManager soundManager;

    private final List<TrashItem> trashItems = new ArrayList<>();
    private final List<Bin> bins = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();

    private final HashMap<TrashType, Bitmap[]> trashBitmaps = new HashMap<>();
    private final Random random = new Random();
    private final HashMap<TrashType, Integer> sortedStats = new HashMap<>();
    private final List<GameMessage> messages = new ArrayList<>();
    private GameThread gameThread;
    private Bitmap backgroundBitmap;
    private Bitmap conveyorBeltBitmap;
    private Bitmap conveyorTopBitmap;
    private Bitmap conveyorBottomBitmap;
    private float beltScrollOffset = 0;

    private int money = INITIAL_MONEY;
    private float gameTime = 0;
    private boolean gameActive = false;
    private int level = 1;
    private int itemsSortedTotal = 0;
    private float totalCo2Saved = 0;
    private float totalTreesSaved = 0;
    private float totalWaterSaved = 0;

    private float spawnTimer = 0;
    private float spawnInterval = INITIAL_SPAWN_INTERVAL;
    private float conveyorSpeed = INITIAL_CONVEYOR_SPEED;

    private Paint paint;
    private Paint textPaint;
    private Paint smallTextPaint;
    private float screenWidth = 0, screenHeight = 0;
    private final int TRASH_SIZE = getOptimalTrashSize();

    private float uiZoneHeight; // the header zone
    private float conveyorZoneHeight;
    private float binZoneHeight;

    private TrashItem draggedItem = null;
    private float dragOffsetX, dragOffsetY;

    private float screenShakeTime = 0;
    private float shakeIntensity = 0;

    private GameEventListener gameEventListener;

    public GameView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);

        soundManager = new SoundManager(context);
        initBasicSettings();
    }

    public void setGameEventListener(GameEventListener listener) {
        this.gameEventListener = listener;
    }

    private void initBasicSettings() {
        paint = new Paint();
        paint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);

        smallTextPaint = new Paint();
        smallTextPaint.setColor(Color.WHITE);
        smallTextPaint.setTextSize(24);
        smallTextPaint.setAntiAlias(true);

        for (TrashType type : TrashType.values()) {
            sortedStats.put(type, 0);
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((MainActivity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;

        Log.d(TAG, "Screen size: " + screenWidth + "x" + screenHeight);

        uiZoneHeight = screenHeight * 0.15f;
        conveyorZoneHeight = screenHeight * 0.7f;
        binZoneHeight = screenHeight - uiZoneHeight - conveyorZoneHeight;

        initGameObjects();

        gameThread = new GameThread(holder, this);
        gameThread.start();
    }

    private void initGameObjects() {
        loadGraphics();
        createBins();
    }

    private Bitmap[] loadTrashBitmaps(TrashType type) {
        Bitmap[] bitmaps;

        switch (type) {
            case PLASTIC:
                bitmaps = new Bitmap[3];
                bitmaps[0] = loadAndScale(R.drawable.plastic_bottle1, TRASH_SIZE);
                bitmaps[1] = loadAndScale(R.drawable.plastic_bottle2, TRASH_SIZE);
                bitmaps[2] = loadAndScale(R.drawable.plastic3, TRASH_SIZE);
                break;
            case PAPER:
                bitmaps = new Bitmap[2];
                bitmaps[0] = loadAndScale(R.drawable.paper1, TRASH_SIZE);
                bitmaps[1] = loadAndScale(R.drawable.paper2, TRASH_SIZE);
                break;
            case GLASS:
                bitmaps = new Bitmap[2];
                bitmaps[0] = loadAndScale(R.drawable.glass1, TRASH_SIZE);
                bitmaps[1] = loadAndScale(R.drawable.glass2, TRASH_SIZE);
                break;
            case METAL:
                bitmaps = new Bitmap[2];
                bitmaps[0] = loadAndScale(R.drawable.metal_can, TRASH_SIZE);
                bitmaps[1] = loadAndScale(R.drawable.metal2, TRASH_SIZE);
                break;
            case BATTERY:
                bitmaps = new Bitmap[2];
                bitmaps[0] = loadAndScale(R.drawable.battery1, TRASH_SIZE);
                bitmaps[1] = loadAndScale(R.drawable.battery2, TRASH_SIZE);
                break;
            default:
                throw new RuntimeException("Unknown type");
        }
        return bitmaps;
    }

    private Bitmap loadAndScale(int resId, int size) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
        return Bitmap.createScaledBitmap(bitmap, size, size, true);
    }

    private void loadGraphics() {
        for (TrashType type : TrashType.values()) {
            trashBitmaps.put(type, loadTrashBitmaps(type));
        }

        conveyorBeltBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.conveyor_belt);
        conveyorBeltBitmap = Utility.scaleBitmapToHeight(conveyorBeltBitmap, BELT_HEIGHT);

        conveyorTopBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.conveyor_top);
        conveyorTopBitmap = Utility.scaleBitmapToWidth(conveyorTopBitmap, (int) screenWidth);

        conveyorBottomBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.conveyor_bottom);
        conveyorBottomBitmap = Utility.scaleBitmapToWidth(conveyorBottomBitmap, (int) screenWidth);

        backgroundBitmap = createGradientBackground((int) screenWidth, (int) screenHeight);
    }

    private void createBins() {
        bins.clear();
        int height = (int) (binZoneHeight + 40);
        float binY = uiZoneHeight + conveyorZoneHeight - 50;
        float binX = 0;
        for (TrashType type : TrashType.values()) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), type.trashBinResource);
            bitmap = Utility.scaleBitmapToHeight(bitmap, height);
            Bin bin = new Bin(binX, binY, bitmap, type);
            bins.add(bin);
            binX += bitmap.getWidth() * 1.3f;
        }
        setupBinsForLevel();
    }

    private void setupBinsForLevel() {
        final float padding = 0.3f;
        float totalVisibleWidth = 0;
        for (Bin bin : bins) {
            if (bin.acceptedType.unlockLevel <= level) {
                bin.show();
                totalVisibleWidth += bin.bitmap.getWidth() * (1.0f + padding);
            } else bin.hide();
        }
        float x = (screenWidth - totalVisibleWidth) / 2.0f;
        for (Bin bin : bins) {
            bin.x = x;
            x += bin.bitmap.getWidth() * (1.0f + padding);
        }
    }

    private TrashType[] getAvailableTrashTypes() {
        List<TrashType> available = new ArrayList<>();
        for (TrashType type : TrashType.values()) {
            if (type.unlockLevel <= level) {
                available.add(type);
            }
        }
        return available.toArray(new TrashType[0]);
    }

    public void update(float deltaTime) {
        if (screenShakeTime > 0) {
            screenShakeTime -= deltaTime;
            shakeIntensity = screenShakeTime * 20;
        }

        if (!gameActive) return;

        gameTime += deltaTime;
        updateDifficulty();
        updateConveyorBelt(deltaTime);

        spawnTimer += deltaTime;
        if (spawnTimer >= spawnInterval) {
            spawnTrash();
            spawnTimer = 0;
        }

        synchronized (trashItems) {
            for (int i = trashItems.size() - 1; i >= 0; i--) {
                TrashItem item = trashItems.get(i);

                if (!item.isDragging) {
                    item.update(conveyorSpeed, deltaTime);

                    if (item.x > screenWidth) {
                        removeTrashItem(item);
                        handleMissedTrash(item);
                        continue;
                    }

                    for (Bin bin : bins) {
                        if (item.collidesWith(bin)) {
                            handleTrashInBin(item, bin);
                            removeTrashItem(item);
                            break;
                        }
                    }
                }
            }
        }

        synchronized (particles) {
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle particle = particles.get(i);
                particle.update(deltaTime);
                if (!particle.isAlive()) {
                    particles.remove(i);
                }
            }
        }

        synchronized (messages) {
            for (int i = messages.size() - 1; i >= 0; i--) {
                GameMessage msg = messages.get(i);
                msg.lifeTime -= deltaTime;
                msg.y -= 50 * deltaTime;
                if (msg.lifeTime <= 0) {
                    messages.remove(i);
                }
            }
        }
    }

    private void updateConveyorBelt(float deltaTime) {
        beltScrollOffset += conveyorSpeed * deltaTime;

        if (conveyorBeltBitmap != null) {
            float originalRatio = (float) conveyorBeltBitmap.getWidth() / conveyorBeltBitmap.getHeight();
            float tileWidth = BELT_HEIGHT * originalRatio;
            if (beltScrollOffset >= tileWidth) {
                beltScrollOffset -= tileWidth;
            }
        }
    }

    private void updateDifficulty() {
        int difficultyLevel = (int) (gameTime / 30) + 1;

        if (difficultyLevel > level) {
            level = difficultyLevel;
            spawnInterval = Math.max(0.5f, INITIAL_SPAWN_INTERVAL - level * 0.5f);
            conveyorSpeed = INITIAL_CONVEYOR_SPEED + level * level * 4.0f;

            setupBinsForLevel();

            soundManager.playSound(SoundManager.SOUND_LEVEL_UP);
            addMessage(String.format(getContext().getString(R.string.msgLevel), level), Color.YELLOW, screenWidth / 2, screenHeight / 2);
        }
    }

    private void spawnTrash() {
        if (trashItems.size() > MAX_TRASH_ITEMS) return;

        TrashType[] availableTypes = getAvailableTrashTypes();
        if (availableTypes.length == 0) return;

        TrashType type = availableTypes[random.nextInt(availableTypes.length)];
        Bitmap[] bitmaps = trashBitmaps.get(type);
        if (bitmaps == null || bitmaps.length == 0) return;

        float beltY = getBeltY();

        float minY = beltY - TRASH_SIZE * 2 / 3;
        float maxY = beltY + BELT_HEIGHT - TRASH_SIZE;

        float startX = -TRASH_SIZE * 2;
        float startYPosition = minY + random.nextFloat() * (maxY - minY);

        Bitmap bitmap = bitmaps[random.nextInt(bitmaps.length)];
        TrashItem item = new TrashItem(startX, startYPosition, bitmap, type);

        item.width = TRASH_SIZE;
        item.height = TRASH_SIZE;

        synchronized (trashItems) {
            trashItems.add(item);
            // sort for correct drawing order
            trashItems.sort(Comparator.comparing(TrashItem::getY));
        }
    }

    private void handleTrashInBin(TrashItem item, Bin bin) {
        TrashType type = item.type;
        float x = item.x;
        float y = item.y;

        if (bin.accepts(item)) {
            // the item was placed into correct bin

            money += type.reward;
            itemsSortedTotal++;
            sortedStats.put(type, sortedStats.get(type) + 1);

            totalCo2Saved += type.co2Saved;
            totalTreesSaved += type.treesSaved;
            totalWaterSaved += type.waterSaved;

            createParticleEffect(x, y, type.getColor(), 20, 1.0f);
            soundManager.playSound(SoundManager.SOUND_CORRECT);
            addMessage("+" + type.reward, Color.GREEN, x, y);

            bin.itemsSorted++;
        } else {
            // wrong sort

            createParticleEffect(x, y, Color.RED, 30, 1.5f);
            screenShakeTime = 0.3f;
            shakeIntensity = 10;
            soundManager.playSound(SoundManager.SOUND_WRONG);
            addMessage(Integer.toString(type.penalty), Color.RED, x, y);

            addPenalty(type.penalty);
        }
    }

    private void addPenalty(int penalty) {
        assert (penalty < 0);
        money += penalty;
        if (money < 0) {
            gameOver();
        }
    }

    private void handleMissedTrash(TrashItem item) {
        soundManager.playSound(SoundManager.SOUND_MISS);
        addMessage(String.format(getContext().getString(R.string.msgMissed), item.type.penalty), Color.RED, screenWidth - 200, item.y);
        addPenalty(item.type.penalty);
    }

    private void createParticleEffect(float x, float y, int color, int count, float speed) {
        synchronized (particles) {
            for (int i = 0; i < count; i++) {
                float angle = random.nextFloat() * (float) Math.PI * 2;
                float velocity = speed * (0.5f + random.nextFloat());
                float vx = (float) Math.cos(angle) * velocity;
                float vy = (float) Math.sin(angle) * velocity;

                Particle particle = new Particle(x, y, vx, vy, color);
                particle.maxLifeTime = 0.5f + random.nextFloat() * 0.5f;
                particle.lifeTime = particle.maxLifeTime;
                particles.add(particle);
            }
        }
    }

    private void gameOver() {
        gameActive = false;
        soundManager.playSound(SoundManager.SOUND_GAME_OVER);
        screenShakeTime = 0.5f;
        shakeIntensity = 20;

        if (gameEventListener != null) {
            gameEventListener.onGameOver(itemsSortedTotal, totalTreesSaved, totalWaterSaved, totalCo2Saved);
        }
    }

    private void addMessage(String text, int color, float x, float y) {
        synchronized (messages) {
            messages.add(new GameMessage(text, color, x, y));
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas == null) return;

        // screen shaking effect
        float shakeX = 0, shakeY = 0;
        if (screenShakeTime > 0) {
            shakeX = (random.nextFloat() - 0.5f) * 2 * shakeIntensity;
            shakeY = (random.nextFloat() - 0.5f) * 2 * shakeIntensity;
        }
        canvas.translate(shakeX, shakeY);

        canvas.drawBitmap(backgroundBitmap, 0, 0, paint);

        drawConveyorSystem(canvas);

        for (Bin bin : bins) {
            if (!bin.isVisible()) continue;

            canvas.drawBitmap(bin.bitmap, bin.x, bin.y, paint);
            smallTextPaint.setColor(Color.WHITE);
            canvas.drawText(bin.itemsSorted + "", bin.x + bin.width / 2 - 10, bin.y - 10, smallTextPaint);
        }

        synchronized (trashItems) {
            for (TrashItem item : trashItems) {
                if (item == draggedItem) continue;
                canvas.drawBitmap(item.bitmap, item.x, item.y, paint);
            }

            // draw dragged item on top of all
            if (draggedItem != null) {
                canvas.drawBitmap(draggedItem.bitmap, draggedItem.x, draggedItem.y, paint);
                Paint highlight = new Paint();
                highlight.setColor(Color.YELLOW);
                highlight.setStyle(Paint.Style.STROKE);
                highlight.setStrokeWidth(5);
                highlight.setAlpha(150);
                canvas.drawCircle(draggedItem.x + draggedItem.width / 2, draggedItem.y + draggedItem.height / 2, draggedItem.width / 2 + 10, highlight);
            }
        }

        synchronized (particles) {
            for (Particle particle : particles) {
                paint.setColor(particle.getAlphaColor());
                canvas.drawCircle(particle.x, particle.y, particle.width / 2, paint);
            }
        }

        synchronized (messages) {
            for (GameMessage msg : messages) {
                textPaint.setColor(msg.color);
                textPaint.setTextSize(30);
                canvas.drawText(msg.text, msg.x, msg.y, textPaint);
            }
        }

        drawUI(canvas);
        canvas.translate(-shakeX, -shakeY);
    }

    private void drawConveyorSystem(Canvas canvas) {
        int topFrameHeight = getConveyorTopFrameHeight();
        int bottomFrameHeight = getConveyorBottomFrameHeight();

        float totalHeight = topFrameHeight + BELT_HEIGHT + bottomFrameHeight;
        float startY = uiZoneHeight + (conveyorZoneHeight - totalHeight) / 2;

        float beltY = startY + topFrameHeight;
        float bottomFrameY = beltY + BELT_HEIGHT;

        canvas.drawBitmap(conveyorTopBitmap, 0, startY, paint);
        drawMovingBelt(canvas, beltY);
        canvas.drawBitmap(conveyorBottomBitmap, 0.0f, bottomFrameY, paint);
    }

    private void drawMovingBelt(Canvas canvas, float beltY) {
        if (conveyorBeltBitmap == null) {
            return;
        }
        final int tileWidth = conveyorBeltBitmap.getWidth();
        float startOffset = (beltScrollOffset % tileWidth) - 2 * tileWidth;
        int tilesToDraw = (int) Math.ceil((screenWidth - startOffset) / tileWidth) + 2;
        for (int i = 0; i < tilesToDraw; i++) {
            float x = startOffset + (i * conveyorBeltBitmap.getWidth());
            canvas.drawBitmap(conveyorBeltBitmap, x, beltY, paint);
        }
    }

    private void drawUI(Canvas canvas) {
        paint.setColor(Color.argb(200, 0, 0, 0));
        // top panel background
        canvas.drawRect(0, 0, screenWidth, uiZoneHeight, paint);

        // score
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("\uD83D\uDCB0 " + money, 20, 50, textPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // level
        canvas.drawText(String.format(getContext().getString(R.string.gameHeaderLevel), level), screenWidth / 2, 50, textPaint);

        // time
        int minutes = (int) (gameTime / 60);
        int seconds = (int) (gameTime % 60);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(String.format(getContext().getString(R.string.gameHeaderTime), minutes, seconds), screenWidth - 10, 50, textPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // statistics
        smallTextPaint.setColor(Color.GREEN);
        canvas.drawText(String.format(getContext().getString(R.string.savedTrees), (int) totalTreesSaved), screenWidth - 150, 90, smallTextPaint);
        canvas.drawText(String.format(getContext().getString(R.string.savedWater), (int) totalWaterSaved), screenWidth - 150, 120, smallTextPaint);
        canvas.drawText(String.format(getContext().getString(R.string.savedCO2), (int) totalCo2Saved), screenWidth - 150, 150, smallTextPaint);

        // in-level progress
        float progressWidth = screenWidth * 0.8f;
        float progressX = screenWidth * 0.1f;
        float progressY = uiZoneHeight - 10;

        paint.setColor(Color.argb(100, 255, 255, 255));
        canvas.drawRect(progressX, progressY - 5, progressX + progressWidth, progressY + 5, paint);

        float levelProgress = gameTime % 30 / 30;
        paint.setColor(Color.GREEN);
        canvas.drawRect(progressX, progressY - 5, progressX + progressWidth * levelProgress, progressY + 5, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!gameActive) return true;

        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                for (TrashItem item : trashItems) {
                    if (item.containsPoint(touchX, touchY)) {
                        draggedItem = item;
                        item.isDragging = true;
                        dragOffsetX = touchX - item.x;
                        dragOffsetY = touchY - item.y;
                        soundManager.playSound(SoundManager.SOUND_CLICK);
                        break;
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (draggedItem != null) {
                    draggedItem.x = touchX - dragOffsetX;
                    draggedItem.y = touchY - dragOffsetY;
                }
                break;

            case MotionEvent.ACTION_UP:
                if (draggedItem != null) {
                    boolean hitBin = false;
                    for (Bin bin : bins) {
                        if (draggedItem.collidesWith(bin)) {
                            handleTrashInBin(draggedItem, bin);
                            hitBin = true;
                            removeTrashItem(draggedItem);
                            break;
                        }
                    }

                    if (!hitBin) {
                        int penalty = -Math.max(1, draggedItem.type.penalty / 3);
                        if (isPointOnBelt(draggedItem.x, draggedItem.y + TRASH_SIZE / 2)) {
                            // dropped on the conveyor belt - it keeps going.
                            draggedItem.isDragging = false;
                            // sort for correct drawing order
                            synchronized (trashItems) {
                                trashItems.sort(Comparator.comparing(TrashItem::getY));
                            }
                            addMessage(String.format(getContext().getString(R.string.msgDidNotGetIt), penalty), Color.YELLOW, draggedItem.x, draggedItem.y);
                        } else {
                            // dropped it past - it disappears
                            soundManager.playSound(SoundManager.SOUND_MISS);
                            createParticleEffect(draggedItem.x, draggedItem.y, Color.GRAY, 15, 1.0f);
                            addMessage(String.format(getContext().getString(R.string.msgMissedOutOfConveyor), penalty), Color.RED, draggedItem.x, draggedItem.y);
                            removeTrashItem(draggedItem);
                        }
                        addPenalty(penalty);
                    }

                    draggedItem = null;
                }
                break;
        }

        return true;
    }

    public boolean canContinue() {
        return gameActive;
    }

    public void restartGame() {
        money = INITIAL_MONEY;
        gameTime = 0;
        gameActive = true;
        level = 1;

        itemsSortedTotal = 0;
        totalCo2Saved = 0;
        totalTreesSaved = 0;
        totalWaterSaved = 0;

        spawnInterval = INITIAL_SPAWN_INTERVAL;
        conveyorSpeed = INITIAL_CONVEYOR_SPEED;

        synchronized (trashItems) {
            trashItems.clear();
        }
        synchronized (particles) {
            particles.clear();
        }
        synchronized (messages) {
            messages.clear();
        }

        for (TrashType type : TrashType.values()) {
            sortedStats.put(type, 0);
        }

        for (Bin bin : bins) {
            bin.itemsSorted = 0;
        }
        setupBinsForLevel();

        soundManager.playSound(SoundManager.SOUND_CLICK);
    }

    public void pauseGame() {
        if (gameThread != null) {
            gameThread.requestPause();
        }
    }

    public void resumeGame() {
        if (gameThread != null) {
            gameThread.requestResume();
        }
    }

    private Bitmap createGradientBackground(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint p = new Paint();

        for (int i = 0; i < height; i++) {
            int blue = 60 + (int) (i * 100.0 / height);
            int green = 40 + (int) (i * 60.0 / height);
            int red = 30;

            p.setColor(Color.rgb(red, green, blue));
            canvas.drawRect(0, i, width, i + 1, p);
        }

        return bitmap;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        gameThread.requestStop();

        while (retry) {
            try {
                gameThread.join();
                retry = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        soundManager.release();
    }

    private int getOptimalTrashSize() {
        if (screenWidth < 1000) {
            return 140;
        } else if (screenWidth < 1500) {
            return 160;
        } else {
            return 180;
        }
    }

    private int getConveyorTopFrameHeight() {
        return (int) (conveyorTopBitmap.getHeight() * screenWidth / conveyorTopBitmap.getWidth());
    }

    private int getConveyorBottomFrameHeight() {
        return (int) (conveyorBottomBitmap.getHeight() * screenWidth / conveyorBottomBitmap.getWidth());
    }

    private float getBeltY() {
        int topFrameHeight = getConveyorTopFrameHeight();
        float totalHeight = topFrameHeight + BELT_HEIGHT + getConveyorBottomFrameHeight();
        float startY = uiZoneHeight + (conveyorZoneHeight - totalHeight) / 2;
        return startY + topFrameHeight;
    }

    private boolean isPointOnBelt(float x, float y) {
        float beltY = getBeltY();
        return y >= beltY && y <= beltY + BELT_HEIGHT && x >= 0 && x <= screenWidth;
    }

    private void removeTrashItem(TrashItem item) {
        synchronized (trashItems) {
            trashItems.remove(item);
        }
    }

    public interface GameEventListener {
        void onGameOver(int itemsSorted, float treesSaved, float waterSaved, float co2Saved);

        void onBackToMenu();
    }

    private static class GameMessage {
        String text;
        int color;
        float x, y;
        float lifeTime = 2.0f;

        GameMessage(String text, int color, float x, float y) {
            this.text = text;
            this.color = color;
            this.x = x;
            this.y = y;
        }
    }
}