package com.glaciersecurity.glaciermessenger.ui.interfaces;

import com.glaciersecurity.glaciermessenger.entities.TwilioCallParticipant;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.RemoteParticipant;
import com.twilio.video.Room;

public interface TwilioCallListener {
    void handleParticipantConnected(TwilioCallParticipant remoteCallParticipant);
    void handleParticipantDisconnected(RemoteParticipant remoteParticipant);

    void handleConnected(Room room);
    void handleReconnecting(boolean reconnecting);
    void handleConnectFailure();
    void endListening();

    LocalAudioTrack getLocalAudioTrack();
    LocalVideoTrack getLocalVideoTrack();
}
