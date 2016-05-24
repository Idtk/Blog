# 自定义PieChart

<img src="https://github.com/Idtk/CustomView/blob/master/gif/CustomView.gif" alt="PieChat" title="PieChat" width="300" /><br>

(**PS: 经过之前[3篇博客](https://github.com/Idtk/Blog)的基础知识学习，终于可以开始编写PieChart~\(≧▽≦)/~啦啦啦**)

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
            name = array.getString(R.styleable.PieChart_name);
            break;
        case R.styleable.PieChart_percentDecimal:
            percentDecimal = array.getInt(R.styleable.PieChart_percentDecimal,percentDecimal);
            break;
        case R.styleable.PieChart_textSize:
            percentTextSize = array.getDimensionPixelSize(R.styleable.PieChart_textSize,percentTextSize);
            break;
    }
}
array.recycle();
```
