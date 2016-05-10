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
public PieChart(Context context) {}
public PieChart(Context context, AttributeSet attrs) {}
public PieChart(Context context, AttributeSet attrs, int defStyleAttr) {}
public PieChart(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {}
```
**context:**上下文
一般新建是调用
```Java
PieChart mPieChart = new PieChart(this);
```
**attrs:**自定义默认属性
一般放置于res/values/attrs.xml中的declare-styleable中，如：
```xml
<resources>
   <declare-styleable name="PieChart">
       <attr name="showText" format="boolean" />
       <attr name="labelPosition" format="enum">
           <enum name="left" value="0"/>
           <enum name="right" value="1"/>
       </attr>
   </declare-styleable>
</resources>
```
```Java
PieChart mPieChart = new PieChart(this,R.attrs.PieChart);
```
**defStyleAttr:**自定义默认主题
一般放置于res/values/attrs.xml中的attribute中，提供默认主题,如:
```xml
<attr name="customViewStyle" format="reference" /> 
```
在styles.xml文件中
```xml
<style name="customviewstyle">  
    <item name="tittle">attr3 from custom_view_style</item>  
    <item name="textsize">attr4 from custom_view_style</item>  
</style>  
```
<br>
**defStyleRes:**自定义默认风格
一般放置于res/values/styles.xml中的resource中，*只有当defStyleAttr无效时，才会使用这个值*,如：
```xml
<style name="xml_style">  
    <item name="ponitsize">attr3 from custom_view_style</item>  
    <item name="touchflag">attr4 from custom_view_style</item>  
</style>  
```
