package dev.oneuiproject.oneui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.HapticFeedbackConstants;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.Nullable;

import dev.oneuiproject.oneui.design.R;

/** Thick One UI-style slider with a background-colored circular thumb. */
public class OneUISlider extends View {
    public interface OnValueChangeListener {
        void onValueChanged(OneUISlider slider, int value, boolean fromUser);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF track = new RectF();
    private int minValue = 0;
    private int maxValue = 100;
    private int value = 50;
    private int stepSize;
    private boolean showTicks;
    private boolean vertical;
    private boolean fillEnabled = true;
    private boolean hapticEnabled;
    private int warningValue = -1;
    private int activeColor;
    private int trackColor;
    private int thumbFillColor;
    private int tickColor;
    private Drawable leadingIcon;
    private OnValueChangeListener listener;

    public OneUISlider(Context context) { this(context, null); }

    public OneUISlider(Context context, @Nullable AttributeSet attrs) { this(context, attrs, 0); }

    public OneUISlider(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        activeColor = themeColor(androidx.appcompat.R.attr.colorPrimary,
                color(R.color.oui_slider_active_color));
        trackColor = color(R.color.oui_slider_track_color);
        thumbFillColor = color(R.color.oui_background_color);
        tickColor = color(R.color.oui_slider_tick_color);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.OneUISlider,
                    defStyleAttr, 0);
            minValue = a.getInt(R.styleable.OneUISlider_minValue, minValue);
            maxValue = a.getInt(R.styleable.OneUISlider_maxValue, maxValue);
            value = a.getInt(R.styleable.OneUISlider_sliderValue, value);
            stepSize = Math.max(0, a.getInt(R.styleable.OneUISlider_stepSize, 0));
            showTicks = a.getBoolean(R.styleable.OneUISlider_showTicks, false);
            vertical = a.getBoolean(R.styleable.OneUISlider_sliderVertical, false);
            fillEnabled = a.getBoolean(R.styleable.OneUISlider_sliderFillEnabled, true);
            hapticEnabled = a.getBoolean(R.styleable.OneUISlider_sliderHapticEnabled, false);
            warningValue = a.getInt(R.styleable.OneUISlider_sliderWarningValue, -1);
            activeColor = a.getColor(R.styleable.OneUISlider_sliderActiveColor, activeColor);
            trackColor = a.getColor(R.styleable.OneUISlider_sliderTrackColor, trackColor);
            thumbFillColor = a.getColor(R.styleable.OneUISlider_thumbFillColor, thumbFillColor);
            leadingIcon = a.getDrawable(R.styleable.OneUISlider_leadingIcon);
            a.recycle();
        }
        if (maxValue <= minValue) maxValue = minValue + 1;
        value = clampAndSnap(value);
        setFocusable(true);
        setClickable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void setRange(int min, int max) {
        if (max <= min) throw new IllegalArgumentException("max must exceed min");
        minValue = min;
        maxValue = max;
        setValue(value);
        invalidate();
    }

    public int getValue() { return value; }
    public int getMinValue() { return minValue; }
    public int getMaxValue() { return maxValue; }

    public void setValue(int newValue) { updateValue(newValue, false); }

    public void setStepSize(int step) {
        stepSize = Math.max(0, step);
        updateValue(value, false);
        invalidate();
    }

    public void setShowTicks(boolean show) { showTicks = show; invalidate(); }
    public void setVertical(boolean enabled) { vertical = enabled; requestLayout(); invalidate(); }
    public void setFillEnabled(boolean enabled) { fillEnabled = enabled; invalidate(); }
    public void setHapticEnabled(boolean enabled) { hapticEnabled = enabled; }
    public void setWarningValue(int threshold) { warningValue = threshold; invalidate(); }
    public void setActiveColor(int color) { activeColor = color; invalidate(); }
    public int getActiveColor() { return activeColor; }
    public void setTrackColor(int color) { trackColor = color; invalidate(); }
    public void setThumbFillColor(int color) { thumbFillColor = color; invalidate(); }
    public void setTickColor(int color) { tickColor = color; invalidate(); }
    public void setLeadingIcon(@Nullable Drawable icon) { leadingIcon = icon; invalidate(); }
    public void setOnValueChangeListener(@Nullable OnValueChangeListener callback) { listener = callback; }

    private void updateValue(int requested, boolean fromUser) {
        int next = clampAndSnap(requested);
        if (value == next) return;
        value = next;
        invalidate();
        if (fromUser && hapticEnabled) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        if (listener != null) listener.onValueChanged(this, value, fromUser);
        if (fromUser) sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED);
    }

    private int currentActiveColor() {
        return warningValue >= minValue && value >= warningValue
                ? color(R.color.oui_slider_warning_color) : activeColor;
    }

    private int clampAndSnap(int requested) {
        int clamped = Math.max(minValue, Math.min(maxValue, requested));
        if (stepSize == 0) return clamped;
        int snapped = minValue + Math.round((clamped - minValue) / (float) stepSize) * stepSize;
        return Math.max(minValue, Math.min(maxValue, snapped));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(resolveSize(dp(vertical ? 48 : 180), widthMeasureSpec),
                resolveSize(dp(vertical ? 180 : 48), heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (vertical) {
            drawVertical(canvas);
            return;
        }
        int centerY = getHeight() / 2;
        float start = getPaddingLeft() + dp(16) + (leadingIcon == null ? 0 : dp(32));
        float end = getWidth() - getPaddingRight() - dp(16);
        if (end <= start) return;
        float thumbX = start + (end - start) * (value - minValue) / (maxValue - minValue);
        float radius = dp(8);
        track.set(start, centerY - radius, end, centerY + radius);
        paint.setColor(trackColor);
        canvas.drawRoundRect(track, radius, radius, paint);
        if (fillEnabled && thumbX > start) {
            track.right = thumbX;
            paint.setColor(currentActiveColor());
            canvas.drawRoundRect(track, radius, radius, paint);
        }
        if (showTicks && stepSize > 0) {
            int count = (maxValue - minValue) / stepSize;
            if (count > 1 && count <= 100) {
                paint.setColor(tickColor);
                for (int i = 0; i <= count; i++) {
                    float tickStart = start + dp(8);
                    float tickWidth = end - start - dp(16);
                    float x = tickStart + tickWidth * i * stepSize / (maxValue - minValue);
                    if (Math.abs(x - thumbX) > dp(12)) canvas.drawCircle(x, centerY, dp(4), paint);
                }
            }
        }
        paint.setColor(currentActiveColor());
        canvas.drawCircle(thumbX, centerY, dp(12), paint);
        paint.setColor(thumbFillColor);
        canvas.drawCircle(thumbX, centerY, dp(9), paint);
        if (leadingIcon != null) {
            Drawable icon = leadingIcon.mutate();
            icon.setTint(currentActiveColor());
            int left = getPaddingLeft();
            icon.setBounds(left, centerY - dp(12), left + dp(24), centerY + dp(12));
            icon.draw(canvas);
        }
    }

    private void drawVertical(Canvas canvas) {
        float centerX = getWidth() / 2f;
        float top = getPaddingTop() + dp(16);
        float bottom = getHeight() - getPaddingBottom() - dp(16);
        if (bottom <= top) return;
        float thumbY = bottom - (bottom - top) * (value - minValue) / (maxValue - minValue);
        float radius = dp(8);
        paint.setColor(trackColor);
        canvas.drawRoundRect(centerX - radius, top, centerX + radius, bottom,
                radius, radius, paint);
        if (fillEnabled && thumbY < bottom) {
            paint.setColor(currentActiveColor());
            canvas.drawRoundRect(centerX - radius, thumbY, centerX + radius, bottom,
                    radius, radius, paint);
        }
        if (showTicks && stepSize > 0) {
            int count = (maxValue - minValue) / stepSize;
            if (count > 1 && count <= 100) {
                paint.setColor(tickColor);
                for (int i = 0; i <= count; i++) {
                    float y = bottom - dp(8) - (bottom - top - dp(16))
                            * i * stepSize / (maxValue - minValue);
                    if (Math.abs(y - thumbY) > dp(12))
                        canvas.drawCircle(centerX, y, dp(4), paint);
                }
            }
        }
        paint.setColor(currentActiveColor());
        canvas.drawCircle(centerX, thumbY, dp(12), paint);
        paint.setColor(thumbFillColor);
        canvas.drawCircle(centerX, thumbY, dp(9), paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                updateFromTouch(vertical ? event.getY() : event.getX());
                return true;
            case MotionEvent.ACTION_MOVE:
                updateFromTouch(vertical ? event.getY() : event.getX());
                return true;
            case MotionEvent.ACTION_UP:
                updateFromTouch(vertical ? event.getY() : event.getX());
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void updateFromTouch(float x) {
        if (vertical) {
            float top = getPaddingTop() + dp(16);
            float bottom = getHeight() - getPaddingBottom() - dp(16);
            if (bottom <= top) return;
            float fraction = Math.max(0f, Math.min(1f, (bottom - x) / (bottom - top)));
            updateValue(minValue + Math.round(fraction * (maxValue - minValue)), true);
            return;
        }
        float start = getPaddingLeft() + dp(16) + (leadingIcon == null ? 0 : dp(32));
        float end = getWidth() - getPaddingRight() - dp(16);
        if (end <= start) return;
        float fraction = Math.max(0f, Math.min(1f, (x - start) / (end - start)));
        updateValue(minValue + Math.round(fraction * (maxValue - minValue)), true);
    }

    @Override public boolean performClick() { super.performClick(); return true; }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                || keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            int direction = keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_UP ? 1 : -1;
            updateValue(value + direction * (stepSize > 0 ? stepSize : 1), true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(android.widget.SeekBar.class.getName());
        info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(
                AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT, minValue, maxValue, value));
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                || action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
            updateValue(value + (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD ? 1 : -1)
                    * (stepSize > 0 ? stepSize : 1), true);
            return true;
        }
        return super.performAccessibilityAction(action, arguments);
    }

    private int color(int id) { return getResources().getColor(id, getContext().getTheme()); }
    private int themeColor(int attribute, int fallback) {
        TypedValue value = new TypedValue();
        if (!getContext().getTheme().resolveAttribute(attribute, value, true)) return fallback;
        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && value.type <= TypedValue.TYPE_LAST_COLOR_INT) return value.data;
        if (value.resourceId != 0)
            return getResources().getColor(value.resourceId, getContext().getTheme());
        return fallback;
    }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
