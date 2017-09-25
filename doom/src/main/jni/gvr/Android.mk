LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := gvr
LOCAL_SRC_FILES := lib/armeabi-v7a/libgvr.so

include $(PREBUILT_SHARED_LIBRARY)
