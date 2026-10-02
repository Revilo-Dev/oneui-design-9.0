package dev.oneuiproject.oneui.widget;

import android.content.res.Configuration;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.appbar.AppBarLayout;
import java.util.IdentityHashMap;
import java.util.Map;

/** Styles the toolbar's actual control groups when content scrolls below them. */
public final class StickyToolbarControls {
    private final AppBarLayout appBar;
    private final Toolbar toolbar;
    private final View content;
    private final Drawable toolbarBackground;
    private View navigation;
    private ActionMenuView actions;
    private Drawable navigationBackground;
    private Drawable actionsBackground;
    private TopActionSurfaceDrawable navigationSurface;
    private TopActionSurfaceDrawable actionsSurface;
    private int navPaddingLeft, navPaddingTop, navPaddingRight, navPaddingBottom;
    private int navigationWidth, navigationHeight;
    private int navigationMarginStart, navigationMarginEnd;
    private float navigationTranslationY;
    private int actionPaddingLeft, actionPaddingTop, actionPaddingRight, actionPaddingBottom;
    private int actionsMarginStart, actionsMarginEnd;
    private float actionsTranslationY;
    private boolean enabled = true;
    private boolean active;
    private boolean blurEnabled;
    private boolean requireContentScroll = true;
    private int offset;
    private int horizontalInset;
    private int verticalOffset;
    private int collapseTolerance;
    private int itemWidth;
    private int surfacePadding;
    private int cornerRadius;
    private float shadowElevation;
    private Integer tintColor;
    private final Map<View, int[]> originalActionSizes = new IdentityHashMap<>();

    public StickyToolbarControls(AppBarLayout appBar, Toolbar toolbar, View content) {
        this.appBar = appBar;
        this.toolbar = toolbar;
        this.content = content;
        toolbarBackground = toolbar.getBackground();
        horizontalInset = dp(16);
        verticalOffset = dp(6);
        collapseTolerance = dp(2);
        itemWidth = dp(48);
        surfacePadding = dp(3);
        cornerRadius = dp(25);
        shadowElevation = dp(5);
        toolbar.setClipChildren(false);
        toolbar.setClipToPadding(false);
        appBar.addOnOffsetChangedListener((bar, verticalOffset) -> {
            offset = verticalOffset;
            refresh();
        });
        toolbar.getViewTreeObserver().addOnGlobalLayoutListener(this::refresh);
        content.getViewTreeObserver().addOnScrollChangedListener(this::refresh);
        content.getViewTreeObserver().addOnGlobalLayoutListener(this::refresh);
    }

    public void setEnabled(boolean value) { enabled = value; refresh(); }
    public boolean isEnabled() { return enabled; }
    public void setRequireContentScroll(boolean value) { requireContentScroll = value; refresh(); }
    public void setHorizontalInset(int pixels) { horizontalInset = Math.max(0, pixels); refresh(); }
    public void setVerticalOffset(int pixels) { verticalOffset = pixels; refresh(); }
    public void setCollapseTolerance(int pixels) { collapseTolerance = Math.max(0, pixels); refresh(); }
    public void setBlurEnabled(boolean value) {
        blurEnabled = value;
        if (navigationSurface != null) navigationSurface.setBlurEnabled(value);
        if (actionsSurface != null) actionsSurface.setBlurEnabled(value);
    }
    public boolean isBlurEnabled() { return blurEnabled; }
    public boolean isContentScrolled() { return hasScrolledContent(content); }
    public void setShadowElevation(float pixels) { shadowElevation = Math.max(0, pixels); refresh(); }
    public void setTintColor(@Nullable Integer color) {
        tintColor = color;
        if (navigationSurface != null) navigationSurface.setTintColor(color);
        if (actionsSurface != null) actionsSurface.setTintColor(color);
    }
    /** Sets each icon's equal-width touch target. */
    public void setItemWidth(int pixels) { itemWidth = Math.max(dp(40), pixels); refresh(); }
    public void setSurfacePadding(int pixels) { surfacePadding = Math.max(0, pixels); refresh(); }
    public void setCornerRadius(int pixels) {
        cornerRadius = Math.max(0, pixels);
        if (navigationSurface != null) navigationSurface.setRadius(cornerRadius);
        if (actionsSurface != null) actionsSurface.setRadius(cornerRadius);
        if (navigation != null) navigation.invalidateOutline();
        if (actions != null) actions.invalidateOutline();
    }
    @Nullable public View getNavigationControl() { return navigation; }
    @Nullable public ActionMenuView getActionsControl() { return actions; }

    public void refresh() {
        boolean shouldShow = enabled && appBar.getTotalScrollRange() > 0
                && Math.abs(offset) >= appBar.getTotalScrollRange() - collapseTolerance
                && (!requireContentScroll || hasScrolledContent(content));
        if (shouldShow != active) {
            active = shouldShow;
            toolbar.setBackground(active ? null : toolbarBackground);
            toolbar.setContentInsetsRelative(0, 0);
        }
        findControls();
        styleNavigation();
        styleActions();
        if (active) {
            if (navigation != null) navigation.invalidate();
            if (actions != null) actions.invalidate();
        }
    }

    private void findControls() {
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View child = toolbar.getChildAt(i);
            if (child instanceof AppCompatImageButton && child != navigation) {
                navigation = child;
                navigationBackground = child.getBackground();
                navPaddingLeft = child.getPaddingLeft();
                navPaddingTop = child.getPaddingTop();
                navPaddingRight = child.getPaddingRight();
                navPaddingBottom = child.getPaddingBottom();
                navigationWidth = child.getLayoutParams().width;
                navigationHeight = child.getLayoutParams().height;
                ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
                navigationMarginStart = margins.getMarginStart();
                navigationMarginEnd = margins.getMarginEnd();
                navigationTranslationY = child.getTranslationY();
                navigationSurface = new TopActionSurfaceDrawable(child, content, cornerRadius);
                navigationSurface.setBlurEnabled(blurEnabled);
                navigationSurface.setTintColor(tintColor);
            } else if (child instanceof ActionMenuView && child != actions) {
                actions = (ActionMenuView) child;
                actionsBackground = child.getBackground();
                actionPaddingLeft = child.getPaddingLeft();
                actionPaddingTop = child.getPaddingTop();
                actionPaddingRight = child.getPaddingRight();
                actionPaddingBottom = child.getPaddingBottom();
                ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
                actionsMarginStart = margins.getMarginStart();
                actionsMarginEnd = margins.getMarginEnd();
                actionsTranslationY = child.getTranslationY();
                actionsSurface = new TopActionSurfaceDrawable(child, content, cornerRadius);
                actionsSurface.setBlurEnabled(blurEnabled);
                actionsSurface.setTintColor(tintColor);
            }
        }
    }

    private void styleNavigation() {
        if (navigation == null) return;
        boolean show = active && toolbar.getNavigationIcon() != null;
        Drawable background = show ? navigationSurface : navigationBackground;
        if (navigation.getBackground() != background) navigation.setBackground(background);
        int left = show ? surfacePadding : navPaddingLeft;
        int top = show ? surfacePadding : navPaddingTop;
        int right = show ? surfacePadding : navPaddingRight;
        int bottom = show ? surfacePadding : navPaddingBottom;
        if (navigation.getPaddingLeft() != left || navigation.getPaddingTop() != top
                || navigation.getPaddingRight() != right || navigation.getPaddingBottom() != bottom)
            navigation.setPadding(left, top, right, bottom);
        ViewGroup.LayoutParams params = navigation.getLayoutParams();
        int size = itemWidth + surfacePadding * 2;
        int width = show ? size : navigationWidth;
        int height = show ? size : navigationHeight;
        if (params.width != width || params.height != height) {
            params.width = width;
            params.height = height;
            navigation.setLayoutParams(params);
        }
        setMargins(navigation, show, navigationMarginStart, navigationMarginEnd);
        navigation.setTranslationY(navigationTranslationY + (show ? verticalOffset : 0));
        updateOutline(navigation, show);
    }

    private void styleActions() {
        if (actions == null) return;
        int count = 0;
        for (int i = 0; i < actions.getChildCount(); i++)
            if (actions.getChildAt(i).getVisibility() == View.VISIBLE) count++;
        boolean show = active && count > 0;
        Drawable background = show ? actionsSurface : actionsBackground;
        if (actions.getBackground() != background) actions.setBackground(background);
        int left = show ? surfacePadding : actionPaddingLeft;
        int top = show ? surfacePadding : actionPaddingTop;
        int right = show ? surfacePadding : actionPaddingRight;
        int bottom = show ? surfacePadding : actionPaddingBottom;
        if (actions.getPaddingLeft() != left || actions.getPaddingTop() != top
                || actions.getPaddingRight() != right || actions.getPaddingBottom() != bottom)
            actions.setPadding(left, top, right, bottom);
        if (show) {
            for (int i = 0; i < actions.getChildCount(); i++) {
                View child = actions.getChildAt(i);
                if (child.getVisibility() != View.VISIBLE) continue;
                ViewGroup.LayoutParams params = child.getLayoutParams();
                if (!originalActionSizes.containsKey(child))
                    originalActionSizes.put(child, new int[]{params.width, child.getMinimumWidth()});
                if (params.width != itemWidth) {
                    params.width = itemWidth;
                    child.setLayoutParams(params);
                }
                child.setMinimumWidth(itemWidth);
            }
        } else if (!originalActionSizes.isEmpty()) {
            for (Map.Entry<View, int[]> entry : originalActionSizes.entrySet()) {
                View child = entry.getKey();
                int[] size = entry.getValue();
                ViewGroup.LayoutParams childParams = child.getLayoutParams();
                if (childParams != null && childParams.width != size[0]) {
                    childParams.width = size[0];
                    child.setLayoutParams(childParams);
                }
                child.setMinimumWidth(size[1]);
            }
            originalActionSizes.clear();
        }
        ViewGroup.LayoutParams params = actions.getLayoutParams();
        if (params.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            actions.setLayoutParams(params);
        }
        setMargins(actions, show, actionsMarginStart, actionsMarginEnd);
        actions.setTranslationY(actionsTranslationY + (show ? verticalOffset : 0));
        updateOutline(actions, show);
    }

    private void setMargins(View view, boolean show, int originalStart, int originalEnd) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        int start = show ? horizontalInset : originalStart;
        int end = show ? horizontalInset : originalEnd;
        if (params.getMarginStart() != start || params.getMarginEnd() != end) {
            params.setMarginStart(start);
            params.setMarginEnd(end);
            view.setLayoutParams(params);
        }
    }

    private void updateOutline(View view, boolean show) {
        if (show) {
            view.setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View target, Outline outline) {
                    outline.setRoundRect(0, 0, target.getWidth(), target.getHeight(), cornerRadius);
                }
            });
            int mode = toolbar.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK;
            float elevation = mode == Configuration.UI_MODE_NIGHT_YES
                    ? Math.min(shadowElevation, dp(2)) : shadowElevation;
            if (view.getElevation() != elevation) view.setElevation(elevation);
        } else {
            if (view.getElevation() != 0) view.setElevation(0);
            view.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        }
    }

    private boolean hasScrolledContent(View view) {
        if (view.getVisibility() != View.VISIBLE) return false;
        if (view.canScrollVertically(-1)) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++)
                if (hasScrolledContent(group.getChildAt(i))) return true;
        }
        return false;
    }

    private int dp(float value) {
        return Math.round(value * toolbar.getResources().getDisplayMetrics().density);
    }
}
