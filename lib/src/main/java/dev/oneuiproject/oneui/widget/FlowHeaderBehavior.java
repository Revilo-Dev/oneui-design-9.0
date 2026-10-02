package dev.oneuiproject.oneui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;

import com.google.android.material.appbar.AppBarLayout;

/** A pull that merely reaches the list top does not reopen the large header. */
public class FlowHeaderBehavior extends AppBarLayout.Behavior {
    private boolean expansionAllowed;

    public FlowHeaderBehavior() { super(); }
    public FlowHeaderBehavior(Context context, AttributeSet attrs) { super(context, attrs); }

    @Override public boolean onStartNestedScroll(CoordinatorLayout parent, AppBarLayout child,
            View directTargetChild, View target, int axes, int type) {
        boolean started = super.onStartNestedScroll(parent, child, directTargetChild,
                target, axes, type);
        if (started && type == ViewCompat.TYPE_TOUCH)
            expansionAllowed = !target.canScrollVertically(-1);
        return started;
    }

    @Override public void onNestedPreScroll(CoordinatorLayout parent, AppBarLayout child,
            View target, int dx, int dy, int[] consumed, int type) {
        if (target.canScrollVertically(-1)) expansionAllowed = false;
        if (dy < 0 && !expansionAllowed) return;
        super.onNestedPreScroll(parent, child, target, dx, dy, consumed, type);
    }

    @Override public void onNestedScroll(CoordinatorLayout parent, AppBarLayout child,
            View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
            int dyUnconsumed, int type, int[] consumed) {
        if (dyUnconsumed < 0 && !expansionAllowed) return;
        super.onNestedScroll(parent, child, target, dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, type, consumed);
    }
}
