package com.lucidvr.ddctrl;

import android.app.Activity;
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
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import net.nullsum.doom.R;

public abstract class AbstractActivity extends Activity implements BluetoothAdapter.LeScanCallback
{
  private BluetoothAdapter mBluetoothAdapter;
  private BluetoothLeService mBluetooth;
  private String mDeviceAddress;
  private boolean mScanning;

  private static final int REQUEST_ENABLE_BT = 1;

  protected abstract void onAddressChanged(String address);
  protected abstract void onConnectionChanged(boolean on);
  protected abstract void onDataReceived();

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
  }

  @Override
  protected void onResume()
  {
    super.onResume();
    final IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
    intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
    intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
    intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
    registerReceiver(mGattUpdateReceiver, intentFilter);
    if (mDeviceAddress != null)
      mBluetooth.connect(mDeviceAddress);

    // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
    // fire an intent to display a dialog asking the user to grant permission to enable it.
    if (!mBluetoothAdapter.isEnabled())
      startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
    scanLeDevice(true);
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
  protected void onPause()
  {
    super.onPause();
    scanLeDevice(false);
    unregisterReceiver(mGattUpdateReceiver);
    mDeviceAddress = null;
    onAddressChanged("");
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
}
