package com.glaciersecurity.glaciermessenger.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.Conversation;
import com.glaciersecurity.glaciermessenger.entities.TwilioCall;
import com.glaciersecurity.glaciermessenger.services.PhonecallReceiver;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;
import com.glaciersecurity.glaciermessenger.ui.util.SoundPoolManager;
import com.glaciersecurity.glaciermessenger.utils.Compatibility;
import com.google.android.material.snackbar.Snackbar;

import rocks.xmpp.addr.Jid;

public class CallActivity extends XmppActivity implements PhonecallReceiver.PhonecallReceiverListener{

	public static final String ACTION_INCOMING_CALL = "incoming_call";
	public static final String ACTION_OUTGOING_CALL = "outgoing_code";
	public static final String ACTION_ACCEPTED_CALL = "call_accepted";

	private static final int CAMERA_MIC_PERMISSION_REQUEST_CODE = 1;
	public static final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 0;

	private AudioManager audioManager;

	private TextView callState;
	private TextView contactText;
	private LinearLayout incomingCallLayout;
	private LinearLayout outgoingCallLayout;
	private AppCompatImageButton rejectCallBtn;
	private AppCompatImageButton acceptCallBtn;
	private AppCompatImageButton endCallBtn;
	private AppCompatImageButton audioBtn;
	private AppCompatImageButton audioBtnOff;
	private AppCompatImageButton speakerBtn;
	private AppCompatImageButton speakerBtnOff;
	private ImageView avatar;

	private TwilioCall currentTwilioCall;
	private Jid contactJid;
	private String conversationUuid;
	private String calltitle;
	private Handler handler = new Handler();

	private PhonecallReceiver phonecallReceiver;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_call);
		setSupportActionBar(findViewById(R.id.toolbar));
		this.callState = findViewById(R.id.call_state);
		this.contactText = findViewById(R.id.participant_selected_identity);
		this.incomingCallLayout = findViewById(R.id.call_status_incoming);
		this.outgoingCallLayout = findViewById(R.id.call_status_outgoing);
		this.avatar = (ImageView) findViewById(R.id.participant_stub_image);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
			this.setTurnScreenOn(true);
			this.setShowWhenLocked(true);
		}

		if (ContextCompat.checkSelfPermission(CallActivity.this,
				Manifest.permission.READ_PHONE_STATE)
				!= PackageManager.PERMISSION_GRANTED) {
			// We do not have this permission. Let's ask the user
			ActivityCompat.requestPermissions(CallActivity.this,
					new String[]{Manifest.permission.READ_PHONE_STATE},
					MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
		}
		phonecallReceiver = new PhonecallReceiver(this);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
				| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		SoundPoolManager.getInstance(CallActivity.this).setPreviousAudioMode(audioManager.getMode());

		registerReceiver(mMessageReceiver, new IntentFilter("callActivityFinish"));

		if (getIntent().getAction().equals(ACTION_OUTGOING_CALL)) {
			try {
				this.contactJid = Jid.of(getIntent().getExtras().getString("receiver"));
				this.conversationUuid = getIntent().getExtras().getString("uuid");
				this.calltitle = getIntent().getExtras().getString("calltitle");

			} catch (final IllegalArgumentException ignored) {
			}
		}
		this.acceptCallBtn= findViewById(R.id.accept_call_button);
		acceptCallBtn.setOnClickListener(v -> {
			if(checkPermissionForCameraAndMicrophone()){
				acceptCall();
			} else {
				requestPermissionForCameraAndMicrophone();
			}
		});
		this.rejectCallBtn= findViewById(R.id.reject_call_button);
		rejectCallBtn.setOnClickListener(v -> {
			endCall();
		});


		this.endCallBtn= findViewById(R.id.end_call_button);
		endCallBtn.setOnClickListener(v -> {
			SoundPoolManager.getInstance(CallActivity.this).stopRinging();
			SoundPoolManager.getInstance(CallActivity.this).setSpeakerOn(false);

			final Intent intent = new Intent(this, XmppConnectionService.class);
			intent.setAction(XmppConnectionService.ACTION_CANCEL_CALL_REQUEST);
			Compatibility.startService(this, intent);
		});

		this.audioBtn = findViewById(R.id.audio_image_button);
		this.audioBtnOff = findViewById(R.id.audio_image_button_off);
		this.audioBtnOff.setEnabled(false);
		this.audioBtnOff.setClickable(false);
		this.audioBtn.setClickable(false);

		// TODO in twilio roomActiviy audio manager is used to manage speaker phone status
		this.speakerBtn= findViewById(R.id.speaker_button);
		this.speakerBtnOff= findViewById(R.id.speaker_button_off);

		this.speakerBtnOff.setEnabled(true);
		speakerBtnOff.setOnClickListener(v -> {
			SoundPoolManager.getInstance(CallActivity.this).setSpeakerOn(true);
			speakerBtn.setVisibility(View.VISIBLE);
			speakerBtnOff.setVisibility(View.GONE);
		});
		speakerBtn.setOnClickListener(v -> {
			SoundPoolManager.getInstance(CallActivity.this).setSpeakerOn(false);
			speakerBtn.setVisibility(View.GONE);
			speakerBtnOff.setVisibility(View.VISIBLE);
		});

	}

	public void endCall(){
		SoundPoolManager.getInstance(CallActivity.this).stopRinging();
		SoundPoolManager.getInstance(CallActivity.this).setSpeakerOn(false);

		//needs access to XmppConnectionService
		final Intent intent = new Intent(this, XmppConnectionService.class);
		intent.setAction(XmppConnectionService.ACTION_REJECT_CALL_REQUEST);
		Compatibility.startService(this, intent);
	}

	public void acceptCall(){
			SoundPoolManager.getInstance(CallActivity.this).stopRinging();
			if (audioManager.isSpeakerphoneOn()) {
				SoundPoolManager.getInstance(CallActivity.this).setSpeakerOn(true);
			}
			//needs access to XmppConnectionService
			final Intent intent = new Intent(this, XmppConnectionService.class);
			intent.setAction(XmppConnectionService.ACTION_ACCEPT_CALL_REQUEST);
			Compatibility.startService(this, intent);
		}
	@Override
	public void onStart() {
		super.onStart();
		Intent intent = getIntent();
		if (intent == null) {
			return;
		}

		final String action = intent.getAction();

		if (CallActivity.ACTION_INCOMING_CALL.equals(action)) {
			TwilioCall call = new TwilioCall(null);
			call.setCallId(intent.getIntExtra("call_id", 0));
			call.setCaller(intent.getStringExtra("caller"));
			call.setStatus(intent.getStringExtra("status"));
			currentTwilioCall = call;

			if (intent.getBooleanExtra("ring", false)) {
				SoundPoolManager.getInstance(CallActivity.this).playRinging();
			}

			this.onIncomingCall();
		} else if (CallActivity.ACTION_OUTGOING_CALL.equals(action)) {
			TwilioCall call = new TwilioCall(null);
			call.setReceiver(intent.getStringExtra("receiver"));
			call.setRoomTitle(intent.getStringExtra("calltitle"));
			currentTwilioCall = call;
			this.onOutgoingCall();
		}

		registerReceiver(phonecallReceiver, new IntentFilter("android.intent.action.PHONE_STATE"));
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			handler.removeCallbacksAndMessages(null);
			finish();
		}
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		SoundPoolManager.getInstance(CallActivity.this).stopRinging();
		handler.removeCallbacksAndMessages(null);
		unregisterReceiver(mMessageReceiver);
		unregisterReceiver(phonecallReceiver);
	}

	@Override
	protected void onBackendConnected() {
		try {
			if (xmppConnectionService != null) {
				Account mAccount = xmppConnectionService.getAccounts().get(0);
				if (mAccount != null && conversationUuid != null) {
					Conversation conversation = xmppConnectionService.findConversationByUuid(conversationUuid);
					avatar.setImageBitmap(avatarService().get(conversation.getContact(), (int) getResources().getDimension(R.dimen.avatar_on_incoming_call_screen_size)));

				}
			}
		} catch (Exception e){

		}
	}


	public void refreshUiReal() {
	}
	private void onIncomingCall(){
		String incoming = getResources().getString(R.string.incoming_call);
		callState.setText(incoming);
		contactText.setText(currentTwilioCall.getCaller());
		if (currentTwilioCall.getRoomTitle() != null) {
			contactText.setText(currentTwilioCall.getRoomTitle());
		}
		incomingCallLayout.setVisibility(View.VISIBLE);
		outgoingCallLayout.setVisibility(View.GONE);

		handler.postDelayed(() -> {
			handler.removeCallbacksAndMessages(null);
			SoundPoolManager.getInstance(CallActivity.this).stopRinging();
			Log.d(Config.LOGTAG, "CallActivity - Cancelling call after 30 sec");
			final Intent intent = new Intent(this, XmppConnectionService.class);
			intent.setAction(XmppConnectionService.ACTION_REJECT_CALL_REQUEST);
			Compatibility.startService(this, intent);
		},30000);
	}

	private void onOutgoingCall(){
		SoundPoolManager.getInstance(CallActivity.this).playRinging();
		callState.setText(getResources().getString(R.string.outgoing_call));
		if (currentTwilioCall.getRoomTitle() != null) {
			contactText.setText(currentTwilioCall.getRoomTitle());
		} else {
			try {
				contactText.setText(Jid.of(currentTwilioCall.getReceiver()).getEscapedLocal());
			} catch (final IllegalArgumentException ignored) {
				contactText.setText(contactJid);
			}
		}
		incomingCallLayout.setVisibility(View.GONE);
		outgoingCallLayout.setVisibility(View.VISIBLE);
	}

	protected boolean hasStoragePermissions() {
		return (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED ||
				checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
	}

	protected void requestPermissions(final int request_code) {
		if (!hasStoragePermissions()) {
			requestPermissions(
					new String[]{
							Manifest.permission.RECORD_AUDIO,
							Manifest.permission.CAMERA,
					},
					request_code
			);
		}
	}

	@Override
	public void onRequestPermissionsResult(final int requestCode,
										   @NonNull final String[] permissions,
										   @NonNull final int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (requestCode == MY_PERMISSIONS_REQUEST_READ_PHONE_STATE) {
			// do nothing
			return;
		}
	}



	private boolean checkPermissionForCameraAndMicrophone() {
		int resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
		int resultMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
		return resultCamera == PackageManager.PERMISSION_GRANTED &&
				resultMic == PackageManager.PERMISSION_GRANTED;
	}

	private void requestPermissionForCameraAndMicrophone() {
		ActivityCompat.requestPermissions(
				this,
				new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
				CAMERA_MIC_PERMISSION_REQUEST_CODE);
	}

	@Override
	public void onIncomingNativeCallAnswered() {
		//cancel any current call
		SoundPoolManager.getInstance(CallActivity.this).stopRinging();
		SoundPoolManager.getInstance(CallActivity.this).setSpeakerOn(false);
		final Intent intent = new Intent(this, XmppConnectionService.class);

		if (incomingCallLayout.getVisibility() == View.VISIBLE) {
			intent.setAction(XmppConnectionService.ACTION_REJECT_CALL_REQUEST); //incoming
		} else {
			intent.setAction(XmppConnectionService.ACTION_CANCEL_CALL_REQUEST); //outgoing?
		}
		Compatibility.startService(this, intent);
	}


	View activityView;
	public Snackbar snackbar = null;

	@Override
	public void onIncomingNativeCallRinging(int call_act) {
		activityView = this.getCurrentFocus();
		if (call_act == 0) {
			snackbar.dismiss();
		} else {
			if (activityView != null) {
				snackbar = Snackbar.make(activityView,R.string.native_ringing, Snackbar.LENGTH_INDEFINITE);

				View mView = snackbar.getView();
				TextView mTextView = (TextView) mView.findViewById(R.id.snackbar_text);
				mTextView.setGravity(Gravity.CENTER_HORIZONTAL);
				mTextView.setBackgroundColor(getResources().getColor(R.color.blue_palette_hex1));
				mTextView.setTextColor(getResources().getColor(R.color.almost_black));

				snackbar.show();
			} else {
				Toast.makeText(this, R.string.native_ringing, Toast.LENGTH_LONG).show();

				// AlertDialog.Builder alert = new AlertDialog.Builder(CallActivity.this);
				// alert.setTitle(R.string.native_ring_alert_title);
				// alert.setMessage(R.string.native_ringing);
				// alert.setPositiveButton("OK",null);
				// alert.show();
			}
		}
	}
}
