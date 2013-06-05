package com.harmonicprocesses.penelopefree.settings;
import com.harmonicprocesses.penelopefree.R;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SettingsFragment extends PreferenceFragment {
	private int mXmlId;
	private String mKey;
	private Resources mRes;
	private FragmentManager mFrag;
	private Activity mAct;
	
    public SettingsFragment(int xmlId) {
		mXmlId = xmlId;
	}


	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAct = getActivity();
        mRes = mAct.getResources();
        mFrag = getFragmentManager();
        // Load the preferences from an XML resource
        addPreferencesFromResource(mXmlId);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }
    
    
    //adapted from http://proandroiddev.blogspot.com/2011/04/honeycomb-tip-1-preferencefragment.html
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen prefScreen, Preference pref){
    	super.onPreferenceTreeClick(prefScreen, pref);
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
        }
    	return false;
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
			if (key.contains("input_device_key")||key.contains("output_device_key")){
				mKey = key;
				if (sharedPreferences.getString(key, "").contains(mRes.getString(R.string.inUSB_default))||
					sharedPreferences.getString(key, "").contains(mRes.getString(R.string.outUSB_default))){
					new UpSaleDialog(R.string.dialog_penelope_full_messsage_usb).show(mFrag,"PaidForVersionDialog");
				}
			} else if (key.contains("turn_on_accelerometer_key")||key.contains("vis_number_of_particles_key")||
					key.contains("invert_audio_key")||key.contains("enable_reverb_key")||
					key.contains("sound_buffer_size_key")||key.contains("sound_wet_dry_key")) {
				new UpSaleDialog(R.string.dialog_restart_simulation, R.string.dialog_button_ok)
					.show(mFrag,"RestartSimulationDialog");
			} 
			
		}
	};

}
  

