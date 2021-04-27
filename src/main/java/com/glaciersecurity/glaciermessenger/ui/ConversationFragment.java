package com.glaciersecurity.glaciermessenger.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.crypto.axolotl.AxolotlService;
import com.glaciersecurity.glaciermessenger.crypto.axolotl.FingerprintStatus;
import com.glaciersecurity.glaciermessenger.databinding.FragmentConversationBinding;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.Blockable;
import com.glaciersecurity.glaciermessenger.entities.Contact;
import com.glaciersecurity.glaciermessenger.entities.Conversation;
import com.glaciersecurity.glaciermessenger.entities.Conversational;
import com.glaciersecurity.glaciermessenger.entities.DownloadableFile;
import com.glaciersecurity.glaciermessenger.entities.Message;
import com.glaciersecurity.glaciermessenger.entities.MucOptions;
import com.glaciersecurity.glaciermessenger.entities.Presence;
import com.glaciersecurity.glaciermessenger.entities.ReadByMarker;
import com.glaciersecurity.glaciermessenger.entities.Transferable;
import com.glaciersecurity.glaciermessenger.entities.TransferablePlaceholder;
import com.glaciersecurity.glaciermessenger.entities.TwilioCall;
import com.glaciersecurity.glaciermessenger.http.HttpDownloadConnection;
import com.glaciersecurity.glaciermessenger.persistance.FileBackend;
import com.glaciersecurity.glaciermessenger.services.MessageArchiveService;
import com.glaciersecurity.glaciermessenger.services.QuickConversationsService;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;
import com.glaciersecurity.glaciermessenger.ui.adapter.MediaPreviewAdapter;
import com.glaciersecurity.glaciermessenger.ui.adapter.MessageAdapter;
import com.glaciersecurity.glaciermessenger.ui.util.ActivityResult;
import com.glaciersecurity.glaciermessenger.ui.util.Attachment;
import com.glaciersecurity.glaciermessenger.ui.util.ConversationMenuConfigurator;
import com.glaciersecurity.glaciermessenger.ui.util.DateSeparator;
import com.glaciersecurity.glaciermessenger.ui.util.DelayedHintHelper;
import com.glaciersecurity.glaciermessenger.ui.util.EditMessageActionModeCallback;
import com.glaciersecurity.glaciermessenger.ui.util.ListViewUtils;
import com.glaciersecurity.glaciermessenger.ui.util.MenuDoubleTabUtil;
import com.glaciersecurity.glaciermessenger.ui.util.MucDetailsContextMenuHelper;
import com.glaciersecurity.glaciermessenger.ui.util.PendingItem;
import com.glaciersecurity.glaciermessenger.ui.util.PresenceSelector;
import com.glaciersecurity.glaciermessenger.ui.util.ScrollState;
import com.glaciersecurity.glaciermessenger.ui.util.SendButtonAction;
import com.glaciersecurity.glaciermessenger.ui.util.SendButtonTool;
import com.glaciersecurity.glaciermessenger.ui.util.ShareUtil;
import com.glaciersecurity.glaciermessenger.ui.util.ViewUtil;
import com.glaciersecurity.glaciermessenger.ui.widget.EditMessage;
import com.glaciersecurity.glaciermessenger.utils.AccountUtils;
import com.glaciersecurity.glaciermessenger.utils.Compatibility;
import com.glaciersecurity.glaciermessenger.utils.GeoHelper;
import com.glaciersecurity.glaciermessenger.utils.MessageUtils;
import com.glaciersecurity.glaciermessenger.utils.NickValidityChecker;
import com.glaciersecurity.glaciermessenger.utils.Patterns;
import com.glaciersecurity.glaciermessenger.utils.QuickLoader;
import com.glaciersecurity.glaciermessenger.utils.StylingHelper;
import com.glaciersecurity.glaciermessenger.utils.TimeframeUtils;
import com.glaciersecurity.glaciermessenger.utils.UIHelper;
import com.glaciersecurity.glaciermessenger.xmpp.XmppConnection;
import com.glaciersecurity.glaciermessenger.xmpp.chatstate.ChatState;
import com.glaciersecurity.glaciermessenger.xmpp.jingle.JingleConnection;

import rocks.xmpp.addr.Jid;

import static com.glaciersecurity.glaciermessenger.ui.XmppActivity.EXTRA_ACCOUNT;
import static com.glaciersecurity.glaciermessenger.ui.XmppActivity.REQUEST_INVITE_TO_CONVERSATION;
import static com.glaciersecurity.glaciermessenger.ui.util.SoftKeyboardUtils.hideSoftKeyboard;
import static com.glaciersecurity.glaciermessenger.utils.PermissionUtils.allGranted;
import static com.glaciersecurity.glaciermessenger.utils.PermissionUtils.getFirstDenied;
import static com.glaciersecurity.glaciermessenger.utils.PermissionUtils.writeGranted;


public class ConversationFragment extends XmppFragment implements EditMessage.KeyboardListener, MessageAdapter.OnContactPictureLongClicked, MessageAdapter.OnContactPictureClicked{

	public static final int REQUEST_SEND_MESSAGE = 0x0201;
	public static final int REQUEST_DECRYPT_PGP = 0x0202;
	public static final int REQUEST_ENCRYPT_MESSAGE = 0x0207;
	public static final int REQUEST_TRUST_KEYS_TEXT = 0x0208;
	public static final int REQUEST_TRUST_KEYS_ATTACHMENTS = 0x0209;
	public static final int REQUEST_START_DOWNLOAD = 0x0210;
	public static final int REQUEST_ADD_EDITOR_CONTENT = 0x0211;
	public static final int REQUEST_COMMIT_ATTACHMENTS = 0x0212;
	public static final int ATTACHMENT_CHOICE_CHOOSE_IMAGE = 0x0301;
	public static final int ATTACHMENT_CHOICE_TAKE_PHOTO = 0x0302;
	public static final int ATTACHMENT_CHOICE_CHOOSE_FILE = 0x0303;
	public static final int ATTACHMENT_CHOICE_RECORD_VOICE = 0x0304;
	public static final int ATTACHMENT_CHOICE_LOCATION = 0x0305;
	public static final int ATTACHMENT_CHOICE_INVALID = 0x0306;
	public static final int ATTACHMENT_CHOICE_RECORD_VIDEO = 0x0307;

	private static final int CAMERA_MIC_PERMISSION_REQUEST_CODE = 0x0308;


	//public static final String RECENTLY_USED_QUICK_ACTION = "recently_used_quick_action";
	public static final String STATE_CONVERSATION_UUID = ConversationFragment.class.getName() + ".uuid";
	public static final String STATE_SCROLL_POSITION = ConversationFragment.class.getName() + ".scroll_position";
	public static final String STATE_PHOTO_URI = ConversationFragment.class.getName() + ".take_photo_uri";
	public static final String STATE_MEDIA_PREVIEWS = ConversationFragment.class.getName() + ".take_photo_uri";
	private static final String STATE_LAST_MESSAGE_UUID = "state_last_message_uuid";

	private final List<Message> messageList = new ArrayList<>();
	private final PendingItem<ActivityResult> postponedActivityResult = new PendingItem<>();
	private final PendingItem<String> pendingConversationsUuid = new PendingItem<>();
	private final PendingItem<ArrayList<Attachment>> pendingMediaPreviews = new PendingItem<>();
	private final PendingItem<Bundle> pendingExtras = new PendingItem<>();
	private final PendingItem<Uri> pendingTakePhotoUri = new PendingItem<>();
	private final PendingItem<ScrollState> pendingScrollState = new PendingItem<>();
	private final PendingItem<String> pendingLastMessageUuid = new PendingItem<>();
	private final PendingItem<Message> pendingMessage = new PendingItem<>();
	public Uri mPendingEditorContent = null;
	protected MessageAdapter messageListAdapter;
	private MediaPreviewAdapter mediaPreviewAdapter;
	private String lastMessageUuid = null;
	private Conversation conversation;
	private FragmentConversationBinding binding;
	private Toast messageLoaderToast;
	private ConversationsActivity activity;
	private boolean reInitRequiredOnStart = true;
	private Handler disappearingHandler;
	private Runnable disRunnable;
	private String lastGroupRemoved = null;

	protected OnClickListener clickToContactDetails = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if(conversation != null) {
				if (conversation.getMode() == Conversation.MODE_MULTI) {
					Intent intent = new Intent(getActivity(), ConferenceDetailsActivity.class);
					intent.setAction(ConferenceDetailsActivity.ACTION_VIEW_MUC);
					intent.putExtra("uuid", conversation.getUuid());
					startActivity(intent);
				} else {
					activity.switchToContactDetails(conversation.getContact());
				}
			}
		}
	};

	private OnClickListener clickToMuc = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(getActivity(), ConferenceDetailsActivity.class);
			intent.setAction(ConferenceDetailsActivity.ACTION_VIEW_MUC);
			intent.putExtra("uuid", conversation.getUuid());
			startActivity(intent);
		}
	};
	private OnClickListener leaveMuc = new OnClickListener() {

		@Override
		public void onClick(View v) {
			activity.xmppConnectionService.archiveConversation(conversation);
		}
	};
	private OnClickListener joinMuc = new OnClickListener() {

		@Override
		public void onClick(View v) {
			activity.xmppConnectionService.joinMuc(conversation);
		}
	};

	private OnClickListener acceptJoin = new OnClickListener() {
		@Override
		public void onClick(View v) {
			conversation.setAttribute("accept_non_anonymous",true);
			activity.xmppConnectionService.updateConversation(conversation);
			activity.xmppConnectionService.joinMuc(conversation);
		}
	};

	private OnClickListener enterPassword = new OnClickListener() {

		@Override
		public void onClick(View v) {
			MucOptions muc = conversation.getMucOptions();
			String password = muc.getPassword();
			if (password == null) {
				password = "";
			}
			activity.quickPasswordEdit(password, value -> {
				activity.xmppConnectionService.providePasswordForMuc(conversation, value);
				return null;
			});
		}
	};
	private OnScrollListener mOnScrollListener = new OnScrollListener() {

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			if (AbsListView.OnScrollListener.SCROLL_STATE_IDLE == scrollState) {
				fireReadEvent();
			}
		}

		@Override
		public void onScroll(final AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

			toggleScrollDownButton(view);
			synchronized (ConversationFragment.this.messageList) {
				activity.updateOfflineStatusBar();
				if (firstVisibleItem < 5 && conversation != null && conversation.messagesLoaded.compareAndSet(true, false) && messageList.size() > 0) {
					long timestamp;
					if (messageList.get(0).getType() == Message.TYPE_STATUS && messageList.size() >= 2) {
						timestamp = messageList.get(1).getTimeSent();
					} else {
						timestamp = messageList.get(0).getTimeSent();
					}
					activity.xmppConnectionService.loadMoreMessages(conversation, timestamp, new XmppConnectionService.OnMoreMessagesLoaded() {
						@Override
						public void onMoreMessagesLoaded(final int c, final Conversation conversation) {
							if (ConversationFragment.this.conversation != conversation) {
								conversation.messagesLoaded.set(true);
								return;
							}
							runOnUiThread(() -> {
								synchronized (messageList) {
									final int oldPosition = binding.messagesView.getFirstVisiblePosition();
									Message message = null;
									int childPos;
									for (childPos = 0; childPos + oldPosition < messageList.size(); ++childPos) {
										message = messageList.get(oldPosition + childPos);
										if (message.getType() != Message.TYPE_STATUS) {
											break;
										}
									}
									final String uuid = message != null ? message.getUuid() : null;
									View v = binding.messagesView.getChildAt(childPos);
									final int pxOffset = (v == null) ? 0 : v.getTop();
									ConversationFragment.this.conversation.populateWithMessages(ConversationFragment.this.messageList);
									try {
										updateStatusMessages();
									} catch (IllegalStateException e) {
										Log.d(Config.LOGTAG, "caught illegal state exception while updating status messages");
									}
									messageListAdapter.notifyDataSetChanged();
									int pos = Math.max(getIndexOf(uuid, messageList), 0);
									binding.messagesView.setSelectionFromTop(pos, pxOffset);
									if (messageLoaderToast != null) {
										messageLoaderToast.cancel();
									}
									conversation.messagesLoaded.set(true);
								}
							});
						}

						@Override
						public void informUser(final int resId) {

							runOnUiThread(() -> {
								if (messageLoaderToast != null) {
									messageLoaderToast.cancel();
								}
								if (ConversationFragment.this.conversation != conversation) {
									return;
								}
								messageLoaderToast = Toast.makeText(view.getContext(), resId, Toast.LENGTH_LONG);
								messageLoaderToast.show();
							});

						}
					});

				}
			}
		}
	};
	private EditMessage.OnCommitContentListener mEditorContentListener = new EditMessage.OnCommitContentListener() {
		@Override
		public boolean onCommitContent(InputContentInfoCompat inputContentInfo, int flags, Bundle opts, String[] contentMimeTypes) {
			// try to get permission to read the image, if applicable
			if ((flags & InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0) {
				try {
					inputContentInfo.requestPermission();
				} catch (Exception e) {
					Log.e(Config.LOGTAG, "InputContentInfoCompat#requestPermission() failed.", e);
					Toast.makeText(getActivity(), activity.getString(R.string.no_permission_to_access_x, inputContentInfo.getDescription()), Toast.LENGTH_LONG
					).show();
					return false;
				}
			}
			if (hasPermissions(REQUEST_ADD_EDITOR_CONTENT, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
				attachEditorContentToConversation(inputContentInfo.getContentUri());
			} else {
				mPendingEditorContent = inputContentInfo.getContentUri();
			}
			return true;
		}
	};
	private Message selectedMessage;
	private OnClickListener mEnableAccountListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			final Account account = conversation == null ? null : conversation.getAccount();
			if (account != null) {
				account.setOption(Account.OPTION_DISABLED, false);
				activity.xmppConnectionService.updateAccount(account);
			}
		}
	};
	private OnClickListener mUnblockClickListener = new OnClickListener() {
		@Override
		public void onClick(final View v) {
			v.post(() -> v.setVisibility(View.INVISIBLE));
			if (conversation.isDomainBlocked()) {
				BlockContactDialog.show(activity, conversation);
			} else {
				unblockConversation(conversation);
			}
		}
	};
	private OnClickListener mBlockClickListener = this::showBlockSubmenu;
	private OnClickListener mAddBackClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			final Contact contact = conversation == null ? null : conversation.getContact();
			if (contact != null) {
				activity.xmppConnectionService.createContact(contact, true);
				activity.switchToContactDetails(contact);
			}
		}
	};
	private View.OnLongClickListener mLongPressBlockListener = this::showBlockSubmenu;
	private OnClickListener mAllowPresenceSubscription = new OnClickListener() {
		@Override
		public void onClick(View v) {
			final Contact contact = conversation == null ? null : conversation.getContact();
			if (contact != null) {
				activity.xmppConnectionService.sendPresencePacket(contact.getAccount(),
						activity.xmppConnectionService.getPresenceGenerator()
								.sendPresenceUpdatesTo(contact));
				hideSnackbar();
			}
		}
	};
	protected OnClickListener clickToDecryptListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			/*PendingIntent pendingIntent = conversation.getAccount().getPgpDecryptionService().getPendingIntent();
			if (pendingIntent != null) {
				try {
					getActivity().startIntentSenderForResult(pendingIntent.getIntentSender(),
							REQUEST_DECRYPT_PGP,
							null,
							0,
							0,
							0);
				} catch (SendIntentException e) {
					Toast.makeText(getActivity(), R.string.unable_to_connect_to_keychain, Toast.LENGTH_SHORT).show();
					conversation.getAccount().getPgpDecryptionService().continueDecryption(true);
				}
			}*/
			updateSnackBar(conversation);
		}
	};
	private AtomicBoolean mSendingPgpMessage = new AtomicBoolean(false);
	private OnEditorActionListener mEditorActionListener = (v, actionId, event) -> {
		if (actionId == EditorInfo.IME_ACTION_SEND) {
			InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
			if (imm != null && imm.isFullscreenMode()) {
				imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
			}
			sendMessage();
			return true;
		} else {
			return false;
		}
	};
	private OnClickListener mScrollButtonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			stopScrolling();
			setSelection(binding.messagesView.getCount() - 1, true);
		}
	};
	private OnClickListener mSendButtonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Object tag = v.getTag();
			if (tag instanceof SendButtonAction) {
				SendButtonAction action = (SendButtonAction) tag;
				switch (action) {
					case TAKE_PHOTO:
					case RECORD_VIDEO:
					case SEND_LOCATION:
					case RECORD_VOICE:
					case CHOOSE_PICTURE:
						attachFile(action.toChoice());
						break;
					case CANCEL:
						if (conversation != null) {
							if (conversation.setCorrectingMessage(null)) {
								binding.textinput.setText("");
								binding.textinput.append(conversation.getDraftMessage());
								conversation.setDraftMessage(null);
							} else if (conversation.getMode() == Conversation.MODE_MULTI) {
								conversation.setNextCounterpart(null);
							}
							updateChatMsgHint();
							updateSendButton();
							updateEditablity();
						}
						break;
					default:
						sendMessage();
				}
			} else {
				sendMessage();
			}
		}
	};
	private int completionIndex = 0;
	private int lastCompletionLength = 0;
	private String incomplete;
	private int lastCompletionCursor;
	private boolean firstWord = false;
	private Message mPendingDownloadableMessage;

	private static ConversationFragment findConversationFragment(Activity activity) {
		Fragment fragment = activity.getFragmentManager().findFragmentById(R.id.main_fragment);
		if (fragment instanceof ConversationFragment) {
			return (ConversationFragment) fragment;
		}
		fragment = activity.getFragmentManager().findFragmentById(R.id.secondary_fragment);
		if (fragment instanceof ConversationFragment) {
			return (ConversationFragment) fragment;
		}
		return null;
	}

	public static void startStopPending(Activity activity) {
		ConversationFragment fragment = findConversationFragment(activity);
		if (fragment != null) {
			fragment.messageListAdapter.startStopPending();
		}
	}

	public static void downloadFile(Activity activity, Message message) {
		ConversationFragment fragment = findConversationFragment(activity);
		if (fragment != null) {
			fragment.startDownloadable(message);
		}
	}

	public static void registerPendingMessage(Activity activity, Message message) {
		ConversationFragment fragment = findConversationFragment(activity);
		if (fragment != null) {
			fragment.pendingMessage.push(message);
		}
	}

	public static void openPendingMessage(Activity activity) {
		ConversationFragment fragment = findConversationFragment(activity);
		if (fragment != null) {
			Message message = fragment.pendingMessage.pop();
			if (message != null) {
				fragment.messageListAdapter.openDownloadable(message);
			}
		}
	}

	public static Conversation getConversation(Activity activity) {
		return getConversation(activity, R.id.secondary_fragment);
	}

	private static Conversation getConversation(Activity activity, @IdRes int res) {
		final Fragment fragment = activity.getFragmentManager().findFragmentById(res);
		if (fragment instanceof ConversationFragment) {
			return ((ConversationFragment) fragment).getConversation();
		} else {
			return null;
		}
	}

	public static ConversationFragment get(Activity activity) {
		FragmentManager fragmentManager = activity.getFragmentManager();
		Fragment fragment = fragmentManager.findFragmentById(R.id.main_fragment);
		if (fragment instanceof ConversationFragment) {
			return (ConversationFragment) fragment;
		} else {
			fragment = fragmentManager.findFragmentById(R.id.secondary_fragment);
			return fragment instanceof ConversationFragment ? (ConversationFragment) fragment : null;
		}
	}

	public static Conversation getConversationReliable(Activity activity) {
		final Conversation conversation = getConversation(activity, R.id.secondary_fragment);
		if (conversation != null) {
			return conversation;
		}
		return getConversation(activity, R.id.main_fragment);
	}

	private static boolean scrolledToBottom(AbsListView listView) {
		final int count = listView.getCount();
		if (count == 0) {
			return true;
		} else if (listView.getLastVisiblePosition() == count - 1) {
			final View lastChild = listView.getChildAt(listView.getChildCount() - 1);
			return lastChild != null && lastChild.getBottom() <= listView.getHeight();
		} else {
			return false;
		}
	}

	private void toggleScrollDownButton() {
		toggleScrollDownButton(binding.messagesView);
	}

	private void toggleScrollDownButton(AbsListView listView) {
		if (conversation == null) {
			return;
		}
		if (scrolledToBottom(listView)) {
			lastMessageUuid = null;
			hideUnreadMessagesCount();
		} else {
			binding.scrollToBottomButton.setEnabled(true);
			binding.scrollToBottomButton.show();
			if (lastMessageUuid == null) {
				lastMessageUuid = conversation.getLatestMessage().getUuid();
			}
			if (conversation.getReceivedMessagesCountSinceUuid(lastMessageUuid) > 0) {
				binding.unreadCountCustomView.setVisibility(View.VISIBLE);
			}
		}
	}

	private int getIndexOf(String uuid, List<Message> messages) {
		if (uuid == null) {
			return messages.size() - 1;
		}
		for (int i = 0; i < messages.size(); ++i) {
			if (uuid.equals(messages.get(i).getUuid())) {
				return i;
			} else {
				Message next = messages.get(i);
				while (next != null && next.wasMergedIntoPrevious()) {
					if (uuid.equals(next.getUuid())) {
						return i;
					}
					next = next.next();
				}

			}
		}
		return -1;
	}

	private ScrollState getScrollPosition() {
		final ListView listView = this.binding == null ? null : this.binding.messagesView;
		if (listView == null || listView.getCount() == 0 || listView.getLastVisiblePosition() == listView.getCount() - 1) {
			return null;
		} else {
			final int pos = listView.getFirstVisiblePosition();
			final View view = listView.getChildAt(0);
			if (view == null) {
				return null;
			} else {
				return new ScrollState(pos, view.getTop());
			}
		}
	}

	private void setScrollPosition(ScrollState scrollPosition, String lastMessageUuid) {
		if (scrollPosition != null) {

			this.lastMessageUuid = lastMessageUuid;
			if (lastMessageUuid != null) {
				binding.unreadCountCustomView.setUnreadCount(conversation.getReceivedMessagesCountSinceUuid(lastMessageUuid));
			}
			//TODO maybe this needs a 'post'
			this.binding.messagesView.setSelectionFromTop(scrollPosition.position, scrollPosition.offset);
			toggleScrollDownButton();
		}
	}

	private void attachLocationToConversation(Conversation conversation, Uri uri) {
		if (conversation == null) {
			return;
		}
		activity.xmppConnectionService.attachLocationToConversation(conversation, uri, new UiCallback<Message>() {

			@Override
			public void success(Message message) {

			}

			@Override
			public void error(int errorCode, Message object) {
				//TODO show possible pgp error
			}

			@Override
			public void userInputRequried(PendingIntent pi, Message object) {

			}
		});
	}

	private void showAttachFileProgress() {
		showCompressionDialog(this.getString(R.string.compressing_file_dialog_message));
		// update UI
	}

	/**
	 * Close progress dialog
	 */
	private void closeCompressionDialog() {
		if (compressDialog != null && compressDialog.isShowing()){
			compressDialog.dismiss();
		}
	}

	static private ProgressDialog compressDialog;
	/**
	 * Display progress dialog
	 *
	 * @param message
	 */
	public void showCompressionDialog(String message) {
		if (compressDialog != null) {
			compressDialog.dismiss();
		}
		compressDialog = new ProgressDialog(this.getActivity());
		compressDialog.setMessage(message); // Setting Message
		compressDialog.setTitle("Compression Progress"); // Setting Title
		compressDialog.setMax(100);
		compressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		compressDialog.show(); // Display Progress Dialog

		new Thread(() -> {
			try {
				while (compressDialog != null && (compressDialog.getProgress() <= compressDialog
						.getMax())) {
					Thread.sleep(200);
					compressDialog.setProgress(activity.xmppConnectionService.getCompressionPercent());

					if (compressDialog != null && compressDialog.isShowing() &&
							(compressDialog.getProgress() >= compressDialog.getMax() -1)) {
						compressDialog.dismiss();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}

	private void attachFileToConversation(Conversation conversation, Uri uri, String type) {
		if (conversation == null) {
			return;
		}
		final Toast prepareFileToast = Toast.makeText(getActivity(), getText(R.string.preparing_file), Toast.LENGTH_LONG);
		prepareFileToast.show();

		if(type != null && type.contains("video")){
			new Handler().postDelayed(() -> showAttachFileProgress(), 2000);
		}


		activity.delegateUriPermissionsToService(uri);
		activity.xmppConnectionService.attachFileToConversation(conversation, uri, type, new UiInformableCallback<Message>() {
			@Override
			public void inform(final String text) {
				hidePrepareFileToast(prepareFileToast);
				runOnUiThread(() -> activity.replaceToast(text));
			}

			@Override
			public void success(Message message) {
				runOnUiThread(() -> activity.hideToast());
				hidePrepareFileToast(prepareFileToast);
			}

			@Override
			public void error(final int errorCode, Message message) {
				closeCompressionDialog();
				hidePrepareFileToast(prepareFileToast);
				runOnUiThread(() -> activity.replaceToast(getString(errorCode)));

			}

			@Override
			public void userInputRequried(PendingIntent pi, Message message) {
				hidePrepareFileToast(prepareFileToast);
			}
		});
	}

	public void attachEditorContentToConversation(Uri uri) {
		mediaPreviewAdapter.addMediaPreviews(Attachment.of(getActivity(), uri, Attachment.Type.FILE));
		toggleInputMethod();
	}

	private void attachImageToConversation(Conversation conversation, Uri uri) {
		if (conversation == null) {
			return;
		}
		final Toast prepareFileToast = Toast.makeText(getActivity(), getText(R.string.preparing_image), Toast.LENGTH_LONG);
		prepareFileToast.show();
		activity.delegateUriPermissionsToService(uri);
		activity.xmppConnectionService.attachImageToConversation(conversation, uri,
				new UiCallback<Message>() {

					@Override
					public void userInputRequried(PendingIntent pi, Message object) {
						hidePrepareFileToast(prepareFileToast);
					}

					@Override
					public void success(Message message) {
						hidePrepareFileToast(prepareFileToast);
					}

					@Override
					public void error(final int error, Message message) {
						hidePrepareFileToast(prepareFileToast);
						activity.runOnUiThread(() -> activity.replaceToast(getString(error)));
					}
				});
	}

	private void hidePrepareFileToast(final Toast prepareFileToast) {
		if (prepareFileToast != null && activity != null) {
			activity.runOnUiThread(prepareFileToast::cancel);
		}
	}

	private void sendMessage() {
		if (mediaPreviewAdapter.hasAttachments()) {
			commitAttachments();
			return;
		}
		final Editable text = this.binding.textinput.getText();
		final String body = text == null ? "" : text.toString();
		final Conversation conversation = this.conversation;
		if (body.length() == 0 || conversation == null) {
			return;
		}
		if (conversation.getNextEncryption() == Message.ENCRYPTION_AXOLOTL && trustKeysIfNeeded(REQUEST_TRUST_KEYS_TEXT)) {
			return;
		}
		final Message message;
		if (conversation.getCorrectingMessage() == null) {
			message = new Message(conversation, body, conversation.getNextEncryption());
			if (conversation.getMode() == Conversation.MODE_MULTI) {
				final Jid nextCounterpart = conversation.getNextCounterpart();
				if (nextCounterpart != null) {
					message.setCounterpart(nextCounterpart);
					message.setTrueCounterpart(conversation.getMucOptions().getTrueCounterpart(nextCounterpart));
					message.setType(Message.TYPE_PRIVATE);
				}
			}
		} else {
			message = conversation.getCorrectingMessage();
			message.setBody(body);
			message.putEdited(message.getUuid(), message.getServerMsgId());
			message.setServerMsgId(null);
			message.setUuid(UUID.randomUUID().toString());
		}
		switch (conversation.getNextEncryption()) {
			case Message.ENCRYPTION_PGP:
				//sendPgpMessage(message);
				break;
			default:
				sendMessage(message);
		}
	}

	protected boolean trustKeysIfNeeded(int requestCode) {
		AxolotlService axolotlService = conversation.getAccount().getAxolotlService();
		final List<Jid> targets = axolotlService.getCryptoTargets(conversation);
		boolean hasUnaccepted = !conversation.getAcceptedCryptoTargets().containsAll(targets);
		boolean hasUndecidedOwn = !axolotlService.getKeysWithTrust(FingerprintStatus.createActiveUndecided()).isEmpty();
		boolean hasUndecidedContacts = !axolotlService.getKeysWithTrust(FingerprintStatus.createActiveUndecided(), targets).isEmpty();
		boolean hasPendingKeys = !axolotlService.findDevicesWithoutSession(conversation).isEmpty();
		boolean hasNoTrustedKeys = axolotlService.anyTargetHasNoTrustedKeys(targets);
		boolean downloadInProgress = axolotlService.hasPendingKeyFetches(targets);
		if (hasUndecidedOwn || hasUndecidedContacts || hasPendingKeys || hasNoTrustedKeys || hasUnaccepted || downloadInProgress) {
			axolotlService.createSessionsIfNeeded(conversation);
			Intent intent = new Intent(getActivity(), TrustKeysActivity.class);
			String[] contacts = new String[targets.size()];
			for (int i = 0; i < contacts.length; ++i) {
				contacts[i] = targets.get(i).toString();
			}
			intent.putExtra("contacts", contacts);
			intent.putExtra(EXTRA_ACCOUNT, conversation.getAccount().getJid().asBareJid().toString());
			intent.putExtra("conversation", conversation.getUuid());

			if (requestCode == REQUEST_TRUST_KEYS_TEXT ||
					requestCode == REQUEST_TRUST_KEYS_ATTACHMENTS) {
				conversation.commitTrusts();
				if (conversation.getMode() == Conversation.MODE_MULTI) {
					activity.xmppConnectionService.updateConversation(conversation);
				}
				return false;
			} else {
				startActivityForResult(intent, requestCode);
			}

			return true;
		} else {
			return false;
		}
	}

	public void updateChatMsgHint() {
		final boolean multi = conversation.getMode() == Conversation.MODE_MULTI;
		if (conversation.getCorrectingMessage() != null) {
			this.binding.textinput.setHint(R.string.send_corrected_message);
		} else if (multi && conversation.getNextCounterpart() != null) {
			this.binding.textinput.setHint(getString(
					R.string.send_private_message_to,
					conversation.getNextCounterpart().getResource()));
		} else if (multi && !conversation.getMucOptions().participating()) {
			this.binding.textinput.setHint(R.string.you_are_not_participating);
		} else {
			this.binding.textinput.setHint(UIHelper.getMessageHint(getActivity(), conversation));
			getActivity().invalidateOptionsMenu();
		}
	}

	public void setupIme() {
		this.binding.textinput.refreshIme();
	}

	private void handleActivityResult(ActivityResult activityResult) {
		if (activityResult.resultCode == Activity.RESULT_OK) {
			handlePositiveActivityResult(activityResult.requestCode, activityResult.data);
		} else {
			handleNegativeActivityResult(activityResult.requestCode);
		}
	}

	private void handlePositiveActivityResult(int requestCode, final Intent data) {
		switch (requestCode) {
			case REQUEST_TRUST_KEYS_TEXT:
				sendMessage();
				break;
			case REQUEST_TRUST_KEYS_ATTACHMENTS:
				commitAttachments();
				break;
			case ATTACHMENT_CHOICE_CHOOSE_IMAGE:
				final List<Attachment> imageUris = Attachment.extractAttachments(getActivity(), data, Attachment.Type.IMAGE);
				mediaPreviewAdapter.addMediaPreviews(imageUris);
				toggleInputMethod();
				break;
			case ATTACHMENT_CHOICE_TAKE_PHOTO:
				final Uri takePhotoUri = pendingTakePhotoUri.pop();
				if (takePhotoUri != null) {
					mediaPreviewAdapter.addMediaPreviews(Attachment.of(getActivity(), takePhotoUri, Attachment.Type.IMAGE));
					toggleInputMethod();
				} else {
					Log.d(Config.LOGTAG, "lost take photo uri. unable to to attach");
				}
				break;
			case ATTACHMENT_CHOICE_CHOOSE_FILE:
			case ATTACHMENT_CHOICE_RECORD_VIDEO:
			case ATTACHMENT_CHOICE_RECORD_VOICE:
				final Attachment.Type type = requestCode == ATTACHMENT_CHOICE_RECORD_VOICE ? Attachment.Type.RECORDING : Attachment.Type.FILE;
				final List<Attachment> fileUris = Attachment.extractAttachments(getActivity(), data, type);
				mediaPreviewAdapter.addMediaPreviews(fileUris);
				toggleInputMethod();
				break;
			case ATTACHMENT_CHOICE_LOCATION:
				double latitude = data.getDoubleExtra("latitude", 0);
				double longitude = data.getDoubleExtra("longitude", 0);
				Uri geo = Uri.parse("geo:" + String.valueOf(latitude) + "," + String.valueOf(longitude));
				mediaPreviewAdapter.addMediaPreviews(Attachment.of(getActivity(), geo, Attachment.Type.LOCATION));
				toggleInputMethod();
				break;
			case REQUEST_INVITE_TO_CONVERSATION:
				XmppActivity.ConferenceInvite invite = XmppActivity.ConferenceInvite.parse(data);
				if (invite != null) {
					if (invite.execute(activity)) {
						activity.mToast = Toast.makeText(activity, R.string.creating_conference, Toast.LENGTH_LONG);
						activity.mToast.show();
					}

					if (conversation != null && conversation.getMode() == Conversation.MODE_MULTI && invite.getJids().size() > 0) {
						activity.xmppConnectionService.sendJoiningGroupMessage(conversation, invite.getJids(), false);
					}
				}
				break;
		}
	}

	private void commitAttachments() {
		if (!hasPermissions(REQUEST_COMMIT_ATTACHMENTS, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			return;
		}
		if (conversation.getNextEncryption() == Message.ENCRYPTION_AXOLOTL && trustKeysIfNeeded(REQUEST_TRUST_KEYS_ATTACHMENTS)) {
			return;
		}
		final List<Attachment> attachments = mediaPreviewAdapter.getAttachments();
		final PresenceSelector.OnPresenceSelected callback = () -> {
			for (Iterator<Attachment> i = attachments.iterator(); i.hasNext(); i.remove()) {
				final Attachment attachment = i.next();
				if (attachment.getType() == Attachment.Type.LOCATION) {
					attachLocationToConversation(conversation, attachment.getUri());
				} else if (attachment.getType() == Attachment.Type.IMAGE) {
					Log.d(Config.LOGTAG, "ConversationsActivity.commitAttachments() - attaching image to conversations. CHOOSE_IMAGE");
					attachImageToConversation(conversation, attachment.getUri());
				} else {
					Log.d(Config.LOGTAG, "ConversationsActivity.commitAttachments() - attaching file to conversations. CHOOSE_FILE/RECORD_VOICE/RECORD_VIDEO");
					attachFileToConversation(conversation, attachment.getUri(), attachment.getMime());
				}
			}
			mediaPreviewAdapter.notifyDataSetChanged();
			toggleInputMethod();
		};
		if (conversation == null || conversation.getMode() == Conversation.MODE_MULTI || FileBackend.allFilesUnderSize(getActivity(), attachments, getMaxHttpUploadSize(conversation))) {
			callback.onPresenceSelected();
		} else {
			activity.selectPresence(conversation, callback);
		}
	}

	public void toggleInputMethod() {
		boolean hasAttachments = mediaPreviewAdapter.hasAttachments();
		binding.textinput.setVisibility(hasAttachments ? View.GONE : View.VISIBLE);
		binding.mediaPreview.setVisibility(hasAttachments ? View.VISIBLE : View.GONE);
		updateSendButton();
	}


	private void handleNegativeActivityResult(int requestCode) {
		switch (requestCode) {
			case ATTACHMENT_CHOICE_TAKE_PHOTO:
				if (pendingTakePhotoUri.clear()) {
					Log.d(Config.LOGTAG, "cleared pending photo uri after negative activity result");
				}
				break;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		ActivityResult activityResult = ActivityResult.of(requestCode, resultCode, data);
		if (activity != null && activity.xmppConnectionService != null) {
			handleActivityResult(activityResult);
		} else {
			this.postponedActivityResult.push(activityResult);
		}
	}

	public void unblockConversation(final Blockable conversation) {
		activity.xmppConnectionService.sendUnblockRequest(conversation);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.d(Config.LOGTAG, "ConversationFragment.onAttach()");
		if (activity instanceof ConversationsActivity) {
			this.activity = (ConversationsActivity) activity;
		} else {
			throw new IllegalStateException("Trying to attach fragment to activity that is not the ConversationsActivity");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		this.activity = null; //TODO maybe not a good idea since some callbacks really need it
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onPrepareOptionsMenu (Menu menu){
		final MenuItem changeStatus = menu.findItem(R.id.action_change_status);
		final MenuItem editStatus = menu.findItem(R.id.action_edit_status);
		if(conversation != null && conversation.getAccount()!= null) {
			if (conversation.getAccount().getPresenceStatus().equals(Presence.Status.ONLINE) && conversation.getAccount().getPresenceStatusMessage() == null) {
				changeStatus.setVisible(true);
				editStatus.setVisible(false);
			} else if (conversation.getAccount().getPresenceStatus().equals(Presence.Status.ONLINE) && conversation.getAccount().getPresenceStatusMessage() != null && conversation.getAccount().getPresenceStatusMessage().isEmpty()) {
				changeStatus.setVisible(true);
				editStatus.setVisible(false);
			} else {
				changeStatus.setVisible(false);
				editStatus.setVisible(true);
			}
		} else {
			changeStatus.setVisible(false);
			editStatus.setVisible(false);
		}
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		menuInflater.inflate(R.menu.fragment_conversation, menu);
		final MenuItem menuInviteContact = menu.findItem(R.id.action_invite);
		final MenuItem menuMute = menu.findItem(R.id.action_mute);
		final MenuItem menuUnmute = menu.findItem(R.id.action_unmute);

		final MenuItem menuLeaveGroup = menu.findItem(R.id.action_leave_group);
		final MenuItem menuEndConversation = menu.findItem(R.id.action_end_conversation);

		final MenuItem changeStatus = menu.findItem(R.id.action_change_status);
		final MenuItem editStatus = menu.findItem(R.id.action_edit_status);

		final MenuItem menuPhoneCall = menu.findItem(R.id.action_call);
		menuPhoneCall.setOnMenuItemClickListener(menuItem -> {
			if (conversation == null){
				return false;
			}
			if (conversation.getMode() == Conversation.MODE_MULTI && conversation.getMucOptions().getUserCount() >= 5) {
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setTitle(R.string.call_limit_alert_title);
				builder.setMessage(R.string.call_limit_alert_msg);
				builder.setPositiveButton(R.string.ok, null);
				builder.create().show();
				return false;
			}

			if(checkPermissionForCameraAndMicrophone()){
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setTitle(R.string.start_call);
				builder.setNegativeButton(R.string.cancel, null);
				builder.setPositiveButton(getString(R.string.call),
						(dialog, which) -> {
							makeCall();
						});
				builder.create().show();
			} else {
				requestPermissionForCameraAndMicrophone();
			}
			return true;

		});

		if (conversation != null) {
			menuPhoneCall.setVisible(true);
			if (conversation.getMode() == Conversation.MODE_MULTI) {
				menuInviteContact.setVisible(conversation.getMucOptions().canInvite());
				menuLeaveGroup.setVisible(true);
				menuEndConversation.setVisible(false);
			} else {
				final XmppConnectionService service = activity.xmppConnectionService;
				menuInviteContact.setVisible(false);
				menuLeaveGroup.setVisible(false);
				menuEndConversation.setVisible(true);
			}
			if (conversation.isMuted()) {
				menuMute.setVisible(false);
			} else {
				menuUnmute.setVisible(false);
			}
			if (conversation.getAccount().getPresenceStatus().equals(Presence.Status.ONLINE) && conversation.getAccount().getPresenceStatusMessage() == null){
				changeStatus.setVisible(true);
				editStatus.setVisible(false);
			} else if (conversation.getAccount().getPresenceStatus().equals(Presence.Status.ONLINE) && conversation.getAccount().getPresenceStatusMessage() != null && conversation.getAccount().getPresenceStatusMessage().isEmpty()){
				changeStatus.setVisible(true);
				editStatus.setVisible(false);
			} else {
				changeStatus.setVisible(false);
				editStatus.setVisible(true);
			}
			ConversationMenuConfigurator.configureAttachmentMenu(conversation, menu);
			ConversationMenuConfigurator.configureEncryptionMenu(conversation, menu);
		}

		super.onCreateOptionsMenu(menu, menuInflater);
	}





	private boolean checkPermissionForCameraAndMicrophone() {
		int resultCamera = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);
		int resultMic = ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO);
		return resultCamera == PackageManager.PERMISSION_GRANTED &&
				resultMic == PackageManager.PERMISSION_GRANTED;
	}

	private void requestPermissionForCameraAndMicrophone() {
		ActivityCompat.requestPermissions(
				activity,
				new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
				CAMERA_MIC_PERMISSION_REQUEST_CODE);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.binding = DataBindingUtil.inflate(inflater, R.layout.fragment_conversation, container, false);

		if (conversation != null) {
			if (conversation.getMode() != Conversation.MODE_MULTI) {
				binding.actionDisapearMessagesSpacer.setVisibility(View.GONE);
				binding.actionDisapearMessages.setVisibility(View.VISIBLE);
			} else
			{
				binding.actionDisapearMessages.setVisibility(View.GONE);
				binding.actionDisapearMessagesSpacer.setVisibility(View.VISIBLE);
			}
		}

//		if (conversation != null) {
//			if (conversation.getMode() != Conversation.MODE_MULTI) {
//				binding.attachRecordVoice.setVisibility(View.GONE);
//				binding.actionDisapearMessages.setVisibility(View.VISIBLE);
//			} else
//			{
//				binding.actionDisapearMessages.setVisibility(View.GONE);
//				binding.attachRecordVoice.setVisibility(View.VISIBLE);
//			}
//		}
//
		binding.getRoot().setOnClickListener(null); //TODO why the heck did we do this?

		binding.textinput.addTextChangedListener(new StylingHelper.MessageEditorStyler(binding.textinput));

		binding.textinput.setOnEditorActionListener(mEditorActionListener);
		binding.textinput.setRichContentListener(new String[]{"image/*"}, mEditorContentListener);

		binding.textSendButton.setOnClickListener(this.mSendButtonListener);

		binding.attachChoosePicture.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				attachFile(ATTACHMENT_CHOICE_CHOOSE_IMAGE);
			}
		});
		binding.attachLocation.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				attachFile(ATTACHMENT_CHOICE_LOCATION);
			}
		});
		binding.attachLocation1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				attachFile(ATTACHMENT_CHOICE_LOCATION);
			}
		});
		binding.attachTakePicture.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				attachFile(ATTACHMENT_CHOICE_TAKE_PHOTO);
			}
		});
		binding.attachChooseFile.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				attachFile(ATTACHMENT_CHOICE_CHOOSE_FILE);
			}
		});
		binding.actionDisapearMessages.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (conversation != null) {
					conversationMessageTimerDialog(conversation);
				}
			}
		});
		binding.attachRecordVideo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (conversation != null) {
					attachFile(ATTACHMENT_CHOICE_RECORD_VIDEO);
				}
			}
		});
		binding.attachRecordVoice.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (conversation != null) {
					attachFile(ATTACHMENT_CHOICE_RECORD_VOICE);
				}
			}
		});

		binding.actionShowSecondary.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				binding.attachChooseFile.setVisibility(View.GONE);
				binding.attachTakePicture.setVisibility(View.GONE);
				binding.attachChoosePicture.setVisibility(View.GONE);
				binding.attachRecordVideo.setVisibility(View.GONE);
				binding.attachRecordVoice.setVisibility(View.GONE);
				binding.attachLocation.setVisibility(View.GONE);
				binding.attachChooseFileSpacer.setVisibility(View.GONE);
				binding.actionDisapearMessages.setVisibility(View.GONE);
				binding.attachLocation1.setVisibility(View.GONE);
				binding.actionDisapearMessagesSpacer.setVisibility(View.GONE);
				binding.actionShowSecondary.setVisibility(View.GONE);
				binding.actionShowPrimary.setVisibility(View.GONE);

				binding.actionDisapearMessagesSpacer.setVisibility(View.VISIBLE);
				binding.attachRecordVideo.setVisibility(View.VISIBLE);
				binding.attachLocation.setVisibility(View.VISIBLE);
				binding.attachRecordVoice.setVisibility(View.VISIBLE);
				binding.actionShowPrimary.setVisibility(View.VISIBLE);
			}
		});

		binding.actionShowPrimary.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				binding.attachChooseFile.setVisibility(View.GONE);
				binding.attachTakePicture.setVisibility(View.GONE);
				binding.attachChoosePicture.setVisibility(View.GONE);
				binding.attachRecordVideo.setVisibility(View.GONE);
				binding.attachRecordVoice.setVisibility(View.GONE);
				binding.attachLocation.setVisibility(View.GONE);
				binding.attachChooseFileSpacer.setVisibility(View.GONE);
				binding.actionDisapearMessages.setVisibility(View.GONE);
				binding.attachLocation1.setVisibility(View.GONE);
				binding.actionDisapearMessagesSpacer.setVisibility(View.GONE);
				binding.actionShowSecondary.setVisibility(View.GONE);
				binding.actionShowPrimary.setVisibility(View.GONE);

				if (conversation != null) {
					if (conversation.getMode() != Conversation.MODE_MULTI)
					{ binding.actionDisapearMessages.setVisibility(View.VISIBLE); }
					else
					{ binding.actionDisapearMessagesSpacer.setVisibility(View.VISIBLE); }
				}
				binding.attachTakePicture.setVisibility(View.VISIBLE);
				binding.attachChoosePicture.setVisibility(View.VISIBLE);
				binding.attachChooseFile.setVisibility(View.VISIBLE);
				binding.actionShowSecondary.setVisibility(View.VISIBLE);
			}
		});

		binding.scrollToBottomButton.setOnClickListener(this.mScrollButtonListener);
		binding.messagesView.setOnScrollListener(mOnScrollListener);
		binding.messagesView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
		mediaPreviewAdapter = new MediaPreviewAdapter(this);
		binding.mediaPreview.setAdapter(mediaPreviewAdapter);
		messageListAdapter = new MessageAdapter((XmppActivity) getActivity(), this.messageList);
		messageListAdapter.setOnContactPictureClicked(this);
		messageListAdapter.setOnContactPictureLongClicked(this);
		messageListAdapter.setOnQuoteListener(this::quoteText);
		binding.messagesView.setAdapter(messageListAdapter);

		registerForContextMenu(binding.messagesView);

		this.binding.textinput.setCustomInsertionActionModeCallback(new EditMessageActionModeCallback(this.binding.textinput));

		return binding.getRoot();
	}

	private void quoteText(String text) {
		if (binding.textinput.isEnabled()) {
			binding.textinput.insertAsQuote(text);
			binding.textinput.requestFocus();
			InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			if (inputMethodManager != null) {
				inputMethodManager.showSoftInput(binding.textinput, InputMethodManager.SHOW_IMPLICIT);
			}
		}
	}

	private void quoteMessage(Message message) {
		quoteText(MessageUtils.prepareQuote(message));
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		synchronized (this.messageList) {
			super.onCreateContextMenu(menu, v, menuInfo);
			AdapterView.AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) menuInfo;
			this.selectedMessage = this.messageList.get(acmi.position);
			populateContextMenu(menu);
		}
	}


	private void populateContextMenu(ContextMenu menu) {
		final Message m = this.selectedMessage;
		final Transferable t = m.getTransferable();
		Message relevantForCorrection = m;
		while (relevantForCorrection.mergeable(relevantForCorrection.next())) {
			relevantForCorrection = relevantForCorrection.next();
		}
		if (m.getType() != Message.TYPE_STATUS) {

			if (m.getEncryption() == Message.ENCRYPTION_AXOLOTL_NOT_FOR_THIS_DEVICE || m.getEncryption() == Message.ENCRYPTION_AXOLOTL_FAILED) {
				return;
			}

			final boolean deleted = m.isDeleted();
			final boolean encrypted = m.getEncryption() == Message.ENCRYPTION_DECRYPTION_FAILED
					|| m.getEncryption() == Message.ENCRYPTION_PGP;
			final boolean receiving = m.getStatus() == Message.STATUS_RECEIVED && (t instanceof JingleConnection || t instanceof HttpDownloadConnection);
			activity.getMenuInflater().inflate(R.menu.message_context, menu);
			menu.setHeaderTitle(R.string.message_options);
			MenuItem openWith = menu.findItem(R.id.open_with);
			MenuItem copyMessage = menu.findItem(R.id.copy_message);
			MenuItem copyLink = menu.findItem(R.id.copy_link);
			MenuItem quoteMessage = menu.findItem(R.id.quote_message);
			//MenuItem retryDecryption = menu.findItem(R.id.retry_decryption);
			MenuItem correctMessage = menu.findItem(R.id.correct_message);
			MenuItem shareWith = menu.findItem(R.id.share_with);
			MenuItem sendAgain = menu.findItem(R.id.send_again);
			MenuItem copyUrl = menu.findItem(R.id.copy_url);
			MenuItem downloadFile = menu.findItem(R.id.download_file);
			MenuItem cancelTransmission = menu.findItem(R.id.cancel_transmission);
			MenuItem deleteFile = menu.findItem(R.id.delete_file);
			MenuItem showErrorMessage = menu.findItem(R.id.show_error_message);
			final boolean showError = m.getStatus() == Message.STATUS_SEND_FAILED && m.getErrorMessage() != null && !Message.ERROR_MESSAGE_CANCELLED.equals(m.getErrorMessage());
			if (!m.isFileOrImage() && !encrypted && !m.isGeoUri() && !m.treatAsDownloadable()) {
				copyMessage.setVisible(true);
				quoteMessage.setVisible(!showError && MessageUtils.prepareQuote(m).length() > 0);
				String body = m.getMergedBody().toString();
				if (ShareUtil.containsXmppUri(body)) {
					copyLink.setTitle(R.string.copy_jabber_id);
					copyLink.setVisible(true);
				} else if (Patterns.AUTOLINK_WEB_URL.matcher(body).find()) {
					copyLink.setVisible(true);
				}
			}
			/*if (m.getEncryption() == Message.ENCRYPTION_DECRYPTION_FAILED && !deleted) {
				retryDecryption.setVisible(true);
			}*/
			if (!showError
					&& relevantForCorrection.getType() == Message.TYPE_TEXT
					&& !m.isGeoUri()
					&& relevantForCorrection.isLastCorrectableMessage()
					&& m.getConversation() instanceof Conversation) {
				correctMessage.setVisible(true);
			}
			if ((m.isFileOrImage() && !deleted && !receiving) || (m.getType() == Message.TYPE_TEXT && !m.treatAsDownloadable())) {
				shareWith.setVisible(true);
			}
			if (m.getStatus() == Message.STATUS_SEND_FAILED) {
				sendAgain.setVisible(true);
			}
			if (m.hasFileOnRemoteHost()
					|| m.isGeoUri()
					|| m.treatAsDownloadable()
					|| (t != null && t instanceof HttpDownloadConnection)) {

			}
			if (m.isFileOrImage() && deleted && m.hasFileOnRemoteHost()) {
				downloadFile.setVisible(true);
				downloadFile.setTitle(activity.getString(R.string.download_x_file, UIHelper.getFileDescriptionString(activity, m)));
			}
			final boolean waitingOfferedSending = m.getStatus() == Message.STATUS_WAITING
					|| m.getStatus() == Message.STATUS_UNSEND
					|| m.getStatus() == Message.STATUS_OFFERED;
			final boolean cancelable = (t != null && !deleted) || waitingOfferedSending && m.needsUploading();
			if (cancelable) {
				cancelTransmission.setVisible(true);
			}
			if (m.isFileOrImage() && !deleted && !cancelable) {
				String path = m.getRelativeFilePath();
				if (path == null || !path.startsWith("/") || FileBackend.isInDirectoryThatShouldNotBeScanned(getActivity(), path) ) {
					deleteFile.setVisible(true);
					deleteFile.setTitle(activity.getString(R.string.delete_x_file, UIHelper.getFileDescriptionString(activity, m)));
				}
			}
			if (showError) {
				showErrorMessage.setVisible(true);
			}
			final String mime = m.isFileOrImage() ? m.getMimeType() : null;
			if ((m.isGeoUri() && GeoHelper.openInOsmAnd(getActivity(),m)) || (mime != null && mime.startsWith("audio/"))) {
				openWith.setVisible(true);
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.share_with:
				ShareUtil.share(activity, selectedMessage);
				return true;
			case R.id.correct_message:
				correctMessage(selectedMessage);
				return true;
			case R.id.copy_message:
				ShareUtil.copyToClipboard(activity, selectedMessage);
				return true;
			case R.id.copy_link:
				ShareUtil.copyLinkToClipboard(activity, selectedMessage);
				return true;
			case R.id.quote_message:
				quoteMessage(selectedMessage);
				return true;
			case R.id.send_again:
				resendMessage(selectedMessage);
				return true;
			case R.id.download_file:
				startDownloadable(selectedMessage);
				return true;
			case R.id.cancel_transmission:
				cancelTransmission(selectedMessage);
				return true;
			case R.id.retry_decryption:
				return true;
			case R.id.delete_file:
				deleteFile(selectedMessage);
				return true;
			case R.id.show_error_message:
				showErrorMessage(selectedMessage);
				return true;
			case R.id.open_with:
				openWith(selectedMessage);
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (MenuDoubleTabUtil.shouldIgnoreTap()) {
			return false;
		} else if (conversation == null) {
			return super.onOptionsItemSelected(item);
		}
		switch (item.getItemId()) {
			case R.id.action_leave_group:
				this.endConversationDialog(conversation);
				break;
			case R.id.action_change_status:
				activity.changePresence(conversation.getAccount());
				break;
			case R.id.action_edit_status:
				activity.changePresence(conversation.getAccount());
				break;
			case R.id.action_contact_details:
				activity.switchToContactDetails(conversation.getContact());
				break;
			case R.id.action_invite:
				if (conversation.getMode() == Conversation.MODE_SINGLE) {
					newGroupNameDialog(conversation);
				} else {
					startActivityForResult(ChooseContactActivity.create(activity, conversation), REQUEST_INVITE_TO_CONVERSATION);
				}
				break;
			case R.id.action_end_conversation:
				endConversationDialog(conversation);
				break;
			case R.id.action_mute:
				muteConversationDialog(conversation);
				break;
			case R.id.action_unmute:
				unmuteConversation(conversation);
				break;
			case R.id.action_block:
			case R.id.action_unblock:
				final Activity activity = getActivity();
				if (activity instanceof XmppActivity) {
					BlockContactDialog.show((XmppActivity) activity, conversation);
				}
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	public void attachFile(final int attachmentChoice) {
		if (attachmentChoice == ATTACHMENT_CHOICE_RECORD_VOICE) {
			if (!hasPermissions(attachmentChoice, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO)) {
				return;
			}
		} else if (attachmentChoice == ATTACHMENT_CHOICE_TAKE_PHOTO || attachmentChoice == ATTACHMENT_CHOICE_RECORD_VIDEO) {
			if (!hasPermissions(attachmentChoice, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)) {
				return;
			}
		} else if (attachmentChoice != ATTACHMENT_CHOICE_LOCATION) {
			if (!hasPermissions(attachmentChoice, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
				return;
			}
		}

		final int encryption = conversation.getNextEncryption();
		final int mode = conversation.getMode();
		if (encryption == Message.ENCRYPTION_PGP) {
			//
		} else {
			selectPresenceToAttachFile(attachmentChoice);
		}
	}

	private void makeCall(){
		final Account account = conversation.getAccount();
		TwilioCall call = new TwilioCall(account);
		// this should be an array of receivers. group will otherwise be groupjid
		if (conversation.getMode() == Conversation.MODE_MULTI) {
			StringBuilder receivers = new StringBuilder();
			List<Jid> groupies = conversation.getAcceptedCryptoTargets();
			for (Jid receiver : groupies) {
				receivers.append(receiver.asBareJid().toString()).append(",");
			}
			if(receivers.length() > 0 ) {
				receivers.deleteCharAt(receivers.length() - 1);
			}
			//call.setReceiver(String.join(",", receivers)); //better but requires API 26
			call.setReceiver(receivers.toString());
			String title = conversation.getMucOptions().getName();
			call.setRoomTitle("#"+title);
		} else {
			call.setReceiver(conversation.getJid().asBareJid().toString());
			call.setRoomTitle(String.valueOf(conversation.getName()));
		}

		//call.setReceiver(conversation.getJid().asBareJid().toString());
		activity.xmppConnectionService.sendCallRequest(call);
		Intent callActivity = new Intent(getContext(), CallActivity.class);
		callActivity.setAction(CallActivity.ACTION_OUTGOING_CALL);
		callActivity.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		callActivity.putExtra("receiver", call.getReceiver());
		callActivity.putExtra("uuid", conversation.getUuid());
		callActivity.putExtra("calltitle", call.getRoomTitle());
		startActivity(callActivity);

		Message msg = Message.createCallStatusMessage(conversation, Message.STATUS_CALL_SENT);
		conversation.add(msg);
		activity.xmppConnectionService.databaseBackend.createMessage(msg);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
		if (grantResults.length > 0) {
			if (allGranted(grantResults)) {
				switch (requestCode) {
					case REQUEST_START_DOWNLOAD:
						if (this.mPendingDownloadableMessage != null) {
							startDownloadable(this.mPendingDownloadableMessage);
						}
						break;
					case REQUEST_ADD_EDITOR_CONTENT:
						if (this.mPendingEditorContent != null) {
							attachEditorContentToConversation(this.mPendingEditorContent);
						}
						break;
					case REQUEST_COMMIT_ATTACHMENTS:
						commitAttachments();
						break;
					case CAMERA_MIC_PERMISSION_REQUEST_CODE:
						makeCall();
						break;
					default:
						attachFile(requestCode);
						break;
				}
			} else {
				@StringRes int res;
				String firstDenied = getFirstDenied(grantResults, permissions);
				if (Manifest.permission.RECORD_AUDIO.equals(firstDenied)) {
					res = R.string.no_microphone_permission;
				} else if (Manifest.permission.CAMERA.equals(firstDenied)) {
					res = R.string.no_camera_permission;
				} else {
					res = R.string.no_storage_permission;
				}
				Toast.makeText(getActivity(), res, Toast.LENGTH_SHORT).show();
			}
		}
		if (writeGranted(grantResults, permissions)) {
			if (activity != null && activity.xmppConnectionService != null) {
				activity.xmppConnectionService.restartFileObserver();
			}
			refresh();
		}
	}

	public void startDownloadable(Message message) {
		if (!hasPermissions(REQUEST_START_DOWNLOAD, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			this.mPendingDownloadableMessage = message;
			return;
		}
		Transferable transferable = message.getTransferable();
		if (transferable != null) {
			if (transferable instanceof TransferablePlaceholder && message.hasFileOnRemoteHost()) {
				createNewConnection(message);
				return;
			}
			if (!transferable.start()) {
				Log.d(Config.LOGTAG, "type: " + transferable.getClass().getName());
				Toast.makeText(getActivity(), R.string.not_connected_try_again, Toast.LENGTH_SHORT).show();
			}
		} else if (message.treatAsDownloadable() || message.hasFileOnRemoteHost()) {
			createNewConnection(message);
		}
	}

	private void createNewConnection(final Message message) {
		if (!activity.xmppConnectionService.getHttpConnectionManager().checkConnection(message)) {
			Toast.makeText(getActivity(), R.string.not_connected_try_again, Toast.LENGTH_SHORT).show();
			return;
		}
		activity.xmppConnectionService.getHttpConnectionManager().createNewDownloadConnection(message, true);
	}


	/**
	 * when in a 1:1 conversation and invite a contact to form a group
	 *
	 * @param conversation
	 */
	@SuppressLint("InflateParams")
	protected void newGroupNameDialog(final Conversation conversation) {
		android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity());
		builder.setTitle(getString(R.string.dialog_title_create_conference));
		View dialogView = getActivity().getLayoutInflater().inflate(
				R.layout.invite_new_conference_dialog, null);
		builder.setView(dialogView);
		final EditText nameBox = (EditText) dialogView
				.findViewById(R.id.group_chat_name);
		builder.setNegativeButton(getString(R.string.cancel), null);
		DelayedHintHelper.setHint(R.string.conference_address_example, nameBox);

		builder.setPositiveButton(getString(R.string.invite_contact), (dialog, which) -> {
			String groupname =nameBox.getText().toString().trim();
			groupname = groupname.replaceAll(" ","");
			Intent intent = ChooseContactActivity.create(activity, conversation);
			intent.putExtra(ChooseContactActivity.EXTRA_GROUP_CHAT_NAME, groupname);
			startActivityForResult(intent, REQUEST_INVITE_TO_CONVERSATION);
		});

		android.app.AlertDialog dialog = builder.create();
		dialog.show();
		Button chooseButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
		chooseButton.setEnabled(false);
		nameBox.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable arg0) { }

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() > 0) {
					chooseButton.setEnabled(true);
				} else {
					chooseButton.setEnabled(false);
				}
			}
		});
	}

	/**
	 * Add end conversation capability
	 *
	 * @param conversation
	 */
	@SuppressLint("InflateParams")
	protected void endConversationDialog(final Conversation conversation) {
		android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity());
		builder.setTitle(getString(R.string.action_end_conversation));
		View dialogView = getActivity().getLayoutInflater().inflate(
				R.layout.dialog_end_conversation, null);
		final CheckBox clearConversationCheckBox = (CheckBox) dialogView
				.findViewById(R.id.clear_conversation_checkbox);
		clearConversationCheckBox.setChecked(true);
		builder.setView(dialogView);
		builder.setNegativeButton(getString(R.string.cancel), null);
		builder.setPositiveButton(getString(R.string.end_conversation), (dialog, which) -> {
			if (clearConversationCheckBox.isChecked()) {
				// clear messages
				this.activity.xmppConnectionService.clearConversationHistory(conversation);
				activity.onConversationsListItemUpdated();
				refresh();
			}

			if (conversation.getMode() == Conversation.MODE_MULTI) {
				sendLeavingGroupMessage(conversation);
			}

			activity.xmppConnectionService.archiveConversation(conversation);
		});
		builder.create().show();
	}

	/**
	 *
	 */
	public void sendLeavingGroupMessage(final Conversation conversation) {
		final Account account = conversation.getAccount();
		String dname = account.getDisplayName();
		if (dname == null) { dname = account.getUsername(); }
		String bod = dname + " " + getString(R.string.left_group);
		Message message = new Message(conversation, bod, Message.ENCRYPTION_NONE);
		activity.xmppConnectionService.sendMessage(message);
		// sleep required so message goes out before conversation thread stopped
		// maybe show a spinner?
		try { Thread.sleep(2000); } catch (InterruptedException ie) {}
	}

	@SuppressLint("InflateParams")
	protected void clearHistoryDialog(final Conversation conversation) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(getString(R.string.clear_conversation_history));
		final View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_clear_history, null);
		final CheckBox endConversationCheckBox = dialogView.findViewById(R.id.end_conversation_checkbox);
		builder.setView(dialogView);
		builder.setNegativeButton(getString(R.string.cancel), null);
		builder.setPositiveButton(getString(R.string.delete_messages), (dialog, which) -> {
			this.activity.xmppConnectionService.clearConversationHistory(conversation);
			if (endConversationCheckBox.isChecked()) {
				if (conversation.getMode() == Conversation.MODE_MULTI) {
					sendLeavingGroupMessage(conversation);
				}
				this.activity.xmppConnectionService.archiveConversation(conversation);
				this.activity.onConversationArchived(conversation);
			} else {
				activity.onConversationsListItemUpdated();
				refresh();
			}
		});
		builder.create().show();
	}

	protected void conversationMessageTimerDialog(final Conversation conversation) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.conversation_message_timer); //Global message timer
		final String[] ctimers = getResources().getStringArray(R.array.timer_options_durations);
		final String[] ctimersStrs = getResources().getStringArray(R.array.timer_options_descriptions);
		builder.setItems(R.array.timer_options_descriptions,
				new android.content.DialogInterface.OnClickListener() {

					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						String timerStr = ctimers[which];
						int timer;
						try {
							timer = Integer.parseInt(timerStr);
						} catch(NumberFormatException nfe) {
							timer = Message.TIMER_NONE;
						}

						conversation.setTimer(timer);
						setTimerStatus(ctimersStrs[which],false);
						activity.xmppConnectionService.updateConversation(conversation);
						activity.onConversationsListItemUpdated();
						refresh();
						getActivity().invalidateOptionsMenu();
					}
				});
		builder.create().show();
	}

	protected void muteConversationDialog(final Conversation conversation) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.disable_notifications);
		final int[] durations = getResources().getIntArray(R.array.mute_options_durations);
		final CharSequence[] labels = new CharSequence[durations.length];
		for (int i = 0; i < durations.length; ++i) {
			if (durations[i] == -1) {
				labels[i] = getString(R.string.until_further_notice);
			} else {
				labels[i] = TimeframeUtils.resolve(activity, 1000L * durations[i]);
			}
		}
		builder.setItems(labels, (dialog, which) -> {
			final long till;
			if (durations[which] == -1) {
				till = Long.MAX_VALUE;
			} else {
				till = System.currentTimeMillis() + (durations[which] * 1000);
			}
			conversation.setMutedTill(till);
			activity.xmppConnectionService.updateConversation(conversation);
			activity.onConversationsListItemUpdated();
			refresh();
			getActivity().invalidateOptionsMenu();
		});
		builder.create().show();
	}

	private boolean hasPermissions(int requestCode, String... permissions) {
		final List<String> missingPermissions = new ArrayList<>();
		for(String permission : permissions) {
			if (Config.ONLY_INTERNAL_STORAGE && permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
				continue;
			}
			if (activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
				missingPermissions.add(permission);
			}
		}
		if (missingPermissions.size() == 0) {
			return true;
		} else {
			requestPermissions(missingPermissions.toArray(new String[missingPermissions.size()]), requestCode);
			return false;
		}
	}

	public void unmuteConversation(final Conversation conversation) {
		conversation.setMutedTill(0);
		this.activity.xmppConnectionService.updateConversation(conversation);
		this.activity.onConversationsListItemUpdated();
		refresh();
		getActivity().invalidateOptionsMenu();
	}

	protected void selectPresenceToAttachFile(final int attachmentChoice) {
		final Account account = conversation.getAccount();
		final PresenceSelector.OnPresenceSelected callback = () -> {
			Intent intent = new Intent();
			boolean chooser = false;
			switch (attachmentChoice) {
				case ATTACHMENT_CHOICE_CHOOSE_IMAGE:
					intent.setAction(Intent.ACTION_GET_CONTENT);
					intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
					intent.setType("image/*");
					chooser = true;
					break;
				case ATTACHMENT_CHOICE_RECORD_VIDEO:
					intent.setAction(MediaStore.ACTION_VIDEO_CAPTURE);
					break;
				case ATTACHMENT_CHOICE_TAKE_PHOTO:
					final Uri uri = activity.xmppConnectionService.getFileBackend().getTakePhotoUri();
					pendingTakePhotoUri.push(uri);
					intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
					intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
					intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
					break;
				case ATTACHMENT_CHOICE_CHOOSE_FILE:
					chooser = true;
					intent.setType("*/*");
					intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
					intent.addCategory(Intent.CATEGORY_OPENABLE);
					intent.setAction(Intent.ACTION_GET_CONTENT);
					break;
				case ATTACHMENT_CHOICE_RECORD_VOICE:
					intent = new Intent(getActivity(), RecordingActivity.class);
					break;
				case ATTACHMENT_CHOICE_LOCATION:
					intent = GeoHelper.getFetchIntent(activity);
					break;
			}
			if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
				if (chooser) {
					startActivityForResult(
							Intent.createChooser(intent, getString(R.string.perform_action_with)),
							attachmentChoice);
				} else {
					startActivityForResult(intent, attachmentChoice);
				}
			}
		};
		if (account.httpUploadAvailable() || attachmentChoice == ATTACHMENT_CHOICE_LOCATION) {
			conversation.setNextCounterpart(null);
			callback.onPresenceSelected();
		} else {
			activity.selectPresence(conversation, callback);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		binding.messagesView.post(this::fireReadEvent);

	}

	private void fireReadEvent() {
		if (activity != null && this.conversation != null) {
			String uuid = getLastVisibleMessageUuid();
			if (uuid != null) {
				activity.onConversationRead(this.conversation, uuid);
			}
		}
	}

	private String getLastVisibleMessageUuid() {
		if (binding == null) {
			return null;
		}
		synchronized (this.messageList) {
			int pos = binding.messagesView.getLastVisiblePosition();
			if (pos >= 0) {
				Message message = null;
				for (int i = pos; i >= 0; --i) {
					try {
						message = (Message) binding.messagesView.getItemAtPosition(i);
					} catch (IndexOutOfBoundsException e) {
						//should not happen if we synchronize properly. however if that fails we just gonna try item -1
						continue;
					}
					if (message.getType() != Message.TYPE_STATUS) {
						break;
					}
				}
				if (message != null) {
					while (message.next() != null && message.next().wasMergedIntoPrevious()) {
						message = message.next();
					}

					if (lastGroupRemoved != null) {
						return lastGroupRemoved;
					}
					return message.getUuid();
				}
			}
		}
		return null;
	}

	private void openWith(final Message message) {
		if (message.isGeoUri()) {
			GeoHelper.view(getActivity(),message);
		} else {
			final DownloadableFile file = activity.xmppConnectionService.getFileBackend().getFile(message);
			ViewUtil.view(activity, file);
		}
	}

	private void showErrorMessage(final Message message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.error_message);
		final String errorMessage = message.getErrorMessage();
		final String[] errorMessageParts = errorMessage == null ? new String[0] : errorMessage.split("\\u001f");
		final String displayError;
		if (errorMessageParts.length == 2) {
			displayError = errorMessageParts[1];
		} else {
			displayError = errorMessage;
		}
		builder.setMessage(displayError);
		builder.setNegativeButton(R.string.copy_to_clipboard, (dialog, which) -> {
			activity.copyTextToClipboard(displayError,R.string.error_message);
			Toast.makeText(activity,R.string.error_message_copied_to_clipboard, Toast.LENGTH_SHORT).show();
		});
		builder.setPositiveButton(R.string.confirm, null);
		builder.create().show();
	}


	private void deleteFile(final Message message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setNegativeButton(R.string.cancel, null);
		builder.setTitle(R.string.delete_file_dialog);
		builder.setMessage(R.string.delete_file_dialog_msg);
		builder.setPositiveButton(R.string.confirm, (dialog, which) -> {
			if (activity.xmppConnectionService.getFileBackend().deleteFile(message)) {
				message.setDeleted(true);
				activity.xmppConnectionService.updateMessage(message, false);
				activity.onConversationsListItemUpdated();
				refresh();
			}
		});
		builder.create().show();

	}

	private void resendMessage(final Message message) {
		if (message.isFileOrImage()) {
			if (!(message.getConversation() instanceof Conversation)) {
				return;
			}
			final Conversation conversation = (Conversation) message.getConversation();
			final DownloadableFile file = activity.xmppConnectionService.getFileBackend().getFile(message);
			if ((file.exists() && file.canRead()) || message.hasFileOnRemoteHost()) {
				final XmppConnection xmppConnection = conversation.getAccount().getXmppConnection();
				if (!message.hasFileOnRemoteHost()
						&& xmppConnection != null
						&& conversation.getMode() == Conversational.MODE_SINGLE
						&& !xmppConnection.getFeatures().httpUpload(message.getFileParams().size)) {
					activity.selectPresence(conversation, () -> {
						message.setCounterpart(conversation.getNextCounterpart());
						activity.xmppConnectionService.resendFailedMessages(message);
						new Handler().post(() -> {
							int size = messageList.size();
							this.binding.messagesView.setSelection(size - 1);
						});
					});
					return;
				}
			} else if (!Compatibility.hasStoragePermission(getActivity())) {
				Toast.makeText(activity, R.string.no_storage_permission, Toast.LENGTH_SHORT).show();
				return;
			} else {
				Toast.makeText(activity, R.string.file_deleted, Toast.LENGTH_SHORT).show();
				message.setDeleted(true);
				activity.xmppConnectionService.updateMessage(message, false);
				activity.onConversationsListItemUpdated();
				refresh();
				return;
			}
		}
		activity.xmppConnectionService.resendFailedMessages(message);
		new Handler().post(() -> {
			int size = messageList.size();
			this.binding.messagesView.setSelection(size - 1);
		});
	}

	private void cancelTransmission(Message message) {
		Transferable transferable = message.getTransferable();
		if (transferable != null) {
			transferable.cancel();
		} else if (message.getStatus() != Message.STATUS_RECEIVED) {
			activity.xmppConnectionService.markMessage(message, Message.STATUS_SEND_FAILED, Message.ERROR_MESSAGE_CANCELLED);
		}
	}

	/*private void retryDecryption(Message message) {
		message.setEncryption(Message.ENCRYPTION_PGP);
		activity.onConversationsListItemUpdated();
		refresh();
		conversation.getAccount().getPgpDecryptionService().decrypt(message, false);
	}*/

	public void privateMessageWith(final Jid counterpart) {
		if (conversation.setOutgoingChatState(Config.DEFAULT_CHATSTATE)) {
			activity.xmppConnectionService.sendChatState(conversation);
		}
		this.binding.textinput.setText("");
		this.conversation.setNextCounterpart(counterpart);
		updateChatMsgHint();
		updateSendButton();
		updateEditablity();
	}

	private void correctMessage(Message message) {
		while (message.mergeable(message.next())) {
			message = message.next();
		}
		this.conversation.setCorrectingMessage(message);
		final Editable editable = binding.textinput.getText();
		this.conversation.setDraftMessage(editable.toString());
		this.binding.textinput.setText("");
		this.binding.textinput.append(message.getBody());

	}

	private void highlightInConference(String nick) {
		final Editable editable = this.binding.textinput.getText();
		String oldString = editable.toString().trim();
		final int pos = this.binding.textinput.getSelectionStart();
		if (oldString.isEmpty() || pos == 0) {
			editable.insert(0, nick + ": ");
		} else {
			final char before = editable.charAt(pos - 1);
			final char after = editable.length() > pos ? editable.charAt(pos) : '\0';
			if (before == '\n') {
				editable.insert(pos, nick + ": ");
			} else {
				if (pos > 2 && editable.subSequence(pos - 2, pos).toString().equals(": ")) {
					if (NickValidityChecker.check(conversation, Arrays.asList(editable.subSequence(0, pos - 2).toString().split(", ")))) {
						editable.insert(pos - 2, ", " + nick);
						return;
					}
				}
				editable.insert(pos, (Character.isWhitespace(before) ? "" : " ") + nick + (Character.isWhitespace(after) ? "" : " "));
				if (Character.isWhitespace(after)) {
					this.binding.textinput.setSelection(this.binding.textinput.getSelectionStart() + 1);
				}
			}
		}
	}

	@Override
	public void startActivityForResult(Intent intent, int requestCode) {
		final Activity activity = getActivity();
		if (activity instanceof ConversationsActivity) {
			((ConversationsActivity) activity).clearPendingViewIntent();
		}
		super.startActivityForResult(intent, requestCode);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (conversation != null) {
			outState.putString(STATE_CONVERSATION_UUID, conversation.getUuid());
			outState.putString(STATE_LAST_MESSAGE_UUID, lastMessageUuid);
			final Uri uri = pendingTakePhotoUri.peek();
			if (uri != null) {
				outState.putString(STATE_PHOTO_URI, uri.toString());
			}
			final ScrollState scrollState = getScrollPosition();
			if (scrollState != null) {
				outState.putParcelable(STATE_SCROLL_POSITION, scrollState);
			}
			final ArrayList<Attachment> attachments = mediaPreviewAdapter == null ? new ArrayList<>() : mediaPreviewAdapter.getAttachments();
			if (attachments.size() > 0) {
				outState.putParcelableArrayList(STATE_MEDIA_PREVIEWS, attachments);
			}
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (savedInstanceState == null) {
			return;
		}
		String uuid = savedInstanceState.getString(STATE_CONVERSATION_UUID);
		ArrayList<Attachment> attachments = savedInstanceState.getParcelableArrayList(STATE_MEDIA_PREVIEWS);
		pendingLastMessageUuid.push(savedInstanceState.getString(STATE_LAST_MESSAGE_UUID, null));
		if (uuid != null) {
			QuickLoader.set(uuid);
			this.pendingConversationsUuid.push(uuid);
			if (attachments != null && attachments.size() > 0) {
				this.pendingMediaPreviews.push(attachments);
			}
			String takePhotoUri = savedInstanceState.getString(STATE_PHOTO_URI);
			if (takePhotoUri != null) {
				pendingTakePhotoUri.push(Uri.parse(takePhotoUri));
			}
			pendingScrollState.push(savedInstanceState.getParcelable(STATE_SCROLL_POSITION));
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		if (this.reInitRequiredOnStart && this.conversation != null) {
			final Bundle extras = pendingExtras.pop();
			reInit(this.conversation, extras != null);
			if (extras != null) {
				processExtras(extras);
			}
		} else if (conversation == null && activity != null && activity.xmppConnectionService != null) {
			final String uuid = pendingConversationsUuid.pop();
			Log.d(Config.LOGTAG, "ConversationFragment.onStart() - activity was bound but no conversation loaded. uuid=" + uuid);
			if (uuid != null) {
				findAndReInitByUuidOrArchive(uuid);
			}
		}

		if (this.conversation == null || this.conversation.getMode() == Conversation.MODE_SINGLE) {
			this.startDisappearingMessagesHandler();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		final Activity activity = getActivity();
		messageListAdapter.unregisterListenerInAudioPlayer();
		if (activity == null || !activity.isChangingConfigurations()) {
			hideSoftKeyboard(activity);
			messageListAdapter.stopAudioPlayer();
		}
		if (this.conversation != null) {
			final String msg = this.binding.textinput.getText().toString();
			storeNextMessage(msg);
			updateChatState(this.conversation, msg);
			this.activity.xmppConnectionService.getNotificationService().setOpenConversation(null);
		}
		this.endDisappearingMessagesHandler();

		this.reInitRequiredOnStart = true;
	}

	private void updateChatState(final Conversation conversation, final String msg) {
		ChatState state = msg.length() == 0 ? Config.DEFAULT_CHATSTATE : ChatState.PAUSED;
		Account.State status = conversation.getAccount().getStatus();
		if (status == Account.State.ONLINE && conversation.setOutgoingChatState(state)) {
			activity.xmppConnectionService.sendChatState(conversation);
		}
	}

	private void saveMessageDraftStopAudioPlayer() {
		final Conversation previousConversation = this.conversation;
		if (this.activity == null || this.binding == null || previousConversation == null) {
			return;
		}
		Log.d(Config.LOGTAG, "ConversationFragment.saveMessageDraftStopAudioPlayer()");
		final String msg = this.binding.textinput.getText().toString();
		storeNextMessage(msg);
		updateChatState(this.conversation, msg);
		messageListAdapter.stopAudioPlayer();
		mediaPreviewAdapter.clearPreviews();
		toggleInputMethod();
	}

	public void reInit(Conversation conversation, Bundle extras) {
		QuickLoader.set(conversation.getUuid());
		this.saveMessageDraftStopAudioPlayer();
		this.clearPending();
		if (this.reInit(conversation, extras != null)) {
			if (extras != null) {
				processExtras(extras);
			}
			this.reInitRequiredOnStart = false;
		} else {
			this.reInitRequiredOnStart = true;
			pendingExtras.push(extras);
		}
		resetUnreadMessagesCount();
	}

	private void reInit(Conversation conversation) {
		reInit(conversation, false);
	}

	private boolean reInit(final Conversation conversation, final boolean hasExtras) {
		if (conversation == null) {
			return false;
		}
		this.conversation = conversation;
		//once we set the conversation all is good and it will automatically do the right thing in onStart()
		if (this.activity == null || this.binding == null) {
			return false;
		}

		if (!activity.xmppConnectionService.isConversationStillOpen(this.conversation)) {
			activity.onConversationArchived(this.conversation);
			return false;
		}

		stopScrolling();
		Log.d(Config.LOGTAG, "reInit(hasExtras=" + Boolean.toString(hasExtras) + ")");

		if (this.conversation.isRead() && hasExtras) {
			Log.d(Config.LOGTAG, "trimming conversation");
			this.conversation.trim();
		}

		setupIme();

		final boolean scrolledToBottomAndNoPending = this.scrolledToBottom() && pendingScrollState.peek() == null;

		this.binding.textSendButton.setContentDescription(activity.getString(R.string.send_message_to_x, conversation.getName()));
		this.binding.textinput.setKeyboardListener(null);
		this.binding.textinput.setText("");
		final boolean participating = conversation.getMode() == Conversational.MODE_SINGLE || conversation.getMucOptions().participating();
		if (participating) {
			this.binding.textinput.append(this.conversation.getNextMessage());
		}
		this.binding.textinput.setKeyboardListener(this);
		messageListAdapter.updatePreferences();
		refresh(false);
		this.conversation.messagesLoaded.set(true);
		Log.d(Config.LOGTAG, "scrolledToBottomAndNoPending=" + Boolean.toString(scrolledToBottomAndNoPending));

		if (disappearingHandler == null) {
			disappearingHandler = new Handler();
			disRunnable = new Runnable() {
				@Override
				public void run() {
					ArrayList<String> removedIds = new ArrayList();
					boolean updateStatus = false;
					synchronized (ConversationFragment.this.messageList) {
						for (int m = ConversationFragment.this.messageList.size() - 1; m >= 0; m--) {
							Message message = ConversationFragment.this.messageList.get(m);
							if (message.getTimer() != Message.TIMER_NONE) {
								if (message.getTimeRemaining() <= 0 && message.getUuid() != null) {
									removedIds.add(message.getUuid());
								} else {
									updateStatus = true;
								}
							}
						}
					}
					if (removedIds.size() > 0) {
						activity.xmppConnectionService.databaseBackend.removeDisappearingMessages(removedIds);
						List<Message> dbmessages = activity.xmppConnectionService.databaseBackend.getMessages(ConversationFragment.this.conversation, Config.PAGE_SIZE);
						ConversationFragment.this.conversation.clearMessages();
						ConversationFragment.this.conversation.addAll(0, dbmessages);
						ConversationFragment.this.conversation.populateWithMessages(ConversationFragment.this.messageList);
						messageListAdapter.notifyDataSetChanged(); // or updateUI?
					} else if (updateStatus) {
						messageListAdapter.notifyDataSetChanged();
					}

					// and here comes the "trick"
					disappearingHandler.postDelayed(this, 1000);
				}
			};
			disappearingHandler.postDelayed(disRunnable, 2000);
		}

		if (hasExtras || scrolledToBottomAndNoPending) {
			resetUnreadMessagesCount();
			synchronized (this.messageList) {
				Log.d(Config.LOGTAG, "jump to first unread message");
				final Message first = conversation.getFirstUnreadMessage();
				final int bottom = Math.max(0, this.messageList.size() - 1);
				final int pos;
				final boolean jumpToBottom;
				if (first == null) {
					pos = bottom;
					jumpToBottom = true;
				} else {
					int i = getIndexOf(first.getUuid(), this.messageList);
					pos = i < 0 ? bottom : i;
					jumpToBottom = false;
				}
				setSelection(pos, jumpToBottom);
			}
		}


		this.binding.messagesView.post(this::fireReadEvent);
		//TODO if we only do this when this fragment is running on main it won't *bing* in tablet layout which might be unnecessary since we can *see* it
		activity.xmppConnectionService.getNotificationService().setOpenConversation(this.conversation);

		if ((conversation.getMode() == Conversation.MODE_MULTI) && !(conversation.getMucOptions().membersOnly())) {
			conversation.setNextEncryption(Message.ENCRYPTION_NONE);
		} else {
			AxolotlService axolotlService = conversation.getAccount().getAxolotlService();
			if (axolotlService != null) {
				axolotlService.createSessionsIfNeeded(conversation);
				conversation.reloadFingerprints(axolotlService.getCryptoTargets(conversation));
				conversation.setNextEncryption(Message.ENCRYPTION_AXOLOTL);
			} else {
				conversation.setNextEncryption(Message.ENCRYPTION_NONE);
			}
		}

		return true;
	}

	public void startDisappearingMessagesHandler() {
		if (disappearingHandler != null && disRunnable != null) {
			disappearingHandler.postDelayed(disRunnable, 1000);
		}
	}

	public void endDisappearingMessagesHandler() {
		if (disappearingHandler != null && disRunnable != null) {
			disappearingHandler.removeCallbacks(disRunnable);
		}
	}

	private void resetUnreadMessagesCount() {
		lastMessageUuid = null;
		hideUnreadMessagesCount();
	}

	private void hideUnreadMessagesCount() {
		if (this.binding == null) {
			return;
		}
		this.binding.scrollToBottomButton.setEnabled(false);
		this.binding.scrollToBottomButton.hide();
		this.binding.unreadCountCustomView.setVisibility(View.GONE);
	}

	private void setSelection(int pos, boolean jumpToBottom) {
		ListViewUtils.setSelection(this.binding.messagesView, pos, jumpToBottom);
		this.binding.messagesView.post(() -> ListViewUtils.setSelection(this.binding.messagesView, pos, jumpToBottom));
		this.binding.messagesView.post(this::fireReadEvent);
	}


	private boolean scrolledToBottom() {
		return this.binding != null && scrolledToBottom(this.binding.messagesView);
	}

	private void processExtras(Bundle extras) {
		final String downloadUuid = extras.getString(ConversationsActivity.EXTRA_DOWNLOAD_UUID);
		final String text = extras.getString(Intent.EXTRA_TEXT);
		final String nick = extras.getString(ConversationsActivity.EXTRA_NICK);
		final boolean asQuote = extras.getBoolean(ConversationsActivity.EXTRA_AS_QUOTE);
		final boolean pm = extras.getBoolean(ConversationsActivity.EXTRA_IS_PRIVATE_MESSAGE, false);
		final boolean doNotAppend = extras.getBoolean(ConversationsActivity.EXTRA_DO_NOT_APPEND, false);
		final List<Uri> uris = extractUris(extras);
		if (uris != null && uris.size() > 0) {
			final List<Uri> cleanedUris = cleanUris(new ArrayList<>(uris));
			mediaPreviewAdapter.addMediaPreviews(Attachment.of(getActivity(), cleanedUris));
			toggleInputMethod();
			return;
		}
		if (nick != null) {
			if (pm) {
				Jid jid = conversation.getJid();
				try {
					Jid next = Jid.of(jid.getLocal(), jid.getDomain(), nick);
					privateMessageWith(next);
				} catch (final IllegalArgumentException ignored) {
					//do nothing
				}
			} else {
				final MucOptions mucOptions = conversation.getMucOptions();
				if (mucOptions.participating() || conversation.getNextCounterpart() != null) {
					highlightInConference(nick);
				}
			}
		} else {
			if (text != null && asQuote) {
				quoteText(text);
			} else {
				appendText(text, doNotAppend);
			}
		}
		final Message message = downloadUuid == null ? null : conversation.findMessageWithFileAndUuid(downloadUuid);
		if (message != null) {
			startDownloadable(message);
		}
	}

	private List<Uri> extractUris(Bundle extras) {
		final List<Uri> uris = extras.getParcelableArrayList(Intent.EXTRA_STREAM);
		if (uris != null) {
			return uris;
		}
		final Uri uri = extras.getParcelable(Intent.EXTRA_STREAM);
		if (uri != null) {
			return Collections.singletonList(uri);
		} else {
			return null;
		}
	}

	private List<Uri> cleanUris(List<Uri> uris) {
		Iterator<Uri> iterator = uris.iterator();
		while (iterator.hasNext()) {
			final Uri uri = iterator.next();
			if (FileBackend.weOwnFile(getActivity(), uri)) {
				iterator.remove();
				Toast.makeText(getActivity(), R.string.security_violation_not_attaching_file, Toast.LENGTH_SHORT).show();
			}
		}
		return uris;
	}


	private boolean showBlockSubmenu(View view) {
		final Jid jid = conversation.getJid();
		final boolean showReject = !conversation.isWithStranger() && conversation.getContact().getOption(Contact.Options.PENDING_SUBSCRIPTION_REQUEST);
		PopupMenu popupMenu = new PopupMenu(getActivity(), view);
		popupMenu.inflate(R.menu.block);
		popupMenu.getMenu().findItem(R.id.block_contact).setVisible(jid.getLocal() != null);
		popupMenu.getMenu().findItem(R.id.reject).setVisible(showReject);
		popupMenu.setOnMenuItemClickListener(menuItem -> {
			Blockable blockable;
			switch (menuItem.getItemId()) {
				case R.id.reject:
					activity.xmppConnectionService.stopPresenceUpdatesTo(conversation.getContact());
					updateSnackBar(conversation);
					return true;
				case R.id.block_domain:
					blockable = conversation.getAccount().getRoster().getContact(Jid.ofDomain(jid.getDomain()));
					break;
				default:
					blockable = conversation;
			}
			BlockContactDialog.show(activity, blockable);
			return true;
		});
		popupMenu.show();
		return true;
	}

	private void updateSnackBar(final Conversation conversation) {
		final Account account = conversation.getAccount();
		final XmppConnection connection = account.getXmppConnection();
		final int mode = conversation.getMode();
		final Contact contact = mode == Conversation.MODE_SINGLE ? conversation.getContact() : null;
		if (conversation.getStatus() == Conversation.STATUS_ARCHIVED) {
			return;
		}

		if (conversation.isBlocked()) {
			showSnackbar(R.string.contact_blocked, R.string.unblock, this.mUnblockClickListener);
		} else if (contact != null && !contact.showInRoster() && contact.getOption(Contact.Options.PENDING_SUBSCRIPTION_REQUEST)) {
			showSnackbar(R.string.contact_added_you, R.string.add_back, this.mAddBackClickListener, this.mLongPressBlockListener);
		} else if (contact != null && contact.getOption(Contact.Options.PENDING_SUBSCRIPTION_REQUEST)) {
			showSnackbar(activity.getString(R.string.contact_asks_for_presence_subscription, contact.getDisplayName()), R.string.allow, this.mAllowPresenceSubscription, this.mLongPressBlockListener);
		} else if (mode == Conversation.MODE_MULTI
				&& !conversation.getMucOptions().online()
				&& account.getStatus() == Account.State.ONLINE) {
			switch (conversation.getMucOptions().getError()) {
				case NICK_IN_USE:
					showSnackbar(R.string.nick_in_use, R.string.edit, clickToMuc);
					break;
				case NO_RESPONSE:
					showSnackbar(R.string.joining_conference, 0, null);
					break;
				case SERVER_NOT_FOUND:
					if (conversation.receivedMessagesCount() > 0) {
						showSnackbar(R.string.remote_server_not_found, R.string.try_again, joinMuc);
					} else {
						showSnackbar(R.string.remote_server_not_found, R.string.leave, leaveMuc);
					}
					break;
				case REMOTE_SERVER_TIMEOUT:
					if (conversation.receivedMessagesCount() > 0) {
						showSnackbar(R.string.remote_server_timeout, R.string.try_again, joinMuc);
					} else {
						showSnackbar(R.string.remote_server_timeout, R.string.leave, leaveMuc);
					}
					break;
				case PASSWORD_REQUIRED:
					showSnackbar(R.string.conference_requires_password, R.string.enter_password, enterPassword);
					break;
				case BANNED:
					showSnackbar(R.string.conference_banned, R.string.leave, leaveMuc);
					break;
				case MEMBERS_ONLY:
					showSnackbar(R.string.conference_members_only, R.string.leave, leaveMuc);
					break;
				case RESOURCE_CONSTRAINT:
					showSnackbar(R.string.conference_resource_constraint, R.string.try_again, joinMuc);
					break;
				case KICKED:
					showSnackbar(R.string.conference_kicked, R.string.join, joinMuc);
					break;
				case UNKNOWN:
					showSnackbar(R.string.conference_unknown_error, R.string.try_again, joinMuc);
					break;
				case INVALID_NICK:
					showSnackbar(R.string.invalid_muc_nick, R.string.edit, clickToMuc);
				case SHUTDOWN:
					showSnackbar(R.string.conference_shutdown, R.string.leave, leaveMuc);
					break;
				case DESTROYED:
					showSnackbar(R.string.conference_destroyed, R.string.leave, leaveMuc);
					break;
				case NON_ANONYMOUS:
					showSnackbar(R.string.group_chat_will_make_your_jabber_id_public, R.string.join, acceptJoin);
					break;
				default:
					hideSnackbar();
					break;
			}
			//} else if (account.hasPendingPgpIntent(conversation)) {
			//	showSnackbar(R.string.openpgp_messages_found, R.string.decrypt, clickToDecryptListener);
		} else if (connection != null
				&& connection.getFeatures().blocking()
				&& conversation.countMessages() != 0
				&& !conversation.isBlocked()
				&& conversation.isWithStranger()) {
			showSnackbar(R.string.received_message_from_stranger, R.string.block, mBlockClickListener);
		} else {
			hideSnackbar();
		}
	}

	@Override
	public void refresh() {
		if (this.binding == null) {
			Log.d(Config.LOGTAG, "ConversationFragment.refresh() skipped updated because view binding was null");
			return;
		}
		if (this.conversation != null && this.activity != null && this.activity.xmppConnectionService != null) {
			if (!activity.xmppConnectionService.isConversationStillOpen(this.conversation)) {
				activity.onConversationArchived(this.conversation);
				return;
			}
		}
		this.refresh(true);
	}

	private void refresh(boolean notifyConversationRead) {

		synchronized (this.messageList) {
			if (this.conversation != null) {
				conversation.populateWithMessages(this.messageList);
				updateSnackBar(conversation);
				updateStatusMessages();
				if (conversation.getMode() == Conversation.MODE_MULTI) {
					updateGroupChanged();
				}
				if (conversation.getReceivedMessagesCountSinceUuid(lastMessageUuid) != 0) {
					binding.unreadCountCustomView.setVisibility(View.VISIBLE);
					binding.unreadCountCustomView.setUnreadCount(conversation.getReceivedMessagesCountSinceUuid(lastMessageUuid));
				}
				this.messageListAdapter.notifyDataSetChanged();
				updateChatMsgHint();
				if (notifyConversationRead && activity != null) {
					binding.messagesView.post(this::fireReadEvent);
				}
				updateSendButton();
				updateEditablity();
				activity.invalidateOptionsMenu();
			}
		}
	}

	protected void messageSent() {
		mSendingPgpMessage.set(false);
		this.binding.textinput.setText("");
		if (conversation.setCorrectingMessage(null)) {
			this.binding.textinput.append(conversation.getDraftMessage());
			conversation.setDraftMessage(null);
		}
		storeNextMessage();
		updateChatMsgHint();
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(activity);
		final boolean prefScrollToBottom = p.getBoolean("scroll_to_bottom", activity.getResources().getBoolean(R.bool.scroll_to_bottom));
		if (prefScrollToBottom || scrolledToBottom()) {
			new Handler().post(() -> {
				int size = messageList.size();
				this.binding.messagesView.setSelection(size - 1);
			});
		}
	}

	private boolean storeNextMessage() {
		return storeNextMessage(this.binding.textinput.getText().toString());
	}

	private boolean storeNextMessage(String msg) {
		final boolean participating = conversation.getMode() == Conversational.MODE_SINGLE || conversation.getMucOptions().participating();
		if (this.conversation.getStatus() != Conversation.STATUS_ARCHIVED && participating && this.conversation.setNextMessage(msg)) {
			this.activity.xmppConnectionService.updateConversation(this.conversation);
			return true;
		}
		return false;
	}

	public void doneSendingPgpMessage() {
		mSendingPgpMessage.set(false);
	}

	public long getMaxHttpUploadSize(Conversation conversation) {
		final XmppConnection connection = conversation.getAccount().getXmppConnection();
		return connection == null ? -1 : connection.getFeatures().getMaxHttpUploadSize();
	}

	private void updateEditablity() {
		boolean canWrite = this.conversation.getMode() == Conversation.MODE_SINGLE || this.conversation.getMucOptions().participating() || this.conversation.getNextCounterpart() != null;
		this.binding.textinput.setFocusable(canWrite);
		this.binding.textinput.setFocusableInTouchMode(canWrite);
		this.binding.textSendButton.setEnabled(canWrite);
		this.binding.textinput.setCursorVisible(canWrite);
		this.binding.textinput.setEnabled(canWrite);
	}

	public void updateSendButton() {
		boolean hasAttachments = mediaPreviewAdapter != null && mediaPreviewAdapter.hasAttachments();
		boolean useSendButtonToIndicateStatus = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("send_button_status", getResources().getBoolean(R.bool.send_button_status));
		final Conversation c = this.conversation;
		final Presence.Status status;
		final String text = this.binding.textinput == null ? "" : this.binding.textinput.getText().toString();
		final SendButtonAction action;
		if (hasAttachments) {
			action = SendButtonAction.TEXT;
		} else {
			action = SendButtonTool.getAction(getActivity(), c, text);
		}
		if (useSendButtonToIndicateStatus && c.getAccount().getStatus() == Account.State.ONLINE) {
			if (activity.xmppConnectionService != null && activity.xmppConnectionService.getMessageArchiveService().isCatchingUp(c)) {
				status = Presence.Status.OFFLINE;
			} else if (c.getMode() == Conversation.MODE_SINGLE) {
				status = c.getContact().getShownStatus();
			} else {
				status = c.getMucOptions().online() ? Presence.Status.ONLINE : Presence.Status.OFFLINE;
			}
		} else {
			status = Presence.Status.OFFLINE;
		}
		this.binding.textSendButton.setTag(action);
	}

	protected void updateGroupChanged() {
		lastGroupRemoved = null;
		boolean onlyStatus = true;
		synchronized (this.messageList) {
			for(int i = this.messageList.size() - 1; i >= 0; --i) {
				final Message current = this.messageList.get(i);
				if (current.getBody().endsWith(getString(R.string.added_to_group)) ||
						current.getBody().endsWith(getString(R.string.left_group))) {
					if (lastGroupRemoved == null && onlyStatus) {
						lastGroupRemoved = current.getUuid();
					}
					this.messageList.add(i+1,Message.createGroupChangedSeparator(current));
					this.messageList.remove(i);
				} else if (current.getEncryption() == Message.ENCRYPTION_AXOLOTL_NOT_FOR_THIS_DEVICE) {
					if (lastGroupRemoved == null && onlyStatus) {
						lastGroupRemoved = current.getUuid();
					}
					this.messageList.add(i+1,Message.createNotForThisDeviceSeparator(current, getString(R.string.not_encrypted_for_this_device)));
					this.messageList.remove(i);
				} else if (current.getType() != Message.TYPE_STATUS) {
					onlyStatus = false;
				}
			}
		}
	}

	protected void setTimerStatus(String tstatus, boolean global) {
		String timerStatus = "Disappearing message time set to " + tstatus;
		if (global) {
			timerStatus = "Global disappearing message time set to " + tstatus;
		}

		Message disMessageStatus = Message.createStatusMessage(conversation, timerStatus);
		disMessageStatus.setTime(System.currentTimeMillis());
		disMessageStatus.setEndTime(Long.MAX_VALUE);
		disMessageStatus.setTimer(Message.TIMER_NONE);
		activity.xmppConnectionService.databaseBackend.createMessage(disMessageStatus);
		this.conversation.add(disMessageStatus);
		this.conversation.populateWithMessages(ConversationFragment.this.messageList);
		messageListAdapter.notifyDataSetChanged();
	}

	protected void updateDateSeparators() {
		synchronized (this.messageList) {
			DateSeparator.addAll(this.messageList);
		}
	}

	protected void updateStatusMessages() {
		updateDateSeparators();
		synchronized (this.messageList) {
			if (showLoadMoreMessages(conversation)) {
				this.messageList.add(0, Message.createLoadMoreMessage(conversation));
			}
			if (conversation.getMode() == Conversation.MODE_SINGLE) {
				ChatState state = conversation.getIncomingChatState();
				if (state == ChatState.COMPOSING) {
					this.messageList.add(Message.createStatusMessage(conversation, getString(R.string.contact_is_typing, conversation.getName())));
				} else if (state == ChatState.PAUSED) {
					this.messageList.add(Message.createStatusMessage(conversation, getString(R.string.contact_has_stopped_typing, conversation.getName())));
				} else {
					for (int i = this.messageList.size() - 1; i >= 0; --i) {
						final Message message = this.messageList.get(i);
						if (message.getType() != Message.TYPE_STATUS) {
							if (message.getStatus() == Message.STATUS_RECEIVED) {
								return;
							} else {
								if (message.getStatus() == Message.STATUS_SEND_DISPLAYED) {
									this.messageList.add(i + 1,
											Message.createStatusMessage(conversation, getString(R.string.contact_has_read_up_to_this_point, conversation.getName())));
									return;
								}
							}
						}
					}
				}
			} else {
				final MucOptions mucOptions = conversation.getMucOptions();
				final List<MucOptions.User> allUsers = mucOptions.getUsers();
				final Set<ReadByMarker> addedMarkers = new HashSet<>();
				ChatState state = ChatState.COMPOSING;
				List<MucOptions.User> users = conversation.getMucOptions().getUsersWithChatState(state, 5);
				if (users.size() == 0) {
					state = ChatState.PAUSED;
					users = conversation.getMucOptions().getUsersWithChatState(state, 5);
				}
				if (mucOptions.isPrivateAndNonAnonymous()) {
					for (int i = this.messageList.size() - 1; i >= 0; --i) {
						final Set<ReadByMarker> markersForMessage = messageList.get(i).getReadByMarkers();
						final List<MucOptions.User> shownMarkers = new ArrayList<>();
						for (ReadByMarker marker : markersForMessage) {
							if (!ReadByMarker.contains(marker, addedMarkers)) {
								addedMarkers.add(marker); //may be put outside this condition. set should do dedup anyway
								MucOptions.User user = mucOptions.findUser(marker);
								if (user != null && !users.contains(user)) {
									shownMarkers.add(user);
								}
							}
						}
						final ReadByMarker markerForSender = ReadByMarker.from(messageList.get(i));
						final Message statusMessage;
						final int size = shownMarkers.size();
						if (size > 1) {
							final String body;
							if (size <= 4) {
								body = getString(R.string.contacts_have_read_up_to_this_point, UIHelper.concatNames(shownMarkers));
							} else if (ReadByMarker.allUsersRepresented(allUsers, markersForMessage, markerForSender)) {
								body = getString(R.string.everyone_has_read_up_to_this_point);
							} else {
								body = getString(R.string.contacts_and_n_more_have_read_up_to_this_point, UIHelper.concatNames(shownMarkers, 3), size - 3);
							}
							statusMessage = Message.createStatusMessage(conversation, body);
							statusMessage.setCounterparts(shownMarkers);
						} else if (size == 1) {
							statusMessage = Message.createStatusMessage(conversation, getString(R.string.contact_has_read_up_to_this_point, UIHelper.getDisplayName(shownMarkers.get(0))));
							statusMessage.setCounterpart(shownMarkers.get(0).getFullJid());
							statusMessage.setTrueCounterpart(shownMarkers.get(0).getRealJid());
						} else {
							statusMessage = null;
						}
						if (statusMessage != null) {
							this.messageList.add(i + 1, statusMessage);
						}
						addedMarkers.add(markerForSender);
						if (ReadByMarker.allUsersRepresented(allUsers, addedMarkers)) {
							break;
						}
					}
				}
				if (users.size() > 0) {
					Message statusMessage;
					if (users.size() == 1) {
						MucOptions.User user = users.get(0);
						int id = state == ChatState.COMPOSING ? R.string.contact_is_typing : R.string.contact_has_stopped_typing;
						statusMessage = Message.createStatusMessage(conversation, getString(id, UIHelper.getDisplayName(user)));
						statusMessage.setTrueCounterpart(user.getRealJid());
						statusMessage.setCounterpart(user.getFullJid());
					} else {
						int id = state == ChatState.COMPOSING ? R.string.contacts_are_typing : R.string.contacts_have_stopped_typing;
						statusMessage = Message.createStatusMessage(conversation, getString(id, UIHelper.concatNames(users)));
						statusMessage.setCounterparts(users);
					}
					this.messageList.add(statusMessage);
				}

			}
		}
	}

	private void stopScrolling() {
		long now = SystemClock.uptimeMillis();
		MotionEvent cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0, 0, 0);
		binding.messagesView.dispatchTouchEvent(cancel);
	}

	private boolean showLoadMoreMessages(final Conversation c) {
		if (activity == null || activity.xmppConnectionService == null) {
			return false;
		}
		final boolean mam = hasMamSupport(c) && !c.getContact().isBlocked();
		final MessageArchiveService service = activity.xmppConnectionService.getMessageArchiveService();
		return mam && (c.getLastClearHistory().getTimestamp() != 0 || (c.countMessages() == 0 && c.messagesLoaded.get() && c.hasMessagesLeftOnServer() && !service.queryInProgress(c)));
	}

	private boolean hasMamSupport(final Conversation c) {
		if (c.getMode() == Conversation.MODE_SINGLE) {
			final XmppConnection connection = c.getAccount().getXmppConnection();
			return connection != null && connection.getFeatures().mam();
		} else {
			return c.getMucOptions().mamSupport();
		}
	}

	protected void showSnackbar(final int message, final int action, final OnClickListener clickListener) {
		showSnackbar(message, action, clickListener, null);
	}

	protected void showSnackbar(final int message, final int action, final OnClickListener clickListener, final View.OnLongClickListener longClickListener) {
		this.binding.snackbar.setVisibility(View.VISIBLE);
		this.binding.snackbar.setOnClickListener(null);
		this.binding.snackbarMessage.setText(message);
		this.binding.snackbarMessage.setOnClickListener(null);
		this.binding.snackbarAction.setVisibility(clickListener == null ? View.GONE : View.VISIBLE);
		if (action != 0) {
			this.binding.snackbarAction.setText(action);
		}
		this.binding.snackbarAction.setOnClickListener(clickListener);
		this.binding.snackbarAction.setOnLongClickListener(longClickListener);
	}

	protected void showSnackbar(final String message, final int action, final OnClickListener clickListener, final View.OnLongClickListener longClickListener) {
		this.binding.snackbar.setVisibility(View.VISIBLE);
		this.binding.snackbar.setOnClickListener(null);
		this.binding.snackbarMessage.setText(message);
		this.binding.snackbarMessage.setOnClickListener(null);
		this.binding.snackbarAction.setVisibility(clickListener == null ? View.GONE : View.VISIBLE);
		if (action != 0) {
			this.binding.snackbarAction.setText(action);
		}
		this.binding.snackbarAction.setOnClickListener(clickListener);
		this.binding.snackbarAction.setOnLongClickListener(longClickListener);
	}

	protected void hideSnackbar() {
		this.binding.snackbar.setVisibility(View.GONE);
	}

	protected void sendMessage(Message message) {
		activity.xmppConnectionService.sendMessage(message);
		messageSent();
	}

	/*protected void sendPgpMessage(final Message message) {
		final XmppConnectionService xmppService = activity.xmppConnectionService;
		final Contact contact = message.getConversation().getContact();
		if (!activity.hasPgp()) {
			activity.showInstallPgpDialog();
			return;
		}
		if (conversation.getAccount().getPgpSignature() == null) {
			activity.announcePgp(conversation.getAccount(), conversation, null, activity.onOpenPGPKeyPublished);
			return;
		}
		if (!mSendingPgpMessage.compareAndSet(false, true)) {
			Log.d(Config.LOGTAG, "sending pgp message already in progress");
		}
		if (conversation.getMode() == Conversation.MODE_SINGLE) {
			if (contact.getPgpKeyId() != 0) {
				xmppService.getPgpEngine().hasKey(contact,
						new UiCallback<Contact>() {

							@Override
							public void userInputRequried(PendingIntent pi, Contact contact) {
								startPendingIntent(pi, REQUEST_ENCRYPT_MESSAGE);
							}

							@Override
							public void success(Contact contact) {
								encryptTextMessage(message);
							}

							@Override
							public void error(int error, Contact contact) {
								activity.runOnUiThread(() -> Toast.makeText(activity,
										R.string.unable_to_connect_to_keychain,
										Toast.LENGTH_SHORT
								).show());
								mSendingPgpMessage.set(false);
							}
						});

			} else {
				showNoPGPKeyDialog(false, (dialog, which) -> {
					conversation.setNextEncryption(Message.ENCRYPTION_NONE);
					xmppService.updateConversation(conversation);
					message.setEncryption(Message.ENCRYPTION_NONE);
					xmppService.sendMessage(message);
					messageSent();
				});
			}
		} else {
			if (conversation.getMucOptions().pgpKeysInUse()) {
				if (!conversation.getMucOptions().everybodyHasKeys()) {
					Toast warning = Toast
							.makeText(getActivity(),
									R.string.missing_public_keys,
									Toast.LENGTH_LONG);
					warning.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
					warning.show();
				}
				encryptTextMessage(message);
			} else {
				showNoPGPKeyDialog(true, (dialog, which) -> {
					conversation.setNextEncryption(Message.ENCRYPTION_NONE);
					message.setEncryption(Message.ENCRYPTION_NONE);
					xmppService.updateConversation(conversation);
					xmppService.sendMessage(message);
					messageSent();
				});
			}
		}
	}

	public void encryptTextMessage(Message message) {
		activity.xmppConnectionService.getPgpEngine().encrypt(message,
				new UiCallback<Message>() {

					@Override
					public void userInputRequried(PendingIntent pi, Message message) {
						startPendingIntent(pi, REQUEST_SEND_MESSAGE);
					}

					@Override
					public void success(Message message) {
						//TODO the following two call can be made before the callback
						getActivity().runOnUiThread(() -> messageSent());
					}

					@Override
					public void error(final int error, Message message) {
						getActivity().runOnUiThread(() -> {
							doneSendingPgpMessage();
							Toast.makeText(getActivity(), error == 0 ? R.string.unable_to_connect_to_keychain : error, Toast.LENGTH_SHORT).show();
						});

					}
				});
	}

	public void showNoPGPKeyDialog(boolean plural, DialogInterface.OnClickListener listener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setIconAttribute(android.R.attr.alertDialogIcon);
		if (plural) {
			builder.setTitle(getString(R.string.no_pgp_keys));
			builder.setMessage(getText(R.string.contacts_have_no_pgp_keys));
		} else {
			builder.setTitle(getString(R.string.no_pgp_key));
			builder.setMessage(getText(R.string.contact_has_no_pgp_key));
		}
		builder.setNegativeButton(getString(R.string.cancel), null);
		builder.setPositiveButton(getString(R.string.send_unencrypted), listener);
		builder.create().show();
	}*/

	public void appendText(String text, final boolean doNotAppend) {
		if (text == null) {
			return;
		}
		final Editable editable = this.binding.textinput.getText();
		String previous = editable == null ? "" : editable.toString();
		if (doNotAppend && !TextUtils.isEmpty(previous)) {
			Toast.makeText(getActivity(), R.string.already_drafting_message, Toast.LENGTH_LONG).show();
			return;
		}
		if (UIHelper.isLastLineQuote(previous)) {
			text = '\n' + text;
		} else if (previous.length() != 0 && !Character.isWhitespace(previous.charAt(previous.length() - 1))) {
			text = " " + text;
		}
		this.binding.textinput.append(text);
	}

	@Override
	public boolean onEnterPressed() {
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getActivity());
		final boolean enterIsSend = p.getBoolean("enter_is_send", getResources().getBoolean(R.bool.enter_is_send));
		if (enterIsSend) {
			sendMessage();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onTypingStarted() {
		final XmppConnectionService service = activity == null ? null : activity.xmppConnectionService;
		if (service == null) {
			return;
		}
		Account.State status = conversation.getAccount().getStatus();
		if (status == Account.State.ONLINE && conversation.setOutgoingChatState(ChatState.COMPOSING)) {
			service.sendChatState(conversation);
		}
		updateSendButton();

	}

	@Override
	public void onTypingStopped() {
		final XmppConnectionService service = activity == null ? null : activity.xmppConnectionService;
		if (service == null) {
			return;
		}
		Account.State status = conversation.getAccount().getStatus();
		if (status == Account.State.ONLINE && conversation.setOutgoingChatState(ChatState.PAUSED)) {
			service.sendChatState(conversation);
		}
	}

	@Override
	public void onTextDeleted() {
		final XmppConnectionService service = activity == null ? null : activity.xmppConnectionService;
		if (service == null) {
			return;
		}
		Account.State status = conversation.getAccount().getStatus();
		if (status == Account.State.ONLINE && conversation.setOutgoingChatState(Config.DEFAULT_CHATSTATE)) {
			service.sendChatState(conversation);
		}
		if (storeNextMessage()) {
			activity.onConversationsListItemUpdated();
		}
		updateSendButton();
	}

	@Override
	public void onTextChanged() {
		if (conversation != null && conversation.getCorrectingMessage() != null) {
			updateSendButton();
		}
	}

	@Override
	public boolean onTabPressed(boolean repeated) {
		if (conversation == null || conversation.getMode() == Conversation.MODE_SINGLE) {
			return false;
		}
		if (repeated) {
			completionIndex++;
		} else {
			lastCompletionLength = 0;
			completionIndex = 0;
			final String content = this.binding.textinput.getText().toString();
			lastCompletionCursor = this.binding.textinput.getSelectionEnd();
			int start = lastCompletionCursor > 0 ? content.lastIndexOf(" ", lastCompletionCursor - 1) + 1 : 0;
			firstWord = start == 0;
			incomplete = content.substring(start, lastCompletionCursor);
		}
		List<String> completions = new ArrayList<>();
		for (MucOptions.User user : conversation.getMucOptions().getUsers()) {
			String name = user.getName();
			if (name != null && name.startsWith(incomplete)) {
				completions.add(name + (firstWord ? ": " : " "));
			}
		}
		Collections.sort(completions);
		if (completions.size() > completionIndex) {
			String completion = completions.get(completionIndex).substring(incomplete.length());
			this.binding.textinput.getEditableText().delete(lastCompletionCursor, lastCompletionCursor + lastCompletionLength);
			this.binding.textinput.getEditableText().insert(lastCompletionCursor, completion);
			lastCompletionLength = completion.length();
		} else {
			completionIndex = -1;
			this.binding.textinput.getEditableText().delete(lastCompletionCursor, lastCompletionCursor + lastCompletionLength);
			lastCompletionLength = 0;
		}
		return true;
	}

	private void startPendingIntent(PendingIntent pendingIntent, int requestCode) {
		try {
			getActivity().startIntentSenderForResult(pendingIntent.getIntentSender(), requestCode, null, 0, 0, 0);
		} catch (final SendIntentException ignored) {
		}
	}

	@Override
	public void onBackendConnected() {
		Log.d(Config.LOGTAG, "ConversationFragment.onBackendConnected()");
		String uuid = pendingConversationsUuid.pop();
		if (uuid != null) {
			if (!findAndReInitByUuidOrArchive(uuid)) {
				return;
			}
		} else {
			if (!activity.xmppConnectionService.isConversationStillOpen(conversation)) {
				clearPending();
				activity.onConversationArchived(conversation);
				return;
			}
		}
		ActivityResult activityResult = postponedActivityResult.pop();
		if (activityResult != null) {
			handleActivityResult(activityResult);
		}
		clearPending();
		activity.updateOfflineStatusBar();
	}

	private boolean findAndReInitByUuidOrArchive(@NonNull final String uuid) {
		Conversation conversation = activity.xmppConnectionService.findConversationByUuid(uuid);
		if (conversation == null) {
			clearPending();
			activity.onConversationArchived(null);
			return false;
		}
		reInit(conversation);
		ScrollState scrollState = pendingScrollState.pop();
		String lastMessageUuid = pendingLastMessageUuid.pop();
		List<Attachment> attachments = pendingMediaPreviews.pop();
		if (scrollState != null) {
			setScrollPosition(scrollState, lastMessageUuid);
		}
		if (attachments != null && attachments.size() > 0) {
			Log.d(Config.LOGTAG, "had attachments on restore");
			mediaPreviewAdapter.addMediaPreviews(attachments);
			toggleInputMethod();
		}
		return true;
	}

	private void clearPending() {
		if (postponedActivityResult.clear()) {
			Log.e(Config.LOGTAG, "cleared pending intent with unhandled result left");
		}
		if (pendingScrollState.clear()) {
			Log.e(Config.LOGTAG, "cleared scroll state");
		}
		if (pendingTakePhotoUri.clear()) {
			Log.e(Config.LOGTAG, "cleared pending photo uri");
		}
		if (pendingConversationsUuid.clear()) {
			Log.e(Config.LOGTAG,"cleared pending conversations uuid");
		}
		if (pendingMediaPreviews.clear()) {
			Log.e(Config.LOGTAG,"cleared pending media previews");
		}
	}

	public Conversation getConversation() {
		return conversation;
	}

	@Override
	public void onContactPictureLongClicked(View v, final Message message) {
		final String fingerprint;
		if (message.getEncryption() == Message.ENCRYPTION_PGP || message.getEncryption() == Message.ENCRYPTION_DECRYPTED) {
			fingerprint = "pgp";
		} else {
			fingerprint = message.getFingerprint();
		}
		final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
		final Contact contact = message.getContact();
		if (message.getStatus() <= Message.STATUS_RECEIVED && (contact == null  || !contact.isSelf())) {
			if (message.getConversation().getMode() == Conversation.MODE_MULTI) {
				final Jid cp = message.getCounterpart();
				if (cp == null || cp.isBareJid()) {
					return;
				}
				final Jid tcp = message.getTrueCounterpart();
				final MucOptions.User userByRealJid = tcp != null ? conversation.getMucOptions().findOrCreateUserByRealJid(tcp, cp) : null;
				final MucOptions.User user = userByRealJid != null ? userByRealJid : conversation.getMucOptions().findUserByFullJid(cp);
				popupMenu.inflate(R.menu.muc_details_context);
				final Menu menu = popupMenu.getMenu();
				MucDetailsContextMenuHelper.configureMucDetailsContextMenu(activity, menu, conversation, user);
				popupMenu.setOnMenuItemClickListener(menuItem -> MucDetailsContextMenuHelper.onContextItemSelected(menuItem, user, activity, fingerprint));
			} else {
				popupMenu.inflate(R.menu.one_on_one_context);
				popupMenu.setOnMenuItemClickListener(item -> {
					switch (item.getItemId()) {
						case R.id.action_contact_details:
							activity.switchToContactDetails(message.getContact(), fingerprint);
							break;
					}
					return true;
				});
			}
		} else {
			popupMenu.inflate(R.menu.account_context);
			final Menu menu = popupMenu.getMenu();
			popupMenu.setOnMenuItemClickListener(item -> {
				switch (item.getItemId()) {
					case R.id.action_account_details:
						activity.switchToAccount(message.getConversation().getAccount(), fingerprint);
						break;
				}
				return true;
			});
		}
		popupMenu.show();
	}

	@Override
	public void onContactPictureClicked(Message message) {
		String fingerprint;
		if (message.getEncryption() == Message.ENCRYPTION_PGP || message.getEncryption() == Message.ENCRYPTION_DECRYPTED) {
			fingerprint = "pgp";
		} else {
			fingerprint = message.getFingerprint();
		}
		final boolean received = message.getStatus() <= Message.STATUS_RECEIVED;
		if (received) {
			if (message.getConversation() instanceof Conversation && message.getConversation().getMode() == Conversation.MODE_MULTI) {
				Jid tcp = message.getTrueCounterpart();
				Jid user = message.getCounterpart();
				if (user != null && !user.isBareJid()) {
					final MucOptions mucOptions = ((Conversation) message.getConversation()).getMucOptions();
					if (mucOptions.participating() || ((Conversation) message.getConversation()).getNextCounterpart() != null) {
						if (!mucOptions.isUserInRoom(user) && mucOptions.findUserByRealJid(tcp == null ? null : tcp.asBareJid()) == null) {
							Toast.makeText(getActivity(), activity.getString(R.string.user_has_left_conference, user.getResource()), Toast.LENGTH_SHORT).show();
						}
						highlightInConference(user.getResource());
					} else {
						Toast.makeText(getActivity(), R.string.you_are_not_participating, Toast.LENGTH_SHORT).show();
					}
				}
				return;
			} else {
				if (!message.getContact().isSelf()) {
					activity.switchToContactDetails(message.getContact(), fingerprint);
					return;
				}
			}
		}
		activity.switchToAccount(message.getConversation().getAccount(), fingerprint);
	}
}