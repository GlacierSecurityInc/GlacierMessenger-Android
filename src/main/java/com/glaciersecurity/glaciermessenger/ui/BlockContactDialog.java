package com.glaciersecurity.glaciermessenger.ui;

import android.databinding.DataBindingUtil;
import android.support.v7.app.AlertDialog;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.TypefaceSpan;
import android.view.View;
import android.widget.Toast;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.databinding.DialogBlockContactBinding;
import com.glaciersecurity.glaciermessenger.entities.Blockable;
import com.glaciersecurity.glaciermessenger.entities.Conversation;
import rocks.xmpp.addr.Jid;

public final class BlockContactDialog {
	public static void show(final XmppActivity xmppActivity, final Blockable blockable) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(xmppActivity);
		final boolean isBlocked = blockable.isBlocked();
		builder.setNegativeButton(R.string.cancel, null);
		DialogBlockContactBinding binding = DataBindingUtil.inflate(xmppActivity.getLayoutInflater(), R.layout.dialog_block_contact, null, false);
		final boolean reporting = blockable.getAccount().getXmppConnection().getFeatures().spamReporting();
		binding.reportSpam.setVisibility(!isBlocked && reporting ? View.VISIBLE : View.GONE);
		builder.setView(binding.getRoot());

		String value;
		SpannableString spannable;
		if (blockable.getJid().getLocal() == null || blockable.getAccount().isBlocked(Jid.ofDomain(blockable.getJid().getDomain()))) {
			builder.setTitle(isBlocked ? R.string.action_unblock_domain : R.string.action_block_domain);
			value = Jid.ofDomain(blockable.getJid().getDomain()).toString();
			spannable = new SpannableString(xmppActivity.getString(isBlocked ? R.string.unblock_domain_text : R.string.block_domain_text, value));
		} else {
			int resBlockAction = blockable instanceof Conversation && ((Conversation) blockable).isWithStranger() ? R.string.block_stranger : R.string.action_block_contact;
			builder.setTitle(isBlocked ? R.string.action_unblock_contact : resBlockAction);
			value = blockable.getJid().asBareJid().getLocal();
			spannable = new SpannableString(xmppActivity.getString(isBlocked ? R.string.unblock_contact_text : R.string.block_contact_text, value));
		}
		int start = spannable.toString().indexOf(value);
		if (start >= 0) {
			spannable.setSpan(new TypefaceSpan("monospace"), start, start + value.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		binding.text.setText(spannable);
		builder.setPositiveButton(isBlocked ? R.string.unblock : R.string.block, (dialog, which) -> {
			if (isBlocked) {
				xmppActivity.xmppConnectionService.sendUnblockRequest(blockable);
			} else {
				boolean toastShown = false;
				if (xmppActivity.xmppConnectionService.sendBlockRequest(blockable, binding.reportSpam.isChecked())) {
					Toast.makeText(xmppActivity, R.string.corresponding_conversations_closed, Toast.LENGTH_SHORT).show();
					toastShown = true;
				}
				if (xmppActivity instanceof ContactDetailsActivity) {
					if (!toastShown) {
						Toast.makeText(xmppActivity, R.string.contact_blocked_past_tense, Toast.LENGTH_SHORT).show();
					}
					xmppActivity.finish();
				}
			}
		});
		builder.create().show();
	}
}
