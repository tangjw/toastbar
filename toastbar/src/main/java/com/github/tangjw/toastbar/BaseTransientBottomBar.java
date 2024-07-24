package com.github.tangjw.toastbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseTransientBottomBar<B extends BaseTransientBottomBar<B>> {

    public static final TimeInterpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    public static final TimeInterpolator LINEAR_OUT_SLOW_IN_INTERPOLATOR =
            new LinearOutSlowInInterpolator();


    public abstract static class BaseCallback<B> {
        /**
         * Indicates that the Snackbar was dismissed via a swipe.
         */
        public static final int DISMISS_EVENT_SWIPE = 0;
        /**
         * Indicates that the Snackbar was dismissed via an action click.
         */
        public static final int DISMISS_EVENT_ACTION = 1;
        /**
         * Indicates that the Snackbar was dismissed via a timeout.
         */
        public static final int DISMISS_EVENT_TIMEOUT = 2;
        /**
         * Indicates that the Snackbar was dismissed via a call to {@link #dismiss()}.
         */
        public static final int DISMISS_EVENT_MANUAL = 3;
        /**
         * Indicates that the Snackbar was dismissed from a new Snackbar being shown.
         */
        public static final int DISMISS_EVENT_CONSECUTIVE = 4;

        /**
         * Annotation for types of Dismiss events.
         */
        @IntDef({
                DISMISS_EVENT_SWIPE,
                DISMISS_EVENT_ACTION,
                DISMISS_EVENT_TIMEOUT,
                DISMISS_EVENT_MANUAL,
                DISMISS_EVENT_CONSECUTIVE
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface DismissEvent {
        }

        /**
         * Called when the given {@link BaseTransientBottomBar} has been dismissed, either through a
         * time-out, having been manually dismissed, or an action being clicked.
         *
         * @param transientBottomBar The transient bottom bar which has been dismissed.
         * @param event              The event which caused the dismissal. One of either: {@link
         *                           #DISMISS_EVENT_SWIPE}, {@link #DISMISS_EVENT_ACTION}, {@link #DISMISS_EVENT_TIMEOUT},
         *                           {@link #DISMISS_EVENT_MANUAL} or {@link #DISMISS_EVENT_CONSECUTIVE}.
         * @see BaseTransientBottomBar#dismiss()
         */
        public void onDismissed(B transientBottomBar, @DismissEvent int event) {
            // empty
        }

        /**
         * Called when the given {@link BaseTransientBottomBar} is visible.
         *
         * @param transientBottomBar The transient bottom bar which is now visible.
         * @see BaseTransientBottomBar#show()
         */
        public void onShown(B transientBottomBar) {
            // empty
        }
    }

    @IntRange(from = LENGTH_INDEFINITE)
    @Retention(RetentionPolicy.SOURCE)
    public @interface Duration {
    }

    public static final int LENGTH_INDEFINITE = -2;

    public static final int LENGTH_SHORT = -1;

    public static final int LENGTH_LONG = 0;

    // Legacy slide animation duration constant.
    static final int ANIMATION_DURATION = 250;
    // Legacy slide animation content fade duration constant.
    static final int ANIMATION_FADE_DURATION = 180;

    // Fade and scale animation constants.
    private static final int ANIMATION_FADE_IN_DURATION = 150;
    private static final int ANIMATION_FADE_OUT_DURATION = 75;
    private static final float ANIMATION_SCALE_FROM_VALUE = 0.8f;

    @NonNull
    static final Handler handler;
    static final int MSG_SHOW = 0;
    static final int MSG_DISMISS = 1;

    private static final String TAG = BaseTransientBottomBar.class.getSimpleName();

    static {
        handler =
                new Handler(
                        Looper.getMainLooper(),
                        message -> {
                            switch (message.what) {
                                case MSG_SHOW:
                                    ((BaseTransientBottomBar) message.obj).showView();
                                    return true;
                                case MSG_DISMISS:
                                    ((BaseTransientBottomBar) message.obj).hideView(message.arg1);
                                    return true;
                                default:
                                    return false;
                            }
                        });
    }

    @NonNull
    protected final ViewGroup targetParent;
    private final Context context;

    @NonNull
    protected final ToastBaseLayout view;

    private int duration;

    private boolean pendingShowingView;

    private List<BaseCallback<B>> callbacks;

    protected BaseTransientBottomBar(
            @NonNull Context context,
            @NonNull ViewGroup parent,
            @NonNull View content, boolean enableMask) {

        targetParent = parent;
        this.context = context;

        view = new ToastBaseLayout(context);
        FrameLayout.LayoutParams layoutParams;
        if (enableMask) {
            layoutParams = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            view.setBackgroundColor(Color.parseColor("#33000000"));
        } else {
            layoutParams = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            layoutParams.gravity = Gravity.CENTER;
            view.setBackgroundColor(Color.TRANSPARENT);
        }


        view.setLayoutParams(layoutParams);
     //  view.setGravity(Gravity.CENTER);
        view.setBaseTransientBottomBar(this);
        view.addView(content);
    }

    @NonNull
    public B setDuration(@Duration int duration) {
        this.duration = duration;
        return (B) this;
    }

    /**
     * Return the duration.
     *
     * @see #setDuration
     */
    @Duration
    public int getDuration() {
        return duration;
    }

    /**
     * Returns the {@link BaseTransientBottomBar}'s context.
     */
    @NonNull
    public Context getContext() {
        return context;
    }

    /**
     * Returns the {@link BaseTransientBottomBar}'s view.
     */
    @NonNull
    public View getView() {
        return view;
    }

    public void show() {
        ToastbarManager.getInstance().show(getDuration(), managerCallback);
    }

    public void dismiss() {
        dispatchDismiss(BaseCallback.DISMISS_EVENT_MANUAL);
    }

    protected void dispatchDismiss(@BaseCallback.DismissEvent int event) {
        ToastbarManager.getInstance().dismiss(managerCallback, event);
    }

    /**
     * Adds the specified callback to the list of callbacks that will be notified of transient bottom
     * bar events.
     *
     * @param callback Callback to notify when transient bottom bar events occur.
     * @see #removeCallback(BaseCallback)
     */
    @NonNull
    public B addCallback(@Nullable BaseCallback<B> callback) {
        if (callback == null) {
            return (B) this;
        }
        if (callbacks == null) {
            callbacks = new ArrayList<BaseCallback<B>>();
        }
        callbacks.add(callback);
        return (B) this;
    }

    /**
     * Removes the specified callback from the list of callbacks that will be notified of transient
     * bottom bar events.
     *
     * @param callback Callback to remove from being notified of transient bottom bar events
     * @see #addCallback(BaseCallback)
     */
    @NonNull
    public B removeCallback(@Nullable BaseCallback<B> callback) {
        if (callback == null) {
            return (B) this;
        }
        if (callbacks == null) {
            // This can happen if this method is called before the first call to addCallback
            return (B) this;
        }
        callbacks.remove(callback);
        return (B) this;
    }

    /**
     * Return whether this {@link BaseTransientBottomBar} is currently being shown.
     */
    public boolean isShown() {
        return ToastbarManager.getInstance().isCurrent(managerCallback);
    }

    /**
     * Returns whether this {@link BaseTransientBottomBar} is currently being shown, or is queued to
     * be shown next.
     */
    public boolean isShownOrQueued() {
        return ToastbarManager.getInstance().isCurrentOrNext(managerCallback);
    }

    @NonNull
    ToastbarManager.Callback managerCallback =
            new ToastbarManager.Callback() {
                @Override
                public void show() {
                    handler.sendMessage(handler.obtainMessage(MSG_SHOW, BaseTransientBottomBar.this));
                }

                @Override
                public void dismiss(int event) {
                    handler.sendMessage(
                            handler.obtainMessage(MSG_DISMISS, event, 0, BaseTransientBottomBar.this));
                }
            };

    final void showView() {
        if (this.view.getParent() == null) {
            LayoutParams lp = this.view.getLayoutParams();
            this.view.addToTargetParent(targetParent);
            recalculateAndUpdateMargins();

            // Set view to INVISIBLE so it doesn't flash on the screen before the inset adjustment is
            // handled and the enter animation is started
            view.setVisibility(View.INVISIBLE);
        }

        if (ViewCompat.isLaidOut(this.view)) {
            showViewImpl();
            return;
        }

        // Otherwise, show it in when laid out
        pendingShowingView = true;
    }

    void onAttachedToWindow() {

    }

    void onDetachedFromWindow() {
        if (isShownOrQueued()) {
            // If we haven't already been dismissed then this event is coming from a
            // non-user initiated action. Hence we need to make sure that we callback
            // and keep our state up to date. We need to post the call since
            // removeView() will call through to onDetachedFromWindow and thus overflow.
            handler.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            onViewHidden(BaseCallback.DISMISS_EVENT_MANUAL);
                        }
                    });
        }
    }

    void onLayoutChange() {
        if (pendingShowingView) {
            BaseTransientBottomBar.this.showViewImpl();
            pendingShowingView = false;
        }
    }

    private void showViewImpl() {
        if (shouldAnimate()) {
            // If animations are enabled, animate it in
            animateViewIn();
        } else {
            // Else if animations are disabled, just make view VISIBLE and call back now
            if (view.getParent() != null) {
                view.setVisibility(View.VISIBLE);
            }
            onViewShown();
        }
    }

    private int getScreenHeight() {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        return displayMetrics.heightPixels;
    }


    private void recalculateAndUpdateMargins() {
        // extraBottomMarginAnchorView = calculateBottomMarginForAnchorView();
    }


    void animateViewIn() {
        // Post to make sure animation doesn't start until after all inset handling has completed
        view.post(() -> {
            // Make view VISIBLE now that we are about to start the enter animation
            if (view.getParent() != null) {
                view.setVisibility(View.VISIBLE);
            }
            startFadeInAnimation();

        });
    }

    private void animateViewOut(int event) {
        startFadeOutAnimation(event);
    }

    private void startFadeInAnimation() {
        ValueAnimator alphaAnimator = getAlphaAnimator(0, 1);
        ValueAnimator scaleAnimator = getScaleAnimator(ANIMATION_SCALE_FROM_VALUE, 1);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(alphaAnimator, scaleAnimator);
        animatorSet.setDuration(ANIMATION_FADE_IN_DURATION);
        animatorSet.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        onViewShown();
                    }
                });
        animatorSet.start();
    }

    private void startFadeOutAnimation(final int event) {
        ValueAnimator animator = getAlphaAnimator(1, 0);
        animator.setDuration(ANIMATION_FADE_OUT_DURATION);
        animator.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        onViewHidden(event);
                    }
                });
        animator.start();
    }

    private ValueAnimator getAlphaAnimator(float... alphaValues) {
        ValueAnimator animator = ValueAnimator.ofFloat(alphaValues);
        animator.setInterpolator(LINEAR_INTERPOLATOR);
        animator.addUpdateListener(
                new AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                        view.setAlpha((Float) valueAnimator.getAnimatedValue());
                    }
                });
        return animator;
    }

    private ValueAnimator getScaleAnimator(float... scaleValues) {
        ValueAnimator animator = ValueAnimator.ofFloat(scaleValues);
        animator.setInterpolator(LINEAR_OUT_SLOW_IN_INTERPOLATOR);
        animator.addUpdateListener(
                new AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                        float scale = (float) valueAnimator.getAnimatedValue();
                        view.setScaleX(scale);
                        view.setScaleY(scale);
                    }
                });
        return animator;
    }

    private int getTranslationYBottom() {
        int translationY = view.getHeight();
        LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams instanceof MarginLayoutParams) {
            translationY += ((MarginLayoutParams) layoutParams).bottomMargin;
        }
        return translationY;
    }

    final void hideView(@BaseCallback.DismissEvent int event) {
        if (shouldAnimate() && view.getVisibility() == View.VISIBLE) {
            animateViewOut(event);
        } else {
            // If anims are disabled or the view isn't visible, just call back now
            onViewHidden(event);
        }
    }

    void onViewShown() {
        ToastbarManager.getInstance().onShown(managerCallback);
        if (callbacks != null) {
            // Notify the callbacks. Do that from the end of the list so that if a callback
            // removes itself as the result of being called, it won't mess up with our iteration
            int callbackCount = callbacks.size();
            for (int i = callbackCount - 1; i >= 0; i--) {
                callbacks.get(i).onShown((B) this);
            }
        }
    }

    void onViewHidden(int event) {
        // First tell the SnackbarManager that it has been dismissed
        ToastbarManager.getInstance().onDismissed(managerCallback);
        if (callbacks != null) {
            // Notify the callbacks. Do that from the end of the list so that if a callback
            // removes itself as the result of being called, it won't mess up with our iteration
            int callbackCount = callbacks.size();
            for (int i = callbackCount - 1; i >= 0; i--) {
                callbacks.get(i).onDismissed((B) this, event);
            }
        }

        // Lastly, hide and remove the view from the parent (if attached)
        ViewParent parent = view.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(view);
        }
    }

    boolean shouldAnimate() {
        return true;
    }

}
