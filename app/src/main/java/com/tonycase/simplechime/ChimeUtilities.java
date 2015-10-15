package com.tonycase.simplechime;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

/**
 * Static utility methods for chime app
 */
public final class ChimeUtilities {

    public static final String HOURS_UPDATED_MSG = "hour_updated_msg";

    private static final String TAG = "ChimeUtilities";
    private static final String DEFAULT_HOURS_PREF = "7 21";

    /**
     * Get simple time range object representing the hours the chime is on.
     */
    public static TimeRange getTimeRange(Context context) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // If user does not have specific hours set, or turns hours off, return ALL_DAY
        boolean allDay = prefs.getBoolean(context.getString(R.string.pref_all_day), true);
        if (allDay) {
            return TimeRange.ALL_DAY;
        }

        String timesStr = prefs.getString(context.getString(R.string.pref_hours),
                DEFAULT_HOURS_PREF);

        // Should not have been set to null, except in previous (1.0) version.
        if (timesStr != null) {
            try {
                String[] hoursArray = timesStr.split(" ");
                int start = Integer.parseInt(hoursArray[0]);
                int end = Integer.parseInt(hoursArray[1]);

                return new TimeRange(start, end);
            } catch (Exception ex) {
                Log.w(TAG, "Unable to parse time string", ex);
                // log and fall through to ALL_DAY
            }
        }
        return TimeRange.ALL_DAY;   // all hours
    }

    /**
     * Check to see if app upgrade has occurred by checking for existance of new preference.  If so,
     * this performs the necessary migration and sends a LocalBroadcast when complete.
     */
    public static void checkAndPerformUpgrade(SharedPreferences prefs, Context context) {

        // This code pertains to upgrade from 1.0 to 1.1

        // check for existence of new preference.  If it's here, we've already upgraded.
        if (prefs.contains(context.getString(R.string.pref_all_day))) {
            // preferences have already been updated
            Log.d(TAG, "no need to migrate, already happened");
            return;
        }

        // move volume up, since we've really adjusted the volume downward with this release.
        int vol = prefs.getInt(context.getString(R.string.pref_volume), 10);
        int newVol = vol + 2;
        newVol = Math.min(newVol, 10);
        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(context.getString(R.string.pref_volume), newVol).commit();

        // Don't bother migrating time range data if timeRange.isAllDay, because in the previous version this
        // means there is no time range.
        TimeRange timeRange = ChimeUtilities.getTimeRange(context);
        if (timeRange.isAllDay()) {
            // there is no data for time range, so no need to migrate
            Log.d(TAG, "no need to migrate, is all day");
            return;
        }

        // The user is on a time range, so switch new preference boolean chime-all-day to off
        editor.putBoolean(context.getString(R.string.pref_all_day), false).commit();

        // adjust time range of end time fro 0-11 to 12-23.
        int start = timeRange.getStart();
        int end = timeRange.getEnd();
        if (end < 12) {
            end += 12;
            // persist new values
            String value = start + " " + end;
            Log.d(TAG, "Migrating to " + value);
            editor.putString(context.getString(R.string.pref_hours), value).commit();

            // local broadcast so UI can be properly updated.
            sendMessage(context);
        }
    }

    // Send an Intent with an action named "custom-event-name". The Intent sent should
    // be received by the ReceiverActivity.
    private static void sendMessage(Context context) {
        Log.d("sender", "Broadcasting message");
        Intent intent = new Intent(HOURS_UPDATED_MSG);
        // no extra data.  (e.g. intent.putExtra("message", "This is my message!"));
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Play a user's stored selected sound at the user's stored volume preference.
     */
    public static void playSound(Context context) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int volume = prefs.getInt(context.getString(R.string.pref_volume), 5);
        playSound(context, volume);
    }

    /**
     * Play a user's stored selected sound at the given volume.
     */
    public static void playSound(Context context, int volume) {

        // play sound
        Intent intentNew = new Intent(context, PlaySoundIntentService.class);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String uriStr = null;
        boolean sourcePhone = prefs.getBoolean(context.getString(R.string.pref_source), false);
        if (sourcePhone) {
            uriStr = prefs.getString(context.getString(R.string.pref_ringtone), null);
        }
        if (uriStr == null) {
            String uriPref = prefs.getString(context.getString(R.string.pref_sound),
                    String.valueOf(R.raw.clong1));
            int resID = context.getResources().getIdentifier(uriPref, "raw", context.getPackageName());
            uriStr = Uri.parse("android.resource://com.tonycase.simplechime/" + resID).toString();
        }

        if (uriStr == null) {
            return;
        }

        intentNew.putExtra(PlaySoundIntentService.URI_KEY, uriStr);
        intentNew.putExtra(PlaySoundIntentService.VOL_KEY, volume);
        intentNew.putExtra(PlaySoundIntentService.PLAY_SOFTER_KEY, !sourcePhone);
        context.startService(intentNew);
    }

    /** Set up alarms with the Alarm Manager, using the user's stored preferences to get the proper
     * hour (today or tomorrow) on which to start.
     */
    public static void startAlarm(Context context) {

        Log.d(TAG, "Setting alarm, current time is " + new Date());

        AlarmManager amgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        int alarmType = AlarmManager.RTC_WAKEUP;

        TimeRange timeRange = getTimeRange(context);
        Log.v(TAG, "Current time range is " + timeRange);

        Calendar cal = new GregorianCalendar();
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        String CHIME_ALARM_ACTION = context.getString(R.string.alarm_action); // "com.tonycase.chimealarm.CHIME_ALARM_ACTION"
        Intent intentToFire = new Intent(CHIME_ALARM_ACTION);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, intentToFire, 0);

        // remove any existing alarms
        amgr.cancel(pIntent);

        // figure out time of new one
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);

        int start = timeRange.getStart();
        int end = timeRange.getEnd();

        // Inverse mode is when the user chooses a time range going past midnight.  E.g. 8 am to 2 am.
        if (timeRange.isInverseMode()) {
            // In inverse mode, end is start and start is end, so..
            // we're chiming if hour is less than or equal to start, or greater than or equal to end
            if (hour <= end || hour >= start) {
                // handle special case, end of day
                if (hour == 23) {
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.setTimeInMillis(cal.getTimeInMillis() + TimeUnit.DAYS.toMillis(1));
                } else {
                    // set alarm to start next hour
                    cal.set(Calendar.HOUR_OF_DAY, hour + 1);
                }
            } else {
                // jump ahead to end (which in inverse, is start time)
                cal.set(Calendar.HOUR_OF_DAY, end);
            }
        } else {
            if (hour < start) {
                // set alarm for start hour
                cal.set(Calendar.HOUR_OF_DAY, start);
            } else if (hour >= end) {
                // set alarm for start hour tomorrow
                cal.set(Calendar.HOUR_OF_DAY, start);
                cal.setTimeInMillis(cal.getTimeInMillis() + TimeUnit.DAYS.toMillis(1));
            } else {
                // set alarm to start next hour
                cal.set(Calendar.HOUR_OF_DAY, hour + 1);
            }
        }

        Log.d("Chime Main", "Next alarm will go off at " + cal.getTime());
        long toStartHour = cal.getTimeInMillis(); // Api-9:
        long oneHourTimeStep = TimeUnit.HOURS.toMillis(1);
        amgr.setRepeating(alarmType, toStartHour, oneHourTimeStep, pIntent);
    }
}
