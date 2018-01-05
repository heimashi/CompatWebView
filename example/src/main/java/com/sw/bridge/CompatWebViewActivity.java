package com.sw.bridge;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
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
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
                return super.onJsPrompt(view, url, message, defaultValue, result);
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                return super.onJsAlert(view, url, message, result);
            }
        });
        webView.setWebViewClient(new CompatWebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                testJavaCallJs();

            }
        });
        webView.compatAddJavascriptInterface(new JInterface(), "JInterface");
        webView.loadUrl("file:///android_asset/web_test.html");

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
        public void testJsCallJava2(String msg, double i, float j) {
            Toast.makeText(MyApp.application, msg + "::" + (i + j), Toast.LENGTH_SHORT).show();
        }
    }

}
