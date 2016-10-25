package com.instachat.android.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Build;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;

public final class AnimationUtil {
    private AnimationUtil() {
    }

    public static void fadeInAnimation(final View view) {
        final Animation anim = AnimationUtils.loadAnimation(view.getContext(), android.R.anim.fade_in);
        view.setVisibility(View.VISIBLE);
        view.startAnimation(anim);
    }

    public static void fadeOutAnimation(final View view) {
        fadeOutAnimation(view, View.GONE);
    }

    public static void fadeOutAnimation(final View view, final int visibility) {
        final Animation anim = AnimationUtils.loadAnimation(view.getContext(), android.R.anim.fade_out);
        view.setVisibility(visibility);
        view.startAnimation(anim);
    }

     /*public static void bottomUpAnimation(final View view) {
         final Animation anim = AnimationUtils.loadAnimation(view.getContext(), R.anim.bottom_up);
         view.startAnimation(anim);
     }

     public static void bottomDownAnimation(final View view) {
         final Animation anim = AnimationUtils.loadAnimation(view.getContext(), R.anim.bottom_down);
         view.startAnimation(anim);
     }

    public static void expandFromBottomRight(final View view) {
       final Animation anim = AnimationUtils.loadAnimation(view.getContext(), R.anim.expand_from_bottom_right);
       view.startAnimation(anim);
    }

    public static void shrinkToBottomRight(final View view) {
       final Animation anim = AnimationUtils.loadAnimation(view.getContext(), R.anim.shrink_to_bottom_right);
       view.startAnimation(anim);
    }*/

    public static void expandFromCenter(final View view) {

        if (Build.VERSION.SDK_INT < 21) {
            view.setVisibility(View.VISIBLE);
            return;
        }

        // get the center for the clipping circle
        final int cx = (view.getLeft() + view.getRight()) / 2;
        final int cy = (view.getTop() + view.getBottom()) / 2;

        // get the final radius for the clipping circle
        final int finalRadius = Math.max(view.getWidth(), view.getHeight());

        // create the animator for this view (the start radius is zero)
        try {
            final Animator anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0, finalRadius);

            // make the view visible and start the animation
            view.setVisibility(View.VISIBLE);
            anim.start();
        } catch (final IllegalStateException e) {
            //view already in detached state
        }
    }

    /*public static void expandFromBottomRightMD(final View view) {

        if (Build.VERSION.SDK_INT < 21) {
            view.setVisibility(View.VISIBLE);
            expandFromBottomRight(view);
            return;
        }

        // get the center for the clipping circle
        final int cx = view.getRight();
        final int cy = view.getBottom();

        // get the final radius for the clipping circle
        final int finalRadius = view.getWidth() * 2;

        // create the animator for this view (the start radius is zero)
        try {
            final Animator anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0, finalRadius);

            // make the view visible and start the animation
            view.setVisibility(View.VISIBLE);
            anim.start();
        } catch (final IllegalStateException e) {
            //view already in detached state
        }
    }*/

    public static void shrinkToCenter(final View view) {

        if (Build.VERSION.SDK_INT < 21) {
            view.setVisibility(View.GONE);
            return;
        }

        // get the center for the clipping circle
        final int cx = (view.getLeft() + view.getRight()) / 2;
        final int cy = (view.getTop() + view.getBottom()) / 2;

        // get the initial radius for the clipping circle
        final int initialRadius = view.getWidth();

        try {
            // create the animation (the final radius is zero)
            final Animator anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, initialRadius, 0);

            // make the view invisible when the animation is done
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    view.setVisibility(View.GONE);
                }
            });

            // start the animation
            anim.start();
        } catch (final IllegalStateException e) {
            //view already in detached state
        }
    }

    /*public static void shrinkToBottomRightMD(final View view) {

        if (Build.VERSION.SDK_INT < 21) {
            view.setVisibility(View.GONE);
            shrinkToBottomRight(view);
            return;
        }

        // get the center for the clipping circle
        final int cx = view.getRight();
        final int cy = view.getBottom();

        // get the initial radius for the clipping circle
        final int initialRadius = view.getWidth() * 2;

        try {
            // create the animation (the final radius is zero)
            final Animator anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, initialRadius, 0);

            // make the view invisible when the animation is done
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    view.setVisibility(View.GONE);
                }
            });

            // start the animation
            anim.start();
        } catch (final IllegalStateException e) {
            //view already in detached state
        }
    }*/

    /*public static void scaleBigToNormal(final View view) {
       view.startAnimation(AnimationUtils.loadAnimation(view.getContext(), R.anim.scale));
    }

    public static void resizeAnimation(final View view, final int fromWidth, final int fromHeight,
                                       final int toWidth, final int toHeight, final int durationMs) {
       final ResizeAnimation resizeAnimation = new ResizeAnimation(view,fromWidth,fromHeight,toWidth,toHeight,durationMs);
       view.startAnimation(resizeAnimation);
    }*/

    public static void scaleInFromCenter(final View view) {

        ScaleAnimation anim = new ScaleAnimation(0f, 1f, 0f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(300);     // animation duration in milliseconds
        anim.setFillAfter(true);    // If fillAfter is true, the transformation that this animation performed will persist when it is finished.
        view.startAnimation(anim);

    }

    public static void scaleInToCenter(final View view) {

        ScaleAnimation anim = new ScaleAnimation(1f, 0f, 1f, 0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(300);     // animation duration in milliseconds
        anim.setFillAfter(true);    // If fillAfter is true, the transformation that this animation performed will persist when it is finished.
        view.startAnimation(anim);

    }

}