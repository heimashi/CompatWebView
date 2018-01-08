package com.sw.compat.webview;


import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.webkit.WebView;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Compatible WebView for addJavascriptInterface below Android 4.2
 *
 * @author shiwang
 */
public class CompatWebView extends WebView {

    private static final String DEFAULT_SCHEME = "CompatScheme";
    private static final String JAVASCRIPT_ANNOTATION = "@android.webkit.JavascriptInterface()";
    private String scheme;
    private HashMap<String, Object> injectHashMap = new HashMap<>();

    public CompatWebView(Context context) {
        this(context, null);
    }

    public CompatWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        scheme = DEFAULT_SCHEME.toLowerCase();
        removeJavascriptInterface("searchBoxJavaBridge_");
        removeJavascriptInterface("accessibility");
        removeJavascriptInterface("accessibilityTraversal");
        setWebViewClient(new CompatWebViewClient());
    }

    /**
     * 设置新的scheme，默认的scheme名是CompatScheme
     *
     * @param scheme scheme名
     **/
    public void setScheme(String scheme) {
        if (TextUtils.isEmpty(scheme)) {
            return;
        }
        this.scheme = scheme.toLowerCase();
    }

    /**
     * 获取当前的scheme，默认的scheme名是CompatScheme
     *
     * @return String 当前的scheme名
     **/
    public String getScheme() {
        return scheme;
    }

    /**
     * 从native调用javascript，api18以下执行loadUrl，以上的执行evaluateJavascript，更加高效
     *
     * @param javascript javascript代码
     **/
    public void compatEvaluateJavascript(String javascript) {
        if (Build.VERSION.SDK_INT <= 18) {
            loadUrl("javascript:" + javascript);
        } else {
            evaluateJavascript("javascript:" + javascript, null);
        }
    }

    /**
     * 从js端调用java方法，api17以下有安全漏洞，参考https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2012-6636
     * compatAddJavascriptInterface兼容性的解决了安全问题，17及之上沿用原api，17之下走封装的通道
     *
     * @param object the Java object to inject into this WebView's JavaScript
     *               context. Null values are ignored.
     * @param name the name used to expose the object in JavaScript
     **/
    @SuppressLint({"JavascriptInterface", "AddJavascriptInterface"})
    public void compatAddJavascriptInterface(Object object, String name) {
        if (Build.VERSION.SDK_INT >= 17) {
            addJavascriptInterface(object, name);
        } else {
            injectHashMap.put(name, object);
        }
    }

    private void injectJsInterfaceForCompat(Object object, String name) {
        Class clazz = object.getClass();
        Method[] methods = clazz.getMethods();
        if (methods == null) {
            return;
        }
        StringBuilder sb = new StringBuilder("window.").append(name).append(" = {};");
        for (Method method : methods) {
            if (!checkMethodValid(method)) {
                return;
            }
            sb.append("window.").append(name).append(".");
            sb.append(method.getName()).append(" = function(");
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
            sb.append("){schemeEncode = encodeURIComponent(\"").append(name).append("?fun=").append(method.getName());
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

            sb.append("); window.location.href =\"").append(scheme).append("://\"").append("+schemeEncode;};");
        }
        compatEvaluateJavascript(sb.toString());
    }

    private static boolean checkMethodValid(Method method) {
        Annotation[] annotations = method.getAnnotations();
        if (annotations != null) {
            for (Annotation annotation : annotations) {
                if (JAVASCRIPT_ANNOTATION.equals(annotation.toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean shouldOverrideUrlLoading(String url) {
        try {
            String urlDecode = URLDecoder.decode(url, "UTF-8");
            if (urlDecode.startsWith(scheme)) {
                JavaMethod javaMethod = decodeMethodFromUri(urlDecode);
                if (javaMethod == null) {
                    return false;
                }
                return javaMethod.invoke(injectHashMap);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return false;
    }

    void onPageFinished() {
        for (String name : injectHashMap.keySet()) {
            Object object = injectHashMap.get(name);
            injectJsInterfaceForCompat(object, name);
        }
    }

    private JavaMethod decodeMethodFromUri(String url) {
        if (url == null) {
            return null;
        }
        Uri decodeUri = Uri.parse(url);
        String dScheme = decodeUri.getScheme();
        String authority = decodeUri.getAuthority();
        Set<String> params = decodeUri.getQueryParameterNames();
        if (!scheme.equals(dScheme) || authority == null || !params.contains("fun")) {
            return null;
        }
        JavaMethod javaMethod = new JavaMethod();
        javaMethod.object = authority;
        javaMethod.methodName = decodeUri.getQueryParameter("fun");
        for (String name : params) {
            if ("fun".equals(name)) {
                continue;
            }
            javaMethod.params.put(name, decodeUri.getQueryParameter(name));
        }
        return javaMethod;
    }

    private static class JavaMethod {

        String object;
        String methodName;
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        private boolean isFindMethod = false;

        private List<Class<?>> getParamTypes(String obj) {
            List<Class<?>> allTypes = new ArrayList<>();
            allTypes.add(String.class);
            if (TextUtils.isDigitsOnly(obj)) {
                allTypes.add(int.class);
                allTypes.add(long.class);
                allTypes.add(short.class);
                allTypes.add(float.class);
                allTypes.add(double.class);
            } else if (isFloatOrDouble(obj)) {
                allTypes.add(float.class);
                allTypes.add(double.class);
            } else if (obj.length() == 1) {
                allTypes.add(char.class);
            }
            return allTypes;
        }

        private boolean isFloatOrDouble(String str) {
            if (null == str || "".equals(str)) {
                return false;
            }
            Pattern pattern = Pattern.compile("^[-\\+]?[.\\d]*$");
            return pattern.matcher(str).matches();
        }

        private Object convertByType(String obj, Class<?> type) {
            if (type == String.class) {
                return obj;
            } else if (type == int.class) {
                return Integer.parseInt(obj);
            } else if (type == long.class) {
                return Long.parseLong(obj);
            } else if (type == short.class) {
                return Short.parseShort(obj);
            } else if (type == float.class) {
                return Float.parseFloat(obj);
            } else if (type == double.class) {
                return Double.parseDouble(obj);
            } else if (type == char.class && obj != null && obj.length() == 1) {
                return obj.charAt(0);
            }
            return obj;
        }

        boolean invoke(HashMap<String, Object> injectHashMap) {
            Class<?>[] paramType = new Class[params.size()];
            Object[] paramObj = new Object[params.size()];
            dfs(injectHashMap, 0, paramType, paramObj);
            return isFindMethod;
        }


        private void dfs(HashMap<String, Object> injectHashMap, int index, Class<?>[] paramType, Object[] paramObj) {
            if (isFindMethod) {
                return;
            }
            if (index == params.size()) {
                tryToInvoke(injectHashMap, paramType, paramObj);
                return;
            }
            String key = (String) params.keySet().toArray()[index];
            List<Class<?>> keyTypes = getParamTypes(params.get(key));
            for (int i = 0; i < keyTypes.size(); i++) {
                paramType[index] = keyTypes.get(i);
                paramObj[index] = convertByType(params.get(key), keyTypes.get(i));
                dfs(injectHashMap, index + 1, paramType, paramObj);
            }
        }


        private void tryToInvoke(HashMap<String, Object> injectHashMap, Class<?>[] paramType, Object[] paramObj) {
            Object injectInstance = injectHashMap.get(object);
            if (injectInstance == null) {
                return;
            }
            Class<?> clazz = injectInstance.getClass();
            try {
                Method method = clazz.getMethod(methodName, paramType);
                if (checkMethodValid(method)) {
                    method.setAccessible(true);
                    method.invoke(injectInstance, paramObj);
                    isFindMethod = true;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                //do nothing;
            }
        }

        @Override
        public String toString() {
            return "JavaMethod{" +
                    "object='" + object + '\'' +
                    ", methodName='" + methodName + '\'' +
                    ", params=" + params +
                    '}';
        }

    }


}
