package com.harmonicprocesses.penelopefree.audio;

import java.lang.ref.WeakReference;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import com.harmonicprocesses.penelopefree.PenelopeMainActivity;
import com.harmonicprocesses.penelopefree.camera.BufferEvent;
import com.harmonicprocesses.penelopefree.camera.BufferEvent.CodecBufferObserver;
import com.harmonicprocesses.penelopefree.camera.MyMediaCodec;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.AudioRecord.OnRecordPositionUpdateListener;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.media.audiofx.PresetReverb;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;

/**
 * AudioThru is a high priority handler thread for handling callback events
 *  for AuidoRecord and AudioTrack listeners. 
 *   Basically AudioTrack.output = AudioRecord.input + volatile.processbuffer.
 *  interfaces for updating processbuffer and controlling scalar wet/dry
 *  also interfaces to start and stop the AudioRecording.
 * 
 * @author Joseph Walter
 *
 */
public class AudioThru extends HandlerThread {

	//some useful constants
	private static int sampleRate = AudioConstants.sampleRate;
	private static int numBytePerFrame = AudioConstants.numBytePerFrame;
	private static int numChannels = AudioConstants.numChannels;
	private static int bufferSizeStatic = 1024;

	
	//instances
	private AudioRecord mAudioRecord;
	private AudioTrack mAudioTrack;
	private PresetReverb plateReverb; 
	private OnNewSamplesReceivedUpdateListener onNewSamplesReceivedUpdateListener = null;
	private Handler processorHandler = null, mAudioEventHandler = null;
	
	//buffers
	private ByteBuffer inputByteBuffer; 
	private AudioPacket[] audioCapturePacket = new AudioPacket[4];
	private int captureBuffIdx = 0, captureFrameIdx = 0;
	private float[] inputBuffer;
	//private float[] outputBuffer;
	private volatile float[] processBuffer;
	private volatile int floatBufferSize;// = bufferSize/(numBytePerFrame*numChannels);
	private volatile float wetDry;// = AudioConstant.wetDry;
	private volatile int bufferSize;// = AudioConstant.bufferSize;
	private volatile boolean invertAudioOn = false;
	private volatile BufferEvent.CodecBufferObserver observer = null;
	
	//Handlers
	Handler processBufferUpdateHandler = null;
	Context mContext;
	private Handler audioCaptureHandler = null;
		
	
	public AudioThru(Context context, int priority, int buffSize){
		super(AudioConstants.AudioThruThreadName,priority);
		mContext = context;
		bufferSize = AudioConstants.getBufferSize(buffSize);
		floatBufferSize = AudioConstants.getFloatBufferSize(buffSize);
		inputByteBuffer = ByteBuffer.allocateDirect(bufferSize);
		processBuffer = new float[floatBufferSize];
		//Setup setup Audio

		int AudioRecordBufferSize = calcRecordBufferSize(sampleRate,bufferSize); 
		mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,  sampleRate, 
				AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, AudioRecordBufferSize);
		AcousticEchoCanceler.create(mAudioRecord.getAudioSessionId());
		NoiseSuppressor.create(mAudioRecord.getAudioSessionId());
		
		//Setup AudioTrack (audio player)
		int mAudioTrackBufferSize = calcTrackBufferSize(sampleRate,bufferSize);
		mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, 
				AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 
				mAudioTrackBufferSize, AudioTrack.MODE_STREAM);
		try {
			plateReverb = new PresetReverb(0, mAudioTrack.getAudioSessionId());
		} catch (Throwable e){
			plateReverb = null;
		}
		if (plateReverb!=null){plateReverb.setPreset(PresetReverb.PRESET_PLATE);}

	
	}
	
	private void setUpAudio() {
		inputByteBuffer = ByteBuffer.allocateDirect(bufferSize);
		processBuffer = new float[floatBufferSize];
		int AudioRecordBufferSize = calcRecordBufferSize(sampleRate,bufferSize); 
		mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,  sampleRate, 
				AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, AudioRecordBufferSize);
		AcousticEchoCanceler.create(mAudioRecord.getAudioSessionId());
		NoiseSuppressor.create(mAudioRecord.getAudioSessionId());
		
		//Setup AudioTrack (audio player)
		int mAudioTrackBufferSize = calcTrackBufferSize(sampleRate, bufferSize);
		mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, 
				AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 
				mAudioTrackBufferSize, AudioTrack.MODE_STREAM);
		try {
			plateReverb = new PresetReverb(0, mAudioTrack.getAudioSessionId());
		} catch (Throwable e){
			plateReverb = null;
		}
		if (plateReverb!=null){plateReverb.setPreset(PresetReverb.PRESET_PLATE);}

	}

	@Override
	protected void onLooperPrepared(){
	}
	
	/*==========================================================================
	 * Public Methods
	 *==========================================================================
	 */
	
	//post looper operations
	public boolean init(){
		if ( mAudioEventHandler==null){
			mAudioEventHandler = new Handler(super.getLooper());
		}
		int setPositionOK = mAudioRecord.setPositionNotificationPeriod(bufferSize/numBytePerFrame); // in frames
		mAudioRecord.setRecordPositionUpdateListener(mRecordListener, mAudioEventHandler);
		return (setPositionOK == AudioRecord.SUCCESS);
	}
	
	public void startAudioThru(boolean ReverbOn, boolean InvertAudioOn, 
			int buffSize, float Wet){
		int BufferSize = AudioConstants.getBufferSize(buffSize);
		if (BufferSize!=bufferSize){
			releaseAudio();
			bufferSize=BufferSize;
			floatBufferSize = AudioConstants.getFloatBufferSize(buffSize);
			setUpAudio();
			init();
		}
		wetDry = Wet;
		mAudioRecord.startRecording();
		int len = mAudioRecord.read(inputByteBuffer, sampleRate); //starts recording
		if (plateReverb!=null){plateReverb.setEnabled(ReverbOn);}
		invertAudioOn = InvertAudioOn;
		mAudioTrack.play();
	}
	
	public void startRecord(BufferEvent.CodecBufferObserver Observer, Handler handler){
		observer = Observer;
		audioCaptureHandler = handler;
		for (int i = 0; i< audioCapturePacket.length; i++){
		audioCapturePacket[i] = new AudioPacket(i,ByteBuffer.allocateDirect(
				(int) MyMediaCodec.pcm_bytes_per_mp4_frame));
		audioCapturePacket[i].pcmBuff.mark(); //set mark to zero
		}
		
	}
	
	public void stopRecord(){
		observer = null;
		audioCaptureHandler.removeMessages(Audio_Capture_Handler_What);
	}
	
	public void stopAudioThru(){
		mAudioRecord.stop();
		mAudioTrack.stop();
		if (plateReverb!=null){plateReverb.setEnabled(false);}
			
	}
	
	public void releaseAudio(){
		mAudioRecord.release();
		mAudioRecord = null;
		mAudioEventHandler = null;
		mAudioTrack.release();
		mAudioTrack = null;
	}
	
	public void updateWetDry(float lev){
		wetDry = lev;
	}
	
	public void updateReverb(boolean reverbOn){
		if (plateReverb!=null) plateReverb.setEnabled(reverbOn);
	}
	
	/*==========================================================================
	 * Listeners and interfaces
	 *==========================================================================
	 */
	
	private OnRecordPositionUpdateListener mRecordListener = new OnRecordPositionUpdateListener(){
		@Override
		public void onMarkerReached(AudioRecord recorder) {}
		@Override
		public void onPeriodicNotification(AudioRecord recorder) {
			inputBuffer = Read();
			float[] outputBuffer = calcOutput(inputBuffer,processBuffer);
			byte[] outputByteArray = Write(outputBuffer);
			onNewSamplesReceived(inputBuffer);
			record(outputByteArray);
			//outputByteBuffer = ByteBuffer;
			//OnBufferListener.this.OnBufferReady(ByteBuffer.wrap(outputByteArray));
		}
	};
	
	public interface OnNewSamplesReceivedUpdateListener {
		void onNewSamplesReceived(float[] inputBuffer);
	}
	
	public void setOnNewSamplesReceivedUpdateListener(Handler handler){
		synchronized (handler){
			processorHandler = handler;
			procHandlerMsg = processorHandler.obtainMessage(newSampleWhat); 
		}
		
	}
	
	public static final int newSampleWhat = 24;
	private static final int Audio_Capture_Handler_What = 21;
	
	protected Message procHandlerMsg;
	
	private void onNewSamplesReceived(float[] in_buff){
		synchronized (processorHandler){
			if (processorHandler!=null){
				procHandlerMsg = Message.obtain(processorHandler, newSampleWhat, in_buff);//processorHandler.obtainMessage(newSampleWhat); 
				//procHandlerMsg.obj = in_buff;
				processorHandler.sendMessage(procHandlerMsg); 
			}
		}
	}
	
	public Handler getUpdatedProcessBufferHandler(){
		if (processBufferUpdateHandler==null){
			processBufferUpdateHandler = new ProcessBufferUpdatedHandler(this,
					super.getLooper());
		}
		return processBufferUpdateHandler;
	}
	
	protected static class ProcessBufferUpdatedHandler extends Handler {
		private final WeakReference<AudioThru> atReference;
		
		public ProcessBufferUpdatedHandler(AudioThru at, Looper looper){
			super(looper);
			atReference = new WeakReference<AudioThru>(at);
		}
		
		public void handleMessage(Message msg) {
			AudioThru at = atReference.get();
			if (at!=null){
				at.onprocessBufferUpdate((float[]) msg.obj);
			}
		}
	}
	
	/*==========================================================================
	 * Process methods called on HandlerThread
	 *==========================================================================
	 */
	
	private float[] Read(){
		mAudioRecord.read(inputByteBuffer, bufferSize);
		return readByteArray(inputByteBuffer.array());
	}
	
	int len;
	float[] out_buff;
	float procScalar;
	private float[] calcOutput(float[] in_buff, float[] proc_buff) {
		if (proc_buff == null) {
			proc_buff = new float[in_buff.length];
		}
		
		if (len!=in_buff.length){
			len = in_buff.length;
			out_buff = new float[len];
		}
			
		procScalar = wetDry;
		if (invertAudioOn){
			procScalar = -procScalar;
		}
		float inScalar = 1.0f - procScalar;
		
		for (int i = 0; i < len; i++){
			out_buff[i] = inScalar * in_buff[i] + procScalar * proc_buff[i];   
		}
		return out_buff;
	}

	byte[] outputByteBuffer;
	private byte[] Write(float[] out_buff) {
		outputByteBuffer = writeByteBuffer(out_buff);
		mAudioTrack.write(outputByteBuffer, 0, outputByteBuffer.length);
		return outputByteBuffer;
	}

	private void onprocessBufferUpdate(float[] new_proc_buff){
		processBuffer = new_proc_buff;
	}
	
	private void record(final byte[] outputByteArray) {
		// break statement if not recording
		if (observer==null||audioCaptureHandler==null){return;}
		
		// put new byte array on audio capture buffer
		try {
			audioCapturePacket[captureBuffIdx].pcmBuff.put(outputByteArray);
		// check if audioCapture Buffer is ready to send to encoder
		} catch (BufferOverflowException e) {
			// fill will remaining samples and send to encoder		
			int remaining = audioCapturePacket[captureBuffIdx]
					.finishPack(captureFrameIdx++,outputByteArray);

			Message msg = audioCaptureHandler.obtainMessage();
			msg.obj = audioCapturePacket[captureBuffIdx];
			msg.what = Audio_Capture_Handler_What;
			audioCaptureHandler.sendMessage(msg);
			
			//iterate packet fill with remaining samples 
			captureBuffIdx = ++captureBuffIdx%4;
			audioCapturePacket[captureBuffIdx]
					.startPack(remaining, outputByteArray);

		}
	}
	
	
	/*==========================================================================
	 * Utility Functions
	 *==========================================================================
	 */
	
	private int calcRecordBufferSize(int sample_rate, int buffer_size) {
		
		int recordBufferSize = AudioRecord.getMinBufferSize(sample_rate, AudioFormat.CHANNEL_IN_MONO, 
				AudioFormat.ENCODING_PCM_16BIT);
		
		while (recordBufferSize<buffer_size){
			recordBufferSize *= 2;
		}
		recordBufferSize += (recordBufferSize%buffer_size);
		return recordBufferSize;
	}
	
	private int calcTrackBufferSize(int sample_rate, int buffer_size) {
		int trackBufferSize = AudioTrack.getMinBufferSize(sample_rate, AudioFormat.CHANNEL_OUT_MONO, 
				AudioFormat.ENCODING_PCM_16BIT);
		trackBufferSize += buffer_size - (trackBufferSize % buffer_size);
		
		return trackBufferSize;
	}
	
	float[] floatArray = null;
	private float[] readByteArray(byte[] byteArray) {
		if (floatArray == null || floatArray.length!=byteArray.length / (numChannels*numBytePerFrame)){
			floatArray = new float[byteArray.length / (numChannels*numBytePerFrame)];
		}
		
		if (numChannels == 1){
			for (int i = 0; i < floatArray.length; i++){
				short tempShort = (short) ((byteArray[2*i+1]<<8) + byteArray[2*i]); 
				floatArray[i] = (float) (tempShort / Math.pow(2,15)); 
			} 
		} //TODO add stereo support
		return floatArray;
	}	
	
	
	byte[] out_byte_buff = new byte[bufferSizeStatic];
	int idx;
	private byte[] writeByteBuffer(float[] out_buff) {
		if (out_byte_buff.length!=out_buff.length*numBytePerFrame*numChannels){
			out_byte_buff = new byte[bufferSize];
		}
		idx = 0;
		
		for (float sample : out_buff) {
            
			// scale to maximum amplitude
        	sample = sample * 16383;
        	final short val = (short) (sample - (sample % 1));
        	//final short val = (short) ((sample * 16383));
        	
            // in 16 bit wav PCM, first byte is the low order byte
        	if (mAudioTrack.getChannelCount()==2){
        		out_byte_buff[idx++] = (byte) (val & 0x00ff);
        		out_byte_buff[idx++] = (byte) ((val & 0xff00) >> 8);
        		out_byte_buff[idx++] = (byte) (val & 0x00ff);
        		out_byte_buff[idx++] = (byte) ((val & 0xff00) >> 8);
        	} else {
        		out_byte_buff[idx++] = (byte) (val & 0x00ff);
        		out_byte_buff[idx++] = (byte) ((val & 0xff00) >> 8);
        	}
        	
        }
		return out_byte_buff;
	}

	public class AudioPacket {
		public int frameIdx;
		public ByteBuffer pcmBuff;
		public AudioPacket(int i, ByteBuffer b){
			frameIdx = i;
			pcmBuff = b;
		}
		
		public void startPack(int r, byte[] inPCM){
			pcmBuff.reset();
			pcmBuff.put(inPCM, r, inPCM.length-r);
		}
		
		public int finishPack(int i, byte[] inPCM){
			frameIdx = i;
			
			int remaining = pcmBuff.limit()-pcmBuff.position();
			pcmBuff.put(inPCM, 0, remaining);
			pcmBuff.reset();
			
			return remaining;
		}
	}

}


