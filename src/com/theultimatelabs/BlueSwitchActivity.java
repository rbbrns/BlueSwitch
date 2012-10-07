package com.theultimatelabs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class BlueSwitchActivity extends Activity {
    private static final int REQUEST_ENABLE_BT = 5;
    private static final String TAG = "BlueSwitchActivity";
    //private static final String DEVICE_ADDR = "00:12:03:10:00:32";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//000001101-0000-1000-80000-00805f9b34fb
	private BluetoothDevice[] mBluetoothDevice = new BluetoothDevice[2];
	private BluetoothSocket[] mBluetoothSocket = new BluetoothSocket[2];
	private OutputStream[] mBluetoothOutputStream = new OutputStream[2];
	private InputStream[] mBluetoothInputStream = new InputStream[2];
	private boolean[] connected = {false, false};
	private Handler mHandler;
	private Button mToggleButton;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mHandler = new Handler();        
       
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
        	Toast.makeText(this, "Bluetooth Not Supported", Toast.LENGTH_LONG);
            this.finish();
        }
        
        if (!bluetoothAdapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 0);
        }
        
        
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
                
        if (BluetoothAdapter.getDefaultAdapter().startDiscovery()) {
        	Toast.makeText(this, "Bluetooth Not Enabled", Toast.LENGTH_LONG);
        }
		
		mToggleButton = (Button) this.findViewById(R.id.ButtonToggle);
		
		mToggleButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				try {
					Log.v(TAG,"TOGGLE");
					mBluetoothOutputStream[0].write((int)'T');
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
        });
		
		this.findViewById(R.id.ButtonCalibrate).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				try {
					Log.v(TAG,"OFF");
					mBluetoothOutputStream[0].write((int)'C');
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
        });
		
		this.findViewById(R.id.ButtonRescan).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.v(TAG,"SCAN");
				boolean success = BluetoothAdapter.getDefaultAdapter().startDiscovery();
				Log.v(TAG,String.format("discovery start: %s",new Boolean(success).toString()));
			}
        });
     
    }
 
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.v(TAG,"onReceive");
			String action = intent.getAction();
			boolean success = BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
	        // When discovery finds a device
	        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
	            // Get the BluetoothDevice object from the Intent
	        	int device = connected[0] ? 1 : 0;
	        	
	            mBluetoothDevice[device] = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	            short RSSI = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) -1);
	           
	            // Add the name and address to an array adapter to show in a ListView
	            //foundDeviceDescriptions.add(device.getName() + "\n" + device.getAddress());
	            Log.v(TAG,mBluetoothDevice[device].getName() + " " + mBluetoothDevice[device].getAddress() + " " + new Short(RSSI).toString() + "dB");
	            
	            
	            try {
	            	//mHandler.post(runnable);
	            	Log.v(TAG,"creating socket");
	     			mBluetoothSocket[device] = mBluetoothDevice[device].createInsecureRfcommSocketToServiceRecord(MY_UUID);
	     			Log.v(TAG,"connecting");
	     			try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	     			mBluetoothSocket[device].connect();
	     			Log.v(TAG,"Connected");
	     			connected[device] = true;
	     			mBluetoothInputStream[device] = mBluetoothSocket[device].getInputStream();
	     			mBluetoothOutputStream[device] = mBluetoothSocket[device].getOutputStream();	
	     			Log.v(TAG,"Start reader");
	     			new BluetoothReader().execute(null);
	     			Log.v(TAG,"Send start byte");
	     			mBluetoothOutputStream[device].write(new String("S").getBytes());
	     			
	     		} catch (IOException e) {
	     			// TODO Auto-generated catch block
	     			e.printStackTrace();
	     		}
	        }
		    
		}
	};
    
	
	private class BluetoothReader extends AsyncTask<Void, Boolean, Void> {

	     protected void onProgressUpdate(Boolean... progress) {
	    	 if(progress[0]) {
	    		 mToggleButton.setText("OFF");
	    		 mToggleButton.setTextColor(Color.BLACK);
	    		 mToggleButton.setBackgroundColor(Color.WHITE);
	    	 }
	    	 else {
	    		 mToggleButton.setText("ON");
	    		 mToggleButton.setTextColor(Color.WHITE);
	    		 mToggleButton.setBackgroundColor(Color.BLACK);
	    	 }
	    		 
	     }

	     protected void onPostExecute(Void result) {
	     }

		@Override
		protected Void doInBackground(Void... params) {
			while(true) {
				try {
					String msg = "";
					while(true) {
						int c = mBluetoothInputStream[0].read();
						//Log.v(TAG,String.format("%d %x %c",c,c,c));
						if (c==-1) {
							Log.e(TAG,"End of Stream");
						}
						else if ((char)c == '\n') {
							Log.v(TAG,"Bluetooth>>>"+msg);
							msg = "";
						}
						else if ((char)c == '\r') {
							
						}
						else if ((char)c == '1') {
							Log.v(TAG,"ON");
							this.publishProgress(true);
						}
						else if ((char)c == '0') {
							Log.v(TAG,"OFF");
							this.publishProgress(false);
						}
						else {
							msg += (char)c;
						}
					}
				} catch (IOException e) {
					Log.e(TAG,"crashed reading bluetooth inputsream");
					e.printStackTrace();
				}
				
			}
		}
	 }
	 
	 
    
}