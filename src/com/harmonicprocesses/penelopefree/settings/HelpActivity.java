package com.harmonicprocesses.penelopefree.settings;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;

import com.harmonicprocesses.penelopefree.R;

public class HelpActivity extends Activity {

		private HelpFragment mHelpFragment;
		
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
	        
			setContentView(R.layout.activity_settings);
			
			// Display the fragment as the main content.
			mHelpFragment = new HelpFragment();
	        getFragmentManager().beginTransaction()
	                .replace(R.id.settingsFragmentView, mHelpFragment)
	                .commit();	
	        //mSettingsFragment.addPreferencesFromResource(R.xml.settings);
	        this.setTitle("Settings");
		}
}
