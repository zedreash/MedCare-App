package com.medcare.app.ui.calendar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

public class SwipeFrameLayout extends FrameLayout {

    public interface OnSwipeListener {
        void onSwipe(int direction);
    }

    private OnSwipeListener listener;
    private boolean swipeEnabled = true;
    private float downX;
    private float downY;
    private boolean intercepting = false;
    private final int touchSlop;

    public SwipeFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!swipeEnabled) return super.onInterceptTouchEvent(ev);
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                intercepting = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = ev.getX() - downX;
                float dy = ev.getY() - downY;
                if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                    intercepting = true;
                    return true;
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!swipeEnabled) return super.onTouchEvent(ev);
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                intercepting = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (intercepting) return true;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                boolean wasIntercepting = intercepting;
                intercepting = false;
                float dx = ev.getX() - downX;
                float dy = ev.getY() - downY;
                if (Math.abs(dx) > touchSlop * 2 && Math.abs(dx) > Math.abs(dy) * 1.2f
                        && listener != null) {
                    listener.onSwipe(dx < 0 ? 1 : -1);
                    return true;
                }
                if (wasIntercepting) return true;
                break;
        }
        return super.onTouchEvent(ev);
    }

    public void setSwipeListener(OnSwipeListener listener) {
        this.listener = listener;
    }

    public void setSwipeEnabled(boolean enabled) {
        this.swipeEnabled = enabled;
    }
}