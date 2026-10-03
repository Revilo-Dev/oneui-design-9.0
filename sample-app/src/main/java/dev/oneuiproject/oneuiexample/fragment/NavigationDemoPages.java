package dev.oneuiproject.oneuiexample.fragment;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.sec.sesl.tester.R;

import java.util.Random;

import dev.oneuiproject.oneui.widget.IconRowView;
import dev.oneuiproject.oneui.widget.ImageTileView;
import dev.oneuiproject.oneui.widget.ScrollEdgeFades;

/** Small, self-contained examples for the three navigation destinations. */
final class NavigationDemoPages {
    private static final int[][] TILE_GRADIENTS = {
            {0xFF8B97AF, 0xFF434B69}, {0xFF79B3BD, 0xFF246779},
            {0xFFF0A795, 0xFF92516A}, {0xFFB7A0CB, 0xFF624B83},
            {0xFFE6A96E, 0xFF9D5C54}, {0xFF8DB0DA, 0xFF465D9A},
            {0xFFA3C58A, 0xFF426B70}, {0xFFC4A6A0, 0xFF74536C}
    };

    private static final int[] ROW_ICONS = {
            R.drawable.beforebed, R.drawable.event, R.drawable.device,
            R.drawable.running, R.drawable.weather, R.drawable.workout,
            R.drawable.game, R.drawable.calendar, R.drawable.music_alt
    };
    private static final int[] ROW_ICON_COLORS = {
            0xFF7B79FF, 0xFFEF696B, 0xFF53BDEC, 0xFF4BCD8C,
            0xFFFFB961, 0xFFE773A5, 0xFFAE80EF, 0xFF69C6BF, 0xFFCF86D9
    };

    private NavigationDemoPages() { }

    static void populate(Context context, FrameLayout grid, FrameLayout list, FrameLayout cards) {
        Random counts = new Random();
        populateGrid(context, grid, counts);
        populateList(context, list);
        populateCards(context, cards);
    }

    private static void populateGrid(Context context, FrameLayout host, Random counts) {
        LinearLayout content = scrollContent(context, host);
        LinearLayout heading = new LinearLayout(context);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(context, "Example albums", 17, true,
                context.getColor(R.color.oui_primary_text_color));
        heading.addView(title, new LinearLayout.LayoutParams(0, dp(context, 52), 1));
        TextView viewAll = text(context, "View all", 14, true, 0xFFE384B1);
        heading.addView(viewAll);
        content.addView(heading);

        // Equal weights keep the gallery tiles three columns wide on any phone size.
        for (int row = 0; row < 3; row++) {
            LinearLayout strip = new LinearLayout(context);
            for (int column = 0; column < 3; column++) {
                int index = row * 3 + column;
                LinearLayout.LayoutParams cell = new LinearLayout.LayoutParams(
                        0, dp(context, 154), 1);
                cell.setMargins(dp(context, 4), dp(context, 4),
                        dp(context, 4), dp(context, 4));
                if (index < TILE_GRADIENTS.length) {
                    strip.addView(albumCard(context, index, "Album " + (index + 1),
                            String.valueOf(1 + counts.nextInt(500)), 15), cell);
                } else {
                    strip.addView(new View(context), cell);
                }
            }
            content.addView(strip);
        }
    }

    private static void populateList(Context context, FrameLayout host) {
        LinearLayout content = scrollContent(context, host);
        TextView heading = text(context, "Example list", 25, true,
                context.getColor(R.color.oui_primary_text_color));
        heading.setPadding(dp(context, 16), dp(context, 10), 0, dp(context, 18));
        content.addView(heading);
        TextView intro = text(context,
                "Choose an example item. Each row can have its own icon, icon color, title and subtitle.",
                16, false, context.getColor(R.color.oui_primary_text_color));
        intro.setPadding(dp(context, 16), 0, dp(context, 16), dp(context, 22));
        content.addView(intro);

        for (int i = 0; i < 8; i++) {
            IconRowView row = exampleRow(context, i);
            row.setTitle("Example item " + (i + 1));
            row.setSubtitle("Example subtitle");
            row.setCardBackgroundColor(context.getColor(R.color.nav_demo_list_surface));
            addRow(context, content, row);
        }
    }

    private static void populateCards(Context context, FrameLayout host) {
        LinearLayout content = scrollContent(context, host, false);
        TextView heading = text(context, "Example cards", 25, true,
                context.getColor(R.color.oui_primary_text_color));
        heading.setPadding(dp(context, 16), dp(context, 10), 0, dp(context, 20));
        content.addView(heading);

        for (int i = 0; i < ROW_ICONS.length; i++) {
            IconRowView card = exampleRow(context, i);
            card.setTitle("Example card " + (i + 1));
            card.setSubtitle("Example");
            card.setExpandedText("Example text");
            card.setCardBackgroundColor(context.getColor(R.color.nav_demo_card_surface));
            addRow(context, content, card);
        }
    }

    private static LinearLayout scrollContent(Context context, FrameLayout host) {
        return scrollContent(context, host, true);
    }

    private static LinearLayout scrollContent(Context context, FrameLayout host, boolean edgeFades) {
        ScrollView scroll = new ScrollView(context);
        if (!edgeFades) ScrollEdgeFades.setAutoAttachEnabled(scroll, false);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        host.addView(scroll, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(context, 12), dp(context, 12),
                dp(context, 12), dp(context, 24));
        scroll.addView(content);
        return content;
    }

    private static IconRowView exampleRow(Context context, int index) {
        IconRowView row = new IconRowView(context);
        row.setIconResource(ROW_ICONS[index]);
        row.setIconBackgroundColor(context.getColor(R.color.nav_demo_icon_background));
        row.setIconTintColor(ROW_ICON_COLORS[index]);
        return row;
    }

    private static void addRow(Context context, LinearLayout content, IconRowView row) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(context, 12));
        content.addView(row, params);
    }

    private static ImageTileView albumCard(Context context, int gradientIndex, String name,
            String count, int radiusDp) {
        ImageTileView card = new ImageTileView(context);
        card.setCornerRadiusDp(radiusDp);
        card.setGradientColors(TILE_GRADIENTS[gradientIndex][0],
                TILE_GRADIENTS[gradientIndex][1]);
        card.setTitle(name);
        card.setSubtitle(count);
        return card;
    }

    private static TextView text(Context context, String value, int size, boolean bold, int color) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private static int dp(Context context, float size) {
        return Math.round(size * context.getResources().getDisplayMetrics().density);
    }

}
