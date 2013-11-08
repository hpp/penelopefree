package com.harmonicprocesses.penelopefree.openGL;

import javax.microedition.khronos.egl.EGLContext;

import com.harmonicprocesses.penelopefree.audio.AudioConstants;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.View;

public abstract class MyGLSurfaceView extends GLSurfaceView {

   
	public static double AMPLITUDE_THRESHOLD =  AudioConstants.AMPLITUDE_THRESHOLD;
	public View.OnTouchListener listenForTouch;
	public int maxAmpIdx;
	public float maxAmplitude;
	public MyGLRenderer mRenderer;
	protected boolean capturingVideo = false;
	EGLContext mEGLContext;

	public MyGLSurfaceView(Context context) {
		super(context);
	}

	public abstract int updateAmplitudes(float[] newSpectrum) throws Exception;

	public abstract EGLContext getEGLContext(); 

	public abstract void makeCurrent();

    public void beginCapture(){
    	capturingVideo = true;
    }
    
    public void endCapture(){
    	capturingVideo = false;
    }
}

