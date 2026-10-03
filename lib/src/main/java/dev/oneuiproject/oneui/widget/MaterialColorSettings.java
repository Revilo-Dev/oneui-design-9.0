package dev.oneuiproject.oneui.widget;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import dev.oneuiproject.oneui.design.R;

/** App-wide switch between the One UI palette and Android wallpaper accent colors. */
public final class MaterialColorSettings {
    public static final String KEY = "global_material_color_palette";

    private MaterialColorSettings() { }

    public static boolean isEnabled(@NonNull Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY, false);
    }

    public static void setEnabled(@NonNull Context context, boolean enabled) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(KEY, enabled).apply();
    }

    /** Call before inflating an activity's views. A recreated activity picks up changes. */
    public static void applyTheme(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isEnabled(activity))
            activity.getTheme().applyStyle(R.style.OneUIMaterialPaletteOverlay, true);
    }

    /** Resolves the system accent for components that draw their own selection surface. */
    public static int accentColor(@NonNull Context context, int fallback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || !isEnabled(context))
            return fallback;
        boolean dark = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int colorId = context.getResources().getIdentifier(
                dark ? "system_accent1_200" : "system_accent1_600", "color", "android");
        return colorId == 0 ? fallback : context.getColor(colorId);
    }
}
