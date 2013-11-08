package com.harmonicprocesses.penelopefree.camera;

import java.nio.ByteBuffer;

import com.harmonicprocesses.penelopefree.camera.BufferEvent.AudioBufferListener;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.CamcorderProfile;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaCodec.BufferInfo;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;

@TargetApi(18)
public class MyMediaCodec{
	
	MediaCodec codec = null;
	boolean isAudioCodec;
	ByteBuffer[] inBuff;
	ByteBuffer[] outBuff;
	private BufferInfo buffInfo;
	BufferEvent.CodecBufferObserver observer;
	private static final String VIDEO_MIME_TYPE = "video/avc";
    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";
    public Surface videoCodecInputSurface;
	public Surface videoInputSurface;
	static final String TAG = "com.hpp.penny.cam.MyMediaCodec"; 
	CaptureManager codecManager;
	int frameIdx = 0;
	// size of a frame, in pixels
	private int mWidth = -1;
	private int mHeight = -1;
	//bit rate, in bits per second
	private int mBitRate = -1;
	private int mFrameRate = -1;
	private boolean useInputSurface = CaptureManager.useInputSurface;
	private boolean codecPrimed=false;

	/**
	 * Constructor for class to wrap the MediaCodec API
	 * @param mCamId: Null if isAudio, The returned camId of the camera profile if not isAudio
	 * @param isAudio: Set true if the codec is for an audio channel;
	 */
	public MyMediaCodec(int mCamId, boolean isAudio, CaptureManager cm, 
						 BufferEvent.CodecBufferObserver Observer){
		CamcorderProfile profile;
		MediaFormat mediaFormat = null;
		Surface surface = null;
		isAudioCodec = isAudio;
		observer = Observer;
		codecManager = cm;
		
		if (isAudioCodec){
			codec = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE);
			mediaFormat = new MediaFormat();
			mediaFormat.setString(MediaFormat.KEY_MIME, AUDIO_MIME_TYPE);
			mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
			mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, 44100);
			mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
			mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
			mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);
			//mediaFormat = MediaFormat.createAudioFormat(AUDIO_MIME_TYPE, 
			//		AudioConstants.sampleRate, 
			//		AudioConstants.numChannels);
			surface = null;
			codec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		} else { //is VideoCodec
			codec = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
			profile = CamcorderProfile.get(mCamId,CamcorderProfile.QUALITY_HIGH);
			mWidth = profile.videoFrameWidth;
			mHeight = profile.videoFrameHeight;
			mBitRate = profile.videoBitRate;
			mFrameRate = profile.videoFrameRate;
			Log.d(TAG, "Camera profile: Frame Rate = " + mFrameRate +
					"; Bit Rate = " + mBitRate +
					"; Heighth = " + mHeight +
					"; Width = " + mWidth);
			mediaFormat = MediaFormat.createVideoFormat(
					VIDEO_MIME_TYPE, mWidth, mHeight);
			mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
			mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
			if (useInputSurface||CaptureManager.useStaticBitmap){
				mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, 
						MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
			} else {
				int colorFormat = selectColorFormat(VIDEO_MIME_TYPE);
				mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, 
						colorFormat);
			}
			//mediaFormat.setInteger(MediaFormat.)
			// COLOR_TI_FormatYUV420PackedSemiPlanar - crash
			// COLOR_QCOM_FormatYUV420SemiPlanar - crash
			// COLOR_FormatYUV420PackedSemiPlanar - crash
			// COLOR_FormatYUV420Planar - crash
			mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
			//surface = (Surface) mGLView.getHolder();
			codec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
			if (useInputSurface) videoInputSurface = codec.createInputSurface();
			if (CaptureManager.useStaticBitmap) {
				videoCodecInputSurface = codec.createInputSurface();
				
			}

			//videoCodecInputSurface = cm.mGLView.grabEGLSurface(videoInputSurface);
		}
		
		codec.start();
		
		buffInfo = new MediaCodec.BufferInfo();
		if (isAudio||(!useInputSurface&&!codecManager.useStaticBitmap)) inBuff = codec.getInputBuffers();
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

	public Surface getSurface(){
		return videoInputSurface;
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
			runQue(false);
			
			return true;
		}
	};


	void runQue(boolean EOS) {
		if (EOS) {
			if (checkEOS(EOS)) { 
				return;
			} 
		}
		
		int outBuffIdx = codec.dequeueOutputBuffer(buffInfo, 16000);
		if (outBuffIdx == MediaCodec.INFO_TRY_AGAIN_LATER) {
			if (!EOS) {
				Log.d(TAG, "output buffer dequeued with 'try again later'");
				return;
			} else if (isAudioCodec || (!useInputSurface && !codecManager.useStaticBitmap)) {
				Log.d(TAG, "queueing EOS to inputBuffer");
				//stop();
				return;
			} else {
				//
				Log.d(TAG, "No encoder data ready, try again later and EOS spinning");
				//codec.releaseOutputBuffer(outBuffIdx, false);
			}
		} else if (outBuffIdx == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
			outBuff = codec.getOutputBuffers();
		} else if (outBuffIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
			// Subsequent data will conform to new format.
			if (!EOS){
				MediaFormat format = codec.getOutputFormat();
				observer.fireCodecBufferFormatChange(format);
			}
		} else if (outBuffIdx < 0) {
			Log.d(TAG,
					"unexpexted result from encoder.dequeueOutputBuffer: " 
					+ outBuffIdx);
		} else { //(outBuffIndx >= 0)
			if ((buffInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG)!=0){
				codecPrimed=true;
				return;
			}
			// outputBuffer is ready to be processed or rendered.
			//observer.fireCodecBufferReady(outBuff[outBuffIdx], isAudioCodec, buffInfo);
			//codec.releaseOutputBuffer(outBuffIdx, false);
			observer.fireCodecBufferReady(outBuff[outBuffIdx], isAudioCodec, buffInfo);
			codec.releaseOutputBuffer(outBuffIdx, false);
			if (checkEOS(EOS)){
				return; //exit
			}
		} 
		
		runQue(EOS); //Loop Recursive Tail
	}
	
	private boolean checkEOS(boolean EOS){
		if ((buffInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0) {
			Log.d(TAG,"EOS reached, EOS = " + EOS);
			if(EOS){
				//it is okay, we are done
				stop();
			}
			return true; //exit
		}
		return false;
	}
	
	public void sendEOS(){
		if (!isAudioCodec && useInputSurface) {
			codec.signalEndOfInputStream();
			runQue(true);
		} 
	}
	
	public void stop(){
		if (codec == null) return; 
		if (!isAudioCodec) {
			videoCodecInputSurface.release();
		}
		codecPrimed=false;
		codec.stop();
		codec.release();
		codec = null;
		codecManager.stopMuxer(isAudioCodec);
	}
	
	//*
	public boolean updateInputSurfaceBitmap(Bitmap bitmap){
		try {
			//Canvas k = videoInputSurface.lockCanvas(null);
			//k.setBitmap(bitmap);
			//videoInputSurface.unlockCanvasAndPost(k);
			generateSurfaceFrame(frameIdx);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//videoCodecInputSurface.makeCurrent();
		long presentationTime = computePresentationTimeNsec(frameIdx++); 
		//videoCodecInputSurface.setPresentationTime(presentationTime);
		
		runQue(false);
		return true;
	}//*/

	public void updateInputSurfaceByteArray(ByteBuffer buff,int size, boolean eos) {
		if (codecPrimed) {
			int inBuffIdx = codec.dequeueInputBuffer((long) 16.667);
			if (inBuffIdx < 0) {
				Log.d(TAG,"Error dequeuing input buffer");
				return;
			}
			// fill inputBuffers[inputBufferIndex] with valid data
			ByteBuffer inputByteBuffer = inBuff[inBuffIdx];
			inputByteBuffer.clear();
			if (size>inputByteBuffer.capacity()) size = inputByteBuffer.capacity();
			inputByteBuffer.put(buff);
			long presentationTimeUs = ((long) 1000000000/(long) mFrameRate);
			if (eos){
				codec.queueInputBuffer(inBuffIdx,0,size,presentationTimeUs,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
			} else {
				codec.queueInputBuffer(inBuffIdx,0,size,presentationTimeUs,0);
			}
		}
		runQue(eos);
	}

	public void updateInputSurfaceByteArray(byte[] buff) {
		inBuff[0].put(ByteBuffer.wrap(buff));
		runQue(false);
	}
    /**
     * Generates the presentation time for frame N, in nanoseconds.
     */
    private static long computePresentationTimeNsec(int frameIndex) {
        final long ONE_BILLION = 1000000000;
        return frameIndex * ONE_BILLION / CaptureManager.frame_rate;
    }
    
    
    // RGB color values for generated frames
    private static final int TEST_R0 = 0;
    private static final int TEST_G0 = 136;
    private static final int TEST_B0 = 0;
    private static final int TEST_R1 = 236;
    private static final int TEST_G1 = 50;
    private static final int TEST_B1 = 186;


    /**
     * Generates a frame of data using GL commands.  We have an 8-frame animation
     * sequence that wraps around.  It looks like this:
     * <pre>
     *   0 1 2 3
     *   7 6 5 4
     * </pre>
     * We draw one of the eight rectangles and leave the rest set to the clear color.
     */
    private void generateSurfaceFrame(int frameIndex) {
        frameIndex %= 8;

        int startX, startY;
        if (frameIndex < 4) {
            // (0,0) is bottom-left in GL
            startX = frameIndex * (mWidth / 4);
            startY = mHeight / 2;
        } else {
            startX = (7 - frameIndex) * (mWidth / 4);
            startY = 0;
        }

        GLES20.glClearColor(TEST_R0 / 255.0f, TEST_G0 / 255.0f, TEST_B0 / 255.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glScissor(startX, startY, mWidth / 4, mHeight / 2);
        GLES20.glClearColor(TEST_R1 / 255.0f, TEST_G1 / 255.0f, TEST_B1 / 255.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    }

    private SurfaceTexture mST;
	private long startTime = 0;
	private long frameTime = 0;
	/** frame delay in nanosecs */
	private long frameDelay = CaptureManager.frame_delay*1000*1000;
	
	public void updateFrame(boolean eos) {
		if (eos) codec.signalEndOfInputStream();
		runQue(eos);
	}
	
	public void updateFrameFromBitmap(Bitmap splashImage, boolean eos) {
		if (eos){
			codec.signalEndOfInputStream();
		} else {
			/*
			nextTime();
			GLES20.glFinish();
			codecManager.mGLView.setPresentationToSurface(frameTime);
			//BitmapDecoder.postBitmaptoSurface(splashImage,videoCodecInputSurface);
			HandlerThread awaitNewImageThread = new HandlerThread("awaitNewImage");
			awaitNewImageThread.start();
			new Handler(awaitNewImageThread.getLooper()).post(new Runnable(){
				@Override
				public void run() {
					codecManager.mPcamera.mStManager.awaitNewImage();
				}
			});
			codecManager.mPcamera.mStManager.drawImage();
			codecManager.mGLView.swapCodecInputSurfaceBuffer();
			//GLES20.glFlush();
			 
			 */
		}
		runQue(eos);
	}
	
	private void nextTime() {
		if (startTime == 0) startTime  = (long) (System.currentTimeMillis() * 1000.0);
		long nSecTime = (long) (1000.0*System.currentTimeMillis() - startTime);
		if (nSecTime > frameTime + frameDelay ) nSecTime = frameTime + frameDelay;
		frameTime = nSecTime;
	}

	private static int selectColorFormat(String mimeType) {
	     int numCodecs = MediaCodecList.getCodecCount();
	     for (int i = 0; i < numCodecs; i++) {
	         MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

	         if (!codecInfo.isEncoder()) {
	             continue;
	         }

	         String[] types = codecInfo.getSupportedTypes();
	         for (int j = 0; j < types.length; j++) {
	             if (types[j].equalsIgnoreCase(mimeType)) {
	                 CodecCapabilities cab = codecInfo.getCapabilitiesForType(mimeType);
	                 int[] colorFormats = cab.colorFormats;
	            	 return colorFormats[0];
	             }
	         }
	     }
	     return -1;
	 }
}
