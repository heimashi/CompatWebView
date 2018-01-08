package com.sw.bridge;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

import com.sw.compat.webview.CompatWebView;
import com.sw.compat.webview.CompatWebViewClient;


public class CompatWebViewActivity extends Activity {

    private CompatWebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview_compat_layout);
        initView();
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void initView() {
        webView = findViewById(R.id.web_view);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDefaultTextEncodingName("utf-8");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new CompatWebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                testJavaCallJs();

            }
        });
        webView.compatAddJavascriptInterface(new JInterface(), "JInterface");
        webView.loadUrl("file:///android_asset/web_compat.html");

    }

    public void testJavaCallJs() {
        webView.compatEvaluateJavascript("javaCallJs()");
    }

    private static class JInterface {
        @JavascriptInterface
        @SuppressWarnings("unused")
        public void testJsCallJava(String msg, int i) {
            Toast.makeText(MyApp.application, msg + ":" + (i + 20), Toast.LENGTH_SHORT).show();
        }

        @JavascriptInterface
        @SuppressWarnings("unused")
        public void toInjectWebViewActivity() {
            MyApp.application.startActivity(new Intent(MyApp.application, InjectWebViewActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }

        @JavascriptInterface
        @SuppressWarnings("unused")
        public void toCommunicateWebViewActivity() {
            MyApp.application.startActivity(new Intent(MyApp.application, CommunicateWebViewActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

}
