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
#### a、创建画笔
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
#### b、绘制坐标轴
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
把原点从左上角移动到画布中心,绘制原点与四个端点
```Java
private Path mPath = new Path();

canvas.translate(mWidth/2,mHeight/2);// 将画布坐标原点移动到中心位置
//绘制坐标原点
mPaint.setColor(Color.BLACK);//设置画笔颜色
mPaint.setStrokeWidth(10);//为了看得清楚,设置了较大的画笔宽度
canvas.drawPoint(0,0,mPaint);
//绘制坐标轴4个断点
canvas.drawPoints(new float[]{
        mWidth/2*0.8f,0
        ,0,mHeight/2*0.8f
        ,-mWidth/2*0.8f,0
        ,0,-mHeight/2*0.8f},mPaint);
```
增加坐标轴与箭头的Path，在完成后使用**canvas.drawPath**一次进行绘制
```Java
mPaint.setStrokeWidth(1);//恢复画笔默认宽度
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

<img src="https://github.com/Idtk/Blog/blob/master/Image/%E5%9D%90%E6%A0%87%E7%B3%BB2.png" alt="坐标系" title="坐标系"width="300"/>
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

<img src="https://github.com/Idtk/Blog/blob/master/Image/addarc.png" alt="addArc" title="addArc" width="300"/>
<br>
再来看看arcTo
```Java
//arcTo
mPath.moveTo(0,0);
mPath.arcTo(mRectF,-60,180);
//绘制Path
canvas.drawPath(mPath,mPaint);
```

<img src="https://github.com/Idtk/Blog/blob/master/Image/arcto.png" alt="arcTo" title="arcTo" width="300"/>
<br>
可以看到arcTo多了**一条从原点到圆弧起点的直线**，而如果设置为mPath.arcTo(mRectF,-60,180,false);效果将和addArc相同。
<br>

## 三、圆角图片以及更多形状
继承**ImageView**,重写父类的**onSizeChanged**方法，获取View尺寸，之后根据View大小对图片进行压缩。
```Java
@Override
protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    mViewWidth = w;
    mViewHeight = h;
    size();//切割尺寸计算
    scaleBitmap();//压缩图片尺寸函数
}
```
在**onDraw**方法中进行样式绘制，在其中使用**clipPath**的方法来实现圆角图片。
```Java
@Override
protected void onDraw(Canvas canvas) {
    canvas.translate(mViewWidth/2,mViewHeight/2);//将画布坐标原点移动到中心位置
    canvas.clipPath(pathFigure(), Region.Op.INTERSECT);//切割
    mPath.reset();
    canvas.drawBitmap(b,rect,rect,mPaint);
}
```
在**scaleBitmap**方法中对图片的尺寸进行压缩
```Java
private void scaleBitmap(){
    Drawable drawable = getDrawable();//获取图片
    if (drawable == null) {
        return;
    }
    if (getWidth() == 0 || getHeight() == 0) {
        return;
    }
    if (!(drawable instanceof BitmapDrawable)) {
        return;
    }
    b = ((BitmapDrawable) drawable).getBitmap();//获取bitmap
    if (null == b) {
        return;
    }
    float scaleWidth = (float) length/b.getWidth();
    float scaleHeight = (float) length/b.getHeight();
    matrix.postScale(scaleWidth,scaleHeight);//缩放矩阵
    b=Bitmap.createBitmap(b,0,0,b.getWidth(),b.getHeight(),matrix,true);//压缩图片
}
```
在**size**方法中设置canvas的切割尺寸
```Java
protected void size(){
    length = Math.min(mViewWidth,mViewHeight)/2;
    rect = new Rect(-(int) length, -(int) length, (int) length, (int) length);//绘制图片矩阵
}
```
**现在就是发挥想象力的时候啦，来编写pathFigure()方法**<br>
#### a、先编写一个简单的圆形图片样式
```Java
protected Path pathFigure(){
    switch (modeFlag){
        case CIRCLE:
            mPath.addCircle(0,0,length, Path.Direction.CW);//增加圆的path
            break;
    }
    return mPath;
}
```
#### b、增加一个圆角图片样式
```Java

private RectF rectF = new RectF();

case ROUNDRECT:
            rectF.left = -length;
            rectF.top = -length;
            rectF.right = length;
            rectF.bottom = length;
            mPath.addRoundRect(rectF,radius,radius, Path.Direction.CW);//圆角矩形，radius为圆角的半径
            break;
```

<img src="https://github.com/Idtk/Blog/blob/master/Image/%E5%9C%86%E8%A7%921.png" alt="圆角" title="圆角" width="300"/>

#### c、再增加一个扇形样式<br>
(**PS:为了可以获得更多的图片面积，需要把圆心下移一个length的距离，半径扩大到之前的两倍**)
```Java
case SECTOR:
    rectF.left = -length*2;
    rectF.top = -length;
    rectF.right = length*2;
    rectF.bottom = length*3;
    mPath.moveTo(0,length);
    mPath.arcTo(rectF,angle,-angle*2-180);//绘制圆弧
    break;
```
<img src="https://github.com/Idtk/Blog/blob/master/Image/%E5%9C%86%E8%A7%922.png" alt="圆角" title="圆角" width="300"/>


## 四、逻辑运算
两条Path可通过多种逻辑运算进行结合，形成新的Path。<br>
API:
```Java
op(Path path, Path.Op op)
op(Path path1, Path path2, Path.Op op)
```
逻辑运算具有五种类型:

| 方法  | 描述 | 示意图 |
| ------------- |-------------|-------------|
| DIFFERENCE | B在A中的相对补集，即A减去A与B的交集 | <img src="https://github.com/Idtk/Blog/blob/master/Image/%E8%A1%A5%E9%9B%861.png" alt="DIFFERENCE" width="100"/> |
| REVERSE_DIFFERENCE | A在B中的相对补集合，即B减去B与A的交集 |<img src="https://github.com/Idtk/Blog/blob/master/Image/%E8%A1%A5%E9%9B%862.png" alt="REVERSE_DIFFERENCE" width="100"/> |
| INTERSECT | A与B的交集 |<img src="https://github.com/Idtk/Blog/blob/master/Image/%E4%BA%A4%E9%9B%86.png" alt="INTERSECT" width="100"/> |
| UNION | A与B的合集 |<img src="https://github.com/Idtk/Blog/blob/master/Image/%E5%90%88%E9%9B%86.png" alt="UNION" width="100"/> |
| XOR | A与B的合集减去A与B的交集 |<img src="https://github.com/Idtk/Blog/blob/master/Image/%E5%BC%82%E6%88%96.png" alt="XOR" width="100"/> |


使用Path.op方法再给圆角图片类，增加一种环形样式:
```Java
case RING:
    rectF.left = -length*2;
    rectF.top = -length;
    rectF.right = length*2;
    rectF.bottom = length*3;
    mPath1.moveTo(0,length);
    mPath1.arcTo(rectF,angle,-angle*2-180);//较大的圆弧
    
    rectF.left = -length/2;
    rectF.top = length/2;
    rectF.right = length/2;
    rectF.bottom = length*3/2;
    mPath2.moveTo(0,length);
    mPath2.arcTo(rectF,angle,-angle*2-180);//较小的圆弧
    
    mPath.op(mPath1,mPath2, Path.Op.XOR);//异或获取环形
```
<img src="https://github.com/Idtk/Blog/blob/master/Image/%E5%9C%86%E8%A7%923.png" alt="圆角" title="圆角" width="300"/>

## 五、小结
本文介绍了Path的基本使用方法与逻辑运算，同时通过圆角图片的例子，进行了实战。在下一章节终于可以进行[PieChart](https://github.com/Idtk/CustomView/blob/master/gif/PieChart.gif)的编写了，虽然只是一个简单的环形图，却是对之前[四篇文章](https://github.com/Idtk/CustomView)的综合有趣的运用。如果在阅读过程中，有任何疑问与问题，欢迎与我联系。<br>
**博客:www.idtkm.com**<br>
**GitHub:https://github.com/Idtk**<br>
**邮箱:IdtkMa@gmail.com**<br>
圆角图片[FigureImageView](https://github.com/Idtk/FigureImageView)源码，通过path方法，还可以增加更多有趣的图形，比如star，多边形，格子图等等。
