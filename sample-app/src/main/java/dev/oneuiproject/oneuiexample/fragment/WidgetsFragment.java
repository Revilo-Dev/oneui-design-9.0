package dev.oneuiproject.oneuiexample.fragment;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSpinner;

import com.sec.sesl.tester.R;

import java.util.ArrayList;
import java.util.List;

import dev.oneuiproject.oneuiexample.activity.MainActivity;
import dev.oneuiproject.oneuiexample.base.BaseFragment;
import dev.oneuiproject.oneui.widget.FloatingSearchBar;
import dev.oneuiproject.oneui.widget.ScrollEdgeFades;

public class WidgetsFragment extends BaseFragment
        implements View.OnClickListener {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int[] Ids = {R.id.fragment_btn_1,
                R.id.fragment_btn_2,
                R.id.fragment_btn_3,
                R.id.fragment_btn_4,
                R.id.fragment_btn_5};
        for (int id : Ids) view.findViewById(id).setOnClickListener(this);

        AppCompatSpinner spinner = view.findViewById(R.id.fragment_spinner);
        List<String> items = new ArrayList<>();
        for (int i = 1; i < 5; i++)
            items.add("Spinner Item " + i);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(mContext,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        FloatingSearchBar search = view.findViewById(R.id.widgets_floating_search);
        View content = view.findViewById(R.id.widgets_scroll_content);
        ScrollView results = view.findViewById(R.id.widgets_search_results);
        LinearLayout rows = view.findViewById(R.id.widgets_search_result_rows);
        search.setSourceView(content);
        search.setHint("Search pages");
        search.setPersistent(true);
        search.setListener(new FloatingSearchBar.Listener() {
            @Override public void onQueryChanged(String query) {
                boolean hasQuery = !query.trim().isEmpty();
                results.setVisibility(hasQuery ? View.VISIBLE : View.GONE);
                if (hasQuery)
                    ((MainActivity) requireActivity()).populatePageSearchResults(rows, query);
                search.setSourceView(hasQuery ? results : content);
            }
        });
        ScrollEdgeFades.attach(results).setColor(requireContext().getColor(R.color.oui_background_color));
    }

    @Override
    public int getLayoutResId() {
        return R.layout.sample3_fragment_widgets;
    }

    @Override
    public int getIconResId() {
        return R.drawable.ic_oui_game_launcher;
    }

    @Override
    public CharSequence getTitle() {
        return "Widgets";
    }

    @Override
    public void onClick(View v) {
        // no-op
    }

}
