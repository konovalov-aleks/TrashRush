package dev.av.konovalov.trashrush;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;

public class TrashItem {
    public float x, y;
    public float width, height;
    public Bitmap bitmap;

    public TrashType type;
    public float speed;
    public boolean isDragging = false;

    float getY() { return y; }

    public TrashItem(float startX, float startY,
                     Bitmap bitmap, TrashType type) {
        this.x = startX;
        this.y = startY;
        this.bitmap = bitmap;
        this.type = type;

        if (bitmap != null) {
            this.width = bitmap.getWidth();
            this.height = bitmap.getHeight();
        }
    }

    public void update(float conveyorSpeed, float deltaTime) {
        if (!isDragging)
            x += conveyorSpeed * deltaTime;
    }

    public boolean collidesWith(Bin bin) {
        return bin.isVisible() &&
                x < bin.x + bin.width &&
                x + width > bin.x &&
                y < bin.y + bin.height &&
                y + height > bin.y;
    }

    public boolean containsPoint(float px, float py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }
}