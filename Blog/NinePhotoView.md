# 自定义九宫格控件（附源码地址）

在阅读本文之前，需要你对View的绘制有一定的了解，如果不了解的可以 看下我之前的文章——[自定义View系列目录](https://github.com/Idtk/Blog)<br>

最近公司在做个类似朋友圈的功能，需要一个九宫格控件，因为算是个常用控件，所以自己撸了一个。<br>

## 职责分解

<br>
<img src="http://ompb0h8qq.bkt.clouddn.com/NinePhotoView/IKNinePhotoView.png" title="NinePhotoView" width="300"/><br>

上图是一个九宫格控件承载图片的显示情况，九宫格控件因为其需要承载其内部的View，所以应该是一个ViewGroup。每个子View基本上是相同的，但显示的内容不同，正好最近在看RecyclerView，因此也就想到了可以使用Adapter来个性化每个子View，正好通过Adapter模式来适配了子View与ViewGroup。最后为了防止在创建、显示子View时的冗余操作，应该给子View增加一些属性，选用ViewHolder正好可以完成这个要求，这样也更方便以后的扩展。<br>

**ViewGroup、Adapter、ViewHolder三者的具体职责**<br>

<table style="border-collapse:collapse">
   <tr>
      <th rowspan=6 >功能</th>
   </tr>
   <tr>
      <th>ViewGroup</th>
      <th>Adapter</th>
	  <th>ViewHolder</t>
   </tr>
   <tr>
      <td>Measure子View</td>
      <td>创建子View布局</td>
	  <td>子View的获取</td>
   </tr>
   <tr>
      <td>Layout子View</td>
      <td>子View的内容display</td>
	  <td>display子View标志</td>
   </tr>
   <tr>
      <td>add子View</td>
      <td>子view数量</td>
      <td></td>
   </tr>
   <tr>
      <td>回收缓存</td>
      <td>数据变化通知</td>
	  <td></td>
   </tr>
</table>

<br>

## ChildView的测量

ChildView测量所需要的数据主要来自于ParentView的测量数据，这里就是我们之前说的ViewGroup的`onMeasure`方法。
```Java
/**
 * 在这之前已经对childView的数量小于等于0或大于9的情况进行了处理
 * 这里讲显示情况分成三种进行测量：
 * 1、ChildView数量为1
 * 2、ChildView数量为2或4
 * 3、ChildView数量的剩余类型
 */
if (adapter.getItemCount() > 1) {
    childSize = (width - border * 2) / 3;
    height = (int) (childSize * (int) Math.ceil(adapter.getItemCount() / 3.0) + border * (int) Math.ceil(adapter.getItemCount() / 3.0
    if (adapter.getItemCount() == 4 || adapter.getItemCount() == 2) {
        int currentWidth = childSize*2 + border;
        setMeasuredDimension(currentWidth + getPaddingLeft() + getPaddingRight(), height + getPaddingTop() + getPaddingBottom());
    }else {
        int currentWidth = childSize*3 + border*2;
        setMeasuredDimension(currentWidth + getPaddingLeft() + getPaddingRight(), height + getPaddingTop() + getPaddingBottom());
    }
} else {
    childSize = width/3;
    height = width/3;
    setMeasuredDimension(width + getPaddingLeft() + getPaddingRight(), height + getPaddingTop() + getPaddingBottom());
}
```

## ChildView的添加
在测量ChildView之后，将使用`addView`方法将其添加到ViewGroup中。
```Java
/**
 * 在增加ChildView之前，需要先行清除已经添加的所有ChildView
 */
removeAllViews();
for (int i = 0; i < adapter.getItemCount(); i++) {
    addView(generateViewHolder(i).getItemView(),generateDefaultLayoutParams());
}
```

### ChildView的回收

generateViewHolder是用于获取ChildView的方法，其中有一个简单的缓存列表

```Java
/**
 * 当需要添加的ChildView的position小于缓存列表大小时，直接从缓存列表中获取ChildView
 * 否则，则调用adapter.createView方法，创建一个新的ViewHolder，同时将其添加到缓存列表中
 */
private IKNinePhotoViewHolder generateViewHolder(int position){
    if (position < mRecyclerList.size()) {
        return mRecyclerList.get(position);
    } else {
        if (adapter != null){
            IKNinePhotoViewHolder holder = adapter.createView(IKNinePhotoView.this);
            if (holder == null){
                return null;
            }
            mRecyclerList.add(holder);
            return holder;
        } else
            return null;
    }
}
```

## ChildView的布局

在完成ChildView的测量和添加之后，需要对ChildView的位置进行确定。而这些数值来自ParentView的`onLayout`方法。
```Java
/**
 * 先对ChildView数量为4的特殊情况，进行处理，将其的列数确定为2。
 */
int count = adapter.getItemCount();
int colNum = 3;
if (count == 4){
    colNum = 2;
}

/**
 * 便利每个ChildView，对其进行布局
 */
for (int i = 0; i < count; i++) {
    View childView = getChildAt(i);
    if (childView == null){
        return;
    }
    if (adapter != null && mRecyclerList.get(i) != null &&!mRecyclerList.get(i).getFlag()) {
        adapter.displayView(generateViewHolder(i), i);
		// 设置这个标志，表示此ViewHolder中包含的ChildView已经完成了布局，防止多余的操作
        mRecyclerList.get(i).setFlag(true);
    }
	// 之前设置的列数
    int rows = i / colNum;
    int cols = i % colNum;
    int childLeft = getPaddingLeft() + (childSize + border) * (cols);
    int childTop = getPaddingTop() + (childSize + border) * (rows);
    int childRight = childLeft + childSize;
    int childBottom = childTop + childSize;
    childView.layout(childLeft, childTop, childRight, childBottom);
}
```

## ChildView数据的更新

到这里已经基本上完成了九宫格控件的编写，最后需要再添加一个数据更新的功能，以完备其功能。这里使用观察者模式的来实现这个功能，Adapter继承Observable类、ViewGroup实现Observer接口。
```Java
// 在Adapter中
public void notifyChanged(){
    setChanged();
    notifyObservers();
}

// 在ViewGroup中

@Override
public void update(Observable o, Object arg) {
    if (o instanceof IKNinePhotoViewAdapter){
        this.adapter = (IKNinePhotoViewAdapter) o;
        adapter.addObserver(this);
        for(IKNinePhotoViewHolder holder: mRecyclerList){
            holder.setFlag(false);
        }
        requestLayout();
        invalidate();
    }
}

```

## 总结

本文详细说明了九宫格控件的实现方法，并对其功能进行了分解。分解成多个组件之后，不仅符合程序的单一性原则，更加易于程序的功能扩展，比如添加监听事件，使用不同的图片库去显示图片，同时九宫格的ChildView也不仅限于ImageView，而可以扩展到全部的View。如果在阅读过程中，有任何疑问与问题，欢迎与我联系。<br>

**博客:www.idtkm.com**<br>
**GitHub:https://github.com/Idtk**<br>
**微博:http://weibo.com/Idtk**<br>
**邮箱:IdtkMa@gmail.com**<br>
<br>

[九宫格控件源码请点击](https://github.com/Idtk/IKNinePhotoView)


