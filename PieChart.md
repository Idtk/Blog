#自定义环形图——基础知识</br>
**效果图如下：**</br>
<img src="https://github.com/Idtk/CustomView/blob/master/gif/CustomView.gif" alt="GitHub" title="GitHub,Social Coding"/><br>
## 一、涉及知识</br>
**View中的坐标、弧度、颜色**</br>
| 作用        | API名           | 备注  |
| ------------- |:-------------:| :----- |
| View坐标      | getLeft,getTop,getRight,getBottom | 依次为，View左上角顶点相对于父布局的左侧和顶部距离，右下角顶点相对于父布局的左侧和顶部距离 |
| MotionEvent坐标      | getX,getY,getRawX,getRawY |   getX,getY相对于当前view的位置坐标，getRawX,getRawY相对于屏幕的位置坐标 |
| 弧度、角度      | toRadians,toDegrees |   toRadians角度转换为近似相等的弧度，toDegrees弧度转换为近似相等的角度| 
| 颜色      | Color.argb(透明度，红，绿，蓝)) |   颜色从透明到不透明，或从浅到深，都用0x00到0xff表示|</br>
**Canvas常用方法**</br>
| 作用        | API           | 备注  |
| ------------- |:-------------:| :----- |
| 绘制图形      | drawPoint, drawPoints, drawLine, drawLines, drawRect, drawRoundRect, drawOval, drawCircle, drawArc | 依次为绘制点、直线、矩形、圆角矩形、椭圆、圆、扇形 |
| 绘制文本      | drawText, drawPosText, drawTextOnPath |    依次为绘制文字、指定每个字符位置绘制文字、根据路径绘制文字|
| Canvas变换      | translate, scale, rotate, skew |   依次为Canvas平移、缩放、旋转、倾斜（错切） |</br>
**Paint常用方法**</br>
| 作用        | API           | 备注  |
| ------------- |:-------------:| :----- |
| 颜色      | setColor,setARGB，setAlpha，setColorFilter | 
依次为设置画笔颜色、透明度，色彩过滤器 |
| 类型      | setStyle |   填充(FILL),描边(STROKE),填充加描边(FILL_AND_STROKE) |
| 抗锯齿      | setAntiAlias |   画笔是否抗锯齿 |
| 字体大小      | setTextSize |   设置字体大小 |
| 字体测量      | getFontMetrics()，getFontMetricsInt()，measureText |   返回字体的行间距，返回值一次为float、int |
| 文字宽度测量      | measureText |   返回文字的宽度 |
| 文字对齐方式      | setTextAlign |   左对齐(LEFT),居中对齐(CENTER),右对齐(RIGHT) |
| 宽度      | setStrokeWidth |   设置画笔宽度 |
| 笔锋      | setStrokeCap |   默认(BUTT),半圆形(ROUND),方形(SQUARE) |
