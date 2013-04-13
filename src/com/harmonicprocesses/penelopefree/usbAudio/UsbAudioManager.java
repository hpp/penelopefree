package com.harmonicprocesses.penelopefree.usbAudio;

import java.util.HashMap;
import java.util.Iterator;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class UsbAudioManager {
	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent; 
	private static final String ACTION_USB_PERMISSION =
		    "com.harmonicprocesses.penelopefree.USB_PERMISSION";
	private UsbInterface inputInterface = null;
	private UsbInterface outputInterface = null;
	public UsbRecord mUsbRecord;
	private UsbDevice mDevice = null;
	private UsbDeviceConnection mConnection = null;

	
	public UsbAudioManager(Context context){
		mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		context.registerReceiver(mUsbReceiver, filter);
		detectUSB();
		mConnection = mUsbManager.openDevice(mDevice);
		if (inputInterface!=null&&mConnection!=null){
			mUsbRecord = new UsbRecord(inputInterface, mConnection);
		}
	}
	
	/**
	 * Get a list of all the audio device connected via USB hub.
	 */
	private void detectUSB() {
		HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		while(deviceIterator.hasNext()){
		    UsbDevice device = deviceIterator.next();
		    mUsbManager.requestPermission(device, mPermissionIntent); // request permission to speak to device
		    if (device.getDeviceClass()==UsbConstants.USB_CLASS_AUDIO) { //check if USB device is an audio device
		    	for (int i = 0; i < device.getInterfaceCount(); i++){
		    		UsbInterface audioInterface = device.getInterface(i);
		    		if (checkUSBInterface(audioInterface, i)){
		    			mDevice = device;
		    		}
		    	}
		    } else if (device.getDeviceClass()==UsbConstants.USB_CLASS_PER_INTERFACE) {
		    	for (int i = 0; i < device.getInterfaceCount(); i++){
		    		UsbInterface audioInterface = device.getInterface(i);
		    		if (audioInterface.getInterfaceClass()==UsbConstants.USB_CLASS_AUDIO){
			    		if (checkUSBInterface(audioInterface, i)){
			    			mDevice = device;
			    		}
		    		} else {
		    			for (int j = 0; j < audioInterface.getEndpointCount(); j++){
			    			UsbEndpoint audioEndpoint = audioInterface.getEndpoint(j);
			    			String description = audioEndpoint.toString();
			    			int subClassID = audioInterface.getInterfaceSubclass();
							Log.d("USBOutputDevice", "Inteface=" + i + 
									"Description=" + description + "; SubClass = " + subClassID +
									"; Device name = " + device.getDeviceName());
		    			}
		    		}
		    	}
		    }
		}
	}
		
	private boolean checkUSBInterface(UsbInterface usbInterface, int i) {
		boolean bool = false;
		for (int j = 0; j < usbInterface.getEndpointCount(); j++){
			UsbEndpoint audioEndpoint = usbInterface.getEndpoint(j);
			String description = audioEndpoint.toString();
			int subClassID = usbInterface.getInterfaceSubclass();
			int descriptor = usbInterface.describeContents();
			int describer = audioEndpoint.describeContents();
			
						
			if (audioEndpoint.getDirection()==UsbConstants.USB_DIR_IN){ // Is an audio input device ...
				// add input audio device to arraylist
				if (inputInterface==null){
					bool = true;
					inputInterface = usbInterface;
				}
				//String[] inputDeviceArray = getResources().getStringArray(R.array.input_devices_entries);
				//inputDeviceArray = description;
				//getStringArray(R.array.input_devices_keys).addItem("Inteface=" + i + ",EndPoint=" + j);
				Log.d("USBInputDevice", "Inteface=" + i + ",EndPoint=" + j + 
						"Description=" + description + "; SubClass = " + subClassID +
						"; Protocol = " + usbInterface.getInterfaceProtocol() +
						"; Descriptor = " + descriptor + "; Describer = " + describer);
			}else if (audioEndpoint.getDirection()==UsbConstants.USB_DIR_OUT){ // Is an audio output device ... 
				// add output audio device to arraylist
				if (outputInterface==null){
					bool = true;
					outputInterface = usbInterface;
				}
				//getStringArray(R.array.output_devices_entries).addItem(description);
				//getStringArray(R.array.output_devices_keys).addItem("Inteface=" + i + ",EndPoint=" + j);
				Log.d("USBOutputDevice", "Inteface=" + i + ",EndPoint=" + j + 
						"Description=" + description + "; SubClass = " + subClassID +
						"; Protocol = " + usbInterface.getInterfaceProtocol() +
						"; Descriptor = " + descriptor + "; Describer = " + describer);
			}
		}
		return bool;
	}
	
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {
		    String action = intent.getAction();
		    if (ACTION_USB_PERMISSION.equals(action)) {
		        synchronized (this) {
		            UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

		            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
		                if(mConnection == null){
		                	//call method to set up device communication
		                	//detectUSB();
		                	mConnection = mUsbManager.openDevice(device);
		                }
		            } else {
		            	Log.d("com.harmonicprocesses.penelopefree.usbAudio.UsbAudioManager", "permission denied for device " + device);
		            }
		        }
		    }
		}
	};
	
	public void close(Context context){
		context.unregisterReceiver(mUsbReceiver);
	}
	

}
