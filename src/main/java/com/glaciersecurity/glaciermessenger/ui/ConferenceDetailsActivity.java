package com.glaciersecurity.glaciermessenger.ui;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

import org.openintents.openpgp.util.OpenPgpUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.crypto.PgpEngine;
import com.glaciersecurity.glaciermessenger.databinding.ActivityMucDetailsBinding;
import com.glaciersecurity.glaciermessenger.databinding.ContactBinding;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.Bookmark;
import com.glaciersecurity.glaciermessenger.entities.Contact;
import com.glaciersecurity.glaciermessenger.entities.Conversation;
import com.glaciersecurity.glaciermessenger.entities.Message;
import com.glaciersecurity.glaciermessenger.entities.MucOptions;
import com.glaciersecurity.glaciermessenger.entities.MucOptions.User;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService.OnConversationUpdate;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService.OnMucRosterUpdate;
import com.glaciersecurity.glaciermessenger.ui.util.MenuDoubleTabUtil;
import com.glaciersecurity.glaciermessenger.ui.util.MyLinkify;
import com.glaciersecurity.glaciermessenger.ui.util.SoftKeyboardUtils;
import com.glaciersecurity.glaciermessenger.utils.EmojiWrapper;
import com.glaciersecurity.glaciermessenger.utils.StringUtils;
import com.glaciersecurity.glaciermessenger.utils.StylingHelper;
import com.glaciersecurity.glaciermessenger.utils.UIHelper;
import com.glaciersecurity.glaciermessenger.utils.XmppUri;
import rocks.xmpp.addr.Jid;

import static com.glaciersecurity.glaciermessenger.entities.Bookmark.printableValue;
import static com.glaciersecurity.glaciermessenger.utils.StringUtils.changed;

public class ConferenceDetailsActivity extends XmppActivity implements OnConversationUpdate, OnMucRosterUpdate, XmppConnectionService.OnAffiliationChanged, XmppConnectionService.OnRoleChanged, XmppConnectionService.OnConfigurationPushed, TextWatcher {
    public static final String ACTION_VIEW_MUC = "view_muc";

    private static final float INACTIVE_ALPHA = 0.4684f; //compromise between dark and light theme

    private Conversation mConversation;
    private OnClickListener inviteListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            inviteToConversation(mConversation);

            if ((mConversation.getMode() == Conversation.MODE_MULTI) && !(mConversation.getMucOptions().membersOnly())) {
                mConversation.setNextEncryption(Message.ENCRYPTION_NONE);
            } else {
                mConversation.setNextEncryption(Message.ENCRYPTION_AXOLOTL);
            }
        }
    };
    private ActivityMucDetailsBinding binding;
    private String uuid = null;
    private User mSelectedUser = null;

    private boolean mAdvancedMode = false;

    private UiCallback<Conversation> renameCallback = new UiCallback<Conversation>() {
        @Override
        public void success(Conversation object) {
            runOnUiThread(() -> {
                Toast.makeText(ConferenceDetailsActivity.this, getString(R.string.your_nick_has_been_changed), Toast.LENGTH_SHORT).show();
                updateView();
            });

        }

        @Override
        public void error(final int errorCode, Conversation object) {
            runOnUiThread(() -> Toast.makeText(ConferenceDetailsActivity.this, getString(errorCode), Toast.LENGTH_SHORT).show());
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
            builder.setTitle(R.string.conference_options);
            final String[] options;
            final boolean[] values;
            if (mAdvancedMode) {
                options = new String[]{
                        getString(R.string.members_only),
                        getString(R.string.moderated),
                        getString(R.string.non_anonymous)
                };
                values = new boolean[]{
                        mucOptions.membersOnly(),
                        mucOptions.moderated(),
                        mucOptions.nonanonymous()
                };
            } else {
                options = new String[]{
                        getString(R.string.members_only),
                        getString(R.string.non_anonymous)
                };
                values = new boolean[]{
                        mucOptions.membersOnly(),
                        mucOptions.nonanonymous()
                };
            }
            builder.setMultiChoiceItems(options, values, (dialog, which, isChecked) -> values[which] = isChecked);
            builder.setNegativeButton(R.string.cancel, null);
            builder.setPositiveButton(R.string.confirm, (dialog, which) -> {
                if (!mucOptions.membersOnly() && values[0]) {
                    xmppConnectionService.changeAffiliationsInConference(mConversation,
                            MucOptions.Affiliation.NONE,
                            MucOptions.Affiliation.MEMBER);
                }
                Bundle options1 = new Bundle();
                options1.putString("muc#roomconfig_membersonly", values[0] ? "1" : "0");
                if (values.length == 2) {
                    options1.putString("muc#roomconfig_whois", values[1] ? "anyone" : "moderators");
                } else if (values.length == 3) {
                    options1.putString("muc#roomconfig_moderatedroom", values[1] ? "1" : "0");
                    options1.putString("muc#roomconfig_whois", values[2] ? "anyone" : "moderators");
                }
                options1.putString("muc#roomconfig_persistentroom", "1");
                final boolean whois = values.length == 2 ? values[1] : values[2];
                if (values[0] == whois) {
                    options1.putString("muc#roomconfig_publicroom", whois ? "0" : "1");
                }
                xmppConnectionService.pushConferenceConfiguration(mConversation,
                        options1,
                        ConferenceDetailsActivity.this);
            });
            builder.create().show();
        }
    };

    public static boolean cancelPotentialWork(User user, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final User old = bitmapWorkerTask.o;
            if (old == null || user != old) {
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
        this.binding.mucMoreDetails.setVisibility(View.GONE);
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
        this.binding.mucEditSubject.addTextChangedListener(this);
        this.binding.mucEditSubject.addTextChangedListener(new StylingHelper.MessageEditorStyler(this.binding.mucEditSubject));
    }

    @Override
    protected void onStart() {
        super.onStart();
        final int theme = findTheme();
        if (this.mTheme != theme) {
            recreate();
        }
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
            case R.id.action_share_http:
                shareLink(true);
                break;
            case R.id.action_share_uri:
                shareLink(false);
                break;
            case R.id.action_save_as_bookmark:
                saveAsBookmark();
                break;
            case R.id.action_delete_bookmark:
                deleteBookmark();
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
            final String subject = mucOptions.getSubject();
            this.binding.mucEditSubject.setText("");
            if (subject != null) {
                this.binding.mucEditSubject.append(subject);
            }
            this.binding.mucEditSubject.setEnabled(mucOptions.canChangeSubject());
            if (!owner) {
                this.binding.mucEditSubject.requestFocus();
            }
        } else {
            String subject = this.binding.mucEditSubject.isEnabled() ? this.binding.mucEditSubject.getEditableText().toString().trim() : null;
            String name = this.binding.mucEditTitle.isEnabled() ? this.binding.mucEditTitle.getEditableText().toString().trim() : null;
            onMucInfoUpdated(subject, name);
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
        if (mucOptions.canChangeSubject() && changed(mucOptions.getSubject(), subject)) {
            xmppConnectionService.pushSubjectToConference(mConversation, subject);
        }
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
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.muc_details, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        Object tag = v.getTag();
        if (tag instanceof User) {
            getMenuInflater().inflate(R.menu.muc_details_context, menu);
            final User user = (User) tag;
            final User self = mConversation.getMucOptions().getSelf();
            this.mSelectedUser = user;
            String name;
            final Contact contact = user.getContact();
            if (contact != null && contact.showInRoster()) {
                name = contact.getDisplayName();
            } else if (user.getRealJid() != null) {
            } else {
            }
            name = user.getName();

            menu.setHeaderTitle(name);

            if (user.getRealJid() != null) {
                MenuItem showContactDetails = menu.findItem(R.id.action_contact_details);
                MenuItem startConversation = menu.findItem(R.id.start_conversation);
                MenuItem giveMembership = menu.findItem(R.id.give_membership);
                MenuItem removeMembership = menu.findItem(R.id.remove_membership);
                MenuItem giveAdminPrivileges = menu.findItem(R.id.give_admin_privileges);
                MenuItem removeAdminPrivileges = menu.findItem(R.id.remove_admin_privileges);
                MenuItem removeFromRoom = menu.findItem(R.id.remove_from_room);
                MenuItem banFromConference = menu.findItem(R.id.ban_from_conference);
                MenuItem invite = menu.findItem(R.id.invite);
                startConversation.setVisible(true);
                if (contact != null && contact.showInRoster()) {
                    showContactDetails.setVisible(!contact.isSelf());
                }
                if (user.getRole() == MucOptions.Role.NONE) {
                    invite.setVisible(true);
                }
                if (self.getAffiliation().ranks(MucOptions.Affiliation.ADMIN) &&
                        self.getAffiliation().outranks(user.getAffiliation())) {
                    if (mAdvancedMode) {
                        if (user.getAffiliation() == MucOptions.Affiliation.NONE) {
                            giveMembership.setVisible(true);
                        } else {
                            removeMembership.setVisible(true);
                        }
                        if (!Config.DISABLE_BAN) {
                            banFromConference.setVisible(true);
                        }
                    } else {
                        if (!Config.DISABLE_BAN || mConversation.getMucOptions().membersOnly()) {
                            removeFromRoom.setVisible(true);
                        }
                    }
                    if (user.getAffiliation() != MucOptions.Affiliation.ADMIN) {
                        giveAdminPrivileges.setVisible(true);
                    } else {
                        removeAdminPrivileges.setVisible(true);
                    }
                }
            } else {
                MenuItem sendPrivateMessage = menu.findItem(R.id.send_private_message);
                sendPrivateMessage.setVisible(user.getRole().ranks(MucOptions.Role.VISITOR));
            }

        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Jid jid = mSelectedUser.getRealJid();
        switch (item.getItemId()) {
            case R.id.action_contact_details:
                Contact contact = mSelectedUser.getContact();
                if (contact != null) {
                    switchToContactDetails(contact);
                }
                return true;
            case R.id.start_conversation:
                startConversation(mSelectedUser);
                return true;
            case R.id.give_admin_privileges:
                xmppConnectionService.changeAffiliationInConference(mConversation, jid, MucOptions.Affiliation.ADMIN, this);
                return true;
            case R.id.give_membership:
                xmppConnectionService.changeAffiliationInConference(mConversation, jid, MucOptions.Affiliation.MEMBER, this);
                return true;
            case R.id.remove_membership:
                xmppConnectionService.changeAffiliationInConference(mConversation, jid, MucOptions.Affiliation.NONE, this);
                return true;
            case R.id.remove_admin_privileges:
                xmppConnectionService.changeAffiliationInConference(mConversation, jid, MucOptions.Affiliation.MEMBER, this);
                return true;
            case R.id.remove_from_room:
                removeFromRoom(mSelectedUser);
                return true;
            case R.id.ban_from_conference:
                xmppConnectionService.changeAffiliationInConference(mConversation, jid, MucOptions.Affiliation.OUTCAST, this);
                if (mSelectedUser.getRole() != MucOptions.Role.NONE) {
                    xmppConnectionService.changeRoleInConference(mConversation, mSelectedUser.getName(), MucOptions.Role.NONE, this);
                }
                return true;
            case R.id.send_private_message:
                if (mConversation.getMucOptions().allowPm()) {
                    privateMsgInMuc(mConversation, mSelectedUser.getName());
                } else {
                    Toast.makeText(this, R.string.private_messages_are_disabled, Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.invite:
                xmppConnectionService.directInvite(mConversation, jid);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void removeFromRoom(final User user) {
        if (mConversation.getMucOptions().membersOnly()) {
            xmppConnectionService.changeAffiliationInConference(mConversation, user.getRealJid(), MucOptions.Affiliation.NONE, this);
            if (user.getRole() != MucOptions.Role.NONE) {
                xmppConnectionService.changeRoleInConference(mConversation, mSelectedUser.getName(), MucOptions.Role.NONE, ConferenceDetailsActivity.this);
            }
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.ban_from_conference);
            builder.setMessage(getString(R.string.removing_from_public_conference, user.getName()));
            builder.setNegativeButton(R.string.cancel, null);
            builder.setPositiveButton(R.string.ban_now, (dialog, which) -> {
                xmppConnectionService.changeAffiliationInConference(mConversation, user.getRealJid(), MucOptions.Affiliation.OUTCAST, ConferenceDetailsActivity.this);
                if (user.getRole() != MucOptions.Role.NONE) {
                    xmppConnectionService.changeRoleInConference(mConversation, mSelectedUser.getName(), MucOptions.Role.NONE, ConferenceDetailsActivity.this);
                }
            });
            builder.create().show();
        }
    }

    protected void startConversation(User user) {
        if (user.getRealJid() != null) {
            Conversation conversation = xmppConnectionService.findOrCreateConversation(this.mConversation.getAccount(), user.getRealJid().asBareJid(), false, true);
            switchToConversation(conversation);
        }
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
        account = mConversation.getAccount().getJid().asBareJid().toString();

        this.binding.editMucNameButton.setVisibility((self.getAffiliation().ranks(MucOptions.Affiliation.OWNER) || mucOptions.canChangeSubject()) ? View.VISIBLE : View.GONE);
        this.binding.yourPhoto.setImageBitmap(avatarService().get(mConversation,(int) getResources().getDimension(R.dimen.avatar_on_details_screen_size)));
        String roomName = mucOptions.getName();
        String subject = mucOptions.getSubject();
        final boolean hasTitle;
        if (printableValue(roomName)) {
            this.binding.mucTitle.setText("#"+EmojiWrapper.transform(roomName));
            this.binding.mucTitle.setVisibility(View.VISIBLE);
            hasTitle = true;
        } else if (!printableValue(subject)) {
            this.binding.mucTitle.setText("#"+EmojiWrapper.transform(mConversation.getName()));
            hasTitle = true;
            this.binding.mucTitle.setVisibility(View.VISIBLE);
        } else {
            hasTitle = false;
            this.binding.mucTitle.setVisibility(View.GONE);
        }
        if (printableValue(subject)) {
            SpannableStringBuilder spannable = new SpannableStringBuilder(subject);
            StylingHelper.format(spannable, this.binding.mucSubject.getCurrentTextColor());
            MyLinkify.addLinks(spannable, false);
            this.binding.mucSubject.setText(EmojiWrapper.transform(spannable));
            this.binding.mucSubject.setTextAppearance(this,subject.length() > (hasTitle ? 128 : 196) ? R.style.TextAppearance_Conversations_Body1_Linkified : R.style.TextAppearance_Conversations_Subhead);
            this.binding.mucSubject.setAutoLinkMask(0);
            this.binding.mucSubject.setVisibility(View.VISIBLE);
        } else {
            this.binding.mucSubject.setVisibility(View.GONE);
        }
        this.binding.mucYourNick.setText(mucOptions.getActualNick());
        if (mucOptions.online()) {
            this.binding.mucMoreDetails.setVisibility(View.VISIBLE);
            this.binding.mucSettings.setVisibility(View.VISIBLE);
            this.binding.mucInfoMore.setVisibility(this.mAdvancedMode ? View.VISIBLE : View.GONE);
            this.binding.mucRole.setVisibility(View.VISIBLE);
            this.binding.mucRole.setText(getStatus(self));
            if (mucOptions.membersOnly()) {
                this.binding.mucConferenceType.setText(R.string.private_conference);
            } else {
                this.binding.mucConferenceType.setText(R.string.public_conference);
            }
            if (mucOptions.mamSupport()) {
                this.binding.mucInfoMam.setText(R.string.server_info_available);
            } else {
                this.binding.mucInfoMam.setText(R.string.server_info_unavailable);
            }
                this.binding.changeConferenceButton.setVisibility(View.GONE);
        } else {
            this.binding.mucMoreDetails.setVisibility(View.GONE);
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

        final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.binding.mucMembers.removeAllViews();
        if (inflater == null) {
            return;
        }
        final ArrayList<User> users = mucOptions.getUsers();
        Collections.sort(users);
        for (final User user : users) {
            ContactBinding binding = DataBindingUtil.inflate(inflater, R.layout.contact, this.binding.mucMembers, false);
            this.setListItemBackgroundOnView(binding.getRoot());
            binding.getRoot().setOnClickListener(view1 -> highlightInMuc(mConversation, user.getName()));
            registerForContextMenu(binding.getRoot());
            binding.getRoot().setTag(user);
            if (mAdvancedMode && user.getPgpKeyId() != 0) {
                binding.key.setVisibility(View.VISIBLE);
                binding.key.setOnClickListener(v -> viewPgpKey(user));
                binding.key.setText(OpenPgpUtils.convertKeyIdToHex(user.getPgpKeyId()));
            }
            Contact contact = user.getContact();
            String name = user.getName();
            if (contact != null) {
                binding.contactDisplayName.setText(contact.getDisplayName());
                //binding.contactJid.setText((name != null ? name + " \u2022 " : "") + getStatus(user));
                if (mConversation.getMode() == Conversation.MODE_MULTI) {
                    binding.contactDisplayName.setText(contact.getDisplayName());
                }
            } else {
                binding.contactDisplayName.setText(name == null ? "" : name);
                //binding.contactJid.setText(getStatus(user));

            }
            loadAvatar(user, binding.contactPhoto);
            if (user.getRole() == MucOptions.Role.NONE) {
                //binding.contactJid.setAlpha(INACTIVE_ALPHA);
                binding.key.setAlpha(INACTIVE_ALPHA);
                binding.contactDisplayName.setAlpha(INACTIVE_ALPHA);
                binding.contactPhoto.setAlpha(INACTIVE_ALPHA);
            }
            this.binding.mucMembers.addView(binding.getRoot());
            if (mConversation.getMucOptions().canInvite()) {
                this.binding.invite.setVisibility(View.VISIBLE);
            } else {
                this.binding.invite.setVisibility(View.GONE);
            }
        }
    }

    private String getStatus(User user) {
        if (mAdvancedMode) {
            return getString(user.getAffiliation().getResId()) +
                    " (" + getString(user.getRole().getResId()) + ')';
        } else {
            return getString(user.getAffiliation().getResId());
        }
    }

    private void viewPgpKey(User user) {
        PgpEngine pgp = xmppConnectionService.getPgpEngine();
        if (pgp != null) {
            PendingIntent intent = pgp.getIntentForKey(user.getPgpKeyId());
            if (intent != null) {
                try {
                    startIntentSenderForResult(intent.getIntentSender(), 0, null, 0, 0, 0);
                } catch (SendIntentException ignored) {

                }
            }
        }
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
    public void onRoleChangedSuccessful(String nick) {

    }

    @Override
    public void onRoleChangeFailed(String nick, int resId) {
        displayToast(getString(resId, nick));
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
        runOnUiThread(() -> Toast.makeText(ConferenceDetailsActivity.this, msg, Toast.LENGTH_SHORT).show());
    }

    public void loadAvatar(User user, ImageView imageView) {
        if (cancelPotentialWork(user, imageView)) {
            final Bitmap bm = avatarService().get(user, getPixel(48), true);
            if (bm != null) {
                cancelPotentialWork(user, imageView);
                imageView.setImageBitmap(bm);
                imageView.setBackgroundColor(0x00000000);
            } else {
                String seed = user.getRealJid() != null ? user.getRealJid().asBareJid().toString() : null;
                imageView.setBackgroundColor(UIHelper.getColorForName(seed == null ? user.getName() : seed));
                imageView.setImageDrawable(null);
                final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
                final AsyncDrawable asyncDrawable = new AsyncDrawable(getResources(), null, task);
                imageView.setImageDrawable(asyncDrawable);
                try {
                    task.execute(user);
                } catch (final RejectedExecutionException ignored) {
                }
            }
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
        if (mConversation == null) {
            return;
        }
        final MucOptions mucOptions = mConversation.getMucOptions();
        if (this.binding.mucEditor.getVisibility() == View.VISIBLE) {
            boolean subjectChanged = changed(binding.mucEditSubject.getEditableText().toString(), mucOptions.getSubject());
            boolean nameChanged = changed(binding.mucEditTitle.getEditableText().toString(), mucOptions.getName());
            if (subjectChanged || nameChanged) {
                this.binding.editMucNameButton.setImageResource(getThemeResource(R.attr.icon_save, R.drawable.ic_save_black_24dp));
            } else {
                this.binding.editMucNameButton.setImageResource(getThemeResource(R.attr.icon_cancel, R.drawable.ic_cancel_black_24dp));
            }
        }
    }

    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference = new WeakReference<>(bitmapWorkerTask);
        }

        BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    class BitmapWorkerTask extends AsyncTask<User, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private User o = null;

        private BitmapWorkerTask(ImageView imageView) {
            imageViewReference = new WeakReference<>(imageView);
        }

        @Override
        protected Bitmap doInBackground(User... params) {
            this.o = params[0];
            if (imageViewReference.get() == null) {
                return null;
            }
            return avatarService().get(this.o, getPixel(48), isCancelled());
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null && !isCancelled()) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                    imageView.setBackgroundColor(0x00000000);
                }
            }
        }
    }

}
