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
- 1、用CompatWebView替换原来的WebView，在需要调用addJavaScriptInterface()的地方调用替换成方法compatAddJavascriptInterface()
```java
webView.compatAddJavascriptInterface(new JInterface(), "JInterface");
```
- 2、如果需要自定义WebViewClient的话，必须继承自CompatWebViewClient，如果不自定义的话可以省掉此步骤
```java
webView.setWebViewClient(new CompatWebViewClient(){
    
});
```