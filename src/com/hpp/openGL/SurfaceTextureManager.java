package com.hpp.openGL;


import java.nio.ByteBuffer;

import com.harmonicprocesses.penelopefree.openGL.MyGLSurfaceView;

import android.graphics.SurfaceTexture;
import android.hardware.Camera.Size;
import android.util.Log;

/**
 * Manages a SurfaceTexture.  Creates SurfaceTexture and TextureRender objects, and provides
 * functions that wait for frames and render them to the current EGL surface.
 * <p>
 * The SurfaceTexture can be passed to Camera.setPreviewTexture() to receive camera output.
 */
public class SurfaceTextureManager
        implements SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "com.hpp.SurfaceTextureManager";
	private SurfaceTexture mSurfaceTexture;
    private STextureRender mTextureRender;

    private Object mFrameSyncObject = new Object();     // guards mFrameAvailable
    private boolean mFrameAvailable;

    /**
     * Creates instances of TextureRender and SurfaceTexture.
     * @param size 
     */
    public SurfaceTextureManager(Size size) {
        mTextureRender = new STextureRender();
        mTextureRender.surfaceCreated();
        mTextureRender.surafaceChanged(size.width, size.height);

        Log.d(TAG, "textureID=" + mTextureRender.getTextureId());
        mSurfaceTexture = new SurfaceTexture(mTextureRender.getTextureId());

        // This doesn't work if this object is created on the thread that CTS started for
        // these test cases.
        //
        // The CTS-created thread has a Looper, and the SurfaceTexture constructor will
        // create a Handler that uses it.  The "frame available" message is delivered
        // there, but since we're not a Looper-based thread we'll never see it.  For
        // this to do anything useful, OutputSurface must be created on a thread without
        // a Looper, so that SurfaceTexture uses the main application Looper instead.
        //
        // Java language note: passing "this" out of a constructor is generally unwise,
        // but we should be able to get away with it here.
        mSurfaceTexture.setOnFrameAvailableListener(this);
    }

    public void release() {
        // this causes a bunch of warnings that appear harmless but might confuse someone:
        //  W BufferQueue: [unnamed-3997-2] cancelBuffer: BufferQueue has been abandoned!
        //mSurfaceTexture.release();

        mTextureRender = null;
        mSurfaceTexture = null;
    }

    /**
     * Returns the SurfaceTexture.
     */
    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    /**
     * Replaces the fragment shader.
     */
    public void changeFragmentShader(String fragmentShader) {
        mTextureRender.changeFragmentShader(fragmentShader);
    }

    /**
     * Latches the next buffer into the texture.  Must be called from the thread that created
     * the OutputSurface object.
     */
    public void awaitNewImage() {
        final int TIMEOUT_MS = 2500; //2500; //try reducing to just shy of a frame 

        synchronized (mFrameSyncObject) {
            while (!mFrameAvailable) {
                try {
                    // Wait for onFrameAvailable() to signal us.  Use a timeout to avoid
                    // stalling the test if it doesn't arrive.
                    mFrameSyncObject.wait(TIMEOUT_MS);
                    if (!mFrameAvailable) {
                        // TODO: if "spurious wakeup", continue while loop
                        throw new RuntimeException("Camera frame wait timed out" + System.currentTimeMillis());
                    }
                } catch (InterruptedException ie) {
                    // shouldn't happen
                    throw new RuntimeException(ie);
                }
            }
            mFrameAvailable = false;
        }

     // Latch the data.
        //mTextureRender.checkGlError("before updateTexImage");
        //mSurfaceTexture.updateTexImage();
    }

    /**
     * Draws the data from SurfaceTexture onto the current EGL surface.
     */
    public ByteBuffer drawImage() {
    	// Latch the data.
        mTextureRender.checkGlError("before updateTexImage");
        mSurfaceTexture.updateTexImage();
        
        return mTextureRender.drawFrame(mSurfaceTexture);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture st) {
        //Log.d(TAG, "new frame available");
        synchronized (mFrameSyncObject) {
            if (mFrameAvailable) {
                Log.d(TAG,"mFrameAvailable already set, frame could be dropped");
            }
            mFrameAvailable = true;
            mFrameSyncObject.notifyAll();
        }
    }
}