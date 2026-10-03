package dev.oneuiproject.oneui.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

/** A rounded image tile with a readable title and subtitle over either an image or a gradient. */
public class ImageTileView extends FrameLayout {
    private final View gradient;
    private final ImageView image;
    private final TextView title;
    private final TextView subtitle;

    public ImageTileView(@NonNull Context context) {
        this(context, null);
    }

    public ImageTileView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(Color.TRANSPARENT);
        setClipToOutline(true);
        setCornerRadiusDp(18);

        gradient = new View(context);
        addView(gradient, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setGradientColors(0xFF62698B, 0xFF272C45);

        image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setVisibility(GONE);
        addView(image, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        View shade = new View(context);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00000000, 0xB9000000}));
        addView(shade, new LayoutParams(LayoutParams.MATCH_PARENT, dp(82), Gravity.BOTTOM));

        LinearLayout captions = new LinearLayout(context);
        captions.setOrientation(LinearLayout.VERTICAL);
        captions.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        captions.setPadding(dp(4), 0, dp(4), dp(10));
        addView(captions, new LayoutParams(LayoutParams.MATCH_PARENT, dp(62), Gravity.BOTTOM));

        title = new TextView(context);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.WHITE);
        title.setTextSize(15);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        captions.addView(title);

        subtitle = new TextView(context);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setTextColor(0xFFE2E2E2);
        subtitle.setTextSize(12);
        subtitle.setPadding(0, dp(2), 0, 0);
        captions.addView(subtitle);
    }

    public void setTitle(@Nullable CharSequence value) {
        title.setText(value);
        updateContentDescription();
    }

    public void setSubtitle(@Nullable CharSequence value) {
        subtitle.setText(value);
        subtitle.setVisibility(TextUtils.isEmpty(value) ? GONE : VISIBLE);
        updateContentDescription();
    }

    public void setCornerRadiusDp(float radius) {
        GradientDrawable outline = new GradientDrawable();
        outline.setColor(Color.TRANSPARENT);
        outline.setCornerRadius(dp(radius));
        setBackground(outline);
    }

    /** Gradient remains underneath images, so an unavailable image still has a background. */
    public void setGradientColors(int startColor, int endColor) {
        gradient.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{startColor, endColor}));
    }

    /** Use a bundled drawable, or pass zero to return to the gradient. */
    public void setImageResource(@DrawableRes int resourceId) {
        image.setImageDrawable(null);
        if (resourceId == 0) {
            image.setVisibility(GONE);
        } else {
            image.setImageResource(resourceId);
            image.setVisibility(VISIBLE);
        }
    }

    /** Accepts a content URI (including the latest photo's URI) or a file URI. */
    public void setImageUri(@Nullable Uri uri) {
        image.setImageDrawable(null);
        if (uri == null) {
            image.setVisibility(GONE);
        } else {
            image.setImageURI(uri);
            image.setVisibility(image.getDrawable() == null ? GONE : VISIBLE);
        }
    }

    /** Accepts a local filesystem path, content:// URI, or file:// URI. */
    public void setImagePath(@Nullable String path) {
        if (TextUtils.isEmpty(path)) {
            setImageUri(null);
            return;
        }
        Uri uri = Uri.parse(path);
        setImageUri(uri.getScheme() == null ? Uri.fromFile(new File(path)) : uri);
    }

    private void updateContentDescription() {
        setContentDescription(TextUtils.isEmpty(subtitle.getText()) ? title.getText()
                : title.getText() + ", " + subtitle.getText());
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
