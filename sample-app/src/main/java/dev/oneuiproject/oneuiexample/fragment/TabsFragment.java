package dev.oneuiproject.oneuiexample.fragment;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.SwitchCompat;

import com.sec.sesl.tester.R;

import dev.oneuiproject.oneui.dialog.GridMenuDialog;
import dev.oneuiproject.oneui.widget.FloatingNavigationBar;
import dev.oneuiproject.oneui.widget.NavigationPageContainer;
import dev.oneuiproject.oneuiexample.base.BaseFragment;
import dev.oneuiproject.oneuiexample.activity.MainActivity;

public class TabsFragment extends BaseFragment {
    private int lastDestination = R.id.nav_grid;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FloatingNavigationBar bar = view.findViewById(R.id.tabs_floating_nav);
        bar.setSelectedIcon(R.id.nav_grid, R.drawable.sample_nav_grid_filled);
        bar.setSelectedIcon(R.id.nav_list, R.drawable.list);
        bar.setSelectedIcon(R.id.nav_cards, R.drawable.credit_card);
        bar.setSelectedIcon(R.id.nav_settings, R.drawable.settings);
        NavigationPageContainer pages = view.findViewById(R.id.nav_pages);
        NavigationDemoPages.populate(mContext,
                (FrameLayout) view.findViewById(R.id.nav_grid),
                (FrameLayout) view.findViewById(R.id.nav_list),
                (FrameLayout) view.findViewById(R.id.nav_cards));
        GridMenuDialog menu = new GridMenuDialog(mContext);
        menu.inflateMenu(R.menu.sample3_tabs_grid_menu);
        menu.setOnItemClickListener(item -> true);

        bar.bindPages(pages);
        if (savedInstanceState == null) bar.setSelectedItemId(R.id.nav_grid);
        lastDestination = bar.getSelectedItemId();
        bar.setOnItemSelectedListener(itemId -> {
            if (itemId == R.id.nav_menu) {
                bar.setSelectedItemId(lastDestination);
                menu.show();
                return;
            }
            lastDestination = itemId;
        });

        SwitchCompat blur = view.findViewById(R.id.nav_toggle_blur);
        blur.setChecked(bar.isBlurEnabled());
        blur.setOnCheckedChangeListener((button, checked) -> bar.setBlurEnabled(checked));
        SwitchCompat animations = view.findViewById(R.id.nav_toggle_animations);
        animations.setChecked(bar.isAnimationsEnabled());
        animations.setOnCheckedChangeListener((button, checked) -> bar.setAnimationsEnabled(checked));
        SwitchCompat swipe = view.findViewById(R.id.nav_toggle_swipe);
        swipe.setChecked(bar.isPageSwipingEnabled());
        swipe.setOnCheckedChangeListener((button, checked) -> bar.setPageSwipingEnabled(checked));
        SwitchCompat searchExtra = view.findViewById(R.id.nav_toggle_search);
        searchExtra.setOnCheckedChangeListener((button, checked) -> {
            if (checked) {
                bar.setAction(R.drawable.sample_nav_search, "Search", clicked -> {
                    ((MainActivity) requireActivity()).toggleSearch();
                });
            } else {
                bar.clearAction();
            }
        });

        AppCompatSpinner display = view.findViewById(R.id.nav_display_mode);
        ArrayAdapter<String> modes = new ArrayAdapter<>(mContext,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Both", "Title", "Icons"});
        display.setAdapter(modes);
        display.setSelection(0);
        display.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View selected,
                                                 int position, long id) {
                bar.setShowLabels(position != 2);
                bar.setShowIcons(position != 1);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    @Override public void onViewStateRestored(@Nullable Bundle state) {
        super.onViewStateRestored(state);
        lastDestination = ((FloatingNavigationBar) requireView()
                .findViewById(R.id.tabs_floating_nav)).getSelectedItemId();
    }

    @Override public int getLayoutResId() { return R.layout.sample3_fragment_tabs; }
    @Override public int getIconResId() { return R.drawable.ic_oui_prompt_from_menu; }
    @Override public CharSequence getTitle() { return "Navigation"; }
}
