package com.tonycase.simplechime;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class HoursSelectDialog extends DialogPreference {
	
	private final String TAG = "Chime HoursSelectDialog";

	public static final String DEFAULT_VALUE = null;

	private RadioButton allDayRB;
	private RadioButton specifyRB;
	private EditText startHourTF;
	private EditText endHourTF;
	private TextView intermedText;
	private TextView endText;
	
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
			try {
				String[] hoursArray = timesStr.split(" ");
				int start = Integer.parseInt(hoursArray[0]);
				int end = Integer.parseInt(hoursArray[1]);
				if (start < 1 || start > 12 || end < 1 || end > 12) throw new IllegalStateException("Save numbers are not legit");
				setSummary("Every Hour from: " + start + ":00 AM to " + end + ":00 PM");
			} catch (Exception ex) {
			}
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

		startHourTF = (EditText) view.findViewById(R.id.editText1);
		endHourTF = (EditText) view.findViewById(R.id.editText2);
		
		intermedText = (TextView) view.findViewById(R.id.tpp_textView1);
		endText = (TextView) view.findViewById(R.id.tpp_textView2);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext()); 

		String timesStr = prefs.getString("PREF_HOURS", DEFAULT_VALUE);
		
		Log.i(TAG, "TC:  Starting up HoursSelectpreference, prev pref is " + timesStr);
	
		if (timesStr == null) {
			allDayRB.setChecked(true);
			specifyRB.setChecked(false);
			startHourTF.setEnabled(false);
			endHourTF.setEnabled(false);
			intermedText.setEnabled(false);
			endText.setEnabled(false);
			setSummary("Every Hour All Day");
		} else {
			
			allDayRB.setChecked(false);
			specifyRB.setChecked(true);
			startHourTF.setEnabled(true);
			endHourTF.setEnabled(true);
			intermedText.setEnabled(true);
			endText.setEnabled(true);
			
			try {
				String[] hoursArray = timesStr.split(" ");
				int start = Integer.parseInt(hoursArray[0]);
				int end = Integer.parseInt(hoursArray[1]);
				if (start < 1 || start > 12 || end < 1 || end > 12) throw new IllegalStateException("Save numbers are not legit");
				startHourTF.setText(hoursArray[0]);
				endHourTF.setText(hoursArray[1]);
				setSummary("Every Hour from: " + start + ":00 AM to " + end + ":00 PM");
			} catch (Exception ex) {
				Log.e(TAG, "Parsing saved preference", ex);
			}
			startHourTF.requestFocus();
			
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				// for now assume bg color of pre-Honeycomb dialogs is black.
				// I've tried but can't seem to force this color to 
				intermedText.setTextColor(Color.LTGRAY);
				endText.setTextColor(Color.LTGRAY);
			}	
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
		startHourTF.setEnabled(false);
		endHourTF.setEnabled(false);
		intermedText.setEnabled(false);
		endText.setEnabled(false);
	}

	private void specifyClicked() {
		allDayRB.setChecked(false);
		specifyRB.setChecked(true);
		startHourTF.setEnabled(true);
		endHourTF.setEnabled(true);
		intermedText.setEnabled(true);
		endText.setEnabled(true);

//		if (startHourTF.getText().length() < 1) startHourTF.setText("7");
//		if (endHourTF.getText().length() < 1) endHourTF.setText("9");
		startHourTF.requestFocus();
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
				try {
					int start = Integer.parseInt(String.valueOf(startHourTF.getText()));
					int end = Integer.parseInt(String.valueOf(endHourTF.getText()));
					if (start < 1 || start > 12 || end < 1 || end > 12) throw new IllegalStateException("Save numbers are not legit");
					String value = start + " " + end;
					persistString(value);
					notifyChanged();
					setSummary("Every Hour from: " + start + ":00 AM to " + end + ":00 PM");
				} catch (Exception ex) {
					//Toast? for an error message, or another dialog
					Toast t = Toast.makeText(getContext(), "Need hour from 1-12", Toast.LENGTH_LONG);
					t.show();
				}
			}
		}
		super.onDialogClosed(positiveResult);
	}
	
// I adapted this code for this class, but until I have a reasonable way to test it, I'm commenting it out.	
	
// save instance state boilerplate (slightly modified) for custom preference
//	@Override
//	protected Parcelable onSaveInstanceState() {
//		Log.d(TAG, "onSaveInstanceState() ");
//	    final Parcelable superState = super.onSaveInstanceState();
//	    // Check whether this Preference is persistent (continually saved)
//	    if (isPersistent()) {
//	        // No need to save instance state since it's persistent, use superclass state
//	        return superState;
//	    }
//
//	    // Create instance of custom BaseSavedState
//	    final SavedState myState = new SavedState(superState);
//	    // Set the state's value with the class member that holds current setting value
//	    myState.tf1 = String.valueOf(startHourTF.getText());
//	    myState.tf2 = String.valueOf(startHourTF.getText());
//	    myState.pressed1 = allDayRB.isChecked();
//	    return myState;
//	}
//
//	@Override
//	protected void onRestoreInstanceState(Parcelable state) {
//		Log.d(TAG, "onRestoreInstanceState() ");
//	    // Check whether we saved the state in onSaveInstanceState
//	    if (state == null || !state.getClass().equals(SavedState.class)) {
//	        // Didn't save the state, so call superclass
//	        super.onRestoreInstanceState(state);
//	        return;
//	    }
//
//	    // Cast state to custom BaseSavedState and pass to superclass
//	    SavedState myState = (SavedState) state;
//	    super.onRestoreInstanceState(myState.getSuperState());
//	}
//	
//	private static class SavedState extends BaseSavedState {
//	    // Member that holds the setting's value
//		String tf1;
//		String tf2;
//		boolean pressed1;
//	    
//
//	    public SavedState(Parcelable superState) {
//	        super(superState);
//	    }
//
//	    public SavedState(Parcel source) {
//	        super(source);
//	        // Get the current preference's value
//	        tf1 = source.readString();
//	        tf2 = source.readString();
//	        boolean[] barray = new boolean[1];
//	        source.readBooleanArray(barray);
//	        pressed1 = barray[0];  
//	    }
//
//	    @Override
//	    public void writeToParcel(Parcel dest, int flags) {
//	        super.writeToParcel(dest, flags);
//	        // Write the preference's value
//	        dest.writeString(tf1);  
//	        dest.writeString(tf2);  
//	        dest.writeBooleanArray(new boolean[] { pressed1 });
//	    }
//
//	    // Standard creator object using an instance of this class
//	    public static final Parcelable.Creator<SavedState> CREATOR =
//	            new Parcelable.Creator<SavedState>() {
//
//	        public SavedState createFromParcel(Parcel in) {
//	            return new SavedState(in);
//	        }
//
//	        public SavedState[] newArray(int size) {
//	            return new SavedState[size];
//	        }
//	    };
//	}	
}
