package com.lucidvr.gzdoom;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.lucidvr.sdk.AbstractActivity;
import com.lucidvr.sdk.DaydreamController;

import java.io.File;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Game extends AbstractActivity implements MyGLSurfaceView.Renderer
{
  private String args;
  private String gamePath;

  private MyGLSurfaceView mGLSurfaceView = null;
  private boolean mInitialized = false;
  private boolean divDone = false;
  private boolean SDLinited = false;
  private int surfaceWidth = -1, surfaceHeight;

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    args = getIntent().getStringExtra("args");
    gamePath = getIntent().getStringExtra("game_path");

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
    File dir = new File(base, "gzdoom_dev");
    if (!dir.exists())
      Utils.copyAsset(this, "zdoom.ini", dir.getAbsolutePath());
    start_game();
  }

  @Override
  protected void onAddressChanged(String address)
  {
  }

  @Override
  protected void onConnectionChanged(boolean on)
  {
    int code = 0x205;
    NativeLib.doAction(1, code);
    NativeLib.doAction(0, code);
  }

  @Override
  protected void onDataReceived()
  {
    float[] angles = new float[3];
    getTransform().getEulerAngles(angles, 0);
    VRController.update(DaydreamController.getStatus(), angles);
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

    mGLSurfaceView = new MyGLSurfaceView(this);

    NativeLib.gv = mGLSurfaceView;

    mGLSurfaceView.setRenderer(this);

    // This will keep the screen on, while your view is visible.
    mGLSurfaceView.setKeepScreenOn(true);

    setContentView(mGLSurfaceView);
    mGLSurfaceView.requestFocus();
    mGLSurfaceView.setFocusableInTouchMode(true);
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
    mGLSurfaceView.onResume();
  }


  @Override
  protected void onDestroy()
  {
    super.onDestroy();
    System.exit(0);
  }

  public void onSurfaceCreated(GL10 gl, EGLConfig config)
  {
  }

  public void onDrawFrame(GL10 gl)
  {
    if (!mInitialized)
      return;

    if (!divDone)
      Game.this.runOnUiThread(new Runnable()
      {
        @Override
        public void run()
        {
          mGLSurfaceView.getHolder().setFixedSize(surfaceWidth, surfaceHeight);
          divDone = true;
        }
      });

    if (divDone)
    {
      String gzdoom_args = "-width " + surfaceWidth + " -height " + surfaceHeight + " +set vid_renderer 1 ";
      String[] args_array = Utils.creatArgs(args + gzdoom_args);
      NativeLib.init(48000, args_array, 0, gamePath);
    }
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
  }

  public void onSurfaceChanged(GL10 gl, int width, int height)
  {
    if (surfaceWidth == -1)
    {
      surfaceWidth = width;
      surfaceHeight = height;
    }

    if (!SDLinited)
    {
      SDLAudio.nativeInit(false);
      SDLinited = true;
    }
  }
}
