package hu.blint.ssldroid.ui;

import android.content.Context;
import android.preference.EditTextPreference;
import android.text.InputType;
import android.util.AttributeSet;

public class TimeoutPreference extends EditTextPreference {
    public TimeoutPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setInputType();
    }

    public TimeoutPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setInputType();
    }

    public TimeoutPreference(Context context) {
        super(context);
        setInputType();
    }

    private void setInputType() {
        getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
    }

    @Override
    public void setText(String text) {
        String parsed = Integer.toString(Settings.parseConnectionTimeout(text));
        super.setText(parsed);
        setSummary(parsed);
    }
}
