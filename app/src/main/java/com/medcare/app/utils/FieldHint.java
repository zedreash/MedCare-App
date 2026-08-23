package com.medcare.app.utils;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import androidx.core.content.ContextCompat;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.textfield.TextInputLayout;

import com.medcare.app.R;

public class FieldHint {
    public static void required(TextInputLayout layout, int hintRes) {
        if (layout == null) return;
        String hint = layout.getContext().getString(hintRes);
        SpannableString ss = new SpannableString(hint + " *");
        int color = MaterialColors.getColor(layout.getContext(),
                com.google.android.material.R.attr.colorPrimary,
                ContextCompat.getColor(layout.getContext(), R.color.primary));
        ss.setSpan(new ForegroundColorSpan(color), ss.length() - 1, ss.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        layout.setHint(ss);
    }
}