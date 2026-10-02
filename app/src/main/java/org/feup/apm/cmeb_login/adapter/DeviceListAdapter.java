package org.feup.apm.cmeb_login.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.feup.apm.cmeb_login.BluetoothDeviceInfo;
import org.feup.apm.cmeb_login.R;

import java.util.ArrayList;

public class DeviceListAdapter extends ArrayAdapter<BluetoothDeviceInfo> {

    private LayoutInflater mLayoutInflater;
    private ArrayList<BluetoothDeviceInfo> mDevices;
    private int mViewResourceId;

    public DeviceListAdapter(Context context, int tvResourceId, ArrayList<BluetoothDeviceInfo> devices) {
        super(context, tvResourceId, devices);
        this.mDevices = devices;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mViewResourceId = tvResourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = mLayoutInflater.inflate(mViewResourceId, null);

        BluetoothDeviceInfo deviceInfo = mDevices.get(position);

        if (deviceInfo != null) {
            TextView deviceName = (TextView) convertView.findViewById(R.id.tvDeviceName);
            TextView deviceAddress = (TextView) convertView.findViewById(R.id.tvDeviceAddress);

            if (deviceName != null) {
                deviceName.setText(deviceInfo.getDeviceName()); // Use the name from BluetoothDeviceInfo
            }

            if (deviceAddress != null) {
                deviceAddress.setText(deviceInfo.getDeviceAddress()); // Use the address from BluetoothDeviceInfo
            }
        }

        return convertView;
    }
}
