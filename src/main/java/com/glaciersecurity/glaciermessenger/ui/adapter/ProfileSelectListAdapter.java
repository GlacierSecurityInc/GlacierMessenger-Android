package com.glaciersecurity.glaciermessenger.ui.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.renderscript.ScriptGroup;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.databinding.ContactBinding;
import com.glaciersecurity.glaciermessenger.databinding.RadioBinding;
import com.glaciersecurity.glaciermessenger.entities.ListItem;
import com.glaciersecurity.glaciermessenger.entities.GlacierProfile;
import com.glaciersecurity.glaciermessenger.ui.util.StyledAttributes;
import com.wefika.flowlayout.FlowLayout;

import java.util.Comparator;
import java.util.List;

public class ProfileSelectListAdapter<T> extends ArrayAdapter<GlacierProfile> {
    private OnItemClickListener mOnItemClickListener;
    protected Context context;
    protected String lastUUID;

    public interface OnItemClickListener {
        void onItemClick(View view, GlacierProfile obj, int position);
    }

    public void setOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mOnItemClickListener = mItemClickListener;
    }



    public ProfileSelectListAdapter(@NonNull Context context, int resource) {
        super(context, resource);
    }

    public ProfileSelectListAdapter(@NonNull Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);
    }

    public ProfileSelectListAdapter(@NonNull Context context, int resource, @NonNull GlacierProfile[] objects) {
        super(context, resource, objects);
    }

    public ProfileSelectListAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull GlacierProfile[] objects) {
        super(context, resource, textViewResourceId, objects);
    }

    public ProfileSelectListAdapter(@NonNull Context context, int resource, @NonNull List<GlacierProfile> objects) {
        super(context, resource, objects);
        this.context = context;
        this.lastUUID = lastProfileUuid();

    }

    public ProfileSelectListAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull List<GlacierProfile> objects) {
        super(context, resource, textViewResourceId, objects);
    }


    @Override
    public void add(@Nullable GlacierProfile object) {
        super.add(object);
    }

    public void select(int position, @Nullable View view, @NonNull ViewGroup parent){
        for(int i = 0; i< parent.getChildCount();i++){
           View v =  parent.getChildAt(i);
               CheckedTextView checkedTextView = v.findViewById(R.id.profname);
               checkedTextView.setChecked(false);

        }
        CheckedTextView checkedTextView = view.findViewById(R.id.profname);
        checkedTextView.setChecked(true);
    }

    private String lastProfileUuid(){
        SharedPreferences sp = this.context.getSharedPreferences("SHARED_PREFS", Context.MODE_PRIVATE);
        return sp.getString("last_spinner_profile", null);

    }

    public void clearSelections(@NonNull ViewGroup parent){
        for(int i = 0; i< parent.getChildCount();i++){
            View v =  parent.getChildAt(i);
            CheckedTextView checkedTextView = v.findViewById(R.id.profname);
            checkedTextView.setChecked(false);

        }
    }

    @Override
    public void sort(@NonNull Comparator comparator) {
        super.sort(comparator);
    }

    @Override
    public int getCount() {
        return super.getCount();
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
       LayoutInflater inflater = LayoutInflater.from(context);
        GlacierProfile item = (GlacierProfile) getItem(position);
        ViewHolder viewHolder;
        if (view == null) {
            RadioBinding binding = DataBindingUtil.inflate(inflater,R.layout.radio,parent,false);
            viewHolder = ViewHolder.get(binding);
            view = binding.getRoot();
            viewHolder.checkedTextView.setText(item.getName());
            if (item.getUuid().equals(lastUUID)){
                viewHolder.checkedTextView.setChecked(true);
            }


        }
        else {
            viewHolder = (ViewHolder) view.getTag();
        }
//        if (Build.VERSION.SDK_INT >= 16) {
//            view.setBackground(StyledAttributes.getDrawable(view.getContext(), R.attr.list_item_background));
//        }
//        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

//        ViewHolder viewHolder;
//        RadioBinding binding = DataBindingUtil.inflate(layoutInflater, R.layout.radio, parent, false);
//        viewHolder = ViewHolder.get(binding);
//        viewHolder.checkedTextView = view.findViewById(R.id.profname);
//
    return view;
//        return super.getView(position, view, parent);
    }



    @Override
    public void setDropDownViewResource(int resource) {
        super.setDropDownViewResource(resource);
    }

    public void setParseProf(String str, int position, @Nullable View view, @NonNull ViewGroup parent) {
        CheckedTextView checkedTextView = view.findViewById(R.id.profname);
        checkedTextView.setText(str);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return super.getFilter();
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }

    private static class ViewHolder {
        private CheckedTextView checkedTextView;

        private ViewHolder() {

        }

        public static ViewHolder get(RadioBinding binding) {
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.checkedTextView = binding.getRoot().findViewById(R.id.profname);
            return viewHolder;
        }

    }

}
