# Path中的贝塞尔曲线

[自定义View系列目录](https://github.com/Idtk/Blog)

## 一、数学中的贝塞尔
### 概述
贝塞尔曲线于1962年，由法国工程师皮埃尔·贝塞尔（Pierre Bézier）所广泛发表，他运用贝塞尔曲线来为汽车的主体进行设计。贝塞尔曲线最初由Paul de Casteljau于1959年运用de Casteljau算法开发，以稳定数值的方法求出贝塞尔曲线。<br>
在计算机图形学中贝赛尔曲线的运用也很广泛，Photoshop中的钢笔效果，Flash5的贝塞尔曲线工具，在软件GUI开发中一般也会提供对应的方法来实现贝赛尔曲线。
### 线性贝塞尔曲线
给定点P0、P1，线性贝兹曲线只是一条两点之间的直线。
就像由0至1的连续t，B（t）描述一条由P0至P1的直线。
<br><br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/Bézier_1_big.gif" alt="Bezier" title="Bezier" width="300" />
<br><br>
### 二次贝塞尔曲线
二次方贝塞尔曲线的路径由给定点P0、P1、P2的函数B（t）追踪：<br>

<br><br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/Bézier_2_big.svg.png" alt="Bezier" title="Bezier" width="300" />
<br><br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/Bézier_2_big.gif" alt="Bezier" title="Bezier" width="300" />
<br><br>
### 三阶贝塞尔曲线
P0、P1、P2、P3四个点在平面或在三维空间中定义了三次方贝塞尔曲线。曲线起始于P0走向P1，并从P2的方向来到P3。一般不会经过P1或P2；这两个点只是在那里提供方向资讯。P0和P1之间的间距，决定了曲线在转而趋进P2之前，走向P1方向的“长度有多长”。<br>
<br><br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/Bézier_3_big.svg.png" alt="Bezier" title="Bezier" width="300" />
<br>
<br><img src="https://github.com/Idtk/Blog/blob/master/Image/Bézier_3_big.gif" alt="Bezier" title="Bezier" width="300" />
<br><br>

(**PS:以上内容来自[Wike](https://zh.wikipedia.org/wiki/%E8%B2%9D%E8%8C%B2%E6%9B%B2%E7%B7%9A)**)
## 二、Android中的贝塞尔使用
数学的一节内容只要有个直观的感受就好，重点主要是Android中的贝塞尔曲线。
<br>
### 1、quadTo
**Path.quadTo**是Android的二次贝塞尔曲线的API，示例如下。

```Java
@Override
protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    mViewWidth = w;
    mViewHeight = h;
    mWidth = mViewWidth - getPaddingLeft() - getPaddingRight();
    mHeight = mViewHeight - getPaddingTop() - getPaddingBottom();
    r = Math.min(mWidth,mHeight)*0.4f;
    rectF = new RectF(-r,-r,r,r);
}


@Override
protected void onDraw(Canvas canvas) {
    mPaint.setColor(Color.MAGENTA);
    mPaint.setStrokeWidth(8);
    canvas.translate(mViewWidth/2,mViewHeight/2);
    mPath.moveTo(-r/2,0);
    mPath.quadTo(0,-r/2,r/2,0);
    canvas.drawPath(mPath,mPaint);
    mPath.rewind();
    mPaint.setColor(Color.GRAY);
    mPaint.setStrokeWidth(20);
    canvas.drawPoints(new float[]{
            start.x,start.y,
            end.x,end.y,
            control1.x,control1.y
    },mPaint);
}
```
<br><img src="https://github.com/Idtk/Blog/blob/master/Image/quadTo.png" alt="quadTo" title="quadTo" width="300" />
<br>

* 使用二次贝塞尔函数完成一个正弦波，这里使用rQuadTo
```Java
@Override
protected void onDraw(Canvas canvas) {
    mPaint.setColor(Color.MAGENTA);
    mPaint.setStrokeWidth(8);
    canvas.translate(mViewWidth/2,mViewHeight/2);
    mPath.moveTo(-r,0);
    mPath.rQuadTo(r/2,-r/8,r,0);
    mPath.rQuadTo(r/2,r/8,r,0);
    canvas.drawPath(mPath,mPaint);
    mPath.rewind();
}
```
<br><img src="https://github.com/Idtk/Blog/blob/master/Image/quadTo2.png" alt="quadTo" title="quadTo" width="300" />
<br>

* 增加一个圆,以r为半径，(0,0)为圆心
```Java
canvas.drawCircle(0,0,r,mPaint2);
```

* 增加一个连接正弦波两端点的半圆弧
```Java
rectF = new RectF(-r,-r,r,r);
mPath.addArc(rectF,0,180);
```
<br><img src="https://github.com/Idtk/Blog/blob/master/Image/quadTo3.png" alt="quadTo" title="quadTo" width="300" />
<br>
* **更进一步**<br>
如果我希望水量是30%，80%或者别的值呢？其实只需要修改正弦值的周期即可。具体代码如下(已省略set方法) : 
```Java
/**
 * Created by Idtk on 2016/6/19.
 * Blog : http://www.idtkm.com
 * GitHub : https://github.com/Idtk
 * 描述 : 显示百分比注水球
 */
public class Bezier extends View {

    private Paint mPaint,mPaint2;
    private Path mPath = new Path();
    protected int mViewWidth,mViewHeight;
    protected int mWidth,mHeight;
    private float r,rArc,x;
    private float percent=0.5f;
    private RectF rectF;
    private PointF mPointF = new PointF(0,0);

    public Bezier2(Context context) {
        this(context, null);

    }

    public Bezier2(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPaint = new Paint();
        mPaint.setColor(Color.BLACK);
        mPaint.setStrokeWidth(3);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setTextSize(100);

        mPaint2 = new Paint();
        mPaint2.setColor(Color.CYAN);
        mPaint2.setStrokeWidth(8);
        mPaint2.setStyle(Paint.Style.FILL);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mViewWidth = w;
        mViewHeight = h;

        mWidth = mViewWidth - getPaddingLeft() - getPaddingRight();
        mHeight = mViewHeight - getPaddingTop() - getPaddingBottom();

        r = Math.min(mWidth,mHeight)*0.4f;
        rectF = new RectF(-r,-r,r,r);
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
        canvas.translate(mViewWidth/2,mViewHeight/2);
        canvas.drawCircle(0,0,r,mPaint);
        rArc = r*(1-2*percent);
        double angle= Math.acos((double) rArc/r);
        x = r*(float) Math.sin(angle);
        mPath.addArc(rectF,90-(float) Math.toDegrees(angle),(float) Math.toDegrees(angle)*2);
        mPath.moveTo(-x,rArc);
        mPath.rQuadTo(x/2,-r/8,x,0);
        mPath.rQuadTo(x/2,r/8,x,0);
        canvas.drawPath(mPath,mPaint2);
        mPath.rewind();
        NumberFormat numberFormat =NumberFormat.getPercentInstance();
        numberFormat.setMinimumFractionDigits(1);
        textCenter(new String[]{numberFormat.format(percent)},mPaint,canvas,mPointF, Paint.Align.CENTER);
    }

    /**
     * 多行文本居中、居右、居左
     * @param strings 文本字符串列表
     * @param paint 画笔
     * @param canvas 画布
     * @param point 点的坐标
     * @param align 居中、居右、居左
     */
    protected void textCenter(String[] strings, Paint paint, Canvas canvas, PointF point, Paint.Align align){
        paint.setTextAlign(align);
        Paint.FontMetrics fontMetrics= paint.getFontMetrics();
        float top = fontMetrics.top;
        float bottom = fontMetrics.bottom;
        int length = strings.length;
        float total = (length-1)*(-top+bottom)+(-fontMetrics.ascent+fontMetrics.descent);
        float offset = total/2-bottom;
        for (int i = 0; i < length; i++) {
            float yAxis = -(length - i - 1) * (-top + bottom) + offset;
            canvas.drawText(strings[i], point.x, point.y + yAxis, paint);
        }
    }
}
```
<br><img src="https://github.com/Idtk/Blog/blob/master/Image/quadTo4.png" alt="quadTo" title="quadTo" width="300" />
<br>

### 2、cubicTo
**Path.cubicTo**是Android的三次贝塞尔曲线的API，示例如下。

```Java
@Override
protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    mViewWidth = w;
    mViewHeight = h;
    mWidth = mViewWidth - getPaddingLeft() - getPaddingRight();
    mHeight = mViewHeight - getPaddingTop() - getPaddingBottom();
    r = Math.min(mWidth,mHeight)*0.4f;
    start.x = -r;
    start.y = 0;
    control1.x = -r/2;
    control1.y = -r/2;
    control2.x = r/2;
    control2.y = r/2;
    end.x = r;
    end.y = 0;
}
@Override
protected void onDraw(Canvas canvas) {
    canvas.translate(mViewWidth/2,mViewHeight/2);
    mPaint.setColor(Color.MAGENTA);
    mPaint.setStrokeWidth(8);
    mPath.moveTo(start.x,start.y);
    mPath.cubicTo(control1.x,control1.y,control2.x,control2.y,end.x,end.y);
    canvas.drawPath(mPath,mPaint);
    mPath.rewind();
    mPaint.setColor(Color.GRAY);
    mPaint.setStrokeWidth(20);
    canvas.drawPoints(new float[]{
            start.x,start.y,
            end.x,end.y,
            control1.x,control1.y,
            control2.x,control2.y
    },mPaint);
}
```
<br><img src="https://github.com/Idtk/Blog/blob/master/Image/cubicTo.png" alt="cubicTo" title="cubicTo" width="300" />
<br>
是不是和之前quadTo生成的水纹很像？如果想要在上面的**百分比注水球类**中加入动画，并且不要求一定是正弦波的情况下，使用cubicTo可以更为方便。<br><br>
我最近在做的开源图表库[SmallChart](https://github.com/Idtk/SmallChart)中绘制曲线时，就是用了cubicTo。同时使用了MPChart项目中的算法，对高阶贝塞尔曲线进行了降阶，相关代码如下 :
```Java
cubicPath.moveTo((cur.x-xAxisData.getMinimum())*xAxisData.getAxisScale(),
        -(cur.y-yAxisData.getMinimum())*yAxisData.getAxisScale()*animatedValue);

for (int j=1; j< curveData.getValue().size(); j++){
    prevPrev = curveData.getValue().get(j == 1 ? 0 : j - 2);
    prev = curveData.getValue().get(j-1);
    cur = curveData.getValue().get(j);
    next = curveData.getValue().size() > j+1 ? curveData.getValue().get(j+1) : cur;
    prevDx = (cur.x-prevPrev.x)*intensity*xAxisData.getAxisScale();
    prevDy = (cur.y-prevPrev.y)*intensity*yAxisData.getAxisScale();
    curDx = (next.x-prev.x)*intensity*xAxisData.getAxisScale();
    curDy = (next.y-prev.y)*intensity*yAxisData.getAxisScale();
    cubicPath.cubicTo((prev.x-xAxisData.getMinimum())*xAxisData.getAxisScale()+prevDx,
            -(((prev.y-yAxisData.getMinimum())*yAxisData.getAxisScale()+prevDy)*animatedValue),
            ((cur.x-xAxisData.getMinimum())*xAxisData.getAxisScale()-curDx),
            -(((cur.y-yAxisData.getMinimum())*yAxisData.getAxisScale()-curDy)*animatedValue),
            ((cur.x-xAxisData.getMinimum())*xAxisData.getAxisScale()),
            -(((cur.y-yAxisData.getMinimum())*yAxisData.getAxisScale())*animatedValue));
}
canvas.save();
canvas.translate(offset,0);
cubicPaint.setColor(curveData.getColor());
canvas.drawPath(cubicPath,cubicPaint);
cubicPath.rewind();
```
<br><img src="https://github.com/Idtk/Blog/blob/master/Image/cubicTo2.png" alt="cubicTo" title="cubicTo" width="300" />
<br>

## 三、小结
本文介绍了Path的贝塞尔曲线，同时通过百分比注水图以及平滑曲线的例子，进行了实战。贝塞尔曲线是Android中非常重要的方法，可以实现多种效果，比如以下的几个例子：

* QQ的拖拽小红点<br>
* 饿了吗点餐动画<br>
* 水滴效果<br>
* 平滑曲线<br>
* 弹性效果<br>

如果在阅读过程中，有任何疑问与问题，欢迎与我联系。<br>
**博客:www.idtkm.com**<br>
**GitHub:https://github.com/Idtk**<br>
**微博:http://weibo.com/Idtk**<br>
**邮箱:IdtkMa@gmail.com**<br>
<br>
