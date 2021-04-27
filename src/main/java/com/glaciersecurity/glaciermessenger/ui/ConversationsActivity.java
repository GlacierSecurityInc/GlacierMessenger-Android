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


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import androidx.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GetDetailsHandler;
import com.glaciersecurity.glaciermessenger.cognito.BackupAccountManager;
import com.glaciersecurity.glaciermessenger.entities.CognitoAccount;
import com.glaciersecurity.glaciermessenger.entities.TwilioCallParticipant;
import com.glaciersecurity.glaciermessenger.services.CallManager;
import com.glaciersecurity.glaciermessenger.services.NotificationService;
import com.glaciersecurity.glaciermessenger.ui.interfaces.TwilioCallListener;
import com.glaciersecurity.glaciermessenger.utils.Compatibility;
import com.google.android.material.navigation.NavigationView;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.service.notification.NotificationListenerService;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

//import org.openintents.openpgp.util.OpenPgpApi;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.cognito.AppHelper;
import com.glaciersecurity.glaciermessenger.cognito.Util;
import com.glaciersecurity.glaciermessenger.crypto.OmemoSetting;
import com.glaciersecurity.glaciermessenger.crypto.axolotl.AxolotlService;
import com.glaciersecurity.glaciermessenger.databinding.ActivityConversationsBinding;
import com.glaciersecurity.glaciermessenger.databinding.DialogPresenceBinding;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.Conversation;
import com.glaciersecurity.glaciermessenger.entities.Presence;
import com.glaciersecurity.glaciermessenger.entities.PresenceTemplate;
import com.glaciersecurity.glaciermessenger.persistance.FileBackend;
import com.glaciersecurity.glaciermessenger.services.ConnectivityReceiver;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;
import com.glaciersecurity.glaciermessenger.ui.interfaces.OnBackendConnected;
import com.glaciersecurity.glaciermessenger.ui.interfaces.OnConversationArchived;
import com.glaciersecurity.glaciermessenger.ui.interfaces.OnConversationRead;
import com.glaciersecurity.glaciermessenger.ui.interfaces.OnConversationSelected;
import com.glaciersecurity.glaciermessenger.ui.interfaces.OnConversationsListItemUpdated;
//import com.glaciersecurity.glaciermessenger.ui.service.EmojiService;
import com.glaciersecurity.glaciermessenger.ui.util.ActivityResult;
import com.glaciersecurity.glaciermessenger.ui.util.ConversationMenuConfigurator;
import com.glaciersecurity.glaciermessenger.ui.util.MenuDoubleTabUtil;
import com.glaciersecurity.glaciermessenger.ui.util.PendingItem;
import com.glaciersecurity.glaciermessenger.utils.EmojiWrapper;
import com.glaciersecurity.glaciermessenger.utils.ExceptionHelper;
import com.glaciersecurity.glaciermessenger.utils.LogoutListener;
import com.glaciersecurity.glaciermessenger.utils.SignupUtils;
import com.glaciersecurity.glaciermessenger.utils.XmppUri;
import com.glaciersecurity.glaciermessenger.xmpp.OnUpdateBlocklist;
import com.glaciersecurity.glaciermessenger.xmpp.OnKeyStatusUpdated;
import com.glaciersecurity.glaciermessenger.xmpp.XmppConnection;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.RemoteParticipant;
import com.twilio.video.Room;

import rocks.xmpp.addr.Jid;

import static com.glaciersecurity.glaciermessenger.entities.Presence.StatusMessage.customIcon;
import static com.glaciersecurity.glaciermessenger.entities.Presence.StatusMessage.meetingIcon;
import static com.glaciersecurity.glaciermessenger.entities.Presence.StatusMessage.sickIcon;
import static com.glaciersecurity.glaciermessenger.entities.Presence.StatusMessage.travelIcon;
import static com.glaciersecurity.glaciermessenger.entities.Presence.StatusMessage.vacationIcon;
import static com.glaciersecurity.glaciermessenger.entities.Presence.getEmojiByUnicode;
import static com.glaciersecurity.glaciermessenger.ui.ConversationFragment.REQUEST_DECRYPT_PGP;

public class ConversationsActivity extends XmppActivity implements OnConversationSelected, OnConversationArchived, OnConversationsListItemUpdated, OnConversationRead, XmppConnectionService.OnAccountUpdate, XmppConnectionService.OnConversationUpdate, XmppConnectionService.OnRosterUpdate, OnUpdateBlocklist, XmppConnectionService.OnShowErrorToast, XmppConnectionService.OnAffiliationChanged, OnKeyStatusUpdated, LogoutListener, ConnectivityReceiver.ConnectivityReceiverListener, TwilioCallListener{

	public static final String ACTION_VIEW_CONVERSATION = "com.glaciersecurity.glaciermessenger.action.VIEW";
	public static final String EXTRA_CONVERSATION = "conversationUuid";
	public static final String EXTRA_DOWNLOAD_UUID = "com.glaciersecurity.glaciermessenger.download_uuid";
	public static final String EXTRA_AS_QUOTE = "as_quote";
	public static final String EXTRA_NICK = "nick";
	public static final String EXTRA_IS_PRIVATE_MESSAGE = "pm";
	public static final String EXTRA_DO_NOT_APPEND = "do_not_append";

	private ConversationFragment conversationFragment;

	private static List<String> VIEW_AND_SHARE_ACTIONS = Arrays.asList(
			ACTION_VIEW_CONVERSATION,
			Intent.ACTION_SEND,
			Intent.ACTION_SEND_MULTIPLE
	);

	public static final int REQUEST_OPEN_MESSAGE = 0x9876;
	public static final int REQUEST_PLAY_PAUSE = 0x5432;
	private static final int REQUEST_CHANGE_STATUS = 0xee11;

	private String mSavedInstanceAccount;

	private ImageView avatar;
	private Account mAccount;
	private final PendingItem<PresenceTemplate> mPendingPresenceTemplate = new PendingItem<>();
	private CallManager mCallManager;



	//secondary fragment (when holding the conversation, must be initialized before refreshing the overview fragment
	private static final @IdRes
	int[] FRAGMENT_ID_NOTIFICATION_ORDER = {R.id.secondary_fragment, R.id.main_fragment};
	private final PendingItem<Intent> pendingViewIntent = new PendingItem<>();
	private final PendingItem<ActivityResult> postponedActivityResult = new PendingItem<>();
	private ActivityConversationsBinding binding;
	private boolean mActivityPaused = true;
	private AtomicBoolean mRedirectInProcess = new AtomicBoolean(false);

	private boolean initialConnect = true;
	private ConnectivityReceiver connectivityReceiver;
	protected LinearLayout offlineLayout;
	private Toolbar returnToCall;

	public ConversationsActivity() {
	}

	private static boolean isViewOrShareIntent(Intent i) {
		Log.d(Config.LOGTAG, "action: " + (i == null ? null : i.getAction()));
		return i != null && VIEW_AND_SHARE_ACTIONS.contains(i.getAction()) && i.hasExtra(EXTRA_CONVERSATION);
	}

	private static Intent createLauncherIntent(Context context) {
		final Intent intent = new Intent(context, ConversationsActivity.class);
		intent.setAction(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		return intent;
	}

	@Override
	protected void refreshUiReal() {
		for (@IdRes int id : FRAGMENT_ID_NOTIFICATION_ORDER) {
			refreshFragment(id);
		}
		updateOfflineStatusBar();
	}

	@Override
	void onBackendConnected() {
		if (performRedirectIfNecessary(true)) {
			return;
		}
		if (mSavedInstanceAccount != null) {
			try {
				this.mAccount = xmppConnectionService.findAccountByJid(Jid.of(mSavedInstanceAccount));
			} catch (IllegalArgumentException e) {
				this.mAccount = null;
			}
		}

		updateOfflineStatusBar();
		xmppConnectionService.getNotificationService().setIsInForeground(true);
		Intent intent = pendingViewIntent.pop();
		if (intent != null) {
			if (processViewIntent(intent)) {
				if (binding.secondaryFragment != null) {
					notifyFragmentOfBackendConnected(R.id.main_fragment);
				}
				invalidateActionBarTitle();
				return;
			}
		}
		for (@IdRes int id : FRAGMENT_ID_NOTIFICATION_ORDER) {
			notifyFragmentOfBackendConnected(id);
		}

		ActivityResult activityResult = postponedActivityResult.pop();
		if (activityResult != null) {
			handleActivityResult(activityResult);
		}

		invalidateActionBarTitle();

		if (xmppConnectionService != null){
			mCallManager = xmppConnectionService.getCallManager();
			if (mCallManager != null && mCallManager.isOnCall()){
				returnToCall.setVisibility(View.VISIBLE);
				mCallManager.setCallListener(this);
			} else {
				returnToCall.setVisibility(View.GONE);
			}

		}
		if (binding.secondaryFragment != null && ConversationFragment.getConversation(this) == null) {
			Conversation conversation = ConversationsOverviewFragment.getSuggestion(this);
			if (conversation != null) {
				openConversation(conversation, null);
			}
		}
		showDialogsIfMainIsOverview();

		if (initialConnect) {
			initialConnect = false;
			for (Account account : xmppConnectionService.getAccounts()) {
				if (account.getStatus() != Account.State.DISABLED) {
					if (account.getXmppConnection() != null) {
						account.getXmppConnection().sendRoomDiscoveries();
					}
				}
			}
		}

	}

	private boolean performRedirectIfNecessary(boolean noAnimation) {
		return performRedirectIfNecessary(null, noAnimation);
	}

	private boolean performRedirectIfNecessary(final Conversation ignore, final boolean noAnimation) {
		if (xmppConnectionService == null) {
			return false;
		}

		boolean isConversationsListEmpty = xmppConnectionService.isConversationsListEmpty(ignore);
		if (isConversationsListEmpty && !xmppConnectionService.getAccounts().isEmpty()){
			return false;
		}
		if (isConversationsListEmpty && mRedirectInProcess.compareAndSet(false, true)) {
			final Intent intent = SignupUtils.getRedirectionIntent(this);
			if (noAnimation) {
				intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			}
			runOnUiThread(() -> {
				startActivity(intent);
				if (noAnimation) {
					overridePendingTransition(0, 0);
				}
			});
		}
		return mRedirectInProcess.get();
	}

	private void showDialogsIfMainIsOverview() {
		if (xmppConnectionService == null) {
			return;
		}
		final Fragment fragment = getFragmentManager().findFragmentById(R.id.main_fragment);
		if (fragment instanceof ConversationsOverviewFragment) {
			if (ExceptionHelper.checkForCrash(this)) {
				return;
			}
			openBatteryOptimizationDialogIfNeeded();
		}
	}

	private String getBatteryOptimizationPreferenceKey() {
		@SuppressLint("HardwareIds") String device = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
		return "show_battery_optimization" + (device == null ? "" : device);
	}

	private void setNeverAskForBatteryOptimizationsAgain() {
		getPreferences().edit().putBoolean(getBatteryOptimizationPreferenceKey(), false).apply();
	}

	private void openBatteryOptimizationDialogIfNeeded() {
		if (isOptimizingBattery()
				&& getPreferences().getBoolean(getBatteryOptimizationPreferenceKey(), true)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.battery_optimizations_enabled);
			builder.setMessage(R.string.battery_optimizations_enabled_dialog);
			builder.setPositiveButton(R.string.next, (dialog, which) -> {
				Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
				Uri uri = Uri.parse("package:" + getPackageName());
				intent.setData(uri);
				try {
					startActivityForResult(intent, REQUEST_BATTERY_OP);
				} catch (ActivityNotFoundException e) {
					Toast.makeText(this, R.string.device_does_not_support_battery_op, Toast.LENGTH_SHORT).show();
				}
			});
			builder.setOnDismissListener(dialog -> setNeverAskForBatteryOptimizationsAgain());
			final AlertDialog dialog = builder.create();
			dialog.setCanceledOnTouchOutside(false);
			dialog.show();
		}
	}

	private boolean hasAccountWithoutPush() {
		for (Account account : xmppConnectionService.getAccounts()) {
			if (account.getStatus() == Account.State.ONLINE && !xmppConnectionService.getPushManagementService().available(account)) {
				return true;
			}
		}
		return false;
	}

	private void notifyFragmentOfBackendConnected(@IdRes int id) {
		final Fragment fragment = getFragmentManager().findFragmentById(id);
		if (fragment instanceof OnBackendConnected) {
			((OnBackendConnected) fragment).onBackendConnected();
		}
	}


	private void refreshFragment(@IdRes int id) {
		final Fragment fragment = getFragmentManager().findFragmentById(id);
		if (fragment instanceof XmppFragment) {
			((XmppFragment) fragment).refresh();
		}

		try {
			updateOfflineStatusBar();
		} catch (Exception e ){
			logOut();
			getCognitoInfo();
			signInUser();
		}


	}
	public void logOut() {
		// logout of Cognito
		cognitoCurrentUserSignout();

		// clear s3bucket client
		Util.clearS3Client(this);
	}

	private void cognitoCurrentUserSignout() {
		// logout of Cognito
		// sometimes if it's been too long, I believe pool doesn't
		// exists and user is no longer logged in
		CognitoUserPool userPool = AppHelper.getPool();
		if (userPool != null) {
			CognitoUser user = userPool.getCurrentUser();
			if (user != null) {
				user.signOut();
			}
		}
	}
	private final String REPLACEMENT_ORG_ID = "<org_id>";
	String username = null;
	String password = null;
	String organization = null;
	/**
	 * Retrieve Cognito account information from file
	 */
	private void getCognitoInfo() {
		// get account from database if exists
		if (xmppConnectionService != null) {
			for (Account account : xmppConnectionService.getAccounts()) {

				CognitoAccount cacct = xmppConnectionService.databaseBackend.getCognitoAccount(account,getApplicationContext());
				if (cacct != null) {
					username = cacct.getUserName();
					password = cacct.getPassword();
					return;
				}
			}
		}
	}

	/**
	 * Sign into Cognito
	 */
	private void signInUser() {
		AppHelper.init(getApplicationContext());
		AppHelper.setUser(username);
		AppHelper.getPool().getUser(username).getSessionInBackground(authenticationHandler);
	}

	AuthenticationHandler authenticationHandler = new AuthenticationHandler() {
		@Override
		public void onSuccess(CognitoUserSession cognitoUserSession, CognitoDevice device) {
			com.glaciersecurity.glaciermessenger.utils.Log.d("Glacier", " -- Auth Success");
			AppHelper.setCurrSession(cognitoUserSession);
			AppHelper.newDevice(device);
		}

		@Override
		public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String username) {
			Locale.setDefault(Locale.US);
			getUserAuthentication(authenticationContinuation, username);
		}

		private void getUserAuthentication(AuthenticationContinuation continuation, String username) {
			if(username != null) {
				username = username;
				AppHelper.setUser(username);
			}
			AuthenticationDetails authenticationDetails = new AuthenticationDetails(username, password, null);
			continuation.setAuthenticationDetails(authenticationDetails);
			continuation.continueTask();
		}

		@Override
		public void getMFACode(MultiFactorAuthenticationContinuation multiFactorAuthenticationContinuation) {

		}

		@Override
		public void onFailure(Exception e) {

		}

		@Override
		public void authenticationChallenge(ChallengeContinuation continuation) {
			/**
			 * For Custom authentication challenge, implement your logic to present challenge to the
			 * user and pass the user's responses to the continuation.
			 */
		}
	};

	private boolean processViewIntent(Intent intent) {
		String uuid = intent.getStringExtra(EXTRA_CONVERSATION);
		Conversation conversation = uuid != null ? xmppConnectionService.findConversationByUuid(uuid) : null;
		if (conversation == null) {
			Log.d(Config.LOGTAG, "unable to view conversation with uuid:" + uuid);
			return false;
		}
		openConversation(conversation, intent.getExtras());
		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
		UriHandlerActivity.onRequestPermissionResult(this, requestCode, grantResults);
		if (grantResults.length > 0) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				switch (requestCode) {
					case REQUEST_OPEN_MESSAGE:
						refreshUiReal();
						ConversationFragment.openPendingMessage(this);
						break;
					case REQUEST_PLAY_PAUSE:
						ConversationFragment.startStopPending(this);
						break;
				}
			}
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		ActivityResult activityResult = ActivityResult.of(requestCode, resultCode, data);
		if (xmppConnectionService != null) {
			handleActivityResult(activityResult);
		} else {
			this.postponedActivityResult.push(activityResult);
		}
	}

	private void handleActivityResult(ActivityResult activityResult) {
		if (activityResult.resultCode == Activity.RESULT_OK) {
			handlePositiveActivityResult(activityResult.requestCode, activityResult.data);
		} else {
			handleNegativeActivityResult(activityResult.requestCode);
		}
	}

	private void handleNegativeActivityResult(int requestCode) {
		Conversation conversation = ConversationFragment.getConversationReliable(this);
		switch (requestCode) {
			case REQUEST_DECRYPT_PGP:
				if (conversation == null) {
					break;
				}
				//conversation.getAccount().getPgpDecryptionService().giveUpCurrentDecryption();
				break;
			case REQUEST_BATTERY_OP:
				setNeverAskForBatteryOptimizationsAgain();
				break;
			case REQUEST_CHANGE_STATUS: {
				com.glaciersecurity.glaciermessenger.utils.Log.d(Config.LOGTAG, "pgp result not ok");
				break;
			}

		}
	}


	private void handlePositiveActivityResult(int requestCode, final Intent data) {
		Conversation conversation = ConversationFragment.getConversationReliable(this);
		if (conversation == null) {
			Log.d(Config.LOGTAG, "conversation not found");
			return;
		}
		switch (requestCode) {
			case REQUEST_DECRYPT_PGP:
				//conversation.getAccount().getPgpDecryptionService().continueDecryption(data);
				break;
			case REQUEST_CHOOSE_PGP_ID:
				/*long id = data.getLongExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, 0);
				if (id != 0) {
					conversation.getAccount().setPgpSignId(id);
					announcePgp(conversation.getAccount(), null, null, onOpenPGPKeyPublished);
				} else {
					choosePgpSignId(conversation.getAccount());
				}*/
				break;
			case REQUEST_ANNOUNCE_PGP:
				//announcePgp(conversation.getAccount(), conversation, data, onOpenPGPKeyPublished);
				break;
			case REQUEST_CHANGE_STATUS: {
				PresenceTemplate template = mPendingPresenceTemplate.pop();
				if (template != null) {
					//generateSignature(data, template);
				}
				break;
			}
		}
	}

	ActionBar actionBar;
	Toolbar toolbar;
	ImageButton statusIcon;
	ImageButton downArrow;
	DrawerLayout drawer;
	NavigationView nav_view;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ConversationMenuConfigurator.reloadFeatures(this);
		OmemoSetting.load(this);
		this.binding = DataBindingUtil.setContentView(this, R.layout.activity_conversations);
		setSupportActionBar((Toolbar) binding.toolbarWithIconStatus);
		configureActionBar(getSupportActionBar());
		this.getFragmentManager().addOnBackStackChangedListener(this::invalidateActionBarTitle);
		this.getFragmentManager().addOnBackStackChangedListener(this::showDialogsIfMainIsOverview);
		this.initializeFragments();
		this.invalidateActionBarTitle();
		final Intent intent;
		if (savedInstanceState == null) {
			intent = getIntent();
		} else {
			intent = savedInstanceState.getParcelable("intent");
			this.mSavedInstanceAccount = savedInstanceState.getString("account");

		}
		if (isViewOrShareIntent(intent)) {
			pendingViewIntent.push(intent);
			setIntent(createLauncherIntent(this));
		}

		setContentView(R.layout.activity_menu_drawer_conversations);
		initToolbar();
		initNavigationMenu();

		this.offlineLayout = findViewById(R.id.offline_layout);

		this.offlineLayout.setOnClickListener(mRefreshNetworkClickListener);
		connectivityReceiver = new ConnectivityReceiver(this);
		checkNetworkStatus();
		updateOfflineStatusBar();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		returnToCall = findViewById(R.id.toolbar_in_call);
		returnToCall.setOnClickListener(returnToCall());

	}

	private View.OnClickListener returnToCall() {
		return v -> {
			if (mCallManager != null) {
				mCallManager.setCallListener(null);
			}

			Intent videoActivity = new Intent(getApplicationContext(), VideoActivity.class);
			videoActivity.putExtra("returning", "true");
			startActivity(videoActivity);

		};
	}



	private void initToolbar() {
		toolbar = (Toolbar) findViewById(R.id.toolbar_with_icon_status);
		setSupportActionBar(toolbar);
		statusIcon = (ImageButton) findViewById(R.id.contact_status_icon);
		downArrow = (ImageButton) findViewById(R.id.down_arrow);
		statusIcon.setVisibility(View.GONE);
		downArrow.setVisibility(View.GONE);
		actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setHomeButtonEnabled(true);

	}

	private void initNavigationMenu() {
		nav_view = (NavigationView) findViewById(R.id.nav_view);
		drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
			public void onDrawerOpened(View drawerView) {
				List<Account> accounts = xmppConnectionService.getAccounts();
				if (!accounts.isEmpty()) {
					mAccount = accounts.get(0);
					avatar = (ImageView) findViewById(R.id.nav_avatar);
					avatar.setOnClickListener(mAvatarClickListener);

					avatar.setImageBitmap(avatarService().get(mAccount, (int) getResources().getDimension(R.dimen.avatar_on_details_screen_size)));
					Button name = findViewById(R.id.nav_name);
					name.setOnClickListener(mAvatarClickListener);
					name.setText(getDisplayName());

					Button status_text = (Button) findViewById(R.id.nav_status_text);
					ImageView status_icon = (ImageView) findViewById(R.id.nav_status_icon);
					status_text.setOnClickListener(mPresenceClickListener);

					if (ConnectivityReceiver.isConnected(getApplicationContext())) {

						Presence.Status status;

						if (dndOnSilentMode() && isPhoneSilenced()) {
							status =  Presence.Status.DND;
						} else if (awayWhenScreenOff() && !isInteractive()) {
							status =  Presence.Status.AWAY;
						} else if (manuallyChangePresence()) {
							status = mAccount.getPresenceStatus();
						} else {
							status =  Presence.Status.ONLINE;
						}

						status_text.setText(status.toDisplayString());
						status_icon.setImageResource(status.getStatusIcon());
						//status_text.setText(mAccount.getPresenceStatus().toDisplayString());
						//status_icon.setImageResource(mAccount.getPresenceStatus().getStatusIcon());
					} else {
						status_text.setText(Presence.Status.OFFLINE.toDisplayString());
						status_icon.setImageResource(Presence.Status.OFFLINE.getStatusIcon());
					}
					Button status_message = (Button) findViewById(R.id.nav_status_message);
					status_message.setOnClickListener(mPresenceClickListener);
					String presenceStatusMessage = mAccount.getPresenceStatusMessage();
					if (presenceStatusMessage != null) {
						//status_message.setText(presenceStatusMessage);
						if (isDefaultStatus(presenceStatusMessage)){
								status_message.setText(presenceStatusMessage);
							} else {
								String customStr = getEmojiByUnicode(customIcon)+"\t" + presenceStatusMessage ;
								status_message.setText(customStr);
							}
					}
				}
				super.onDrawerOpened(drawerView);
			}
			public void onDrawerClosed(View drawerView) {
				super.onDrawerClosed(drawerView);
				updateOfflineStatusBar();
			}

			private final View.OnClickListener mAvatarClickListener = new View.OnClickListener() {
				@Override
				public void onClick(final View view) {
					if (mAccount != null && mAccount.getStatus() == Account.State.ONLINE) {
						Intent intent = new Intent(getApplicationContext(), EditAccountActivity.class);
						intent.putExtra("jid", mAccount.getJid().asBareJid().toString());
						startActivity(intent);

						drawer.closeDrawer(GravityCompat.START);
					}
				}
			};
			private final View.OnClickListener mPresenceClickListener = new View.OnClickListener() {
				@Override
				public void onClick(final View view) {
					if (mAccount != null) {
						changePresence(mAccount);
					}
					drawer.closeDrawer(GravityCompat.START);
				}
			};
		};
		drawer.setDrawerListener(toggle);
		toggle.syncState();
		nav_view.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(final MenuItem item) {
				onItemNavigationClicked(item);
				return true;
			}
		});
	}

	private boolean isDefaultStatus(String statusMessage){
		if (statusMessage.equals(getEmojiByUnicode(meetingIcon)+"\tIn a meeting")) {
				return true;
			} else if (statusMessage.equals(getEmojiByUnicode(travelIcon)+"\tOn travel")) {
				return true;
			} else if (statusMessage.equals(getEmojiByUnicode(sickIcon)+"\tOut sick")) {
				return true;
			} else if (statusMessage.equals(getEmojiByUnicode(vacationIcon)+"\tVacation")) {
				return true;
			} else if (statusMessage.isEmpty()) {
				return true;
			}
		return false;
	}

	private boolean dndOnSilentMode() {
		return getBooleanPreference(SettingsActivity.DND_ON_SILENT_MODE, R.bool.dnd_on_silent_mode);
	}

	private boolean treatVibrateAsSilent() {
		return getBooleanPreference(SettingsActivity.TREAT_VIBRATE_AS_SILENT, R.bool.treat_vibrate_as_silent);
	}
	private boolean awayWhenScreenOff() {
		return getBooleanPreference(SettingsActivity.AWAY_WHEN_SCREEN_IS_OFF, R.bool.away_when_screen_off);
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public boolean isInteractive() {
		try {
			final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			return pm.isInteractive();
		} catch (RuntimeException e) {
			return false;
		}
	}
	private boolean isPhoneSilenced() {
		final boolean notificationDnd;
		final NotificationManager notificationManager = getSystemService(NotificationManager.class);
		final int filter = notificationManager == null ? NotificationManager.INTERRUPTION_FILTER_UNKNOWN : notificationManager.getCurrentInterruptionFilter();
		notificationDnd = filter >= NotificationManager.INTERRUPTION_FILTER_PRIORITY;
		final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		final int ringerMode = audioManager == null ? AudioManager.RINGER_MODE_NORMAL : audioManager.getRingerMode();
		try {
			if (treatVibrateAsSilent()) {
				return notificationDnd || ringerMode != AudioManager.RINGER_MODE_NORMAL;
			} else {
				return notificationDnd || ringerMode == AudioManager.RINGER_MODE_SILENT;
			}
		} catch (Throwable throwable) {
			Log.d(Config.LOGTAG, "platform bug in isPhoneSilenced (" + throwable.getMessage() + ")");
			return notificationDnd;
		}
	}

	@Override
	public void onBackPressed() {
		if (this.drawer.isDrawerOpen(GravityCompat.START)) {
			this.drawer.closeDrawer(GravityCompat.START);
		} else {
			super.onBackPressed();
		}
	}

	protected void changePresence(Account fragAccount) {
		android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
		final DialogPresenceBinding binding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.dialog_presence, null, false);
		if (mAccount == null){
			mAccount = fragAccount;
		}
		String current = mAccount.getPresenceStatusMessage();
		if (current != null && !current.trim().isEmpty()) {
			binding.statusMessage.append(current);
		}
		setAvailabilityRadioButton(mAccount.getPresenceStatus(), binding);
		setStatusMessageRadioButton(mAccount.getPresenceStatusMessage(), binding);
		List<PresenceTemplate> templates = xmppConnectionService.getPresenceTemplates(mAccount);

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
			xmppConnectionService.changeStatus(mAccount, template, null);
			if (template.getStatus().equals(Presence.Status.OFFLINE)){
				disableAccount(mAccount);
			} else {
				if (!template.getStatus().equals(Presence.Status.OFFLINE) && mAccount.getStatus().equals(Account.State.DISABLED)){
					enableAccount(mAccount);
				}
			}
			updateOfflineStatusBar();
			updateStatusIcon();

		});
		builder.create().show();
	}


	/*private void generateSignature(Intent intent, PresenceTemplate template) {
		xmppConnectionService.getPgpEngine().generateSignature(intent, mAccount, template.getStatusMessage(), new UiCallback<String>() {
			@Override
			public void success(String signature) {
				xmppConnectionService.changeStatus(mAccount, template, signature);
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

	protected void show1v1ChatToolbar(Conversation conversation, ConversationFragment mainFrag){
		Presence.Status s =conversation.getContact().getShownStatus();
		statusIcon.setImageResource(s.getStatusIconMenu());
		statusIcon.setVisibility(View.VISIBLE);
		downArrow.setVisibility(View.VISIBLE);
		toolbar = (Toolbar) findViewById(R.id.toolbar_with_icon_status);
		if (mainFrag != null) {
			toolbar.setOnClickListener(mainFrag.clickToContactDetails);
		}
		toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_black_24dp));
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
	}

	protected void showGroupToolbar(){
		statusIcon.setVisibility(View.GONE);
		downArrow.setVisibility(View.VISIBLE);
		toolbar = (Toolbar) findViewById(R.id.toolbar_with_icon_status);
		if (conversationFragment != null) {
			toolbar.setOnClickListener(conversationFragment.clickToContactDetails);
		}
		toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_black_24dp));
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
	}

	protected void hideStatusToolbar(){
		if(statusIcon != null) {
			statusIcon.setVisibility(View.GONE);
		}
		if (downArrow != null) {
			downArrow.setVisibility(View.GONE);
		}
	}

	protected void showNavMenu(){
		toolbar = (Toolbar) findViewById(R.id.toolbar_with_icon_status);
		toolbar.setOnClickListener(null);
		toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_menu_black_24dp));
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (drawer.isDrawerOpen(GravityCompat.START)) {
					drawer.closeDrawer(GravityCompat.START);
				} else {
					drawer.openDrawer(GravityCompat.START);
				}
			}
		});
	}

	private String getDisplayName(){
		if(mAccount != null){
			String disname = null;
			disname = mAccount.getDisplayName();
			if (disname == null) {
				disname = mAccount.getUsername();
			}
			return disname;
		}
		return "";
	}

	private void onItemNavigationClicked(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.Core: {
				Intent coreActivity = new Intent(getApplicationContext(), OpenVPNActivity.class);
				startActivity(coreActivity);
				break;
			}
			case R.id.Chats: {
				FragmentManager fm = getFragmentManager();
				if (fm.getBackStackEntryCount() > 0) {
					try {
						fm.popBackStack();
					} catch (IllegalStateException e) {
						Log.w(Config.LOGTAG,"Unable to pop back stack after pressing home button");
					}
				}
				Intent chatsActivity = new Intent(getApplicationContext(), ConversationsActivity.class);
				startActivity(chatsActivity);
				break;
			}
			case R.id.Contacts: {
				Intent contactsActivity = new Intent(getApplicationContext(), StartConversationActivity.class);
				startActivity(contactsActivity);
				break;
			}

//			case R.id.Dial: {
//				Intent contactsActivity = new Intent(getApplicationContext(), DialPadActivity.class);
//				startActivity(contactsActivity);
//				break;
//			}
			case R.id.File_safe: {
				startActivity(new Intent(this, FileSafeActivity.class));
				break;
			}
			case R.id.Search: {
				Intent coreActivity = new Intent(getApplicationContext(), SearchActivity.class);
				startActivity(coreActivity);
				break;
			}
			case R.id.Settings: {
				Intent settingsActivity = new Intent(getApplicationContext(), SettingsActivity.class);
				startActivity(settingsActivity);
				break;
			}
			case R.id.Support: {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("https://glaciersecurity.zendesk.com"));
				startActivity(intent);
				break;
			}
			case R.id.VoiceApp: {
				try {
					PackageManager pm = getPackageManager();
					Intent intent = pm.getLaunchIntentForPackage("com.glaciersecurity.glaciervoice");
					startActivity(intent);
				} catch (java.lang.NullPointerException e) {
					// If not installed, then download Voice from Play store
					try {
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse(
								"https://play.google.com/store/apps/details?id=com.glaciersecurity.glaciervoice"));
						intent.setPackage("com.android.vending");
						startActivity(intent);
					} catch (java.lang.Exception ex) {
						for (int i = 0; i < nav_view.getMenu().size(); i++) {
							nav_view.getMenu().getItem(i).setChecked(false);
						}
						displayToast("Your device cannot access the Play Store. To use Glacier Dial, install the apk directly.");

						FragmentManager fm2 = getFragmentManager();
						if (fm2.getBackStackEntryCount() > 0) {
							try {
								fm2.popBackStack();
							} catch (IllegalStateException exc) {
								Log.w(Config.LOGTAG,"Unable to pop back stack after pressing home button");
							}
						}
						Intent chatsActivity2 = new Intent(getApplicationContext(), ConversationsActivity.class);
						startActivity(chatsActivity2);
						break;

					}
				}
				break;
			}
			case R.id.Logout: {
				showLogoutConfirmationDialog();
				break;
			}
			default:
				Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_SHORT).show();
				break;
		}
		drawer.closeDrawers();
	}

	/**
	 * Display Logout confirmation
	 */
	private void showLogoutConfirmationDialog() {
		new android.app.AlertDialog.Builder(this)
				.setTitle("Logout Confirmation")
				.setMessage(getString(R.string.account_logout_confirmation))
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(R.string.logout_button_key, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						doLogout();
					}})
				.setNegativeButton(android.R.string.no, null).show();
	}

	private void doLogout() {
		LogoutListener activity = (LogoutListener) this;
		activity.onLogout();
	}

	@Override
	public void onLogout() {
		getPreferences().edit().putBoolean("use_core_connect", false).apply();

		// clear all conversations
		List<Conversation> conversations = xmppConnectionService.getConversations();

		for (int i = (conversations.size() - 1); i >= 0; i--) {
			xmppConnectionService.clearConversationHistory(conversations.get(i));
			// endConversation(conversations.get(i), false, true);
		}

		// wipe all accounts
		List<Account> accounts = xmppConnectionService.getAccounts();

		for (Account account : accounts) {
			xmppConnectionService.deleteAccount(account);
		}

		BackupAccountManager backupAccountManager = new BackupAccountManager(getApplicationContext());
		backupAccountManager.deleteAccountFiles();

		// logout of Cognito
		// sometimes if it's been too long, I believe pool doesn't
		// exists and user is no longer logged in
		CognitoUserPool userPool = AppHelper.getPool();
		if (userPool != null) {
			CognitoUser user = userPool.getCurrentUser();
			if (user != null) {
				user.signOut();
			}
		}

		// clear s3bucket client
		Util.clearS3Client(getApplicationContext());

		// clear all stored content
		clearCachedFiles();

		// login screen
		Intent editAccount = new Intent(this, EditAccountActivity.class);
		startActivity(editAccount);
		Intent stepperWizard = new Intent(this, StepperWizard.class);
		startActivity(stepperWizard);
	}

	/**
	 * Clear images, files from directory
	 */
	private void clearCachedFiles() {
		// clear images, etc
		clearLocalFiles();

		// clear images, etc
		clearPictures();

		// clear voice recordings from plugin
		clearVoiceRecordings();

		// clear shared location
		clearSharedLocations();

		// clear internal storage
		clearExternalStorage();

		clearAppCache();
	}

	private void clearAppCache() {
		try {
			File dir = getApplicationContext().getCacheDir();
			deleteDir(dir);
		} catch (Exception e) { e.printStackTrace();}
	}

	public static boolean deleteDir(File dir) {
		if (dir != null && dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
			return dir.delete();
		} else if(dir!= null && dir.isFile()) {
			return dir.delete();
		} else {
			return false;
		}
	}

	/**
	 * Clear local files for Messenger
	 */
	private void clearLocalFiles() {
		// Retrieve directory
		String extStore = System.getenv("EXTERNAL_STORAGE") + "/Messenger";
		File f_exts = new File(extStore);

		// check if directory exists
		if (f_exts.exists()) {
			File[] fileDir = f_exts.listFiles();
			String[] deletedFiles = new String[fileDir.length];
			int deletedFilesIndex = 0;

			// delete file
			for (int i = 0; i < fileDir.length; i++) {
				// do not delete lollipin db
				if (!(fileDir[i].getName().startsWith("LollipinDB") || (fileDir[i].getName().startsWith("AppLockImpl"))) && (fileDir[i].delete())) {
					deletedFiles[deletedFilesIndex] = fileDir[i].toString();
					deletedFilesIndex++;
					com.glaciersecurity.glaciermessenger.utils.Log.d("Glacier", "File list: Successfully deleted " + fileDir[i]);
				} else {
					com.glaciersecurity.glaciermessenger.utils.Log.d("Glacier", "File list: Did not delete " + fileDir[i]);
				}
			}
		}
	}

	/**
	 * Clear voice recordings
	 */
	private void clearVoiceRecordings() {
		// Retrieve directory
		String extStore = System.getenv("EXTERNAL_STORAGE") + "/Voice Recorder";
		File f_exts = new File(extStore);

		// check if directory exists
		if (f_exts.exists()) {
			File[] fileDir = f_exts.listFiles();
			String[] deletedFiles = new String[fileDir.length];
			int deletedFilesIndex = 0;

			// delete file
			for (int i = 0; i < fileDir.length; i++) {
				if (fileDir[i].delete()) {
					deletedFiles[deletedFilesIndex] = fileDir[i].toString();
					deletedFilesIndex++;
					com.glaciersecurity.glaciermessenger.utils.Log.d("Glacier", "File list: Successfully deleted " + fileDir[i]);
				} else {
					com.glaciersecurity.glaciermessenger.utils.Log.d("Glacier", "File list: Did not delete " + fileDir[i]);
				}
			}
		}
	}

	/**
	 * Clear shared locations
	 */
	private void clearSharedLocations() {
		// Retrieve directory
		//String extStore = System.getenv("EXTERNAL_STORAGE") + "/Android/data/com.glaciersecurity.glaciermessenger.sharelocation/cache";
		ArrayList<String> deletedFiles = new ArrayList<String>();
		String extStore = System.getenv("EXTERNAL_STORAGE") + "/Android/data/com.glaciersecurity.glaciermessenger.sharelocation";
		File f_exts = new File(extStore);

		// check if directory exists
		if (f_exts.exists()) {

			String extStore2 = extStore + "/cache";
			File f_exts2 = new File(extStore2);

			if (f_exts2.exists()) {
				File[] fileDir = f_exts2.listFiles();

				// delete file
				for (int i = 0; i < fileDir.length; i++) {
					if (fileDir[i].delete()) {
						deletedFiles.add(fileDir[i].toString());
						com.glaciersecurity.glaciermessenger.utils.Log.d("Glacier", "File list: Successfully deleted " + fileDir[i]);
					} else {
						com.glaciersecurity.glaciermessenger.utils.Log.d("Glacier", "File list: Did not delete " + fileDir[i]);
					}
				}
				if (f_exts2.delete()) {
					deletedFiles.add(f_exts2.toString());
				}
			}

			if (f_exts.delete()) {
				deletedFiles.add(f_exts.toString());
			}

			// String[] delFile = {fileDir[fileDir.length-1].toString()};
			String[] stringArray = deletedFiles.toArray(new String[0]);
			// callBroadcast(deletedFiles.toArray(new String[0]));
		}
	}

	private void clearExternalStorage() {
		FileBackend.removeStorageDirectory();
	}

	/**
	 * Clear pictures in Pictures/Messenger directory
	 */
	private void clearPictures() {
		// Retrieve directory
		String extStore = System.getenv("EXTERNAL_STORAGE") + "/Pictures/Messenger";
		File f_exts = new File(extStore);

		// check if directory exists
		if (f_exts.exists()) {
			File[] fileDir = f_exts.listFiles();
			String[] deletedFiles = new String[fileDir.length];
			int deletedFilesIndex = 0;

			// delete file
			for (int i = 0; i < fileDir.length; i++) {
				if (fileDir[i].delete()) {
					deletedFiles[deletedFilesIndex] = fileDir[i].toString();
					deletedFilesIndex++;
					com.glaciersecurity.glaciermessenger.utils.Log.d("Glacier", "File list: Successfully deleted " + fileDir[i]);
				} else {
					com.glaciersecurity.glaciermessenger.utils.Log.d("Glacier", "File list: Did not delete " + fileDir[i]);
				}
			}
		}

		// Remove higher level files
		extStore = System.getenv("EXTERNAL_STORAGE") + "/Pictures";
		f_exts = new File(extStore);

		// check if directory exists
		if (f_exts.exists()) {
			File[] fileDir = f_exts.listFiles();
			String[] deletedFiles = new String[fileDir.length];
			int deletedFilesIndex = 0;

			// delete file
			for (int i = 0; i < fileDir.length; i++) {
				// do not remove directory
				if ((!fileDir[i].isDirectory()) && (fileDir[i].delete())) {
					deletedFiles[deletedFilesIndex] = fileDir[i].toString();
					deletedFilesIndex++;
					com.glaciersecurity.glaciermessenger.utils.Log.d("Glacier", "File list: Successfully deleted " + fileDir[i]);
				} else {
					com.glaciersecurity.glaciermessenger.utils.Log.d("Glacier", "File list: Did not delete " + fileDir[i]);
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_conversations, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onConversationSelected(Conversation conversation) {
		clearPendingViewIntent();
		if (ConversationFragment.getConversation(this) == conversation) {
			Log.d(Config.LOGTAG, "ignore onConversationSelected() because conversation is already open");
			return;
		}
		openConversation(conversation, null);
	}

	public void clearPendingViewIntent() {
		if (pendingViewIntent.clear()) {
			Log.e(Config.LOGTAG, "cleared pending view intent");
		}
	}

	private void displayToast(final String msg) {
		runOnUiThread(() -> Toast.makeText(ConversationsActivity.this, msg, Toast.LENGTH_SHORT).show());
	}

	@Override
	public void onAffiliationChangedSuccessful(Jid jid) {

	}

	@Override
	public void onAffiliationChangeFailed(Jid jid, int resId) {
		displayToast(getString(resId, jid.asBareJid().toString()));
	}

	private void openConversation(Conversation conversation, Bundle extras) {
		conversationFragment = (ConversationFragment) getFragmentManager().findFragmentById(R.id.secondary_fragment);
		final boolean mainNeedsRefresh;
		if (conversationFragment == null) {
			mainNeedsRefresh = false;
			Fragment mainFragment = getFragmentManager().findFragmentById(R.id.main_fragment);
			if (mainFragment instanceof ConversationFragment) {
				conversationFragment = (ConversationFragment) mainFragment;

			} else {
				conversationFragment = new ConversationFragment();
				FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
				fragmentTransaction.replace(R.id.main_fragment, conversationFragment);
				fragmentTransaction.addToBackStack(null);
				try {
					fragmentTransaction.commit();
				} catch (IllegalStateException e) {
					Log.w(Config.LOGTAG,"sate loss while opening conversation",e);
					//allowing state loss is probably fine since view intents et all are already stored and a click can probably be 'ignored'
					return;
				}
			}
		} else {
			mainNeedsRefresh = true;
		}
		conversationFragment.reInit(conversation, extras == null ? new Bundle() : extras);
		if (mainNeedsRefresh) {
			refreshFragment(R.id.main_fragment);
		} else {
			invalidateActionBarTitle();
		}
	}

	public boolean onXmppUriClicked(Uri uri) {
		XmppUri xmppUri = new XmppUri(uri);
		if (xmppUri.isJidValid() && !xmppUri.hasFingerprints()) {
			final Conversation conversation = xmppConnectionService.findUniqueConversationByJid(xmppUri);
			if (conversation != null) {
				openConversation(conversation, null);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (MenuDoubleTabUtil.shouldIgnoreTap()) {
			return false;
		}
		switch (item.getItemId()) {
			case android.R.id.home:
				FragmentManager fm = getFragmentManager();
				if (fm.getBackStackEntryCount() > 0) {
					try {
						fm.popBackStack();
					} catch (IllegalStateException e) {
						Log.w(Config.LOGTAG,"Unable to pop back stack after pressing home button");
					}
					return true;
				}
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		Intent pendingIntent = pendingViewIntent.peek();
		savedInstanceState.putParcelable("intent", pendingIntent != null ? pendingIntent : getIntent());
		if (mAccount != null) {
			savedInstanceState.putString("account", mAccount.getJid().asBareJid().toString());
		}
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	protected void onStart() {
		final int theme = findTheme();
		if (this.mTheme != theme) {
			this.mSkipBackgroundBinding = true;
			recreate();
		} else {
			this.mSkipBackgroundBinding = false;
		}
		mRedirectInProcess.set(false);
		registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));


		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(connectivityReceiver);
	}

	@Override
	public void onNetworkConnectionChanged(boolean isConnected) {
		updateStatusIcon();


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
						//	generateSignature(null, template);
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
				if(isVisible){
					offlineLayout.setVisibility(View.VISIBLE);
				} else {
					offlineLayout.setVisibility(View.GONE);
				}
				reconfigureOfflineText(str, withRefresh);
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

	@Override
	protected void onNewIntent(final Intent intent) {
		if (isViewOrShareIntent(intent)) {
			if (xmppConnectionService != null) {
				clearPendingViewIntent();
				processViewIntent(intent);

				String uuid = intent.getStringExtra(EXTRA_CONVERSATION);
				Conversation conversation = uuid != null ? xmppConnectionService.findConversationByUuid(uuid) : null;
				if (conversation != null) {
					if (conversation.getMode() == Conversation.MODE_MULTI && (conversation.getMucOptions().membersOnly())) {
						AxolotlService axolotlService = conversation.getAccount().getAxolotlService();
						axolotlService.createSessionsIfNeeded(conversation);
						conversation.reloadFingerprints(axolotlService.getCryptoTargets(conversation));
					}
				}
			} else {
				pendingViewIntent.push(intent);
			}
		}
		setIntent(createLauncherIntent(this));
	}

	@Override
	public void onKeyStatusUpdated(final AxolotlService.FetchStatus report) {
		Conversation conversation = ConversationFragment.getConversationReliable(this);
		if (conversation != null && conversation.getMode() == Conversation.MODE_MULTI) {
			AxolotlService axolotlService = conversation.getAccount().getAxolotlService();
			conversation.reloadFingerprints(axolotlService.getCryptoTargets(conversation));
			conversation.commitTrusts();
			xmppConnectionService.updateConversation(conversation);
		}
	}

	@Override
	public void onPause() {
		this.mActivityPaused = true;
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		this.mActivityPaused = false;
		for (int i = 0; i < nav_view.getMenu().size(); i++) {
			nav_view.getMenu().getItem(i).setChecked(false);
		}
		updateStatusIcon();
		updateOfflineStatusBar();
	}

	private void updateStatusIcon(){
		if (this.drawer.isDrawerOpen(GravityCompat.START)){
			Button status_text = (Button) findViewById(R.id.nav_status_text);
			ImageView status_icon = (ImageView) findViewById(R.id.nav_status_icon);
			try {
				if (ConnectivityReceiver.isConnected(getApplicationContext())) {
					status_text.setText(mAccount.getPresenceStatus().toDisplayString());
					status_icon.setImageResource(mAccount.getPresenceStatus().getStatusIcon());
				} else {
					status_text.setText(Presence.Status.OFFLINE.toDisplayString());
					status_icon.setImageResource(Presence.Status.OFFLINE.getStatusIcon());
				}
			} catch (Exception e) {

			}
		}
	}
	private void initializeFragments() {
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		Fragment mainFragment = getFragmentManager().findFragmentById(R.id.main_fragment);
		Fragment secondaryFragment = getFragmentManager().findFragmentById(R.id.secondary_fragment);
		if (mainFragment != null) {
			if (binding.secondaryFragment != null) {
				if (mainFragment instanceof ConversationFragment) {
					getFragmentManager().popBackStack();
					transaction.remove(mainFragment);
					transaction.commit();
					getFragmentManager().executePendingTransactions();
					transaction = getFragmentManager().beginTransaction();
					transaction.replace(R.id.secondary_fragment, mainFragment);
					transaction.replace(R.id.main_fragment, new ConversationsOverviewFragment());
					transaction.commit();
					return;
				}
			} else {
				if (secondaryFragment instanceof ConversationFragment) {
					transaction.remove(secondaryFragment);
					transaction.commit();
					getFragmentManager().executePendingTransactions();
					transaction = getFragmentManager().beginTransaction();
					transaction.replace(R.id.main_fragment, secondaryFragment);
					transaction.addToBackStack(null);
					transaction.commit();
					return;
				}
			}
		} else {
			transaction.replace(R.id.main_fragment, new ConversationsOverviewFragment());
		}
		if (binding.secondaryFragment != null && secondaryFragment == null) {
			transaction.replace(R.id.secondary_fragment, new ConversationFragment());
		}
		transaction.commit();
	}

	private void invalidateActionBarTitle() {
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			Fragment mainFragment = getFragmentManager().findFragmentById(R.id.main_fragment);
			if (mainFragment instanceof ConversationFragment) {
				final Conversation conversation = ((ConversationFragment) mainFragment).getConversation();
				if (conversation != null) {
					actionBar.setTitle(EmojiWrapper.transform(conversation.getName()));
					if (conversation.getMode() == Conversation.MODE_MULTI) {
						actionBar.setTitle(EmojiWrapper.transform("#"+conversation.getName()));
						showGroupToolbar();
					} else {
						show1v1ChatToolbar(conversation,(ConversationFragment) mainFragment);
					}
					actionBar.setDisplayHomeAsUpEnabled(true);
					return;
				}
			}
			actionBar.setTitle(R.string.app_name);
			actionBar.setDisplayHomeAsUpEnabled(true);
			showNavMenu();
			hideStatusToolbar();
		}
	}

	@Override
	public void onConversationArchived(Conversation conversation) {
		if (performRedirectIfNecessary(conversation, false)) {
			return;
		}
		Fragment mainFragment = getFragmentManager().findFragmentById(R.id.main_fragment);
		if (mainFragment instanceof ConversationFragment) {
			try {
				getFragmentManager().popBackStack();
			} catch (IllegalStateException e) {
				Log.w(Config.LOGTAG,"state loss while popping back state after archiving conversation",e);
				//this usually means activity is no longer active; meaning on the next open we will run through this again
			}
			return;
		}
		Fragment secondaryFragment = getFragmentManager().findFragmentById(R.id.secondary_fragment);
		if (secondaryFragment instanceof ConversationFragment) {
			if (((ConversationFragment) secondaryFragment).getConversation() == conversation) {
				Conversation suggestion = ConversationsOverviewFragment.getSuggestion(this, conversation);
				if (suggestion != null) {
					openConversation(suggestion, null);
				}
			}
		}
	}

	@Override
	public void onConversationsListItemUpdated() {
		Fragment fragment = getFragmentManager().findFragmentById(R.id.main_fragment);
		if (fragment instanceof ConversationsOverviewFragment) {
			((ConversationsOverviewFragment) fragment).refresh();

		}
	}

	@Override
	public void switchToConversation(Conversation conversation) {
		Log.d(Config.LOGTAG, "override");
		openConversation(conversation, null);
	}

	@Override
	public void onConversationRead(Conversation conversation, String upToUuid) {
		if (!mActivityPaused && pendingViewIntent.peek() == null) {
			xmppConnectionService.sendReadMarker(conversation, upToUuid);
		} else {
			Log.d(Config.LOGTAG, "ignoring read callback. mActivityPaused=" + Boolean.toString(mActivityPaused));
		}
	}

	@Override
	public void onAccountUpdate() {
		this.refreshUi();
		runOnUiThread(() -> {
			this.invalidateActionBarTitle();
		});
	}

	@Override
	public void onConversationUpdate() {
		if (performRedirectIfNecessary(false)) {
			return;
		}
		this.refreshUi();
	}

	@Override
	public void onRosterUpdate() {
		this.refreshUi();
	}


	@Override
	public void OnUpdateBlocklist(OnUpdateBlocklist.Status status) {
		this.refreshUi();
	}

	@Override
	public void onShowErrorToast(int resId) {
		runOnUiThread(() -> Toast.makeText(this, resId, Toast.LENGTH_SHORT).show());
	}

	@Override
	public void handleParticipantConnected(TwilioCallParticipant remoteCallParticipant) {

	}

	@Override
	public void handleParticipantDisconnected(RemoteParticipant remoteParticipant) {
		if (mCallManager.getRemoteParticipants().size() > 0) {
			return; //still in call with other members
		}

		returnToCall.setVisibility(View.GONE);

		final Intent intent = new Intent(this, XmppConnectionService.class);
		intent.setAction(XmppConnectionService.ACTION_FINISH_CALL);
		Compatibility.startService(this, intent);
		mCallManager.handleDisconnect();
		mCallManager.setCallListener(null);

	}

	@Override
	public void handleConnected(Room room) {

	}

	@Override
	public void handleReconnecting(boolean reconnecting) {

	}

	@Override
	public void handleConnectFailure() {
		returnToCall.setVisibility(View.GONE);

	}

	@Override
	public void endListening() {
		returnToCall.setVisibility(View.GONE);
		mCallManager.handleDisconnect();
		mCallManager.setCallListener(null);

	}

	@Override
	public LocalAudioTrack getLocalAudioTrack() {
		return null;
	}

	@Override
	public LocalVideoTrack getLocalVideoTrack() {
		return null;
	}



}
