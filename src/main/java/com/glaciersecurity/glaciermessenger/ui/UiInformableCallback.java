package com.glaciersecurity.glaciermessenger.ui;

public interface UiInformableCallback<T> extends UiCallback<T> {
    void inform(String text);
}
