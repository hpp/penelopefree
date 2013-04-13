package com.harmonicprocesses.penelopefree.camera;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.harmonicprocesses.penelopefree.R;
import com.harmonicprocesses.penelopefree.settings.UpSaleDialog;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

public class Pcamera {
	private static final String TAG = "com.harmonicprocesses.penelopefree.camera.Pcamera";
	Context mContext;
	Camera mCamera;
	private CameraPreview mCameraPreview;
	private boolean isRecording = false;
    MediaRecorder mMediaRecorder;
    Button captureButton;
    static int mCamId;
    private FragmentManager mFrag;
	
	public Pcamera(Activity context, Button recordButton) {
		mContext = context;
		mFrag = context.getFragmentManager();
		captureButton = recordButton;
		captureButton.setOnClickListener(RecordButtonListener);
				
		if (!checkCameraHardware()) {
			//TODO what if no camera
		}
		
		mCamera = getCameraInstance();

		mCameraPreview = new CameraPreview(mContext, mCamera);
		//prepareVideoRecorder();
	}
	
	public void start(ViewGroup vg){
		if (mCamera==null) {
			mCamera = getCameraInstance();
		}
		if (mCameraPreview==null){
			mCameraPreview = new CameraPreview(mContext, mCamera);
		}
		//if (mMediaRecorder==null) {
		//	prepareVideoRecorder();
		//}
		vg.addView(mCameraPreview);
	}
	
	public void stop(FrameLayout vg) {
		vg.removeView(mCameraPreview);
		releaseMediaRecorder();
		releaseCamera();
		mCameraPreview = null;
	}
	
	private boolean checkCameraHardware() {
	    if (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
	        // this device has a camera
	        return true;
	    } else {
	        // no camera on this device
	        return false;
	    }
	}
	
	public static Camera getCameraInstance(){
	    Camera c = null;
	    mCamId = 0;
		if (Camera.getNumberOfCameras()>1){
			mCamId = 1; //this should be the face cam
		} 
		
	    try {
	        c = Camera.open(mCamId); // attempt to get a Camera instance
	    }
	    catch (Exception e){
	        // Camera is not available (in use or does not exist)
	    	Log.d(TAG,"Camera failed to open");
	    }
	    return c; // returns null if camera is unavailable
	}

	private boolean prepareVideoRecorder(){
		mMediaRecorder = new MediaRecorder();
		
	    // Step 1: Unlock and set camera to MediaRecorder
	    mCamera.unlock();
	    mMediaRecorder.setCamera(mCamera);

	    // Step 2: Set sources
	    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
	    mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

	    // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
	    mMediaRecorder.setProfile(CamcorderProfile.get(mCamId,CamcorderProfile.QUALITY_HIGH));

	    // Step 4: Set output file
	    mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());

	    // Step 5: Set the preview output
	    mMediaRecorder.setPreviewDisplay(mCameraPreview.getHolder().getSurface());

	    // Step 6: Prepare configured MediaRecorder
	    try {
	        mMediaRecorder.prepare();
	    } catch (IllegalStateException e) {
	        Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
	        releaseMediaRecorder();
	        return false;
	    } catch (IOException e) {
	        Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
	        releaseMediaRecorder();
	        return false;
	    }
	    return true;
	}

	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;

	/** Create a file Uri for saving an image or video */
	private static Uri getOutputMediaFileUri(int type){
	      return Uri.fromFile(getOutputMediaFile(type));
	}

	/** Create a File for saving an image or video */
	private static File getOutputMediaFile(int type){
	    // To be safe, you should check that the SDCard is mounted
	    // using Environment.getExternalStorageState() before doing this.

	    File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
	              Environment.DIRECTORY_PICTURES), "MyCameraApp");
	    // This location works best if you want the created images to be shared
	    // between applications and persist after your app has been uninstalled.

	    // Create the storage directory if it does not exist
	    if (! mediaStorageDir.exists()){
	        if (! mediaStorageDir.mkdirs()){
	            Log.d("MyCameraApp", "failed to create directory");
	            return null;
	        }
	    }

	    // Create a media file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    File mediaFile;
	    if (type == MEDIA_TYPE_IMAGE){
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "IMG_"+ timeStamp + ".jpg");
	    } else if(type == MEDIA_TYPE_VIDEO) {
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "VID_"+ timeStamp + ".mp4");
	    } else {
	        return null;
	    }

	    return mediaFile;
	}
	
    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }
    
    

 // Add a listener to the Capture button
    OnClickListener RecordButtonListener = new OnClickListener() {
         @Override
         public void onClick(View v) {
             if (isRecording) {
                 // stop recording and release camera
            	 try { 
            	 	mMediaRecorder.stop();  // stop the recording
            	 } catch(Exception e) {
        			 String msg = e.getMessage();
        			 if (msg==null) msg = "Exception Stopping Recording, unhandled exception.";
        			 Log.d(TAG,msg);
            	 } finally {
            		 releaseMediaRecorder(); // release the MediaRecorder object
            		 mCamera.lock();         // take camera access back from MediaRecorder
            	 }
            
                 // inform the user that recording has stopped
                 captureButton.setText("Record");
                 isRecording = false;
             } else {
            	 // initialize video camera
            	 new UpSaleDialog(R.string.dialog_penelope_full_messsage_capture)
            	 				.show(mFrag,"PaidForVersionDialog");
 					
            	 if (prepareVideoRecorder()) {
                     // Camera is available and unlocked, MediaRecorder is prepared,
                     // now you can start recording
            		 try {
            			 mMediaRecorder.start();
            		 } catch(Exception e) {
            			 String msg = e.getMessage();
            			 if (msg==null) msg = "Can't Start Recording, unhandled exception.";
            			 Log.d(TAG,msg);
            		 }

                     // inform the user that recording has started
                     captureButton.setText("Stop");
                     isRecording = true;
                 } else {
                     // prepare didn't work, release the camera
                     releaseMediaRecorder();
                     // inform user
                 }
             }
         }

			
		};    
	
}

