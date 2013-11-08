/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.harmonicprocesses.penelopefree.openGL;    
import com.harmonicprocesses.penelopefree.*;
import com.harmonicprocesses.penelopefree.R.drawable;
import com.harmonicprocesses.penelopefree.R.raw;
import com.harmonicprocesses.penelopefree.openGL.shapes.DarkParticles;
import com.harmonicprocesses.penelopefree.openGL.shapes.LightParticles;
import com.harmonicprocesses.penelopefree.openGL.shapes.NoteBillboard;
import com.harmonicprocesses.penelopefree.openGL.shapes.OuterCircle;
import com.harmonicprocesses.penelopefree.openGL.shapes.Particles;
import com.harmonicprocesses.penelopefree.openGL.shapes.Square;
import com.harmonicprocesses.penelopefree.openGL.shapes.Triangle;
import com.harmonicprocesses.penelopefree.openGL.utils.Accelmeter;
import com.harmonicprocesses.penelopefree.openGL.utils.SoundParticle;
import com.harmonicprocesses.penelopefree.openGL.utils.SoundParticleHexBins;
import com.harmonicprocesses.penelopefree.settings.SettingsActivity;
import com.harmonicprocesses.penelopefree.renderscript.ScriptC_particleFilter;
import com.harmonicprocesses.penelopefree.renderscript.ScriptField_particle;
import com.hpp.openGL.MyEGLWrapper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.drawable.LevelListDrawable;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Build;
import android.preference.PreferenceManager;
import android.renderscript.Allocation;
import android.renderscript.BaseObj;
import android.renderscript.Element;
import android.renderscript.Element.DataType;
import android.renderscript.Matrix3f;
import android.renderscript.RenderScript;
import android.renderscript.Script;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class MyGLRenderer implements GLSurfaceView.Renderer {
	
	private Context mContext;
	private int subDivision = 12;
	private int numSquares = subDivision*4*subDivision;
	private int numParticles = 6;
	private float particleDelta = 0.01f; //distance sound travels per iteration on plate
	
    private static final String TAG = "MyGLRenderer";
    private Triangle mTriangle;
    private Square mSquare[] = new Square[96];
    private NoteBillboard mBillboard;
    private OuterCircle mOuterCircle;
    private SoundParticle[] mParticle = new SoundParticle[numParticles];
    private Particles mParticles;
    private LightParticles mLightParticles; 
    private DarkParticles mDarkParticles; 
    
    private SoundParticleHexBins mParticleBins;
    private float[] xOffset = new float[numParticles], yOffset = new float[numParticles];
    private ScriptField_particle[] particleData = new ScriptField_particle[numParticles];
    private float[] particleVBO = new float[numParticles*3];

    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjMatrix = new float[16];
    private final float[] mVMatrix = new float[16];
    private final float[] mRotationMatrix = new float[16];
    private final float[] mTranslationMatrix = new float[16];
    private final float[] mTransposeMatrix = new float[16];
    private final float[] mScaleMatrix = new float[16];
    
    private int mTextureID;
    private Paint mLabelPaint;
    //private LabelMaker mLabels;
    //private NumericSprite mNumericSprite;
    private float mWidth = (float) 1.0;
    private float mHeight = (float) 1.0;
    
    /** This will be used to pass in the texture. */
    private int mTextureUniformHandle;
     
    /** This will be used to pass in model texture coordinate information. */
    private int mTextureCoordinateHandle;
     
    /** Size of the texture coordinate data in elements. */
    private final int mTextureCoordinateDataSize = 2;
     
    /** This is a handle to our texture data. */
    private int mTextureDataHandle;
    
    // Declare as volatile because we are updating it from another thread
    public volatile float mAngle, mNoteAmp;
    public volatile float[] mAmplitude = new float[numSquares];
    public volatile int mNote = 1, mMode = 4;
    
    
    //Declare variables for renderscript
    private RenderScript mRS;
    private Allocation mInAllocation;
    private Allocation mOutAllocation;
    private ScriptC_particleFilter mScript;
    private SharedPreferences mSharedPrefs;
    public Accelmeter mAccelmeter;
	private MyGLSurfaceView mGLSurfaceView;
    
    
    
    public MyGLRenderer(Context context, MyGLSurfaceView glSurfaceView){
    	mContext = context;
    	mGLSurfaceView = glSurfaceView;
    	mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mLabelPaint = new Paint();
        mLabelPaint.setTextSize(32);
        mLabelPaint.setAntiAlias(true);
        mLabelPaint.setARGB(0xff, 0x00, 0x00, 0x00);
        mAccelmeter = new Accelmeter(context);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        
    	if (mSharedPrefs.getBoolean("turn_on_accelerometer_key", true)){
    		mAccelmeter.start();
    	}
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        mTextureID = textures[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
        		GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
        		GLES20.GL_TEXTURE_MAG_FILTER,
        		GLES20.GL_LINEAR);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
        		GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
        		GLES20.GL_CLAMP_TO_EDGE);

        //GLES20.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE,
        //        GL10.GL_REPLACE);
        
        /*
        if (mLabels != null) {
            mLabels.shutdown(gl);
        } else {
            mLabels = new LabelMaker(true, 256, 256);
        }

        if (mNumericSprite != null) {
            mNumericSprite.shutdown(gl);
        } else {
            mNumericSprite = new NumericSprite();
        }
        mNumericSprite.initialize(gl, mLabelPaint);*/
        
        //mTriangle = new Triangle();
        //mSquare = new Square(0.f);
        /*for (int i = 0; i<mSquare.length; i++){
        	mSquare[i] = new Square(0.01f*i-0.48f);
        }*/
		float len = 1.0f/subDivision;
		int dist = 2*subDivision;
		
		/* Commented out for square lattice rendering
		for (int i = 0; i<dist; i++){
			for (int j = 0; j<dist; j++){
				mSquare[i*dist+j] = new Square(-1+j*len,-1+i*len,len);
			}
		}//*/
		
		for (int i = 0; i<96; i++){
			mSquare[i] = new Square(0.5f-i*0.01f,0.0f,0.01f);
		}
        
        mParticleBins = new SoundParticleHexBins(len, dist);
        
        mBillboard = new NoteBillboard(mContext);
        mOuterCircle = new OuterCircle(mNote, 64); // start at A,    
        
        //SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        int numLightParticles = mSharedPrefs.getInt("vis_number_of_particles_key", 6000);
        //int numLightParticles = Integer.valueOf(numLightParticlesSting);
        mLightParticles = new LightParticles(numLightParticles);
        mDarkParticles = new DarkParticles(numLightParticles);
        
        for (int i = 0; i<numParticles; i++){
        	mParticle[i] = new SoundParticle(particleDelta, (float) Math.cos(2*Math.PI*i*440/44100));
        	for (int j = i; j>=0; j--){
        		mParticle[i].next();
        	}
        }
        
        float[] particleInits = new float[numParticles*3];
        for (int i = 0; i<numParticles; i++){
        	//particleData[i].set(0, 0, mParticle[i].location[0]);
        	//particleData[i].set(0, 1, mParticle[i].location[1]);
        	//particleData[i].set(0, 2, mParticle[i].amplitude);
        	//particleData[i].set(1, 0, mParticle[i].launchAngle);
        	//particleData[i].set(1, 1, mParticle[i].theta);
        	//particleData[i].set(1, 2, mParticle[i].distance2edge);
            
        	//float x;
        	//float y;
        	//float launchAngle;
        	//float theta;
        	//float furlong;
        	//float distance2edge;
        	//float delta;
        	//float amplitude;
        	
            particleVBO[i*3+0] = mParticle[i].location[0];
        	particleVBO[i*3+1] = mParticle[i].location[1];
        	//particleVBO[i*8+2] = mParticle[i].launchAngle; 
        	//particleVBO[i*8+3] = mParticle[i].theta; 
        	//particleVBO[i*8+4] = mParticle[i].furlong;
        	//particleVBO[i*8+5] = mParticle[i].distance2edge;
        	//particleVBO[i*8+6] = mParticle[i].delta;
        	particleVBO[i*3+2] = 0; //mParticle[i].amplitude;
        }        
        
       //createParticleScript(particleInits);
       //mParticles = new Particles(new float[] {0.0f, 1.0f, 0.0f, 0.1f},particleVBO, 
       // 		particleInits, particleDelta);
    }


	@Override
    public void onDrawFrame(GL10 gl) {
		drawFrame();
		
    }

	public void drawFrame() {
		// Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mVMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mVMatrix, 0);

        // move particles and calc bins
        //mParticleBins.clear();
        
        /*****************************************
        for (int i = 0;i<numParticles;i++){
        	//Matrix.setIdentityM(mTranslationMatrix, 0);
        	mParticle[i].next();
        	mParticleBins.add(mParticle[i]);
            particleVBO[i*3] = mParticle[i].location[0];
        	particleVBO[i*3+1] = mParticle[i].location[1];
        }
        mParticleBins.normallize();
        //*/
        
        int j = 0;
        for (int i = 0; i < mSquare.length; i++){
        	Matrix.setIdentityM(mScaleMatrix, 0);
        	//Matrix.setIdentityM(mTranslationMatrix, 0);
        	Matrix.scaleM(mScaleMatrix, 0, 1.0f, 1000.0f*mAmplitude[j++], 1.0f);
        	Matrix.multiplyMM(mTransposeMatrix, 0, mScaleMatrix , 0, mMVPMatrix, 0);
        	//Matrix.translateM(mTranslationMatrix, 0, 0.3f, 0.3f, 0.0f);
        	//Matrix.multiplyMM(mTransposeMatrix, 0, mScaleMatrix, 0, mRotationMatrix, 0);
        	if (i == 12+mMode*12+mNote) { // is fundamental
        		mSquare[i].draw(mTransposeMatrix, 1.0f);//mParticleBins.normallizedBins[i]);
        	} else if ( i > 12+mMode*12 && i < 24+mMode*12) { // in mode
        		mSquare[i].draw(mTransposeMatrix, 0.3f);//mParticleBins.normallizedBins[i]);
        	} else {
        		mSquare[i].draw(mTransposeMatrix, 0.0f);//mParticleBins.normallizedBins[i]);
        	}
        }
        //***************************************/
        
        //Matrix.multiplyMM(mMVPMatrix, 0,mMVPMatrix , 0,mTransposeMatrix , 0);
        // Create a rotation for the triangle
//        long time = SystemClock.uptimeMillis() % 4000L;
//        float angle = 0.090f * ((int) time);
        //Matrix.setRotateM(mRotationMatrix, 0, mAngle, 0, 0, -1.0f);
        //Matrix.setIdentityM(mRotationMatrix, 0);
        //Matrix.translateM(mRotationMatrix, 0, 0.3f, 0.3f, 0.0f);
               
        // Combine the rotation matrix with the projection and camera view
        //Matrix.multiplyMM(mMVPMatrix, 0, mRotationMatrix, 0, mMVPMatrix, 0);
        
        //mScript.invoke_getNextPosition();
        double ay = Math.abs(mAccelmeter.linear_acceleration[1]);
        if (ay>1.0){
        	mMode = 0;
        }
        
        // Draw triangle
        mBillboard.draw(mMVPMatrix,mNote);
        mOuterCircle.draw(mMVPMatrix, mNote);
        mLightParticles.draw(mMVPMatrix, mOuterCircle.getRadius(), mMode, mNoteAmp);
        mDarkParticles.draw(mMVPMatrix, mOuterCircle.getRadius(), mMode, mNoteAmp);
        //nextParticleVBO();
        //mParticles.draw(mMVPMatrix, particleVBO);
        
		
	}
		

	@Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        // Adjust the viewport based on geometry changes,
        // such as screen rotation
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 3, 7);

    }

    public static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader " + shaderType + ":");
                Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    public void sizeChanged(GL10 gl, int w, int h) {
        mWidth = w;
        mHeight = h;
    }
    
    /**
     * Utility method for debugging OpenGL calls. Provide the name of the call
     * just after making it:
     *
     * <pre>
     * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
     * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
     *
     * If the operation is not successful, the check throws an error.
     *
     * @param glOperation - Name of the OpenGL call to check.
     */
    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }
    
    private void createParticleScript(float[] inits) {
    	mRS = RenderScript.create(mContext);
        mInAllocation = Allocation.createSized(mRS, ScriptField_particle.createElement(mRS), numParticles);
        mOutAllocation = Allocation.createTyped(mRS, mInAllocation.getType());
        mScript = new ScriptC_particleFilter(mRS, mContext.getResources(), R.raw.particlefilter);
        nextParticleVBO();
        //return output;
	}
    
    private void nextParticleVBO() {
	    mInAllocation.copy1DRangeFromUnchecked(0, numParticles, particleVBO);
	    //mScript.forEach_root(mInAllocation, mOutAllocation);
	     
	    //mOutAllocation.;
	    
	}


    
       
}

