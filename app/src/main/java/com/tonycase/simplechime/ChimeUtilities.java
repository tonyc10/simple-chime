package com.tonycase.simplechime;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.SynchronousQueue;
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
     * Get simple time range object representing the hours the chime is on.
     */
    public static TimeRange getTimeRangeOldVersion(Context context) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String timesStr = prefs.getString(context.getString(R.string.pref_hours),
                null);

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
     * Check to see if app upgrade has occurred by checking for existence of new preference.  If so,
     * this performs the necessary migration and sends a LocalBroadcast when complete.
     */
    public static void checkAndPerformUpgrade(SharedPreferences prefs, Context context) {

        // This code pertains to upgrade from 1.0 to 1.1

        // check for existence of new preference.  If it's here, we've already upgraded.
        if (prefs.contains(context.getString(R.string.pref_all_day))) {
            // preferences have already been updated
            Log.d(TAG, "no need to migrate, already happened");
            Toast.makeText(context, "No need to migrate!", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(context).edit();

        // move volume up, since we've really adjusted the volume downward with this release.
        int vol = prefs.getInt(context.getString(R.string.pref_volume), 10);
        if (vol < 5) {
            int newVol = vol + 2;
            newVol = Math.min(newVol, 10);
            editor.putInt(context.getString(R.string.pref_volume), newVol).commit();
        }


        // Don't bother migrating time range data if timeRange.isAllDay, because in the previous version this
        // means there is no time range.
        TimeRange timeRange = ChimeUtilities.getTimeRangeOldVersion(context);
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
        Intent intentNew = getPlaySoundIntent(context, volume);

        if (intentNew == null) {
            return;
        }

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
        int currentHour = cal.get(Calendar.HOUR_OF_DAY);
        int currentMinute = cal.get(Calendar.MINUTE);

        // the current time of day, in minutes
        int currentTimeMinutes = currentHour*60 + currentMinute;

        String CHIME_ALARM_ACTION = context.getString(R.string.alarm_action); // "com.tonycase.chimealarm.CHIME_ALARM_ACTION"
        Intent intentToFire = new Intent(CHIME_ALARM_ACTION);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, intentToFire, 0);

        // remove any existing alarms
        amgr.cancel(pIntent);

        // second always zero
        cal.set(Calendar.SECOND, 0);

        // minute zero too, unless offset set.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int offsetMinutes = prefs.getInt(context.getString(R.string.pref_offset), 0);

        cal.set(Calendar.MINUTE, 0);

        int startHour = timeRange.getStart();
        // the time of the day to start the chime, in minutes
        int startTimeMinutes = startHour*60;

        int endHour = timeRange.getEnd();
        // the time of the day to start the chime, in minutes
        int endTimeMinutes = endHour*60;


        // flat to set whether we are setting chime for this upcoming hour.  Needed for minutes-offset logic
        boolean chimeActiveNow = false;

        // Inverse mode is when the user chooses a time range going past midnight.  E.g. 8 am to 2 am.
        if (timeRange.isInverseMode()) {
            // In inverse mode, end is start and start is end, so..
            // we're chiming if hour is less than or equal to start, or greater than or equal to end
            if (currentTimeMinutes <= endTimeMinutes || currentTimeMinutes >= startTimeMinutes) {
                chimeActiveNow = true;
                // handle special case, end of day
                if (currentHour == 23) {
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.setTimeInMillis(cal.getTimeInMillis() + TimeUnit.DAYS.toMillis(1));
                } else {
                    // set alarm to start next hour
                    cal.set(Calendar.HOUR_OF_DAY, currentHour + 1);
                }
            } else {
                // jump ahead to end (which in inverse, is start time)
                cal.set(Calendar.HOUR_OF_DAY, endHour);
            }
        } else {
            // regular mode.  Start hour comes before ending mode

            if (currentTimeMinutes < startTimeMinutes) {
                // we haven't reached chimes hours yet.  Set alarm for start hour
                cal.set(Calendar.HOUR_OF_DAY, startHour);
            } else if (currentTimeMinutes >= endTimeMinutes) {
                // After chime hours.  Set alarm for start hour tomorrow
                cal.set(Calendar.HOUR_OF_DAY, startHour);
                cal.setTimeInMillis(cal.getTimeInMillis() + TimeUnit.DAYS.toMillis(1));
            } else {
                chimeActiveNow = true;
                // During chime hours.  Set alarm to start next hour
                cal.set(Calendar.HOUR_OF_DAY, currentHour + 1);
            }
        }
//        cal.set(Calendar.MINUTE, offsetMinutes);

        long startTime = cal.getTimeInMillis(); // Api-9:
        long oneHourTimeStep = TimeUnit.HOURS.toMillis(1);

        // apply offset
        startTime += TimeUnit.MINUTES.toMillis(offsetMinutes);
        // handle case where first time alarm set for *before* current time, or more than an hour away due
        //   to offset
        long now = System.currentTimeMillis();
        if (chimeActiveNow) {
            if (now > startTime) {
                startTime += TimeUnit.HOURS.toMillis(1);
            }
            else if (now < startTime - TimeUnit.HOURS.toMillis(1)) {
                startTime -= TimeUnit.HOURS.toMillis(1);
            }
        }
        Log.d("Chime Main", "Next alarm will go off at " + new Date(startTime));

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            // we can safely set a repeating alamr before KitKat -- should get delivered exactly on time.
            amgr.setRepeating(alarmType, startTime, oneHourTimeStep, pIntent);
        } else {
            // with KitKat, need to specify new "Exact" API, non-repeating, to get exact results.
            amgr.setExact(alarmType, startTime, pIntent);
        }
    }

    /** Cancel the alarm if it's set.  Otherwise, no effect. */
    public static void cancelAlarm(Activity activity) {
        AlarmManager amgr = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);

        String CHIME_ALARM_ACTION = activity.getString(R.string.alarm_action); // "com.tonycase.chimealarm.CHIME_ALARM_ACTION"
        Intent intentToFire = new Intent(CHIME_ALARM_ACTION);
        PendingIntent pIntent = PendingIntent.getBroadcast(
                activity, 0, intentToFire, 0);
        amgr.cancel(pIntent);
    }

    public static Intent getPlaySoundIntent(Context context, int volume) {

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
            return null;
        }

        intentNew.putExtra(PlaySoundIntentService.URI_KEY, uriStr);
        intentNew.putExtra(PlaySoundIntentService.VOL_KEY, volume);
        intentNew.putExtra(PlaySoundIntentService.PLAY_SOFTER_KEY, !sourcePhone);

        return intentNew;
    }
}
