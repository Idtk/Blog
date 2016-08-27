# Android事件分发机制源码解析

在[更简单的学习Android事件分发](http://www.idtkm.com/customview/customview11/)中，使用日志、比喻、流程图相结合的方式，以更简单的方法去分析了Android的事件分发机制。本篇文章将采用分析源码的方式，更深入的解析Android的事件分发机制。

******

## 自定义View系列

* View篇

	* [Android坐标系与View绘制流程](https://github.com/Idtk/Blog/blob/master/Blog/1%E3%80%81CoordinateAndProcess.md)
	* [Canvas与ValueAnimator](https://github.com/Idtk/Blog/blob/master/Blog/2%E3%80%81CanvasAndValueAnimator.md)
	* [View多行文本居中](https://github.com/Idtk/Blog/blob/master/Blog/3%E3%80%81Multi-lineTextCenter.md)
	* [Path图形与逻辑运算](https://github.com/Idtk/Blog/blob/master/Blog/4%E3%80%81PathFigureAndLogical.md)
	* [PieChart扇形图的实现](https://github.com/Idtk/Blog/blob/master/Blog/5%E3%80%81PieChart.md)
	* [Path中的贝塞尔曲线](https://github.com/Idtk/Blog/blob/master/Blog/6%E3%80%81Bezier.md)
	* [雷达图(蜘蛛网图)的实现](https://github.com/Idtk/Blog/blob/master/Blog/7%E3%80%81RadarChart.md)

* ViewGroup篇

	* [View的弹性滑动](https://github.com/Idtk/Blog/blob/master/Blog/8%E3%80%81Scroll.md)
	* [View的invalidate传递与绘制流程分析](https://github.com/Idtk/Blog/blob/master/Blog/9%E3%80%81Invalidate.md)
	* [View的requestLayout传递与测量、布局流程分析](https://github.com/Idtk/Blog/blob/master/Blog/10%E3%80%81RequestLayout.md)
	* [更简单的学习Android事件分发](https://github.com/Idtk/Blog/blob/master/Blog/11%E3%80%81TouchEvent.md)
	* Android事件分发机制源码解析


******

## 一、一切从Activity开始

Android的触摸事件，是由windowManagerService进行采集，之后传递到Activiy进行处理。我们这里从Activity#dispatchTouchEvent方法开始解析
```Java
public boolean dispatchTouchEvent(MotionEvent ev) {
    if (ev.getAction() == MotionEvent.ACTION_DOWN) {
        onUserInteraction();
    }
    if (getWindow().superDispatchTouchEvent(ev)) {
        return true;
    }
    return onTouchEvent(ev);
}
```

上述代码中，`onUserInteraction()`是一个空的实现，我们直接来看下
`getWindow().superDispatchTouchEvent(ev)`方法。window是一个抽象的方法，不过系统给它提供了一个实现类PhoneWindow，我们这里看下它的`superDispatchTouchEvent(ev)`方法。

```Java
public boolean superDispatchTouchEvent(MotionEvent event) {
    return mDecor.superDispatchTouchEvent(event);
}

```

上述代码调用了DecorView类的`superDispatchTouchEvent`方法，继续跟进

```Java
public boolean superDispatchTouchEvent(MotionEvent event) {
    return super.dispatchTouchEvent(event);
}
```
上述代码调用了父类的`dispatchTouchEvent`方法，DecorView的父类为FrameLayout，其直接继承了`ViewGroup#dispatchTouchEvent`方法。

## 二、ViewGroup中的事件分发

ViewGroup#dispatchTouchEvent方法比较长，这里只截取部分进行分析

```Java
public boolean dispatchTouchEvent(MotionEvent ev) {
	
	...
		// 在ACTION_DOWN事件时，初始化Touch标记
		// Handle an initial down.
		if (actionMasked == MotionEvent.ACTION_DOWN) {
			// Throw away all previous state when starting a new touch gesture.
			// The framework may have dropped the up or cancel event for the previous gesture
			// due to an app switch, ANR, or some other state change.
			cancelAndClearTouchTargets(ev);
			resetTouchState();
		}

		// Check for interception.
		final boolean intercepted;
		if (actionMasked == MotionEvent.ACTION_DOWN
				|| mFirstTouchTarget != null) {
			// 是否拦截的标志位，假如设置requestDisallowInterceptTouchEvent(true)，
			// 则为true，不拦截事件
			final boolean disallowIntercept = (mGroupFlags & FLAG_DISALLOW_INTERCEPT) != 0;
			if (!disallowIntercept) {
				// 默认返回false
				intercepted = onInterceptTouchEvent(ev);
				ev.setAction(action); // restore action in case it was changed
			} else {
				intercepted = false;
			}
		} else {
			// There are no touch targets and this action is not an initial down
			// so this view group continues to intercept touches.
			intercepted = true;
		}

		...

		// Check for cancelation.
		final boolean canceled = resetCancelNextUpFlag(this)
				|| actionMasked == MotionEvent.ACTION_CANCEL;

		// Update list of touch targets for pointer down, if needed.
		final boolean split = (mGroupFlags & FLAG_SPLIT_MOTION_EVENTS) != 0;
		TouchTarget newTouchTarget = null;
		boolean alreadyDispatchedToNewTouchTarget = false;
		// 不是ACTION_CANCEL事件，并且不拦截事件
		if (!canceled && !intercepted) {

			// If the event is targeting accessiiblity focus we give it to the
			// view that has accessibility focus and if it does not handle it
			// we clear the flag and dispatch the event to all children as usual.
			// We are looking up the accessibility focused host to avoid keeping
			// state since these events are very rare.
			View childWithAccessibilityFocus = ev.isTargetAccessibilityFocus()
					? findChildWithAccessibilityFocus() : null;

			if (actionMasked == MotionEvent.ACTION_DOWN
					|| (split && actionMasked == MotionEvent.ACTION_POINTER_DOWN)
					|| actionMasked == MotionEvent.ACTION_HOVER_MOVE) {
				final int actionIndex = ev.getActionIndex(); // always 0 for down
				final int idBitsToAssign = split ? 1 << ev.getPointerId(actionIndex)
						: TouchTarget.ALL_POINTER_IDS;

				// Clean up earlier touch targets for this pointer id in case they
				// have become out of sync.
				removePointersFromTouchTargets(idBitsToAssign);

				final int childrenCount = mChildrenCount;
				if (newTouchTarget == null && childrenCount != 0) {
					// 获取触摸坐标
					final float x = ev.getX(actionIndex);
					final float y = ev.getY(actionIndex);
					// Find a child that can receive the event.
					// Scan children from front to back.
					final ArrayList<View> preorderedList = buildOrderedChildList();
					final boolean customOrder = preorderedList == null
							&& isChildrenDrawingOrderEnabled();
					final View[] children = mChildren;
					// 遍历所有子View
					for (int i = childrenCount - 1; i >= 0; i--) {
						final int childIndex = customOrder
								? getChildDrawingOrder(childrenCount, i) : i;
						final View child = (preorderedList == null)
								? children[childIndex] : preorderedList.get(childIndex);

						...
						
						resetCancelNextUpFlag(child);
						// 把事件(ACTION_DOWN、ACTION_POINTER_DOWN、ACTION_HOVER_MOVE)传递给子View处理
						if (dispatchTransformedTouchEvent(ev, false, child, idBitsToAssign)) {
							// Child wants to receive touch within its bounds.
							mLastTouchDownTime = ev.getDownTime();
							if (preorderedList != null) {
								// childIndex points into presorted list, find original index
								for (int j = 0; j < childrenCount; j++) {
									if (children[childIndex] == mChildren[j]) {
										mLastTouchDownIndex = j;
										break;
									}
								}
							} else {
								mLastTouchDownIndex = childIndex;
							}
							mLastTouchDownX = ev.getX();
							mLastTouchDownY = ev.getY();
							newTouchTarget = addTouchTarget(child, idBitsToAssign);
							alreadyDispatchedToNewTouchTarget = true;
							break;
						}
						...
					}
					...
				}
				...
				}
			}
		}
		
		// 分发事件到目标View
		// Dispatch to touch targets.
		if (mFirstTouchTarget == null) {
			// 没有找到事件分发目标的情况，将会调用自己的onTouchEvent方法
			// No touch targets so treat this as an ordinary view.
			handled = dispatchTransformedTouchEvent(ev, canceled, null,
					TouchTarget.ALL_POINTER_IDS);
		} else {
			
			// Dispatch to touch targets, excluding the new touch target if we already
			// dispatched to it.  Cancel touch targets if necessary.
			TouchTarget predecessor = null;
			TouchTarget target = mFirstTouchTarget;
			// 这里找到了事件分发的目标
			while (target != null) {
				final TouchTarget next = target.next;
				// ACTION_DOWN已经完成事件分发，并消费了事件，直接返回true
				if (alreadyDispatchedToNewTouchTarget && target == newTouchTarget) {
					handled = true;
				} else {
					final boolean cancelChild = resetCancelNextUpFlag(target.child)
							|| intercepted;
					// 其余事件则需要传递给目标View进行处理
					if (dispatchTransformedTouchEvent(ev, cancelChild,
							target.child, target.pointerIdBits)) {
						handled = true;
					}
					if (cancelChild) {
						if (predecessor == null) {
							mFirstTouchTarget = next;
						} else {
							predecessor.next = next;
						}
						target.recycle();
						target = next;
						continue;
					}
				}
				predecessor = target;
				target = next;
			}
		}
		
		// 对ACTION_CANCEL事件进行处理
		// Update list of touch targets for pointer up or cancel, if needed.
		if (canceled
				|| actionMasked == MotionEvent.ACTION_UP
				|| actionMasked == MotionEvent.ACTION_HOVER_MOVE) {
			// 重置Touch状态
			resetTouchState();
		} else if (split && actionMasked == MotionEvent.ACTION_POINTER_UP) {
			final int actionIndex = ev.getActionIndex();
			final int idBitsToRemove = 1 << ev.getPointerId(actionIndex);
			removePointersFromTouchTargets(idBitsToRemove);
		}
	}

	...
	return handled;
}

// 默认返回false
public boolean onInterceptTouchEvent(MotionEvent ev) {
    return false;
}
```

我们现在来看看传递事件的`dispatchTransformedTouchEvent`方法，同样我也只是截取了其中比较关键的部分

```Java
private boolean dispatchTransformedTouchEvent(MotionEvent event, boolean cancel,
		View child, int desiredPointerIdBits) {
	final boolean handled;
	...
	final MotionEvent transformedEvent;
	// 对transformedEvent的一系列计算
	...
	if (child == null) {
		// 如果没有子View，则执行super.dispatchTouchEvent方法，
		// 调用自己的onTouchEvent方法
		handled = super.dispatchTouchEvent(transformedEvent);
	} else {
		final float offsetX = mScrollX - child.mLeft;
		final float offsetY = mScrollY - child.mTop;
		transformedEvent.offsetLocation(offsetX, offsetY);
		if (! child.hasIdentityMatrix()) {
			transformedEvent.transform(child.getInverseMatrix());
		}
		// 如果有子View，则调用子View#dispatchTouchEvent方法
		handled = child.dispatchTouchEvent(transformedEvent);
	}

	// Done.
	transformedEvent.recycle();
	return handled;
}
```

## 三、View中的事件处理

ViewGroup中不拦截事件，调用子View#dispatchTouchEvent方法进行处理

```Java
public boolean dispatchTouchEvent(MotionEvent event) {
	
	...
	if (onFilterTouchEventForSecurity(event)) {
		// 如果设置了OnTouchListener，使用onTouch对事件进行处理，
		// 并返回true，则不需要再执行onTouchEvent方法
		//noinspection SimplifiableIfStatement
		ListenerInfo li = mListenerInfo;
		if (li != null && li.mOnTouchListener != null
				&& (mViewFlags & ENABLED_MASK) == ENABLED
				&& li.mOnTouchListener.onTouch(this, event)) {
			result = true;
		}

		if (!result && onTouchEvent(event)) {
			result = true;
		}
	}
	...

	return result;
}
```

这里继续看看View#onTouchEvent方法

```Java
public boolean onTouchEvent(MotionEvent event) {
	final float x = event.getX();
	final float y = event.getY();
	final int viewFlags = mViewFlags;
	final int action = event.getAction();

	...

	if (((viewFlags & CLICKABLE) == CLICKABLE ||
			(viewFlags & LONG_CLICKABLE) == LONG_CLICKABLE) ||
			(viewFlags & CONTEXT_CLICKABLE) == CONTEXT_CLICKABLE) {
		switch (action) {
			case MotionEvent.ACTION_UP:
				...
				// 移除长按
				removeLongPressCallback();
				...
					// 检查单击
					performClick();
				...
				break;

			case MotionEvent.ACTION_DOWN:
				...
				// 检测是否为长按
				checkForLongClick(0);
				...
				break;

			....
		}

		return true;
	}

	return false;
}
```
上述代码，主要是检查View是否可以点击，如果可点击，则会返回true，同时也会执行可点击的事件。

## 四、小结
通过本文的源码解析，我们可以更深入的理解Android的事件分发。可以简单的推出一个流程 : Activity→PhoneWindow→DecorView→ViewGroup→View。如果在阅读过程中，有任何疑问与问题，欢迎与我联系。<br>
**博客:www.idtkm.com**<br>
**GitHub:https://github.com/Idtk**<br>
**微博:http://weibo.com/Idtk**<br>
**邮箱:IdtkMa@gmail.com**<br>
<br>

