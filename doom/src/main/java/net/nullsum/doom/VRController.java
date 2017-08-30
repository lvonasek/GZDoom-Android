package net.nullsum.doom;

import android.util.SparseIntArray;
import android.view.KeyEvent;

import com.beloko.touchcontrols.ControlConfig;
import com.beloko.touchcontrols.ControlInterpreter;
import com.lucidvr.ddctrl.DaydreamController;

class VRController
{
  private static long timestamp;

  public static void update(SparseIntArray data, ControlInterpreter controlInterp)
  {
    //touchpad - walking
    int x = data.get(DaydreamController.SWP_X);
    int y = data.get(DaydreamController.SWP_Y);
    if (Math.abs(x) < 25)
      x = 0;
    if (Math.abs(y) < 25)
      y = 0;
    NativeLib.analogYaw(0, -x * 0.00003f);
    NativeLib.analogFwd(-y * 0.02f);

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
      sendKeyEvent(controlInterp, KeyEvent.KEYCODE_CTRL_LEFT);
    }
    if (data.get(DaydreamController.BTN_HOME) > 0)
      sendMenuEvent(controlInterp, KeyEvent.KEYCODE_BACK);
    if (data.get(DaydreamController.BTN_APP) > 0)
      sendKeyEvent(controlInterp, KeyEvent.KEYCODE_SPACE);
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

  private static void sendKeyEvent(final ControlInterpreter controlInterp, final int code)
  {
    new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        controlInterp.onKeyDown(code, null);
        try
        {
          Thread.sleep(50);
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
