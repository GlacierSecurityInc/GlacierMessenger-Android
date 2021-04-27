package com.glaciersecurity.glaciermessenger.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.glaciersecurity.glaciermessenger.R;

public class ForgotPasswordActivity extends AppCompatActivity {

	private EditText emailTextEdit;
	private Button continueButton;
	private Button supportButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_reset_password);

		emailTextEdit = (EditText) findViewById(R.id.recovery_email);
		emailTextEdit.addTextChangedListener(resetTextWatcher);
		continueButton = (Button) findViewById(R.id.continue_button);
		continueButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

			}
		});
		supportButton = (Button) findViewById(R.id.support_button);
		supportButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("https://glaciersecurity.zendesk.com"));
				startActivity(intent);
			}
		});
		checkEnabledSignUpButton();
	}

	private TextWatcher resetTextWatcher = new TextWatcher() {
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {

		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			checkEnabledSignUpButton();

		}

		@Override
		public void afterTextChanged(Editable s) {

		}
	};

	public void checkEnabledSignUpButton(){
		String emailInput = emailTextEdit.getText().toString().trim();

		continueButton.setEnabled(!emailInput.isEmpty() );
		if (!continueButton.isEnabled()){
			continueButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_gray_300));
		} else {
			continueButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_accent_300));
		}
	}
}
