# View的弹性滑动

**滑动是Android开发中非常重要的UI效果，几乎所有应用都包含了滑动效果，而本文将对滑动的使用以及原理进行介绍。**

[自定义View系列目录](https://github.com/Idtk/Blog)


## 一、scrollTo与ScrollBy
View提供了专门的方法用于实现滑动效果，分别为scrollTo与scrollBy。先来看看它们的源码：
```Java
/**
 * Set the scrolled position of your view. This will cause a call to
 * {@link #onScrollChanged(int, int, int, int)} and the view will be
 * invalidated.
 * @param x the x position to scroll to
 * @param y the y position to scroll to
 */
public void scrollTo(int x, int y) {
    if (mScrollX != x || mScrollY != y) {
        int oldX = mScrollX;
        int oldY = mScrollY;
        mScrollX = x;
        mScrollY = y;
        invalidateParentCaches();
        onScrollChanged(mScrollX, mScrollY, oldX, oldY);
        if (!awakenScrollBars()) {
            postInvalidateOnAnimation();
        }
    }
}


/**
 * Move the scrolled position of your view. This will cause a call to
 * {@link #onScrollChanged(int, int, int, int)} and the view will be
 * invalidated.
 * @param x the amount of pixels to scroll by horizontally
 * @param y the amount of pixels to scroll by vertically
 */
public void scrollBy(int x, int y) {
    scrollTo(mScrollX + x, mScrollY + y);
}
```
从源码中可以看出scrollBy实际上是调用了scrollTo函数来实现它的功能。scrollBy实现的是输入参数的相对滑动，而scrollTo是绝对滑动。需要说明的是mScrollX、mScrollY这两个View的属性，这两个属性可以通过getScrollX、getScrollY获得。<br>

* mScrollX : View的左边缘在View内容的左边缘的右边时，为正值，反之为负值。
* mScrollY : View的上边缘在View内容的上边缘的下边时，为正值，反之为负值。
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/mScrollXY.png" alt="mScrollXY" title="mScrollXY" width="500" />
<br>
下面我们来实现一个滑动的效果：
```Java
public class HorizontalScroller extends ViewGroup {

    private int mTouchSlop;

    private float mLastXIntercept=0;
    private float mLastYIntercept=0;

    private float mLastX=0;
    private float mLastY=0;

    private int leftBorder;
    private int rightBorder;

    public HorizontalScroller(Context context) {
        super(context);
        init(context);
    }

    public HorizontalScroller(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HorizontalScroller(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context){
        ViewConfiguration configuration = ViewConfiguration.get(context);
        // 获取TouchSlop值
        mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercept = false;
        float xIntercept = ev.getX();
        float yIntercept = ev.getY();

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                intercept = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaX = xIntercept-mLastXIntercept;
                float deltaY = yIntercept-mLastYIntercept;
                // 当水平方向的滑动距离大于竖直方向的滑动距离，且手指拖动值大于TouchSlop值时，拦截事件
                if (Math.abs(deltaX)>Math.abs(deltaY) && Math.abs(deltaX)>mTouchSlop) {
                    intercept=true;
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

        return intercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float xTouch = event.getX();
        float yTouch = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaX = xTouch-mLastX;
                float deltaY = yTouch-mLastY;
                float scrollByStart = deltaX;
                if (getScrollX() - deltaX < leftBorder) {
                    scrollByStart = getScrollX()-leftBorder;
                } else if (getScrollX() + getWidth() - deltaX > rightBorder) {
                    scrollByStart = rightBorder-getWidth()-getScrollX();
                }
                scrollBy((int) -scrollByStart, 0);
                break;
            case MotionEvent.ACTION_UP:
                // 当手指抬起时，根据当前的滚动值来判定应该滚动到哪个子控件的界面
                int targetIndex = (getScrollX() + getWidth() / 2) / getWidth();
                int dx = targetIndex * getWidth() - getScrollX();
                scrollTo(getScrollX()+dx,0);
                break;
            default:
                break;
        }

        mLastX = xTouch;
        mLastY = yTouch;

        return super.onTouchEvent(event);
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
}
```
现在我们来分析下这段代码:<br>

* 首先在构造函数中获取了最小滑动距离TouchSlop。
* 重写onInterceptTouchEvent拦截事件，记录当前坐标。点下时，默认不拦截，只有当滑动还未完成的情况下，才继续拦截。在移动时，对滑动冲突进行了处理，当水平方向的移动距离大于竖直方向的移动距离，并且移动距离大于最小滑动距离时，我们判断此时为水平滑动，拦截事件自己处理；否则不拦截，交由子View处理。提起手指时，同样不拦截事件。
* 重写onTouchEvent处理事件，记录当前坐标。在手指按下时，与拦截事件时做相似处理。在ACTION_MOVE时，向左滑动，如果滑动距离超过左边界，则对滑动距离进行处理，相对的滑动距离超出又边界，也是一样处理，之后把滑动的距离交给scrollBy进行处理。当手指抬起时，根据当前的滚动值来判定应该滚动到哪个子控件的界面，然后使用scrollTo滑动到那个子控件。
* 重写了onMeasure和onLayout方法，在onMeasure中测量每一个子控件的大小值，在onLayout中对每一个子view在水平方向上进行布局。子view的layout的right增加父类的paddingLeft参数，来处理设置padding的情况。这两个函数的流程分析将会放在之后的文章中详细说明。

这个类的使用方法如下 : 
```Java
<?xml version="1.0" encoding="utf-8"?>
<com.idtk.customscroll.HorizontalScroller
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="10dp"
    tools:context="com.idtk.customscroll.MainActivity">

    <ImageView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:src="@drawable/zhiqinchun"
        android:clickable="true"/>

    <ImageView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:src="@drawable/hanzhan"
        android:clickable="true"/>

    <ImageView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:src="@drawable/shengui"
        android:clickable="true"/>

    <ImageView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:src="@drawable/dayu"
        android:clickable="true"/>

</com.idtk.customscroll.HorizontalScroller>
```
HorizontalScroller设置全屏，padding为10dp。使用4个ImageView作为子View，并且都设置为可点击状态。示例效果图如下:<br>
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/scroll0.gif" alt="scroll" title="scroll" width="300" />
<br>


## 二、Scroller
可以看到上面使用scrollTo与ScrollBy方法的滑动都是瞬时完成的，这有些无法满足我们在切换子view时的需求。我们希望切换子View时，可以拥有滑动过程的效果，而Scroller正好可以完成这一点。
Scroller的使用方法：
* 1、创建Scroller实例
* 2、使用startScroll方法，对其进行初始化
* 3、重写computeScroll()方法，在其内部调用scrollTo或ScrollBy方法，完成滑动过程。

```Java
//创建实例
mScroller = new Scroller(context);

public void smoothScrollTo(){
    int ScrollX = getScrollX();
    int ScrollY = getScrollY();
	//初始化，1000ms内缓慢滑动到deltaX
    mScroller.startScroll(ScrollX, 0, 0, deltaX, 1000);
    invalidate();
}

@Override
public void computeScroll() {
    if(mScroller.computeScrollOffset()){
        int currX = mScroller.getCurrX();
        int currY = mScroller.getCurrY();
        scrollTo(currX, currY);
        postInvalidate();
    }
}
```

上面的代码是Scroller的典型用法，也就是传说中的套路。当时Scroller使用startScroll方法时，只是对一系列参数进行了初始化。我们从下面的源码中可以看出。
```Java
public void startScroll(int startX, int startY, int dx, int dy, int duration) {
    mMode = SCROLL_MODE;
    mFinished = false;
    mDuration = duration;
    mStartTime = AnimationUtils.currentAnimationTimeMillis();
    mStartX = startX;
    mStartY = startY;
    mFinalX = startX + dx;
    mFinalY = startY + dy;
    mDeltaX = dx;
    mDeltaY = dy;
    mDurationReciprocal = 1.0f / (float) mDuration;
}
```
参数中，startX、startY是滑动的起点，dx、dy是滑动的距离，duration是滑动的时间系统设置为250ms。我们可以看到startScroll只是进行了滑动时间、是否滑动完成、起点、终点、滑动距离等的参数的设置，那么是如何调用computeScroll()函数的呢？其实computeScroll()的调用是由之后的invalidate()函数来完成的，invalidate可以请求View重绘，在View重绘时会调用draw方法，draw方法又会去调用computeScroll函数。但computeScroll()函数在view中是一个空的函数，需要我们去实现它。<br>
computeScroll()函数的实现已经在上面给出了，有了computeScroll方法之后，就可以实现View的弹性滑动了。来看下computeScroll()的实现过程，首先要进行computeScrollOffset()的判断，来看下它的源码 : 

```Java
public boolean computeScrollOffset() {
    if (mFinished) {
        return false;
    }
    int timePassed = (int)(AnimationUtils.currentAnimationTimeMillis() - mStartTime);
    if (timePassed < mDuration) {
        switch (mMode) {
        case SCROLL_MODE:
            final float x = mInterpolator.getInterpolation(timePassed * mDurationReciprocal);
            mCurrX = mStartX + Math.round(x * mDeltaX);
            mCurrY = mStartY + Math.round(x * mDeltaY);
            break;
        case FLING_MODE:
            final float t = (float) timePassed / mDuration;
            final int index = (int) (NB_SAMPLES * t);
            float distanceCoef = 1.f;
            float velocityCoef = 0.f;
            if (index < NB_SAMPLES) {
                final float t_inf = (float) index / NB_SAMPLES;
                final float t_sup = (float) (index + 1) / NB_SAMPLES;
                final float d_inf = SPLINE_POSITION[index];
                final float d_sup = SPLINE_POSITION[index + 1];
                velocityCoef = (d_sup - d_inf) / (t_sup - t_inf);
                distanceCoef = d_inf + (t - t_inf) * velocityCoef;
            }
            mCurrVelocity = velocityCoef * mDistance / mDuration * 1000.0f;
            
            mCurrX = mStartX + Math.round(distanceCoef * (mFinalX - mStartX));
            // Pin to mMinX <= mCurrX <= mMaxX
            mCurrX = Math.min(mCurrX, mMaxX);
            mCurrX = Math.max(mCurrX, mMinX);
            
            mCurrY = mStartY + Math.round(distanceCoef * (mFinalY - mStartY));
            // Pin to mMinY <= mCurrY <= mMaxY
            mCurrY = Math.min(mCurrY, mMaxY);
            mCurrY = Math.max(mCurrY, mMinY);
            if (mCurrX == mFinalX && mCurrY == mFinalY) {
                mFinished = true;
            }
            break;
        }
    }
    else {
        mCurrX = mFinalX;
        mCurrY = mFinalY;
        mFinished = true;
    }
    return true;
}
```
computeScrollOffset()首先检测scroller是否完成滑动，完成则返回false，未完成则继续AnimationUtils.currentAnimationTimeMillis获取当前的毫秒值，减去之前startScroll方法时获得毫秒值，就是当前滑动的执行时间。之后判断执行时间是否小于设置的总时间，如果小于，根据startScroll时设置的模式SCROLL_MODE，然后根据Interpolator计算出当前滑动的mcurrX、mcurrY（顺便提一下在实例化scroller的时候，是可以设置动画插值器。）；如果执行时间大于或者等于设置的总时间，则直接设置mcurrX、mcurrY为终点值，并且设置mFinished，表示动画已经完成。<br>
Scroller弹性滑动的流程如下<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/scroller.png" alt="scroller" title="scroller" width="300" />
<br>
现在使用Scroller方法来更改一下上面的代码，当ACTION_UP时，子View的滑动可以有一个过程，而不是瞬时完成。

```Java

    private Scroller mScroller;
	...
    private void init(Context context){
        // 第一步，创建Scroller的实例
        mScroller = new Scroller(context);
        ...
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float xTouch = event.getX();
        float yTouch = event.getY();
        switch (event.getAction()) {
            ...
            case MotionEvent.ACTION_UP:
                // 当手指抬起时，根据当前的滚动值来判定应该滚动到哪个子控件的界面
                int targetIndex = (getScrollX() + getWidth() / 2) / getWidth();
                int dx = targetIndex * getWidth() - getScrollX();
                // 第二步，使用startScroll方法，对其进行初始化
                mScroller.startScroll(getScrollX(), 0, dx, 0);
                invalidate();
                break;
			default:
                break;
        }

        mLastX = xTouch;
        mLastY = yTouch;

        return super.onTouchEvent(event);
    }
    ...
    @Override
    public void computeScroll() {
        // 第三步，重写computeScroll()方法，在其内部调用scrollTo或ScrollBy方法，完成滑动过程
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
    }
}
```
上面就是代码中需要增加和修改的部分，我们来简单分析下。<br>

* 在构造函数中增加对**Scroller进行了实例化**。
* 替换onTouchEvent中手指抬起后的方法，改为**使用startScroll方法，对mScroller进行初始化**，之后invalidate请求重绘。
* 增加**重写的computeScroll()方法**，在其内部调用scrollTo或ScrollBy方法，完成滑动过程，之后使用postInvalidate()请求view重绘。

示例效果图如下 : <br>
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/scroll.gif" alt="scroll" title="scroll" width="300" />
<br>

## 三、回弹效果
从上面的效果图可以看出，我们已经实现了view的平滑滚动，滑动位置超过当前view的1/2时，松手之后变会自动滑出此item的View。可是如果想要在首位两端实现回弹效果，该如何做呢？其实只要修改onTouchEvent方法即可。
```Java
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
			//如果超出边界，则把滑动距离缩小到1/3
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
			//如果超过右边界，则回弹到最后一个View
            if (targetIndex>getChildCount()-1){
                targetIndex = getChildCount()-1;
			//如果超过左边界，则回弹到第一个View
            }else if (targetIndex<0){
                targetIndex =0;
            }
            int dx = targetIndex * getWidth() - getScrollX();
            // 第二步，使用startScroll方法，对其进行初始化
            mScroller.startScroll(getScrollX(), 0, dx, 0);
            invalidate();
            break;
        default:
            break;
    }
    mLastX = xTouch;
    mLastY = yTouch;
    return super.onTouchEvent(event);
}
```
来简单分析下修改的onTouchEvent方法:<br>

在滑动的过程中，如果滑动的位置超过了试图的左、右边界，则缩小View的滑动距离，使之为手指滑动距离的1/3。当手指离开时，如果通过view宽度获得的当前inder小与0，则index为第一个View；如果获得的当前index超过了子View的数量-1，则index为最后一个View。View的回弹效果如下:<br>
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/scroll2.gif" alt="scroll" title="scroll" width="300" />
<br>

## 四、小结
本文介绍弹性滑动的实现方法，并对弹性滑动的过程进行了详细分析。在之后通过例子实现了view的弹性滑动以及回弹效果，但**最后还留有两个问题，即invalidate与postInvalidate的区别又在哪里呢？invalidate是如何调用computeScroll()函数的呢？**，这些问题我将在下一篇文章中进行详细的分析。<br>
如果在阅读过程中，有任何疑问与问题，欢迎与我联系。<br>

**博客:www.idtkm.com**<br>
**GitHub:https://github.com/Idtk**<br>
**微博:http://weibo.com/Idtk**<br>
**邮箱:IdtkMa@gmail.com**<br>
<br>
