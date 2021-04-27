package com.glaciersecurity.glaciermessenger.ui.util;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;

public class StyledAttributes {
    public static android.graphics.drawable.Drawable getDrawable(Context context, @AttrRes int id) {
        TypedArray typedArray = context.obtainStyledAttributes(new int[]{id});
        android.graphics.drawable.Drawable drawable = typedArray.getDrawable(0);
        typedArray.recycle();
        return drawable;
    }

    public static float getFloat(Context context, @AttrRes int id) {
        TypedArray typedArray = context.obtainStyledAttributes(new int[]{id});
        float value = typedArray.getFloat(0,0f);
        typedArray.recycle();
        return value;
    }

    public static @ColorInt int getColor(Context context, @AttrRes int attr) {
        TypedArray typedArray = context.obtainStyledAttributes(new int[]{attr});
        int color = typedArray.getColor(0,0);
        typedArray.recycle();
        return color;
    }
}
