package com.bitle.android.voipsms.activities;

import com.bitle.android.voipsms.R;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

public class UserPreferenceActivity extends PreferenceActivity {
	private GoogleAnalyticsTracker mTracker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mTracker = GoogleAnalyticsTracker.getInstance();
        mTracker.start("UA-23922796-1", this);
		
		tryToLoadPhoneNumber();
		
		
		addPreferencesFromResource(R.xml.preference);
		addListeners();
	}

	private void addListeners() {
		Preference phoneFromPref = findPreference("phoneNumberFrom");
		phoneFromPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				mTracker.trackEvent("Preferences", "Phone Number", "updated", 0);
				return true;
			}
		});
		
		Preference saveInThreadsPref = findPreference("putInThreads");
		saveInThreadsPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				mTracker.trackEvent("Preferences", "Save in threads", newValue.toString(), 0);
				return true;
			}
		});
	}

	@Override
	protected void onPause() {
		mTracker.dispatch();
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		mTracker.trackPageView("/properties");
		super.onResume();
	}
	
	@Override
	protected void onDestroy() {
		mTracker.stop();
		super.onDestroy();
	}

	private void tryToLoadPhoneNumber() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String phoneNumber = prefs.getString("phoneNumberFrom", null);
		
		if (phoneNumber == null) {
			TelephonyManager tMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			phoneNumber = tMgr.getLine1Number();
			
			prefs.edit().putString("phoneNumberFrom", phoneNumber).commit();
		}
	}
}
