package com.lucidvr.gzdoom;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

class Utils
{
  private static String LOG = "Utils";

  private static void copyFile(InputStream in, OutputStream out) throws IOException
  {
    byte[] buffer = new byte[1024];
    int read;
    while ((read = in.read(buffer)) != -1)
    {
      out.write(buffer, 0, read);
    }
    out.close();
  }

  static String[] creatArgs(String appArgs)
  {
    ArrayList<String> a = new ArrayList<>(Arrays.asList(appArgs.split(" ")));

    Iterator<String> iter = a.iterator();
    while (iter.hasNext())
    {
      if (iter.next().contentEquals(""))
      {
        iter.remove();
      }
    }

    return a.toArray(new String[a.size()]);
  }


  public static void expand(final View v)
  {
    v.measure(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
    final int targtetHeight = v.getMeasuredHeight();

    v.getLayoutParams().height = 0;
    v.setVisibility(View.VISIBLE);
    Animation a = new Animation()
    {
      @Override
      protected void applyTransformation(float interpolatedTime, Transformation t)
      {
        v.getLayoutParams().height = interpolatedTime == 1
                ? LayoutParams.WRAP_CONTENT
                : (int) (targtetHeight * interpolatedTime);
        v.requestLayout();
      }

      @Override
      public boolean willChangeBounds()
      {
        return true;
      }
    };

    // 1dp/ms
    a.setDuration((int) (targtetHeight / v.getContext().getResources().getDisplayMetrics().density));
    v.startAnimation(a);
  }

  static void copyAsset(Context ctx, String file, String destdir)
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

  static void setImmersionMode(final Activity act)
  {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
    {

      act.getWindow().getDecorView().setSystemUiVisibility(
              View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                      | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                      | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                      | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                      | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                      | View.SYSTEM_UI_FLAG_IMMERSIVE
                      | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

      View decorView = act.getWindow().getDecorView();
      decorView.setOnSystemUiVisibilityChangeListener
              (new View.OnSystemUiVisibilityChangeListener()
              {
                @Override
                public void onSystemUiVisibilityChange(int visibility)
                {
                  Log.d(LOG, "onSystemUiVisibilityChange");

                  act.getWindow().getDecorView().setSystemUiVisibility(
                          View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                  | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                  | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                  | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                  | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                  | View.SYSTEM_UI_FLAG_IMMERSIVE
                                  | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

                }
              });
    }
  }

  static void onWindowFocusChanged(final Activity act, final boolean hasFocus)
  {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
    {

      Handler handler = new Handler();

      handler.postDelayed(new Runnable()
      {
        public void run()
        {

          if (hasFocus)
          {
            act.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
          }
        }

      }, 2000);
    }
  }
}
