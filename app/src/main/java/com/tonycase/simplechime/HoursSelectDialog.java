package com.tonycase.simplechime;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
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

	public static final String DEFAULT_VALUE = null;

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
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext()); 
		String timesStr = prefs.getString("PREF_HOURS", DEFAULT_VALUE);
		
		Log.i(TAG, "Starting up HoursSelectpreference, prev pref is " + timesStr);
	
		if (timesStr == null) {
			setSummary("Every Hour All Day");
		} else {
            TimeRange timeRange = ChimeUtilities.getTimeRange(getContext());
		    setSummary(summaryText(timeRange.getStart(), timeRange.getEnd()));
		}
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
// Apply the adapter to the spinner
		startHourDropdown.setAdapter(adapter1);

		ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(getContext(),
				R.array.hours, android.R.layout.simple_spinner_item);
		// Specify the layout to use when the list of choices appears
		adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the spinner
		startHourDropdown.setAdapter(adapter2);

		intermedText = (TextView) view.findViewById(R.id.tpp_textView1);

        TimeRange timeRange = ChimeUtilities.getTimeRange(getContext());

		if (timeRange.isAllDay()) {
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

            int start = timeRange.getStart();
            int end = timeRange.getEnd();

            startHourDropdown.setSelection(start);
            endHourDropdown.setSelection(end);
            setSummary(summaryText(start, end));
			startHourDropdown.requestFocus();
		}

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
	 * @see android.preference.Preference#onCreateView(android.view.ViewGroup)
	 */
	@Override
	protected View onCreateView(ViewGroup parent) {
		Log.d(TAG, "onCreateView() ");
		return super.onCreateView(parent);
	}

	/* (non-Javadoc)
	 * @see android.preference.DialogPreference#onDialogClosed(boolean)
	 */
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		
		Log.d(TAG, "onDialogClosed");
		
		if (positiveResult) {
			if (allDayRB.isChecked()) {
				persistString(null);
				notifyChanged();
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
					notifyChanged();
					setSummary(summaryText(start, end));
			}
		}
		super.onDialogClosed(positiveResult);
	}

	private String summaryText(int start, int end) {
		Calendar startCal = new GregorianCalendar();
		startCal.set(Calendar.SECOND, 0);
		startCal.set(Calendar.MINUTE, 0);
		startCal.set(Calendar.HOUR_OF_DAY, start);

		Calendar endCal = new GregorianCalendar();
		endCal.set(Calendar.SECOND, 0);
		endCal.set(Calendar.MINUTE, 0);
		endCal.set(Calendar.HOUR_OF_DAY, end);

		DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
		return getContext().getString(R.string.every_hour_from)
                + dateFormat.format(startCal.getTime()) + " "
				+ getContext().getString(R.string.to)   + " "
                + dateFormat.format(endCal.getTime());
	}

}
