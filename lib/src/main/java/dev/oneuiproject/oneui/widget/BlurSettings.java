package dev.oneuiproject.oneui.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

/** App-wide switch for backdrop blur; individual components keep their own blur options. */
public final class BlurSettings {
    public static final String KEY = "global_blur_enabled";

    private BlurSettings() { }

    public static boolean isEnabled(@NonNull Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY, true);
    }

    public static void setEnabled(@NonNull Context context, boolean enabled) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putBoolean(KEY, enabled).apply();
    }

    /** Redraws a component immediately when the preference changes. Call once per view. */
    public static void observe(@NonNull View view) {
        observe(view, view::invalidate);
    }

    public static void observe(@NonNull View view, @NonNull Runnable onChange) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(view.getContext());
        SharedPreferences.OnSharedPreferenceChangeListener listener = (shared, key) -> {
            if (KEY.equals(key)) onChange.run();
        };
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View attached) {
                preferences.registerOnSharedPreferenceChangeListener(listener);
            }
            @Override public void onViewDetachedFromWindow(View detached) {
                preferences.unregisterOnSharedPreferenceChangeListener(listener);
            }
        });
        if (view.isAttachedToWindow())
            preferences.registerOnSharedPreferenceChangeListener(listener);
    }
}
