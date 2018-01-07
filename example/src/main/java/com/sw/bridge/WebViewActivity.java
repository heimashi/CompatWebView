package com.sw.bridge;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Set;

public class WebViewActivity extends Activity {

    private WebView webView;
    private static final String SCHEME = "jtscheme";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview_layout);
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
                Uri uri = Uri.parse(message);
                if (SCHEME.equals(uri.getScheme())) {
                    String authority = uri.getAuthority();
                    Set<String> params = uri.getQueryParameterNames();
                    for (String s : params) {
                        Log.i("WEB_", s + ":" + uri.getQueryParameter(s));
                    }
                    Toast.makeText(MyApp.application, "Prompt::" + authority, Toast.LENGTH_SHORT).show();
                }
                return super.onJsPrompt(view, url, message, defaultValue, result);
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                Toast.makeText(MyApp.application, "Alert::" + message, Toast.LENGTH_SHORT).show();
                return super.onJsAlert(view, url, message, result);
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                Toast.makeText(MyApp.application, "Confirm::" + message, Toast.LENGTH_SHORT).show();
                return super.onJsConfirm(view, url, message, result);
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Toast.makeText(MyApp.application, "Console::" + consoleMessage.message(), Toast.LENGTH_SHORT).show();
                return super.onConsoleMessage(consoleMessage);
            }
        });
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                try {
                    url = URLDecoder.decode(url, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (url.startsWith(SCHEME)) {
                    Toast.makeText(WebViewActivity.this, url, Toast.LENGTH_SHORT).show();
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                testJavaCallJs();
            }
        });
        webView.addJavascriptInterface(new JInterface(), "JInterface");
        webView.loadUrl("file:///android_asset/web_js_android.html");
        test();

    }

    public void testJavaCallJs() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            webView.loadUrl("javascript:javaCallJs()");
        } else {
            webView.evaluateJavascript("javascript:javaCallJs()", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {

                }
            });
        }
    }

    public static class JInterface {
        @JavascriptInterface
        @SuppressWarnings("unused")
        public void testJsCallJava(String msg, int i) {
            Toast.makeText(MyApp.application, msg + ":" + (i + 20), Toast.LENGTH_SHORT).show();
        }
    }

    public void test() {
        Util.test();
    }


}
