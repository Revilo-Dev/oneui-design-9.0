package dev.oneuiproject.oneui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dev.oneuiproject.oneui.design.R;

/** A One UI style icon row that can optionally expand to show a short detail. */
public class IconRowView extends LinearLayout {
    private final FrameLayout badge;
    private final ImageView icon;
    private final ImageView chevron;
    private final TextView title;
    private final TextView subtitle;
    private final TextView detail;
    private boolean expandable;

    public IconRowView(@NonNull Context context) {
        this(context, null);
    }

    public IconRowView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setCardBackgroundColor(context.getColor(R.color.oui_floating_nav_surface));

        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), dp(12), dp(18), dp(12));
        addView(row, new LayoutParams(LayoutParams.MATCH_PARENT, dp(82)));

        badge = new FrameLayout(context);
        row.addView(badge, new LayoutParams(dp(44), dp(44)));
        setIconBackgroundColor(context.getColor(R.color.oui_floating_nav_press));
        icon = new ImageView(context);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        badge.addView(icon, new FrameLayout.LayoutParams(dp(25), dp(25), Gravity.CENTER));

        LinearLayout labels = new LinearLayout(context);
        labels.setOrientation(VERTICAL);
        LayoutParams labelsParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1);
        labelsParams.leftMargin = dp(18);
        row.addView(labels, labelsParams);
        title = new TextView(context);
        title.setTextColor(context.getColor(R.color.oui_primary_text_color));
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        labels.addView(title);
        subtitle = new TextView(context);
        subtitle.setTextColor(context.getColor(R.color.oui_floating_nav_secondary));
        subtitle.setTextSize(14);
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        labels.addView(subtitle);

        chevron = new ImageView(context);
        chevron.setImageResource(R.drawable.oui_chevron_down);
        chevron.setImageTintList(ColorStateList.valueOf(
                context.getColor(R.color.oui_floating_nav_secondary)));
        chevron.setVisibility(GONE);
        row.addView(chevron, new LayoutParams(dp(24), dp(24)));

        detail = new TextView(context);
        detail.setTextSize(14);
        detail.setTextColor(context.getColor(R.color.oui_floating_nav_secondary));
        detail.setPadding(dp(80), 0, dp(24), dp(18));
        detail.setVisibility(GONE);
        addView(detail);
        setOnClickListener(v -> {
            if (expandable) setExpanded(detail.getVisibility() != VISIBLE);
        });
        setClickable(false);
    }

    public void setTitle(@Nullable CharSequence value) {
        title.setText(value);
        updateContentDescription();
    }

    public void setSubtitle(@Nullable CharSequence value) {
        subtitle.setText(value);
        subtitle.setVisibility(TextUtils.isEmpty(value) ? GONE : VISIBLE);
        updateContentDescription();
    }

    public void setIconResource(@DrawableRes int resourceId) {
        icon.setImageResource(resourceId);
    }

    public void setIcon(@Nullable Drawable drawable) {
        icon.setImageDrawable(drawable);
    }

    public void setIconTintColor(@ColorInt int color) {
        icon.setImageTintList(ColorStateList.valueOf(color));
    }

    public void setIconBackgroundColor(@ColorInt int color) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(22));
        badge.setBackground(background);
    }

    public void setCardBackgroundColor(@ColorInt int color) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(28));
        setBackground(background);
    }

    /** Pass null to make a plain row; nonempty text shows the One UI chevron. */
    public void setExpandedText(@Nullable CharSequence value) {
        detail.setText(value);
        expandable = !TextUtils.isEmpty(value);
        chevron.setVisibility(expandable ? VISIBLE : GONE);
        setClickable(expandable);
        setFocusable(expandable);
        setExpanded(false);
    }

    public void setExpanded(boolean expanded) {
        boolean show = expandable && expanded;
        detail.setVisibility(show ? VISIBLE : GONE);
        chevron.setRotation(show ? 180f : 0f);
    }

    private void updateContentDescription() {
        setContentDescription(TextUtils.isEmpty(subtitle.getText()) ? title.getText()
                : title.getText() + ", " + subtitle.getText());
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
