package com.harmonicprocesses.penelopefree;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.vending.billing.IInAppBillingService;
import com.harmonicprocesses.penelopefree.R;
import com.harmonicprocesses.penelopefree.audio.AudioConstants;
import com.harmonicprocesses.penelopefree.audio.AudioOnAir;
import com.harmonicprocesses.penelopefree.audio.AudioProcessor;
import com.harmonicprocesses.penelopefree.audio.DSPEngine;
import com.harmonicprocesses.penelopefree.audio.OnAir;
import com.harmonicprocesses.penelopefree.camera.Pcamera;
import com.harmonicprocesses.penelopefree.openGL.MyGLSurfaceView;
import com.harmonicprocesses.penelopefree.openGL.MyGLSurfaceViewLegacy;
import com.harmonicprocesses.penelopefree.settings.HelpActivity;
import com.harmonicprocesses.penelopefree.settings.SettingsActivity;
import com.harmonicprocesses.penelopefree.settings.SettingsFragment;
import com.harmonicprocesses.penelopefree.settings.SpecialEffects;
import com.harmonicprocesses.penelopefree.settings.UpSaleDialog;
import com.harmonicprocesses.penelopefree.usbAudio.UsbAudioManager;
import com.harmonicprocesses.penelopefree.util.SystemUiHider;
import com.harmonicprocesses.penelopefree.util.SystemUiHiderBase;
import com.hpp.billing.PurchaseDialog;
import com.hpp.billing.PurchaseManager;
import com.hpp.openGL.MyEGLWrapper;
//import com.harmonicprocesses.penelopefree.settings.SubSettingsFragment;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.OnMenuVisibilityListener;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Camera;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.TextureView;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

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
	private static final int HIDER_FLAGS = SystemUiHiderBase.FLAG_FULLSCREEN;

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
	private FrameLayout mFragmentViewGroup, mOpenGLViewGroup;
	
	/**
	 * The instance of the {@link UsbAudioManager} for this activity
	 */
	private UsbAudioManager mUsbAudioManager;
	
	
	Pcamera mPcamera;
	SharedPreferences mSharedPrefs;
	public AudioProcessor mAudioProcessor;
	
	Handler procNoteHandler = null;
	
	TextView backgroundText;
	Button onAirButton;
	public PurchaseManager purchaseManager;
	final static public boolean netConnection = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/*ActivityManager activityManager = (ActivityManager) this.getSystemService( ACTIVITY_SERVICE );
	    List<RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
	    for(int i = 0; i < procInfos.size(); i++){
	        if(procInfos.get(i).processName.equals("com.harmonicprocesses.penelopefree")) {
	            Toast.makeText(getApplicationContext(), "Penelope Is Already Running", Toast.LENGTH_LONG).show();
	            onDestroy();
	        }
	    }//*/
		
		//ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		//NetworkInfo ni = cm.getActiveNetworkInfo();
		//netConnection = (ni != null && ni.isAvailable() && ni.isConnected());
		
		// connect to Google Play Billing service
		purchaseManager = new PurchaseManager(this);
		
		// check login stuff
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		int number_of_runs = mSharedPrefs.getInt("number_of_runs", 0);
		int ask_to_rate = mSharedPrefs.getInt("ask_to_rate", 0);
		if (number_of_runs<=1){
			Bundle bundle = new Bundle();
			bundle.putInt("messageId", R.string.dialog_welcome_to_penelope);
			bundle.putInt("button1", R.string.dialog_button_ok);
			UpSaleDialog dialog = new UpSaleDialog();
			dialog.setArguments(bundle);
			dialog.show(getFragmentManager(),"PaidForVersionDialog");
			
		} else if (number_of_runs>=3 && ask_to_rate==0){
			Bundle bundle = new Bundle();
			bundle.putInt("messageId", R.string.dialog_rate_penelope);
			bundle.putInt("button1", R.string.dialog_button_ok);
			bundle.putInt("button2", R.string.dialog_button_next_time);
			UpSaleDialog dialog = new UpSaleDialog();
			dialog.setArguments(bundle);
			dialog.show(getFragmentManager(),"PaidForVersionDialog");
			mSharedPrefs.edit().putInt("ask_to_rate", 1).apply();
		}
		mSharedPrefs.edit().putInt("number_of_runs", ++number_of_runs).apply();
		
		int version_code=13;
		try {
			version_code = this.getPackageManager()
				    .getPackageInfo(this.getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int new_features_level = mSharedPrefs.getInt("new_features_level", version_code);
		if (new_features_level<version_code) {
			//New Features Dialog Covering Special Effects Products
			UpSaleDialog.BuildUpSaleDialog(this, R.string.dialog_welcome_to_new_features,
					R.string.dialog_button_buy_mug, R.string.dialog_button_ok, 3)
					.show(getFragmentManager(),"PaidForVersionDialog");
			mSharedPrefs.edit().putInt("new_features_level", version_code).apply();
		}
		
		
		
		//mAudioProcessor.dsp = new DSPEngine(mAudioProcessor.bufferSize,
		//		AudioConstant.sampleRate, getBaseContext());
		//maybe creating our dsp engine on the processor thread will
		// alleviate some reflection and allocation calls.
		/*procNoteHandler.post(new Runnable(){

			@Override
			public void run() {
				mAudioProcessor.dsp = new DSPEngine(mAudioProcessor.bufferSize,
						AudioConstant.sampleRate, getBaseContext());
				
			}
			
		});//*/
		
		
		getOverflowMenu();
		setContentView(R.layout.activity_fullscreen);

		
		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = findViewById(R.id.fullscreen_content);
		backgroundText = (TextView) findViewById(R.id.fullscreen_content);
		onAirButton = (Button) findViewById(R.id.dummy_button); 
		mFragmentViewGroup = (FrameLayout) findViewById(R.id.fragment_container);
		mOpenGLViewGroup = (FrameLayout) findViewById(R.id.opengl_container);
		final SurfaceView mCameraSurfaceView = new SurfaceView(mContext);
		mGLView = new MyGLSurfaceViewLegacy(mContext);
		
		mAudioProcessor = new AudioProcessor(this,
				android.os.Process.THREAD_PRIORITY_AUDIO,
				android.os.Process.THREAD_PRIORITY_URGENT_AUDIO,
				mSharedPrefs.getInt("sound_buffer_size_key", 2));
		
		mAudioProcessor.setSprectrumUpdateHandler(new UpdateSpectrumHandler(this,getMainLooper()));
		ArrayList<String> skus = mAudioProcessor.checkSpecialEffects(this,purchaseManager);
		if (!skus.isEmpty()){
			for (String sku:skus){
				PurchaseDialog dialog = new PurchaseDialog()
						.setPurchaseManager(purchaseManager)
						.setAudioProcessor(mAudioProcessor)
						.setSku(sku);
				dialog.show(getFragmentManager(), "Purchase " + sku);
			}
		}
		mAudioProcessor.updateToneGenerator(mSharedPrefs.getBoolean("enable_tone_generator_key", false));
		mAudioProcessor.updateReverb(mSharedPrefs.getBoolean("enable_reverb_key", false));
		mAudioProcessor.start();
		procNoteHandler = mAudioProcessor.getNoteUpdateHandler();

		mPcamera = new Pcamera(this,(Button) findViewById(R.id.record_button),
				mGLView, mAudioProcessor.getThru(), mCameraSurfaceView);
		
		
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
		
				
		View.OnTouchListener onTouchGeneralListener = new View.OnTouchListener(){

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				mHideHandler.removeCallbacks(mToast1Runnable);
				if (event.getAction()==MotionEvent.ACTION_UP||
						event.getAction()==MotionEvent.ACTION_CANCEL){
					if (TOGGLE_ON_CLICK) {
						mSystemUiHider.toggle();
					
					} else {
						mSystemUiHider.hide();
					}
					
				}
				if (TOGGLE_ONAIR_CLICK) {mGLView.listenForTouch.onTouch(v, event);}
				return true;//mGLView.listenForTouch.onTouch(v, event);
			}
		
		}; //*/
		
				
		contentView.setOnTouchListener(onTouchGeneralListener);
		//controlsView.setOnClickListener(onClickGeneralListener);
		
		
		
		/*
		contentView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
					
				} else {
					mSystemUiHider.hide();
				}
				

			}
		}); //*/

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		onAirButton.setOnTouchListener(
				mDelayHideTouchListener);
		onAirButton.setOnClickListener(
				mClickListener);
		
				
		// Create instance on AudioOnAir
		// get list of USB connected devices.
		
		//mUsbAudioManager = new UsbAudioManager(this);
		//mAudioOnAir = new AudioOnAir((Button) findViewById(R.id.dummy_button), 
		//		(TextView) findViewById(R.id.fullscreen_content), 
		//		mUsbAudioManager);
		
		//mGLTextureView = new OpenGLTextureViewSample(this);
		//mGLTextureView.setSurfaceTextureListener(this);
		
        // 
		//mSettingsFragment = new SettingsFragment();
	
		
		
		//mPcamera.start(mOpenGLViewGroup);
		
		mSharedPrefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
		//ActionBar mActionBar = getActionBar();
		//mActionBar.onMenuVisibilitychange();
		
		
		if (number_of_runs<=3){
			mHideHandler.postDelayed(mToast1Runnable, AUTO_HIDE_DELAY_MILLIS);
			mHideHandler.postDelayed(mToast2Runnable, 2*AUTO_HIDE_DELAY_MILLIS);
		}
		
		mHideHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				onAirButton.performClick();
			}}, AUTO_HIDE_DELAY_MILLIS / 2);		
	}
	
	
	@Override
	protected void onPause(){
		super.onPause();
		
		if (checkSleep(this)){
			if (TOGGLE_ONAIR_CLICK){
				findViewById(R.id.dummy_button).performClick();
				mPcamera.stop(mOpenGLViewGroup);
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
		//mAudioOnAir.StopAudio();
		mAudioProcessor.stopAudio();
		mPcamera.stop(mOpenGLViewGroup);
	}
	
	
	@Override 
	protected void onDestroy(){
		super.onDestroy();
		//mAudioOnAir.kill();
		mAudioProcessor.releaseAudio();
		mAudioProcessor.quit();
		//mUsbAudioManager.close(this);
		purchaseManager.unbind();

	}
	
	@Override
	protected void onRestart(){
		super.onRestart();
		//mAudioOnAir.StartAudio();
		if (TOGGLE_ONAIR_CLICK){
			startAudio();
			if (RECORD_MODE) { //TODO change this to and record
			//mPcamera.start(mOpenGLViewGroup);
			mPcamera.start();
			}
		}
	}
	
	
	
	private void startAudio() {
		procNoteHandler.post(new Runnable(){

			@Override
			public void run() {
				mAudioProcessor.startAudio(mSharedPrefs.getBoolean("enable_reverb_key", true),
						mSharedPrefs.getBoolean("invert_audio_key", false), 
						mSharedPrefs.getInt("sound_buffer_size_key", 2),
						((float) mSharedPrefs.getInt("sound_wet_dry_key", 92)/100.0f));
				
			}
			
		});

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
	
	boolean optionsMenuPrepared = false;
	
	public boolean onPrepareOptionsMenu(Menu menu){
		if (!optionsMenuPrepared){
			optionsMenuPrepared = true;
		} else {
			mHideHandler.removeCallbacks(mHideRunnable);
		}
		
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
	
	protected static class UpdateSpectrumHandler extends Handler {
		private final WeakReference<PenelopeMainActivity> pennyReference;
		
		public UpdateSpectrumHandler(PenelopeMainActivity penny, Looper looper){
			super(looper);
			pennyReference = new WeakReference<PenelopeMainActivity>(penny);
		}
		
		public void handleMessage(Message msg) {
			PenelopeMainActivity penny = pennyReference.get();
			if (penny!=null){
				penny.onNewSpectrum((float[]) msg.obj);
			}
		}
	}
	
	protected void onNewSpectrum(float[] newSpectrum) {
		int note = 0;
		if (TOGGLE_ONAIR_CLICK){
			try {
				note = mGLView.updateAmplitudes(newSpectrum);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		synchronized(procNoteHandler){
			Message msg = procNoteHandler.obtainMessage(); 
			msg.obj = note;
			procNoteHandler.sendMessage(msg); 
		}
	}
	
	View.OnClickListener mClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			//mAudioOnAir.Toggle(mGLView);
			TOGGLE_ONAIR_CLICK = !TOGGLE_ONAIR_CLICK;
			mHideHandler.removeCallbacks(mToast2Runnable);
			
			CharSequence temp = backgroundText.getText();
			backgroundText.setText(onAirButton.getText());
			onAirButton.setText(temp);
			
			if (TOGGLE_ONAIR_CLICK) {
				//intend to start the audio 
				//Intent intent = new Intent(view.getContext(), AudioOnAir.class);				
				
				//findViewById(R.id.fullscreen_content);
				//setContentView(R.layout.activity_onair);
				
				mOpenGLViewGroup.addView(mGLView);
				findViewById(R.id.fullscreen_content).animate()
					.alpha(0f)
					.setDuration(AUTO_HIDE_DELAY_MILLIS)
					.setListener(null);
				mGLView.setOnTouchListener(mGLView.listenForTouch);
				
				startAudio();
				/*/
					mAudioProcessor.startAudio(mSharedPrefs.getBoolean("enable_reverb_key", true),
							mSharedPrefs.getBoolean("invert_audio_key", false), 
							mSharedPrefs.getInt("sound_buffer_size_key", 2),
							((float) mSharedPrefs.getInt("sound_wet_dry_key", 50)/100.0f));
				}//*/
				
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
				mOpenGLViewGroup.removeView(mGLView);
				
				mAudioProcessor.stopAudio();
				
				//setContentView(R.layout.activity_fullscreen);
			}
			
		}
	};
	
	OnMenuItemClickListener RecordOptionMenuListener = new OnMenuItemClickListener(){

		@Override
		public boolean onMenuItemClick(MenuItem item) {
			RECORD_MODE = !RECORD_MODE;
			if (RECORD_MODE) {
				//mPcamera.start(mOpenGLViewGroup);
				item.setChecked(true);
				if (!mPcamera.start()){
					RECORD_MODE = !RECORD_MODE;
					item.setChecked(false);
					//return true;
				} else {
					findViewById(R.id.record_button).setVisibility(View.VISIBLE);
				}
				UpSaleDialog.BuildUpSaleDialog(mContext,
						R.string.dialog_penelope_full_messsage_record)
						.show(getFragmentManager(),"PaidForVersionDialog");
			} else {
				item.setChecked(false);
				mPcamera.stop(mOpenGLViewGroup);
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
		if (mFragmentViewGroup.getVisibility() == View.VISIBLE){
			mSystemUiHider.enable();
			mFragmentViewGroup.setVisibility(View.GONE);
		} else if (mSystemUiHider.isVisible()){
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
	
	Context mContext = this;
	
	Runnable mToast1Runnable = new Runnable() {
		@Override
		public void run() {
			Toast.makeText(mContext, R.string.touch_screen, AUTO_HIDE_DELAY_MILLIS).show();
			mHideHandler.postDelayed(mToast1Runnable, 2*AUTO_HIDE_DELAY_MILLIS);
		}
	};
	
	Runnable mToast2Runnable = new Runnable() {
		@Override
		public void run() {
			Toast.makeText(mContext, R.string.go_onair, AUTO_HIDE_DELAY_MILLIS).show();
			mHideHandler.postDelayed(mToast2Runnable, 2*AUTO_HIDE_DELAY_MILLIS);
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
		} else if (item.getItemId()==R.id.options_menu_special_efects) {
			//Intent intent = new Intent(this, SpecialEffects.class);
			mSettingsFragment = new SettingsFragment().setXmlId(R.xml.special_effects);
			mSettingsFragment.setPurchaseManager(purchaseManager)
					.setAudioProcessor(mAudioProcessor)
					.setPcam(mPcamera);
			getFragmentManager().beginTransaction()
				.replace(R.id.fragment_container, mSettingsFragment)
				.commit();
			mFragmentViewGroup.setVisibility(View.VISIBLE);
			mSystemUiHider.hide();
			mSystemUiHider.disable();
		}
		
		return false;
	}
	
	private void setPcam(Pcamera mPcamera2) {
		// TODO Auto-generated method stub
		
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
	
//*
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) { 
		
		if (requestCode == purchaseManager.pitchCorrect_RequestCode) {           
			int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
			String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
			String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
			
			if (resultCode == RESULT_OK) {
				try {
					JSONObject jo = new JSONObject(purchaseData);
					String msg = getString(R.string.dialog_purchase1) +
							jo.getString("productId") + getString(R.string.dialog_purchase2);
				
					Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
					mAudioProcessor.setPitchCorrect(true);
					
				}
				catch (JSONException e) {
					Toast.makeText(mContext, R.string.dialog_purchase_fail, Toast.LENGTH_LONG).show();
					// uncheck the box
					mAudioProcessor.setPitchCorrect(false);
					mSettingsFragment.checkPref.setChecked(false);
					e.printStackTrace();
				}
			} else {
				Toast.makeText(mContext, R.string.dialog_purchase_fail, Toast.LENGTH_LONG).show();
				// uncheck the box
				mAudioProcessor.setPitchCorrect(false);
				mSettingsFragment.checkPref.setChecked(false);
			}
		}
	}
	//*/
}


