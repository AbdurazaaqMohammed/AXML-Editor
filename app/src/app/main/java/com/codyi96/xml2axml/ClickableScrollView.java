package com.codyi96.xml2axml;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class ClickableScrollView extends ScrollView {

    public ClickableScrollView(Context context) {
        super(context);
    }

    public ClickableScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ClickableScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Intercept touch events to detect clicks on the scrollbar
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            float y = ev.getY();
            if (y < getTop() + getVerticalScrollbarWidth()) {
                // Click detected on the top scrollbar
                fullScroll(ScrollView.FOCUS_UP);
                return true;
            } else if (y > getBottom() - getVerticalScrollbarWidth()) {
                // Click detected on the bottom scrollbar
                fullScroll(ScrollView.FOCUS_DOWN);
                return true;
            }
        }
        return super.onTouchEvent(ev);
    }
}
