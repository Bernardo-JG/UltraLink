package org.feup.apm.cmeb_login;

import android.bluetooth.BluetoothDevice;

public class BluetoothDeviceInfo {
    private BluetoothDevice device;
    private String deviceName;
    private String deviceAddress;

    public BluetoothDeviceInfo(BluetoothDevice device, String deviceName, String deviceAddress) {
        this.device = device;
        this.deviceName = deviceName;
        this.deviceAddress = deviceAddress;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }
}
