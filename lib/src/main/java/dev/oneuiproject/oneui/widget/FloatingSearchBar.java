package dev.oneuiproject.oneui.widget;

import android.content.Context;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.Outline;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import dev.oneuiproject.oneui.design.R;

/** Bottom floating search surface. Its listener owns filtering and any page action visibility. */
public class FloatingSearchBar extends FrameLayout {
    public interface Listener {
        void onQueryChanged(String query);
        default void onQuerySubmitted(String query) { }
        default void onVisibilityChanged(boolean visible) { }
    }

    private final GlassSurfaceView surface;
    private final ImageView searchIcon;
    private final EditText input;
    private final ImageButton close;
    private Listener listener;
    private Runnable closeRequested;
    private boolean open;
    private boolean suppressTextCallback;
    private boolean persistent;
    private int openDuration = 180;
    private int closeDuration = 130;

    public FloatingSearchBar(Context context) { this(context, null); }
    public FloatingSearchBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        int minHeight = dp(56);
        setMinimumHeight(minHeight);
        setClipChildren(false);
        setClipToPadding(false);
        setVisibility(GONE);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(28));
            }
        });
        setElevation(dp(10));
        surface = new GlassSurfaceView(context);
        surface.setCornerRadius(dp(28));
        addView(surface, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        addView(row, new LayoutParams(LayoutParams.MATCH_PARENT, minHeight));

        searchIcon = new ImageView(context);
        searchIcon.setImageResource(R.drawable.oui_ic_search);
        searchIcon.setContentDescription(null);
        int primary = context.getColor(R.color.oui_primary_text_color);
        ColorStateList iconTint = ColorStateList.valueOf(primary);
        searchIcon.setImageTintList(iconTint);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(24), dp(24));
        iconParams.leftMargin = dp(20);
        iconParams.rightMargin = dp(12);
        row.addView(searchIcon, iconParams);

        input = new EditText(context);
        input.setSingleLine(true);
        input.setTextSize(16);
        input.setTextColor(primary);
        input.setHintTextColor(context.getColor(R.color.oui_floating_nav_secondary));
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        input.setHint("Search");
        row.addView(input, new LinearLayout.LayoutParams(0, dp(48), 1));
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!suppressTextCallback && listener != null) listener.onQueryChanged(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return false;
            if (listener != null) listener.onQuerySubmitted(input.getText().toString());
            return true;
        });

        close = new ImageButton(context);
        close.setImageResource(R.drawable.oui_ic_close);
        close.setImageTintList(iconTint);
        close.setContentDescription("Close search");
        TypedValue ripple = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless,
                ripple, true);
        if (ripple.resourceId != 0) close.setBackgroundResource(ripple.resourceId);
        close.setOnClickListener(v -> {
            if (closeRequested != null) closeRequested.run();
            else close();
        });
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        closeParams.leftMargin = dp(4);
        closeParams.rightMargin = dp(8);
        row.addView(close, closeParams);
    }

    public void setListener(@Nullable Listener callback) { listener = callback; }
    /** Keeps the full search field visible as an embedded or floating page widget. */
    public void setPersistent(boolean enabled) {
        persistent = enabled;
        animate().cancel();
        open = enabled;
        setVisibility(enabled ? VISIBLE : GONE);
        setAlpha(1f);
        setTranslationY(0);
        setScaleX(1f);
        setScaleY(1f);
        if (!enabled) setQuery("", false);
    }
    public boolean isPersistent() { return persistent; }
    public void setOnCloseRequested(@Nullable Runnable callback) { closeRequested = callback; }
    public void setSourceView(@Nullable android.view.View source) { surface.setSourceView(source); }
    public GlassSurfaceView getSurface() { return surface; }
    public EditText getInput() { return input; }
    public ImageButton getCloseButton() { return close; }
    public ImageView getSearchIcon() { return searchIcon; }
    public void setHint(CharSequence hint) { input.setHint(hint); }
    public void setSearchIcon(@Nullable Drawable icon) { searchIcon.setImageDrawable(icon); }
    public void setCloseIcon(@Nullable Drawable icon) { close.setImageDrawable(icon); }
    public void setAnimationDurations(int openMillis, int closeMillis) {
        openDuration = Math.max(0, openMillis);
        closeDuration = Math.max(0, closeMillis);
    }
    public boolean isOpen() { return open; }
    public String getQuery() { return input.getText().toString(); }
    public void setQuery(@Nullable CharSequence query, boolean notify) {
        suppressTextCallback = !notify;
        input.setText(query == null ? "" : query);
        input.setSelection(input.length());
        suppressTextCallback = false;
    }

    public void open(@Nullable CharSequence previousQuery) {
        if (isOpen()) return;
        open = true;
        setQuery(previousQuery, false);
        animate().cancel();
        setVisibility(VISIBLE);
        setAlpha(0f);
        setTranslationY(dp(28));
        setScaleX(.94f);
        setScaleY(.94f);
        animate().alpha(1f).translationY(0).scaleX(1f).scaleY(1f)
                .setDuration(openDuration).withEndAction(() -> {
                    input.requestFocus();
                    ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                            .showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
                }).start();
        if (listener != null) listener.onVisibilityChanged(true);
    }

    public void close() {
        if (!isOpen()) return;
        if (persistent) {
            ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(input.getWindowToken(), 0);
            input.clearFocus();
            setQuery("", true);
            return;
        }
        open = false;
        animate().cancel();
        ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(input.getWindowToken(), 0);
        input.clearFocus();
        setQuery("", false);
        if (listener != null) {
            listener.onQueryChanged("");
            listener.onVisibilityChanged(false);
        }
        animate().alpha(0f).translationY(dp(20)).scaleX(.96f).scaleY(.96f)
                .setDuration(closeDuration).withEndAction(() -> setVisibility(GONE)).start();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
