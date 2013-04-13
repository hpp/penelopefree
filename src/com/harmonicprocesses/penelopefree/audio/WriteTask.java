package com.harmonicprocesses.penelopefree.audio;

import java.nio.ByteBuffer;

import com.harmonicprocesses.penelopefree.PenelopeMainActivity;

import android.app.AlertDialog;
import android.os.AsyncTask;

public class WriteTask extends AsyncTask <ByteBuffer, Void, float[]>{
	float[] processBuffer;
	
	@Override
	protected float[] doInBackground(ByteBuffer... inputByteBuffer) {
		byte[] tempByte = inputByteBuffer[0].array();
		processBuffer = readByteArray(tempByte);
		
		inputByteBuffer[0] = null; //garbage collector
		return null;
	}
	
	@Override 
	protected void onPostExecute(float[] arg0){
	
	
	}
	
	private float[] readByteArray(byte[] tempByte) {
		float[] output = new float[tempByte.length / (AudioOnAir.numChannels*AudioOnAir.numBytePerFrame)]; 
		for (int i = 0; i < output.length; i++){
			if (AudioOnAir.numChannels == 1){
				short tempShort = (short) ((tempByte[2*i+1]<<8) + tempByte[2*i]); 
				processBuffer[i] = (float) (tempShort / Math.pow(2,15)); 
			} //TODO else for stereo type record.
			
		}
		return output;
	}
	/*
	
	private void Write() {
		outputBufferReadyForRead = true;
		
		byte[] bufferTemp = new byte[bufferSize];
		bufferTemp = TestSound(bufferTemp, mGLView.maxAmpIdx, mGLView.maxAmplitude);
		if (mSharedPref.getBoolean("turn_on_output_audio_key", true)){
			mAudioTrack.write(bufferTemp, writeOffset, bufferSize);
		}
		
		if (PenelopeMainActivity.checkSleep(mContext)){
			onAirButton.performClick();
		}

	}
	
	private boolean ProcessBuffer() {
		float[] amplitudes = new float[bufferSize/4]; //two samples per frame
		
		
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
		
		
		return true;
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
	}//*/
}
