package com.lucidvr.gzdoom;

import android.util.SparseIntArray;

import com.lucidvr.sdk.DaydreamController;

class VRController
{
  private static boolean initialized = false;
  private static float pitch;
  private static float yaw;
  private static long timestamp;

  public static void update(SparseIntArray data, float[] head)
  {
    int PORT_ACT_MENU_UP     = 0x200;
    int PORT_ACT_MENU_DOWN   = 0x201;
    int PORT_ACT_MENU_LEFT   = 0x202;
    int PORT_ACT_MENU_RIGHT  = 0x203;
    int PORT_ACT_MENU_SELECT = 0x204;
    int PORT_ACT_MENU_BACK   = 0x205;
    int PORT_ACT_USE         = 11;
    int PORT_ACT_ATTACK      = 13;
    int PORT_ACT_NEXT_WEP    = 16;
    int PORT_ACT_PREV_WEP    = 17;

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
      sendActionEvent(PORT_ACT_MENU_RIGHT);
    if (data.get(DaydreamController.SWP_X) < -50)
      sendActionEvent(PORT_ACT_MENU_LEFT);
    if (data.get(DaydreamController.SWP_Y) > 50)
      sendActionEvent(PORT_ACT_MENU_DOWN);
    if (data.get(DaydreamController.SWP_Y) < -50)
      sendActionEvent(PORT_ACT_MENU_UP);

    //buttons
    if (data.get(DaydreamController.BTN_CLICK) > 0)
    {
      sendMenuEvent(PORT_ACT_MENU_SELECT);
      sendKeyEvent(PORT_ACT_ATTACK, true);
    }
    if (data.get(DaydreamController.BTN_HOME) > 0)
      sendMenuEvent(PORT_ACT_MENU_BACK);
    if (data.get(DaydreamController.BTN_APP) > 0)
      sendKeyEvent(PORT_ACT_USE, false);
    if (data.get(DaydreamController.BTN_VOL_UP) > 0)
      sendActionEvent(PORT_ACT_NEXT_WEP);
    if (data.get(DaydreamController.BTN_VOL_DOWN) > 0)
      sendActionEvent(PORT_ACT_PREV_WEP);
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

  private static void sendKeyEvent(final int code, final boolean fire)
  {
    new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        NativeLib.doAction(1, code);
        try
        {
          Thread.sleep(fire ? 50 : 200);
        } catch (InterruptedException e)
        {
          e.printStackTrace();
        }
        NativeLib.doAction(0, code);
      }
    }).start();
  }

  private static void sendMenuEvent(int code)
  {
    if (timestamp + 250 < System.currentTimeMillis())
    {
      timestamp = System.currentTimeMillis();
      NativeLib.doAction(1, code);
      NativeLib.doAction(0, code);
    }
  }
}
