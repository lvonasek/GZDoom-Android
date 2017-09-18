package com.lucidvr.gzdoom;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.io.File;

public class EntryActivity extends Activity
{
  private static String gzdoomBaseDir;

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
    gzdoomBaseDir = new File(ctx.getFilesDir(), ctx.getPackageName()).getAbsolutePath();
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
