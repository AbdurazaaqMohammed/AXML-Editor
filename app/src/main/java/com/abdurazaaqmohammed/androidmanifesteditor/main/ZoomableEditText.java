package com.abdurazaaqmohammed.androidmanifesteditor.main;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.EditText;

public class ZoomableEditText extends EditText {
    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.0f;
    // Other necessary fields (e.g., drawing tools, touch coordinates)

    public ZoomableEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            mScaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.scale(mScaleFactor, mScaleFactor);
        super.onDraw(canvas);
        canvas.restore();
    }

    final static float move = 200;
    float ratio = 1.0f;
    int bastDst;
    float baseratio;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
            if (event.getPointerCount() == 2) {
                int action = event.getAction();
                int mainaction = action & MotionEvent.ACTION_MASK;
                if (mainaction == MotionEvent.ACTION_POINTER_DOWN) {
                    bastDst = getDistance(event);
                    baseratio = ratio;
                } else {
                    // if ACTION_POINTER_UP then after finding the distance
                    // we will increase the text size by 15
                    float scale = (getDistance(event) - bastDst) / move;
                    float factor = (float) Math.pow(2, scale);
                    ratio = Math.min(1024.0f, Math.max(0.1f, baseratio * factor));
                    this.setTextSize(ratio + 15);
                }
            }
        }
        return true;
    }

    // get distance between the touch event
    @TargetApi(Build.VERSION_CODES.ECLAIR)
    private int getDistance(MotionEvent event) {
        int dx = (int) (event.getX(0) - event.getX(1));
        int dy = (int) (event.getY(0) - event.getY(1));
        return (int) Math.sqrt(dx * dx + dy * dy);
    }
    @TargetApi(Build.VERSION_CODES.FROYO)
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            // Limit the scale factor if needed
            // ...
            invalidate(); // Redraw the view
            return true;
        }
    }
}