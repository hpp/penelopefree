package com.harmonicprocesses.penelopefree.openGL;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import com.harmonicprocesses.penelopefree.audio.AudioConstants;
import com.harmonicprocesses.penelopefree.audio.DSPEngine;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.EGLContextFactory;
import android.opengl.GLSurfaceView.EGLWindowSurfaceFactory;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class MyGLSurfaceViewLegacy extends MyGLSurfaceView {

    //public final MyGLRenderer mRenderer;
    
    Context mContext;
    
    private int[] NoteSpectrum;
	public float maxAmplitude = 0;
	public int maxAmpIdx = 0;
	private boolean fingerDown = false;
	private SharedPreferences mSharedPrefs;
	private MyContextFactory mContextFactory;
	
	
    public TextView note_display;

	private MyEGLWindowSurfaceFactory mEGLWindowSurfaceFactory;

    public MyGLSurfaceViewLegacy(Context context) {
        super(context);
        setListenForTouchOnTouchListener();
        NoteSpectrum = DSPEngine.staticCalcNoteBins(AudioConstants.defaultBufferSize*2, 
        		AudioConstants.sampleRate);
        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);
        // Set the Renderer for drawing on the GLSurfaceView
        mRenderer = new MyGLRenderer(context,this);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mContextFactory = new MyContextFactory();
        setEGLContextFactory(mContextFactory);
        //mEGLWindowSurfaceFactory = new MyEGLWindowSurfaceFactory();
        //setEGLWindowSurfaceFactory(mEGLWindowSurfaceFactory);
        setRenderer(mRenderer);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        
        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        
        
        
        mContext = context;
        
        //bring textView into view
        //note_display = findViewById(R.id.note_reading).;
        
        
        //note_display = createTextView(mContext);
        
        //builder.makeText(context, R.array.notes, 1/44100);
        //builder.setView(this);
        //builder.show();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
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

	public MyGLSurfaceViewLegacy getMyGLSurfaceView(Context context){
    	return this;
    }
	
	
	public MyGLRenderer getRenderer(){
		return mRenderer;
	}
	
	
    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private float mPreviousX;
    private float mPreviousY;

    private void setListenForTouchOnTouchListener(){ 
	    super.listenForTouch = new OnTouchListener() {
	
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
    	
    }

	private void sendRequestRender() {
		if (mSharedPrefs.getBoolean("turn_on_visualization_key",true) && !capturingVideo){
			requestRender();
		}
	}
    		
	public int updateAmplitudes(float[] amplitudes) throws Exception{
		mRenderer.mAmplitude = amplitudes;
    	maxAmplitude = mRenderer.mAmplitude[maxAmpIdx];
    	mRenderer.mNoteAmp = maxAmplitude;
    	double minAmplitude = AMPLITUDE_THRESHOLD;
    	
    	
    	
    	for (int i=24;i<72;i++){
    		if (mRenderer.mAmplitude[i]>maxAmplitude && mRenderer.mAmplitude[i]>minAmplitude){
    			maxAmplitude = mRenderer.mAmplitude[i];
    			if (!fingerDown){
    				maxAmpIdx = i;
    			}
    		}
    	}
    	
    	
    		
    	//builder.cancel();
    	//builder.
    	//builder.show();
    	if (maxAmpIdx>=24) {
    		mRenderer.mNote = maxAmpIdx%12;
    		int temp = (int) Math.ceil((maxAmpIdx-23.0f)/12.0f);
    		if (temp==0){
    			//catch me
    			temp = 1;
    		}
    		mRenderer.mMode = temp;
    	}
    	
    	
    	
    	//note_display.bringToFront();
    	sendRequestRender();
    	
    	float xAccel = mRenderer.mAccelmeter.linear_acceleration[0];
		if (xAccel>1.0){
			return maxAmpIdx + 1;
		} else if (xAccel<-1.0){
			return maxAmpIdx -1;
		} else { 
			return maxAmpIdx;
		}
    }
	
	@Override
	public void makeCurrent(){
		mContextFactory.makeCurrent(mEGLWindowSurfaceFactory.getSurface());
	}
	
	@Override
	public EGLContext getEGLContext(){
		return mContextFactory.getContext();
	}
    
}

class MyContextFactory implements EGLContextFactory {
    private int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    private EGLContext mEGLContext;
    private EGLDisplay mEGLDisplay;
    private MyGLSurfaceView mMyGLSurfaceView;
    private EGL10 mEGL;
    
	@Override
	public EGLContext createContext(EGL10 egl, EGLDisplay display,
			EGLConfig eglConfig) {
		int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL10.EGL_NONE };
		mEGL = egl;
		mEGLContext = egl.eglGetCurrentContext();
		mEGLDisplay = display;
		mEGLContext = egl.eglCreateContext(display, eglConfig, 
				egl.eglGetCurrentContext(), attrib_list);
		return mEGLContext;
	}

	@Override
	public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
        if (!egl.eglDestroyContext(display, context)) {
            Log.e("MyContextFactory", "display:" + display + " context: " + context);
            Log.i("MyContextFactory", "tid=" + Thread.currentThread().getId());
        }
	}
	
	public EGLContext getContext(){
		return mEGLContext;
	}
	
	public void makeCurrent(EGLSurface surface){
		mEGL.eglMakeCurrent(mEGLDisplay, surface, surface, mEGLContext);
	}

}

class MyEGLWindowSurfaceFactory implements EGLWindowSurfaceFactory {
	
	private EGLSurface mEGLSurface;
	
	@Override
	public EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display,
			EGLConfig config, Object nativeWindow) {
		mEGLSurface = egl.eglCreateWindowSurface(display, config, nativeWindow, null);
		return mEGLSurface;
	}

	@Override
	public void destroySurface(EGL10 egl, EGLDisplay display, EGLSurface surface) {
		egl.eglDestroySurface(display, surface);		
	}
	
	public EGLSurface getSurface(){
		return mEGLSurface;
	}
}

