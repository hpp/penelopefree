package com.harmonicprocesses.penelopefree.camera;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.harmonicprocesses.penelopefree.audio.AudioThru.AudioPacket;

import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;

public class BufferEvent {
	CodecBufferObserver observer;
	
	public interface CodecBufferReadyListener{
		public boolean OnCodecBufferReady(ByteBuffer inBuff,boolean isAudio,BufferInfo buffInfo);

		public boolean OnCodecBufferFormatChange(MediaFormat videoFormat, boolean isAudio);
	}
	
	public interface AudioBufferListener {
		public boolean OnAudioBufferReady(AudioPacket obj);
	}
	
	public BufferEvent(){
		observer = new CodecBufferObserver();
	}

	public class CodecBufferObserver implements CodecBufferObservable{
	
		  // code to maintain listeners
		  private List<CodecBufferReadyListener> codecListeners = new ArrayList<CodecBufferReadyListener>();
		  private List<AudioBufferListener> audioListeners = new ArrayList<AudioBufferListener>();
		  
		  public void add(CodecBufferReadyListener listener) {codecListeners.add(listener);}
		  public void add(AudioBufferListener listener) {audioListeners.add(listener);}
		  
		  public void remove(CodecBufferReadyListener listener) {codecListeners.remove(listener);}
		  public void remove(AudioBufferListener listener) {audioListeners.remove(listener);}
			
		  // notification code
		  public void fireCodecBufferReady(ByteBuffer inBuff,boolean isAudio,BufferInfo buffInfo) {
			  for (CodecBufferReadyListener listener:codecListeners) {
				  listener.OnCodecBufferReady(inBuff,isAudio,buffInfo);
			  }
		  }
		  
		  public void fireCodecBufferFormatChange(MediaFormat format, boolean isAudio){
			  for (CodecBufferReadyListener listener:codecListeners) {
				  listener.OnCodecBufferFormatChange(format, isAudio);
			  }
		  }
		  
		  public void fireAudioBufferReady(AudioPacket obj){
			  for (AudioBufferListener listener:audioListeners) {
				  listener.OnAudioBufferReady(obj);
			  }
		  }
		  
	}
	
	public interface CodecBufferObservable {
		  public void add(CodecBufferReadyListener listener);
		  public void remove(CodecBufferReadyListener listener);
	}
	

}