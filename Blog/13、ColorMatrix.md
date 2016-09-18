# ColorMatrix详解

[自定义View系列目录](https://github.com/Idtk/Blog)

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
```java
[ a, b, c, d, e,
    f, g, h, i, j,
    k, l, m, n, o,
    p, q, r, s, t ]
```

一个颜色则使用`[R, G, B, A]`的方式进行表示，所以矩阵与颜色的计算方式则为<br>
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/颜色矩阵计算.png" alt="颜色矩阵计算" title="颜色矩阵计算" />
<br>

从上述的公式可以看出,颜色矩阵的功能划分如下<br>

* `a, b, c, d, e` 表示三原色中的红色
* `f, g, h, i, j` 表示三原色中的绿色
* `k, l, m, n, o` 表示三原色中的蓝色
* `p, q, r, s, t` 表示颜色的透明度
* 第五列用于表示颜色的偏移量

### 使用示例

首先我们在不改变初始矩阵的情况下，来看一下图片的效果
```Java
private ColorMatrix mColorMatrix;
private Paint mPaint;
private Bitmap oldBitmap;

mColorMatrix = new ColorMatrix();
mPaint = new Paint();
// 设置画笔的颜色过滤器
mPaint.setColorFilter(new ColorMatrixColorFilter(mColorMatrix));
Log.d("TAG", Arrays.toString(mColorMatrix.getArray()));
// 创建Bitmap
oldBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.header);

// 在画布上显示图片
canvas.drawBitmap(oldBitmap,0,0,mPaint);

// Log
TAG: [1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0]
```

<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/初始矩阵.png" alt="初始矩阵" title="初始矩阵" />
<br>

现在我们新建一个矩阵，使用set方法来使用这个矩阵，改变图片的颜色
```Java
mColorMatrix.set(new float[]{
        1,0.5f,0,0,0
        ,0,1,0,0,0
        ,0,0,1,0,0
        ,0,0,0,1,0});
```
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/0.5红矩阵.png" alt="红矩阵" title="红矩阵" />
<br>

## 二、常用方法
### 1、旋转
API如下：

```Java
/**
* 用于色调的旋转运算
* axis=0 表示色调围绕红色进行旋转
* axis=1 表示色调围绕绿色进行旋转
* axis=2 表示色调围绕蓝色进行旋转
*/

public void setRotate(int axis, float degrees)
```
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/三原色坐标系.png" alt="三原色坐标系" title="三原色坐标系" />
<br>

#### a、围绕红色轴旋转
我们可以根据三原色来建立一个三维**向量**坐标系，当围绕红色旋转时，我们将红色虚化为一个点，绿色为横坐标，蓝色为纵坐标，旋转θ°。<br>

坐标系示例
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/红色坐标系.png" alt="红色坐标系" title="红色坐标系" />
<br>

根据平行四边形法则R、G、B、A各值计算结果:

![](http://latex.codecogs.com/png.latex?$$ R = R' $$)<br>
![](http://latex.codecogs.com/png.latex?$$ G = G'cosθ + B'sinθ $$)<br>
![](http://latex.codecogs.com/png.latex?$$ B = -G'sinθ + B'cosθ $$)<br>
![](http://latex.codecogs.com/png.latex?$$ A = A' $$)

矩阵表示:

![](http://latex.codecogs.com/png.latex?
$$
\\left [ 
\\begin{matrix} 
R\\\\
G\\\\
B\\\\
A
\\end{1} 
\\right ] 
 = 
\\left [ 
\\begin{matrix}  
 1   &  0   &  0  &  0  &  0 \\\\
 0   &  cosθ   &  sinθ  &  0  &  0 \\\\
 0   &  -sinθ   &  cosθ  &  0  &  0 \\\\
 0   &  0   &  0  &  1  &  0
\\end{1} 
\\right ] 
\\left [ 
\\begin{matrix} 
R'\\\\
G'\\\\
B'\\\\
A'
\\end{1} 
\\right ]
$$)

#### b、围绕绿色轴旋转
绿色虚化为一个点，蓝色为横坐标轴，红色为纵坐标轴，旋转θ°。<br>
坐标系示例
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/绿色坐标系.png" alt="绿色坐标系" title="绿色坐标系" />
<br>

根据平行四边形法则R、G、B、A各值计算结果:

![](http://latex.codecogs.com/png.latex?$$ R = R'cosθ-B'sinθ $$)<br>
![](http://latex.codecogs.com/png.latex?$$ G = G' $$)<br>
![](http://latex.codecogs.com/png.latex?$$ B = R'sinθ + B'cosθ $$)<br>
![](http://latex.codecogs.com/png.latex?$$ A = A' $$)

矩阵表示:

![](http://latex.codecogs.com/png.latex?
$$
\\left [ 
\\begin{matrix} 
R\\\\
G\\\\
B\\\\
A
\\end{1} 
\\right ] 
 = 
\\left [ 
\\begin{matrix}  
 cosθ   &  0   &  -sinθ  &  0  &  0 \\\\
 0   &  1   &  0  &  0  &  0 \\\\
 sinθ   &  0   &  cosθ  &  0  &  0 \\\\
 0   &  0   &  0  &  1  &  0
\\end{1} 
\\right ] 
\\left [ 
\\begin{matrix} 
R'\\\\
G'\\\\
B'\\\\
A'
\\end{1} 
\\right ]
$$)

#### c、围绕蓝色轴旋转
蓝色虚化为一个点，红色为横坐标轴，绿色为纵坐标轴，旋转θ°。<br>
坐标系示例

<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/蓝色坐标系.png" alt="蓝色坐标系" title="蓝色坐标系" />
<br>

根据平行四边形法则R、G、B、A各值计算结果:

![](http://latex.codecogs.com/png.latex?$$ R = R'cosθ+G'sinθ $$)<br>
![](http://latex.codecogs.com/png.latex?$$ G = -R'sinθ+G'cos $$)<br>
![](http://latex.codecogs.com/png.latex?$$ B = B' $$)<br>
![](http://latex.codecogs.com/png.latex?$$ A = A' $$)

矩阵表示:

![](http://latex.codecogs.com/png.latex?
$$
\\left [ 
\\begin{matrix} 
R\\\\
G\\\\
B\\\\
A
\\end{1} 
\\right ] 
 = 
\\left [ 
\\begin{matrix}  
 cosθ   &  sinθ   &  0  &  0  &  0 \\\\
 -sinθ   &  cosθ   &  0  &  0  &  0 \\\\
 0   &  0   &  1  &  0  &  0 \\\\
 0   &  0   &  0  &  1  &  0
\\end{1} 
\\right ] 
\\left [ 
\\begin{matrix} 
R'\\\\
G'\\\\
B'\\\\
A'
\\end{1} 
\\right ]
$$)

#### 使用示例

这里设置色调围绕红色轴旋转90°
```Java
// 旋转绿色、蓝色
mColorMatrix.setRotate(0,90);

// Log
D/TAG: [1.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.371139E-8, 1.0, 0.0, 0.0, 0.0, -1.0, -4.371139E-8, 0.0, 0.0,
        0.0, 0.0, 0.0, 1.0, 0.0]
```

从Log中我们可以看出，其结果也验证了我们的上述理论，图片效果如下:

<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/旋转矩阵.png" alt="旋转矩阵" title="旋转矩阵" />
<br>

### 2、缩放

API如下：

```Java
/**
* rScale 表示红色的数值的缩放比例
* gScale 表示绿色的数值的缩放比例
* bScale 表示蓝色的数值的缩放比例
* aScale 表示透明度的数值的缩放比例
*/

public void setScale(float rScale, float gScale, float bScale,float aScale)
```
ColorMatrix的缩放方法，其实就是根据矩阵的运算规则，对`R、G、B、A`的数值分别进行缩放操作，当然在操作之前，会对现有的ColorMatrix进行初始化操作。<br>

R、G、B、A各值计算结果:

![](http://latex.codecogs.com/png.latex?$$ R = R'rScale $$)<br>
![](http://latex.codecogs.com/png.latex?$$ G = G'gScale $$)<br>
![](http://latex.codecogs.com/png.latex?$$ B = B'bScale $$)<br>
![](http://latex.codecogs.com/png.latex?$$ A = A'aScale $$)

矩阵表示:

![](http://latex.codecogs.com/png.latex?
$$
\\left [ 
\\begin{matrix} 
R\\\\
G\\\\
B\\\\
A
\\end{1} 
\\right ] 
 = 
\\left [ 
\\begin{matrix}  
 rScale   &  0   &  0  &  0  &  0 \\\\
 0   &  gScale   &  0  &  0  &  0 \\\\
 0   &  0   &  bScale  &  0  &  0 \\\\
 0   &  0   &  0  &  aScale  &  0
\\end{1} 
\\right ] 
\\left [ 
\\begin{matrix} 
R'\\\\
G'\\\\
B'\\\\
A'
\\end{1} 
\\right ]
$$)

#### 使用示例
这里设置，所有的缩放比例为1.1
```Java
// 设置缩放比例
mColorMatrix.setScale(1.1f,1.1f,1.1f,1.1f);

// Log
D/TAG: [1.1, 0.0, 0.0, 0.0, 0.0, 0.0, 1.1, 0.0, 0.0, 0.0, 0.0, 0.0, 1.1, 0.0, 0.0, 0.0, 0.0, 0.0, 1.1, 0.0]
```

从Log中我们可以看出，其结果也验证了对于缩放的理解，图片效果如下 :

<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/缩放矩阵.png" alt="缩放矩阵" title="缩放矩阵" />
<br>

我们还可以制作一个颜色通道，比如红色 :
```Java
// 红色通道
mColorMatrix.setScale(1,0,0,1);

// Log
D/TAG: [1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0]
```

<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/红色通道.png" alt="红色通道" title="红色通道" />
<br>

### 3、饱和度

API如下：

```Java
/**
* 设置矩阵颜色的饱和度
* 
* sat 0表示灰度、1表示本身
*/

public void setSaturation(float sat)
```

`setSaturation`方法可以根据一定比例，整体的增加或者减少颜色的饱和度，当设置0时，表示灰度图片；当设置为1时，表示颜色不变化。<br>

灰度图片的去色原理：只要把RGB的三色通道的数值设置为一样，即`R=G=B`,那么图像就变成了灰色，同时为了保证图像的亮度，需要使同一个通道中的`R+G+B`的结果接近1。

* 在matlab中按照 0.2989 R，0.5870 G 和 0.1140 B 的比例构成像素灰度值
* 在OpenCV中按照 0.299 R， 0.587 G 和 0.114 B 的比例构成像素灰度值
* 在Android中按照0.213 R，0.715 G 和 0.072 B 的比例构成像素灰度值

### 使用示例

这里设置饱和度为0，测试下灰度效果
```Java
// 灰度
mColorMatrix.setSaturation(0f);

// Log
D/TAG: [0.213, 0.715, 0.072, 0.0, 0.0, 0.213, 0.715, 0.072, 0.0, 0.0, 0.213, 0.715, 0.072, 0.0, 0.0, 
        0.0, 0.0, 0.0, 1.0, 0.0]
```
<br>
打印出的Log也验证了上述对于图片灰度的说明，当然源码中还有对于饱和度从0%——100%的计算，感兴趣的同学可以查看以下源码。灰度图片效果如下:

<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/饱和度矩阵.png" alt="饱和度矩阵" title="饱和度矩阵" />
<br>

## 三、ColorMatrix相乘

### 1、设置

设置新的矩阵覆盖之前的内容，可以设置一个单独的矩阵，也可以设置两个矩阵的相乘。API如下:

```Java
public void set(ColorMatrix src)
public void set(float[] src)

public void setConcat(ColorMatrix matA, ColorMatrix matB)
```

这里主要说一下`setConcat`方法，此方法表示两个ColorMatrix相乘![](http://latex.codecogs.com/png.latex?$$ M =  M_A \\cdot M_B $$)，矩阵表示为:<br>

![](http://latex.codecogs.com/png.latex?
$$
\\left [ 
\\begin{matrix} 
 & &\\\\
 & Result ColorMatrix &\\\\
 & &
\\end{1} 
\\right ] 
 = 
\\left [ 
\\begin{matrix} 
 a_0   &  a_1   &  a_2  &  a_3  &  a_4 \\\\
 a_5   &  a_6   &  a_7  &  a_8  &  a_9 \\\\
 a_1_0   &  a_1_1   &  a_1_2  &  a_1_3  &  a_1_4 \\\\
 a_1_5   &  a_1_6   &  a_1_7  &  a_1_8  &  a_1_9
\\end{1} 
\\right ]  
\\left [ 
\\begin{matrix}  
 b_0   &  b_1   &  b_2  &  b_3  &  b_4 \\\\
 b_5   &  b_6   &  b_7  &  b_8  &  b_9 \\\\
 b_1_0   &  b_1_1   &  b_1_2  &  b_1_3  &  b_1_4 \\\\
 b_1_5   &  b_1_6   &  b_1_7  &  b_1_8  &  b_1_9
\\end{1} 
\\right ]
$$)

#### 使用示例
```Java

mColorMatrixA = new ColorMatrix(new float[]{
        1,0.3f,0,0,0
        ,0,1,0.3f,0,0.1f
        ,0,0.6f,1,0,0
        ,0,0,0,1,1
});
mColorMatrixB = new ColorMatrix(new float[]{
        1,0,0,0,1
        ,0,1,0,0,0.5f
        ,0.1f,0.9f,0.8f,0,0
        ,0,0,0,1,0.8f
});
mColorMatrix = new ColorMatrix(new float[]{
        0,0,0,0,0
        ,0,0,0,0,0
        ,0,0,0,0,0
        ,0,0,0,0,0
});

mColorMatrix.setConcat(mColorMatrixA,mColorMatrixB);

Log.d("TAGA", Arrays.toString(mColorMatrixA.getArray()));
Log.d("TAGB", Arrays.toString(mColorMatrixB.getArray()));
Log.d("TAGAB", Arrays.toString(mColorMatrix.getArray()));

// Log
D/TAGA: [1.0, 0.3, 0.0, 0.0, 0.0, 0.0, 1.0, 0.3, 0.0, 0.1, 0.0, 0.6, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0]
D/TAGB: [1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.5, 0.1, 0.9, 0.8, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.8]
D/TAGAB: [1.0, 0.3, 0.0, 0.0, 1.15, 0.030000001, 1.27, 0.24000001, 0.0, 0.6, 0.1, 1.5, 0.8, 0.0, 0.3, 0.0, 0.0, 0.0, 1.0, 1.8]
```

### 2、前乘

前乘相当于，当前矩阵乘以输入的矩阵![](http://latex.codecogs.com/png.latex?$$ M' =  M \\cdot S $$)，这里看一下源码，可以更容易的理解:
```Java
// 逻辑上相当于调用setConcat(this, prematrix)
public void preConcat(ColorMatrix prematrix) {
    setConcat(this, prematrix);
}
```
从源码上可以明显的看出前乘的规则，preConcat(prematrix)方法相当于调用setConcat(this, prematrix)方法

#### 使用示例

```Java
mColorMatrix.reset();
mColorMatrix.preConcat(mColorMatrixA);
mColorMatrix.preConcat(mColorMatrixB);
```

上例多次调用preConcat，则相当于<br>

![](http://latex.codecogs.com/png.latex?
$$
\\left [ 
\\begin{matrix} 
 & &\\\\
 & Result Matrix &\\\\
 & &
\\end{1} 
\\right ]  
 = 
\\left [ 
\\begin{matrix} 
 & &\\\\
 & Initial Matrix &\\\\
 & &
\\end{1} 
\\right ] 
\\left [ 
\\begin{matrix} 
 a_0   &  a_1   &  a_2  &  a_3  &  a_4 \\\\
 a_5   &  a_6   &  a_7  &  a_8  &  a_9 \\\\
 a_1_0   &  a_1_1   &  a_1_2  &  a_1_3  &  a_1_4 \\\\
 a_1_5   &  a_1_6   &  a_1_7  &  a_1_8  &  a_1_9
\\end{1} 
\\right ]  
\\left [ 
\\begin{matrix}  
 b_0   &  b_1   &  b_2  &  b_3  &  b_4 \\\\
 b_5   &  b_6   &  b_7  &  b_8  &  b_9 \\\\
 b_1_0   &  b_1_1   &  b_1_2  &  b_1_3  &  b_1_4 \\\\
 b_1_5   &  b_1_6   &  b_1_7  &  b_1_8  &  b_1_9
\\end{1} 
\\right ]
$$)

### 3、后乘

后乘相当于，输入的矩阵乘以当前矩阵![](http://latex.codecogs.com/png.latex?$$ M' =  S \\cdot M $$)，这里看一下源码，可以更容易的理解:
```Java
// 逻辑上相当于调用setConcat(postmatrix, this)
public void postConcat(ColorMatrix postmatrix) {
    setConcat(postmatrix, this);
}
```

从源码上可以明显的看出前乘的规则,postConcat(prematrix)方法相当于调用setConcat(postmatrix, this)

# 使用示例

```Java
mColorMatrix.reset();
mColorMatrix.postConcat(mColorMatrixA);
mColorMatrix.postConcat(mColorMatrixB);
```

上例多次调用postConcat，因为矩阵满足交换律，则相当于<br>

![](http://latex.codecogs.com/png.latex?
$$
\\left [ 
\\begin{matrix} 
 & &\\\\
 & Result Matrix &\\\\
 & &
\\end{1} 
\\right ]  
 = 
\\left [ 
\\begin{matrix} 
 b_0   &  b_1   &  b_2  &  b_3  &  b_4 \\\\
 b_5   &  b_6   &  b_7  &  b_8  &  b_9 \\\\
 b_1_0   &  b_1_1   &  b_1_2  &  b_1_3  &  b_1_4 \\\\
 b_1_5   &  b_1_6   &  b_1_7  &  b_1_8  &  b_1_9
\\end{1} 
\\right ] 
\\left [ 
\\begin{matrix} 
 a_0   &  a_1   &  a_2  &  a_3  &  a_4 \\\\
 a_5   &  a_6   &  a_7  &  a_8  &  a_9 \\\\
 a_1_0   &  a_1_1   &  a_1_2  &  a_1_3  &  a_1_4 \\\\
 a_1_5   &  a_1_6   &  a_1_7  &  a_1_8  &  a_1_9
\\end{1} 
\\right ]  
\\left [ 
\\begin{matrix}  
 & &\\\\
 & Initial Matrix &\\\\
 & &
\\end{1} 
\\right ]
$$)

## 四、总结

本文我们学习了ColorMatrix的原理，并分析了其`setRotate、setScale、setSaturation`方法以及矩阵的乘法(前乘、后乘)。如果在阅读过程中，有任何疑问与问题，欢迎与我联系。<br>
**博客:www.idtkm.com**<br>
**GitHub:https://github.com/Idtk**<br>
**微博:http://weibo.com/Idtk**<br>
**邮箱:IdtkMa@gmail.com**<br>
<br>

## 五、参考
[ColorMatrix](https://developer.android.com/reference/android/graphics/ColorMatrix.html)<br>
[Paint之ColorMatrix与滤镜效果](http://blog.csdn.net/harvic880925/article/details/51187277)<br>
[Android Matrix矩阵详解](http://blog.csdn.net/maplejaw_/article/details/51728818)
