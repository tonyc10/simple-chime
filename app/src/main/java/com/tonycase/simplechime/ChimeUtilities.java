package com.tonycase.simplechime;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

/**
 * Static utility methods for chime app
 */
public final class ChimeUtilities {


    public static void playSound(Context context) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int volume = prefs.getInt("PREF_VOLUME", 5);
        playSound(context, volume);
    }

    public static void playSound(Context context, int volume) {

        // play sound
        Intent intentNew = new Intent(context, PlaySoundIntentService.class);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String uriStr = null;
        boolean sourcePhone = prefs.getBoolean(context.getString(R.string.pref_source), false);
        if (sourcePhone) {
            uriStr = prefs.getString("PREF_NOTIFICATION", null);
        }
        if (uriStr == null) {
            String uriPref = prefs.getString("PREF_SOUND", String.valueOf(R.raw.clong1));
            int resID = context.getResources().getIdentifier(uriPref , "raw", context.getPackageName());
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

    public static void startAlarm(Context act) {

        AlarmManager amgr = (AlarmManager) act.getSystemService(Context.ALARM_SERVICE);
        int alarmType = AlarmManager.RTC_WAKEUP;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(act);
        String timesStr = prefs.getString("PREF_HOURS", null);

        Calendar cal = new GregorianCalendar();
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        int start = 0;
        int end = 23;

        if (timesStr != null) {
            try {
                String[] hoursArray = timesStr.split(" ");
                start = Integer.parseInt(hoursArray[0]);
                end = Integer.parseInt(hoursArray[1]);
            } catch (Exception ex) {
                Log.w("Chime startAlarm()", "Unable to parse time string", ex);
            }
        }

        String CHIME_ALARM_ACTION = act.getString(R.string.alarm_action); // "com.tonycase.chimealarm.CHIME_ALARM_ACTION"
        Intent intentToFire = new Intent(CHIME_ALARM_ACTION);
        PendingIntent pIntent = PendingIntent.getBroadcast(act, 0, intentToFire, 0);

        // remove any existing alarms
        amgr.cancel(pIntent);

        // figure out time of new one
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);

        if (hour < start) {
            // set alarm for start hour
            cal.set(Calendar.HOUR_OF_DAY, start);
        }
        else if (hour >= end+12) {
            // set alarm for start hour tomorrow
            cal.set(Calendar.HOUR_OF_DAY, start);
            cal.setTimeInMillis(cal.getTimeInMillis() + TimeUnit.DAYS.toMillis(1));
        } else {
            // set alarm to start next hour
            cal.set(Calendar.HOUR_OF_DAY, hour+1);
        }

        Log.i("Chime Main", "Next alarm will go off at " + cal.getTime());
        long toStartHour = cal.getTimeInMillis(); // Api-9:
        long oneHourTimeStep = TimeUnit.HOURS.toMillis(1);
        amgr.setRepeating(alarmType, toStartHour, oneHourTimeStep, pIntent);
    }
}
