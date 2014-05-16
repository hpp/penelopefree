package com.hpp.openGL;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

public class JuliaDream {

    final float[] JULIA_COMPLEX_VALUES = {-0.68972704986346f, -0.30674095052674f};//-0.732261f,0.225087f};//{0.373f, 0.093f};//{-0.8f, 0.156f};
    final int JULIA_NUM_ITERS = 100;
	final String TAG = "io.hpp.JuliaDream";
    int mJuliaTime, aDisplayProgram, aPositionHandle;
    int[] juliaFBO, juliaRBO, juliaTex, jCalcTex;;
    IntBuffer juliaTexBuff;
	int mJuliaC, mJuliaIter, mTime = 1;
	STextureRender rain;
	boolean juliaEnable;
	int juMVPMatrixHandle;
	int juSTMatrixHandle;
	
	
	int jaPositionHandle;
	int jaTextureHandle;
	int juliaDisplayProgram=-1;
	private int juDisplayTextureHandle;
	
	
	public JuliaDream(){
	}
	
	public JuliaDream setRenderer(STextureRender stRenderer){
		rain = stRenderer;
		return this;
	}

	public void bindFrameBuffer(){
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, juliaFBO[0]);
		rain.setView();
		
		// bind FBO, Tex to FBO, and RBO to FBO as color
    	//Log.d(TAG, "Julia FBO = " + juliaFBO[0] + "; RBO = " +  juliaFBO[0] +
    	//		"; Tex = " + juliaTex[0]);
		
		//swap texture I/O, last output goes to input and old input is used for new output
		int[] temp = jCalcTex;
		jCalcTex = juliaTex;
		juliaTex = temp;
		    	
    	GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, 
    			GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_RENDERBUFFER, 
    			juliaRBO[0]);
    	rain.checkGlError("glFramebufferRenderbuffer");
    	GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, 
    			GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, 
    			juliaTex[0], 0);
    	rain.checkGlError("glFramebufferTexture2D");

    	int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
    	
    	//error checker
    	if (status == GLES20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT){
    		Log.d(TAG,"Julia FBO Failed to attach, error Framebuffer_Incomplete_Attachment");
    	} else if (status != GLES20.GL_FRAMEBUFFER_COMPLETE){
    	    Log.d(TAG,"Julia FBO Failed to attach! Frabuffer Status = " + status);
		}
    	
	}
	
	public void setupDrawFrameBuffer(int program) {

    	int locCalc = GLES20.glGetUniformLocation(program, "calcTexture");
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    	GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, jCalcTex[0]);
    	GLES20.glUniform1i(locCalc, 1);
	}

	public void DrawDisplayFrame(float[] mMVPMatrix, float[] mSTMatrix) {
		surafaceChanged(rain.width, rain.height);
		
		//Return display buffer 0
    	GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    	rain.setView();
    	
    	GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    	GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT );
    	
        GLES20.glUseProgram(juliaDisplayProgram);
        rain.checkGlError("glUseProgram julia");
    	
    	// bind the framebuffer texture 
    	GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
    	GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, juliaTex[0]);
    	GLES20.glUniform1i(juDisplayTextureHandle, 0);
    	
    	mTriangleVertices.position(rain.TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(jaPositionHandle, 3, GLES20.GL_FLOAT, false,
                rain.TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
        rain.checkGlError("glVertexAttribPointer jaPositionHandle");
        
        GLES20.glEnableVertexAttribArray(jaPositionHandle);
        rain.checkGlError("glEnableVertexAttribArray jaPositionHandle");

        mTriangleVertices.position(rain.TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(jaTextureHandle, 2, GLES20.GL_FLOAT, false,
        		rain.TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
        rain.checkGlError("glVertexAttribPointer jaTextureHandle");
        GLES20.glEnableVertexAttribArray(jaTextureHandle);
        rain.checkGlError("glEnableVertexAttribArray jaTextureHandle");
        
        //Matrix.setIdentityM(mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(juMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(juSTMatrixHandle, 1, false, mSTMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        rain.checkGlError("glDrawArrays");
	}
	
	public void setupJuliaDisplayProgram() {

    	juliaDisplayProgram = rain.createProgram(rain.VERTEX_SHADER, rain.FRAGMENT_SHADER2);
    	if (juliaDisplayProgram == 0) {
            Log.d(TAG,"failed creating program");
        }    	
    	
        jaPositionHandle = GLES20.glGetAttribLocation(juliaDisplayProgram, "aPosition");
        rain.checkGlError("glGetAttribLocation aPosition");
        if (jaPositionHandle == -1) {
            Log.d(TAG,"Could not get attrib location for jaPosition");
        }
        jaTextureHandle = GLES20.glGetAttribLocation(juliaDisplayProgram, "aTextureCoord");
        rain.checkGlError("glGetAttribLocation aTextureCoord");
        if (jaTextureHandle == -1) {
        	Log.d(TAG,"Could not get attrib location for jaTextureCoord");
        }
    	
        juMVPMatrixHandle = GLES20.glGetUniformLocation(juliaDisplayProgram, "uMVPMatrix");
        rain.checkGlError("glGetUniformLocation uMVPMatrix");
        if (juMVPMatrixHandle == -1) {
        	Log.d(TAG,"Could not get attrib location for juMVPMatrixHandle");
        }

        juSTMatrixHandle = GLES20.glGetUniformLocation(juliaDisplayProgram, "uSTMatrix");
        rain.checkGlError("glGetUniformLocation uSTMatrix");
        if (juSTMatrixHandle == -1) {
        	Log.d(TAG,"Could not get attrib location for juSTMatrixHandle");
        }
        
        juDisplayTextureHandle = GLES20.glGetUniformLocation(juliaDisplayProgram, "displayTexture");
        rain.checkGlError("glGetUniformLocation juDisplayTextureHandle");
        if (juSTMatrixHandle == -1) {
            Log.d(TAG,"Could not get uniform location for juDisplayTextureHandle");
        }

	}

	public void setupFrameBuffers() {
		juliaFBO = new int[1];
    	juliaRBO = new int[1];
    	juliaTex = new int[1];
    	jCalcTex = new int[1];
        
    	
    	// Gen buffers and texture
    	GLES20.glGenFramebuffers(1, juliaFBO, 0);
    	GLES20.glGenRenderbuffers(1, juliaRBO, 0);
    	GLES20.glGenTextures(1, juliaTex, 0);
    	GLES20.glGenTextures(1, jCalcTex, 0);

    	GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,juliaFBO[0]);
    	
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, jCalcTex[0]);
        rain.checkGlError("glBindTexture mTextureID");

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        rain.checkGlError("glTexParameter CalcTex");
    	
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 
    			rain.width, rain.height, 0, GLES20.GL_RGBA, 
    			GLES20.GL_UNSIGNED_SHORT_4_4_4_4, null);
    	
        
    	
    	// bind texture
    	GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, juliaTex[0]);
    	
    	// clamp texture to edges
    	GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
    	        GLES20.GL_CLAMP_TO_EDGE);
    	GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
    	        GLES20.GL_CLAMP_TO_EDGE);
    	GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
    	        GLES20.GL_LINEAR);
    	GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
    	        GLES20.GL_LINEAR);
    	rain.checkGlError("glTexParameter JuliaTex");
    	
    	// create it
    	/*
    	int[] buf = new int[rain.width * rain.height];
    	juliaTexBuff = ByteBuffer.allocateDirect(buf.length
    	        * rain.FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asIntBuffer();
    	 */
    	// generate the textures
    	GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 
    			rain.width, rain.height, 0, GLES20.GL_RGBA, 
    			GLES20.GL_UNSIGNED_SHORT_4_4_4_4, null);
    	 
    	// create render buffer and bind 16-bit depth buffer
    	GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, juliaRBO[0]);
    	rain.checkGlError("glBindRenderBuffer");
    	GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_RGBA4, 
    			rain.width, rain.height);
    	Log.d(TAG,"Julia Params: Width = " + rain.width + "; Height = " + rain.height);
    	rain.checkGlError("glRenderBufferStorage");
    	
    	//GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, 
    	//		GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, 
    	//		juliaTex[0], 0);
    	
    	// return to display frame buffer
    	GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    	rain.checkGlError("glBindFrameBuffer 0");
    }

	public void startFragmentShader() {
		juliaDisplayProgram = rain.createProgram(rain.VERTEX_SHADER, rain.FRAGMENT_SHADER2);
        if (juliaDisplayProgram == 0) {
            Log.d(TAG,"failed creating program");
        }
	}

	public boolean checkActive(String fragmentShader) {
		if (fragmentShader.contains(rain.JULIA_FRAGMENT_SHADER))
			juliaEnable = true;
		else 
			juliaEnable = false;
		//juliaEnable = false;
		return juliaEnable;
	}
	
	public boolean checkActive(){
		return juliaEnable;
	}

	public void setJuliaShaderAttribs(int program) {
		mJuliaIter = GLES20.glGetUniformLocation(program, "juliaIter");
        rain.checkGlError("glGetUniformLocation mJuliaIter");
        if (mJuliaIter == -1) {
        	Log.d(TAG,"Could not get attrib location for juliaIter");
        }
        
        mJuliaC = GLES20.glGetUniformLocation(program, "juliaC");
        rain.checkGlError("glGetUniformLocation aTextureCoord");
        if (mJuliaC == -1) {
        	Log.d(TAG,"Could not get attrib location for juliaC");
        }
        
        mJuliaTime = GLES20.glGetUniformLocation(program, "time");
        rain.checkGlError("glGetUniformLocation aTextureCoord");
        if (mJuliaTime == -1) {
        	Log.d(TAG,"Could not get attrib location for mJuliaTime");
        }
	}

	public void clearJuliaShaderAttribs() {
		mJuliaTime = -1;
		mJuliaC = -1;
		mJuliaIter = -1; 
	}

	public void clearDisplayProgram() {
		if (juliaDisplayProgram!=-1){
			GLES20.glDeleteProgram(juliaDisplayProgram);
		}
	}
	
	public void surafaceChanged(int w, int h){
    	float ratio = (float) w / (float) h;  
    	if(mRatio==ratio)return;
    	mRatio = ratio;
    		
        // Adjust the viewport based on geometry changes,
    	if (ratio>1) { //Landscape
        	float[] landscapeVerts = {
                    // X, Y, Z, U, V
                    -ratio, -1.0f, 0.0f, 0.0f, 1.0f,
                    ratio, -1.0f, 0.0f, 1.0f, 1.0f, 
                    -ratio,  1.0f, 0.0f, 0.0f, 0.0f,
                    ratio,  1.0f, 0.0f, 1.0f, 0.0f,
            };
        	mTriangleVertices.put(landscapeVerts).position(0);
    	} else { //Portrait
    		float[] portraitVerts = {
                    // X, Y, Z, U, V
                    -ratio, -1.0f, 0.0f, 1.0f, 1.0f,
                    ratio, -1.0f, 0.0f, 1.0f, 0.0f,
                    -ratio,  1.0f, 0.0f, 0.0f, 1.0f,
                    ratio,  1.0f, 0.0f, 0.0f, 0.0f,
            };
    		mTriangleVertices.put(portraitVerts).position(0);
    	}
    	 

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        //Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
    }
	//float[] mProjMatrix;
	FloatBuffer mTriangleVertices;
	float mRatio;

}
