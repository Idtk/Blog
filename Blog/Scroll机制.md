<br><img src="https://github.com/Idtk/Blog/blob/master/Image/scroll.png" alt="scroll" title="scroll" /><br>

```Java
/**
 * 初始化ScrollX、ScrollY,同时获取子View的实例，获取其半径参数
 *
 * startScroll(int startX, int startY, int dx, int dy, int duration)方法：
 * startX、startY表示滑动开始的坐标；dx、dy表示需要位移的距离；duration表示移位的时间
 *
 * invalidate()方法：在View树重绘的时候会调用computeScrollOffset()方法
 */
public void smoothScrollTo(){
    viewA = (ViewA) getChildAt(0);
    int ScrollX = getScrollX();
    int ScrollY = getScrollY();
    realHeight = getHeight()-2*viewA.getRadius();
    mScroller.startScroll(ScrollX, 0, 0, -realHeight, 1000);
    invalidate();
}
/**
 * 先调用computeScrollOffset()方法，计算出新的CurrX和CurrY值，
 * 判断是否需要继续滑动。
 *
 * scrollTo(currX,currY):滑动到上面计算出的新的currX和currY位置处
 *
 * postInvalidate():通知View树重绘，作用和invalidate()方法一样
 */
@Override
public void computeScroll() {
    if(mScroller.computeScrollOffset()){
        int currX = mScroller.getCurrX();
        int currY = mScroller.getCurrY();
        Log.d("cylog", "滑动坐标"+"("+getScrollX()+","+getScrollY()+")");
        scrollTo(currX, currY);
        postInvalidate();
    }
}
```