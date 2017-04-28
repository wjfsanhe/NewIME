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
import android.widget.ImageView;


public class Pointer {
    private final String TAG = "IMEPointer";
    private WindowManager wM;  
    private WindowManager.LayoutParams lP;  
    private static final int MSG_UDPATE_VIEW = 1;
    private ImageView ivPointer;
    private Context mContext;  
    private int mAxisX,mAxisY;
    private boolean mEnable = false;
    private int mXStep=25;
    private int mYStep=25;
    private UpdateThread mUpdateThread; 
    public  Pointer(Context context) {
        mContext=context;
    }
    public void setup(){
        wM = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);  
        //top view for Pointer;  
        //copy from frameworks/base/policy/src/com/android/internal/policy/impl/PhoneWindowManager.java  
        lP = new WindowManager.LayoutParams();  
        lP.height = WindowManager.LayoutParams.WRAP_CONTENT;  
        lP.width = WindowManager.LayoutParams.WRAP_CONTENT;  
        //need:<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />  
        //lP.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;  
        lP.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;  
        lP.flags = WindowManager.LayoutParams.FLAG_FULLSCREEN  
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE  
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE  
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        lP.format = PixelFormat.TRANSLUCENT;  
        //notice ,x=y=0; means center of window;  
        lP.x = 0;  
        lP.y = 0;  
        //Pointer image;  
        //ivPointer = new ImageView(this.getBaseContext());  
        //use Application context ,not active context.
        ivPointer = new ImageView(mContext);  
        ivPointer.setImageResource(R.drawable.cursor);  
        wM.addView(ivPointer, lP);  
        //wM.updateViewLayout(ivPointer, lP);
        //enablePointer(true);
        
    }
    public void release(){
        if(null != wM){
            wM.removeView(ivPointer);
        }
        mEnable = false;
    }
    public boolean enablePointer(int keyCode) {
        if (keyCode != KeyEvent.KEYCODE_BUTTON_THUMBL) return false; 
        mEnable = !mEnable ;
        if (mEnable) {
            Log.d(TAG,"enable Pointer");
            setup();
            wM.updateViewLayout(ivPointer, lP);
            //mUpdateThread = new UpdateThread();
            //mUpdateThread.start();
        } else {
            Log.d(TAG,"disable Pointer");
            release();
        }
        return true;      
    }
    public boolean move(int keyCode) {
        if(mEnable) {
            switch (keyCode){
                case KeyEvent.KEYCODE_DPAD_UP:
                Log.d(TAG,"up");
                //mAxisY -=mYStep;
                lP.y -=mYStep;
                break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                Log.d(TAG,"down");
                lP.y +=mYStep;
                break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                Log.d(TAG,"left");
                //mAxisY -=mYStep;
                lP.x -=mXStep;
                break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                Log.d(TAG,"right");
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
    private void setAxis(int x, int y) {
        lP.x = x;
        lP.y = y;
        //updatePointer();
    }
    class UpdateThread extends Thread {
        public void run () {
            while (mEnable){
                try {
                    Thread.sleep(10); 
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Message m = new Message();
                m.what = MSG_UDPATE_VIEW;
                mMessageHandler.sendMessage(m);
            }
        }
    }
    Handler mMessageHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UDPATE_VIEW:
                if (mEnable == true) {
                    Log.d(TAG,"update cursor" + lP.x + "," + lP.y);
                    setAxis(mAxisX,mAxisY);
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
