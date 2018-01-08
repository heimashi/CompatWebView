CompatWebView
-------------

CompatWebView是为了解决WebView的JavaScriptInterface注入漏洞
- [漏洞介绍：CVE-2012-6636](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2012-6636)

- 在Android的api小于17（android4.2）调用addJavaScriptInterface注入java对象会有安全风险，可以通过js注入反射调用到Java层的方法，造成安全隐患。[漏洞验证案例](https://github.com/heimashi/CompatWebView/blob/master/example/src/main/java/com/sw/bridge/InjectWebViewActivity.java)

- 解决方案：在大于等于api17中延用addJavaScriptInterface，在小于api17中采用另外的通道与js进行交互，同时保持api调用的一致性，
CompatWebView做到了对客户端开发透明，复用了原来addJavaScriptInterface的api，对前端开发也是透明的，前端不用写两套交互方式。

How to use
-----------
[使用案例](https://github.com/heimashi/CompatWebView/blob/master/example/src/main/java/com/sw/bridge/CompatWebViewActivity.java)
- 1、用CompatWebView替换原来的WebView，在需要调用addJavaScriptInterface()的地方替换成方法compatAddJavascriptInterface()
```java
webView.compatAddJavascriptInterface(new JInterface(), "JInterface");
```
- 2、如果需要自定义WebViewClient的话，必须继承自CompatWebViewClient，如果不自定义的话可以省掉此步骤
```java
webView.setWebViewClient(new CompatWebViewClient(){
    
});
```

原理介绍
-------
### JavaScript与Android通信方式总结
总的来说JavaScript与Android native通信的方式有三大类：[使用案例](https://github.com/heimashi/CompatWebView/blob/master/example/src/main/java/com/sw/bridge/CommunicateWebViewActivity.java)
- 通过JavaScriptInterface注入java对象
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
- 通过WebViewClient，实现shouldOverrideUrlLoading
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
        <a href="jtscheme://hello2?a=1&b=c">ShouldOverrideUrlLoading</a>
    ```
- 通过WebChromeClient，这种有四种方式prompt(提示框)、alert(警告框)、confirm(确认框)、console(log控制台)
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
        