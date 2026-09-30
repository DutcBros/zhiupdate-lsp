package io.zhiupdate.lsp;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Every hook in this module goes through here. A missing class, a changed
 * signature, or a host NPE must never escape into the target process.
 */
public final class Safe {
    private Safe() {}

    public static void run(String label, Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            log(label, t);
        }
    }

    public static void log(String message) {
        try {
            XposedBridge.log("[止更] " + message);
        } catch (Throwable ignored) {
        }
    }

    public static void log(String label, Throwable t) {
        try {
            XposedBridge.log("[止更] " + label + ": " + t);
        } catch (Throwable ignored) {
        }
    }

    public static Class<?> find(ClassLoader loader, String name) {
        try {
            return XposedHelpers.findClassIfExists(name, loader);
        } catch (Throwable t) {
            return null;
        }
    }

    public static void hook(Class<?> clazz, String method, Object... typesAndCallback) {
        if (clazz == null || method == null) return;
        try {
            Object[] wrapped = wrapCallback(method, typesAndCallback);
            XposedHelpers.findAndHookMethod(clazz, method, wrapped);
        } catch (Throwable t) {
            log("skip " + clazz.getName() + "#" + method, t);
        }
    }

    public static void hook(String className, ClassLoader loader, String method, Object... typesAndCallback) {
        try {
            Object[] wrapped = wrapCallback(className + "#" + method, typesAndCallback);
            XposedHelpers.findAndHookMethod(className, loader, method, wrapped);
        } catch (Throwable t) {
            log("skip " + className + "#" + method, t);
        }
    }

    public static void hookAll(Class<?> clazz, String method, XC_MethodHook callback) {
        if (clazz == null) return;
        try {
            XposedBridge.hookAllMethods(clazz, method, neverThrow(method, callback));
        } catch (Throwable t) {
            log("skip-all " + clazz.getName() + "#" + method, t);
        }
    }

    private static Object[] wrapCallback(String label, Object[] typesAndCallback) {
        if (typesAndCallback == null || typesAndCallback.length == 0) {
            return typesAndCallback;
        }
        Object[] copy = typesAndCallback.clone();
        int last = copy.length - 1;
        if (copy[last] instanceof XC_MethodHook) {
            copy[last] = neverThrow(label, (XC_MethodHook) copy[last]);
        }
        return copy;
    }

    /** Callbacks that throw would crash the host. Swallow everything. */
    public static XC_MethodHook neverThrow(String label, XC_MethodHook inner) {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    inner.beforeHookedMethod(param);
                } catch (Throwable t) {
                    log("before " + label, t);
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    inner.afterHookedMethod(param);
                } catch (Throwable t) {
                    log("after " + label, t);
                }
            }
        };
    }
}
