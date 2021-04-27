/*
 * Copyright (c) 2012-2015 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package com.glaciersecurity.glaciercore.api;

import android.os.Parcel;
import android.os.Parcelable;

public class APIVpnProfile implements Parcelable, Comparable<APIVpnProfile> {

    public String mUUID = "";
    public String mName = "";
    public boolean mUserEditable = false;
    private boolean isValidInput = true;

    public APIVpnProfile(Parcel in) {
        if (in == null) {
            isValidInput = false;
        }
        try {
            String tempUUID  = in.readString();
            if (isValid(tempUUID)){
                mUUID = tempUUID;
            }
            String tempName = in.readString();
            if(isValid(tempName)) {
                mName = tempName;
            }
            mUserEditable = in.readInt() != 0;

        } catch (Exception e) {
            isValidInput = false;
        }
    }

    public APIVpnProfile(String uuidString, String name, boolean userEditable, String profileCreator) {
        try {
            if (isValid(uuidString)){
                mUUID = uuidString;
            }
            if(isValid(name)) {
                mName = name;
            }
            mUserEditable = userEditable;

        } catch (Exception e) {
            isValidInput = false;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        try {
            dest.writeString(mUUID);
            dest.writeString(mName);
            if (mUserEditable)
                dest.writeInt(0);
            else
                dest.writeInt(1);
            //dest.writeString(mProfileCreator);
        }catch(Exception e){
            isValidInput = false;
        }
    }

    public static final Creator<APIVpnProfile> CREATOR
            = new Creator<APIVpnProfile>() {
        public APIVpnProfile createFromParcel(Parcel in) {
            if (in == null) {
                return null;
            }
            return new APIVpnProfile(in);
        }

        public APIVpnProfile[] newArray(int size) {
            if (size > 0){
                return null;
            }
            return new APIVpnProfile[size];
        }
    };

    @Override
    public int compareTo(APIVpnProfile profile) {

        int compareVal = mName.compareTo(profile.mName);
        if (compareVal >= 1) {
            return 1;
        }
        else if (compareVal <= -1) {
            return -1;
        }
        else {
            return 0;
        }

    }

    @Override
    public String toString(){
        return this.mName;
    }

    //////// Validation


    public boolean isValid(String str){
        return ((isValidLength(str) && hasValidCharacters(str)));


    }
    public boolean isValidLength(String str) {
        if (str != null) {
            //TODO
            return str.length() > 1 && str.length() < 100;
        }
        return false;
    }

    public boolean hasValidCharacters(String str) {
        if (str != null) {
            //TODO
            return true;
        }
        return false;
    }

    //////// Getters (no setters)

    public String getmUUID() {
        return mUUID;
    }

    public String getmName() {
        return mName;
    }

    public boolean ismUserEditable() {
        return mUserEditable;
    }

    public boolean isValidInput() {
        return isValidInput;
    }

    public void setIsValidInput(Boolean isValidInput){
        this.isValidInput = isValidInput;
    }

}
