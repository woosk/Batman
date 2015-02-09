package com.gretec.batman;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";
	//private ProgressDialog mProgressDialog;
	//private Button 			mButtonStart;
	private Boolean 			mBluetoothEnabled = false;
    private ProgressBar 		mProgress;
    
	private static final int REQUEST_ENABLE_BT = 1;
	private BluetoothAdapter mBluetoothAdapter;
    
	private Button mSendButton;
	
	private Set<BluetoothDevice> mBTPairedDevices = null;
	private ArrayAdapter<String> mBTDeviceInfosAdapter = null;
	
	private static final int MSG_BLUETOOTH_READ_DATA = 100;
	
	private final BroadcastReceiver mBTFoundReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// add the name and the MAC address of the object to the
				// arrayAdapter
				mBTDeviceInfosAdapter.add(device.getName() + "\n" + device.getAddress());
				mBTDeviceInfosAdapter.notifyDataSetChanged();
			}
		}
	};
	
	private final BroadcastReceiver mBTPairReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
 
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                 final int state        = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                 final int prevState    = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
 
                 if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                     Toast.makeText(context, "Paired", Toast.LENGTH_SHORT).show();
                 } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                     Toast.makeText(context, "Unpaired", Toast.LENGTH_SHORT).show();
                     //showToast("Unpaired");
                 }
 
            }
        }
    };
    
    private static int clickCount = 0;
    
    private View.OnClickListener mSendClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Log.d(TAG, "Send to device" + clickCount%2);
			if (mBTSocket.isConnected()) {
				//mSendButton.setEnabled(true);
				//Character a = Character.valueOf('A');
				char a = clickCount%2==1? '0':'1';
				try {
					mBTOutputStream.write((int)a);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			++clickCount;
		}
	};
    
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//mButtonStart = (Button) findViewById(R.id.buttonStart);
		
		mSendButton = (Button) findViewById(R.id.buttonStart);
		//mSendButton.setEnabled(false);
		mSendButton.setOnClickListener(mSendClickListener);
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		
		Resources res = getResources();
		Drawable drawable = res.getDrawable(R.drawable.circular);
		
		mProgress = (ProgressBar) findViewById(R.id.circularProgressbar);
		mProgress.setProgress(25);   // Main Progress
		//mProgress.setSecondaryProgress(50); // Secondary Progress
		mProgress.setMax(100); // Maximum Progress
		mProgress.setProgressDrawable(drawable);
		
		IntentFilter intent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		registerReceiver(mBTPairReceiver, intent);
		
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.layout.menu, menu);
        return true;
    }
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if (requestCode == REQUEST_ENABLE_BT) {
			if (mBluetoothAdapter.isEnabled()) {
				//text.setText("Status: Enabled");
			} else {
				//text.setText("Status: Disabled");
			}
		}
	}
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		onBluetoothOn();
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		if (!mBluetoothEnabled) {
			onBluetoothOff();
		}
		unregisterReceiver(mBTFoundReceiver);
		super.onDestroy();
	}
	

	public void onBluetoothOn() {
		if (!mBluetoothAdapter.isEnabled()) {
			Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);

			Toast.makeText(getApplicationContext(), "Bluetooth turned on", Toast.LENGTH_LONG).show();
		}
		else {
			mBluetoothEnabled = true;
			Toast.makeText(getApplicationContext(), "Bluetooth is already on", Toast.LENGTH_LONG).show();
		}

		mBTPairedDevices = mBluetoothAdapter.getBondedDevices();
	}


	public void onBluetoothOff() {
		mBluetoothAdapter.disable();
		//text.setText("Status: Disconnected");

		Toast.makeText(getApplicationContext(), "Bluetooth turned off", Toast.LENGTH_LONG).show();
	}

	public void onBluetoothDeviceFind() {
		if (mBluetoothAdapter.isDiscovering()) {
			// the button is pressed when it discovers, so cancel the discovery
			mBluetoothAdapter.cancelDiscovery();
		} else {
			mBTDeviceInfosAdapter.clear();
			mBluetoothAdapter.startDiscovery();

			registerReceiver(mBTFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
		}
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		// TODO Auto-generated method stub
		
		Log.e(TAG, "onMenuItemSelected featureId="+featureId+" MenuItemId="+item.getItemId()+" MenuItem="+item);
		
		//item.getItemId() == 장치연결
		if (item.getItemId()==R.id.menu_show_paired_devices) {
			//onConnectDevice();
			showDialogBluetoothDevices();
		}
		
		return super.onMenuItemSelected(featureId, item);
	}

	private void pairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	private void unpairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	
	private void showDialogBluetoothDevices() {
		//String names[] ={"A","B","C","D"};
		mBTDeviceInfosAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		for (BluetoothDevice device : mBTPairedDevices) {
			mBTDeviceInfosAdapter.add(device.getName() + "\n" + device.getAddress());
		}
		
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
		LayoutInflater inflater = getLayoutInflater();
		View convertView = (View) inflater.inflate(R.layout.list_dialog, null);
		alertDialogBuilder.setView(convertView);
		alertDialogBuilder.setTitle("Paired Devices");
//		ListView lv = (ListView) convertView.findViewById(R.id.list_dialog_listView1);
//		lv.setAdapter(mBTDeviceInfosAdapter);
		
		alertDialogBuilder.setAdapter(mBTDeviceInfosAdapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String strName = mBTDeviceInfosAdapter.getItem(which);
				Log.e(TAG, "selected "+strName);
				final BluetoothDevice device = (BluetoothDevice)(mBTPairedDevices.toArray()[which]);
				if (device!=null) {
					new Thread() {
						public void run() {
							connect(device);
						};
					}.start();
				}
			}
		});

		alertDialogBuilder.setNegativeButton("cancel",
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
		
		alertDialogBuilder.show();
	}
	
	private BluetoothSocket mBTSocket;
	private InputStream mBTInputStream;
	private OutputStream mBTOutputStream;
	private Object mBTConnectSyncObject = new Object();
	private Handler mBTConnectHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
//			byte[] writeBuf = (byte[]) msg.obj;
//			int begin = (int) msg.arg1;
//			int end = (int) msg.arg2;

			switch (msg.what) {
			case MSG_BLUETOOTH_READ_DATA:
				//String writeMessage = new String(writeBuf);
				//writeMessage = writeMessage.substring(begin, end);
				
				//Log.d(TAG, "mBTConnectHandler : writeMessage=" + writeMessage);
				Log.d(TAG, "mBTConnectHandler : writeMessage=" + (String)(msg.obj));
				break;
			}
			super.handleMessage(msg);
		}
		
	};
	private final int mBTReadMaxLength = 2048;
	
	protected void connect(BluetoothDevice device) {
		try {
			Log.d(TAG, "연결중...");
			// Create a Socket connection: need the server's UUID number of
			// registered
			Method m = device.getClass().getMethod("createRfcommSocket",
					new Class[] { int.class });
			mBTSocket = (BluetoothSocket) m.invoke(device, 1);
			mBTSocket.connect();
			Log.d(TAG, ">>Client connectted");

//			if (mBTSocket.isConnected()) {
//				mSendButton.setEnabled(true);
//			}
			mBTInputStream = mBTSocket.getInputStream();
			mBTOutputStream = mBTSocket.getOutputStream();
			int read = 0;
			int begin = 0;
			final byte[] buffer = new byte[mBTReadMaxLength];
			Arrays.fill( buffer, (byte) 0 );
			final StringBuilder outString = new StringBuilder();
			while (true) {
				synchronized (mBTConnectSyncObject) {
					/*
					read += mBTInputStream.read(buffer, read, buffer.length - read);
					for(int i = begin; i < read; i++) {
						outString.append(Character.toChars(buffer[i]));

						if(buffer[i] == "#".getBytes()[0]) {
						//if(buffer[i] == 0) {
							Log.d(TAG, "test1:" + outString.toString());
							mBTConnectHandler.obtainMessage(1, begin, i, buffer).sendToTarget();

							int end = outString.length();
							begin = i + 1;
							if(i == read - 1) {
								read = 0;
								begin = 0;
								outString.delete(0, end);
							}
						}
					}
					*/
					
					read = mBTInputStream.read(buffer);
					Log.d(TAG, "read:" + read);
					if (read > 0) {
						final int count = read;
						for(int i=0; i < count; i++) {
							outString.append(Character.toChars(buffer[i]));
							Log.d(TAG, "append: count="+i+" char="+new String(Character.toChars(buffer[i])) + " code=" +buffer[i]);
							//Log.d(TAG, "append:"+buffer[i]);
							if (buffer[i]==10) {	// Carriage return: 13, Line feed: 10
								Log.d(TAG, "test1:" + outString.toString());
								mBTConnectHandler.obtainMessage(MSG_BLUETOOTH_READ_DATA, begin, outString.toString().length(), outString.toString()).sendToTarget();
								outString.setLength(0);
							}
						}
						//outString.append(buffer, 0, read);
						//String str = new String(buffer);
						
/*
 						String str = SamplesUtils.byteToHex(buffer, count);

//						Log.d(TAG, "test1:" + str);
						String hex = hexString.toString();
						if (hex == "") {
							hexString.append("<--");
						} else {
							if (hex.lastIndexOf("<--") < hex.lastIndexOf("-->")) {
								hexString.append("\n<--");
							}
						}
						hexString.append(str);
						hex = hexString.toString();
//						Log.d(TAG, "test2:" + hex);
						if (hex.length() > mBTReadMaxLength) {
							try {
								hex = hex.substring(hex.length() - mBTReadMaxLength,
										hex.length());
								hex = hex.substring(hex.indexOf(" "));
								hex = "<--" + hex;
								hexString = new StringBuffer();
								hexString.append(hex);
							} catch (Exception e) {
								e.printStackTrace();
								Log.e(TAG, "e", e);
							}
						}
*/
						mBTConnectHandler.post(new Runnable() {
							public void run() {
							}
						});
					}
				}
			}
		} catch (Exception e) {
			Log.e(TAG, ">>", e);
			Toast.makeText(getBaseContext(),
					//getResources().getString(R.string.ioexception),
					"IO Exception",
					Toast.LENGTH_SHORT).show();
			return;
		} finally {
			if (mBTSocket != null) {
				try {
					Log.d(TAG, ">>Client Socket Close");
					mBTSocket.close();
					mBTSocket = null;
					// this.finish();
					return;
				} catch (IOException e) {
					Log.e(TAG, ">>", e);
				}
			}
		}
	}
}
