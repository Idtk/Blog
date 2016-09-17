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

```
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
我们可以根据三原色来建立一个三维向量坐标系，当围绕红色旋转时，我们将红色虚化为一个点，绿色为横坐标，蓝色为纵坐标，旋转θ°。<br>

坐标系示例
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/红色坐标系.png" alt="红色坐标系" title="红色坐标系" />
<br>

R、G、B、A各值计算结果:

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
A
\\end{1} 
\\right ]
$$)

#### b、围绕绿色轴旋转
绿色虚化为一个点，蓝色为横坐标轴，红色为纵坐标轴，旋转θ°。<br>
坐标系示例
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/绿色坐标系.png" alt="绿色坐标系" title="绿色坐标系" />
<br>

R、G、B、A各值计算结果:

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
A
\\end{1} 
\\right ]
$$)

#### c、围绕蓝色轴旋转
蓝色虚化为一个点，红色为横坐标轴，绿色为纵坐标轴，旋转θ°。<br>
坐标系示例

<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/蓝色坐标系.png" alt="蓝色坐标系" title="蓝色坐标系" />
<br>

R、G、B、A各值计算结果:

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
A
\\end{1} 
\\right ]
$$)

### 2、饱和度
### 3、缩放
## 三、总结
## 四、参考
[ColorMatrix](https://developer.android.com/reference/android/graphics/ColorMatrix.html)<br>
[Paint之ColorMatrix与滤镜效果](http://blog.csdn.net/harvic880925/article/details/51187277)
