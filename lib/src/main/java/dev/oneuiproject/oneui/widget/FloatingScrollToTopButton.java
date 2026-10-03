package dev.oneuiproject.oneui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import dev.oneuiproject.oneui.design.R;

/** A scroll action placed above a page's scroll fade and floating controls. */
public class FloatingScrollToTopButton extends FrameLayout {
    private RecyclerView list;
    private final RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
        @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            refresh();
        }
    };

    public FloatingScrollToTopButton(Context context) { this(context, null); }
    public FloatingScrollToTopButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setVisibility(GONE);
        setElevation(dp(16));
        setClickable(true);
        setFocusable(true);
        setContentDescription("Scroll to top");
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(context.getColor(R.color.oui_floating_nav_surface));
        setBackground(background);
        ImageView icon = new ImageView(context);
        icon.setImageResource(R.drawable.oui_ic_scroll_top);
        icon.setImageTintList(ColorStateList.valueOf(
                context.getColor(R.color.oui_primary_text_color)));
        addView(icon, new LayoutParams(dp(24), dp(24), Gravity.CENTER));
        setOnClickListener(v -> {
            if (list != null) list.smoothScrollToPosition(0);
        });
    }

    public void bind(@Nullable RecyclerView recyclerView) {
        if (list != null) list.removeOnScrollListener(scrollListener);
        list = recyclerView;
        if (list != null) {
            list.addOnScrollListener(scrollListener);
            list.post(this::refresh);
        } else setVisibility(View.GONE);
    }

    private void refresh() {
        setVisibility(list != null && list.canScrollVertically(-1) ? VISIBLE : GONE);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
