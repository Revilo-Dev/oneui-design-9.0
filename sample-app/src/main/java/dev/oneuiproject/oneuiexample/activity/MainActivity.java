package dev.oneuiproject.oneuiexample.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.sec.sesl.tester.R;
import com.sec.sesl.tester.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

import dev.oneuiproject.oneui.utils.ActivityUtils;
import dev.oneuiproject.oneui.widget.FloatingSearchBar;
import dev.oneuiproject.oneui.widget.DialogBlur;
import dev.oneuiproject.oneui.widget.TipPopup;
import dev.oneuiproject.oneuiexample.base.FragmentInfo;
import dev.oneuiproject.oneuiexample.fragment.AppPickerFragment;
import dev.oneuiproject.oneuiexample.fragment.IconsFragment;
import dev.oneuiproject.oneuiexample.fragment.IndexScrollFragment;
import dev.oneuiproject.oneuiexample.fragment.PickersFragment;
import dev.oneuiproject.oneuiexample.fragment.PreferencesFragment;
import dev.oneuiproject.oneuiexample.fragment.ProgressBarFragment;
import dev.oneuiproject.oneuiexample.fragment.QRCodeFragment;
import dev.oneuiproject.oneuiexample.fragment.SeekBarFragment;
import dev.oneuiproject.oneuiexample.fragment.SwipeRefreshFragment;
import dev.oneuiproject.oneuiexample.fragment.TabsFragment;
import dev.oneuiproject.oneuiexample.fragment.ToolbarFragment;
import dev.oneuiproject.oneuiexample.fragment.WidgetsFragment;
import dev.oneuiproject.oneuiexample.ui.drawer.DrawerListAdapter;
import dev.oneuiproject.oneuiexample.utils.DarkModeUtils;

public class MainActivity extends AppCompatActivity
        implements DrawerListAdapter.DrawerListener {
    private ActivityMainBinding mBinding;
    private FragmentManager mFragmentManager;
    private final List<Fragment> fragments = new ArrayList<>();
    private View navigationBar;
    private View toolbarBar;
    private ScrollView searchResults;
    private LinearLayout searchResultRows;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override public void onFragmentStarted(@NonNull FragmentManager manager,
                                                            @NonNull Fragment fragment) {
                        if (fragment instanceof DialogFragment)
                            DialogBlur.apply(((DialogFragment) fragment).getDialog());
                    }
                    @Override public void onFragmentStopped(@NonNull FragmentManager manager,
                                                            @NonNull Fragment fragment) {
                        if (fragment instanceof DialogFragment)
                            DialogBlur.release(((DialogFragment) fragment).getDialog());
                    }
                }, true);

        initFragmentList();
        initDrawer();
        initFragments();
        initSearch();

        mBinding.drawerLayout.post(() -> {
            TipPopup tipPopup = new TipPopup(mBinding.drawerLayout.getToolbar().getChildAt(0), TipPopup.MODE_TRANSLUCENT);
            tipPopup.setMessage("I'm Mr. Mee6, look at me!");
            tipPopup.setAction("Close", view -> {
            });
            //tipPopup.setExpanded(true);
            tipPopup.show(TipPopup.DIRECTION_BOTTOM_RIGHT);
        });
    }

    @Override
    public void attachBaseContext(Context context) {
        // pre-OneUI
        if (Build.VERSION.SDK_INT <= 28) {
            super.attachBaseContext(DarkModeUtils.createDarkModeContextWrapper(context));
        } else {
            super.attachBaseContext(context);
        }
    }

    private void initFragmentList() {
        fragments.add(new WidgetsFragment());
        fragments.add(new ProgressBarFragment());
        fragments.add(new SeekBarFragment());
        fragments.add(new SwipeRefreshFragment());
        fragments.add(new PreferencesFragment());
        fragments.add(null);
        fragments.add(new TabsFragment());
        fragments.add(new ToolbarFragment());
        fragments.add(null);
        fragments.add(new AppPickerFragment());
        fragments.add(new IndexScrollFragment());
        fragments.add(new PickersFragment());
        fragments.add(null);
        fragments.add(new QRCodeFragment());
        fragments.add(new IconsFragment());
    }

    @Override
    public void onBackPressed() {
        // Fix O memory leak
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O
                && isTaskRoot()
                && mFragmentManager.getBackStackEntryCount() == 0) {
            finishAfterTransition();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // pre-OneUI
        if (Build.VERSION.SDK_INT <= 28) {
            final Resources res = getResources();
            res.getConfiguration().setTo(DarkModeUtils.createDarkModeConfig(this, newConfig));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.sample3_menu_main, menu);
        MenuItem search = menu.findItem(R.id.menu_search_pages);
        if (search != null && search.getIcon() != null)
            search.getIcon().mutate().setTint(getColor(R.color.oui_primary_text_color));
        MenuCompat.setGroupDividerEnabled(menu, true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_search_pages) {
            toggleSearch();
            return true;
        }
        if (item.getItemId() == R.id.menu_about_app) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return false;
    }

    private void initDrawer() {
        mBinding.drawerLayout.setDrawerButtonIcon(getDrawable(R.drawable.ic_oui_info_outline));
        mBinding.drawerLayout.setDrawerButtonTooltip("About page");
        mBinding.drawerLayout.setDrawerButtonOnClickListener(v ->
                ActivityUtils.startPopOverActivity(this,
                        new Intent(MainActivity.this, SampleAboutActivity.class),
                        null,
                        ActivityUtils.POP_OVER_POSITION_TOP | ActivityUtils.POP_OVER_POSITION_CENTER_HORIZONTAL));

        mBinding.drawerListView.setLayoutManager(new LinearLayoutManager(this));
        mBinding.drawerListView.setAdapter(new DrawerListAdapter(this, fragments, this));
        mBinding.drawerListView.setItemAnimator(null);
        mBinding.drawerListView.setHasFixedSize(true);
        mBinding.drawerListView.seslSetLastRoundedCorner(false);
    }

    private void initFragments() {
        mFragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        for (Fragment fragment : fragments) {
            if (fragment != null) transaction.add(R.id.main_content, fragment);
        }
        transaction.commit();
        mFragmentManager.executePendingTransactions();

        onDrawerItemSelected(0);
    }

    private void initSearch() {
        searchResults = new ScrollView(this);
        searchResults.setFillViewport(true);
        searchResults.setBackgroundColor(getColor(R.color.oui_background_color));
        searchResults.setVisibility(View.GONE);
        searchResultRows = new LinearLayout(this);
        searchResultRows.setOrientation(LinearLayout.VERTICAL);
        searchResultRows.setPadding(dp(16), dp(8), dp(16), dp(80));
        searchResults.addView(searchResultRows);
        mBinding.mainContent.addView(searchResults, new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        dev.oneuiproject.oneui.widget.ScrollEdgeFades.attach(searchResults)
                .setColor(getColor(R.color.oui_background_color));
        mBinding.drawerLayout.getFloatingSearchBar().setHint("Search pages");
        mBinding.drawerLayout.setSearchModeListener(new dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String text) {
                updateSearchResults(text);
                return true;
            }
            @Override public void onSearchModeToggle(FloatingSearchBar bar, boolean visible) {
                searchResults.setVisibility(visible ? View.VISIBLE : View.GONE);
                if (visible) {
                    searchResults.bringToFront();
                    updateSearchResults(bar.getQuery());
                }
            }
        });
    }

    public void toggleSearch() {
        if (mBinding.drawerLayout.isSearchMode()) mBinding.drawerLayout.dismissSearchMode();
        else mBinding.drawerLayout.showSearchMode();
    }

    private void updateSearchResults(String query) {
        if (searchResultRows != null) populatePageSearchResults(searchResultRows, query);
    }

    /** Populates either the toolbar search overlay or a full embedded search widget. */
    public void populatePageSearchResults(LinearLayout container, String query) {
        container.removeAllViews();
        String needle = query.trim().toLowerCase(java.util.Locale.ROOT);
        for (int index = 0; index < fragments.size(); index++) {
            Fragment fragment = fragments.get(index);
            if (!(fragment instanceof FragmentInfo)) continue;
            String title = ((FragmentInfo) fragment).getTitle().toString();
            if (!title.toLowerCase(java.util.Locale.ROOT).contains(needle)) continue;
            final int selected = index;
            TextView row = new TextView(this);
            row.setText(title);
            row.setTextColor(getColor(R.color.oui_primary_text_color));
            row.setTextSize(18);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setMinHeight(dp(56));
            row.setPadding(dp(16), 0, dp(16), 0);
            row.setBackgroundResource(android.R.drawable.list_selector_background);
            row.setOnClickListener(v -> onDrawerItemSelected(selected));
            container.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        if (container.getChildCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText("No matching pages");
            empty.setTextColor(getColor(R.color.oui_floating_nav_secondary));
            empty.setPadding(dp(16), dp(24), dp(16), dp(24));
            container.addView(empty);
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public boolean onDrawerItemSelected(int position) {
        if (mBinding.drawerLayout.isSearchMode()) mBinding.drawerLayout.dismissSearchMode();
        Fragment newFragment = fragments.get(position);
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        for (Fragment fragment : mFragmentManager.getFragments()) {
            transaction.hide(fragment);
        }
        transaction.show(newFragment).commit();

        if (newFragment instanceof TabsFragment && navigationBar == null
                && newFragment.getView() != null)
            navigationBar = newFragment.getView().findViewById(R.id.tabs_floating_nav);
        if (newFragment instanceof ToolbarFragment && toolbarBar == null
                && newFragment.getView() != null)
            toolbarBar = newFragment.getView().findViewById(R.id.toolbar_switcher);
        View stickyBar = newFragment instanceof TabsFragment ? navigationBar
                : newFragment instanceof ToolbarFragment ? toolbarBar : null;
        mBinding.drawerLayout.setStickyBottomBar(stickyBar);

        if (newFragment instanceof FragmentInfo) {
            if (!((FragmentInfo) newFragment).isAppBarEnabled()) {
                mBinding.drawerLayout.setExpanded(false, false);
                mBinding.drawerLayout.setExpandable(false);
            } else {
                mBinding.drawerLayout.setExpandable(true);
                mBinding.drawerLayout.setExpanded(false, false);
            }
            mBinding.drawerLayout.setTitle(getString(R.string.app_name), ((FragmentInfo) newFragment).getTitle());
            mBinding.drawerLayout.setExpandedSubtitle(((FragmentInfo) newFragment).getTitle());
            mBinding.drawerLayout.setCollapsedSubtitle(getString(R.string.app_name));
        }
        mBinding.drawerLayout.setDrawerOpen(false, true);

        return true;
    }
}
