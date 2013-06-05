package com.harmonicprocesses.penelopefree.audio;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.harmonicprocesses.penelopefree.audio.AudioThru.OnNewSamplesReceivedUpdateListener;

public class ProcHandler extends Handler{
	public ProcHandler(Looper myLooper) { 
        super(myLooper);
    }
	
    public void handleMessage(Message msg) { 
    	newSamplesListener.onNewSamplesReceived((float[]) msg.obj);
    }
    
	public OnNewSamplesReceivedUpdateListener newSamplesListener = new OnNewSamplesReceivedUpdateListener(){

		@Override
		public void onNewSamplesReceived(float[] in_buff) {
			//inputBuffer = in_buff;
		}
		
	};
}
