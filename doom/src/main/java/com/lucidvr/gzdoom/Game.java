package com.lucidvr.gzdoom;

import android.app.Activity;
import android.os.Bundle;

import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;
import com.google.vr.sdk.controller.Controller;
import com.google.vr.sdk.controller.ControllerManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import javax.microedition.khronos.egl.EGLConfig;

public class Game extends Activity implements GvrView.StereoRenderer
{
  static
  {
    SDLAudio.loadSDL();
    System.loadLibrary("fmod");
    System.loadLibrary("openal");
    System.loadLibrary("gzdoom");
  }


  static native void init(int mem,String[] args,int game,String path);
  static native boolean loop();
  static native void keypress(int down, int qkey, int unicode);
  static native void doAction(int state, int action);
  static native void analogFwd(float v);
  static native void analogSide(float v);
  static native void analogPitch(int mode,float v);
  static native void analogYaw(int mode,float v);

  private String args;
  private String gamePath;
  private boolean doomInit = false;

  private ControllerManager controllerManager;
  private HeadTransform mHead;

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    args = getIntent().getStringExtra("args");
    gamePath = getIntent().getStringExtra("game_path");

    //set GVR
    GvrView gvrView = new com.google.vr.sdk.base.GvrView(this);
    gvrView.setEGLConfigChooser(8, 8, 8, 8, 24, 8);
    gvrView.setEGLContextClientVersion(1); //TODO:GLES2
    gvrView.setRenderer(this);
    gvrView.setTransitionViewEnabled(true);
    setContentView(gvrView);

    AndroidCompat.setVrModeEnabled(this, true);
  }

  @Override
  protected void onPause()
  {
    SDLAudio.nativePause();
    SDLAudio.onPause();
    super.onPause();
  }

  @Override
  protected void onResume()
  {
    SDLAudio.onResume();
    SDLAudio.nativeResume();
    super.onResume();
  }


  @Override
  protected void onDestroy()
  {
    super.onDestroy();
    System.exit(0);
  }

  @Override
  protected void onStart() {
    super.onStart();
    controllerManager = new ControllerManager(this, new ControllerManager.EventListener()
    {
      @Override
      public void onApiStatusChanged(int i)
      {
      }

      @Override
      public void onRecentered()
      {
        int code = 0x205;
        doAction(1, code);
        doAction(0, code);
      }
    });
    controllerManager.start();
  }

  @Override
  protected void onStop() {
    controllerManager.stop();
    controllerManager = null;
    super.onStop();
  }

  @Override
  public void onNewFrame(HeadTransform headTransform)
  {
    mHead = headTransform;
  }

  @Override
  public void onDrawEye(Eye eye)
  {
    Viewport viewport = eye.getViewport();
    //TODO:move rendering here when it will be compatible with GLES2
  }

  @Override
  public void onFinishFrame(Viewport viewport)
  {
    if (!doomInit)
    {
      //init doom
      SDLAudio.nativeInit(false);
      args += "-width " + (viewport.width / 2) + " -height " + viewport.height;
      ArrayList<String> a = new ArrayList<>(Arrays.asList(args.split(" ")));
      Iterator<String> iter = a.iterator();
      while (iter.hasNext())
        if (iter.next().contentEquals(""))
          iter.remove();
      init(48000, a.toArray(new String[a.size()]), 0, gamePath);
      doomInit = true;
    }
    else
    {
      //update VR state
      runOnUiThread(new Runnable()
      {
        @Override
        public void run()
        {
          //get head status
          float[] angles = new float[3];
          mHead.getEulerAngles(angles, 0);
          //update controller
          if (controllerManager != null)
          {
            Controller controller = controllerManager.getController();
            controller.update();
            VRController.update(controller, angles);
          }
        }
      });

      //render
      if (!loop())
        finish();
    }
  }

  @Override
  public void onSurfaceChanged(int width, int height)
  {
  }

  @Override
  public void onSurfaceCreated(EGLConfig eglConfig)
  {
  }

  @Override
  public void onRendererShutdown()
  {
  }
}
