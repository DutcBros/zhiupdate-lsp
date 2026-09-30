package io.zhiupdate.lsp;

import android.os.Handler;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;

/**
 * Remembers that we just intercepted a force-update, so ExitPatch can
 * refuse a follow-up System.exit / killProcess, and so a short-lived
 * looper guard can swallow leftover NPEs from the dismissed UI.
 *
 * This is intentionally NOT a global crash-eater: swallowing every
 * exception (as some GitHub "anti-crash" modules do) leaves apps in a
 * zombie state. We only cover the few seconds after a blocked update.
 */
public final class Guard {
    private static final long WINDOW_MS = 12_000L;
    private static volatile long lastBlockAt;
    private static volatile String lastReason = "";
    private static volatile boolean looperHooked;

    private Guard() {}

    public static void mark(String reason) {
        lastBlockAt = System.currentTimeMillis();
        lastReason = reason == null ? "force-update" : reason;
        Safe.log("blocked: " + lastReason);
    }

    public static boolean recentlyBlocked() {
        long at = lastBlockAt;
        return at != 0 && (System.currentTimeMillis() - at) < WINDOW_MS;
    }

    public static String reason() {
        return lastReason;
    }

    public static void install() {
        if (looperHooked) return;
        looperHooked = true;
        Safe.run("Guard.looper", () -> {
            XposedBridge.hookAllMethods(Handler.class, "dispatchMessage",
                    Safe.neverThrow("dispatchMessage", new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) {
                            try {
                                return XposedBridge.invokeOriginalMethod(
                                        param.method, param.thisObject, param.args);
                            } catch (Throwable t) {
                                if (recentlyBlocked()) {
                                    Safe.log("swallowed leftover crash after block: " + t);
                                    return null;
                                }
                                if (t instanceof RuntimeException) throw (RuntimeException) t;
                                if (t instanceof Error) throw (Error) t;
                                throw new RuntimeException(t);
                            }
                        }
                    }));
        });
    }

    public static boolean stackLooksLikeUpdate() {
        try {
            StackTraceElement[] st = new Throwable().getStackTrace();
            if (st == null) return false;
            int n = Math.min(st.length, 18);
            for (int i = 0; i < n; i++) {
                String c = st[i].getClassName();
                if (c == null) continue;
                String s = c.toLowerCase();
                if (s.contains("forceupdate") || s.contains("mustupdate")
                        || s.contains("forcedupdate") || s.contains("appupdate")
                        || s.contains("upgradeutil") || s.contains("upgrademgr")
                        || s.contains("updatemanager") || s.contains("updatehelper")
                        || s.contains("updateutils") || s.contains("versioncheck")
                        || s.contains("checkupdate") || s.contains("updateinterceptor")) {
                    return true;
                }
                if ((s.contains(".update.") || s.contains(".upgrade."))
                        && (s.contains("dialog") || s.contains("activity")
                        || s.contains("presenter") || s.contains("manager"))) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean shouldBlockSelfKill() {
        return recentlyBlocked() || stackLooksLikeUpdate();
    }
}
