package com.lucidvr.gzdoom;

import android.content.Context;

class NativeLib {

	public static native void createRenderer(ClassLoader appClassLoader, Context context, long gvr);
	public static native void init(int mem,String[] args,int game,String path);
	public static native void initGL();
	public static native boolean loop();

	public static native void keypress(int down, int qkey, int unicode);
	public static native void doAction(int state, int action);
	public static native void analogFwd(float v);
	public static native void analogSide(float v);
	public static native void analogPitch(int mode,float v);
	public static native void analogYaw(int mode,float v);
}
