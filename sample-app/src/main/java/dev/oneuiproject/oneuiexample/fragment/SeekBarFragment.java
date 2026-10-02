package dev.oneuiproject.oneuiexample.fragment;

import com.sec.sesl.tester.R;
import dev.oneuiproject.oneuiexample.base.BaseFragment;

public class SeekBarFragment extends BaseFragment {

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
