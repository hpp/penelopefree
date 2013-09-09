package com.harmonicprocesses.penelopefree.camera;

import java.nio.ByteBuffer;

import com.harmonicprocesses.penelopefree.audio.AudioThru;
import com.harmonicprocesses.penelopefree.camera.BufferEvent.AudioBufferListener;
import com.harmonicprocesses.penelopefree.openGL.MyGLSurfaceView;

import android.media.CamcorderProfile;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaCodec.BufferInfo;
import android.view.Surface;
import android.webkit.MimeTypeMap;

public class MyMediaCodec{
	
	MediaCodec codec = null;
	boolean isAudioCodec;
	ByteBuffer[] inBuff;
	ByteBuffer[] outBuff;
	private BufferInfo buffInfo;
	BufferEvent.CodecBufferObserver observer;
	
	/**
	 * Constructor for class to wrap the MediaCodec API
	 * @param mCamId: Null if isAudio, The returned camId of the camera profile if not isAudio
	 * @param isAudio: Set true if the codec is for an audio channel;
	 */
	public MyMediaCodec(int mCamId, boolean isAudio, MyGLSurfaceView mGLView, 
						 BufferEvent.CodecBufferObserver Observer){
		CamcorderProfile profile;
		MediaFormat mediaFormat = null;
		Surface surface = null;
		String mime = MimeTypeMap.getSingleton()
				.getMimeTypeFromExtension("mp4");
		codec = MediaCodec.createEncoderByType(mime);
		isAudioCodec = isAudio;
		observer = Observer;
		
		if (isAudioCodec){
			
		} else { //is VideoCodec
			profile = CamcorderProfile.get(mCamId,CamcorderProfile.QUALITY_HIGH);
			mediaFormat = MediaFormat.createVideoFormat(
					mime, profile.videoFrameWidth, profile.videoFrameHeight);
			surface = (Surface) mGLView.getHolder();
		}
		codec.configure(mediaFormat, surface, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		codec.start();
		
		buffInfo = new MediaCodec.BufferInfo();
		inBuff = codec.getInputBuffers();
		outBuff = codec.getOutputBuffers();
		
	}
	
	/*Put this in all places that need to post data to the codec 
		 
	public interface OnBufferListener {
		public boolean OnBufferReady(ByteBuffer[] newBuff);
	}
	
	public interface OnBufferReadyListener {
		public boolean OnBufferReady(ByteBuffer inBuff,boolean isAudio,BufferInfo buffInfo);
	}
	*/
	
	public interface OnBufferReadyListener {
		public boolean OnBufferReady(ByteBuffer inBuff,boolean isAudio,BufferInfo buffInfo);
	}

	
	AudioBufferListener onBufferListener = new AudioBufferListener() {
		
		@Override
		public boolean OnAudioBufferReady(ByteBuffer newBuff){
			if (codec==null){return false;}

			inBuff[0] = newBuff;
			/*
			int newLen = newBuff.length;
			int inLen = inBuff.length;
			int inBuffIdx = codec.dequeueInputBuffer(-1);
			if (inBuffIdx >= 0) {
				// fill inBuff[inBuffIdx] with valid data
				int n = 0;
				for (int i=0;i<newLen;i++){
					if (inBuffIdx+i+n>=inLen){n-=inLen;}
					inBuff[inBuffIdx+i+n]=newBuff[i];
				}
				codec.queueInputBuffer(inBuffIdx, 0, newLen, -1, 0);
			}
			*/
			
			int outBuffIdx = codec.dequeueOutputBuffer(null, 16000);
			if (outBuffIdx >= 0) {
				// outputBuffer is ready to be processed or rendered.
				observer.fireCodecBufferReady(outBuff[0], isAudioCodec, buffInfo);
				codec.releaseOutputBuffer(outBuffIdx, false);
			} else if (outBuffIdx == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
				outBuff = codec.getOutputBuffers();
			} else if (outBuffIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
				// Subsequent data will conform to new format.
				MediaFormat format = codec.getOutputFormat();
			}
			return true;
		}
	};
		
	public void stop(){
		codec.stop();
		codec.release();
		codec = null;
	}
	
}
