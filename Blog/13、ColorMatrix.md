# ColorMatrix详解

**涉及方法**<br>

| 类别        | API           |描述  |
| ------------- |:-------------:|-----|
| 旋转     | setRotate  |  设置(非输入轴颜色的)色调 |
| 饱和度   | setSaturation  | 设置饱和度 |
| 缩放     | setScale   | 三原色的取值的比例 |
| 设置     | set、setConcat | 设置颜色矩阵、两个颜色矩阵的乘积 |
| 重置     | reset  | 重置颜色矩阵为初始状态 |
| 矩阵运算  | preConcat、postConcat  | 颜色矩阵的前乘、后乘 |

## 一、颜色矩阵
颜色矩阵是一个用来表示三原色和透明度的4x5的矩阵，表示为一个数组的形式
```Java
[ a, b, c, d, e,
    f, g, h, i, j,
    k, l, m, n, o,
    p, q, r, s, t ]
```

一个颜色则使用`[R, G, B, A]`的方式进行表示，所以矩阵与颜色的计算方式则为<br>
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/颜色矩阵计算.png" alt="颜色矩阵计算" title="颜色矩阵计算" />
<br>

<img src="http://chart.googleapis.com/chart?cht=tx&chl=\Large x= $ \begin{Bmatrix} a & b & c & d & e \\ f & g & h & i & j \\ k & l & m & n & o \\ p & q & r & s & t \\ \end{Bmatrix} $ " style="border:none;">

从上述的公式可以看出,颜色矩阵的功能划分如下<br>

* `a, b, c, d, e` 表示三原色中的红色
* `f, g, h, i, j` 表示三原色中的绿色
* `k, l, m, n, o` 表示三原色中的蓝色
* `p, q, r, s, t` 表示颜色的透明度
* 第五列用于表示颜色的偏移量

## 二、常用方法
### 1、旋转
### 2、饱和度
### 3、缩放
### 三、矩阵运算
## 四、总结
## 五、参考
[ColorMatrix](https://developer.android.com/reference/android/graphics/ColorMatrix.html)
