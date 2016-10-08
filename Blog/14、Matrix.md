# Matrix原理

[自定义View系列目录](https://github.com/Idtk/Blog)

## 一、Matrix结构

在Android开发中，矩阵是一个非常强大且有趣的工具，在之前的一篇文章中对ColorMatrix的原理进行了详细的分析，用其实现了一些简单的颜色过滤功能，这一次我们来探寻一下同样强大的Matrix，它具有更改图像图形的有趣功能。我们先来看看Matrix的结构。

![](http://latex.codecogs.com/png.latex?
$$
\\left [ 
\\begin{matrix} 
MSCALE\\_X & MSKEW\\_X & MTRANS\\_X \\\\
\\\\
MSKEW\\_Y & MSCALE\\_Y & MTRANS\\_Y \\\\
\\\\
MPERSP\\_0 & MPERSP\\_1 & MPERSP\\_2 
\\end{1} 
\\right ] 
$$)

我们可以看到Matrix是一个3X3的矩阵。

* MSCALE_X、MSCALE_Y、MPERSP_2 分别表示X、Y、w(透视)的缩放
* MSKEW_X、MSKEW_Y 分别表示X、Y的错切
* MTRANS_X、MTRANS_Y 分别表示X、Y的平移
* MPERSP_0、MPERSP_1 分别表示X、Y方向上的透视

### 使用示例

在Android的很多地方其实都使用到了Matrix的方法，比如图片、Canvas、动画等，我们这里以图片为例，像在[Canvas与ValueAnimator](https://github.com/Idtk/Blog/blob/master/Blog/2%E3%80%81CanvasAndValueAnimator.md)章节中一样，在`onDraw`函数中我们先绘制一个坐标系，然后来绘制一个矩形。

```Java
// 平移画布
canvas.translate(mWidth/2,mHeight/2);
mPaint.setStrokeWidth(1);// 恢复画笔默认宽度
// 绘制X轴
canvas.drawLine(-mWidth/2*0.8f,0,mWidth/2*0.8f,0,mPaint);
// 绘制Y轴
canvas.drawLine(0,-mHeight/2*0.8f,0,mHeight/2*0.8f,mPaint);
mPaint.setStrokeWidth(3);
// 绘制X轴箭头
canvas.drawLines(new float[]{
        mWidth/2*0.8f,0,mWidth/2*0.8f*0.95f,-mWidth/2*0.8f*0.05f,
        mWidth/2*0.8f,0,mWidth/2*0.8f*0.95f,mWidth/2*0.8f*0.05f
},mPaint);
// 绘制Y轴箭头
canvas.drawLines(new float[]{
        0,mHeight/2*0.8f,mWidth/2*0.8f*0.05f,mHeight/2*0.8f-mWidth/2*0.8f*0.05f,
        0,mHeight/2*0.8f,-mWidth/2*0.8f*0.05f,mHeight/2*0.8f-mWidth/2*0.8f*0.05f,
},mPaint);
// 创建矩阵
mMatrix = new Matrix();
/**
 *  测试的Matrix操作
 */

// 绘制图片
canvas.drawBitmap(mBitmap,mMatrix,null);
```
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/14/Matix0.png" alt="Matix" title="Matix" />
<br>

## 二、Matrix原理
### 1、缩放变换

将点的X轴和y轴方向分别缩放![](http://latex.codecogs.com/png.latex?$$  k_0  $$)和![](http://latex.codecogs.com/png.latex?$$  k_1  $$)倍。x、y的计算结果为 : 

![](http://latex.codecogs.com/png.latex?$$ x = k_0\\cdot x_0 $$)<br>
![](http://latex.codecogs.com/png.latex?$$ y = k_1\\cdot y_0 $$)<br>

用矩阵表示 : <br>

![](http://latex.codecogs.com/png.latex?
$$
\\left [ 
\\begin{matrix} 
x\\\\
y\\\\
1
\\end{1} 
\\right ] 
 = 
\\left [ 
\\begin{matrix}  
 k_0  &  0  &  0 \\\\
 0  &  k_1  &  0 \\\\
 0  &  0  &  1
\\end{1} 
\\right ] 
\\left [ 
\\begin{matrix} 
x_0\\\\
y_0\\\\
1
\\end{1} 
\\right ]
$$)

效果如图所示 : <br>
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/14/缩放.png" alt="缩放矩阵" title="缩放矩阵" />
<br>

### 使用示例

现在我们使用`Matrix`自带的`setScale`方法 : 
```Java
/**
 *  测试的Matrix操作
 */
mMatrix.setScale(0.5f,0.5f);

Log.d("TAG",mMatrix.toString());

// Log
D/TAG: Matrix{[0.5, 0.0, 0.0][0.0, 0.5, 0.0][0.0, 0.0, 1.0]}
```

<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/14/Matix1.png" alt="Matix" title="Matix" />
<br>

### 2、错切变换

错切变换的效果就是让所有点的x坐标(或者y坐标)保持不变，而对于的y坐标(或者x坐标)则按照比例发生平移。<br>

#### 水平错切

保持y不变，但其x坐标则按比例发生平移。x、y的计算结果为 :

![](http://latex.codecogs.com/png.latex?$$ x = x_0 + k\\cdot y_0 $$)<br>
![](http://latex.codecogs.com/png.latex?$$ y = y_0 $$)<br>

用矩阵表示 : <br>

![](http://latex.codecogs.com/png.latex?
$$
\\left [ 
\\begin{matrix} 
x\\\\
y\\\\
1
\\end{1} 
\\right ] 
 = 
\\left [ 
\\begin{matrix}  
 1  &  k  &  0 \\\\
 0  &  1  &  0 \\\\
 0  &  0  &  1
\\end{1} 
\\right ] 
\\left [ 
\\begin{matrix} 
x_0\\\\
y_0\\\\
1
\\end{1} 
\\right ]
$$)

效果如图所示 : <br>
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/14/水平错切.png" alt="水平错切" title="水平错切" />
<br>

#### 垂直错切

保持x不变，但其y坐标则按比例发生平移。x、y的计算结果为 :

![](http://latex.codecogs.com/png.latex?$$ x = x_0 $$)<br>
![](http://latex.codecogs.com/png.latex?$$ y = y_0 + k\\cdot x_0 $$)<br>

用矩阵表示 : <br>

![](http://latex.codecogs.com/png.latex?
$$
\\left [ 
\\begin{matrix} 
x\\\\
y\\\\
1
\\end{1} 
\\right ] 
 = 
\\left [ 
\\begin{matrix}  
 1  &  0  &  0 \\\\
 k  &  1  &  0 \\\\
 0  &  0  &  1
\\end{1} 
\\right ] 
\\left [ 
\\begin{matrix} 
x_0\\\\
y_0\\\\
1
\\end{1} 
\\right ]
$$)

效果如图所示 : <br>
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/14/垂直错切.png" alt="垂直错切" title="垂直错切" />
<br>

当然你也可以同时进行水平错切和垂直错切的变换。

### 使用示例

现在我们使用`Matrix`自带的`setSkew`方法 : 
```Java
/**
 *  测试的Matrix操作
 */
mMatrix.setSkew(0f,0.5f);
Log.d("TAG",mMatrix.toString());

// Log
D/TAG: Matrix{[1.0, 0.0, 0.0][0.5, 1.0, 0.0][0.0, 0.0, 1.0]}
```

<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/14/Matix2.png" alt="Matix" title="Matix" />
<br>

### 3、平移变换

假设有坐标为[](http://latex.codecogs.com/png.latex?$$ x_0、y_0 $$)，将其点进行平移，移动到点[](http://latex.codecogs.com/png.latex?$$ x、y $$)，其x、y计算结果为 : <br>

![](http://latex.codecogs.com/png.latex?$$ x = x_0 + \\Delta x $$)<br>
![](http://latex.codecogs.com/png.latex?$$ y = y_0 + \\Delta y $$)<br>

用矩阵表示 : <br>

![](http://latex.codecogs.com/png.latex?
$$
\\left [ 
\\begin{matrix} 
x\\\\
y\\\\
1
\\end{1} 
\\right ] 
 = 
\\left [ 
\\begin{matrix}  
 1  &  0  &  \\Delta x \\\\
 0  &  1  &  \\Delta y \\\\
 0  &  0  &  1
\\end{1} 
\\right ] 
\\left [ 
\\begin{matrix} 
x_0\\\\
y_0\\\\
1
\\end{1} 
\\right ]
$$)

效果如图所示 : <br>
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/14/平移.png" alt="平移" title="平移" />
<br>

### 使用示例

现在我们使用`Matrix`自带的`setTranslate`方法 : 
```Java
/**
 *  测试的Matrix操作
 */
mMatrix.setTranslate(-200,-200);
Log.d("TAG",mMatrix.toString());

// Log
D/TAG: Matrix{[1.0, 0.0, -200.0][0.0, 1.0, -200.0][0.0, 0.0, 1.0]}
```

<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/14/Matix3.png" alt="Matix" title="Matix" />
<br>

### 4、旋转变换

假设有一点坐标为[](http://latex.codecogs.com/png.latex?$$ x_0、y_0 $$)，距离原点为r，与x轴方向的夹角为α，绕原点旋转θ后，变换为点[](http://latex.codecogs.com/png.latex?$$ x、y $$)，其变换前后各点计算结果为 : <br>

![](http://latex.codecogs.com/png.latex?$$ x_0 = r\\cdot\\cos \\alpha $$)<br>
![](http://latex.codecogs.com/png.latex?$$ y_0 = r\\cdot\\sin \\alpha $$)<br>
![](http://latex.codecogs.com/png.latex?
$$ x 
= r \\cdot \\cos （\\alpha + \\theta）
= r \\cdot\\cos\\alpha\\cos\\theta - r \\cdot\\sin\\alpha\\sin\\theta 
= x_0\\cos\\theta - y_0\\sin\\theta
$$)<br>
![](http://latex.codecogs.com/png.latex?
$$ y 
= r \\cdot \\sin （\\alpha + \\theta）
= r \\cdot\\sin\\alpha\\cos\\theta + r \\cdot\\cos\\alpha\\sin\\theta 
= y_0\\cos\\theta + x_0\\sin\\theta
$$)<br>

用矩阵表示为 : <br>

![](http://latex.codecogs.com/png.latex?
$$
\\left [ 
\\begin{matrix} 
x\\\\
y\\\\
1
\\end{1} 
\\right ] 
 = 
\\left [ 
\\begin{matrix}  
 \\cos\\theta  &  -\\sin\\theta  &  0 \\\\
 \\sin\\theta  &  \\cos\\theta  &  0 \\\\
 0  &  0  &  1
\\end{1} 
\\right ] 
\\left [ 
\\begin{matrix} 
x_0\\\\
y_0\\\\
1
\\end{1} 
\\right ]
$$)

效果如图所示 : <br>
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/14/旋转.png" alt="旋转" title="旋转" />
<br>

### 使用示例

现在我们使用`Matrix`自带的`setRotate`方法 : 
```Java
/**
 *  测试的Matrix操作
 */
mMatrix.setRotate(180);
Log.d("TAG",mMatrix.toString());

// Log
D/TAG: Matrix{[-1.0, -0.0, 0.0][0.0, -1.0, 0.0][0.0, 0.0, 1.0]}
```

<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/14/Matix4.png" alt="Matix" title="Matix" />
<br>

### 5、透视变换

我们在之前的变换中，一直没有说到最后一行的三个参数`MPERSP_0、MPERSP_1、MPERSP_2`，这里我们来稍微聊聊这三个参数所表示的透视。我们一般在图像中的一个点将使用如下方式进行表示(x, y, w),而Android中的二维矩阵计算是基于齐次坐标的，齐次坐标要求w的值为1，所以这个点的表示方法就变化为(x/w, y/w, 1)。<br>
透视变换的效果其实类似于投影机的方式，我们看下w=3时，坐标(15,21,3)的效果 : <br>
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/14/透视1.png" alt="透视" title="透视" />
<br>

<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/14/透视3.png" alt="透视" title="透视" />
<br>

<br>
现在看下(15,21,3)计算出的齐次坐标系坐标(5,7,1)的效果 : <br>

<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/14/透视2.png" alt="透视" title="透视" />
<br>

根据这个规则，也就解释了我们在使用过程中修改`MPERSP_2`参数时，图像会发生的类似缩放的效果，其实就是透视变换的效果。<br>

### 使用示例

现在我们使用`Matrix`自带的`setValues`方法 : 
```Java
/**
 *  测试的Matrix操作
 */
mMatrix.setValues(new float[]{1, 0, 0, 0, 1, 0, 0, 0, 1.5f});
Log.d("TAG",mMatrix.toString());

// Log
D/TAG: Matrix{[1.0, 0.0, 0.0][0.0, 1.0, 0.0][0.0, 0.0, 1.5]}
```

<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/14/Matix5.png" alt="Matix" title="Matix" />
<br>

## 三、Matrix前乘与后乘

Matrix前乘与后乘的情况跟我在之前的文章[ColorMatrix详解](http://www.idtkm.com/customview/cutomview13/)中**ColorMatrix相乘**章节所描述的基本相同。<br>

### 前乘

前乘相当于，当前矩阵乘以输入的矩阵![](http://latex.codecogs.com/png.latex?$$ M' =  M \\cdot S $$)<br>

示例如下:

```Java
mMatrix.reset();
mMatrix.preScale(sx,sy);
mMatrix.preTranslate(dx,dy);
```

用矩阵表示为 : 

![](http://latex.codecogs.com/png.latex?
$$
\\left [ 
\\begin{matrix} 
 & &\\\\
 & Result & Matrix &\\\\
 & &
\\end{1} 
\\right ]  
 = 
\\left [ 
\\begin{matrix} 
 & &\\\\
 & Initial & Matrix &\\\\
 & &
\\end{1} 
\\right ] 
\\left [ 
\\begin{matrix} 
 sx  &  0  &  0 \\\\
 0  &  sy  &  0 \\\\
 0  &  0  &  1
\\end{1} 
\\right ]  
\\left [ 
\\begin{matrix}  
 1  &  0  &  tx \\\\
 0  &  1  &  ty \\\\
 0  &  0  &  1
\\end{1} 
\\right ]
$$)

### 后乘

后乘相当于，输入的矩阵乘以当前矩阵![](http://latex.codecogs.com/png.latex?$$ M' =  S \\cdot M $$)<br>

```Java
mMatrix.reset();
mMatrix.postScale(sx,sy);
mMatrix.postTranslate(dx,dy);
```

![](http://latex.codecogs.com/png.latex?
$$
\\left [ 
\\begin{matrix} 
 & &\\\\
 & Result & Matrix &\\\\
 & &
\\end{1} 
\\right ]  
 = 
\\left [ 
\\begin{matrix} 
 1  &  0  &  tx \\\\
 0  &  1  &  ty \\\\
 0  &  0  &  1
\\end{1} 
\\right ] 
\\left [ 
\\begin{matrix} 
 sx  &  0  &  0 \\\\
 0  &  sy  &  0 \\\\
 0  &  0  &  1
\\end{1} 
\\right ]  
\\left [ 
\\begin{matrix}  
 & &\\\\
 & Initial & Matrix &\\\\
 & &
\\end{1} 
\\right ]
$$)

## 四、总结

本文深入分析了Matrix的原理，并讲解了其常用方法和前后乘的方法。如果在阅读过程中，有任何疑问与问题，欢迎与我联系。<br>
**博客:www.idtkm.com**<br>
**GitHub:https://github.com/Idtk**<br>
**微博:http://weibo.com/Idtk**<br>
**邮箱:IdtkMa@gmail.com**<br>
<br>

## 参考
[Matrix](https://developer.android.com/reference/android/graphics/Matrix.html)<br>
[Android Matrix](http://www.cnblogs.com/qiengo/archive/2012/06/30/2570874.html#code)<br>
[Android中关于矩阵（Matrix）前乘后乘的一些认识](http://blog.csdn.net/linmiansheng/article/details/18820599)<br>
[齐次坐标系入门级思考](https://oncemore2020.github.io/blog/homogeneous/)<br>
[次坐标和投影](http://www.jianshu.com/p/7e701d7bfd79)<br>
[变换矩阵](https://zh.wikipedia.org/wiki/%E5%8F%98%E6%8D%A2%E7%9F%A9%E9%98%B5)<br>
