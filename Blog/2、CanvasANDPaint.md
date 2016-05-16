#自定义View——Canvas与Paint<br>

## 一、涉及知识<br>
**绘制过程**<br>

| 类别        | API           |描述  |
| ------------- |:-------------:|-----|
| 布局     | onMeasure  |  测量View与Child View的大小 |
|         | onLayout  |   确定Child View的位置|
|         | onSizeChanged  |   确定View的大小|
| 绘制     | onDraw  |   实际绘制View的内容|
| 事件处理     | onTouchEvent  |   处理屏幕触摸事件|
| 重绘     | invalidate  |   调用onDraw方法，重绘View中变化的部分|
<br>
**Canvas涉及方法**</br>

| 类别        | API           | 描述   |  
| ------------- |:-------------:| -----   |  
| 绘制图形      | drawPoint, drawPoints, drawLine, drawLines, drawRect, drawRoundRect, drawOval, drawCircle, drawArc | 依次为绘制点、直线、矩形、圆角矩形、椭圆、圆、扇形 |
| 绘制文本      | drawText, drawPosText, drawTextOnPath |    依次为绘制文字、指定每个字符位置绘制文字、根据路径绘制文字|
| 画布变换      | translate, scale, rotate, skew |   依次为平移、缩放、旋转、倾斜（错切） |
| 画布裁剪      | clipPath, clipRect, clipRegion |   依次为按路径、按矩形、按区域对画布进行裁剪 |
</br>
**Paint涉及方法**</br>

| 类别        | API           | 描述  |
| ------------- |:-------------:| -----   | 
| 颜色      | setColor,setARGB，setAlpha，setColorFilter | 依次为设置画笔颜色、透明度，色彩过滤器 |
| 类型      | setStyle |   填充(FILL),描边(STROKE),填充加描边(FILL_AND_STROKE) |
| 抗锯齿      | setAntiAlias |   画笔是否抗锯齿 |
| 字体大小      | setTextSize |   设置字体大小 |
| 字体测量      | getFontMetrics()，getFontMetricsInt()，measureText |   返回字体的行间距，返回值依次为float、int |
| 文字宽度测量      | measureText |   返回文字的宽度 |
| 文字对齐方式      | setTextAlign |   左对齐(LEFT),居中对齐(CENTER),右对齐(RIGHT) |
| 宽度      | setStrokeWidth |   设置画笔宽度 |
| 笔锋      | setStrokeCap |   默认(BUTT),半圆形(ROUND),方形(SQUARE) |
<br>

（**PS: 因API较多，只列出了涉及的方法，想了解更多，请查看[官方文档](http://developer.android.com/reference/packages.html)**)<br>

## 二、Canvas
**(PS: 以下的代码中未指定函数名的都是在onDraw函数中进行使用,同时为了演示方便，在onDraw中使用了一些new方法，请在实际使用中不要这样做，因为onDraw函数是经常需要重新运行的)**
### 1、创建画笔
创建画笔并初始化
```Java
//创建画笔
private Paint mPaint = new Paint();

private void initPaint(){
    //初始化画笔
	mPaint.setStyle(Paint.Style.FILL);//设置画笔类型
    mPaint.setAntiAlias(true);//抗锯齿
}
```

### 2、绘制坐标轴
使用onSizeChanged方法，获取根据父布局等因素确认的View宽高
```Java
//宽高
private int mWidth;
private int mHeight;

@Override
protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    mWidth = w;
    mHeight = h;
}
```
把原点从左上角移动到画布中心
```Java
@Override
protected void onDraw(Canvas canvas) {
	super.onDraw(canvas);
	canvas.translate(mWidth/2,mHeight/2);// 将画布坐标原点移动到中心位置
}
```
绘制坐标原点，使用**drawPoint**
```Java
//绘制坐标原点
mPaint.setColor(Color.BLACK);//设置画笔颜色
mPaint.setStrokeWidth(10);//为了看得清楚,设置了较大的画笔宽度
canvas.drawPoint(0,0,mPaint);
```
图

绘制坐标系的4个端点，一次绘制多个点，这次使用**drawPoints**
```Java
//绘制坐标轴4个断点
canvas.drawPoints(new float[]{
	mWidth/2*0.8f,0
    ,0,mHeight/2*0.8f
    ,-mWidth/2*0.8f,0
    ,0,-mHeight/2*0.8f},mPaint);
```
图

绘制坐标轴，使用**drawLine**
```Java
mPaint.setStrokeWidth(1);//恢复画笔默认宽度
//绘制X轴
canvas.drawLine(-mWidth/2*0.8f,0,mWidth/2*0.8f,0,mPaint);
//绘制Y轴
canvas.drawLine(0,mHeight/2*0.8f,0,mHeight/2*0.8f,mPaint);
```
图

绘制坐标轴箭头，一次绘制多条线，这次使用**drawLines**
```Java
mPaint.setStrokeWidth(3);
//绘制X轴箭头
canvas.drawLines(new float[]{
	mWidth/2*0.8f,0,mWidth/2*0.8f*0.95f,-mWidth/2*0.8f*0.05f,            mWidth/2*0.8f,0,mWidth/2*0.8f*0.95f,mWidth/2*0.8f*0.05f
},mPaint);
//绘制Y轴箭头
canvas.drawLines(new float[]{
      0,mHeight/2*0.8f,mWidth/2*0.8f*0.05f,mHeight/2*0.8f-mWidth/2*0.8f*0.05f,
      0,mHeight/2*0.8f,-mWidth/2*0.8f*0.05f,mHeight/2*0.8f-mWidth/2*0.8f*0.05f,
},mPaint);
```
图

为什么Y轴的箭头是向下的呢？这是因为原坐标系原点在左上角，向下为Y轴正方向，有疑问的可以查看我之前的文章[自定义View——Android坐标系与View绘制流程](http://www.idtkm.com/customview/piechart1/)
<br>
如果觉得不舒服，一定要箭头向上的话，可以在绘制Y轴箭头之前翻转坐标系
```Java
canvas.scale(1,-1);//翻转Y轴
```
### 3、绘制图形
#### 矩形以及画布缩放，旋转，错切
绘制一个矩形
```Java
//绘制矩形
mPaint.setStyle(Paint.Style.STROKE);//设置画笔类型
canvas.drawRect(-mWidth/8,-mHeight/8,mWidth/8,mHeight/8,mPaint);
```
图

缩放，同时使用**new Rect**方法设置矩形
```Java
canvas.scale(0.5f,0.5f);
mPaint.setColor(Color.BLUE);
canvas.drawRect(new RectF(-mWidth/8,-mHeight/8,mWidth/8,mHeight/8),mPaint);
```
图

旋转
```Java
canvas.rotate(90);
mPaint.setColor(Color.BLUE);
canvas.drawRect(new RectF(-mWidth/8,-mHeight/8,mWidth/8,mHeight/8),mPaint);
```

错切
```Java
canvas.skew(1,0.5f);
mPaint.setColor(Color.BLUE);
canvas.drawRect(new RectF(-mWidth/8,-mHeight/8,mWidth/8,mHeight/8),mPaint);
```
图

#### 豆瓣的加载时候的笑脸表情
```Java
mPaint.setColor(Color.GREEN);
mPaint.setStrokeWidth(10);
float point = Math.min(mWidth,mHeight)*0.2f/2;
float r = point*(float) Math.sqrt(2);
RectF rectF = new RectF(-r,-r,r,r);
canvas.drawArc(rectF,0,180,false,mPaint);
canvas.drawPoints(new float[]{
        point,-point
        ,-point,-point
},mPaint);
```

```Java
mPaint.setColor(Color.GREEN);
mPaint.setStrokeWidth(10);
float point = Math.min(mWidth,mHeight)*0.2f/2;
float r = point*(float) Math.sqrt(2);
RectF rectF = new RectF(-r,-r,r,r);
canvas.drawArc(rectF,-45,270,false,mPaint);
```
