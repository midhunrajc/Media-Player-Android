package com.tss.myusb;

public interface USBEventListener {
    void onUSBDeviceAttached();
    void onUSBDeviceDetached();
}
