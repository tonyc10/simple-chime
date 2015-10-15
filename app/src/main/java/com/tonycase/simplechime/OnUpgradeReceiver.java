package com.tonycase.simplechime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by tonycase on 10/6/15.
 */
public class OnUpgradeReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        // migrate data from 1.0 to 1.1
        ChimeUtilities.checkAndPerformUpgrade(sharedPreferences, context);

        // reset alarm
        boolean chimeOn = sharedPreferences.getBoolean(context.getString(R.string.pref_on_off), false);
        if (chimeOn) {
            ChimeUtilities.startAlarm(context);
        }
    }
}
