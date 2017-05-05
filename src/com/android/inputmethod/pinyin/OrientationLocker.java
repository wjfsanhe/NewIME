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
 * See the License for the specific language governing 
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
import android.widget.LinearLayout;
import android.content.res.Configuration;
import android.content.pm.ActivityInfo;

public class OrientationLocker {
        private final String TAG = "IMEOriLocker";
        private Context mContext;
        private WindowManager wm;
        private LinearLayout mLayout;
        
        public OrientationLocker(Context ctx) {
                mContext = ctx;
        }
        public void enable() {

                mLayout = new LinearLayout(mContext);
                // need the permission android.permission.SYSTEM_ALERT_WINDOW
                WindowManager.LayoutParams orientationLayout = new WindowManager.LayoutParams(WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY, 0, PixelFormat.RGBA_8888);
                
                orientationLayout.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

                wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
                wm.addView(mLayout, orientationLayout);
                mLayout.setVisibility(View.VISIBLE);
        }
        public void disable() {
                if (null != wm)
                wm.removeView(mLayout);
        }

}
