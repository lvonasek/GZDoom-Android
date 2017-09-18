package com.lucidvr.gzdoom;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;


/**
    SDL Activity
 */
class SDLAudio
{

	private static final Object threadLock = new Object();
	private static boolean resumed = false;
	
	static void loadSDL()
	{

		try {
			Log.i("JNI", "Trying to load SDL.so");

			System.loadLibrary("SDL");
			System.loadLibrary("SDL_mixer");
			System.loadLibrary("SDL_image");
		}
		catch (UnsatisfiedLinkError ule) {
			Log.e("JNI", "WARNING: Could not load SDL.so: " + ule.toString());
		}
	}

	// Audio
	private static Thread mAudioThread;
	private static AudioTrack mAudioTrack;

	// C functions we call
	public static native void nativeInit(boolean launch);
	public static native void nativeQuit();
	public static native void nativePause();
	public static native void nativeResume();
	public static native void onNativeResize(int x, int y, int format);
	public static native void onNativeKeyDown(int keycode);
	public static native void onNativeKeyUp(int keycode);
	public static native void onNativeTouch(int touchDevId, int pointerFingerId,
			int action, float x, 
			float y, float p);
	public static native void onNativeAccel(float x, float y, float z);
	public static native void nativeRunAudioThread();


	// Java functions called from C

	public static boolean createGLContext(int majorVersion, int minorVersion) {
		return true;
	}

	public static void flipBuffers() {

	}

	public static void setActivityTitle(String title) {

	}

	// Audio
	static void onPause()
	{
		resumed = false;
	}
	
	static void onResume()
	{	
		resumed = true;
		synchronized (threadLock){
			threadLock.notifyAll();
		}	
	}

	public static Object audioInit(int sampleRate, boolean is16Bit, boolean isStereo, int desiredFrames) {
		int channelConfig = isStereo ? AudioFormat.CHANNEL_CONFIGURATION_STEREO : AudioFormat.CHANNEL_CONFIGURATION_MONO;
		int audioFormat = is16Bit ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
		int frameSize = (isStereo ? 2 : 1) * (is16Bit ? 2 : 1);

		Log.v("SDL", "SDL audio: wanted " + (isStereo ? "stereo" : "mono") + " " + (is16Bit ? "16-bit" : "8-bit") + " " + ((float)sampleRate / 1000f) + "kHz, " + desiredFrames + " frames buffer");

		// Let the user pick a larger buffer if they really want -- but ye
		// gods they probably shouldn't, the minimums are horrifyingly high
		// latency already
		desiredFrames = Math.max(desiredFrames, (AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat) + frameSize - 1) / frameSize);

		mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
				channelConfig, audioFormat, desiredFrames * frameSize, AudioTrack.MODE_STREAM);

		audioStartThread();

		Log.v("SDL", "SDL audio: got " + ((mAudioTrack.getChannelCount() >= 2) ? "stereo" : "mono") + " " + ((mAudioTrack.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT) ? "16-bit" : "8-bit") + " " + ((float)mAudioTrack.getSampleRate() / 1000f) + "kHz, " + desiredFrames + " frames buffer");

		Object buf;
		if (is16Bit) {
			buf = new short[desiredFrames * (isStereo ? 2 : 1)];
		} else {
			buf = new byte[desiredFrames * (isStereo ? 2 : 1)]; 
		}
		return buf;
	}

	private static void audioStartThread() {
		mAudioThread = new Thread(new Runnable() {
			public void run() {
				mAudioTrack.play();
				nativeRunAudioThread();
			}
		});

		// I'd take REALTIME if I could get it!
		mAudioThread.setPriority(Thread.MAX_PRIORITY);
		mAudioThread.start();
	}

	public static void audioWriteShortBuffer(short[] buffer) {
		if (!resumed)
			synchronized (threadLock) {
				try {
					if (mAudioTrack != null)
						mAudioTrack.pause();
					threadLock.wait();
					if (mAudioTrack != null)
						mAudioTrack.play();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		
		for (int i = 0; i < buffer.length; ) {
			int result = mAudioTrack.write(buffer, i, buffer.length - i);
			if (result > 0) {
				i += result;
			} else if (result == 0) {
				try {
					Thread.sleep(1);
				} catch(InterruptedException e) {
					// Nom nom
				}
			} else {
				Log.w("SDL", "SDL audio: error return from write(short)");
				return;
			}
		}
	}

	public static void audioWriteByteBuffer(byte[] buffer) {
		if (!resumed)
			synchronized (threadLock) {
				try {
					if (mAudioTrack != null)
						mAudioTrack.pause();
					threadLock.wait();
					if (mAudioTrack != null)
						mAudioTrack.play();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		for (int i = 0; i < buffer.length; ) {
			int result = mAudioTrack.write(buffer, i, buffer.length - i);
			if (result > 0) {
				i += result;
			} else if (result == 0) {
				try {
					Thread.sleep(1);
				} catch(InterruptedException e) {
					// Nom nom
				}
			} else {
				Log.w("SDL", "SDL audio: error return from write(short)");
				return;
			}
		}
	}

	public static void audioQuit() {
		if (mAudioThread != null) {
			try {
				mAudioThread.join();
			} catch(Exception e) {
				Log.v("SDL", "Problem stopping audio thread: " + e);
			}
			mAudioThread = null;

			//Log.v("SDL", "Finished waiting for audio thread");
		}

		if (mAudioTrack != null) {
			mAudioTrack.stop();
			mAudioTrack = null;
		}
	}
}
