package com.tonycase.simplechime;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.NumberPicker;

/**
 * A {@link android.preference.Preference} that displays a number picker as a dialog.
 */
public class NumberPickerDialog extends DialogPreference {

    // allowed range
    public static final int MAX_VALUE = 59;
    // enable or disable the 'circular behavior'
    public static final boolean WRAP_SELECTOR_WHEEL = true;

    private NumberPicker picker;
    private int value;

    public NumberPickerDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NumberPickerDialog(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected View onCreateDialogView() {
        Log.d("NumberPickerDialog", "onCreateDialogView");
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;

        picker = new NumberPicker(getContext());
        picker.setLayoutParams(layoutParams);

        FrameLayout dialogView = new FrameLayout(getContext());
        dialogView.addView(picker);

        return dialogView;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        Log.d("NumberPickerDialog", "onBindDialogView");

        picker.setMinValue(0);
        picker.setMaxValue(MAX_VALUE);
        picker.setWrapSelectorWheel(WRAP_SELECTOR_WHEEL);
        picker.setValue(getValueForPicker());
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        Log.d("NumberPickerDialog", "onDialogClose");
        if (positiveResult) {
            picker.clearFocus();
            setValueFromPicker(picker.getValue());
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        setValue(restorePersistedValue ? getPersistedInt(0) : (Integer) defaultValue);
    }

    private int getValueForPicker() {
        return value < 0 ? value+60 : value;
    }

    private void setValueFromPicker(int pickerValue) {
        int newValue = pickerValue > 30 ? pickerValue-60 : pickerValue;
        if (callChangeListener(newValue)) {
            setValue(newValue);
        }
    }

    // set the value of the preference
    public void setValue(int value) {
        this.value = value;
        persistInt(this.value);
    }

    // Get the value of the preference  (the value shown to the user ADJUSTED)
    public int getValue() {
        return this.value;
    }
}