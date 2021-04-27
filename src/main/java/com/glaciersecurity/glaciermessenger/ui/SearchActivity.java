/*
 * Copyright (c) 2018, Daniel Gultsch All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.glaciersecurity.glaciermessenger.ui;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import androidx.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.IdRes;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.databinding.ActivitySearchBinding;
import com.glaciersecurity.glaciermessenger.databinding.DialogPresenceBinding;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.Contact;
import com.glaciersecurity.glaciermessenger.entities.Conversation;
import com.glaciersecurity.glaciermessenger.entities.Conversational;
import com.glaciersecurity.glaciermessenger.entities.Message;
import com.glaciersecurity.glaciermessenger.entities.Presence;
import com.glaciersecurity.glaciermessenger.entities.PresenceTemplate;
import com.glaciersecurity.glaciermessenger.services.ConnectivityReceiver;
import com.glaciersecurity.glaciermessenger.services.MessageSearchTask;
import com.glaciersecurity.glaciermessenger.ui.adapter.MessageAdapter;
import com.glaciersecurity.glaciermessenger.ui.interfaces.OnSearchResultsAvailable;
import com.glaciersecurity.glaciermessenger.ui.util.ChangeWatcher;
import com.glaciersecurity.glaciermessenger.ui.util.DateSeparator;
import com.glaciersecurity.glaciermessenger.ui.util.ListViewUtils;
import com.glaciersecurity.glaciermessenger.ui.util.PendingItem;
import com.glaciersecurity.glaciermessenger.ui.util.ShareUtil;
import com.glaciersecurity.glaciermessenger.ui.util.StyledAttributes;
import com.glaciersecurity.glaciermessenger.utils.FtsUtils;
import com.glaciersecurity.glaciermessenger.utils.MessageUtils;
import com.glaciersecurity.glaciermessenger.xmpp.XmppConnection;

import static com.glaciersecurity.glaciermessenger.entities.Presence.StatusMessage.meetingIcon;
import static com.glaciersecurity.glaciermessenger.entities.Presence.StatusMessage.sickIcon;
import static com.glaciersecurity.glaciermessenger.entities.Presence.StatusMessage.travelIcon;
import static com.glaciersecurity.glaciermessenger.entities.Presence.StatusMessage.vacationIcon;
import static com.glaciersecurity.glaciermessenger.entities.Presence.getEmojiByUnicode;
import static com.glaciersecurity.glaciermessenger.ui.util.SoftKeyboardUtils.hideSoftKeyboard;
import static com.glaciersecurity.glaciermessenger.ui.util.SoftKeyboardUtils.showKeyboard;

public class SearchActivity extends XmppActivity implements TextWatcher, OnSearchResultsAvailable, MessageAdapter.OnContactPictureClicked, ConnectivityReceiver.ConnectivityReceiverListener {

	private static final String EXTRA_SEARCH_TERM = "search-term";

	private ActivitySearchBinding binding;
	private MessageAdapter messageListAdapter;
	private final List<Message> messages = new ArrayList<>();
	private WeakReference<Message> selectedMessageReference = new WeakReference<>(null);
	private final ChangeWatcher<List<String>> currentSearch = new ChangeWatcher<>();
	private final PendingItem<String> pendingSearchTerm = new PendingItem<>();
	private final PendingItem<List<String>> pendingSearch = new PendingItem<>();

	private ConnectivityReceiver connectivityReceiver;
	private LinearLayout offlineLayout;


	@Override
	public void onCreate(final Bundle bundle) {
		final String searchTerm = bundle == null ? null : bundle.getString(EXTRA_SEARCH_TERM);
		if (searchTerm != null) {
			pendingSearchTerm.push(searchTerm);
		}
		super.onCreate(bundle);
		this.binding = DataBindingUtil.setContentView(this, R.layout.activity_search);
		setSupportActionBar((Toolbar) this.binding.toolbar);
		configureActionBar(getSupportActionBar());
		this.messageListAdapter = new MessageAdapter(this, this.messages);
		this.messageListAdapter.setOnContactPictureClicked(this);
		this.binding.searchResults.setAdapter(messageListAdapter);
		registerForContextMenu(this.binding.searchResults);

        this.offlineLayout = findViewById(R.id.offline_layout);
        this.offlineLayout.setOnClickListener(mRefreshNetworkClickListener);
        connectivityReceiver = new ConnectivityReceiver(this);
		checkNetworkStatus();
		updateOfflineStatusBar();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.activity_search, menu);
		final MenuItem searchActionMenuItem = menu.findItem(R.id.action_search);
		final EditText searchField = searchActionMenuItem.getActionView().findViewById(R.id.search_field);
		final String term = pendingSearchTerm.pop();
		if (term != null) {
			searchField.append(term);
			List<String> searchTerm = FtsUtils.parse(term);
			if (xmppConnectionService != null) {
				if (currentSearch.watch(searchTerm)) {
					xmppConnectionService.search(searchTerm, this);
				}
			} else {
				pendingSearch.push(searchTerm);
			}
		}
		searchField.addTextChangedListener(this);
		searchField.setHint(R.string.search_messages);
		searchField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
		if (term == null) {
			showKeyboard(searchField);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
		final Message message = this.messages.get(acmi.position);
		this.selectedMessageReference = new WeakReference<>(message);
		getMenuInflater().inflate(R.menu.search_result_context, menu);
		MenuItem copy = menu.findItem(R.id.copy_message);
		MenuItem quote = menu.findItem(R.id.quote_message);
		if (message.isGeoUri()) {
			copy.setVisible(false);
			quote.setVisible(false);
		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			hideSoftKeyboard(this);
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final Message message = selectedMessageReference.get();
		if (message != null) {
			switch (item.getItemId()) {
				case R.id.open_conversation:
					switchToConversation(wrap(message.getConversation()));
					break;
				case R.id.share_with:
					ShareUtil.share(this, message);
					break;
				case R.id.copy_message:
					ShareUtil.copyToClipboard(this, message);
					break;
				case R.id.quote_message:
					quote(message);
					break;
			}
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onSaveInstanceState(Bundle bundle) {
		List<String> term = currentSearch.get();
		if (term != null && term.size() > 0) {
			bundle.putString(EXTRA_SEARCH_TERM,FtsUtils.toUserEnteredString(term));
		}
		super.onSaveInstanceState(bundle);
	}

	private void quote(Message message) {
		switchToConversationAndQuote(wrap(message.getConversation()), MessageUtils.prepareQuote(message));
	}

	private Conversation wrap(Conversational conversational) {
		if (conversational instanceof Conversation) {
			return (Conversation) conversational;
		} else {
			return xmppConnectionService.findOrCreateConversation(conversational.getAccount(),
					conversational.getJid(),
					conversational.getMode() == Conversational.MODE_MULTI,
					true,
					true);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}

	@Override
	protected void onStop () {
		unregisterReceiver(connectivityReceiver);
		super.onStop();
	}

	@Override
	protected void refreshUiReal() {
		updateOfflineStatusBar();
	}

	@Override
	void onBackendConnected() {
		final List<String> searchTerm = pendingSearch.pop();
		if (searchTerm != null && currentSearch.watch(searchTerm)) {
			xmppConnectionService.search(searchTerm, this);
		}
		updateOfflineStatusBar();
	}


	private void changeBackground(boolean hasSearch, boolean hasResults) {
		if (hasSearch) {
			if (hasResults) {
				binding.searchResults.setBackgroundColor(StyledAttributes.getColor(this, R.attr.color_background_secondary));
			} else {
				binding.searchResults.setBackground(StyledAttributes.getDrawable(this, R.attr.activity_background_no_results));
			}
		} else {
			binding.searchResults.setBackground(StyledAttributes.getDrawable(this, R.attr.activity_background_search));
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	@Override
	public void afterTextChanged(Editable s) {
		final List<String> term = FtsUtils.parse(s.toString().trim());
		if (!currentSearch.watch(term)) {
			return;
		}
		if (term.size() > 0) {
			xmppConnectionService.search(term, this);
		} else {
			MessageSearchTask.cancelRunningTasks();
			this.messages.clear();
			messageListAdapter.setHighlightedTerm(null);
			messageListAdapter.notifyDataSetChanged();
			changeBackground(false, false);
		}
	}

	@Override
	public void onSearchResultsAvailable(List<String> term, List<Message> messages) {
		runOnUiThread(() -> {
			this.messages.clear();
			messageListAdapter.setHighlightedTerm(term);
			DateSeparator.addAll(messages);
			this.messages.addAll(messages);
			messageListAdapter.notifyDataSetChanged();
			changeBackground(true, messages.size() > 0);
			ListViewUtils.scrollToBottom(this.binding.searchResults);
		});
	}

	@Override
	public void onContactPictureClicked(Message message) {
		String fingerprint;
		if (message.getEncryption() == Message.ENCRYPTION_PGP || message.getEncryption() == Message.ENCRYPTION_DECRYPTED) {
			fingerprint = "pgp";
		} else {
			fingerprint = message.getFingerprint();
		}
		if (message.getStatus() == Message.STATUS_RECEIVED) {
			final Contact contact = message.getContact();
			if (contact != null) {
				if (contact.isSelf()) {
					switchToAccount(message.getConversation().getAccount(), fingerprint);
				} else {
					switchToContactDetails(contact, fingerprint);
				}
			}
		} else {
			switchToAccount(message.getConversation().getAccount(), fingerprint);
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
						//if (account.getPgpId() != 0 && hasPgp()) {
						//	generateSignature(null, template, account);
						//} else {
							xmppConnectionService.changeStatus(account, template, null);
						//}
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
