# CompatWebView

CompatWebView是为了解决WebView的JavaScriptInterface注入漏洞
[参考此文：https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2012-6636](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2012-6636)

- 总结一下漏洞：在Android的api小于17（android4.2）调用addJavaScriptInterface注入java对象会有安全风险，可以通过js注入反射调用到Java层的方法，造成安全隐患。

- 解决方案：在等于大于api17中沿用addJavaScriptInterface，在小于api17中采用另外的方式与js进行交互，同时保持api调用的一致性，
CompatWebView做到了对客户端开发透明，基本复用了原来addJavaScriptInterface的api，对前端开发也是透明的，前端不用写两套交互方式。