# 属性动画是如何产生出连续数值的呢？

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
<img src="https://github.com/Idtk/Blog/blob/master/Image/Animator/Log.png" alt="Log" title="Log" />
<br>

***注：本文基于源码sdk level 24***

## 那么它是如何产生这样一组连续变化的数值呢？

从`ValueAnimator.start`方法开始
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
            // Calculate the fraction of the current iteration.
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
这里比较重要的代码有两段`animationHandler.addAnimationFrameCallback(this, (long) (mStartDelay * sDurationScale));`和`startAnimation();`

## Vsync的注册
我们先看看`animationHandler.addAnimationFrameCallback`方法。
```Java
AnimationHandler.addAnimationFrameCallback→
getProvider().postFrameCallback→
MyFrameCallbackProvider.postFrameCallback→
Choreographer.postFrameCallback→
Choreographer.postFrameCallbackDelayed→
Choreographer.postCallbackDelayedInternal
```
