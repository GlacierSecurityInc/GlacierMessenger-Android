package com.glaciersecurity.glaciermessenger.ui.adapter;

import android.content.SharedPreferences;

import androidx.databinding.DataBindingUtil;

import android.os.Build;
import android.preference.PreferenceManager;

import androidx.appcompat.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.glaciersecurity.glaciermessenger.entities.Contact;
import com.glaciersecurity.glaciermessenger.ui.SettingsActivity;
import com.glaciersecurity.glaciermessenger.ui.util.AvatarWorkerTask;
import com.glaciersecurity.glaciermessenger.ui.util.StyledAttributes;
import com.wefika.flowlayout.FlowLayout;

import java.util.List;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.databinding.ContactBinding;
import com.glaciersecurity.glaciermessenger.entities.ListItem;
import com.glaciersecurity.glaciermessenger.ui.XmppActivity;
import com.glaciersecurity.glaciermessenger.utils.EmojiWrapper;
import com.glaciersecurity.glaciermessenger.utils.IrregularUnicodeDetector;

import rocks.xmpp.addr.Jid;

public class ListItemAdapter extends ArrayAdapter<ListItem> {

	protected XmppActivity activity;
	private boolean showDynamicTags = false;
	private OnTagClickedListener mOnTagClickedListener = null;
	private View.OnClickListener onTagTvClick = view -> {
		if (view instanceof TextView && mOnTagClickedListener != null){
			TextView tv = (TextView) view;
			final String tag = tv.getText().toString();
			mOnTagClickedListener.onTagClicked(tag);
		}
	};

	public OnContactClickedListener mOnContactClickedListener = null;
	public OnContactLongClickedListener mOnContactLongClickedListener = null;

	private View.OnClickListener onContactTvClick = (view)-> {
		if (view instanceof RelativeLayout && mOnContactClickedListener != null){
			RelativeLayout relativeLayout = (RelativeLayout) view;
			LinearLayout linearLayout= (LinearLayout) relativeLayout.getChildAt(1);
			AppCompatTextView textView = (AppCompatTextView) linearLayout.getChildAt(1);
			if (textView != null) {
				String jid = textView.getText().toString();
				mOnContactClickedListener.onContactClicked(jid);
			}
		}
	};

	private View.OnLongClickListener onContactLongTvClick = (view)-> {
			view.showContextMenu();
			return true;
	};


	public ListItemAdapter(XmppActivity activity, List<ListItem> objects) {
		super(activity, 0, objects);
		this.activity = activity;
	}

	public void refreshSettings() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
		this.showDynamicTags = preferences.getBoolean(SettingsActivity.SHOW_DYNAMIC_TAGS, false);
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		LayoutInflater inflater = activity.getLayoutInflater();
		ListItem item = getItem(position);
		ViewHolder viewHolder;
		if (view == null) {
			ContactBinding binding = DataBindingUtil.inflate(inflater,R.layout.contact,parent,false);
			viewHolder = ViewHolder.get(binding);
			view = binding.getRoot();
		} else {
			viewHolder = (ViewHolder) view.getTag();
		}
		if (Build.VERSION.SDK_INT >= 16) {
			view.setBackground(StyledAttributes.getDrawable(view.getContext(), R.attr.list_item_background));
		}

		List<ListItem.Tag> tags = item.getTags(activity);
		if (tags.size() == 0 || !this.showDynamicTags) {
			viewHolder.tags.setVisibility(View.GONE);
		} else {
			viewHolder.tags.setVisibility(View.VISIBLE);
			viewHolder.tags.removeAllViewsInLayout();
			for (ListItem.Tag tag : tags) {
				TextView tv = (TextView) inflater.inflate(R.layout.list_item_tag, viewHolder.tags, false);
				tv.setText(tag.getName());
				tv.setBackgroundColor(tag.getColor());
				tv.setOnClickListener(this.onTagTvClick);
				viewHolder.tags.addView(tv);
			}
		}
		final Jid jid = item.getJid();
		viewHolder.jid.setVisibility(View.INVISIBLE);
		if (jid != null) {
			// USERNAME - don't show jid in contact list
			viewHolder.jid.setText(IrregularUnicodeDetector.style(activity, jid));
		}
		viewHolder.name.setText(EmojiWrapper.transform(item.getDisplayName()));

		if (item.getShownStatusMessage() != null ) {
			viewHolder.statusMessage.setVisibility(View.VISIBLE);
			viewHolder.statusMessage.setText(item.getShownStatusMessage());
		} else
		{
			viewHolder.statusMessage.setVisibility(View.GONE);
		}
		if (item.getShownStatus() != null ) {
			viewHolder.status.setVisibility(View.VISIBLE);
			viewHolder.status.setImageResource(item.getShownStatus().getStatusIconMenu());
		} else
		{
			viewHolder.status.setVisibility(View.GONE);
		}
		AvatarWorkerTask.loadAvatar(item, viewHolder.avatar, R.dimen.avatar);
		if (item instanceof Contact) {
			viewHolder.contact_field.setOnClickListener(this.onContactTvClick);
			viewHolder.contact_field.setOnLongClickListener(this.onContactLongTvClick);
		}
		return view;
	}

	public void setOnTagClickedListener(OnTagClickedListener listener) {
		this.mOnTagClickedListener = listener;
	}

	public interface OnTagClickedListener {
		void onTagClicked(String tag);
	}

	public void setOnContactClickedListener(OnContactClickedListener listener) {
		this.mOnContactClickedListener = listener;
	}

	public void setOnContactLongClickedListener(OnContactLongClickedListener listener) {
		this.mOnContactLongClickedListener = listener;
	}

	public interface OnContactClickedListener {
		void onContactClicked(String contactJidString);
	}

	public interface OnContactLongClickedListener {
		void onContactClicked(String contactJidString);
	}

	private static class ViewHolder {
		private TextView name;
		private TextView jid;
		private ImageView avatar;
		private ImageButton status;
		private TextView statusMessage;
		private FlowLayout tags;
		private RelativeLayout contact_field;

		private ViewHolder() {

		}

		public static ViewHolder get(ContactBinding binding) {
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.name = binding.contactDisplayName;
			viewHolder.status = binding.contactStatusIcon;
			viewHolder.statusMessage = binding.contactStatusMessage;
			viewHolder.jid = binding.contactJid;
			viewHolder.avatar = binding.contactPhoto;
			viewHolder.tags = binding.tags;
			viewHolder.contact_field = binding.contactSelect;
			binding.getRoot().setTag(viewHolder);
			return viewHolder;
		}

	}

}
