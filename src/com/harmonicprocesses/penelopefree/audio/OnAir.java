package com.harmonicprocesses.penelopefree.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioRecord.OnRecordPositionUpdateListener;
import android.media.AudioTrack;
import android.media.MediaRecorder;

public class OnAir {
	private Runnable mRunnable;
	private Thread mThread;
	protected AudioRecord mAudioRecord;
	private AudioTrack mAudioTrack;
	
	public OnAir(AudioRecord audioRecord, AudioTrack audioTrack){
		mAudioRecord = audioRecord;
		mAudioTrack = audioTrack;
		
		mRunnable = new Runnable() {

			@Override
			public void run() {
				
			}
		};
		
		mThread = new Thread(mRunnable, "OnAir");
		
	}
	
	AudioRecord.OnRecordPositionUpdateListener mRecordListener =
			new AudioRecord.OnRecordPositionUpdateListener() {
				
				@Override
				public void onPeriodicNotification(AudioRecord recorder) {
					//Read();
					
				}
				
				@Override
				public void onMarkerReached(AudioRecord recorder) {
					// has to include this stub					
				}
	};
	
	public void start(){
		mThread.start();
	}
}
