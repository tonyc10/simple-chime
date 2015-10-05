package com.tonycase.simplechime;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ChimeAlarmReceiver extends BroadcastReceiver {
	
	private final String TAG = "Chime Receiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		
		Log.d(TAG, "onReceive");

        // We need a calendar set to current time for a couple operations here
        Calendar cal = new GregorianCalendar();

        // check the hour
        HourState hourState = determineHourState(context, cal);

        if (hourState == HourState.INVALID) {
            return;
        }

        // If the user happens to be looking at their phone
        makeToast(context, cal);

        // Play the chime sound
		ChimeUtilities.playSound(context);

        // cancel and reset for tomorrow if needed
        if (hourState == HourState.RESET_FOR_TOMORROW) {
            Log.i(TAG, "Resetting for tomorrow");
             ChimeUtilities.startAlarm(context);    // cancels existing alarm, starts new one for tomorrow a.m.
        }
	}

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
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
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