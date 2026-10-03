package dev.oneuiproject.oneui.widget;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import dev.oneuiproject.oneui.design.R;

/** Attaches touch-transparent scroll edge fades without changing a page's view hierarchy. */
public final class ScrollEdgeFades {
    private final View scrollable;
    private final EdgeDrawable drawable = new EdgeDrawable();
    private final ViewTreeObserver.OnScrollChangedListener scrollListener = this::refresh;
    private final View.OnLayoutChangeListener layoutListener = (v, l, t, r, b, ol, ot, or, ob) -> refresh();
    private final View.OnAttachStateChangeListener attachListener = new View.OnAttachStateChangeListener() {
        @Override public void onViewAttachedToWindow(View v) {
            v.getViewTreeObserver().addOnScrollChangedListener(scrollListener);
            refresh();
        }
        @Override public void onViewDetachedFromWindow(View v) {
            if (v.getViewTreeObserver().isAlive())
                v.getViewTreeObserver().removeOnScrollChangedListener(scrollListener);
        }
    };
    private int topHeight;
    private int bottomHeight;
    private int color;
    private boolean topAllowed = true;
    private boolean bottomAllowed = true;

    private ScrollEdgeFades(View view) {
        scrollable = view;
        topHeight = bottomHeight = Math.round(56 * view.getResources().getDisplayMetrics().density);
        color = resolveSurfaceColor(view);
        view.getOverlay().add(drawable);
        if (view.isAttachedToWindow())
            view.getViewTreeObserver().addOnScrollChangedListener(scrollListener);
        view.addOnAttachStateChangeListener(attachListener);
        view.addOnLayoutChangeListener(layoutListener);
        BlurSettings.observe(view, this::refresh);
        refresh();
    }

    public static ScrollEdgeFades attach(@NonNull View scrollable) {
        ScrollEdgeFades fades = (ScrollEdgeFades) scrollable.getTag(R.id.oui_scroll_edge_fades);
        if (fades == null) {
            fades = new ScrollEdgeFades(scrollable);
            scrollable.setTag(R.id.oui_scroll_edge_fades, fades);
        }
        return fades;
    }

    /** Opt a particular scrolling view out of automatic edge fades. */
    public static void setAutoAttachEnabled(@NonNull View scrollable, boolean enabled) {
        scrollable.setTag(R.id.oui_scroll_edge_fades_disabled, !enabled);
        ScrollEdgeFades existing = (ScrollEdgeFades) scrollable.getTag(R.id.oui_scroll_edge_fades);
        if (!enabled && existing != null) existing.detach();
    }

    private static boolean shouldAutoAttach(View view) {
        return !(view.getParent() instanceof ProgressiveBlurLayout)
                && !Boolean.TRUE.equals(view.getTag(R.id.oui_scroll_edge_fades_disabled));
    }

    /** Installs on every vertical scrolling descendant, including newly visible pages. */
    public static void attachTree(@NonNull View root) {
        if (root instanceof RecyclerView || root instanceof NestedScrollView
                || root instanceof ScrollView || root instanceof AbsListView) {
            if (shouldAutoAttach(root)) attach(root);
            return;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) attachTree(group.getChildAt(i));
        }
    }

    /** Applies one surface color to every scrolling descendant of a custom panel. */
    public static void attachTree(@NonNull View root, int surfaceColor) {
        if (root instanceof RecyclerView || root instanceof NestedScrollView
                || root instanceof ScrollView || root instanceof AbsListView) {
            if (shouldAutoAttach(root))
                attach(root).setColor(surfaceColor);
            return;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++)
                attachTree(group.getChildAt(i), surfaceColor);
        }
    }

    /** Uses the nearest solid view background, falling back to the screen surface. */
    public static int resolveSurfaceColor(@NonNull View view) {
        View current = view;
        while (current != null) {
            Drawable background = current.getBackground();
            Integer color = null;
            if (background instanceof ColorDrawable)
                color = ((ColorDrawable) background).getColor();
            else if (background instanceof GradientDrawable) {
                ColorStateList colors = ((GradientDrawable) background).getColor();
                if (colors != null) color = colors.getDefaultColor();
            }
            if (color != null && (color >>> 24) != 0)
                return color | 0xff000000;
            current = current.getParent() instanceof View ? (View) current.getParent() : null;
        }
        return view.getContext().getColor(R.color.oui_background_color);
    }

    public ScrollEdgeFades setHeights(int topPixels, int bottomPixels) {
        topHeight = Math.max(0, topPixels);
        bottomHeight = Math.max(0, bottomPixels);
        refresh();
        return this;
    }
    public ScrollEdgeFades setEdges(boolean top, boolean bottom) {
        topAllowed = top;
        bottomAllowed = bottom;
        refresh();
        return this;
    }
    public ScrollEdgeFades setColor(int fadeColor) { color = fadeColor; refresh(); return this; }
    public void detach() {
        scrollable.getOverlay().remove(drawable);
        if (scrollable.getViewTreeObserver().isAlive())
            scrollable.getViewTreeObserver().removeOnScrollChangedListener(scrollListener);
        scrollable.removeOnLayoutChangeListener(layoutListener);
        scrollable.removeOnAttachStateChangeListener(attachListener);
        scrollable.setTag(R.id.oui_scroll_edge_fades, null);
    }

    private void refresh() {
        drawable.setBounds(0, 0, scrollable.getWidth(), scrollable.getHeight());
        drawable.invalidateSelf();
    }

    private final class EdgeDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        @Override public void draw(@NonNull Canvas canvas) {
            if (!BlurSettings.isEnabled(scrollable.getContext())) return;
            int width = getBounds().width();
            int height = getBounds().height();
            if (width == 0 || height == 0) return;
            if (topAllowed && topHeight > 0 && scrollable.canScrollVertically(-1))
                drawEdge(canvas, 0, Math.min(topHeight, height), true, width);
            if (bottomAllowed && bottomHeight > 0 && scrollable.canScrollVertically(1)) {
                int size = Math.min(bottomHeight, height);
                drawEdge(canvas, height - size, size, false, width);
            }
        }
        private void drawEdge(Canvas canvas, int y, int size, boolean top, int width) {
            int clear = color & 0x00ffffff;
            paint.setShader(new LinearGradient(0, y, 0, y + size,
                    top ? color : clear, top ? clear : color, Shader.TileMode.CLAMP));
            canvas.drawRect(0, y, width, y + size, paint);
            paint.setShader(null);
        }
        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(ColorFilter filter) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
