package com.harmonicprocesses.penelopefree.audio;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import com.harmonicprocesses.penelopefree.R;
import com.harmonicprocesses.penelopefree.audio.AudioThru.OnNewSamplesReceivedUpdateListener;
import com.hpp.billing.PurchaseDialog;
import com.hpp.billing.PurchaseManager;
import com.hpp.dsp.Filter;
import com.hpp.dsp.Wavelet;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.util.Log;

public class AudioProcessor extends HandlerThread{
	//private float[] inputBuffer;
	private int floatBufferSize;
	public int bufferSize;
	private static int sampleRate = AudioConstants.sampleRate;
	private static String TAG = "com.hpp.penny.AudioProcessor";
	Context mContext;
	
	private AudioThru mAudioThru;
	public DSPEngine dsp;
	public volatile boolean boolPitchCorrectEnabled, boolToneGenerateEnable = false;
	public volatile float pitchCorrectLevel;
	private Handler spectrumUpdateHandler = null, noteUpdateHandler = null,
			processBufferUpdateHandler = null;
	private int mNote;
	private double phase = 0.0;
	

	public AudioProcessor(Context context, int audioProcessorPriority, 
			int audioThruPriority,	int buffSize) {
		super(AudioConstants.AudioProcessorThreadName, audioProcessorPriority);
		mAudioThru = new AudioThru(context, audioThruPriority, buffSize);
		bufferSize = AudioConstants.getBufferSize(buffSize);
		floatBufferSize = AudioConstants.getFloatBufferSize(buffSize);
		dsp = new DSPEngine(floatBufferSize/2, AudioConstants.sampleRate, context);
	}
	
	public AudioProcessor getProcessor(){
		return this;
	}
	
	public AudioThru getThru(){
		return mAudioThru;
	}
	
	
	@Override
	protected void onLooperPrepared(){
		mAudioThru.start();
		mAudioThru.init();
		//Handler procHandler = new ProcHandler(super.getLooper());
		mAudioThru.setOnNewSamplesReceivedUpdateListener(new ReceiveNewSamplesHandler(
				this, super.getLooper()));
		processBufferUpdateHandler = mAudioThru.getUpdatedProcessBufferHandler();
		mAudioThru.startAudioThru(false, false, 2, 0.5f);
		mAudioThru.stopAudioThru();
	}
	
	protected static class ReceiveNewSamplesHandler extends Handler {
		private final WeakReference<AudioProcessor> apReference;
		private final MessageQueue msgQueue;
		
		public ReceiveNewSamplesHandler(AudioProcessor ap, Looper looper){
			super(looper);
			apReference = new WeakReference<AudioProcessor>(ap);
			msgQueue = Looper.myQueue();
		}
		
		public void handleMessage(Message msg) {
			AudioProcessor ap = apReference.get();
			if (ap!=null){
				
				ap.onNewSamples((float[]) msg.obj);
			}
		}
	}
	
	/**
	 * things to be initiallized after thread starts
	 */
	public void init(){
		
	}
	
	/*==========================================================================
	 * Public Methods
	 *==========================================================================
	 */
	
	public void startAudio(boolean reverbOn, boolean invertAudioOn, 
			int buffSize, float Wet){
		synchronized(mAudioThru){
			
			int FloatBufferSize = AudioConstants.getFloatBufferSize(buffSize);
			if (FloatBufferSize != floatBufferSize){
				floatBufferSize = FloatBufferSize;
				dsp.setBufferSize(floatBufferSize/2); //downsampled to reduce dsp engine wear and tear
				mAudioThru.startAudioThru(reverbOn, invertAudioOn, buffSize, Wet);
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				mAudioThru.stopAudioThru();
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			mAudioThru.startAudioThru(reverbOn, invertAudioOn, buffSize, Wet);
		}
	}
	
	public void stopAudio() {
		mAudioThru.stopAudioThru();		
	}
	
	public void releaseAudio() {
		mAudioThru.releaseAudio();
		mAudioThru.quit();
	}
	
	public void setPitchCorrect(boolean on){
		boolPitchCorrectEnabled = on;
		PreferenceManager.getDefaultSharedPreferences(dsp.getContext())
				.edit().putBoolean(PurchaseManager.pitchCorrect,on).commit();
	}
	
	public void updatePitchCorrect(int level){
		pitchCorrectLevel = level/100.0f;
	}
	
	public void updateReverb(boolean on){
		mAudioThru.updateReverb(on);
	}
	
	public void updateWetDry(int level){
		mAudioThru.updateWetDry(level/100.0f);
	}
	
	
	/*==========================================================================
	 * Process methods called on HandlerThread
	 *==========================================================================
	 */
	
	float[] spectrum, proc_buff, processedBuffer;
	Wavelet wavelet = null;
	
	private void onNewSamples(float[] in_buff) {
		// this could all be an async task
		/*proc_buff = downSample(in_buff);
		if (doubleDown){
			proc_buff = doubleDownSample(proc_buff);
		}
		in_buff = null; //GC
		
		/*
		try {
			spectrum = dsp.filterSamples(proc_buff);
		} catch (Exception e) {
			Log.d(TAG,e.getMessage());
			spectrum = new float[DSPEngine.numFilters];
		}
		//*/
		if (wavelet==null) wavelet = new Wavelet();
		processedBuffer=wavelet.transform(in_buff,0);
		
		onSpectrumReady(wavelet.getSpectrum());
		
		// this is the only time sensitive 
		//processedBuffer = createProcessBuffer(mNote, spectrum[mNote]);
		synchronized(processBufferUpdateHandler){
			Message msg = processBufferUpdateHandler.obtainMessage(); 
			msg.obj = processedBuffer;
			processBufferUpdateHandler.sendMessage(msg); 
		}
		
	
	}
	
	private void onNoteReady(int note){
		mNote = note;
	}
	
	/*==========================================================================
	 * Listeners and interfaces
	 *==========================================================================
	 */
	
	public void setSprectrumUpdateHandler(Handler handler){
		synchronized (handler){
			spectrumUpdateHandler = handler;
		}
	}
	
	private void onSpectrumReady(float[] spectrum){
		synchronized (spectrumUpdateHandler){
			if (spectrumUpdateHandler!=null){
				int what = AudioConstants.AudioSpectrumWhat;
				Message msg = spectrumUpdateHandler.obtainMessage(what); 
				msg.obj = spectrum;
				spectrumUpdateHandler.removeMessages(what); //we only want the latest spectrum on the que
				spectrumUpdateHandler.sendMessage(msg); 
			}
		}
	}
	
	public Handler getNoteUpdateHandler(){
		if (noteUpdateHandler==null){
			noteUpdateHandler = new UpdateNoteHandler(this,
					super.getLooper());
		}
		
		return noteUpdateHandler;
	}
	
	protected static class UpdateNoteHandler extends Handler {
		private final WeakReference<AudioProcessor> apReference;
		
		public UpdateNoteHandler(AudioProcessor ap, Looper looper){
			super(looper);
			apReference = new WeakReference<AudioProcessor>(ap);
		}
		
		public void handleMessage(Message msg) {
			AudioProcessor ap = apReference.get();
			if (ap!=null){
				ap.onNoteReady((Integer) msg.obj);
				if (!ap.noteUpdateHandler.hasMessages(AudioThru.newSampleWhat)){
					ap.doubleDown = true;
				} else {
					ap.doubleDown = false;
					Log.d(TAG,"doubeDown = " + ap.doubleDown);
				}
				//Log.d(TAG,"doubeDown = " + ap.doubleDown);
			}
		}
	}
	
	protected boolean doubleDown = false;
	
	public void sendNoteUpdate(int note){
		Message msg = noteUpdateHandler.obtainMessage(); 
		msg.obj = note;
		noteUpdateHandler.sendMessage(msg); 
	}
	

	
	/*==========================================================================
	 * Utility Functions
	 *==========================================================================
	 */

	int len;
	float[] down_buff;
	//coeffs for lp quarter filter
	double[] aCoeffs = AudioConstants.quarterFilterACoeffs,
			bCoeffs= AudioConstants.quarterFilterBCoeffs;
	Filter filter = null;
	private float[] downSample(float[] in_buff) {
		if (len!=in_buff.length/2){
			len = in_buff.length/2;
			down_buff=null;
		}
		if (down_buff==null) {down_buff = new float[len];}
		if (filter==null){filter = new Filter(aCoeffs.length,in_buff.length);}
		in_buff = filter.filter(in_buff,bCoeffs,aCoeffs);
		for (int i = 0; i<len; i++){
			down_buff[i] = (in_buff[2*i]+in_buff[2*i+1])/2.0f;
		}
		return down_buff;
	}
	
	int len2;
	float[] down_buff2;
	private float[] doubleDownSample(float[] in_buff) {
		if (len2!=in_buff.length/2){
			len2 = in_buff.length/2;
			down_buff2=null;
		}
		if (down_buff2==null) {down_buff2 = new float[len2];}
		
		for (int i = 0; i<len2; i++){
			down_buff2[i] = (in_buff[2*i]+in_buff[2*i+1])/2.0f;
		}
		return down_buff2;
	}
	
	float[] proc_sample;
	double baseNote = DSPEngine.baseNote; 
	double	freqControl = baseNote * Math.pow(2.0, (double) baseNote / 12.0 );
	

	private float[] createProcessBuffer(int note, float amp){
		if (proc_sample==null) {proc_sample = new float[floatBufferSize];}
		if (boolToneGenerateEnable) {
			proc_sample = generateTone(note,amp);
		} else {
			proc_sample = new float[floatBufferSize]; //clear samples
		}
		
		if (boolPitchCorrectEnabled) {proc_sample = dsp.getPitchCorrectBuffer(proc_sample, spectrum);}
		
		return proc_sample;
	}

	private float[] generateTone(int note, float amp) {
		freqControl = baseNote * Math.pow(2.0, (double) note / 12.0 );
		
		
		/*
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
		//*/
		
		double phaseInc = 2.0 * Math.PI * freqControl / (double) sampleRate;
				
	    for (int i = 0; i < proc_sample.length; ++i) {
	    	phase += phaseInc;
	    	if (phase >= 2.0 * Math.PI) {phase %= 2.0 * Math.PI;}
	    	proc_sample[i] = (float) (2.0 * amp * Math.sin(phase));//[wave_index_tracker++];
        }
		return proc_sample;
	}

	public ArrayList<String> checkSpecialEffects(Context context,PurchaseManager pm) {
		
		ArrayList<String> SEFX_on_but_not_purchased = new ArrayList<String>();
		String sku = PurchaseManager.pitchCorrect;
		boolPitchCorrectEnabled = PreferenceManager
				.getDefaultSharedPreferences(context)
				.getBoolean(sku,false);
		if (boolPitchCorrectEnabled && !pm.purchasedList.contains(sku)) {
			SEFX_on_but_not_purchased.add(sku);
		}
		return SEFX_on_but_not_purchased;
	}
}
