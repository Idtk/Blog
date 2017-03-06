# RecyclerView绘制流程的简单分析

本文将简单分析下RecyclerView的绘制流程。

## onMeasure

既然是一个View布局，就从`onMeasure`开始。

```Java
protected void onMeasure(int widthSpec, int heightSpec) {
	if (mLayout.mAutoMeasure) {
		final int widthMode = MeasureSpec.getMode(widthSpec);
		final int heightMode = MeasureSpec.getMode(heightSpec);
		final boolean skipMeasure = widthMode == MeasureSpec.EXACTLY
				&& heightMode == MeasureSpec.EXACTLY;
				
		mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec);
		
		if (mState.mLayoutStep == State.STEP_START) {
			dispatchLayoutStep1();
		}

		mLayout.setMeasureSpecs(widthSpec, heightSpec);
		
		dispatchLayoutStep2();

		mLayout.setMeasuredDimensionFromChildren(widthSpec, heightSpec);
	}
}
```

在`onMeasure`中，`mLayout`就是一个LayoutManager对象，RecyclerView将onMeasure的计算交给了LayoutManager，在`LayoutManager#onMeasure`中又调用了`RecyclerView#defaultOnMeasure`方法，在其中调用了`LayoutManager#chooseSize`方法。

```Java
public static int chooseSize(int spec, int desired, int min) {
	final int mode = View.MeasureSpec.getMode(spec);
	final int size = View.MeasureSpec.getSize(spec);
	switch (mode) {
		case View.MeasureSpec.EXACTLY:
			return size;
		case View.MeasureSpec.AT_MOST:
			return Math.min(size, Math.max(desired, min));
		case View.MeasureSpec.UNSPECIFIED:
		default:
			return Math.max(desired, min);
	}
}
```
可以很明显的看出此方法，根据MessureSpec类型计算了View的宽高尺寸，之后将会调用`dispatchLayoutStep2`方法对item以及子view进行测量。`onLayout`中也将调用`dispatchLayoutStep2`方法，我们将在其中一起说明。

## onLayout

这里看下`onLayout`方法。

```java
protected void onLayout(boolean changed, int l, int t, int r, int b) {
	dispatchLayout();	
}

void dispatchLayout() {
	if (mState.mLayoutStep == State.STEP_START) {
		dispatchLayoutStep1();
		mLayout.setExactMeasureSpecsFrom(this);
		dispatchLayoutStep2();
	} 
	
	dispatchLayoutStep3();
}
```

`dispatchLayoutStep2()`方法中又调用`LayoutManager#onLayoutChildren`方法进行布局。这里以LinearLayoutManager为例。

```java
public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
	// layout algorithm:
	// 1) by checking children and other variables, find an anchor coordinate and an anchor
	//  item position.
	// 2) fill towards start, stacking from bottom
	// 3) fill towards end, stacking from top
	// 4) scroll to fulfill requirements like stack from bottom.
	// create layout state
	
	int startOffset;
	int endOffset;
	final int firstLayoutDirection;

	onAnchorReady(recycler, state, mAnchorInfo, firstLayoutDirection);
	detachAndScrapAttachedViews(recycler);
	mLayoutState.mInfinite = resolveIsInfinite();
	mLayoutState.mIsPreLayout = state.isPreLayout();
	if (mAnchorInfo.mLayoutFromEnd) {

	} else {
		// 向下布局
		updateLayoutStateToFillEnd(mAnchorInfo);
		mLayoutState.mExtra = extraForEnd;
		// 填充Item
		fill(recycler, mLayoutState, state, false);
		endOffset = mLayoutState.mOffset;
		final int lastElement = mLayoutState.mCurrentPosition;
		if (mLayoutState.mAvailable > 0) {
			extraForStart += mLayoutState.mAvailable;
		}
		// 向上布局
		updateLayoutStateToFillStart(mAnchorInfo);
		mLayoutState.mExtra = extraForStart;
		mLayoutState.mCurrentPosition += mLayoutState.mItemDirection;
		fill(recycler, mLayoutState, state, false);
		startOffset = mLayoutState.mOffset;
		
	}
	
}
```

布局的流程在开头的注释中已经清楚的说明了，这里不再赘述。这里的关注点在`LinearLayoutManager#fill`方法。

```java
int fill(RecyclerView.Recycler recycler, LayoutState layoutState,
		RecyclerView.State state, boolean stopOnFocusable) {
	// 存储当前可见空间
	final int start = layoutState.mAvailable;
	// 计算可用布局的宽高
	int remainingSpace = layoutState.mAvailable + layoutState.mExtra;
	LayoutChunkResult layoutChunkResult = mLayoutChunkResult;
	// 迭代填充item
	while ((layoutState.mInfinite || remainingSpace > 0) && layoutState.hasMore(state)) {
		layoutChunkResult.resetInternal();
		// 布局item
		layoutChunk(recycler, state, layoutState, layoutChunkResult);
		if (layoutChunkResult.mFinished) {
			break;
		}
		// 计算布局的偏移位置
		layoutState.mOffset += layoutChunkResult.mConsumed * layoutState.mLayoutDirection;

		if (!layoutChunkResult.mIgnoreConsumed || mLayoutState.mScrapList != null
				|| !state.isPreLayout()) {
			layoutState.mAvailable -= layoutChunkResult.mConsumed;
			// 计算剩余的空间
			remainingSpace -= layoutChunkResult.mConsumed;
		}
	}
	return start - layoutState.mAvailable;
}
```

在`fill`方法中将会循环调用`layoutChunk`方法进行布局。每次布局完成之后将计算剩余的可用空间，之后判断是否还需要继续布局Item。我们这里来看下布局的`LinearLayoutManager#layoutChunk`方法。

```java
void layoutChunk(RecyclerView.Recycler recycler, RecyclerView.State state,
		LayoutState layoutState, LayoutChunkResult result) {
	// 获取item view
	View view = layoutState.next(recycler);
	// 获取布局参数
	LayoutParams params = (LayoutParams) view.getLayoutParams();
	if (layoutState.mScrapList == null) {
		if (mShouldReverseLayout == (layoutState.mLayoutDirection
				== LayoutState.LAYOUT_START)) {
			// 增加item view
			addView(view);
		} else {
			addView(view, 0);
		}
	} 
	// 测量item
	measureChildWithMargins(view, 0, 0);
	// 计算item使用的空间
	result.mConsumed = mOrientationHelper.getDecoratedMeasurement(view);
	int left, top, right, bottom;
	// 按照水平或者数值方向布局，计算item坐标
	if (mOrientation == VERTICAL) {
		if (isLayoutRTL()) {
			right = getWidth() - getPaddingRight();
			left = right - mOrientationHelper.getDecoratedMeasurementInOther(view);
		} else {
			left = getPaddingLeft();
			right = left + mOrientationHelper.getDecoratedMeasurementInOther(view);
		}
		if (layoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
			bottom = layoutState.mOffset;
			top = layoutState.mOffset - result.mConsumed;
		} else {
			top = layoutState.mOffset;
			bottom = layoutState.mOffset + result.mConsumed;
		}
	} else {
	   
	}
  	// item布局
	layoutDecoratedWithMargins(view, left, top, right, bottom);
}

public void measureChildWithMargins(View child, int widthUsed, int heightUsed) {
	final LayoutParams lp = (LayoutParams) child.getLayoutParams();
	// 测量分割线
	final Rect insets = mRecyclerView.getItemDecorInsetsForChild(child);
	widthUsed += insets.left + insets.right;
	heightUsed += insets.top + insets.bottom;

	final int widthSpec = getChildMeasureSpec(getWidth(), getWidthMode(),
			getPaddingLeft() + getPaddingRight() +
					lp.leftMargin + lp.rightMargin + widthUsed, lp.width,
			canScrollHorizontally());
	final int heightSpec = getChildMeasureSpec(getHeight(), getHeightMode(),
			getPaddingTop() + getPaddingBottom() +
					lp.topMargin + lp.bottomMargin + heightUsed, lp.height,
			canScrollVertically());
	if (shouldMeasureChild(child, widthSpec, heightSpec, lp)) {
      	// 子View测量
		child.measure(widthSpec, heightSpec);
	}
}

public void layoutDecoratedWithMargins(View child, int left, int top, int right,
		int bottom) {
	final LayoutParams lp = (LayoutParams) child.getLayoutParams();
	final Rect insets = lp.mDecorInsets;
   // 子View布局
	child.layout(left + insets.left + lp.leftMargin, top + insets.top + lp.topMargin,
			right - insets.right - lp.rightMargin,
			bottom - insets.bottom - lp.bottomMargin);
}
```

从代码中可以看出，最后分别调用了item的`measure`函数与`layout`函数对view进行了测量和布局。下面我们来看下`onDraw`方法。

## onDraw

```java
public void onDraw(Canvas c) {
	super.onDraw(c);

	final int count = mItemDecorations.size();
	for (int i = 0; i < count; i++) {
		mItemDecorations.get(i).onDraw(c, this, mState);
	}
}
```

`onDraw`的代码比较简单，除了调用`super.onDraw`外，还对分割线进行了绘制。



##  最后

本文通过View的基本方法对RecyclerView的绘制进行了简单的分析。RecyclerView通过LayoutManager类将测量、布局、绘制等从自身中分离了出来，减少了代码的耦合，使其更加灵活、更易扩展。如果在阅读过程中，有任何疑问与问题，欢迎与我联系。

&nbsp;&nbsp;**博客: www.idtkm.com**<br>
&nbsp;&nbsp;**GitHub: https://github.com/Idtk**<br>
&nbsp;&nbsp;**微博: http://weibo.com/Idtk**<br>
&nbsp;&nbsp;**邮箱: IdtkMa@gmail.com**<br>

