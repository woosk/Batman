package com.gretec.batman;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

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
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";
	//private ProgressDialog mProgressDialog;
	//private Button 			mButtonStart;
	private Boolean 			mBluetoothEnabled = false;
    
    private static final String FunctionButtonName[] = {
    	"Fuel",
    	"Capacity",
    	"Voltage",
    	"Ampere",
    	"Temperature",
    	"Time"
    };

    private static final Character ReceivedCharacter[] = {
    	'F',
    	'C',
    	'V',
    	'A',
    	'T',
    	'M' //time
    };

//    private static final Integer ID_PROGRESS_BAR[] = {
//    	R.id.circular_progress_1,
//    	R.id.circular_progress_2,
//    	R.id.circular_progress_3,
//    	R.id.circular_progress_4,
//    	R.id.circular_progress_5,
//    	R.id.circular_progress_6,
//    };
    
    private static final Integer ID_FUNCTION_BUTTON[] = {
    	R.id.button_fuel,
    	R.id.button_capacity,
    	R.id.button_voltage,
    	R.id.button_ampere,
    	R.id.button_temperature,
    	R.id.button_time,
    };
    
    private static final int REQUEST_DISCOVERY	= 1;
	private static final int REQUEST_ENABLE_BT = 2;
	
	private BluetoothAdapter mBluetoothAdapter;
    
	private Button mBtnConnect;		// button_connect
	private Button mBtnTimerStart;
	private Button mBtnSendMessage;	// buttonSendMessage

	private Button mBtnFunction[] = new Button[6];	// Function buttons

    //private ProgressBar mProgress[] = new ProgressBar[ID_PROGRESS_BAR.length];

    private ProgressBar 	mProgress;
	private TextView 		mProgressTitle;

	private TextView mTVStatus;
	private TextView mTVReceivedMessage;
	
	private Set<BluetoothDevice> mBTPairedDevices = null;
	private ArrayAdapter<String> mBTDeviceInfosAdapter = null;
	
	private static final int MSG_SEND_DATA_BUTTON_ENABLED = 100;
	private static final int MSG_BLUETOOTH_READ_DATA = 101;
	
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
    
    private int updateCount = 0;
    
    private View.OnClickListener mSendClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Log.d(TAG, "Send to device " + updateCount%2);
			//updateDeviceInfo();
			if (v.getId() == R.id.buttonStart) {
				updateTimerToggle();
			}
			else if (v.getId() == R.id.buttonSendMessage) {
				onSendMessage();
			}
			else if (v.getId() == R.id.button_connect) {
				onConnectToDevice();
			}
			else {
				final int id = v.getId();
				ArrayList<Integer> functionButtonArray = new ArrayList<Integer>(Arrays.asList(ID_FUNCTION_BUTTON));
				if (functionButtonArray.contains(id)) {
					int idx = functionButtonArray.indexOf(id);
					onFunctionButton(idx);
				}
			}
		}
	};
	
	private void onConnectToDevice() {
		showDialogBluetoothDevices();
	}
	
	private void onFunctionButton(int idx) {
		if (mBTSocket.isConnected()) {
			char a = (char)(((int)'a')+idx);
			try {
				mBTOutputStream.write((int)a);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void onSendMessage() {
		if (mBTSocket.isConnected()) {
			EditText editText = (EditText) findViewById(R.id.send_data_string);
			Editable edit = editText.getText();
			if (edit.length()<=0) {
				mTVStatus.setText("문자를 입력하세요.");
				return;
			}
			char a = edit.charAt(0);
			try {
				mBTOutputStream.write((int)a);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private boolean mUpdateTimerStarted = false;
	private Timer mUpdateTimer;
    
	private void updateTimerToggle() {
		if (mUpdateTimerStarted) {
			mUpdateTimer.cancel();
			mUpdateTimer.purge();
			updateCount = updateCount%2;
			mUpdateTimerStarted = false;
		}
		else {
			mUpdateTimer = new Timer();
			mUpdateTimer.scheduleAtFixedRate(new TimerTask() {
	
				@Override
				public void run() {
					updateDeviceInfo();
					++updateCount;
				}
				
			}, 0, 1000);
			mUpdateTimerStarted = true;
		}
	}
	
	private void updateDeviceInfo() {
		if (mBTSocket.isConnected()) {
			//mSendButton.setEnabled(true);
			//Character a = Character.valueOf('A');
			char a = (char)(((int)'a')+updateCount%6);
			try {
				Log.d(TAG, "Send Data : "+a);
				mBTOutputStream.write((int)a);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//mButtonStart = (Button) findViewById(R.id.buttonStart);
		
		BluetoothDevice finalDevice = this.getIntent().getParcelableExtra(	BluetoothDevice.EXTRA_DEVICE);
		BatmanApplication app = (BatmanApplication) getApplicationContext();
		BluetoothDevice currentDevice = app.getDevice();
		if (finalDevice == null) {
			if (currentDevice == null) {
				Log.d(TAG, "Search Device");
				//Intent intent = new Intent(this, SearchDeviceActivity.class);
				//startActivity(intent);
				//finish();
				//return;
			}
		} else if (finalDevice != null) {
			Log.d(TAG, "Bluetooth finalDevice is "+finalDevice);
			app.setDevice(finalDevice);
			new ConnectThread().start();
		}
		
		mBtnConnect = (Button) findViewById(R.id.button_connect);
		mBtnConnect.setOnClickListener(mSendClickListener);
		
		mBtnTimerStart = (Button) findViewById(R.id.buttonStart);
		mBtnTimerStart.setEnabled(false);
		mBtnTimerStart.setOnClickListener(mSendClickListener);
		
		mBtnSendMessage = (Button) findViewById(R.id.buttonSendMessage);
		mBtnSendMessage.setEnabled(false);
		mBtnSendMessage.setOnClickListener(mSendClickListener);
		
		mTVReceivedMessage = (TextView) findViewById(R.id.received_message);
		
		mTVStatus = (TextView) findViewById(R.id.tv_status_message);
		
		for (int i=0; i<ID_FUNCTION_BUTTON.length; i++) {
			mBtnFunction[i] = (Button) findViewById(ID_FUNCTION_BUTTON[i]);
			mBtnFunction[i].setOnClickListener(mSendClickListener);
		}
		
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		
		Resources res = getResources();
		Drawable drawable = res.getDrawable(R.drawable.circular);
		
/*
		Random r = new Random();
		for (int i=0; i<ID_PROGRESS_BAR.length; i++) {
			mProgress[i] = (ProgressBar) findViewById(ID_PROGRESS_BAR[i]);
			mProgress[i].setProgress(r.nextInt(100));   // Main Progress
			//mProgress.setSecondaryProgress(50); // Secondary Progress
			mProgress[i].setMax(100); // Maximum Progress
			mProgress[i].setProgressDrawable(drawable);
			Log.d(TAG, "find progress index="+i+", id="+ID_PROGRESS_BAR[i]+", mProgress[i]="+mProgress[i]);
		}
*/
		mProgress = (ProgressBar) findViewById(R.id.circular_progress_1);
		mProgress.setProgress(0);   // Main Progress
		//mProgress.setSecondaryProgress(50); // Secondary Progress
		mProgress.setMax(100); // Maximum Progress
		mProgress.setProgressDrawable(drawable);
		
		mProgressTitle = (TextView) findViewById(R.id.circular_progress_title);

//		mProgress1 = (ProgressBar) findViewById(R.id.circular_progress_1);
//		mProgress1.setProgress(20);   // Main Progress
//		//mProgress.setSecondaryProgress(50); // Secondary Progress
//		mProgress1.setMax(100); // Maximum Progress
//		mProgress1.setProgressDrawable(drawable);
//		mProgress2 = (ProgressBar) findViewById(R.id.circular_progress_2);
//		mProgress2.setProgress(60);   // Main Progress
//		//mProgress.setSecondaryProgress(50); // Secondary Progress
//		mProgress2.setMax(100); // Maximum Progress
//		mProgress2.setProgressDrawable(drawable);

		IntentFilter intent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		registerReceiver(mBTPairReceiver, intent);
		
	}
	
	class ConnectThread extends Thread
	{
//		BluetoothDevice mDevice;
//		ConnectThread(BluetoothDevice device) {
//			mDevice = device;
//		}
		public void run() {
			BatmanApplication app = (BatmanApplication) getApplicationContext();
			if (app.getDevice()!=null) {
				connect(app.getDevice());
			}
		};
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.layout.menu, menu);
        return true;
    }
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//		if (requestCode == REQUEST_DISCOVERY) {
//			final BluetoothDevice device = data.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//			new Thread() {
//				public void run() {
//					connect(device);
//				};
//			}.start();
//		}
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
		onBluetoothOn();
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		if (mBTSocket != null) {
			try {
				Log.d(TAG, ">>Client Socket Close");
				mBTSocket.close();
				mBTSocket = null;
				// this.finish();
				//return;
			} catch (IOException e) {
				Log.e(TAG, ">>", e);
			}
		}
		
		unregisterReceiver(mBTFoundReceiver);

		if (!mBluetoothEnabled) {
			onBluetoothOff();
		}
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
		Log.e(TAG, "onMenuItemSelected featureId="+featureId+" MenuItemId="+item.getItemId()+" MenuItem="+item);
		
		//item.getItemId() == 장치연결
		if (item.getItemId()==R.id.menu_show_paired_devices) {
			onConnectToDevice();
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
					((BatmanApplication)getApplication()).setDevice(device);
					new ConnectThread().start();
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
				
				final String receivedString = (String)(msg.obj);
				Log.d(TAG, "mBTConnectHandler : writeMessage=" + receivedString);
				mTVReceivedMessage.setText(receivedString);

				parseReceivedMessage(receivedString);

//				char c = receivedString.charAt(0);
//				ArrayList<Character> receivedCharArray = new ArrayList<Character>(Arrays.asList(ReceivedCharacter));
//				Log.d(TAG, "mBTConnectHandler : First=" + c);
//				if (receivedCharArray.contains(Character.valueOf(c))) {
//					int idx = receivedCharArray.indexOf(Character.valueOf(c));
//					String valueString = receivedString.substring(1);
//					Log.d(TAG, "mBTConnectHandler : First=" + c + ", Index=" + idx + ", valueString=" + valueString + ", mProgress[idx]=" + mProgress[idx]);
//					//if (idx!=2) 
//					{
//						Float value = Float.parseFloat(valueString);
//						mProgress[idx].setProgress(value.intValue());
//					}
//				}
				break;
			case MSG_SEND_DATA_BUTTON_ENABLED:
				Log.d(TAG, "mBTConnectHandler : MSG_SEND_DATA_BUTTON_ENABLED=" + msg.arg1);
				mBtnConnect.setEnabled(msg.arg1==1?false:true);
				mBtnTimerStart.setEnabled(msg.arg1==1?true:false);
				mBtnSendMessage.setEnabled(msg.arg1==1?true:false);
				for (int i =0; i< ID_FUNCTION_BUTTON.length; i++) {
					mBtnFunction[i].setEnabled(msg.arg1==1?true:false);
				}
				//mSendButton.setVisibility(visibility);
				break;
			}
			super.handleMessage(msg);
		}

	private void parseReceivedMessage(String receivedString) {
		// TODO Auto-generated method stub
		char c = receivedString.charAt(0);
		ArrayList<Character> receivedCharArray = new ArrayList<Character>(Arrays.asList(ReceivedCharacter));
		Log.d(TAG, "parseReceivedMessage : First=" + c);
		if (receivedCharArray.contains(Character.valueOf(c))) {
			int idx = receivedCharArray.indexOf(Character.valueOf(c));
			String valueString = receivedString.substring(1);
			//Log.d(TAG, "parseReceivedMessage : First=" + c + ", Index=" + idx + ", valueString=" + valueString + ", mProgress[idx]=" + mProgress[idx]);
			Log.d(TAG, "parseReceivedMessage : First=" + c + ", Index=" + idx + ", valueString=" + valueString);
			int value = 0;
			Float tempValue = 0.0f;
			switch (idx) {
			case 0:		// F, Fuel, 			0~100 %
				tempValue = Float.parseFloat(valueString);
				break;
			case 1:		// C, Capacity,			0~10000
				tempValue = (Float.parseFloat(valueString))/100.0f;
				break;
			case 2:		// V, Voltage,			0~15.00V
				tempValue = (Float.parseFloat(valueString)/15.0f)*100.0f;
				break;
			case 3:		// A, current Ampere	0~10000
				tempValue = (Float.parseFloat(valueString))/100.0f;
				break;
			case 4:		// T, Temperature		0~120.0
				tempValue = Float.parseFloat(valueString)/120.0f*100f;
				break;
			case 5:		// M, available tiMe	0~6000
				tempValue = Float.parseFloat(valueString);
				int t = tempValue.intValue();
				int hours = t / 60;
				int minutes = t % 60;
				mTVReceivedMessage.append(String.format("\t 남은시간:%02d시간 %02d분", hours, minutes));
				tempValue = (tempValue)/1440.0f*100.0f;
				break;
			default:
				break;
			}
			mProgressTitle.setText(FunctionButtonName[idx]);
			mProgress.setProgress(tempValue.intValue());
			//Float value = Float.parseFloat(valueString);
			//mProgress[idx].setProgress(value.intValue());
		}		
	}
		
	};
	private final int mBTReadMaxLength = 2048;
	
	protected void connect(BluetoothDevice device) {
		if (device==null) {
			//Toast.makeText(getApplicationContext(), "디바이스가 없습니다.", Toast.LENGTH_LONG).show();
			Log.d(TAG, "디바이스가 없습니다.");
		}
		if (mBTSocket!=null && mBTSocket.isConnected()) {
			Log.d(TAG, "이미 연결 되었습니다. device=" + mBTSocket.getRemoteDevice().getName());
			return;
		}
		try {
			Log.d(TAG, "연결중...");
			// Create a Socket connection: need the server's UUID number of
			// registered
			Method m = device.getClass().getMethod("createRfcommSocket",
					new Class[] { int.class });
			mBTSocket = (BluetoothSocket) m.invoke(device, 1);
			mBTSocket.connect();
			Log.d(TAG, ">>Client connected");

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
			
			mBTConnectHandler.obtainMessage(MSG_SEND_DATA_BUTTON_ENABLED, 1, 0).sendToTarget();

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
								Log.d(TAG, "test1=" + outString.toString());
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
					mBTConnectHandler.obtainMessage(MSG_SEND_DATA_BUTTON_ENABLED, 0, 0).sendToTarget();
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
