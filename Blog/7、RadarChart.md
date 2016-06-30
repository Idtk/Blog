#自定义View——雷达图(蜘蛛网图)<br>
效果图:<br>
<img src="https://github.com/Idtk/SmallChart/blob/master/image/radar.png" alt="radar" title="radar" width="300"/><br>

## 一、获取View宽高以及cos、sin
在onSizeChanged函数中，可以获取当前View的宽高以及根据padding值计算出的实际绘制区域的宽高，同时进行出雷达图得半径设置并通过PathMeasure类的**getPosTan**方法获得此任意正多边形各角坐标的余弦值、正弦值。如果不熟悉此函数可以查看我之前的文章[Android坐标系与View绘制流程](https://github.com/Idtk/Blog/blob/master/Blog/1%E3%80%81CoordinateAndProcess.md)<br>

因为在之前的文章中并没有介绍getPosTan方法，这里对其进行一个简单的介绍。
```
boolean getPosTan (float distance, float[] pos, float[] tan)
```

* distance为距离当前path起点的距离，取值范围为0到path的长度。
* pos 如果不为null，则返回path当前距离的位置坐标，pos[0] = x,pos[1] = y 。
* tan 如果不为null，则返回当前位置坐标的切线，tan[0] = x, tan[1] = y 。
* 返回值为boolean，true表示成功，数据会存入pas、tan，反之则为失败，数据也不会存入pas、tan。


```Java
@Override
protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    mViewWidth = w;
	mViewHeight = h;
	mWidth = mViewWidth - getPaddingLeft() - getPaddingRight();
	mHeight = mViewHeight - getPaddingTop() - getPaddingBottom();
    radius = Math.min(mWidth,mHeight)*0.35f;
    ...
    //增加圆形路径，起点从90度开始，顺时针旋转
    mPath.addCircle(0,0,mRadarAxisData.getAxisLength(), Path.Direction.CW);
    //为PathMeasure设置路径
    measure.setPath(mPath,true);
   
    float[] cosArray = new float[mRadarAxisData.getTypes().length];
    float[] sinArray = new float[mRadarAxisData.getTypes().length];
    for (int i=0; i<mRadarAxisData.getTypes().length; i++){
    	//获取Path距离起点当前距离的坐标，以及切线
        measure.getPosTan((float) (Math.PI*2*mRadarAxisData.getAxisLength()*i/
                mRadarAxisData.getTypes().length),pos,tan);
        //装填cos、sin
        cosArray[i] = tan[0];
        sinArray[i] = tan[1];
    }
    mPath.reset();
    ...
}
```


## 二、绘制坐标网络
雷达图的坐标网络(即正多边形)的绘制将在onDraw函数中进行。
* 首先通过画布缩放的方式绘制一圈圈的网格。
```Java
for (int i=0; i<number; i++){
    canvas.save();
    //缩放画布
    canvas.scale(1-i/number,1-i/number);
    移动至第一点
    mPathRing.moveTo(0,radarAxisData.getAxisLength());
    //连接个点
    if (radarAxisData.getTypes()!=null)
        for (int j=0; j<radarAxisData.getTypes().length; j++){
            mPathRing.lineTo(radarAxisData.getAxisLength()*radarAxisData.getCosArray()[j],
                    radarAxisData.getAxisLength()*radarAxisData.getSinArray()[j]);
        }
    //闭合路径
    mPathRing.close();
    //绘制路径
    canvas.drawPath(mPathRing,mPaintLine);
    mPathRing.reset();
    canvas.restore();
}
```
* 然后是绘制正多边形各角的连线以及对应的名称
```Java
if (radarAxisData.getTypes()!=null)
    for (int j=0; j<radarAxisData.getTypes().length; j++){
    	//连接各点
        mPathLine.moveTo(0,0);
        mPathLine.lineTo(radarAxisData.getAxisLength()*radarAxisData.getCosArray()[j],
                radarAxisData.getAxisLength()*radarAxisData.getSinArray()[j]);
        //绘制文字
        canvas.save();
        canvas.rotate(180);
        //设置文字坐标
        mPointF.y = -radarAxisData.getAxisLength()*radarAxisData.getSinArray()[j]*1.1f;
        mPointF.x = -radarAxisData.getAxisLength()*radarAxisData.getCosArray()[j]*1.1f;
        //根据cos值，判断文字位置，设置居左、居中、居右
        if (radarAxisData.getCosArray()[j]>0.2){
            textCenter(new String[]{radarAxisData.getTypes()[j]},mPaintText,canvas,mPointF, Paint.Align.RIGHT);
        }else if (radarAxisData.getCosArray()[j]<-0.2){
            textCenter(new String[]{radarAxisData.getTypes()[j]},mPaintText,canvas,mPointF, Paint.Align.LEFT);
        }else {
            textCenter(new String[]{radarAxisData.getTypes()[j]},mPaintText,canvas,mPointF, Paint.Align.CENTER);
        }
        canvas.restore();
    }
mPathLine.close();
canvas.drawPath(mPathLine,mPaintLine);
mPathLine.reset();
canvas.restore();
```
因为文字的方向性，所以必须在绘制的过程中旋转回正方向。同时通过判断cos值的大小，来设置文字的居左、居中、居右。

* 最后给网格绘制刻度，因为y轴正方向是向下的，所以在设置坐标是需这只负值。
```Java
//设置小数点位数
NumberFormat numberFormat = NumberFormat.getNumberInstance();
numberFormat.setMaximumFractionDigits(radarAxisData.getDecimalPlaces());
if (radarAxisData.getIsTextSize())
    for (int i=1; i<number+1; i++){
        mPointF.x = 0;
        mPointF.y = -radarAxisData.getAxisLength()*(1-i/number);
        //绘制文字
        canvas.drawText(numberFormat.format(radarAxisData.getMinimum()+radarAxisData.getInterval()*(number-i))
                +" "+radarAxisData.getUnit(), mPointF.x, mPointF.y, mPaintText);
    }
```
## 三、绘制数据覆盖区域
绘制实际数据也是在onDraw中进行的，只需计算出各个数据在画布上的实际长度，再乘以相应的cos、sin之后，就可以获得相应的坐标点。位移需要注意的是，绘制的点数需要以传入的各角的字符串的数量为准，同时在数据为空的情况下，设置数据为0即可。
```Java
@Override
public void drawGraph(Canvas canvas, float animatedValue) {
    for (int i=0 ; i<radarAxisData.getTypes().length; i++){
        if (i<radarData.getValue().size()) {
            float value = radarData.getValue().get(i);
            float yValue = (value-radarAxisData.getMinimum())*radarAxisData.getAxisScale();
            if (i==0){
            	//移动至第一点
                mPath.moveTo(yValue*radarAxisData.getCosArray()[i],yValue*radarAxisData.getSinArray()[i]);
            }else {
            	//连接其余各点
                mPath.lineTo(yValue*radarAxisData.getCosArray()[i],yValue*radarAxisData.getSinArray()[i]);
            }
        }else {
            mPath.lineTo(0,0);
        }
    }
    mPath.close();
    //填充区域绘制
    mPaintFill.setColor(radarData.getColor());
    mPaintFill.setAlpha(radarData.getAlpha());
    canvas.drawPath(mPath,mPaintFill);
    //描线路径绘制
    mPaintStroke.setColor(radarData.getColor());
    canvas.drawPath(mPath,mPaintStroke);
    mPath.reset();
}
```

## 四、适应wrap_content
View原有的**onMeasure**函数中，使用了**getDefaultSize**方法，来根据不同的测量方式，生成View的实际宽高。来看下**getDefaultSize**的源码 : 
```Java
public static int getDefaultSize(int size, int measureSpec) {
    int result = size;
    int specMode = MeasureSpec.getMode(measureSpec);//获取测量方式
    int specSize = MeasureSpec.getSize(measureSpec);//获取测量数值
    switch (specMode) {
    case MeasureSpec.UNSPECIFIED:
        result = size;
        break;
    case MeasureSpec.AT_MOST:
    case MeasureSpec.EXACTLY:
        result = specSize;
        break;
    }
    return result;
}
```
可以看出**getDefaultSize方法**中，对于xml中设置wrap_content时，使用的**AT_MOST**测量方法并没有做相应处理。<br>
<br>
View中还有另一个方法**resolveSizeAndState**可以满足我们对**AT_MOST**情况下View宽高的需求。源码 : 
```Java
public static int resolveSizeAndState(int size, int measureSpec, int childMeasuredState) {
    final int specMode = MeasureSpec.getMode(measureSpec);
    final int specSize = MeasureSpec.getSize(measureSpec);
    final int result;
    switch (specMode) {
        case MeasureSpec.AT_MOST:
            if (specSize < size) {
                result = specSize | MEASURED_STATE_TOO_SMALL;
            } else {
                result = size;
            }
            break;
        case MeasureSpec.EXACTLY:
            result = specSize;
            break;
        case MeasureSpec.UNSPECIFIED:
        default:
            result = size;
    }
    return result | (childMeasuredState & MEASURED_STATE_MASK);
}
```
* **resolveSizeAndState**方法中，在**AT_MOST**测量模式下。如果**onMeasure**传递的measureSpec值小于，你给定的size值，则会使用
MEASURED_STATE_TOO_SMALL(值为**0x01000000**)整理后的specSize值；如果你给定的size更小，那么就是用你的size作为返回。最后通过与MEASURED_STATE_MASK合成出返回值。

* 另两种情况下和之前的**getDefaultSize**时相同，在**EXACTLY**时，即给定宽高值得情况下，使用了**onMeasure**中获取的值。

* 而在**UNSPECIFIED**时，即View想要多大就多大的情况下，使用了给定的size作为返回值，而我们没有子View，childMeasuredState设置为0即可。最后通过与MEASURED_STATE_MASK合成出返回值。

现在使用**resolveSizeAndState**方法只差size值了，获取size值的方法与之前的[PieChart](https://github.com/Idtk/Blog/blob/master/Blog/5%E3%80%81PieChart.md)类似，通过计算需要绘制文字的宽高以及数量，来计算出size值。
```Java
public int getCurrentWidth() {
    int wrapSize;
    if (mDataList!=null&&mDataList.size()>1&&mRadarAxisData.getTypes().length>1){
    	//设置小数位数
        NumberFormat numberFormat =NumberFormat.getPercentInstance();
        numberFormat.setMinimumFractionDigits(mRadarAxisData.getDecimalPlaces());
        paintText.setStrokeWidth(mRadarAxisData.getPaintWidth());
        paintText.setTextSize(mRadarAxisData.getTextSize());
        //获取FontMetrics
        Paint.FontMetrics fontMetrics= paintText.getFontMetrics();
        float top = fontMetrics.top;//获取baseline之上高度
        float bottom = fontMetrics.bottom; //获取baseline之下高度
        float webWidth = (bottom-top)*(float) Math.ceil((mRadarAxisData.getMaximum()-mRadarAxisData.getMinimum())
                /mRadarAxisData.getInterval());//计算单个高度*数量
        float nameWidth = paintText.measureText(mRadarAxisData.getTypes()[0]);//计算正多边形各角字符的长度
        wrapSize = (int) (webWidth*2+nameWidth*1.1);
    }else {
        wrapSize = 0;
    }
    return wrapSize;
}
```
由代码可以看出通过计算出刻度值的高度与各角字符的高度来合成Size值。<br>
最后只要在**onMeasure**中使用size值，即可实现雷达图**wrap_content**效果。与getSuggestedMinimumWidth()获取的值相比较是为了防止，size过小而出现以外，虽然此情况出现的几率并不大。
```Java
@Override
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    setMeasuredDimension(
            Math.max(getSuggestedMinimumWidth(),
                    resolveSize(getCurrentWidth(),
                            widthMeasureSpec)),
            Math.max(getSuggestedMinimumHeight(),
                    resolveSize(getCurrentHeight(),
                            heightMeasureSpec)));
}
```

## 五、小结
本文详细的说明了雷达图(蜘蛛网图)的具体实现，同时介绍了View中的相关流程函数，以及**resolveSizeAndState**和**getDefaultSize**的大致内容，以选取更合适的方法来动态的适应**wrap_content**。并且通过使用PathMeasure类的**getPosTan**方法，更方便的获取雷达图各顶点方向的cos、sin值。<br>

如果在阅读过程中，有任何疑问与问题，欢迎与我联系。<br>
**博客:www.idtkm.com**<br>
**GitHub:https://github.com/Idtk**<br>
**微博:http://weibo.com/Idtk**<br>
**邮箱:IdtkMa@gmail.com**<br>
<br>

[雷达图源码](https://github.com/Idtk/SmallChart)
