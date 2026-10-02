package dev.oneuiproject.oneui.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import dev.oneuiproject.oneui.design.R;
import dev.oneuiproject.oneui.widget.OneUISlider;

/** A persistent preference backed by {@link OneUISlider}. */
public class OneUISliderPreference extends Preference {
    private final int min;
    private final int max;
    private final int step;
    private final boolean ticks;
    private final boolean fill;
    private int value;

    public OneUISliderPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.oui_preference_oneui_slider);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.OneUISlider);
        min = a.getInt(R.styleable.OneUISlider_minValue, 0);
        max = Math.max(min + 1, a.getInt(R.styleable.OneUISlider_maxValue, 100));
        step = Math.max(0, a.getInt(R.styleable.OneUISlider_stepSize, 0));
        ticks = a.getBoolean(R.styleable.OneUISlider_showTicks, false);
        fill = a.getBoolean(R.styleable.OneUISlider_sliderFillEnabled, true);
        a.recycle();
    }

    public OneUISliderPreference(@NonNull Context context) { this(context, null); }

    @Override protected @Nullable Object onGetDefaultValue(@NonNull TypedArray a, int index) {
        return a.getInt(index, min);
    }

    @Override protected void onSetInitialValue(@Nullable Object defaultValue) {
        int initial = defaultValue instanceof Integer ? (Integer) defaultValue : min;
        setValue(getPersistedInt(initial));
    }

    public int getValue() { return value; }

    public void setValue(int requested) {
        int next = Math.max(min, Math.min(max, requested));
        if (step > 0) next = Math.min(max, min + Math.round((next - min) / (float) step) * step);
        if (value != next) {
            value = next;
            persistInt(next);
            notifyChanged();
        }
    }

    @Override public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        OneUISlider slider = (OneUISlider) holder.findViewById(R.id.preference_slider);
        TextView valueText = (TextView) holder.findViewById(R.id.preference_slider_value);
        slider.setRange(min, max);
        slider.setStepSize(step);
        slider.setShowTicks(ticks);
        slider.setFillEnabled(fill);
        slider.setValue(value);
        slider.setEnabled(isEnabled());
        valueText.setText(String.valueOf(value));
        slider.setOnValueChangeListener((view, newValue, fromUser) -> {
            if (!fromUser) return;
            if (callChangeListener(newValue)) {
                value = newValue;
                persistInt(newValue);
                valueText.setText(String.valueOf(newValue));
            } else {
                view.setValue(value);
            }
        });
    }
}
