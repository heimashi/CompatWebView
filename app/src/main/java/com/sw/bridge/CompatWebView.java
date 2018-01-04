package com.sw.bridge;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.WebView;

public class CompatWebView extends WebView {

    public CompatWebView(Context context) {
        this(context, null);
    }

    public CompatWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressLint({"JavascriptInterface", "AddJavascriptInterface"})
    @Override
    public void addJavascriptInterface(Object object, String name) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            super.addJavascriptInterface(object, name);
        } else {

        }
    }
}
