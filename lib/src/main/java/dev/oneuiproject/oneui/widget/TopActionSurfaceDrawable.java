package dev.oneuiproject.oneui.widget;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dev.oneuiproject.oneui.design.R;

/** Rounded background owned by a toolbar control group, with optional backdrop blur. */
public final class TopActionSurfaceDrawable extends Drawable {
    private static final int SAMPLE = 6;
    private final View host;
    private final View source;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path path = new Path();
    private Bitmap bitmap;
    private int[] sourcePixels;
    private int[] blurredPixels;
    private boolean blurEnabled;
    private float radius;
    private Integer tintColor;

    public TopActionSurfaceDrawable(View host, View source, float radius) {
        this.host = host;
        this.source = source;
        this.radius = radius;
        BlurSettings.observe(host);
        host.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View view) { }
            @Override public void onViewDetachedFromWindow(View view) { release(); }
        });
    }

    public void setBlurEnabled(boolean enabled) { blurEnabled = enabled; invalidateSelf(); }
    public void setTintColor(@Nullable Integer color) { tintColor = color; invalidateSelf(); }
    public void setRadius(float pixels) { radius = pixels; invalidateSelf(); }

    @Override public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) return;
        int save = canvas.save();
        path.reset();
        path.addRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom,
                radius, radius, Path.Direction.CW);
        canvas.clipPath(path);
        boolean effectiveBlur = blurEnabled && BlurSettings.isEnabled(host.getContext());
        if (effectiveBlur && source.isAttachedToWindow() && !host.isInEditMode()) {
            capture(bounds.width(), bounds.height());
            if (bitmap != null) {
                paint.setColor(Color.WHITE);
                canvas.drawBitmap(bitmap, null, bounds, paint);
            }
        }
        int solid = host.getResources().getColor(R.color.oui_floating_nav_surface,
                host.getContext().getTheme());
        int glass = host.getResources().getColor(R.color.oui_floating_nav_glass,
                host.getContext().getTheme());
        paint.setColor(tintColor != null ? tintColor : effectiveBlur ? glass : solid);
        canvas.drawRect(bounds, paint);
        canvas.restoreToCount(save);
    }

    private void capture(int hostWidth, int hostHeight) {
        int width = Math.max(1, (hostWidth + SAMPLE - 1) / SAMPLE);
        int height = Math.max(1, (hostHeight + SAMPLE - 1) / SAMPLE);
        if (bitmap == null || bitmap.getWidth() != width || bitmap.getHeight() != height) {
            if (bitmap != null) bitmap.recycle();
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            sourcePixels = new int[width * height];
            blurredPixels = new int[width * height];
        }
        bitmap.eraseColor(Color.TRANSPARENT);
        Canvas sample = new Canvas(bitmap);
        sample.scale((float) width / hostWidth, (float) height / hostHeight);
        int[] here = new int[2];
        int[] there = new int[2];
        host.getLocationInWindow(here);
        source.getLocationInWindow(there);
        sample.translate(there[0] - here[0], there[1] - here[1]);
        source.draw(sample);
        bitmap.getPixels(sourcePixels, 0, width, 0, 0, width, height);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int a = 0, r = 0, g = 0, b = 0;
            for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                int pixel = sourcePixels[Math.max(0, Math.min(height - 1, y + dy)) * width
                        + Math.max(0, Math.min(width - 1, x + dx))];
                a += pixel >>> 24;
                r += pixel >> 16 & 255;
                g += pixel >> 8 & 255;
                b += pixel & 255;
            }
            blurredPixels[y * width + x] = (a / 9 << 24) | (r / 9 << 16)
                    | (g / 9 << 8) | b / 9;
        }
        bitmap.setPixels(blurredPixels, 0, width, 0, 0, width, height);
    }

    public void release() {
        if (bitmap != null) bitmap.recycle();
        bitmap = null;
    }

    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(@Nullable ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
