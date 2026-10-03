package dev.oneuiproject.oneui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.Nullable;
import androidx.annotation.DrawableRes;

import dev.oneuiproject.oneui.design.R;

/** Large volume/brightness control with optional icon, value, menu, and warning range. */
public class OneUIThickSlider extends View {
    public interface OnValueChangeListener {
        void onValueChanged(OneUIThickSlider slider, int value, boolean fromUser);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF track = new RectF();
    private static boolean capturingAnyBackdrop;
    private Bitmap backdrop;
    private int[] pixels;
    private int[] blurredPixels;
    private long lastBackdropCapture;
    private boolean capturingBackdrop;
    private boolean vertical;
    private boolean showNumber;
    private boolean showMenuButton;
    private boolean dragging;
    private int minValue = 0;
    private int maxValue = 100;
    private int value = 30;
    private int warningValue = -1;
    private int activeColor;
    private int trackColor;
    private int warningAreaColor;
    private int errorColor;
    private Drawable icon;
    private Runnable menuClickListener;
    private OnValueChangeListener valueListener;

    public OneUIThickSlider(Context context) { this(context, null); }
    public OneUIThickSlider(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        activeColor = themeColor(context, androidx.appcompat.R.attr.colorPrimary,
                context.getColor(R.color.oui_slider_active_color));
        trackColor = context.getColor(R.color.oui_slider_track_color);
        warningAreaColor = context.getColor(R.color.oui_slider_warning_area_color);
        errorColor = context.getColor(R.color.oui_functional_red_color);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.OneUIThickSlider);
            vertical = a.getBoolean(R.styleable.OneUIThickSlider_thickVertical, false);
            minValue = a.getInt(R.styleable.OneUIThickSlider_thickMinValue, 0);
            maxValue = Math.max(minValue + 1,
                    a.getInt(R.styleable.OneUIThickSlider_thickMaxValue, 100));
            value = a.getInt(R.styleable.OneUIThickSlider_thickValue, 30);
            warningValue = a.getInt(R.styleable.OneUIThickSlider_thickWarningValue, -1);
            icon = a.getDrawable(R.styleable.OneUIThickSlider_thickIcon);
            showNumber = a.getBoolean(R.styleable.OneUIThickSlider_thickShowNumber, false);
            showMenuButton = a.getBoolean(R.styleable.OneUIThickSlider_thickShowMenuButton, false);
            activeColor = a.getColor(R.styleable.OneUIThickSlider_thickActiveColor, activeColor);
            trackColor = a.getColor(R.styleable.OneUIThickSlider_thickTrackColor, trackColor);
            a.recycle();
        }
        value = clamp(value);
        setFocusable(true);
        setClickable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        BlurSettings.observe(this);
    }

    public void setVertical(boolean enabled) { vertical = enabled; requestLayout(); invalidate(); }
    public void setRange(int min, int max) {
        if (max <= min) throw new IllegalArgumentException("max must exceed min");
        minValue = min; maxValue = max; setValue(value);
        invalidate();
    }
    public int getValue() { return value; }
    public void setValue(int next) { updateValue(next, false); }
    public void setWarningValue(int threshold) { warningValue = threshold; invalidate(); }
    public void setWarningAreaColor(int color) { warningAreaColor = color; invalidate(); }
    public void setErrorColor(int color) { errorColor = color; invalidate(); }
    public void setActiveColor(int color) { activeColor = color; invalidate(); }
    public void setTrackColor(int color) { trackColor = color; invalidate(); }
    public void setIcon(@Nullable Drawable drawable) { icon = drawable; invalidate(); }
    public void setIconResource(@DrawableRes int resourceId) {
        setIcon(resourceId == 0 ? null : getContext().getDrawable(resourceId));
    }
    public void setShowNumber(boolean show) { showNumber = show; invalidate(); }
    public void setShowMenuButton(boolean show) { showMenuButton = show; invalidate(); }
    public void setOnMenuClickListener(@Nullable Runnable listener) { menuClickListener = listener; }
    public void setOnValueChangeListener(@Nullable OnValueChangeListener listener) {
        valueListener = listener;
    }

    private int clamp(int next) { return Math.max(minValue, Math.min(maxValue, next)); }
    private boolean inWarningRange() { return warningValue >= minValue && value >= warningValue; }
    private float fraction() { return (value - minValue) / (float) (maxValue - minValue); }
    private float warningFraction() {
        return (warningValue - minValue) / (float) (maxValue - minValue);
    }

    private void updateValue(int next, boolean fromUser) {
        int clamped = clamp(next);
        if (clamped == value) return;
        value = clamped;
        invalidate();
        if (fromUser) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        if (valueListener != null) valueListener.onValueChanged(this, value, fromUser);
        if (fromUser) sendAccessibilityEvent(
                android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(resolveSize(dp(vertical ? 80 : 260), widthSpec),
                resolveSize(dp(vertical ? 260 : 64), heightSpec));
    }

    @Override public void draw(Canvas canvas) {
        if (!capturingBackdrop) super.draw(canvas);
    }

    @Override protected void onDetachedFromWindow() {
        if (backdrop != null) { backdrop.recycle(); backdrop = null; }
        lastBackdropCapture = 0;
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float inset = dp(6);
        track.set(getPaddingLeft() + inset, getPaddingTop() + inset,
                getWidth() - getPaddingRight() - inset,
                getHeight() - getPaddingBottom() - inset);
        if (track.width() <= 0 || track.height() <= 0) return;
        float radius = Math.min(track.width(), track.height()) / 2f;
        boolean blur = BlurSettings.isEnabled(getContext()) && !isInEditMode();
        if (blur && !capturingAnyBackdrop) captureBackdrop();
        int backgroundSave = canvas.save();
        canvas.clipPath(roundedTrackPath(radius));
        if (blur && backdrop != null) {
            paint.setColor(Color.WHITE);
            paint.setFilterBitmap(true);
            canvas.drawBitmap(backdrop, null, new RectF(0, 0, getWidth(), getHeight()), paint);
            paint.setFilterBitmap(false);
            paint.setColor(0x30FFFFFF);
        } else {
            paint.setColor(trackColor);
        }
        canvas.drawRect(track, paint);
        canvas.restoreToCount(backgroundSave);
        int save = canvas.save();
        canvas.clipPath(roundedTrackPath(radius));
        if (warningValue > minValue && warningValue < maxValue) {
            paint.setColor(warningAreaColor);
            if (vertical) {
                float boundary = track.bottom - track.height() * warningFraction();
                canvas.drawRect(track.left, track.top, track.right, boundary, paint);
            } else {
                float boundary = track.left + track.width() * warningFraction();
                canvas.drawRect(boundary, track.top, track.right, track.bottom, paint);
            }
        }
        int fill = inWarningRange() ? errorColor : activeColor;
        paint.setColor(fill);
        if (vertical) {
            float fillTop = Math.min(track.bottom - track.width(),
                    track.bottom - track.height() * fraction());
            canvas.drawRoundRect(track.left, fillTop, track.right, track.bottom,
                    radius, radius, paint);
        } else {
            float fillRight = Math.max(track.left + track.height(),
                    track.left + track.width() * fraction());
            canvas.drawRoundRect(track.left, track.top, fillRight, track.bottom,
                    radius, radius, paint);
        }
        canvas.restoreToCount(save);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(0.5f));
        paint.setColor(0x80FFFFFF);
        canvas.drawRoundRect(track, radius, radius, paint);
        paint.setStyle(Paint.Style.FILL);

        int foreground = contrastColor(fill);
        if (icon != null) {
            Drawable drawnIcon = icon.mutate();
            drawnIcon.setTint(foreground);
            float cx = vertical ? track.centerX() : track.left + radius;
            float cy = vertical ? track.bottom - radius : track.centerY();
            int half = dp(14);
            drawnIcon.setBounds(Math.round(cx) - half, Math.round(cy) - half,
                    Math.round(cx) + half, Math.round(cy) + half);
            drawnIcon.draw(canvas);
        }
        if (showNumber) {
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(sp(vertical ? 20 : 15));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(vertical ? contrastColor(trackColor) : foreground);
            float x = vertical ? track.centerX() : track.right - dp(showMenuButton ? 82 : 32);
            float y = vertical ? track.top + dp(showMenuButton ? 76 : 46)
                    : track.centerY() + (paint.descent() - paint.ascent()) / 2f - paint.descent();
            canvas.drawText(String.valueOf(value), x, y, paint);
        }
        if (showMenuButton) {
            paint.setColor(vertical ? contrastColor(trackColor) : contrastColor(fill));
            float centerX = vertical ? track.centerX() : track.right - dp(28);
            float centerY = track.top + dp(vertical ? 24 : 20);
            for (int i = -1; i <= 1; i++)
                canvas.drawCircle(centerX + (vertical ? 0 : i * dp(8)),
                        centerY + (vertical ? i * dp(8) : 0), dp(2f), paint);
        }
    }

    private void captureBackdrop() {
        if (getWidth() == 0 || getHeight() == 0 || !isAttachedToWindow()
                || SystemClock.uptimeMillis() - lastBackdropCapture < 80) return;
        View source = getRootView();
        if (source == this) return;
        int width = Math.max(1, (getWidth() + 5) / 6);
        int height = Math.max(1, (getHeight() + 5) / 6);
        if (backdrop == null || backdrop.getWidth() != width || backdrop.getHeight() != height) {
            if (backdrop != null) backdrop.recycle();
            backdrop = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            pixels = new int[width * height];
            blurredPixels = new int[width * height];
        }
        backdrop.eraseColor(Color.TRANSPARENT);
        Canvas sample = new Canvas(backdrop);
        sample.scale((float) width / getWidth(), (float) height / getHeight());
        int[] here = new int[2];
        int[] there = new int[2];
        getLocationOnScreen(here);
        source.getLocationOnScreen(there);
        sample.translate(there[0] - here[0], there[1] - here[1]);
        capturingAnyBackdrop = true;
        capturingBackdrop = true;
        try {
            source.draw(sample);
        } catch (IndexOutOfBoundsException unstableHierarchy) {
            backdrop.eraseColor(Color.TRANSPARENT);
        } finally {
            capturingBackdrop = false;
            capturingAnyBackdrop = false;
        }
        backdrop.getPixels(pixels, 0, width, 0, 0, width, height);
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
            blurredPixels[y * width + x] = (a / 9 << 24) | (r / 9 << 16)
                    | (g / 9 << 8) | b / 9;
        }
        backdrop.setPixels(blurredPixels, 0, width, 0, 0, width, height);
        lastBackdropCapture = SystemClock.uptimeMillis();
    }

    private android.graphics.Path roundedTrackPath(float radius) {
        android.graphics.Path path = new android.graphics.Path();
        path.addRoundRect(track, radius, radius, android.graphics.Path.Direction.CW);
        return path;
    }

    private int contrastColor(int background) {
        int light = (Color.red(background) * 299 + Color.green(background) * 587
                + Color.blue(background) * 114) / 1000;
        return light > 155 ? Color.BLACK : Color.WHITE;
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (showMenuButton && isInMenuHitTarget(event.getX(), event.getY())) {
                    dragging = false;
                    return true;
                }
                dragging = true;
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                updateFromTouch(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (dragging) updateFromTouch(event);
                return true;
            case MotionEvent.ACTION_UP:
                if (dragging) updateFromTouch(event);
                else if (showMenuButton && isInMenuHitTarget(event.getX(), event.getY())
                        && menuClickListener != null) menuClickListener.run();
                dragging = false;
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                return true;
            default: return super.onTouchEvent(event);
        }
    }

    private boolean isInMenuHitTarget(float x, float y) {
        return vertical ? y < track.top + dp(52)
                : x > track.right - dp(52) && y < track.top + dp(52);
    }

    private void updateFromTouch(MotionEvent event) {
        float part = vertical ? (track.bottom - event.getY()) / track.height()
                : (event.getX() - track.left) / track.width();
        updateValue(minValue + Math.round(Math.max(0f, Math.min(1f, part))
                * (maxValue - minValue)), true);
    }

    @Override public boolean performClick() { super.performClick(); return true; }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            int direction = keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                    || keyCode == KeyEvent.KEYCODE_DPAD_UP ? 1 : -1;
            updateValue(value + direction, true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(android.widget.SeekBar.class.getName());
        info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(
                AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT, minValue, maxValue, value));
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
    }

    @Override public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                || action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
            updateValue(value + (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD ? 1 : -1),
                    true);
            return true;
        }
        return super.performAccessibilityAction(action, arguments);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }

    private static int themeColor(Context context, int attribute, int fallback) {
        TypedValue value = new TypedValue();
        if (!context.getTheme().resolveAttribute(attribute, value, true)) return fallback;
        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && value.type <= TypedValue.TYPE_LAST_COLOR_INT) return value.data;
        return value.resourceId == 0 ? fallback : context.getColor(value.resourceId);
    }
}
