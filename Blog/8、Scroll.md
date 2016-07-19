# 自定义View——弹性滑动引起的invalidate流程分析

## 一、弹性滑动
### 1、scrollTo与ScrollBy
View拥有两个函数API来实现滑动功能，分别为scrollTo与scrollBy。下面是它们的源码：
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

* mScrollX : View的左边缘与View内容的左边缘在水平方向上的距离，即从右向左滑动时，为正值，反之为负值。
* mScrollY : View的上边缘与View内容的上边缘在竖直方向上的距离，即从下向上滑动时，为正值，反之为负值。
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/mScrollXY.png" alt="mScrollXY" title="mScrollXY" width="500" />
<br>

### 2、Scroller
已经有了scrollTo与ScrollBy方法了，为什么还要介绍scroller呢？因为scrollTo与ScrollBy方法的滑动都是瞬时完成的，但我需要的是拥有滑动过程的效果，而Scroller正好可以完成这一点。
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
下面用一个Scroller使用的简单示例。

```Java
public class HorizontalScroller extends ViewGroup {

    private Scroller mScroller;
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
        // 第一步，创建Scroller的实例
        mScroller = new Scroller(context);
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
                //当滑动没有完成时，拦截事件
                if (!mScroller.isFinished()){
                    mScroller.abortAnimation();
                    intercept = true;
                }
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
                if (!mScroller.isFinished())
                    mScroller.abortAnimation();
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

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int childCount = getChildCount();

        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            // 测量每一个子View的大小
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
                // 在水平方向上对子View进行布局
                childView.layout(i * getMeasuredWidth(), 0, i * getMeasuredWidth()+childView.getMeasuredWidth()++getPaddingLeft(), childView.getMeasuredHeight());
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
            postInvalidate();
        }
    }
}
```
上面的示例代码并不长，来分析下这段代码。<br>

* 首先在构造函数中对**Scroller进行了实例化**，并获取了最小滑动距离TouchSlop。
* 重写onInterceptTouchEvent拦截事件，记录当前坐标。点下时，默认不拦截，只有当滑动还未完成的情况下，才继续拦截。在移动时，对滑动冲突进行了处理，当水平方向的移动距离大于竖直方向的移动距离，并且移动距离大于最小滑动距离时，我们判断此时为水平滑动，拦截事件自己处理；否则不拦截，交由子View处理。提起手指时，同样不拦截事件。
* 重写onTouchEvent处理事件，记录当前坐标。在手指按下时，与拦截事件时做相似处理。在ACTION_MOVE时，向左滑动，如果滑动距离超过左边界，则对滑动距离进行处理，相对的滑动距离超出又边界，也是一样处理。当手指抬起时，根据当前的滚动值来判定应该滚动到哪个子控件的界面，然后**使用startScroll方法，对mScroller进行初始化**，之后invalidate请求重绘。
* 重写了onMeasure和onLayout方法，在onMeasure中测量每一个子控件的大小值，在onLayout中对每一个子view在水平方向上进行布局。子view的layout的right增加父类的paddingLeft参数，来处理设置padding的情况。这两个函数的流程分析将会放在之后的文章中详细说明。
* 重写重写computeScroll()方法，在其内部调用scrollTo或ScrollBy方法，完成滑动过程，之后使用postInvalidate()请求view重绘。

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
HorizontalScroller设置全屏，padding为10dp。使用4个ImageView作为子View，并且都设置为可点击状态。示例效果图如下 : <br>
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/scroll.gif" alt="scroll" title="scroll" width="300" />
<br>

## 二、invalidate与postInvalidate
invalidate与postInvadlidate都是用于请求View重绘的API，invalidate在主线程中进行调用，而postInvadlidate则在子线程中进行调用。<br>
来分析下postInvadlidate的源码 : 
```Java
public void postInvalidate() {
    postInvalidateDelayed(0);
}
```
postInvalidate()蒋会调用postInvalidateDelayed(0)方法，继续跟进。
```Java
public void postInvalidateDelayed(long delayMilliseconds) {
    final AttachInfo attachInfo = mAttachInfo;
    if (attachInfo != null) {
        attachInfo.mViewRootImpl.dispatchInvalidateDelayed(this, delayMilliseconds);
    }
}
```
postInvalidateDelayed函数，通过attachInfo获取到当前的ViewRootImpl对象，调用它的dispatchInvalidateDelayed，现在再来看看dispatchInvalidateDelayed方法
```Java
public void dispatchInvalidateDelayed(View view, long delayMilliseconds) {
    Message msg = mHandler.obtainMessage(MSG_INVALIDATE, view);
    mHandler.sendMessageDelayed(msg, delayMilliseconds);
}
```
从上面的源码已经可以看出，postInvalidate的子线程这一个特性了。再继续跟下去看看。
```Java
@Override
public void handleMessage(Message msg) {
    switch (msg.what) {
    case MSG_INVALIDATE:
        ((View) msg.obj).invalidate();
        break;

	...
	}
}
```
代码跟到这里，也就明白了，postInvalidate通过sendMessageDelayed的方法，加入到了looper中，之后在handleMessage中再调用对应View的invalidate()方法，请求View重绘。

## 三、invalidate流程分析

犹豫本人的好奇心，所以又有了这一节(╮(╯▽╰)╭)。invalidate是如何让View进行重绘的呢？<br>
*(PS:我这里使用的API版本为23，具体的代码可能和低版本的有稍许不同)*

### 1、invalidate的向上传递

先来看一下View#invalidate的传递过程，首先在view中调用View#invalidate()。
```Java
public void invalidate() {
    invalidate(true);
}

void invalidate(boolean invalidateCache) {
    invalidateInternal(0, 0, mRight - mLeft, mBottom - mTop, invalidateCache, true);
}
```
View#invalidate会调用经过传递之后会进入View#invalidateInternal方法。
```Java
void invalidateInternal(int l, int t, int r, int b, boolean invalidateCache,
        boolean fullInvalidate) {

	//如果View重绘，则它也将重绘
    if (mGhostView != null) {
        mGhostView.invalidate(true);
        return;
    }

	//View是否可见，是否在动画运行中
    if (skipInvalidate()) {
        return;
    }

	//根据View的标记来判断View是否需要进行重绘
    if ((mPrivateFlags & (PFLAG_DRAWN | PFLAG_HAS_BOUNDS)) == (PFLAG_DRAWN | PFLAG_HAS_BOUNDS)
            || (invalidateCache && (mPrivateFlags & PFLAG_DRAWING_CACHE_VALID) == PFLAG_DRAWING_CACHE_VALID)
            || (mPrivateFlags & PFLAG_INVALIDATED) != PFLAG_INVALIDATED
            || (fullInvalidate && isOpaque() != mLastIsOpaque)) {
        if (fullInvalidate) {
            mLastIsOpaque = isOpaque();
            mPrivateFlags &= ~PFLAG_DRAWN;
        }

		//设置标志，表明View正在被重绘
        mPrivateFlags |= PFLAG_DIRTY;
		//清除缓存，设置标志，表明重绘由当前View发起
        if (invalidateCache) {
            mPrivateFlags |= PFLAG_INVALIDATED;
            mPrivateFlags &= ~PFLAG_DRAWING_CACHE_VALID;
        }
        // 把需要重绘的View区域传递给父View
        final AttachInfo ai = mAttachInfo;
        final ViewParent p = mParent;
        if (p != null && ai != null && l < r && t < b) {
            final Rect damage = ai.mTmpInvalRect;
            damage.set(l, t, r, b);
			//关键代码，调用父View的方法，向上传递重绘事件
            p.invalidateChild(this, damage);
        }
        ...
    }
}
```
在View#invalidateInternal方法中，会判断当前View的状态，是否需要进行重绘，之后设置一系列标记位。通过父View的**invalidateChild(this, damage)**方法，将需要重绘的区域传递给父View。<br>
接着来看下ViewGroup#invalidateChild方法:
```Java
public final void invalidateChild(View child, final Rect dirty) {
    ViewParent parent = this;
    final AttachInfo attachInfo = mAttachInfo;
    if (attachInfo != null) {
        ...
		//保存子View的left、top
		final int[] location = attachInfo.mInvalidateChildLocation;
		location[CHILD_LEFT_INDEX] = child.mLeft;
		location[CHILD_TOP_INDEX] = child.mTop;
        if (!childMatrix.isIdentity() ||
                (mGroupFlags & ViewGroup.FLAG_SUPPORT_STATIC_TRANSFORMATIONS) != 0) {
            RectF boundingRect = attachInfo.mTmpTransformRect;
            boundingRect.set(dirty);
            Matrix transformMatrix;
            if ((mGroupFlags & ViewGroup.FLAG_SUPPORT_STATIC_TRANSFORMATIONS) != 0) {
                Transformation t = attachInfo.mTmpTransformation;
                boolean transformed = getChildStaticTransformation(child, t);
                if (transformed) {
                    transformMatrix = attachInfo.mTmpMatrix;
                    transformMatrix.set(t.getMatrix());
                    if (!childMatrix.isIdentity()) {
                        transformMatrix.preConcat(childMatrix);
                    }
                } else {
                    transformMatrix = childMatrix;
                }
            } else {
                transformMatrix = childMatrix;
            }
            transformMatrix.mapRect(boundingRect);
			//设置需要重绘的区域
            dirty.set((int) (boundingRect.left - 0.5f),
                    (int) (boundingRect.top - 0.5f),
                    (int) (boundingRect.right + 0.5f),
                    (int) (boundingRect.bottom + 0.5f));
        }
        do {
            View view = null;
            if (parent instanceof View) {
                view = (View) parent;
            }
            if (drawAnimation) {
                if (view != null) {
                    view.mPrivateFlags |= PFLAG_DRAW_ANIMATION;
                } else if (parent instanceof ViewRootImpl) {
                    ((ViewRootImpl) parent).mIsAnimating = true;
                }
            }
            // If the parent is dirty opaque or not dirty, mark it dirty with the opaque
            // flag coming from the child that initiated the invalidate
            if (view != null) {
                if ((view.mViewFlags & FADING_EDGE_MASK) != 0 &&
                        view.getSolidColor() == 0) {
                    opaqueFlag = PFLAG_DIRTY;
                }
                if ((view.mPrivateFlags & PFLAG_DIRTY_MASK) != PFLAG_DIRTY) {
                    view.mPrivateFlags = (view.mPrivateFlags & ~PFLAG_DIRTY_MASK) | opaqueFlag;
                }
            }
			//这里是关键代码，他会调用父类的
            parent = parent.invalidateChildInParent(location, dirty);
            if (view != null) {
                // Account for transform on current parent
                Matrix m = view.getMatrix();
                if (!m.isIdentity()) {
                    RectF boundingRect = attachInfo.mTmpTransformRect;
                    boundingRect.set(dirty);
                    m.mapRect(boundingRect);
                    dirty.set((int) (boundingRect.left - 0.5f),
                            (int) (boundingRect.top - 0.5f),
                            (int) (boundingRect.right + 0.5f),
                            (int) (boundingRect.bottom + 0.5f));
                }
            }
        } while (parent != null);
    }
}
```
上面的这段代码是invalidate传递中很重要的一段。在其中保存了子View的top、left，设置了需要重绘的区域dirty。之后再do...while函数中，反复的调用**parent = parent.invalidateChildInParent(location, dirty)**方法，一层层的调用父类的invalidateChildInParent来设置需要重绘的区域dirty，以及给mPrivateFlags变量添加DRAWN标识。<br>
当调用到我们平时setContentView(layout)的View时，继续传递就会到整个window最顶层的View————DecorView。它继承自FrameLayout，含有该Window的装饰。(关于DecorView的由来这个坑，我会在之后的文章中进行说明，这里把它当成一个ViewGroup就好)。当它再次调用parent.invalidateChildInParent(location, dirty)之后，就会传递到ViewRoot的实现类ViewRootImpl之中。ViewRoot并不是View，我们来看下它的invalidateChildInParent方法。
```Java
@Override
public ViewParent invalidateChildInParent(int[] location, Rect dirty) {
	//检查线程是否为创建View的原线程
    checkThread();
    if (DEBUG_DRAW) Log.v(TAG, "Invalidate child: " + dirty);
	//检查重绘区域
    if (dirty == null) {
        invalidate();
        return null;
    } else if (dirty.isEmpty() && !mIsAnimating) {
        return null;
    }
	//动画和滑动的检查设置
    if (mCurScrollY != 0 || mTranslator != null) {
        mTempRect.set(dirty);
        dirty = mTempRect;
        if (mCurScrollY != 0) {
            dirty.offset(0, -mCurScrollY);
        }
        if (mTranslator != null) {
            mTranslator.translateRectInAppWindowToScreen(dirty);
        }
        if (mAttachInfo.mScalingRequired) {
            dirty.inset(-1, -1);
        }
    }
    invalidateRectOnScreen(dirty);
    return null;
}

private void invalidateRectOnScreen(Rect dirty) {
    ...
    if (!mWillDrawSoon && (intersected || mIsAnimating)) {
		//关键代码，ViewTree列表
        scheduleTraversals();
    }
}
```
ViewRootImpl#invalidateChildInParent方法，进入之后会调用一系列的检查，之后调用invalidateRectOnScreen方法，然后调用scheduleTraversals()方法。这个方法非常非常的重要，它是invalidate从子View向父View传递的终点。之后将会发送Handler消息，从父View开始向子View传递。

### 2、ViewRootImpl内的invalidate传递

来继续看看ViewRootImpl#scheduleTraversals()。
```Java
void scheduleTraversals() {
    if (!mTraversalScheduled) {
        mTraversalScheduled = true;
        mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
		//异步
        mChoreographer.postCallback(
                Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
        if (!mUnbufferedInputDispatch) {
            scheduleConsumeBatchedInput();
        }
        notifyRendererOfFramePending();
        pokeDrawLockIfNeeded();
    }
}


final TraversalRunnable mTraversalRunnable = new TraversalRunnable();

final class TraversalRunnable implements Runnable {
    @Override
    public void run() {
        doTraversal();
    }
}

void doTraversal() {
    if (mTraversalScheduled) {
        mTraversalScheduled = false;
        mHandler.getLooper().getQueue().removeSyncBarrier(mTraversalBarrier);
        if (mProfile) {
            Debug.startMethodTracing("ViewAncestor");
        }
		//关键代码，执行ViewTree遍历
        performTraversals();
        if (mProfile) {
            Debug.stopMethodTracing();
            mProfile = false;
        }
    }
}
```
ViewRootImpl#scheduleTraversals会通过mChoreographer调用mTraversalRunnable类，从而异步调用doTraversal方法，最后调用performTraversals()执行ViewTree的遍历。<br>

现在继续查看ViewRootImpl#performTraversals()方法。
```Java
private void performTraversals() {
	...
    if (!cancelDraw && !newSurface) {
        if (!skipDraw || mReportNextDraw) {
            if (mPendingTransitions != null && mPendingTransitions.size() > 0) {
                for (int i = 0; i < mPendingTransitions.size(); ++i) {
                    mPendingTransitions.get(i).startChangingAnimations();
                }
                mPendingTransitions.clear();
            }
			//关键代码
            performDraw();
        }
    } 
	...
}

private void performDraw() {
    ...
	final boolean fullRedrawNeeded = mFullRedrawNeeded;
    mFullRedrawNeeded = false;
    mIsDrawing = true;
    Trace.traceBegin(Trace.TRACE_TAG_VIEW, "draw");
    try {
		//关键代码
        draw(fullRedrawNeeded);
    } finally {
        mIsDrawing = false;
        Trace.traceEnd(Trace.TRACE_TAG_VIEW);
    }
    ...
}

```
在其中进行View的是否可见，surfasce，正在绘制，删除列表中等判断，之后调用performDraw()开始执行绘制。在performDraw()又调用了ViewRootImpl的draw方法，并传递了fullRedrawNeeded参数，此参数源自mFullRedrawNeeded成员变量，用于表示是否需要重新绘制全部的View。现在继续看看ViewRootImpl#draw源码。

```Java
private void draw(boolean fullRedrawNeeded) {
    Surface surface = mSurface;
    ...
	//获取mDirty，该值表示需要重绘的区域
    final Rect dirty = mDirty;
    if (mSurfaceHolder != null) {
        // The app owns the surface, we won't draw.
        dirty.setEmpty();
        if (animating) {
            if (mScroller != null) {
                mScroller.abortAnimation();
            }
            disposeResizeBuffer();
        }
        return;
    }
	//如果为ture，则设置dirty区域为全屏
    if (fullRedrawNeeded) {
        mAttachInfo.mIgnoreDirtyState = true;
        dirty.set(0, 0, (int) (mWidth * appScale + 0.5f), (int) (mHeight * appScale + 0.5f));
    }
    ...
	//重绘区域、动画判断
		//硬件渲染判断
			//关键代码
            if (!drawSoftware(surface, mAttachInfo, xOffset, yOffset, scalingRequired, dirty)) {
                return;
            }
    ...
}
```
在draw方法中，根据传如fullRedrawNeeded参数，设置需要重绘的dirty区域，最后调用drawSoftware方法，把参数传递进去，现在继续看ViewRootImpl#drawSoftware源码。
```Java
private boolean drawSoftware(Surface surface, AttachInfo attachInfo, int xoff, int yoff,
        boolean scalingRequired, Rect dirty) {
    ...
    try {
        
        if (!canvas.isOpaque() || yoff != 0 || xoff != 0) {
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        }
        dirty.setEmpty();
        mIsAnimating = false;
        mView.mPrivateFlags |= View.PFLAG_DRAWN;
        
        try {
            canvas.translate(-xoff, -yoff);
            if (mTranslator != null) {
                mTranslator.translateCanvas(canvas);
            }
            canvas.setScreenDensity(scalingRequired ? mNoncompatDensity : 0);
            attachInfo.mSetIgnoreDirtyState = false;
			//关键代码，mView为DecorView，开启View绘制
            mView.draw(canvas);
            drawAccessibilityFocusedDrawableIfNeeded(canvas);
        } finally {
            if (!attachInfo.mSetIgnoreDirtyState) {
                // Only clear the flag if it was not set during the mView.draw() call
                attachInfo.mIgnoreDirtyState = false;
            }
        }
    } 
	...
}
```
首先对canvas进行一些属性设置，包括色块、平移等。之后调用mView.draw(canvas)方法，开始对View进行绘制。mView就是我们之前说的，window中的顶级视图DecorView。

### 3、invalidate的向下传递

DecorView继承自FrameLayout，而ViewGroup的draw方法继承自View，so，快来看看View#draw的源码吧。
```Java
public void draw(Canvas canvas) {
    final int privateFlags = mPrivateFlags;
    final boolean dirtyOpaque = (privateFlags & PFLAG_DIRTY_MASK) == PFLAG_DIRTY_OPAQUE &&
            (mAttachInfo == null || !mAttachInfo.mIgnoreDirtyState);
    mPrivateFlags = (privateFlags & ~PFLAG_DIRTY_MASK) | PFLAG_DRAWN;
    /*
     * Draw traversal performs several drawing steps which must be executed
     * in the appropriate order:
     *
     *      1. Draw the background
     *      2. If necessary, save the canvas' layers to prepare for fading
     *      3. Draw view's content
     *      4. Draw children
     *      5. If necessary, draw the fading edges and restore layers
     *      6. Draw decorations (scrollbars for instance)
     */
    // Step 1, draw the background, if needed
    int saveCount;
    if (!dirtyOpaque) {
        drawBackground(canvas);
    }
    // skip step 2 & 5 if possible (common case)
    final int viewFlags = mViewFlags;
    boolean horizontalEdges = (viewFlags & FADING_EDGE_HORIZONTAL) != 0;
    boolean verticalEdges = (viewFlags & FADING_EDGE_VERTICAL) != 0;
    if (!verticalEdges && !horizontalEdges) {
        // Step 3, draw the content
        if (!dirtyOpaque) onDraw(canvas);
        // Step 4, draw the children
        dispatchDraw(canvas);
        // Overlay is part of the content and draws beneath Foreground
        if (mOverlay != null && !mOverlay.isEmpty()) {
            mOverlay.getOverlayView().dispatchDraw(canvas);
        }
        // Step 6, draw decorations (foreground, scrollbars)
        onDrawForeground(canvas);
        // we're done...
        return;
    }
    ...
}
```
draw方法中，官方对其的步骤进行了清晰的注释，我们来看下流程，在执行流程之前会检查绘制区域是否透明:
* 1、绘制View背景，如果透明则不绘制
* 2、如果需要，则保存画布的图层
* 3、绘制View内容，如果透明则不绘制
* 4、绘制子View————这个很重要
* 5、如果需要，则绘制View的褪色边缘和恢复图层
* 6、绘制装饰滚动条

这里最重要的步骤是第四步，绘制子View，现在我们来看下这个ViewGroup#dispatchDraw(canvas)方法，注意这里的View是一个DecorView，所以要在ViewGroup中去查看这个方法，View中的这个方法是一个空方法。
```Java
protected void dispatchDraw(Canvas canvas) {
    ...
    for (int i = 0; i < childrenCount; i++) {
        while (transientIndex >= 0 && mTransientIndices.get(transientIndex) == i) {
            final View transientChild = mTransientViews.get(transientIndex);
            if ((transientChild.mViewFlags & VISIBILITY_MASK) == VISIBLE ||
                    transientChild.getAnimation() != null) {
                more |= drawChild(canvas, transientChild, drawingTime);
            }
            transientIndex++;
            if (transientIndex >= transientCount) {
                transientIndex = -1;
            }
        }
        int childIndex = customOrder ? getChildDrawingOrder(childrenCount, i) : i;
        final View child = (preorderedList == null)
                ? children[childIndex] : preorderedList.get(childIndex);
        if ((child.mViewFlags & VISIBILITY_MASK) == VISIBLE || child.getAnimation() != null) {
            more |= drawChild(canvas, child, drawingTime);
        }
    }
    while (transientIndex >= 0) {
        // there may be additional transient views after the normal views
        final View transientChild = mTransientViews.get(transientIndex);
        if ((transientChild.mViewFlags & VISIBILITY_MASK) == VISIBLE ||
                transientChild.getAnimation() != null) {
            more |= drawChild(canvas, transientChild, drawingTime);
        }
        transientIndex++;
        if (transientIndex >= transientCount) {
            break;
        }
    }
    ...
}
```
这里对所有的子View进行遍历，并调用ViewGroup#drawChild方法。
```Java
protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
    return child.draw(canvas, this, drawingTime);
}
```
这里又调用了子View的draw方法，这样绘制就传递了下去，当然这个draw方法和之前这一小节一开始介绍的View#draw方法并不一样，我们来看看
```Java
boolean draw(Canvas canvas, ViewGroup parent, long drawingTime) {
	...
	if (!drawingWithRenderNode) {
    	computeScroll();
    	sx = mScrollX;
    	sy = mScrollY;
	}
	...
    if (!drawingWithDrawingCache) {
        if (drawingWithRenderNode) {
            mPrivateFlags &= ~PFLAG_DIRTY_MASK;
            ((DisplayListCanvas) canvas).drawRenderNode(renderNode);
        } else {
            // Fast path for layouts with no backgrounds
            if ((mPrivateFlags & PFLAG_SKIP_DRAW) == PFLAG_SKIP_DRAW) {
                mPrivateFlags &= ~PFLAG_DIRTY_MASK;
                dispatchDraw(canvas);
            } else {
                draw(canvas);
            }
        }
    } else if (cache != null) {
        mPrivateFlags &= ~PFLAG_DIRTY_MASK;
        if (layerType == LAYER_TYPE_NONE) {
            // no layer paint, use temporary paint to draw bitmap
            Paint cachePaint = parent.mCachePaint;
            if (cachePaint == null) {
                cachePaint = new Paint();
                cachePaint.setDither(false);
                parent.mCachePaint = cachePaint;
            }
            cachePaint.setAlpha((int) (alpha * 255));
            canvas.drawBitmap(cache, 0.0f, 0.0f, cachePaint);
        } else {
            // use layer paint to draw the bitmap, merging the two alphas, but also restore
            int layerPaintAlpha = mLayerPaint.getAlpha();
            mLayerPaint.setAlpha((int) (alpha * layerPaintAlpha));
            canvas.drawBitmap(cache, 0.0f, 0.0f, mLayerPaint);
            mLayerPaint.setAlpha(layerPaintAlpha);
        }
    }
	...
}
```
会先判断之前是否进行过了绘制，如果没有则进入快速绘制通道，对没有背景的View进行绘制。判断是否需要跳过自身的draw绘制方法，如果跳过则进入dispatchDraw，不跳过则进入当前View的draw方法，即这一小节开头的draw方法，就此形成了循环。同时我们在这里看到了computeScroll()方法，也就印证了本篇开头对于弹性滑动过程的描述。

<br>
图
<br>

## 四、小结
本文对弹性滑动以及弹性滑动的过程进行了详细分析，同时对因此引出的invalidate流程进行了源码分析。<br>

如果在阅读过程中，有任何疑问与问题，欢迎与我联系。<br>
**博客:www.idtkm.com**<br>
**GitHub:https://github.com/Idtk**<br>
**微博:http://weibo.com/Idtk**<br>
**邮箱:IdtkMa@gmail.com**<br>
<br>