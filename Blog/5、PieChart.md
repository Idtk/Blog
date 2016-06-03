# 自定义PieChart

<img src="https://github.com/Idtk/CustomView/blob/master/gif/PieChart.gif" alt="PieChat" title="PieChat" width="300" /><br>

(**PS: 经过之前[4篇博客](http://www.idtkm.com/category/customview/)的基础知识学习，终于可以开始编写PieChart了 ~\(≧▽≦)/~啦啦啦**)

## 一、数据需求
来分析下，用户需要提供怎样的数据，首先要有数据的值，然后还需要对应的数据名称，以及颜色。绘制PieChart需要什么呢，由图可以看出，需要百分比值，扇形角度，色块颜色。所以总共属性有:
```Java
public class PieData {
    private String name;
    private float value;
    private float percentage;
    private int color = 0;
    private float angle = 0;
}
```
各属性的set与get请自行增加。

## 二、构造函数
构造函数中，增加一些xml设置，创建一个attrs.xml
```Java
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <declare-styleable name="PieChart">
        <attr name="name" format="string"/>
        <attr name="percentDecimal" format="integer"/>
        <attr name="textSize" format="dimension"/>
    </declare-styleable>
</resources>
```
这是只设置了一部分属性，如果你有强迫症希望全部设置的话，可以自行增加。在PieChart中使用TypedArray进行属性的获取。建议使用如下的写法，**可以避免在没有设置属性时，也运行getXXX方法**。
```Java
TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.PieChart, defStyleAttr,defStyleRes);
int n = array.getIndexCount();
for (int i=0; i<n; i++){
    int attr = array.getIndex(i);
    switch (attr){
        case R.styleable.PieChart_name:
            name = array.getString(attr);
            break;
        case R.styleable.PieChart_percentDecimal:
            percentDecimal = array.getInt(attr,percentDecimal);
            break;
        case R.styleable.PieChart_textSize:
            percentTextSize = array.getDimensionPixelSize(attr,percentTextSize);
            break;
    }
}
array.recycle();
```
## 三、动画函数
绘制一个完整的圆，旋转的角度为360，动画时间为可set参数，默认5秒，监听animatedValue参数，用于与绘制时进行计算。ValueAnimator类涉及到的参数的意义请查看[自定义View——Canvas与ValueAnimator](http://www.idtkm.com/customview/customview2/)文章。
```Java
private void initAnimator(long duration){
    if (animator !=null &&animator.isRunning()){
        animator.cancel();
        animator.start();
    }else {
        animator=ValueAnimator.ofFloat(0,360).setDuration(duration);
        animator.setInterpolator(timeInterpolator);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                animatedValue = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.start();
    }
}
```
## 四、onMeasure
View默认的onMeasure方法中，并没有根据测量模式，对布局宽高进行调整，所以为了适应**wrap_content**的布局设置，需要对onMeasure方法进行重写。<br>

```Java
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int width = measureDimension(widthMeasureSpec);
    int height = measureDimension(heightMeasureSpec);
    setMeasuredDimension(width,height);
}
```
<br>
重写的onMeasure方法，调用了自定义的**measureDimension**方法处理数据，完成后交给系统的setMeasuredDimension方法。接下来看下自定义的measureDimension方法。<br>
```Java
private int measureDimension(int measureSpec){
    int size = measureWrap(mPaint);
    int specMode = MeasureSpec.getMode(measureSpec);
    int specSize = MeasureSpec.getSize(measureSpec);
    switch (specMode){
        case MeasureSpec.UNSPECIFIED:
            size = measureWrap(mPaint);
            break;
        case MeasureSpec.EXACTLY:
            size = specSize;
            break;
        case MeasureSpec.AT_MOST:
			//合适尺寸不得大于View的尺寸
            size = Math.min(specSize,measureWrap(mPaint));
            break;
    }
    return size;
}
```
<br>
**measureDimension**根据测量的类型，分别计算尺寸的长度，每个类型的含义在[第一篇](http://www.idtkm.com/customview/customview1/)中已经进行了说明，在这里不在赘述。**EXACTLY**是在xml中定义**match_parent以及具体的数值**是使用，而**AT_MOST**则是在**wrap_content**时使用，**measureWrap**方法用于计算当前PieChart的最小合适长度，接下来看看这个方法。
<br>
```Java
private int measureWrap(Paint paint){
    float wrapSize;
    if (mPieData!=null&&mPieData.size()>1){
        NumberFormat numberFormat =NumberFormat.getPercentInstance();
        numberFormat.setMinimumFractionDigits(percentDecimal);
        paint.setTextSize(percentTextSize);
        float percentWidth = paint.measureText(numberFormat.format(mPieData.get(stringId).getPercentage())+"");
        paint.setTextSize(centerTextSize);
        float nameWidth = paint.measureText(name+"");
        wrapSize = (percentWidth*4+nameWidth*1.0f)*(float) offsetScaleRadius;
    }else {
        wrapSize = 0;
    }
    return (int) wrapSize;
}
```
<br>
测量宽高的方式类似于TextView，根据PieChart中的**图名与百分比文本的宽度**进行计算的。其中stringId是在处理数据的过程中，计算出的拥有最长字符的区域Id。<br>

从代码中可以看出，**wrap_content情况下的，PieChart的宽高就等于百分比字符长度的4倍，加上图名的长度。**
<br>

## 五、onSizeChanged
在此函数中，获取当前View的宽高以及根据**padding**值计算出的宽高，同时进行PieChart绘制所需的半径以及布局位置设置。<br>
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/onSizeChange.png" alt="onSizeChanged" title="onSizeChanged" width="300" /><br>
```Java
protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    mWidth = w-getPaddingLeft()-getPaddingRight();//适应padding设置
    mHeight = h-getPaddingTop()-getPaddingBottom();//适应padding设置
    mViewWidth = w;
    mViewHeight = h;
    //标准圆环
    //圆弧
    r = (float) (Math.min(mWidth,mHeight)/2*widthScaleRadius);// 饼状图半径
    // 饼状图绘制区域
    rectF.left = -r;
    rectF.top = -r;
    rectF.right =r;
    rectF.bottom = r;
    //白色圆弧
    //透明圆弧
    rTra = (float) (r*radiusScaleTransparent);
    rectFTra.left = -rTra;
    rectFTra.top = -rTra;
    rectFTra.right = rTra;
    rectFTra.bottom = rTra;
    //白色圆
    rWhite = (float) (r*radiusScaleInside);

    //浮出圆环
    //圆弧
    // 饼状图半径
    rF = (float) (Math.min(mWidth,mHeight)/2*widthScaleRadius*offsetScaleRadius);
    // 饼状图绘制区域
    rectFF.left = -rF;
    rectFF.top = -rF;
    rectFF.right = rF;
    rectFF.bottom = rF;
    ...
}
```
## 六、onDraw
onDraw分为绘制扇形，绘制文本，绘制图名三个部分。绘制扇形和文本时需要与Valueanimator的监听值进行计算，完成动画；另外还要在Touch时进行交互，完成浮出动画。<br>
在进行具体的绘制之前，需要坐标原点平移至中心位置，并且判断数据是否为空。
### 1、绘制扇形

```Java
float currentStartAngle = 0;// 当前起始角度
canvas.save();
canvas.rotate(mStartAngle);
float drawAngle;
for (int i=0; i<mPieData.size(); i++){
    PieData pie = mPieData.get(i);
    if (Math.min(pie.getAngle()-1,animatedValue-currentStartAngle)>=0){
        drawAngle = Math.min(pie.getAngle()-1,animatedValue-currentStartAngle);
    }else {
        drawAngle = 0;
    }
    if (i==angleId){
        drawArc(canvas,currentStartAngle,drawAngle,pie,rectFF,rectFTraF,reatFWhite,mPaint);
    }else {
        drawArc(canvas,currentStartAngle,drawAngle,pie,rectF,rectFTra,rectFIn,mPaint);
    }
    currentStartAngle += pie.getAngle();
}
canvas.restore();
```
* 根据当前的初始角度旋转画布。初始化扇形的起始角度，通过累加计算出下一次的起始角度。<br>
* drawArc用于绘制扇形，和[上一篇](http://www.idtkm.com/customview/customview4/)最后的环形图片一样，通过一大一小两个扇形进行补集运算，获得可知半径的及宽度的圆环，只不过这里多了一个为了立体效果而增加的半透明圆弧。<br>

<img src="https://github.com/Idtk/Blog/blob/master/Image/%E7%BB%98%E5%88%B6%E6%89%87%E5%BD%A2.png" alt="绘制扇形" title="绘制扇形" width="300" /><br>

* 绘制扇形时，使用当前的动画值减去起始角度与当前的扇形经过的角度对比取小，作为当前扇形的需要绘制的经过角度。减1是为了生存扇形区域之间的间隔。<br>
* angleId用于Touch时显示点击是哪一块扇形，具体判断会在TouchEvent中进行。

### 2、绘制文本

```Java
//扇形百分比文字
currentStartAngle = mStartAngle;
for (int i=0; i<mPieData.size(); i++){
    PieData pie = mPieData.get(i);
    mPaint.setColor(percentTextColor);
    mPaint.setTextSize(percentTextSize);
    mPaint.setTextAlign(Paint.Align.CENTER);
    NumberFormat numberFormat =NumberFormat.getPercentInstance();
    numberFormat.setMinimumFractionDigits(percentDecimal);
    //根据Paint的TextSize计算Y轴的值
    if (animatedValue>pieAngles[i]-pie.getAngle()/2&&percentFlag) {
        if (i == angleId) {
            drawText(canvas,pie,currentStartAngle,numberFormat,true);
        } else {
            if (pie.getAngle() > minAngle) {
                drawText(canvas,pie,currentStartAngle,numberFormat,false);
            }
        }
        currentStartAngle += pie.getAngle();
    }
}
```
<br>
* **文本是有方向的，无法在画布旋转后绘制**，所以初始化当前扇形的起始角度为PieChart的起始角度。<br>
* 然后循环绘制文本，当扇形绘制到当前区域的1/2时，开始绘制当前区域的文字。为了防止文本遮挡视线，在绘制前需要判断此扇形经过的角度是否大于最小显示角度。<br>
* angleId用于Touch时显示点击是哪一块扇形，具体判断会在TouchEvent中进行。
<br>

```Java
private void drawText(Canvas canvas, PieData pie ,float currentStartAngle, NumberFormat numberFormat,boolean flag){
    int textPathX = (int) (Math.cos(Math.toRadians(currentStartAngle + (pie.getAngle() / 2))) * (r + rTra) / 2);
    int textPathY = (int) (Math.sin(Math.toRadians(currentStartAngle + (pie.getAngle() / 2))) * (r + rTra) / 2);
    mPoint.x = textPathX;
    mPoint.y = textPathY;
    String[] strings;
    if (flag){
        strings = new String[]{pie.getName() + "", numberFormat.format(pie.getPercentage()) + ""};
    }else {
        strings = new String[]{numberFormat.format(pie.getPercentage()) + ""};
    }
    textCenter(strings, mPaint, canvas, mPoint, Paint.Align.CENTER);
}
```
<img src="https://github.com/Idtk/Blog/blob/master/Image/%E7%BB%98%E5%88%B6%E6%96%87%E6%9C%AC.png" alt="绘制文本" title="绘制文本" width="300" /><br>
drawText函数的主要作用就是根据传入的Pie，获取大小扇形的半径合除以2，角度取一半，计算出扇形中心点，然后使用之前介绍的[textCenter多行文本居中函数](http://www.idtkm.com/customview/customview3/)进行文本绘制。最后累加当前扇形的起始角度，用于下一个扇形使用。<br>

### 3、绘制图名
```Java
//饼图名
mPaint.setColor(centerTextColor);
mPaint.setTextSize(centerTextSize);
mPaint.setTextAlign(Paint.Align.CENTER);
//根据Paint的TextSize计算Y轴的值
mPoint.x=0;
mPoint.y=0;
String[] strings = new String[]{name+""};
textCenter(strings,mPaint,canvas,mPoint, Paint.Align.CENTER);
```
绘制图名的部分就比较简单了，和之前绘制单个Pie时类似，获取x，y坐标为(0,0),然后使用textCenter多行文本绘制函数进行文本绘制。

## 七、onTouchEvent
onTouchEvent用于处理当前的点击事件，具体内容在[第一篇文章](http://www.idtkm.com/customview/customview1/)中已经进行了说明，这里使用其中的ACTION_DOWN与ACTION_UP事件。<br>

```Java
public boolean onTouchEvent(MotionEvent event) {
    if (touchFlag&&mPieData.size()>0){
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                float x = event.getX()-(mWidth/2);
                float y = event.getY()-(mHeight/2);
                float touchAngle = 0;
                if (x<0&&y<0){
                    touchAngle += 180;
                }else if (y<0&&x>0){
                    touchAngle += 360;
                }else if (y>0&&x<0){
                    touchAngle += 180;
                }
                touchAngle +=Math.toDegrees(Math.atan(y/x));
                touchAngle = touchAngle-mStartAngle;
                if (touchAngle<0){
                    touchAngle = touchAngle+360;
                }
                float touchRadius = (float) Math.sqrt(y*y+x*x);
                if (rTra< touchRadius && touchRadius< r){
                    angleId = -Arrays.binarySearch(pieAngles,(touchAngle))-1;
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                angleId = -1;
                invalidate();
                return true;
        }
    }
    return super.onTouchEvent(event);
}
```
* 运行之前需要判断PieChart是否开启了点击效果，同事需要判断数据不为空。<br>
* 在用户点击下的时候，获取当前的坐标，计算出这个点与原点的距离以及角度。通过距离可以判断出是否点击在了扇形区域上，而通过角度可以判断出点击了哪一个区域。将判断出的区域Id传递给angleId值，就像我们之前在onDraw中说的那样，重新绘制，根据angleId浮出指定的扇形区域。<br>
* 用户手指离开屏幕时，重置angleId为默认值，并使用invalidate()函数，重新绘制onDraw中变化的部分。
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/onTouchEvent.png" alt="onTouchEvent" title="onTouchEvent" width="300" /><br>
## 八、小结
经过之前4篇的知识准备，终于迎来了本章的PieChart的具体实现。在本文中重温了之前的绘制流程的各个函数，VlaueAnimator函数，以及Canvas、Path的使用方法，并使用这些方法完成了一个自定义饼图的绘制。在之后的文章中还会进行几个图表的实战，比如下面这个曲线图。<br>

<img src="https://github.com/Idtk/Blog/blob/master/Image/cubic.gif" alt="曲线图" title="曲线图" width="300" /><br>
如果在阅读过程中，有任何疑问与问题，欢迎与我联系。<br>
**博客:www.idtkm.com**<br>
**GitHub:https://github.com/Idtk**<br>
**邮箱:IdtkMa@gmail.com**<br>
<br>
[PieChart源码](https://github.com/Idtk/CustomView)请点击
