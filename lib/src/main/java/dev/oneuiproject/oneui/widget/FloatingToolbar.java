package dev.oneuiproject.oneui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import dev.oneuiproject.oneui.design.R;

/** Compact floating action toolbar; each item invokes an independent command. */
public class FloatingToolbar extends LinearLayout {
    public interface OnActionClickListener {
        void onActionClick(int actionId);
    }

    private static final int MAX_ACTIONS = 5;
    private final List<ActionItem> actions = new ArrayList<>();
    private OnActionClickListener listener;
    private boolean showLabels = true;
    private boolean lightSurface;
    private boolean blurEnabled;
    private boolean animationsEnabled = true;
    private boolean capturingBackdrop;
    private Bitmap backdrop;
    private int[] pixels;
    private int[] blurred;
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint surfacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clipPath = new Path();

    public FloatingToolbar(Context context) { this(context, null); }
    public FloatingToolbar(Context context, AttributeSet attrs) { this(context, attrs, 0); }

    public FloatingToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        lightSurface = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                != Configuration.UI_MODE_NIGHT_YES;
        int menuRes = 0;
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FloatingToolbar,
                    defStyleAttr, 0);
            showLabels = a.getBoolean(R.styleable.FloatingToolbar_toolbarShowLabels, true);
            lightSurface = a.getBoolean(R.styleable.FloatingToolbar_toolbarLightSurface, lightSurface);
            menuRes = a.getResourceId(R.styleable.FloatingToolbar_toolbarMenu, 0);
            a.recycle();
        }
        setOrientation(HORIZONTAL);
        setWillNotDraw(false);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(dp(4), dp(4), dp(4), dp(4));
        setClipToOutline(true);
        setElevation(dp(8));
        updateColors();
        BlurSettings.observe(this, () -> { updateColors(); invalidate(); });
        if (menuRes != 0) inflateMenu(menuRes);
    }

    /** Inflates up to five visible menu items, using their IDs for click callbacks. */
    public void inflateMenu(@MenuRes int menuRes) {
        PopupMenu popup = new PopupMenu(getContext(), this);
        popup.getMenuInflater().inflate(menuRes, popup.getMenu());
        clearActions();
        Menu menu = popup.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.isVisible()) addAction(item.getItemId(), item.getIcon(), item.getTitle());
        }
    }

    public void clearActions() { actions.clear(); removeAllViews(); }

    public void addAction(int id, @DrawableRes int iconRes, @NonNull CharSequence title) {
        addAction(id, getContext().getDrawable(iconRes), title);
    }

    public void addAction(int id, @Nullable Drawable icon, @NonNull CharSequence title) {
        if (actions.size() == MAX_ACTIONS) throw new IllegalStateException("Maximum five toolbar actions");
        if (id == View.NO_ID) throw new IllegalArgumentException("Toolbar actions need stable IDs");
        for (ActionItem item : actions) {
            if (item.id == id) throw new IllegalArgumentException("Duplicate toolbar action ID");
        }
        ActionItem item = new ActionItem(id, icon, title);
        actions.add(item);
        addView(item.view, new LayoutParams(0, LayoutParams.MATCH_PARENT, 1));
        updateColors();
    }

    public void setOnActionClickListener(@Nullable OnActionClickListener callback) { listener = callback; }

    public void setShowLabels(boolean show) {
        showLabels = show;
        for (ActionItem item : actions) item.label.setVisibility(show ? VISIBLE : GONE);
    }

    public void setLightSurface(boolean light) { lightSurface = light; updateColors(); }
    public void setBlurEnabled(boolean enabled) { blurEnabled = enabled; updateColors(); invalidate(); }
    public boolean isBlurEnabled() { return blurEnabled; }
    private boolean isBlurActive() {
        return blurEnabled && BlurSettings.isEnabled(getContext());
    }
    public void setAnimationsEnabled(boolean enabled) { animationsEnabled = enabled; }
    public boolean isAnimationsEnabled() { return animationsEnabled; }

    private void updateColors() {
        int backgroundColor = color(lightSurface
                ? R.color.oui_floating_toolbar_light : R.color.oui_floating_toolbar_dark);
        int foregroundColor = color(lightSurface
                ? R.color.oui_floating_toolbar_light_text : R.color.oui_floating_toolbar_dark_text);
        GradientDrawable background = new GradientDrawable();
        background.setColor(isBlurActive() ? Color.TRANSPARENT : backgroundColor);
        background.setCornerRadius(dp(32));
        setBackground(background);
        SurfaceShadow.apply(this, lightSurface, dp(8), dp(32));
        for (ActionItem item : actions) {
            item.icon.setColorFilter(foregroundColor);
            item.label.setTextColor(foregroundColor);
        }
    }

    @Override public void draw(Canvas canvas) {
        if (!capturingBackdrop) super.draw(canvas);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isBlurActive() || isInEditMode() || getWidth() == 0 || getHeight() == 0) return;
        captureBackdrop();
        if (backdrop == null) return;
        int save = canvas.save();
        clipPath.reset();
        clipPath.addRoundRect(0, 0, getWidth(), getHeight(), getHeight() / 2f,
                getHeight() / 2f, Path.Direction.CW);
        canvas.clipPath(clipPath);
        canvas.drawBitmap(backdrop, null, new Rect(0, 0, getWidth(), getHeight()), bitmapPaint);
        surfacePaint.setColor(color(lightSurface ? R.color.oui_floating_toolbar_glass_light
                : R.color.oui_floating_toolbar_glass_dark));
        canvas.drawRect(0, 0, getWidth(), getHeight(), surfacePaint);
        canvas.restoreToCount(save);
    }

    private void captureBackdrop() {
        FloatingBarSwitcher switcher = getParent() instanceof FloatingBarSwitcher
                ? (FloatingBarSwitcher) getParent() : null;
        View source = switcher != null && switcher.getBackdropView() != null
                ? switcher.getBackdropView() : getRootView();
        int width = Math.max(1, (getWidth() + 5) / 6);
        int height = Math.max(1, (getHeight() + 5) / 6);
        if (backdrop == null || backdrop.getWidth() != width || backdrop.getHeight() != height) {
            if (backdrop != null) backdrop.recycle();
            backdrop = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            pixels = new int[width * height];
            blurred = new int[width * height];
        }
        backdrop.eraseColor(Color.TRANSPARENT);
        Canvas capture = new Canvas(backdrop);
        capture.scale((float) width / getWidth(), (float) height / getHeight());
        int[] position = new int[2];
        int[] hostPosition = new int[2];
        getLocationInWindow(position);
        source.getLocationInWindow(hostPosition);
        capture.translate(-(position[0] - hostPosition[0]),
                -(position[1] - hostPosition[1]));
        capturingBackdrop = true;
        if (switcher != null) switcher.setBackdropCapture(true);
        try { source.draw(capture); } finally {
            if (switcher != null) switcher.setBackdropCapture(false);
            capturingBackdrop = false;
        }
        backdrop.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int a = 0, r = 0, g = 0, b = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    int y = Math.max(0, Math.min(height - 1, row + dy));
                    for (int dx = -1; dx <= 1; dx++) {
                        int x = Math.max(0, Math.min(width - 1, col + dx));
                        int sample = pixels[y * width + x];
                        a += sample >>> 24;
                        r += sample >> 16 & 255;
                        g += sample >> 8 & 255;
                        b += sample & 255;
                    }
                }
                blurred[row * width + col] = (a / 9 << 24) | (r / 9 << 16)
                        | (g / 9 << 8) | b / 9;
            }
        }
        backdrop.setPixels(blurred, 0, width, 0, 0, width, height);
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (backdrop != null) { backdrop.recycle(); backdrop = null; }
    }

    private class ActionItem {
        final int id;
        final LinearLayout view;
        final ImageView icon;
        final TextView label;

        ActionItem(int id, Drawable drawable, CharSequence title) {
            this.id = id;
            view = new LinearLayout(getContext());
            view.setOrientation(VERTICAL);
            view.setGravity(Gravity.CENTER);
            view.setContentDescription(title);
            view.setClickable(true);
            view.setFocusable(true);
            GradientDrawable mask = new GradientDrawable();
            mask.setColor(Color.WHITE);
            mask.setCornerRadius(dp(28));
            view.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33000000), null, mask));
            view.setOnClickListener(v -> {
                if (animationsEnabled) {
                    v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(100)
                            .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f)
                                    .setDuration(180).withEndAction(() -> notifyAction(id)).start())
                            .start();
                } else notifyAction(id);
            });
            icon = new ImageView(getContext());
            icon.setImageDrawable(drawable);
            icon.setVisibility(drawable == null ? GONE : VISIBLE);
            view.addView(icon, new LayoutParams(dp(22), dp(22)));
            label = new TextView(getContext());
            label.setText(title);
            label.setSingleLine(true);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            label.setTextSize(10);
            label.setGravity(Gravity.CENTER);
            label.setVisibility(showLabels ? VISIBLE : GONE);
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(2);
            view.addView(label, lp);
        }
    }

    private void notifyAction(int id) { if (listener != null) listener.onActionClick(id); }

    private int color(int id) { return getResources().getColor(id, getContext().getTheme()); }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
