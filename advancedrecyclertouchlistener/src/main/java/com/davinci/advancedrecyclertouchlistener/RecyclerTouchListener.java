package com.davinci.advancedrecyclertouchlistener;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * Created by Amey on 4/15/17.
 */

public class RecyclerTouchListener implements RecyclerView.OnItemTouchListener
{
    Activity act;
    private List<Integer> rightCartItemOptionViews;

    private Set<Integer> ignoredViewTypes;
    private Set<Integer> ignoredLeftSwipeViewTypes = new HashSet<>();

    private int touchSlop;
    private int minFlingVel;
    private int maxFlingVel;
    private RecyclerView rView;
    private int bgRightWidth = 1, bgLeftWidth = 1; // 1 and not 0 to prevent dividing by zero
    private float touchedX;
    private float touchedY;
    private boolean isFgSwiping;
    private int mSwipingSlop;
    private VelocityTracker mVelocityTracker;
    private int touchedPosition;
    private View touchedView;
    private boolean mPaused;
    private boolean bgRightVisible, bgLeftVisible;
    private int bgVisiblePosition;
    private View bgVisibleView;
    private int screenWidth;
    private View fgView;
    private View bgRightView;
    private int fgViewID;
    private int bgRightViewID, bgLeftViewID;
    private OnRowClickListener mRowClickListener;
    private SwipeDirection swipeDirection;

    private boolean swipedLeft = false;
    private boolean swipedRight = false;
    /*
     * swipedLeftProper and swipedRightProper are true if user swipes in the respective direction
     * and its more than half of the bgView (in respective direction)
     */
    private boolean swipedLeftProper = false;
    private boolean swipedRightProper = false;

    // user choices
    private boolean swipeable = false;
    private View bgLeftView;
    private boolean removeItem;
    private boolean itemSwiped;

    private AnimatorListenerAdapter animationFinishedListener = new AnimatorListenerAdapter()
    {
        @Override
        public void onAnimationEnd(Animator animation)
        {
            rView.getChildAt(touchedPosition).setVisibility(View.GONE);
            closeBgOptions();
            if (swipeDirection == SwipeDirection.LEFT)
            {
                mRowClickListener.onSwipedLeft(touchedPosition);
            }
            else if (swipeDirection == SwipeDirection.RIGHT)
            {
                mRowClickListener.onSwipedRight(touchedPosition);
            }
            resetParams();
        }
    };

    //==============================================================================================
    //  Constructor
    //==============================================================================================

    public RecyclerTouchListener(Activity a, RecyclerView recyclerView) {
        this.act = a;
        ViewConfiguration vc = ViewConfiguration.get(recyclerView.getContext());
        touchSlop = vc.getScaledTouchSlop();
        minFlingVel = vc.getScaledMinimumFlingVelocity() * 16;
        maxFlingVel = vc.getScaledMaximumFlingVelocity();
        rView = recyclerView;
        bgRightVisible = false;
        bgVisiblePosition = -1;
        bgVisibleView = null;
        ignoredViewTypes = new HashSet<>();
        rightCartItemOptionViews = new ArrayList<>();
        DisplayMetrics displaymetrics = new DisplayMetrics();
        act.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        screenWidth = displaymetrics.widthPixels;

        rView.addOnScrollListener(new RecyclerView.OnScrollListener()
        {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState)
            {
                mPaused = newState == RecyclerView.SCROLL_STATE_DRAGGING;
            }

        });
    }

    public RecyclerTouchListener setIgnoredViewTypes(Integer... viewTypes)
    {
        ignoredViewTypes.clear();
        ignoredViewTypes.addAll(Arrays.asList(viewTypes));
        return this;
    }

    public RecyclerTouchListener setIgnoredLeftViewTypes(Integer... viewTypes)
    {
        ignoredLeftSwipeViewTypes.clear();
        ignoredLeftSwipeViewTypes.addAll(Arrays.asList(viewTypes));
        return this;
    }

    public RecyclerTouchListener setForeground(int foregroundID)
    {
        this.swipeable = true;
        fgViewID = foregroundID;
        return this;
    }

    public RecyclerTouchListener setBackground(int rightBgViewID, int leftBgViewID)
    {
        bgRightViewID = rightBgViewID;
        bgLeftViewID = leftBgViewID;
        return this;
    }

    public RecyclerTouchListener setRowClickListener(OnRowClickListener listener)
    {
        this.mRowClickListener = listener;
        return this;
    }

    public RecyclerTouchListener setRightCartItemOptionViews(Integer... viewIds)
    {
        this.rightCartItemOptionViews = new ArrayList<>(Arrays.asList(viewIds));
        return this;
    }

    //==============================================================================================
    //  OnItemTouchListener implementation
    //==============================================================================================

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent motionEvent)
    {
        return handleTouchEvent(motionEvent);
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent motionEvent)
    {
        handleTouchEvent(motionEvent);
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept)
    {

    }

    //==============================================================================================
    //  Instance Methods
    //==============================================================================================

    private int getOptionViewID(List<Integer> optionViews, MotionEvent motionEvent)
    {
        for (int i = 0; i < optionViews.size(); i++) {
            if (touchedView != null) {
                Rect rect = new Rect();
                int x = (int) motionEvent.getRawX();
                int y = (int) motionEvent.getRawY();
                touchedView.findViewById(optionViews.get(i)).getGlobalVisibleRect(rect);
                if (rect.contains(x, y)) {
                    return optionViews.get(i);
                }
            }
        }
        return -1;
    }

    private void closeBGView()
    {
        AnimationUtils.closeView(bgVisibleView);
        setVisibility(false, false, null, -1);
    }

    public void closeBgOptions()
    {
        animateFG(Animation.CLOSE, -bgRightWidth);
        setVisibility(false, false, null, -1);
    }

    private boolean ignoreLeftSwipe()
    {
        return ignoredLeftSwipeViewTypes.contains(rView.getAdapter().getItemViewType(touchedPosition));
    }

    private void animateFG(Animation animateType, int translationAmount)
    {
        if (animateType == Animation.OPEN)
        {
            AnimationUtils.openView(fgView, translationAmount);
        }
        else if (animateType == Animation.CLOSE)
        {
            AnimationUtils.closeView(fgView);
        }
    }

    private boolean handleTouchEvent(MotionEvent motionEvent)
    {

        if (swipeable && bgRightWidth < 2)
        {
            if (act.findViewById(bgRightViewID) != null)
            {
                bgRightWidth = act.findViewById(bgRightViewID).getMinimumWidth();
            }

        }
        if (swipeable && bgLeftWidth < 2)
        {
            if (act.findViewById(bgLeftViewID) != null)
            {
                bgLeftWidth = act.findViewById(bgLeftViewID).getMinimumWidth();
            }
        }

        switch (motionEvent.getActionMasked())
        {

            // When finger touches screen
            case MotionEvent.ACTION_DOWN:
            {
                if (mPaused)
                {
                    break;
                }

                Rect rect = findTouchedRecyclerViewItem(motionEvent);
                int x;
                int y;

                if (touchedView != null)
                {
                    touchedX = motionEvent.getRawX();
                    touchedY = motionEvent.getRawY();
                    touchedPosition = rView.getChildAdapterPosition(touchedView);

                    if (shouldIgnoreAction(touchedPosition))
                    {
                        if (swipeable && touchedPosition != bgVisiblePosition
                                && (bgLeftVisible || bgRightVisible))
                        {
                            closeBGView();
                        }
                        touchedPosition = ListView.INVALID_POSITION;

                        return false;
                    }


//                    if (isLongPressable())
//                    {
//                        mLongClickPerformed = false;
//                        handler.postDelayed(mLongPressed, LONG_CLICK_DELAY);
//                    }


                    if (swipeable)
                    {
                        mVelocityTracker = VelocityTracker.obtain();
                        mVelocityTracker.addMovement(motionEvent);
                        fgView = touchedView.findViewById(fgViewID);
                        bgRightView = touchedView.findViewById(bgRightViewID);
                        bgLeftView = touchedView.findViewById(bgLeftViewID);

                        /*
                        * bgRightVisible is true when the options menu is opened
                        * This block is to register fgPartialViewClicked status - Partial view is the view that is still
                        * shown on the screen if the options width is < device width
                        */
                        if (bgRightVisible && fgView != null)
                        {
//                            handler.removeCallbacks(mLongPressed);
                            x = (int) motionEvent.getRawX();
                            y = (int) motionEvent.getRawY();
                            fgView.getGlobalVisibleRect(rect);
                        }
                    }
                }

                /*
                 * If options menu is shown and the touched position is not the same as the row for which the
                 * options is displayed - close the options menu for the row which is displaying it
                 * (bgVisibleView and bgVisiblePosition is used for this purpose which registers which view and
                 * which position has it's options menu opened)
                 */
                x = (int) motionEvent.getRawX();
                y = (int) motionEvent.getRawY();
                rView.getHitRect(rect);
                if (swipeable && touchedPosition != bgVisiblePosition
                        && (bgLeftVisible || bgRightVisible))
                {
                    closeBGView();
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            {
                touchEventCanceled();
                break;
            }

            case MotionEvent.ACTION_UP:
            {

                if (mVelocityTracker == null && swipeable)
                {
                    break;
                }
                if (touchedPosition < 0)
                {
                    break;
                }

                isViewSwiped(motionEvent);

                if (swipedLeftToShowRightOption())
                {
                    swipedLeft();
                }
                else if (swipedToShowLeftOptions())
                {
                    swipedRight();
                }
                else if (isOptionClicked())
                {
                    onOptionClicked(motionEvent);
                }
                else
                {
                    closeBgOptions();
                }
            }

            if (swipeable)
            {
                mVelocityTracker.recycle();
                mVelocityTracker = null;
            }

            if (!itemSwiped)
            {
                resetParams();
            }
            break;

            case MotionEvent.ACTION_MOVE:
            {
                if (mVelocityTracker == null || mPaused || !swipeable)
                {
                    break;
                }

                mVelocityTracker.addMovement(motionEvent);
                float deltaX = motionEvent.getRawX() - touchedX;
                float deltaY = motionEvent.getRawY() - touchedY;

                if (isSwipedHorizontally(deltaX, deltaY))
                {
//                    handler.removeCallbacks(mLongPressed);
                    isFgSwiping = true;
                    mSwipingSlop = (deltaX > 0 ? touchSlop : -touchSlop);
                }

                if (isSwiping())
                {

                    bgRightView.setVisibility(View.GONE);

                    if (!ignoreLeftSwipe())
                    {
                        bgLeftView.setVisibility(View.GONE);
                    }

                    if (swipingToShowRightOptions(deltaX))
                    {
                        unveilingRightOptions(deltaX);
                    }
                    else if (swipingToCloseLeftOptions(deltaX))
                    {
                        closingLeftOptions(deltaX);
                    }
                    else if (deltaX > 0)
                    {
                        if (bgRightVisible)
                        {
                            closingRightOptions(deltaX);
                        }
                        else if (!ignoreLeftSwipe())
                        {
                            unveilingLeftOptions(deltaX);
                        }
                    }

                    return true;
                }

                break;
            }
        }
        return false;
    }

    private boolean isSwipedHorizontally(float deltaX, float deltaY)
    {
        return !isFgSwiping && Math.abs(deltaX) > touchSlop
                && Math.abs(deltaY) < Math.abs(deltaX) / 2;
    }

    private boolean isSwiping()
    {
        return swipeable && isFgSwiping;
    }

    private boolean swipingToCloseLeftOptions(float deltaX)
    {
        return deltaX < touchSlop && !bgRightVisible && !ignoreLeftSwipe() && bgLeftVisible;
    }

    private boolean swipingToShowRightOptions(float deltaX)
    {
        return deltaX < touchSlop && !bgLeftVisible;
    }

    private boolean isOptionClicked()
    {
        return !swipedRight && !swipedLeft;
    }

    private boolean swipedToShowLeftOptions()
    {
        return swipeable && !swipedLeft && swipedRightProper
                && touchedPosition != RecyclerView.NO_POSITION
                && !bgRightVisible && !ignoreLeftSwipe();
    }

    private boolean swipedLeftToShowRightOption()
    {
        return swipeable && !swipedRight && swipedLeftProper
                && touchedPosition != RecyclerView.NO_POSITION
                && !bgLeftVisible;
    }

    @NonNull
    private Rect findTouchedRecyclerViewItem(MotionEvent motionEvent)
    {
        // Find the child view that was touched (perform a hit test)
        Rect rect = new Rect();
        int childCount = rView.getChildCount();
        int[] listViewCoords = new int[2];
        rView.getLocationOnScreen(listViewCoords);
        // x and y values respective to the recycler view
        int x = (int) motionEvent.getRawX() - listViewCoords[0];
        int y = (int) motionEvent.getRawY() - listViewCoords[1];
        View child;

        for (int i = 0; i < childCount; i++)
        {
            child = rView.getChildAt(i);
            child.getHitRect(rect);
            if (rect.contains(x, y))
            {
                touchedView = child;
                break;
            }
        }
        return rect;
    }

    //==============================================================================================
    // Action Up methods
    //==============================================================================================

    private void isViewSwiped(MotionEvent motionEvent)
    {
        float mFinalDelta = motionEvent.getRawX() - touchedX;
        swipedLeft = false;
        swipedRight = false;
        swipedLeftProper = false;
        swipedRightProper = false;

        // if swiped in a direction, make that respective variable true
        if (isFgSwiping)
        {
            swipedLeft = mFinalDelta < 0;
            swipedRight = mFinalDelta > 0;
        }

        /*
         * If the user has swiped more than half of the width of the options menu, or if the
         * velocity of swiping is between min and max fling values
         * "proper" variable are set true
         */
        if ((Math.abs(mFinalDelta) > bgRightWidth / 2) && isFgSwiping)
        {
            swipedLeftProper =  mFinalDelta < 0;
        }
        if ((Math.abs(mFinalDelta) > bgLeftWidth / 2) && isFgSwiping)
        {
            swipedRightProper = mFinalDelta > 0;
        }

        if (bgLeftVisible || bgRightVisible)
        {
            swipedLeftProper = swipedLeft;
            swipedRightProper = swipedRight;
        }

        else if (swipeable)
        {
            mVelocityTracker.addMovement(motionEvent);
            mVelocityTracker.computeCurrentVelocity(1000);
            float velocityX = mVelocityTracker.getXVelocity();
            float absVelocityX = Math.abs(velocityX);
            float absVelocityY = Math.abs(mVelocityTracker.getYVelocity());

            // Determine if the view was flung
            if (minFlingVel <= absVelocityX && absVelocityX <= maxFlingVel
                    && absVelocityY < absVelocityX && isFgSwiping)
            {
                // dismiss only if flinging in the same direction as dragging
                swipedLeftProper = (velocityX < 0) == (mFinalDelta < 0);
                swipedRightProper = (velocityX > 0) == (mFinalDelta > 0);
            }
        }
    }

    private void onOptionClicked(MotionEvent motionEvent)
    {
        if (swipeable)
        {
            closeBgOptions();
        }

        mRowClickListener.onOptionClicked(getOptionViewID(rightCartItemOptionViews, motionEvent));

    }

    private void swipedRight()
    {
        bgVisibleView = fgView;

        // if stretched more than threshold
        if (removeItem)
        {
            itemSwiped = true;
            swipeDirection = SwipeDirection.RIGHT;
            final AnimatorSet animatorSet =
                    AnimationUtils.animateFullStretching(bgLeftView, fgView, screenWidth);
            animatorSet.addListener(animationFinishedListener);
        }
        else
        {
            final int downPosition = touchedPosition;

            AnimationUtils.animateShrink(bgLeftView, fgView, bgLeftWidth, null);
            setVisibility(true, false, bgVisibleView, downPosition);
        }
    }

    private void swipedLeft()
    {
        bgVisibleView = fgView;

        // if stretched more than threshold
        if (removeItem)
        {
            itemSwiped = true;
            swipeDirection = SwipeDirection.LEFT;
            final AnimatorSet animatorSet =
                    AnimationUtils.animateFullStretching(bgRightView, fgView, -screenWidth);
            animatorSet.addListener(animationFinishedListener);
        }
        else
        {
            final int downPosition = touchedPosition;

            AnimationUtils.animateShrink(bgRightView, fgView, -bgRightWidth,
                    rightCartItemOptionViews);
            setVisibility(false, true, bgVisibleView, downPosition);
        }
    }

    //==============================================================================================
    // Action Move methods
    //==============================================================================================

    private void closingRightOptions(float deltaX)
    {
        bgRightView.setVisibility(View.VISIBLE);
        float translateAmount = (deltaX - mSwipingSlop) - bgRightWidth;
        fgView.setTranslationX(translateAmount > 0 ? 0 : translateAmount);
    }

    private void closingLeftOptions(float deltaX)
    {
        bgLeftView.setVisibility(View.VISIBLE);
        float translateAmount = (deltaX - mSwipingSlop) + bgLeftWidth;
        fgView.setTranslationX(translateAmount
                < -bgRightWidth ? -bgRightWidth : translateAmount);
    }

    private void unveilingLeftOptions(float deltaX)
    {
        bgLeftView.setVisibility(View.VISIBLE);
        float translateAmount = (deltaX - mSwipingSlop);

        if (bgLeftVisible)
        {
            translateAmount = translateAmount + bgLeftWidth;
            fgView.setTranslationX(translateAmount);
        }
        else
        {
            fgView.setTranslationX(Math.abs(translateAmount));
        }

        bgRightView.setVisibility(View.GONE);

        if (fgView.getX() > bgLeftWidth)
        {
            final ViewGroup.LayoutParams layoutParams = bgLeftView.getLayoutParams();

            layoutParams.width = (int) fgView.getX();
            bgLeftView.setLayoutParams(layoutParams);
            bgLeftView.requestLayout();
        }

        removeItem = fgView.getX() > screenWidth / 2;

    }

    private void unveilingRightOptions(float deltaX)
    {
        float translateAmount = deltaX - mSwipingSlop;
        bgRightView.setVisibility(View.VISIBLE);

        if (!ignoreLeftSwipe())
        {
            bgLeftView.setVisibility(View.GONE);
        }
        // swipe fg till width of bg. If swiped further, nothing happens (stalls at width of bg)

        if (bgRightVisible)
        {
            translateAmount = translateAmount - bgRightWidth;
            fgView.setTranslationX(translateAmount);
        }
        else
        {
            fgView.setTranslationX(translateAmount);
        }

        if (Math.abs(fgView.getX()) > bgRightWidth)
        {
            final int stretchingWidth = (int) Math.abs(fgView.getX());
            final float  translationPercent =
                    (Math.abs(translateAmount) - bgRightWidth) / (bgLeftWidth / 4);

            removeItem =
                    AnimationUtils.animateStretching(translationPercent,
                            bgRightView, stretchingWidth, rightCartItemOptionViews);
        }
        else
        {
            resetStretching();
        }
    }

    private void resetStretching()
    {
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);

        for (int viewID : rightCartItemOptionViews)
        {
            bgRightView.findViewById(viewID).setLayoutParams(param);
        }
    }


    //==============================================================================================
    // Action Cancel methods
    //==============================================================================================

    private void touchEventCanceled()
    {
        if (mVelocityTracker == null)
        {
            return;
        }
        if (swipeable)
        {
            if (touchedView != null && isFgSwiping)
            {
                // cancel
                animateFG(Animation.CLOSE, -bgRightWidth);
            }
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
        resetParams();
    }

    private void setVisibility(boolean bgLeftVisible, boolean bgRightVisible,
                               View bgVisibleView, int bgVisiblePosition)
    {
        this.bgLeftVisible = bgLeftVisible;
        this.bgRightVisible = bgRightVisible;
        this.bgVisibleView = bgVisibleView;
        this.bgVisiblePosition = bgVisiblePosition;
    }

    private void resetParams()
    {
        touchedX = 0;
        touchedY = 0;
        touchedView = null;
        isFgSwiping = false;
        bgRightView = null;
        itemSwiped = false;
        removeItem = false;
    }

    private boolean shouldIgnoreAction(int touchedPosition)
    {
        return rView == null && touchedPosition != ListView.INVALID_POSITION
                || ignoredViewTypes.contains(rView.getAdapter().getItemViewType(touchedPosition));
    }

    private enum Animation
    {
        OPEN, CLOSE
    }

    private enum SwipeDirection
    {
        LEFT, RIGHT
    }

    //==============================================================================================
    //  Interfaces
    //==============================================================================================

    public interface OnRowClickListener
    {
        void onSwipedRight(int position);

        void onSwipedLeft(int position);

        void onOptionClicked(int optionID);

        void onRowClicked(int position);
    }

}
