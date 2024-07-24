package com.github.tangjw.toastbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;

public class Toastbar extends BaseTransientBottomBar<Toastbar> {

    private int status = 0;

    public int getStatus() {
        return status;
    }

    @NonNull
    public static Toastbar make(
            @NonNull View view,
            @NonNull CharSequence text,
            @Duration int duration) {
        return makeInternal(view, text, duration, false, false, null);
    }

    @NonNull
    public static Toastbar makeLoading(
            @NonNull View view,
            @NonNull CharSequence text,
            boolean enableMask) {
        return makeInternal(view, text, Toastbar.LENGTH_INDEFINITE, true, enableMask, null);
    }

    @NonNull
    private static Toastbar makeInternal(
            @NonNull View view,
            @NonNull CharSequence text,
            @Duration int duration, boolean enableLoading, boolean enableMask,
            @Nullable Drawable iconDrawable
    ) {

        final ViewGroup parent = findSuitableParent(view);
        if (parent == null) {
            throw new IllegalArgumentException(
                    "No suitable parent found from the given view. Please provide a valid view.");
        }
        Context context = parent.getContext();
        LinearLayout contentLayout = new LinearLayout(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        contentLayout.setLayoutParams(params);
        contentLayout.setOrientation(LinearLayout.VERTICAL);

        ProgressBar progressBar = new ProgressBar(context);
        progressBar.setId(View.generateViewId());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp2px(context, 112), dp2px(context, 62));
        progressBar.setPadding(0, dp2px(context, 20), 0, 0);
        progressBar.setVisibility(View.GONE);
        progressBar.setLayoutParams(layoutParams);
        progressBar.setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));
        if (iconDrawable != null) {
            progressBar.setIndeterminateDrawable(iconDrawable);
        }
        contentLayout.addView(progressBar, 0);

        TextView textView = new TextView(context);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (enableLoading || iconDrawable != null) {
            textView.setMaxLines(1);
            textView.setGravity(Gravity.CENTER);
        } else {
            textView.setMaxLines(2);
            textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        }

        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setIncludeFontPadding(false);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f);
        textView.setText(text);
        textView.setTextColor(Color.WHITE);
        textView.setMinHeight(dp2px(context, 50));
        //textView.setMaxWidth(dp2px(context,300));
        textView.setPadding(dp2px(context, 14), 0, dp2px(context, 14), 0);
        contentLayout.addView(textView, 1);

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp2px(context, 8));

        background.setColor(Color.parseColor("#DD3C3C3C"));
        contentLayout.setBackground(DrawableCompat.wrap(background));

        final Toastbar toastbar = new Toastbar(parent, contentLayout, enableMask);
        toastbar.setDuration(duration);
        return toastbar;
    }

    public void setText(@NonNull CharSequence message) {
        status = 0;
        view.post(() -> {
            getMessageView().setText(message);
            getMessageView().setGravity(Gravity.CENTER_VERTICAL);
            getIconView().setVisibility(View.GONE);
        });
    }

    public void setLoading(boolean isMask) {
        status = 1;
        view.post(() -> {
            getIconView().setVisibility(View.VISIBLE);
            getMessageView().setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
            if (isMask) {
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                view.setBackgroundColor(Color.parseColor("#33000000"));
                view.setLayoutParams(params);
            } else {
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.gravity = Gravity.CENTER;
                view.setBackground(null);
                view.setLayoutParams(params);
            }
        });
    }

    public void setIcon(Drawable iconDrawable) {
        status = 2;
        view.post(() -> {
            Rect bounds = getIconView().getIndeterminateDrawable().getBounds();
            getIconView().setIndeterminateDrawable(iconDrawable);
            getIconView().getIndeterminateDrawable().setBounds(bounds);
            getIconView().setVisibility(View.VISIBLE);
            getMessageView().setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.CENTER;
            view.setBackground(null);
            view.setLayoutParams(params);
        });
    }


    private TextView getMessageView() {
        return (TextView) getContentLayout().getChildAt(1);
    }

    private ProgressBar getIconView() {
        return (ProgressBar) getContentLayout().getChildAt(0);
    }

    private LinearLayout getContentLayout() {
        return (LinearLayout) view.getChildAt(0);
    }

    @Override
    public void show() {
        super.show();
    }

    protected Toastbar(@NonNull ViewGroup parent, @NonNull View content, boolean enableMask) {
        super(parent.getContext(), parent, content, enableMask);
    }


    public static class Callback extends BaseCallback<Toastbar> {
        /**
         * Indicates that the Snackbar was dismissed via a swipe.
         */
        public static final int DISMISS_EVENT_SWIPE = BaseCallback.DISMISS_EVENT_SWIPE;
        /**
         * Indicates that the Snackbar was dismissed via an action click.
         */
        public static final int DISMISS_EVENT_ACTION = BaseCallback.DISMISS_EVENT_ACTION;
        /**
         * Indicates that the Snackbar was dismissed via a timeout.
         */
        public static final int DISMISS_EVENT_TIMEOUT = BaseCallback.DISMISS_EVENT_TIMEOUT;
        /**
         * Indicates that the Snackbar was dismissed via a call to {@link #dismiss()}.
         */
        public static final int DISMISS_EVENT_MANUAL = BaseCallback.DISMISS_EVENT_MANUAL;
        /**
         * Indicates that the Snackbar was dismissed from a new Snackbar being shown.
         */
        public static final int DISMISS_EVENT_CONSECUTIVE = BaseCallback.DISMISS_EVENT_CONSECUTIVE;

        @Override
        public void onShown(Toastbar sb) {
            // Stub implementation to make API check happy.
        }

        @Override
        public void onDismissed(Toastbar transientBottomBar, @DismissEvent int event) {
            // Stub implementation to make API check happy.
        }
    }

    /**
     * 查找 SnackBar 插入的目标ViewGroup
     */
    @Nullable
    private static ViewGroup findSuitableParent(View view) {
        ViewGroup fallback = null;
        do {
            if (view instanceof FrameLayout) {
                if (view.getId() == android.R.id.content) {
                    return (ViewGroup) view;
                } else {
                    fallback = (ViewGroup) view;
                }
            }

            if (view != null) {
                final ViewParent parent = view.getParent();
                view = parent instanceof View ? (View) parent : null;
            }
        } while (view != null);

        return fallback;
    }

    private static int dp2px(Context context, float dp) {
//        context.getApplicationContext()
//        context

//        view.getApp
//         context.getResources().getDisplayMetrics()

        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics());
    }

}
