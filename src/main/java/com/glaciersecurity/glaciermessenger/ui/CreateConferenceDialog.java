package com.glaciersecurity.glaciermessenger.ui;

import android.app.Dialog;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.databinding.CreateConferenceDialogBinding;
import com.glaciersecurity.glaciermessenger.ui.util.DelayedHintHelper;

public class CreateConferenceDialog extends DialogFragment {

    private static final String ACCOUNTS_LIST_KEY = "activated_accounts_list";
    private CreateConferenceDialogListener mListener;

    public static CreateConferenceDialog newInstance(List<String> accounts) {
        CreateConferenceDialog dialog = new CreateConferenceDialog();
        Bundle bundle =  new Bundle();
        bundle.putStringArrayList(ACCOUNTS_LIST_KEY, (ArrayList<String>) accounts);
        dialog.setArguments(bundle);
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_title_create_conference);
        CreateConferenceDialogBinding binding = DataBindingUtil.inflate(getActivity().getLayoutInflater(), R.layout.create_conference_dialog, null, false);
        builder.setView(binding.getRoot());
        builder.setPositiveButton(R.string.choose_participants, (dialog, which) -> mListener.onCreateDialogPositiveClick(null, binding.groupChatName.getText().toString().trim(), binding.inviteOnly, binding.groupChatName));
        builder.setNegativeButton(R.string.cancel, null);
        DelayedHintHelper.setHint(R.string.conference_address_example, binding.groupChatName);
        return builder.create();
    }

    public interface CreateConferenceDialogListener {
        void onCreateDialogPositiveClick(Spinner spinner, String subject, CheckBox inviteOnly, EditText editName);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (CreateConferenceDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement CreateConferenceDialogListener");
        }
    }

    @Override
    public void onDestroyView() {
        Dialog dialog = getDialog();
        if (dialog != null && getRetainInstance()) {
            dialog.setDismissMessage(null);
        }
        super.onDestroyView();
    }
}
