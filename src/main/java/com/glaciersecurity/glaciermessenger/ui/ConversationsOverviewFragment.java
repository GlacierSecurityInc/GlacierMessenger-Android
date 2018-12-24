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
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.databinding.FragmentConversationsOverviewBinding;
import com.glaciersecurity.glaciermessenger.entities.Conversation;
import com.glaciersecurity.glaciermessenger.persistance.FileBackend;
import com.glaciersecurity.glaciermessenger.ui.adapter.ConversationAdapter;
import com.glaciersecurity.glaciermessenger.ui.interfaces.OnConversationArchived;
import com.glaciersecurity.glaciermessenger.ui.interfaces.OnConversationSelected;
import com.glaciersecurity.glaciermessenger.ui.util.Color;
import com.glaciersecurity.glaciermessenger.ui.util.MenuDoubleTabUtil;
import com.glaciersecurity.glaciermessenger.ui.util.PendingActionHelper;
import com.glaciersecurity.glaciermessenger.ui.util.PendingItem;
import com.glaciersecurity.glaciermessenger.ui.util.ScrollState;
import com.glaciersecurity.glaciermessenger.utils.ThemeHelper;

import static android.support.v7.widget.helper.ItemTouchHelper.LEFT;
import static android.support.v7.widget.helper.ItemTouchHelper.RIGHT;

public class ConversationsOverviewFragment extends XmppFragment {

	private static final String STATE_SCROLL_POSITION = ConversationsOverviewFragment.class.getName()+".scroll_state";

	private final List<Conversation> conversations = new ArrayList<>();
	private final PendingItem<Conversation> swipedConversation = new PendingItem<>();
	private final PendingItem<ScrollState> pendingScrollState = new PendingItem<>();
	private FragmentConversationsOverviewBinding binding;
	private ConversationAdapter conversationsAdapter;
	private XmppActivity activity;
	private float mSwipeEscapeVelocity = 0f;
	private PendingActionHelper pendingActionHelper = new PendingActionHelper();

	private ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0,LEFT|RIGHT) {
		@Override
		public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
			//todo maybe we can manually changing the position of the conversation
			return false;
		}

		@Override
		public float getSwipeEscapeVelocity (float defaultValue) {
			return mSwipeEscapeVelocity;
		}

		@Override
		public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
									float dX, float dY, int actionState, boolean isCurrentlyActive) {
			super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
			if(actionState != ItemTouchHelper.ACTION_STATE_IDLE){
				Paint paint = new Paint();
				paint.setColor(Color.get(activity,R.attr.conversations_overview_background));
				paint.setStyle(Paint.Style.FILL);
				c.drawRect(viewHolder.itemView.getLeft(),viewHolder.itemView.getTop()
						,viewHolder.itemView.getRight(),viewHolder.itemView.getBottom(), paint);
			}
		}

		@Override
		public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
			super.clearView(recyclerView, viewHolder);
			viewHolder.itemView.setAlpha(1f);
		}

		@Override
		public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
			pendingActionHelper.execute();
			int position = viewHolder.getLayoutPosition();
			try {
				swipedConversation.push(conversations.get(position));
			} catch (IndexOutOfBoundsException e) {
				return;
			}
			conversationsAdapter.remove(swipedConversation.peek(), position);
			activity.xmppConnectionService.markRead(swipedConversation.peek());

			if (position == 0 && conversationsAdapter.getItemCount() == 0) {
				final Conversation c = swipedConversation.pop();
				activity.xmppConnectionService.archiveConversation(c);
				return;
			}
			final boolean formerlySelected = ConversationFragment.getConversation(getActivity()) == swipedConversation.peek();
			if (activity instanceof OnConversationArchived) {
				((OnConversationArchived) activity).onConversationArchived(swipedConversation.peek());
			}
			boolean isMuc = swipedConversation.peek().getMode() == Conversation.MODE_MULTI;
			int title = isMuc ? R.string.title_undo_swipe_out_muc : R.string.title_undo_swipe_out_conversation;

			final Snackbar snackbar = Snackbar.make(binding.list, title, 5000)
					.setAction(R.string.undo, v -> {
						pendingActionHelper.undo();
						Conversation c = swipedConversation.pop();
						conversationsAdapter.insert(c, position);
						if (formerlySelected) {
							if (activity instanceof OnConversationSelected) {
								((OnConversationSelected) activity).onConversationSelected(c);
							}
						}
						LinearLayoutManager layoutManager = (LinearLayoutManager) binding.list.getLayoutManager();
						if (position > layoutManager.findLastVisibleItemPosition()) {
							binding.list.smoothScrollToPosition(position);
						}
					})
					.addCallback(new Snackbar.Callback() {
						@Override
						public void onDismissed(Snackbar transientBottomBar, int event) {
							switch (event) {
								case DISMISS_EVENT_SWIPE:
								case DISMISS_EVENT_TIMEOUT:
									pendingActionHelper.execute();
									break;
							}
						}
					});

			pendingActionHelper.push(() -> {
				if (snackbar.isShownOrQueued()) {
					snackbar.dismiss();
				}
				Conversation c = swipedConversation.pop();
				if(c != null){
					if (!c.isRead() && c.getMode() == Conversation.MODE_SINGLE) {
						return;
					}
					activity.xmppConnectionService.archiveConversation(c);
				}
			});

			ThemeHelper.fix(snackbar);
			snackbar.show();
		}
	};

	private ItemTouchHelper touchHelper = new ItemTouchHelper(callback);

	public static Conversation getSuggestion(Activity activity) {
		final Conversation exception;
		Fragment fragment = activity.getFragmentManager().findFragmentById(R.id.main_fragment);
		if (fragment != null && fragment instanceof ConversationsOverviewFragment) {
			exception = ((ConversationsOverviewFragment) fragment).swipedConversation.peek();
		} else {
			exception = null;
		}
		return getSuggestion(activity, exception);
	}

	public static Conversation getSuggestion(Activity activity, Conversation exception) {
		Fragment fragment = activity.getFragmentManager().findFragmentById(R.id.main_fragment);
		if (fragment != null && fragment instanceof ConversationsOverviewFragment) {
			List<Conversation> conversations = ((ConversationsOverviewFragment) fragment).conversations;
			if (conversations.size() > 0) {
				Conversation suggestion = conversations.get(0);
				if (suggestion == exception) {
					if (conversations.size() > 1) {
						return conversations.get(1);
					}
				} else {
					return suggestion;
				}
			}
		}
		return null;

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (savedInstanceState == null) {
			return;
		}
		pendingScrollState.push(savedInstanceState.getParcelable(STATE_SCROLL_POSITION));
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof XmppActivity) {
			this.activity = (XmppActivity) activity;
		} else {
			throw new IllegalStateException("Trying to attach fragment to activity that is not an XmppActivity");
		}
	}

	@Override
	public void onPause() {
		Log.d(Config.LOGTAG,"ConversationsOverviewFragment.onPause()");
		pendingActionHelper.execute();
		super.onPause();
	}

	@Override
	public void onDetach() {
		super.onDetach();
		this.activity = null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.mSwipeEscapeVelocity = getResources().getDimension(R.dimen.swipe_escape_velocity);
		this.binding = DataBindingUtil.inflate(inflater, R.layout.fragment_conversations_overview, container, false);
		this.binding.fab.setOnClickListener((view) -> StartConversationActivity.launch(getActivity()));

		this.conversationsAdapter = new ConversationAdapter(this.activity, this.conversations);
		this.conversationsAdapter.setConversationClickListener((view, conversation) -> {
			if (activity instanceof OnConversationSelected) {
				((OnConversationSelected) activity).onConversationSelected(conversation);
			} else {
				Log.w(ConversationsOverviewFragment.class.getCanonicalName(), "Activity does not implement OnConversationSelected");
			}
		});
		this.binding.list.setAdapter(this.conversationsAdapter);
		this.binding.list.setLayoutManager(new LinearLayoutManager(getActivity(),LinearLayoutManager.VERTICAL,false));
		this.touchHelper.attachToRecyclerView(this.binding.list);
		return binding.getRoot();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		menuInflater.inflate(R.menu.fragment_conversations_overview, menu);
	}

	@Override
	public void onBackendConnected() {
		refresh();
	}

	@Override
	public void onSaveInstanceState(Bundle bundle) {
		super.onSaveInstanceState(bundle);
		ScrollState scrollState = getScrollState();
		if (scrollState != null) {
			bundle.putParcelable(STATE_SCROLL_POSITION, scrollState);
		}
	}

	private ScrollState getScrollState() {
		if (this.binding == null) {
			return null;
		}
		LinearLayoutManager layoutManager = (LinearLayoutManager) this.binding.list.getLayoutManager();
		int position = layoutManager.findFirstVisibleItemPosition();
		final View view = this.binding.list.getChildAt(0);
		if (view != null) {
			return new ScrollState(position,view.getTop());
		} else {
			return new ScrollState(position, 0);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d(Config.LOGTAG, "ConversationsOverviewFragment.onStart()");
		if (activity.xmppConnectionService != null) {
			refresh();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(Config.LOGTAG, "ConversationsOverviewFragment.onResume()");
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (MenuDoubleTabUtil.shouldIgnoreTap()) {
			return false;
		}
		switch (item.getItemId()) {
			case R.id.action_search:
				startActivity(new Intent(getActivity(), SearchActivity.class));
				return true;
			case R.id.action_wipe_all_history:
				wipeAllHistoryDialog();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	void refresh() {
		if (this.binding == null || this.activity == null) {
			Log.d(Config.LOGTAG,"ConversationsOverviewFragment.refresh() skipped updated because view binding or activity was null");
			return;
		}
		this.activity.xmppConnectionService.populateWithOrderedConversations(this.conversations);
		Conversation removed = this.swipedConversation.peek();
		if (removed != null) {
			if (removed.isRead()) {
				this.conversations.remove(removed);
			} else {
				pendingActionHelper.execute();
			}
		}
		this.conversationsAdapter.notifyDataSetChanged();
		ScrollState scrollState = pendingScrollState.pop();
		if (scrollState != null) {
			setScrollPosition(scrollState);
		}
	}

	private void setScrollPosition(ScrollState scrollPosition) {
		if (scrollPosition != null) {
			LinearLayoutManager layoutManager = (LinearLayoutManager) binding.list.getLayoutManager();
			layoutManager.scrollToPositionWithOffset(scrollPosition.position, scrollPosition.offset);
		}
	}









	/**
	 * end ALL existing conversations
	 */
	@SuppressLint("InflateParams")
	protected void wipeAllHistoryDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(getString(R.string.action_wipe_all_history));
		View dialogView = getActivity().getLayoutInflater().inflate(
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
			for (int i = (conversations.size() - 1); i >= 0; i--) {

				// delete messages
				if (deleteAllMessagesEachChatCheckBox.isChecked()) {
					activity.xmppConnectionService.clearConversationHistory(conversations.get(i));
					((ConversationsActivity) getActivity()).onConversationsListItemUpdated();
					refresh();
				}

				// end chat
				if (endAllChatCheckBox.isChecked()) {
					activity.xmppConnectionService.archiveConversation(conversations.get(i));
				}
			}

			// delete everything in cache
			if (deleteAllCachedFilesCheckBox.isChecked()) {
				clearCachedFiles();
			}
		});
		builder.create().show();
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
	}

	/**
	 * clear storage area
	 */
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
					com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER", "File list: Successfully deleted " + fileDir[i]);
				} else {
					com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER", "File list: Did not delete " + fileDir[i]);
				}
			}

			// Need to do something to update after deleting
			// String[] delFile = {fileDir[fileDir.length-1].toString()};
			callBroadcast(deletedFiles);
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
					com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER", "File list: Successfully deleted " + fileDir[i]);
				} else {
					com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER", "File list: Did not delete " + fileDir[i]);
				}
			}

			// Need to do something to update after deleting
			// String[] delFile = {fileDir[fileDir.length-1].toString()};
			callBroadcast(deletedFiles);
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
					com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER", "File list: Successfully deleted " + fileDir[i]);
				} else {
					com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER", "File list: Did not delete " + fileDir[i]);
				}
			}

			// Need to do something to update after deleting
			// String[] delFile = {fileDir[fileDir.length-1].toString()};
			callBroadcast(deletedFiles);
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
					com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER", "File list: Successfully deleted " + fileDir[i]);
				} else {
					com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER", "File list: Did not delete " + fileDir[i]);
				}
			}

			// Need to do something to update after deleting
			// String[] delFile = {fileDir[fileDir.length-1].toString()};
			callBroadcast(deletedFiles);
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
						com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER", "File list: Successfully deleted " + fileDir[i]);
					} else {
						com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER", "File list: Did not delete " + fileDir[i]);
					}
				}
				if (f_exts2.delete()) {
					deletedFiles.add(f_exts2.toString());
				}
			}

			if (f_exts.delete()) {
				deletedFiles.add(f_exts.toString());
			}

			// Need to do something to update after deleting
			String[] stringArray = deletedFiles.toArray(new String[0]);
			callBroadcast(deletedFiles.toArray(new String[0]));
		}
	}

	/**
	 * Notify everyone of file status change.  Must send specific files (not necessarily directories.
	 */
	private void callBroadcast(String[] files) {
		if (Build.VERSION.SDK_INT >= 14) {
			com.glaciersecurity.glaciermessenger.utils.Log.d("-->", " >= 14");
			MediaScannerConnection.scanFile(getActivity(), files, null, new MediaScannerConnection.OnScanCompletedListener() {
				/*
				 *   (non-Javadoc)
				 * @see android.media.MediaScannerConnection.OnScanCompletedListener#onScanCompleted(java.lang.String, android.net.Uri)
				 */
				public void onScanCompleted(String path, Uri uri) {
					com.glaciersecurity.glaciermessenger.utils.Log.d("ExternalStorage", "Scanned " + path + ":");
					com.glaciersecurity.glaciermessenger.utils.Log.d("ExternalStorage", "-> uri=" + uri);
				}
			});
		} else {
			com.glaciersecurity.glaciermessenger.utils.Log.d("-->", " < 14");
			getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
					Uri.parse("file://" + Environment.getExternalStorageDirectory())));
		}
	}
}
