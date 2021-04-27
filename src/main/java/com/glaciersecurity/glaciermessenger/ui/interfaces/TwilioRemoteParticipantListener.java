package com.glaciersecurity.glaciermessenger.ui.interfaces;

import com.twilio.video.RemoteVideoTrack;

public interface TwilioRemoteParticipantListener {
    void handleAddRemoteParticipantVideo(RemoteVideoTrack videoTrack);
    void handleRemoveRemoteParticipantVideo(RemoteVideoTrack videoTrack);
    void handleVideoTrackEnabled();
    void handleVideoTrackDisabled();
    void handleAudioTrackEnabled();
    void handleAudioTrackDisabled();
}
