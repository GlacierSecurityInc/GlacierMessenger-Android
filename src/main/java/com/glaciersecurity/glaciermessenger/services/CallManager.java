package com.glaciersecurity.glaciermessenger.services;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.entities.Contact;
import com.glaciersecurity.glaciermessenger.entities.TwilioCall;
import com.glaciersecurity.glaciermessenger.entities.TwilioCallParticipant;
import com.glaciersecurity.glaciermessenger.ui.CallActivity;
import com.glaciersecurity.glaciermessenger.ui.VideoActivity;
import com.glaciersecurity.glaciermessenger.ui.interfaces.TwilioCallListener;
import com.twilio.video.AudioCodec;
import com.twilio.video.ConnectOptions;
import com.twilio.video.EncodingParameters;
import com.twilio.video.G722Codec;
import com.twilio.video.H264Codec;
import com.twilio.video.IsacCodec;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalParticipant;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.OpusCodec;
import com.twilio.video.PcmaCodec;
import com.twilio.video.PcmuCodec;
import com.twilio.video.RemoteAudioTrack;
import com.twilio.video.RemoteAudioTrackPublication;
import com.twilio.video.RemoteDataTrack;
import com.twilio.video.RemoteDataTrackPublication;
import com.twilio.video.RemoteParticipant;
import com.twilio.video.RemoteVideoTrack;
import com.twilio.video.RemoteVideoTrackPublication;
import com.twilio.video.Room;
import com.twilio.video.TwilioException;
import com.twilio.video.Video;
import com.twilio.video.VideoCodec;
import com.twilio.video.Vp8Codec;
import com.twilio.video.Vp9Codec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import androidx.annotation.NonNull;

import rocks.xmpp.addr.Jid;

public class CallManager {
    private static final String TAG = "CallManager";

    private String accessToken;
    private int callid;
    private String caller;
    private String roomname;
    private String receiver;

    private String roomtitle;
    private List<TwilioCallParticipant> callParticipants;

    private TwilioCallListener twilioCallListener;

    /*
     * A Room represents communication between a local participant and one or more participants.
     */
    private Room room;
    private LocalParticipant localParticipant;
    private AudioManager audioManager;

    /*
     * AudioCodec and VideoCodec represent the preferred codec for encoding and decoding audio and
     * video.
     */
    private AudioCodec audioCodec;
    private VideoCodec videoCodec;

    /*
     * Encoding parameters represent the sender side bandwidth constraints.
     */
    private EncodingParameters encodingParameters;

    /*
     * Android shared preferences used for settings
     */
    private SharedPreferences preferences;

    public static final String PREF_AUDIO_CODEC = "audio_codec";
    public static final String PREF_AUDIO_CODEC_DEFAULT = OpusCodec.NAME;
    public static final String PREF_VIDEO_CODEC = "video_codec";
    public static final String PREF_VIDEO_CODEC_DEFAULT = Vp8Codec.NAME;
    public static final String PREF_SENDER_MAX_AUDIO_BITRATE = "sender_max_audio_bitrate";
    public static final String PREF_SENDER_MAX_AUDIO_BITRATE_DEFAULT = "0";
    public static final String PREF_SENDER_MAX_VIDEO_BITRATE = "sender_max_video_bitrate";
    public static final String PREF_SENDER_MAX_VIDEO_BITRATE_DEFAULT = "0";
    public static final String PREF_VP8_SIMULCAST = "vp8_simulcast";
    public static final boolean PREF_VP8_SIMULCAST_DEFAULT = false;
    public static final String PREF_ENABLE_AUTOMATIC_SUBSCRIPTION = "enable_automatic_subscription";
    public static final boolean PREF_ENABLE_AUTOMATIC_SUBSCRIPTION_DEFAULT = true;
    private boolean enableAutomaticSubscription;
    HashMap<String, Contact> contactByDisplayName = new HashMap<>();

    protected XmppConnectionService mXmppConnectionService;

    public CallManager(XmppConnectionService service) {
        mXmppConnectionService = service;
        callParticipants = new ArrayList<TwilioCallParticipant>();
    }

    public Contact getContactFromDisplayName(String displayName){
        return contactByDisplayName.get(displayName);
    }

    public Contact getRemoteContact(String rcString){
        if (rcString.contains("@")){
            return mXmppConnectionService.getAccounts().get(0).getRoster().getContact(Jid.of(rcString));
        } else {
            return getContactFromDisplayName(rcString);
        }
    }

    public void initCall(TwilioCall call) {

        if (audioCodec == null) {
            initPreferences();
        }

        callid = call.getCallId();
        accessToken = call.getToken();
        roomname = call.getRoomName();
        caller = call.getCaller();
        receiver = call.getReceiver();
        roomtitle = call.getRoomTitle();

        Intent callIntent = new Intent(mXmppConnectionService, VideoActivity.class);
        callIntent.setAction(CallActivity.ACTION_ACCEPTED_CALL);
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        callIntent.putExtra("roomname", roomname);
        callIntent.putExtra("caller", caller);
        callIntent.putExtra("receiver", receiver);
        callIntent.putExtra("roomtitle", roomtitle);
        mXmppConnectionService.startActivity(callIntent);

        getCaller();
        getReceivers();

        //if (room == null || room.getState() == Room.State.DISCONNECTED) {
        //    connectToRoom(roomname);
        //}


    }

    public void getCaller(){
        try {
            Jid remoteJid = Jid.of(caller);
            Contact contact = mXmppConnectionService.getAccounts().get(0).getRoster().getContact(remoteJid);
            contactByDisplayName.put(contact.getDisplayName(), contact);
        }catch (Exception e){

        }
    }
    public void getReceivers(){
        List<String> receiverJids = Arrays.asList(receiver.split(","));
        for(String receiver: receiverJids) {
            try {
                Jid remoteJid = Jid.of(receiver);
                Contact contact = mXmppConnectionService.getAccounts().get(0).getRoster().getContact(remoteJid);
                contactByDisplayName.put(contact.getDisplayName(), contact);
            }
            catch (Exception e){

            }
        }
    }

    private void initPreferences() {
        /*
         * Get shared preferences to read settings
         */
        //preferences = mXmppConnectionService.getPreferences();
        preferences = PreferenceManager.getDefaultSharedPreferences(mXmppConnectionService);

        audioCodec = getAudioCodecPreference(PREF_AUDIO_CODEC,
                PREF_AUDIO_CODEC_DEFAULT);
        videoCodec = getVideoCodecPreference(PREF_VIDEO_CODEC,
                PREF_VIDEO_CODEC_DEFAULT);
        enableAutomaticSubscription = getAutomaticSubscriptionPreference(PREF_ENABLE_AUTOMATIC_SUBSCRIPTION,
                PREF_ENABLE_AUTOMATIC_SUBSCRIPTION_DEFAULT);

        /*
         * Get latest encoding parameters
         */
        encodingParameters = initEncodingParameters();
    }

    //the UI listener will call this
    public void readyToConnect() {
        if (room == null || room.getState() == Room.State.DISCONNECTED) {
            connectToRoom(roomname);
        } else if (room.getState() == Room.State.CONNECTED) {
            if (twilioCallListener != null) {
                twilioCallListener.handleConnected(room);
            }
        }
    }

    public boolean isOnCall(){
        if (room == null || room.getState() == Room.State.DISCONNECTED) {
            return false;
        } else if (room.getState() == Room.State.CONNECTED) {
            if (twilioCallListener != null) {
                return true;
            }
        }
            return false;
    }

    public void setCallListener(TwilioCallListener listener) {
        twilioCallListener = listener;
    }

    private boolean getAutomaticSubscriptionPreference(String key, boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }

    private EncodingParameters initEncodingParameters() {
        final int maxAudioBitrate = Integer.parseInt(
                preferences.getString(PREF_SENDER_MAX_AUDIO_BITRATE,
                        PREF_SENDER_MAX_AUDIO_BITRATE_DEFAULT));
        final int maxVideoBitrate = Integer.parseInt(
                preferences.getString(PREF_SENDER_MAX_VIDEO_BITRATE,
                        PREF_SENDER_MAX_VIDEO_BITRATE_DEFAULT));

        return new EncodingParameters(maxAudioBitrate, maxVideoBitrate);
    }

    public EncodingParameters getEncodingParameters() {
        return encodingParameters;
    }

    private void connectToRoom(String roomName) {
        ConnectOptions.Builder connectOptionsBuilder = new ConnectOptions.Builder(accessToken)
                .roomName(roomName);

        /*
         * Add local audio track to connect options to share with participants.
         */
        if (twilioCallListener != null && twilioCallListener.getLocalAudioTrack() != null) {
            connectOptionsBuilder
                    .audioTracks(Collections.singletonList(twilioCallListener.getLocalAudioTrack()));
        }

        /*
         * Add local video track to connect options to share with participants.
         */
        if (twilioCallListener != null && twilioCallListener.getLocalVideoTrack() != null) {
            connectOptionsBuilder.videoTracks(Collections.singletonList(twilioCallListener.getLocalVideoTrack()));
        }

        /*
         * Set the preferred audio and video codec for media.
         */
        connectOptionsBuilder.preferAudioCodecs(Collections.singletonList(audioCodec));
        connectOptionsBuilder.preferVideoCodecs(Collections.singletonList(videoCodec));
        connectOptionsBuilder.enableIceGatheringOnAnyAddressPorts(true);

        /*
         * Set the sender side encoding parameters.
         */
        connectOptionsBuilder.encodingParameters(encodingParameters);

        /*
         * Toggles automatic track subscription. If set to false, the LocalParticipant will receive
         * notifications of track publish events, but will not automatically subscribe to them. If
         * set to true, the LocalParticipant will automatically subscribe to tracks as they are
         * published. If unset, the default is true. Note: This feature is only available for Group
         * Rooms. Toggling the flag in a P2P room does not modify subscription behavior.
         */
        connectOptionsBuilder.enableAutomaticSubscription(enableAutomaticSubscription);

        room = Video.connect(mXmppConnectionService, connectOptionsBuilder.build(), roomListener());
    }


    /*
     * Get the preferred audio codec from shared preferences
     */
    private AudioCodec getAudioCodecPreference(String key, String defaultValue) {
        final String audioCodecName = preferences.getString(key, defaultValue);

        switch (audioCodecName) {
            case IsacCodec.NAME:
                return new IsacCodec();
            case OpusCodec.NAME:
                return new OpusCodec();
            case PcmaCodec.NAME:
                return new PcmaCodec();
            case PcmuCodec.NAME:
                return new PcmuCodec();
            case G722Codec.NAME:
                return new G722Codec();
            default:
                return new OpusCodec();
        }
    }

    /*
     * Get the preferred video codec from shared preferences
     */
    private VideoCodec getVideoCodecPreference(String key, String defaultValue) {
        final String videoCodecName = preferences.getString(key, defaultValue);
        if(Build.MODEL == "Pixel 3" || Build.MODEL == "Pixel 4" ){ //using a plugin to detect mobile model
            return new H264Codec();
        }
        switch (videoCodecName) {
            case Vp8Codec.NAME:
                boolean simulcast = preferences.getBoolean(PREF_VP8_SIMULCAST,
                        PREF_VP8_SIMULCAST_DEFAULT);
                return new Vp8Codec(simulcast);
            case H264Codec.NAME:
                return new H264Codec();
            case Vp9Codec.NAME:
                return new Vp9Codec();
            default:
                return new Vp8Codec();
        }
    }

    public LocalParticipant getLocalParticipant() {
        return localParticipant;
    }

    public List<TwilioCallParticipant> getRemoteParticipants() {
        return callParticipants;
    }

    /*
     * Room events listener
     */
    @SuppressLint("SetTextI18n")
    private Room.Listener roomListener() {
        return new Room.Listener() {
            @Override
            public void onConnected(Room room) {
                localParticipant = room.getLocalParticipant();

                for (RemoteParticipant remoteParticipant : room.getRemoteParticipants()) {
                    TwilioCallParticipant tcallParticipant = new TwilioCallParticipant(remoteParticipant);
                    remoteParticipant.setListener(tcallParticipant.getRemoteParticipantListener());
                    Contact c = getRemoteContact(remoteParticipant.getIdentity());
                    if (c != null) {
                        Bitmap av = mXmppConnectionService.getAvatarService().get(c, (int) mXmppConnectionService.getResources().getDimension(R.dimen.avatar_on_call_screen_size));
                        tcallParticipant.setAvatar(av);
                    }

                    callParticipants.add(tcallParticipant);
                }

                if (twilioCallListener != null) {
                    twilioCallListener.handleConnected(room);
                }
            }

            @Override
            public void onReconnecting(@NonNull Room room, @NonNull TwilioException twilioException) {
                if (twilioCallListener != null) {
                    twilioCallListener.handleReconnecting(true);
                }
            }

            @Override
            public void onReconnected(@NonNull Room room) {
                if (!room.getRemoteParticipants().isEmpty()){
                    if (twilioCallListener != null) {
                        twilioCallListener.handleReconnecting(false);
                    }
                }
            }

            @Override
            public void onConnectFailure(Room room, TwilioException e) {
                if (twilioCallListener != null) {
                    twilioCallListener.handleConnectFailure();
                }
            }

            @Override
            public void onDisconnected(Room room, TwilioException e) {
                releaseLocalParticipantResources();
                localParticipant = null;
                CallManager.this.room = null;
                // Only reinitialize the UI if disconnect was not called from onDestroy()
                if (twilioCallListener != null) {
                    twilioCallListener.endListening();
                }
                callParticipants.clear();
            }

            @Override
            public void onParticipantConnected(Room room, RemoteParticipant remoteParticipant) {
                TwilioCallParticipant tcallParticipant = new TwilioCallParticipant(remoteParticipant);
                remoteParticipant.setListener(tcallParticipant.getRemoteParticipantListener());
                Contact c = getRemoteContact(remoteParticipant.getIdentity());
                if (c != null) {
                    Bitmap av = mXmppConnectionService.getAvatarService().get(c, (int) mXmppConnectionService.getResources().getDimension(R.dimen.avatar_on_call_screen_size));
                    tcallParticipant.setAvatar(av);
                }

                callParticipants.add(tcallParticipant);

                if (twilioCallListener != null) {
                    twilioCallListener.handleParticipantConnected(tcallParticipant);
                }
            }

            @Override
            public void onParticipantDisconnected(Room room, RemoteParticipant remoteParticipant) {
                Iterator<TwilioCallParticipant> iterator = callParticipants.iterator();
                while (iterator.hasNext()) {
                    TwilioCallParticipant party = iterator.next();
                    if (party.getRemoteParticipant().getIdentity().equals(remoteParticipant.getIdentity())) {
                        iterator.remove();
                    }
                }
                if (callParticipants.size() != room.getRemoteParticipants().size()) {
                    Log.d(TAG, "onParticipantDisconnected we have sync issues");
                }

                if (twilioCallListener != null) {
                    twilioCallListener.handleParticipantDisconnected(remoteParticipant);
                }

                // should not disconnect ourselves if still participants in room
                if (room.getRemoteParticipants().size() > 0) {
                    return;
                }

                releaseLocalParticipantResources();
                localParticipant = null;
                handleDisconnect();
            }

            @Override
            public void onRecordingStarted(Room room) {
                /*
                 * Indicates when media shared to a Room is being recorded. Note that
                 * recording is only available in our Group Rooms developer preview.
                 */
                Log.d(TAG, "onRecordingStarted");
            }

            @Override
            public void onRecordingStopped(Room room) {
                /*
                 * Indicates when media shared to a Room is no longer being recorded. Note that
                 * recording is only available in our Group Rooms developer preview.
                 */
                Log.d(TAG, "onRecordingStopped");
            }
        };
    }

    private void releaseLocalParticipantResources() {
        if (localParticipant != null) {
            if (localParticipant.getLocalVideoTracks().size() > 0) {
                LocalVideoTrack lvTrack = localParticipant.getLocalVideoTracks().get(0).getLocalVideoTrack();
                try {
                    localParticipant.unpublishTrack(lvTrack);
                    lvTrack.release();
                } catch (Exception ae) { } //do nothing
            }

            if (localParticipant.getLocalAudioTracks().size() > 0) {
                LocalAudioTrack laTrack = localParticipant.getLocalAudioTracks().get(0).getLocalAudioTrack();
                try {
                    localParticipant.unpublishTrack(laTrack);
                    laTrack.release();
                } catch (Exception ae) { } //do nothing
            }
        }
    }

    public void handleDisconnect() {
        if (room != null) {
            room.disconnect();
        }

        CallManager.this.room = null;
    }
}

