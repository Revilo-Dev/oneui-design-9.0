package dev.oneuiproject.oneuiexample.fragment;

import com.sec.sesl.tester.R;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dev.oneuiproject.oneui.widget.OneUIThickSlider;
import dev.oneuiproject.oneuiexample.base.BaseFragment;

public class SeekBarFragment extends BaseFragment {

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        bindExampleMenu(view.findViewById(R.id.slider_thick_volume));
        bindExampleMenu(view.findViewById(R.id.slider_thick_vertical_volume));
    }

    private void bindExampleMenu(OneUIThickSlider slider) {
        slider.setOnMenuClickListener(() -> {
            PopupMenu menu = new PopupMenu(requireContext(), slider);
            menu.getMenu().add("Example option");
            menu.show();
        });
    }

    @Override
    public int getLayoutResId() {
        return R.layout.sample3_fragment_seek_bar;
    }

    @Override
    public int getIconResId() {
        return R.drawable.drawer_page_icon_seekbar;
    }

    @Override
    public CharSequence getTitle() {
        return "SeekBar";
    }

    @Override
    public boolean isAppBarEnabled() {
        return false;
    }

}
