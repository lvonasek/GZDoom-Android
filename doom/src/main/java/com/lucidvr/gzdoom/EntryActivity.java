package com.lucidvr.gzdoom;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class EntryActivity extends Activity
{
  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    Context ctx = getApplicationContext();
    String pkgDir = new File(ctx.getFilesDir(), ctx.getPackageName()).getAbsolutePath();

    //Extract data
    String base = pkgDir + "/config";
    copyAsset(this, "gzdoom.pk3", base);
    copyAsset(this, "gzdoom.sf2", base);
    copyAsset(this, "doom1.wad", base);
    File dir = new File(base, "gzdoom_dev");
    if (!dir.exists())
      copyAsset(this, "zdoom.ini", dir.getAbsolutePath());

    //open WAD file
    startGame(base, "doom1.wad");
    finish();
  }

  private void copyAsset(Context ctx, String file, String destdir)
  {
    AssetManager assetManager = ctx.getAssets();
    new File(destdir).mkdirs();

    InputStream in;
    OutputStream out;

    try
    {
      in = assetManager.open(file);
      out = new FileOutputStream(destdir + "/" + file);
      copyFile(in, out);
      in.close();
      out.flush();
      out.close();
    } catch (IOException e)
    {
      Log.e("tag", "Failed to copy asset file: " + file);
    }
  }

  private void copyFile(InputStream in, OutputStream out) throws IOException
  {
    byte[] buffer = new byte[1024];
    int read;
    while ((read = in.read(buffer)) != -1)
    {
      out.write(buffer, 0, read);
    }
    out.close();
  }

  private void startGame(String base, String game)
  {
    String args = "-iwad " + game + " ";
    args += " -savedir " + base + "/gzdoom_saves";
    args += " +set fluid_patchset gzdoom.sf2 +set midi_dmxgus 0 ";

    Intent intent = new Intent(this, Game.class);
    intent.putExtra("game_path", base);
    intent.putExtra("game", "doom");
    intent.putExtra("args", args);
    startActivity(intent);
  }
}
