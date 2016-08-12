package com.idtk.customscroll;

import android.view.MotionEvent;

/**
 * Created by Idtk on 2016/8/10.
 * Blog : http://www.idtkm.com
 * GitHub : https://github.com/Idtk
 * 描述 : 事件数值转名称
 */
public class ToFlag {
    public static String toFlage(MotionEvent event){
        String flag = "";
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                flag = "ACTION_DOWN";
                break;
            case MotionEvent.ACTION_UP:
                flag = "ACTION_UP";
                break;
            case MotionEvent.ACTION_MOVE:
                flag = "ACTION_MOVE";
                break;
            case MotionEvent.ACTION_CANCEL:
                flag = "ACTION_CANCEL";
                break;
            default:
                flag = event.getAction()+"";
        }
        return flag;
    }
}
