package com.glaciersecurity.glaciermessenger.ui;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import android.transition.Slide;
import android.transition.TransitionManager;
import android.view.Gravity;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.ui.util.Tools;
import com.glaciersecurity.glaciermessenger.utils.Log;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class StepperWizard extends AppCompatActivity {

    RelativeLayout relativeLayout;

    private static final int MAX_STEP = 4;

    private ViewPager viewPager;
    private MyViewPagerAdapter myViewPagerAdapter;
    private ImageView image;
    private TextView title;
    private TextView description;
    private Button btn_got_it;
    private Button btn_next;
    private String title_array[] = {
            "Welcome to Glacier",
            "How it works",
            "For your eyes only",
            "Talk openly"
    };

    private int color_array[] = {
            R.color.primary_bg_color,   // black_accent, grey950
            R.color.grey965,  // blue_grey_800, grey900
            R.color.grey880,  // blue_grey_700, grey851
            R.color.grey825   // blue_grey_900, grey800
    };

    private String description_array[] = {
            "We’re building the world’s most human secure communications company.",
            "Launch a private, obfuscated, and encrypted communications network to protect your devices and secure your team's most sensitive conversations.",
            "A secure communications platform built for protecting your team’s messages, calls, devices, and data from outside threats.",
            "Glacier uses strong cryptography to ensure your conversations stay private. Even we are unable to read your messages.",
    };
    private int about_images_array[] = {
            R.mipmap.android_icon_white_foreground,
            R.drawable.step2_howitworks2,
            R.drawable.step3_foryoureyes2,
            R.drawable.step4_talkopenly2
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stepper_wizard_color);
        image = findViewById(R.id.image);
        title = findViewById(R.id.title);
        description = findViewById(R.id.description);

        initComponent();

        Tools.setSystemBarColor(this, R.color.primary_bg_color);
    }

    private void initComponent() {

        viewPager = (ViewPager) findViewById(R.id.view_pager);
        btn_got_it = (Button) findViewById(R.id.btn_got_it);
        btn_next =  (Button) findViewById(R.id.btn_next);

        // adding bottom dots
        bottomProgressDots(0);

        myViewPagerAdapter = new MyViewPagerAdapter();
        viewPager.setAdapter(myViewPagerAdapter);
        viewPager.addOnPageChangeListener(viewPagerPageChangeListener);

        btn_got_it.setVisibility(View.GONE);
        btn_got_it.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!hasAllPermissionGranted()){

                    setContentView(R.layout.activity_stepper_permission_ex);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        relativeLayout = findViewById(R.id.relativeLayout);
                        Slide slide = new Slide();
                        slide.setSlideEdge(Gravity.START);
                        TransitionManager.beginDelayedTransition(relativeLayout, slide);
                    } else {
                        setContentView(R.layout.activity_stepper_permission_ex);
                    }
                } else {
                    Intent intent = new Intent(getApplicationContext(), EditAccountActivity.class);
                    startActivity(intent);
                }

            }
        });

        btn_next.setVisibility(View.VISIBLE);
        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(getItem(+1), true);
            }
        });

        ((Button) findViewById(R.id.btn_skip)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!hasAllPermissionGranted()){
                    setContentView(R.layout.activity_stepper_permission_ex);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        relativeLayout = findViewById(R.id.relativeLayout);
                        Slide slide = new Slide();
                        slide.setSlideEdge(Gravity.START);
                        TransitionManager.beginDelayedTransition(relativeLayout, slide);
                    } else {
                        setContentView(R.layout.activity_stepper_permission_ex);
                    }

                } else {
                    Intent intent = new Intent(getApplicationContext(), EditAccountActivity.class);
                    startActivity(intent);
                }
            }
        });

    }
    private int getItem(int i) {
        return viewPager.getCurrentItem() + i;
    }

    public boolean hasAllPermissionGranted() {
        return checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
    private void bottomProgressDots(int current_index) {
        LinearLayout dotsLayout = (LinearLayout) findViewById(R.id.layoutDots);
        ImageView[] dots = new ImageView[MAX_STEP];

        dotsLayout.removeAllViews();
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new ImageView(this);
            int width_height = 15;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(new ViewGroup.LayoutParams(width_height, width_height));
            params.setMargins(10, 10, 10, 10);
            dots[i].setLayoutParams(params);
            dots[i].setImageResource(R.drawable.shape_circle);
            dots[i].setColorFilter(getResources().getColor(R.color.grey_20), PorterDuff.Mode.SRC_IN);
            dotsLayout.addView(dots[i]);
        }

        if (dots.length > 0) {
            dots[current_index].setImageResource(R.drawable.shape_circle);
            dots[current_index].setColorFilter(getResources().getColor(R.color.blue_palette_bright_hex2), PorterDuff.Mode.SRC_IN);
        }
    }

    public void continuePerm(View view) {
        askForPermissions();
    }


    //  viewpager change listener
    ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(final int position) {
            bottomProgressDots(position);
            if (position == title_array.length - 1) {
                btn_got_it.setVisibility(View.VISIBLE);
                btn_next.setVisibility(View.GONE);
            } else {
                btn_got_it.setVisibility(View.GONE);
                btn_next.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }
    };

    /**
     * View pager adapter
     */
    public class MyViewPagerAdapter extends PagerAdapter {
        private LayoutInflater layoutInflater;

        public MyViewPagerAdapter() {
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = layoutInflater.inflate(R.layout.item_stepper_wizard_color, container, false);
            ((TextView) view.findViewById(R.id.title)).setText(title_array[position]);
            ((TextView) view.findViewById(R.id.description)).setText(description_array[position]);
            ((ImageView) view.findViewById(R.id.image)).setImageResource(about_images_array[position]);
            ((RelativeLayout) view.findViewById(R.id.lyt_parent)).setBackgroundColor(getResources().getColor(color_array[position]));
            container.addView(view);

            return view;
        }

        @Override
        public int getCount() {
            return title_array.length;
        }


        @Override
        public boolean isViewFromObject(View view, Object obj) {
            return view == obj;
        }


        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View view = (View) object;
            container.removeView(view);
        }
    }


    /**
     * PERMISSIONS - Ask for permissions
     */
    private void askForPermissions() {
        final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;

        //String[] request = {Manifest.permission.READ_CONTACTS, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        List<String> permissionsNeeded = new ArrayList<String>();

        final List<String> permissionsList = new ArrayList<String>();
        // added WRITE_EXTERNAL_STORAGE permission ahead of time so that it doesn't ask
        // when time comes which inevitably fails at that point.
        if (!addPermission(permissionsList, Manifest.permission.RECORD_AUDIO))
            permissionsNeeded.add("RECORD_AUDIO");
        if (!addPermission(permissionsList, Manifest.permission.CAMERA))
            permissionsNeeded.add("Camera");
        if (!addPermission(permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            permissionsNeeded.add("Write Storage");
        if (!addPermission(permissionsList, Manifest.permission.READ_PHONE_STATE))
            permissionsNeeded.add("Read Phone State");
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // restore accounts from file if exists
        //I think this ONLY works for single sign on so probably irrelevant for us
        Intent intent = new Intent(getApplicationContext(), EditAccountActivity.class);
        startActivity(intent);
    }

}