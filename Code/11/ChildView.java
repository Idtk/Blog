package com.idtk.customscroll;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

/**
 * Created by Idtk on 2016/8/2.
 * Blog : http://www.idtkm.com
 * GitHub : https://github.com/Idtk
 * 描述 : 继承自ImageView，用于测试
 */
public class ChildView extends ImageView {
    public ChildView(Context context) {
        super(context);
    }

    public ChildView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ChildView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        Log.w("TAG","View dispatchTouchEvent,action "+ToFlag.toFlage(event)+" ");
        boolean result = super.dispatchTouchEvent(event);
        Log.w("TAG","View dispatchTouchEvent return "+result);
        return result;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.w("TAG","View onTouchEvent,action "+ToFlag.toFlage(event)+" ");
        boolean result = super.onTouchEvent(event);
        Log.w("TAG","View onTouchEvent,return "+result+" ");
        return result;
    }
}
