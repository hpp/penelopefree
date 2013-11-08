package com.harmonicprocesses.penelopefree.camera;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import com.harmonicprocesses.penelopefree.audio.AudioThru;
import com.harmonicprocesses.penelopefree.openGL.MyGLSurfaceView;
import com.hpp.openGL.BitmapDecoder;
import com.hpp.openGL.MyEGLWrapper;
import com.hpp.openGL.STextureRender;
import com.hpp.openGL.SurfaceTextureManager;

import android.R;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class CaptureManager {
	
	public static final boolean audioOn = false /*audio on*/, 
			videoOn = true,
			useInputSurface = false, useStaticBitmap = true,
			useDrawingCache = false,useFrameBuffer = false;
	/**the audio codec*/ private  MyMediaCodec audioCodec = null;
	private MyMediaCodec videoCodec = null;
	private MyMediaMux mediaMux;
	private AudioThru mAudio;
	private boolean isRecording = false, audioActive = false, 
			videoActive = false;
	private int mCamId;
	private Context mContext;
	public HandlerThread captureThread;
	private Handler videoHandler, audioHandler;
	private ViewGroup videoSurfaceViewGroup;
	
	/**time between frames in milliseconds*/ 
	public static final long frame_delay = (long) 33.3333333333;
	/**frame rate in frames per second*/ 
	public static final long frame_rate=30;
	private static final String TAG = "io.hpp.CaptureManager";
	
	public MyEGLWrapper mEGLWrapper;
	private MyGLSurfaceView mGLView;
	public Pcamera mPcamera;
	private SurfaceTexture mST;
	private BufferEvent.CodecBufferObserver observer = null;
	private boolean recordingStopped = true;
		
	public CaptureManager(Context context, 
			Pcamera pcamera, MyGLSurfaceView glSurfaceView,AudioThru audioThru, SurfaceView camSV){
		mContext = context;
		mPcamera = pcamera;
		captureThread = new HandlerThread("CaptureManager");
		captureThread.start();
		mAudio = audioThru;
		initCapture();
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2&&videoOn){
			if (!useInputSurface){ 
				//videoCodec.videoInputSurface = camSV.getHolder().getSurface();
				mEGLWrapper = new MyEGLWrapper(mContext, videoCodec.videoCodecInputSurface , glSurfaceView);
			} else {
				mEGLWrapper = new MyEGLWrapper(mContext, null, glSurfaceView);
			}
		}
	}
	
	private void initCapture(){
		
		try {
			observer = new BufferEvent().observer;
			if (audioOn){
				audioCodec = new MyMediaCodec(mCamId, true, this, observer);
				audioHandler = new AudioHandler(captureThread.getLooper(),observer);
				audioActive=true;
			}
			if (videoOn){
				videoCodec = new MyMediaCodec(mCamId, false, this, observer);
				//videoCaptureLoop();
				videoActive=true;
			}
			
			//mGLView.setOutputSurface(videoCodec.getInputSurface());
			mediaMux = new MyMediaMux(audioCodec,videoCodec,observer);
			

		} catch (IllegalStateException e) {
			Log.e("com.hpp.camera.pcam",e.toString());
			for (StackTraceElement i:e.getStackTrace()){
				Log.e("com.hpp.camera.pcam",i.toString());
			}
			Toast.makeText(mContext, 
					"Failed to start capture", 
					Toast.LENGTH_LONG).show();
		} 
	}
	
	public void beginCapture(ViewGroup vg,int camId){
		videoSurfaceViewGroup = vg;
		if (videoOn) {
			isRecording = true;
			recordingStopped = false;
		}
		if (audioOn) mAudio.startRecord(observer,audioHandler);
		
	}
	
	public void endCapture(){
		//send signal to stop audio capture
		if (audioOn) {
			mAudio.stopRecord();
			sendEOS2Loop(audioHandler,audioCodec);
		}
		
		//send signal to end video capture
		if (videoOn) isRecording = false;
		
		//Muxer will be stopped with stopMuxer after
		//both audio and video codecs pass EOS
	}
	
	public void stopMuxer(boolean isAudio){
		if (isAudio){
			audioActive = false;
		} else {
			videoActive = false;
		}
		if (!(audioActive||videoActive)){
			videoHandler.postDelayed(new Runnable(){

				@Override
				public void run() {
					mediaMux.stop();
				}
				
			},frame_delay);
		}
	}
	
	private void sendEOS2Loop(Handler mHandler, final MyMediaCodec mCodec){
		mHandler.post(new Runnable(){
			@Override
			public void run() {
				mCodec.sendEOS();
			}
		});
	}
	
	public SurfaceTextureManager mStManager;
	private SurfaceTexture mSt;
	
	
    /**
     * Configures SurfaceTexture for camera preview.  Initializes mStManager, and sets the
     * associated SurfaceTexture as the Camera's "preview texture".
     * <p>
     * Configure the EGL surface that will be used for output before calling here.
     */
	public void prepareSurfaceTexture(final Camera cam) {
		//post the first frame to draw on capture thread Looper
		postRenderThread(new Runnable(){
		@Override
		public void run() {
			try {
				mEGLWrapper.init();
				mEGLWrapper.makeCurrent(true,null);//going to screen not encoder no input surface yet
				mStManager = new SurfaceTextureManager(cam.getParameters().getPreviewSize());
		        mSt = mStManager.getSurfaceTexture();
		        cam.setPreviewTexture(mSt);
		        cam.startPreview();
			} catch (Exception e) {
				Log.e(TAG,"Fatal Exception post Draw = " + e.getMessage());
			}
			cameraPreviewLoop();	// TODO Auto-generated method stub
			
			}
		});
    }
	
    /**
     * Releases the SurfaceTexture.
     */
	public void releaseSurfaceTexture() {
		mPcamera.stopPreview();
		//start(videoSurfaceViewGroup);
        if (mStManager != null) {
            mStManager.release();
            mStManager = null;
        }
    }
	
	/**
	 * Post new image on CaptureThread
	 */
	void cameraPreviewLoop(){
		if (videoHandler==null) videoHandler = new Handler(captureThread.getLooper());
		
		videoHandler.post(new Runnable(){

			@Override
			public void run() {
				try {
					//captureManager.mGLView.makeCurrent();
			        mStManager.awaitNewImage();
				} catch (Exception e) {
					Log.e(TAG,"Fatal Exception post Draw = " + e.getMessage());
				}
				callBackDrawOnRenderThread();
			}
		});
		
	}


	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void callBackDrawOnRenderThread() {
		postRenderThread(new Runnable(){
			@Override
			public void run() {
				//draw once for screen
				mEGLWrapper.makeCurrent(true, videoCodec.videoCodecInputSurface);
				ByteBuffer frame = mStManager.drawImage();
				videoCaptureLoop(frame);
			}
		});
	}
	
	/**
	 * Method to be called on GL "Render" Thread
	 */
	void videoCaptureLoop(final ByteBuffer frame) {
		if (recordingStopped) {
			videoHandler.postDelayed(new Runnable(){
				@Override
				public void run() {
					cameraPreviewLoop();
				}
			}, frame_delay);
			return;
		}
		
		if (!isRecording && useInputSurface) {
			//videoHandler.removeCallbacks(runVideoCaptureLoop);
			sendEOS2Loop(videoHandler,videoCodec);
			recordingStopped  = true;
			return;
		}
		

		/*/Post capture video on 30 fps interval
		videoHandler.post(new Runnable(){
			@Override
			public void run() {
				try {
					//mGLView.swapBuffers(isRecording);
					if (useInputSurface) {
						videoCodec.updateFrame(!isRecording);
					} else if (useStaticBitmap) {
						drawFrameOnInputSurface();
					} else {
						getDrawingBuffer(frame);
					}
				} catch (Exception e) {
						Log.e("com.hpp.CaptureManager",e.toString());
						e.printStackTrace();
				}
				videoHandler.postDelayed(new Runnable(){
					@Override
					public void run() {
						cameraPreviewLoop();
					}
				}, frame_delay);
			}
		});
		*/
		drawFrameOnInputSurface();
		
		videoHandler.postDelayed(new Runnable(){
			@Override
			public void run() {
				cameraPreviewLoop();
			}
		}, frame_delay);
		
		/*
		try {
			/*Parcel specialDelivery = Parcel.obtain();
			
			//videoSurfaceViewGroup.getDrawingCache().writeToParcel(specialDelivery, 0);
			videoSurfaceViewGroup.getApplicationWindowToken()
					.transact(
							IBinder.DUMP_TRANSACTION,
							specialDelivery, null,
							0);
			//videoCodec.videoInputSurface.readFromParcel(specialDelivery);
			//videoCodec.updateInputSurfaceByteArray(specialDelivery.createByteArray());
			//specialDelivery.recycle();
			///
			//videoSurfaceViewGroup.buildDrawingCache();
			//videoCodec.updateInputSurfaceBitmap(videoSurfaceViewGroup.getDrawingCache());
			videoCodec.updateFrame(mPcamera);
		} catch (Exception e) {
				Log.e("com.hpp.CaptureManager",e.toString());
				e.printStackTrace();
		}//*/
	}
	
	Runnable runVideoCaptureLoop = new Runnable(){

		@Override
		public void run() {
			videoCaptureLoop(null);
		}
		
	};

	public Surface getVideoSurface() {
		return videoCodec.getSurface();
	}
	
	public MyEGLWrapper getMyEGLWrapper() {
		return mEGLWrapper;
	}
	
	private void getDrawingBuffer(ByteBuffer frame) {
		//Parcel specialDelivery = Parcel.obtain();
		//videoCodec.videoInputSurface.
		
		//mGLView.getView().getDrawingCache().writeToParcel(specialDelivery, 0);
		Bitmap drawing;
		if (!useFrameBuffer){
			if (useDrawingCache ){
				View mView = mEGLWrapper.getView();
				if (!mView.isDrawingCacheEnabled())
					mView.setDrawingCacheEnabled(true);
				drawing = mView.getDrawingCache();
				int size = drawing.getByteCount();
				byte[] drawingArray = new byte[size];
				ByteBuffer drawingBuffer = ByteBuffer.wrap(drawingArray);
				drawing.copyPixelsToBuffer(drawingBuffer);
				//videoCodec.videoInputSurface.writeToParcel(specialDelivery, 0);
				videoCodec.updateInputSurfaceByteArray(drawingBuffer,size,!isRecording);
			} else {
				BitmapFactory.Options opt = new BitmapFactory.Options();
				opt.inPreferredConfig = Config.ARGB_8888;
				int bitmapId = mContext.getResources()
						.getIdentifier("screen", "drawable", mContext.getPackageName());
				drawing = BitmapFactory.decodeResource(
						mContext.getResources(), bitmapId, opt);
				int size = drawing.getByteCount();
				byte[] drawingArray = new byte[size];
				ByteBuffer drawingBuffer = ByteBuffer.wrap(drawingArray);
				drawing.copyPixelsToBuffer(drawingBuffer);
				Uri path = Uri.parse("android.resource://com.harmonicprocesses.penelopefree/" + bitmapId);
				
				videoCodec.updateInputSurfaceByteArray(drawingBuffer,size,!isRecording);
			} 

		} else {
			videoCodec.updateInputSurfaceByteArray(frame,frame.capacity(),!isRecording);
		}
	}
	
	private void drawFrameOnInputSurface() {
		/*
		int bitmapId = mContext.getResources()
				.getIdentifier("screen", "drawable", mContext.getPackageName());
		Bitmap splashImage = BitmapFactory.decodeResource(mContext.getResources(), bitmapId);
		*/
		//draw a second time for inputSurface 
		mEGLWrapper.makeCurrent(false, videoCodec.videoCodecInputSurface);
		drawFrame();
		//ByteBuffer frame = mStManager.drawImage();
		videoCodec.runQue(false); // clear que before posting should this be on this thread???
		mEGLWrapper.setPresentationTime(mSt.getTimestamp(),!recordingStopped);
		mEGLWrapper.swapBuffers(!recordingStopped);
		videoHandler.post(new Runnable(){
			@Override
			public void run(){
				videoCodec.updateFrame(!isRecording);
			}
		});
	}
	
	private void drawFrame(){
		mStManager.drawImage();
		mGLView.mRenderer.drawFrame();
	}
	/**
	 * Post a runnable on the render thread.
	 * @param r, the runnable to run on the render thread
	 */
	public void postRenderThread(Runnable r){
		mEGLWrapper.getView().queueEvent(r);
	}


}

class AudioHandler extends Handler{
	private final WeakReference<BufferEvent.CodecBufferObserver> observerRef;
	
	public AudioHandler(Looper looper, BufferEvent.CodecBufferObserver Oberver) {
		super(looper);
		observerRef = new WeakReference<BufferEvent.CodecBufferObserver>(Oberver);
	}
	
	public void handleMessage(Message msg) {
		BufferEvent.CodecBufferObserver observer = observerRef.get();
		if (observer!=null){
			observer.fireAudioBufferReady((ByteBuffer) msg.obj);
		}
	}
}
