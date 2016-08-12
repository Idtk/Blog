package com.idtk.customscroll;

import android.content.Context;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

/**
 * Created by Idtk on 2016/7/17.
 * Blog : http://www.idtkm.com
 * GitHub : https://github.com/Idtk
 * 描述 : 具有弹性滑动效果的ViewGroup
 */
public class ParentView extends ViewGroup {

    private Scroller mScroller;
    private int mTouchSlop;

    private float mLastXIntercept=0;
    private float mLastYIntercept=0;

    private float mLastX=0;
    private float mLastY=0;

    private int leftBorder;
    private int rightBorder;

    public ParentView(Context context) {
        super(context);
        init(context);
    }

    public ParentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ParentView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context){
        // 第一步，创建Scroller的实例
        mScroller = new Scroller(context);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        // 获取TouchSlop值
        mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Log.i("TAG","ViewGroup dispatchTouchEvent,action "+ToFlag.toFlage(ev)+" ");
        boolean result = super.dispatchTouchEvent(ev);
        Log.i("TAG","ViewGroup dispatchTouchEvent return "+result);
        return result;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercept = false;
        float xIntercept = ev.getX();
        float yIntercept = ev.getY();

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                intercept = false;
                //当滑动没有完成时，拦截事件
                if (!mScroller.isFinished()){
                    mScroller.abortAnimation();
//                    intercept = true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaX = xIntercept-mLastXIntercept;
                float deltaY = yIntercept-mLastYIntercept;
                // 当水平方向的滑动距离大于竖直方向的滑动距离，且手指拖动值大于TouchSlop值时，拦截事件
                if (Math.abs(deltaX)>Math.abs(deltaY) && Math.abs(deltaX)>mTouchSlop) {
//                    intercept=true;
                }else {
                    intercept = false;
                }
                break;
            case MotionEvent.ACTION_UP:
                intercept = false;
                break;
            default:
                break;
        }

        mLastX = xIntercept;
        mLastY = yIntercept;
        mLastXIntercept = xIntercept;
        mLastYIntercept = yIntercept;

//        intercept=true;
        Log.i("TAG","ViewGroup onInterceptTouchEvent,action "+ToFlag.toFlage(ev)+" ");
        Log.i("TAG","ViewGroup onInterceptTouchEvent,return "+intercept);
        return intercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float xTouch = event.getX();
        float yTouch = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished())
                    mScroller.abortAnimation();
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaX = xTouch-mLastX;
                float deltaY = yTouch-mLastY;
                float scrollByStart = deltaX;
                if (getScrollX() - deltaX < leftBorder) {
                    scrollByStart = deltaX/3;
                } else if (getScrollX() + getWidth() - deltaX > rightBorder) {
                    scrollByStart = deltaX/3;
                }
                scrollBy((int) -scrollByStart, 0);
                break;
            case MotionEvent.ACTION_UP:
                // 当手指抬起时，根据当前的滚动值来判定应该滚动到哪个子控件的界面
                int targetIndex = (getScrollX() + getWidth() / 2) / getWidth();
                if (targetIndex>getChildCount()-1){
                    targetIndex = getChildCount()-1;
                }else if (targetIndex<0){
                    targetIndex =0;
                }
                int dx = targetIndex * getWidth() - getScrollX();
                // 第二步，使用startScroll方法，对其进行初始化
                mScroller.startScroll(getScrollX(), 0, dx, 0,1000);
                invalidate();
                break;
            default:
                break;
        }

        mLastX = xTouch;
        mLastY = yTouch;

        Log.i("TAG","ViewGroup onTouchEvent,action "+ToFlag.toFlage(event)+" ");
        boolean result = super.onTouchEvent(event);
        Log.i("TAG","ViewGroup onTouchEvent,return "+result+" ");
        return result;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            // 测量每一个子控件的大小
            measureChild(childView, widthMeasureSpec, heightMeasureSpec);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childView = getChildAt(i);
                // 在水平方向上对子控件进行布局
                childView.layout(i * getMeasuredWidth(), 0, i * getMeasuredWidth()+childView.getMeasuredWidth()+getPaddingLeft(), childView.getMeasuredHeight());
            }
            // 初始化左右边界值
            leftBorder = 0;
            rightBorder = getChildCount()*getMeasuredWidth();
        }
    }

    @Override
    public void computeScroll() {
        // 第三步，重写computeScroll()方法，在其内部调用scrollTo或ScrollBy方法，完成滑动过程
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            invalidate();
        }
    }
}
