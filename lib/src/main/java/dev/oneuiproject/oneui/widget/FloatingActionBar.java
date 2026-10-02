package dev.oneuiproject.oneui.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import dev.oneuiproject.oneui.design.R;

/** A two-command floating bar, typically used for Cancel and Save. */
public class FloatingActionBar extends LinearLayout {
    private final TextView cancel;
    private final TextView confirm;
    private final View divider;
    private boolean lightSurface;
    private boolean animationsEnabled = true;

    public FloatingActionBar(Context context) { this(context, null); }
    public FloatingActionBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        lightSurface = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                != Configuration.UI_MODE_NIGHT_YES;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(dp(6), dp(6), dp(6), dp(6));
        setElevation(dp(8));
        cancel = makeAction("Cancel");
        divider = new View(context);
        confirm = makeAction("Save");
        addView(cancel, new LayoutParams(0, LayoutParams.MATCH_PARENT, 1));
        addView(divider, new LayoutParams(dp(1), dp(24)));
        addView(confirm, new LayoutParams(0, LayoutParams.MATCH_PARENT, 1));
        updateColors();
    }

    public void setLabels(CharSequence cancelLabel, CharSequence confirmLabel) {
        cancel.setText(cancelLabel);
        confirm.setText(confirmLabel);
        cancel.setContentDescription(cancelLabel);
        confirm.setContentDescription(confirmLabel);
    }
    public void setOnCancelClickListener(@Nullable OnClickListener listener) {
        cancel.setOnClickListener(v -> press(cancel, listener));
    }
    public void setOnConfirmClickListener(@Nullable OnClickListener listener) {
        confirm.setOnClickListener(v -> press(confirm, listener));
    }
    public void setAnimationsEnabled(boolean enabled) { animationsEnabled = enabled; }
    public void setLightSurface(boolean light) { lightSurface = light; updateColors(); }

    private TextView makeAction(String title) {
        TextView view = new TextView(getContext());
        view.setText(title);
        view.setContentDescription(title);
        view.setTextSize(18);
        view.setGravity(Gravity.CENTER);
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private void press(View view, OnClickListener listener) {
        if (!animationsEnabled) { if (listener != null) listener.onClick(view); return; }
        view.animate().scaleX(0.88f).scaleY(0.88f).setDuration(100)
                .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(180)
                        .withEndAction(() -> { if (listener != null) listener.onClick(view); })
                        .start()).start();
    }

    private void updateColors() {
        int surface = getResources().getColor(lightSurface
                ? R.color.oui_floating_toolbar_light : R.color.oui_floating_toolbar_dark,
                getContext().getTheme());
        int text = getResources().getColor(lightSurface
                ? R.color.oui_floating_toolbar_light_text : R.color.oui_floating_toolbar_dark_text,
                getContext().getTheme());
        GradientDrawable background = new GradientDrawable();
        background.setColor(surface);
        background.setCornerRadius(dp(32));
        setBackground(background);
        cancel.setTextColor(text);
        confirm.setTextColor(text);
        divider.setBackgroundColor((text & 0x00ffffff) | 0x33000000);
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
