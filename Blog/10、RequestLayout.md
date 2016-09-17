# View的requestLayout传递与测量、布局流程分析

**在之前的[invalidate传递与绘制流程分析](https://github.com/Idtk/Blog/blob/master/Blog/9%E3%80%81Invalidate.md)文章中我们对invalidate的流程进行了详细分析，现在来继续分析下requestLayout的流程吧**

[自定义View系列目录](https://github.com/Idtk/Blog)

## 一、requestLayout的请求传递

我们从View#requestLayout方法的源码开始

```Java
public void requestLayout() {
    if (mMeasureCache != null) mMeasureCache.clear();
    if (mAttachInfo != null && mAttachInfo.mViewRequestingLayout == null) {
        // Only trigger request-during-layout logic if this is the view requesting it,
        // not the views in its parent hierarchy
        ViewRootImpl viewRoot = getViewRootImpl();
        if (viewRoot != null && viewRoot.isInLayout()) {
            if (!viewRoot.requestLayoutDuringLayout(this)) {
                return;
            }
        }
        mAttachInfo.mViewRequestingLayout = this;
    }
	// 增加PFLAG_FORCE_LAYOUT标记，在measure时会校验此属性
    mPrivateFlags |= PFLAG_FORCE_LAYOUT;
    mPrivateFlags |= PFLAG_INVALIDATED;
	// 父类不为空&&父类没有请求重新布局(是否有PFLAG_FORCE_LAYOUT标志)
    if (mParent != null && !mParent.isLayoutRequested()) {
		// 调用父类的requestLayout
        mParent.requestLayout();
    }
    if (mAttachInfo != null && mAttachInfo.mViewRequestingLayout == this) {
        mAttachInfo.mViewRequestingLayout = null;
    }
}
```

在源码中，会对measure的缓存进行清除，之后会判断ViewTree是否正在布局流程，接着为View设置标记，PFLAG_FORCE_LAYOUT标记会在View进行measure时验证。之后非常重要会判断父类是否为空以及检查是否正在请求重新布局(即检查之间设置的PFLAG_FORCE_LAYOUT标记)，如果满足条件则会父类的requestLayout方法，而ViewGroup继承自View，其requestLayout方法调用了View的requestLayout方法，所以会不断迭代的调用父类的requestLayout方法，直到DecorView的父类ViewRoot。<br>

ViewRoot的实现类为ViewRootImpl，在这个类中的requestLayout方法与View中的并不相同，我们来看下他的源码

```Java
@Override
public void requestLayout() {
	// 是否在处理requestLayout
    if (!mHandlingLayoutInLayoutRequest) {
		// 检查创建view的线程是否为当前线程
        checkThread();
        mLayoutRequested = true;
        scheduleTraversals();
    }
}
```
在ViewRootImpl#requestLayout中,首先判断是否正在requestLayout中，之后检查当前线程是否为创建View的线程。接着调用scheduleTraversals方法，发起请求。<br>

ViewRootImpl#scheduleTraversals方法调用performTraversals方法的过程已经在[自定义View——invalidate流程分析](https://github.com/Idtk/Blog/blob/master/Blog/9%E3%80%81Invalidate.md)中进行了详细说明，在这里就不在赘述了。

```Java
private void performTraversals() {
	...
	Rect frame = mWinFrame;
	if (mFirst) {
		mFullRedrawNeeded = true;
		mLayoutRequested = true;
		if (lp.type == WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL
				|| lp.type == WindowManager.LayoutParams.TYPE_INPUT_METHOD) {
			// NOTE -- system code, won't try to do compat mode.
			Point size = new Point();
			mDisplay.getRealSize(size);
			desiredWindowWidth = size.x;
			desiredWindowHeight = size.y;
		} else {
			DisplayMetrics packageMetrics =
				mView.getContext().getResources().getDisplayMetrics();
			desiredWindowWidth = packageMetrics.widthPixels;
			desiredWindowHeight = packageMetrics.heightPixels;
		}
		// We used to use the following condition to choose 32 bits drawing caches:
		// PixelFormat.hasAlpha(lp.format) || lp.format == PixelFormat.RGBX_8888
		// However, windows are now always 32 bits by default, so choose 32 bits
		mAttachInfo.mUse32BitDrawingCache = true;
		mAttachInfo.mHasWindowFocus = false;
		mAttachInfo.mWindowVisibility = viewVisibility;
		mAttachInfo.mRecomputeGlobalAttributes = false;
		viewVisibilityChanged = false;
		mLastConfiguration.setTo(host.getResources().getConfiguration());
		mLastSystemUiVisibility = mAttachInfo.mSystemUiVisibility;
		// Set the layout direction if it has not been set before (inherit is the default)
		if (mViewLayoutDirectionInitial == View.LAYOUT_DIRECTION_INHERIT) {
			host.setLayoutDirection(mLastConfiguration.getLayoutDirection());
		}
		host.dispatchAttachedToWindow(mAttachInfo, 0);
		mAttachInfo.mTreeObserver.dispatchOnWindowAttachedChange(true);
		dispatchApplyInsets(host);
		//Log.i(TAG, "Screen on initialized: " + attachInfo.mKeepScreenOn);
	} else {
		desiredWindowWidth = frame.width();
		desiredWindowHeight = frame.height();
		if (desiredWindowWidth != mWidth || desiredWindowHeight != mHeight) {
			if (DEBUG_ORIENTATION) Log.v(TAG,
					"View " + host + " resized to: " + frame);
			mFullRedrawNeeded = true;
			mLayoutRequested = true;
			windowSizeMayChange = true;
		}
	}
	
	...
	
	if (mFirst || windowShouldResize || insetsChanged ||
			viewVisibilityChanged || params != null) {
		...
		if (!mStopped || mReportNextDraw) {
			boolean focusChangedDueToTouchMode = ensureTouchModeLocally(
					(relayoutResult&WindowManagerGlobal.RELAYOUT_RES_IN_TOUCH_MODE) != 0);
			if (focusChangedDueToTouchMode || mWidth != host.getMeasuredWidth()
					|| mHeight != host.getMeasuredHeight() || contentInsetsChanged) {
				int childWidthMeasureSpec = getRootMeasureSpec(mWidth, lp.width);
				int childHeightMeasureSpec = getRootMeasureSpec(mHeight, lp.height);

				if (DEBUG_LAYOUT) Log.v(TAG, "Ooops, something changed!  mWidth="
						+ mWidth + " measuredWidth=" + host.getMeasuredWidth()
						+ " mHeight=" + mHeight
						+ " measuredHeight=" + host.getMeasuredHeight()
						+ " coveredInsetsChanged=" + contentInsetsChanged);

				 // Ask host how big it wants to be
				performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);

				// Implementation of weights from WindowManager.LayoutParams
				// We just grow the dimensions as needed and re-measure if
				// needs be
				int width = host.getMeasuredWidth();
				int height = host.getMeasuredHeight();
				boolean measureAgain = false;

				if (lp.horizontalWeight > 0.0f) {
					width += (int) ((mWidth - width) * lp.horizontalWeight);
					childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width,
							MeasureSpec.EXACTLY);
					measureAgain = true;
				}
				if (lp.verticalWeight > 0.0f) {
					height += (int) ((mHeight - height) * lp.verticalWeight);
					childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height,
							MeasureSpec.EXACTLY);
					measureAgain = true;
				}

				if (measureAgain) {
					if (DEBUG_LAYOUT) Log.v(TAG,
							"And hey let's measure once more: width=" + width
							+ " height=" + height);
					performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
				}

				layoutRequested = true;
			}
		}
	} else {
		...
	}

	final boolean didLayout = layoutRequested && (!mStopped || mReportNextDraw);
	boolean triggerGlobalLayoutListener = didLayout
			|| mAttachInfo.mRecomputeGlobalAttributes;
	if (didLayout) {
		performLayout(lp, desiredWindowWidth, desiredWindowHeight);
	...
}
```

上面截取了performTraversals的主要相关代码，在其中一般情况下蒋会执行performMeasure、performLayout、performDraw三个方法。调用**performMeasure**方法之前，会先分别对焦点、测量的宽高、View内容的变化情况进行判断，如果变化则会执行performMeasure。接着会检查窗口属性是否包含weight，如果包含蒋会再执行一次performMeasure方法，在之后会设置layoutRequested = true，表示需要重新布局。在执行**performLayout**方法之前，会对didLayout参数进行检查，判断是否请求重新布局，窗口是否停止，是否需要再次绘制，而layoutRequested参数再performMeasure后设置为true，mStopped默认为false，所以将执行performMeasure方法。**performDraw**方法就像在[自定义View——invalidate流程分析](https://github.com/Idtk/Blog/blob/master/Blog/9%E3%80%81Invalidate.md)中分析的一样，蒋会执行，但是在ViewRootImpl#draw中进行dirty判断时，会发现dirty为空，所以不会继续执行绘制过程。那么一般情况下的进行requestLayout请求后，view的重新绘制在什么地方呢？这将会在稍后的layout过程中看到答案。


## 二、测量流程

在查看performMeasure的源码之前，我们先看看传入performMeasure的两个参数childWidthMeasureSpec, childHeightMeasureSpec的获取，代码中可以看出参数是通过调用getRootMeasureSpec方法来获得，现在来看下getRootMeasureSpec的源码

```Java
private static int getRootMeasureSpec(int windowSize, int rootDimension) {
    int measureSpec;
    switch (rootDimension) {
    case ViewGroup.LayoutParams.MATCH_PARENT:
        // Window can't resize. Force root view to be windowSize.
        measureSpec = MeasureSpec.makeMeasureSpec(windowSize, MeasureSpec.EXACTLY);
        break;
    case ViewGroup.LayoutParams.WRAP_CONTENT:
        // Window can resize. Set max size for root view.
        measureSpec = MeasureSpec.makeMeasureSpec(windowSize, MeasureSpec.AT_MOST);
        break;
    default:
        // Window wants to be an exact size. Force root view to be that size.
        measureSpec = MeasureSpec.makeMeasureSpec(rootDimension, MeasureSpec.EXACTLY);
        break;
    }
    return measureSpec;
}
```
通过上述getRootMeasureSpec的代码，就可以清楚的看出LayoutParams与测量模式的对应关系。<br>

| LayoutParams   | Mode         | size  |
| -------------- |:------------:|-------|
| MATCH_PARENT   | EXACTLY      |  windowSize    |
| WRAP_CONTENT   | AT_MOST      |  windowSize    |
| 固定大小        | EXACTLY      |  rootSize      |

<br>

我们继续看看performMeasure方法
```Java
private void performMeasure(int childWidthMeasureSpec, int childHeightMeasureSpec) {
    Trace.traceBegin(Trace.TRACE_TAG_VIEW, "measure");
    try {
        mView.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    } finally {
        Trace.traceEnd(Trace.TRACE_TAG_VIEW);
    }
}
```
从上述代码可以看出，将会执行View的measure方法，measure方法中将会执行onMeasure方法，ViewRootImpl的调用的view为DecorView(DecorView为布局的顶层view),现在来看看DecorView#onMeasure方法<br>
```Java
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
    final boolean isPortrait = metrics.widthPixels < metrics.heightPixels;
    final int widthMode = getMode(widthMeasureSpec);
    final int heightMode = getMode(heightMeasureSpec);
    boolean fixedWidth = false;
    if (widthMode == AT_MOST) {
        final TypedValue tvw = isPortrait ? mFixedWidthMinor : mFixedWidthMajor;
		// tvw不会为NULL，等级也不会为NULL，具体原因可以跟踪一下源码。
        if (tvw != null && tvw.type != TypedValue.TYPE_NULL) {
            final int w;
			// 获取视图宽度
            if (tvw.type == TypedValue.TYPE_DIMENSION) {
                w = (int) tvw.getDimension(metrics);
            } else if (tvw.type == TypedValue.TYPE_FRACTION) {
                w = (int) tvw.getFraction(metrics.widthPixels, metrics.widthPixels);
            } else {
                w = 0;
            }
			// 设置测量模式为EXACTLY
            if (w > 0) {
                final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(
                        Math.min(w, widthSize), EXACTLY);
                fixedWidth = true;
            }
        }
    }
	// heightMode的处理方式与widthMode相同
    if (heightMode == AT_MOST) {
        ...
    }
    ...
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	...
}
```
上述代码中，将会分别检查宽高测量模式，这里以宽度测量属性为例，首先检查测量模式是否为AT_MOST，如果是，则获取视图的宽度，然后与宽度测量属性的大小取小，接着与测量模式EXACTLY，作为MeasureSpec.makeMeasureSpec方法的参数一起生成新的宽度测量属性。之后会把新生成的测量属性传递给DecorView的父类，也就是FrameLayout的onMeasure方法继续处理。我们来看看它是怎么做的。
```Java
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int count = getChildCount();
    final boolean measureMatchParentChildren =
            MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY ||
            MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY;
    mMatchParentChildren.clear();
    int maxHeight = 0;
    int maxWidth = 0;
    int childState = 0;
    for (int i = 0; i < count; i++) {
        final View child = getChildAt(i);
        if (mMeasureAllChildren || child.getVisibility() != GONE) {
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
			// 获取子view中最大的宽度和高度
            maxWidth = Math.max(maxWidth,
                    child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
            maxHeight = Math.max(maxHeight,
                    child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
            childState = combineMeasuredStates(childState, child.getMeasuredState());
            if (measureMatchParentChildren) {
				// 如果子View的宽or高为MATCH_PARENT，则保存子View
                if (lp.width == LayoutParams.MATCH_PARENT ||
                        lp.height == LayoutParams.MATCH_PARENT) {
                    mMatchParentChildren.add(child);
                }
            }
        }
    }

    // Account for padding too
    maxWidth += getPaddingLeftWithForeground() + getPaddingRightWithForeground();
    maxHeight += getPaddingTopWithForeground() + getPaddingBottomWithForeground();
    // Check against our minimum height and width
    maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
    maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());
    // Check against our foreground's minimum height and width
    final Drawable drawable = getForeground();
    if (drawable != null) {
        maxHeight = Math.max(maxHeight, drawable.getMinimumHeight());
        maxWidth = Math.max(maxWidth, drawable.getMinimumWidth());
    }
	// 保存测量结果
    setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
            resolveSizeAndState(maxHeight, heightMeasureSpec,
                    childState << MEASURED_HEIGHT_STATE_SHIFT));
    count = mMatchParentChildren.size();
	// 对之前保存的子view，分别重新测量MeasureSpec
    if (count > 1) {
        for (int i = 0; i < count; i++) {
            final View child = mMatchParentChildren.get(i);
            final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
            final int childWidthMeasureSpec;
            if (lp.width == LayoutParams.MATCH_PARENT) {
                final int width = Math.max(0, getMeasuredWidth()
                        - getPaddingLeftWithForeground() - getPaddingRightWithForeground()
                        - lp.leftMargin - lp.rightMargin);
                childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                        width, MeasureSpec.EXACTLY);
            } else {
                childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                        getPaddingLeftWithForeground() + getPaddingRightWithForeground() +
                        lp.leftMargin + lp.rightMargin,
                        lp.width);
            }
            final int childHeightMeasureSpec;
            if (lp.height == LayoutParams.MATCH_PARENT) {
                final int height = Math.max(0, getMeasuredHeight()
                        - getPaddingTopWithForeground() - getPaddingBottomWithForeground()
                        - lp.topMargin - lp.bottomMargin);
                childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                        height, MeasureSpec.EXACTLY);
            } else {
                childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                        getPaddingTopWithForeground() + getPaddingBottomWithForeground() +
                        lp.topMargin + lp.bottomMargin,
                        lp.height);
            }
			// 测量子View
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        }
    }
}
```
FrameLayout的测量过程中，首先遍历子View，调用measureChildWithMargins方法，之后获取子view中的最大宽度or高度，这是因为FrameLayout的布局，如果在wrap_content的情况下，其宽度就等于所以子View中的最大宽度，高度就等于所以子View中最大的高度。然后对子View的宽or高为MATCH_PARENT的View进行存储。之后处理一些属性，保存测量结果。resolveSizeAndState方法，我已经在之前的[自定义View——雷达图(蜘蛛网图)](https://github.com/Idtk/Blog/blob/master/Blog/7%E3%80%81RadarChart.md)中进行过分析。最后就是对之前保存的子view进行处理了。从measureChildWithMargins方法的参数可以看出，测量值与view的测量值以及子view相关，现在我们来看下它的测量过程
```Java
protected void measureChildWithMargins(View child,
        int parentWidthMeasureSpec, int widthUsed,
        int parentHeightMeasureSpec, int heightUsed) {
    final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
    final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
            mPaddingLeft + mPaddingRight + lp.leftMargin + lp.rightMargin
                    + widthUsed, lp.width);
    final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
            mPaddingTop + mPaddingBottom + lp.topMargin + lp.bottomMargin
                    + heightUsed, lp.height);
    child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
}
```

上述方法中，会先调用getChildMeasureSpec方法，获取测量的宽高属性之后，最后对子View进行测量。由代码可以看出测量属性的获取与父view的MeasureSpec、View的padding、子View的LayoutParams相关，具体的关系我们来看看ViewGroup的getChildMeasureSpec方法

```Java
public static int getChildMeasureSpec(int spec, int padding, int childDimension) {
    int specMode = MeasureSpec.getMode(spec);
    int specSize = MeasureSpec.getSize(spec);
    int size = Math.max(0, specSize - padding);
    int resultSize = 0;
    int resultMode = 0;
    switch (specMode) {
    // Parent has imposed an exact size on us
    case MeasureSpec.EXACTLY:
        if (childDimension >= 0) {
            resultSize = childDimension;
            resultMode = MeasureSpec.EXACTLY;
        } else if (childDimension == LayoutParams.MATCH_PARENT) {
            // Child wants to be our size. So be it.
            resultSize = size;
            resultMode = MeasureSpec.EXACTLY;
        } else if (childDimension == LayoutParams.WRAP_CONTENT) {
            // Child wants to determine its own size. It can't be
            // bigger than us.
            resultSize = size;
            resultMode = MeasureSpec.AT_MOST;
        }
        break;
    // Parent has imposed a maximum size on us
    case MeasureSpec.AT_MOST:
        if (childDimension >= 0) {
            // Child wants a specific size... so be it
            resultSize = childDimension;
            resultMode = MeasureSpec.EXACTLY;
        } else if (childDimension == LayoutParams.MATCH_PARENT) {
            // Child wants to be our size, but our size is not fixed.
            // Constrain child to not be bigger than us.
            resultSize = size;
            resultMode = MeasureSpec.AT_MOST;
        } else if (childDimension == LayoutParams.WRAP_CONTENT) {
            // Child wants to determine its own size. It can't be
            // bigger than us.
            resultSize = size;
            resultMode = MeasureSpec.AT_MOST;
        }
        break;
    // Parent asked to see how big we want to be
    case MeasureSpec.UNSPECIFIED:
        if (childDimension >= 0) {
            // Child wants a specific size... let him have it
            resultSize = childDimension;
            resultMode = MeasureSpec.EXACTLY;
        } else if (childDimension == LayoutParams.MATCH_PARENT) {
            // Child wants to be our size... find out how big it should
            // be
            resultSize = View.sUseZeroUnspecifiedMeasureSpec ? 0 : size;
            resultMode = MeasureSpec.UNSPECIFIED;
        } else if (childDimension == LayoutParams.WRAP_CONTENT) {
            // Child wants to determine its own size.... find out how
            // big it should be
            resultSize = View.sUseZeroUnspecifiedMeasureSpec ? 0 : size;
            resultMode = MeasureSpec.UNSPECIFIED;
        }
        break;
    }
    return MeasureSpec.makeMeasureSpec(resultSize, resultMode);
}
```
上述方法并不难，写的如此有规律，它主要是根据View的MeasureSpec与子View的LayoutParams参数来确定子View的MeasureSpec属性。接下来，我们为getChildMeasureSpec方法的逻辑建立一个表格。<br>
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/getChildMeasureSpec.png" alt="getChildMeasureSpec" title="getChildMeasureSpec" width="800"/><br>
<br>

现在我们回到measureChildWithMargins方法，测量完成之后，就是对子View的测量，DecorView的子View就是我们平时setContentView中的布局，这里以LinearLayout为例。自然也是和之前一样LinearLayout的measure调用onMeasure，直接来看看LinearLayout#onMeasure<br>
```Java
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    if (mOrientation == VERTICAL) {
        measureVertical(widthMeasureSpec, heightMeasureSpec);
    } else {
        measureHorizontal(widthMeasureSpec, heightMeasureSpec);
    }
}
```
上述代码可以看出，LinearLayout将根据属性来选择一种测量方式，我们选择LinearLayout的水平布局的测量方式，即measureHorizontal，这里简单的挑选其中的主要部分说明一下
```Java
void measureHorizontal(int widthMeasureSpec, int heightMeasureSpec) {
	...
	// See how wide everyone is. Also remember max height.
	for (int i = 0; i < count; ++i) {
		final View child = getVirtualChildAt(i);
		...
		final LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)
				child.getLayoutParams();
		// 计算比重
		totalWeight += lp.weight;
		
		if (widthMode == MeasureSpec.EXACTLY && lp.width == 0 && lp.weight > 0) {
			// Optimization: don't bother measuring children who are going to use
			// leftover space. These views will get measured again down below if
			// there is any leftover space.
			if (isExactly) {
				mTotalLength += lp.leftMargin + lp.rightMargin;
			} else {
				final int totalLength = mTotalLength;
				mTotalLength = Math.max(totalLength, totalLength +
						lp.leftMargin + lp.rightMargin);
			}
			...
		} else {
			int oldWidth = Integer.MIN_VALUE;
			//子View宽度为0，有weight，LayoutParams为WRAP_CONTENT，
			//转换父view的LayoutParams为WRAP_CONTENT
			if (lp.width == 0 && lp.weight > 0) {
				// widthMode is either UNSPECIFIED or AT_MOST, and this
				// child
				// wanted to stretch to fill available space. Translate that to
				// WRAP_CONTENT so that it does not end up with a width of 0
				oldWidth = 0;
				lp.width = LayoutParams.WRAP_CONTENT;
			}

			// Determine how big this child would like to be. If this or
			// previous children have given a weight, then we allow it to
			// use all available space (and we will shrink things later
			// if needed).
			// 测量子View，调用之前说的measureChildWithMargins()方法
			measureChildBeforeLayout(child, i, widthMeasureSpec,
					totalWeight == 0 ? mTotalLength : 0,
					heightMeasureSpec, 0);
			...
		}
		...
	}
	...

	// Add in our padding
	//处理padding
	mTotalLength += mPaddingLeft + mPaddingRight;
	
	int widthSize = mTotalLength;
	
	// Check against our minimum width
	//view高度与背景尺寸和mMinWidth的运算结果比较，取最大值
	widthSize = Math.max(widthSize, getSuggestedMinimumWidth());
	
	// Reconcile our calculated size with the widthMeasureSpec
	// MEASURED_SIZE_MASK = 0x00ffffff，取得测量属性的后30位，即尺寸
	int widthSizeAndState = resolveSizeAndState(widthSize, widthMeasureSpec, 0);
	widthSize = widthSizeAndState & MEASURED_SIZE_MASK;
	
	// Either expand children with weight to take up available space or
	// shrink them if they extend beyond our current bounds. If we skipped
	// measurement on any children, we need to measure them now.
	int delta = widthSize - mTotalLength;
	if (skippedMeasure || delta != 0 && totalWeight > 0.0f) {
		float weightSum = mWeightSum > 0.0f ? mWeightSum : totalWeight;

		maxAscent[0] = maxAscent[1] = maxAscent[2] = maxAscent[3] = -1;
		maxDescent[0] = maxDescent[1] = maxDescent[2] = maxDescent[3] = -1;
		maxHeight = -1;

		mTotalLength = 0;

		for (int i = 0; i < count; ++i) {
			final View child = getVirtualChildAt(i);

			if (child == null || child.getVisibility() == View.GONE) {
				continue;
			}
			
			final LinearLayout.LayoutParams lp =
					(LinearLayout.LayoutParams) child.getLayoutParams();

			float childExtra = lp.weight;
			if (childExtra > 0) {
				// Child said it could absorb extra space -- give him his share
				// 高度 = 子View的weight*剩余高度/总weight
				int share = (int) (childExtra * delta / weightSum);
				weightSum -= childExtra;
				delta -= share;
				//测量子View
				final int childHeightMeasureSpec = getChildMeasureSpec(
						heightMeasureSpec,
						mPaddingTop + mPaddingBottom + lp.topMargin + lp.bottomMargin,
						lp.height);

				// TODO: Use a field like lp.isMeasured to figure out if this
				// child has been previously measured
				if ((lp.width != 0) || (widthMode != MeasureSpec.EXACTLY)) {
					// child was measured once already above ... base new measurement
					// on stored values
					int childWidth = child.getMeasuredWidth() + share;
					if (childWidth < 0) {
						childWidth = 0;
					}

					child.measure(
						MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
						childHeightMeasureSpec);
				} else {
					// child was skipped in the loop above. Measure for this first time here
					child.measure(MeasureSpec.makeMeasureSpec(
							share > 0 ? share : 0, MeasureSpec.EXACTLY),
							childHeightMeasureSpec);
				}
		...
	}
}
```
上述代码，在关键部分增加了一些注释，这里再简单说一下，遍历子View，计算下当前的比重，之后调用measureChildBeforeLayout方法，这个方法将会调用我们之前说的measureChildWithMargins()方法，来完成对子View的测量。接下来用view当前宽度与背景宽度和mMinWidth的运算结果比较，取最大值；再使用resolveSizeAndState([自定义View——雷达图(蜘蛛网图)](https://github.com/Idtk/Blog/blob/master/Blog/7%E3%80%81RadarChart.md))方法获取测量属性，之后与MEASURED_SIZE_MASK按位与获取view的宽度值。然后再按照weight的属性，对view的剩余宽度进行分配，之后调用getChildMeasureSpec方法进行测量值获取。最后依旧使用child.measure方法，继续对子View进行测量。<br>

现在测量流程到了LinearLayout的子View，我们这里假设是一个View。自然也是调用View的measure方法，之后调用onMeasure方法
```Java
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
            getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
}
```
上述代码就是默认的View测量方法，其中setMeasuredDimension将会设置View的测量值，这里需要关注的是getDefaultSize方法，这在我之前的文章[自定义View——雷达图(蜘蛛网图)](https://github.com/Idtk/Blog/blob/master/Blog/7%E3%80%81RadarChart.md)，已经进行了分析。但是getDefaultSize方法一般是无法满足我们对LayoutParams = wrap_content情况下的测量要求的，需要我们自己进行一定的修改。<br>

现在已经完成了整个View的测量过程，在整个测量的过程中，我们不断的通过child.measure对子View进行测量，而测量值的获取主要根据View的MeasureSpec、padding，子View的size、LayoutParams、margin以及View的自身特性(比如weight)等属性来完成，这也是我们自己在自定义View时，编写onMeaure方法的主要方式。

## 三、布局流程

接着第一节的结尾，Layout流程的分析从ViewRootImpl的performLayout开始
```Java
private void performLayout(WindowManager.LayoutParams lp, int desiredWindowWidth,
		int desiredWindowHeight) {
	mLayoutRequested = false;
	mScrollMayChange = true;
	mInLayout = true;
	
	final View host = mView;
	Trace.traceBegin(Trace.TRACE_TAG_VIEW, "layout");
	try {
		host.layout(0, 0, host.getMeasuredWidth(), host.getMeasuredHeight());
		...
	} finally {
		Trace.traceEnd(Trace.TRACE_TAG_VIEW);
	}
	mInLayout = false;
}
```
布局流程中View的组成和测量流程中一样(DecorView→LinearLayout→View)，上述代码，调用了DecorView的layout方法(即View的layout方法)，并传入了参数。left、top传入0，rigth传入host.getMeasuredWidth()，bottom传入host.getMeasuredHeight()。接着来看看layout方法
```Java
public void layout(int l, int t, int r, int b) {
    if ((mPrivateFlags3 & PFLAG3_MEASURE_NEEDED_BEFORE_LAYOUT) != 0) {
        onMeasure(mOldWidthMeasureSpec, mOldHeightMeasureSpec);
        mPrivateFlags3 &= ~PFLAG3_MEASURE_NEEDED_BEFORE_LAYOUT;
    }
    int oldL = mLeft;
    int oldT = mTop;
    int oldB = mBottom;
    int oldR = mRight;
	// 这里会判断要不要invalidate
    boolean changed = isLayoutModeOptical(mParent) ?
            setOpticalFrame(l, t, r, b) : setFrame(l, t, r, b);
	// 验证PFLAG_LAYOUT_REQUIRED标记
    if (changed || (mPrivateFlags & PFLAG_LAYOUT_REQUIRED) == PFLAG_LAYOUT_REQUIRED) {
		// 需要自定义的确定子布局方法
        onLayout(changed, l, t, r, b);
		// 去除measure时增加的标记
        mPrivateFlags &= ~PFLAG_LAYOUT_REQUIRED;
        ListenerInfo li = mListenerInfo;
        if (li != null && li.mOnLayoutChangeListeners != null) {
            ArrayList<OnLayoutChangeListener> listenersCopy =
                    (ArrayList<OnLayoutChangeListener>)li.mOnLayoutChangeListeners.clone();
            int numListeners = listenersCopy.size();
            for (int i = 0; i < numListeners; ++i) {
                listenersCopy.get(i).onLayoutChange(this, l, t, r, b, oldL, oldT, oldR, oldB);
            }
        }
    }
	// 去除在requestLayout中增加的标记
    mPrivateFlags &= ~PFLAG_FORCE_LAYOUT;
    mPrivateFlags3 |= PFLAG3_IS_LAID_OUT;
}
```
上述代码在主要部分都进行了注释，这里主要看下setFrame方法，使用这个方法传入l, t, r, b，用于确定view在父View中的位置。setOpticalFrame方法最终也会调用setFrame，我们来看下setFrame方法
```Java
protected boolean setFrame(int left, int top, int right, int bottom) {
    boolean changed = false;
    
    if (mLeft != left || mRight != right || mTop != top || mBottom != bottom) {
        changed = true;
        // Remember our drawn bit
        int drawn = mPrivateFlags & PFLAG_DRAWN;
        int oldWidth = mRight - mLeft;
        int oldHeight = mBottom - mTop;
        int newWidth = right - left;
        int newHeight = bottom - top;
        boolean sizeChanged = (newWidth != oldWidth) || (newHeight != oldHeight);
        // Invalidate our old position
        invalidate(sizeChanged);
        ...
    }
    return changed;
}
```
从上述代码可以看出，在setFrame中我们将会判断新旧的位置参数，如果有一个不相等，则会发起invalidate请求，进行View重绘。看到这里也就明白，为什么在requestLayout之后一般都会进行View重绘了。<br>
现在继续来看上面的DecorView#layout方法，在判断是否需要invalidate之后，将会进行PFLAG_LAYOUT_REQUIRED标记的验证，之后运行DecorView的onLayout方法，它会对超出的View会进行平移，这个只是提一下，主要关注的是其继承的FrameLayout的onLayout方法
```Java
protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
	layoutChildren(left, top, right, bottom, false /* no force left gravity */);
}

void layoutChildren(int left, int top, int right, int bottom,
							  boolean forceLeftGravity) {
	final int count = getChildCount();

	final int parentLeft = getPaddingLeftWithForeground();
	final int parentRight = right - left - getPaddingRightWithForeground();

	final int parentTop = getPaddingTopWithForeground();
	final int parentBottom = bottom - top - getPaddingBottomWithForeground();

	for (int i = 0; i < count; i++) {
		final View child = getChildAt(i);
		if (child.getVisibility() != GONE) {
			final LayoutParams lp = (LayoutParams) child.getLayoutParams();

			final int width = child.getMeasuredWidth();
			final int height = child.getMeasuredHeight();

			int childLeft;
			int childTop;

			int gravity = lp.gravity;
			if (gravity == -1) {
				gravity = DEFAULT_CHILD_GRAVITY;
			}

			final int layoutDirection = getLayoutDirection();
			final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
			final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;
			// 根据不同属性确定子View的位置
			switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
				case Gravity.CENTER_HORIZONTAL:
					childLeft = parentLeft + (parentRight - parentLeft - width) / 2 +
					lp.leftMargin - lp.rightMargin;
					break;
				case Gravity.RIGHT:
					if (!forceLeftGravity) {
						childLeft = parentRight - width - lp.rightMargin;
						break;
					}
				case Gravity.LEFT:
				default:
					childLeft = parentLeft + lp.leftMargin;
			}

			switch (verticalGravity) {
				case Gravity.TOP:
					childTop = parentTop + lp.topMargin;
					break;
				case Gravity.CENTER_VERTICAL:
					childTop = parentTop + (parentBottom - parentTop - height) / 2 +
					lp.topMargin - lp.bottomMargin;
					break;
				case Gravity.BOTTOM:
					childTop = parentBottom - height - lp.bottomMargin;
					break;
				default:
					childTop = parentTop + lp.topMargin;
			}

			child.layout(childLeft, childTop, childLeft + width, childTop + height);
		}
	}
}
```
上述代码中，主要是遍历所有子View，根据不同的居中情况(如果设置了居中属性的话)，重新确定子View的left与top布局，之后根据这些位置以及View测量的宽高，确定right、bottom的位置，最后传递给子View的layout方法。这里的子View按照我们之前在measure中的流程，就是LinearLayout，我们来看看他的onLayout方法
```Java
protected void onLayout(boolean changed, int l, int t, int r, int b) {
    if (mOrientation == VERTICAL) {
        layoutVertical(l, t, r, b);
    } else {
        layoutHorizontal(l, t, r, b);
    }
}
```
LinearLayout的onLyaout方法和onMeasure的逻辑一样，根据LinearLayout属性来选择布局方法，我们也和上次一样选取layoutHorizontal方法看看，这里截取了其中的主要代码
```Java
void layoutHorizontal(int left, int top, int right, int bottom) {
	...
	final int count = getVirtualChildCount();
	...
	for (int i = 0; i < count; i++) {
		int childIndex = start + dir * i;
		final View child = getVirtualChildAt(childIndex);

		if (child == null) {
			childLeft += measureNullChild(childIndex);
		} else if (child.getVisibility() != GONE) {
			final int childWidth = child.getMeasuredWidth();
			final int childHeight = child.getMeasuredHeight();
			int childBaseline = -1;

			final LinearLayout.LayoutParams lp =
					(LinearLayout.LayoutParams) child.getLayoutParams();

			if (baselineAligned && lp.height != LayoutParams.MATCH_PARENT) {
				childBaseline = child.getBaseline();
			}
			
			int gravity = lp.gravity;
			if (gravity < 0) {
				gravity = minorGravity;
			}
			
			switch (gravity & Gravity.VERTICAL_GRAVITY_MASK) {
				case Gravity.TOP:
					childTop = paddingTop + lp.topMargin;
					if (childBaseline != -1) {
						childTop += maxAscent[INDEX_TOP] - childBaseline;
					}
					break;

				case Gravity.CENTER_VERTICAL:
					// Removed support for baseline alignment when layout_gravity or
					// gravity == center_vertical. See bug #1038483.
					// Keep the code around if we need to re-enable this feature
					// if (childBaseline != -1) {
					//     // Align baselines vertically only if the child is smaller than us
					//     if (childSpace - childHeight > 0) {
					//         childTop = paddingTop + (childSpace / 2) - childBaseline;
					//     } else {
					//         childTop = paddingTop + (childSpace - childHeight) / 2;
					//     }
					// } else {
					childTop = paddingTop + ((childSpace - childHeight) / 2)
							+ lp.topMargin - lp.bottomMargin;
					break;

				case Gravity.BOTTOM:
					childTop = childBottom - childHeight - lp.bottomMargin;
					if (childBaseline != -1) {
						int descent = child.getMeasuredHeight() - childBaseline;
						childTop -= (maxDescent[INDEX_BOTTOM] - descent);
					}
					break;
				default:
					childTop = paddingTop;
					break;
			}

			if (hasDividerBeforeChildAt(childIndex)) {
				childLeft += mDividerWidth;
			}

			childLeft += lp.leftMargin;
			// 确定子View布局
			setChildFrame(child, childLeft + getLocationOffset(child), childTop,
					childWidth, childHeight);
			// 不断增大的childLeft
			childLeft += childWidth + lp.rightMargin +
					getNextLocationOffset(child);

			i += getChildrenSkipCount(child, childIndex);
		}
	}
}
```
在上述代码中，会遍历所有的子View，并根据它的居中属性，对childTop进行调整。调用setChildFrame方法来确定子View的布局，如果你跟踪一下，你会发现，它其实调用了child.layout方法，同时每次调用之后，不断增大当前的childLeft，以使下一次的布局不断平移。<br>

按照measure的流程，接下来应该是View的layout流程，View#layout方法，之后又跳转到View的onLayout方法，这个方法只是一个空的实现，一般情况下我们也不需要重载该方法。
```Java
protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
}
```
现在我们已经完成了layout方法的分析。<br>
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/requestLayout.png" alt="requestLayout" title="requestLayout" width="800"/><br>

## 四、总结
本文分析了View的requestLayout流程，并以DecorView和LinearLayout为例，对View的测量流程、布局流程进行了分析。如果在阅读过程中，有任何疑问与问题，欢迎与我联系。<br>

**博客:www.idtkm.com**<br>
**GitHub:https://github.com/Idtk**<br>
**微博:http://weibo.com/Idtk**<br>
**邮箱:IdtkMa@gmail.com**<br>
<br>
