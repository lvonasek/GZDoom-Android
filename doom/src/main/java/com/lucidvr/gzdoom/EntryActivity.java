package com.lucidvr.gzdoom;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.beloko.touchcontrols.TouchSettings;

public class EntryActivity extends Activity
{
  private static String gzdoomBaseDir;
  private static String graphicsDir = "";

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    reloadSettings(getApplication());
    startGame(getFullDir(), "-iwad doom1.wad ");
    finish();
  }

  static void reloadSettings(Context ctx)
  {
    TouchSettings.reloadSettings(ctx);
    gzdoomBaseDir = "/data/data/" + ctx.getPackageName();
    graphicsDir = ctx.getFilesDir().toString() + "/";
  }

  static String getGfxDir()
  {
    return graphicsDir;
  }

  static String getFullDir()
  {
    return gzdoomBaseDir + "/config";
  }

  private void startGame(String base, String args)
  {
    args += " -savedir " + base + "/gzdoom_saves";
    args += " +set fluid_patchset gzdoom.sf2 +set midi_dmxgus 0 ";

    Intent intent = new Intent(this, Game.class);
    intent.setAction(Intent.ACTION_MAIN);
    intent.addCategory(Intent.CATEGORY_LAUNCHER);
    intent.putExtra("game_path", base);
    intent.putExtra("game", "doom");
    intent.putExtra("args", args);
    startActivity(intent);
  }
}
