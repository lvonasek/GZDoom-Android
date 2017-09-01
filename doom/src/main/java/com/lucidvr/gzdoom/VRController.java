package com.lucidvr.gzdoom;

import android.util.SparseIntArray;
import android.view.KeyEvent;

import com.beloko.touchcontrols.ControlConfig;
import com.beloko.touchcontrols.ControlInterpreter;
import com.lucidvr.sdk.DaydreamController;

class VRController
{
  private static boolean initialized = false;
  private static float pitch;
  private static float yaw;
  private static long timestamp;

  public static void update(SparseIntArray data, float[] head, ControlInterpreter controlInterp)
  {
    //touchpad - walking
    int x = data.get(DaydreamController.SWP_X);
    int y = data.get(DaydreamController.SWP_Y);
    if (Math.abs(x) < 25)
      x = 0;
    if (Math.abs(y) < 25)
      y = 0;
    if (Math.abs(x) > Math.abs(y))
      y = 0;
    else
      x = 0;
    NativeLib.analogSide(x * 0.02f);
    NativeLib.analogFwd(-y * 0.02f);

    //head rotation
    if(!initialized)
    {
      initialized = true;
      pitch = head[0];
      yaw = head[1];
    } else {
      float gap = head[1] - yaw;
      //TODO:better handling angle between 359 and 0 degrees
      if (Math.abs(gap) > Math.PI * 0.5)
        gap = 0;
      NativeLib.analogPitch(0, (head[0] - pitch) * 0.5f);
      NativeLib.analogYaw(0, gap * 0.25f);
      pitch = head[0];
      yaw = head[1];
    }

    //touchpad - menu
    if (data.get(DaydreamController.SWP_X) > 50)
      sendActionEvent(ControlConfig.MENU_RIGHT);
    if (data.get(DaydreamController.SWP_X) < -50)
      sendActionEvent(ControlConfig.MENU_LEFT);
    if (data.get(DaydreamController.SWP_Y) > 50)
      sendActionEvent(ControlConfig.MENU_DOWN);
    if (data.get(DaydreamController.SWP_Y) < -50)
      sendActionEvent(ControlConfig.MENU_UP);

    //buttons
    if (data.get(DaydreamController.BTN_CLICK) > 0)
    {
      sendMenuEvent(controlInterp, KeyEvent.KEYCODE_ENTER);
      sendKeyEvent(controlInterp, KeyEvent.KEYCODE_CTRL_LEFT, true);
    }
    if (data.get(DaydreamController.BTN_HOME) > 0)
      sendMenuEvent(controlInterp, KeyEvent.KEYCODE_BACK);
    if (data.get(DaydreamController.BTN_APP) > 0)
      sendKeyEvent(controlInterp, KeyEvent.KEYCODE_SPACE, false);
    if (data.get(DaydreamController.BTN_VOL_UP) > 0)
      sendActionEvent(ControlConfig.PORT_ACT_NEXT_WEP);
    if (data.get(DaydreamController.BTN_VOL_DOWN) > 0)
      sendActionEvent(ControlConfig.PORT_ACT_PREV_WEP);
  }

  private static void sendActionEvent(int code)
  {
    if (timestamp + 250 < System.currentTimeMillis())
    {
      timestamp = System.currentTimeMillis();
      NativeLib.doAction(1, code);
      NativeLib.doAction(0, code);
    }
  }

  private static void sendKeyEvent(final ControlInterpreter controlInterp, final int code, final boolean fire)
  {
    new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        controlInterp.onKeyDown(code, null);
        try
        {
          Thread.sleep(fire ? 50 : 200);
        } catch (InterruptedException e)
        {
          e.printStackTrace();
        }
        controlInterp.onKeyUp(code, null);
      }
    }).start();
  }

  private static void sendMenuEvent(final ControlInterpreter controlInterp, int code)
  {
    if (timestamp + 250 < System.currentTimeMillis())
    {
      timestamp = System.currentTimeMillis();
      controlInterp.onKeyDown(code, null);
      controlInterp.onKeyUp(code, null);
    }
  }
}
