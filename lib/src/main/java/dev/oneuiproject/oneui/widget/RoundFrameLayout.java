package dev.oneuiproject.oneui.widget;

import static androidx.appcompat.util.SeslRoundedCorner.ROUNDED_CORNER_NONE;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.util.SeslRoundedCorner;

import dev.oneuiproject.oneui.design.R;

/**
 * FrameLayout with rounded corners.
 */
public class RoundFrameLayout extends FrameLayout {
    private Context mContext;
    private SeslRoundedCorner mRoundedCorner;
    private int mRoundedCornerColor = -1;
    private boolean surfaceShadowEnabled;
    private boolean surfaceShadowExplicit;
    private float surfaceShadowElevation;

    public RoundFrameLayout(@NonNull Context context) {
        this(context, null);
    }

    public RoundFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs,
                            int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RoundFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs,
                            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mContext = context;

        TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.RoundFrameLayout);
        final int roundedCorners = a.getInt(R.styleable.RoundFrameLayout_roundedCorners,
                SeslRoundedCorner.ROUNDED_CORNER_ALL);
        surfaceShadowExplicit = a.hasValue(R.styleable.RoundFrameLayout_surfaceShadowEnabled);
        surfaceShadowEnabled = a.getBoolean(R.styleable.RoundFrameLayout_surfaceShadowEnabled,
                getBackground() != null);
        surfaceShadowElevation = a.getDimension(R.styleable.RoundFrameLayout_surfaceShadowElevation,
                3 * getResources().getDisplayMetrics().density);
        a.recycle();

        mRoundedCorner = new SeslRoundedCorner(mContext);
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.roundedCornerColor, typedValue, true);
        if (typedValue.resourceId > 0) {
            mRoundedCornerColor = context.getResources().getColor(typedValue.resourceId, context.getTheme());
        }

        if (mRoundedCornerColor != -1 && roundedCorners != SeslRoundedCorner.ROUNDED_CORNER_NONE){
            mRoundedCorner.setRoundedCornerColor(roundedCorners, mRoundedCornerColor);
        }
        mRoundedCorner.setRoundedCorners(roundedCorners);
        SurfaceShadow.apply(this, surfaceShadowEnabled, surfaceShadowElevation,
                18 * getResources().getDisplayMetrics().density);
    }

    public void setSurfaceShadowEnabled(boolean enabled) {
        surfaceShadowExplicit = true;
        surfaceShadowEnabled = enabled;
        SurfaceShadow.apply(this, enabled, surfaceShadowElevation,
                18 * getResources().getDisplayMetrics().density);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!surfaceShadowExplicit && getBackground() != null && !surfaceShadowEnabled) {
            surfaceShadowEnabled = true;
            SurfaceShadow.apply(this, true, surfaceShadowElevation,
                    18 * getResources().getDisplayMetrics().density);
        }
    }

    public void setSurfaceShadowElevation(float pixels) {
        surfaceShadowElevation = Math.max(0, pixels);
        setSurfaceShadowEnabled(surfaceShadowEnabled);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        mRoundedCorner.drawRoundedCorner(canvas);
    }

    public void setRoundedCorners(int roundedCorners) {
        if (mRoundedCornerColor != -1 && roundedCorners != SeslRoundedCorner.ROUNDED_CORNER_NONE){
            mRoundedCorner.setRoundedCornerColor(roundedCorners, mRoundedCornerColor);
        }
        mRoundedCorner.setRoundedCorners(roundedCorners);
        invalidate();
    }

    public void setRoundedCornerColor(int roundedCorners, @ColorInt int color) {
        mRoundedCorner.setRoundedCornerColor(roundedCorners, color);
        invalidate();
    }
}
