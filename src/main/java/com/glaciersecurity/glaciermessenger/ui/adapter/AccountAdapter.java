package com.glaciersecurity.glaciermessenger.ui.adapter;

import androidx.databinding.DataBindingUtil;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.databinding.AccountRowBinding;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.ui.XmppActivity;
import com.glaciersecurity.glaciermessenger.ui.util.AvatarWorkerTask;
import com.glaciersecurity.glaciermessenger.ui.util.StyledAttributes;

public class AccountAdapter extends ArrayAdapter<Account> {

	private XmppActivity activity;
	private boolean showStateButton;

	public AccountAdapter(XmppActivity activity, List<Account> objects, boolean showStateButton) {
		super(activity, 0, objects);
		this.activity = activity;
		this.showStateButton = showStateButton;
	}

	public AccountAdapter(XmppActivity activity, List<Account> objects) {
		super(activity, 0, objects);
		this.activity = activity;
		this.showStateButton = true;
	}

	@Override
	public View getView(int position, View view, @NonNull ViewGroup parent) {
		final Account account = getItem(position);
		final ViewHolder viewHolder;
		if (view == null) {
			AccountRowBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.account_row, parent, false);
			view = binding.getRoot();
			viewHolder = new ViewHolder(binding);
			view.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) view.getTag();
		}
		// USERNAME - display only username
		viewHolder.binding.accountJid.setText(account.getUsername());
		/*if (Config.DOMAIN_LOCK != null) {
			viewHolder.binding.accountJid.setText(account.getJid().getLocal());
		} else {
			viewHolder.binding.accountJid.setText(account.getJid().asBareJid().toString());
		}*/
		AvatarWorkerTask.loadAvatar(account, viewHolder.binding.accountImage, R.dimen.avatar);
		viewHolder.binding.accountStatus.setText(getContext().getString(account.getStatus().getReadableId()));
		switch (account.getStatus()) {
			case ONLINE:
				viewHolder.binding.accountStatus.setTextColor(StyledAttributes.getColor(activity, R.attr.TextColorOnline));
				break;
			case DISABLED:
			case CONNECTING:
				viewHolder.binding.accountStatus.setTextColor(StyledAttributes.getColor(activity, android.R.attr.textColorSecondary));
				break;
			default:
				viewHolder.binding.accountStatus.setTextColor(StyledAttributes.getColor(activity, R.attr.TextColorError));
				break;
		}
		final boolean isDisabled = (account.getStatus() == Account.State.DISABLED);
		viewHolder.binding.tglAccountStatus.setOnCheckedChangeListener(null);
		viewHolder.binding.tglAccountStatus.setChecked(!isDisabled);
		if (this.showStateButton) {
			viewHolder.binding.tglAccountStatus.setVisibility(View.VISIBLE);
		} else {
			viewHolder.binding.tglAccountStatus.setVisibility(View.GONE);
		}
		viewHolder.binding.tglAccountStatus.setOnCheckedChangeListener((compoundButton, b) -> {
			if (b == isDisabled && activity instanceof OnTglAccountState) {
				((OnTglAccountState) activity).onClickTglAccountState(account, b);
			}
		});
		return view;
	}

	private static class ViewHolder {
		private final AccountRowBinding binding;

		private ViewHolder(AccountRowBinding binding) {
			this.binding = binding;
		}
	}



	public interface OnTglAccountState {
		void onClickTglAccountState(Account account, boolean state);
	}

}