package com.glaciersecurity.glaciermessenger.ui;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.databinding.ActivityMucDetailsBinding;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.Bookmark;
import com.glaciersecurity.glaciermessenger.entities.Conversation;
import com.glaciersecurity.glaciermessenger.entities.Message;
import com.glaciersecurity.glaciermessenger.entities.MucOptions;
import com.glaciersecurity.glaciermessenger.entities.MucOptions.User;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService.OnConversationUpdate;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService.OnMucRosterUpdate;
import com.glaciersecurity.glaciermessenger.ui.adapter.MediaAdapter;
import com.glaciersecurity.glaciermessenger.ui.adapter.UserPreviewAdapter;
import com.glaciersecurity.glaciermessenger.ui.interfaces.OnMediaLoaded;
import com.glaciersecurity.glaciermessenger.ui.util.Attachment;
import com.glaciersecurity.glaciermessenger.ui.util.AvatarWorkerTask;
import com.glaciersecurity.glaciermessenger.ui.util.GridManager;
import com.glaciersecurity.glaciermessenger.ui.util.MenuDoubleTabUtil;
import com.glaciersecurity.glaciermessenger.ui.util.MucConfiguration;
import com.glaciersecurity.glaciermessenger.ui.util.MucDetailsContextMenuHelper;
import com.glaciersecurity.glaciermessenger.ui.util.MyLinkify;
import com.glaciersecurity.glaciermessenger.ui.util.SoftKeyboardUtils;
import com.glaciersecurity.glaciermessenger.utils.Compatibility;
import com.glaciersecurity.glaciermessenger.utils.EmojiWrapper;
import com.glaciersecurity.glaciermessenger.utils.StringUtils;
import com.glaciersecurity.glaciermessenger.utils.StylingHelper;
import com.glaciersecurity.glaciermessenger.utils.XmppUri;
import me.drakeet.support.toast.ToastCompat;
import rocks.xmpp.addr.Jid;

import static com.glaciersecurity.glaciermessenger.entities.Bookmark.printableValue;
import static com.glaciersecurity.glaciermessenger.utils.StringUtils.changed;

public class ConferenceDetailsActivity extends XmppActivity implements OnConversationUpdate, OnMucRosterUpdate, XmppConnectionService.OnAffiliationChanged, XmppConnectionService.OnConfigurationPushed, XmppConnectionService.OnRoomDestroy, TextWatcher, OnMediaLoaded {
    public static final String ACTION_VIEW_MUC = "view_muc";

    private OnClickListener inviteListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            inviteToConversation(mConversation);

            // automatically go into OMEMO when starting conversation or when creating own room
            // Stay unencrypted when going to public/persistent room.  We do this here b/c otherwise it will not
            // send a secure message when creating own room.
            if ((mConversation.getMode() == Conversation.MODE_MULTI) && !(mConversation.getMucOptions().membersOnly())) {
                mConversation.setNextEncryption(Message.ENCRYPTION_NONE);
            } else {
                mConversation.setNextEncryption(Message.ENCRYPTION_AXOLOTL);
            }
        }
    };

    private Conversation mConversation;
    private ActivityMucDetailsBinding binding;
    private MediaAdapter mMediaAdapter;
    private UserPreviewAdapter mUserPreviewAdapter;
    private String uuid = null;

    private boolean mAdvancedMode = false;

    private UiCallback<Conversation> renameCallback = new UiCallback<Conversation>() {
        @Override
        public void success(Conversation object) {
            displayToast(getString(R.string.your_nick_has_been_changed));
            runOnUiThread(() -> {
                updateView();
            });

        }

        @Override
        public void error(final int errorCode, Conversation object) {
            displayToast(getString(errorCode));
        }

        @Override
        public void userInputRequried(PendingIntent pi, Conversation object) {

        }
    };

    private OnClickListener mNotifyStatusClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ConferenceDetailsActivity.this);
            builder.setTitle(R.string.pref_notification_settings);
            String[] choices = {
                    getString(R.string.notify_on_all_messages),
                    getString(R.string.notify_only_when_highlighted),
                    getString(R.string.notify_never)
            };
            final AtomicInteger choice;
            if (mConversation.getLongAttribute(Conversation.ATTRIBUTE_MUTED_TILL, 0) == Long.MAX_VALUE) {
                choice = new AtomicInteger(2);
            } else {
                choice = new AtomicInteger(mConversation.alwaysNotify() ? 0 : 1);
            }
            builder.setSingleChoiceItems(choices, choice.get(), (dialog, which) -> choice.set(which));
            builder.setNegativeButton(R.string.cancel, null);
            builder.setPositiveButton(R.string.ok, (dialog, which) -> {
                if (choice.get() == 2) {
                    mConversation.setMutedTill(Long.MAX_VALUE);
                } else {
                    mConversation.setMutedTill(0);
                    mConversation.setAttribute(Conversation.ATTRIBUTE_ALWAYS_NOTIFY, String.valueOf(choice.get() == 0));
                }
                xmppConnectionService.updateConversation(mConversation);
                updateView();
            });
            builder.create().show();
        }
    };

    private OnClickListener mChangeConferenceSettings = new OnClickListener() {
        @Override
        public void onClick(View v) {
            final MucOptions mucOptions = mConversation.getMucOptions();
            AlertDialog.Builder builder = new AlertDialog.Builder(ConferenceDetailsActivity.this);
            MucConfiguration configuration = MucConfiguration.get(ConferenceDetailsActivity.this, mAdvancedMode, mucOptions);
            builder.setTitle(configuration.title);
            final boolean[] values = configuration.values;
            builder.setMultiChoiceItems(configuration.names, values, (dialog, which, isChecked) -> values[which] = isChecked);
            builder.setNegativeButton(R.string.cancel, null);
            builder.setPositiveButton(R.string.confirm, (dialog, which) -> {
                Bundle options = configuration.toBundle(values);
                options.putString("muc#roomconfig_persistentroom", "1");
                xmppConnectionService.pushConferenceConfiguration(mConversation,
                        options,
                        ConferenceDetailsActivity.this);
            });
            builder.create().show();
        }
    };


    @Override
    public void onConversationUpdate() {
        refreshUi();
    }

    @Override
    public void onMucRosterUpdate() {
        refreshUi();
    }

    @Override
    protected void refreshUiReal() {
        updateView();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.binding = DataBindingUtil.setContentView(this, R.layout.activity_muc_details);
        this.binding.changeConferenceButton.setOnClickListener(this.mChangeConferenceSettings);
        this.binding.invite.setOnClickListener(inviteListener);
        setSupportActionBar((Toolbar) binding.toolbar);
        configureActionBar(getSupportActionBar());
        this.binding.editNickButton.setOnClickListener(v -> quickEdit(mConversation.getMucOptions().getActualNick(),
                R.string.nickname,
                value -> {
                    if (xmppConnectionService.renameInMuc(mConversation, value, renameCallback)) {
                        return null;
                    } else {
                        return getString(R.string.invalid_muc_nick);
                    }
                }));
        this.mAdvancedMode = getPreferences().getBoolean("advanced_muc_mode", false);
        this.binding.mucInfoMore.setVisibility(this.mAdvancedMode ? View.VISIBLE : View.GONE);
        this.binding.notificationStatusButton.setOnClickListener(this.mNotifyStatusClickListener);
        this.binding.yourPhoto.setOnClickListener(v -> {
            final MucOptions mucOptions = mConversation.getMucOptions();
            if (!mucOptions.hasVCards()) {
                Toast.makeText(this, R.string.host_does_not_support_group_chat_avatars, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!mucOptions.getSelf().getAffiliation().ranks(MucOptions.Affiliation.OWNER)) {
                Toast.makeText(this, R.string.only_the_owner_can_change_group_chat_avatar, Toast.LENGTH_SHORT).show();
                return;
            }
            final Intent intent = new Intent(this, PublishGroupChatProfilePictureActivity.class);
            intent.putExtra("uuid", mConversation.getUuid());
            startActivity(intent);
        });
        this.binding.editMucNameButton.setOnClickListener(this::onMucEditButtonClicked);
        this.binding.mucEditTitle.addTextChangedListener(this);
        this.mMediaAdapter = new MediaAdapter(this, R.dimen.media_size);
        this.mUserPreviewAdapter = new UserPreviewAdapter();
        this.binding.media.setAdapter(mMediaAdapter);
        this.binding.users.setAdapter(mUserPreviewAdapter);
        GridManager.setupLayoutManager(this, this.binding.media, R.dimen.media_size);
        GridManager.setupLayoutManager(this, this.binding.users, R.dimen.media_size);
        this.binding.invite.setOnClickListener(v -> inviteToConversation(mConversation));
        this.binding.showUsers.setOnClickListener(v -> {
            Intent intent = new Intent(this, MucUsersActivity.class);
            intent.putExtra("uuid", mConversation.getUuid());
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        final int theme = findTheme();
        if (this.mTheme != theme) {
            recreate();
        }
        binding.mediaWrapper.setVisibility(Compatibility.hasStoragePermission(this) ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (MenuDoubleTabUtil.shouldIgnoreTap()) {
            return false;
        }
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                finish();
                break;
//            case R.id.action_share_http:
//                shareLink(true);
//                break;
//            case R.id.action_share_uri:
//                shareLink(false);
//                break;
            case R.id.action_save_as_bookmark:
                saveAsBookmark();
                break;
            case R.id.action_delete_bookmark:
                deleteBookmark();
                break;
            case R.id.action_destroy_room:
                destroyRoom();
                break;
            case R.id.action_advanced_mode:
                this.mAdvancedMode = !menuItem.isChecked();
                menuItem.setChecked(this.mAdvancedMode);
                getPreferences().edit().putBoolean("advanced_muc_mode", mAdvancedMode).apply();
                final boolean online = mConversation != null && mConversation.getMucOptions().online();
                this.binding.mucInfoMore.setVisibility(this.mAdvancedMode && online ? View.VISIBLE : View.GONE);
                invalidateOptionsMenu();
                updateView();
                break;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (!MucDetailsContextMenuHelper.onContextItemSelected(item, mUserPreviewAdapter.getSelectedUser(), this)) {
            return super.onContextItemSelected(item);
        }
        return true;
    }

    public void onMucEditButtonClicked(View v) {
        if (this.binding.mucEditor.getVisibility() == View.GONE) {
            final MucOptions mucOptions = mConversation.getMucOptions();
            this.binding.mucEditor.setVisibility(View.VISIBLE);
            this.binding.mucDisplay.setVisibility(View.GONE);
            this.binding.editMucNameButton.setImageResource(getThemeResource(R.attr.icon_cancel, R.drawable.ic_cancel_black_24dp));
            final String name = mucOptions.getName();
            this.binding.mucEditTitle.setText("");
            final boolean owner = mucOptions.getSelf().getAffiliation().ranks(MucOptions.Affiliation.OWNER);
            if (owner || printableValue(name)) {
                this.binding.mucEditTitle.setVisibility(View.VISIBLE);
                if (name != null) {
                    this.binding.mucEditTitle.append(name);
                }
            } else {
                this.binding.mucEditTitle.setVisibility(View.GONE);
            }
            this.binding.mucEditTitle.setEnabled(owner);
        } else {
            String name = this.binding.mucEditTitle.isEnabled() ? this.binding.mucEditTitle.getEditableText().toString().trim() : null;
            onMucInfoUpdated(null, name);
            SoftKeyboardUtils.hideSoftKeyboard(this);
            hideEditor();
        }
    }

    private void hideEditor() {
        this.binding.mucEditor.setVisibility(View.GONE);
        this.binding.mucDisplay.setVisibility(View.VISIBLE);
        this.binding.editMucNameButton.setImageResource(getThemeResource(R.attr.icon_edit_body, R.drawable.ic_edit_black_24dp));
    }

    private void onMucInfoUpdated(String subject, String name) {
        final MucOptions mucOptions = mConversation.getMucOptions();
        if (mucOptions.getSelf().getAffiliation().ranks(MucOptions.Affiliation.OWNER) && changed(mucOptions.getName(), name)) {
            Bundle options = new Bundle();
            options.putString("muc#roomconfig_persistentroom", "1");
            options.putString("muc#roomconfig_roomname", StringUtils.nullOnEmpty(name));
            xmppConnectionService.pushConferenceConfiguration(mConversation, options, this);
        }
    }


    @Override
    protected String getShareableUri(boolean http) {
        if (mConversation != null) {
            if (http) {
                return "https://conversations.im/j/" + XmppUri.lameUrlEncode(mConversation.getJid().asBareJid().toEscapedString());
            } else {
                return "xmpp:" + mConversation.getJid().asBareJid() + "?join";
            }
        } else {
            return null;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItemSaveBookmark = menu.findItem(R.id.action_save_as_bookmark);
        MenuItem menuItemDeleteBookmark = menu.findItem(R.id.action_delete_bookmark);
        MenuItem menuItemAdvancedMode = menu.findItem(R.id.action_advanced_mode);
        MenuItem menuItemDestroyRoom = menu.findItem(R.id.action_destroy_room);
        menuItemAdvancedMode.setChecked(mAdvancedMode);
        if (mConversation == null) {
            return true;
        }
        if (mConversation.getBookmark() != null) {
            menuItemSaveBookmark.setVisible(false);
            menuItemDeleteBookmark.setVisible(true);
        } else {
            menuItemDeleteBookmark.setVisible(false);
            menuItemSaveBookmark.setVisible(true);
        }
        menuItemDestroyRoom.setVisible(mConversation.getMucOptions().getSelf().getAffiliation().ranks(MucOptions.Affiliation.OWNER));
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final boolean groupChat = mConversation != null && mConversation.isPrivateAndNonAnonymous();
        getMenuInflater().inflate(R.menu.muc_details, menu);
        final MenuItem destroy = menu.findItem(R.id.action_destroy_room);
        destroy.setTitle(groupChat ? R.string.destroy_room : R.string.destroy_channel);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onMediaLoaded(List<Attachment> attachments) {
        runOnUiThread(() -> {
            int limit = GridManager.getCurrentColumnCount(binding.media);
            mMediaAdapter.setAttachments(attachments.subList(0, Math.min(limit, attachments.size())));
            binding.mediaWrapper.setVisibility(attachments.size() > 0 ? View.VISIBLE : View.GONE);
        });

    }


    protected void saveAsBookmark() {
        xmppConnectionService.saveConversationAsBookmark(mConversation, mConversation.getMucOptions().getName());
    }

    protected void deleteBookmark() {
        Account account = mConversation.getAccount();
        Bookmark bookmark = mConversation.getBookmark();
        account.getBookmarks().remove(bookmark);
        bookmark.setConversation(null);
        xmppConnectionService.pushBookmarks(account);
        updateView();
    }

    protected void destroyRoom() {
        final boolean groupChat = mConversation != null && mConversation.isPrivateAndNonAnonymous();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(groupChat ? R.string.destroy_room : R.string.destroy_channel);
        builder.setMessage(groupChat ? R.string.destroy_room_dialog : R.string.destroy_channel_dialog);
        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            xmppConnectionService.destroyRoom(mConversation, ConferenceDetailsActivity.this);
        });
        builder.setNegativeButton(R.string.cancel, null);
        final AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    @Override
    void onBackendConnected() {
        if (mPendingConferenceInvite != null) {
            mPendingConferenceInvite.execute(this);
            mPendingConferenceInvite = null;
        }
        if (getIntent().getAction().equals(ACTION_VIEW_MUC)) {
            this.uuid = getIntent().getExtras().getString("uuid");
        }
        if (uuid != null) {
            this.mConversation = xmppConnectionService.findConversationByUuid(uuid);
            if (this.mConversation != null) {
                if (Compatibility.hasStoragePermission(this)) {
                    final int limit = GridManager.getCurrentColumnCount(this.binding.media);
                    xmppConnectionService.getAttachments(this.mConversation, limit, this);
                    this.binding.showMedia.setOnClickListener((v) -> MediaBrowserActivity.launch(this, mConversation));
                }
                updateView();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (this.binding.mucEditor.getVisibility() == View.VISIBLE) {
            hideEditor();
        } else {
            super.onBackPressed();
        }
    }

    private void updateView() {
        invalidateOptionsMenu();
        final MucOptions mucOptions = mConversation.getMucOptions();
        final User self = mucOptions.getSelf();
        String account;
        if (Config.DOMAIN_LOCK != null) {
            account = mConversation.getAccount().getJid().getLocal();
        } else {
            account = mConversation.getAccount().getJid().asBareJid().toString();
        }
        setTitle(mucOptions.isPrivateAndNonAnonymous() ? R.string.action_muc_details : R.string.channel_details);
        this.binding.editMucNameButton.setVisibility((self.getAffiliation().ranks(MucOptions.Affiliation.OWNER)) ? View.VISIBLE : View.GONE);
        AvatarWorkerTask.loadAvatar(mConversation, binding.yourPhoto, R.dimen.avatar_on_details_screen_size);
        String roomName = mucOptions.getName();
        String subject = mucOptions.getSubject();
        final boolean hasTitle;
        if (printableValue(roomName)) {
            //for groups next 2
            this.binding.mucTitle.setText("#"+EmojiWrapper.transform(roomName));
            this.binding.mucTitle.setVisibility(View.VISIBLE);
            hasTitle = true;
        } else if (!printableValue(subject)) {
            this.binding.mucTitle.setText("#"+EmojiWrapper.transform(mConversation.getName()));
            hasTitle = true;
            this.binding.mucTitle.setVisibility(View.VISIBLE);
        } else {
            this.binding.mucTitle.setText("#"+EmojiWrapper.transform(mConversation.getName()));
            hasTitle = true;
            this.binding.mucTitle.setVisibility(View.VISIBLE);
        }
        this.binding.mucYourNick.setText(mucOptions.getActualNick());
        if (mucOptions.online()) {
            this.binding.usersWrapper.setVisibility(View.VISIBLE);
            this.binding.mucInfoMore.setVisibility(this.mAdvancedMode ? View.VISIBLE : View.GONE);
            this.binding.mucRole.setVisibility(View.VISIBLE);
            this.binding.mucRole.setText(getStatus(self));
            if (mucOptions.getSelf().getAffiliation().ranks(MucOptions.Affiliation.OWNER)) {
                this.binding.mucSettings.setVisibility(View.VISIBLE);
                this.binding.mucConferenceType.setText(MucConfiguration.describe(this, mucOptions));
            } else if (!mucOptions.isPrivateAndNonAnonymous() && mucOptions.nonanonymous()) {
                this.binding.mucSettings.setVisibility(View.VISIBLE);
                this.binding.mucConferenceType.setText(R.string.group_chat_will_make_your_jabber_id_public);
            }
            else if(!mucOptions.getSelf().getAffiliation().ranks(MucOptions.Affiliation.OWNER) && mucOptions.isPrivateAndNonAnonymous()){
                this.binding.mucSettings.setVisibility(View.VISIBLE);
                this.binding.mucConferenceType.setText(R.string.anyone_can_invite_others);
            }else {
                this.binding.mucSettings.setVisibility(View.GONE);
            }
            if (mucOptions.mamSupport()) {
                this.binding.mucInfoMam.setText(R.string.server_info_available);
            } else {
                this.binding.mucInfoMam.setText(R.string.server_info_unavailable);
            }
            this.binding.changeConferenceButton.setVisibility(View.GONE);
        } else {
            this.binding.usersWrapper.setVisibility(View.GONE);
            this.binding.mucInfoMore.setVisibility(View.GONE);
            this.binding.mucSettings.setVisibility(View.GONE);
        }

        int ic_notifications = getThemeResource(R.attr.icon_notifications, R.drawable.ic_notifications_black_24dp);
        int ic_notifications_off = getThemeResource(R.attr.icon_notifications_off, R.drawable.ic_notifications_off_black_24dp);
        int ic_notifications_paused = getThemeResource(R.attr.icon_notifications_paused, R.drawable.ic_notifications_paused_black_24dp);
        int ic_notifications_none = getThemeResource(R.attr.icon_notifications_none, R.drawable.ic_notifications_none_black_24dp);

        long mutedTill = mConversation.getLongAttribute(Conversation.ATTRIBUTE_MUTED_TILL, 0);
        if (mutedTill == Long.MAX_VALUE) {
            this.binding.notificationStatusText.setText(R.string.notify_never);
            this.binding.notificationStatusButton.setImageResource(ic_notifications_off);
        } else if (System.currentTimeMillis() < mutedTill) {
            this.binding.notificationStatusText.setText(R.string.notify_paused);
            this.binding.notificationStatusButton.setImageResource(ic_notifications_paused);
        } else if (mConversation.alwaysNotify()) {
            this.binding.notificationStatusText.setText(R.string.notify_on_all_messages);
            this.binding.notificationStatusButton.setImageResource(ic_notifications);
        } else {
            this.binding.notificationStatusText.setText(R.string.notify_only_when_highlighted);
            this.binding.notificationStatusButton.setImageResource(ic_notifications_none);
        }
        final List<User> users = mucOptions.getUsers();
        Collections.sort(users, (a, b) -> {
            if (b.getAffiliation().outranks(a.getAffiliation())) {
                return 1;
            } else if (a.getAffiliation().outranks(b.getAffiliation())) {
                return -1;
            } else {
                if (a.getAvatar() != null && b.getAvatar() == null) {
                    return -1;
                } else if (a.getAvatar() == null && b.getAvatar() != null) {
                    return 1;
                } else {
                    return a.getComparableName().compareToIgnoreCase(b.getComparableName());
                }
            }
        });
        this.mUserPreviewAdapter.submitList(MucOptions.sub(users, GridManager.getCurrentColumnCount(binding.users)));
        this.binding.invite.setVisibility(mucOptions.canInvite() ? View.VISIBLE : View.GONE);
        this.binding.showUsers.setVisibility(users.size() > 0 ? View.VISIBLE : View.GONE);
        this.binding.usersWrapper.setVisibility(users.size() > 0 || mucOptions.canInvite() ? View.VISIBLE : View.GONE);
        if (users.size() == 0) {
            this.binding.noUsersHints.setText(mucOptions.isPrivateAndNonAnonymous() ? R.string.no_users_hint_group_chat : R.string.no_users_hint_channel);
            this.binding.noUsersHints.setVisibility(View.VISIBLE);
        } else {
            this.binding.noUsersHints.setVisibility(View.GONE);
        }

    }

    public static String getStatus(Context context, User user, final boolean advanced) {
        if (advanced) {
            return String.format("%s (%s)", context.getString(user.getAffiliation().getResId()), context.getString(user.getRole().getResId()));
        } else {
            return context.getString(user.getAffiliation().getResId());
        }
    }

    private String getStatus(User user) {
        return getStatus(this, user, mAdvancedMode);
    }


    @Override
    public void onAffiliationChangedSuccessful(Jid jid) {
        refreshUi();
    }

    @Override
    public void onAffiliationChangeFailed(Jid jid, int resId) {
        displayToast(getString(resId, jid.asBareJid().toString()));
    }

    @Override
    public void onRoomDestroySucceeded() {
        finish();
    }

    @Override
    public void onRoomDestroyFailed() {
        final boolean groupChat = mConversation != null && mConversation.isPrivateAndNonAnonymous();
        displayToast(getString(groupChat ? R.string.could_not_destroy_room : R.string.could_not_destroy_channel));
    }

    @Override
    public void onPushSucceeded() {
        displayToast(getString(R.string.modified_conference_options));
    }

    @Override
    public void onPushFailed() {
        displayToast(getString(R.string.could_not_modify_conference_options));
    }

    private void displayToast(final String msg) {
        runOnUiThread(() -> {
            if (isFinishing()) {
                return;
            }
            ToastCompat.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        if (mConversation == null) {
            return;
        }
        final MucOptions mucOptions = mConversation.getMucOptions();
        if (this.binding.mucEditor.getVisibility() == View.VISIBLE) {
            boolean subjectChanged = false;//changed(binding.mucEditSubject.getEditableText().toString(), mucOptions.getSubject());
            boolean nameChanged = changed(binding.mucEditTitle.getEditableText().toString(), mucOptions.getName());
            if (subjectChanged || nameChanged) {
                this.binding.editMucNameButton.setImageResource(getThemeResource(R.attr.icon_save, R.drawable.ic_save_black_24dp));
            } else {
                this.binding.editMucNameButton.setImageResource(getThemeResource(R.attr.icon_cancel, R.drawable.ic_cancel_black_24dp));
            }
        }
    }

}