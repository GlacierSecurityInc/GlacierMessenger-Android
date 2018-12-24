package com.glaciersecurity.glaciermessenger.ui;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.ActionBar;
//import android.support.v7.app.AlertDialog;
//import android.support.v7.app.AlertDialog.Builder;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.glaciersecurity.glaciercore.api.APIVpnProfile;
import com.glaciersecurity.glaciercore.api.IOpenVPNAPIService;
import com.glaciersecurity.glaciercore.api.IOpenVPNStatusCallback;
import com.glaciersecurity.glaciermessenger.cognito.BackupAccountManager;
import com.glaciersecurity.glaciermessenger.cognito.Constants;
import com.glaciersecurity.glaciermessenger.cognito.Util;
import com.glaciersecurity.glaciermessenger.utils.Log;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.openintents.openpgp.util.OpenPgpUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.cognito.AppHelper;
import com.glaciersecurity.glaciermessenger.crypto.axolotl.AxolotlService;
import com.glaciersecurity.glaciermessenger.crypto.axolotl.XmppAxolotlSession;
import com.glaciersecurity.glaciermessenger.databinding.ActivityEditAccountBinding;
import com.glaciersecurity.glaciermessenger.databinding.DialogPresenceBinding;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.Presence;
import com.glaciersecurity.glaciermessenger.entities.PresenceTemplate;
import com.glaciersecurity.glaciermessenger.services.BarcodeProvider;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService.OnAccountUpdate;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService.OnCaptchaRequested;
import com.glaciersecurity.glaciermessenger.ui.adapter.PresenceTemplateAdapter;
import com.glaciersecurity.glaciermessenger.ui.util.MenuDoubleTabUtil;
import com.glaciersecurity.glaciermessenger.ui.util.PendingItem;
import com.glaciersecurity.glaciermessenger.ui.util.SoftKeyboardUtils;
import com.glaciersecurity.glaciermessenger.utils.CryptoHelper;
import com.glaciersecurity.glaciermessenger.utils.UIHelper;
import com.glaciersecurity.glaciermessenger.utils.XmppUri;
import com.glaciersecurity.glaciermessenger.xml.Element;
import com.glaciersecurity.glaciermessenger.xmpp.OnKeyStatusUpdated;
import com.glaciersecurity.glaciermessenger.xmpp.OnUpdateBlocklist;
import com.glaciersecurity.glaciermessenger.xmpp.XmppConnection;
import com.glaciersecurity.glaciermessenger.xmpp.XmppConnection.Features;
import com.glaciersecurity.glaciermessenger.xmpp.forms.Data;
import com.glaciersecurity.glaciermessenger.xmpp.pep.Avatar;
import rocks.xmpp.addr.Jid;

public class EditAccountActivity extends OmemoActivity implements OnAccountUpdate, OnUpdateBlocklist,
		OnKeyStatusUpdated, OnCaptchaRequested, KeyChainAliasCallback, XmppConnectionService.OnShowErrorToast, XmppConnectionService.OnMamPreferencesFetched, Handler.Callback, OpenVPNProfileListener {

	private static final int REQUEST_DATA_SAVER = 0x37af244;
	private static final int MSG_UPDATE_STATE = 0;
	private static final int ICS_OPENVPN_PERMISSION = 7;

	private final String REPLACEMENT_ORG_ID = "<org_id>";
	private final int VPN_STATE_UNKNOWN     = 0;
	private final int VPN_STATE_NOPROCESS   = 1;
	private final int VPN_STATE_MISC        = 2;
	private final int VPN_STATE_CONNECTED   = 3;

	private static final int REQUEST_CHANGE_STATUS = 0xee11;
	private TextInputLayout mAccountJidLayout;
	private TextInputEditText mJid;

	private EditText mPassword;
	private TextInputLayout mAccountPasswordLayout;

	// COGNITO
	private EditText mOrgID;
	private TextInputLayout mAccountOrgIDLayout;
	private Button mLoginButton;
	private String mConnectionType = null;

	private Button mDisableOsOptimizationsButton;
	private TextView getmDisableOsOptimizationsBody;
	private TableLayout mMoreTable;

	private TextView mAxolotlFingerprint;
	private TextView mPgpFingerprint;
	private TextView mOwnFingerprintDesc;
	private TextView getmPgpFingerprintDesc;
	private ImageView mAvatar;
	private RelativeLayout mAxolotlFingerprintBox;
	private RelativeLayout mPgpFingerprintBox;
	private ImageButton mAxolotlFingerprintToClipboardButton;
	private ImageButton mPgpDeleteFingerprintButton;
	private LinearLayout keys;
	private LinearLayout mNamePort;
	private EditText mHostname;
	private TextInputLayout mHostnameLayout;
	private EditText mPort;
	private TextInputLayout mPortLayout;
	private AlertDialog mCaptchaDialog = null;

	private Jid jidToEdit;
	private boolean mInitMode = false;
	private boolean mUsernameMode = Config.DOMAIN_LOCK != null;
	private boolean mShowOptions = false;
	private Account mAccount;
	private String messageFingerprint;

	private final PendingItem<PresenceTemplate> mPendingPresenceTemplate = new PendingItem<>();

	private boolean mFetchingAvatar = false;

	// Cognito Details - remember when retry to login
	private String username = null;
	private String password = null;
	private String organization = null;

	protected IOpenVPNAPIService mService = null;

	// track vpn downloads so we know when to stop
	private int downloadCount = 0;

	private int lastConnectionState = VPN_STATE_UNKNOWN;

	// reset values
	private String download_keys = null;
	private Handler mHandler;

	private AlertDialog userDialog = null;
	private AlertDialog waitDialog = null;

	// keep track of profiles downloaded from AWS
	private ArrayList<String> keyList = new ArrayList<String>();

	private Toast mFetchingMamPrefsToast;
	private String mSavedInstanceAccount;
	private boolean mSavedInstanceInit = false;
	private Button mClearDevicesButton;
	private XmppUri pendingUri = null;
	private boolean mUseTor;
	private ActivityEditAccountBinding binding;

	private boolean conversationStarted = false;

	private String currentProfileUUID = null;
	private String currentProfileName = null;

	public void refreshUiReal() {
		invalidateOptionsMenu();
		if (mAccount != null
				&& mAccount.getStatus() != Account.State.ONLINE
				&& mFetchingAvatar) {
			startActivity(new Intent(getApplicationContext(),
					ManageAccountActivity.class));
			finish();
		} else if (mInitMode && mAccount != null && mAccount.getStatus() == Account.State.ONLINE) {
			if (!mFetchingAvatar) {
				mFetchingAvatar = true;
				xmppConnectionService.checkForAvatar(mAccount, mAvatarFetchCallback);
			}

			// COGNITO - Call Conversation activity immediately after account is online/connected
			// This will help minimize any wait time for pulling up contact information.
			if (!conversationStarted) {
				conversationStarted = true;
				if (mAccount.getStatus() == Account.State.ONLINE) {
					Intent intent = new Intent(getApplicationContext(),
							StartConversationActivity.class);
					if (xmppConnectionService != null && xmppConnectionService.getAccounts().size() == 1) {
						intent.putExtra("init", true);
					}
					startActivity(intent);
					finish();
				}
			}
		}
		if (mAccount != null) {
			updateAccountInformation(false);
		}

	}

	@Override
	public boolean onNavigateUp() {
		deleteAccountAndReturnIfNecessary();
		return super.onNavigateUp();
	}

	@Override
	public void onBackPressed() {

	}

	private void deleteAccountAndReturnIfNecessary() {
		if (mInitMode && mAccount != null && !mAccount.isOptionSet(Account.OPTION_LOGGED_IN_SUCCESSFULLY)) {
			xmppConnectionService.deleteAccount(mAccount);
		}

		if (xmppConnectionService.getAccounts().size() == 0 && Config.MAGIC_CREATE_DOMAIN != null) {
			Intent intent = new Intent(EditAccountActivity.this, WelcomeActivity.class);
			WelcomeActivity.addInviteUri(intent, getIntent());
			startActivity(intent);
		}
	}

	@Override
	public void onAccountUpdate() {
		refreshUi();
	}

	private final UiCallback<Avatar> mAvatarFetchCallback = new UiCallback<Avatar>() {

		@Override
		public void userInputRequried(final PendingIntent pi, final Avatar avatar) {
			finishInitialSetup(avatar);
		}

		@Override
		public void success(final Avatar avatar) {
			finishInitialSetup(avatar);
		}

		@Override
		public void error(final int errorCode, final Avatar avatar) {
			finishInitialSetup(avatar);
		}
	};
	private final TextWatcher mTextWatcher = new TextWatcher() {

		@Override
		public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {

		}

		@Override
		public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
		}

		@Override
		public void afterTextChanged(final Editable s) {

		}
	};

	private View.OnFocusChangeListener mEditTextFocusListener = new View.OnFocusChangeListener() {
		@Override
		public void onFocusChange(View view, boolean b) {
			EditText et = (EditText) view;
			if (b) {
				int resId = mUsernameMode ? R.string.username : R.string.account_settings_example_jabber_id;
				if (view.getId() == R.id.hostname) {
					resId = mUseTor ? R.string.hostname_or_onion : R.string.hostname_example;
				}
				final int res = resId;
				new Handler().postDelayed(() -> et.setHint(res), 200);
			} else {
				et.setHint(null);
			}
		}
	};


	private final OnClickListener mAvatarClickListener = new OnClickListener() {
		@Override
		public void onClick(final View view) {
			if (mAccount != null) {
				final Intent intent = new Intent(getApplicationContext(), PublishProfilePictureActivity.class);
				intent.putExtra(EXTRA_ACCOUNT, mAccount.getJid().asBareJid().toString());
				startActivity(intent);
			}
		}
	};

	protected void finishInitialSetup(final Avatar avatar) {
		runOnUiThread(() -> {
			SoftKeyboardUtils.hideSoftKeyboard(EditAccountActivity.this);
			final Intent intent;
			final XmppConnection connection = mAccount.getXmppConnection();
			final boolean wasFirstAccount = xmppConnectionService != null && xmppConnectionService.getAccounts().size() == 1;
			if (avatar != null || (connection != null && !connection.getFeatures().pep())) {
				intent = new Intent(getApplicationContext(), StartConversationActivity.class);
				if (wasFirstAccount) {
					intent.putExtra("init", true);
				}
			} else {
				intent = new Intent(getApplicationContext(),
						StartConversationActivity.class);
				if (xmppConnectionService != null && xmppConnectionService.getAccounts().size() == 1) {
					intent.putExtra("init", true);
				}
			}
			if (wasFirstAccount) {
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			}
			WelcomeActivity.addInviteUri(intent, getIntent());
			startActivity(intent);
			finish();
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_BATTERY_OP || requestCode == REQUEST_DATA_SAVER) {
			updateAccountInformation(mAccount == null);
		}
		if (requestCode == REQUEST_CHANGE_STATUS) {
			PresenceTemplate template = mPendingPresenceTemplate.pop();
			if (template != null && resultCode == Activity.RESULT_OK) {
				generateSignature(data, template);
			} else {
				Log.d(Config.LOGTAG, "pgp result not ok");
			}
		}
	}

	@Override
	protected void processFingerprintVerification(XmppUri uri) {
		processFingerprintVerification(uri, true);
	}


	protected void processFingerprintVerification(XmppUri uri, boolean showWarningToast) {
		if (mAccount != null && mAccount.getJid().asBareJid().equals(uri.getJid()) && uri.hasFingerprints()) {
			if (xmppConnectionService.verifyFingerprints(mAccount, uri.getFingerprints())) {
				Toast.makeText(this, R.string.verified_fingerprints, Toast.LENGTH_SHORT).show();
				updateAccountInformation(false);
			}
		} else if (showWarningToast) {
			Toast.makeText(this, R.string.invalid_barcode, Toast.LENGTH_SHORT).show();
		}
	}

	protected boolean accountInfoEdited() {
		if (this.mAccount == null) {
			return false;
		}
		return jidEdited() ||
				!this.mAccount.getPassword().equals(this.mPassword.getText().toString()) ||
				!this.mAccount.getHostname().equals(this.mHostname.getText().toString()) ||
				!String.valueOf(this.mAccount.getPort()).equals(this.mPort.getText().toString());
	}

	protected boolean jidEdited() {
		final String unmodified;
		if (mUsernameMode) {
			unmodified = this.mAccount.getJid().getLocal();
		} else {
			unmodified = this.mAccount.getJid().asBareJid().toString();
		}
		return !unmodified.equals(this.binding.accountJid.getText().toString());
	}

	protected boolean passwordChangedInMagicCreateMode() {
		return mAccount != null
				&& mAccount.isOptionSet(Account.OPTION_MAGIC_CREATE)
				&& !this.mAccount.getPassword().equals(this.mPassword.getText().toString())
				&& !this.jidEdited()
				&& mAccount.isOnlineAndConnected();
	}

	@Override
	protected String getShareableUri(boolean http) {
		if (mAccount != null) {
			return http ? mAccount.getShareableLink() : mAccount.getShareableUri();
		} else {
			return null;
		}
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.password = null;
		this.username = null;
		this.organization = null;

		// COGNITO
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		if (savedInstanceState != null) {
			this.mSavedInstanceAccount = savedInstanceState.getString("account");
			this.mSavedInstanceInit = savedInstanceState.getBoolean("initMode", false);
		}
		this.binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_account);
		setSupportActionBar((Toolbar) binding.toolbar);
		configureActionBar(getSupportActionBar());
		binding.accountJid.addTextChangedListener(this.mTextWatcher);
		binding.accountJid.setOnFocusChangeListener(this.mEditTextFocusListener);
		this.mAccountJidLayout = (TextInputLayout) findViewById(R.id.account_jid_layout);
		this.mJid = (TextInputEditText) findViewById(R.id.account_password);
		this.mPassword = (EditText) findViewById(R.id.account_password);
		this.mPassword.addTextChangedListener(this.mTextWatcher);
		this.mAccountPasswordLayout = (TextInputLayout) findViewById(R.id.account_password_layout);

		// GOOBER COGNITO
		this.mOrgID = (EditText) findViewById(R.id.account_orgid);
		this.mAccountOrgIDLayout = (TextInputLayout) findViewById(R.id.account_orgid_layout);
		this.mLoginButton = (Button) findViewById(R.id.account_login);

		this.mAvatar = (ImageView) findViewById(R.id.avater);
		this.mAvatar.setOnClickListener(this.mAvatarClickListener);
		this.mDisableOsOptimizationsButton = (Button) findViewById(R.id.os_optimization_disable);
		this.getmDisableOsOptimizationsBody = (TextView) findViewById(R.id.os_optimization_body);
		this.mPgpFingerprintBox = (RelativeLayout) findViewById(R.id.pgp_fingerprint_box);
		this.mPgpFingerprint = (TextView) findViewById(R.id.pgp_fingerprint);
		this.getmPgpFingerprintDesc = (TextView) findViewById(R.id.pgp_fingerprint_desc);
		this.mPgpDeleteFingerprintButton = (ImageButton) findViewById(R.id.action_delete_pgp);
		this.mAxolotlFingerprint = (TextView) findViewById(R.id.axolotl_fingerprint);
		this.mAxolotlFingerprintBox = (RelativeLayout) findViewById(R.id.axolotl_fingerprint_box);
		this.mAxolotlFingerprintToClipboardButton = (ImageButton) findViewById(R.id.action_copy_axolotl_to_clipboard);
		this.mOwnFingerprintDesc = (TextView) findViewById(R.id.own_fingerprint_desc);
		this.keys = (LinearLayout) findViewById(R.id.other_device_keys);
		this.mNamePort = (LinearLayout) findViewById(R.id.name_port);
		this.mHostname = (EditText) findViewById(R.id.hostname);
		this.mHostname.addTextChangedListener(mTextWatcher);
		this.mHostname.setOnFocusChangeListener(mEditTextFocusListener);
		this.mHostnameLayout = (TextInputLayout) findViewById(R.id.hostname_layout);
		this.mClearDevicesButton = (Button) findViewById(R.id.clear_devices);
		this.mClearDevicesButton.setOnClickListener(v -> showWipePepDialog());
		this.mPort = (EditText) findViewById(R.id.port);
		this.mPort.setText("5222");
		this.mPort.addTextChangedListener(mTextWatcher);
		this.mPortLayout = (TextInputLayout) findViewById(R.id.port_layout);
		this.mMoreTable = (TableLayout) findViewById(R.id.server_info_more);
		if (savedInstanceState != null && savedInstanceState.getBoolean("showMoreTable")) {
			changeMoreTableVisibility(true);
		}
		final OnCheckedChangeListener OnCheckedShowConfirmPassword = new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
				//
			}
		};
		this.binding.accountRegisterNew.setOnCheckedChangeListener(OnCheckedShowConfirmPassword);
		if (Config.DISALLOW_REGISTRATION_IN_UI) {
			this.binding.accountRegisterNew.setVisibility(View.GONE);
		}
		askForPermissions();

		// Cognito - Initialize application
		AppHelper.init(getApplicationContext());

		// COGNITO - Test
		cognitoCurrentUserSignout();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.editaccount, menu);
		final MenuItem showBlocklist = menu.findItem(R.id.action_show_block_list);
		final MenuItem showMoreInfo = menu.findItem(R.id.action_server_info_show_more);
		final MenuItem changePassword = menu.findItem(R.id.action_change_password_on_server);
		final MenuItem renewCertificate = menu.findItem(R.id.action_renew_certificate);
		final MenuItem mamPrefs = menu.findItem(R.id.action_mam_prefs);
		final MenuItem changePresence = menu.findItem(R.id.action_change_presence);
		final MenuItem share = menu.findItem(R.id.action_share);
		renewCertificate.setVisible(mAccount != null && mAccount.getPrivateKeyAlias() != null);

		share.setVisible(mAccount != null && !mInitMode);

		if (mAccount != null && mAccount.isOnlineAndConnected()) {
			if (!mAccount.getXmppConnection().getFeatures().blocking()) {
				showBlocklist.setVisible(false);
			}

			if (!mAccount.getXmppConnection().getFeatures().register()) {
				changePassword.setVisible(false);
			}
			mamPrefs.setVisible(mAccount.getXmppConnection().getFeatures().mam());
			changePresence.setVisible(!mInitMode);
		} else {
			showBlocklist.setVisible(false);
			showMoreInfo.setVisible(false);
			changePassword.setVisible(false);
			mamPrefs.setVisible(false);
			changePresence.setVisible(false);
		}

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		final MenuItem showMoreInfo = menu.findItem(R.id.action_server_info_show_more);
		if (showMoreInfo.isVisible()) {
			showMoreInfo.setChecked(mMoreTable.getVisibility() == View.VISIBLE);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected void onStart() {
		super.onStart();
		final Intent intent = getIntent();
		final int theme = findTheme();
		if (this.mTheme != theme) {
			recreate();
		} else if (intent != null) {
			try {
				this.jidToEdit = Jid.of(intent.getStringExtra("jid"));
			} catch (final IllegalArgumentException | NullPointerException ignored) {
				this.jidToEdit = null;
			}
			if (jidToEdit != null && intent.getData() != null && intent.getBooleanExtra("scanned", false)) {
				final XmppUri uri = new XmppUri(intent.getData());
				if (xmppConnectionServiceBound) {
					processFingerprintVerification(uri, false);
				} else {
					this.pendingUri = uri;
				}
			}
			boolean init = intent.getBooleanExtra("init", false);
			this.mInitMode = init || this.jidToEdit == null;
			this.messageFingerprint = intent.getStringExtra("fingerprint");
			if (!mInitMode) {
				this.binding.accountRegisterNew.setVisibility(View.GONE);
			} else {
				this.mAvatar.setVisibility(View.GONE);
				ActionBar ab = getSupportActionBar();
				if (ab != null) {
					if (init && Config.MAGIC_CREATE_DOMAIN == null) {
						ab.setDisplayShowHomeEnabled(false);
						ab.setDisplayHomeAsUpEnabled(false);
					}
					ab.setTitle(R.string.action_add_account);
				}
			}
		}
		SharedPreferences preferences = getPreferences();
		mUseTor = Config.FORCE_ORBOT || preferences.getBoolean("use_tor", false);
		this.mShowOptions = mUseTor || preferences.getBoolean("show_connection_options", false);
		this.mNamePort.setVisibility(mShowOptions ? View.VISIBLE : View.GONE);

		// CORE integration
		mHandler = new Handler(this);
		bindService();
	}

	@Override
	public void onNewIntent(Intent intent) {
		if (intent != null && intent.getData() != null) {
			final XmppUri uri = new XmppUri(intent.getData());
			if (xmppConnectionServiceBound) {
				processFingerprintVerification(uri, false);
			} else {
				this.pendingUri = uri;
			}
		}
	}

	@Override
	public void onSaveInstanceState(final Bundle savedInstanceState) {
		if (mAccount != null) {
			savedInstanceState.putString("account", mAccount.getJid().asBareJid().toString());
			savedInstanceState.putBoolean("initMode", mInitMode);
			savedInstanceState.putBoolean("showMoreTable", mMoreTable.getVisibility() == View.VISIBLE);
		}
		super.onSaveInstanceState(savedInstanceState);
	}

	protected void onBackendConnected() {
		boolean init = true;
		if (mSavedInstanceAccount != null) {
			try {
				this.mAccount = xmppConnectionService.findAccountByJid(Jid.of(mSavedInstanceAccount));
				this.mInitMode = mSavedInstanceInit;
				init = false;
			} catch (IllegalArgumentException e) {
				this.mAccount = null;
			}

		} else if (this.jidToEdit != null) {
			this.mAccount = xmppConnectionService.findAccountByJid(jidToEdit);
		}

		if (mAccount != null) {
			this.mInitMode |= this.mAccount.isOptionSet(Account.OPTION_REGISTER);
			this.mUsernameMode |= mAccount.isOptionSet(Account.OPTION_MAGIC_CREATE) && mAccount.isOptionSet(Account.OPTION_REGISTER);
			if (this.mAccount.getPrivateKeyAlias() != null) {
				this.mPassword.setHint(R.string.authenticate_with_certificate);
				if (this.mInitMode) {
					this.mPassword.requestFocus();
				}
			}
			if (mPendingFingerprintVerificationUri != null) {
				processFingerprintVerification(mPendingFingerprintVerificationUri, false);
				mPendingFingerprintVerificationUri = null;
			}
			updateAccountInformation(init);
		}

		if (mUsernameMode) {
			this.binding.accountJid.setHint(R.string.username_hint);
		} else {
			//
		}

		if (pendingUri != null) {
			processFingerprintVerification(pendingUri, false);
			pendingUri = null;
		}

		invalidateOptionsMenu();

		// COGNITO - get storage permsissions


	}

	private String getUserModeDomain() {
		if (mAccount != null && mAccount.getJid().getDomain() != null) {
			return mAccount.getJid().getDomain();
		} else {
			return Config.DOMAIN_LOCK;
		}
	}



	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (MenuDoubleTabUtil.shouldIgnoreTap()) {
			return false;
		}
		switch (item.getItemId()) {
			case R.id.action_show_block_list:
				final Intent showBlocklistIntent = new Intent(this, BlocklistActivity.class);
				showBlocklistIntent.putExtra(EXTRA_ACCOUNT, mAccount.getJid().toString());
				startActivity(showBlocklistIntent);
				break;
			case R.id.action_server_info_show_more:
				changeMoreTableVisibility(!item.isChecked());
				break;
			case R.id.action_share_barcode:
				shareBarcode();
				break;
			case R.id.action_share_http:
				shareLink(true);
				break;
			case R.id.action_share_uri:
				shareLink(false);
				break;
			case R.id.action_change_password_on_server:
				gotoChangePassword(null);
				break;
			case R.id.action_mam_prefs:
				editMamPrefs();
				break;
			case R.id.action_renew_certificate:
				renewCertificate();
				break;
			case R.id.action_change_presence:
				changePresence();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void shareBarcode() {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_STREAM, BarcodeProvider.getUriForAccount(this, mAccount));
		intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		intent.setType("image/png");
		startActivity(Intent.createChooser(intent, getText(R.string.share_with)));
	}

	private void changeMoreTableVisibility(boolean visible) {
		mMoreTable.setVisibility(visible ? View.VISIBLE : View.GONE);
	}

	private void gotoChangePassword(String newPassword) {
		final Intent changePasswordIntent = new Intent(this, ChangePasswordActivity.class);
		changePasswordIntent.putExtra(EXTRA_ACCOUNT, mAccount.getJid().toString());
		if (newPassword != null) {
			changePasswordIntent.putExtra("password", newPassword);
		}
		startActivity(changePasswordIntent);
	}

	private void renewCertificate() {
		KeyChain.choosePrivateKeyAlias(this, this, null, null, null, -1, null);
	}

	private void changePresence() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		boolean manualStatus = sharedPreferences.getBoolean(SettingsActivity.MANUALLY_CHANGE_PRESENCE, getResources().getBoolean(R.bool.manually_change_presence));
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final DialogPresenceBinding binding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.dialog_presence, null, false);
		String current = mAccount.getPresenceStatusMessage();
		if (current != null && !current.trim().isEmpty()) {
			binding.statusMessage.append(current);
		}
		setAvailabilityRadioButton(mAccount.getPresenceStatus(), binding);
		binding.show.setVisibility(manualStatus ? View.VISIBLE : View.GONE);
		List<PresenceTemplate> templates = xmppConnectionService.getPresenceTemplates(mAccount);
		PresenceTemplateAdapter presenceTemplateAdapter = new PresenceTemplateAdapter(this, R.layout.simple_list_item, templates);
		binding.statusMessage.setAdapter(presenceTemplateAdapter);
		binding.statusMessage.setOnItemClickListener((parent, view, position, id) -> {
			PresenceTemplate template = (PresenceTemplate) parent.getItemAtPosition(position);
			setAvailabilityRadioButton(template.getStatus(), binding);
		});
		builder.setTitle(R.string.edit_status_message_title);
		builder.setView(binding.getRoot());
		builder.setNegativeButton(R.string.cancel, null);
		builder.setPositiveButton(R.string.confirm, (dialog, which) -> {
			PresenceTemplate template = new PresenceTemplate(getAvailabilityRadioButton(binding), binding.statusMessage.getText().toString().trim());
			if (mAccount.getPgpId() != 0 && hasPgp()) {
				generateSignature(null, template);
			} else {
				xmppConnectionService.changeStatus(mAccount, template, null);
			}
		});
		builder.create().show();
	}

	private void generateSignature(Intent intent, PresenceTemplate template) {
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
	}

	private static void setAvailabilityRadioButton(Presence.Status status, DialogPresenceBinding binding) {
		if (status == null) {
			binding.online.setChecked(true);
			return;
		}
		switch (status) {
			case DND:
				binding.dnd.setChecked(true);
				break;
			case XA:
				binding.xa.setChecked(true);
				break;
			case AWAY:
				binding.xa.setChecked(true);
				break;
			default:
				binding.online.setChecked(true);
		}
	}

	private static Presence.Status getAvailabilityRadioButton(DialogPresenceBinding binding) {
		if (binding.dnd.isChecked()) {
			return Presence.Status.DND;
		} else if (binding.xa.isChecked()) {
			return Presence.Status.XA;
		} else if (binding.away.isChecked()) {
			return Presence.Status.AWAY;
		} else {
			return Presence.Status.ONLINE;
		}
	}

	@Override
	public void alias(String alias) {
		if (alias != null) {
			xmppConnectionService.updateKeyInAccount(mAccount, alias);
		}
	}

	private void updateAccountInformation(boolean init) {
		if (init) {
			this.binding.accountJid.getEditableText().clear();
			if (mUsernameMode) {
				this.binding.accountJid.getEditableText().append(this.mAccount.getJid().getLocal());
			} else {
				this.binding.accountJid.getEditableText().append(this.mAccount.getJid().asBareJid().toString());
			}
			this.mPassword.getEditableText().clear();
			this.mPassword.getEditableText().append(this.mAccount.getPassword());
			this.mHostname.setText("");
			this.mHostname.getEditableText().append(this.mAccount.getHostname());
			this.mPort.setText("");
			this.mPort.getEditableText().append(String.valueOf(this.mAccount.getPort()));
			this.mNamePort.setVisibility(mShowOptions ? View.VISIBLE : View.GONE);

		}

		final boolean editable = !mAccount.isOptionSet(Account.OPTION_LOGGED_IN_SUCCESSFULLY);
		this.binding.accountJid.setEnabled(editable);
		this.binding.accountJid.setFocusable(editable);
		this.binding.accountJid.setFocusableInTouchMode(editable);


		if (mAccount.isOptionSet(Account.OPTION_MAGIC_CREATE) || !mAccount.isOptionSet(Account.OPTION_LOGGED_IN_SUCCESSFULLY)) {
			this.binding.accountPasswordLayout.setPasswordVisibilityToggleEnabled(true);
		} else {
			this.binding.accountPasswordLayout.setPasswordVisibilityToggleEnabled(false);
		}

		if (!mInitMode) {
			this.mAvatar.setVisibility(View.VISIBLE);
			this.mAvatar.setImageBitmap(avatarService().get(this.mAccount, (int) getResources().getDimension(R.dimen.avatar_on_details_screen_size)));
		} else {
			this.mAvatar.setVisibility(View.GONE);
		}
		this.binding.accountRegisterNew.setChecked(this.mAccount.isOptionSet(Account.OPTION_REGISTER));
		if (this.mAccount.isOptionSet(Account.OPTION_MAGIC_CREATE)) {
			if (this.mAccount.isOptionSet(Account.OPTION_REGISTER)) {
				ActionBar actionBar = getSupportActionBar();
				if (actionBar != null) {
					actionBar.setTitle(R.string.create_account);
				}
			}
			this.binding.accountRegisterNew.setVisibility(View.GONE);
		} else if (this.mAccount.isOptionSet(Account.OPTION_REGISTER)) {
			this.binding.accountRegisterNew.setVisibility(View.VISIBLE);
		} else {
			this.binding.accountRegisterNew.setVisibility(View.GONE);
		}
		if (this.mAccount.isOnlineAndConnected() && !this.mFetchingAvatar) {
			Features features = this.mAccount.getXmppConnection().getFeatures();
			this.binding.stats.setVisibility(View.VISIBLE);
			boolean showBatteryWarning = !xmppConnectionService.getPushManagementService().available(mAccount) && isOptimizingBattery();
			boolean showDataSaverWarning = isAffectedByDataSaver();
			showOsOptimizationWarning(showBatteryWarning, showDataSaverWarning);
			this.binding.sessionEst.setText(UIHelper.readableTimeDifferenceFull(this, this.mAccount.getXmppConnection()
					.getLastSessionEstablished()));
			if (features.rosterVersioning()) {
				this.binding.serverInfoRosterVersion.setText(R.string.server_info_available);
			} else {
				this.binding.serverInfoRosterVersion.setText(R.string.server_info_unavailable);
			}
			if (features.carbons()) {
				this.binding.serverInfoCarbons.setText(R.string.server_info_available);
			} else {
				this.binding.serverInfoCarbons.setText(R.string.server_info_unavailable);
			}
			if (features.mam()) {
				this.binding.serverInfoMam.setText(R.string.server_info_available);
			} else {
				this.binding.serverInfoMam.setText(R.string.server_info_unavailable);
			}
			if (features.csi()) {
				this.binding.serverInfoCsi.setText(R.string.server_info_available);
			} else {
				this.binding.serverInfoCsi.setText(R.string.server_info_unavailable);
			}
			if (features.blocking()) {
				this.binding.serverInfoBlocking.setText(R.string.server_info_available);
			} else {
				this.binding.serverInfoBlocking.setText(R.string.server_info_unavailable);
			}
			if (features.sm()) {
				this.binding.serverInfoSm.setText(R.string.server_info_available);
			} else {
				this.binding.serverInfoSm.setText(R.string.server_info_unavailable);
			}
			if (features.pep()) {
				AxolotlService axolotlService = this.mAccount.getAxolotlService();
				if (axolotlService != null && axolotlService.isPepBroken()) {
					this.binding.serverInfoPep.setText(R.string.server_info_broken);
				} else if (features.pepPublishOptions() || features.pepOmemoWhitelisted()) {
					this.binding.serverInfoPep.setText(R.string.server_info_available);
				} else {
					this.binding.serverInfoPep.setText(R.string.server_info_partial);
				}
			} else {
				this.binding.serverInfoPep.setText(R.string.server_info_unavailable);
			}
			if (features.httpUpload(0)) {
				this.binding.serverInfoHttpUpload.setText(R.string.server_info_available);
			} else if (features.p1S3FileTransfer()) {
				this.binding.serverInfoHttpUploadDescription.setText(R.string.p1_s3_filetransfer);
				this.binding.serverInfoHttpUpload.setText(R.string.server_info_available);
			} else {
				this.binding.serverInfoHttpUpload.setText(R.string.server_info_unavailable);
			}

			this.binding.pushRow.setVisibility(xmppConnectionService.getPushManagementService().isStub() ? View.GONE : View.VISIBLE);

			if (xmppConnectionService.getPushManagementService().available(mAccount)) {
				this.binding.serverInfoPush.setText(R.string.server_info_available);
			} else {
				this.binding.serverInfoPush.setText(R.string.server_info_unavailable);
			}
			final long pgpKeyId = this.mAccount.getPgpId();
			if (pgpKeyId != 0 && Config.supportOpenPgp()) {
				OnClickListener openPgp = view -> launchOpenKeyChain(pgpKeyId);
				OnClickListener delete = view -> showDeletePgpDialog();
				this.mPgpFingerprintBox.setVisibility(View.GONE);
				this.mPgpFingerprint.setText(OpenPgpUtils.convertKeyIdToHex(pgpKeyId));
				this.mPgpFingerprint.setOnClickListener(openPgp);
				if ("pgp".equals(messageFingerprint)) {
					this.getmPgpFingerprintDesc.setTextAppearance(this, R.style.TextAppearance_Conversations_Caption_Highlight);
				}
				this.getmPgpFingerprintDesc.setOnClickListener(openPgp);
				this.mPgpDeleteFingerprintButton.setOnClickListener(delete);
			} else {
				this.mPgpFingerprintBox.setVisibility(View.GONE);
			}
			final String ownAxolotlFingerprint = this.mAccount.getAxolotlService().getOwnFingerprint();
			if (ownAxolotlFingerprint != null && Config.supportOmemo()) {
				this.mAxolotlFingerprintBox.setVisibility(View.VISIBLE);
				if (ownAxolotlFingerprint.equals(messageFingerprint)) {
					this.mOwnFingerprintDesc.setTextAppearance(this, R.style.TextAppearance_Conversations_Caption_Highlight);
					this.mOwnFingerprintDesc.setText(R.string.glacier_id_selected_message);
				} else {
					this.mOwnFingerprintDesc.setTextAppearance(this, R.style.TextAppearance_Conversations_Caption);
					this.mOwnFingerprintDesc.setText(R.string.glacier_id);
				}
				this.mAxolotlFingerprint.setText(CryptoHelper.prettifyFingerprint(ownAxolotlFingerprint.substring(2)));
				this.mAxolotlFingerprintToClipboardButton.setVisibility(View.GONE);
				this.mAxolotlFingerprintToClipboardButton.setOnClickListener(v -> copyOmemoFingerprint(ownAxolotlFingerprint));
			} else {
				this.mAxolotlFingerprintBox.setVisibility(View.GONE);
			}
			boolean hasKeys = false;
			keys.removeAllViews();
			for (XmppAxolotlSession session : mAccount.getAxolotlService().findOwnSessions()) {
				if (!session.getTrust().isCompromised()) {
					boolean highlight = session.getFingerprint().equals(messageFingerprint);
					addFingerprintRow(keys, session, highlight);
					hasKeys = true;
				}
			}
			if (hasKeys && Config.supportOmemo()) {
				this.binding.otherDeviceKeysCard.setVisibility(View.VISIBLE);
				Set<Integer> otherDevices = mAccount.getAxolotlService().getOwnDeviceIds();
				if (otherDevices == null || otherDevices.isEmpty()) {
					mClearDevicesButton.setVisibility(View.GONE);
				} else {
					mClearDevicesButton.setVisibility(View.VISIBLE);
				}
			} else {
				this.binding.otherDeviceKeysCard.setVisibility(View.GONE);
			}
		} else {
			final TextInputLayout errorLayout;
			if (this.mAccount.errorStatus()) {
				// COGNITO - close wait dialog and clear password
				closeWaitDialog();
				this.binding.accountJid.getEditableText().clear();
				mJid.setText("");
				mPassword.setText("");

				if (this.mAccount.getStatus() == Account.State.UNAUTHORIZED) {
					errorLayout = this.mAccountPasswordLayout;
				} else if (mShowOptions
						&& this.mAccount.getStatus() == Account.State.SERVER_NOT_FOUND
						&& this.mHostname.getText().length() > 0) {
					errorLayout = this.mHostnameLayout;
				} else {
					errorLayout = this.mAccountJidLayout;
				}
				errorLayout.setError(getString(this.mAccount.getStatus().getReadableId()));
				if (init || !accountInfoEdited()) {
					errorLayout.requestFocus();
				}
			} else {
				errorLayout = null;
			}

			binding.accountJid.setText(mAccount.getDisplayName());

			removeErrorsOnAllBut(errorLayout);
			this.binding.stats.setVisibility(View.GONE);
			this.binding.otherDeviceKeysCard.setVisibility(View.GONE);
		}
	}

	private void removeErrorsOnAllBut(TextInputLayout exception) {
		if (this.mAccountJidLayout != exception) {
			this.mAccountJidLayout.setErrorEnabled(false);
			this.mAccountJidLayout.setError(null);
		}
		if (this.mAccountPasswordLayout != exception) {
			this.mAccountPasswordLayout.setErrorEnabled(false);
			this.mAccountPasswordLayout.setError(null);
		}
		if (this.mHostnameLayout != exception) {
			this.mHostnameLayout.setErrorEnabled(false);
			this.mHostnameLayout.setError(null);
		}
		if (this.mPortLayout != exception) {
			this.mPortLayout.setErrorEnabled(false);
			this.mPortLayout.setError(null);
		}
	}

	private void showDeletePgpDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.unpublish_pgp);
		builder.setMessage(R.string.unpublish_pgp_message);
		builder.setNegativeButton(R.string.cancel, null);
		builder.setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
			mAccount.setPgpSignId(0);
			mAccount.unsetPgpSignature();
			xmppConnectionService.databaseBackend.updateAccount(mAccount);
			xmppConnectionService.sendPresence(mAccount);
			refreshUiReal();
		});
		builder.create().show();
	}

	private void showOsOptimizationWarning(boolean showBatteryWarning, boolean showDataSaverWarning) {
		this.binding.osOptimization.setVisibility(showBatteryWarning || showDataSaverWarning ? View.VISIBLE : View.GONE);
		if (showDataSaverWarning) {
			this.binding.osOptimizationHeadline.setText(R.string.data_saver_enabled);
			this.getmDisableOsOptimizationsBody.setText(R.string.data_saver_enabled_explained);
			this.mDisableOsOptimizationsButton.setText(R.string.allow);
			this.mDisableOsOptimizationsButton.setOnClickListener(v -> {
				Intent intent = new Intent(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS);
				Uri uri = Uri.parse("package:" + getPackageName());
				intent.setData(uri);
				try {
					startActivityForResult(intent, REQUEST_DATA_SAVER);
				} catch (ActivityNotFoundException e) {
					Toast.makeText(EditAccountActivity.this, R.string.device_does_not_support_data_saver, Toast.LENGTH_SHORT).show();
				}
			});
		} else if (showBatteryWarning) {
			this.mDisableOsOptimizationsButton.setText(R.string.disable);
			this.binding.osOptimizationHeadline.setText(R.string.battery_optimizations_enabled);
			this.getmDisableOsOptimizationsBody.setText(R.string.battery_optimizations_enabled_explained);
			this.mDisableOsOptimizationsButton.setOnClickListener(v -> {
				Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
				Uri uri = Uri.parse("package:" + getPackageName());
				intent.setData(uri);
				try {
					startActivityForResult(intent, REQUEST_BATTERY_OP);
				} catch (ActivityNotFoundException e) {
					Toast.makeText(EditAccountActivity.this, R.string.device_does_not_support_battery_op, Toast.LENGTH_SHORT).show();
				}
			});
		}
	}

	public void showWipePepDialog() {
		Builder builder = new Builder(this);
		builder.setTitle(getString(R.string.clear_other_devices));
		builder.setIconAttribute(android.R.attr.alertDialogIcon);
		builder.setMessage(getString(R.string.clear_other_devices_desc));
		builder.setNegativeButton(getString(R.string.cancel), null);
		builder.setPositiveButton(getString(R.string.accept),
				(dialog, which) -> mAccount.getAxolotlService().wipeOtherPepDevices());
		builder.create().show();
	}

	private void editMamPrefs() {
		this.mFetchingMamPrefsToast = Toast.makeText(this, R.string.fetching_mam_prefs, Toast.LENGTH_LONG);
		this.mFetchingMamPrefsToast.show();
		xmppConnectionService.fetchMamPreferences(mAccount, this);
	}

	@Override
	public void onKeyStatusUpdated(AxolotlService.FetchStatus report) {
		refreshUi();
	}

	@Override
	public void onCaptchaRequested(final Account account, final String id, final Data data, final Bitmap captcha) {
		runOnUiThread(() -> {
			if (mCaptchaDialog != null && mCaptchaDialog.isShowing()) {
				mCaptchaDialog.dismiss();
			}
			final Builder builder = new Builder(EditAccountActivity.this);
			final View view = getLayoutInflater().inflate(R.layout.captcha, null);
			final ImageView imageView = view.findViewById(R.id.captcha);
			final EditText input = view.findViewById(R.id.input);
			imageView.setImageBitmap(captcha);

			builder.setTitle(getString(R.string.captcha_required));
			builder.setView(view);

			builder.setPositiveButton(getString(R.string.ok),
					(dialog, which) -> {
						String rc = input.getText().toString();
						data.put("username", account.getUsername());
						data.put("password", account.getPassword());
						data.put("ocr", rc);
						data.submit();

						if (xmppConnectionServiceBound) {
							xmppConnectionService.sendCreateAccountWithCaptchaPacket(account, id, data);
						}
					});
			builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
				if (xmppConnectionService != null) {
					xmppConnectionService.sendCreateAccountWithCaptchaPacket(account, null, null);
				}
			});

			builder.setOnCancelListener(dialog -> {
				if (xmppConnectionService != null) {
					xmppConnectionService.sendCreateAccountWithCaptchaPacket(account, null, null);
				}
			});
			mCaptchaDialog = builder.create();
			mCaptchaDialog.show();
			input.requestFocus();
		});
	}

	public void onShowErrorToast(final int resId) {
		runOnUiThread(() -> Toast.makeText(EditAccountActivity.this, resId, Toast.LENGTH_SHORT).show());
	}

	@Override
	public void onPreferencesFetched(final Element prefs) {
		runOnUiThread(() -> {
			if (mFetchingMamPrefsToast != null) {
				mFetchingMamPrefsToast.cancel();
			}
			Builder builder = new Builder(EditAccountActivity.this);
			builder.setTitle(R.string.server_side_mam_prefs);
			String defaultAttr = prefs.getAttribute("default");
			final List<String> defaults = Arrays.asList("never", "roster", "always");
			final AtomicInteger choice = new AtomicInteger(Math.max(0, defaults.indexOf(defaultAttr)));
			builder.setSingleChoiceItems(R.array.mam_prefs, choice.get(), (dialog, which) -> choice.set(which));
			builder.setNegativeButton(R.string.cancel, null);
			builder.setPositiveButton(R.string.ok, (dialog, which) -> {
				prefs.setAttribute("default", defaults.get(choice.get()));
				xmppConnectionService.pushMamPreferences(mAccount, prefs);
			});
			builder.create().show();
		});
	}

	/**
	 * Add default DOMAIN if not specified by user
	 *
	 * @param _accountJID
	 * @return
	 */
	public String getFullyQualifiedJid(String _accountJID) {
		if (_accountJID.contains("@") == true) {
			return _accountJID;
		} else {
			return _accountJID + "@" + StartConversationActivity.DOMAIN_IP;
		}
	}

	/**
	 * Return first portion of fully qualified name
	 *
	 * @param _qualifiedUsername
	 * @return
	 */
	public String getCustomUsername(String _qualifiedUsername) {
		if (_qualifiedUsername.contains("@" + StartConversationActivity.DOMAIN_IP)) {
			return _qualifiedUsername.split("@" + StartConversationActivity.DOMAIN_IP)[0];
		}
		return _qualifiedUsername;
	}

	@Override
	public void onPreferencesFetchFailed() {
		runOnUiThread(() -> {
			if (mFetchingMamPrefsToast != null) {
				mFetchingMamPrefsToast.cancel();
			}
			Toast.makeText(EditAccountActivity.this, R.string.unable_to_fetch_mam_prefs, Toast.LENGTH_LONG).show();
		});
	}

	@Override
	public void OnUpdateBlocklist(Status status) {
		refreshUi();
	}

	/**
	 * *********** COGNITO MODIFICATIONS **************
 	 */
	/**
	 * COGNITO - gather login info and report any errors
	 *
	 * @param view
	 */
	public void logIn(View view) {
		if ((mLoginButton.getText().toString().compareTo(getString(R.string.login_button_label))) == 0) {
			// log into Cognito and then messenger
			username = binding.accountJid.getText().toString();
			password = mPassword.getText().toString();
			organization = mOrgID.getText().toString();

			// make sure all fields are filled in before logging in
			if (username.trim().length() == 0) {
				// showDialogMessage("Login", "Username cannot be empty.");
				mAccountJidLayout.setError("Username cannot be blank");
				mAccountJidLayout.requestFocus();
			} else if (password.trim().length() == 0) {
				// showDialogMessage("Login", "Password cannot be empty.");
				mPassword.setError("Password cannot be blank");
				mPassword.requestFocus();
			} else if (organization.trim().length() == 0) {
				// showDialogMessage("Login", "Organization cannot be empty.");
				mOrgID.setError("Org ID cannot be blank");
				mOrgID.requestFocus();
			} else {
				showWaitDialog(getString(R.string.wait_dialog_logging_in));
				signInUser();
			}
		} else if ((mLoginButton.getText().toString().compareTo(getString(R.string.continue_button_label))) == 0) {
			// assume logged into Cognito.  Log into Messenger
			showWaitDialog(getString(R.string.wait_dialog_retrieving_account_info));
			//autoLoginMessenger();
		} else if ((mLoginButton.getText().toString().compareTo(getString(R.string.retry_button_label))) == 0) {
			showWaitDialog(getString(R.string.wait_dialog_logging_in));
			signInUser();
		}
	}

	/**
	 * COGNITO - login AWS
	 */
	private void signInUser() {
		AppHelper.setUser(username);
		AppHelper.getPool().getUser(username).getSessionInBackground(authenticationHandler);
	}

	/**
	 * Callbacks
	 */
	AuthenticationHandler authenticationHandler = new AuthenticationHandler() {
		@Override
		public void onSuccess(CognitoUserSession cognitoUserSession, CognitoDevice device) {
			Log.d("GOOBER", " -- Auth Success");
			AppHelper.setCurrSession(cognitoUserSession);
			AppHelper.newDevice(device);
			// closeWaitDialog();

			// username/password is correct.  Now check if bucket exists
			if (organization != null) {
				// Clear list of keys before logging in
				keyList.clear();

				// try to list objects in directory
				downloadS3Files();

				// This is where we call download file
				launchUser();
			} else {
				closeWaitDialog();
				showDialogMessage(getString(R.string.signin_fail_title), "Invalid Org ID");
			}
		}

		@Override
		public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String username) {
			// closeWaitDialog();
			Locale.setDefault(Locale.US);
			getUserAuthentication(authenticationContinuation, username);
		}

		private void getUserAuthentication(AuthenticationContinuation continuation, String username) {
			//closeWaitDialog();
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
			// COGNITO - do nothing
		}

		@Override
		public void onFailure(Exception e) {
			closeWaitDialog();

			showDialogMessage(getString(R.string.error_connecting), getString(R.string.unknown_login_error));

			// Go back to login screen
			setLoginContentView();
		}

		@Override
		public void authenticationChallenge(ChallengeContinuation continuation) {
			/**
			 * For Custom authentication challenge, implement your logic to present challenge to the
			 * user and pass the user's responses to the continuation.
			 */
		}
	};

	private IOpenVPNStatusCallback mCallback = new IOpenVPNStatusCallback.Stub() {
		/**
		 * This is called by the remote service regularly to tell us about
		 * new values.  Note that IPC calls are dispatched through a thread
		 * pool running in each process, so the code executing here will
		 * NOT be running in our main thread like most other things -- so,
		 * to update the UI, we need to use a Handler to hop over there.
		 */

		@Override
		public void newStatus(String uuid, String state, String message, String level)
				throws RemoteException {
			Message msg = Message.obtain(mHandler, MSG_UPDATE_STATE, state + "|" + message);
			msg.sendToTarget();

			// COGNITO - save current uuid so we can compare
			currentProfileUUID = uuid;

			// COGNITO - Retrieve name of uuid and set the profile text
			/* String profileName = getProfileName(uuid);
			if (profileName != null) {
				currentProfile.setText(profileName);
			}*/
		}
	};

	/**
	 * Listener to know when files are done downloading (ie profiles, account info)
	 */
	private class DownloadListener implements TransferListener {
		String key;

		public DownloadListener(String key) {
			super();

			this.key = key;
		}
		@Override
		public void onError(int id, Exception e) {
			Log.d("GOOBER", "Error during download (" + key + "): " + id, e);
			// s3DownloadInterface.inDownloadError(e.toString());
		}

		@Override
		public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
			Log.d("GOOBER", String.format("onProgressChanged (" + key + "): %d, total: %d, current: %d", id, bytesCurrent, bytesTotal));
		}

		@Override
		public void onStateChanged(int id, TransferState newState) {
			Log.d("GOOBER", "onStateChanged(" + key + "): " + id + "," + newState);
			if (newState == TransferState.COMPLETED) {
				this.toString();

				// File destFile = new File(Environment.getExternalStorageDirectory() + "/dave.glacier");
				// readFile(new File(Environment.getExternalStorageDirectory() + "/" + key));

				File tmpFile = new File(Environment.getExternalStorageDirectory() + "/" + key);
				if (tmpFile.exists()) {
					// track how many have completed download
					downloadCount--;
					Log.d("GOOBER", "File confirmed: " + Environment.getExternalStorageDirectory() + "/" + key);

					if (key.endsWith("glacier") == true) {
						// save cognito information and account information
						BackupAccountManager backupAccountManager = new BackupAccountManager(getApplicationContext());
						backupAccountManager.createAppConfigFile(tmpFile, username, password, organization, BackupAccountManager.LOCATION_PUBLIC, BackupAccountManager.APPTYPE_VOICE);
						backupAccountManager.createAppConfigFile(tmpFile, username, password, organization, BackupAccountManager.LOCATION_PUBLIC, BackupAccountManager.APPTYPE_MESSENGER);
						tmpFile.delete();
					} else if (key.endsWith("ovpn") == true) {
						// move file
						Log.d("GOOBER", "Key Count (COMPLETED): " + downloadCount);
						moveFile(Environment.getExternalStorageDirectory().toString(), key, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
						// Rather than exporting the file immediately, keep list of files to export
						// exportProfile(key);
						keyList.add(key);
					}

					// check if finished download all files
					if (downloadCount == 0) {
						// COGNITO - remove all profiles for fresh start
						// commented out b/c we want to keep any existing profiles
						// not being replaced.
						// deleteExistingProfiles();

						// replace existing profiles for Core and add
						// those that aren't in Core
						addVPNProfiles();

						// fill out login fields
						restoreAccountsFromFile();

						// check if we're suppose to use vpn
						if ((mConnectionType == null) || ((mConnectionType != null) && (mConnectionType.compareTo("openvpn") == 0))) {
							showVPNProfileDialog();
						} else {
							// closeWaitDialog();
							showWaitDialog(getString(R.string.wait_dialog_retrieving_account_info));
							autoLoginMessenger();
						}
					}

				} else {
					Log.d("GOOBER", "File unconfirmed: " + Environment.getExternalStorageDirectory() + "/" + key);
				}
				// s3DownloadInterface.onDownloadSuccess("Success");
			}
		}
	}

	/**
	 * COGNITO - move file to different directory
	 *
	 * @param inputPath
	 * @param inputFile
	 * @param outputPath
	 */
	private void moveFile(String inputPath, String inputFile, String outputPath) {

		InputStream in = null;
		OutputStream out = null;
		try {

			//create output directory if it doesn't exist
			File dir = new File (outputPath);
			if (!dir.exists())
			{
				dir.mkdirs();
			}


			in = new FileInputStream(inputPath + "/" + inputFile);
			out = new FileOutputStream(outputPath + "/" + inputFile);

			byte[] buffer = new byte[1024];
			int read;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
			in.close();
			in = null;

			// write the output file
			out.flush();
			out.close();
			out = null;

			// delete the original file
			new File(inputPath + "/" + inputFile).delete();
		}

		catch (FileNotFoundException fnfe1) {
			Log.e("tag", fnfe1.getMessage());
		}
		catch (Exception e) {
			Log.e("tag", e.getMessage());
		}
	}

	/**
	 * COGNITO - Currently not used but left it here in
	 * case we want to delete all profiles for a fresh start for
	 * a user.
	 *
	 * Delete all the profiles from Core
	 */
	private void deleteExistingProfiles() {
		try {
			if (mService != null) {
				// disconnect VPN first
				mService.disconnect();

				List<APIVpnProfile> list = mService.getProfiles();

				// check if profile exists and delete it
				for (APIVpnProfile prof : list) {
					mService.removeProfile(prof.mUUID);
				}
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * COGNITO - Delete all the profiles from Core
	 */
	private void addVPNProfiles() {
		boolean profileFound = false;
		try {
			// make sure service exists
			if (mService != null) {
				// get profiles
				List<APIVpnProfile> list = mService.getProfiles();

				// compare profiles from AWS with what is in Core
				for (int i = 0; i < keyList.size(); i++) {

					// retrieve AWS profile name (aws names have ".ovpn" extension
					String key = keyList.get(i);
					profileFound = false;

					// cycle through Core profiles and compare with AWS profiles
					for (APIVpnProfile prof : list) {

						// check if names match and if so, replace it
						if (key.compareTo(prof.mName + ".ovpn") == 0 ) {
							mService.removeProfile(prof.mUUID);
							exportProfile(key);
							profileFound = true;

							// check if what we have is also current running profile because
							// we will need to stop it and restart it
							if ((currentProfileUUID != null) && (currentProfileUUID.compareTo(prof.mUUID) == 0)) {
								currentProfileName = prof.mName;
							}
							// found it, break out of loop
							break;
						}



						/*if (currentProfileUUID != null) {
							if (currentProfileUUID.compareTo(prof.mUUID) == 0) {
								// get name since we don't know if new profile will have same uuid
								currentProfileName = prof.mName;
							}
						} */

						// if profile exists, remove old profile and add new one
						// NOTE: we will disconnect.  We will stop the old and restart the new one later
						/* if (profileExists(prof.mName)) {
							mService.removeProfile(prof.mUUID);
							exportProfile(prof.mName + ".ovpn");
						} else {
							exportProfile(prof.mName + ".ovpn");
						}*/
					}

					if (!profileFound) {
						exportProfile(key);
					}
				}
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Check if old profile still exists currently
	 *
	 * @param oldProfile
	 *
	 * @return
	 */
	private boolean profileExists(String oldProfile) {
		String oldProfileName = oldProfile + ".ovpn";
		for (int i = 0; i < keyList.size(); i++) {
			String key = keyList.get(i);
			if (oldProfileName.compareTo(keyList.get(i)) == 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Add new profile list
	 */
	private void addNewProfileList() {
		for (int i = 0; i < keyList.size(); i++) {
			String key = keyList.get(i);
			exportProfile(key);
		}
	}

	/**
	 * Export profile to Core
	 *
	 * @param inputFile
	 */
	private void exportProfile(String inputFile) {
		try {
			File location = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
			location = new File(location.toString() + "/" + inputFile);

			FileInputStream config2 = new FileInputStream(location.toString());
			InputStreamReader isr = new InputStreamReader(config2);
			BufferedReader br = new BufferedReader(isr);
			String config="";
			String line;
			while(true) {
				line = br.readLine();
				if(line == null)
					break;
				config += line + "\n";
			}
			br.readLine();
			br.close();
			isr.close();
			config2.close();

			// strip off extension from end of filename
			String profileName = inputFile.substring(0, inputFile.length()-".ovpn".length());

			// add profile to GlacierCore
			addToCore(profileName, config);

			// delete profile
			location.delete();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Add profile to Glacier Core
	 *
	 * @param profile
	 * @param config
	 * @return
	 */
	/* private boolean addVPNProfile(String profile, String config) {
		try {

			if (mService != null) {
				List<APIVpnProfile> list = mService.getProfiles();

				// check if profile exists and delete it
				for (APIVpnProfile prof : list) {
					if (prof.mName.compareTo(profile) == 0) {
						mService.removeProfile(prof.mUUID);
					}
				}

				// add vpn profile
				mService.addNewVPNProfile(profile, true, config);
				return true;
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return false;
	}*/

	private void launchUser() {
		// reset valoues
		download_keys = null;

		// sign out current user
		cognitoCurrentUserSignout();

		mPassword.setText("");
		mOrgID.setText("");
	}

	/**
	 * Restore accounts from file
	 */
	private boolean restoreAccountsFromFile() {
		Log.d("GOOBER", "Permissions granted!!");
		BackupAccountManager backupAccountManager = new BackupAccountManager(getApplicationContext());

		// TODO: Need to change AccountManager to BackupAccountManager!!
		if (backupAccountManager.accountFileExists(BackupAccountManager.LOCATION_PUBLIC, BackupAccountManager.APPTYPE_MESSENGER)) {
			// retreive account information
			BackupAccountManager.AccountInfo accountInfo = backupAccountManager.getAccountInfo(BackupAccountManager.LOCATION_PUBLIC, BackupAccountManager.APPTYPE_MESSENGER);
			if (accountInfo != null) {

				// retrieve account information
				ArrayList<BackupAccountManager.Account> accounts = accountInfo.getAccounts();

				// should only be one account...
				for (int i = 0; i < accounts.size(); i++) {
					BackupAccountManager.Account tmpAccount = accounts.get(i);

					// Auto login with username/password
					String goober = tmpAccount.getAttribute(BackupAccountManager.USERNAME_KEY);
					binding.accountJid.setText(getFullyQualifiedJid(tmpAccount.getAttribute(BackupAccountManager.USERNAME_KEY))); // + "@" + StartConversationActivity.DOMAIN_IP);
					mPassword.setText(tmpAccount.getAttribute(BackupAccountManager.PASSWORD_KEY));
					mOrgID.setText(tmpAccount.getAttribute(BackupAccountManager.EXTERNALNUMBER_KEY));
					mConnectionType = tmpAccount.getAttribute(BackupAccountManager.CONNECTION_KEY);

					// TEST - Move/Don't move account file.  Used to test auto login feature.
					backupAccountManager.copyAccountFileFromPublicToPrivate(BackupAccountManager.APPTYPE_MESSENGER);
					backupAccountManager.deleteAccountFile(BackupAccountManager.LOCATION_PUBLIC, BackupAccountManager.APPTYPE_MESSENGER);

					return true;
				}
			}
		}

		return false;
	}


	/**
	 * add new profile to Core
	 *
	 * @param profile
	 * @param config
	 * @return
	 */
	private boolean addToCore(String profile, String config) {
		try {
			// we assume Core is clean
			if (mService != null) {
				// add vpn profile
				mService.addNewVPNProfile(profile, true, config);
				return true;
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * show components of login screen
	 */
	private void setLoginContentView() {
		// reset
		// clear s3bucket client
		Util.clearS3Client(getApplicationContext());

		username = null;
		password = null;
		organization = null;

		// set content
		this.binding.accountJid.getEditableText().clear();
		mPassword.setText("");
		mOrgID.setText("");

		// set visibility
		mAccountJidLayout.setVisibility(View.VISIBLE);
		mAccountJidLayout.setError(null);
		mAccountPasswordLayout.setVisibility(View.VISIBLE);
		mAccountPasswordLayout.setError(null);
		mOrgID.setVisibility(View.VISIBLE);
		mOrgID.setError(null);

		mLoginButton.setText(getString(R.string.login_button_label));
		this.binding.accountJid.requestFocus();
	}

	private void downloadS3Files() {
		String bucketName = Constants.BUCKET_NAME.replace(REPLACEMENT_ORG_ID,organization);
		AmazonS3 sS3Client = Util.getS3Client(getApplicationContext());
		TransferUtility transferUtility = Util.getTransferUtility(getApplicationContext(), bucketName);

		try {
			// with correct login, I can get that bucket exists
			if (sS3Client.doesBucketExist(bucketName)) {
				List<S3ObjectSummary> objectListing = sS3Client.listObjects(bucketName, Constants.KEY_PREFIX).getObjectSummaries();
				for (S3ObjectSummary summary : objectListing) {
					Log.d("GOOBER", "Keys found in S3 Bucket (" + summary.getBucketName() + "): " + summary.getKey());
					String tmpString = stripDirectory(summary.getKey().toString());

					if ((summary.getKey().contains("_" + username + ".ovpn") || (tmpString.compareTo(username + ".glacier") == 0))) {
						Log.d("GOOBER", "File we want to download: " + summary.getKey());
						String destFilename = summary.getKey().substring(Constants.KEY_PREFIX.length() + 1, summary.getKey().length());

						// bump the number of files to download
						downloadCount++;

						File destFile = new File(Environment.getExternalStorageDirectory() + "/" + destFilename);
						TransferObserver observer = transferUtility.download(summary.getKey(), destFile, new DownloadListener(destFilename));
						if (download_keys == null) {
							download_keys = destFilename;
						} else {
							download_keys = download_keys + "\n" + destFilename;
						}
					}
				}
			} else {
				closeWaitDialog();
				showDialogMessage(getString(R.string.signin_fail_title), getString(R.string.login_error_message));
			}
		} catch (AmazonS3Exception ase) {
			//
		} catch (AmazonServiceException ase) {
			//
		} catch (AmazonClientException ace) {
			//
		}
	}

	/**
	 * strip off derectory and return filename
	 *
	 * @param value
	 * @return
	 */
	private String stripDirectory(String value) {
		String tmpStringArray[] = value.split("/");

		if (tmpStringArray.length > 1) {
			return tmpStringArray[tmpStringArray.length -1];
		} else {
			return tmpStringArray[0];
		}
	}


	public void closeWaitDialog() {
		if (waitDialog != null) {
			waitDialog.dismiss();
		}
	}

	public void showWaitDialog(String message) {
		LayoutInflater inflater = getLayoutInflater();
		View layout = inflater.inflate(R.layout.dialog_wait,null);
		TextView textField = layout.findViewById(R.id.status_message);
		textField.setText(message);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(layout);
		builder.setCancelable(false); // if you want user to wait for some process to finish,
		builder.setTitle(getString(R.string.wait_dialog_title));

		waitDialog = builder.create();
		waitDialog.show();
	}

	private void showDialogMessage(String title, String body) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(title).setMessage(body).setNeutralButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				try {
					userDialog.dismiss();
				} catch (Exception e) {
					//
				}
			}
		});
		userDialog = builder.create();
		userDialog.show();
	}

	/**
	 * display profile spinner...maybe...hopefullly
	 */
	private void showVPNProfileDialog() {
		// retrieve list of vpn and pick one to start
		try {
			closeWaitDialog();

			if (mService != null) {
				// retrieve and sort list
				List<APIVpnProfile> list = mService.getProfiles();
				Collections.sort(list);
				OpenVPNProfileDialog dialog = new OpenVPNProfileDialog(this, list);
				dialog.show();
			} else {
				// DO SOMETHING?
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * COGNITO - logout of Cognito
	 * sometimes if it's been too long, I believe pool doesn't
	 * exists and user is no longer logged in
	 */
	private void cognitoCurrentUserSignout() {
		CognitoUserPool userPool = AppHelper.getPool();
		if (userPool != null) {
			CognitoUser user = userPool.getCurrentUser();
			if (user != null) {
				user.signOut();
			}
		}
	}

	/**
	 * COGNITO - log in automatically to messenger with given username/password
	 */
	private void autoLoginMessenger() {
		final String password = mPassword.getText().toString();
		final boolean wasDisabled = mAccount != null && mAccount.getStatus() == Account.State.DISABLED;

		if (!mInitMode && passwordChangedInMagicCreateMode()) {
			gotoChangePassword(password);
			return;
		}
		if (mInitMode && mAccount != null) {
			mAccount.setOption(Account.OPTION_DISABLED, false);
		}
		if (mAccount != null && mAccount.getStatus() == Account.State.DISABLED && !accountInfoEdited()) {
			mAccount.setOption(Account.OPTION_DISABLED, false);
			if (!xmppConnectionService.updateAccount(mAccount)) {
				Toast.makeText(EditAccountActivity.this, R.string.unable_to_update_account, Toast.LENGTH_SHORT).show();
			}
			return;
		}
		final boolean registerNewAccount = binding.accountRegisterNew.isChecked() && !Config.DISALLOW_REGISTRATION_IN_UI;
		if (mUsernameMode && binding.accountJid.getText().toString().contains("@")) {
			mAccountJidLayout.setError(getString(R.string.invalid_username));
			removeErrorsOnAllBut(mAccountJidLayout);
			binding.accountJid.requestFocus();
			return;
		}

		XmppConnection connection = mAccount == null ? null : mAccount.getXmppConnection();
		boolean openRegistrationUrl = registerNewAccount && mAccount != null && mAccount.getStatus() == Account.State.REGISTRATION_WEB;
		boolean openPaymentUrl = mAccount != null && mAccount.getStatus() == Account.State.PAYMENT_REQUIRED;
		final boolean redirectionWorthyStatus = openPaymentUrl || openRegistrationUrl;
		URL url = connection != null && redirectionWorthyStatus ? connection.getRedirectionUrl() : null;
		if (url != null && !wasDisabled) {
			try {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url.toString())));
				return;
			} catch (ActivityNotFoundException e) {
				Toast.makeText(EditAccountActivity.this, R.string.application_found_to_open_website, Toast.LENGTH_SHORT).show();
				return;
			}
		}

		final Jid jid;
		try {
			if (mUsernameMode) {
				jid = Jid.of(binding.accountJid.getText().toString(), getUserModeDomain(), null);
			} else {
				jid = Jid.of(binding.accountJid.getText().toString());
			}
		} catch (final NullPointerException | IllegalArgumentException e) {
			if (mUsernameMode) {
				mAccountJidLayout.setError(getString(R.string.invalid_username));
			} else {
				mAccountJidLayout.setError(getString(R.string.invalid_jid));
			}
			binding.accountJid.requestFocus();
			removeErrorsOnAllBut(mAccountJidLayout);
			return;
		}
		String hostname = null;
		int numericPort = 5222;
		if (mShowOptions) {
			hostname = mHostname.getText().toString().replaceAll("\\s", "");
			final String port = mPort.getText().toString().replaceAll("\\s", "");
			if (hostname.contains(" ")) {
				mHostnameLayout.setError(getString(R.string.not_valid_hostname));
				mHostname.requestFocus();
				removeErrorsOnAllBut(mHostnameLayout);
				return;
			}
			try {
				numericPort = Integer.parseInt(port);
				if (numericPort < 0 || numericPort > 65535) {
					mPortLayout.setError(getString(R.string.not_a_valid_port));
					removeErrorsOnAllBut(mPortLayout);
					mPort.requestFocus();
					return;
				}

			} catch (NumberFormatException e) {
				mPortLayout.setError(getString(R.string.not_a_valid_port));
				removeErrorsOnAllBut(mPortLayout);
				mPort.requestFocus();
				return;
			}
		}

		if (jid.getLocal() == null) {
			if (mUsernameMode) {
				mAccountJidLayout.setError(getString(R.string.invalid_username));
			} else {
				mAccountJidLayout.setError(getString(R.string.invalid_jid));
			}
			removeErrorsOnAllBut(mAccountJidLayout);
			binding.accountJid.requestFocus();
			return;
		}
		if (mAccount != null) {
			if (mInitMode && mAccount.isOptionSet(Account.OPTION_MAGIC_CREATE)) {
				mAccount.setOption(Account.OPTION_MAGIC_CREATE, mAccount.getPassword().contains(password));
			}
			mAccount.setJid(jid);
			mAccount.setPort(numericPort);
			mAccount.setHostname(hostname);
			mAccountJidLayout.setError(null);
			mAccount.setPassword(password);
			mAccount.setOption(Account.OPTION_REGISTER, registerNewAccount);
			if (!xmppConnectionService.updateAccount(mAccount)) {
				Toast.makeText(EditAccountActivity.this, R.string.unable_to_update_account, Toast.LENGTH_SHORT).show();
				return;
			}
		} else {
			if (xmppConnectionService.findAccountByJid(jid) != null) {
				mAccountJidLayout.setError(getString(R.string.account_already_exists));
				removeErrorsOnAllBut(mAccountJidLayout);
				binding.accountJid.requestFocus();
				return;
			}
			mAccount = new Account(jid.asBareJid(), password);
			mAccount.setPort(numericPort);
			mAccount.setHostname(hostname);
			mAccount.setOption(Account.OPTION_USETLS, true);
			mAccount.setOption(Account.OPTION_USECOMPRESSION, true);
			mAccount.setOption(Account.OPTION_REGISTER, registerNewAccount);
			xmppConnectionService.createAccount(mAccount);
		}
		mHostnameLayout.setError(null);
		mPortLayout.setError(null);
		if (mAccount.isEnabled()
				&& !registerNewAccount
				&& !mInitMode) {
			finish();
		} else {
			//updateSaveButton();
			updateAccountInformation(true);
		}
	}

	/**
	 * *********** CORE MODIFICATIONS **************
 	 */

	/**
	 * COGNITO - Return profile from OpenVPNProfileDialog where the user selects the profile
	 * they want to start
	 *
	 * @param vpnName
	 */
	@Override
	public void onReturnValue(String vpnName) {

		// if vpnName = null,  user cancelled dialog box
		// if currentProfileName is null, no profile was started
		// if vpnName is null and currentProfileName isn't, user wants to start currentProfileName
		if ((vpnName == null) && (currentProfileName != null)) {
			vpnName = currentProfileName;
		}

		// disconect VPN
		try {
			mService.disconnect();
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		// try to start up VPN if valid
		if (vpnName != null) {
			try {
				List<APIVpnProfile> list = mService.getProfiles();
				String mUUID = null;
				for (APIVpnProfile profile : list) {

					// find UUID based on name of profile you want to start
					if (profile.mName.compareTo(vpnName) == 0) {
						mUUID = profile.mUUID;
						break;
					}
				}

				// start UUID if valid
				if (mUUID != null) {
					mService.startProfile(mUUID);
				}

			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			// COGNITO - Go back to login screen if hit cancel on vpndialog box
			//setLoginContentView();

			// try logging in anyway in case user has own vpn running
			showWaitDialog(getString(R.string.wait_dialog_retrieving_account_info));
			autoLoginMessenger();
		}
	}

	@Override
	public boolean handleMessage(Message msg) {
		Log.d("GOOBER", "EditAccountActivity::handleMessage(): " + msg.obj.toString() + "::What = " + msg.what);

		if(msg.what == MSG_UPDATE_STATE) {
			if (msg.obj.toString().startsWith("CONNECTED")) {
				// check if vpn was in the process of coming up and is now connected
				if ((lastConnectionState == VPN_STATE_MISC) || (lastConnectionState == VPN_STATE_NOPROCESS)) {
					showWaitDialog(getString(R.string.wait_dialog_retrieving_account_info));

					// COGNITO - sleep before trying to log in.
					// Assumption is that backend still needs to do stuff
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					autoLoginMessenger();
				}
				lastConnectionState = VPN_STATE_CONNECTED;
			} else if (msg.obj.toString().startsWith("NOPROCESS")) {
				// no vpn running
				lastConnectionState = VPN_STATE_NOPROCESS;
			} else {
				// in the process of coming up
				lastConnectionState = VPN_STATE_MISC;
				showWaitDialog(getString(R.string.wait_dialog_connecting_vpn));
			}
		}
		return true;
	}

	private void bindService() {
		Intent icsopenvpnService = new Intent(IOpenVPNAPIService.class.getName());
		icsopenvpnService.setPackage("com.glaciersecurity.glaciercore");

		// ERROR - Reports error on occassion but doesn't seem to effect anything
		bindService(icsopenvpnService, mConnection, Context.BIND_AUTO_CREATE);
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className,
									   IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  We are communicating with our
			// service through an IDL interface, so get a client-side
			// representation of that from the raw service object.

			mService = IOpenVPNAPIService.Stub.asInterface(service);

			try {
				// Request permission to use the API
				Intent i = mService.prepare(getPackageName());
				if (i!=null) {
					startActivityForResult(i, ICS_OPENVPN_PERMISSION);
				} else {
					onActivityResult(ICS_OPENVPN_PERMISSION, Activity.RESULT_OK,null);
				}

				// COGNITO - trigger to get status/current running profile from Core
				try {
					mService.registerStatusCallback(mCallback);
				} catch (RemoteException e) {
					e.printStackTrace();
				}

			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			mService = null;
		}
	};

	/**
	 * PERMISSIONS - Ask for permissions
	 */
	private void askForPermissions() {
		final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;

		//String[] request = {Manifest.permission.READ_CONTACTS, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			List<String> permissionsNeeded = new ArrayList<String>();

			final List<String> permissionsList = new ArrayList<String>();
			// WRITE_EXTERNAL_STORAGE permission ahead of time so that it doesn't ask
			// when time comes which inevitably fails at that point.
			if (!addPermission(permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE))
				permissionsNeeded.add("Write Storage");
			// if (!addPermission(permissionsList, Manifest.permission.READ_EXTERNAL_STORAGE))
			//	permissionsNeeded.add("Read Storage");

			if (permissionsList.size() > 0) {
				if (permissionsNeeded.size() > 0) {
					// Need Rationale
					String message = "You need to grant access to " + permissionsNeeded.get(0);
					for (int i = 1; i < permissionsNeeded.size(); i++) {
						message = message + ", " + permissionsNeeded.get(i);
					}

					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
						requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
								REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
					}

					return;
				}
				requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
						REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);

				return;
			}
		}
	}

	/**
	 * PERMISSIONS - This is how we ensure that permissions are granted and then accounts are restored
	 *
	 * @param requestCode
	 * @param permissions
	 * @param grantResults
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		// restore accounts from file if exists
		if (restoreAccountsFromFile() == true) {
			showWaitDialog(getString(R.string.wait_dialog_retrieving_account_info));
			autoLoginMessenger();
		}
	}


	/**
	 * PERMISSIONS - add permission
	 *
	 * @param permissionsList
	 * @param permission
	 * @return
	 */
	private boolean addPermission(List<String> permissionsList, String permission) {

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (this.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
				permissionsList.add(permission);
				// Check for Rationale Option
				if (!shouldShowRequestPermissionRationale(permission))
					return false;
			}
			return true;
		}
		return false;
	}
}