package com.lucidvr.sdk;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;
import com.lucidvr.gzdoom.R;

import javax.microedition.khronos.egl.EGLConfig;

public abstract class AbstractActivity extends GvrActivity implements
        BluetoothAdapter.LeScanCallback,
        GvrView.StereoRenderer
{
  private BluetoothAdapter mBluetoothAdapter;
  private BluetoothLeService mBluetooth;
  private String mDeviceAddress;
  private boolean mScanning;
  private GvrView mGvrView;
  private HeadTransform mHead;

  private static final int PERMISSIONS_CODE = 1987;
  private static final int REQUEST_ENABLE_BT = 1;

  protected abstract void onAddressChanged(String address);
  protected abstract void onConnectionChanged(boolean on);
  protected abstract void onDataReceived();
  protected abstract void onInitFinished();

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    // Use this check to determine whether BLE is supported on the device.  Then you can
    // selectively disable BLE-related features.
    if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
    {
      Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
      finish();
    }

    // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
    // BluetoothAdapter through BluetoothManager.
    BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    mBluetoothAdapter = bluetoothManager.getAdapter();

    // Checks if Bluetooth is supported on the device.
    if (mBluetoothAdapter == null)
    {
      Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
      finish();
    }

    // Transformation
    mGvrView = new com.google.vr.sdk.base.GvrView(this);
    mGvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8);
    mGvrView.setRenderer(this);
    mGvrView.setTransitionViewEnabled(true);
    mGvrView.setVisibility(View.INVISIBLE);
    setGvrView(mGvrView);
  }

  @Override
  public void setContentView(View view)
  {
    RelativeLayout layout = new RelativeLayout(this);
    layout.addView(view);
    layout.addView(mGvrView);
    super.setContentView(layout);
  }

  @Override
  public void setContentView(int layoutResID)
  {
    Log.e("AbstractActivity", "Unsupported, use setContentView(View)");
  }

  @Override
  public void setContentView(View view, ViewGroup.LayoutParams params)
  {
    Log.e("AbstractActivity", "Unsupported, use setContentView(View)");
  }

  @Override
  public void onNewIntent(Intent intent) {
    String action = intent.getAction();
    if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
            NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) ||
            NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    // User chose not to enable Bluetooth.
    if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED)
    {
      finish();
      return;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  protected void onDestroy()
  {
    super.onDestroy();
    unbindService(mServiceConnection);
    mBluetooth = null;
  }

  @Override
  protected void onResume()
  {
    super.onResume();
    if (!mBluetoothAdapter.isEnabled())
      startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
    else
      setupPermissions();
  }

  @Override
  protected void onPause()
  {
    super.onPause();
    try {
      //BT
      scanLeDevice(false);
      unregisterReceiver(mGattUpdateReceiver);
      mDeviceAddress = null;
      onAddressChanged("");
      //NFC
      NfcAdapter.getDefaultAdapter(this).disableForegroundDispatch(this);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  private void scanLeDevice(final boolean enable)
  {
    if (enable)
    {
      mScanning = true;
      mBluetoothAdapter.startLeScan(this);
    } else
    {
      mScanning = false;
      mBluetoothAdapter.stopLeScan(this);
    }
  }

  // Code to manage Service lifecycle.
  private final ServiceConnection mServiceConnection = new ServiceConnection()
  {

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service)
    {
      mBluetooth = ((BluetoothLeService.LocalBinder) service).getService();
      if (!mBluetooth.initialize())
        finish();

      // Automatically connects to the device upon successful start-up initialization.
      mBluetooth.connect(mDeviceAddress);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName)
    {
      mBluetooth = null;
    }
  };

  private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver()
  {
    @Override
    public void onReceive(Context context, Intent intent)
    {
      final String action = intent.getAction();
      if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action))
      {
        onConnectionChanged(true);
      } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action))
      {
        onConnectionChanged(false);
      } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action))
      {
        for (BluetoothGattCharacteristic ch : mBluetooth.getSupportedGattService().getCharacteristics())
        {
          if (!DaydreamController.lookup(ch.getUuid().toString()))
            continue;
          mBluetooth.setCharacteristicNotification(ch, true);
        }
      } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action))
      {
        onDataReceived();
      }
    }
  };

  @Override
  public void onLeScan(final BluetoothDevice device, int i, byte[] bytes)
  {
    runOnUiThread(new Runnable()
    {
      @Override
      public void run()
      {
        if (device == null)
          return;
        mDeviceAddress = device.getAddress();
        onAddressChanged(mDeviceAddress);
        Intent gattServiceIntent = new Intent(getBaseContext(), BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        if (mScanning)
        {
          mBluetoothAdapter.stopLeScan(AbstractActivity.this);
          mScanning = false;
        }
      }
    });
  }

  @Override
  public void onRendererShutdown() {}

  @Override
  public void onSurfaceChanged(int width, int height) {}

  @Override
  public void onSurfaceCreated(EGLConfig config) {}

  @Override
  public synchronized void onNewFrame(HeadTransform headTransform)
  {
    mHead = headTransform;
  }

  @Override
  public void onDrawEye(Eye eye) {}

  @Override
  public void onFinishFrame(Viewport viewport) {}

  public synchronized HeadTransform getTransform()
  {
    return mHead;
  }


  protected void setupPermissions() {
    String[] permissions = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.NFC,
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
        {
          //BT
          final IntentFilter intentFilter = new IntentFilter();
          intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
          intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
          intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
          intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
          registerReceiver(mGattUpdateReceiver, intentFilter);
          if (mDeviceAddress != null)
            mBluetooth.connect(mDeviceAddress);
          scanLeDevice(true);
          //NFC
          try {
            NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
            adapter.enableForegroundDispatch(this, pendingIntent, null, null);
          } catch(Exception e) {
            e.printStackTrace();
          }
          runOnUiThread(new Runnable()
          {
            @Override
            public void run()
            {
              onInitFinished();
            }
          });
        } else
          finish();
        break;
      }
    }
  }
}
