# 更简单的学习Android事件分发

事件分发是Android中非常重要的机制，是用户与界面交互的基础。这篇文章将通过示例打印出的Log，绘制出事件分发的流程图，让大家更容易的去理解Android的事件分发机制。<br>


## 一、必要的基础知识

### 1、相关方法

Android中与事件分发相关的方法主要包括dispatchTouchEvent、onInterceptTouchEvent、onTouchEvent三个方法，而事件分发一般会经过三种容器，分别为Activity、ViewGroup、View。下表对这三种容器分别拥有的事件分发相关方法进行了整理。

| 事件相关方法 | 方法功能 | Activity | ViewGroup | View |
| :--------------: |:-----------:|-------|-------|-------|
| public boolean dispatchTouchEvent | 事件分发 | Yes | Yes | Yes |
| public boolean onInterceptTouchEvent | 事件拦截 | No | Yes | No |
| public boolean onTouchEvent | 事件消费 | Yes | Yes | Yes |

* 分发: dispatchTouchEvent如果返回true，则表示在当前View或者其子View(子子...View)中，需找到了处理事件的View；反之，则表示没有寻找到
* 拦截: onInterceptTouchEvent如果返回true，则表示这个事件由当前View进行处理，不管处理结果如何，都不会再向子View传递这个事件；反之，则表示当前View不主动处理这个事件，除非他的子View返回的事件分发结果为false
* 消费: onTouchEvent如果返回true，则表示当前View就是事件传递的终点；反之，则表示当前View不是事件传递的终点


<br>

### 2、相关事件

这篇文章中我们只考虑4种触摸事件: ACTION_DOWN、ACTION_UP、ACTION_MOVE、ACTION_CANAL。<br>
事件序列:一个事件序列是指从手指触摸屏幕开始，到手指离开屏幕结束，这个过程中产生的一系列事件。一个事件序列以ACTION_DOWN事件开始，中间可能经过若干个MOVE，以ACTION_UP事件结束。<br>
接下来我们将使用之前的文章[自定义View——弹性滑动](http://www.idtkm.com/customview/customview8/)中例子来作为本文的示例，简单增加一些代码即可，修改之后的代码，请参见本文最后的[附录](https://github.com/Idtk/Blog/tree/master/Code/11)部分。<br>

## 二、示例的默认情况

我们可以从xml中看出，图片都是可点击的，那么我们现在来点击一下，查看下打印出的日志。<br>
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/onTouchTrue.png" alt="onTouchTrue" title="onTouchTrue" />
<br>

根据打印出的log来绘制一张事件传递的流程图<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/onTouchTrueXmind.png" alt="onTouchTrueXmind" title="onTouchTrueXmind" />
<br>

现在来理一下事件序列的流程:
* ACTION_DOWN事件从Activity#dispatchTouchEvent方法开始
* ACTION_DOWN事件传递至ViewGroup#dispatchTouchEvent方法，ViewGroup#onInterceptTouchEvent返回false，表示不拦截ACTION_DOWN
* ACTION_DOWN事件传递到View#dispatchTouchEvent方法，在View#onTouchEvent进行执行，返回true，表示事件已经被消费
* 返回的结果true，被回传到View#dispatchTouchEvent，之后回传到ACTION_DOWN事件的起点Activity#dispatchTouchEvent方法
* ACTION_UP事件的传递过程与ACTION_DOWN相同，这里不再复述

******
通过上面的传递过程，我们可以得出一些结论:
* 事件总是由父元素分发给子元素
* 某个ViewGroup如果onInterceptTouchEvent返回为false，则表示ViewGroup不拦截事件，而是将其传递给View#dispatchTouchEvent方法
* 某个View如果onTouchEvent返回true，表示事件被消费，则其结果将直接通过dispatchTouchEvent方法传递回Activity
* 如果某个View消费了ACTION_DOWN事件，那么这个事件序列中的后续事件也将交由其进行处理(有一些特殊情况除外，比如在序列中的之后事件进行拦截）

## 三、在View中不消费事件

我们现在修改示例中的xml部分，`android:clickable="true"`全部修改为`android:clickable="false"`，这时再点击一下，查看新打印出的日志<br>
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/onTouchFalse.png" alt="onTouchFalse" title="onTouchFalse" />
<br>

现在根据log中显示的逻辑，分别绘制ACTION_DOWN事件与ACTION_UP事件传递的流程图<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/onTouchFalseXmind.png" alt="onTouchFalseXmind" title="onTouchFalseXmind"/>
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/onTouchFalseXmind2.png" alt="onTouchFalseXmind" title="onTouchFalseXmind"/>
<br>

我们来整理下这个事件序列的流程：
* ACTION_DOWN事件的传递与之前相同，不同的地方在于，返回值的传递
* 因为不可点击onTouchEvent的返回值为false，将其传递给自己的dispatchTouchEvent方法，之后传递到ViewGroup#dispatchTouchEvent方法，再传递到ViewGroup#onTouchEvent方法
* ViewGroup返回false之后，ACTION_DOWN事件交由Activity#onTouchEvent方法进行处理，然而依旧返回false，最后ACTION_DOWN事件的返回结果即为false
* ACTION_UP事件在发现View、ViewGroup并不处理ACTION_DOWN事件后，直接将其传递给了Activity#onTouch方法处理，处理返回false，ACTION_UP事件的返回结果即为false

******

通过结合上面两个例子，可以得出一些结论:
* 某个View如果onTouchEvent返回false，表示事件没有被消费，则事件将传递给其父View的onTouchEvent进行处理
* 某个View如果它不消耗ACTION_DOWN事件，那么这个序列的后续事件也不会再交由它来处理
* 如果事件没有View对其进行处理，那么最后将有Activity进行处理
* View默认的onTouchEvent在View可点击的情况下，将会消耗事件，返回true；不可点击的情况下，则不消耗事件，返回false(longClickable的情况，读者可以自行测试，结果与clickable相同)

## 四、在ViewGroup中拦截事件

事件分发中拦截的情况，这里我把它分为2种，一种是在ACTION_DOWN事件时，就进行拦截的；另一种是在ACTION_DOWN之后的事件序列中，对事件进行了拦截。

### 1、在事件开始时拦截

为了达到在ViewGroup中，一开始就拦截触摸事件的效果，我们需要进行修改，在ParentView#onInterceptTouchEvent方法的最后部分，我注释掉的`intercept=true;`进行恢复，然后为activity_main.xml中的ParentView增加`android:clickable="true"`属性。<br>
修改完成后，在此运行点击，查看打印出的log<br>
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/interceptTrue onTouchTrue.png" alt="interceptTrue onTouchTrue" title="interceptTrue onTouchTrue"/>
<br>

我们现在来看下拦截情况下的事件流程图<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/interceptTrue onTouchTrueXmind.png" alt="interceptTrue onTouchTrueXmind" title="interceptTrue onTouchTrueXmind"/>
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/interceptTrue onTouchTrueXmind2.png" alt="interceptTrue onTouchTrueXmind" title="interceptTrue onTouchTrueXmind"/>
<br>

这里大部分和之前的例子相同，主要的区别是在于ViewGroup#onInterceptTouchEvent方法中，对传递的事件进行了拦截，返回true，ACTION_DOWN事件就传递到了ViewGroup#onTouchEvent中进行处理，ACTION_DOWN事件之后的传递就与之前的例子相同了。另一点重要的区别是，在ViewGroup拦截下事件之后，此事件序列的其余事件，在进入ViewGroup#dispatchTouchEvent方法之后，不在需要进行是否拦截事件的判断，而是直接进入了onTouchEvent方法之中。<br>

******
通过上面的例子，可以得出一些结论:
* 某个ViewGroup如果onInterceptTouchEvent返回为true，则ViewGroup拦截事件，将事件传递给其onTouchEvent方法进行处理
* 某个ViewGroup如果它的onInterceptTouchEvent返回为true，那么这个事件序列中的后续事件，不会在进行onInterceptTouchEvent的判断，而是由它的dispatchTouchEvent方法直接传递给onTouchEvent方法进行处理

### 2、在事件序列中拦截

这里把使用的示例恢复到初始状态，然后把我在ParentView#onInterceptTouchEvent方法，switch内的两个注释掉的`intercept = true;`代码进行恢复。重新运行之后，滑动一个图片，来看看Log<br>
<br>
<img src="https://github.com/Idtk/Blog/blob/master/Image/cancel1.png" alt="cancel" title="cancel" width="400"/>
<img src="https://github.com/Idtk/Blog/blob/master/Image/cancel2.png" alt="cancel" title="cancel" width="400"/>
<br>
<br>
这里分成两张图片，是因为中间有很多ACTION_MOVE，这里为了方便观察，所以只截取了Log的首尾部分。<br>
这里的关键部分，就是红框中的ACTION_CANCEL,可以看到ACTION_DOWN事件的传递时onInterceptTouchEvent并没有拦截，返回false，在其后的事件ACTION_MOVE再次进入onInterceptTouchEvent时，ViewGroup对事件进行了拦截，这样将会对View传递一个ACTION_CANCEL事件，之后的ACTION_MOVE事件就不再传递给View了。

******
从这里也可以得出一个结论:
* 某个View接收了ACTION_DOWN之后，这个序列的后续事件中，如果在某一刻被父View拦截了，则这个字View会收到一个ACTION_CANCEL事件，并且也不会再收到这个事件序列中的后续事件。

## 五、小结
本文通过示例打印出的各种Log对Android的事件分发机制进行，得出如下结论。<br>

* 一个事件序列是指从手指触摸屏幕开始，到手指离开屏幕结束，这个过程中产生的一系列事件。一个事件序列以ACTION_DOWN事件开始，中间可能经过若干个MOVE，以ACTION_UP事件结束。
* 事件的传递过程是由外向内的，即事件总是由父元素分发给子元素
* 如果某个View消费了ACTION_DOWN事件，那么通常情况下，这个事件序列中的后续事件也将交由其进行处理，但可以通过调用其父View的onInterceptTouchEvent方法，对后续事件进行拦截
* 如果某个View它不消耗ACTION_DOWN事件，那么这个序列的后续事件也不会再交由它来处理
* 如果事件没有View对其进行处理，那么最后将有Activity进行处理
* 如果事件传递的结果为true，回传的结果直接通过不断调用父View#dispatchTouchEvent方法，传递给Activity；如果事件传递的结果为false，回传的结果不断调用父View#onTouchEvent方法，获取返回结果。
* View默认的onTouchEvent在View可点击的情况下，将会消耗事件，返回true；不可点击的情况下，则不消耗事件，返回false(longClickable的情况，读者可以自行测试，结果与clickable相同)
* 如果某个ViewGroup的onInterceptTouchEvent返回为true，那么这个事件序列中的后续事件，不会在进行onInterceptTouchEvent的判断，而是由它的dispatchTouchEvent方法直接传递给onTouchEvent方法进行处理
* 如果某个View接收了ACTION_DOWN之后，这个序列的后续事件中，在某一刻被父View拦截了，则这个字View会收到一个ACTION_CANCEL事件，并且也不会再收到这个事件序列中的后续事件

<br>

| 事件相关方法 | 方法功能 | Activity | ViewGroup | View |
| :--------------: |:-----------:|-------|-------|-------|
| public boolean dispatchTouchEvent | 事件分发 | Yes | Yes | Yes |
| public boolean onInterceptTouchEvent | 事件拦截 | No | Yes | No |
| public boolean onTouchEvent | 事件消费 | Yes | Yes | Yes |

<br>
如果在阅读过程中，有任何疑问与问题，欢迎与我联系。<br>
**博客:www.idtkm.com**<br>
**GitHub:https://github.com/Idtk**<br>
**微博:http://weibo.com/Idtk**<br>
**邮箱:IdtkMa@gmail.com**<br>
<br>

