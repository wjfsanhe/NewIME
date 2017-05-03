LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
         $(call all-subdir-java-files) \
         com/android/inputmethod/pinyin/IPinyinDecoderService.aidl \
         com/android/qiyicontroller/AIDLController.aidl \
         com/android/qiyicontroller/AIDLListener.aidl

LOCAL_MODULE := com.android.inputmethod.pinyin.lib

include $(BUILD_STATIC_JAVA_LIBRARY)
