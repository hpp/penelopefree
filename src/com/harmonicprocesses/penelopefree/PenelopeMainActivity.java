package com.harmonicprocesses.penelopefree;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;

import com.harmonicprocesses.penelopefree.audio.AudioOnAir;
import com.harmonicprocesses.penelopefree.audio.OnAir;
import com.harmonicprocesses.penelopefree.camera.Pcamera;
import com.harmonicprocesses.penelopefree.openGL.MyGLSurfaceView;
import com.harmonicprocesses.penelopefree.settings.HelpActivity;
import com.harmonicprocesses.penelopefree.settings.SettingsFragment;
import com.harmonicprocesses.penelopefree.settings.SettingsActivity;
import com.harmonicprocesses.penelopefree.settings.UpSaleDialog;
//import com.harmonicprocesses.penelopefree.settings.SubSettingsFragment;
import com.harmonicprocesses.penelopefree.usbAudio.UsbAudioManager;
import com.harmonicprocesses.penelopefree.util.SystemUiHider;
import com.harmonicprocesses.penelopefree.util.SystemUiHiderBase;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.OnMenuVisibilityListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Camera;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.TextureView;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class PenelopeMainActivity extends Activity implements TextureView.SurfaceTextureListener {
	
	
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static boolean AUTO_HIDE = true;
	
	
	
	public final String TAG = "com.harmonicprocesses.penelopefree";

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will hide the system UI visibility upon interaction.
	 */
	private static boolean TOGGLE_ON_CLICK = true;
	
	private static boolean RECORD_MODE = false;
	/**
	 * If set, will toggle the OnAir/OffAir background.,
	 * will also turn on and off main play back functionality.
	 */
	private static boolean TOGGLE_ONAIR_CLICK = false;


	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHiderBase.FLAG_HIDE_NAVIGATION;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;
	
	/**
	 * The instance of the {@link AudioOnAir} for this activity.
	 */
	private AudioOnAir mAudioOnAir;
	
	
	/**
	 * The instance of the {@link GLSurfaceView} for this activity.
	 */
	public MyGLSurfaceView mGLView;
	
	/**
	 * The instance of the {@link OpenGLTextureViewSample} for this activity.
	 */
	public OpenGLTextureViewSample mGLTextureView;
	
	/**
	 * The instance of the {@link Camera} for this activity.
	 */
	private Camera mCamera;
	
	/**
	 * The instance of the activity bar menu
	 */
	private Menu mMenu;

	/**
	 * The instance of the activity bar options MenuItem
	 */
	private MenuItem mMenuItem;
	
	/**
	 * The instance of the Settings Menu
	 */
	private Menu mSettingsMenu;
	
	/**
	 * The instance of the items in the Settings Menu
	 */
	private MenuItem[] mSettingsMenuItems;
		
	private SettingsFragment mSettingsFragment;
	
	/**
	 * The instance of the {@link UsbManager} for this activity.
	 */
	public UsbManager mUsbManager;
	
	
	private PendingIntent mPermissionIntent;
	
	/**
	 * The instance of the {@link UsbManager} for this activity.
	 */
	private static final String ACTION_USB_PERMISSION =
		    "com.harmonicprocesses.penelopefree.USB_PERMISSION";



	public static final String EXTRA_MESSAGE = "com.harmonicprocesses.penelopefree.SETTINGS_MESSAGE";
		
	/**
	 * The instance of the renderscript particleFilter used in MyGLRenderer
	 */
	
	/**
	 * The instance of the {@link FrameLayout} for this activity
	 */
	private FrameLayout mFragmentViewGroup;
	
	/**
	 * The instance of the {@link UsbAudioManager} for this activity
	 */
	private UsbAudioManager mUsbAudioManager;
	
	
	Pcamera mPcamera;
	SharedPreferences mSharedPrefs;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		int number_of_runs = mSharedPrefs.getInt("number_of_runs", 0);
		if (number_of_runs==0){
			new UpSaleDialog(R.string.dialog_welcome_to_penelope, R.string.dialog_button_enjoy)
				.show(getFragmentManager(),"PaidForVersionDialog");
		}
		mSharedPrefs.edit().putInt("number_of_runs", ++number_of_runs).apply();
		
		getOverflowMenu();
		setContentView(R.layout.activity_fullscreen);

		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = findViewById(R.id.fullscreen_content);
		mFragmentViewGroup = (FrameLayout) findViewById(R.id.fragment_container);

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, contentView,
				HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider.setOnVisibilityChangeListener(new 
				SystemUiHider.OnVisibilityChangeListener() {
					// Cached values.
					int mControlsHeight;
					int mShortAnimTime;

					@Override
					@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
					public void onVisibilityChange(boolean visible) {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
							// If the ViewPropertyAnimator API is available
							// (Honeycomb MR2 and later), use it to animate the
							// in-layout UI controls at the bottom of the
							// screen.
							if (mControlsHeight == 0) {
								mControlsHeight = controlsView.getHeight();
							}
							if (mShortAnimTime == 0) {
								mShortAnimTime = getResources().getInteger(
										android.R.integer.config_shortAnimTime);
							}
							controlsView
									.animate()
									.translationY(visible ? 0 : mControlsHeight)
									.setDuration(mShortAnimTime);
						} else {
							// If the ViewPropertyAnimator APIs aren't
							// available, simply show or hide the in-layout UI
							// controls.
							controlsView.setVisibility(visible ? View.VISIBLE
									: View.GONE);
						}

						if (visible && AUTO_HIDE) {
							// Schedule a hide().
							delayedHide(AUTO_HIDE_DELAY_MILLIS);
						}
					}
				});

		
		// Set up the user interaction to manually show or hide the system UI.
		//*
		contentView.setOnTouchListener(new View.OnTouchListener(){

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return mGLView.listenForTouch.onTouch(v, event);
			}
		
		}); //*/
		
		contentView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
					
				} else {
					mSystemUiHider.hide();
					
				}
				

			}
		});

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		findViewById(R.id.dummy_button).setOnTouchListener(
				mDelayHideTouchListener);
		findViewById(R.id.dummy_button).setOnClickListener(
				mClickListener);
		
				
		// Create instance on AudioOnAir
		// get list of USB connected devices.
		
		mUsbAudioManager = new UsbAudioManager(this);
		mAudioOnAir = new AudioOnAir((Button) findViewById(R.id.dummy_button), 
				(TextView) findViewById(R.id.fullscreen_content), 
				mUsbAudioManager);
		mGLView = new MyGLSurfaceView(this,mAudioOnAir.NoteSpectrum);
		mGLTextureView = new OpenGLTextureViewSample(this);
		mGLTextureView.setSurfaceTextureListener(this);
		
        // 
		//mSettingsFragment = new SettingsFragment();
	
		
		mPcamera = new Pcamera(this,(Button) findViewById(R.id.record_button));
		//mPcamera.start(mFragmentViewGroup);
		
		mSharedPrefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
		//ActionBar mActionBar = getActionBar();
		//mActionBar.onMenuVisibilitychange();
		


		
	}
	
	@Override
	protected void onPause(){
		super.onPause();
		
		if (checkSleep(this)){
			if (TOGGLE_ONAIR_CLICK){
				findViewById(R.id.dummy_button).performClick();
				mPcamera.stop(mFragmentViewGroup);
			}
		}
	}
	
	
	/**
	 * Check whether the device has been put to sleep (screenOff) or if
	 *  the user is on the phone (onPhoneCall)
	 * @param context the context to check.
	 * @return true if on call or asleep otherwise false.
	 */
	public static boolean checkSleep(Context context){
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		boolean screenOff = !pm.isScreenOn();
		int onPhoneCall = tm.getCallState();
		if (screenOff||(onPhoneCall!=0)){
			return true;

		}
		return false;
	}
	
	@Override 
	protected void onStop(){
		super.onStop();
		mAudioOnAir.StopAudio();
		mPcamera.stop(mFragmentViewGroup);
	}
	
	
	@Override 
	protected void onDestroy(){
		super.onDestroy();
		mAudioOnAir.kill();
		mUsbAudioManager.close(this);
	}
	
	@Override
	protected void onRestart(){
		super.onRestart();
		mAudioOnAir.StartAudio();
		if (TOGGLE_ONAIR_CLICK && RECORD_MODE) { //TODO change this to and record
			mPcamera.start(mFragmentViewGroup);
		}
	}
	
	
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
	}
	
	/*@Override
	public boolean onTouchEvent(MotionEvent e) {
		super.onTouchEvent(e);
		if (! (mGLView==null)){
			mGLView.performClick();
		}
		
		return false;
	}//*/

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.xml.menu, menu);
	    mMenuItem = menu.findItem(R.id.options_menu_item);
	    mMenuItem.setOnActionExpandListener(mOptionsExpandListener);
	    mSettingsMenu = mMenuItem.getSubMenu();
	    //mMenuItem.setOnMenuItemClickListener(mMenuItemClickedListener);
	    //nflater.mSettingsMenuItems = new MenuItem[mSettingsMenu.size()];
	    //for (int i = 0; i<menu.size(); i++){
	    //	mSettingsMenuItems[i] = mSettingsMenu.getItem(i);
	    //	mSettingsMenuItems[i].setOnMenuItemClickListener(mMenuItemClickedListener);
	    //}
	    
	    
	    mMenu = menu;
	    return true;
	}
	
	public boolean onPrepareOptionsMenu(Menu menu){
		mHideHandler.removeCallbacks(mHideRunnable);
		if (TOGGLE_ONAIR_CLICK){
			menu.findItem(R.id.options_menu_item_record).setEnabled(true);
			menu.findItem(R.id.options_menu_item_record).setOnMenuItemClickListener(RecordOptionMenuListener);
		} else {
			menu.findItem(R.id.options_menu_item_record).setEnabled(false);
			//findViewById(R.id.options_menu_item_record).removeOnClickListener(RecordOptionMenuListener);
		}
		
		return true;
		
	}
	
	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
				
			} 
			return false;
		}
	};
	
	View.OnClickListener mClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			mAudioOnAir.Toggle(mGLView);
			TOGGLE_ONAIR_CLICK = !TOGGLE_ONAIR_CLICK;
			
			if (TOGGLE_ONAIR_CLICK) {
				//intend to start the audio 
				//Intent intent = new Intent(view.getContext(), AudioOnAir.class);				
				
				//findViewById(R.id.fullscreen_content);
				//setContentView(R.layout.activity_onair);
				mFragmentViewGroup.addView(mGLView);
				findViewById(R.id.fullscreen_content).animate()
					.alpha(0f)
					.setDuration(AUTO_HIDE_DELAY_MILLIS)
					.setListener(null);
				mGLView.setOnTouchListener(mGLView.listenForTouch);
				//setContentView(mGLView);
				//setContentView(mGLTextureView);
				//mGLView.builder.show();
			}else {
				if (RECORD_MODE) {
					RecordOptionMenuListener.onMenuItemClick(mMenu.findItem(R.id.options_menu_item_record));
					//((View) mMenu.findItem(R.id.options_menu_item_record)).performClick();
				}
				//mGLView.setVisibility(View.GONE);
				findViewById(R.id.fullscreen_content).setVisibility(View.VISIBLE);
				findViewById(R.id.fullscreen_content).animate()
					.alpha(1f)
					.setDuration(AUTO_HIDE_DELAY_MILLIS/3)
					.setListener(null);
				//mGLView.removeOnTouchListener(mGLView.listenForTouch);
				mFragmentViewGroup.removeView(mGLView);

				//setContentView(R.layout.activity_fullscreen);
			}
			
		}
	};
	
	OnMenuItemClickListener RecordOptionMenuListener = new OnMenuItemClickListener(){

		@Override
		public boolean onMenuItemClick(MenuItem item) {
			RECORD_MODE = !RECORD_MODE;
			if (RECORD_MODE) {
				mPcamera.start(mFragmentViewGroup);
				findViewById(R.id.record_button).setVisibility(View.VISIBLE);
				new UpSaleDialog(R.string.dialog_penelope_full_messsage_record)
					.show(getFragmentManager(),"PaidForVersionDialog");
			} else {
				mPcamera.stop(mFragmentViewGroup);
				findViewById(R.id.record_button).setVisibility(View.GONE);
			}
			return true;
		}
		
	};
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
	    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ECLAIR
	            && keyCode == KeyEvent.KEYCODE_BACK
	            && event.getRepeatCount() == 0) {
	        // Take care of calling this method on earlier versions of
	        // the platform where it doesn't exist.
	        onBackPressed();
	    }

	    return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onBackPressed() {
	    // This will be called either automatically for you on 2.0
	    // or later, or by the code above on earlier versions of the
	    // platform.
		if (mSystemUiHider.isVisible()){
			mSystemUiHider.hide();
		} else if (TOGGLE_ONAIR_CLICK) {
			findViewById(R.id.dummy_button).performClick();
		} else finish();
		
	    return;
	}
	
	/*/
	public void OpenDevicePreferences(View view){
		// Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();	
	}//*/

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			mSystemUiHider.hide();
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}
	
	
	
	public boolean onOptionsItemSelected(MenuItem item){
		   
		mHideHandler.removeCallbacks(mHideRunnable);
		if (item.getItemId() == R.id.options_menu_item) {
			Intent intent = new Intent(this, SettingsActivity.class);	
			intent.putExtra(EXTRA_MESSAGE, R.xml.settings);
			startActivity(intent);
		} else if (item.getItemId()==R.id.options_menu_item_help) {
			Intent intent = new Intent(this, SettingsActivity.class);	
			intent.putExtra(EXTRA_MESSAGE,  R.xml.help);
			startActivity(intent);
		}
		
		return false;
	}
	
	/**
	 * Remove the UI's hide routine when the options menu is expanded
	 * and hide UI when options menu is collapsed.
	 */
	private MenuItem.OnActionExpandListener mOptionsExpandListener = new MenuItem.OnActionExpandListener() {
		
		@Override
		public boolean onMenuItemActionExpand(MenuItem item) {
			mHideHandler.removeCallbacks(mHideRunnable);
			return false;
		}
		
		@Override
		public boolean onMenuItemActionCollapse(MenuItem item) {
			delayedHide(AUTO_HIDE_DELAY_MILLIS);
			return false;
		}
	};

	
	/**
	 * Open the Preference fragment associated with the item when 
	 * the item is selected.
	 */
	/*private MenuItem.OnMenuItemClickListener mMenuItemClickedListener = new MenuItem.OnMenuItemClickListener() {

		@Override
		public boolean onMenuItemClick(MenuItem item) {
			//mSystemUiHider.hide();
			//mSystemUiHider.disable();
			if (item.getItemId() == R.id.devices_menu_item) {
				
				TOGGLE_ON_CLICK = false; //turn off the UI while in settings.
				mSettingsFragment.addPreferencesFromResource(R.xml.devices);
			} else if (item.getItemId() == R.id.visualizations_menu_item) {
				
			} else if (item.getItemId() == R.id.addons_menu_item) {
				
			}
			return false;
		}
		

	};//*/

	
	
	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
			int height) {
        /*mCamera = Camera.open();

        try {
        	mGLView.getMyGLSurfaceView(this).;
            mCamera.setPreviewTexture(surface);
            mCamera.startPreview();
        } catch (IOException ioe) {
            // Something bad happened
        }*/
		//mGLTextureView.onSurfaceTextureAvailable(surface, width, height);
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
	
	private void getOverflowMenu() {

	     try {
	        ViewConfiguration config = ViewConfiguration.get(this);
	        Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
	        if(menuKeyField != null) {
	            menuKeyField.setAccessible(true);
	            menuKeyField.setBoolean(config, false);
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	private OnMenuVisibilityListener ActionBarMenuListerner = new OnMenuVisibilityListener(){

		@Override
		public void onMenuVisibilityChanged(boolean isVisible) {
			if (isVisible){
				mHideHandler.removeCallbacks(mHideRunnable);
			} else {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			
		}
		
		
	};
	
	OnSharedPreferenceChangeListener preferenceChangeListener = new OnSharedPreferenceChangeListener() {
		
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
				String key) {
			if (key.contains("turn_on_accelerometer_key")) {
				/*/
				if (sharedPreferences.getBoolean(key, true)){
					mGLView.mRenderer.mAccelmeter.start();
				} else {
					mGLView.mRenderer.mAccelmeter.stop();
					mGLView.mRenderer.mAccelmeter.linear_acceleration[0] = 0.0f;
					mGLView.mRenderer.mAccelmeter.linear_acceleration[1] = 0.0f;//reset
				}//*/
			}
			
		}
	};
	
}


