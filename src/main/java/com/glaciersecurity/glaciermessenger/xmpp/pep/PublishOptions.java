package com.glaciersecurity.glaciermessenger.xmpp.pep;

import android.os.Bundle;

public class PublishOptions {

    private PublishOptions() {

    }

    public static Bundle openAccess() {
        final Bundle options = new Bundle();
        options.putString("pubsub#access_model","open");
        return options;
    }

}
