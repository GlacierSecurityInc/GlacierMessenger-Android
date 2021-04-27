package com.glaciersecurity.glaciermessenger.entities;

public class TwilioCall {

    protected Account taccount;
    protected String tcaller;
    protected String treceiver;
    protected String ttoken;
    protected String troomname;
    protected String troomtitle;
    protected String tstatus;
    protected int tcallid;
    protected long tcalltime = 0;

    public TwilioCall(Account account) {
        taccount = account;
    }

    public Account getAccount() {
        return taccount;
    }

    public String getCaller() {
        return tcaller;
    }

    public void setCaller(String caller) {
        tcaller = caller;
    }

    public String getReceiver() {
        return treceiver;
    }

    public void setReceiver(String receiver) {
        treceiver = receiver;
    }

    public String getToken() {
        return ttoken;
    }

    public void setToken(String token) {
        ttoken = token;
    }

    public String getRoomName() {
        return troomname;
    }

    public void setRoomName(String roomname) {
        troomname = roomname;
    }

    public String getRoomTitle() {
        return troomtitle;
    }

    public void setRoomTitle(String roomtitle) {
        troomtitle = roomtitle;
    }

    public String getStatus() {
        return tstatus;
    }

    public void setStatus(String status) {
        tstatus = status;
    }

    public int getCallId() {
        return tcallid;
    }

    public void setCallId(int callid) {
        tcallid = callid;
    }

    public long getCallTime() {
        return tcalltime;
    }

    public void setCallTime(long calltime) {
        tcalltime = calltime;
    }

    public TwilioCall getCopy() {
        TwilioCall newcall = new TwilioCall(getAccount());
        newcall.setCallId(getCallId());
        newcall.setCaller(getCaller());
        newcall.setReceiver(getReceiver());
        newcall.setToken(getToken());
        newcall.setRoomName(getRoomName());
        newcall.setStatus(getStatus());
        newcall.setCallTime(getCallTime());
        return newcall;
    }
}
