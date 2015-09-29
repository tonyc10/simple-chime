package com.tonycase.simplechime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Restarts the alarm, if it's turned on, after reboot.
 */
public class BootCompletedReceiver extends BroadcastReceiver {
	
	private final String TAG = getClass().getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {

		// if preference is On, reset alarm.
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context); 
		boolean chimeOn = prefs.getBoolean(context.getString(R.string.pref_on_off), false);
		
		if (chimeOn) {
            ChimeUtilities.startAlarm(context);
		}

	}


}