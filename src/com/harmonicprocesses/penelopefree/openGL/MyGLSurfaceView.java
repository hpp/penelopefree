package com.harmonicprocesses.penelopefree.openGL;


import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MyGLSurfaceView extends GLSurfaceView {

    public final MyGLRenderer mRenderer;
    
    Context mContext;
    
    private int[] NoteSpectrum;
	public float maxAmplitude = 0;
	public int maxAmpIdx = 0;
	private boolean fingerDown = false;
	private SharedPreferences mSharedPrefs;
    
    public TextView note_display;

    public MyGLSurfaceView(Context context,int[] noteSpectrum) {
        super(context);

        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);
        // Set the Renderer for drawing on the GLSurfaceView
        mRenderer = new MyGLRenderer(context);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        setRenderer(mRenderer);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);

        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        
        NoteSpectrum = noteSpectrum;
        
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

	public MyGLSurfaceView getMyGLSurfaceView(Context context){
    	return this;
    }
	
	public MyGLRenderer getRenderer(){
		return mRenderer;
	}
    

    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private float mPreviousX;
    private float mPreviousY;
    
    public View.OnTouchListener listenForTouch = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent e) {
			// MotionEvent reports input details from the touch screen
	        // and other input controls. In this case, you are only
	        // interested in events where the touch position changed.
	    	if (e.getAction()==MotionEvent.ACTION_DOWN){
	    		fingerDown = true;
	    	} else if (e.getAction()==MotionEvent.ACTION_UP){
	    		fingerDown = false;
	    	}
	    	
	    	
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
	        //return true;
			return true;
		}


    	
    };

	private void sendRequestRender() {
		if (mSharedPrefs.getBoolean("turn_on_visualization_key",true)){
    		requestRender();
    	}
	}
    
    public boolean updateAmplitudes(float[] amplitudes) throws Exception{
    	maxAmplitude = mRenderer.mAmplitude[maxAmpIdx];
    	mRenderer.mNoteAmp = maxAmplitude;
    	float minAmplitude = 0.001f;
    	mRenderer.mAmplitude = amplitudes;
    	
    	
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
    	return true;
    }
}
