package com.lucidvr.gzdoom;

import com.google.vr.sdk.controller.Controller;

class VRController
{
  private static final int PORT_ACT_MENU_UP     = 0x200;
  private static final int PORT_ACT_MENU_DOWN   = 0x201;
  private static final int PORT_ACT_MENU_LEFT   = 0x202;
  private static final int PORT_ACT_MENU_RIGHT  = 0x203;
  private static final int PORT_ACT_MENU_SELECT = 0x204;
  private static final int PORT_ACT_MENU_BACK   = 0x205;
  private static final int PORT_ACT_USE         = 11;
  private static final int PORT_ACT_ATTACK      = 13;
  private static final int PORT_ACT_NEXT_WEP    = 16;
  private static final int PORT_ACT_PREV_WEP    = 17;

  private static boolean initialized = false;
  private static float pitch;
  private static float yaw;
  private static long timestamp;
  private static int swipeX;
  private static int swipeY;
  private static int touchInitX;
  private static int touchInitY;

  public static void update(Controller controller, float[] head)
  {
    //detect swipe
    int touchX = (int) (controller.touch.x * 255);
    int touchY = (int) (controller.touch.y * 255);
    if (!controller.isTouching)
    {
      swipeX = 0;
      swipeY = 0;
      touchInitX = 0;
      touchInitY = 0;
    } else if ((touchInitX == 0) && (touchInitY == 0)) {
      touchInitX = touchX;
      touchInitY = touchY;
    } else {
      swipeX = touchX - touchInitX;
      swipeY = touchY - touchInitY;
    }

    //touchpad - walking
    int x = swipeX;
    int y = swipeY;
    if (Math.abs(x) < 25)
      x = 0;
    if (Math.abs(y) < 25)
      y = 0;
    if (Math.abs(x) > Math.abs(y))
      y = 0;
    else
      x = 0;
    Game.analogSide(x * 0.02f);
    Game.analogFwd(-y * 0.02f);

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
      Game.analogPitch(0, (head[0] - pitch) * 0.5f);
      Game.analogYaw(0, gap * 0.25f);
      pitch = head[0];
      yaw = head[1];
    }

    //touchpad - menu
    if (swipeX > 50)
      sendActionEvent(PORT_ACT_MENU_RIGHT);
    if (swipeX < -50)
      sendActionEvent(PORT_ACT_MENU_LEFT);
    if (swipeY > 50)
      sendActionEvent(PORT_ACT_MENU_DOWN);
    if (swipeY < -50)
      sendActionEvent(PORT_ACT_MENU_UP);

    //buttons
    if (controller.clickButtonState)
    {
      sendMenuEvent(PORT_ACT_MENU_SELECT);
      sendKeyEvent(PORT_ACT_ATTACK, true);
    }
    if (controller.appButtonState)
      sendKeyEvent(PORT_ACT_USE, false);
    if (controller.volumeUpButtonState)
      sendActionEvent(PORT_ACT_NEXT_WEP);
    if (controller.volumeDownButtonState)
      sendActionEvent(PORT_ACT_PREV_WEP);
  }

  private static void sendActionEvent(int code)
  {
    if (timestamp + 250 < System.currentTimeMillis())
    {
      timestamp = System.currentTimeMillis();
      Game.doAction(1, code);
      Game.doAction(0, code);
    }
  }

  private static void sendKeyEvent(final int code, final boolean fire)
  {
    new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        Game.doAction(1, code);
        try
        {
          Thread.sleep(fire ? 50 : 200);
        } catch (InterruptedException e)
        {
          e.printStackTrace();
        }
        Game.doAction(0, code);
      }
    }).start();
  }

  private static void sendMenuEvent(int code)
  {
    if (timestamp + 250 < System.currentTimeMillis())
    {
      timestamp = System.currentTimeMillis();
      Game.doAction(1, code);
      Game.doAction(0, code);
    }
  }
}
