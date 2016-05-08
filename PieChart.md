#自定义环形图——基础知识</br>
**效果图如下：**</br>
<img src="https://github.com/Idtk/CustomView/blob/master/gif/CustomView.gif" alt="GitHub" title="GitHub,Social Coding"/><br>
## 一、涉及知识</br>
| 作用        | 相关方法           | 备注  |
| ------------- |:-------------:| :----- |
| View坐标      | getLeft(),getTop(),getRight(),getBottom() |    View左上角顶点相对于父布局的左侧和顶部距离，右下角顶点相对于父布局的左侧和顶部距离   |
| MotionEvent坐标 | getX(),getY(),getRawX(),getRawY() |    getX(),getY()相对于原点的位置坐标，getRawX(),getRawY()相对于屏幕的位置坐标 |
| 弧度、角度 | toDegrees(double angdeg),toDegrees(double angrad)   |           toDegrees将用弧度表示的角转换为近似相等的用角度表示的角,toDegrees将用角度表示的角转换为近似相等的用弧度表示的角   |
| 颜色 | Color.argb(透明度，红，绿，蓝) | 颜色从透明到不透明，颜色从浅到深，都用从0x00到0xff表示 |
