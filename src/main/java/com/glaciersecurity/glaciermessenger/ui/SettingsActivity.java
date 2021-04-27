package com.glaciersecurity.glaciermessenger.ui;

import android.annotation.SuppressLint;
import android.preference.CheckBoxPreference;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import java.io.File;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.crypto.OmemoSetting;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.Conversation;
import com.glaciersecurity.glaciermessenger.entities.Message;
import com.glaciersecurity.glaciermessenger.persistance.FileBackend;
import com.glaciersecurity.glaciermessenger.services.ExportBackupService;
import com.glaciersecurity.glaciermessenger.services.MemorizingTrustManager;
import com.glaciersecurity.glaciermessenger.services.QuickConversationsService;
import com.glaciersecurity.glaciermessenger.ui.util.Color;
import com.glaciersecurity.glaciermessenger.utils.GeoHelper;
import com.glaciersecurity.glaciermessenger.utils.TimeframeUtils;

public class SettingsActivity extends XmppActivity implements OnSharedPreferenceChangeListener{

	public static final String KEEP_FOREGROUND_SERVICE = "enable_foreground_service";
	public static final String AWAY_WHEN_SCREEN_IS_OFF = "away_when_screen_off";
	public static final String TREAT_VIBRATE_AS_SILENT = "treat_vibrate_as_silent";
	public static final String DND_ON_SILENT_MODE = "dnd_on_silent_mode";
	public static final String MANUALLY_CHANGE_PRESENCE = "manually_change_presence";
	public static final String BLIND_TRUST_BEFORE_VERIFICATION = "btbv";
	public static final String AUTOMATIC_MESSAGE_DELETION = "automatic_message_deletion";
	public static final String GLOBAL_MESSAGE_TIMER = "global_message_timer";
	public static final String BROADCAST_LAST_ACTIVITY = "last_activity";
	public static final String THEME = "theme";
	public static final String SHOW_DYNAMIC_TAGS = "show_dynamic_tags";
	public static final String OMEMO_SETTING = "omemo";
	public static final String SCREEN_SECURITY = "screen_security";

	public static final int REQUEST_CREATE_BACKUP = 0xbf8701;
	private SettingsFragment mSettingsFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		FragmentManager fm = getFragmentManager();
		mSettingsFragment = (SettingsFragment) fm.findFragmentById(R.id.settings_content);
		if (mSettingsFragment == null || !mSettingsFragment.getClass().equals(SettingsFragment.class)) {
			mSettingsFragment = new SettingsFragment();
			fm.beginTransaction().replace(R.id.settings_content, mSettingsFragment).commit();
		}
		mSettingsFragment.setActivityIntent(getIntent());
		this.mTheme = findTheme();
		setTheme(this.mTheme);
		getWindow().getDecorView().setBackgroundColor(Color.get(this, R.attr.color_background_primary));
		setSupportActionBar(findViewById(R.id.toolbar));
		configureActionBar(getSupportActionBar());
	}

	@Override
	void onBackendConnected() {
		//
	}

	@Override
	public void onStart() {
		super.onStart();
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

		changeOmemoSettingSummary();

		if (QuickConversationsService.isQuicksy()) {
			PreferenceCategory connectionOptions = (PreferenceCategory) mSettingsFragment.findPreference("connection_options");
			PreferenceScreen expert = (PreferenceScreen) mSettingsFragment.findPreference("expert");
			if (connectionOptions != null) {
				expert.removePreference(connectionOptions);
			}
		}

		PreferenceScreen mainPreferenceScreen = (PreferenceScreen) mSettingsFragment.findPreference("main_screen");

		PreferenceCategory attachmentsCategory = (PreferenceCategory) mSettingsFragment.findPreference("attachments");
		CheckBoxPreference locationPlugin = (CheckBoxPreference) mSettingsFragment.findPreference("use_share_location_plugin");
		if (attachmentsCategory != null && locationPlugin != null) {
			if (!GeoHelper.isLocationPluginInstalled(this)) {
				attachmentsCategory.removePreference(locationPlugin);
			}
		}


		//this feature is only available on Huawei Android 6.
		PreferenceScreen huaweiPreferenceScreen = (PreferenceScreen) mSettingsFragment.findPreference("huawei");
		if (huaweiPreferenceScreen != null) {
			Intent intent = huaweiPreferenceScreen.getIntent();
			PreferenceCategory generalCategory = (PreferenceCategory) mSettingsFragment.findPreference("general");
			generalCategory.removePreference(huaweiPreferenceScreen);
			if (generalCategory.getPreferenceCount() == 0) {
				if (mainPreferenceScreen != null) {
					mainPreferenceScreen.removePreference(generalCategory);
				}
			}
		}

		ListPreference automaticMessageDeletionList = (ListPreference) mSettingsFragment.findPreference(AUTOMATIC_MESSAGE_DELETION);
		if (automaticMessageDeletionList != null) {
			final int[] choices = getResources().getIntArray(R.array.automatic_message_deletion_values);
			CharSequence[] entries = new CharSequence[choices.length];
			CharSequence[] entryValues = new CharSequence[choices.length];
			for (int i = 0; i < choices.length; ++i) {
				entryValues[i] = String.valueOf(choices[i]);
				if (choices[i] == 0) {
					entries[i] = getString(R.string.never);
				} else {
					entries[i] = TimeframeUtils.resolve(this, 1000L * choices[i]);
				}
			}
			automaticMessageDeletionList.setEntries(entries);
			automaticMessageDeletionList.setEntryValues(entryValues);
		}


		boolean removeLocation = new Intent("com.glaciersecurity.glaciermessenger.location.request").resolveActivity(getPackageManager()) == null;
		boolean removeVoice = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION).resolveActivity(getPackageManager()) == null;

		final Preference removeCertsPreference = mSettingsFragment.findPreference("remove_trusted_certificates");
		if (removeCertsPreference != null) {
			removeCertsPreference.setOnPreferenceClickListener(preference -> {
				final MemorizingTrustManager mtm = xmppConnectionService.getMemorizingTrustManager();
				final ArrayList<String> aliases = Collections.list(mtm.getCertificates());
				if (aliases.size() == 0) {
					displayToast(getString(R.string.toast_no_trusted_certs));
					return true;
				}
				final ArrayList<Integer> selectedItems = new ArrayList<>();
				final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(SettingsActivity.this);
				dialogBuilder.setTitle(getResources().getString(R.string.dialog_manage_certs_title));
				dialogBuilder.setMultiChoiceItems(aliases.toArray(new CharSequence[aliases.size()]), null,
						(dialog, indexSelected, isChecked) -> {
							if (isChecked) {
								selectedItems.add(indexSelected);
							} else if (selectedItems.contains(indexSelected)) {
								selectedItems.remove(Integer.valueOf(indexSelected));
							}
							if (selectedItems.size() > 0)
								((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
							else {
								((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
							}
						});

				dialogBuilder.setPositiveButton(
						getResources().getString(R.string.dialog_manage_certs_positivebutton), (dialog, which) -> {
							int count = selectedItems.size();
							if (count > 0) {
								for (int i = 0; i < count; i++) {
									try {
										Integer item = Integer.valueOf(selectedItems.get(i).toString());
										String alias = aliases.get(item);
										mtm.deleteCertificate(alias);
									} catch (KeyStoreException e) {
										e.printStackTrace();
										displayToast("Error: " + e.getLocalizedMessage());
									}
								}
								if (xmppConnectionServiceBound) {
									reconnectAccounts();
								}
								displayToast(getResources().getQuantityString(R.plurals.toast_delete_certificates, count, count));
							}
						});
				dialogBuilder.setNegativeButton(getResources().getString(R.string.dialog_manage_certs_negativebutton), null);
				AlertDialog removeCertsDialog = dialogBuilder.create();
				removeCertsDialog.show();
				removeCertsDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
				return true;
			});
		}

		final Preference createBackupPreference = mSettingsFragment.findPreference("create_backup");
		if (createBackupPreference != null) {
			createBackupPreference.setSummary(getString(R.string.pref_create_backup_summary, FileBackend.getBackupDirectory(this)));
			createBackupPreference.setOnPreferenceClickListener(preference -> {
				if (hasStoragePermission(REQUEST_CREATE_BACKUP)) {
					createBackup();
				}
				return true;
			});
		}

		final Preference wipeAllHistoryPreference = mSettingsFragment.findPreference("wipe_all_history");
		if (wipeAllHistoryPreference != null){
			wipeAllHistoryPreference.setOnPreferenceClickListener(preference -> {
				wipeAllHistoryDialog();
				return true;
			});
		}

		if (Config.ONLY_INTERNAL_STORAGE) {
			final Preference cleanCachePreference = mSettingsFragment.findPreference("clean_cache");
			if (cleanCachePreference != null) {
				cleanCachePreference.setOnPreferenceClickListener(preference -> cleanCache());
			}

			final Preference cleanPrivateStoragePreference = mSettingsFragment.findPreference("clean_private_storage");
			if (cleanPrivateStoragePreference != null) {
				cleanPrivateStoragePreference.setOnPreferenceClickListener(preference -> cleanPrivateStorage());
			}
		}

		final Preference deleteOmemoPreference = mSettingsFragment.findPreference("delete_omemo_identities");
		if (deleteOmemoPreference != null) {
			deleteOmemoPreference.setOnPreferenceClickListener(preference -> deleteOmemoIdentities());
		}
	}


	/**  WIPE ALL  HISTORY  **/
	/**
	 * end ALL existing conversations
	 */
	@SuppressLint("InflateParams")
	protected void wipeAllHistoryDialog() {
		android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.action_wipe_all_history));
		View dialogView = this.getLayoutInflater().inflate(
				R.layout.dialog_wipe_all_history, null);
		final CheckBox deleteAllMessagesEachChatCheckBox = (CheckBox) dialogView
				.findViewById(R.id.delete_all_messages_each_chat_checkbox);
		final CheckBox endAllChatCheckBox = (CheckBox) dialogView
				.findViewById(R.id.end_all_chat_checkbox);
		final CheckBox deleteAllCachedFilesCheckBox = (CheckBox) dialogView
				.findViewById(R.id.delete_all_cached_files_checkbox);
		// default to true
		deleteAllMessagesEachChatCheckBox.setChecked(true);
		endAllChatCheckBox.setChecked(true);
		deleteAllCachedFilesCheckBox.setChecked(true);

		builder.setView(dialogView);
		builder.setNegativeButton(getString(R.string.cancel), null);
		builder.setPositiveButton(getString(R.string.wipe_all_history), (dialog, which) -> {
			// go through each conversation and either delete or end each chat depending
			// on what was checked.
			new Thread(() -> {
				List<Conversation> conversations = xmppConnectionService.getConversations();

				boolean hadMulti = false;
				for (int i = (conversations.size() - 1); i >= 0; i--) {
					if (endAllChatCheckBox.isChecked() && conversations.get(i).getMode() == Conversation.MODE_MULTI) {
						sendLeavingGroupMessage(conversations.get(i));
						hadMulti = true;
					}
				}
				if (hadMulti) {
					// sleep required so message goes out before conversation thread stopped
					try { Thread.sleep(3000); } catch (InterruptedException ie) {}
				}

				for (int i = (conversations.size() - 1); i >= 0; i--) {

					// delete messages
					if (deleteAllMessagesEachChatCheckBox.isChecked()) {
						xmppConnectionService.clearConversationHistory(conversations.get(i));
					}

					// end chat
					if (endAllChatCheckBox.isChecked()) {
						xmppConnectionService.archiveConversation(conversations.get(i));
					}
				}

				// delete everything in cache
				if (deleteAllCachedFilesCheckBox.isChecked()) {
					clearCachedFiles();
				}
			}).start();
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
		Message message = new Message(conversation, bod, conversation.getNextEncryption());
		this.xmppConnectionService.sendMessage(message);
		// sleep required so message goes out before conversation thread stopped
		// maybe show a spinner?
		//try { Thread.sleep(2000); } catch (InterruptedException ie) {} //moved to each place
	}
	private void changeOmemoSettingSummary() {
		ListPreference omemoPreference = (ListPreference) mSettingsFragment.findPreference(OMEMO_SETTING);
		if (omemoPreference != null) {
			String value = omemoPreference.getValue();
			switch (value) {
				case "always":
					omemoPreference.setSummary(R.string.pref_glacier_setting_summary_always);
					break;
				case "default_on":
					omemoPreference.setSummary(R.string.pref_glacier_setting_summary_default_on);
					break;
				case "default_off":
					omemoPreference.setSummary(R.string.pref_glacier_setting_summary_default_off);
					break;
			}
		} else {
			Log.d(Config.LOGTAG,"unable to find preference named "+OMEMO_SETTING);
		}
	}

	private boolean isCallable(final Intent i) {
		return i != null && getPackageManager().queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
	}


	private boolean cleanCache() {
		Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
		intent.setData(Uri.parse("package:" + getPackageName()));
		startActivity(intent);
		return true;
	}

	private boolean cleanPrivateStorage() {
		for(String type : Arrays.asList("Images", "Videos", "Files", "Recordings")) {
		        cleanPrivateFiles(type);
	    }
		return true;
	}

	private void cleanPrivateFiles(final String type) {
		try {
			File dir = new File(getFilesDir().getAbsolutePath(), "/" + type + "/");
			File[] array = dir.listFiles();
			if (array != null) {
				for (int b = 0; b < array.length; b++) {
					String name = array[b].getName().toLowerCase();
					if (name.equals(".nomedia")) {
						continue;
					}
					if (array[b].isFile()) {
						array[b].delete();
					}
				}
			}
		} catch (Throwable e) {
			Log.e("CleanCache", e.toString());
		}
	}

	private boolean deleteOmemoIdentities() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.pref_delete_glacier_identities);
		builder.setMessage(R.string.pref_reset_omemo_message);

		builder.setNegativeButton(R.string.cancel, null);
		builder.setPositiveButton(R.string.reset, (dialog, which) -> {
			for(Account account : xmppConnectionService.getAccounts()) {
				if (account != null) {
					account.getAxolotlService().regenerateKeys(true);
				}
			}
		});
		AlertDialog dialog = builder.create();
		dialog.show();
		return true;
	}

	@Override
	public void onStop() {
		super.onStop();
		PreferenceManager.getDefaultSharedPreferences(this)
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences preferences, String name) {
		final List<String> resendPresence = Arrays.asList(
				"confirm_messages",
				DND_ON_SILENT_MODE,
				AWAY_WHEN_SCREEN_IS_OFF,
				"allow_message_correction",
				TREAT_VIBRATE_AS_SILENT,
				MANUALLY_CHANGE_PRESENCE,
				BROADCAST_LAST_ACTIVITY);
		if (name.equals(OMEMO_SETTING)) {
			OmemoSetting.load(this, preferences);
			changeOmemoSettingSummary();
		} else if (name.equals(KEEP_FOREGROUND_SERVICE)) {
			xmppConnectionService.toggleForegroundService();
		} else if (resendPresence.contains(name)) {
			if (xmppConnectionServiceBound) {
				if (name.equals(AWAY_WHEN_SCREEN_IS_OFF) || name.equals(MANUALLY_CHANGE_PRESENCE)) {
					xmppConnectionService.toggleScreenEventReceiver();
				}
				xmppConnectionService.refreshAllPresences();
			}
		} else if (name.equals("dont_trust_system_cas")) {
			xmppConnectionService.updateMemorizingTrustmanager();
			reconnectAccounts();
		} else if (name.equals("use_tor")) {
			reconnectAccounts();
		} else if (name.equals(AUTOMATIC_MESSAGE_DELETION)) {
			xmppConnectionService.expireOldMessages(true);
		} else if (name.equals(GLOBAL_MESSAGE_TIMER)) {
			int timer = Message.TIMER_NONE;
			String timerStr = preferences.getString(name, null);
			if (timerStr != null)
			{
				try {
					timer = Integer.parseInt(timerStr);
				} catch(NumberFormatException nfe) {
					timer = Message.TIMER_NONE;
				}
			}
			changeGlobalTimer(timer);
		} else if (name.equals(THEME)) {
			final int theme = findTheme();
			if (this.mTheme != theme) {
				recreate();
			}
		} else if (name.equals(SCREEN_SECURITY)){
			initializeScreenshotSecurity();
		}

	}

	public void changeGlobalTimer(int timer) {
		for (Account account : xmppConnectionService.getAccounts()) {
			account.setTimer(timer);
			xmppConnectionService.databaseBackend.updateAccount(account);
		}
	}

	public void changeDisplayName(String newname) {
		for (Account account : xmppConnectionService.getAccounts()) {
			account.setDisplayName(newname);
			xmppConnectionService.databaseBackend.updateAccount(account);
			xmppConnectionService.publishDisplayName(account);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (grantResults.length > 0)
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				if (requestCode == REQUEST_CREATE_BACKUP) {
					createBackup();
				}
			} else {
				Toast.makeText(this, R.string.no_storage_permission, Toast.LENGTH_SHORT).show();
			}
	}

	private void createBackup() {
		ContextCompat.startForegroundService(this, new Intent(this, ExportBackupService.class));
	}

	private void displayToast(final String msg) {
		runOnUiThread(() -> Toast.makeText(SettingsActivity.this, msg, Toast.LENGTH_LONG).show());
	}

	private void reconnectAccounts() {
		for (Account account : xmppConnectionService.getAccounts()) {
			if (account.isEnabled()) {
				xmppConnectionService.reconnectAccountInBackground(account);
			}
		}
	}

	public void refreshUiReal() {
		//nothing to do. This Activity doesn't implement any listeners
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
}
