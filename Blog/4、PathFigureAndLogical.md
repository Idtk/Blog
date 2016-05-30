#自定义View——Path图形与逻辑运算<br>

## 涉及知识<br>
**Path**<br>

| 类型  | API | 描述 |
| ------------- |:-------------:| ------------- |
| 添加路径 | addArc, addCircle, addOval, addPath, addRect, addRoundRect, arcTo | 依次为添加圆弧、圆、椭圆、路径、矩形、圆角矩形、圆弧|
| 移动起点 | moveTo | 移动起点位置，仅对之后路径产生影响 |
| 移动终点 | setLastPoint | 移动终点位置，对前后的路径都会产生影响|
| 直线 | lineTo | 增加一条道指定点的直线 |
| 贝塞尔 | quadTo, cubicTo | 二阶、三阶贝塞尔曲线 |
| 闭合路径 | close | 路径终点连接到起点|
| 逻辑运算 | op | A\B(DIFFERENCE), A∩B(INTERSECT), B\A(REVERSE_DIFFERENCE), A∪B(UNION), A⊕B(XOR)|
| 替换路径 | set | 用新的路径替换当前路径 |
| 重置 | reset, rewind| 清除path使它为空，清除path但保留内部的数据结构 |
| 计算边界 | computeBounds| 计算路径的矩形边界 |
<br>

**本来这章应该是PieChart的实战，可是我在编写的时候发现了一个设置背景图片的bug。作为一个强迫症(ಥ _ ಥ)，我只好引入了Path来解决这个bug，所以就有了这一篇内容。**


## 一、什么是Path

看看官方描述:<br>
Path class 封装了由直线、二次、三次贝塞尔曲线构成的多重曲线几何路径。它可以用canvas.drawPath(path,paint)方法绘图，填充和线都可以（根据paint的样式），或者它可以用于在绘图路径上裁剪或者绘出文本。
<br>

我的理解:
Path由任意多条直线、二次贝塞尔或三次贝塞尔曲线组成,可以选择填充或者描边模式，可以使用它裁剪画布或者绘制文字。

## 二、添加路径
### 1、lineTo,moveTo
在之前的文章中，使用canvas的函数绘制过[坐标系](http://www.idtkm.com/customview/customview2/)，这次使用path来绘制。<br>

绘制坐标轴
```Java
private Path mPath = new Path();

canvas.translate(mWidth/2,mHeight/2);// 将画布坐标原点移动到中心位置
mPaint.setStyle(Paint.Style.STROKE);
mPaint.setColor(Color.BLACK);
mPaint.setStrokeWidth(10);//设置画笔宽度
//绘制原点
canvas.drawPoint(0,0,mPaint);
//绘制坐标轴4个断点
canvas.drawPoints(new float[]{
        mWidth/2*0.8f,0
        ,0,mHeight/2*0.8f
        ,-mWidth/2*0.8f,0
        ,0,-mHeight/2*0.8f},mPaint);
mPaint.setStrokeWidth(1);//恢复至默认画笔宽度
//x轴
mPath.moveTo(-mWidth/2*0.8f,0);//移动path起点到(-mWidth/2*0.8f,0)
mPath.lineTo(mWidth/2*0.8f,0);//直线终点为(mWidth/2*0.8f,0)
//y轴
mPath.moveTo(0,-mHeight/2*0.8f);//移动path起点到(0,-mHeight/2*0.8f)
mPath.lineTo(0,mHeight/2*0.8f);//直线终点为(0,mHeight/2*0.8f)
//x箭头
mPath.moveTo(mWidth/2*0.8f*0.95f,-mWidth/2*0.8f*0.05f);
mPath.lineTo(mWidth/2*0.8f,0);
mPath.lineTo(mWidth/2*0.8f*0.95f,mWidth/2*0.8f*0.05f);
//y箭头
mPath.moveTo(mWidth/2*0.8f*0.05f,mHeight/2*0.8f-mWidth/2*0.8f*0.05f);
mPath.lineTo(0,mHeight/2*0.8f);
mPath.lineTo(-mWidth/2*0.8f*0.05f,mHeight/2*0.8f-mWidth/2*0.8f*0.05f);
//绘制Path
canvas.drawPath(mPath,mPaint);
```

图
<br>
可以看出moveTo方法，可以移动下一次增加path的起点，而lineTo中的参数，即为直线的终点。

### 2、addArc与arcTo

| 方法  | 区别 |
| ------------- |-------------|
| addArc | 画一段圆弧 |
| arcTo | 画一段圆弧，当上一次的终点与圆弧起点未连接时，可以设置是否连接这两点 |

addArc
```Java
r = Math.min(mWidth,mHeight)*0.6f/2;
mRectF.left = 0;
mRectF.top = -r;
mRectF.right = r;
mRectF.bottom = 0;
mPath.addArc(mRectF,-60,180);
//绘制Path
canvas.drawPath(mPath,mPaint);
```
图
<br>
再来看看arcTo
```Java
//arcTo
mPath.moveTo(0,0);
mPath.arcTo(mRectF,-60,180);
//绘制Path
canvas.drawPath(mPath,mPaint);
```
图
<br>
可以看到arcTo多了一条从原点到圆弧起点的直线，而如果设置为mPath.arcTo(mRectF,-60,180,false);效果将和addArc相同。
<br>

## 三、圆角图片以及更多形状
继承ImageView,重写父类的onSizeChanged和onDraw方法，在其中使用clipPath的方法来实现圆角图片。
```Java
@Override
protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    mViewWidth = w;
    mViewHeight = h;
    size();
    scaleBitmap();
}
@Override
protected void onDraw(Canvas canvas) {
    canvas.translate(mViewWidth/2,mViewHeight/2);
    canvas.clipPath(pathFigure(), Region.Op.INTERSECT);
    mPath.reset();
    canvas.drawBitmap(b,rect,rect,mPaint);
}
```
在scaleBitmap中对图片的尺寸进行压缩
```Java
private void scaleBitmap(){
    Drawable drawable = getDrawable();
    if (drawable == null) {
        return;
    }
    if (getWidth() == 0 || getHeight() == 0) {
        return;
    }
    if (!(drawable instanceof BitmapDrawable)) {
        return;
    }
    b = ((BitmapDrawable) drawable).getBitmap();
    if (null == b) {
        return;
    }
    float scaleWidth = (float) length/b.getWidth();
    float scaleHeight = (float) length/b.getHeight();
    matrix.postScale(scaleWidth,scaleHeight);
    b=Bitmap.createBitmap(b,0,0,b.getWidth(),b.getHeight(),matrix,true);
}
```
在size中对canvas的切割尺寸进行设置
```Java
protected void size(){
    length = Math.min(mViewWidth,mViewHeight)/2;
    rect = new Rect(-(int) length, -(int) length, (int) length, (int) length);
    rectF = new RectF(-length, -length, length, length);
}
```
现在就是发挥想象力的时候啦，来编写pathFigure()方法,先来写基本的圆角和圆形图片。
```Java
protected Path pathFigure(){
    switch (modeFlag){
        case CIRCLE:
            mPath.addCircle(0,0,length, Path.Direction.CW);
            break;
        case ROUNDRECT:
            rectF.left = -length;
            rectF.top = -length;
            rectF.right = length;
            rectF.bottom = length;
            mPath.addRoundRect(rectF,radius,radius, Path.Direction.CW);
            break;
    }
    return mPath;
}
```
图

然后在写一个扇形，这时候为了可以获得更多的图片面积，需要把圆心下移一个length的距离，半径扩大到之前的两倍
```Java
case SECTOR:
    rectF.left = -length*2;
    rectF.top = -length;
    rectF.right = length*2;
    rectF.bottom = length*3;
    mPath.moveTo(0,length);
    mPath.arcTo(rectF,angle,-angle*2-180);
    break;
```
图


## 四、逻辑运算

API:
```Java
op(Path path, Path.Op op)
op(Path path1, Path path2, Path.Op op)
```
逻辑运算具有五种类型:

| 方法  | 描述 | 示意图 |
| ------------- |-------------|-------------|
| DIFFERENCE | B在A中的相对补集，即A减去A与B的交集 | <img src="https://zh.wikipedia.org/wiki/%E9%80%BB%E8%BE%91%E8%BF%90%E7%AE%97%E7%AC%A6#/media/File:Venn0100.svg" alt="DIFFERENCE" width="300"/> |
| REVERSE_DIFFERENCE | A在B中的相对补集合，即B减去B与A的交集 |<img src="https://zh.wikipedia.org/wiki/%E9%80%BB%E8%BE%91%E8%BF%90%E7%AE%97%E7%AC%A6#/media/File:Venn0010.svg" alt="REVERSE_DIFFERENCE" width="300"/> |
| INTERSECT | A与B的交集 |<img src="https://zh.wikipedia.org/wiki/%E9%80%BB%E8%BE%91%E8%BF%90%E7%AE%97%E7%AC%A6#/media/File:Venn0001.svg" alt="INTERSECT" width="300"/> |
| UNION | A与B的合集 |<img src="https://zh.wikipedia.org/wiki/%E9%80%BB%E8%BE%91%E8%BF%90%E7%AE%97%E7%AC%A6#/media/File:Venn0111.svg" alt="UNION" width="300"/> |
| XOR | A与B的合集减去A与B的交集 |<img src="https://zh.wikipedia.org/wiki/%E9%80%BB%E8%BE%91%E8%BF%90%E7%AE%97%E7%AC%A6#/media/File:Venn0110.svg" alt="XOR" width="300"/> |

这里使用Path.op方法再给圆角图片类，增加一种样式:
```Java
case RING:
    rectF.left = -length*2;
    rectF.top = -length;
    rectF.right = length*2;
    rectF.bottom = length*3;
    mPath1.moveTo(0,length);
    mPath1.arcTo(rectF,angle,-angle*2-180);
    rectF.left = -length/2;
    rectF.top = length/2;
    rectF.right = length/2;
    rectF.bottom = length*3/2;
    mPath2.moveTo(0,length);
    mPath2.arcTo(rectF,angle,-angle*2-180);
    mPath.op(mPath1,mPath2, Path.Op.XOR);
```
图

## 五、小结
本文介绍了Path的基本使用方法与逻辑运算，同时通过圆角图片的例子，进行了实战。在下一章节终于可以进行[PieChart](https://github.com/Idtk/CustomView/blob/master/gif/PieChart.gif)的编写了，虽然只是一个简单的环形图，却是对之前[四篇文章](https://github.com/Idtk/CustomView)的综合有趣的运用。如果在阅读过程中，有任何疑问与问题，欢迎与我联系。<br>
**博客:www.idtkm.com**<br>
**GitHub:https://github.com/Idtk**<br>
**邮箱:IdtkMa@gmail.com**<br>
圆角图片[FigureImageView](https://github.com/Idtk/FigureImageView)源码，通过path方法，还可以增加更多有趣的图形，比如star，比如多边形等等。


