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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

        Class clazz = object.getClass();
        Method[] methods = clazz.getDeclaredMethods();
        if (methods == null) {
            return;
        }
        StringBuilder sb = new StringBuilder("window.JInterface = {");

        for (Method method : methods) {
            Annotation[] annotations = method.getAnnotations();
            if (annotations != null) {
                for (Annotation annotation : annotations) {
                    Log.i("WEB_", annotation.toString());
                }
            }
            sb.append(method.getName()).append("(");
            Class<?>[] parameterTypes = method.getParameterTypes();
            int paramSize = parameterTypes.length;
            List<String> paramList = new ArrayList<>();
            for (int i = 0; i < paramSize; i++) {
                String tmp = "param" + i;
                sb.append(tmp);
                paramList.add(tmp);
                if (i < (paramSize - 1)) {
                    sb.append(",");
                }
            }
            sb.append("){compatMsgIFrame.src =\"").append(scheme).append("://").append(name).append("?fun=").append(method.getName());
            if (paramList.size() == 0) {
                sb.append("\"");
            } else {
                for (int i = 0; i < paramList.size(); i++) {
                    sb.append("&").append(paramList.get(i)).append("=\"+").append(paramList.get(i));
                    if (i < (paramSize - 1)) {
                        sb.append("+\"");
                    }
                }
            }

            sb.append(";}");
        }
        sb.append("}");
        compatEvaluateJavascript(sb.toString());
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
