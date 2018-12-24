package com.glaciersecurity.glaciermessenger.ui;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.utils.PhoneHelper;

public class AboutPreference extends Preference {
	public AboutPreference(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		setSummary();
	}

	public AboutPreference(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		setSummary();
	}

    @Override
    protected void onClick() {
        super.onClick();
        final Intent intent = new Intent(getContext(), AboutActivity.class);
        getContext().startActivity(intent);
    }

    private void setSummary() {
		setSummary(getContext().getString(R.string.app_name) +' '+ PhoneHelper.getVersionName(getContext()));
	}
}

