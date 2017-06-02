package com.davinci.advancedrecyclertouchlistener;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Amey on 4/15/17.
 */

public class AnimationUtils
{
    private final static long ANIMATION_STANDARD = 300;
    private final static long ANIMATION_CLOSE = 150;

    public static boolean animateStretching(float translatePercent, View slidingView,
                                            int stretchingWidth, List<Integer> childViewIDs)
    {
        final ViewGroup.LayoutParams layoutParams = slidingView.getLayoutParams();

        layoutParams.width = stretchingWidth;
        slidingView.setLayoutParams(layoutParams);

        // translatePercent starts from 0 so added one
        final float weight = Math.min(1 + (translatePercent), childViewIDs.size());

        float smallCellWeight = Math.min(childViewIDs.size() - weight, 1);

        LinearLayout.LayoutParams smallLayoutParam = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, smallCellWeight);

        // shrink other buttons
        for (int i = 1; i<childViewIDs.size(); i++)
        {
            slidingView.findViewById(childViewIDs.get(i))
                    .setLayoutParams(smallLayoutParam);
        }

        // Stretch the first button
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                weight);

        slidingView.findViewById(childViewIDs.get(0))
                .setLayoutParams(param);

        slidingView.requestLayout();

        return weight == childViewIDs.size();
    }

    public static void animateShrink(final View bgView, View fgView,
                                     int fgTranslationX, List<Integer> cartItemOptionViews)
    {
        // ArrayList of ObjectAnimators
        ArrayList<ValueAnimator> arrayListObjectAnimators = new ArrayList<>();

        // Shrink bgView's width
        ValueAnimator valueAnimator = ValueAnimator.ofInt(bgView.getMeasuredWidth(),
                bgView.getMinimumWidth());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                int val = (Integer) animation.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = bgView.getLayoutParams();
                layoutParams.width = val;
                bgView.setLayoutParams(layoutParams);
            }
        });
        arrayListObjectAnimators.add(valueAnimator);

        // Open the forground to correct position
        ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(
                fgView, View.TRANSLATION_X, fgTranslationX);
        translateAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
        arrayListObjectAnimators.add(translateAnimator);

        if (cartItemOptionViews != null)
        {
            // Reset individual options weight
            for (int viewId : cartItemOptionViews)
            {
                final View view = bgView.findViewById(viewId);

                valueAnimator = ValueAnimator.ofFloat
                        (((LinearLayout.LayoutParams) view.getLayoutParams()).weight, 1.0f);
                valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
                {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation)
                    {
                        float val = (float) animation.getAnimatedValue();
                        LinearLayout.LayoutParams layoutParams =
                                (LinearLayout.LayoutParams) view.getLayoutParams();
                        layoutParams.weight = val;
                        view.setLayoutParams(layoutParams);
                    }
                });

                arrayListObjectAnimators.add(valueAnimator);
            }
        }

        // Set of all animations to run them in Parallel
        ValueAnimator[] objectAnimators = arrayListObjectAnimators
                .toArray(new ValueAnimator[arrayListObjectAnimators.size()]);
        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(objectAnimators);
        animSet.setDuration(ANIMATION_CLOSE);
        animSet.start();
    }

    public static AnimatorSet animateFullStretching(final View bgView, View fgView,
                                                    int fgTranslationX)
    {
        //ArrayList of ObjectAnimators
        ArrayList<ValueAnimator> arrayListObjectAnimators = new ArrayList<>();

        ValueAnimator valueAnimator = ValueAnimator.ofInt(bgView.getMeasuredWidth(),
                fgView.getWidth());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                int val = (Integer) animation.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = bgView.getLayoutParams();
                layoutParams.width = val;
                bgView.setLayoutParams(layoutParams);
            }
        });
        arrayListObjectAnimators.add(valueAnimator);

        // Open the foreground to correct position
        ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(
                fgView, View.TRANSLATION_X, fgTranslationX);
        translateAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
        arrayListObjectAnimators.add(translateAnimator);

        // Set of all animations to run them in Parallel
        ValueAnimator[] objectAnimators = arrayListObjectAnimators
                .toArray(new ValueAnimator[arrayListObjectAnimators.size()]);
        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(objectAnimators);
        animSet.setDuration(ANIMATION_STANDARD);
        animSet.start();

        return animSet;

    }

    public static void openView(View view, long translationAmount)
    {
        ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(
                view, View.TRANSLATION_X, translationAmount);
        translateAnimator.setDuration(ANIMATION_STANDARD);
        translateAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
        translateAnimator.start();
    }

    public static void closeView(View view)
    {
        ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(
                view, View.TRANSLATION_X, 0f);
        translateAnimator.setDuration(ANIMATION_CLOSE);
        translateAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
        translateAnimator.start();
    }

}
