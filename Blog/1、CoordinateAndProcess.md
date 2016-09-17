#Android坐标系与View绘制流程

[自定义View系列目录](https://github.com/Idtk/Blog)

## 涉及知识<br>
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
| 颜色      | setColor, setARGB, setAlpha | 依次为设置画笔颜色、透明度 |
| 类型      | setStyle |   填充(FILL),描边(STROKE),填充加描边(FILL_AND_STROKE) |
| 抗锯齿      | setAntiAlias |   画笔是否抗锯齿 |
| 字体大小      | setTextSize |   设置字体大小 |
| 字体测量      | getFontMetrics()，getFontMetricsInt() |   返回字体的测量，返回值依次为float、int |
| 文字宽度      | measureText |   返回文字的宽度 |
| 文字对齐方式      | setTextAlign |   左对齐(LEFT),居中对齐(CENTER),右对齐(RIGHT) |
| 宽度      | setStrokeWidth |   设置画笔宽度 |
| 笔锋      | setStrokeCap |   默认(BUTT),半圆形(ROUND),方形(SQUARE) |
<br>

（**Ps:因API较多，只列出了涉及的方法，想了解更多，请查看[官方文档](http://developer.android.com/reference/packages.html)**)<br>
## 一、坐标系
### 1、屏幕坐标系
&nbsp;&nbsp;屏幕坐标系以手机屏幕的左上角为坐标原点，过的原点水平直线为X轴，向右为正方向；过原点的垂线为Y轴，向下为正方向。<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/%E5%B1%8F%E5%B9%95.png" alt="屏幕坐标系" title="屏幕坐标系"width="300"/>
<br>
### 2、View坐标系
&nbsp;&nbsp;View坐标系以父视图的左上角为坐标原点，过的原点水平直线为X轴，向右为正方向；过原点的垂线为Y轴，向下为正方向。<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/%E8%A7%86%E5%9B%BE.png" alt="View坐标系" title="View坐标系"width="300"/>

View内部拥有四个函数,用于获取View的位置
```Java
getTop();     //View的顶边到其Parent View的顶边的距离，即View的顶边与View坐标系的X轴之间的距离
getLeft();    //View的左边到其Parent View的左边的距离，即View的左边与View坐标系的Y轴之间的距离
getBottom();  //View的底边到其Parent View的顶边的距离，即View的底边与View坐标系的X轴之间的距离
getRight();   //View的右边到其Parent View的左边的距离，即View的右边与View坐标系的Y轴之间的距离
```
图示如下:<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/getTop.png" alt="View坐标系" title="View坐标系"width="300"/>

## 二、绘制过程<br>
### 1、构造函数
&nbsp;&nbsp;构造函数用于读取一些参数、属性对View进行初始化操作<br>
&nbsp;&nbsp;View的构造函数有四种重载方法，分别如下:<br>
```Java
public BaseChart(Context context) {}
public BaseChart(Context context, AttributeSet attrs) {}
public BaseChart(Context context, AttributeSet attrs, int defStyleAttr) {}
public BaseChart(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {}
```
**context:**上下文，新建时传入,如:
```Java
BaseChart baseChart = new BaseChart(this);
```
**AttributeSet:**是节点的属性集合,如:
```xml
<com.customview.BaseChart
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:attr1="attr1 from xml"
    app:attr2="attr2 from xml"/>
```
即com.customview.PieChart节点中的属性集合<br>
<br>
**defStyleAttr:**默认风格，是指它在当前Application或Activity所用的Theme中的默认Style,如:<br>
在attrs.xml中添加
```xml
<attr name="base_chart_style" format="reference" />
```
引用的是styles.xml文件中
```xml
<style name="base_chart_style">
	<item name="attr2">@string/attr2</item>
    <item name="attr3">@string/attr3</item>
</style>
```
在当前默认主题中添加这个style
```xml
<style name="AppTheme"parent="Theme.AppCompat.Light.DarkActionBar">
	...
	<item name="base_chart_style">@stylebase_chart_style</item>
	...
</style>
```
<br>
**defStyleRes:**默认风格，*只有当defStyleAttr无效时，才会使用这个值*,如：<br>
在style.xml中添加
```xml
<style name="base_chart_res">
	<item name="attr4">attr4 from base_chart_res</item>
    <item name="attr5">attr5 from base_chart_res</item>
</style>
```
<br>
======
### 一个实例——BaseChart<br>
新建BaseChart类机成自view
```Java
public class BaseChart extends View {

    private String TAG = "BaseChart";
    public BaseChart(Context context) {
        this(context,null);
    }

    public BaseChart(Context context, AttributeSet attrs) {
        this(context, attrs,R.attr.base_chart_style);
    }

    public BaseChart(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr,R.style.base_chart_res);
    }

    public BaseChart(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.base_chart, defStyleAttr,defStyleRes);
        int n = array.getIndexCount();
        for (int i=0; i<n; i++){
            int attr = array.getIndex(i);
            switch (attr){
                case R.styleable.base_chart_attr1:
                    Log.d(TAG,"attr1 =>" + array.getString(attr));
                    break;
                case R.styleable.base_chart_attr2:
                    Log.d(TAG,"attr2 =>" + array.getString(attr));
                    break;
                case R.styleable.base_chart_attr3:
                    Log.d(TAG,"attr3 =>" + array.getString(attr));
                    break;
                case R.styleable.base_chart_attr4:
                    Log.d(TAG,"attr4 =>" + array.getString(attr));
                    break;
                case R.styleable.base_chart_attr5:
                    Log.d(TAG,"attr5 =>" + array.getString(attr));
                    break;
            }
        }
    }
```
obtainStyledAttributes(AttributeSet set, int[] attrs, int defStyleAttr, int defStyleRes)新增加的attrs属性说明如下:<br>
**attrs:**默认属性，告诉系统需要获取那些属性的值，有多种Value类型，这里使用string类型，如：<br>
在attrs.xml中添加
```xml
<declare-styleable name="base_chart">
    <attr name="attr1" format="string" />
    <attr name="attr2" format="string"/>
    <attr name="attr3" format="string"/>
    <attr name="attr4" format="string"/>
    <attr name="attr5" format="string"/>
</declare-styleable>
```
使用上面提到的变量属性和布局文件<br>

#### a、defStyleAttr与defStyleRes参数先设置为0<br>
运行后显示如下:
```
BaseChart: attr1 =>attr1 from xml
BaseChart: attr2 =>attr2 from xml
BaseChart: attr3 =>null
BaseChart: attr4 =>null
BaseChart: attr5 =>null
```
attr1与attr2输出均来自布局文件的设置<br>

======

#### b、修改BaseView.java设置，引入defStyleAttr:
```Java
TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.base_chart, defStyleAttr,0);
```
相当于在布局文件中设置:
```xml
app:theme="@style/base_chart_style"
```
运行后显示如下:
```
BaseChart: attr1 =>attr1 from xml
BaseChart: attr2 =>attr2 from xml
BaseChart: attr3 =>attr3 from BaseChartStyle
BaseChart: attr4 =>null
BaseChart: attr5 =>null
```
attr1:仅在布局文件中设置，所以输出为 *attr1 from xml*<br>
attr2:在布局文件与默认主题的base_chart_style都进行了设置，布局文件中的设置优先级更高，所以输出为 *attr2 from xml*<br>
attr3:仅在默认主题base_chart_style中进行了设置，所以输出为 *attr3 from BaseChartStyle*<br>

======

#### c、在布局文件中增加自定义的style
```xml
<com.customview.BaseChart
	android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:attr1="attr1 from xml"
    app:attr2="attr2 from xml"
    style="@style/xml_style"/>
```
运行后结果如下:
```
BaseChart: attr1 =>attr1 from xml
BaseChart: attr2 =>attr2 from xml
BaseChart: attr3 =>attr3 from xml_style
BaseChart: attr4 =>attr4 from xml_style
BaseChart: attr5 =>null
```
attr1:仅在布局文件中设置，所以输出为 *attr1 from xml*<br>
attr2:在布局文件与默认主题的base_chart_style都进行了设置，布局文件中的设置优先级更高，所以输出为 *attr2 from xml*<br>
attr3:在默认主题base_chart_style与自定义主题的xml_style都进行了设置，自定义主题优先级更高，所以输出为 *attr3 from xml_style*<br>
attr4:仅在自定义主题xml_style中进行了设置，所以输出为 *attr4 from xml_style*<br>

======

#### d、修改BaseView.java设置，引入defStyleRes，修改defStyleAttr为0，否则引入的R.style.base_chart_res不会生效:
```Java
TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.base_chart, 0 ,R.style.base_chart_res);
```
运行后输入结果如下:
```
BaseChart: attr1 =>attr1 from xml
BaseChart: attr2 =>attr2 from xml
BaseChart: attr3 =>attr3 from xml_style
BaseChart: attr4 =>attr4 from xml_style
BaseChart: attr5 =>attr5 =>attr5 from base_chart_res
```
attr1:仅在布局文件中设置，所以输出为 *attr1 from xml*<br>
attr2:仅在布局文件中进行了设置，所以输出为 *attr2 from xml*<br>
attr3:仅在自定义主题xml_style中进行了设置，所以输出为 *attr3 from xml_style*<br>
attr4:在自定义主题xml_style和defStyleRes中都进行了设置，自定义主题优先级更高，所以输出为 *attr4 from xml_style*<br>
attr5:仅在defStyleRes中进行了设置,所以输出为 *attr5 from base_chart_res*<br>

======

### 2、onMeasure
View会在此函数中完成自己的Measure以及递归的遍历完成Child View的Measure，某些情况下需要多次Measure才能确定View的大小。<br>
可以从onMeasure中取出宽高及其他属性:
```Java
@Override
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    //Width
    int widthMode = MeasureSpec.getMode(widthMeasureSpec);//宽度值
    int widthSize = MeasureSpec.getSize(widthMeasureSpec);//宽度测量模式
    //Height
    int heightMode = MeasureSpec.getMode(heightMeasureSpec);//高度值
    int heightSize = MeasureSpec.getSize(heightMeasureSpec);//高度测量模式
}
```
由此可见**widthMeasureSpec, heightMeasureSpec**并不仅仅是宽高的值，还对应了宽高的测量模式。<br>
MeasureSpec是View内部的一个静态类，下面给出它的部分源码:
```Java
public static class MeasureSpec {
	private static final int MODE_SHIFT = 30;
    private static final int MODE_MASK  = 0x3 << MODE_SHIFT;
    public static final int UNSPECIFIED = 0 << MODE_SHIFT;
    public static final int EXACTLY     = 1 << MODE_SHIFT;
    public static final int AT_MOST     = 2 << MODE_SHIFT;
    public static int makeMeasureSpec(int size, int mode) {
		if (sUseBrokenMakeMeasureSpec) {
			return size + mode;
        } else {
            return (size & ~MODE_MASK) | (mode & MODE_MASK);
				}
        }
	}

	public static int getMode(int measureSpec) {
    	return (measureSpec & MODE_MASK);
    }

    public static int getSize(int measureSpec) {
        return (measureSpec & ~MODE_MASK);
    }
	
	...
}
```
可以看出**MeasureSpec代表一个32的int值，高2位代表测量模式SpecMode，低30位代表测量值SpecSize**。拥有3种测量模式，分别为**UNSPECIFIED、EXACTLY、AT_MOST**。<br>

| 测量类型        | 对应数值           |描述  |
| ------------- |:-------------:|-----|
| UNSPECIFIED | 0  |   父容器不对 view 有任何限制，要多大给多大 |
| EXACTLY     | 1  |   父容器已经检测出 view 所需要的大小,比如固定大小xxdp |
| AT_MOST     | 2  |   父容器指定了一个大小， view 的大小不能大于这个值 |

======
### 3、onLayout
用于确定View以及其子View的布局位置，在ViewGroup中，当位置被确定后，它在onLayout中会遍历所有的child并调用其layout，然后layout内部会再调用child的onLayout确定child View的布局位置。<br>
layout方法如下:
```Java
public void layout(int l, int t, int r, int b) {
	...
    int oldL = mLeft;
    int oldT = mTop;
    int oldB = mBottom;
    int oldR = mRight;
	...
	
}
```
**mLeft, mTop, mBottom, mRight**四个参数分别通过**getLeft(),getTop(),getRight(),getBottom()**四个函数获得。这一组old值会在位置改变时，调用onLayoutChange时使用到。<br>

======
### 4、onSizeChanged
如其名，在View大小改变时调用此函数，用于确定View的大小。至于View大小为什么会改变，因为View的大小不仅由本身确定，同时还受父View的影响。
```Java
@Override
protected void onSizeChanged(int w, int h, int oldw, int oldh) {
	super.onSizeChanged(w, h, oldw, oldh);
}
```
这里的**w、h**就是确定后的宽高值，如果查看View中的onLayoutChange也会看到类似的情况，拥有l, t, r, b, oldL, oldT, oldR, oldB，新旧两组参数。

======
### 5、onDraw
onDraw是View的绘制部分，给了我们一张空白的画布，使用Canvas进行绘制。也是后面几篇文章所要分享的内容。
```Java
@Override
protected void onDraw(Canvas canvas) {
	super.onDraw(canvas);
}
```

======
### 6、其他方法以及监听回调
如onTouchEvent、invalidate、setOnTouchListener等方法。<br>
**onTouchEvent**用于处理传递到的View手势事件。
```Java
@Override
public boolean onTouchEvent(MotionEvent event) {
	return super.onTouchEvent(event);
}
```
当返回**true**时，说明该View消耗了触摸事件，后续的触摸事件也由它来进行处理。返回**false**时，说明该View对触摸事件不感兴趣，事件继续传递下去。<br>
触屏事件类型被封装在MotionEvent中，MotionEvent提供了很多类型的事件，主要关心如下几种类型:<br>

| 事件类型        | 描述  |
| ------------- |-----|
| ACTION_DOWN    | 手指按下  |
| ACTION_MOVE    | 手指移动  |
| ACTION_UP      | 手指抬起  |
<br>
**事件效果如下:**<br>
<img src="http://upload-images.jianshu.io/upload_images/623378-34fd10214730ea5f.gif?imageMogr2/auto-orient/strip" alt="屏幕触摸事件" title="屏幕触摸事件" width="300"/><br>
在MotionEvent中有两组可以获得触摸位置的函数
```Java
event.getX();      //触摸点相对于View坐标系的X坐标
event.getY();	   //触摸点相对于View坐标系的Y坐标
event.getRawX();   //触摸点相对于屏幕坐标系的X坐标
event.getRawY();   //触摸点相对于屏幕坐标系的Y坐标
```
图示如下:<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/getRawX.png" alt="View坐标系" title="View坐标系"width="300"/><br>

**onWindowFocusChanged**运行于onMeasure与onLayout之后，可以获取到正确的width、height、top、left等属性值。<br>
## 三、小结
&nbsp;&nbsp;简单分析了自定义View的入门准备知识，包括屏幕坐标系、View坐标、View的绘制过程中的主要函数、以及屏幕触摸事件。后面的内容将会围绕onDraw函数展开，在完成涉及知识点的分析之后，将会实战去编写PieView的代码。如果在阅读过程中，有任何疑问与问题，欢迎与我联系。<br>
**博客:www.idtkm.com**<br>
**GitHub:https://github.com/Idtk**<br>
**微博:http://weibo.com/Idtk**<br>
**邮箱:IdtkMa@gmail.com**<br>

**PieChart效果图如下：**<br>
<img src="https://github.com/Idtk/CustomView/blob/master/gif/PieChart.gif" alt="PieChart" title="PieChart"/><br>
