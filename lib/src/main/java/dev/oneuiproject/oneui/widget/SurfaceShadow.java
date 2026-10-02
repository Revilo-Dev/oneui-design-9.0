package dev.oneuiproject.oneui.widget;

import android.content.res.Configuration;
import android.graphics.Outline;
import android.view.View;
import android.view.ViewOutlineProvider;

/** Shared light-theme elevation for card and bar surfaces. */
public final class SurfaceShadow {
    private SurfaceShadow() { }

    public static void apply(View view, boolean enabled, float elevation, float radius) {
        boolean light = (view.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES;
        view.setElevation(enabled && light ? Math.max(0, elevation) : 0);
        if (enabled) {
            view.setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View target, Outline outline) {
                    outline.setRoundRect(0, 0, target.getWidth(), target.getHeight(), radius);
                }
            });
        } else view.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        view.setClipToOutline(enabled);
    }
}
