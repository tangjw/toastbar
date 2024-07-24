package com.github.tangjw.toastbar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;

class ToastBaseLayout extends FrameLayout {
    @SuppressLint("ClickableViewAccessibility")
    private static final OnTouchListener consumeAllTouchListener =
            (v, event) -> {
                // Prevent touches from passing through this view.
                return true;
            };

    @Nullable
    private BaseTransientBottomBar<?> baseTransientBottomBar;

    private ColorStateList backgroundTint;
    private PorterDuff.Mode backgroundTintMode;

    protected ToastBaseLayout(@NonNull Context context) {
        this(context, null);
    }

    protected ToastBaseLayout(@NonNull Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    protected ToastBaseLayout(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, null, defStyleAttr);
        setOnTouchListener(consumeAllTouchListener);
        setFocusable(true);

        if (getBackground() == null) {
           // ViewCompat.setBackground(this, createThemedBackground());
        }
    }


    @Override
    public void setBackground(@Nullable Drawable drawable) {
        setBackgroundDrawable(drawable);
    }

    @Override
    public void setBackgroundDrawable(@Nullable Drawable drawable) {
        if (drawable != null && backgroundTint != null) {
            drawable = DrawableCompat.wrap(drawable.mutate());
            DrawableCompat.setTintList(drawable, backgroundTint);
            DrawableCompat.setTintMode(drawable, backgroundTintMode);
        }
        super.setBackgroundDrawable(drawable);
    }

    @Override
    public void setBackgroundTintList(@Nullable ColorStateList backgroundTint) {
        this.backgroundTint = backgroundTint;
        if (getBackground() != null) {
            Drawable wrappedBackground = DrawableCompat.wrap(getBackground().mutate());
            DrawableCompat.setTintList(wrappedBackground, backgroundTint);
            DrawableCompat.setTintMode(wrappedBackground, backgroundTintMode);
            if (wrappedBackground != getBackground()) {
                super.setBackgroundDrawable(wrappedBackground);
            }
        }
    }

    @Override
    public void setBackgroundTintMode(@Nullable PorterDuff.Mode backgroundTintMode) {
        this.backgroundTintMode = backgroundTintMode;
        if (getBackground() != null) {
            Drawable wrappedBackground = DrawableCompat.wrap(getBackground().mutate());
            DrawableCompat.setTintMode(wrappedBackground, backgroundTintMode);
            if (wrappedBackground != getBackground()) {
                super.setBackgroundDrawable(wrappedBackground);
            }
        }
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener onClickListener) {
        setOnTouchListener(onClickListener != null ? null : consumeAllTouchListener);
        super.setOnClickListener(onClickListener);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (baseTransientBottomBar != null) {
            baseTransientBottomBar.onLayoutChange();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (baseTransientBottomBar != null) {
            baseTransientBottomBar.onAttachedToWindow();
        }
        ViewCompat.requestApplyInsets(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (baseTransientBottomBar != null) {
            baseTransientBottomBar.onDetachedFromWindow();
        }
    }

    void addToTargetParent(ViewGroup targetParent) {
        targetParent.addView(this);
    }

    void setBaseTransientBottomBar(BaseTransientBottomBar<?> baseTransientBottomBar) {
        this.baseTransientBottomBar = baseTransientBottomBar;
    }

    private Drawable createThemedBackground() {
        float cornerRadius = 32f;

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(cornerRadius);

        background.setColor(Color.parseColor("#66000000"));
        if (backgroundTint != null) {
            Drawable wrappedDrawable = DrawableCompat.wrap(background);
            DrawableCompat.setTintList(wrappedDrawable, backgroundTint);
            return wrappedDrawable;
        } else {
            return DrawableCompat.wrap(background);
        }
    }
}