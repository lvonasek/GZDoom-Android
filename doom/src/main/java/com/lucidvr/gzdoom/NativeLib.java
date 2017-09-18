package com.lucidvr.gzdoom;

import android.util.Log;

import com.beloko.libsdl.SDLLib;

class NativeLib {

	static void loadLibraries()
	{

		try {
			Log.i("JNI", "Trying to load libraries");

			SDLLib.loadSDL();
			System.loadLibrary("fmod");
			System.loadLibrary("openal");
			System.loadLibrary("gzdoom");
		}
		catch (UnsatisfiedLinkError ule) {
			Log.e("JNI", "WARNING: Could not load shared library: " + ule.toString());
		}

	}

	public static native void init(int mem,String[] args,int game,String path);

	public static native void keypress(int down, int qkey, int unicode);
	public static native void doAction(int state, int action);
	public static native void analogFwd(float v);
	public static native void analogSide(float v);
	public static native void analogPitch(int mode,float v);
	public static native void analogYaw(int mode,float v);

	static MyGLSurfaceView gv;

	static void swapBuffers()
	{
		boolean canDraw;
		do
		{
			gv.swapBuffers();
			canDraw = gv.setupSurface();
		}while (!canDraw);
	}
}
