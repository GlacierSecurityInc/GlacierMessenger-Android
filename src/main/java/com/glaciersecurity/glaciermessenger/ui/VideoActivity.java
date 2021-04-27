package com.glaciersecurity.glaciermessenger.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.entities.Contact;
import com.glaciersecurity.glaciermessenger.entities.TwilioCallParticipant;
import com.glaciersecurity.glaciermessenger.services.CallManager;
import com.glaciersecurity.glaciermessenger.services.PhonecallReceiver;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;
import com.glaciersecurity.glaciermessenger.ui.interfaces.TwilioCallListener;
import com.glaciersecurity.glaciermessenger.ui.util.CameraCapturerCompat;
import com.glaciersecurity.glaciermessenger.ui.util.SoundPoolManager;
import com.glaciersecurity.glaciermessenger.utils.Compatibility;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.makeramen.roundedimageview.RoundedImageView;
import com.twilio.audioswitch.selection.AudioDevice;
import com.twilio.audioswitch.selection.AudioDeviceSelector;
import com.twilio.video.CameraCapturer;
import com.twilio.video.CameraCapturer.CameraSource;
import com.twilio.video.EncodingParameters;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.RemoteParticipant;
import com.twilio.video.RemoteVideoTrack;
import com.twilio.video.RemoteVideoTrackPublication;
import com.twilio.video.Room;
import com.twilio.video.VideoRenderer;
import com.twilio.video.VideoTrack;
import com.twilio.video.VideoView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import kotlin.Unit;
import rocks.xmpp.addr.Jid;


public class VideoActivity extends XmppActivity implements SensorEventListener, PhonecallReceiver.PhonecallReceiverListener, TwilioCallListener {
    private static final int CAMERA_MIC_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "VideoActivity";

    private SensorManager sensorManager;
    private Sensor proximity;
    private CallManager callManager;

    /*
     * Audio and video tracks can be created with names. This feature is useful for categorizing
     * tracks of participants. For example, if one participant publishes a video track with
     * ScreenCapturer and CameraCapturer with the names "screen" and "camera" respectively then
     * other participants can use RemoteVideoTrack#getName to determine which video track is
     * produced from the other participant's screen or camera.
     */
    private static final String LOCAL_AUDIO_TRACK_NAME = "mic";
    private static final String LOCAL_VIDEO_TRACK_NAME = "camera";

    /*
     * A Room represents communication between a local participant and one or more participants.
     */
    //private Room room;
    //private LocalParticipant localParticipant;
    private AudioManager audioManager;
    //    private MenuItem turnSpeakerOnMenuItem;
//    private MenuItem turnSpeakerOffMenuItem;
    private int previousAudioMode;
    private boolean previousMicrophoneMute;
    private boolean isSpeakerPhoneEnabled = false;
    private Boolean isAudioMuted = false;
    private Boolean isVideoMuted = true;

    private boolean closeProximity = false;

    private boolean endingCall = false;
    private Handler returningNotificationHandler;
    private boolean returning = false;

    /*
     * A VideoView receives frames from a local or remote video track and renders them
     * to an associated view.
     */
    private VideoView thumbnailVideoView;

    private CallParticipantsLayout callParticipantsLayout;

    /*
     * Android application UI elements
     */
    private CameraCapturerCompat cameraCapturerCompat;
    private LocalAudioTrack localAudioTrack;
    private LocalVideoTrack localVideoTrack;
    private ImageView connectActionFab;
    private ImageView switchCameraActionFab;
    private View switchCameraActionSpace;
    private RelativeLayout callView;
    private ImageView localVideoActionFab;
    private ImageView muteActionFab;
    private ImageView speakerPhoneActionFab;
    private RelativeLayout callBar;
    private int currentVideoIcon;

    private LinearLayout reconnectingProgressBar;

    private AlertDialog connectDialog;
    private String remoteParticipantIdentity;
    private TextView primaryTitle;
    private ImageButton minimizeVideo;

    private static final String IS_AUDIO_MUTED = "IS_AUDIO_MUTED";
    private static final String IS_VIDEO_MUTED = "IS_VIDEO_MUTED";
    private static final String IS_SPEAKERPHONE_ENABLED = "IS_SPEAKERPHONE_ENABLED";

    /*
     * Audio management
     */
    private int savedVolumeControlStream;
    private AudioDeviceSelector audioDeviceSelector;
    private AudioFocusRequest focusRequest;
    private VideoRenderer localVideoView;

    private PhonecallReceiver phonecallReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);


        thumbnailVideoView = findViewById(R.id.thumbnail_video_view);
        reconnectingProgressBar = findViewById(R.id.reconnecting_progress_bar_layout);


        phonecallReceiver = new PhonecallReceiver(this);


        connectActionFab = findViewById(R.id.connect_action_fab);
        switchCameraActionFab = findViewById(R.id.switch_camera_action_fab);
        switchCameraActionSpace = findViewById(R.id.switch_camera_action_space);
        callView = findViewById(R.id.call_view);
        localVideoActionFab = findViewById(R.id.local_video_action_fab);
        speakerPhoneActionFab = findViewById(R.id.speaker_phone_action_fab);
        muteActionFab = findViewById(R.id.mute_action_fab);
        this.primaryTitle  = findViewById(R.id.primary_video_title);
        callBar = findViewById(R.id.call_action_bar);
        this.currentVideoIcon = R.drawable.ic_videocam_off_gray_24px;
        minimizeVideo = findViewById(R.id.down_arrow);
        minimizeVideo.setOnClickListener(minimizeCall());

        if (getIntent().getStringExtra("returning") != null) {
            returning = true;
        }

        callParticipantsLayout = findViewById(R.id.call_screen_call_participants);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            this.setTurnScreenOn(true);
            this.setShowWhenLocked(true);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (savedInstanceState != null) {
            isAudioMuted = savedInstanceState.getBoolean(IS_AUDIO_MUTED);
            isVideoMuted = savedInstanceState.getBoolean(IS_VIDEO_MUTED);
            isSpeakerPhoneEnabled = savedInstanceState.getBoolean(IS_SPEAKERPHONE_ENABLED);
        }

        /*
         * Setup audio management and set the volume control stream
         */
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        savedVolumeControlStream = getVolumeControlStream();
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        audioDeviceSelector = new AudioDeviceSelector(getApplicationContext());

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        reconnectingProgressBar.setVisibility(View.VISIBLE);

        createAudioAndVideoTracks();

        /*
         * Set the initial state of the UI
         */
        intializeUI();


    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        final String action = intent.getAction();

        final String title = intent.getStringExtra("roomtitle");
        if (title != null) {
            setTitle(title);
            primaryTitle.setText(title);
        }

        /*
         * Route audio through cached value.
         */
        audioManager.setSpeakerphoneOn(isSpeakerPhoneEnabled);

        registerReceiver(phonecallReceiver, new IntentFilter("android.intent.action.PHONE_STATE"));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == CAMERA_MIC_PERMISSION_REQUEST_CODE) {
            boolean cameraAndMicPermissionGranted = true;

            for (int grantResult : grantResults) {
                cameraAndMicPermissionGranted &= grantResult == PackageManager.PERMISSION_GRANTED;
            }

            if (cameraAndMicPermissionGranted) {
                createAudioAndVideoTracks();
            } else {
                Toast.makeText(this,
                        R.string.permissions_needed,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onResume() {
        super.onResume();

        recreateVideoTrackIfNeeded();
        if(!isVideoMuted) {
            if (currentVideoIcon == R.drawable.ic_videocam_off_gray_24px) {
                localVideoActionFab.callOnClick();
            } else {
                localVideoTrack.enable(true);
            }

            //localVideoTrack.enable(true);
        }

        /*
         * Route audio through cached value.
         */
        audioManager.setSpeakerphoneOn(isSpeakerPhoneEnabled);

        int muteIcon = !isAudioMuted ?
                R.drawable.ic_mic_white_24dp : R.drawable.ic_mic_off_gray_24dp;
        if (!isAudioMuted){
            muteActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.lobbyMediaControls)));
        } else {
            muteActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
        }
        muteActionFab.setImageDrawable(ContextCompat.getDrawable(
                VideoActivity.this, muteIcon));

        sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL);

        isPaused = false;
    }

    private void recreateVideoTrackIfNeeded() {
        /*
         * If the local video track was released when the app was put in the background, recreate.
         */
        if (localVideoTrack == null && checkPermissionForCameraAndMicrophone()) {
            localVideoTrack = LocalVideoTrack.create(this,
                    false,
                    cameraCapturerCompat.getVideoCapturer(),
                    LOCAL_VIDEO_TRACK_NAME);
            localVideoTrack.addRenderer(localVideoView);

            /*
             * If connected to a Room then share the local video track.
             */
            if (callManager != null && callManager.getLocalParticipant() != null) {
                callManager.getLocalParticipant().publishTrack(localVideoTrack);
                callManager.getLocalParticipant().setEncodingParameters(callManager.getEncodingParameters());
            }
        }
    }

    @Override
    public void onPause() {
        /*
         * Release the local video track before going in the background. This ensures that the
         * camera can be used by other applications while this app is in the background.
         */
        // onPause gets called after open if the screen was locked causing video not to work
        if (localVideoTrack != null && remoteParticipantIdentity != null && endingCall) {
            /*
             * If this local video track is being shared in a Room, unpublish from room before
             * releasing the video track. Participants will be notified that the track has been
             * unpublished.
             */
            if (callManager != null && callManager.getLocalParticipant() != null) {
                callManager.getLocalParticipant().unpublishTrack(localVideoTrack);
            }

            localVideoTrack.release();
            localVideoTrack = null;
        }
        sensorManager.unregisterListener(this);

        isPaused = true;
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        savedInstanceState.putBoolean(IS_AUDIO_MUTED, isAudioMuted);
        savedInstanceState.putBoolean(IS_VIDEO_MUTED, isVideoMuted);
        savedInstanceState.putBoolean(IS_SPEAKERPHONE_ENABLED, isSpeakerPhoneEnabled);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        float distance = event.values[0];
        if (distance == 0f){
            closeProximity = true;
            callBar.setClickable(false);
            speakerPhoneActionFab.setClickable(false);
            switchCameraActionFab.setClickable(false);
            connectActionFab.setClickable(false);
            localVideoActionFab.setClickable(false);
            muteActionFab.setClickable(false);
            minimizeVideo.setClickable(false);
        } else {
            closeProximity = false;
            if (collapseNotificationHandler != null) {
                collapseNotificationHandler.removeCallbacksAndMessages(null);
            }

            callBar.setClickable(true);
            speakerPhoneActionFab.setClickable(true);
            switchCameraActionFab.setClickable(true);
            connectActionFab.setClickable(true);
            localVideoActionFab.setClickable(true);
            muteActionFab.setClickable(true);
            minimizeVideo.setClickable(true);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        currentFocus = hasFocus;
        if (!hasFocus && closeProximity && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            collapseNow();
        }
    }

    boolean currentFocus; // To keep track of activity's window focus
    boolean isPaused; // To keep track of activity's foreground/background status
    Handler collapseNotificationHandler;
    public void collapseNow() {

        // Initialize 'collapseNotificationHandler'
        if (collapseNotificationHandler == null) {
            collapseNotificationHandler = new Handler();
        }

        // If window focus has been lost && activity is not in a paused state
        // Its a valid check because showing of notification panel
        // steals the focus from current activity's window, but does not
        // 'pause' the activity
        if (!currentFocus && !isPaused) {
            // Post a Runnable with some delay - currently set to 300 ms
            collapseNotificationHandler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    // Use reflection to trigger a method from 'StatusBarManager'
                    @SuppressLint("WrongConstant") Object statusBarService = getSystemService("statusbar");
                    Class<?> statusBarManager = null;

                    try {
                        statusBarManager = Class.forName("android.app.StatusBarManager");
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }

                    Method collapseStatusBar = null;
                    try {
                        collapseStatusBar = statusBarManager.getMethod("collapsePanels");
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }

                    collapseStatusBar.setAccessible(true);

                    try {
                        collapseStatusBar.invoke(statusBarService);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }

                    // Check if the window focus has been returned
                    // If it hasn't been returned, post this Runnable again
                    // Currently, the delay is 100 ms. You can change this
                    // value to suit your needs.
                    if (!currentFocus && !isPaused) {
                        collapseNotificationHandler.postDelayed(this, 100L);
                    }

                    if (!currentFocus && isPaused) {
                        collapseNotificationHandler.removeCallbacksAndMessages(null);
                    }

                }
            }, 100L);
        }
    }

    @Override
    protected void onDestroy() {

        /*
         * Release the local audio and video tracks ensuring any memory allocated to audio
         * or video is freed.
         */
        if (localAudioTrack != null && endingCall) {
            localAudioTrack.release();
            localAudioTrack = null;
        }

        if (localVideoTrack != null && endingCall) {
            localVideoTrack.release();
            localVideoTrack = null;
        }

        audioDeviceSelector.stop();
        unregisterReceiver(phonecallReceiver);

        if (endingCall) {
            configureAudio(false);
            setVolumeControlStream(savedVolumeControlStream);
        } else {
            //cleanup CallParticipant views when minimizing
            callParticipantsLayout.update(Collections.emptyList());
            if (localVideoTrack != null) {
                localVideoTrack.removeRenderer(thumbnailVideoView);
            }
        }

        closeProximity = false;
        if (collapseNotificationHandler != null) {
            collapseNotificationHandler.removeCallbacksAndMessages(null);
        }

        if (returningNotificationHandler != null) {
            returningNotificationHandler.removeCallbacksAndMessages(null);
        }

        super.onDestroy();
    }

    private boolean checkPermissionForCameraAndMicrophone() {
        int resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int resultMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        return resultCamera == PackageManager.PERMISSION_GRANTED &&
                resultMic == PackageManager.PERMISSION_GRANTED;
    }

    private void createAudioAndVideoTracks() {
        // Share your microphone
        localAudioTrack = LocalAudioTrack.create(this, true, LOCAL_AUDIO_TRACK_NAME);

        // Share your camera
        if (cameraCapturerCompat == null) {
            cameraCapturerCompat = new CameraCapturerCompat(this, getAvailableCameraSource());
        }

        setupLocalVideoTrack();
    }

    private void setupLocalVideoTrack() {

        localVideoTrack = LocalVideoTrack.create(this,
                false,
                cameraCapturerCompat.getVideoCapturer(),
                LOCAL_VIDEO_TRACK_NAME);
        localVideoTrack.addRenderer(thumbnailVideoView);
        localVideoView = thumbnailVideoView;
        thumbnailVideoView.setMirror(cameraCapturerCompat.getCameraSource() ==
                CameraSource.FRONT_CAMERA);
    }

    private CameraSource getAvailableCameraSource() {
        return (CameraCapturer.isSourceAvailable(CameraSource.FRONT_CAMERA)) ?
                (CameraSource.FRONT_CAMERA) :
                (CameraSource.BACK_CAMERA);
    }

    /*
     * The initial state when there is no active room.
     */
    private void intializeUI() {
        connectActionFab.setImageDrawable(ContextCompat.getDrawable(this,
                R.drawable.ic_call_end_white_24px));
        connectActionFab.setVisibility(View.VISIBLE);
        connectActionFab.setOnClickListener(disconnectClickListener());
        switchCameraActionFab.setOnClickListener(switchCameraClickListener());
        localVideoActionFab.setVisibility(View.VISIBLE);
        localVideoActionFab.setOnClickListener(localVideoClickListener());
        muteActionFab.setVisibility(View.VISIBLE);
        muteActionFab.setOnClickListener(muteClickListener());
        speakerPhoneActionFab.setVisibility(View.VISIBLE);
        speakerPhoneActionFab.setOnClickListener(speakerPhoneClickListener());
    }


    /*
     * Called when remote participant joins the room
     */
    @SuppressLint("SetTextI18n")
    private void addRemoteParticipant(TwilioCallParticipant remoteCallParticipant) {

        callParticipantsLayout.update(callManager.getRemoteParticipants());

        //initialize a new view for this specific user, not for
        //VideoActivity as a whole
        RemoteParticipant remoteParticipant = remoteCallParticipant.getRemoteParticipant();

        remoteParticipantIdentity = remoteParticipant.getIdentity();
        Contact remoteContact = getRemoteContact(remoteParticipantIdentity);
        if(remoteContact != null){
            //Todo setAvatar
        }
    }

    public Contact getRemoteContact(String rcString){
        if (rcString.contains("@")){
            return xmppConnectionService.getAccounts().get(0).getRoster().getContact(Jid.of(rcString));
        } else {
            return callManager.getContactFromDisplayName(remoteParticipantIdentity);
        }
    }
    /*
     * Called when remote participant leaves the room
     */
    @SuppressLint("SetTextI18n")
    private void removeRemoteParticipant(RemoteParticipant remoteParticipant) {
        callParticipantsLayout.update(callManager.getRemoteParticipants());
    }

    private void configureAudio(boolean enable) {
        if (enable) {
            previousAudioMode = audioManager.getMode();
            // Request audio focus before making any device switch
            requestAudioFocus();
            /*
             * Use MODE_IN_COMMUNICATION as the default audio mode. It is required
             * to be in this mode when playout and/or recording starts for the best
             * possible VoIP performance. Some devices have difficulties with
             * speaker mode if this is not set.
             */
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            /*
             * Always disable microphone mute during a WebRTC call.
             */
            previousMicrophoneMute = audioManager.isMicrophoneMute();
            audioManager.setMicrophoneMute(false);

            audioDeviceSelector.start((audioDevices, audioDevice) -> Unit.INSTANCE);

            if (SoundPoolManager.getInstance(VideoActivity.this).getSpeakerOn()) {
                List<AudioDevice> availableAudioDevices = audioDeviceSelector.getAvailableAudioDevices();
                for (AudioDevice a : availableAudioDevices) {
                    if (a instanceof AudioDevice.Speakerphone) {
                        audioDeviceSelector.selectDevice(a);
                    }
                }
            }

            updateAudioDeviceIcon(audioDeviceSelector.getSelectedAudioDevice());
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.abandonAudioFocusRequest(focusRequest);
            } else {
                audioManager.abandonAudioFocus(null);
            }
            audioManager.setMicrophoneMute(previousMicrophoneMute);
            audioManager.setMode(previousAudioMode);
        }
    }

    private void requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (focusRequest == null) {
                AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build();
                focusRequest =
                        new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                                .setAudioAttributes(playbackAttributes)
                                .setAcceptsDelayedFocusGain(true)
                                .setOnAudioFocusChangeListener(
                                        i -> {
                                        })
                                .build();
            }
            audioManager.requestAudioFocus(focusRequest);
        } else {
            audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }
    }

    /*
     * Show the current available audio devices.
     */
    private void showAudioDevices() {
        AudioDevice selectedDevice = audioDeviceSelector.getSelectedAudioDevice();
        List<AudioDevice> availableAudioDevices = audioDeviceSelector.getAvailableAudioDevices();

        if (selectedDevice != null) {
            int selectedDeviceIndex = availableAudioDevices.indexOf(selectedDevice);

            ArrayList<String> audioDeviceNames = new ArrayList<>();
            for (AudioDevice a : availableAudioDevices) {
                audioDeviceNames.add(a.getName());
            }

            new AlertDialog.Builder(this)
                    .setTitle(R.string.room_screen_select_device)
                    .setSingleChoiceItems(
                            audioDeviceNames.toArray(new CharSequence[0]),
                            selectedDeviceIndex,
                            (dialog, index) -> {
                                dialog.dismiss();
                                AudioDevice selectedAudioDevice = availableAudioDevices.get(index);
                                updateAudioDeviceIcon(selectedAudioDevice);
                                audioDeviceSelector.selectDevice(selectedAudioDevice);
                            }).create().show();
        }
    }

    /*
     * Update the menu icon based on the currently selected audio device.
     */
    private void updateAudioDeviceIcon(AudioDevice selectedAudioDevice) {
        int audioDeviceIcon = R.drawable.ic_volume_off_gray_24dp;
        speakerPhoneActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));

        if (selectedAudioDevice instanceof AudioDevice.BluetoothHeadset) {
            speakerPhoneActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.lobbyMediaControls)));
            audioDeviceIcon = R.drawable.ic_bluetooth_white_24dp;
            SoundPoolManager.getInstance(VideoActivity.this).setSpeakerOn(false);
        } else if (selectedAudioDevice instanceof AudioDevice.WiredHeadset) {
            speakerPhoneActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.lobbyMediaControls)));
            audioDeviceIcon = R.drawable.ic_headset_mic_white_24dp;
            SoundPoolManager.getInstance(VideoActivity.this).setSpeakerOn(false);
        } else if (selectedAudioDevice instanceof AudioDevice.Earpiece) {
            audioDeviceIcon = R.drawable.ic_volume_off_gray_24dp;
            speakerPhoneActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
            SoundPoolManager.getInstance(VideoActivity.this).setSpeakerOn(false);
        } else if (selectedAudioDevice instanceof AudioDevice.Speakerphone) {
            speakerPhoneActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.lobbyMediaControls)));
            audioDeviceIcon = R.drawable.ic_volume_up_white_24dp;
            SoundPoolManager.getInstance(VideoActivity.this).setSpeakerOn(true);
        }


        speakerPhoneActionFab.setImageDrawable(
                ContextCompat.getDrawable(VideoActivity.this, audioDeviceIcon));
    }

    private View.OnClickListener disconnectClickListener() {
        return v -> {
            /*
             * Disconnect from room
             */
            if (callManager != null) {
                callManager.handleDisconnect();
            }
            handleDisconnect();
        };
    }

    private void handleDisconnect() {
        endingCall = true;
        SoundPoolManager.getInstance(VideoActivity.this).playDisconnect();
        final Intent intent = new Intent(this, XmppConnectionService.class);
        intent.setAction(XmppConnectionService.ACTION_FINISH_CALL);
        Compatibility.startService(this, intent);

        SoundPoolManager.getInstance(this).setSpeakerOn(false);
        audioManager.setMode(SoundPoolManager.getInstance(this).getPreviousAudioMode());

        endListening();
    }

    private View.OnClickListener minimizeCall() {
        return v -> {
            endingCall = false;
            Intent chatsActivity = new Intent(getApplicationContext(), ConversationsActivity.class);
            startActivity(chatsActivity);
        };
    }

    private View.OnClickListener switchCameraClickListener() {
        return v -> {
            if (cameraCapturerCompat != null) {
                CameraSource cameraSource = cameraCapturerCompat.getCameraSource();
                cameraCapturerCompat.switchCamera();
                if (thumbnailVideoView.getVisibility() == View.VISIBLE) {
                    thumbnailVideoView.setMirror(cameraSource == CameraSource.BACK_CAMERA);
                    thumbnailVideoView.bringToFront();
                }
            }
        };
    }

    private View.OnClickListener localVideoClickListener() {
        return v -> {
            /*
             * Enable/disable the local video track
             */
            if (localVideoTrack != null) {
                boolean enable = !localVideoTrack.isEnabled();
                localVideoTrack.enable(enable);
                if (enable) {
                    currentVideoIcon = R.drawable.ic_videocam_white_24dp;
                    localVideoActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.lobbyMediaControls)));
                    switchCameraActionFab.setVisibility(View.VISIBLE);
                    switchCameraActionSpace.setVisibility(View.VISIBLE);


                    enableSpeakerPhone(true);
                    recreateVideoTrackIfNeeded();
                    isVideoMuted = false;
                    thumbnailVideoView.setVisibility(View.VISIBLE);

                } else {
                    currentVideoIcon = R.drawable.ic_videocam_off_gray_24px;
                    localVideoActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
                    switchCameraActionFab.setVisibility(View.GONE);
                    switchCameraActionSpace.setVisibility(View.GONE);
                    enableSpeakerPhone(false);
                    isVideoMuted = true;
                    thumbnailVideoView.setVisibility(View.GONE);
                }
                localVideoActionFab.setImageDrawable(
                        ContextCompat.getDrawable(VideoActivity.this, currentVideoIcon));
            }
        };
    }

    private View.OnClickListener speakerPhoneClickListener() {
        return v -> {
            List<AudioDevice> availableAudioDevices = audioDeviceSelector.getAvailableAudioDevices();
            if (availableAudioDevices.size()>2){
                showAudioDevices();
            }else{
                boolean expectedSpeakerPhoneState = !audioManager.isSpeakerphoneOn();

                audioManager.setSpeakerphoneOn(expectedSpeakerPhoneState);
                isSpeakerPhoneEnabled = expectedSpeakerPhoneState;
                SoundPoolManager.getInstance(VideoActivity.this).setSpeakerOn(expectedSpeakerPhoneState);

                int icon;
                if (expectedSpeakerPhoneState) {
                    icon = R.drawable.ic_volume_up_white_24dp;
                    speakerPhoneActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.lobbyMediaControls)));


                } else {
                    icon = R.drawable.ic_volume_off_gray_24dp;
                    speakerPhoneActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));

                }
                speakerPhoneActionFab.setImageDrawable(
                        ContextCompat.getDrawable(VideoActivity.this, icon));
            }
        };
    }
    private void enableSpeakerPhone(boolean expectedSpeakerPhoneState){
        if (audioManager != null) {
            if (audioDeviceSelector.getSelectedAudioDevice()  instanceof AudioDevice.BluetoothHeadset) {
            } else {
                audioManager.setSpeakerphoneOn(expectedSpeakerPhoneState);
                isSpeakerPhoneEnabled = expectedSpeakerPhoneState;
                SoundPoolManager.getInstance(VideoActivity.this).setSpeakerOn(expectedSpeakerPhoneState);

                int icon;
                if (expectedSpeakerPhoneState) {
                    icon = R.drawable.ic_volume_up_white_24dp;
                    speakerPhoneActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.lobbyMediaControls)));

                } else {
                    icon = R.drawable.ic_volume_off_gray_24dp;
                    speakerPhoneActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));

                }
                speakerPhoneActionFab.setImageDrawable(
                        ContextCompat.getDrawable(VideoActivity.this, icon));
            }
        }

    }

    private View.OnClickListener muteClickListener() {
        return v -> {
            /*
             * Enable/disable the local audio track. The results of this operation are
             * signaled to other Participants in the same Room. When an audio track is
             * disabled, the audio is muted.
             */
            if (localAudioTrack != null) {
                boolean enable = !localAudioTrack.isEnabled();
                localAudioTrack.enable(enable);
                isAudioMuted = !enable;
                int icon = enable ?
                        R.drawable.ic_mic_white_24dp : R.drawable.ic_mic_off_gray_24dp;


                if (enable){
                    muteActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.lobbyMediaControls)));
                } else {
                    muteActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
                }
                muteActionFab.setImageDrawable(ContextCompat.getDrawable(
                        VideoActivity.this, icon));
            }
        };
    }

    @Override
    protected void onBackendConnected() {
        try {
            if (xmppConnectionService != null && callManager == null) {
                configureAudio(true);
                callManager = xmppConnectionService.getCallManager();
                callManager.setCallListener(this);
                callManager.readyToConnect();
                boolean localAudioHandled = false;

                if (callManager.getLocalParticipant() != null) {
                    if (callManager.getLocalParticipant().getLocalVideoTracks().size() > 0) {
                        LocalVideoTrack lvTrack = callManager.getLocalParticipant().getLocalVideoTracks().get(0).getLocalVideoTrack();
                        boolean wasEnabled = lvTrack.isEnabled();
                        callManager.getLocalParticipant().unpublishTrack(lvTrack);
                        lvTrack.release();
                        callManager.getLocalParticipant().publishTrack(localVideoTrack);
                        if (wasEnabled) {
                            localVideoActionFab.callOnClick();
                        }
                    } else {
                        callManager.getLocalParticipant().publishTrack(localVideoTrack);
                    }

                    callManager.getLocalParticipant().setEncodingParameters(callManager.getEncodingParameters());

                    if (callManager.getLocalParticipant().getLocalAudioTracks().size() > 0) {
                        localAudioTrack = callManager.getLocalParticipant().getLocalAudioTracks().get(0).getLocalAudioTrack();
                        isAudioMuted = !localAudioTrack.isEnabled();
                        int muteIcon = !isAudioMuted ?
                                R.drawable.ic_mic_white_24dp : R.drawable.ic_mic_off_gray_24dp;
                        if (!isAudioMuted){
                            muteActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.lobbyMediaControls)));
                        } else {
                            muteActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
                        }
                        muteActionFab.setImageDrawable(ContextCompat.getDrawable(
                                VideoActivity.this, muteIcon));
                        localAudioHandled = true;
                    } else {
                        callManager.getLocalParticipant().publishTrack(localAudioTrack);
                    }
                }
                if (isAudioMuted && !localAudioHandled) {
                    muteActionFab.callOnClick();
                }

                if (returning) {
                    returningNotificationHandler = new Handler();
                    returningNotificationHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if(callManager!= null) {
                                callParticipantsLayout.update(callManager.getRemoteParticipants());
                            }
                        }
                    }, 1000);
                }
            }
        } catch (Exception e){

        }
    }


    public void refreshUiReal() {
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void onIncomingNativeCallAnswered() {
        callManager.handleDisconnect();
        handleDisconnect();
    }

    View activityView;
    public Snackbar snackbar = null;

    @Override
    public void onIncomingNativeCallRinging(int call_act) {
        activityView = this.callView;
        if (call_act == 0) {
            if (snackbar != null) {
                snackbar.dismiss();
            }
        } else {
            if (activityView != null) {
                snackbar = Snackbar.make(activityView, R.string.native_ringing, Snackbar.LENGTH_INDEFINITE);

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

    public void handleParticipantConnected(TwilioCallParticipant remoteCallParticipant) {
        addRemoteParticipant(remoteCallParticipant);
        reconnectingProgressBar.setVisibility(View.GONE);
    }

    public void handleParticipantDisconnected(RemoteParticipant remoteParticipant) {
        // this shouldn't handle disconnect unless no other participants
        removeRemoteParticipant(remoteParticipant);

        if (callManager.getRemoteParticipants().size() == 0) {
            handleDisconnect();
        }
    }

    public void handleConnected(Room room){
        setTitle(room.getName());

        audioDeviceSelector.activate();
        updateAudioDeviceIcon(audioDeviceSelector.getSelectedAudioDevice());

        callParticipantsLayout.update(callManager.getRemoteParticipants());

        if (!room.getRemoteParticipants().isEmpty()){
            reconnectingProgressBar.setVisibility(View.GONE);
        }
    }

    public void handleReconnecting(boolean reconnecting){
        if (reconnecting) {
            reconnectingProgressBar.setVisibility(View.VISIBLE);
        } else {
            reconnectingProgressBar.setVisibility(View.GONE);
        }
    }

    public void handleConnectFailure(){
        configureAudio(false);
        audioDeviceSelector.deactivate();
        intializeUI();
    }

    public void endListening(){
        reconnectingProgressBar.setVisibility(View.GONE);
        configureAudio(false);
        audioDeviceSelector.deactivate();
        if (callManager != null) {
            callManager.setCallListener(null);
            callManager = null;
        }
        finish();
    }

    public LocalAudioTrack getLocalAudioTrack() {
        return localAudioTrack;
    }

    public LocalVideoTrack getLocalVideoTrack() {
        return localVideoTrack;
    }
}