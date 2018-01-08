package com.sw.bridge;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;


public class InjectWebViewActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview_layout);
        initView();
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void initView() {
        WebView webView = findViewById(R.id.web_view);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDefaultTextEncodingName("utf-8");
        webView.setWebChromeClient(new WebChromeClient(){
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Toast.makeText(MyApp.application, "Console::" + consoleMessage.message(), Toast.LENGTH_SHORT).show();
                return super.onConsoleMessage(consoleMessage);
            }
        });
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new JInterface(), "JInterface");
        webView.loadUrl("file:///android_asset/web_inject.html");

    }

    public class JInterface {
        @JavascriptInterface
        @SuppressWarnings("unused")
        public void testJsCallJava(String msg, int i) {
            Toast.makeText(MyApp.application, msg + ":" + (i + 20), Toast.LENGTH_SHORT).show();
        }

        @JavascriptInterface
        @SuppressWarnings("unused")
        public void finishSelf() {
            InjectWebViewActivity.this.finish();
        }
    }


}
