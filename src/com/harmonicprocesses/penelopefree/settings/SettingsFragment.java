package com.harmonicprocesses.penelopefree.settings;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.harmonicprocesses.penelopefree.PenelopeMainActivity;
import com.harmonicprocesses.penelopefree.R;
import com.harmonicprocesses.penelopefree.audio.AudioProcessor;
import com.harmonicprocesses.penelopefree.camera.Pcamera;
import com.hpp.billing.PurchaseDialog;
import com.hpp.billing.PurchaseManager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SettingsFragment extends PreferenceFragment {
	
	private int mXmlId;
	private String mKey;
	private Resources mRes;
	private FragmentManager mFrag;
	private Activity mAct;
	//private PenelopeMainActivity mPan;
	private PurchaseManager mPurchaseManager;
	private AudioProcessor mAudioProc;
	private Pcamera mPcam = null;
	public CheckBoxPreference checkPref;
	private Bundle constructorArgs = null;
	
    public SettingsFragment() {
	}
    
    public SettingsFragment setXmlId(int xmlId){
    	checkConstructorArgs();
    	constructorArgs.putInt("mXmlId", xmlId);
    	this.setArguments(constructorArgs);
    	return this;
    }

    private void checkConstructorArgs(){
    	if (constructorArgs == null){
    		constructorArgs = new Bundle();
    	}
    }

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAct = getActivity();
        mRes = mAct.getResources();
        mFrag = getFragmentManager();
        // Load the preferences from an XML resource
        addPreferencesFromResource(getArguments().getInt("mXmlId"));
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }
    
    
    //adapted from http://proandroiddev.blogspot.com/2011/04/honeycomb-tip-1-preferencefragment.html
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen prefScreen, Preference pref){
    	super.onPreferenceTreeClick(prefScreen, pref);
    	trackPreferenceTreeClick(pref);
    	
    	mKey = pref.getKey();
    	if (mKey.contains("devices_category_key")) {
    		// Display the fragment as the main content.
    		transact(R.xml.devices,"Devices");
    		return true;
        } else if (mKey.contains("visualizations_category_key")) {
    		// Display the fragment as the main content.
        	transact(R.xml.visualizations, "Visualizations");
    		return true;
        }  else if (mKey.contains("addons_category_key")) {
    		// Display the fragment as the main content.
        	transact(R.xml.addons,"Add-Ons");
    		return true;
        } else if (mKey.contains("enable_pitch_correct_key")){
        	//toggle pitch correct
        	checkPref = ((CheckBoxPreference) pref);
        	if (mAudioProc.boolPitchCorrectEnabled||!checkPref.isChecked()){
        		mAudioProc.setPitchCorrect(false);
        		checkPref.setChecked(false);
        		return true;
        	}
        	//check if customer owns pitch correction
        	if (mPurchaseManager.purchasedList.contains("Pitch Correction")){
        		//if owned activate in AudioProc
        		mAudioProc.setPitchCorrect(true);
        		return true;
        	} else {
        		//if not launch buy/try dialog
        		PurchaseDialog dialog = new PurchaseDialog()
        				.setPurchaseManager(mPurchaseManager)
        				.setAudioProcessor(mAudioProc)
        				.setSettingsFragment(this)
        				.setSku(mPurchaseManager.pitchCorrect);
        		dialog.show(mFrag, "PurchasePitchCorrect");
        		return true;
        	}
        	
        	
        	
        } else if (mKey.contains("ad_infinitum_key")){
        	
        	checkPref = ((CheckBoxPreference) pref);
        	if (mPurchaseManager.purchasedList.contains("Ad Infinitum")){
        		//if owned make sure the box is checked
        		checkPref.setChecked(true);
        		return true;
        	} else {
        		checkPref.setChecked(false);
        		
        		//if not launch buy/try dialog
        		PurchaseDialog dialog = new PurchaseDialog()
        				.setPurchaseManager(mPurchaseManager)
        				.setAudioProcessor(mAudioProc)
        				.setSettingsFragment(this)
        				.setSku(mPurchaseManager.adInfinitum)
        				.setMId1(R.string.dialog_purchase_buy_ad_infinitum)
        				.setMId2(R.string.dialog_purchase_adInfinitum)
        				.setButton1(R.string.dialog_button_more_info, 
        					new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									final Intent intent = new Intent(Intent.ACTION_VIEW)
										.setData(Uri.parse("http://penny.hpp.io/?p=1"));
									getActivity().startActivity(intent);
								}
							});
        		dialog.show(mFrag, "PurchasePitchCorrect");
        		return true;
        	} 
        } 	
    	return false;
    }
    
    private void trackPreferenceTreeClick(Preference pref) {
   		EasyTracker easyTracker = EasyTracker.getInstance(getActivity());
   		// MapBuilder.createEvent().build() returns a Map of event fields and values
   		// that are set and sent with the hit.
   		easyTracker.send(MapBuilder.createEvent(
   				"pref_tree_click",     // Event category (required)
   				pref.getKey(),  // Event action (required)
   				null,   // Event label
   				null)            // Event value
   		.build());
   	}
    
    private void trackPreferenceChanged(String key, String value) {
   		EasyTracker easyTracker = EasyTracker.getInstance(getActivity());
   		// MapBuilder.createEvent().build() returns a Map of event fields and values
   		// that are set and sent with the hit.
   		
   		easyTracker.send(MapBuilder.createEvent(
   				"pref_change",     // Event category (required)
   				key,  // Event action (required)
   				value,   // Event label
   				null)            // Event value
   		.build());
   	}

	private void transact(int id, String title){
    	mFrag.beginTransaction().replace(R.id.settingsFragmentView, 
				new SubSettingsFragment().setArguments(id))
			.addToBackStack(null).commit();
    	mAct.setTitle(title); 
    }


    OnSharedPreferenceChangeListener preferenceChangeListener = new OnSharedPreferenceChangeListener() {
		
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
				String key) {
			String value = null;
			if (key.contains("input_device_key")||key.contains("output_device_key")){
				mKey = key;
				if (sharedPreferences.getString(key, "").contains(mRes.getString(R.string.inUSB_default))||
					sharedPreferences.getString(key, "").contains(mRes.getString(R.string.outUSB_default))){
					Bundle bundle = new Bundle();
					bundle.putInt("messageId", R.string.dialog_penelope_full_messsage_usb);
					UpSaleDialog dialog = new UpSaleDialog();
					dialog.setArguments(bundle);
					dialog.show(mFrag,"PaidForVersionDialog");
				}
			} else if (key.contains("turn_on_accelerometer_key")||key.contains("vis_number_of_particles_key")||
					key.contains("invert_audio_key")||key.contains("sound_buffer_size_key")) {
				Bundle bundle = new Bundle();
				bundle.putInt("messageId", R.string.dialog_restart_simulation);
				bundle.putInt("button1",R.string.dialog_button_ok);
				UpSaleDialog dialog = new UpSaleDialog();
				dialog.setArguments(bundle);
				dialog.show(mFrag,"RestartSimulationDialog");
			} else if (key.contains("SEFX_pitch_correct")) {
				int valueInt = sharedPreferences.getInt(key,100);
				mAudioProc.updatePitchCorrect(valueInt);
				value = Integer.toString(valueInt);
			} else if (key.contains("enable_reverb_key")){
				boolean valueBool = sharedPreferences.getBoolean(key,true);
				mAudioProc.updateReverb(valueBool);
				value = String.valueOf(valueBool);
			} else if (key.contains("sound_wet_dry_key")){
				int valueInt = sharedPreferences.getInt(key,92);
				mAudioProc.updateWetDry(sharedPreferences.getInt(key,92));
				value = Integer.toString(valueInt);
			} else if (key.contains("video_effect_key")){
				value = sharedPreferences.getString(key,"sorbel");
	    		if (mPcam == null) return;
	    		mPcam.ChangeVideoEffect(value);
	    	} else if (key.contains("enable_tone_generator_key")){
	    		boolean valueBool = sharedPreferences.getBoolean(key,false);
	    		mAudioProc.updateToneGenerator(valueBool);
	    		value = String.valueOf(valueBool);
	    	}
			trackPreferenceChanged(key, value);
		}
	};
	

	public SettingsFragment setPurchaseManager(PurchaseManager pm) {
		
		mPurchaseManager = pm;
    	return this;
	}
	
	public SettingsFragment setAudioProcessor(AudioProcessor ap) {
		mAudioProc = ap;
		return this;
	}
	
	public SettingsFragment setPcam(Pcamera pc){
		mPcam = pc;
		return this;
	}
	
}
  

