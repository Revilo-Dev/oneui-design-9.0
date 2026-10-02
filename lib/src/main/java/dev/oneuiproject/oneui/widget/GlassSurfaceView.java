package dev.oneuiproject.oneui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.graphics.Outline;

import androidx.annotation.Nullable;

import dev.oneuiproject.oneui.design.R;

/** A reusable rounded glass surface that blurs a supplied view behind it on API 23+. */
public class GlassSurfaceView extends View {
    private static final int SAMPLE = 6;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path clip = new Path();
    private View source;
    private Bitmap bitmap;
    private int[] pixels;
    private int[] blurred;
    private float radius;
    private boolean blurEnabled = true;
    private Integer tintColor;

    public GlassSurfaceView(Context context) { this(context, null); }
    public GlassSurfaceView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        radius = dp(28);
        setWillNotDraw(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        BlurSettings.observe(this);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
    }

    public void setSourceView(@Nullable View view) { source = view; invalidate(); }
    public void setCornerRadius(float pixels) {
        radius = Math.max(0, pixels);
        invalidateOutline();
        invalidate();
    }
    public void setBlurEnabled(boolean enabled) { blurEnabled = enabled; invalidate(); }
    public void setTintColor(@Nullable Integer color) { tintColor = color; invalidate(); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() == 0 || getHeight() == 0) return;
        clip.reset();
        clip.addRoundRect(0, 0, getWidth(), getHeight(), radius, radius, Path.Direction.CW);
        int save = canvas.save();
        canvas.clipPath(clip);
        boolean effectiveBlur = blurEnabled && BlurSettings.isEnabled(getContext());
        if (effectiveBlur && source != null && source != this && !isInEditMode()) {
            capture();
            canvas.drawBitmap(bitmap, null, new Rect(0, 0, getWidth(), getHeight()), paint);
        }
        paint.setColor(tintColor != null ? tintColor : getResources().getColor(
                effectiveBlur ? R.color.oui_glass_surface : R.color.oui_floating_nav_surface,
                getContext().getTheme()));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        canvas.restoreToCount(save);
    }

    private void capture() {
        int width = Math.max(1, (getWidth() + SAMPLE - 1) / SAMPLE);
        int height = Math.max(1, (getHeight() + SAMPLE - 1) / SAMPLE);
        if (bitmap == null || bitmap.getWidth() != width || bitmap.getHeight() != height) {
            if (bitmap != null) bitmap.recycle();
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            pixels = new int[width * height];
            blurred = new int[width * height];
        }
        bitmap.eraseColor(0);
        Canvas sample = new Canvas(bitmap);
        sample.scale((float) width / getWidth(), (float) height / getHeight());
        int[] here = new int[2];
        int[] there = new int[2];
        getLocationInWindow(here);
        source.getLocationInWindow(there);
        sample.translate(there[0] - here[0], there[1] - here[1]);
        source.draw(sample);
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int a = 0, r = 0, g = 0, b = 0;
            for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                int color = pixels[Math.max(0, Math.min(height - 1, y + dy)) * width
                        + Math.max(0, Math.min(width - 1, x + dx))];
                a += color >>> 24;
                r += color >> 16 & 255;
                g += color >> 8 & 255;
                b += color & 255;
            }
            blurred[y * width + x] = (a / 9 << 24) | (r / 9 << 16)
                    | (g / 9 << 8) | b / 9;
        }
        bitmap.setPixels(blurred, 0, width, 0, 0, width, height);
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (bitmap != null) { bitmap.recycle(); bitmap = null; }
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
