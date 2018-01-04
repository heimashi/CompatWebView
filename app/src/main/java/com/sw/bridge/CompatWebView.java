package com.sw.bridge;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.HashMap;

public class CompatWebView extends WebView {

    private boolean isInjectCompatJsFlag = false;
    private static final String DEFAULT_SCHEME = "CompatScheme";
    private String scheme;
    private HashMap<String, Object> objectHashMap = new HashMap<>();

    public CompatWebView(Context context) {
        this(context, null);
    }

    public CompatWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        scheme = DEFAULT_SCHEME.toLowerCase();
        setWebViewClient(new CompatWebViewClient());
    }

    public void setScheme(String scheme) {
        if (TextUtils.isEmpty(scheme)) {
            return;
        }
        this.scheme = scheme.toLowerCase();
    }

    public void compatEvaluateJavascript(String javascript) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            loadUrl("javascript:" + javascript);
        } else {
            evaluateJavascript("javascript:" + javascript, null);
        }
    }

    @SuppressLint({"JavascriptInterface", "AddJavascriptInterface"})
    public void compatAddJavascriptInterface(Object object, String name) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//            super.addJavascriptInterface(object, name);
//        } else {
//
//        }
        objectHashMap.put(name, object);
        initCompatJs();
        addInterfaceJsString(object, name);

    }

    private void initCompatJs() {
        if (!isInjectCompatJsFlag) {
            isInjectCompatJsFlag = true;
            String initIFrame = "compatMsgIFrame = document.createElement('iframe');\n" +
                    "            compatMsgIFrame.style.display = 'none';\n" +
                    "            document.documentElement.appendChild(compatMsgIFrame);";
            compatEvaluateJavascript(initIFrame);
        }
    }


    private void addInterfaceJsString(Object object, String name) {
        StringBuilder sb = new StringBuilder();
        Class clazz = object.getClass();
        Method[] methods = clazz.getDeclaredMethods();
        if (methods == null) {
            return;
        }
        for (Method method : methods) {
            Annotation[] annotations = method.getAnnotations();
            Log.i("WEB_", annotations.toString());

        }

        String ss = "window.JInterface = {" +
                "        testJsCallJava(msg, code){" +
                "           compatMsgIFrame.src = \"CompatScheme://JInterface?fun=testJsCallJava&msg=\"+msg+\"&code=\"+code;"+
                "        }" +
                "    }";
        loadUrl("javascript:" + ss);
    }

    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        try {
            url = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (url.startsWith(scheme)) {
            Log.i("WEB_", url);
            return true;
        }
        return false;
    }
}
