package net.nullsum.doom;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.content.pm.PackageManager;

public class EntryActivity extends Activity
{
    private static final int PERMISSIONS_CODE = 1993;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppSettings.reloadSettings(getApplication());
        setupPermissions();
    }

    protected void setupPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean ok = true;
            for (String s : permissions)
                if (checkSelfPermission(s) != PackageManager.PERMISSION_GRANTED)
                    ok = false;

            if (!ok)
                requestPermissions(permissions, PERMISSIONS_CODE);
            else
                onRequestPermissionsResult(PERMISSIONS_CODE, null, new int[]{PackageManager.PERMISSION_GRANTED});
        } else
            onRequestPermissionsResult(PERMISSIONS_CODE, null, new int[]{PackageManager.PERMISSION_GRANTED});
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case PERMISSIONS_CODE:
            {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    startGame(AppSettings.getFullDir(),"-iwad doom1.wad ");
                finish();
                break;
            }
        }
    }


    private void startGame(final String base,String args)
    {
        //Extract data
        Utils.copyAsset(this,"gzdoom.pk3",base);
        Utils.copyAsset(this,"gzdoom.sf2",base);
        Utils.copyAsset(this,"doom1.wad",base);

        AppSettings.setStringOption(this, "last_tab", "gzdoom");
        Intent intent = new Intent(this, net.nullsum.doom.Game.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        int resDiv = AppSettings.getIntOption(this,  "gzdoom_res_div", 1);
        intent.putExtra("res_div",resDiv);

        intent.putExtra("game_path",base);
        intent.putExtra("game", "doom");

        String saveDir;
        saveDir = " -savedir " + base + "/gzdoom_saves";

        String fluidSynthFile = "gzdoom.sf2";

        intent.putExtra("args",args + saveDir + " +set fluid_patchset " + fluidSynthFile + " +set midi_dmxgus 0 ");

        startActivity(intent);
    }
}
