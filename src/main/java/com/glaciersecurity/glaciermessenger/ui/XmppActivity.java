package com.glaciersecurity.glaciermessenger.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import androidx.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import androidx.annotation.BoolRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDelegate;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import com.glaciersecurity.glaciermessenger.entities.PresenceTemplate;
import com.glaciersecurity.glaciermessenger.lollipin.lib.PinActivity;
import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
//import com.glaciersecurity.glaciermessenger.crypto.PgpEngine;
import com.glaciersecurity.glaciermessenger.databinding.DialogQuickeditBinding;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.Contact;
import com.glaciersecurity.glaciermessenger.entities.Conversation;
import com.glaciersecurity.glaciermessenger.entities.Message;
import com.glaciersecurity.glaciermessenger.entities.Presences;
import com.glaciersecurity.glaciermessenger.services.AvatarService;
import com.glaciersecurity.glaciermessenger.services.BarcodeProvider;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService.XmppConnectionBinder;
import com.glaciersecurity.glaciermessenger.ui.service.EmojiService;
import com.glaciersecurity.glaciermessenger.ui.util.MenuDoubleTabUtil;
import com.glaciersecurity.glaciermessenger.ui.util.PendingItem;
import com.glaciersecurity.glaciermessenger.ui.util.PresenceSelector;
import com.glaciersecurity.glaciermessenger.ui.util.SoftKeyboardUtils;
import com.glaciersecurity.glaciermessenger.utils.ExceptionHelper;
import com.glaciersecurity.glaciermessenger.utils.ThemeHelper;
import com.glaciersecurity.glaciermessenger.xmpp.OnKeyStatusUpdated;
import com.glaciersecurity.glaciermessenger.xmpp.OnUpdateBlocklist;
import rocks.xmpp.addr.Jid;

import static android.Manifest.permission.READ_CONTACTS;

// PIN - extend to use PIN everywhere
// public abstract class XmppActivity extends ActionBarActivity {
public abstract class XmppActivity extends PinActivity {

	public static final String EXTRA_ACCOUNT = "account";
	protected static final int REQUEST_ANNOUNCE_PGP = 0x0101;
	protected static final int REQUEST_INVITE_TO_CONVERSATION = 0x0102;
	protected static final int REQUEST_CHOOSE_PGP_ID = 0x0103;
	protected static final int REQUEST_BATTERY_OP = 0x49ff;
	protected static final String CORE_APK_PACKAGE = "com.glaciersecurity.glaciercore";
	public XmppConnectionService xmppConnectionService;
	public boolean xmppConnectionServiceBound = false;

	private final PendingItem<PresenceTemplate> mPendingPresenceTemplate = new PendingItem<>();


	protected int mColorRed;

	protected static final String FRAGMENT_TAG_DIALOG = "dialog";

	private boolean isCameraFeatureAvailable = false;

	protected int mTheme;
	protected boolean mUsingEnterKey = false;
	protected Toast mToast;
	//public Runnable onOpenPGPKeyPublished = () -> Toast.makeText(XmppActivity.this, R.string.openpgp_has_been_published, Toast.LENGTH_SHORT).show();
	protected ConferenceInvite mPendingConferenceInvite = null;
	protected ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			XmppConnectionBinder binder = (XmppConnectionBinder) service;
			xmppConnectionService = binder.getService();
			xmppConnectionServiceBound = true;
			registerListeners();
			onBackendConnected();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			xmppConnectionServiceBound = false;
		}
	};
	private DisplayMetrics metrics;
	private long mLastUiRefresh = 0;
	private Handler mRefreshUiHandler = new Handler();
	private Runnable mRefreshUiRunnable = () -> {
		mLastUiRefresh = SystemClock.elapsedRealtime();
		refreshUiReal();
	};
	private UiCallback<Conversation> adhocCallback = new UiCallback<Conversation>() {
		@Override
		public void success(final Conversation conversation) {
			runOnUiThread(() -> {
				switchToConversation(conversation);
				hideToast();
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
	public boolean mSkipBackgroundBinding = false;

	public static boolean cancelPotentialWork(Message message, ImageView imageView) {
		final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

		if (bitmapWorkerTask != null) {
			final Message oldMessage = bitmapWorkerTask.message;
			if (oldMessage == null || message != oldMessage) {
				bitmapWorkerTask.cancel(true);
			} else {
				return false;
			}
		}
		return true;
	}

	private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
		if (imageView != null) {
			final Drawable drawable = imageView.getDrawable();
			if (drawable instanceof AsyncDrawable) {
				final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}

	protected void hideToast() {
		if (mToast != null) {
			mToast.cancel();
		}
	}

	protected void replaceToast(String msg) {
		replaceToast(msg, true);
	}

	protected void replaceToast(String msg, boolean showlong) {
		hideToast();
		mToast = Toast.makeText(this, msg, showlong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
		mToast.show();
	}

	protected final void refreshUi() {
		final long diff = SystemClock.elapsedRealtime() - mLastUiRefresh;
		if (diff > Config.REFRESH_UI_INTERVAL) {
			mRefreshUiHandler.removeCallbacks(mRefreshUiRunnable);
			runOnUiThread(mRefreshUiRunnable);
		} else {
			final long next = Config.REFRESH_UI_INTERVAL - diff;
			mRefreshUiHandler.removeCallbacks(mRefreshUiRunnable);
			mRefreshUiHandler.postDelayed(mRefreshUiRunnable, next);
		}
	}

	abstract protected void refreshUiReal();



	@Override
	protected void onStart() {
		super.onStart();
		if (!xmppConnectionServiceBound) {
			if (this.mSkipBackgroundBinding) {
				Log.d(Config.LOGTAG,"skipping background binding");
			} else {
				connectToBackend();
			}
		} else {
			this.registerListeners();
			this.onBackendConnected();
		}
	}

	public void connectToBackend() {
		Intent intent = new Intent(this, XmppConnectionService.class);
		intent.setAction("ui");
		try {
			startService(intent);
		}catch (IllegalStateException e) {
			Log.w(Config.LOGTAG,"unable to start service from "+getClass().getSimpleName());
		}
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (xmppConnectionServiceBound) {
			this.unregisterListeners();
			unbindService(mConnection);
			xmppConnectionServiceBound = false;
		}
	}


	/*public boolean hasPgp() {
		return xmppConnectionService.getPgpEngine() != null;
	}

	public void showInstallPgpDialog() {
		Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.openkeychain_required));
		builder.setIconAttribute(android.R.attr.alertDialogIcon);
		builder.setMessage(getText(R.string.openkeychain_required_long));
		builder.setNegativeButton(getString(R.string.cancel), null);
		builder.setNeutralButton(getString(R.string.restart),
				(dialog, which) -> {
					if (xmppConnectionServiceBound) {
						unbindService(mConnection);
						xmppConnectionServiceBound = false;
					}
					stopService(new Intent(XmppActivity.this,
							XmppConnectionService.class));
					finish();
				});
		builder.setPositiveButton(getString(R.string.install),
				(dialog, which) -> {
					Uri uri = Uri
							.parse("market://details?id=org.sufficientlysecure.keychain");
					Intent marketIntent = new Intent(Intent.ACTION_VIEW,
							uri);
					PackageManager manager = getApplicationContext()
							.getPackageManager();
					List<ResolveInfo> infos = manager
							.queryIntentActivities(marketIntent, 0);
					if (infos.size() > 0) {
						startActivity(marketIntent);
					} else {
						uri = Uri.parse("https://www.openkeychain.org/");
						Intent browserIntent = new Intent(
								Intent.ACTION_VIEW, uri);
						startActivity(browserIntent);
					}
					finish();
				});
		builder.create().show();
	}*/

	abstract void onBackendConnected();

	protected void registerListeners() {
		if (this instanceof XmppConnectionService.OnConversationUpdate) {
			this.xmppConnectionService.setOnConversationListChangedListener((XmppConnectionService.OnConversationUpdate) this);
		}
		if (this instanceof XmppConnectionService.OnAccountUpdate) {
			this.xmppConnectionService.setOnAccountListChangedListener((XmppConnectionService.OnAccountUpdate) this);
		}
		if (this instanceof XmppConnectionService.OnCaptchaRequested) {
			this.xmppConnectionService.setOnCaptchaRequestedListener((XmppConnectionService.OnCaptchaRequested) this);
		}
		if (this instanceof XmppConnectionService.OnRosterUpdate) {
			this.xmppConnectionService.setOnRosterUpdateListener((XmppConnectionService.OnRosterUpdate) this);
		}
		if (this instanceof XmppConnectionService.OnMucRosterUpdate) {
			this.xmppConnectionService.setOnMucRosterUpdateListener((XmppConnectionService.OnMucRosterUpdate) this);
		}
		if (this instanceof OnUpdateBlocklist) {
			this.xmppConnectionService.setOnUpdateBlocklistListener((OnUpdateBlocklist) this);
		}
		if (this instanceof XmppConnectionService.OnShowErrorToast) {
			this.xmppConnectionService.setOnShowErrorToastListener((XmppConnectionService.OnShowErrorToast) this);
		}
		if (this instanceof OnKeyStatusUpdated) {
			this.xmppConnectionService.setOnKeyStatusUpdatedListener((OnKeyStatusUpdated) this);
		}
	}

	protected void unregisterListeners() {
		if (this instanceof XmppConnectionService.OnConversationUpdate) {
			this.xmppConnectionService.removeOnConversationListChangedListener((XmppConnectionService.OnConversationUpdate) this);
		}
		if (this instanceof XmppConnectionService.OnAccountUpdate) {
			this.xmppConnectionService.removeOnAccountListChangedListener((XmppConnectionService.OnAccountUpdate) this);
		}
		if (this instanceof XmppConnectionService.OnCaptchaRequested) {
			this.xmppConnectionService.removeOnCaptchaRequestedListener((XmppConnectionService.OnCaptchaRequested) this);
		}
		if (this instanceof XmppConnectionService.OnRosterUpdate) {
			this.xmppConnectionService.removeOnRosterUpdateListener((XmppConnectionService.OnRosterUpdate) this);
		}
		if (this instanceof XmppConnectionService.OnMucRosterUpdate) {
			this.xmppConnectionService.removeOnMucRosterUpdateListener((XmppConnectionService.OnMucRosterUpdate) this);
		}
		if (this instanceof OnUpdateBlocklist) {
			this.xmppConnectionService.removeOnUpdateBlocklistListener((OnUpdateBlocklist) this);
		}
		if (this instanceof XmppConnectionService.OnShowErrorToast) {
			this.xmppConnectionService.removeOnShowErrorToastListener((XmppConnectionService.OnShowErrorToast) this);
		}
		if (this instanceof OnKeyStatusUpdated) {
			this.xmppConnectionService.removeOnNewKeysAvailableListener((OnKeyStatusUpdated) this);
		}
	}

	protected boolean isAppInstalled(String packageName) {
		Intent mIntent = getPackageManager().getLaunchIntentForPackage(packageName);
		if (mIntent != null) {
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_settings:
				startActivity(new Intent(this, SettingsActivity.class));
				break;
			case android.R.id.home:
				finish();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 *
	 */
	private void doCoreErrorAction() {
		androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
		builder.setTitle(R.string.core_missing);
		builder.setMessage(R.string.glacier_core_install);
		builder.setPositiveButton(R.string.next, (dialog, which) -> {
			try {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(getString(R.string.glacier_core_https)));
				startActivity(intent);
				dialog.dismiss();
			}
			catch(Exception e2){
				e2.printStackTrace();
			}
		});
		final androidx.appcompat.app.AlertDialog dialog = builder.create();
		dialog.setCanceledOnTouchOutside(false);
		dialog.show();
	}

	public void selectPresence(final Conversation conversation, final PresenceSelector.OnPresenceSelected listener) {
		final Contact contact = conversation.getContact();
		if (!contact.showInRoster()) {
			showAddToRosterDialog(conversation.getContact());
		} else {
			final Presences presences = contact.getPresences();
			if (presences.size() == 0) {
				if (!contact.getOption(Contact.Options.TO)
						&& !contact.getOption(Contact.Options.ASKING)
						&& contact.getAccount().getStatus() == Account.State.ONLINE) {
					showAskForPresenceDialog(contact);
				} else if (!contact.getOption(Contact.Options.TO)
						|| !contact.getOption(Contact.Options.FROM)) {
					PresenceSelector.warnMutualPresenceSubscription(this, conversation, listener);
				} else {
					conversation.setNextCounterpart(null);
					listener.onPresenceSelected();
				}
			} else if (presences.size() == 1) {
				String presence = presences.toResourceArray()[0];
				try {
					conversation.setNextCounterpart(Jid.of(contact.getJid().getLocal(), contact.getJid().getDomain(), presence));
				} catch (IllegalArgumentException e) {
					conversation.setNextCounterpart(null);
				}
				listener.onPresenceSelected();
			} else {
				PresenceSelector.showPresenceSelectionDialog(this, conversation, listener);
			}
		}
	}

	//private boolean hasAddressedPermissions = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setVolumeControlStream(AudioManager.STREAM_NOTIFICATION);
		metrics = getResources().getDisplayMetrics();
		ExceptionHelper.init(getApplicationContext());
		new EmojiService(this).init();
		this.isCameraFeatureAvailable = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);

		mColorRed = ContextCompat.getColor(this, R.color.red_800);

		this.mTheme = findTheme();
		setTheme(this.mTheme);

		this.mUsingEnterKey = usingEnterKey();
	}

	protected boolean isCameraFeatureAvailable() {
		return this.isCameraFeatureAvailable;
	}

	public boolean isDarkTheme() {
		return ThemeHelper.isDark(mTheme);
	}

	public int getThemeResource(int r_attr_name, int r_drawable_def) {
		int[] attrs = {r_attr_name};
		TypedArray ta = this.getTheme().obtainStyledAttributes(attrs);

		int res = ta.getResourceId(0, r_drawable_def);
		ta.recycle();

		return res;
	}

	protected boolean isOptimizingBattery() {
		final PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
		return pm != null
				&& !pm.isIgnoringBatteryOptimizations(getPackageName());
	}

	protected boolean isAffectedByDataSaver() {
		final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		return cm != null
				&& cm.isActiveNetworkMetered()
				&& cm.getRestrictBackgroundStatus() == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED;
	}

	protected boolean usingEnterKey() {
		return getBooleanPreference("display_enter_key", R.bool.display_enter_key);
	}

	protected SharedPreferences getPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	}

	protected boolean getBooleanPreference(String name, @BoolRes int res) {
		return getPreferences().getBoolean(name, getResources().getBoolean(res));
	}

	public void switchToConversation(Conversation conversation) {
		switchToConversation(conversation, null);
	}

	public void switchToConversationAndQuote(Conversation conversation, String text) {
		switchToConversation(conversation, text, true, null, false, false);
	}

	public void switchToConversation(Conversation conversation, String text) {
		switchToConversation(conversation, text, false, null, false, false);
	}

	public void switchToConversationDoNotAppend(Conversation conversation, String text) {
		switchToConversation(conversation, text, false, null, false, true);
	}

	public void highlightInMuc(Conversation conversation, String nick) {
		switchToConversation(conversation, null, false, nick, false, false);
	}

	public void privateMsgInMuc(Conversation conversation, String nick) {
		switchToConversation(conversation, null, false, nick, true, false);
	}

	private void switchToConversation(Conversation conversation, String text, boolean asQuote, String nick, boolean pm, boolean doNotAppend) {
		Intent intent = new Intent(this, ConversationsActivity.class);
		intent.setAction(ConversationsActivity.ACTION_VIEW_CONVERSATION);
		intent.putExtra(ConversationsActivity.EXTRA_CONVERSATION, conversation.getUuid());
		if (text != null) {
			intent.putExtra(Intent.EXTRA_TEXT, text);
			if (asQuote) {
				intent.putExtra(ConversationsActivity.EXTRA_AS_QUOTE, true);
			}
		}
		if (nick != null) {
			intent.putExtra(ConversationsActivity.EXTRA_NICK, nick);
			intent.putExtra(ConversationsActivity.EXTRA_IS_PRIVATE_MESSAGE, pm);
		}
		if (doNotAppend) {
			intent.putExtra(ConversationsActivity.EXTRA_DO_NOT_APPEND, true);
		}
		intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		finish();
	}

	public void switchToContactDetails(Contact contact) {
		switchToContactDetails(contact, null);
	}

	public void switchToContactDetails(Contact contact, String messageFingerprint) {
		Intent intent = new Intent(this, ContactDetailsActivity.class);
		intent.setAction(ContactDetailsActivity.ACTION_VIEW_CONTACT);
		intent.putExtra(EXTRA_ACCOUNT, contact.getAccount().getJid().asBareJid().toString());
		intent.putExtra("contact", contact.getJid().toString());
		intent.putExtra("fingerprint", messageFingerprint);
		startActivity(intent);
	}

	public void switchToAccount(Account account, String fingerprint) {
		switchToAccount(account, false, fingerprint);
	}

	public void switchToAccount(Account account) {
		switchToAccount(account, false, null);
	}

	public void switchToAccount(Account account, boolean init, String fingerprint) {
		Intent intent = new Intent(this, EditAccountActivity.class);
		intent.putExtra("jid", account.getJid().asBareJid().toString());
		intent.putExtra("init", init);
		if (init) {
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
		}
		if (fingerprint != null) {
			intent.putExtra("fingerprint", fingerprint);
		}
		startActivity(intent);
		if (init) {
			overridePendingTransition(0, 0);
		}
	}

	protected void delegateUriPermissionsToService(Uri uri) {
		Intent intent = new Intent(this,XmppConnectionService.class);
		intent.setAction(Intent.ACTION_SEND);
		intent.setData(uri);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		try {
			startService(intent);
		} catch (Exception e) {
			Log.e(Config.LOGTAG,"unable to delegate uri permission",e);
		}
	}

	protected void inviteToConversation(Conversation conversation) {
		startActivityForResult(ChooseContactActivity.create(this,conversation), REQUEST_INVITE_TO_CONVERSATION);
	}

	/*protected void announcePgp(final Account account, final Conversation conversation, Intent intent, final Runnable onSuccess) {
		if (account.getPgpId() == 0) {
			choosePgpSignId(account);
		} else {
			String status = null;
			if (manuallyChangePresence()) {
				status = account.getPresenceStatusMessage();
			}
			if (status == null) {
				status = "";
			}
			xmppConnectionService.getPgpEngine().generateSignature(intent, account, status, new UiCallback<String>() {

				@Override
				public void userInputRequried(PendingIntent pi, String signature) {
					try {
						startIntentSenderForResult(pi.getIntentSender(), REQUEST_ANNOUNCE_PGP, null, 0, 0, 0);
					} catch (final SendIntentException ignored) {
					}
				}

				@Override
				public void success(String signature) {
					account.setPgpSignature(signature);
					xmppConnectionService.databaseBackend.updateAccount(account);
					xmppConnectionService.sendPresence(account);
					if (conversation != null) {
						conversation.setNextEncryption(Message.ENCRYPTION_PGP);
						xmppConnectionService.updateConversation(conversation);
						refreshUi();
					}
					if (onSuccess != null) {
						runOnUiThread(onSuccess);
					}
				}

				@Override
				public void error(int error, String signature) {
					if (error == 0) {
						account.setPgpSignId(0);
						account.unsetPgpSignature();
						xmppConnectionService.databaseBackend.updateAccount(account);
						choosePgpSignId(account);
					} else {
						displayErrorDialog(error);
					}
				}
			});
		}
	}*/

	@SuppressWarnings("deprecation")
	protected void setListItemBackgroundOnView(View view) {
		view.setBackground(getResources().getDrawable(R.drawable.greybackground));
	}

	protected void displayErrorDialog(final int errorCode) {
		runOnUiThread(() -> {
			Builder builder = new Builder(XmppActivity.this);
			builder.setIconAttribute(android.R.attr.alertDialogIcon);
			builder.setTitle(getString(R.string.error));
			builder.setMessage(errorCode);
			builder.setNeutralButton(R.string.accept, null);
			builder.create().show();
		});

	}

	protected void showAddToRosterDialog(final Contact contact) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(contact.getJid().toString());
		builder.setMessage(getString(R.string.not_in_roster));
		builder.setNegativeButton(getString(R.string.cancel), null);
		builder.setPositiveButton(getString(R.string.add_contact), (dialog, which) -> xmppConnectionService.createContact(contact,true));
		builder.create().show();
	}

	private void showAskForPresenceDialog(final Contact contact) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(contact.getJid().toString());
		builder.setMessage(R.string.request_presence_updates);
		builder.setNegativeButton(R.string.cancel, null);
		builder.setPositiveButton(R.string.request_now,
				(dialog, which) -> {
					if (xmppConnectionServiceBound) {
						xmppConnectionService.sendPresencePacket(contact
								.getAccount(), xmppConnectionService
								.getPresenceGenerator()
								.requestPresenceUpdatesFrom(contact));
					}
				});
		builder.create().show();
	}

	protected void quickEdit(String previousValue, @StringRes int hint, OnValueEdited callback) {
		quickEdit(previousValue, callback, hint, false, false);
	}

	protected void quickEdit(String previousValue, @StringRes int hint, OnValueEdited callback, boolean permitEmpty) {
		quickEdit(previousValue, callback, hint, false, permitEmpty);
	}

	protected void quickPasswordEdit(String previousValue, OnValueEdited callback) {
		quickEdit(previousValue, callback, R.string.password, true, false);
	}

	@SuppressLint("InflateParams")
	private void quickEdit(final String previousValue,
	                       final OnValueEdited callback,
	                       final @StringRes int hint,
	                       boolean password,
	                       boolean permitEmpty) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		DialogQuickeditBinding binding = DataBindingUtil.inflate(getLayoutInflater(),R.layout.dialog_quickedit, null, false);
		if (password) {
			binding.inputEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		}
		builder.setPositiveButton(R.string.accept, null);
		if (hint != 0) {
			binding.inputLayout.setHint(getString(hint));
		}
		binding.inputEditText.requestFocus();
		if (previousValue != null) {
			binding.inputEditText.getText().append(previousValue);
		}
		builder.setView(binding.getRoot());
		builder.setNegativeButton(R.string.cancel, null);
		final AlertDialog dialog = builder.create();
		dialog.setOnShowListener(d -> SoftKeyboardUtils.showKeyboard(binding.inputEditText));
		dialog.show();
		View.OnClickListener clickListener = v -> {
			String value = binding.inputEditText.getText().toString();
			if (!value.equals(previousValue) && (!value.trim().isEmpty() || permitEmpty)) {
				String error = callback.onValueEdited(value);
				if (error != null) {
					binding.inputLayout.setError(error);
					return;
				}
			}
			SoftKeyboardUtils.hideSoftKeyboard(binding.inputEditText);
			dialog.dismiss();
		};
		dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(clickListener);
		dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener((v -> {
			SoftKeyboardUtils.hideSoftKeyboard(binding.inputEditText);
			dialog.dismiss();
		}));
		dialog.setCanceledOnTouchOutside(false);
		dialog.setOnDismissListener(dialog1 -> {
			SoftKeyboardUtils.hideSoftKeyboard(binding.inputEditText);
        });
	}

	protected boolean hasStoragePermission(int requestCode) {
		if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
			return false;
		} else {
			return true;
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_INVITE_TO_CONVERSATION && resultCode == RESULT_OK) {
			mPendingConferenceInvite = ConferenceInvite.parse(data);
			if (xmppConnectionServiceBound && mPendingConferenceInvite != null) {
				if (mPendingConferenceInvite.execute(this)) {
					mToast = Toast.makeText(this, R.string.creating_conference, Toast.LENGTH_LONG);
					mToast.show();
				}
				mPendingConferenceInvite = null;
			}

			if (mPendingConferenceInvite != null) {
				Conversation conv = xmppConnectionService.findConversationByUuid(mPendingConferenceInvite.uuid);
				if (conv != null && conv.getMode() == Conversation.MODE_MULTI && mPendingConferenceInvite.jids.size() > 0) {
					xmppConnectionService.sendJoiningGroupMessage(conv, mPendingConferenceInvite.jids, false);
				}
			}
		}
	}

	public int getWarningTextColor() {
		return this.mColorRed;
	}

	public int getPixel(int dp) {
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		return ((int) (dp * metrics.density));
	}

	public boolean copyTextToClipboard(String text, int labelResId) {
		ClipboardManager mClipBoardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		String label = getResources().getString(labelResId);
		if (mClipBoardManager != null) {
			ClipData mClipData = ClipData.newPlainText(label, text);
			mClipBoardManager.setPrimaryClip(mClipData);
			return true;
		}
		return false;
	}

	protected boolean manuallyChangePresence() {
		return getBooleanPreference(SettingsActivity.MANUALLY_CHANGE_PRESENCE, R.bool.manually_change_presence);
	}

	protected String getShareableUri() {
		return getShareableUri(false);
	}

	protected String getShareableUri(boolean http) {
		return null;
	}

	protected void shareLink(boolean http) {
		String uri = getShareableUri(http);
		if (uri == null || uri.isEmpty()) {
			return;
		}
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, getShareableUri(http));
		try {
			startActivity(Intent.createChooser(intent, getText(R.string.share_uri_with)));
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, R.string.no_application_to_share_uri, Toast.LENGTH_SHORT).show();
		}
	}

	protected void launchOpenKeyChain(long keyId) {
		/*PgpEngine pgp = XmppActivity.this.xmppConnectionService.getPgpEngine();
		try {
			startIntentSenderForResult(
					pgp.getIntentForKey(keyId).getIntentSender(), 0, null, 0,
					0, 0);
		} catch (Throwable e) {
			Toast.makeText(XmppActivity.this, R.string.openpgp_error, Toast.LENGTH_SHORT).show();
		}*/
	}

	@Override
	public void onResume() {
		super.onResume();
		initializeScreenshotSecurity();
	}

	protected int findTheme() {
		return ThemeHelper.find(this);
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public boolean onMenuOpened(int id, Menu menu) {
		if(id == AppCompatDelegate.FEATURE_SUPPORT_ACTION_BAR && menu != null) {
			MenuDoubleTabUtil.recordMenuOpen();
		}
		return super.onMenuOpened(id, menu);
	}

	protected void showQrCode() {
		showQrCode(getShareableUri());
	}

	protected void showQrCode(final String uri) {
		if (uri == null || uri.isEmpty()) {
			return;
		}
		Point size = new Point();
		getWindowManager().getDefaultDisplay().getSize(size);
		final int width = (size.x < size.y ? size.x : size.y);
		Bitmap bitmap = BarcodeProvider.create2dBarcodeBitmap(uri, width);
		ImageView view = new ImageView(this);
		view.setBackgroundColor(Color.WHITE);
		view.setImageBitmap(bitmap);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(view);
		builder.create().show();
	}

	protected Account extractAccount(Intent intent) {
		String jid = intent != null ? intent.getStringExtra(EXTRA_ACCOUNT) : null;
		try {
			return jid != null ? xmppConnectionService.findAccountByJid(Jid.of(jid)) : null;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public AvatarService avatarService() {
		return xmppConnectionService.getAvatarService();
	}

	public void loadBitmap(Message message, ImageView imageView) {
		Bitmap bm;
		try {
			bm = xmppConnectionService.getFileBackend().getThumbnail(message, (int) (metrics.density * 288), true);
		} catch (IOException e) {
			bm = null;
		}
		if (bm != null) {
			cancelPotentialWork(message, imageView);
			imageView.setImageBitmap(bm);
			imageView.setBackgroundColor(0x00000000);
		} else {
			if (cancelPotentialWork(message, imageView)) {
				imageView.setBackgroundColor(0xff333333);
				imageView.setImageDrawable(null);
				final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
				final AsyncDrawable asyncDrawable = new AsyncDrawable(
						getResources(), null, task);
				imageView.setImageDrawable(asyncDrawable);
				try {
					task.execute(message);
				} catch (final RejectedExecutionException ignored) {
					ignored.printStackTrace();
				}
			}
		}
	}

	protected interface OnValueEdited {
		String onValueEdited(String value);
	}

	protected interface OnBoolEdited {
		boolean onBoolEdited(boolean bool);
	}

	public static class ConferenceInvite {
		private String uuid;
		private String groupname;
		private List<Jid> jids = new ArrayList<>();

		public static ConferenceInvite parse(Intent data) {
			ConferenceInvite invite = new ConferenceInvite();
			invite.uuid = data.getStringExtra(ChooseContactActivity.EXTRA_CONVERSATION);
			invite.groupname = data.getStringExtra(ChooseContactActivity.EXTRA_GROUP_CHAT_NAME);
			if (invite.uuid == null) {
				return null;
			}
			invite.jids.addAll(ChooseContactActivity.extractJabberIds(data));
			return invite;
		}

        public List<Jid> getJids() {
		    return jids;
        }

		public boolean execute(XmppActivity activity) {
			XmppConnectionService service = activity.xmppConnectionService;
			Conversation conversation = service.findConversationByUuid(this.uuid);
			if (conversation == null) {
				return false;
			}
			if (conversation.getMode() == Conversation.MODE_MULTI) {
				for (Jid jid : jids) {
					service.invite(conversation, jid);
				}
				return false;
			} else {
				jids.add(conversation.getJid().asBareJid());
				return service.createAdhocConference(conversation.getAccount(), this.groupname, jids, false, activity.adhocCallback);
			}
		}
	}

	static class BitmapWorkerTask extends AsyncTask<Message, Void, Bitmap> {
		private final WeakReference<ImageView> imageViewReference;
		private Message message = null;

		private BitmapWorkerTask(ImageView imageView) {
			this.imageViewReference = new WeakReference<>(imageView);
		}

		@Override
		protected Bitmap doInBackground(Message... params) {
			if (isCancelled()) {
				return null;
			}
			message = params[0];
			try {
				final XmppActivity activity = find(imageViewReference);
				if (activity != null && activity.xmppConnectionService != null) {
					return activity.xmppConnectionService.getFileBackend().getThumbnail(message, (int) (activity.metrics.density * 288), false);
				} else {
					return null;
				}
			} catch (IOException e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(final Bitmap bitmap) {
			if (!isCancelled()) {
				final ImageView imageView = imageViewReference.get();
				if (imageView != null) {
					imageView.setImageBitmap(bitmap);
					imageView.setBackgroundColor(bitmap == null ? 0xff333333 : 0x00000000);
				}
			}
		}
	}

	private static class AsyncDrawable extends BitmapDrawable {
		private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

		private AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
			super(res, bitmap);
			bitmapWorkerTaskReference = new WeakReference<>(bitmapWorkerTask);
		}

		private BitmapWorkerTask getBitmapWorkerTask() {
			return bitmapWorkerTaskReference.get();
		}
	}

	public static XmppActivity find(@NonNull  WeakReference<ImageView> viewWeakReference) {
		final View view = viewWeakReference.get();
		return view == null ? null : find(view);
	}

	public static XmppActivity find(@NonNull final View view) {
		Context context = view.getContext();
		while (context instanceof ContextWrapper) {
			if (context instanceof XmppActivity) {
				return (XmppActivity) context;
			}
			context = ((ContextWrapper)context).getBaseContext();
		}
		return null;
	}

	/**
	 * PERMISSIONS - Ask for permissions
	 */
	private void askForPermissions() {
		final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
		List<String> permissionsNeeded = new ArrayList<String>();

		final List<String> permissionsList = new ArrayList<String>();
		// added WRITE_EXTERNAL_STORAGE permission ahead of time so that it doesn't ask
		// when time comes which inevitably fails at that point.
		if (!addPermission(permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE))
			permissionsNeeded.add("Write Storage");
		if (!addPermission(permissionsList, Manifest.permission.CAMERA))
			permissionsNeeded.add("Camera");
		if (!addPermission(permissionsList, Manifest.permission.RECORD_AUDIO))
			permissionsNeeded.add("Record Audio");
		if (!addPermission(permissionsList, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS))
			permissionsNeeded.add("Ignore Battery Optimizations");

		if (permissionsList.size() > 0) {
			if (permissionsNeeded.size() > 0) {
				// Need Rationale
				String message = "You need to grant access to " + permissionsNeeded.get(0);
				for (int i = 1; i < permissionsNeeded.size(); i++) {
					message = message + ", " + permissionsNeeded.get(i);
				}

				requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
						REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);

				return;
			}
			requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
					REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);

			return;
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
		if (this.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
			permissionsList.add(permission);
			// Check for Rationale Option
			if (!shouldShowRequestPermissionRationale(permission))
				return false;
		}
		return true;
	}

	//retry actions, permission might have been updated/granted and additional service is possible
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
		UriHandlerActivity.onRequestPermissionResult(this, requestCode, grantResults);
		if (grantResults.length > 0) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				if (xmppConnectionService != null){
					xmppConnectionService.checkNewPermission();
				}
				this.isCameraFeatureAvailable = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
			}
		}
	}

}
