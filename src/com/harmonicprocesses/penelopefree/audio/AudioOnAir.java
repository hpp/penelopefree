package com.harmonicprocesses.penelopefree.audio;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.harmonicprocesses.penelopefree.PenelopeMainActivity;
import com.harmonicprocesses.penelopefree.R;
import com.harmonicprocesses.penelopefree.openGL.MyGLRenderer;
import com.harmonicprocesses.penelopefree.openGL.MyGLSurfaceView;
import com.harmonicprocesses.penelopefree.settings.SettingsActivity;
import com.harmonicprocesses.penelopefree.usbAudio.UsbAudioManager;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.media.audiofx.PresetReverb;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.renderscript.Double2;



/**
 * A Class for handling the turning on and off of the On Air Switch
 * Also handles the hand off of inputAudio to outputAudio
 * 
 */
public class AudioOnAir {
	
	private boolean onAirBool=false, inputBufferReady4Proc=false, outputBufferReady4Proc=false,
			inputBufferReadyForRead=false,outputBufferReadyForRead=false;
	
	private Button onAirButton;
	
	private AudioManager mAudioManager;
	
	private TextView backgroundText;
	
	private CharSequence tempStringHolder;
	
	private Context mContext;
	private MyGLRenderer mGLRenderer;
	private MyGLSurfaceView mGLView;
	
	private AudioRecord mAudioRecord; 
	private AudioTrack mAudioTrack;
	private int mAudioTrackBufferSize;
	//private AudioRecord.OnRecordPositionUpdateListener mOnRecordListener; 
	
	private int bufferSize = 8*512, sampleRate = 44100, writeOffset = 0, wave_index_tracker = 0;
	
	private ByteBuffer inputBuffer = ByteBuffer.allocateDirect(bufferSize);

	private byte[] outputBuffer = new byte[bufferSize];
	private byte[] mAudioTrackBuffer;
	private int mAudioTrackIdx = 0;
	public float[] processBuffer = new float[bufferSize/2];
	
	private double[] wave_samples;
	private double phase = 0.0;
	
	private DSPEngine dsp;
	private  Modal mModal;
	private OnAir mOnAir;
	public static int[] NoteSpectrum;
	private SharedPreferences mSharedPref;
	private UsbAudioManager mUsbAudioManager;
		
	public final static int numChannels = 1;
	int numRecChannels;

	public final static int numBytePerFrame=2;

	private int numTrackChannels;
	PresetReverb plateReverb;
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public AudioOnAir(Button OnAirButton, TextView textView, UsbAudioManager usbAM) {
		mUsbAudioManager = usbAM;
		onAirButton = OnAirButton;
		backgroundText = textView;
		mContext = textView.getContext();
		mAudioManager =	(AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		dsp = new DSPEngine(bufferSize>>2, sampleRate, mContext);
		NoteSpectrum = dsp.noteFactor;
		
		//Setup input buffer
		int AudioRecordBufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, 
				AudioFormat.ENCODING_PCM_16BIT);
		Log.i("com.hp.vocalx.AudioOnAir", "Track buffer size " + mAudioTrackBufferSize +
				"; Record Buffersize = " + AudioRecordBufferSize);
		
		while (AudioRecordBufferSize<bufferSize){
			AudioRecordBufferSize *= 2;
		}
		
		AudioRecordBufferSize += (AudioRecordBufferSize%bufferSize);
		mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,  sampleRate, 
				AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, AudioRecordBufferSize);
		mAudioRecord.setPositionNotificationPeriod(bufferSize/numBytePerFrame); // in frames, two bytes per frame pcm16 and monoChannel
		mAudioRecord.setRecordPositionUpdateListener(mRecordListener); 
		numRecChannels	= mAudioRecord.getChannelCount();
		mAudioRecord.startRecording();
		mAudioRecord.read(inputBuffer, sampleRate);
		AcousticEchoCanceler.create(mAudioRecord.getAudioSessionId());
		NoiseSuppressor.create(mAudioRecord.getAudioSessionId());
		
		
		//Setup output buffer
		mAudioTrackBufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, 
				AudioFormat.ENCODING_PCM_16BIT);
		mAudioTrackBufferSize += bufferSize - (mAudioTrackBufferSize % bufferSize);
		//mAudioTrackBufferSize *= 10;
		mAudioTrackBuffer = new byte[mAudioTrackBufferSize];
		mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, 
				AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 
				mAudioTrackBufferSize, AudioTrack.MODE_STREAM);
		//mAudioTrack.setPositionNotificationPeriod(bufferSize >> 1);
		//mAudioTrack.setPlaybackPositionUpdateListener(mPlaybackListener);
		Log.i("com.hp.vocalx.AudioOnAir", "format record = " + mAudioRecord.getAudioFormat() + 
				"; sample rate record = " + mAudioRecord.getSampleRate()
				+ "; format track = " + mAudioTrack.getAudioFormat() + 
				"; sample Rate Track " + mAudioTrack.getSampleRate() + 
				"; track playback rate " + mAudioTrack.getPlaybackRate());
		Log.i("com.hp.vocalx.AudioOnAir", "Track buffer size " + mAudioTrackBufferSize +
				"; Record Buffersize = " + AudioRecordBufferSize);
		//mOnAir = new OnAir(mAudioRecord, mAudioTrack);
		wave_samples = CalcWave();
		numTrackChannels = mAudioTrack.getChannelCount();
		
		//
		plateReverb = new PresetReverb(0, mAudioTrack.getAudioSessionId());
		plateReverb.setPreset(PresetReverb.PRESET_PLATE);
				
		mSharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
		mAudioRecord.stop();
	}
	
	Handler mAudioEventHandler = new Handler(Looper.getMainLooper()){
				
	};
	
	public void Toggle(MyGLSurfaceView gLView) {
		//tempStringHolder = onAirButton.getText();
		//onAirButton.setText(backgroundText.getText());
		//backgroundText.setText(tempStringHolder);
		mGLView = gLView;
		
		//backgroundText.setContent(mGLView);

		 
		if (tempStringHolder == mContext.getString(R.string.OnAirTrue)){
			onAirBool = true;
			if (mSharedPref.getBoolean("enable_reverb_key", true)){
				plateReverb.setEnabled(true);
			}
			StartAudio();
		}else {
			onAirBool = false;
			if (plateReverb.getEnabled()){
				plateReverb.setEnabled(false);
			}
			StopAudio();
		}
			
	}
	
	public void StartAudio() {
		if (!onAirBool == true) {return;}
		String inputDevice = mSharedPref.getString("input_device_key", mContext.getString(R.string.input_default));
	
		if (inputDevice.contains(mContext.getString(R.string.input_default))){
			mAudioRecord.startRecording();
			//readThread.start();
			Read();
			int RecordingState = mAudioRecord.getRecordingState();
		} else if (inputDevice.matches(mContext.getString(R.string.inUSB_default))) {
			readByteArray(mUsbAudioManager.mUsbRecord.run()); 
		}
		

		
		
		mAudioTrack.play();
		//writeThread.start();
		Write();
		int PlayState = mAudioTrack.getPlayState();
		
		processThread.run();
		Thread.State processState = processThread.getState();
		
		//Log.i("OnAir",mAudioManager.isMicrophoneMute() + " " + mAudioManager.isMusicActive()
		//		+ " " + mAudioManager.isSpeakerphoneOn() + " " + mAudioManager.getMode() +
		//		"; PlayState = " + PlayState + "; RecordingState = " + RecordingState +
		//		"; Process State = " + processState);
	}
	
	public void StopAudio() {
		if (!onAirBool == false) {return;}
		mAudioRecord.stop();
		mAudioTrack.pause();
	}
	
	public void kill(){
		mAudioRecord.release();
		mAudioTrack.release();
	}
	
	private Thread readThread = new Thread(new Runnable() {
		@Override
		public void run() {
			Read();
		}
	}, "OnAirRead");
	
	private class readTask extends AsyncTask <Void, Void, Void>{

		@Override
		protected Void doInBackground(Void... arg0) {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override 
		protected void onPostExecute(Void arg0){
		
		
		}
		
	}
	

	
	private void Read() {
		inputBufferReadyForRead = true;
		//ProcessBuffer();
		mAudioRecord.read(inputBuffer, sampleRate);
		//ProcessBuffer();
		//Log.i("OnAir",mAudioManager.isMicrophoneMute() + " " + mAudioManager.isMusicActive()
		//		+ " " + mAudioManager.isSpeakerphoneOn() + " " + mAudioManager.getMode());
		//outputBuffer = TestSound();//inputBuffer.array();
		//reverse(outputBuffer);
		//Write();
		byte[] tempByte = inputBuffer.array();
		readByteArray(tempByte);

		//ProcessBuffer();
	}
	
	private void readByteArray(byte[] tempByte) {
		for (int i = 0; i < tempByte.length / (numRecChannels*numBytePerFrame); i++){
			if (numRecChannels == 1){
				short tempShort = (short) ((tempByte[2*i+1]<<8) + tempByte[2*i]); 
				processBuffer[i] = (float) (tempShort / Math.pow(2,15)); 
			} //TODO else for stereo type record.
			
		}
	}

	private Thread writeThread = new Thread(new Runnable() {

		@Override
		public void run() {
			
		}
		
	}, "OnAirWrite");
	
	private class writeTask extends AsyncTask <Void, Void, Void>{

		@Override
		protected Void doInBackground(Void... arg0) {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override 
		protected void onPostExecute(Void arg0){
		
		
		}
		
	}
	
	private void Write() {
		outputBufferReadyForRead = true;
		//ProcessBuffer();
		//Setup output buffer
		/*mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, 
				AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, 
				10 * AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, 
						AudioFormat.ENCODING_PCM_16BIT), AudioTrack.MODE_STATIC);
		//mAudioTrack.setPositionNotificationPeriod(bufferSize >> 1);
		//mAudioTrack.setPlaybackPositionUpdateListener(mPlaybackListener);*/ 
		//mAudioTrack.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume());
		/*Log.i("com.hp.vocalx.AudioTrack","Track Notification Marker Poision = " + mAudioTrack.getNotificationMarkerPosition() +
				"; Playback Head Poistion = " + mAudioTrack.getPlaybackHeadPosition() +
				"; Notification Period = " + mAudioTrack.getPositionNotificationPeriod() + 
				"; state = " + mAudioTrack.getState());*/
		//if (mAudioTrack.getPlaybackHeadPosition() > mAudioTrackBufferSize / mAudioTrack.getChannelCount()){
		//	mAudioTrack.setPlaybackHeadPosition(0);
		//}
		byte[] bufferTemp = new byte[bufferSize];
		bufferTemp = TestSound(bufferTemp, mGLView.maxAmpIdx, mGLView.maxAmplitude);
		if (mSharedPref.getBoolean("turn_on_output_audio_key", true)){
			mAudioTrack.write(bufferTemp, writeOffset, bufferSize);
		}
		
		if (PenelopeMainActivity.checkSleep(mContext)){
			onAirButton.performClick();
		}
		
		
		
		/*/ move the outputBuffer to the AudioTrackBuffer
		for (int i = 0; i < bufferSize*2; i++){
			mAudioTrackBuffer[mAudioTrackIdx++] = outputBuffer[i];
		}
		
		if (mAudioTrackIdx >= mAudioTrackBufferSize){
			mAudioTrack.write(mAudioTrackBuffer, writeOffset, mAudioTrackBufferSize);
			mAudioTrackIdx = 0;
		}//*/
		
		//if (mAudioTrack.getState() != 3) {mAudioTrack.play();}
		
		
		//mAudioTrack.write(outputBuffer, writeOffset, bufferSize);
		//Log.i("OnAir",mAudioManager.isMicrophoneMute() + " " + mAudioManager.isMusicActive()
		//		+ " " + mAudioManager.isSpeakerphoneOn() + " " + mAudioManager.getMode());
					

	}
	
	private Thread processThread = new Thread(new Runnable(){

		@Override
		public void run() {
			ProcessBuffer();
		}
		
	}, "OnAirProcess");
	
	
	private boolean ProcessBuffer() {
		float[] amplitudes = new float[bufferSize/4]; //two samples per frame
		
		//outputBuffer = processBuffer;
		
		//for (int i=0; i<bufferSize>>2;i++){
		//	outputBuffer[4*i] = processBuffer[i];
		//	outputBuffer[4*i+1] = processBuffer[i+1];
		//	outputBuffer[4*i+2] = processBuffer[i];
		//	outputBuffer[4*i+3] = processBuffer[i+1];
		//}
		//processBuffer = dsp.newSamples(processBuffer,0);
		
		/*
		for (int i=0; i<bufferSize;i++){
			for (int j=0; j<2;j++){
				short temp = (short) ((processBuffer[4*i+2*j+1]<<8) + processBuffer[4*i+2*j]); 
				amplitudes[i] += (float) temp;
			}
			amplitudes[i] /= (float) Math.pow(2.0f, 15.0f);//take four samples and sum into one sample, also normalize
		}//*/
		for (int i=0; i < amplitudes.length; i++){
			amplitudes[i] = (processBuffer[2*i+1] + processBuffer[2*i])/2.0f;
		}
		
		try {
			//mGLView.updateAmplitudes(dsp.newSamples(amplitudes, 0));
			mGLView.updateAmplitudes(dsp.filterSamples(amplitudes));
		} catch (Exception e) {
			AlertDialog.Builder builder;
	        builder = new AlertDialog.Builder(mContext);
	        builder.setMessage(e.getMessage());
	        builder.show();
	        //debugger.break();
			e.printStackTrace();
		}
		
		//mGLView = mGLView.getMyGLSurfaceView(mContext);
		//mGLView.updateAmplitudes(amplitudes);
		
/*		if(inputBufferReadyForRead && !inputBufferReady4Proc){
			mAudioRecord.read(inputBuffer, bufferSize);
			inputBufferReadyForRead=false;
			inputBufferReady4Proc=true;
		}
		if(outputBufferReadyForRead && !outputBufferReady4Proc){
			mAudioTrack.write(outputBuffer, writeOffset, bufferSize);
			outputBufferReadyForRead=false;
			outputBufferReady4Proc=true;
		}
			
		if(inputBufferReady4Proc && outputBufferReady4Proc){ 
			outputBuffer = inputBuffer.array();
			inputBufferReady4Proc = false;
			outputBufferReady4Proc = false;
			return true;
		}
		return false;*/
		return true;
	}

	AudioRecord.OnRecordPositionUpdateListener mRecordListener =
			new AudioRecord.OnRecordPositionUpdateListener() {
				
				@Override
				public void onPeriodicNotification(AudioRecord recorder) {
					if (onAirBool){
						/*
						if (!readThread.isAlive()){
							readThread.run();
						}
						if (!writeThread.isAlive()){
							writeThread.run();
						}
						 //*/
						
						Read();
						Write();
						if (!processThread.isAlive()){
							processThread.run();
						}
					}
					
				}
				
				@Override
				public void onMarkerReached(AudioRecord recorder) {
					// has to include this stub					
				}
	};
	
	AudioTrack.OnPlaybackPositionUpdateListener mPlaybackListener =
			new AudioTrack.OnPlaybackPositionUpdateListener() {
				
				@Override
				public void onPeriodicNotification(AudioTrack track) {
					if (onAirBool){
						//Write();
					}					
				}
				
				@Override
				public void onMarkerReached(AudioTrack track) {
					// must have this override here
					
				}
			};
			
	private void reverse(final byte[] pArray) {
	    if (pArray == null) {
	      return;
	    }
	    int i = 0;
	    int j = pArray.length - 1;
	    byte tmp;
	    while (j > i) {
	      tmp = pArray[j];
	      pArray[j] = pArray[i];
	      pArray[i] = tmp;
	      j--;
	      i++;
	    }
	  }
	
	private byte[] TestSound(byte[] buff, int note, float amp){
		double[] sample = new double[buff.length>>mAudioTrack.getChannelCount()];
		byte[] generatedSnd = new byte[buff.length];
		double baseNote = 55.0; 
		double	freqControl = baseNote * Math.pow(2.0, (double) note / 12.0 );
		float invert = 1.0f;
		if (mSharedPref.getBoolean("invert_audio_key", false)){
			invert = -1.0f;
		}
		
		if (!(mGLRenderer==null)){
			if (mGLRenderer.mAccelmeter.linear_acceleration[0]>1.0){
				freqControl = baseNote * Math.pow(2.0, (double) (note+1) / 12.0 );
			} else if (mGLRenderer.mAccelmeter.linear_acceleration[0]<-1.0){
				freqControl = baseNote * Math.pow(2.0, (double) (note-1) / 12.0 );
			} 
		}
		
		double phaseInc = 2.0 * Math.PI * freqControl / (double) sampleRate;
				
	    for (int i = 0; i < sample.length; ++i) {
	    	if (phase >= 2.0 * Math.PI) {phase %= 2.0 * Math.PI;}
	    	phase += phaseInc;
            sample[i] = 2.0 * amp * Math.sin(phase) + invert*processBuffer[i];//[wave_index_tracker++];
        }
        
		
        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        //Log.i("homo", String.valueOf((short) (21.586 - (21.586 % 1))));
        
        for (double dVal : sample) {
            // scale to maximum amplitude
        	dVal = dVal * 16383;
        	final short val = (short) (dVal - (dVal % 1));
        	
            //final short val = (short) ((dVal * 16383));
            // in 16 bit wav PCM, first byte is the low order byte
        	if (mAudioTrack.getChannelCount()==2){
	            generatedSnd[idx++] = (byte) (val & 0x00ff);
	            generatedSnd[idx++] = (byte) ((val & 0xff00) >> 8);
	            generatedSnd[idx++] = (byte) (val & 0x00ff);
	            generatedSnd[idx++] = (byte) ((val & 0xff00) >> 8);
        	} else {
	            generatedSnd[idx++] = (byte) (val & 0x00ff);
	            generatedSnd[idx++] = (byte) ((val & 0xff00) >> 8);
        	}
        	
        }
        return generatedSnd;
	}
	
	private double[] CalcWave(){
		int freqControl = 441;
		double[] sample = new double[sampleRate/freqControl];
		int j = wave_index_tracker;
	    for (int i = 0; i < sample.length; ++i) {
            sample[i] = Math.sin(2 * Math.PI * j++ * freqControl / sampleRate);
        }
		return sample;
	}
	
	private static byte[] toBytes(short s) {
        return new byte[]{(byte)(s & 0x00FF),(byte)((s & 0xFF00)>>8)};
    }
}

class Modal {
	int mode;
	short[] details;
	short[] averages;
	
	public Modal(short[] theDetails, int theMode, short[] theAverages){
		mode = theMode;
		details = theDetails;
		averages = theAverages;
		
	}
		
}

