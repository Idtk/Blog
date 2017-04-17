# RecyclerView缓存简单分析
# 缓存介绍

```Java
public final class Recycler {
        final ArrayList<ViewHolder> mAttachedScrap = new ArrayList<>();
        ArrayList<ViewHolder> mChangedScrap = null;

        final ArrayList<ViewHolder> mCachedViews = new ArrayList<ViewHolder>();
		// mAttachedScrap的不可变视图
        private final List<ViewHolder>
                mUnmodifiableAttachedScrap = Collections.unmodifiableList(mAttachedScrap);
		// 应保留的缓存数
        private int mRequestedCacheMax = DEFAULT_CACHE_SIZE;
		// 最大缓存数
        int mViewCacheMax = DEFAULT_CACHE_SIZE;

        RecycledViewPool mRecyclerPool;

        private ViewCacheExtension mViewCacheExtension;

        static final int DEFAULT_CACHE_SIZE = 2;
}
```

* mAttachedScrap 未与RecyclerView分离的ViewHolder列表
* mChangedScrap RecyclerView中需要改变的ViewHolder列表
* mCachedViews RecyclerView的ViewHolder缓存列表(即一级缓存)
* mViewCacheExtension 用户设置的RecyclerView的ViewHolder缓存列表扩展(即二级缓存)
* mRecyclerPool RecyclerView的ViewHolder缓存池(即三级缓存)

# 获取缓存
# 创建缓存