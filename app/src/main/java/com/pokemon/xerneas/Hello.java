package com.pokemon.xerneas;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class Hello {
    public static void Toast(Context context, String s) {
        try {
            int a = 0;
            int b = 0;
        } catch (@Nullable Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(context, "" + s, Toast.LENGTH_LONG).show();
    }
}
