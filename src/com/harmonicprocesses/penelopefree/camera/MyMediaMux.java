package com.harmonicprocesses.penelopefree.camera;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.harmonicprocesses.penelopefree.audio.AudioConstant;
import com.harmonicprocesses.penelopefree.camera.BufferEvent.CodecBufferObserver;
import com.harmonicprocesses.penelopefree.camera.BufferEvent.CodecBufferReadyListener;
import com.harmonicprocesses.penelopefree.camera.MyMediaCodec.OnBufferReadyListener;
import com.harmonicprocesses.penelopefree.camera.Pcamera;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.support.v4.media.*;
import android.media.MediaMuxer.OutputFormat;
import android.os.Build;

@SuppressLint("NewApi")
public class MyMediaMux {
	MediaMuxer muxer = null;
	int audioTrackIndex, videoTrackIndex;
	final int bufferSize = AudioConstant.defaultBufferSize*16;
	CodecBufferObserver bufferObserver;
				
	public MyMediaMux(MyMediaCodec audioCodec, MyMediaCodec videoCodec, CodecBufferObserver observer){
		try {
			String filePath = Pcamera.getOutputMediaFile(Pcamera.MEDIA_TYPE_VIDEO).toString();
			muxer = new MediaMuxer(filePath, OutputFormat.MUXER_OUTPUT_MPEG_4);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		MediaFormat audioFormat = audioCodec.codec.getOutputFormat();
		MediaFormat videoFormat = videoCodec.codec.getOutputFormat();
		
		audioTrackIndex = muxer.addTrack(audioFormat);
		videoTrackIndex = muxer.addTrack(videoFormat);
		
		boolean finished = false;
		muxer.start();
		bufferObserver = observer;
		bufferObserver.add(onBufferReadyListener);
	}
	
	public CodecBufferReadyListener onBufferReadyListener = new CodecBufferReadyListener(){
		@Override
		public boolean OnCodecBufferReady(ByteBuffer inputBuffer, 
				boolean isAudioSample, BufferInfo bufferInfo) {
			
			int currentTrackIndex = isAudioSample ? audioTrackIndex : videoTrackIndex;
			muxer.writeSampleData(currentTrackIndex, inputBuffer, bufferInfo);
			
			return true;
		}
	};
	
	public void stop(){
		bufferObserver.remove(onBufferReadyListener);
		muxer.stop();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
			release();
		}
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private void release(){
		muxer.release();
	}
	
	
}
