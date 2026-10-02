package dev.oneuiproject.oneuiexample.fragment;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import com.sec.sesl.tester.R;

import dev.oneuiproject.oneui.widget.FloatingActionBar;
import dev.oneuiproject.oneui.widget.FloatingBarSwitcher;
import dev.oneuiproject.oneui.widget.FloatingNavigationBar;
import dev.oneuiproject.oneui.widget.FloatingToolbar;
import dev.oneuiproject.oneui.widget.NavigationPageContainer;
import dev.oneuiproject.oneui.widget.Toast;
import dev.oneuiproject.oneuiexample.base.BaseFragment;

public class ToolbarFragment extends BaseFragment {
    private boolean animationsEnabled = true;

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        NavigationPageContainer pages = view.findViewById(R.id.toolbar_pages);
        FloatingNavigationBar navigation = view.findViewById(R.id.toolbar_bottom_nav);
        FloatingToolbar toolbar = view.findViewById(R.id.floating_toolbar);
        FloatingActionBar actionBar = view.findViewById(R.id.floating_action_bar);
        FloatingBarSwitcher switcher = view.findViewById(R.id.toolbar_switcher);
        Button openToolbar = view.findViewById(R.id.open_toolbar_button);
        Button openActionBar = view.findViewById(R.id.open_action_bar_button);

        switcher.setNavigationBar(navigation);
        switcher.setBackdropView(pages);
        navigation.bindPages(pages);
        toolbar.setBlurEnabled(true);
        toolbar.setOnActionClickListener(actionId -> {
            String name = "Action";
            if (actionId == R.id.toolbar_add) name = "Add";
            else if (actionId == R.id.toolbar_copy) name = "Copy";
            else if (actionId == R.id.toolbar_share) name = "Share";
            else if (actionId == R.id.toolbar_delete) name = "Delete";
            else if (actionId == R.id.toolbar_more) name = "More";
            Toast.makeText(mContext, name, Toast.LENGTH_SHORT).show();
        });

        openToolbar.setOnClickListener(clicked -> press(clicked, () -> {
            if (switcher.getCurrentBar() == toolbar) {
                switcher.showNavigation();
                openToolbar.setText("Open toolbar");
            } else {
                switcher.showBar(toolbar);
                openToolbar.setText("Close toolbar");
            }
        }));
        openActionBar.setOnClickListener(clicked -> press(clicked, () -> {
            if (switcher.getCurrentBar() == actionBar) {
                switcher.showNavigation();
                openActionBar.setText("Open action bar");
            } else {
                switcher.showBar(actionBar);
                openActionBar.setText("Close action bar");
            }
        }));
        actionBar.setOnCancelClickListener(clicked -> {
            switcher.showNavigation();
            openActionBar.setText("Open action bar");
        });
        actionBar.setOnConfirmClickListener(clicked -> {
            Toast.makeText(mContext, "Save", Toast.LENGTH_SHORT).show();
            switcher.showNavigation();
            openActionBar.setText("Open action bar");
        });

        SwitchCompat animations = view.findViewById(R.id.toolbar_animations);
        animations.setOnCheckedChangeListener((button, enabled) -> {
            animationsEnabled = enabled;
            switcher.setAnimationsEnabled(enabled);
            navigation.setAnimationsEnabled(enabled);
            toolbar.setAnimationsEnabled(enabled);
            actionBar.setAnimationsEnabled(enabled);
        });
    }

    private void press(View button, Runnable action) {
        if (!animationsEnabled) { action.run(); return; }
        button.animate().scaleX(0.92f).scaleY(0.92f).setDuration(60)
                .withEndAction(() -> button.animate().scaleX(1f).scaleY(1f).setDuration(90)
                        .withEndAction(action).start()).start();
    }

    @Override public int getLayoutResId() { return R.layout.sample3_fragment_toolbar; }
    @Override public int getIconResId() { return R.drawable.ic_oui_drawer; }
    @Override public CharSequence getTitle() { return "Toolbar"; }
}
