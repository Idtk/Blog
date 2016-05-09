#自定义环形图——基础知识</br>
**效果图如下：**</br>
<img src="https://github.com/Idtk/CustomView/blob/master/gif/CustomView.gif" alt="GitHub" title="GitHub,Social Coding"/><br>
## 一、涉及知识</br>
**View中的坐标、弧度、颜色**</br>

| 作用        | API名           | 备注  |
| ------------- |:-------------:| -----:|
| View坐标      | getLeft,getTop,getRight,getBottom | 依次为，View左上角顶点相对于父布局的左侧和顶部距离，右下角顶点相对于父布局的左侧和顶部距离 |
| MotionEvent坐标      | getX,getY,getRawX,getRawY |   getX,getY相对于当前view的位置坐标，getRawX,getRawY相对于屏幕的位置坐标 |
| 弧度、角度      | toRadians,toDegrees |   toRadians角度转换为近似相等的弧度，toDegrees弧度转换为近似相等的角度| 
| 颜色      | Color.argb(透明度，红，绿，蓝)) |   颜色从透明到不透明，或从浅到深，都用0x00到0xff表示|
