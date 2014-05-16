package com.hpp.openGL;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.harmonicprocesses.penelopefree.camera.CaptureManager;
import com.harmonicprocesses.penelopefree.openGL.MyGLSurfaceView;
import com.harmonicprocesses.penelopefree.openGL.shapes.OuterCircle;

import android.R;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

/**
 * Code for rendering a texture onto a surface using OpenGL ES 2.0.
 */
public class STextureRender {
    static final int FLOAT_SIZE_BYTES = 4;
    static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
    private static final float z_0 = 0.0f;
    private float[] mTriangleVerticesData = {
            // X, Y, Z, U, V
            -2.0f, -1.0f, z_0, 1.0f, 0.0f,
             2.0f, -1.0f, z_0, 0.0f, 0.0f,
            -2.0f,  1.0f, z_0, 1.0f, 1.0f,
             2.0f,  1.0f, z_0, 0.0f, 1.0f,
    };//*/

    FloatBuffer mTriangleVertices;

    static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 uSTMatrix;\n" +
            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTextureCoord;\n" +
            "varying vec2 vTextureCoord;\n" +
            "varying vec2 calcTexCoord;\n" +
            "void main() {\n" +
            "  gl_Position = uMVPMatrix * aPosition;\n" +
            "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
            "  calcTexCoord = aTextureCoord.xy;\n" +
            "}\n";

    public static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +      
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "void main() {\n" +
            "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
            "}\n";
    
    
    public static final String SORBEL_JULIA_FRAGMENT_SHADER = 
    		"#extension GL_OES_EGL_image_external : require\n" +
    		"precision mediump float;\n" +      
    		"varying vec2 vTextureCoord;\n" +
    		"uniform samplerExternalOES sTexture;\n" +
    		"uniform vec2 juliaC;\n" +
			"uniform int juliaIter;\n" +
			"uniform int time;\n" +
			"void main() {\n" +
			//Sorbel Filter
    		"	vec3 irgb = texture2D(sTexture, vTextureCoord).rgb;\n" +
    		"	float ResS = 720.;\n" +
    		"	float ResT = 720.;\n" +
    		"	vec2 stp0 = vec2(1./ResS, 0.);\n" +
    		"	vec2 st0p = vec2(0., 1./ResT);\n" +
    		"   vec2 stpp = vec2(1./ResS, 1./ResT);\n" +
    		"   vec2 stpm = vec2(1./ResS, -1./ResT);\n" +
    		"   const vec3 W = vec3(0.2125, 0.7154, 0.0721);\n" +
    		"   float i00 = dot(texture2D(sTexture, vTextureCoord).rgb, W);\n" +
    		"   float im1m1 = dot(texture2D(sTexture, vTextureCoord-stpp).rgb, W);\n" +
    		"   float ip1p1 = dot(texture2D(sTexture, vTextureCoord+stpp).rgb, W);\n" +
    		"   float im1p1 = dot(texture2D(sTexture, vTextureCoord-stpm).rgb, W);\n" +
    		"   float ip1m1 = dot(texture2D(sTexture, vTextureCoord+stpm).rgb, W);\n" +
    		"   float im10 = dot(texture2D(sTexture, vTextureCoord-stp0).rgb, W);\n" +
    		"   float ip10 = dot(texture2D(sTexture, vTextureCoord+stp0).rgb, W);\n" +
    		"   float i0m1 = dot(texture2D(sTexture, vTextureCoord-st0p).rgb, W);\n" +
    		"   float i0p1 = dot(texture2D(sTexture, vTextureCoord+st0p).rgb, W);\n" +
    		"   float h = -1.*im1p1 - 2.*i0p1 - 1.*ip1p1 + 1.*im1m1 + 2.*i0m1 + 1.*ip1m1;\n" +
    		"   float v = -1.*im1m1 - 2.*im10 - 1.*im1p1 + 1.*ip1m1 + 2.*ip10 + 1.*ip1p1;\n" +
    		"   float mag = length(vec2(h, v));\n" +
    		//"   vec3 target = vec3(mag, mag, mag);\n" +
    		//"   gl_FragColor = vec4(mix(irgb, target, 1.0),1.);\n" +
			//Julia Calc
			"	vec2 z;\n" +
			"   int iter = int(mag*500.0);\n" +
			//"	if (iter>=juliaIter) iter = juliaIter;\n" +
			"	float scale = 10.0 / float(time);\n" +
			"	z.x = scale * 3.0 * (vTextureCoord.x - 0.5);\n" +
			"	z.y = scale * 2.0 * (vTextureCoord.y - 0.5);\n" +
			"	int i;\n" +
			"   for(i=0; i<iter; i++) {\n" +
			"		float x = (z.x * z.x - z.y * z.y) + juliaC.x;\n" +
			"		float y = (z.y * z.x + z.x * z.y) + juliaC.y;\n" +
			"		if((x * x + y * y) > 4.0) break;\n" +
			"		z.x = x;\n" +
			"		z.y = y;\n" +
			"	}\n" +
			"	\n" +
			"   float hue = 0.0;" +
			"	if (i >= iter) {\n"+
			"		hue = 0.0;\n" +
			"	} else {\n" +
			//"		hue = 1.0;" +
			"		hue = float(i) / float(iter);\n" +	
			"   }\n" +
			"	if (z.x == 0.0 && z.y == 0.0) {\n" +
			"		if (hue == 0.0) {\n" +
			"		}\n" +
			"	}\n" +
			"	gl_FragColor = vec4(0.0,0.0,hue,1.0);\n" +
			"}\n";
	
	public static final String JULIA_FRAGMENT_SHADER = 
			"#extension GL_OES_EGL_image_external : require\n" +
    		"precision mediump float;\n" +      
    		"varying vec2 vTextureCoord;\n" +
    		"varying vec2 calcTexCoord;\n" +
            "uniform sampler2D calcTexture;\n" +
    		"uniform samplerExternalOES sTexture;\n" +
    		//"uniform sampler2d juliaKnot;\n" +
    		"uniform vec2 juliaC;\n" +
			"uniform int juliaIter;\n" +
			"uniform int time;\n" +
			"bool nextZ(int i, inout vec2 z);\n" +
			"bool prevZ(int i, inout vec2 z);\n" +
			"void denormZ(inout vec2 zFloats);\n" +
			"float normZ(float zFloat);\n" +
			"void main() {\n" +
			//Julia Calc
			"	vec2 z;\n" +
			"   int iter = juliaIter;\n" +
			"	float scale = 10.0 / float(time);\n" +
			"	z.x = scale * 3.0 * (vTextureCoord.x - 0.5);\n" +
			"	z.y = scale * 2.0 * (vTextureCoord.y - 0.5);\n" +
			//"	float " +
			"	int i;\n" +
			"	if (scale < 0.5) {\n" +
			"		float mode = float(time)-mod(float(time),10.0);" +
			"		vec4 temp = texture2D(calcTexture, calcTexCoord,float(time-1)*scale/10.0);\n" +
			"		denormZ(temp.xy);\n" +
			"		z = temp.xy;\n" +
			"		int start = int(temp.w*mode+temp.z*float(iter));\n" +
			"		if (nextZ(start,temp.xy)) {\n" +
						//Julia MIIM Algorithm
			"			int finish = max(start - iter,0);\n" +
			"			for(i=start; i>finish; i) {\n" +
			"				if (prevZ(i,z)) break;\n" +
			"			}\n" +
			"		} else {\n" +
			"			for(i=start; i<start+iter; ++i) {\n" +
			"				if (nextZ(i,z)) break;\n" +
			"			}\n" +
			"		}\n" +
			"	} else {\n" +
			"   	for(i=0; i<iter; i++) {\n" +
			"			if (nextZ(i,z)) break;\n" +
			"		}\n" +
			"	}\n" +
			"	\n" +
			"	float hue = mod(float(i),float(iter));\n" +
			"	hue = (float(i)-hue) / float(iter);\n" +	
			"	float next = float(time)+1.0;\n" +
			"	next = (next-mod(next,10.0))/10.0;\n" +
			"	float alpha = ((float(i)-mod(float(i),float(iter)))/float(iter))/next;\n" +
			"	gl_FragColor = vec4(normZ(z.x),normZ(z.y),hue,alpha);\n" +
			"}\n" +
			
			"\n" +
			"float normZ(float zFloat){\n" +
			"	return((zFloat+4.0)/8.0);\n" +
			"}\n" +
			"\n" +
			"void denormZ(inout vec2 zFloats){\n" +
			"	zFloats.x = (zFloats.x*8.0)-4.0;\n" +
			"	zFloats.y = (zFloats.y*8.0)-4.0;\n" +
			"}\n" +
			
			"bool nextZ(int i, inout vec2 z) {\n" +
			"   float x = (z.x * z.x - z.y * z.y) + juliaC.x;\n" +
			"	float y = (z.y * z.x + z.x * z.y) + juliaC.y;\n" +
			"	if((x * x + y * y) > 4.0) return(true);\n" +
			"	z.x = x;\n" +
			"	z.y = y;\n" +
			"	return(false);\n" +
			"}\n" +
			
			"bool prevZ(int i, inout vec2 z) {\n" +
			"   float s = 2.0*mod(float(i),2.0)-1.0;\n 	 " +
			"	float real = z.x-juliaC.x;\n" +
			"	float imag = z.y-juliaC.y;\n" +
			"	float mag = sqrt(real*real + imag*imag);\n" +
			"   float x = s*sqrt((mag + real)/2.0);\n" +
			"	float y = s*sign(imag)*sqrt((mag-real)/2.0);\n" +
			"	if((x * x + y * y) < 4.0) return(true);\n" +
			"	z.x = x;\n" +
			"	z.y = y;\n" +
			"	return(false);\n" +
			"}\n";
	
	
    public static final String FRAGMENT_SHADER2 =
            "precision mediump float;\n" +      
            "varying vec2 vTextureCoord;\n" +
            "uniform sampler2D displayTexture;\n" +
            "void main() {\n" +
            "  vec4 color = texture2D(displayTexture, vTextureCoord);\n" +
            "  gl_FragColor = vec4(color.r,color.g,color.b,1.0);\n" +
            
            "}\n";
	
    public static final String SORBEL_FRAGMENT_SHADER =
    		"#extension GL_OES_EGL_image_external : require\n" +
    		"precision mediump float;\n" +
    		"uniform samplerExternalOES sTexture;\n" +
    		"varying vec2 vTextureCoord;\n" +
    		"void main() {\n" +
    		"	vec3 irgb = texture2D(sTexture, vTextureCoord).rgb;\n" +
    		"	float ResS = 720.;\n" +
    		"	float ResT = 720.;\n" +
    		"	vec2 stp0 = vec2(1./ResS, 0.);\n" +
    		"	vec2 st0p = vec2(0., 1./ResT);\n" +
    		"   vec2 stpp = vec2(1./ResS, 1./ResT);\n" +
    		"   vec2 stpm = vec2(1./ResS, -1./ResT);\n" +
    		"   const vec3 W = vec3(0.2125, 0.7154, 0.0721);\n" +
    		"   float i00 = dot(texture2D(sTexture, vTextureCoord).rgb, W);\n" +
    		"   float im1m1 = dot(texture2D(sTexture, vTextureCoord-stpp).rgb, W);\n" +
    		"   float ip1p1 = dot(texture2D(sTexture, vTextureCoord+stpp).rgb, W);\n" +
    		"   float im1p1 = dot(texture2D(sTexture, vTextureCoord-stpm).rgb, W);\n" +
    		"   float ip1m1 = dot(texture2D(sTexture, vTextureCoord+stpm).rgb, W);\n" +
    		"   float im10 = dot(texture2D(sTexture, vTextureCoord-stp0).rgb, W);\n" +
    		"   float ip10 = dot(texture2D(sTexture, vTextureCoord+stp0).rgb, W);\n" +
    		"   float i0m1 = dot(texture2D(sTexture, vTextureCoord-st0p).rgb, W);\n" +
    		"   float i0p1 = dot(texture2D(sTexture, vTextureCoord+st0p).rgb, W);\n" +
    		"   float h = -1.*im1p1 - 2.*i0p1 - 1.*ip1p1 + 1.*im1m1 + 2.*i0m1 + 1.*ip1m1;\n" +
    		"   float v = -1.*im1m1 - 2.*im10 - 1.*im1p1 + 1.*ip1m1 + 2.*ip10 + 1.*ip1p1;\n" +
    		"   float mag = length(vec2(h, v));\n" +
    		"   vec3 target = vec3(mag, mag, mag);\n" +
    		"   gl_FragColor = vec4(mix(irgb, target, 1.0),1.);\n" +
    		"}";

    //private float[] mMVPMatrix = new float[16];
    private float[] mSTMatrix = new float[16];

    private int mProgram;
    private int mTextureID = -12345;
    private int muMVPMatrixHandle;
    private int muSTMatrixHandle;
    private int maPositionHandle;
    private int maTextureHandle;
	private float[] mProjMatrix = new float[16];
	private OuterCircle mOuterCircle;
	int width;
	int height;
	float mRatio;
	private final static String TAG = "com.hpp.STextureRender";
	private JuliaDream jd;
	//private MyGLSurfaceView mGLView;

    public STextureRender(String fs) {
    	fragmentShader = fs;
        mTriangleVertices = ByteBuffer.allocateDirect(
                mTriangleVerticesData.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangleVertices.put(mTriangleVerticesData).position(0);
        //mGLView = glView;
        Matrix.setIdentityM(mSTMatrix, 0);
        jd = new JuliaDream().setRenderer(this);
        jd.mTriangleVertices = ByteBuffer.allocateDirect(
                mTriangleVerticesData.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    public int getTextureId() {
        return mTextureID;
    }

    public void drawFrame(SurfaceTexture st, float[] mMVPMatrix, float[] mProjMatrix) {
        checkGlError("onDrawFrame start");
        st.getTransformMatrix(mSTMatrix);
            
        if (jd.checkActive()){
        	jd.bindFrameBuffer();
        } 

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
            
        GLES20.glUseProgram(mProgram);
        checkGlError("glUseProgram");
        
        if (jd.checkActive()){
        	jd.setupDrawFrameBuffer(mProgram);
        }
        /*
     // Set the camera position (View matrix)
        Matrix.setLookAtM(mSTMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mSTMatrix, 0);
		//*/
        
        
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);

        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
        checkGlError("glVertexAttribPointer maPosition");
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        checkGlError("glEnableVertexAttribArray maPositionHandle");

        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
        checkGlError("glVertexAttribPointer maTextureHandle");
        GLES20.glEnableVertexAttribArray(maTextureHandle);
        checkGlError("glEnableVertexAttribArray maTextureHandle");

        if (jd.mJuliaIter != -1) {
        	GLES20.glUniform1i(jd.mJuliaIter, jd.JULIA_NUM_ITERS);
            checkGlError("glUniform1i mJuliaIter");
        } 
        
        if (jd.mJuliaC != -1) {
        	GLES20.glUniform2f(jd.mJuliaC, jd.JULIA_COMPLEX_VALUES[0], jd.JULIA_COMPLEX_VALUES[1]);
            checkGlError("glUniform1i mJuliaC");
        } 
        
        if (jd.mJuliaTime != -1) {
        	GLES20.glUniform1i(jd.mJuliaTime, jd.mTime++);
            checkGlError("glUniform1i mJuliaC");
        }
        
        //Matrix.setIdentityM(mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        checkGlError("glDrawArrays");
        
        if (jd.checkActive()){
        	jd.DrawDisplayFrame(mMVPMatrix, mSTMatrix);
        }
        
        
        //mOuterCircle.draw(mMVPMatrix, 1);
        //draw frame ontop of camera
        //mGLView.requestRender();
        
        
        //GLES20.glFinish();
        //GLES20.glFlush();
        return;
    }
    
    

    private ByteBuffer getPixelBuffer() {
    	ByteBuffer pixelBuffer = ByteBuffer.allocateDirect(height*width*2);
        IntBuffer frameBuffers = IntBuffer.allocate(1);
        GLES20.glGenFramebuffers(1,frameBuffers);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers.get());
        int frameBufferStatus = GLES20.GL_FRAMEBUFFER_COMPLETE;
        GLES20.glCheckFramebufferStatus(frameBufferStatus);
        if ((frameBufferStatus & GLES20.GL_FRAMEBUFFER_COMPLETE) != 0) {
        	GLES20.glReadPixels(0 /*x*/, 0/*y*/, 
        			width /*width*/, height /*height*/, 
        			GLES20.GL_RGB /*format*/, 
        			GLES20. GL_UNSIGNED_SHORT_5_6_5 /*type*/, 
        			pixelBuffer /*pixels*/);
        }
		return pixelBuffer;
	}

    String fragmentShader;

	/**
     * Initializes GL state.  Call this after the EGL surface has been created and made current.
     * @param mNote 
     */
    public void surfaceCreated() {
        mProgram = createProgram(VERTEX_SHADER, fragmentShader);
        if (mProgram == 0) {
            throw new RuntimeException("failed creating program");
        }
        
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        checkGlError("glGetAttribLocation aPosition");
        if (maPositionHandle == -1) {
            Log.d(TAG,"Could not get attrib location for aPosition");
        }
        maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        checkGlError("glGetAttribLocation aTextureCoord");
        if (maTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }
         
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        checkGlError("glGetUniformLocation uMVPMatrix");
        if (muMVPMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }

        muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
        checkGlError("glGetUniformLocation uSTMatrix");
        if (muSTMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uSTMatrix");
        }

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        mTextureID = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
        checkGlError("glBindTexture mTextureID");

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        checkGlError("glTexParameter");
        
    	jd.setJuliaShaderAttribs(mProgram);
    	
        if (jd.checkActive(fragmentShader)){
        	jd.setupFrameBuffers();
            jd.setupJuliaDisplayProgram();
        }
    }
    
    
    public void surafaceChanged(int w, int h){
    	float ratio = (float) w / (float) h;  
    	if(mRatio==ratio)return;
    	width = w;
    	height = h;
    	mRatio = ratio;
    		
        // Adjust the viewport based on geometry changes,
    	if (ratio>1) { //Landscape
        	float[] landscapeVerts = {
                    // X, Y, Z, U, V
                    -ratio, -1.0f, z_0, 1.0f, 0.0f,
                    ratio, -1.0f, z_0, 0.0f, 0.0f,
                    -ratio,  1.0f, z_0, 1.0f, 1.0f,
                    ratio,  1.0f, z_0, 0.0f, 1.0f,
            };
        	mTriangleVertices.put(landscapeVerts).position(0);
    	} else { //Portrait
    		float[] portraitVerts = {
                    // X, Y, Z, U, V
                    -ratio, -1.0f, z_0, 1.0f, 1.0f,
                    ratio, -1.0f, z_0, 1.0f, 0.0f,
                    -ratio,  1.0f, z_0, 0.0f, 1.0f,
                    ratio,  1.0f, z_0, 0.0f, 0.0f,
            };
    		mTriangleVertices.put(portraitVerts).position(0);
    	}
    	 

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
    }

    
    /**
     * Replaces the fragment shader.  Pass in null to reset to default.
     */
    public void changeFragmentShader(String fragmentShader) {
    	jd.mTime = 0;
        if (fragmentShader == null) {
            fragmentShader = FRAGMENT_SHADER;
        }
        GLES20.glDeleteProgram(mProgram);
        mProgram = createProgram(VERTEX_SHADER, fragmentShader);
        if (mProgram == 0) {
            Log.d(TAG,"failed creating program");
        }
        if (jd.checkActive(fragmentShader)){
        	Log.d(TAG,"SETUP JULIA DREAM FOR RENDER");
        	jd.setJuliaShaderAttribs(mProgram);
        	jd.setupFrameBuffers();
        	jd.clearDisplayProgram();
        	jd.setupJuliaDisplayProgram();
        	jd.startFragmentShader();
        } else {
        	jd.clearJuliaShaderAttribs();
        }
    }

    private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        checkGlError("glCreateShader type=" + shaderType);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG , "Could not compile shader " + shaderType + ":");
            Log.e(TAG, " " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        checkGlError("glCreateProgram");
        if (program == 0) {
            Log.e(TAG, "Could not create program");
        }
        GLES20.glAttachShader(program, vertexShader);
        checkGlError("glAttachShader");
        GLES20.glAttachShader(program, pixelShader);
        checkGlError("glAttachShader");
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Could not link program: ");
            Log.e(TAG, GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }

    public void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            //Log.d(TAG,op + ": glError " + error);
        }
    }

	public void setView() {
		GLES20.glViewport(0, 0	, width, height);
	}
}