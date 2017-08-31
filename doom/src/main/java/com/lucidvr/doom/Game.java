/*
 * Copyright (C) 2009 jeyries@yahoo.fr
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
 */
package com.lucidvr.doom;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

import com.bda.controller.Controller;
import com.bda.controller.ControllerListener;
import com.bda.controller.StateEvent;
import com.beloko.libsdl.SDLLib;
import com.beloko.touchcontrols.ControlInterpreter;
import com.beloko.touchcontrols.MogaHack;
import com.beloko.touchcontrols.ShowKeyboard;
import com.beloko.touchcontrols.TouchControlsEditing;
import com.beloko.touchcontrols.TouchControlsSettings;
import com.beloko.touchcontrols.TouchSettings;
import com.lucidvr.sdk.AbstractActivity;
import com.lucidvr.sdk.DaydreamController;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class Game extends AbstractActivity implements Handler.Callback
{
  String LOG = "Game";

  private ControlInterpreter controlInterp;

  private final MogaControllerListener mogaListener = new MogaControllerListener();
  Controller mogaController = null;

  private String args;
  private String gamePath;

  private GameView mGLSurfaceView = null;
  private DoomRenderer mRenderer = new DoomRenderer();
  private boolean mInitialized = false;

  Activity act;

  int surfaceWidth = -1, surfaceHeight;

  private Handler handlerUI;

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    act = this;

    handlerUI = new Handler(this);

    args = getIntent().getStringExtra("args");
    gamePath = getIntent().getStringExtra("game_path");

    mogaController = Controller.getInstance(this);
    MogaHack.init(mogaController, this);
    mogaController.setListener(mogaListener, new Handler());

    // fullscreen
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);

    // keep screen on
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    Utils.setImmersionMode(this);

    //Extract data
    String base = EntryActivity.getFullDir();
    Utils.copyAsset(this, "gzdoom.pk3", base);
    Utils.copyAsset(this, "gzdoom.sf2", base);
    Utils.copyAsset(this, "doom1.wad", base);
    start_game();
  }

  @Override
  protected void onAddressChanged(String address)
  {
  }

  @Override
  protected void onConnectionChanged(boolean on)
  {
    controlInterp.onKeyDown(KeyEvent.KEYCODE_BACK, null);
    controlInterp.onKeyUp(KeyEvent.KEYCODE_BACK, null);
  }

  @Override
  protected void onDataReceived()
  {
    float[] angles = new float[3];
    getTransform().getEulerAngles(angles, 0);
    VRController.update(DaydreamController.getStatus(), angles, controlInterp);
  }

  @Override
  protected void onInitFinished()
  {
    mInitialized = true;
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus)
  {
    super.onWindowFocusChanged(hasFocus);
    Utils.onWindowFocusChanged(this, hasFocus);
  }


  public void start_game()
  {

    NativeLib.loadLibraries();

    NativeLib engine = new NativeLib();


    controlInterp = new ControlInterpreter(engine, Utils.getGameGamepadConfig(), TouchSettings.gamePadControlsFile, TouchSettings.gamePadEnabled);

    TouchControlsSettings.setup(act, engine);
    TouchControlsSettings.loadSettings(act);
    TouchControlsSettings.sendToQuake();

    TouchControlsEditing.setup(act);

    mGLSurfaceView = new GameView(this);

    NativeLib.gv = mGLSurfaceView;

    ShowKeyboard.setup(act, mGLSurfaceView);

    mGLSurfaceView.setEGLConfigChooser(new BestEglChooser(getApplicationContext()));

    mGLSurfaceView.setRenderer(mRenderer);

    // This will keep the screen on, while your view is visible.
    mGLSurfaceView.setKeepScreenOn(true);

    setContentView(mGLSurfaceView);
    mGLSurfaceView.requestFocus();
    mGLSurfaceView.setFocusableInTouchMode(true);
  }


  @Override
  protected void onPause()
  {
    Log.i(LOG, "onPause");
    SDLLib.nativePause();
    SDLLib.onPause();
    mogaController.onPause();
    super.onPause();
  }

  @Override
  protected void onResume()
  {

    Log.i(LOG, "onResume");
    SDLLib.onResume();
    SDLLib.nativeResume();
    mogaController.onResume();
    super.onResume();
    mGLSurfaceView.onResume();
  }


  @Override
  protected void onDestroy()
  {
    Log.i(LOG, "onDestroy");
    super.onDestroy();
    mogaController.exit();
    System.exit(0);
  }

  private class MogaControllerListener implements ControllerListener
  {


    @Override
    public void onKeyEvent(com.bda.controller.KeyEvent event)
    {
      //Log.d(LOG,"onKeyEvent " + event.getKeyCode());
      controlInterp.onMogaKeyEvent(event, mogaController.getState(Controller.STATE_CURRENT_PRODUCT_VERSION));
    }

    @Override
    public void onMotionEvent(com.bda.controller.MotionEvent event)
    {
      controlInterp.onGenericMotionEvent(event);
    }

    @Override
    public void onStateEvent(StateEvent event)
    {
      Log.d(LOG, "onStateEvent " + event.getState());
    }
  }

  class GameView extends MyGLSurfaceView
  {

		/*--------------------
     * Event handling
		 *--------------------*/


    public GameView(Context context)
    {
      super(context);

    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event)
    {
      return controlInterp.onGenericMotionEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
      return controlInterp.onTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
      return controlInterp.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
      return controlInterp.onKeyUp(keyCode, event);
    }

  }


  ///////////// GLSurfaceView.Renderer implementation ///////////

  private class DoomRenderer implements MyGLSurfaceView.Renderer
  {


    boolean divDone = false;

    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
      Log.d("Renderer", "onSurfaceCreated");
    }

    private void init(int width, int height)
    {

      Log.i(LOG, "screen size : " + width + "x" + height);

      NativeLib.setScreenSize(width, height);

      Utils.copyPNGAssets(getApplicationContext(), EntryActivity.getGfxDir());

      Log.i(LOG, "DoomInit start");

      //args = "-width 1280 -height 736 +set vid_renderer 1 -iwad tnt.wad -file brutal19.pk3 +set fluid_patchset /sdcard/WeedsGM3.sf2";
      //args = "+set vid_renderer 1 ";
      String gzdoom_args = "-width " + surfaceWidth + " -height " + surfaceHeight + " +set vid_renderer 1 ";
      String[] args_array = Utils.creatArgs(args + gzdoom_args);

      int audioSameple = AudioTrack.getNativeOutputSampleRate(AudioTrack.MODE_STREAM);
      Log.d(LOG, "audioSample = " + audioSameple);

      if ((audioSameple != 48000) && (audioSameple != 44100)) //Just in case
        audioSameple = 48000;

      int ret = NativeLib.init(EntryActivity.getGfxDir(), audioSameple, args_array, 0, gamePath);

      Log.i(LOG, "DoomInit done - " + ret);

    }

    public void onDrawFrame(GL10 gl)
    {

      if (!mInitialized)
        return;

      Log.d("Renderer", "onDrawFrame");

      if (!divDone)
        handlerUI.post(new Runnable()
        {
          @Override
          public void run()
          {
            mGLSurfaceView.getHolder().setFixedSize(surfaceWidth, surfaceHeight);
            divDone = true;
          }
        });

      if (divDone)
        init(surfaceWidth, surfaceHeight);
      else
      {
        try
        {
          Thread.sleep(200);
        } catch (InterruptedException e)
        {
          e.printStackTrace();
        }
      }
      Log.d("Renderer", "onDrawFrame END");

    }

    boolean SDLinited = false;

    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
      Log.d("Renderer", String.format("onSurfaceChanged %dx%d", width, height));

      if (surfaceWidth == -1)
      {
        surfaceWidth = width;
        surfaceHeight = height;
      }

      if (!SDLinited)
      {
        SDLLib.nativeInit(false);
        SDLLib.surfaceChanged(PixelFormat.RGBA_8888, surfaceWidth, surfaceHeight);
        SDLinited = true;
      }
      controlInterp.setScreenSize(surfaceWidth, surfaceHeight);
    }
  }


  @Override
  public boolean handleMessage(Message msg)
  {
    return false;
  }
}


