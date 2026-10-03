package dev.oneuiproject.oneui.widget;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.graphics.Outline;
import android.view.PixelCopy;
import android.view.Window;

import androidx.annotation.Nullable;

import dev.oneuiproject.oneui.design.R;

/** A reusable rounded glass surface that blurs a supplied view behind it on API 23+. */
public class GlassSurfaceView extends View {
    private static final int SAMPLE = 6;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path clip = new Path();
    private View source;
    private Window captureWindow;
    private Bitmap bitmap;
    private int[] pixels;
    private int[] blurred;
    private float radius;
    private boolean blurEnabled = true;
    private Integer tintColor;
    private boolean capturePending;
    private boolean captureFailed;
    private int captureGeneration;
    private long lastCaptureTime;

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

    /** Requests a fresh window sample; useful when content moves behind a floating bar. */
    public void refreshCapture() {
        if (captureWindow == null) { invalidate(); return; }
        if (capturePending || SystemClock.uptimeMillis() - lastCaptureTime < 60) return;
        if (bitmap != null) { bitmap.recycle(); bitmap = null; }
        captureFailed = false;
        invalidate();
    }

    /** Copies an already rendered activity window instead of redrawing its view tree. */
    public void setCaptureWindow(@Nullable Window window) {
        captureWindow = window;
        captureGeneration++;
        capturePending = false;
        captureFailed = false;
        if (bitmap != null) { bitmap.recycle(); bitmap = null; }
        invalidate();
    }
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
        if (effectiveBlur && !isInEditMode()) {
            if (captureWindow != null) {
                requestWindowCapture();
            } else if (source != null && source != this) {
                capture();
            }
            if (bitmap != null)
                canvas.drawBitmap(bitmap, null, new Rect(0, 0, getWidth(), getHeight()), paint);
        }
        paint.setColor(tintColor != null ? tintColor : getResources().getColor(
                effectiveBlur && bitmap != null ? R.color.oui_glass_surface
                        : R.color.oui_floating_nav_surface,
                getContext().getTheme()));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        canvas.restoreToCount(save);
    }

    private void requestWindowCapture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || capturePending
                || captureFailed || bitmap != null)
            return;
        View decor = captureWindow.getDecorView();
        if (!decor.isAttachedToWindow()) return;
        int[] here = new int[2];
        int[] there = new int[2];
        getLocationOnScreen(here);
        decor.getLocationOnScreen(there);
        Rect region = new Rect(here[0] - there[0], here[1] - there[1],
                here[0] - there[0] + getWidth(), here[1] - there[1] + getHeight());
        if (!region.intersect(0, 0, decor.getWidth(), decor.getHeight())) return;
        Bitmap sample = Bitmap.createBitmap(Math.max(1, getWidth() / SAMPLE),
                Math.max(1, getHeight() / SAMPLE), Bitmap.Config.ARGB_8888);
        int generation = captureGeneration;
        capturePending = true;
        try {
            PixelCopy.request(captureWindow, region, sample, result -> {
                if (generation != captureGeneration || !isAttachedToWindow()) {
                    sample.recycle();
                    return;
                }
                capturePending = false;
                if (result == PixelCopy.SUCCESS) {
                    bitmap = sample;
                    blurBitmap(bitmap);
                    lastCaptureTime = SystemClock.uptimeMillis();
                } else {
                    sample.recycle();
                    captureFailed = true;
                }
                invalidate();
            }, new Handler(Looper.getMainLooper()));
        } catch (IllegalArgumentException error) {
            capturePending = false;
            captureFailed = true;
            sample.recycle();
        }
    }

    private void blurBitmap(Bitmap sample) {
        int width = sample.getWidth();
        int height = sample.getHeight();
        pixels = new int[width * height];
        blurred = new int[width * height];
        sample.getPixels(pixels, 0, width, 0, 0, width, height);
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
        sample.setPixels(blurred, 0, width, 0, 0, width, height);
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
        getLocationOnScreen(here);
        source.getLocationOnScreen(there);
        sample.translate(there[0] - here[0], there[1] - here[1]);
        try {
            source.draw(sample);
        } catch (IndexOutOfBoundsException badDrawOrder) {
            // A view hierarchy changing mid-frame cannot be sampled safely.
            bitmap.recycle();
            bitmap = null;
            return;
        }
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
        captureGeneration++;
        capturePending = false;
        if (bitmap != null) { bitmap.recycle(); bitmap = null; }
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
