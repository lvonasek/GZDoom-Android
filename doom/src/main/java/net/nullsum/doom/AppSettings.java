package net.nullsum.doom;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import com.beloko.touchcontrols.TouchSettings;

public class AppSettings {

    public static String gzdoomBaseDir;

    public static String musicBaseDir;

    public static String graphicsDir = "";

    public static boolean vibrate;
    public static boolean immersionMode;

    public static Context ctx;

    public static void resetBaseDir(Context ctx)
    {
        gzdoomBaseDir  =  Environment.getExternalStorageDirectory().toString() + "/GZDoom";
        setStringOption(ctx, "base_path", gzdoomBaseDir);
    }

    public static void reloadSettings(Context ctx)
    {
        AppSettings.ctx = ctx;

        TouchSettings.reloadSettings(ctx);

        gzdoomBaseDir = getStringOption(ctx, "base_path", null);
        if (gzdoomBaseDir == null)
        {
            resetBaseDir(ctx);
        }

        String music = getStringOption(ctx, "music_path", null);
        if (music == null)
        {
            music  =  gzdoomBaseDir + "/doom/Music";
            setStringOption(ctx, "music_path", music);
        }

        musicBaseDir =  music;

        graphicsDir = ctx.getFilesDir().toString() + "/";

        vibrate =  getBoolOption(ctx, "vibrate", true);

        immersionMode = getBoolOption(ctx, "immersion_mode", true);
    }

    public static String getFullDir()
    {
        String quakeFilesDir = AppSettings.gzdoomBaseDir;
        return quakeFilesDir + "/config";
    }

    public static boolean getBoolOption(Context ctx,String name, boolean def)
    {
        SharedPreferences settings = ctx.getSharedPreferences("OPTIONS",    Context.MODE_MULTI_PROCESS);
        return settings.getBoolean(name, def);
    }

    public static int getIntOption(Context ctx,String name, int def)
    {
        SharedPreferences settings = ctx.getSharedPreferences("OPTIONS",    Context.MODE_MULTI_PROCESS);
        return settings.getInt(name, def);
    }

    public static String getStringOption(Context ctx,String name, String def)
    {
        SharedPreferences settings = ctx.getSharedPreferences("OPTIONS",    Context.MODE_MULTI_PROCESS);
        return settings.getString(name, def);
    }

    public static void setStringOption(Context ctx,String name, String value)
    {
        SharedPreferences settings = ctx.getSharedPreferences("OPTIONS",    Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(name, value);
        editor.apply();
    }
}
