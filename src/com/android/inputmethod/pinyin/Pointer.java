/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Author: qiyi_framework
 *
 */
package com.android.inputmethod.pinyin;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.View;
import android.app.Instrumentation;
import android.widget.ImageView;
import android.content.res.Configuration;
import android.content.pm.ActivityInfo;

public class Pointer {
    private final String TAG = "IMEPointer";
    private final boolean DEBUG = true;
    private final int MAX_MOVE_SPAN = 30;
    private final int DEFAULT_X_MARGIN = 1920;
    private final int DEFAULT_Y_MARGIN = 1080;
    private int X_MARGIN = DEFAULT_X_MARGIN;
    private int Y_MARGIN = DEFAULT_Y_MARGIN;
    private WindowManager wM;
    private WindowManager.LayoutParams lP;
    private static final int MSG_UDPATE_VIEW = 1;
    private ImageView ivPointer;
    private Context mContext;
    private int mAxisX,mAxisY;
    private boolean mEnable = false;
    private boolean mReleased = false;
    private boolean mMoveEnable = false;
    private int mXStep=25;
    private int mMoveXStep=10;
    private int mMoveYStep=10;
    private int mYStep=25;
    private int lastPointerX = 0;
    private int lastPointerY = 0;
    private UpdateThread mUpdateThread;
    private int mOrientationLand = Configuration.ORIENTATION_LANDSCAPE;
    public  Pointer(Context context) {
        mContext=context;
    }
    private void  updateScreenOrientation() {

        Configuration mConfiguration = mContext.getResources().getConfiguration();
        int ori = mConfiguration.orientation ;

        if (ori != mOrientationLand) {
            mOrientationLand = ori ;
            if(ori == mConfiguration.ORIENTATION_LANDSCAPE){
                if (DEBUG) Log.d(TAG,"Land scape :" + mConfiguration.screenWidthDp + "," + mConfiguration.screenHeightDp);
                X_MARGIN = DEFAULT_X_MARGIN;
                Y_MARGIN = DEFAULT_Y_MARGIN;
            }else if(ori == mConfiguration.ORIENTATION_PORTRAIT){
                if (DEBUG) Log.d(TAG,"portrait scape :" + mConfiguration.screenWidthDp + "," + mConfiguration.screenHeightDp);
                X_MARGIN = DEFAULT_Y_MARGIN;
                Y_MARGIN = DEFAULT_X_MARGIN;
            }
        }
    }
    public void setup(){
        wM = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        //top view for Pointer;
        //copy from frameworks/base/policy/src/com/android/internal/policy/impl/PhoneWindowManager.java
        lP = new WindowManager.LayoutParams();
        lP.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lP.width = WindowManager.LayoutParams.WRAP_CONTENT;
        //need:<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
        lP.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        //lP.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        //lP.type = WindowManager.LayoutParams.TYPE_TOAST;
        //lP.type = WindowManager.LayoutParams.TYPE_INPUT_METHOD;
        lP.flags = WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                //| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        lP.format = PixelFormat.TRANSLUCENT;
        //lP.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

        //notice ,x=y=0; means center of window;
        lP.x = lastPointerX;
        lP.y = lastPointerY;
        //Pointer image;
        //ivPointer = new ImageView(this.getBaseContext());
        //use Application context ,not active context.
        ivPointer = new ImageView(mContext);
        ivPointer.setImageResource(R.drawable.cursor);
        wM.addView(ivPointer, lP);
        //wM.updateViewLayout(ivPointer, lP);
        //enablePointer(true);
        mReleased = false;
    }
    public void release(){
        if(null != wM && mReleased == false){
            wM.removeView(ivPointer);
            mReleased = true;
        }
        mEnable = false;
    }
    public boolean enablePointer(int keyCode, boolean forceSync) {
        if (keyCode != KeyEvent.KEYCODE_BUTTON_THUMBL) return false;
        if (forceSync) {
            mEnable = false;
        } else {
            mEnable = !mEnable ;
        }
        if (mEnable) {
            if (DEBUG) Log.d(TAG,"enable Pointer");
            setup();
            wM.updateViewLayout(ivPointer, lP);
            //mUpdateThread = new UpdateThread();
            //mUpdateThread.start();
        } else {
            if (DEBUG) Log.d(TAG,"disable Pointer");
            release();
        }
        return false;
    }
    public boolean JoystickMove(float x, float y){
        if (!mEnable) return false;
        updateScreenOrientation() ;
        if (x == 0 && y == 0) {
            if (DEBUG) Log.d(TAG,"reset...");
            mMoveEnable = false;
            mMoveXStep = 0;
            mMoveYStep = 0;
            return true ;
        }
        if (DEBUG) Log.d(TAG,"x-y(update): " + x + "-" + y );
        if (mMoveEnable == true) {
            mMoveXStep = (int)(x * MAX_MOVE_SPAN);
            mMoveYStep = (int)(y * MAX_MOVE_SPAN);
        } else {
            mMoveEnable = true;
            mMoveXStep = (int)(x * MAX_MOVE_SPAN);
            mMoveYStep = (int)(y * MAX_MOVE_SPAN);
            mUpdateThread = new UpdateThread();
            mUpdateThread.start();
        }
        return true;
    }
    public boolean move(int keyCode) {
        if(mEnable) {
            switch (keyCode){
                case KeyEvent.KEYCODE_DPAD_UP:
                if (DEBUG) Log.d(TAG,"up");
                //mAxisY -=mYStep;
                lP.y -=mYStep;
                break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                if (DEBUG) Log.d(TAG,"down");
                lP.y +=mYStep;
                break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                if (DEBUG) Log.d(TAG,"left");
                //mAxisY -=mYStep;
                lP.x -=mXStep;
                break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (DEBUG) Log.d(TAG,"right");
                lP.x +=mXStep;
                break;
                default :
                return false;
            }
            wM.updateViewLayout(ivPointer, lP);
            return true;
        } else {
            return false;
        }
    }
    public boolean touchDown(int keyCode){
        if(keyCode != KeyEvent.KEYCODE_BUTTON_R1 || mEnable ==false) return false;
        new TouchDownThread().start();
        return true;
    }
    public boolean touchUp(int keyCode){
        if(keyCode != KeyEvent.KEYCODE_BUTTON_R1 || mEnable ==false) return false;
        new TouchUpThread().start();
        return true;
    }
    private void setAxis(int x, int y) {
        lP.x = x;
        lP.y = y;
        //updatePointer();
    }
    class UpdateThread extends Thread {
        public void run () {
            while (mMoveEnable){
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Message m = new Message();
                lP.x += mMoveXStep;
                lP.y += mMoveYStep;
                if (lP.x > X_MARGIN) lP.x = X_MARGIN;
                if (lP.y > Y_MARGIN) lP.y = Y_MARGIN;
                if (lP.x < -X_MARGIN) lP.x = -X_MARGIN;
                if (lP.y < -Y_MARGIN) lP.y = -Y_MARGIN;

                if (DEBUG) Log.d(TAG,"x-y : " + lP.x + "-" + lP.y );
                if (DEBUG) Log.d(TAG,"x-y(step) : " + mMoveXStep + "-" + mMoveYStep );
                lastPointerX = lP.x;
                lastPointerY = lP.y;
                m.what = MSG_UDPATE_VIEW;
                mMessageHandler.sendMessage(m);
                
                Instrumentation inst = new Instrumentation();

                if (DEBUG) Log.d(TAG,"touchMove on :" + lP.x + "," + lP.y);
                inst.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),
                                        MotionEvent.ACTION_MOVE, lP.x + X_MARGIN, lP.y + Y_MARGIN, 0));
            }
        }
    }
    class TouchDownThread extends Thread {
        public void run () {
            Instrumentation inst = new Instrumentation();

            if (DEBUG) Log.d(TAG,"touchDown on :" + lP.x + "," + lP.y);
            inst.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),
                MotionEvent.ACTION_DOWN, lP.x + X_MARGIN, lP.y + Y_MARGIN, 0));
        }
    }
    class TouchUpThread extends Thread {
        public void run () {
            Instrumentation inst = new Instrumentation();

            if (DEBUG) Log.d(TAG,"touchUp on:" + lP.x + "," + lP.y);
            inst.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),
                MotionEvent.ACTION_UP, lP.x + X_MARGIN, lP.y + Y_MARGIN, 0));
        }
    }
    Handler mMessageHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UDPATE_VIEW:
                if (mEnable == true) {
                    if(DEBUG) Log.d(TAG,"update cursor" + lP.x + "," + lP.y);
                    wM.updateViewLayout(ivPointer, lP);

                }else{
                    /*lP.x=4000;
                    lP.y=4000;
                    wM.updateViewLayout(ivPointer, lP);*/
                }
                break ;
            }
            super.handleMessage(msg);
        }
    };

}
