package com.tonycase.simplechime;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Receiver called with intent called on the AlarmManager.
 * Makes toast, plays sounds, and if applicable schedules next alarm.
 */
public class ChimeAlarmReceiver extends WakefulBroadcastReceiver {

    private final String TAG = "Chime Receiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int offset = prefs.getInt(context.getString(R.string.pref_offset), 0);

        Log.d(TAG, "onReceive");

        // We need a calendar set to current time for a couple operations here
        Calendar cal = new GregorianCalendar();

        // check the hour
        HourState hourState = determineHourState(context, cal);

        if (hourState == HourState.INVALID) {
            return;
        }

        // only chime if we got it right
        int minute = (60 + cal.get(Calendar.MINUTE) - offset) % 60;
        boolean accurate = minute < 3 || minute > 58;

        if (accurate) {
            // If the user happens to be looking at their phone
            makeToast(context, cal);

            // Play the chime sound, with wakeful intent pattern
            int volume = prefs.getInt(context.getString(R.string.pref_volume), 5);
            Intent serviceIntent = ChimeUtilities.getPlaySoundIntent(context, volume);
            startWakefulService(context, serviceIntent);

            if (BuildConfig.DEBUG) {
                DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
                String message = "Chime at " + df.format(cal.getTime());
                sendNotification(context, message);
            }
        } else {
            if (BuildConfig.DEBUG) {
                DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
                String message = "NOT accurate, no chime at " + df.format(cal.getTime());
                sendNotification(context, message);
            }
        }

        // cancel and reset for tomorrow if needed
        if (hourState == HourState.RESET_FOR_TOMORROW
                   // With Kitkat and later, we don't use repeating alarm, so reset everytime.
                || Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            Log.d(TAG, "Resetting for tomorrow");
            ChimeUtilities.startAlarm(context);    // cancels existing alarm, starts new one for tomorrow a.m.
        }
    }

    private void sendNotification(Context context, String text) {
        long time = System.currentTimeMillis();

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context);

        mBuilder.setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Simple Chime")
                        .setContentText(text);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, MainActivity.class);

        // not sure I need this stackbuilder stuff.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Log.d("ChimeAlarmReceiver", "sending notification to notificationManager: " + mBuilder);

        int id = (int) (time/10000);
        mNotificationManager.notify(id, mBuilder.build());
    }

    /** enum for specifying what kind of hour this is:  an hour to play the chime (normal), and hour to
     *  not play the chime (invalid) or an hour to reschedule the chime (last chime of the day).
     */
    enum HourState {

        INVALID,
        RESET_FOR_TOMORROW,
        NORMAL;
    }

    private HourState determineHourState(Context context, Calendar cal) {

        TimeRange timeRange = ChimeUtilities.getTimeRange(context);

        int start = timeRange.getStart();
        int end = timeRange.getEnd();

        int hour = cal.get(Calendar.HOUR_OF_DAY);
        Log.d(TAG, "Hour of day, start and end are: " + hour + ", " + start + ", " + end);

        if (timeRange.isInverseMode()) {
            if (hour < start && hour > end)
                return HourState.INVALID;

            if (hour == end) {
                return HourState.RESET_FOR_TOMORROW;
            }
        } else {
            if (hour < start || hour > end)
                return HourState.INVALID;

            if (hour == end) {
                return HourState.RESET_FOR_TOMORROW;
            }
        }
        return HourState.NORMAL;
    }

    private void makeToast(Context context, Calendar cal) {

        // hh:mm a
        DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
        String message = df.format(cal.getTime());
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, message, duration);
        toast.setGravity(Gravity.CENTER, 0, 0);

        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.HORIZONTAL);

        TextView tv = new TextView(context);
        //LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(5, 5, 5, 5);
        tv.setText(message);

        ImageView iv = new ImageView(context);
        iv.setImageDrawable(context.getResources().getDrawable(R.drawable.bell_launcher));

        ll.setBackgroundColor(Color.BLACK);
        ll.addView(iv);
        ll.addView(tv);

        toast.setView(ll);
        toast.show();
    }
}

/*
  Time Interval:
  Regular mode:
    7am - 6pm
    hour < 7 (invalid)
    hour = 7 (normal)
    hour > 7, < 18 (normal)
    hour = 18 (reset)
    hour > 18 (invalid)

  Inverse mode:
     9 am - 1am
     hour < 9, > 1, (invalid)
     hour == 1 (reset)
     else (normal)
 */