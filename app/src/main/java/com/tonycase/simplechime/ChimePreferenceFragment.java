package com.tonycase.simplechime;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

@SuppressLint("NewApi")
public class ChimePreferenceFragment extends PreferenceFragment
        implements OnSharedPreferenceChangeListener {

    private final String TAG = getClass().getSimpleName();

    public ChimePreferenceFragment() {
    }

    /* (non-Javadoc)
     * @see android.preference.PreferenceFragment#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() ");
        addPreferencesFromResource(R.xml.userpreferences);
    }

    // Our handler for received Intents. This will be called whenever an Intent
// with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            setHourSummary(prefs);
        }
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        prefs.registerOnSharedPreferenceChangeListener(this);

        // Register to receive messages.
        // We are registering an observer (messageReceiver) to receive Intents
        // with actions named "custom-event-name".
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(messageReceiver,
                new IntentFilter(ChimeUtilities.HOURS_UPDATED_MSG));


        setSoundSummary(prefs);
        setRingtoneSummary(prefs);
        setSource(prefs);
        setActive(prefs);
        setHourSummary(prefs);
    }

    @Override
    public void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(messageReceiver);
        super.onDestroy();
    }

    // needed for after migration only
    private void setHourSummary(SharedPreferences sharedPreferences) {

        TimeRange timeRange = ChimeUtilities.getTimeRange(getActivity());
        if (timeRange.isAllDay()) {
            return;
        }
        String summary = HoursSelectDialog.summaryText(
                timeRange.getStart(), timeRange.getEnd(), getActivity());
        Preference timePref = findPreference(getString(R.string.pref_hours));
        timePref.setSummary(summary);
    }

    private void setActive(SharedPreferences sharedPreferences) {

        SwitchPreference sourcePref = (SwitchPreference) findPreference(getString(R.string.pref_source));
        ListPreference soundPref = (ListPreference) findPreference(getString(R.string.pref_sound));
        RingtonePreference ringtonePref =
                (RingtonePreference) findPreference(getString(R.string.pref_ringtone));
        Preference timePref = findPreference(getString(R.string.pref_hours));
        Preference volPref = findPreference(getString(R.string.pref_volume));

        boolean isOn = sharedPreferences.getBoolean(getString(R.string.pref_on_off), false);

        if (!isOn) {
            sourcePref.setEnabled(false);
            soundPref.setEnabled(false);
            ringtonePref.setEnabled(false);
            timePref.setEnabled(false);
            volPref.setEnabled(false);
        } else {
            sourcePref.setEnabled(true);
            soundPref.setEnabled(true);
            ringtonePref.setEnabled(true);
            timePref.setEnabled(true);
            volPref.setEnabled(true);
            // one must be off
            setSource(sharedPreferences);
        }
    }

    private void setSource(SharedPreferences sharedPreferences) {

        SwitchPreference sourcePref = (SwitchPreference) findPreference(getString(R.string.pref_source));
        ListPreference soundPref = (ListPreference) findPreference(getString(R.string.pref_sound));
        RingtonePreference ringtonePref =
                (RingtonePreference) findPreference(getString(R.string.pref_ringtone));
        boolean sourcePhone = sharedPreferences.getBoolean(getString(R.string.pref_source), false);
        if (sourcePhone) {
            sourcePref.setSummary(R.string.source_summary_phone);
            soundPref.setEnabled(false);
            ringtonePref.setEnabled(true);
        } else {
            sourcePref.setSummary(R.string.source_summary_app);
            soundPref.setEnabled(true);
            ringtonePref.setEnabled(false);
        }
    }



    private void setSoundSummary(SharedPreferences sharedPreferences) {

        ListPreference soundPref = (ListPreference) findPreference(getString(R.string.pref_sound));
        soundPref.setSummary(getString(R.string.sound_summary_base) + soundPref.getEntry());
    }

    private void setRingtoneSummary(SharedPreferences sharedPreferences) {

        String value = sharedPreferences.getString(getString(R.string.pref_ringtone), null);
        Uri ringtoneUri = value != null
                ? Uri.parse(value)
                : Settings.System.DEFAULT_NOTIFICATION_URI;

        Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), ringtoneUri);
        String name = ringtone.getTitle(getActivity());

        RingtonePreference ringtonePreference =
                (RingtonePreference) findPreference(getString(R.string.pref_ringtone));
        ringtonePreference.setSummary(getString(R.string.sound_summary_base) + name);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {

        if (getActivity() == null) {
            return;
        }

        if (key.equals(getString(R.string.pref_source))) {
            setSource(sharedPreferences);
        } else if (key.equals(R.string.pref_increment)) {

        } else if (key.equals(getString(R.string.pref_sound))) {

            if (getActivity() == null) {
                Log.i(TAG, "fragment not attached to activity.  returning");
                return;
            }

            ((MainActivity) getActivity()).playSoundShakeBell();
            setSoundSummary(sharedPreferences);

        } else if (key.equals(getString(R.string.pref_ringtone))) {

            if (getActivity() == null) {
                Log.i(TAG, "fragment not attached to activity.  returning");
                return;
            }
            setRingtoneSummary(sharedPreferences);
        } else if (key.equals(getActivity().getString(R.string.pref_on_off))
                   || key.equals(getString(R.string.pref_hours))) {

            setActive(sharedPreferences);

            boolean chimeOn = sharedPreferences.getBoolean(
                    getActivity().getString(R.string.pref_on_off), false);
            Activity act = getActivity();
            if (act == null) {
                Log.i(TAG, "zombie fragment, activity is null.");
                return;
            }

            if (chimeOn) {

                ChimeUtilities.startAlarm(getActivity());

                Toast toast = Toast.makeText(getActivity(), R.string.chime_is_on, Toast.LENGTH_SHORT);
                toast.show();
            } else {
                Toast toast = Toast.makeText(getActivity(), R.string.chime_is_off, Toast.LENGTH_SHORT);
                toast.show();
                AlarmManager amgr = (AlarmManager) act.getSystemService(Context.ALARM_SERVICE);

                String CHIME_ALARM_ACTION = getString(R.string.alarm_action); // "com.tonycase.chimealarm.CHIME_ALARM_ACTION"
                Intent intentToFire = new Intent(CHIME_ALARM_ACTION);
                PendingIntent pIntent = PendingIntent.getBroadcast(
                        getActivity(), 0, intentToFire, 0);
                amgr.cancel(pIntent);
            }
        }
    }
}

/*
Set alarm logic:
  -- Set repeating alarm for every hour, beginning next hour if currentHour >= startHour && currentHour < endHour
  --                                 else beginning first hour next day

  -- When receiving alarm, if it's the last hour of the day, cancel the alarm and start over the next day.


*/