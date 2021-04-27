package com.glaciersecurity.glaciermessenger.entities;

import androidx.annotation.NonNull;

import java.lang.Comparable;
import java.util.Locale;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.xml.Element;

public class Presence implements Comparable {

	public enum Status {
		CHAT, ONLINE, AWAY, XA, DND, OFFLINE;

		public String toShowString() {
			switch(this) {
				case CHAT: return "chat";
				case AWAY: return "away";
				case XA:   return "xa";
				case DND:  return "dnd";
			}
			return null;
		}

		public String toDisplayString() {
			switch(this) {
				case ONLINE: return "Online";
				case AWAY: return "Away";
				case DND:  return "Do Not Disturb";
				case XA:   return "Offline";
				case OFFLINE: return "Offline";
				case CHAT: return "Online";
			}
			return null;
		}


		public int getStatusIcon() {
			switch (this) {
				case ONLINE:
					return R.drawable.ic_green;
				case CHAT:
					return R.drawable.ic_green;
				case AWAY:
					return R.drawable.ic_orange;
				case OFFLINE:
					return R.drawable.ic_grey;
				case XA:
					return R.drawable.ic_grey;
				case DND:
					return R.drawable.ic_red;
				default:
					return R.drawable.ic_green;
			}

		}

		public int getStatusIconMenu() {
			switch (this) {
				case ONLINE:
					return R.drawable.ic_green_small;
				case CHAT:
					return R.drawable.ic_green_small;
				case AWAY:
					return R.drawable.ic_orange_small;
				case OFFLINE:
					return R.drawable.ic_grey_small;
				case XA:
					return R.drawable.ic_grey_small;
				case DND:
					return R.drawable.ic_red_small;
				default:
					return R.drawable.ic_green_small;
			}

		}

		public static Status fromShowString(String show) {
			if (show == null) {
				return ONLINE;
			} else {
				switch (show.toLowerCase(Locale.US)) {
					case "away":
						return AWAY;
					case "xa":
						return XA;
					case "dnd":
						return DND;
					case "chat":
						return CHAT;
					default:
						return ONLINE;
				}
			}
		}
	}

	public static String getEmojiByUnicode(int unicode){
		return new String(Character.toChars(unicode));
	}

	public enum StatusMessage {
		IN_MEETING, ON_TRAVEL, OUT_SICK, VACATION, CUSTOM;

		public final static int meetingIcon = 0x1F4C5; 	// could use other calendar 0x1F4C6
		public final static int travelIcon = 0x1F6EB;  	// could use rocket ship 0x1F680
		public final static int sickIcon = 0x1F912;
		public final static int vacationIcon = 0x1F334;
		public final static int customIcon = 0x1F4AC;  	// could use yellow notepad 0x1F4D2

		public String toShowString() {
			switch(this) {
				case IN_MEETING: return getEmojiByUnicode(meetingIcon)+"\tIn a meeting";
				case ON_TRAVEL: return getEmojiByUnicode(travelIcon)+"\tOn travel";
				case OUT_SICK:   return  getEmojiByUnicode(sickIcon)+"\tOut sick";
				case VACATION:  return getEmojiByUnicode(vacationIcon)+ "\tVacation" ;
			}
			return "";
		}

		public String toShowIcon() {
			switch(this) {
				case IN_MEETING: return getEmojiByUnicode(meetingIcon);
				case ON_TRAVEL: return getEmojiByUnicode(travelIcon);
				case OUT_SICK:   return  getEmojiByUnicode(sickIcon);
				case VACATION:  return getEmojiByUnicode(vacationIcon);
				case CUSTOM: return getEmojiByUnicode(customIcon);
			}
			return "";
		}
	}

	private final Status status;
	private ServiceDiscoveryResult disco;
	private final String ver;
	private final String hash;
	private final String node;
	private final String message;

	private Presence(Status status, String ver, String hash, String node, String message) {
		this.status = status;
		this.ver = ver;
		this.hash = hash;
		this.node = node;
		this.message = message;
	}

	public static Presence parse(String show, Element caps, String message) {
		final String hash = caps == null ? null : caps.getAttribute("hash");
		final String ver = caps == null ? null : caps.getAttribute("ver");
		final String node = caps == null ? null : caps.getAttribute("node");
		return new Presence(Status.fromShowString(show), ver, hash, node, message);
	}

	public int compareTo(@NonNull Object other) {
		return this.status.compareTo(((Presence)other).status);
	}

	public Status getStatus() {
		return this.status;
	}

	public boolean hasCaps() {
		return ver != null && hash != null;
	}

	public String getVer() {
		return this.ver;
	}

	public String getNode() {
		return this.node;
	}

	public String getHash() {
		return this.hash;
	}

	public String getMessage() {
		return this.message;
	}

	public void setServiceDiscoveryResult(ServiceDiscoveryResult disco) {
		this.disco = disco;
	}

	public ServiceDiscoveryResult getServiceDiscoveryResult() {
		return disco;
	}
}
