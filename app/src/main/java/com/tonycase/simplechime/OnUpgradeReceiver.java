package com.tonycase.simplechime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class OnUpgradeReceiver extends BroadcastReceiver {

    private static final String TAG = OnUpgradeReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // check for existence of new preference
        if (prefs.contains(context.getString(R.string.pref_all_day))) {
            // preferences have already been updated
            Log.d(TAG, "no need to migrate, already happened");
            return;
        }

        // migrate data.
        TimeRange timeRange = ChimeUtilities.getTimeRange(context);
        if (timeRange.isAllDay()) {
            // there is no data for time range, so no need to migrate
            Log.d(TAG, "no need to migrate, is all day");
            return;
        }

        // switch new preference boolean chimeAllDay to off.
        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(context.getString(R.string.pref_all_day), false).commit();

        // adjust time range of end time to 0-23.
        int start = timeRange.getStart();
        int end = timeRange.getEnd();
        if (end < 12) {
            end += 12;
            // persist new values
            String value = start + " " + end;
            Log.d(TAG, "Migrating to " + value);
            // if preference is On, reset alarm.
            editor.putString(context.getString(R.string.pref_hours), value).commit();
        }
    }
}
