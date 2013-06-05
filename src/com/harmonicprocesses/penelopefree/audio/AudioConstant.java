package com.harmonicprocesses.penelopefree.audio;

public class AudioConstant {
	public final static String AudioThruThreadName = "AudioThru";
	public static final String AudioProcessorThreadName = "AudioProcessor";
	
	public final static int sampleRate = 44100;
	public final static int AudioSpectrumWhat = 16;
	

	public final static int numBytePerFrame = 2; //two bytes per frame, pcm16 and monoChannel
	public final static int numChannels = 1; //set for mono both record and track for now
	
	public static int defaultBufferSize = 512;
	public float wetDry = 0.5f;
	
	
	public static int getFloatBufferSize(int buffSize) {
		int floatBufferSize = (defaultBufferSize*buffSize)/(numBytePerFrame*numChannels);
		return floatBufferSize;
	}
	
	public static int getBufferSize(int buffSize) {
		int bufferSize = (defaultBufferSize*buffSize);
		return bufferSize;
	}
}
