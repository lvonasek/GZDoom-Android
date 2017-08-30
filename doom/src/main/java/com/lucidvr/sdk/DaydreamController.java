package com.lucidvr.sdk;

import android.util.SparseIntArray;

import java.util.HashMap;
import java.util.UUID;

public class DaydreamController
{
  // status IDs
  public static final int BTN_CLICK = 0x1;
  public static final int BTN_HOME = 0x2;
  public static final int BTN_APP = 0x4;
  public static final int BTN_VOL_DOWN = 0x8;
  public static final int BTN_VOL_UP = 0x10;
  public static final int SYN_SEQ = 0x11;
  public static final int SYN_TIME = 0x12;
  public static final int TPD_X = 0x13;
  public static final int TPD_Y = 0x14;
  public static final int SWP_X = 0x15;
  public static final int SWP_Y = 0x16;
  public static final int ACC_X = 0x21;
  public static final int ACC_Y = 0x22;
  public static final int ACC_Z = 0x23;
  public static final int GYR_X = 0x31;
  public static final int GYR_Y = 0x32;
  public static final int GYR_Z = 0x33;
  public static final int ORI_X = 0x41;
  public static final int ORI_Y = 0x42;
  public static final int ORI_Z = 0x43;

  // Controller status
  private static SparseIntArray status = new SparseIntArray();
  private static int touchInitX;
  private static int touchInitY;

  // Remote connection
  private static HashMap<String, String> attributes = new HashMap<>();
  private static final String CHARACTERISTIC_CLIENTID = "00002902-0000-1000-8000-00805f9b34fb";
  private static final String DAYDREAM_CUSTOM_SERVICE = "0000fe55-0000-1000-8000-00805f9b34fb";
  private static final String DAYDREAM_CHARACTERISTIC = "00000001-1000-1000-8000-00805f9b34fb";

  static
  {
    attributes.put(DAYDREAM_CUSTOM_SERVICE, "DAYDREAM_CUSTOM_SERVICE");
    attributes.put(DAYDREAM_CHARACTERISTIC, "DAYDREAM_CHARACTERISTIC");
  }

  static UUID getClientID()
  {
    return UUID.fromString(CHARACTERISTIC_CLIENTID);
  }

  static UUID getCharacteric()
  {
    return UUID.fromString(DAYDREAM_CHARACTERISTIC);
  }

  static UUID getServiceID()
  {
    return UUID.fromString(DAYDREAM_CUSTOM_SERVICE);
  }

  static boolean lookup(String uuid)
  {
    return attributes.get(uuid) != null;
  }

  static synchronized void decode(byte[] data)
  {
    int value;

    //synchronisation
    status.put(SYN_TIME, (data[0] & 0xFF) << 1 | (data[1] & 0x80) >> 7);
    status.put(SYN_SEQ, (data[1] & 0x7C) >> 2);

    //touch-pad
    status.put(TPD_X, (data[16] & 0x1F) << 3 | (data[17] & 0xE0) >> 5);
    status.put(TPD_Y, (data[17] & 0x1F) << 3 | (data[18] & 0xE0) >> 5);
    status.put(BTN_CLICK, Math.min(1, data[18] & BTN_CLICK));

    //buttons
    status.put(BTN_HOME, Math.min(1, data[18] & BTN_HOME));
    status.put(BTN_APP, Math.min(1, data[18] & BTN_APP));
    status.put(BTN_VOL_DOWN, Math.min(1, data[18] & BTN_VOL_DOWN));
    status.put(BTN_VOL_UP, Math.min(1, data[18] & BTN_VOL_UP));

    value = (data[6] & 0x07) << 10 | (data[7] & 0xFF) << 2 | (data[8] & 0xC0) >> 6;
    status.put(ACC_X, (value << 19) >> 19);
    value = (data[8] & 0x3F) << 7 | (data[9] & 0xFE) >>> 1;
    status.put(ACC_Y, (value << 19) >> 19);
    value = (data[9] & 0x01) << 12 | (data[10] & 0xFF) << 4 | (data[11] & 0xF0) >> 4;
    status.put(ACC_Z, (value << 19) >> 19);

    value = ((data[11] & 0x0F) << 9 | (data[12] & 0xFF) << 1 | (data[13] & 0x80) >> 7);
    status.put(GYR_X, (value << 19) >> 19);
    value = ((data[13] & 0x7F) << 6 | (data[14] & 0xFC) >> 2);
    status.put(GYR_Y, (value << 19) >> 19);
    value = ((data[14] & 0x03) << 11 | (data[15] & 0xFF) << 3 | (data[16] & 0xE0) >> 5);
    status.put(GYR_Z, (value << 19) >> 19);

    value = (data[1] & 0x03) << 11 | (data[2] & 0xFF) << 3 | (data[3] & 0xE0) >> 5;
    status.put(ORI_X, (value << 19) >> 19);
    value = (data[3] & 0x1F) << 8 | (data[4] & 0xFF);
    status.put(ORI_Y, (value << 19) >> 19);
    value = (data[5] & 0xFF) << 5 | (data[6] & 0xF8) >> 3;
    status.put(ORI_Z, (value << 19) >> 19);

    generateVirtualStatuses();
  }

  private static void generateVirtualStatuses()
  {
    int touchX = status.get(TPD_X);
    int touchY = status.get(TPD_Y);
    if ((touchX == 0) && (touchY == 0))
    {
      status.put(SWP_X, 0);
      status.put(SWP_Y, 0);
      touchInitX = 0;
      touchInitY = 0;
    } else if ((touchInitX == 0) && (touchInitY == 0)) {
      touchInitX = touchX;
      touchInitY = touchY;
    } else {
      status.put(SWP_X, touchX - touchInitX);
      status.put(SWP_Y, touchY - touchInitY);
    }
  }

  public synchronized static SparseIntArray getStatus()
  {
    return status.clone();
  }

  static synchronized String rawValues()
  {
    String output = "SYN_TIME: " + status.get(SYN_TIME) + ", SYN_SEQ: " + status.get(SYN_SEQ) + "\n";
    output += "ACC: " + status.get(ACC_X) + ", " + status.get(ACC_Y) + ", " + status.get(ACC_Z) + "\n";
    output += "GYR: " + status.get(GYR_X) + ", " + status.get(GYR_Y) + ", " + status.get(GYR_Z) + "\n";
    output += "ORI: " + status.get(ORI_X) + ", " + status.get(ORI_Y) + ", " + status.get(ORI_Z) + "\n";
    output += "TPD: " + status.get(TPD_X) + ", " + status.get(TPD_Y) + "\n";
    output += "BTN_CLICK: " + status.get(BTN_CLICK) + "\n";
    output += "BTN_APP: " + status.get(BTN_APP) + "\n";
    output += "BTN_HOME: " + status.get(BTN_HOME) + "\n";
    output += "BTN_VOL_UP: " + status.get(BTN_VOL_UP) + "\n";
    output += "BTN_VOL_DOWN: " + status.get(BTN_VOL_DOWN) + "\n";
    return output;
  }
}
