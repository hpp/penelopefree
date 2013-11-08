package com.hpp.openGL;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;
import android.view.Surface.OutOfResourcesException;
import android.view.SurfaceView;

public class BitmapDecoder {
	private static final String TAG = "io.hpp.BitmapDecoder";
	
	
	public static void postBitmaptoSurface(Bitmap bitmap, Surface surface){
		Canvas c;
		try {
			c = surface.lockCanvas(null);
			c.drawBitmap(bitmap, new Matrix(), null);
			surface.unlockCanvasAndPost(c);
		} catch (IllegalArgumentException e) {
			Log.d(TAG,"failed to lock Canvas IllegalArgumentException = " + e.getMessage());
			e.printStackTrace();
		} catch (OutOfResourcesException e) {
			Log.d(TAG,"failed to lock Canvas OutOfResourcesException = " + e.getMessage());
			e.printStackTrace();
		}
		
	}


	 
}
