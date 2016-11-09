# 属性动画连续输出值分析


大家在开发过程中，经常会使用到属性动画，这里以ValueAnimator为例，它可以通过addUpdateListener来监听输出的数值，就像这样 :
```Java
private void initAnimator(long duration) {
    if (animator != null && animator.isRunning()) {
        animator.cancel();
        animator.start();
    } else {
        animator = ValueAnimator.ofFloat(0, 1000).setDuration(duration);
        animator.setInterpolator(timeInterpolator);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                animatedValue = (float) animation.getAnimatedValue();
                now = SystemClock.elapsedRealtime();
                diff = now - preNow;
                preNow = now;
                Log.d("animator", "animatedValue: " + animatedValue + " now: " + now + " diff: " + diff);
            }
        });
        animator.start();
    }
}
```
其输出值animatedValue是一个随时间变化的连续数值，就像这样 :
<br>
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/Animator/Log.png" alt="Log" title="Log" />
<br>

***注：本文基于源码sdk level 24***

## 那么它是如何产生这样一组连续变化的数值呢？

从`animator.start()`方法开始

##### `ValueAnimator.start`

```Java
public void start() {
    start(false);
}

private void start(boolean playBackwards) {
    if (Looper.myLooper() == null) {
        throw new AndroidRuntimeException("Animators may only be run on Looper threads");
    }
    mReversing = playBackwards;
    if (playBackwards && mSeekFraction != -1 && mSeekFraction != 0) {
        if (mRepeatCount == INFINITE) {
            float fraction = (float) (mSeekFraction - Math.floor(mSeekFraction));
            mSeekFraction = 1 - fraction;
        } else {
            mSeekFraction = 1 + mRepeatCount - mSeekFraction;
        }
    }
    mStarted = true;
    mPaused = false;
    mRunning = false;
    mLastFrameTime = 0;
    AnimationHandler animationHandler = AnimationHandler.getInstance();
	// 注册Vsync并执行
    animationHandler.addAnimationFrameCallback(this, (long) (mStartDelay * sDurationScale));
    if (mStartDelay == 0 || mSeekFraction >= 0) {
		// 开始动画
        startAnimation();
        if (mSeekFraction == -1) {
            setCurrentPlayTime(0);
        } else {
            setCurrentFraction(mSeekFraction);
        }
    }
}
```
`start`方法先对Looper进行了检查，之后是一个回放计算以及一些设置。然后获取了`AnimationHandler`的单例，`AnimationHandler`并不是一个Handler，而是包含了一个Calback。这里比较重要的代码有两段`animationHandler.addAnimationFrameCallback(this, (long) (mStartDelay * sDurationScale));`和`startAnimation();`，他们分别进行了Vsync的注册以及动画的启动。

### 加入回调

##### `AnimationHandler.addAnimationFrameCallback`

```Java
public void addAnimationFrameCallback(final AnimationFrameCallback callback, long delay) {
    if (mAnimationCallbacks.size() == 0) {
      // mFrameCallback就是之前说的Callback
      // getProvider获得的是一个实现了AnimationFrameCallbackProvider
      // 接口的内部类MyFrameCallbackProvider
        getProvider().postFrameCallback(mFrameCallback);
    }
    if (!mAnimationCallbacks.contains(callback)) {
      // 把动画加入回调列表
        mAnimationCallbacks.add(callback);
    }
    if (delay > 0) {
        mDelayedCallbackStartTime.put(callback, (SystemClock.uptimeMillis() + delay));
    }
}
```

##### `MyFrameCallbackProvider.postFrameCallback`

```Java
public void postFrameCallback(Choreographer.FrameCallback callback) {
  // mChoreographer也是个单例
  // 将mFrameCallback继续传递至Choreographer
    mChoreographer.postFrameCallback(callback);
}
```

##### `Choreographer.postFrameCallback`

```java
public void postFrameCallback(FrameCallback callback) {
    postFrameCallbackDelayed(callback, 0);
}
```

##### `Choreographer.postFrameCallbackDelayed`

```java
public void postFrameCallbackDelayed(FrameCallback callback, long delayMillis) {
    if (callback == null) {
        throw new IllegalArgumentException("callback must not be null");
    }
    postCallbackDelayedInternal(CALLBACK_ANIMATION,
            callback, FRAME_CALLBACK_TOKEN, delayMillis);
}
```

##### `Choreographer.postCallbackDelayedInternal`

```Java
private void postCallbackDelayedInternal(int callbackType,
        Object action, Object token, long delayMillis) {
    if (DEBUG_FRAMES) {
        Log.d(TAG, "PostCallback: type=" + callbackType
                + ", action=" + action + ", token=" + token
                + ", delayMillis=" + delayMillis);
    }
    synchronized (mLock) {
        final long now = SystemClock.uptimeMillis();
        final long dueTime = now + delayMillis;
      // 将要执行的回调用保存到mCallbackQueues中
        mCallbackQueues[callbackType].addCallbackLocked(dueTime, action, token);
      // delayMillis=0,所以执行scheduleFrameLocked
        if (dueTime <= now) {
            scheduleFrameLocked(now);
        } else {
            Message msg = mHandler.obtainMessage(MSG_DO_SCHEDULE_CALLBACK, action);
            msg.arg1 = callbackType;
            msg.setAsynchronous(true);
            mHandler.sendMessageAtTime(msg, dueTime);
        }
    }
}
```

### Vsync的注册

##### Choreographer.scheduleFrameLocked

```java
private void scheduleFrameLocked(long now) {
    if (!mFrameScheduled) {
        mFrameScheduled = true;
      // 检查是否使用了Vsync机制
        if (USE_VSYNC) {
            if (DEBUG_FRAMES) {
                Log.d(TAG, "Scheduling next frame on vsync.");
            }
          // 检查是否运行在Looper线程
            if (isRunningOnLooperThreadLocked()) {
                scheduleVsyncLocked();
            } else {
                Message msg = mHandler.obtainMessage(MSG_DO_SCHEDULE_VSYNC);
                msg.setAsynchronous(true);
                mHandler.sendMessageAtFrontOfQueue(msg);
            }
        } else {
            final long nextFrameTime = Math.max(
                    mLastFrameTimeNanos / TimeUtils.NANOS_PER_MS + sFrameDelay, now);
            if (DEBUG_FRAMES) {
                Log.d(TAG, "Scheduling next frame in " + (nextFrameTime - now) + " ms.");
            }
            Message msg = mHandler.obtainMessage(MSG_DO_FRAME);
            msg.setAsynchronous(true);
            mHandler.sendMessageAtTime(msg, nextFrameTime);
        }
    }
}
```

##### `Choreographer.scheduleVsyncLocked`

```java
mDisplayEventReceiver = USE_VSYNC ? new FrameDisplayEventReceiver(looper) : null;

private void scheduleVsyncLocked() {
    mDisplayEventReceiver.scheduleVsync();
}
```

在查看`DisplayEventReceiver.scheduleVsync`方法之前，我们先看看FrameDisplayEventReceiver的构造函数

```java
private final class FrameDisplayEventReceiver extends DisplayEventReceiver
        implements Runnable {
	...
    public FrameDisplayEventReceiver(Looper looper) {
        super(looper);
    }

public DisplayEventReceiver(Looper looper) {
    if (looper == null) {
        throw new IllegalArgumentException("looper must not be null");
    }
    mMessageQueue = looper.getQueue();
  // 注册vsync
    mReceiverPtr = nativeInit(new WeakReference<DisplayEventReceiver>(this), mMessageQueue);
    mCloseGuard.open("dispose");
}
```

`FrameDisplayEventReceiver`继承了`DisplayEventReceiver`，成为了Vsync事件的接收者，同时也实现了起`onVsync`方法，这个方法我将在后面进行介绍。而最重要的`nativeInit`方法实现了当前动画与Vsync的关联，注册成为Vsync的接收者。现在我们接着看`scheduleVsync`方法。

### 执行Vsync

##### `DisplayEventReceiver.scheduleVsync`

```java
public void scheduleVsync() {
    if (mReceiverPtr == 0) {
        Log.w(TAG, "Attempted to schedule a vertical sync pulse but the display event "
                + "receiver has already been disposed.");
    } else {
      // 接收Vsync信号
        nativeScheduleVsync(mReceiverPtr);
    }
}
```

通过底层代码nativeScheduleVsync将会调用Vsync接收者的`onVsync`方法，而我们之前说的`FrameDisplayEventReceiver`就是这个接收者。

##### `FrameDisplayEventReceiver.onVsync`

```java
public void onVsync(long timestampNanos, int builtInDisplayId, int frame) {
    if (builtInDisplayId != SurfaceControl.BUILT_IN_DISPLAY_ID_MAIN) {
        Log.d(TAG, "Received vsync from secondary display, but we don't support "
                + "this case yet.  Choreographer needs a way to explicitly request "
                + "vsync for a specific display to ensure it doesn't lose track "
                + "of its scheduled vsync.");
        scheduleVsync();
        return;
    }
    long now = System.nanoTime();
    if (timestampNanos > now) {
        Log.w(TAG, "Frame time is " + ((timestampNanos - now) * 0.000001f)
                + " ms in the future!  Check that graphics HAL is generating vsync "
                + "timestamps using the correct timebase.");
        timestampNanos = now;
    }
    if (mHavePendingVsync) {
        Log.w(TAG, "Already have a pending vsync event.  There should only be "
                + "one at a time.");
    } else {
        mHavePendingVsync = true;
    }
  // 同步时间
    mTimestampNanos = timestampNanos;
  // 同步帧
    mFrame = frame;
    Message msg = Message.obtain(mHandler, this);
    msg.setAsynchronous(true);
    mHandler.sendMessageAtTime(msg, timestampNanos / TimeUtils.NANOS_PER_MS);
}
```

通过mhandler，将执行doFrame方法

##### `FrameDisplayEventReceiver.doFrame`

```java
void doFrame(long frameTimeNanos, int frame) {
    final long startNanos;
    synchronized (mLock) {
        if (!mFrameScheduled) {
            return; // no work to do
        }
        ...
      // 消息传递之前的时间
        long intendedFrameTimeNanos = frameTimeNanos;
      // 起始时间
        startNanos = System.nanoTime();
      // 计算消息传递花费时间
        final long jitterNanos = startNanos - frameTimeNanos;
        if (jitterNanos >= mFrameIntervalNanos) {
          // 计算消息传递错过的帧数
            final long skippedFrames = jitterNanos / mFrameIntervalNanos;
            ...
            final long lastFrameOffset = jitterNanos % mFrameIntervalNanos;
            ...
            frameTimeNanos = startNanos - lastFrameOffset;
        }
        if (frameTimeNanos < mLastFrameTimeNanos) {
            ...
            scheduleVsyncLocked();
            return;
        }
        mFrameInfo.setVsync(intendedFrameTimeNanos, frameTimeNanos);
        mFrameScheduled = false;
        mLastFrameTimeNanos = frameTimeNanos;
    }
  // 执行回调，也就是这之前的mFrameCallback
    try {
        Trace.traceBegin(Trace.TRACE_TAG_VIEW, "Choreographer#doFrame");
        mFrameInfo.markInputHandlingStart();
        doCallbacks(Choreographer.CALLBACK_INPUT, frameTimeNanos);
        mFrameInfo.markAnimationsStart();
        doCallbacks(Choreographer.CALLBACK_ANIMATION, frameTimeNanos);
        mFrameInfo.markPerformTraversalsStart();
        doCallbacks(Choreographer.CALLBACK_TRAVERSAL, frameTimeNanos);
        doCallbacks(Choreographer.CALLBACK_COMMIT, frameTimeNanos);
    } finally {
        Trace.traceEnd(Trace.TRACE_TAG_VIEW);
    }
    ...
}
```

在这里对动画的运行时间进行了跳帧，这也就是我们在Log中看到的动画启动时，间隔时间较长的原因。

### 执行回调

##### `FrameDisplayEventReceiver.doCallbacks`

```Java
void doCallbacks(int callbackType, long frameTimeNanos) {
    CallbackRecord callbacks;
    synchronized (mLock) {
        final long now = System.nanoTime();
      // 获取到达执行时间的Callback
        callbacks = mCallbackQueues[callbackType].extractDueCallbacksLocked(
                now / TimeUtils.NANOS_PER_MS);
        if (callbacks == null) {
            return;
        }
        mCallbacksRunning = true;
        if (callbackType == Choreographer.CALLBACK_COMMIT) {
            final long jitterNanos = now - frameTimeNanos;
            Trace.traceCounter(Trace.TRACE_TAG_VIEW, "jitterNanos", (int) jitterNanos);
            if (jitterNanos >= 2 * mFrameIntervalNanos) {
                final long lastFrameOffset = jitterNanos % mFrameIntervalNanos
                        + mFrameIntervalNanos;
                frameTimeNanos = now - lastFrameOffset;
                mLastFrameTimeNanos = frameTimeNanos;
            }
        }
    }
    try {
        Trace.traceBegin(Trace.TRACE_TAG_VIEW, CALLBACK_TRACE_TITLES[callbackType]);
      // 逐个执行到达时间的Callback
        for (CallbackRecord c = callbacks; c != null; c = c.next) {
            c.run(frameTimeNanos);
        }
    } finally {
        synchronized (mLock) {
            mCallbacksRunning = false;
            do {
                final CallbackRecord next = callbacks.next;
                recycleCallbackLocked(callbacks);
                callbacks = next;
            } while (callbacks != null);
        }
        Trace.traceEnd(Trace.TRACE_TAG_VIEW);
    }
}
```

`c.run`将执行实现了`FrameCallback`接口的`doFrame`方法，也就是我们之前说的回调

##### `mFrameCallback`

```java
private final Choreographer.FrameCallback mFrameCallback = new Choreographer.FrameCallback() {
    @Override
    public void doFrame(long frameTimeNanos) {
        doAnimationFrame(getProvider().getFrameTime());
        if (mAnimationCallbacks.size() > 0) {
          // 这里是循环的地方
            getProvider().postFrameCallback(this);
        }
    }
};
```

##### `AnimationHandler.doAnimationFrame`

```java
private void doAnimationFrame(long frameTime) {
    int size = mAnimationCallbacks.size();
    long currentTime = SystemClock.uptimeMillis();
    for (int i = 0; i < size; i++) {
        final AnimationFrameCallback callback = mAnimationCallbacks.get(i);
        if (callback == null) {
            continue;
        }
        if (isCallbackDue(callback, currentTime)) {
          // 这里的callback就是实现了AnimationFrameCallback
          // 接口的ValueAnimator
            callback.doAnimationFrame(frameTime);
            if (mCommitCallbacks.contains(callback)) {
                getProvider().postCommitCallback(new Runnable() {
                    @Override
                    public void run() {
                        commitAnimationFrame(callback, getProvider().getFrameTime());
                    }
                });
            }
        }
    }
    cleanUpList();
}
```

##### `ValueAnimator.doAnimationFrame`

```java
public final void doAnimationFrame(long frameTime) {
    AnimationHandler handler = AnimationHandler.getInstance();
    if (mLastFrameTime == 0) {
        // First frame
        handler.addOneShotCommitCallback(this);
        if (mStartDelay > 0) {
            startAnimation();
        }
        if (mSeekFraction < 0) {
            mStartTime = frameTime;
        } else {
            long seekTime = (long) (getScaledDuration() * mSeekFraction);
            mStartTime = frameTime - seekTime;
            mSeekFraction = -1;
        }
        mStartTimeCommitted = false; // allow start time to be compensated for jank
    }
    mLastFrameTime = frameTime;
    if (mPaused) {
        mPauseTime = frameTime;
        handler.removeCallback(this);
        return;
    } else if (mResumed) {
        mResumed = false;
        if (mPauseTime > 0) {
            // Offset by the duration that the animation was paused
            mStartTime += (frameTime - mPauseTime);
            mStartTimeCommitted = false; // allow start time to be compensated for jank
        }
        handler.addOneShotCommitCallback(this);
    }
    final long currentTime = Math.max(frameTime, mStartTime);
    boolean finished = animateBasedOnTime(currentTime);
    if (finished) {
        endAnimation();
    }
}
```

##### `ValueAnimator.animateBasedOnTime`

```java
boolean animateBasedOnTime(long currentTime) {
    boolean done = false;
  // mRunning要到执行startAnimation才会为true
    if (mRunning) {
        final long scaledDuration = getScaledDuration();
        final float fraction = scaledDuration > 0 ?
                (float)(currentTime - mStartTime) / scaledDuration : 1f;
        final float lastFraction = mOverallFraction;
        final boolean newIteration = (int) fraction > (int) lastFraction;
        final boolean lastIterationFinished = (fraction >= mRepeatCount + 1) &&
                (mRepeatCount != INFINITE);
        if (scaledDuration == 0) {
            // 0 duration animator, ignore the repeat count and skip to the end
            done = true;
        } else if (newIteration && !lastIterationFinished) {
            // Time to repeat
            if (mListeners != null) {
                int numListeners = mListeners.size();
                for (int i = 0; i < numListeners; ++i) {
                    mListeners.get(i).onAnimationRepeat(this);
                }
            }
        } else if (lastIterationFinished) {
            done = true;
        }
        mOverallFraction = clampFraction(fraction);
      // 获取当前执行的分数
        float currentIterationFraction = getCurrentIterationFraction(mOverallFraction);
      // 设置动画值
        animateValue(currentIterationFraction);
    }
    return done;
}
```

所以我们这里先来看一下startAnimation函数

##### `ValueAnimator.startAnimation`

```java
private void startAnimation() {
  ...
    mAnimationEndRequested = false;
  // 初始化
    initAnimation();
  // 设置mRunning为true，animateBasedOnTime函数中的代码得以执行
    mRunning = true;
    if (mSeekFraction >= 0) {
        mOverallFraction = mSeekFraction;
    } else {
        mOverallFraction = 0f;
    }
    if (mListeners != null) {
      // 通知动画启动
        notifyStartListeners();
    }
}
```

我们这里继续接着之前的animateBasedOnTime函数，这里假设动画执行的分数还不为1。

##### `ValueAnimator.animateValue`

```java
void animateValue(float fraction) {
    fraction = mInterpolator.getInterpolation(fraction);
    mCurrentFraction = fraction;
    int numValues = mValues.length;
    for (int i = 0; i < numValues; ++i) {
      // 根据分数计算当前数值
        mValues[i].calculateValue(fraction);
    }
    if (mUpdateListeners != null) {
        int numListeners = mUpdateListeners.size();
        for (int i = 0; i < numListeners; ++i) {
          // onAnimationUpdate就是我们监听输出值时的接口
            mUpdateListeners.get(i).onAnimationUpdate(this);
        }
    }
}
```

### 循环产生连续的值

我们现在再来回头看看mFrameCallback

```java
private final Choreographer.FrameCallback mFrameCallback = new Choreographer.FrameCallback() {
    @Override
    public void doFrame(long frameTimeNanos) {
      // 这是我们之前的
        doAnimationFrame(getProvider().getFrameTime());
        if (mAnimationCallbacks.size() > 0) {
            getProvider().postFrameCallback(this);
        }
    }
};
```

当Vsync执行其`onVsync`方法后，经过跳转执行到了`doFrame`方法，而在`animationHandler.addAnimationFrameCallback`方法中我们将动画本身加入了`mAnimationCallbacks`列表中，所以这里经过判断之后，将会继续执行`getProvider().postFrameCallback(this)`，之后的流程就像之前一样了。

我们现在找到了循环的地方，那么什么时候循环将会停止呢？

## 那么什么时候输出值将会结束呢？

对于这个问题，我们回头看一下animateBasedOnTime函数

##### `ValueAnimator.animateBasedOnTime`

```java
boolean animateBasedOnTime(long currentTime) {
    boolean done = false;
  // mRunning要到执行startAnimation才会为true
    if (mRunning) {
        final long scaledDuration = getScaledDuration();
      // 当执行时间 >= (设置的时间*执行次数)时，fraction >= 1
        final float fraction = scaledDuration > 0 ?
                (float)(currentTime - mStartTime) / scaledDuration : 1f;
        final float lastFraction = mOverallFraction;
        final boolean newIteration = (int) fraction > (int) lastFraction;
      // 这里假设重复次数为0，执行时间刚好等于设置的时间
      // lastIterationFinished =(1 >= 0+1) && (0 != -1)
        final boolean lastIterationFinished = (fraction >= mRepeatCount + 1) &&
                (mRepeatCount != INFINITE);
        if (scaledDuration == 0) {
            // 0 duration animator, ignore the repeat count and skip to the end
            done = true;
        } else if (newIteration && !lastIterationFinished) {
            // Time to repeat
            if (mListeners != null) {
                int numListeners = mListeners.size();
                for (int i = 0; i < numListeners; ++i) {
                    mListeners.get(i).onAnimationRepeat(this);
                }
            }
        } else if (lastIterationFinished) {
          // lastIterationFinished为true，这里将会执行
            done = true;
        }
        mOverallFraction = clampFraction(fraction);
      // 获取当前执行的分数
        float currentIterationFraction = getCurrentIterationFraction(mOverallFraction);
      // 设置动画值
        animateValue(currentIterationFraction);
    }
  // 所以返回结果为true
    return done;
}
```

当动画执行大于等于设定时间时，animateBasedOnTime返回true。

在`ValueAnimator.doAnimationFrame`中，如果animateBasedOnTime返回true，则将执行`endAnimation()`函数。

##### `ValueAnimator.endAnimation`

```java
private void endAnimation() {
    if (mAnimationEndRequested) {
        return;
    }
    AnimationHandler handler = AnimationHandler.getInstance();
  // 将会设置当前动画的回调为null
    handler.removeCallback(this);
    mAnimationEndRequested = true;
    mPaused = false;
    if ((mStarted || mRunning) && mListeners != null) {
        if (!mRunning) {
            // If it's not yet running, then start listeners weren't called. Call them now.
            notifyStartListeners();
         }
        ArrayList<AnimatorListener> tmpListeners =
                (ArrayList<AnimatorListener>) mListeners.clone();
        int numListeners = tmpListeners.size();
        for (int i = 0; i < numListeners; ++i) {
            tmpListeners.get(i).onAnimationEnd(this);
        }
    }
    mRunning = false;
    mStarted = false;
    mStartListenersCalled = false;
    mReversing = false;
    mLastFrameTime = 0;
    if (Trace.isTagEnabled(Trace.TRACE_TAG_VIEW)) {
        Trace.asyncTraceEnd(Trace.TRACE_TAG_VIEW, getNameForTrace(),
                System.identityHashCode(this));
    }
}
```

##### `AnimationHandler.removeCallback`

```java
public void removeCallback(AnimationFrameCallback callback) {
    mCommitCallbacks.remove(callback);
    mDelayedCallbackStartTime.remove(callback);
    int id = mAnimationCallbacks.indexOf(callback);
    if (id >= 0) {
        mAnimationCallbacks.set(id, null);
        mListDirty = true;
    }
}
```

设置为null之后有什么用了，我们这里回到Vsync的调用，`onVsync→doFrame→doCallbacks→doFrame→doAnimationFrame`，在`AnimationHandler.doAnimationFrame`的方法最后将执行cleanUpList方法。

##### `AnimationHandler.cleanUpList`

```java
private void cleanUpList() {
    if (mListDirty) {
        for (int i = mAnimationCallbacks.size() - 1; i >= 0; i--) {
            if (mAnimationCallbacks.get(i) == null) {
              // 如果回调为null，则将移除回调
                mAnimationCallbacks.remove(i);
            }
        }
        mListDirty = false;
    }
}
```

设置为null的回调被移除后，动画也就自然而然的停止了。



## 总结

最后整理下流程 : 

<br>
<img src="http://ww3.sinaimg.cn/mw690/9e727a53gw1f9llc9cnb3j20k41lg472.jpg" alt="Animator" title="Animator" />
<br>

如果在阅读过程中，有任何疑问与问题，欢迎与我联系。<br>
**博客:www.idtkm.com**<br>
**GitHub:https://github.com/Idtk**<br>
**微博:http://weibo.com/Idtk**<br>
**邮箱:IdtkMa@gmail.com**<br>
<br>

## Thanks

[D_clock爱吃葱花](http://www.jianshu.com/users/ec95b5891948/latest_articles)

[姚家艺](http://blog.desmondyao.com/)
