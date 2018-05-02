package com.lucidvr.gzdoom;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class EntryActivity extends Activity
{
  private final int PERMISSIONS_CODE = 1987;
  private String base = null;
  private String wad = null;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    Context ctx = getApplicationContext();
    String pkgDir = new File(ctx.getFilesDir(), ctx.getPackageName()).getAbsolutePath();
    base = pkgDir + "/config";

    //Unpack data from intent
    wad = getIntent().getDataString();
    if (wad != null)
    {
      setupPermissions();
    } else {
      run();
    }
  }

  private void run()
  {
    //Extract data
    copyAsset(this, "gzdoom.pk3", base);
    copyAsset(this, "gzdoom.sf2", base);
    copyAsset(this, "doom1.wad", base);
    copyAsset(this, "zdoom.ini", new File(base, "gzdoom_dev").getAbsolutePath());

    //open WAD file
    startGame(base, "doom1.wad");
    finish();
  }

  protected void setupPermissions() {
    String[] permissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    boolean ok = true;
    for (String s : permissions)
      if (checkSelfPermission(s) != PackageManager.PERMISSION_GRANTED)
        ok = false;

    if (!ok)
      requestPermissions(permissions, PERMISSIONS_CODE);
    else
      onRequestPermissionsResult(PERMISSIONS_CODE, null, new int[]{PackageManager.PERMISSION_GRANTED});
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
  {
    switch (requestCode)
    {
      case PERMISSIONS_CODE:
      {
        for (int r : grantResults)
        {
          if (r != PackageManager.PERMISSION_GRANTED)
          {
            finish();
            return;
          }
        }

        try
        {
          deleteRecursive(new File(base));
          new File(base).mkdirs();
          InputStream in = new URL(wad).openStream();
          OutputStream out = new FileOutputStream(base + "/doom1.wad");
          copyFile(in, out);
          Toast.makeText(this, R.string.unpacked, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
          e.printStackTrace();
          deleteRecursive(new File(base));
          Toast.makeText(this, R.string.unpacked_err, Toast.LENGTH_LONG).show();
        }
        finish();
        break;
      }
    }
  }

  private void copyAsset(Context ctx, String file, String destdir)
  {
    AssetManager assetManager = ctx.getAssets();
    new File(destdir).mkdirs();
    if (new File(destdir + "/" + file).exists())
      return;

    try
    {
      InputStream in = assetManager.open(file);
      OutputStream out = new FileOutputStream(destdir + "/" + file);
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

  void deleteRecursive(File fileOrDirectory)
  {
    if (fileOrDirectory.isDirectory())
      for (File child : fileOrDirectory.listFiles())
        deleteRecursive(child);

    fileOrDirectory.delete();
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
