package com.glaciersecurity.glaciermessenger.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import androidx.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.databinding.ActivityStartConversationBinding;
import com.glaciersecurity.glaciermessenger.databinding.DialogPresenceBinding;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.Account.OnGroupUpdate;
import com.glaciersecurity.glaciermessenger.entities.Bookmark;
import com.glaciersecurity.glaciermessenger.entities.Contact;
import com.glaciersecurity.glaciermessenger.entities.Conversation;
import com.glaciersecurity.glaciermessenger.entities.ListItem;
import com.glaciersecurity.glaciermessenger.entities.Presence;
import com.glaciersecurity.glaciermessenger.entities.PresenceTemplate;
import com.glaciersecurity.glaciermessenger.services.ConnectivityReceiver;
import com.glaciersecurity.glaciermessenger.services.QuickConversationsService;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService.OnRosterUpdate;
import com.glaciersecurity.glaciermessenger.ui.adapter.ListItemAdapter;
import com.glaciersecurity.glaciermessenger.ui.interfaces.OnBackendConnected;
//import com.glaciersecurity.glaciermessenger.ui.service.EmojiService;
import com.glaciersecurity.glaciermessenger.ui.util.JidDialog;
import com.glaciersecurity.glaciermessenger.ui.util.MenuDoubleTabUtil;
import com.glaciersecurity.glaciermessenger.ui.util.PendingItem;
import com.glaciersecurity.glaciermessenger.ui.util.SoftKeyboardUtils;
import com.glaciersecurity.glaciermessenger.ui.widget.SwipeRefreshListFragment;
import com.glaciersecurity.glaciermessenger.utils.XmppUri;
import com.glaciersecurity.glaciermessenger.xmpp.OnUpdateBlocklist;
import com.glaciersecurity.glaciermessenger.xmpp.XmppConnection;
import rocks.xmpp.addr.Jid;

import static com.glaciersecurity.glaciermessenger.entities.Presence.StatusMessage.meetingIcon;
import static com.glaciersecurity.glaciermessenger.entities.Presence.StatusMessage.sickIcon;
import static com.glaciersecurity.glaciermessenger.entities.Presence.StatusMessage.travelIcon;
import static com.glaciersecurity.glaciermessenger.entities.Presence.StatusMessage.vacationIcon;
import static com.glaciersecurity.glaciermessenger.entities.Presence.getEmojiByUnicode;

public class StartConversationActivity extends XmppActivity implements XmppConnectionService.OnConversationUpdate, OnRosterUpdate, OnUpdateBlocklist, OnGroupUpdate, CreateConferenceDialog.CreateConferenceDialogListener, JoinConferenceDialog.JoinConferenceDialogListener, SwipeRefreshLayout.OnRefreshListener, ConnectivityReceiver.ConnectivityReceiverListener {

	public final static String DOMAIN_IP = "172.16.2.240";
	public static final String EXTRA_INVITE_URI = "com.glaciersecurity.glaciermessenger.invite_uri";

	private final int REQUEST_SYNC_CONTACTS = 0x28cf;
	private final int REQUEST_CREATE_CONFERENCE = 0x39da;
	private final PendingItem<Intent> pendingViewIntent = new PendingItem<>();
	private final PendingItem<String> mInitialSearchValue = new PendingItem<>();
	private final AtomicBoolean oneShotKeyboardSuppress = new AtomicBoolean();
	public int conference_context_id;
	public int contact_context_id;
	private ListPagerAdapter mListPagerAdapter;
	private List<ListItem> contacts = new ArrayList<>();
	private ListItemAdapter mContactsAdapter;
	private List<ListItem> conferences = new ArrayList<>();
	private ListItemAdapter mConferenceAdapter;
	private List<String> mActivatedAccounts = new ArrayList<>();
	private EditText mSearchEditText;
	private AtomicBoolean mRequestedContactsPermission = new AtomicBoolean(false);
	private boolean mHideOfflineContacts = false;
	private boolean createdByViewIntent = false;
	private Account curAccount = null;

	private ConnectivityReceiver connectivityReceiver;
	private LinearLayout offlineLayout;

	private MenuItem.OnActionExpandListener mOnActionExpandListener = new MenuItem.OnActionExpandListener() {

		@Override
		public boolean onMenuItemActionExpand(MenuItem item) {
			mSearchEditText.post(() -> {
				updateSearchViewHint();
				mSearchEditText.requestFocus();
				if (oneShotKeyboardSuppress.compareAndSet(true, false)) {
					return;
				}
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				if (imm != null) {
					imm.showSoftInput(mSearchEditText, InputMethodManager.SHOW_IMPLICIT);
				}
			});

			return true;
		}

		@Override
		public boolean onMenuItemActionCollapse(MenuItem item) {
			SoftKeyboardUtils.hideSoftKeyboard(StartConversationActivity.this);
			mSearchEditText.setText("");
			filter(null);
			return true;
		}
	};
	private TextWatcher mSearchTextWatcher = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable editable) {
			filter(editable.toString());
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
		}
	};
	private MenuItem mMenuSearchView;
	private ListItemAdapter.OnTagClickedListener mOnTagClickedListener = new ListItemAdapter.OnTagClickedListener() {
		@Override
		public void onTagClicked(String tag) {
			if (mMenuSearchView != null) {
				mMenuSearchView.expandActionView();
				mSearchEditText.setText("");
				mSearchEditText.append(tag);
				filter(tag);
			}
		}
	};

	private ListItemAdapter.OnContactClickedListener mOnContactClickedListener = new ListItemAdapter.OnContactClickedListener() {
		@Override
		public void onContactClicked(String contactJidString) {
			Jid jid = Jid.of(contactJidString);
			Contact contact = jid == null ? null : curAccount.getRoster().getContact(jid);
			openConversationForContact(contact);
		}
	};

	private ListItemAdapter.OnContactLongClickedListener mOnContactLongClickedListener = new ListItemAdapter.OnContactLongClickedListener() {
			@Override
			public void onContactClicked(String contactJidString) {
					Jid jid = Jid.of(contactJidString);
					Contact contact = jid == null ? null : curAccount.getRoster().getContact(jid);
					//openContextMenu(view);
			}
	};

	private Pair<Integer, Intent> mPostponedActivityResult;
	private Toast mToast;
	private UiCallback<Conversation> mAdhocConferenceCallback = new UiCallback<Conversation>() {
		@Override
		public void success(final Conversation conversation) {
			runOnUiThread(() -> {
				hideToast();
				switchToConversation(conversation);
			});
		}

		@Override
		public void error(final int errorCode, Conversation object) {
			runOnUiThread(() -> replaceToast(getString(errorCode)));
		}

		@Override
		public void userInputRequried(PendingIntent pi, Conversation object) {

		}
	};
	private ActivityStartConversationBinding binding;
	private TextView.OnEditorActionListener mSearchDone = new TextView.OnEditorActionListener() {
		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			int pos = binding.startConversationViewPager.getCurrentItem();
			if (pos == 0) {
				if (contacts.size() == 1) {
					openConversationForContact((Contact) contacts.get(0));
					return true;
				} else if (contacts.size() == 0 && conferences.size() == 1) {
					openConversationsForBookmark((Bookmark) conferences.get(0));
					return true;
				}
			} else {
				if (conferences.size() == 1) {
					openConversationsForBookmark((Bookmark) conferences.get(0));
					return true;
				} else if (conferences.size() == 0 && contacts.size() == 1) {
					openConversationForContact((Contact) contacts.get(0));
					return true;
				}
			}
			SoftKeyboardUtils.hideSoftKeyboard(StartConversationActivity.this);
			mListPagerAdapter.requestFocus(pos);
			return true;
		}
	};

	public static void populateAccountSpinner(Context context, List<String> accounts, Spinner spinner) {
		if (accounts.size() > 0) {
			ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.simple_list_item, accounts);
			adapter.setDropDownViewResource(R.layout.simple_list_item);
			spinner.setAdapter(adapter);
			spinner.setEnabled(true);
		} else {
			ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
					R.layout.simple_list_item,
                    Collections.singletonList(context.getString(R.string.no_accounts)));
			adapter.setDropDownViewResource(R.layout.simple_list_item);
			spinner.setAdapter(adapter);
			spinner.setEnabled(false);
		}
	}

	public static void launch(Context context) {
		final Intent intent = new Intent(context, StartConversationActivity.class);
		context.startActivity(intent);
	}

	private static Intent createLauncherIntent(Context context) {
		final Intent intent = new Intent(context, StartConversationActivity.class);
		intent.setAction(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		return intent;
	}

	private static boolean isViewIntent(final Intent i) {
		return i != null && (Intent.ACTION_VIEW.equals(i.getAction()) || Intent.ACTION_SENDTO.equals(i.getAction()) || i.hasExtra(EXTRA_INVITE_URI));
	}

	protected void hideToast() {
		if (mToast != null) {
			mToast.cancel();
		}
	}

	protected void replaceToast(String msg) {
		hideToast();
		mToast = Toast.makeText(this, msg, Toast.LENGTH_LONG);
		mToast.show();
	}

	@Override
	public void onRosterUpdate() {
		this.refreshUi();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.binding = DataBindingUtil.setContentView(this, R.layout.activity_start_conversation);
		Toolbar toolbar = (Toolbar) binding.toolbar;
		setSupportActionBar(toolbar);
		configureActionBar(getSupportActionBar());

		this.offlineLayout = findViewById(R.id.offline_layout);
		this.offlineLayout.setOnClickListener(mRefreshNetworkClickListener);
		connectivityReceiver = new ConnectivityReceiver(this);
		updateOfflineStatusBar();
		checkNetworkStatus();

		this.binding.fab.hide();
		this.binding.fab.setOnClickListener((v) -> {
			if (binding.startConversationViewPager.getCurrentItem() == 0) {
				/*String searchString = mSearchEditText != null ? mSearchEditText.getText().toString() : null;
				if (searchString != null && !searchString.trim().isEmpty()) {
					try {
						Jid jid = Jid.of(searchString);
						if (jid.getLocal() != null && jid.isBareJid() && jid.getDomain().contains(".")) {
							showCreateContactDialog(jid.toString(), null);
							return;
						}
					} catch (IllegalArgumentException ignored) {
						//ignore and fall through
					}
				}
				showCreateContactDialog(null, null);*/
			} else {
				showCreateConferenceDialog();
			}
		});
		binding.tabLayout.setupWithViewPager(binding.startConversationViewPager);
		binding.startConversationViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				updateSearchViewHint();
				onTabChanged();
			}
		});
		mListPagerAdapter = new ListPagerAdapter(getSupportFragmentManager());

		binding.startConversationViewPager.setAdapter(mListPagerAdapter);
		mConferenceAdapter = new ListItemAdapter(this, conferences);
		mContactsAdapter = new ListItemAdapter(this, contacts);
		mContactsAdapter.setOnTagClickedListener(this.mOnTagClickedListener);
		mContactsAdapter.setOnContactClickedListener(this.mOnContactClickedListener);
		mContactsAdapter.setOnContactLongClickedListener(this.mOnContactLongClickedListener);


		final SharedPreferences preferences = getPreferences();

		this.mHideOfflineContacts = QuickConversationsService.isConversations() && preferences.getBoolean("hide_offline", false);

		final boolean startSearching = preferences.getBoolean("start_searching",getResources().getBoolean(R.bool.start_searching));

		final Intent intent;
		if (savedInstanceState == null) {
			intent = getIntent();
		} else {
			createdByViewIntent = savedInstanceState.getBoolean("created_by_view_intent", false);
			final String search = savedInstanceState.getString("search");
			if (search != null) {
				mInitialSearchValue.push(search);
			}
			intent = savedInstanceState.getParcelable("intent");
		}

		if (isViewIntent(intent)) {
			pendingViewIntent.push(intent);
			createdByViewIntent = true;
			setIntent(createLauncherIntent(this));
		} else if (startSearching && mInitialSearchValue.peek() == null) {
			mInitialSearchValue.push("");
		}

		mRequestedContactsPermission.set(savedInstanceState != null && savedInstanceState.getBoolean("requested_contacts_permission",false));
		/*binding.speedDial.setOnActionSelectedListener(actionItem -> {
			final String searchString = mSearchEditText != null ? mSearchEditText.getText().toString() : null;
			final String prefilled;
			if (isValidJid(searchString)) {
				prefilled = Jid.ofEscaped(searchString).toEscapedString();
			} else {
				prefilled = null;
			}
			switch (actionItem.getId()) {
				case R.id.join_public_channel:
					showJoinConferenceDialog(prefilled);
					break;
				case R.id.create_private_group_chat:
					showCreatePrivateGroupChatDialog();
					break;
				case R.id.create_public_channel:
					showPublicChannelDialog();
					break;
				case R.id.create_contact:
					showCreateContactDialog(prefilled,null);
					break;
			}
			return false;
		});*/
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		Intent pendingIntent = pendingViewIntent.peek();
		savedInstanceState.putParcelable("intent", pendingIntent != null ? pendingIntent : getIntent());
		savedInstanceState.putBoolean("requested_contacts_permission",mRequestedContactsPermission.get());
		savedInstanceState.putBoolean("created_by_view_intent",createdByViewIntent);
		if (mMenuSearchView != null && mMenuSearchView.isActionViewExpanded()) {
			savedInstanceState.putString("search", mSearchEditText != null ? mSearchEditText.getText().toString() : null);
		}
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	protected void onStop () {
		unregisterReceiver(connectivityReceiver);
		super.onStop();
	}

	@Override
	public void onStart() {
		super.onStart();
		registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

		final int theme = findTheme();
		if (this.mTheme != theme) {
			recreate();
		}
//		else {
//			if (pendingViewIntent.peek() == null) {
//				askForPermissions();
//				//askForContactsPermissions();
//			}
//		}
		mConferenceAdapter.refreshSettings();
		mContactsAdapter.refreshSettings();
	}

	@Override
	public void onNewIntent(final Intent intent) {
		if (xmppConnectionServiceBound) {
			processViewIntent(intent);
		} else {
			pendingViewIntent.push(intent);
		}
		setIntent(createLauncherIntent(this));
	}

	protected void openConversationForContact(int position) {
		Contact contact = (Contact) contacts.get(position);
		openConversationForContact(contact);
	}

	protected void openConversationForContact(Contact contact) {
		Conversation conversation = xmppConnectionService.findOrCreateConversation(contact.getAccount(), contact.getJid(), false, true);
		SoftKeyboardUtils.hideSoftKeyboard(this);
		switchToConversation(conversation);
	}

	protected void openConversationForBookmark() {
		openConversationForBookmark(conference_context_id);
	}

	protected void openConversationForBookmark(int position) {
		Bookmark bookmark = (Bookmark) conferences.get(position);
		openConversationsForBookmark(bookmark);
	}

	protected void shareBookmarkUri() {
		shareBookmarkUri(conference_context_id);
	}

	protected void shareBookmarkUri(int position) {
		Bookmark bookmark = (Bookmark) conferences.get(position);
		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.putExtra(Intent.EXTRA_TEXT, "xmpp:" + bookmark.getJid().asBareJid().toEscapedString() + "?join");
		shareIntent.setType("text/plain");
		try {
			startActivity(Intent.createChooser(shareIntent, getText(R.string.share_uri_with)));
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, R.string.no_application_to_share_uri, Toast.LENGTH_SHORT).show();
		}
	}

	protected void openConversationsForBookmark(Bookmark bookmark) {
		final Jid jid = bookmark.getFullJid();
		if (jid == null) {
			Toast.makeText(this, R.string.invalid_jid, Toast.LENGTH_SHORT).show();
			return;
		}
		if (bookmark.getBookmarkName().startsWith("#")) {
			bookmark.setBookmarkName(bookmark.getBookmarkName().substring(1));
		}
		Conversation conversation = xmppConnectionService.findOrCreateConversation(bookmark.getAccount(), jid, true, true, true);
		bookmark.setConversation(conversation);
		if (!bookmark.autojoin() && getPreferences().getBoolean("autojoin", getResources().getBoolean(R.bool.autojoin))) {
			bookmark.setAutojoin(true);
		}
		SoftKeyboardUtils.hideSoftKeyboard(this);
		switchToConversation(conversation);

		// should only send join group message if bookmark doesn't already exist.
		boolean found = false;
		for (Bookmark existbookmark : bookmark.getAccount().getBookmarks()) {
			if (existbookmark.getJid().toString().equals(bookmark.getJid().asBareJid().toString())) {
				found = true;
			}
		}
		if (!found) {
			bookmark.getAccount().getBookmarks().add(bookmark);
			xmppConnectionService.pushBookmarks(bookmark.getAccount());
			xmppConnectionService.sendJoiningGroupMessage(conversation, new ArrayList(), true);
		}
	}

	protected void openDetailsForContact() {
		int position = contact_context_id;
		Contact contact = (Contact) contacts.get(position);
		switchToContactDetails(contact);
	}

	protected void showQrForContact() {
		int position = contact_context_id;
		Contact contact = (Contact) contacts.get(position);
		showQrCode("xmpp:"+contact.getJid().asBareJid().toEscapedString());
	}

	protected void toggleContactBlock() {
		final int position = contact_context_id;
		BlockContactDialog.show(this, (Contact) contacts.get(position));
	}

	protected void deleteContact() {
		final int position = contact_context_id;
		final Contact contact = (Contact) contacts.get(position);
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setNegativeButton(R.string.cancel, null);
		builder.setTitle(R.string.action_delete_contact);
		builder.setMessage(JidDialog.style(this, R.string.remove_contact_text, contact.getJid().getLocal()));
		builder.setPositiveButton(R.string.delete, (dialog, which) -> {
			xmppConnectionService.deleteContactOnServer(contact);
			filter(mSearchEditText.getText().toString());
		});
		builder.create().show();
	}

	protected void deleteConference() {
		int position = conference_context_id;
		final Bookmark bookmark = (Bookmark) conferences.get(position);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setNegativeButton(R.string.cancel, null);
		builder.setTitle(R.string.delete_bookmark);
		builder.setMessage(JidDialog.style(this, R.string.remove_bookmark_text,
				bookmark.getJid().getLocal()));
		builder.setPositiveButton(R.string.delete, (dialog, which) -> {
			bookmark.setConversation(null);
			Account account = bookmark.getAccount();
			account.getBookmarks().remove(bookmark);
			xmppConnectionService.pushBookmarks(account);
			filter(mSearchEditText.getText().toString());
		});
		builder.create().show();

	}

	@SuppressLint("InflateParams")
	protected void showCreateContactDialog(final String prefilledJid, final Invite invite) {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_DIALOG);
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		EnterJidDialog dialog = EnterJidDialog.newInstance(
				mActivatedAccounts,
				getString(R.string.dialog_title_create_contact),
				getString(R.string.add),
				prefilledJid,
				null,
				invite == null || !invite.hasFingerprints()
		);

		dialog.setOnEnterJidDialogPositiveListener((accountJid, contactJid) -> {
			if (!xmppConnectionServiceBound) {
				return false;
			}

			final Account account = xmppConnectionService.findAccountByJid(accountJid);
			if (account == null) {
				return true;
			}

			final Contact contact = account.getRoster().getContact(contactJid);
			if (invite != null && invite.getName() != null) {
				contact.setServerName(invite.getName());
			}
			if (contact.isSelf()) {
				switchToConversation(contact);
				return true;
			} else if (contact.showInRoster()) {
				throw new EnterJidDialog.JidError(getString(R.string.contact_already_exists));
			} else {
				xmppConnectionService.createContact(contact, true);
				if (invite != null && invite.hasFingerprints()) {
					xmppConnectionService.verifyFingerprints(contact, invite.getFingerprints());
				}
				switchToConversationDoNotAppend(contact, invite == null ? null : invite.getBody());
				return true;
			}
		});
		dialog.show(ft, FRAGMENT_TAG_DIALOG);
	}

	@SuppressLint("InflateParams")
	protected void showJoinConferenceDialog(final String prefilledJid) {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_DIALOG);
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		JoinConferenceDialog joinConferenceFragment = JoinConferenceDialog.newInstance(prefilledJid, mActivatedAccounts);
		joinConferenceFragment.show(ft, FRAGMENT_TAG_DIALOG);
	}

	private void showCreateConferenceDialog() {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_DIALOG);
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		CreateConferenceDialog createConferenceFragment = CreateConferenceDialog.newInstance(mActivatedAccounts);
		createConferenceFragment.show(ft, FRAGMENT_TAG_DIALOG);
	}

	private Account getSelectedAccount(Spinner spinner) {
		if (!spinner.isEnabled()) {
			return null;
		}
		Jid jid;
		try {
			if (Config.DOMAIN_LOCK != null) {
				jid = Jid.of((String) spinner.getSelectedItem(), Config.DOMAIN_LOCK, null);
			} else {
				jid = Jid.of((String) spinner.getSelectedItem());
			}
		} catch (final IllegalArgumentException e) {
			return null;
		}
		return xmppConnectionService.findAccountByJid(jid);
	}

	protected void switchToConversation(Contact contact) {
		Conversation conversation = xmppConnectionService.findOrCreateConversation(contact.getAccount(), contact.getJid(), false, true);
		switchToConversation(conversation);
	}

	protected void switchToConversationDoNotAppend(Contact contact, String body) {
		Conversation conversation = xmppConnectionService.findOrCreateConversation(contact.getAccount(), contact.getJid(), false, true);
		switchToConversationDoNotAppend(conversation, body);
	}

	@Override
	public void invalidateOptionsMenu() {
		boolean isExpanded = mMenuSearchView != null && mMenuSearchView.isActionViewExpanded();
		String text = mSearchEditText != null ? mSearchEditText.getText().toString() : "";
		if (isExpanded) {
			mInitialSearchValue.push(text);
			oneShotKeyboardSuppress.set(true);
		}
		super.invalidateOptionsMenu();
	}

	private void updateSearchViewHint() {
		if (binding == null || mSearchEditText == null) {
			return;
		}
		if (binding.startConversationViewPager.getCurrentItem() == 0) {
			mSearchEditText.setHint(R.string.search_contacts);
		} else {
			mSearchEditText.setHint(R.string.search_groups);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.start_conversation, menu);
		MenuItem menuHideOffline = menu.findItem(R.id.action_hide_offline);
		menuHideOffline.setChecked(this.mHideOfflineContacts);
		mMenuSearchView = menu.findItem(R.id.action_search);
		mMenuSearchView.setOnActionExpandListener(mOnActionExpandListener);
		View mSearchView = mMenuSearchView.getActionView();
		mSearchEditText = mSearchView.findViewById(R.id.search_field);
		mSearchEditText.addTextChangedListener(mSearchTextWatcher);
		mSearchEditText.setOnEditorActionListener(mSearchDone);


		String initialSearchValue = mInitialSearchValue.pop();
		if (initialSearchValue != null) {
			mMenuSearchView.expandActionView();
			mSearchEditText.append(initialSearchValue);
			filter(initialSearchValue);
		}
		updateSearchViewHint();
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (MenuDoubleTabUtil.shouldIgnoreTap()) {
			return false;
		}
		switch (item.getItemId()) {
			case android.R.id.home:
				navigateBack();
				return true;
			case R.id.action_hide_offline:
				mHideOfflineContacts = !item.isChecked();
				getPreferences().edit().putBoolean("hide_offline", mHideOfflineContacts).apply();
				if (mSearchEditText != null) {
					filter(mSearchEditText.getText().toString());
				}
				invalidateOptionsMenu();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_SEARCH && !event.isLongPress()) {
			openSearch();
			return true;
		}
		int c = event.getUnicodeChar();
		if (c > 32) {
			if (mSearchEditText != null && !mSearchEditText.isFocused()) {
				openSearch();
				mSearchEditText.append(Character.toString((char) c));
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}

	private void openSearch() {
		if (mMenuSearchView != null) {
			mMenuSearchView.expandActionView();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_OK) {
			if (xmppConnectionServiceBound) {
				this.mPostponedActivityResult = null;
				if (requestCode == REQUEST_CREATE_CONFERENCE) {
					Account account = extractAccount(intent);
					final String name = intent.getStringExtra(ChooseContactActivity.EXTRA_GROUP_CHAT_NAME);
					final List<Jid> jids = ChooseContactActivity.extractJabberIds(intent);

					boolean publicgroup = false;
					if (intent.getBooleanExtra(ChooseContactActivity.EXTRA_PUBLIC_GROUP, false)) {
						publicgroup = true;
					}

					if (account != null && jids.size() > 0) {
						if (xmppConnectionService.createAdhocConference(account, name, jids, publicgroup, mAdhocConferenceCallback)) {
							mToast = Toast.makeText(this, R.string.creating_conference, Toast.LENGTH_LONG);
							mToast.show();
						}
					}
				}
			} else {
				this.mPostponedActivityResult = new Pair<>(requestCode, intent);
			}
		}
		super.onActivityResult(requestCode, requestCode, intent);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
		if (grantResults.length > 0)
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				ScanActivity.onRequestPermissionResult(this, requestCode, grantResults);
				if (requestCode == REQUEST_SYNC_CONTACTS && xmppConnectionServiceBound) {
					if (QuickConversationsService.isQuicksy()) {
						setRefreshing(true);
					}
					xmppConnectionService.loadPhoneContacts();
					xmppConnectionService.startContactObserver();
				}
			}
	}

//	private void configureHomeButton() {
//		final ActionBar actionBar = getSupportActionBar();
//		if (actionBar == null) {
//			return;
//		}
//		boolean openConversations = !createdByViewIntent && !xmppConnectionService.isConversationsListEmpty(null);
//		actionBar.setDisplayHomeAsUpEnabled(true);
//		actionBar.setDisplayHomeAsUpEnabled(true);
//	}

	@Override
	protected void onBackendConnected() {
		if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
			xmppConnectionService.getQuickConversationsService().considerSyncBackground(false);
		}
		if (mPostponedActivityResult != null) {
			onActivityResult(mPostponedActivityResult.first, RESULT_OK, mPostponedActivityResult.second);
			this.mPostponedActivityResult = null;
		}
		this.mActivatedAccounts.clear();
		updateOfflineStatusBar();
		this.curAccount = null;
		for (Account account : xmppConnectionService.getAccounts()) {
			if (account.getStatus() != Account.State.DISABLED) {
				if (Config.DOMAIN_LOCK != null) {
					this.mActivatedAccounts.add(account.getJid().getLocal());
				} else {
					this.mActivatedAccounts.add(account.getJid().asBareJid().toString());
				}

				if (this.curAccount == null) {
					this.curAccount = account;
					this.curAccount.setGroupUpdateListener(this);
					this.curAccount.getXmppConnection().sendRoomDiscoveries();
				}
			}
		}
		//configureHomeButton();
		Intent intent = pendingViewIntent.pop();
		if (intent != null && processViewIntent(intent)) {
			filter(null);
		} else {
			if (mSearchEditText != null) {
				filter(mSearchEditText.getText().toString());
			} else {
				filter(null);
			}
		}
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_DIALOG);
		if (fragment instanceof OnBackendConnected) {
			Log.d(Config.LOGTAG, "calling on backend connected on dialog");
			((OnBackendConnected) fragment).onBackendConnected();
		}
		if (QuickConversationsService.isQuicksy()) {
			setRefreshing(xmppConnectionService.getQuickConversationsService().isSynchronizing());
		}
	}

	@Override
	public void onGroupUpdate() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				filterConferences(null);
			}
		});
	}

	protected boolean processViewIntent(@NonNull Intent intent) {
		final String inviteUri = intent.getStringExtra(EXTRA_INVITE_URI);
		if (inviteUri != null) {
			Invite invite = new Invite(inviteUri);
			if (invite.isJidValid()) {
				return invite.invite();
			}
		}
		final String action = intent.getAction();
		if (action == null) {
			return false;
		}
		switch (action) {
			case Intent.ACTION_SENDTO:
			case Intent.ACTION_VIEW:
				Uri uri = intent.getData();
				if (uri != null) {
					Invite invite = new Invite(intent.getData(), intent.getBooleanExtra("scanned", false));
					invite.account = intent.getStringExtra("account");
					return invite.invite();
				} else {
					return false;
				}
		}
		return false;
	}

	private boolean handleJid(Invite invite) {
		List<Contact> contacts = xmppConnectionService.findContacts(invite.getJid(), invite.account);
		if (invite.isAction(XmppUri.ACTION_JOIN)) {
			Conversation muc = xmppConnectionService.findFirstMuc(invite.getJid());
			if (muc != null) {
				switchToConversationDoNotAppend(muc, invite.getBody());
				return true;
			} else {
				showJoinConferenceDialog(invite.getJid().asBareJid().toString());
				return false;
			}
		} else if (contacts.size() == 0) {
			showCreateContactDialog(invite.getJid().toString(), invite);
			return false;
		} else if (contacts.size() == 1) {
			Contact contact = contacts.get(0);
			if (!invite.isSafeSource() && invite.hasFingerprints()) {
				displayVerificationWarningDialog(contact, invite);
			} else {
				if (invite.hasFingerprints()) {
					if (xmppConnectionService.verifyFingerprints(contact, invite.getFingerprints())) {
						Toast.makeText(this, R.string.verified_fingerprints, Toast.LENGTH_SHORT).show();
					}
				}
				if (invite.account != null) {
					xmppConnectionService.getShortcutService().report(contact);
				}
				switchToConversationDoNotAppend(contact, invite.getBody());
			}
			return true;
		} else {
			if (mMenuSearchView != null) {
				mMenuSearchView.expandActionView();
				mSearchEditText.setText("");
				mSearchEditText.append(invite.getJid().toString());
				filter(invite.getJid().toString());
			} else {
				mInitialSearchValue.push(invite.getJid().toString());
			}
			return true;
		}
	}

	private void displayVerificationWarningDialog(final Contact contact, final Invite invite) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.verify_glacier_keys);
		View view = getLayoutInflater().inflate(R.layout.dialog_verify_fingerprints, null);
        final CheckBox isTrustedSource = view.findViewById(R.id.trusted_source);
        TextView warning = view.findViewById(R.id.warning);
        warning.setText(JidDialog.style(this, R.string.verifying_glacier_keys_trusted_source, contact.getJid().asBareJid().toEscapedString(), contact.getDisplayName()));
		builder.setView(view);
		builder.setPositiveButton(R.string.confirm, (dialog, which) -> {
			if (isTrustedSource.isChecked() && invite.hasFingerprints()) {
				xmppConnectionService.verifyFingerprints(contact, invite.getFingerprints());
			}
			switchToConversationDoNotAppend(contact, invite.getBody());
		});
		builder.setNegativeButton(R.string.cancel, (dialog, which) -> StartConversationActivity.this.finish());
		AlertDialog dialog = builder.create();
		dialog.setCanceledOnTouchOutside(false);
		dialog.setOnCancelListener(dialog1 -> StartConversationActivity.this.finish());
		dialog.show();
	}

	protected void filter(String needle) {
		if (xmppConnectionServiceBound) {
			this.filterContacts(needle);
			this.filterConferences(needle);
		}
	}

	protected void filterContacts(String needle) {
		this.contacts.clear();
		for (Account account : xmppConnectionService.getAccounts()) {
			if (account.getStatus() != Account.State.DISABLED) {
				for (Contact contact : account.getRoster().getContacts()) {
					Presence.Status s = contact.getShownStatus();
					if (contact.showInContactList() && contact.match(this, needle)
							&& (!this.mHideOfflineContacts
							|| (needle != null && !needle.trim().isEmpty())
							|| s.compareTo(Presence.Status.OFFLINE) < 0)) {
						this.contacts.add(contact);
					}
				}
			}
		}
		Collections.sort(this.contacts);
		mContactsAdapter.notifyDataSetChanged();
	}

	protected void filterConferences(String needle) {
		this.conferences.clear();
		for (Account account : xmppConnectionService.getAccounts()) {
			if (account.getStatus() != Account.State.DISABLED) {
				for (Jid group : account.getAvailableGroups()) {
					final Bookmark gbookmark = new Bookmark(account, group.asBareJid());
					gbookmark.setAutojoin(getPreferences().getBoolean("autojoin", true));
					String nick = group.getResource();
					if (nick != null && !nick.isEmpty()) {
						gbookmark.setNick(nick);
					}
					gbookmark.setBookmarkName("#"+group.getLocal());

					this.conferences.add(gbookmark);
				}
			}
		}
		Collections.sort(this.conferences);
		Log.d(Config.LOGTAG, "Conferences updated with " + this.conferences.size());
		mConferenceAdapter.notifyDataSetChanged();
	}

	private void onTabChanged() {
		@DrawableRes final int fabDrawable;
		if (binding.startConversationViewPager.getCurrentItem() > 0) {
			fabDrawable = R.drawable.ic_group_add_white_24dp;
			binding.fab.setImageResource(fabDrawable);
			binding.fab.show();
		} else {
			binding.fab.hide();
		}
		invalidateOptionsMenu();
	}

	@Override
	public void OnUpdateBlocklist(final Status status) {
		refreshUi();
	}

	@Override
	protected void refreshUiReal() {
		if (mSearchEditText != null) {
			filter(mSearchEditText.getText().toString());
		}
		//configureHomeButton();
		if (QuickConversationsService.isQuicksy()) {
			setRefreshing(xmppConnectionService.getQuickConversationsService().isSynchronizing());
		}
	}

	@Override
	public void onBackPressed() {
		navigateBack();
	}

	private void navigateBack() {
		if (!createdByViewIntent && xmppConnectionService != null ) {
			Intent intent = new Intent(this, ConversationsActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			startActivity(intent);
		}
		finish();
	}

	@Override
	public void onCreateDialogPositiveClick(Spinner spinner, String name, CheckBox inviteOnly, EditText editName) {
		if (!xmppConnectionServiceBound) {
			return;
		}

		final Account account;
		if (spinner == null) {
			account = this.curAccount;
		} else {
			account = getSelectedAccount(spinner);
		}

		if (account == null) {
			return;
		}

		String groupname = name.replaceAll("[^a-zA-Z0-9]", "");
		final Jid conferenceJid;
		try {
			conferenceJid = Jid.of(groupname + "@conference." + account.getJid().getDomain());
		} catch (final Exception e) {
			editName.setError(getString(R.string.invalid_jid));
			mToast = Toast.makeText(this, R.string.invalid_jid, Toast.LENGTH_LONG);
			mToast.show();
			Log.d(Config.LOGTAG, "PROBLEM - The group name you entered is not a valid group name!");
			return;
		}

		if (curAccount.hasBookmarkFor(conferenceJid) || curAccount.groupExists(conferenceJid)) {
			editName.setError(getString(R.string.group_already_exists));
			mToast = Toast.makeText(this, R.string.group_already_exists, Toast.LENGTH_LONG);
			mToast.show();
			Log.d(Config.LOGTAG, "PROBLEM - a group chat named " + conferenceJid + " already exists!");
		} else {
			Intent intent = new Intent(getApplicationContext(), ChooseContactActivity.class);
			intent.putExtra(ChooseContactActivity.EXTRA_SHOW_ENTER_JID, true);
			intent.putExtra(ChooseContactActivity.EXTRA_SELECT_MULTIPLE, true);
			intent.putExtra(ChooseContactActivity.EXTRA_GROUP_CHAT_NAME, groupname.trim());

			if (!inviteOnly.isChecked()) {
				intent.putExtra(ChooseContactActivity.EXTRA_PUBLIC_GROUP, true);
			}
			intent.putExtra(EXTRA_ACCOUNT, account.getJid().asBareJid().toString());
			intent.putExtra(ChooseContactActivity.EXTRA_TITLE_RES_ID, R.string.choose_participants);

			startActivityForResult(intent, REQUEST_CREATE_CONFERENCE);
		}
	}

	@Override
	public void onJoinDialogPositiveClick(Dialog dialog, Spinner spinner, AutoCompleteTextView jid, boolean isBookmarkChecked) {
		if (!xmppConnectionServiceBound) {
			return;
		}
		final Account account = getSelectedAccount(spinner);
		if (account == null) {
			return;
		}
		final Jid conferenceJid;
		try {
			conferenceJid = Jid.of(jid.getText().toString());
		} catch (final IllegalArgumentException e) {
			jid.setError(getString(R.string.invalid_jid));
			return;
		}

		if (isBookmarkChecked) {
			if (account.hasBookmarkFor(conferenceJid)) {
				jid.setError(getString(R.string.bookmark_already_exists));
			} else {
				final Bookmark bookmark = new Bookmark(account, conferenceJid.asBareJid());
				bookmark.setAutojoin(getBooleanPreference("autojoin", R.bool.autojoin));
				String nick = conferenceJid.getResource();
				if (nick != null && !nick.isEmpty()) {
					bookmark.setNick(nick);
				}
				account.getBookmarks().add(bookmark);
				xmppConnectionService.pushBookmarks(account);
				final Conversation conversation = xmppConnectionService
						.findOrCreateConversation(account, conferenceJid, true, true, true);
				bookmark.setConversation(conversation);
				dialog.dismiss();
				switchToConversation(conversation);
			}
		} else {
			final Conversation conversation = xmppConnectionService
					.findOrCreateConversation(account, conferenceJid, true, true, true);
			dialog.dismiss();
			switchToConversation(conversation);
		}
	}

	@Override
	public void onConversationUpdate() {
		refreshUi();
	}

	@Override
	public void onRefresh() {
		Log.d(Config.LOGTAG,"user requested to refresh");
		if (QuickConversationsService.isQuicksy() && xmppConnectionService != null) {
			xmppConnectionService.getQuickConversationsService().considerSyncBackground(true);
		}
	}


	private void setRefreshing(boolean refreshing) {
		MyListFragment fragment = (MyListFragment) mListPagerAdapter.getItem(0);
		if (fragment != null) {
			fragment.setRefreshing(refreshing);
		}
	}

	/*
	@Override
	public void onCreatePublicChannel(Account account, String name, Jid address) {
		mToast = Toast.makeText(this, R.string.creating_channel, Toast.LENGTH_LONG);
		mToast.show();
		xmppConnectionService.createPublicChannel(account, name, address, new UiCallback<Conversation>() {
			@Override
			public void success(Conversation conversation) {
				runOnUiThread(() -> {
					hideToast();
					switchToConversation(conversation);
				});

			}

			@Override
			public void error(int errorCode, Conversation conversation) {
				runOnUiThread(() -> {
					replaceToast(getString(errorCode));
					switchToConversation(conversation);
				});
			}

			@Override
			public void userInputRequried(PendingIntent pi, Conversation object) {

			}
		});
	}*/


	public static class MyListFragment extends SwipeRefreshListFragment {
		private AdapterView.OnItemClickListener mOnItemClickListener;
		private int mResContextMenu;

		public void setContextMenu(final int res) {
			this.mResContextMenu = res;
		}

		@Override
		public void onListItemClick(final ListView l, final View v, final int position, final long id) {
			if (mOnItemClickListener != null) {
				mOnItemClickListener.onItemClick(l, v, position, id);
			}
		}

		public void setOnListItemClickListener(AdapterView.OnItemClickListener l) {
			this.mOnItemClickListener = l;
		}

		@Override
		public void onViewCreated(@NonNull final View view, final Bundle savedInstanceState) {
			super.onViewCreated(view, savedInstanceState);
			registerForContextMenu(getListView());
			getListView().setFastScrollEnabled(true);
			getListView().setDivider(null);
			getListView().setDividerHeight(0);
		}

		@Override
		public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
			super.onCreateContextMenu(menu, v, menuInfo);
			final StartConversationActivity activity = (StartConversationActivity) getActivity();
			if (activity == null || mResContextMenu == R.menu.conference_context) {
				return;
			}
			activity.getMenuInflater().inflate(mResContextMenu, menu);
			final AdapterView.AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) menuInfo;
			if (mResContextMenu == R.menu.conference_context) {
				activity.conference_context_id = acmi.position;
			} else if (mResContextMenu == R.menu.contact_context) {
				activity.contact_context_id = acmi.position;
				final Contact contact = (Contact) activity.contacts.get(acmi.position);
//				final MenuItem blockUnblockItem = menu.findItem(R.id.context_contact_block_unblock);
				final MenuItem showContactDetailsItem = menu.findItem(R.id.context_contact_details);
//				final MenuItem deleteContactMenuItem = menu.findItem(R.id.context_delete_contact);
				if (contact.isSelf()) {
					showContactDetailsItem.setVisible(false);
				}
				//deleteContactMenuItem.setVisible(contact.showInRoster() && !contact.getOption(Contact.Options.SYNCED_VIA_OTHER));
				XmppConnection xmpp = contact.getAccount().getXmppConnection();
//				if (xmpp != null && xmpp.getFeatures().blocking() && !contact.isSelf()) {
//					if (contact.isBlocked()) {
//						blockUnblockItem.setTitle(R.string.unblock_contact);
//					} else {
//						blockUnblockItem.setTitle(R.string.block_contact);
//					}
//				} else {
//					blockUnblockItem.setVisible(false);
//				}
			}
		}

		@Override
		public boolean onContextItemSelected(final MenuItem item) {
			StartConversationActivity activity = (StartConversationActivity) getActivity();
			if (activity == null) {
				return true;
			}
			switch (item.getItemId()) {
				case R.id.context_contact_details:
					activity.openDetailsForContact();
					break;
				case R.id.context_join_conference:
					activity.openConversationForBookmark();
					break;
				case R.id.context_delete_conference:
					activity.deleteConference();
			}
			return true;
		}
	}

	public class ListPagerAdapter extends PagerAdapter {
		FragmentManager fragmentManager;
		MyListFragment[] fragments;

		ListPagerAdapter(FragmentManager fm) {
			fragmentManager = fm;
			fragments = new MyListFragment[2];
		}

		public void requestFocus(int pos) {
			if (fragments.length > pos) {
				fragments[pos].getListView().requestFocus();
			}
		}

		@Override
		public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
			FragmentTransaction trans = fragmentManager.beginTransaction();
			trans.remove(fragments[position]);
			trans.commit();
			fragments[position] = null;
		}

		@NonNull
		@Override
		public Fragment instantiateItem(@NonNull ViewGroup container, int position) {
			final Fragment fragment = getItem(position);
			final FragmentTransaction trans = fragmentManager.beginTransaction();
			trans.add(container.getId(), fragment, "fragment:" + position);
			try {
				trans.commit();
			} catch (IllegalStateException e) {
				//ignore
			}
			return fragment;
		}

		@Override
		public int getCount() {
			return fragments.length;
		}

		@Override
		public boolean isViewFromObject(@NonNull View view, @NonNull Object fragment) {
			return ((Fragment) fragment).getView() == view;
		}

		@Nullable
		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
				case 0:
					return getResources().getString(R.string.contacts);
				case 1:
					return getResources().getString(R.string.conferences);
				default:
					return super.getPageTitle(position);
			}
		}

		Fragment getItem(int position) {
			if (fragments[position] == null) {
				final MyListFragment listFragment = new MyListFragment();
				if (position == 1) {
					listFragment.setListAdapter(mConferenceAdapter);
					listFragment.setContextMenu(R.menu.conference_context);
					listFragment.setOnListItemClickListener((arg0, arg1, p, arg3) -> openConversationForBookmark(p));
				} else {

					listFragment.setListAdapter(mContactsAdapter);
					listFragment.setContextMenu(R.menu.contact_context);
					listFragment.setOnListItemClickListener((arg0, arg1, p, arg3) -> openConversationForContact(p));

					if (QuickConversationsService.isQuicksy()) {
						listFragment.setOnRefreshListener(StartConversationActivity.this);
					}
				}
				fragments[position] = listFragment;
			}
			return fragments[position];
		}
	}

	public static void addInviteUri(Intent to, Intent from) {
		if (from != null && from.hasExtra(EXTRA_INVITE_URI)) {
			to.putExtra(EXTRA_INVITE_URI, from.getStringExtra(EXTRA_INVITE_URI));
		}
	}

	private class Invite extends XmppUri {

		public String account;

		public Invite(final Uri uri) {
			super(uri);
		}

		public Invite(final String uri) {
			super(uri);
		}

		public Invite(Uri uri, boolean safeSource) {
			super(uri, safeSource);
		}

		boolean invite() {
			if (!isJidValid()) {
				Toast.makeText(StartConversationActivity.this, R.string.invalid_jid, Toast.LENGTH_SHORT).show();
				return false;
			}
			if (getJid() != null) {
				return handleJid(this);
			}
			return false;
		}
	}


	///// OFFLINE STATUS BAR  TODO move to own class, rather than duplicated over code
	@Override
	public void onNetworkConnectionChanged(boolean isConnected) {
		updateOfflineStatusBar();

	}
	private void checkNetworkStatus() {
		updateOfflineStatusBar();
	}

	private View.OnClickListener mRefreshNetworkClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			TextView networkStatus = findViewById(R.id.network_status);
			networkStatus.setCompoundDrawables(null, null, null, null);
			String previousNetworkState = networkStatus.getText().toString();
			final Account account = xmppConnectionService.getAccounts().get(0);
			if (account != null) {
				// previousNetworkState: ie what string is displayed currently in the offline status bar
				if (previousNetworkState != null) {

				    /*
				     Case 1a. PRESENCE -> OFFLINE ) "_____: tap to Reconnect"
				     -> refresh to "Attempting to Connect"
				     -> presence is offline, need to reenable account
				     -> change presence to online
				      */
					if (previousNetworkState.contains(getResources().getString(R.string.status_tap_to_enable))) {
						networkStatus.setText(getResources().getString(R.string.refreshing_connection));
						if (account.getPresenceStatus().equals(Presence.Status.OFFLINE)){
							enableAccount(account);
						}
						PresenceTemplate template = new PresenceTemplate(Presence.Status.ONLINE, account.getPresenceStatusMessage());
						xmppConnectionService.changeStatus(account, template, null);

					}
					/*
				     Case 1b. PRESENCE) "_____: tap to set to Available"
				     -> refresh to "Changing status to Available"
				     -> if was offline need to reenable account
				     -> change presence to online
				      */
					else if (previousNetworkState.contains(getResources().getString(R.string.status_tap_to_available))) {
						networkStatus.setText(getResources().getString(R.string.refreshing_status));
						changePresence(account);

                     /*
				     Case 2. ACCOUNT) "Disconnected: tap to connect"
				     -> refresh to "Attempting to Connect"
				     -> toggle account connection(ie what used to be manage accounts toggle)
				      */
					} else if (previousNetworkState.contains(getResources().getString(R.string.disconnect_tap_to_connect))) {
						networkStatus.setText(getResources().getString(R.string.refreshing_connection));
						if (!(account.getStatus().equals(Account.State.CONNECTING) || account.getStatus().equals(Account.State.ONLINE))){
							enableAccount(account);
						}
                     /*
				     Case 2. NETWORK) "No internet connection"
				     -> refresh to "Checking for signal"
				     -> ???
				      */
					} else if (previousNetworkState.contains(getResources().getString(R.string.status_no_network))) {
						networkStatus.setText(getResources().getString(R.string.refreshing_network));
						enableAccount(account);
					}
				} else {
					// should not reach here... Offline status message state should be defined in one of the above cases
					networkStatus.setText(getResources().getString(R.string.refreshing_connection));
				}

				updateOfflineStatusBar();
			}

		}
	};

	protected void updateOfflineStatusBar(){
		if (ConnectivityReceiver.isConnected(this)) {
			if (xmppConnectionService != null  && !xmppConnectionService.getAccounts().isEmpty()){
				final Account account = xmppConnectionService.getAccounts().get(0);
				Account.State accountStatus = account.getStatus();
				Presence.Status presenceStatus = account.getPresenceStatus();
				if (presenceStatus.equals(Presence.Status.OFFLINE)){
					runStatus( getResources().getString(R.string.status_tap_to_enable) ,true, true);
					Log.w(Config.LOGTAG ,"updateOfflineStatusBar " + presenceStatus.toDisplayString()+ getResources().getString(R.string.status_tap_to_enable));
				} else if (!presenceStatus.equals(Presence.Status.ONLINE)){
					runStatus( presenceStatus.toDisplayString()+ getResources().getString(R.string.status_tap_to_available) ,true, true);
					Log.w(Config.LOGTAG ,"updateOfflineStatusBar " + presenceStatus.toDisplayString()+ getResources().getString(R.string.status_tap_to_available));
				} else {
					if (accountStatus == Account.State.ONLINE ) {
						runStatus("", false, false);
					} else if (accountStatus == Account.State.CONNECTING) {
						runStatus(getResources().getString(R.string.connecting),true, false);
						Log.w(Config.LOGTAG ,"updateOfflineStatusBar " + getResources().getString(accountStatus.getReadableId()));
						new Handler().postDelayed(new Runnable() {
							@Override
							public void run() {
								updateOfflineStatusBar();
							}
						},1000);
					} else {
						runStatus(getResources().getString(R.string.disconnect_tap_to_connect),true, true);
						Log.w(Config.LOGTAG ,"updateOfflineStatusBar " + getResources().getString(accountStatus.getReadableId()));
					}
				}
			}
		} else {
			runStatus(getResources().getString(R.string.status_no_network), true, true);
			Log.w(Config.LOGTAG ,"updateOfflineStatusBar disconnected from network");

		}
	}
	private void disableAccount(Account account) {
		account.setOption(Account.OPTION_DISABLED, true);
		if (!xmppConnectionService.updateAccount(account)) {
			Toast.makeText(this, R.string.unable_to_update_account, Toast.LENGTH_SHORT).show();
		}
	}

	private void enableAccount(Account account) {
		account.setOption(Account.OPTION_DISABLED, false);
		final XmppConnection connection = account.getXmppConnection();
		if (connection != null) {
			connection.resetEverything();
		}
		if (!xmppConnectionService.updateAccount(account)) {
			Toast.makeText(this, R.string.unable_to_update_account, Toast.LENGTH_SHORT).show();
		}
	}

	private void runStatus(String str, boolean isVisible, boolean withRefresh){
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				reconfigureOfflineText(str, withRefresh);
				if(isVisible){
					offlineLayout.setVisibility(View.VISIBLE);
				} else {
					offlineLayout.setVisibility(View.GONE);
				}
			}
		}, 1000);
	}
	private void reconfigureOfflineText(String str, boolean withRefresh) {
		if (offlineLayout.isShown()) {
			TextView networkStatus = findViewById(R.id.network_status);
			if (networkStatus != null) {
				networkStatus.setText(str);
				if (withRefresh) {
					Drawable refreshIcon =
							ContextCompat.getDrawable(this, R.drawable.ic_refresh_black_24dp);

					networkStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(refreshIcon, null, null, null);
				} else {
					networkStatus.setCompoundDrawables(null, null, null, null);
				}
			}
		}
	}
	protected void changePresence(Account fragAccount) {
		android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
		final DialogPresenceBinding binding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.dialog_presence, null, false);
		String current = fragAccount.getPresenceStatusMessage();
		if (current != null && !current.trim().isEmpty()) {
			binding.statusMessage.append(current);
		}
		setAvailabilityRadioButton(fragAccount.getPresenceStatus(), binding);
		setStatusMessageRadioButton(fragAccount.getPresenceStatusMessage(), binding);
		List<PresenceTemplate> templates = xmppConnectionService.getPresenceTemplates(fragAccount);

		binding.clearPrefs.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				binding.statuses.clearCheck();
				binding.statusMessage.setText("");
			}
		});
		binding.statuses.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
			public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
				switch(checkedId){
					case R.id.in_meeting:
						binding.statusMessage.setText(Presence.StatusMessage.IN_MEETING.toShowString());
						binding.statusMessage.setEnabled(false);
						break;
					case R.id.on_travel:
						binding.statusMessage.setText(Presence.StatusMessage.ON_TRAVEL.toShowString());
						binding.statusMessage.setEnabled(false);
						break;
					case R.id.out_sick:
						binding.statusMessage.setText(Presence.StatusMessage.OUT_SICK.toShowString());
						binding.statusMessage.setEnabled(false);
						break;
					case R.id.vacation:
						binding.statusMessage.setText(Presence.StatusMessage.VACATION.toShowString());
						binding.statusMessage.setEnabled(false);
						break;
					case R.id.custom:
						binding.statusMessage.setEnabled(true);
						break;
					default:
						binding.statusMessage.setEnabled(false);
						break;
				}
			}
		});

		builder.setTitle(R.string.edit_status_message_title);
		builder.setView(binding.getRoot());
		builder.setNegativeButton(R.string.cancel, null);
		builder.setPositiveButton(R.string.confirm, (dialog, which) -> {
			PresenceTemplate template = new PresenceTemplate(getAvailabilityRadioButton(binding), binding.statusMessage.getText().toString().trim());
			xmppConnectionService.changeStatus(fragAccount, template, null);
			if (template.getStatus().equals(Presence.Status.OFFLINE)){
				disableAccount(fragAccount);
			} else {
				if (!template.getStatus().equals(Presence.Status.OFFLINE) && fragAccount.getStatus().equals(Account.State.DISABLED)){
					enableAccount(fragAccount);
				}
			}
			updateOfflineStatusBar();

		});
		builder.create().show();
	}


	/*private void generateSignature(Intent intent, PresenceTemplate template, Account fragAccount) {
		xmppConnectionService.getPgpEngine().generateSignature(intent, fragAccount, template.getStatusMessage(), new UiCallback<String>() {
			@Override
			public void success(String signature) {
				xmppConnectionService.changeStatus(fragAccount, template, signature);
			}

			@Override
			public void error(int errorCode, String object) {

			}

			@Override
			public void userInputRequried(PendingIntent pi, String object) {
				mPendingPresenceTemplate.push(template);
				try {
					startIntentSenderForResult(pi.getIntentSender(), REQUEST_CHANGE_STATUS, null, 0, 0, 0);
				} catch (final IntentSender.SendIntentException ignored) {
				}
			}
		});
	}*/

	private static final int REQUEST_CHANGE_STATUS = 0xee11;
	private final PendingItem<PresenceTemplate> mPendingPresenceTemplate = new PendingItem<>();


	private static void setAvailabilityRadioButton(Presence.Status status, DialogPresenceBinding binding) {
		if (status == null) {
			binding.online.setChecked(true);
			return;
		}
		switch (status) {
			case DND:
				binding.dnd.setChecked(true);
				break;
			case OFFLINE:
				binding.xa.setChecked(true);
				break;
			case XA:
				binding.xa.setChecked(true);
				break;
			case AWAY:
				binding.away.setChecked(true);
				break;
			default:
				binding.online.setChecked(true);
		}
	}

	private static void setStatusMessageRadioButton(String statusMessage, DialogPresenceBinding binding) {
		if (statusMessage == null) {
			binding.statuses.clearCheck();
			binding.statusMessage.setEnabled(false);
			return;
		}
		binding.statuses.clearCheck();
		binding.statusMessage.setEnabled(false);
		if (statusMessage.equals(getEmojiByUnicode(meetingIcon)+"\tIn a meeting")) {
			binding.inMeeting.setChecked(true);
			return;
		} else if (statusMessage.equals(getEmojiByUnicode(travelIcon)+"\tOn travel")) {
			binding.onTravel.setChecked(true);
			return;
		} else if (statusMessage.equals(getEmojiByUnicode(sickIcon)+"\tOut sick")) {
			binding.outSick.setChecked(true);
			return;
		} else if (statusMessage.equals(getEmojiByUnicode(vacationIcon)+"\tVacation")) {
			binding.vacation.setChecked(true);
			return;
		} else if (!statusMessage.isEmpty()) {
			binding.custom.setChecked(true);
			binding.statusMessage.setEnabled(true);
			return;
		} else {
			binding.statuses.clearCheck();
			binding.statusMessage.setEnabled(false);
			return;
		}

	}

	private static Presence.Status getAvailabilityRadioButton(DialogPresenceBinding binding) {
		if (binding.dnd.isChecked()) {
			return Presence.Status.DND;
		} else if (binding.xa.isChecked()) {
			return Presence.Status.OFFLINE;
		} else if (binding.away.isChecked()) {
			return Presence.Status.AWAY;
		} else {
			return Presence.Status.ONLINE;
		}
	}

}
