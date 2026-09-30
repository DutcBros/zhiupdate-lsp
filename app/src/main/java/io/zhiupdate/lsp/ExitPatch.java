package io.zhiupdate.lsp;

import android.app.Activity;
import android.os.Process;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Force-update walls almost always call System.exit / Process.killProcess
 * when the user (or our dialog skip) "refuses". Block that path only when
 * we just intercepted an update — do NOT globally freeze quit, unlike
 * github.com/daniel-penn/ForceBlockAllCrashesLSPosed.
 */
public final class ExitPatch {
    private ExitPatch() {}

    public static void install(XC_LoadPackage.LoadPackageParam lpparam) {
        XC_MethodReplacement block = new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                if (Guard.shouldBlockSelfKill()) {
                    Safe.log("blocked self-kill " + param.method.getName()
                            + " reason=" + Guard.reason());
                    return null;
                }
                try {
                    return de.robv.android.xposed.XposedBridge.invokeOriginalMethod(
                            param.method, param.thisObject, param.args);
                } catch (Throwable t) {
                    if (t instanceof RuntimeException) throw (RuntimeException) t;
                    if (t instanceof Error) throw (Error) t;
                    throw new RuntimeException(t);
                }
            }
        };

        Safe.hook(System.class, "exit", int.class, block);
        Safe.hook(Runtime.class, "exit", int.class, block);
        Safe.hook(Process.class, "killProcess", int.class, block);
        Safe.hook(Process.class, "killProcessGroup", int.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (Guard.shouldBlockSelfKill()) {
                    Safe.log("blocked killProcessGroup");
                    param.setResult(0);
                }
            }
        });

        Safe.hook(Activity.class, "finishAffinity", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!Guard.shouldBlockSelfKill()) return;
                Activity a = (Activity) param.thisObject;
                Safe.log("rewrote finishAffinity → finish");
                param.setResult(null);
                Safe.run("finish-only", a::finish);
            }
        });
        Safe.hook(Activity.class, "finishAndRemoveTask", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!Guard.shouldBlockSelfKill()) return;
                Activity a = (Activity) param.thisObject;
                if (Targets.isForceUpdateClass(a.getClass().getName())) {
                    return; // let the update activity actually close
                }
                Safe.log("blocked finishAndRemoveTask");
                param.setResult(null);
            }
        });
    }
}
