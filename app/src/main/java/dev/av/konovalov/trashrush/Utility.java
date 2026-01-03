package dev.av.konovalov.trashrush;

import android.graphics.Bitmap;

public class Utility {
    public static Bitmap scaleBitmapToWidth(Bitmap original, int targetWidth) {
        float scale = (float) targetWidth / original.getWidth();
        int targetHeight = (int) (original.getHeight() * scale);
        return Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true);
    }

    public static Bitmap scaleBitmapToHeight(Bitmap original, int targetHeight) {
        float scale = (float) targetHeight / original.getHeight();
        int targetWidth = (int) (original.getWidth() * scale);
        return Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true);
    }

    public static Bitmap fitBitmap(Bitmap original, int targetWidth, int targetHeight) {
        float ratio = (float) original.getWidth() / original.getHeight();
        float targetRatio = (float) targetWidth / targetHeight;
        if (ratio > targetRatio) {
            return scaleBitmapToWidth(original, targetWidth);
        } else {
            return scaleBitmapToHeight(original, targetHeight);
        }
    }
}
