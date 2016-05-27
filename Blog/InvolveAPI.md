**绘制过程**<br>

| 类别        | API           |描述  |
| ------------- |:-------------:|-----|
| 布局     | onMeasure  |  测量View与Child View的大小 |
|         | onLayout  |   确定Child View的位置|
|         | onSizeChanged  |   确定View的大小|
| 绘制     | onDraw  |   实际绘制View的内容|
| 事件处理     | onTouchEvent  |   处理屏幕触摸事件|
| 重绘     | invalidate  |   调用onDraw方法，重绘View中变化的部分|
| 重新布局     | requestLayout  |   调用onLayout、onMeasure方法，重新布局|
<br>
**Canvas涉及方法**</br>

| 类别        | API           | 描述   |  
| ------------- |:-------------:| -----   |  
| 绘制图形      | drawPoint, drawPoints, drawLine, drawLines, drawRect, drawRoundRect, drawOval, drawCircle, drawArc | 依次为绘制点、直线、矩形、圆角矩形、椭圆、圆、扇形 |
| 绘制文本      | drawText, drawPosText, drawTextOnPath |    依次为绘制文字、指定每个字符位置绘制文字、根据路径绘制文字|
| 画布变换      | translate, scale, rotate, skew |   依次为平移、缩放、旋转、倾斜（错切） |
| 画布裁剪      | clipPath, clipRect, clipRegion |   依次为按路径、按矩形、按区域对画布进行裁剪 |
| 画布状态      | sava,restore |   保存当前画布矩阵，恢复之前保存的画布 |
</br>
**Paint涉及方法**</br>

| 类别        | API           | 描述  |
| ------------- |:-------------:| -----   | 
| 颜色      | setColor, setARGB, setAlpha | 依次为设置画笔颜色、透明度 |
| 类型      | setStyle |   填充(FILL),描边(STROKE),填充加描边(FILL_AND_STROKE) |
| 抗锯齿      | setAntiAlias |   画笔是否抗锯齿 |
| 字体大小      | setTextSize |   设置字体大小 |
| 字体测量      | getFontMetrics()，getFontMetricsInt() |   返回字体的各种测量值，返回值依次为float、int |
| 文字宽度测量      | measureText |   返回文字的宽度 |
| 文字对齐方式      | setTextAlign |   左对齐(LEFT),居中对齐(CENTER),右对齐(RIGHT) |
| 宽度      | setStrokeWidth |   设置画笔宽度 |
| 笔锋      | setStrokeCap |   默认(BUTT),半圆形(ROUND),方形(SQUARE) |
<br>
**ValueAnimator**<br>

| API        | 简介           |
| ------------- | ------------- |
|ofFloat(float... values)|构建ValueAnimator，设置动画的浮点值，需要设置2个以上的值|
|setDuration(long duration)|设置动画时长，默认的持续时间为300毫秒。|
|setInterpolator(TimeInterpolator value)|设置动画的线性非线性运动，默认AccelerateDecelerateInterpolator|
|addUpdateListener(ValueAnimator.AnimatorUpdateListener listener)|监听动画属性每一帧的变化|
<br>
**Path**<br>

| 类型  | API | 描述 |
| ------------- |:-------------:| ------------- |
| 添加路径 | addArc, addCircle, addOval, addPath, addRect, addRoundRect, arcTo | 依次为添加圆弧、圆、椭圆、路径、矩形、圆角矩形、圆弧|
| 移动起点 | moveTo | 移动起点位置，仅对之后路径产生影响 |
| 移动终点 | setLastPoint | 移动终点位置，对前后的路径都会产生影响|
| 直线 | lineTo | 增加一条道指定点的直线 |
| 贝塞尔 | quadTo, cubicTo | 二阶、三阶贝塞尔曲线 |
| 闭合路径 | close | 路径终点连接到起点|
| rXXX | rMoveTo, rLineTo, rQuadTo, rCubicTo, | 依次为相对路径的移动起点，增加直线，二阶贝塞尔曲线，三阶贝塞尔曲线 | 
| 偏移 | offset | 对路径进行平移|
| 判断 | isConvex, isEmpty, isInverseFillType, isRect | 依次为判断路径的凹凸、是否为空、是否为逆填充模式，是否为矩形|
| 逻辑运算 | op | A\B(DIFFERENCE), A∩B(INTERSECT), B\A(REVERSE_DIFFERENCE), A∪B(UNION), A⊕B(XOR)|
| 替换路径 | set | 用新的路径替换当前路径 |
| 重置 | reset, rewind| 清除path使它为空，清除path但保留内部的数据结构 |
| 计算边界 | computeBounds| 计算路径的矩形边界 |
| 填充 | getFillType, setFillType, toggleInverseFillType| 获取，设置，切换填充模式|
| Matrix | transform | 矩阵变换，缩放，旋转、平移、倾斜(错切)等 |
<br>

# staticLayout居中方法，源码阅读

# [Data Binding](https://developer.android.com/topic/libraries/data-binding/index.html)

# Matrix 理论与应用