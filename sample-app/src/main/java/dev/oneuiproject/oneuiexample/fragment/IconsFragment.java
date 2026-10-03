package dev.oneuiproject.oneuiexample.fragment;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sec.sesl.tester.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.oneuiproject.oneui.widget.FloatingSearchBar;
import dev.oneuiproject.oneuiexample.base.BaseFragment;

public class IconsFragment extends BaseFragment {
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView iconListView = view.findViewById(R.id.icons_list);
        TextView empty = view.findViewById(R.id.icons_empty);
        ImageAdapter adapter = new ImageAdapter();
        iconListView.setLayoutManager(new LinearLayoutManager(mContext));
        iconListView.setAdapter(adapter);
        iconListView.addItemDecoration(new ItemDecoration(mContext));
        iconListView.setItemAnimator(null);
        iconListView.seslSetFillBottomEnabled(true);
        iconListView.seslSetLastRoundedCorner(true);
        iconListView.seslSetFastScrollerEnabled(true);
        iconListView.seslSetGoToTopEnabled(false);
        ((dev.oneuiproject.oneui.widget.FloatingScrollToTopButton)
                view.findViewById(R.id.icons_scroll_top)).bind(iconListView);
        iconListView.seslSetSmoothScrollEnabled(true);

        FloatingSearchBar search = view.findViewById(R.id.icons_floating_search);
        search.setSourceView(iconListView);
        search.setHint("Search icons");
        search.setPersistent(true);
        search.setCloseButtonVisible(false);
        search.setListener(new FloatingSearchBar.Listener() {
            @Override public void onQueryChanged(String query) {
                adapter.setQuery(query);
                empty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                iconListView.scrollToPosition(0);
            }
        });
    }

    @Override public int getLayoutResId() { return R.layout.sample3_fragment_icons; }
    @Override public int getIconResId() { return R.drawable.emoji_2; }
    @Override public CharSequence getTitle() { return "Icons"; }

    private class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder>
            implements SectionIndexer {
        private final List<ImportedIconCatalog.Entry> visible = new ArrayList<>();
        private final List<String> sections = new ArrayList<>();
        private final List<Integer> positionsForSection = new ArrayList<>();
        private final List<Integer> sectionForPosition = new ArrayList<>();

        ImageAdapter() { setQuery(""); }

        void setQuery(String query) {
            visible.clear();
            String normalized = query.trim().toLowerCase(Locale.ROOT);
            String[] words = normalized.isEmpty() ? new String[0] : normalized.split("[\\s_]+");
            for (ImportedIconCatalog.Entry entry : ImportedIconCatalog.ALL) {
                boolean matches = true;
                for (String word : words) {
                    if (!entry.name.contains(word)) { matches = false; break; }
                }
                if (matches) visible.add(entry);
            }
            rebuildSections();
            notifyDataSetChanged();
        }

        private void rebuildSections() {
            sections.clear();
            positionsForSection.clear();
            sectionForPosition.clear();
            for (int i = 0; i < visible.size(); i++) {
                String name = visible.get(i).name;
                String letter = name.substring(0, 1).toUpperCase(Locale.ROOT);
                if (Character.isDigit(letter.charAt(0))) letter = "#";
                if (sections.isEmpty() || !sections.get(sections.size() - 1).equals(letter)) {
                    sections.add(letter);
                    positionsForSection.add(i);
                }
                sectionForPosition.add(sections.size() - 1);
            }
        }

        @Override public int getItemCount() { return visible.size(); }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                int viewType) {
            View row = LayoutInflater.from(mContext).inflate(
                    R.layout.sample3_view_icon_listview_item, parent, false);
            return new ViewHolder(row);
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ImportedIconCatalog.Entry entry = visible.get(position);
            holder.imageView.setImageResource(entry.drawableId);
            holder.imageView.setImageTintList(entry.tintForTheme
                    ? ColorStateList.valueOf(mContext.getColor(R.color.oui_primary_text_color))
                    : null);
            if (entry.tintForTheme) {
                holder.imageView.setBackground(null);
            } else {
                // A neutral chip keeps light and dark details visible in mixed-colour art.
                GradientDrawable backplate = new GradientDrawable();
                backplate.setColor(mContext.getColor(R.color.icons_preview_backplate));
                backplate.setCornerRadius(getResources().getDisplayMetrics().density * 12);
                holder.imageView.setBackground(backplate);
            }
            holder.textView.setText(entry.name);
        }

        @Override public Object[] getSections() { return sections.toArray(new String[0]); }
        @Override public int getPositionForSection(int sectionIndex) {
            return sectionIndex >= 0 && sectionIndex < positionsForSection.size()
                    ? positionsForSection.get(sectionIndex) : 0;
        }
        @Override public int getSectionForPosition(int position) {
            return position >= 0 && position < sectionForPosition.size()
                    ? sectionForPosition.get(position) : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView imageView;
            final TextView textView;

            ViewHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.icon_list_item_icon);
                textView = itemView.findViewById(R.id.icon_list_item_text);
            }
        }
    }

    private static class ItemDecoration extends RecyclerView.ItemDecoration {
        private final Drawable divider;

        ItemDecoration(@NonNull Context context) {
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.isLightTheme, outValue, true);
            divider = context.getDrawable(outValue.data == 0
                    ? R.drawable.sesl_list_divider_dark
                    : R.drawable.sesl_list_divider_light);
        }

        @Override public void onDraw(@NonNull Canvas canvas, @NonNull RecyclerView parent,
                @NonNull RecyclerView.State state) {
            super.onDraw(canvas, parent, state);
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                int top = child.getBottom()
                        + ((ViewGroup.MarginLayoutParams) child.getLayoutParams()).bottomMargin;
                divider.setBounds(parent.getLeft(), top, parent.getRight(),
                        top + divider.getIntrinsicHeight());
                divider.draw(canvas);
            }
        }
    }
}
