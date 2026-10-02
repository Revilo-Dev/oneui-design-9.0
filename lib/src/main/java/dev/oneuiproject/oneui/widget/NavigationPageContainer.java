package dev.oneuiproject.oneui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

/**
 * Holds pages whose view IDs match {@link FloatingNavigationBar} destination IDs.
 * The navigation bar binds with {@link FloatingNavigationBar#bindPages(NavigationPageContainer)}.
 * Horizontal swipes are optional; vertical scrolling and controls inside a page remain usable.
 */
public class NavigationPageContainer extends FrameLayout {
    public interface OnSwipeListener { void onSwipe(int direction); }

    private final int touchSlop;
    private OnSwipeListener swipeListener;
    private boolean swipeEnabled = true;
    private float downX;
    private float downY;
    private int currentPageId = View.NO_ID;

    public NavigationPageContainer(Context context) { this(context, null); }
    public NavigationPageContainer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setSwipeEnabled(boolean enabled) { swipeEnabled = enabled; }
    public boolean isSwipeEnabled() { return swipeEnabled; }
    public int getCurrentPageId() { return currentPageId; }
    public boolean hasPage(int id) { return findViewById(id) != null; }
    public void setOnSwipeListener(@Nullable OnSwipeListener listener) { swipeListener = listener; }

    /** Opens a direct child page and slides the previous page away when animated. */
    public void showPage(int id, boolean animate) {
        View next = findViewById(id);
        if (next == null || next.getParent() != this)
            throw new IllegalArgumentException("No direct child page with destination ID " + id);
        if (currentPageId == id) return;
        View previous = currentPageId == View.NO_ID ? null : findViewById(currentPageId);
        int direction = pageIndex(id) >= pageIndex(currentPageId) ? 1 : -1;
        currentPageId = id;
        for (int i = 0; i < getChildCount(); i++) {
            View page = getChildAt(i);
            page.animate().cancel();
            page.setTranslationX(0);
            page.setAlpha(1f);
            page.setVisibility(page == next || page == previous ? VISIBLE : GONE);
        }
        next.setVisibility(VISIBLE);
        if (!animate || previous == null || getWidth() == 0) {
            if (previous != null) previous.setVisibility(GONE);
            return;
        }
        float distance = getWidth();
        next.setTranslationX(direction * distance * 0.35f);
        next.setAlpha(0f);
        previous.animate().translationX(-direction * distance * 0.35f).alpha(0f)
                .setDuration(130).withEndAction(() -> previous.setVisibility(GONE)).start();
        next.animate().translationX(0).alpha(1f).setDuration(150)
                .setInterpolator(new OvershootInterpolator(0.4f)).start();
    }

    private int pageIndex(int id) {
        for (int i = 0; i < getChildCount(); i++) if (getChildAt(i).getId() == id) return i;
        return -1;
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!swipeEnabled || swipeListener == null) return super.onInterceptTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            downX = event.getX();
            downY = event.getY();
        } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            float dx = event.getX() - downX;
            float dy = event.getY() - downY;
            if (Math.abs(dx) > touchSlop * 2 && Math.abs(dx) > Math.abs(dy) * 1.4f)
                return true;
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!swipeEnabled || swipeListener == null) return super.onTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            float dx = event.getX() - downX;
            if (Math.abs(dx) > Math.max(touchSlop * 4, getWidth() * 0.18f))
                swipeListener.onSwipe(dx < 0 ? 1 : -1);
            return true;
        }
        return true;
    }
}
