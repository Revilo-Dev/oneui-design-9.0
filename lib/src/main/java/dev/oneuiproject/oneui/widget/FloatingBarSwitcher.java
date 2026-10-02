package dev.oneuiproject.oneui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Keeps navigation and contextual bars in the same bottom slot. Set the navigation view,
 * then call {@link #showBar(View)} or {@link #showNavigation()} to swap them.
 */
public class FloatingBarSwitcher extends FrameLayout {
    private View navigation;
    private View backdropView;
    private View current;
    private boolean animationsEnabled = true;
    private boolean backdropCapture;

    public FloatingBarSwitcher(@NonNull Context context) { this(context, null); }
    public FloatingBarSwitcher(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setClipChildren(false);
    }

    public void setNavigationBar(@NonNull View bar) {
        requireChild(bar);
        navigation = bar;
        if (current == null) current = bar;
    }

    public void setAnimationsEnabled(boolean enabled) { animationsEnabled = enabled; }
    public boolean isAnimationsEnabled() { return animationsEnabled; }
    public View getCurrentBar() { return current; }
    /** Optional content view sampled by blurred bars; place it behind this switcher. */
    public void setBackdropView(@Nullable View content) { backdropView = content; }
    @Nullable View getBackdropView() { return backdropView; }
    void setBackdropCapture(boolean capturing) { backdropCapture = capturing; }
    @Override public void draw(Canvas canvas) {
        if (!backdropCapture) super.draw(canvas);
    }
    public void showNavigation() {
        if (navigation == null) throw new IllegalStateException("Set a navigation bar first");
        showBar(navigation);
    }

    public void showBar(@NonNull View next) {
        requireChild(next);
        if (next == current) return;
        View old = current;
        current = next;
        if (old != null) old.animate().cancel();
        next.animate().cancel();
        if (old == null || !animationsEnabled) {
            if (old != null) { old.setVisibility(GONE); old.setScaleX(1f); old.setAlpha(1f); }
            next.setScaleX(1f);
            next.setAlpha(1f);
            next.setVisibility(VISIBLE);
            return;
        }
        old.animate().scaleX(0f).alpha(0f).setDuration(100).withEndAction(() -> {
            old.setVisibility(GONE);
            old.setScaleX(1f);
            old.setAlpha(1f);
            next.setScaleX(0f);
            next.setAlpha(0f);
            next.setVisibility(VISIBLE);
            next.animate().scaleX(1f).alpha(1f).setDuration(130).start();
        }).start();
    }

    private void requireChild(View view) {
        if (view.getParent() != this)
            throw new IllegalArgumentException("Bar must be a direct child of the switcher");
    }
}
