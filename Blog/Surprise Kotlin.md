# 令人惊喜的Kotlin特性

本文并不准备去详细说明Kotlin的AS配置以及基本的语法，而是介绍一下在使用过程中让我惊喜的Kotlin特性，希望让更多的人喜欢上这个语言。



## 和findViewById说再见

第一个要说的应该是很多已经使用了kotlin的同学都体会到的特性。

假设当前Activity对于的布局为如下所示：

```xml
<LinearLayout
        xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:tools="http://schemas.android.com/tools"
        android:id="@+id/activity_main"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical">
    <TextView
        android:id="@+id/main_tv"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Hello Java!"/>
</LinearLayout>
```

我们一般会使用如下两个方法之一：

```java
findViewById(R.id.main_tv);
//or
@BindView(R.id.main_tv)
Button mMainTv;
```

不过现在我们可以直接使用对应的id，并且import对应的文件：

```kotlin
main_tv.text = "Hello Kotlin!"
import kotlinx.android.synthetic.main.activity_main.*
```

如果是Adapter的item的xml文件，相应的可以如下这样使用：

```kotlin
holder!!.itemView.time_item_tv.text = "${date}"
import kotlinx.android.synthetic.main.item_time.view.*
```

虽然这样直接把view的id命名放在类文件中有些与其余变量的驼峰式命名风格不太统一，但是其在代码量上的减少以及代码追踪跳转时的便捷，让我们有足够的理由来使用它。



## 我们都是final的

读过《Effective Java》的同学，应该都记得其中对final使用的说明，我们在创建非基类和对应方法时，都应该对其加上final关键字，但是在实际使用过程中很多人却很少可以做到这一点。

让人惊喜的是kotlin的类和方法默认情况下就是final的，像如下这样的情况是无法通过编译的：

```kotlin
class ClassA{
    fun function(){
    }
}
// ClassB 与 function都会报错
// This type is final, so it cannot be inherited from
// 'function' in 'ClassA' is final and cannot be overridden
class ClassB:ClassA(){
    override fun function() {
        super.function()
    }
}
```

如果要让其可以继承，需要在相应的位置增加open关键字，就像下面这样：

```kotlin
open class ClassA{
    open fun function(){
        
    }
}
```



## 没有外部指针的内部类

刚开始学习Android的时候，很多同学都或多或少的遇到过因为内部类而引起的内存泄漏的情况吧。比如Handler就是如此，其内部持有了Activity的引用，而可能在一定情况下会引起内存泄漏，当然解决办法也众所周知的，静态内部类+弱引用。

Kotlin在这点上对于初学者是非常友好的，其内部类在默认情况下并没有持有外部类的引用，类似于Java中的静态内部类。

```kotlin
class Outer(){
    var outer = "outer"
    class Inner{
        // 这里无法引用到outer，
        // 因为Inner并没有持有Outer的引用
        // var inner = outer
    }
}
```

如果内部类需要使用外部类的属性或者方法，一种是传参，还有一中就是使用inner关键字。

```kotlin
class Outer(){
    var outer = "outer"
    inner class Inner{
        // 这里可以使用外部类的outer属性
        var inner = outer
    }
}
```



## 函数的默认参数

我在用了Java之后就一直心心念念之前用C++和Python时候的函数默认参数，而Kotlin满足了我这个期望。使用带有默认参数的函数时，默认参数被忽略会直接使用默认值，这样可以减少重载方法的定义。

```kotlin
fun imageProxy(id:Int,context:Context = BaseApplication.getInstance()){
}
```



## when

Kotlin中when的出现替代了switch，但其功能更加强大。

```kotlin
fun case(obj:Any){
    when(obj){
        1-> print("1")
        in 2..16 -> print("Child")
        is Long -> print("Long")
        sumLambda -> print("sumLambda")
        else -> print("No")
    }
}
```

有木有感到很厉害！



## 字符串

Kotlin在字符串之中可以使用变量，相对与在Java中的字符串拼接，更然人感觉到舒服。

```kotlin
var name = "Idtk"
var hello = "你好,$name !"
```



## Lambda

Kotlin可以这样很简单的写出一个匿名函数：

```kotlin
val sumLambda = {x: Int, y: Int -> x + y}
```

当然更有意义的是你还可以像这样定义一个变量：

```kotlin
var twoNum: (x:Int,y:Int) -> Int
```

看上去是不是有点像C++的函数指针。

## 高阶函数

Kotlin是可以支持函数式编程的，比较明显的一个特征就是，一个函数可以作为另一个函数的输入和输出。

现在来把上面提到的twoNum当作一个函数参数来使用：

```kotlin
fun numFun(a:Int,b:Int,twoNum: (x:Int,y:Int) -> Int):Int{
    return twoNum(a,b)
}
```

使用起来也很简单，只需传入的函数满足twoNum的签名即可：

```kotlin
var sum = numFun(1,2,sumLambda)
```

上面说了输入，现在再来说说输出，Kotlin可以支持函数作为返回值：

```kotlin
// Function2是Kotlin中定义的一个接口
// public interface Function2<in P1, in P2, out R> : Function<R>
fun add(a: Int, b: Int): Function2<Int, Int, Int> {
    return sumLambda
}
```

## 总结

当然Kotlin的特性远不止我上面说到的这些，还有拓展函数、null安全等等，而正是它的这些特性大大的提升了我们的开发效率和代码的安全性。现在，你看完上面这些有趣的特性之后，不想马上来试一下Kotlin嘛？

如果在阅读过程中，有任何疑问与问题，欢迎与我联系。<br>
**博客:www.idtkm.com**<br>
**GitHub:https://github.com/Idtk**<br>
**微博:http://weibo.com/Idtk**<br>
**邮箱:IdtkMa@gmail.com**<br>
<br>