package com.hpp.openGL;

import com.harmonicprocesses.penelopefree.audio.AudioConstants;
import com.harmonicprocesses.penelopefree.audio.DSPEngine;
import com.harmonicprocesses.penelopefree.openGL.MyGLRenderer;
import com.harmonicprocesses.penelopefree.openGL.MyGLSurfaceView;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Parcel;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;
import android.widget.Toast;

@TargetApi(18)
public class MyEGLWrapper {

	Context mContext;
    
	private int[] NoteSpectrum;
	public float maxAmplitude = 0;
	public int maxAmpIdx = 0;
	private boolean fingerDown = false;
	private SharedPreferences mSharedPrefs;
	public static double AMPLITUDE_THRESHOLD =  AudioConstants.AMPLITUDE_THRESHOLD;
	
    public TextView note_display;

	private MyGLSurfaceView mView;

	public MyEGLWrapper(Context context,Surface codecInputSurface, MyGLSurfaceView view) {
		
		NoteSpectrum = DSPEngine.staticCalcNoteBins(AudioConstants.defaultBufferSize*2, 
				AudioConstants.sampleRate);
		// Create an OpenGL ES 2.0 context.
		mView = view;
		if (codecInputSurface != null) {
			//mSurface = camSurfView.getHolder().getSurface();
			mSurface = codecInputSurface;
			//camSurfView.getHolder().addCallback(getSurfaceCallback());
			//eglSetup();
		} else {
			//;
		}
        
        
        //SurfaceHolder
        /*/
        mView = surface;
        SurfaceHolder sHolder = mView.getHolder();
        sHolder.addCallback(getSurfaceCallback());
        sHolder.setFormat(PixelFormat.TRANSLUCENT);
        //*/
        
        // Set the Renderer for drawing on the GLSurfaceView
        //mRenderer = new MyGLRenderer(context);
        
        /*
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        setRenderer(mRenderer);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        
        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        //*/
        
        
        
        mContext = context;
        
        //bring textView into view
        //note_display = findViewById(R.id.note_reading).;
        
        
        //note_display = createTextView(mContext);
        
        //builder.makeText(context, R.array.notes, 1/44100);
        //builder.setView(this);
        //builder.show();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    }
    
    private Callback getSurfaceCallback() {
		return new SurfaceHolder.Callback(){

			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				mSurface = holder.getSurface();
		        eglSetup();
			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format,
					int width, int height) {
				// TODO Auto-generated method stub
			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				release();
			}
		};
	}

	private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
    private EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;
    private EGLSurface mEGLRecordSurface = EGL14.EGL_NO_SURFACE;
    private static final int EGL_RECORDABLE_ANDROID = 0x3142;

	private static final String TAG = "io.hpp.MyGLSurfaceView18";
    private Surface mSurface;
    private EGLConfig[] configs;
    private int[] version, configsAttribs = {
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE
    }, numConfigs, contextAttribs = {
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
    }, surfaceAttribs = {
            EGL14.EGL_NONE
    };

	public void init(){
		if (mSurface == null) {
			mSurface = getView().getHolder().getSurface();
			
		}
		eglSetup();
	}
    
    @TargetApi(17)
    private void eglSetup() {
    	mView.beginCapture();
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("unable to get EGL14 display");
        }
        version = new int[2];
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            throw new RuntimeException("unable to initialize EGL14");
        }

        //*
        // Configure EGL for recording and OpenGL ES 2.0.
        configs = new EGLConfig[1];
        numConfigs = new int[1];
        
        EGL14.eglChooseConfig(mEGLDisplay, configsAttribs, 0, configs, 0, configs.length,
                numConfigs, 0);
        checkEglError("eglCreateContext RGB888+recordable ES2");

        
        // Configure context for OpenGL ES 2.0.
       
        
        // replace eglCreateContext with eglGetCurrectContext 
        //mEGLContext = EGL14.eglGetCurrentContext();
        //checkEglError("eglGetCurrentContext");
        /* For reference: EGL14.eglGetCurrentContext() == javax.microedition.khronos.egl.EGLContext mView.getEGLContext();*/
        //EGLContext currentContext = EGL14.eglGetCurrentContext();
        //javax.microedition.khronos.egl.EGLContext mGLViewContext = mView.getEGLContext();
        saveToScreenRenderState();
        
        mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], mScreenEglContext, 
        		contextAttribs, 0);
        checkEglError("eglCreateContext");

        
        // Create a window surface, and attach it to the Surface we received.
        
        mEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, configs[0], mSurface,
                      surfaceAttribs, 0);
        checkEglError("eglCreateWindowSurface");
        //*/
    }
    
    EGLDisplay mScreenEglDisplay;
    EGLSurface mScreenEglDrawSurface;
    EGLSurface mScreenEglReadSurface;
    EGLContext mScreenEglContext;
    /**
     * Saves the current projection matrix and EGL state.
     */
    private void saveToScreenRenderState() {
    	//System.arraycopy(mProjectionMatrix, 0, mSavedMatrix, 0, mProjectionMatrix.length);
    	mScreenEglDisplay = EGL14.eglGetCurrentDisplay();
    	mScreenEglDrawSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW);
    	mScreenEglReadSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_READ);
    	mScreenEglContext = EGL14.eglGetCurrentContext();
    }
    
    
    /**
     * Discards all resources held by this class, notably the EGL context.  Also releases the
     * Surface that was passed to our constructor.
     */
    public void release() {
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
            EGL14.eglReleaseThread();
            EGL14.eglTerminate(mEGLDisplay);
        }
        mSurface.release();

        mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        mEGLContext = EGL14.EGL_NO_CONTEXT;
        mEGLSurface = EGL14.EGL_NO_SURFACE;

        mSurface = null;
    
    }
    
    /**
     * Make the EGL Context current with the Camera Preview Surface or the 
     * MediaCodec input surface. Call this method on Main (or whatever thread
     * owns the EGLContext). 
     * @param isRecording, if false camera preview, true => recording 
     * @param surface, The media codec input surface
     */
    public void makeCurrent(boolean toScreen, Surface surface) {
    	if (toScreen) { //as opposed to toEncoder
    		makeScreenSurfaceCurrent();
    		return;
    	} 
    	if (mEGLSurface.equals(EGL14.EGL_NO_SURFACE)){
    		EGL14.eglDestroySurface(mEGLDisplay,mEGLRecordSurface);
    		checkEglError("eglDestroySurface");
    		
    		mEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, configs[0], mSurface,
    				surfaceAttribs, 0);
    		checkEglError("eglCreateWindowSurface");
    	}
    	EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext);
    	checkEglError("eglMakeCurrent");
    }

	private void makeScreenSurfaceCurrent() {
    	EGL14.eglMakeCurrent(mScreenEglDisplay, mScreenEglDrawSurface, 
    			mScreenEglReadSurface, mScreenEglContext);
    	checkEglError("eglMakeCurrent");
	}

	private void makeCodecSurfaceCurrent(Surface surface) {
		if (mEGLRecordSurface.equals(EGL14.EGL_NO_SURFACE)){
    		//EGLConfig[] tConfig = new EGLConfig[10];
    		//int tConfigSize = 10;
    		//int[] num_config = new int[10], attrib_list = new int[1];
    		
    		//EGL14.eglGetConfigs(mEGLDisplay, tConfig, 0, tConfigSize, num_config, 0);
    		//for (int i = 0; i < num_config[0]; i++){
    		//	EGL14.eglGetConfigAttrib(mEGLDisplay, tConfig[0], attribute, value, offset)
    		//}
    		//mEGLRecordSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, tConfig[0], surface, attrib_list, 0);

    		EGL14.eglDestroySurface(mEGLDisplay,mEGLSurface);
    		checkEglError("eglDestroySurface");
    		
    		mEGLRecordSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, configs[0], surface,
    				surfaceAttribs, 0);
    		checkEglError("eglCreateWindowSurface");
    	}
    	EGL14.eglMakeCurrent(mEGLDisplay, mEGLRecordSurface, mEGLRecordSurface, mEGLContext);
    	checkEglError("eglMakeCurrect");
	}

	/**
     * Calls eglSwapBuffers.  Use this to "publish" the current frame.
     */
    public boolean swapBuffers(boolean isRecording) {
    	boolean result;
        /* if (isRecording) {
        	result = EGL14.eglSwapBuffers(mEGLDisplay, mEGLRecordSurface);
        	checkEglError("eglSwapBuffers");
        	return result;
        } */
        	
    	result = EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface);
        checkEglError("eglSwapBuffers");
        
        return result;
    }

    /**
     * Sends the presentation time stamp to EGL.  Time is expressed in nanoseconds.
     */
    public void setPresentationTime(long nsecs, boolean isRecording) {
    	/*if (isRecording){
    		setPresentationToSurface(nsecs);
    		return;
    	} */
    	EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, nsecs);	
    	checkEglError("eglPresentationTimeANDROID");
    }

    /**
     * Sends the presentation time stamp to EGL.  Time is expressed in nanoseconds.
     */
    public void setPresentationToSurface(Long nsecs) {
    	
    	
    	EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLRecordSurface, nsecs);
        checkEglError("eglPresentationTimeANDROID");
    }
    
    /*private TextView createTextView(Context context) {
        TextView noteView = new TextView(context);
        noteView.setId(R.id.note_reading);
        noteView.setGravity(TEXT_ALIGNMENT_TEXT_START);
        noteView.setHeight(100);
        noteView.setWidth(100);
        noteView.setText(R.string.StartingNote);
        noteView.setBackgroundColor(0);
    	noteView.bringToFront();
    	/*<TextView
        android:id="@+id/note_reading"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center_horizontal"
        android:text="@string/StartingNote"
        android:textAppearance="?android:attr/textAppearanceMedium" />
		return noteView;
	}*/

	public MyEGLWrapper getMyEGLWrapper(Context context){
    	return this;
    }
	
	public Surface setOutputSurface(Surface surface){
		/*
		EGL14.eglSurfaceAttrib(dpy, surface, attribute, value)
		mSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, configs[0], surface,
                surfaceAttribs, 0);
        checkEglError("eglCreateWindowSurface");
        surface.readFromParcel(new Parcel());
        //*/
        return mSurface;
	}
	
    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private float mPreviousX;
    private float mPreviousY;
    
    public View.OnTouchListener listenForTouch = new View.OnTouchListener() {
	
		@Override
		public boolean onTouch(View v, MotionEvent e) {
			// MotionEvent reports input details from the touch screen
	        // and other input controls. In this case, you are only
	        // interested in events where the touch position changed.
	    	if (e.getAction()==MotionEvent.ACTION_DOWN){
	    		fingerDown = true;
	    	} else if (e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){
	    		fingerDown = false;
	    	}
	    	
	    	/*********
	        float x = e.getX();
	        float y = e.getY();

	        switch (e.getAction()) {
	            case MotionEvent.ACTION_MOVE:

	                float dx = x - mPreviousX;
	                float dy = y - mPreviousY;

	                // reverse direction of rotation above the mid-line
	                if (y > getHeight() / 2) {
	                  dx = dx * -1 ;
	                }

	                // reverse direction of rotation to left of the mid-line
	                if (x < getWidth() / 2) {
	                  dy = dy * -1 ;
	                }

	                mRenderer.mAngle += (dx + dy) * TOUCH_SCALE_FACTOR;  // = 180.0f / 320
	                sendRequestRender();
	        }

	        mPreviousX = x;
	        mPreviousY = y;
	        //********/
	    	
	        //return true;
			return true;
		}
	
	
	    	
	};
   

	private void sendRequestRender() {
		if (mSharedPrefs.getBoolean("turn_on_visualization_key",true)){
    		//swapBuffers();
    	}
	}
    
    /**
     * Checks for EGL errors.  Throws an exception if one is found.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	private void checkEglError(String msg) {
        int error;
        if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            Log.d(TAG,msg + ": EGL error: 0x" + Integer.toHexString(error));
            new Exception().printStackTrace();
        }
    }

	public MyGLSurfaceView getView() {
		return mView;
	}

	public void setOnTouchListener(OnTouchListener listenForTouch2) {
		mView.setOnTouchListener(listenForTouch2);	
	}

	public void swapCodecInputSurfaceBuffer() {
        boolean result = EGL14.eglSwapBuffers(mEGLDisplay, mEGLRecordSurface);
        checkEglError("eglSwapBuffers");

	}

	public void makeCurrent(EGLContext context) {
		mView.makeCurrent();
	}
}

