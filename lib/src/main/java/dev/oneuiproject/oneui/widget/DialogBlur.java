package dev.oneuiproject.oneui.widget;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import java.util.WeakHashMap;

/** Applies the shared blur setting to popups backed by an Android dialog window. */
public final class DialogBlur {
    private static final WeakHashMap<Dialog, View> BACKDROPS = new WeakHashMap<>();

    private DialogBlur() { }

    public static void apply(@Nullable Dialog dialog) {
        if (dialog == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        Window window = dialog.getWindow();
        if (window == null) return;
        boolean enabled = BlurSettings.isEnabled(dialog.getContext());
        int behind = dp(dialog.getContext(), 20);
        window.setBackgroundBlurRadius(enabled ? dp(dialog.getContext(), 24) : 0);
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.setBlurBehindRadius(enabled ? behind : 0);
        window.setAttributes(attributes);
        if (enabled) window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        else window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        Activity activity = activityFrom(dialog.getContext());
        if (activity == null) return;
        View backdrop = activity.findViewById(android.R.id.content);
        if (backdrop == null) return;
        if (enabled) {
            backdrop.setRenderEffect(RenderEffect.createBlurEffect(behind, behind,
                    Shader.TileMode.CLAMP));
            BACKDROPS.put(dialog, backdrop);
        } else release(dialog);
    }

    public static void release(@Nullable Dialog dialog) {
        if (dialog == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        View backdrop = BACKDROPS.remove(dialog);
        if (backdrop != null) backdrop.setRenderEffect(null);
    }

    @Nullable private static Activity activityFrom(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    private static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
