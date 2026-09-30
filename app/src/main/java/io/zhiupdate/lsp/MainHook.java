package io.zhiupdate.lsp;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * LSPosed / Xposed entry. Legacy API 82 so it loads on LSPosed_mod 1.9.x
 * (JingMatrix fork) and on older official builds.
 *
 * Inspired by:
 *  - Gift1a/Xposed_Module_Template (xposed_init + scope array)
 *  - daniel-penn/ForceBlockAllCrashesLSPosed (self-kill hooks — used surgically here)
 */
public class MainHook implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            if (lpparam == null) return;
            if (!Targets.shouldHook(lpparam.packageName, lpparam.processName)) return;
            Safe.log("attach " + lpparam.packageName + " pid=" + android.os.Process.myPid()
                    + " proc=" + lpparam.processName);
            Guard.install();
            JsonPatch.install(lpparam);
            DialogPatch.install(lpparam);
            ActivityPatch.install(lpparam);
            ExitPatch.install(lpparam);
            WebPatch.install(lpparam);
            OkHttpPatch.install(lpparam);
        } catch (Throwable t) {
            Safe.log("handleLoadPackage", t);
        }
    }
}
