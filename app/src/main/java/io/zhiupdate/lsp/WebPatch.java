package io.zhiupdate.lsp;

import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 古文岛 / 古诗文网 and some 夸克 pages push an H5 force-update.
 * Only block URLs that look like a force-update wall, not every "update".
 */
public final class WebPatch {
    private WebPatch() {}

    public static void install(XC_LoadPackage.LoadPackageParam lpparam) {
        Safe.hook(WebView.class, "loadUrl", String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (isForceUrl((String) param.args[0])) {
                    Guard.mark("webview " + param.args[0]);
                    param.setResult(null);
                }
            }
        });
        Safe.hook(WebView.class, "loadUrl", String.class, java.util.Map.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (isForceUrl((String) param.args[0])) {
                    Guard.mark("webview " + param.args[0]);
                    param.setResult(null);
                }
            }
        });

        Class<?> chrome = Safe.find(lpparam.classLoader, "android.webkit.WebViewClient");
        if (chrome != null) {
            Safe.hookAll(chrome, "shouldOverrideUrlLoading", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String url = extractUrl(param.args);
                    if (isForceUrl(url)) {
                        Guard.mark("override " + url);
                        param.setResult(true);
                    }
                }
            });
        }
    }

    private static String extractUrl(Object[] args) {
        if (args == null) return null;
        for (Object a : args) {
            if (a instanceof String) return (String) a;
            if (a instanceof WebResourceRequest) {
                try {
                    android.net.Uri u = ((WebResourceRequest) a).getUrl();
                    return u == null ? null : u.toString();
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    static boolean isForceUrl(String url) {
        if (url == null) return false;
        String u = url.toLowerCase();
        if (!(u.startsWith("http://") || u.startsWith("https://") || u.startsWith("file://"))) {
            return false;
        }
        boolean update = u.contains("update") || u.contains("upgrade") || url.contains("更新");
        if (!update) return false;
        return u.contains("force") || u.contains("must") || u.contains("mandatory")
                || url.contains("强制") || u.contains("forceupdate") || u.contains("force_update");
    }
}
