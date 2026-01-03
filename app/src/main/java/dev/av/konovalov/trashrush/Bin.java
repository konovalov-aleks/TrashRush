package dev.av.konovalov.trashrush;

import android.graphics.Bitmap;

public class Bin {
    public float x, y;
    public float width, height;
    public Bitmap bitmap;

    public TrashType acceptedType;
    public int itemsSorted = 0;
    private boolean isVisible = false;

    public Bin(float x, float y, Bitmap bitmap, TrashType acceptedType) {
        this.x = x;
        this.y = y;
        this.bitmap = bitmap;
        this.acceptedType = acceptedType;
        this.width = bitmap.getWidth();
        this.height = bitmap.getHeight();
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void show() {
        isVisible = true;
    }

    public void hide() {
        isVisible = false;
    }

    public boolean accepts(TrashItem item) {
        return isVisible && item.type == acceptedType;
    }
}