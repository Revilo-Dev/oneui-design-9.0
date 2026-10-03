package dev.oneuiproject.oneui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

/** Lays scrolling content across the whole page while AppBarLayout floats above it. */
public class FullBleedScrollingBehavior extends CoordinatorLayout.Behavior<View> {
    public FullBleedScrollingBehavior() { }
    public FullBleedScrollingBehavior(Context context, AttributeSet attrs) { super(context, attrs); }

    @Override public boolean onMeasureChild(CoordinatorLayout parent, View child,
            int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec,
            int heightUsed) {
        parent.onMeasureChild(child, parentWidthMeasureSpec, widthUsed,
                parentHeightMeasureSpec, heightUsed);
        return true;
    }

    @Override public boolean onLayoutChild(CoordinatorLayout parent, View child,
            int layoutDirection) {
        parent.onLayoutChild(child, layoutDirection);
        return true;
    }
}
