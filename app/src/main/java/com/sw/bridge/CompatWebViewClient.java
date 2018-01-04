package com.sw.bridge;


import android.os.Build;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class CompatWebViewClient extends WebViewClient {

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {

        }
        if (view instanceof CompatWebView) {
            if (((CompatWebView) view).shouldOverrideUrlLoading(view, url)) {
                return true;
            }
        }
        return super.shouldOverrideUrlLoading(view, url);
    }
}
