package com.tonycase.simplechime;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class HoursSelectDialog extends DialogPreference {

    private final String TAG = "Chime HoursSelectDialog";

    private RadioButton allDayRB;
    private RadioButton specifyRB;
    private Spinner startHourDropdown;
    private Spinner endHourDropdown;
    private TextView intermedText;

    public HoursSelectDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HoursSelectDialog(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {

        Log.d(TAG, "init() ");

        setDialogLayoutResource(R.layout.time_picker_preference);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        setDialogIcon(null);
    }

    /* (non-Javadoc)
     * @see android.preference.DialogPreference#onBindDialogView(android.view.View)
     */
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        Log.d(TAG, "onBindDialogView() ");
        allDayRB = (RadioButton) view.findViewById(R.id.radioButton1);
        specifyRB = (RadioButton) view.findViewById(R.id.radioButton2);

        startHourDropdown = (Spinner) view.findViewById(R.id.editText1);
        endHourDropdown = (Spinner) view.findViewById(R.id.editText2);

        ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(getContext(),
                R.array.hours, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        startHourDropdown.setAdapter(adapter1);

        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(getContext(),
                R.array.hours, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        startHourDropdown.setAdapter(adapter2);

        intermedText = (TextView) view.findViewById(R.id.tpp_textView1);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean allDay = prefs.getBoolean(getContext().getString(R.string.pref_all_day), true);

        TimeRange timeRange = ChimeUtilities.getTimeRange(getContext());

        int start = timeRange.getStart();
        int end = timeRange.getEnd();

        if (allDay) {
            allDayRB.setChecked(true);
            specifyRB.setChecked(false);
            startHourDropdown.setEnabled(false);
            endHourDropdown.setEnabled(false);
            intermedText.setEnabled(false);
            setSummary("Every Hour All Day");
        } else {
            allDayRB.setChecked(false);
            specifyRB.setChecked(true);
            startHourDropdown.setEnabled(true);
            endHourDropdown.setEnabled(true);
            intermedText.setEnabled(true);

            setSummary(summaryText(start, end, getContext()));
            startHourDropdown.requestFocus();
        }

        startHourDropdown.setSelection(start);
        endHourDropdown.setSelection(end);

        allDayRB.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                allDayClicked();
            }

        });
        specifyRB.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                specifyClicked();
            }
        });
    }

    private void allDayClicked() {
        allDayRB.setChecked(true);
        specifyRB.setChecked(false);
        startHourDropdown.setEnabled(false);
        endHourDropdown.setEnabled(false);
        intermedText.setEnabled(false);
    }

    private void specifyClicked() {
        allDayRB.setChecked(false);
        specifyRB.setChecked(true);
        startHourDropdown.setEnabled(true);
        endHourDropdown.setEnabled(true);
        intermedText.setEnabled(true);

        startHourDropdown.requestFocus();
    }


    /* (non-Javadoc)
     * @see android.preference.DialogPreference#onDialogClosed(boolean)
     */
    @Override
    protected void onDialogClosed(boolean positiveResult) {

        Log.d(TAG, "onDialogClosed");

        if (positiveResult) {

            persistAllDay(allDayRB.isChecked());
            if (allDayRB.isChecked()) {
                setSummary("Every Hour All Day");
            } else {
                // validate here
                int start = startHourDropdown.getSelectedItemPosition();
                int end = endHourDropdown.getSelectedItemPosition();
                Log.d(TAG, "Dialog closed, start and end are " + start + ", " + end);
                if (start < 0 || start > 23 || end < 0 || end > 23)
                    throw new IllegalStateException("Save hours are not legit");
                String value = start + " " + end;
                persistString(value);
                setSummary(summaryText(start, end, getContext()));
            }
            notifyChanged();
        }
        super.onDialogClosed(positiveResult);
    }

    private void persistAllDay(boolean allDay) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(getContext().getString(R.string.pref_all_day), allDay).commit();
    }

    public static String summaryText(int start, int end, Context context) {
        Calendar startCal = new GregorianCalendar();
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.HOUR_OF_DAY, start);

        Calendar endCal = new GregorianCalendar();
        endCal.set(Calendar.SECOND, 0);
        endCal.set(Calendar.MINUTE, 0);
        endCal.set(Calendar.HOUR_OF_DAY, end);

        DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
        return context.getString(R.string.every_hour_from)
                + dateFormat.format(startCal.getTime()) + " "
                + context.getString(R.string.to) + " "
                + dateFormat.format(endCal.getTime());
    }

}
