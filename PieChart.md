#自定义环形图——基础知识<br>
**效果图如下：**<br>
<img src="https://github.com/Idtk/CustomView/blob/master/gif/CustomView.gif" alt="GitHub" title="GitHub,Social Coding"/><br>
## 一、涉及知识<br>
**绘制过程**<br>

| 类别        | API           |描述  |
| ------------- |:-------------:|-----|
| 布局     | onMeasure  |  确定View与Child View的大小 |
|         | onLayout  |   确定Child View的位置|
|         | onSizeChanged  |   确定View的大小|
| 绘制     | onDraw  |   实际绘制View的内容|
| 事件处理     | onTouchEvent  |   处理屏幕触摸事件|
| 重绘     | invalidate  |   调用ondraw方法，重绘view中变化的部分|
<br>
**坐标、弧度、颜色**<br>

| 类别        | API           | 描述  |
| ------------- |:-------------:| -----|
| View坐标      | getLeft,getTop,getRight,getBottom   | 依次为，View左上角顶点相对于父布局的左侧和顶部距离，右下角顶点相对于父布局的左侧和顶部距离 |
| MotionEvent坐标      | getX,getY,getRawX,getRawY |   getX,getY相对于当前view的位置坐标，getRawX,getRawY相对于屏幕的位置坐标 |
| 弧度、角度      | toRadians,toDegrees |   toRadians角度转换为近似相等的弧度，toDegrees弧度转换为近似相等的角度| 
| 颜色      | Color.argb(透明度，红，绿，蓝)) |   颜色从透明到不透明，或从浅到深，都用0x00到0xff表示|
<br>
**Canvas涉及方法**</br>

| 类别        | API           | 描述   |  
| ------------- |:-------------:| -----   |  
| 绘制图形      | drawPoint, drawPoints, drawLine, drawLines, drawRect, drawRoundRect, drawOval, drawCircle, drawArc | 依次为绘制点、直线、矩形、圆角矩形、椭圆、圆、扇形 |
| 绘制文本      | drawText, drawPosText, drawTextOnPath |    依次为绘制文字、指定每个字符位置绘制文字、根据路径绘制文字|
| Canvas变换      | translate, scale, rotate, skew |   依次为平移、缩放、旋转、倾斜（错切） |
</br>
**Paint涉及方法**</br>

| 类别        | API           | 备注  |
| ------------- |:-------------:| -----   | 
| 颜色      | setColor,setARGB，setAlpha，setColorFilter | 依次为设置画笔颜色、透明度，色彩过滤器 |
| 类型      | setStyle |   填充(FILL),描边(STROKE),填充加描边(FILL_AND_STROKE) |
| 抗锯齿      | setAntiAlias |   画笔是否抗锯齿 |
| 字体大小      | setTextSize |   设置字体大小 |
| 字体测量      | getFontMetrics()，getFontMetricsInt()，measureText |   返回字体的行间距，返回值一次为float、int |
| 文字宽度测量      | measureText |   返回文字的宽度 |
| 文字对齐方式      | setTextAlign |   左对齐(LEFT),居中对齐(CENTER),右对齐(RIGHT) |
| 宽度      | setStrokeWidth |   设置画笔宽度 |
| 笔锋      | setStrokeCap |   默认(BUTT),半圆形(ROUND),方形(SQUARE) |
<br>
（**Ps:因API较多，只列出了涉及的方法，想了解更多，请查看[官方文档](http://developer.android.com/reference/packages.html)**)<br>
## 一、绘制过程<br>
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
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.base_chart, 0,0);
        Log.d(TAG,"attr1 =>" + array.getString(R.styleable.base_chart_attr1));
        Log.d(TAG,"attr2 =>" + array.getString(R.styleable.base_chart_attr2));
        Log.d(TAG,"attr3 =>" + array.getString(R.styleable.base_chart_attr3));
        Log.d(TAG,"attr4 =>" + array.getString(R.styleable.base_chart_attr4));
        Log.d(TAG,"attr5 =>" + array.getString(R.styleable.base_chart_attr5));
    }
```
obtainStyledAttributes(AttributeSet set, int[] attrs, int defStyleAttr, int defStyleRes)新增加的attrs属性说明如下:<br>
**attrs:**默认属性，告诉系统需要获取那些属性的值，如：<br>
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
