package com.tonycase.simplechime;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

public class VolumeSelectDialog extends DialogPreference {

    private final String TAG = "VolumeSelectDialog";

    public static final int DEFAULT_VALUE = 10;

    private SeekBar seekBar;
    private TextView valueTV;

    public VolumeSelectDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VolumeSelectDialog(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {

        Log.d(TAG, "init() ");

        setDialogLayoutResource(R.layout.volume_select_preference);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        setDialogIcon(null);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        int level = prefs.getInt("PREF_VOLUME", DEFAULT_VALUE);
        setSummaryWithLevel(level);
    }

    private void setSummaryWithLevel(int level) {

        String summary0 = (String) getSummary();
        int index = summary0.indexOf(':');
        String summary = summary0.substring(0, index + 2) + level;
        setSummary(summary);
    }

    /* (non-Javadoc)
     * @see android.preference.DialogPreference#onBindDialogView(android.view.View)
     */
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        Log.d(TAG, "onBindDialogView() ");
        seekBar = (SeekBar) view.findViewById(R.id.seekBar1);
        valueTV = (TextView) view.findViewById(R.id.textView1);
        seekBar.setMax(10);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        int level = prefs.getInt("PREF_VOLUME", DEFAULT_VALUE);

        seekBar.setProgress(level);
        valueTV.setText(String.valueOf(level));
        seekBar.setOnSeekBarChangeListener(listener);
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
     * @see android.preference.Preference#onPrepareForRemoval()
     */
    @Override
    protected void onPrepareForRemoval() {
        Log.d(TAG, "onPrepareForRemoval() ");
        super.onPrepareForRemoval();
    }


    SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            ChimeUtilities.playSound(getContext(), seekBar.getProgress());
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            valueTV.setText(String.valueOf(progress));
        }
    };

    /* (non-Javadoc)
     * @see android.preference.DialogPreference#onDialogClosed(boolean)
     */
    @Override
    protected void onDialogClosed(boolean positiveResult) {

        Log.d(TAG, "onDialogClosed");

        if (positiveResult) {
            setSummaryWithLevel(seekBar.getProgress());
            persistInt(seekBar.getProgress());
            notifyChanged();
        }

        super.onDialogClosed(positiveResult);
    }

    // save instance state boilerplate (slightly modified) for custom preference
    @Override
    protected Parcelable onSaveInstanceState() {
        Log.d(TAG, "onSaveInstanceState() ");
        final Parcelable superState = super.onSaveInstanceState();
        // Check whether this Preference is persistent (continually saved)
        if (isPersistent()) {
            // No need to save instance state since it's persistent, use superclass state
            return superState;
        }

        // Create instance of custom BaseSavedState
        final SavedState myState = new SavedState(superState);
        // Set the state's value with the class member that holds current setting value
        myState.value = seekBar.getProgress();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Log.d(TAG, "onRestoreInstanceState() ");
        // Check whether we saved the state in onSaveInstanceState
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save the state, so call superclass
            super.onRestoreInstanceState(state);
            return;
        }

        // Cast state to custom BaseSavedState and pass to superclass
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());

        seekBar.setMax(10);
        // Set this Preference's widget to reflect the restored state
        seekBar.setProgress(myState.value);
    }

    private static class SavedState extends BaseSavedState {
        // Member that holds the setting's value
        int value;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            // Get the current preference's value
            value = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            // Write the preference's value
            dest.writeInt(value);
        }

        // Standard creator object using an instance of this class
        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {

                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
