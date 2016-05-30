# 自定义PieChart

<img src="https://github.com/Idtk/CustomView/blob/master/gif/PieChart.gif" alt="PieChat" title="PieChat" width="300" /><br>

(**PS: 经过之前[3篇博客](http://www.idtkm.com/category/customview/)的基础知识学习，终于可以开始编写PieChart了 ~\(≧▽≦)/~啦啦啦**)

## 一、数据需求
来分析下，用户需要提供怎样的数据，首先要有数据的值，然后还需要对应的数据名称，以及颜色。绘制PieChart需要什么呢，由图可以看出，需要百分比值，扇形角度，色块颜色。所以总共属性有:
```Java
public class PieData {
    private String name;
    private float value;
    private float percentage;
    private int color = 0;
    private float angle = 0;
}
```
各属性的set与get请自行增加。

## 二、构造函数
构造函数中，增加一些xml设置，创建一个attrs.xml
```Java
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <declare-styleable name="PieChart">
        <attr name="name" format="string"/>
        <attr name="percentDecimal" format="integer"/>
        <attr name="textSize" format="dimension"/>
    </declare-styleable>
</resources>
```
这是只设置了一部分属性，如果你有强迫症希望全部设置的话，可以自行增加。在PieChart中使用TypedArray进行属性的获取。建议使用如下的写法，可以避免在没有设置属性时，也运行getXXX方法。
```Java
TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.PieChart, defStyleAttr,defStyleRes);
int n = array.getIndexCount();
for (int i=0; i<n; i++){
    switch (i){
        case R.styleable.PieChart_name:
            name = array.getString(i);
            break;
        case R.styleable.PieChart_percentDecimal:
            percentDecimal = array.getInt(i,percentDecimal);
            break;
        case R.styleable.PieChart_textSize:
            percentTextSize = array.getDimensionPixelSize(i,percentTextSize);
            break;
    }
}
array.recycle();
```
## 三、动画函数
绘制一个完整的圆，旋转的角度为360，动画时间为可set参数，默认5秒。
```Java
private void initAnimator(long duration){
    if (animator !=null &&animator.isRunning()){
        animator.cancel();
        animator.start();
    }else {
        animator=ValueAnimator.ofFloat(0,360).setDuration(duration);
        animator.setInterpolator(timeInterpolator);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                animatedValue = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.start();
    }
}
```
## 四、onSizeChanged
在此函数中，获取当前view的宽高，同时进行PieChart绘制所需的半径以及布局位置设置。<br>
图<br>

```Java
protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    mWidth = w;
    mHeight = h;
    //标准圆环
    //圆弧
    r = (float) (Math.min(mWidth,mHeight)/2*widthScaleRadius);// 饼状图半径
    // 饼状图绘制区域
    rectF.left = -r;
    rectF.top = -r;
    rectF.right =r;
    rectF.bottom = r;
    //白色圆弧
    //透明圆弧
    rTra = (float) (r*radiusScaleTransparent);
    rectFTra.left = -rTra;
    rectFTra.top = -rTra;
    rectFTra.right = rTra;
    rectFTra.bottom = rTra;
    //白色圆
    rWhite = (float) (r*radiusScaleInside);

    //浮出圆环
   	...
}
```
## 五、onDraw
坐标原点平移至中心位置。根据设定的圆环起始角度(默认为0)，旋转画布。
### 1、绘制圆弧
累加每个扇形的角度，获取每个扇形的起始角度。

### 2、绘制文字