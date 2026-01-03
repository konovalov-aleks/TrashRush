package dev.av.konovalov.trashrush;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class GameThread extends Thread {

    private final SurfaceHolder surfaceHolder;
    private final GameView gameView;
    private boolean running = true;
    private boolean paused = false;
    private long lastTime;

    public GameThread(SurfaceHolder holder, GameView gameView) {
        this.surfaceHolder = holder;
        this.gameView = gameView;
        this.lastTime = System.nanoTime();
    }

    public void requestStop() {
        this.running = false;
        synchronized (this) {
            notify();
        }
    }

    public void requestPause() {
        this.paused = true;
    }

    public void requestResume() {
        if (this.paused) {
            this.paused = false;
            synchronized (this) {
                notify();
            }
        }
    }

    @Override
    public void run() {
        while (running) {
            if (paused) {
                try {
                    synchronized (this) {
                        wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                lastTime = System.nanoTime();
                continue;
            }

            long currentTime = System.nanoTime();
            float deltaTime = (currentTime - lastTime) / 1000000000.0f;
            lastTime = currentTime;

            gameView.update(deltaTime);

            Canvas canvas = null;
            try {
                canvas = surfaceHolder.lockCanvas();
                if (canvas != null) {
                    synchronized (surfaceHolder) {
                        gameView.draw(canvas);
                    }
                }
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }

            boolean limitFPS = false;
            if (limitFPS) {
                try {
                    sleep(16); // ~60 FPS
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}