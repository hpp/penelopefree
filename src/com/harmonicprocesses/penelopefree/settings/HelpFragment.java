package com.harmonicprocesses.penelopefree.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.harmonicprocesses.penelopefree.R;

public class HelpFragment  extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.help);
    }
    
    
    //adapted from http://proandroiddev.blogspot.com/2011/04/honeycomb-tip-1-preferencefragment.html
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen prefScreen, Preference pref){
    	super.onPreferenceTreeClick(prefScreen, pref);
    	if (pref.getKey().contains("devices_category_key")) {
    		// Display the fragment as the main content.
    		getFragmentManager().beginTransaction().replace(R.id.settingsFragmentView, 
    				new SubSettingsFragment().setArguments(R.xml.devices))
    			.addToBackStack(null).commit();
    		getActivity().setTitle("Devices");
    		return true;
        } else if (pref.getKey().contains("visualizations_category_key")) {
    		// Display the fragment as the main content.
    		getFragmentManager().beginTransaction().replace(R.id.settingsFragmentView, 
    				new SubSettingsFragment().setArguments(R.xml.visualizations))
    			.addToBackStack(null).commit();
    		getActivity().setTitle("Visualizations");
    		return true;
        }  else if (pref.getKey().contains("addons_category_key")) {
    		// Display the fragment as the main content.
    		getFragmentManager().beginTransaction().replace(R.id.settingsFragmentView, 
    				new SubSettingsFragment().setArguments(R.xml.addons))
    			.addToBackStack(null).commit();
    		getActivity().setTitle("Visualizations"); 
    		return true;
        }
    	return false;
    }
}