package com.mattaniahbeezy.hangup;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.RelativeLayout;

/**
 * Created by Beezy Works Studios on 6/4/2017.
 */

public class FourSideSlideLayout extends RelativeLayout implements View.OnTouchListener {
    private final int LEFT_SLIDE_VIEW = 0;
    private final int RIGHT_SLIDE_VIEW = 1;
    private final int TOP_SLIDE_VIEW = 2;
    private final int BOTTOM_SLIDE_VIEW = 3;

    private final int NONE_SELECTED = -1;
    private View[] slideViews = new View[4];
    float[] actionPoints = new float[4];
    private int currentSlideView = NONE_SELECTED;
    private float touchMargin;
    private boolean isAnimatingOpen;
    private float grabPadding;

    interface SlideCompleteListener {
        void onLeftSlide();

        void onRightSlide();

        void onTopSlide();

        void onBottomSlide();
    }

    SlideCompleteListener completionListener;

    public void setCompletionListener(SlideCompleteListener completionListener) {
        this.completionListener = completionListener;
    }

    public FourSideSlideLayout(Context context) {
        super(context);
        initialize();
    }

    public FourSideSlideLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public FourSideSlideLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        touchMargin = getContext().getResources().getDimension(R.dimen.slide_button_touch_margin);
        grabPadding = getContext().getResources().getDimension(R.dimen.slide_button_grab_padding);
        for (int i = 0; i < slideViews.length; i++) {
            slideViews[i] = createSlideView();
            addView(slideViews[i]);
            setSlideViewClosed(i);
        }

        slideViews[LEFT_SLIDE_VIEW].setBackgroundColor(ContextCompat.getColor(getContext(), R.color.red));
        setOnTouchListener(this);
    }

    private float getClosedPosition(int slideViewIndex) {
        switch (slideViewIndex) {
            case LEFT_SLIDE_VIEW:
                return -slideViews[slideViewIndex].getWidth();
            case TOP_SLIDE_VIEW:
                return -slideViews[slideViewIndex].getHeight();
            case RIGHT_SLIDE_VIEW:
                return slideViews[slideViewIndex].getWidth() * 2;
            case BOTTOM_SLIDE_VIEW:
                return slideViews[slideViewIndex].getHeight() * 2;
        }
        return 0;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        actionPoints[LEFT_SLIDE_VIEW] = getWidth() * .6f;
        actionPoints[TOP_SLIDE_VIEW] = getHeight() * .6f;
        actionPoints[RIGHT_SLIDE_VIEW] = getWidth() * .4f;
        actionPoints[BOTTOM_SLIDE_VIEW] = getHeight() * .4f;
        closeAllSlideViews();
    }

    private boolean isSlideViewVertical(int slideViewIndex) {
        return slideViewIndex == BOTTOM_SLIDE_VIEW || slideViewIndex == TOP_SLIDE_VIEW;
    }

    private View createSlideView() {
        View slideView = new View(getContext());
        slideView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        slideView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.dark_grey));
        slideView.setAlpha(0.8f);
        return slideView;
    }

    private void closeAllSlideViews(){
        for(int i = 0 ;i<slideViews.length;i++){
            setSlideViewClosed(i);
        }
    }

    private void setSlideViewClosed(int slideViewIndex) {
        if (isSlideViewVertical(slideViewIndex)) {
            slideViews[slideViewIndex].setTranslationY(getClosedPosition(slideViewIndex));
        } else {
            slideViews[slideViewIndex].setTranslationX(getClosedPosition(slideViewIndex));
        }
    }

    private void animateSlideViewClosed(int slideViewIndex) {
        long duration = 200;
        TimeInterpolator interpolator = new AccelerateInterpolator();
        if (isSlideViewVertical(slideViewIndex)) {
            slideViews[slideViewIndex].animate().translationY(getClosedPosition(slideViewIndex)).setDuration(duration).setInterpolator(interpolator).start();
        } else {
            slideViews[slideViewIndex].animate().translationX(getClosedPosition(slideViewIndex)).setDuration(duration).setInterpolator(interpolator).start();
        }
    }

    private void handleMove(float newPoint) {
        if ((currentSlideView == TOP_SLIDE_VIEW || currentSlideView == LEFT_SLIDE_VIEW) && newPoint >= actionPoints[currentSlideView]) {
            performAction();
        }
         if ((currentSlideView == BOTTOM_SLIDE_VIEW || currentSlideView == RIGHT_SLIDE_VIEW) && newPoint <= actionPoints[currentSlideView]) {
            performAction();
        }  {
            switch (currentSlideView){
                case LEFT_SLIDE_VIEW:
                    slideViews[currentSlideView].setTranslationX(-slideViews[currentSlideView].getWidth() + (newPoint+grabPadding));
                    break;
                case TOP_SLIDE_VIEW:
                    slideViews[currentSlideView].setTranslationY(-slideViews[currentSlideView].getHeight() + (newPoint+grabPadding));
                    break;
                case RIGHT_SLIDE_VIEW:
                    slideViews[currentSlideView].setTranslationX((newPoint-grabPadding));
                    break;
                case BOTTOM_SLIDE_VIEW:
                    slideViews[currentSlideView].setTranslationY((newPoint-grabPadding));
                    break;
            }
        }
    }

    private void animateOpen() {
        isAnimatingOpen = true;
        TimeInterpolator interpolator = new AccelerateInterpolator();
        long duration = 200;
        android.animation.Animator.AnimatorListener listener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                isAnimatingOpen = false;
                closeAllSlideViews();
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        };

        if (isSlideViewVertical(currentSlideView)) {
            slideViews[currentSlideView].animate().translationY(0).setDuration(duration).setInterpolator(interpolator).setListener(listener).start();
        } else {
            slideViews[currentSlideView].animate().translationX(0).setDuration(duration).setInterpolator(interpolator).setListener(listener).start();
        }
    }

    private void performAction() {
        animateOpen();
        if (completionListener != null) {
            switch (currentSlideView) {
                case LEFT_SLIDE_VIEW:
                    completionListener.onLeftSlide();
                    break;
                case TOP_SLIDE_VIEW:
                    completionListener.onTopSlide();
                    break;
                case RIGHT_SLIDE_VIEW:
                    completionListener.onRightSlide();
                    break;
                case BOTTOM_SLIDE_VIEW:
                    completionListener.onBottomSlide();
                    break;
            }
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                float x = motionEvent.getRawX();
                float y = motionEvent.getRawY();
                if (x <= touchMargin) {
                    currentSlideView = LEFT_SLIDE_VIEW;
                } else if (x >= getWidth() - touchMargin) {
                    currentSlideView = RIGHT_SLIDE_VIEW;
                } else if (y <= touchMargin) {
                    currentSlideView = TOP_SLIDE_VIEW;
                } else if (y >= getHeight() - touchMargin) {
                    currentSlideView = BOTTOM_SLIDE_VIEW;
                } else {
                    currentSlideView = NONE_SELECTED;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (currentSlideView == NONE_SELECTED || isAnimatingOpen) break;
                float newPoint = isSlideViewVertical(currentSlideView) ? motionEvent.getRawY() : motionEvent.getRawX();
                handleMove(newPoint);
                break;
            case MotionEvent.ACTION_UP:
                if (currentSlideView == NONE_SELECTED) break;
                if (!isAnimatingOpen) {
                    animateSlideViewClosed(currentSlideView);
                }
                currentSlideView = NONE_SELECTED;
                break;
        }
        return true;
    }
}