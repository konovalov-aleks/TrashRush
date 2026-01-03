package dev.av.konovalov.trashrush;

import android.graphics.Color;

public class Particle {
    public float x, y;
    public float width, height;
    public float velocityX, velocityY;
    public float lifeTime = 1.0f;
    public float maxLifeTime = 1.0f;
    public int color;

    public Particle(float x, float y, float vx, float vy, int color) {
        this.x = x;
        this.y = y;
        this.velocityX = vx;
        this.velocityY = vy;
        this.color = color;
        this.width = 10;
        this.height = 10;
    }

    public void update(float deltaTime) {
        x += velocityX * deltaTime * 60;
        y += velocityY * deltaTime * 60;
        velocityY += 0.1f * deltaTime * 60; // gravity
        lifeTime -= deltaTime;

        float scale = lifeTime / maxLifeTime;
        width = 10 * scale;
        height = 10 * scale;
    }

    public boolean isAlive() {
        return lifeTime > 0;
    }

    public int getAlphaColor() {
        int alpha = (int) (255 * (lifeTime / maxLifeTime));
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }
}
