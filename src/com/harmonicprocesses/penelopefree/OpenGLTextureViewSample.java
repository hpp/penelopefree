package com.harmonicprocesses.penelopefree;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

//import javax.microedition.khronos.egl.EGL10;
//import javax.microedition.khronos.opengles.GL;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.GLUtils;
//import android.opengl.GLES20.*;
//import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.Log;
import android.view.TextureView;
/*GLSurfaceView handles GL setup for you, which TextureView will not do. A
TextureView can be used as the native window when you create an EGL
surface. Here is an example (the interesting part is the call
to eglCreateWindowSurface()):

http://grokbase.com/t/gg/android-developers/11bqmgb7sw/how-to-replace-glsurfaceview-with-textureview-in-android-ice-cream-sandwich

Create an OpenGL context
Generate an OpenGL texture name
Create a SurfaceTexture with the texture name
Pass the SurfaceTexture to Camera
Listen for updates
On SurfaceTexture update, draw the texture with OpenGL using the shader you want
*/

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class OpenGLTextureViewSample extends TextureView implements TextureView.SurfaceTextureListener{
	
	private Context mContext;
	private RenderThread mRenderThread; 
	
	public OpenGLTextureViewSample(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		mContext = context;
	}

	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int
	width, int height) {
		mRenderThread = new RenderThread(mContext.getResources(), surface);
		mRenderThread.start();
	}

	@TargetApi(17)
	private static class RenderThread extends Thread {
		private static final String LOG_TAG = "GLTextureView";
	
		static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
		static final int EGL_OPENGL_ES2_BIT = 4;
	
		private volatile boolean mFinished;
	
	
		private final Resources mResources;
		private final SurfaceTexture mSurface;
	
		//private EGL14 mEgl;
		private EGLDisplay mEglDisplay;
		private EGLConfig mEglConfig;
		private EGLContext mEglContext;
		private EGLSurface mEglSurface;
		//private GL mGL;
	
		RenderThread(Resources resources, SurfaceTexture surface) {
			mResources = resources;
			mSurface = surface;
		}

	private static final String sSimpleVS =
		"attribute vec4 position;\n" +
		"attribute vec2 texCoords;\n" +
		"varying vec2 outTexCoords;\n" +
		"\nvoid main(void) {\n" +
		" outTexCoords = texCoords;\n" +
		" gl_Position = position;\n" +
		"}\n\n";
	private static final String sSimpleFS =
		"precision mediump float;\n\n" +
		"varying vec2 outTexCoords;\n" +
		"uniform sampler2D texture;\n" +
		"\nvoid main(void) {\n" +
		" gl_FragColor = texture2D(texture, outTexCoords);\n" +
		"}\n\n";
		
	private static final int FLOAT_SIZE_BYTES = 4;
	private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 *
			FLOAT_SIZE_BYTES;
	private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
	private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
	private final float[] mTriangleVerticesData = {
		// X, Y, Z, U, V
		-1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
		1.0f, -1.0f, 0.0f, 1.0f, 0.0f,
		-1.0f, 1.0f, 0.0f, 0.0f, 1.0f,
		1.0f, 1.0f, 0.0f, 1.0f, 1.0f,
		};

	@Override
	public void run() {
		initGL();
	
		FloatBuffer triangleVertices =
		ByteBuffer.allocateDirect(mTriangleVerticesData.length
		*
		FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
		triangleVertices.put(mTriangleVerticesData).position(0);
		
		int texture = loadTexture(R.drawable.note0);
		int program = buildProgram(sSimpleVS, sSimpleFS);
		
		int attribPosition = GLES20.glGetAttribLocation(program, "position");
		checkGlError();
		
		int attribTexCoords = GLES20.glGetAttribLocation(program, "texCoords");
		checkGlError();
		
		int uniformTexture = GLES20.glGetUniformLocation(program, "texture");
		checkGlError();
		
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
		checkGlError();
		
		GLES20.glUseProgram(program);
		checkGlError();
		
		GLES20.glEnableVertexAttribArray(attribPosition);
		checkGlError();
		
		GLES20.glEnableVertexAttribArray(attribTexCoords);
		checkGlError();
		
		GLES20.glUniform1i(uniformTexture, texture);
		checkGlError();
		
		while (!mFinished) {
		checkCurrent();
		
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		checkGlError();
		
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		checkGlError();
		
		// drawQuad
		
		triangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
		GLES20.glVertexAttribPointer(attribPosition, 3, GLES20.GL_FLOAT, false,
		TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
		triangleVertices);
		
		triangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
		GLES20.glVertexAttribPointer(attribTexCoords, 3, GLES20.GL_FLOAT, false,
		TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
		triangleVertices);
		
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		
		if (!EGL14.eglSwapBuffers(mEglDisplay, mEglSurface)) {
			throw new RuntimeException("Cannot swap buffers");
		}
		checkEglError();
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		// Ignore
		}
	}

	finishGL();
	}

	private int loadTexture(int resource) {
		int[] textures = new int[1];
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glGenTextures(1, textures, 0);
		checkGlError();
		
		int texture = textures[0];
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
		checkGlError();
		
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
				GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
				GLES20.GL_LINEAR);
		
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
				GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
				GLES20.GL_CLAMP_TO_EDGE);
		
		Bitmap bitmap = BitmapFactory.decodeResource(mResources,
				resource);
		
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmap,
				GLES20.GL_UNSIGNED_BYTE, 0);
		checkGlError();
		
		bitmap.recycle();
	
		return texture;
	}

	private int buildProgram(String vertex, String fragment) {
		int vertexShader = buildShader(vertex, GLES20.GL_VERTEX_SHADER);
		if (vertexShader == 0) return 0;
	
		int fragmentShader = buildShader(fragment, GLES20.GL_FRAGMENT_SHADER);
		if (fragmentShader == 0) return 0;
	
		int program = GLES20.glCreateProgram();
		GLES20.glAttachShader(program, vertexShader);
		checkGlError();
	
		GLES20.glAttachShader(program, fragmentShader);
		checkGlError();
		
		GLES20.glLinkProgram(program);
		checkGlError();
	
		int[] status = new int[1];
		GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
		if (status[0] != GLES20.GL_TRUE) {
			String error = GLES20.glGetProgramInfoLog(program);
			Log.d(LOG_TAG, "Error while linking program:\n" + error);
			GLES20.glDeleteShader(vertexShader);
			GLES20.glDeleteShader(fragmentShader);
			GLES20.glDeleteProgram(program);
			return 0;
		}

	return program;
	}

	private int buildShader(String source, int type) {
		int shader = GLES20.glCreateShader(type);
		
		GLES20.glShaderSource(shader, source);
		checkGlError();
		
		GLES20.glCompileShader(shader);
		checkGlError();
		
		int[] status = new int[1];
		GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
		if (status[0] != GLES20.GL_TRUE) {
		String error = GLES20.glGetShaderInfoLog(shader);
		Log.d(LOG_TAG, "Error while compiling shader:\n" + error);
		GLES20.glDeleteShader(shader);
		return 0;
	}

	return shader;
	}

	private void checkEglError() {
		int error = EGL14.eglGetError();
		if (error != EGL14.EGL_SUCCESS) {
			Log.w(LOG_TAG, "EGL error = 0x" +
			Integer.toHexString(error));
		}
	}

	private void checkGlError() {
		int error = GLES20.glGetError();
		if (error != GLES20.GL_NO_ERROR) {
			Log.w(LOG_TAG, "GL error = 0x" +
			Integer.toHexString(error));
		}
	}

	private void finishGL() {
		EGL14.eglDestroyContext(mEglDisplay, mEglContext);
		EGL14.eglDestroySurface(mEglDisplay, mEglSurface);
	}

	private void checkCurrent() {
		if (!mEglContext.equals(EGL14.eglGetCurrentContext()) ||
			!mEglSurface.equals(EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW))) {
			if (!EGL14.eglMakeCurrent(mEglDisplay, mEglSurface,
				mEglSurface, mEglContext)) {
				throw new RuntimeException("eglMakeCurrent failed "
					+
					GLUtils.getEGLErrorString(EGL14.eglGetError()));
			}
		}
	}

	private void initGL() {
		//mEgl = (EGL14) EGLContext.getEGL();
		int[] attrib_list = { EGL_CONTEXT_CLIENT_VERSION, 2,
				EGL14.EGL_NONE };
		
		mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
		if (mEglDisplay == EGL14.EGL_NO_DISPLAY) {
			throw new RuntimeException("eglGetDisplay failed "
					+ GLUtils.getEGLErrorString(EGL14.eglGetError()));
		}
		
		int[] version = new int[2];
		if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
			throw new RuntimeException("eglInitialize failed " +
					GLUtils.getEGLErrorString(EGL14.eglGetError()));
		}
		
		mEglConfig = chooseEglConfig(attrib_list);
		if (mEglConfig == null) {
			throw new RuntimeException("eglConfig not initialized");
		}
		
		mEglContext = createContext(mEglDisplay, mEglConfig);
		
		
		mEglSurface = EGL14.eglCreateWindowSurface(mEglDisplay,
		mEglConfig, mSurface, attrib_list, 0);
		
		if (mEglSurface == null || mEglSurface == EGL14.EGL_NO_SURFACE)
		{
			int error = EGL14.eglGetError();
			if (error == EGL14.EGL_BAD_NATIVE_WINDOW) {
				Log.e(LOG_TAG, "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
				return;
			}
			throw new RuntimeException("createWindowSurface failed "
					+ GLUtils.getEGLErrorString(error));
			}
		
			if (!EGL14.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface,
					mEglContext)) {
				throw new RuntimeException("eglMakeCurrent failed "
						+ GLUtils.getEGLErrorString(EGL14.eglGetError()));
			}
		
			//mGL = mEglContext.getGL();
		}


		EGLContext createContext(EGLDisplay eglDisplay,
		EGLConfig eglConfig) {
			int[] attrib_list = { EGL_CONTEXT_CLIENT_VERSION, 2,
					EGL14.EGL_NONE };
			return EGL14.eglCreateContext(eglDisplay, eglConfig,
					EGL14.EGL_NO_CONTEXT, attrib_list, 0);
		}

	private EGLConfig chooseEglConfig(int[] attrib_list) {
		int[] configsCount = new int[1];
		EGLConfig[] configs = new EGLConfig[1];
		int[] configSpec = getConfig();
		if (!EGL14.eglChooseConfig(mEglDisplay, attrib_list, 0, configs, 0, 1, configsCount, 0)) {
			throw new IllegalArgumentException("eglChooseConfig failed" +
					GLUtils.getEGLErrorString(EGL14.eglGetError()));
		} else if (configsCount[0] > 0) {
			return configs[0];
		}
	return null;
	}

	private int[] getConfig() {
		return new int[] {
			EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
			EGL14.EGL_RED_SIZE, 8,
			EGL14.EGL_GREEN_SIZE, 8,
			EGL14.EGL_BLUE_SIZE, 8,
			EGL14.EGL_ALPHA_SIZE, 8,
			EGL14.EGL_DEPTH_SIZE, 0,
			EGL14.EGL_STENCIL_SIZE, 0,
			EGL14.EGL_NONE
		};
	}

	void finish() {
		mFinished = true;
		}
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
			int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
		// TODO Auto-generated method stub
		
	}
}
