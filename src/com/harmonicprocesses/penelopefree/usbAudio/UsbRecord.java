package com.harmonicprocesses.penelopefree.usbAudio;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.media.AudioRecord;

public class UsbRecord {
	public volatile byte[] inputBuffer;
	private static int TIMEOUT = 0;
	private boolean forceClaim = true;
	private UsbDeviceConnection Usbconnection;
	private UsbEndpoint endpoint;
	
	public UsbRecord(UsbInterface intf, UsbDeviceConnection connection){
		endpoint = intf.getEndpoint(0);
		Usbconnection = connection; 
		connection.claimInterface(intf, forceClaim);
		inputBuffer = new byte[endpoint.getMaxPacketSize()];
	}
	
	public byte[] run(){
		UsbReadThread.run();
		return inputBuffer;
	}
	
	
	private Thread UsbReadThread = new Thread(new Runnable(){

		@Override
		public void run() {
			Usbconnection.bulkTransfer(endpoint, inputBuffer, inputBuffer.length, TIMEOUT); //do in another thread
			
		}
		
	});
	
    //---------------------------------------------------------
    // Interface definitions
    //--------------------
    /**
     * Interface definition for a callback to be invoked when an AudioRecord has
     * reached a notification marker set by {@link AudioRecord#setNotificationMarkerPosition(int)}
     * or for periodic updates on the progress of the record head, as set by
     * {@link AudioRecord#setPositionNotificationPeriod(int)}.
     */
    public interface OnRecordPositionUpdateListener  {
        /**
         * Called on the listener to notify it that the previously set marker has been reached
         * by the recording head.
         */
        void onMarkerReached(AudioRecord recorder);
        
        /**
         * Called on the listener to periodically notify it that the record head has reached
         * a multiple of the notification period.
         */
        void onPeriodicNotification(AudioRecord recorder);
    }
}
