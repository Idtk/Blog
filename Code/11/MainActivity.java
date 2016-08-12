package com.idtk.customscroll;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Created by Idtk on 2016/7/17.
 * Blog : http://www.idtkm.com
 * GitHub : https://github.com/Idtk
 */
public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Log.d("TAG","Activity dispatchTouchEvent "+ToFlag.toFlage(ev)+" ");
        boolean result = super.dispatchTouchEvent(ev);
        Log.d("TAG","Activity dispatchTouchEvent return "+result);
        return result;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d("TAG","Activity onTouchEvent,action "+ToFlag.toFlage(event)+" ");
        boolean result = super.onTouchEvent(event);
        Log.d("TAG","Activity onTouchEvent,return "+result+" ");
        return result;
    }
}