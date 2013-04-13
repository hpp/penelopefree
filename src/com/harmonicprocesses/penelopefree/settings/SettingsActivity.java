package com.harmonicprocesses.penelopefree.settings;

import com.harmonicprocesses.penelopefree.PenelopeMainActivity;
import com.harmonicprocesses.penelopefree.R;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SettingsActivity extends Activity {

	private SettingsFragment mSettingsFragment;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
		setContentView(R.layout.activity_settings);
		
		Intent intent = getIntent();
		int xmlId = intent.getIntExtra(PenelopeMainActivity.EXTRA_MESSAGE, R.xml.settings);
		// Display the fragment as the main content.
		mSettingsFragment = new SettingsFragment(xmlId);

				
        getFragmentManager().beginTransaction()
                .replace(R.id.settingsFragmentView, mSettingsFragment)
                .commit();	
        //mSettingsFragment.addPreferencesFromResource(R.xml.settings);
        this.setTitle("Settings");
	}
}
