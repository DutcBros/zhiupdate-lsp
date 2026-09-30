package io.zhiupdate.lsp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Dedicated force-update activities are finished immediately.
 * We still let onCreate run (so fields initialise) then finish —
 * skipping onCreate is a common cause of later NPEs / 闪退.
 */
public final class ActivityPatch {
    private ActivityPatch() {}

    public static void install(XC_LoadPackage.LoadPackageParam lpparam) {
        Safe.hook(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Activity a = (Activity) param.thisObject;
                if (a == null) return;
                String name = a.getClass().getName();
                if (!Targets.isForceUpdateClass(name)) return;
                Guard.mark("activity " + name);
                Safe.run("finish-update-activity", () -> {
                    try {
                        a.setFinishOnTouchOutside(true);
                    } catch (Throwable ignored) {
                    }
                    a.finish();
                });
            }
        });

        XC_MethodHook blockIntent = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Intent intent = findIntent(param.args);
                blockStart(param, intent);
            }
        };
        Safe.hookAll(Activity.class, "startActivity", blockIntent);
        Safe.hookAll(Activity.class, "startActivityForResult", blockIntent);
        Safe.hookAll(Activity.class, "startActivityIfNeeded", blockIntent);

        Class<?> inst = Safe.find(lpparam.classLoader, "android.app.Instrumentation");
        if (inst != null) {
            Safe.hookAll(inst, "execStartActivity", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Intent intent = findIntent(param.args);
                    if (intent == null) return;
                    String target = classNameOf(intent);
                    if (Targets.isForceUpdateClass(target)) {
                        Guard.mark("execStartActivity " + target);
                        param.setResult(null);
                    }
                }
            });
        }
    }

    private static Intent findIntent(Object[] args) {
        if (args == null) return null;
        for (Object a : args) {
            if (a instanceof Intent) return (Intent) a;
        }
        return null;
    }

    private static void blockStart(XC_MethodHook.MethodHookParam param, Intent intent) {
        String target = classNameOf(intent);
        if (Targets.isForceUpdateClass(target)) {
            Guard.mark("startActivity " + target);
            param.setResult(null);
        }
    }

    private static String classNameOf(Intent intent) {
        if (intent == null) return null;
        try {
            if (intent.getComponent() != null) {
                return intent.getComponent().getClassName();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
