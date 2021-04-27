package com.glaciersecurity.glaciermessenger.ui.util;

import android.widget.ImageView;

import com.glaciersecurity.glaciermessenger.entities.Account;

public class MenuDrawerConfig {

    private static MenuDrawerConfig menuDrawerConfig;

    private ImageView mAvatarImage;
    private Account mAccount;

    public static MenuDrawerConfig getInstance(){
        if (menuDrawerConfig == null){
            menuDrawerConfig = new MenuDrawerConfig();
        }
        return menuDrawerConfig;
    }

    private MenuDrawerConfig(){
    }

    public static MenuDrawerConfig getMenuDrawerConfig() {
        return menuDrawerConfig;
    }

    public static void setMenuDrawerConfig(MenuDrawerConfig menuDrawerConfig) {
        MenuDrawerConfig.menuDrawerConfig = menuDrawerConfig;
    }

    public ImageView getmAvatarImage() {
        return mAvatarImage;
    }

    public void setmAvatarImage(ImageView mAvatarImage) {
        this.mAvatarImage = mAvatarImage;
    }

    public Account getmAccount() {
        return mAccount;
    }

    public void setmAccount(Account mAccount) {
        this.mAccount = mAccount;
    }

    public void setMenuDrawerConfig(ImageView mAvatarImage, Account mAccount){
        this.mAccount = mAccount;
        this.mAvatarImage = mAvatarImage;
    }
}
