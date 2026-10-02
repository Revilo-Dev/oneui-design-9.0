package dev.oneuiproject.oneui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import dev.oneuiproject.oneui.design.R;

/** A noninteractive, theme-aware progressive color fade over a scrollable child. */
public class ProgressiveBlurLayout extends FrameLayout {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int topHeight;
    private int bottomHeight;
    private boolean fadeTop = true;
    private boolean fadeBottom = true;
    private int fadeColor;
    private boolean customFadeColor;

    public ProgressiveBlurLayout(Context context) { this(context, null); }
    public ProgressiveBlurLayout(Context context, AttributeSet attrs) { this(context, attrs, 0); }
    public ProgressiveBlurLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        topHeight = bottomHeight = Math.round(56 * getResources().getDisplayMetrics().density);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ProgressiveBlurLayout,
                    defStyleAttr, 0);
            topHeight = bottomHeight = a.getDimensionPixelSize(
                    R.styleable.ProgressiveBlurLayout_blurHeight, topHeight);
            fadeTop = a.getBoolean(R.styleable.ProgressiveBlurLayout_blurTop, true);
            fadeBottom = a.getBoolean(R.styleable.ProgressiveBlurLayout_blurBottom, true);
            customFadeColor = a.hasValue(R.styleable.ProgressiveBlurLayout_fadeColor);
            if (customFadeColor) fadeColor = a.getColor(
                    R.styleable.ProgressiveBlurLayout_fadeColor,
                    context.getColor(R.color.oui_background_color));
            a.recycle();
        }
        if (!customFadeColor) fadeColor = context.getColor(R.color.oui_background_color);
        setWillNotDraw(false);
        BlurSettings.observe(this);
    }

    public void setBlurHeight(int pixels) { setFadeHeights(pixels, pixels); }
    public void setFadeHeights(int topPixels, int bottomPixels) {
        topHeight = Math.max(0, topPixels);
        bottomHeight = Math.max(0, bottomPixels);
        invalidate();
    }
    public void setBlurEdges(boolean top, boolean bottom) {
        fadeTop = top;
        fadeBottom = bottom;
        invalidate();
    }
    public void setFadeColor(int color) { fadeColor = color; customFadeColor = true; invalidate(); }
    public void useLocalSurfaceColor() { customFadeColor = false; invalidate(); }

    @Override protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (!BlurSettings.isEnabled(getContext())) return;
        if (getChildCount() != 1) return;
        View content = getChildAt(0);
        if (!customFadeColor) fadeColor = ScrollEdgeFades.resolveSurfaceColor(content);
        if (fadeTop && topHeight > 0 && content.canScrollVertically(-1))
            drawFade(canvas, 0, Math.min(topHeight, getHeight()), true);
        if (fadeBottom && bottomHeight > 0 && content.canScrollVertically(1)) {
            int height = Math.min(bottomHeight, getHeight());
            drawFade(canvas, getHeight() - height, height, false);
        }
    }

    private void drawFade(Canvas canvas, int y, int height, boolean top) {
        int clear = fadeColor & 0x00ffffff;
        paint.setShader(new LinearGradient(0, y, 0, y + height,
                top ? fadeColor : clear, top ? clear : fadeColor, Shader.TileMode.CLAMP));
        canvas.drawRect(0, y, getWidth(), y + height, paint);
        paint.setShader(null);
    }
}
