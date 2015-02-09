package com.gretec.batman;

import android.app.Application;
import android.bluetooth.BluetoothDevice;

public class BatmanApplication extends Application {

	private BluetoothDevice mDevice = null;

	public BluetoothDevice getDevice() {
		return mDevice;
	}

	public void setDevice(BluetoothDevice device) {
		this.mDevice = device;
	}
}
