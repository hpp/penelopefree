package com.harmonicprocesses.penelopefree.audio;

import com.harmonicprocesses.penelopefree.R;
import com.harmonicprocesses.penelopefree.R.id;
import com.harmonicprocesses.penelopefree.R.layout;
import com.harmonicprocesses.penelopefree.R.xml;
import com.harmonicprocesses.penelopefree.openGL.MyGLSurfaceView;
import com.harmonicprocesses.penelopefree.openGL.MyGLSurfaceViewLegacy;
import com.harmonicprocesses.penelopefree.util.SystemUiHider;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class OnAirActivity extends Activity {
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;
	
	/**
	 * If set, will toggle the OnAir/OffAir background.,
	 * will also turn on and off main play back functionality.
	 */
	private static boolean TOGGLE_ONAIR_CLICK = false;


	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

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
	private MenuItem mSettingsMenu;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_onair);

		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = findViewById(R.id.fullscreen_content);
		

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
		contentView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
					
				} else {
					mSystemUiHider.show();
					
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
		//mAudioOnAir = new AudioOnAir((Button) findViewById(R.id.dummy_button), (TextView) findViewById(R.id.fullscreen_content));
		mGLView = new MyGLSurfaceViewLegacy(this);
	
        // Display the fragment as the main content.
        //getFragmentManager().beginTransaction()
        //        .replace(android.R.id.content, new SettingsFragment())
        //        .commit();	
	}
	
	
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.xml.menu, menu);
	    mMenuItem = menu.findItem(R.id.options_menu_item);
	    mMenuItem.setOnActionExpandListener(mOptionsExpandListener);
	    //mSettingsMenu = menu.findItem(R.id.settings_menu);
	    mMenu = menu;
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
			TOGGLE_ONAIR_CLICK = !TOGGLE_ONAIR_CLICK;
			if (TOGGLE_ONAIR_CLICK) {
				//intend to start the audio 
				Intent intent = new Intent(view.getContext(), AudioOnAir.class);				
				mAudioOnAir.Toggle(mGLView);
				//findViewById(R.id.fullscreen_content);
				setContentView(R.layout.activity_fullscreen);
				//mGLView.builder.show();
			}else setContentView(R.layout.activity_onair);
			
		}
	};

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
		
		return false;
	}
	
	/**
	 * Remove the UI's hide routine when the options menu is expanded
	 * and hide UI when options menu is collapsed.
	 */
	@SuppressLint("NewApi")
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
	
	
}


