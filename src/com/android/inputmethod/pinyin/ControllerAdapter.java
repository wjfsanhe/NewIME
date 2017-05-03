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

import android.content.ComponentName;
import com.android.qiyicontroller.AIDLController;
import com.android.qiyicontroller.AIDLListener;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;


public class ControllerAdapter {
    private final static String TAG = "IMEController";
    private final boolean Debug = true;
    private AIDLController mController;
    private Context mContext;

    public ControllerAdapter(Context ctx) {
            mContext = ctx;
    }
    private void Log(String str) {
        if (Debug) Log.d(TAG,str);
    }

    private AIDLListener mCallbackListener = new AIDLListener.Stub() {
            //here we fill all callback module.
            public void quansDataEvent(float x, float y, float z, float w) throws RemoteException
            {
                    Log("<IN>[x,y,z,w]:" + x + " ," + y + " ," + w);
            }
            public void shortClickBackEvent(int state) throws android.os.RemoteException {};
            public void clickAppButtonEvent(int state) throws android.os.RemoteException {};
            public void clickAndTriggerEvent(int state) throws android.os.RemoteException {};
            public void batterLevelEvent(int level) throws android.os.RemoteException {};
            public void shakeEvent(int timeStamp, int event, int eventParameter) throws android.os.RemoteException {};
            public void longClickHomeEvent(int state) throws android.os.RemoteException {};
            public void gyroDataEvent(float x, float y, float z) throws android.os.RemoteException {};
            public void accelDataEvent(float x, float y, float z) throws android.os.RemoteException {};
            public void touchDataEvent(float x, float y) throws android.os.RemoteException {};
            public void handDeviceVersionInfoEvent(int appVersion, int deviceVersion, int deviceType) throws android.os.RemoteException {};

    };
    private ServiceConnection mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                            IBinder service) {
                    Log("Controller service connected");
                    mController = AIDLController.Stub.asInterface(service);
                    try {
                            //register callback here.
                            mController.registerListener(mCallbackListener);
                    } catch (RemoteException e) {
                            e.printStackTrace() ;
                    }
            }
            public void onServiceDisconnected(ComponentName className) {
                    Log("Controller disconnect service");
                    mController = null;
            }
    };

    public void StopAdapter() {
            Log("Stop Controller");
            mContext.unbindService(mConnection);
    }
    public void StartAdapter() {
            Log("Start Controller");
            Bundle args = new Bundle();
            Intent intent = new Intent("com.android.qiyicontroller.AIDLControllerService");
            intent.setPackage("com.google.vr.vrcore");
            intent.setAction("com.android.qiyicontroller.BIND");
            
            //intent.putExtras(args);
            mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            mContext.startService(intent);
    }
}


