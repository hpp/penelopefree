package com.harmonicprocesses.penelopefree.camera;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.harmonicprocesses.penelopefree.audio.AudioConstants;
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
import android.util.Log;
import android.media.MediaMuxer.OutputFormat;
import android.os.Build;

@TargetApi(18)
public class MyMediaMux {
	MediaMuxer muxer = null;
	int audioTrackIndex = -1, videoTrackIndex = -1;
	final int bufferSize = AudioConstants.defaultBufferSize*16;
	CodecBufferObserver bufferObserver;
	protected boolean muxerStarted;
				
	public MyMediaMux(MyMediaCodec audioCodec, MyMediaCodec videoCodec, CodecBufferObserver observer){
		try {
			String filePath = Pcamera.getOutputMediaFile(Pcamera.MEDIA_TYPE_VIDEO).toString();
			muxer = new MediaMuxer(filePath, OutputFormat.MUXER_OUTPUT_MPEG_4);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (audioCodec!=null){
			MediaFormat audioFormat = audioCodec.codec.getOutputFormat();
			//MediaFormat videoFormat = videoCodec.codec.getOutputFormat();
			audioTrackIndex = muxer.addTrack(audioFormat);
		}
		
		if (videoCodec!=null){
			//videoTrackIndex = muxer.addTrack(videoFormat);
			muxerStarted = false; //TODO if only audio start right away
		}
		
		bufferObserver = observer;
		bufferObserver.add(onBufferReadyListener);
	}
	
	
	public CodecBufferReadyListener onBufferReadyListener = new CodecBufferReadyListener(){
		@Override
		public boolean OnCodecBufferReady(ByteBuffer inputBuffer, 
				boolean isAudioSample, BufferInfo bufferInfo)  {
			if (inputBuffer == null) {
				Log.e("com.hpp.MyMediaMux","Encoded Buffer Null ");
			}
			
			if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0){
				//ignore
				bufferInfo.size = 0;
			}
			
			if (bufferInfo.size != 0){
				if (!muxerStarted) {
					Log.e("com.hpp.MyMediaMux","Trying to write data before muxer started");
					return false;
				}
				
				int currentTrackIndex = isAudioSample ? audioTrackIndex : videoTrackIndex;
				muxer.writeSampleData(currentTrackIndex, inputBuffer, bufferInfo);
			
			}
			return true;
		}
		
		@Override
		public boolean OnCodecBufferFormatChange(MediaFormat format, boolean isAudio){
			if (isAudio){
				//audioTrackIndex = muxer.addTrack(format);
				return true; //set when MyMediaMuxer is initiallized
			} else {
				videoTrackIndex = muxer.addTrack(format);
			}
			//if (audioTrackIndex>=0 && videoTrackIndex>=0){
				muxer.start();
				muxerStarted = true;
			//}
			return true;
		}
	};
	
	public void stop(){
		bufferObserver.remove(onBufferReadyListener);
		if (muxerStarted){
			muxer.stop();
			muxer.release();
			muxerStarted = false;
		}
	}
}
