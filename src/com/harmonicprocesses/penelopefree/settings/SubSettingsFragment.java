package com.harmonicprocesses.penelopefree.settings;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.harmonicprocesses.penelopefree.R;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
class SubSettingsFragment extends PreferenceFragment {
	
	private int mXmlId;
	
	public SubSettingsFragment setArguments(int xmlId) {
		mXmlId = xmlId;
		return this;
	};
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(mXmlId);
    }
}
