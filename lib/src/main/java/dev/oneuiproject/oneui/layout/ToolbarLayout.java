package dev.oneuiproject.oneui.layout;

import android.app.SearchManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.PathInterpolator;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.IdRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import dev.oneuiproject.oneui.design.R;
import dev.oneuiproject.oneui.utils.internal.ToolbarLayoutUtils;
import dev.oneuiproject.oneui.view.internal.NavigationBadgeIcon;
import dev.oneuiproject.oneui.widget.FloatingSearchBar;
import dev.oneuiproject.oneui.widget.ScrollEdgeFades;
import dev.oneuiproject.oneui.widget.StickyToolbarControls;

/**
 * Collapsing app bar with pinned actions, floating search, and action mode.
 */
public class ToolbarLayout extends LinearLayout {
    private static final String TAG = "ToolbarLayout";


    public static final int AMT_GROUP_MENU_ID = 9999;
    private int mAMTMenuShowAlwaysMax = 2;
    private boolean switchActionModeMenu = false;
    private int mSelectedItemsCount = 0;

    public interface ActionModeCallback {
        void onShow(ToolbarLayout toolbarLayout);
        void onDismiss(ToolbarLayout toolbarLayout);
    }

    private ActionModeCallback mActionModeCallback;

    private static final int MAIN_CONTENT = 0;
    private static final int APPBAR_HEADER = 1;
    private static final int FOOTER = 2;
    private static final int ROOT = 3;

    public static final int N_BADGE = -1;

    protected AppCompatActivity mActivity;
    protected Context mContext;

    private final OnBackPressedCallback mOnBackPressedCallback
            = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            if (mIsSearchMode) dismissSearchMode();
            if (mIsActionMode) dismissActionMode();
        }
    };
    private AppBarOffsetListener mActionModeTitleFadeListener = new AppBarOffsetListener();

    protected int mLayout;
    protected boolean mExpandable;
    protected boolean mExpanded;
    protected Drawable mNavigationIcon;
    private LayerDrawable mNavigationBadgeIcon;
    protected CharSequence mTitleCollapsed;
    protected CharSequence mTitleExpanded;
    protected CharSequence mSubtitleCollapsed;
    protected CharSequence mSubtitleExpanded;

    private AppBarLayout mAppBarLayout;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    private Toolbar mMainToolbar;
    private Toolbar mActionModeToolbar;
    private CoordinatorLayout mCoordinatorLayout;
    protected FrameLayout mMainContainer;
    private FrameLayout mFooterContainer;
    private FrameLayout mStickyBottomHost;
    private FloatingSearchBar mFloatingSearchBar;
    private StickyToolbarControls mStickyControls;
    private StickyToolbarControls mActionModeStickyControls;
    private boolean mStickyTitleEnabled;
    private boolean mStickyActionsEnabled = true;
    private boolean mStickyActionBlurEnabled = true;
    private BottomNavigationView mBottomActionModeBar;

    private LinearLayout mActionModeSelectAll;
    private AppCompatCheckBox mActionModeCheckBox;
    private TextView mActionModeTitleTextView;

    private SearchModeListener mSearchModeListener;

    private boolean mIsSearchMode = false;
    private boolean mIsActionMode = false;

    /**
     * Callback for the floating search bar.
     *
     * @see #showSearchMode()
     * @see #dismissSearchMode()
     */
    public interface SearchModeListener {
        boolean onQueryTextSubmit(String query);

        boolean onQueryTextChange(String newText);

        void onSearchModeToggle(FloatingSearchBar searchBar, boolean visible);
    }

    public ToolbarLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mActivity = getActivity();
        mContext = context;

        setOrientation(VERTICAL);

        initLayoutAttrs(attrs);
        inflateChildren();
        initAppBar();

        if (!isInEditMode()) {
            mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            mActivity.getOnBackPressedDispatcher().addCallback(mOnBackPressedCallback);
        }

        refreshLayout(getResources().getConfiguration());
    }

    protected void initLayoutAttrs(@Nullable AttributeSet attrs) {
        TypedArray a = mContext.getTheme()
                .obtainStyledAttributes(
                        attrs, R.styleable.ToolbarLayout, 0, 0);
        try {
            mLayout = a.getResourceId(R.styleable.ToolbarLayout_android_layout,
                    R.layout.oui_layout_toolbarlayout_appbar);
            mExpandable = a.getBoolean(R.styleable.ToolbarLayout_expandable, true);
            mExpanded = a.getBoolean(R.styleable.ToolbarLayout_expanded, mExpandable);
            mNavigationIcon = a.getDrawable(R.styleable.ToolbarLayout_navigationIcon);
            mTitleExpanded
                    = mTitleCollapsed = a.getString(R.styleable.ToolbarLayout_title);
            mSubtitleExpanded = a.getString(R.styleable.ToolbarLayout_subtitle);
            mStickyTitleEnabled = a.getBoolean(R.styleable.ToolbarLayout_stickyTitleEnabled, false);
            mStickyActionsEnabled = a.getBoolean(R.styleable.ToolbarLayout_stickyActionsEnabled, true);
            mStickyActionBlurEnabled = a.getBoolean(R.styleable.ToolbarLayout_stickyActionBlurEnabled, true);
        } finally {
            a.recycle();
        }
    }

    protected void inflateChildren() {
        if (mLayout != R.layout.oui_layout_toolbarlayout_appbar) {
            Log.w(TAG, "Inflating custom " + TAG);
        }

        LayoutInflater inflater = LayoutInflater.from(mContext);
        inflater.inflate(mLayout, this, true);
        addView(inflater.inflate(
                R.layout.oui_layout_toolbarlayout_footer, this, false));
    }

    private void initAppBar() {
        mCoordinatorLayout = findViewById(R.id.toolbarlayout_coordinator_layout);
        mAppBarLayout = mCoordinatorLayout.findViewById(R.id.toolbarlayout_app_bar);
        mCollapsingToolbarLayout = mAppBarLayout.findViewById(R.id.toolbarlayout_collapsing_toolbar);
        mMainToolbar = mCollapsingToolbarLayout.findViewById(R.id.toolbarlayout_main_toolbar);
        mActionModeToolbar = mCollapsingToolbarLayout.findViewById(R.id.toolbarlayout_action_mode_toolbar);

        mActionModeSelectAll = mActionModeToolbar.findViewById(R.id.toolbarlayout_selectall);
        mActionModeCheckBox = mActionModeSelectAll.findViewById(R.id.toolbarlayout_selectall_checkbox);
        mActionModeTitleTextView = mActionModeToolbar.findViewById(R.id.toolbar_layout_action_mode_title);

        mActionModeSelectAll.setOnClickListener(
                view -> mActionModeCheckBox.setChecked(!mActionModeCheckBox.isChecked()));

        mMainContainer = findViewById(R.id.toolbarlayout_main_container);
        mFooterContainer = findViewById(R.id.toolbarlayout_footer_container);
        mStickyBottomHost = findViewById(R.id.toolbarlayout_sticky_bottom_host);
        mFloatingSearchBar = new FloatingSearchBar(mContext);
        CoordinatorLayout.LayoutParams searchParams = new CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        searchParams.gravity = android.view.Gravity.BOTTOM;
        searchParams.leftMargin = searchParams.rightMargin = dp(16);
        searchParams.bottomMargin = dp(16);
        mCoordinatorLayout.addView(mFloatingSearchBar, searchParams);
        mFloatingSearchBar.setSourceView(mMainContainer);
        mFloatingSearchBar.setOnCloseRequested(this::dismissSearchMode);
        mFloatingSearchBar.setListener(new FloatingSearchBar.Listener() {
            @Override public void onQueryChanged(String query) {
                if (mSearchModeListener != null) mSearchModeListener.onQueryTextChange(query);
            }
            @Override public void onQuerySubmitted(String query) {
                if (mSearchModeListener != null) mSearchModeListener.onQueryTextSubmit(query);
            }
            @Override public void onVisibilityChanged(boolean visible) {
                if (visible && mIsActionMode) dismissActionMode();
                mIsSearchMode = visible;
                mOnBackPressedCallback.setEnabled(visible || mIsActionMode);
                if (!mIsActionMode) mFooterContainer.setVisibility(visible ? GONE : VISIBLE);
                mStickyBottomHost.setVisibility(visible ? GONE : VISIBLE);
                if (mSearchModeListener != null)
                    mSearchModeListener.onSearchModeToggle(mFloatingSearchBar, visible);
            }
        });
        ViewCompat.setOnApplyWindowInsetsListener(mCoordinatorLayout, (view, insets) -> {
            int bottom = Math.max(insets.getInsets(WindowInsetsCompat.Type.ime()).bottom,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams)
                    mFloatingSearchBar.getLayoutParams();
            int margin = dp(16) + bottom;
            if (lp.bottomMargin != margin) {
                lp.bottomMargin = margin;
                mFloatingSearchBar.setLayoutParams(lp);
            }
            return insets;
        });
        mMainContainer.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            ScrollEdgeFades.attachTree(mMainContainer);
        });
        mCollapsingToolbarLayout.setContentScrimColor(Color.TRANSPARENT);
        mCollapsingToolbarLayout.setCollapsedTitleTextColor(Color.TRANSPARENT);
        mStickyControls = new StickyToolbarControls(mAppBarLayout, mMainToolbar, mMainContainer);
        mStickyControls.setEnabled(mStickyActionsEnabled);
        mStickyControls.setBlurEnabled(mStickyActionBlurEnabled);
        mActionModeStickyControls = new StickyToolbarControls(
                mAppBarLayout, mActionModeToolbar, mMainContainer);
        mActionModeStickyControls.setEnabled(mStickyActionsEnabled);
        mActionModeStickyControls.setBlurEnabled(mStickyActionBlurEnabled);
        mAppBarLayout.addOnOffsetChangedListener((bar, offset) -> updateStickyTitle());
        mMainContainer.getViewTreeObserver().addOnScrollChangedListener(this::updateStickyTitle);
        mBottomActionModeBar = findViewById(R.id.toolbarlayout_bottom_nav_view);

        if (!isInEditMode()) {
            mActivity.setSupportActionBar(mMainToolbar);
            mActivity.getSupportActionBar()
                    .setDisplayHomeAsUpEnabled(false);
            mActivity.getSupportActionBar()
                    .setDisplayShowTitleEnabled(false);

        }

        setNavigationButtonIcon(mNavigationIcon);
        setTitle(mTitleExpanded, mTitleCollapsed);
        setExpandedSubtitle(mSubtitleExpanded);

    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (mMainContainer == null || mFooterContainer == null) {
            super.addView(child, index, params);
        } else {
            switch (((ToolbarLayoutParams) params).layout_location) {
                default:
                case MAIN_CONTENT:
                    mMainContainer.addView(child, params);
                    break;
                case APPBAR_HEADER:
                    setCustomTitleView(child,
                            new CollapsingToolbarLayout.LayoutParams(params));
                    break;
                case FOOTER:
                    mFooterContainer.addView(child, params);
                    break;
                case ROOT:
                    mCoordinatorLayout
                            .addView(child, CLLPWrapper((LayoutParams) params));
                    break;
            }
        }
    }

    @Override
    public LayoutParams generateDefaultLayoutParams() {
        return new ToolbarLayoutParams(getContext(), null);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new ToolbarLayoutParams(getContext(), attrs);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        resetAppBar();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        refreshLayout(newConfig);
        resetAppBar();
        updateActionModeMenuVisibility(newConfig);
    }

    @Nullable
    private AppCompatActivity getActivity() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof AppCompatActivity) {
                return (AppCompatActivity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    private void refreshLayout(Configuration newConfig) {
        if (!isInEditMode())
            ToolbarLayoutUtils
                    .hideStatusBarForLandscape(mActivity, newConfig.orientation);

        ToolbarLayoutUtils.updateListBothSideMargin(mActivity,
                mMainContainer);
        ToolbarLayoutUtils.updateListBothSideMargin(mActivity,
                findViewById(R.id.toolbarlayout_bottom_corners));
        ToolbarLayoutUtils.updateListBothSideMargin(mActivity,
                findViewById(R.id.toolbarlayout_footer_content));

        final boolean isLandscape
                = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;

        setExpanded(!isLandscape & mExpanded);

        if (mNavigationBadgeIcon != null) {
            NavigationBadgeIcon badgeIcon
                    = (NavigationBadgeIcon) mNavigationBadgeIcon
                    .getDrawable(1);
            badgeIcon.setOrientation(isLandscape);
        }
    }

    private void resetAppBar() {
        if (mAppBarLayout != null) {
            if (mExpandable) {
                mAppBarLayout.setEnabled(true);
                mAppBarLayout.seslSetCustomHeightProportion(false, 0);
            } else {
                mAppBarLayout.setEnabled(false);
                mAppBarLayout.seslSetCustomHeight(mContext.getResources()
                        .getDimensionPixelSize(R.dimen.sesl_action_bar_height_with_padding));
            }
        } else
            Log.w(TAG, "resetAppBar: mAppBarLayout is null.");
    }

    //
    // AppBar methods
    //

    /**
     * Returns the {@link AppBarLayout}.
     */
    @NonNull
    public AppBarLayout getAppBarLayout() {
        return mAppBarLayout;
    }

    /** Controls whether the pinned toolbar gains glass action surfaces over scrolled content. */
    public void setStickyActionsEnabled(boolean enabled) {
        mStickyActionsEnabled = enabled;
        if (mStickyControls != null) mStickyControls.setEnabled(enabled);
        if (mActionModeStickyControls != null) mActionModeStickyControls.setEnabled(enabled);
    }

    public void setStickyActionBlurEnabled(boolean enabled) {
        mStickyActionBlurEnabled = enabled;
        if (mStickyControls != null) mStickyControls.setBlurEnabled(enabled);
        if (mActionModeStickyControls != null) mActionModeStickyControls.setBlurEnabled(enabled);
    }

    public void setStickyActionShadowElevation(float pixels) {
        if (mStickyControls != null) mStickyControls.setShadowElevation(pixels);
        if (mActionModeStickyControls != null)
            mActionModeStickyControls.setShadowElevation(pixels);
    }

    public void setStickyActionTintColor(@Nullable Integer color) {
        if (mStickyControls != null) mStickyControls.setTintColor(color);
        if (mActionModeStickyControls != null) mActionModeStickyControls.setTintColor(color);
    }

    /** Direct access lets screens add, remove, and configure any toolbar action. */
    public FloatingSearchBar getFloatingSearchBar() { return mFloatingSearchBar; }

    @Nullable
    public StickyToolbarControls getStickyToolbarControls() { return mStickyControls; }

    @Nullable
    public StickyToolbarControls getActionModeStickyControls() {
        return mActionModeStickyControls;
    }

    /** Keeps the compact title pinned with the actions. Disabled by default. */
    public void setStickyTitleEnabled(boolean enabled) {
        mStickyTitleEnabled = enabled;
        updateStickyTitle();
    }

    public boolean isStickyTitleEnabled() { return mStickyTitleEnabled; }

    private void updateStickyTitle() {
        if (mMainToolbar == null || mCollapsingToolbarLayout == null) return;
        boolean show = mAppBarLayout.seslIsCollapsed()
                && (mStickyTitleEnabled || mStickyControls == null
                        || !mStickyControls.isContentScrolled());
        CharSequence title = show ? mTitleCollapsed : null;
        CharSequence subtitle = show ? mSubtitleCollapsed : null;
        if (!TextUtils.equals(mMainToolbar.getTitle(), title)) mMainToolbar.setTitle(title);
        if (!TextUtils.equals(mMainToolbar.getSubtitle(), subtitle)) mMainToolbar.setSubtitle(subtitle);
    }

    /** Keeps a bottom bar at the viewport edge while the app bar and page scroll. */
    public void setStickyBottomBar(@Nullable View bar) {
        if (mStickyBottomHost == null) return;
        mStickyBottomHost.removeAllViews();
        if (bar == null) return;
        ViewGroup parent = (ViewGroup) bar.getParent();
        if (parent != null) parent.removeView(bar);
        mStickyBottomHost.addView(bar);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /**
     * Returns the {@link Toolbar}.
     */
    @NonNull
    public Toolbar getToolbar() {
        return mMainToolbar;
    }

    /**
     * Set the title of both the collapsed and expanded Toolbar.
     * The expanded title might not be visible in landscape or on devices with small dpi.
     */
    public void setTitle(@Nullable CharSequence title) {
        setTitle(title, title);
    }

    /**
     * Set the title of the collapsed and expanded Toolbar independently.
     * The expanded title might not be visible in landscape or on devices with small dpi.
     */
    public void setTitle(@Nullable CharSequence expandedTitle,
                         @Nullable CharSequence collapsedTitle) {
        mTitleCollapsed = collapsedTitle;
        mCollapsingToolbarLayout.setTitle(mTitleExpanded = expandedTitle);
        updateStickyTitle();
    }

    /**
     * Set the subtitle of the {@link CollapsingToolbarLayout}.
     * This might not be visible in landscape or on devices with small dpi.
     */
    public void setExpandedSubtitle(@Nullable CharSequence expandedSubtitle) {
        mCollapsingToolbarLayout.seslSetSubtitle(mSubtitleExpanded = expandedSubtitle);
    }

    /**
     * Set the subtitle of the collapsed Toolbar.
     */
    public void setCollapsedSubtitle(@Nullable CharSequence collapsedSubtitle) {
        mSubtitleCollapsed = collapsedSubtitle;
        updateStickyTitle();

    }

    /**
     * Enable or disable the expanding Toolbar functionality.
     * If you simply want to programmatically expand or collapse the toolbar.
     *
     * @see #setExpanded(boolean)
     */
    public void setExpandable(boolean expandable) {
        if (mExpandable != expandable) {
            mExpandable = expandable;
            resetAppBar();
        }
    }

    /**
     * Returns if the expanding Toolbar functionality is enabled or not.
     *
     * @see #setExpandable(boolean)
     */
    public boolean isExpandable() {
        return mExpandable;
    }

    /**
     * Programmatically expand or collapse the Toolbar.
     */
    public void setExpanded(boolean expanded) {
        setExpanded(expanded, ViewCompat.isLaidOut(mAppBarLayout));
    }

    /**
     * Programmatically expand or collapse the Toolbar with an optional animation.
     *
     * @param animate whether or not to animate the expanding or collapsing.
     */
    public void setExpanded(boolean expanded, boolean animate) {
        if (mExpandable) {
            mExpanded = expanded;
            mAppBarLayout.setExpanded(expanded, animate);
        } else
            Log.d(TAG, "setExpanded: mExpandable is " + mExpandable);
    }

    /**
     * Get the current state of the toolbar.
     *
     * @see #setExpanded(boolean)
     * @see #setExpanded(boolean, boolean)
     */
    public boolean isExpanded() {
        return mExpandable && !mAppBarLayout.seslIsCollapsed();
    }

    /**
     * Replace the title of the expanded Toolbar with a custom View.
     * This might not be visible in landscape or on devices with small dpi.
     */
    public void setCustomTitleView(@NonNull View view) {
        setCustomTitleView(view,
                new CollapsingToolbarLayout.LayoutParams(view.getLayoutParams()));
    }

    /**
     * Replace the title of the expanded Toolbar with a custom View including LayoutParams.
     * This might not be visible in landscape or on devices with small dpi.
     */
    public void setCustomTitleView(@NonNull View view,
                                   @Nullable CollapsingToolbarLayout.LayoutParams params) {
        if (params == null) {
            params = new CollapsingToolbarLayout
                    .LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        }
        params.seslSetIsTitleCustom(true);
        mCollapsingToolbarLayout.seslSetCustomTitleView(view, params);
    }

    /**
     * Replace the subtitle of the expanded Toolbar with a custom View.
     * This might not be visible in landscape or on devices with small dpi.
     */
    public void setCustomSubtitle(@NonNull View view) {
        mCollapsingToolbarLayout.seslSetCustomSubtitle(view);
    }

    /**
     * Enable or disable the immersive scroll of the Toolbar.
     * When this is enabled the Toolbar will completely hide when scrolling up.
     */
    public void setImmersiveScroll(boolean activate) {
        if (Build.VERSION.SDK_INT >= 30) {
            mAppBarLayout.seslSetImmersiveScroll(activate);
        } else {
            Log.e(TAG, "setImmersiveScroll: immersive scroll is available only on api 30 and above");
        }
    }

    /**
     * Returns true if the immersive scroll is enabled.
     *
     * @see #setImmersiveScroll(boolean)
     */
    public boolean isImmersiveScroll() {
        return mAppBarLayout.seslGetImmersiveScroll();
    }


    /**
     * Set the badge of a Toolbar MenuItem. Only use this for MenuItems which show as action! It won't work for overflow items.
     */
    public void setMenuItemBadgeText(@IdRes int id, String text) {
        for (int i = 0; i < mMainToolbar.getChildCount(); i++) {
            View v1 = mMainToolbar.getChildAt(i);
            if (v1 instanceof ActionMenuView) {
                ActionMenuView menuView = (ActionMenuView) v1;
                for (int j = 0; j < menuView.getChildCount(); j++) {
                    View v2 = menuView.getChildAt(j);

                    if (v2 instanceof ActionMenuItemView) {
                        ActionMenuItemView menuItemView = (ActionMenuItemView) v2;
                        if (menuItemView.getItemData().getItemId() == id) {

                            menuView.removeView(menuItemView);
                            FrameLayout fl = new FrameLayout(mContext);
                            fl.addView(menuItemView);

                            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            ViewGroup mBadgeBackground = (ViewGroup) inflater.inflate(androidx.appcompat.R.layout.sesl_action_menu_item_badge, fl, false);
                            TextView mBadgeText = (TextView) mBadgeBackground.getChildAt(0);
                            fl.addView(mBadgeBackground);

                            setMenuItemBadgeText(mBadgeBackground, mBadgeText, text);

                            menuView.addView(fl, j);
                            return;
                        }
                    } else if (v2 instanceof FrameLayout) {
                        FrameLayout fl = (FrameLayout) v2;
                        View v3 = fl.getChildAt(0);
                        if (v3 instanceof ActionMenuItemView && ((ActionMenuItemView) v3).getItemData().getItemId() == id) {
                            ViewGroup mBadgeBackground = (ViewGroup) fl.getChildAt(1);
                            TextView mBadgeText = (TextView) mBadgeBackground.getChildAt(0);
                            setMenuItemBadgeText(mBadgeBackground, mBadgeText, text);
                            return;
                        }
                    }
                }

                Log.e(TAG, "no MenuItem with id " + id);
                return;
            }
        }

        Log.e(TAG, "no ActionMenuView in Toolbar");
    }

    private void setMenuItemBadgeText(ViewGroup mBadgeBackground, TextView mBadgeText, String text) {
        mBadgeText.setText(text);
        mBadgeBackground.setVisibility(text == null || text.isEmpty() ? GONE : VISIBLE);
        if (text == null) return;
        ViewGroup.MarginLayoutParams lp = (MarginLayoutParams) mBadgeBackground.getLayoutParams();
        lp.setMarginEnd(0);
        lp.width = (int) (getResources().getDimension(androidx.appcompat.R.dimen.sesl_badge_default_width) + (text.length() * getResources().getDimension(androidx.appcompat.R.dimen.sesl_badge_additional_width)));
        mBadgeBackground.setLayoutParams(lp);
    }


    //
    // Navigation Button methods
    //

    /**
     * Set the navigation icon of the Toolbar.
     * Don't forget to also set a Tooltip with {@link #setNavigationButtonTooltip(CharSequence)}.
     */
    public void setNavigationButtonIcon(@Nullable Drawable icon) {
        mNavigationIcon = icon;
        if (mNavigationBadgeIcon != null) {
            mNavigationBadgeIcon.setDrawable(0, mNavigationIcon);
            mNavigationBadgeIcon.invalidateSelf();
            mMainToolbar.setNavigationIcon(mNavigationBadgeIcon);
        } else {
            mMainToolbar.setNavigationIcon(mNavigationIcon);
        }
    }

    /**
     * Change the visibility of the navigation button.
     */
    public void setNavigationButtonVisible(boolean visible) {
        if (mNavigationBadgeIcon != null) {
            mMainToolbar.setNavigationIcon(visible
                    ? mNavigationBadgeIcon : null);
        } else if (mNavigationIcon != null) {
            mMainToolbar.setNavigationIcon(visible
                    ? mNavigationIcon : null);
        } else {
            mActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(visible);
        }
    }

    /**
     * Add a badge to the navigation button.
     * The badge is small orange circle in the top right of the icon which contains text.
     * It can either be a 'N' or a number up to 99.
     *
     * @param count {@link #N_BADGE} to show a 'N', 0 to hide the badge or any number up to 99.
     */
    public void setNavigationButtonBadge(int count) {
        if (mNavigationIcon != null) {
            if (count != 0) {
                NavigationBadgeIcon badgeIcon;
                if (mNavigationBadgeIcon == null) {
                    badgeIcon = new NavigationBadgeIcon(mContext);
                    mNavigationBadgeIcon = new LayerDrawable(
                            new Drawable[]{mNavigationIcon, badgeIcon});
                } else {
                    badgeIcon = (NavigationBadgeIcon) mNavigationBadgeIcon
                            .getDrawable(1);
                }

                badgeIcon.setOrientation(getResources().getConfiguration()
                        .orientation == Configuration.ORIENTATION_LANDSCAPE);

                if (count == N_BADGE) {
                    badgeIcon.setText(mContext.getResources()
                            .getString(R.string.oui_new_badge_text));
                } else {
                    badgeIcon.setText(count > 99
                            ? "99" : String.valueOf(count));
                }

                mNavigationBadgeIcon.invalidateSelf();
                mMainToolbar.setNavigationIcon(mNavigationBadgeIcon);
            } else {
                mNavigationBadgeIcon = null;
                mMainToolbar.setNavigationIcon(mNavigationIcon);
            }
        } else
            Log.d(TAG, "setNavigationButtonBadge: no navigation icon" +
                    " has been set");
    }

    /**
     * Set the Tooltip of the navigation button.
     */
    public void setNavigationButtonTooltip(@Nullable CharSequence tooltipText) {
        mMainToolbar.setNavigationContentDescription(tooltipText);
    }

    /**
     * Callback for the navigation button click event.
     */
    public void setNavigationButtonOnClickListener(@Nullable OnClickListener listener) {
        mMainToolbar.setNavigationOnClickListener(listener);
    }

    /**
     * Sets the icon the a back icon, the tooltip to 'Navigate up' and calls {@link AppCompatActivity#onBackPressed()} when clicked.
     *
     * @see #setNavigationButtonIcon(Drawable)
     * @see #setNavigationButtonTooltip(CharSequence)
     * @see android.app.ActionBar#setDisplayHomeAsUpEnabled(boolean)
     */
    public void setNavigationButtonAsBack() {
        if (!isInEditMode()) {
            mActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            setNavigationButtonOnClickListener(v -> mActivity.onBackPressed());
        }
    }

    //
    // Search Mode methods
    //

    /**
     * Show floating search above the navigation bar or keyboard.
     */
    public void showSearchMode() {
        showSearchMode("");
    }

    public void showSearchMode(@Nullable CharSequence initialQuery) {
        if (mIsSearchMode) return;
        mFloatingSearchBar.open(initialQuery);
    }

    /**
     * Dismiss floating search and clear its active query.
     *
     * @see #showSearchMode()
     */
    public void dismissSearchMode() {
        if (!mIsSearchMode) return;
        mFloatingSearchBar.close();
    }

    /**
     * Check whether floating search is open.
     */
    public boolean isSearchMode() {
        return mIsSearchMode;
    }

    /**
     * Set the {@link SearchModeListener} for the Toolbar's SearchMode.
     */
    public void setSearchModeListener(SearchModeListener listener) {
        mSearchModeListener = listener;
    }

    /**
     * Forward a voice input result to the floating search field.
     */
    public void onSearchModeVoiceInputResult(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            mFloatingSearchBar.setQuery(intent.getStringExtra(SearchManager.QUERY), true);
        }
    }


    public void setActionModeToolbarShowAlwaysMax(int max){
        mAMTMenuShowAlwaysMax = max;
    }


    public void setOnActionModeListener (ActionModeCallback callback) {
        mActionModeCallback  = callback;
    }


    //
    // Action Mode methods
    //

    /**
     * Show the Toolbar's ActionMode. This will show a 'All' Checkbox instead of the navigation button,
     * temporarily replace the Toolbar's title with a counter ('x selected')
     * and show a {@link BottomNavigationView} in the footer.
     * The ActionMode is useful when the user can select items in a list.
     *
     * @see #setActionModeCount(int, int)
     * @see #setActionModeCheckboxListener(CompoundButton.OnCheckedChangeListener)
     * @see #setActionModeMenu(int)
     * @see #setActionModeMenuListener(NavigationBarView.OnItemSelectedListener)
     * @see #setActionModeToolbarMenu(int)
     * @see #setActionModeToolbarMenuListener(Toolbar.OnMenuItemClickListener) (int)
     * @see #setActionModeBottomMenu(int)
     * @see #setActionModeBottomMenuListener(NavigationBarView.OnItemSelectedListener)
     */
    public void showActionMode() {
        mIsActionMode = true;
        if (mIsSearchMode) dismissSearchMode();
        mOnBackPressedCallback.setEnabled(true);
        animatedVisibility(mMainToolbar, GONE);
        animatedVisibility(mActionModeToolbar, VISIBLE);
        mFooterContainer.setVisibility(GONE);
        mBottomActionModeBar.setVisibility(VISIBLE);

        // setActionModeCount(0, -1);
        mAppBarLayout.addOnOffsetChangedListener(mActionModeTitleFadeListener);
        mCollapsingToolbarLayout.seslSetSubtitle(null);
        mMainToolbar.setSubtitle(null);

        updateActionModeMenuVisibility(mContext.getResources().getConfiguration());

        if ( mActionModeCallback != null) {
            mActionModeCallback.onShow(this);
        }
    }


    private void updateActionModeMenuVisibility(Configuration config) {
        if (isActionMode()) {
            if (mSelectedItemsCount > 0) {
                if (switchActionModeMenu && config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mBottomActionModeBar.setVisibility(GONE);
                    mActionModeToolbar.getMenu().setGroupVisible(AMT_GROUP_MENU_ID, true);
                } else {
                    mBottomActionModeBar.setVisibility(VISIBLE);
                    mActionModeToolbar.getMenu().setGroupVisible(AMT_GROUP_MENU_ID, false);
                }
            }else{
                mBottomActionModeBar.setVisibility(GONE);
                mActionModeToolbar.getMenu().setGroupVisible(AMT_GROUP_MENU_ID, false);
            }
        }
    }



    /**
     * Dismiss the ActionMode.
     *
     * @see #showActionMode()
     */
    public void dismissActionMode() {
        mIsActionMode = false;
        mOnBackPressedCallback.setEnabled(false);
        animatedVisibility(mActionModeToolbar, GONE);
        animatedVisibility(mMainToolbar, VISIBLE);
        mFooterContainer.setVisibility(VISIBLE);
        mBottomActionModeBar.setVisibility(GONE);
        setTitle(mTitleExpanded, mTitleCollapsed);
        mAppBarLayout.removeOnOffsetChangedListener(mActionModeTitleFadeListener);
        mCollapsingToolbarLayout.seslSetSubtitle(mSubtitleExpanded);
        updateStickyTitle();
        setActionModeAllSelector(0,  true,  false);
        if (mActionModeCallback != null) {
            mActionModeCallback.onDismiss(this);
        }
    }

    /**
     * Checks if the ActionMode is enabled.
     */
    public boolean isActionMode() {
        return mIsActionMode;
    }

    /**
     * Set the menu resource for the ActionMode's {@link BottomNavigationView}
     * @deprecated Use {@link #setActionModeMenu(int)}
     */
    @Deprecated
    public void setActionModeBottomMenu(@MenuRes int menuRes) {
        mBottomActionModeBar.inflateMenu(menuRes);
    }


    /**
     * Set the menu resource for the ActionMode's {@link BottomNavigationView}.
     * On landscape orientation where ActionMode's {@link BottomNavigationView} will be hidden,
     * the visible items from this menu resource we be shown to ActionMode's {@link Toolbar} {@link Menu}
     */
    public void setActionModeMenu(@MenuRes int menuRes){
        getActionModeBottomMenu().clear();
        getActionModeToolbarMenu().removeGroup(AMT_GROUP_MENU_ID);
        mBottomActionModeBar.inflateMenu(menuRes);
        Menu AMToolbarMenu =  mActionModeToolbar.getMenu();
        AMToolbarMenu.removeGroup(AMT_GROUP_MENU_ID);
        Menu AMBottomMenu = mBottomActionModeBar.getMenu();
        int size = AMBottomMenu.size();
        int menuItemsAdded = 0;
        for (int a=0; a<size; a++){
            MenuItem ambMenuItem = AMBottomMenu.getItem(a);
            if (ambMenuItem.isVisible()){
                menuItemsAdded++;
                MenuItem amtMenuItem = AMToolbarMenu.add(AMT_GROUP_MENU_ID, ambMenuItem.getItemId(), Menu.NONE, ambMenuItem.getTitle());
                if (menuItemsAdded <= mAMTMenuShowAlwaysMax){
                    amtMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                }
            }
        }
        switchActionModeMenu = true;
    }

    /**
     * Returns the {@link Menu} of the ActionMode's {@link BottomNavigationView}.
     */
    public Menu getActionModeBottomMenu() {
        return mBottomActionModeBar.getMenu();
    }

    /**
     * Set the listener for the ActionMode's {@link BottomNavigationView}.
     * @deprecated See {@link #setActionModeMenuListener(NavigationBarView.OnItemSelectedListener)}
     */
    public void setActionModeBottomMenuListener(NavigationBarView.OnItemSelectedListener listener) {
        mBottomActionModeBar.setOnItemSelectedListener(listener);
    }


    /**
     * Set the listener for the ActionMode's {@link BottomNavigationView}.
     * On landscape orientation, the same listener will be invoke for ActionMode's {@link Toolbar} {@link MenuItem}s
     * which are copied from ActionMode's {@link BottomNavigationView}
     */
    public void setActionModeMenuListener(NavigationBarView.OnItemSelectedListener listener) {
        mBottomActionModeBar.setOnItemSelectedListener(listener);
        mActionModeToolbar.setOnMenuItemClickListener(item ->
                listener.onNavigationItemSelected(mActionModeToolbar.getMenu().findItem(item.getItemId()))
        );
    }


    /**
     * Set the menu resource for the ActionMode's {@link Toolbar}.
     */
    public void setActionModeToolbarMenu(@MenuRes int menuRes) {
        mActionModeToolbar.inflateMenu(menuRes);
    }


    /**
     * Set the listener for the ActionMode's {@link Toolbar}.
     */
    public void setActionModeToolbarMenuListener(Toolbar.OnMenuItemClickListener listener) {
        mActionModeToolbar.setOnMenuItemClickListener(listener);
    }


    /**
     * Returns the {@link Menu} of the ActionMode's {@link Toolbar}.
     *
     */
    public Menu getActionModeToolbarMenu() {
        return mActionModeToolbar.getMenu();
    }


    /**
     * Set the ActionMode's count and  checkbox enabled state.
     * Check state will stay.
     *
     * @param count number of selected items in the list
     * @param enabled enabled click
     */
    public void  setActionModeAllSelector(int count,  Boolean enabled) {
        setActionModeAllSelector(count, enabled, null);
    }


    /**
     * Set the ActionMode's count and Select all checkBox's enabled state and check state
     *
     * @param count number of selected items in the list
     * @param enabled enable or disable click
     * @param checked
     */
    public void  setActionModeAllSelector(int count,  Boolean enabled,  @Nullable Boolean checked) {
        if (mSelectedItemsCount != count) {
            mSelectedItemsCount = count;
            String title = count > 0
                    ? getResources().getString(R.string.oui_action_mode_n_selected, count)
                    : getResources().getString(R.string.oui_action_mode_select_items);
            mCollapsingToolbarLayout.setTitle(title);
            mActionModeTitleTextView.setText(title);
            updateActionModeMenuVisibility(mContext.getResources().getConfiguration());
        }
        if (checked != null && checked != mActionModeCheckBox.isChecked()) {
            mActionModeCheckBox.setChecked(checked);
        }
        if (enabled != mActionModeSelectAll.isEnabled()) {
            mActionModeSelectAll.setEnabled(enabled);
        }
    }


    /**
     * Set the ActionMode's count. This will change the count in the Toolbar's title
     * and if count = total, the 'All' Checkbox will be checked.
     *
     * @param count number of selected items in the list
     * @param total number of total items in the list
     * @deprecated use {@link #setActionModeAllSelector(int, Boolean, Boolean)}
     */
    @Deprecated
    public void setActionModeCount(int count, int total) {
        mSelectedItemsCount = count;
        String title = count > 0
                ? getResources().getString(R.string.oui_action_mode_n_selected, count)
                : getResources().getString(R.string.oui_action_mode_select_items);

        mCollapsingToolbarLayout.setTitle(title);
        mActionModeTitleTextView.setText(title);
        updateActionModeMenuVisibility(mContext.getResources().getConfiguration());
        mActionModeCheckBox.setChecked(count == total);
    }

    /**
     * Set the listener for the 'All' Checkbox of the ActionMode.
     */
    public void setActionModeCheckboxListener(CompoundButton.OnCheckedChangeListener listener) {
        mActionModeCheckBox.setOnCheckedChangeListener(listener);
    }

    //
    // others
    //
    public static class ToolbarLayoutParams extends LayoutParams {
        public int layout_location;

        public ToolbarLayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            if (c != null && attrs != null) {
                TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.ToolbarLayoutParams);
                layout_location = a.getInteger(R.styleable.ToolbarLayoutParams_layout_location, 0);
                a.recycle();
            }
        }
    }

    private CoordinatorLayout.LayoutParams CLLPWrapper(LayoutParams oldLp) {
        CoordinatorLayout.LayoutParams newLp = new CoordinatorLayout.LayoutParams(oldLp);
        newLp.width = oldLp.width;
        newLp.height = oldLp.height;
        newLp.leftMargin = oldLp.leftMargin;
        newLp.topMargin = oldLp.topMargin;
        newLp.rightMargin = oldLp.rightMargin;
        newLp.bottomMargin = oldLp.bottomMargin;
        newLp.gravity = oldLp.gravity;
        return newLp;
    }

    private void animatedVisibility(View view, int visibility) {
        view.setVisibility(VISIBLE);
        view.animate()
                .alphaBy(1.0f)
                .alpha(visibility == VISIBLE ? 1.0f : 0.0f)
                .setDuration(200)
                .setInterpolator(
                        new PathInterpolator(0.33f, 0.0f, 0.1f, 1.0f))
                .withEndAction(() -> view.setVisibility(visibility))
                .start();
    }

    private class AppBarOffsetListener implements AppBarLayout.OnOffsetChangedListener {
        @Override
        public void onOffsetChanged(AppBarLayout layout, int verticalOffset) {
            if (mActionModeToolbar.getVisibility() == View.VISIBLE) {
                int layoutPosition = Math.abs(mAppBarLayout.getTop());
                float alphaRange = ((float) mCollapsingToolbarLayout.getHeight()) * 0.17999999f;
                float toolbarTitleAlphaStart = ((float) mCollapsingToolbarLayout.getHeight()) * 0.35f;

                if (mAppBarLayout.seslIsCollapsed()) {
                    mActionModeTitleTextView.setAlpha(1.0f);
                } else {
                    float collapsedTitleAlpha = ((150.0f / alphaRange)
                            * (((float) layoutPosition) - toolbarTitleAlphaStart));

                    if (collapsedTitleAlpha >= 0.0f && collapsedTitleAlpha <= 255.0f) {
                        collapsedTitleAlpha /= 255.0f;
                        mActionModeTitleTextView.setAlpha(collapsedTitleAlpha);
                    } else if (collapsedTitleAlpha < 0.0f)
                        mActionModeTitleTextView.setAlpha(0.0f);
                    else
                        mActionModeTitleTextView.setAlpha(1.0f);
                }
            }
        }
    }

}
