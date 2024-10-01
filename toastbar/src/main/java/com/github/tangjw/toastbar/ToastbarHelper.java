package com.github.tangjw.toastbar;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

public class ToastbarHelper {

    private View parent;
    private String message;
    private int duration = Toastbar.LENGTH_SHORT;
    private Drawable drawable;
    private int loadingStatus;
    private Toastbar toastbar;

    public ToastbarHelper(View parent) {
        this.parent = parent;
    }

    public void message(String message) {
        this.message(message, Toastbar.LENGTH_SHORT);
    }

    public void message(String message, int duration) {
        showMsg(message, duration);
    }

    public void progress(String message) {
        this.progress(message, false, 0);
    }

    public void progress(String message, boolean isMask) {
        this.progress(message, isMask, Color.parseColor("#33000000"));
    }

    public void progress(String message, boolean isMask, @ColorInt int colorMask) {
        showLoadingMsg(message, isMask, Toastbar.LENGTH_INDEFINITE, colorMask);
    }

    public void progress(String message, int duration, boolean isMask, @ColorInt int colorMask) {
        showLoadingMsg(message, isMask, duration, colorMask);
    }

    public void toastIcon(String message) {
        toastIcon(message, R.drawable.ic_done_toastbar);
    }

    public void success(String message) {
        toastIcon(message, R.drawable.ic_done_toastbar);
    }

    public void toastIcon(String message, @DrawableRes int drawableId) {
        showIcon(message, drawableId);
    }

    private void showIcon(String message, int drawableId) {
        if (toastbar != null && toastbar.isShownOrQueued() && toastbar.getStatus() != 0) {
            toastbar.setText(message);
            toastbar.setDuration(duration);
        } else {
            toastbar = Toastbar.make(parent, message, Toastbar.LENGTH_SHORT);
        }

        toastbar.setIcon(ContextCompat.getDrawable(parent.getContext(), drawableId));
        toastbar.show();
    }

    private void showMsg(String message, int duration) {
        if (toastbar != null && toastbar.isShownOrQueued() && toastbar.getStatus() == 0) {
            toastbar.setText(message);
            toastbar.setDuration(duration);
        } else {
            toastbar = Toastbar.make(parent, message, duration);
        }
        toastbar.show();
    }

    private void showLoadingMsg(String message, boolean isMask, int colorMask, int duration) {
        if (toastbar != null && toastbar.isShownOrQueued() && toastbar.getStatus() == 1) {
            toastbar.setText(message);
            toastbar.setDuration(duration);
        } else {
            toastbar = Toastbar.make(parent, message, duration);
        }
        // toastbar = Toastbar.make(parent, message, duration);
        toastbar.setLoading(isMask, colorMask);
        toastbar.show();
    }

    public void dismiss() {
        if (toastbar != null && toastbar.isShownOrQueued()) {
            toastbar.dismiss();
        }
    }


}
