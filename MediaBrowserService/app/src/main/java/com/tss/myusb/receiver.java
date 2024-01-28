package com.tss.myusb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class receiver extends BroadcastReceiver {
    private static final String TAG = "receiver";
    private USBEventListener mUSBEventListener;
    public void registerUSBDeviceEventCallback(MainActivity usbEventListener) {
        mUSBEventListener = usbEventListener;
        Log.d(TAG,"Value mUSbEventListener  " + mUSBEventListener);
    }
    private static final String ACTION_USB_PERMISSION = "com.tss.USB_PERMISSION";
    private UsbDevice gUsbDevice;
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Context appContext = context.getApplicationContext();
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            Log.d(TAG,"USB ATTACHED");
            //  UsbDevice usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            gUsbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (gUsbDevice != null) {
                Log.d(TAG, "USB Device Attached: " + gUsbDevice.getDeviceName());
                Intent usbPermissionIntent = new Intent(ACTION_USB_PERMISSION);
                usbPermissionIntent.putExtra(UsbManager.EXTRA_DEVICE, gUsbDevice);
                PendingIntent permissionPendingIntent = PendingIntent.getBroadcast(
                        context,
                        0,
                        usbPermissionIntent,
                        PendingIntent.FLAG_IMMUTABLE
                );

                usbManager.requestPermission(gUsbDevice, permissionPendingIntent);

            } else {
                Log.d(TAG, "UsbDevice is null");

            }
        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            // Handle USB device detached event
            Log.d(TAG,"USB Device Detached ");
            if(mUSBEventListener != null) {

                Intent localIntent = new Intent("ACTION_USB_DEVICE_DETACHED");
                LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
                mUSBEventListener.onUSBDeviceDetached();
            }


        } else if (ACTION_USB_PERMISSION.equals(action)) {
            Log.d(TAG, "ACTION_USB_PERMISSION");
            synchronized (this) {
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (gUsbDevice == null)  Log.d(TAG, "Device is null");
                    if (gUsbDevice != null)  Log.d(TAG, "Device is accessible ");
                }
                else {
                    Log.d(TAG, "Permission Denied for device " + gUsbDevice);
                }
                Log.d(TAG, "onReceive: Final Permission: " + usbManager.hasPermission(gUsbDevice));
                 // Send a local broadcast to trigger onLoadChildren in YourMediaBrowserService
                Intent localIntent = new Intent("ACTION_USB_PERMISSION");
                LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);

                if(usbManager.hasPermission(gUsbDevice)){
                    mUSBEventListener.onUSBDeviceAttached();
                }

            }

        }
    }



}
