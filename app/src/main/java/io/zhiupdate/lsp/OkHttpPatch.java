package io.zhiupdate.lsp;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Optional: rewrite OkHttp response bodies that are force-update JSON.
 * Entire class is skipped if OkHttp is absent or the signature moved.
 */
public final class OkHttpPatch {
    private OkHttpPatch() {}

    public static void install(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> body = Safe.find(lpparam.classLoader, "okhttp3.ResponseBody");
        if (body == null) return;

        Safe.hook(body, "string", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Object r = param.getResult();
                if (!(r instanceof String)) return;
                String s = (String) r;
                if (!Keys.looksLikeForcePayload(s)) return;
                String patched = patchBody(s);
                if (patched != null && !patched.equals(s)) {
                    param.setResult(patched);
                    Guard.mark("okhttp-string");
                }
            }
        });
    }

    private static String patchBody(String raw) {
        try {
            org.json.JSONObject o = new org.json.JSONObject(raw);
            java.util.Iterator<String> it = o.keys();
            java.util.ArrayList<String> keys = new java.util.ArrayList<>();
            while (it.hasNext()) keys.add(it.next());
            boolean changed = false;
            for (String key : keys) {
                if (Keys.isForceBool(key)) {
                    o.put(key, false);
                    changed = true;
                } else if (Keys.isForceInt(key) || (Keys.isTypeInt(key) && o.optInt(key, 0) >= 2)) {
                    o.put(key, 0);
                    changed = true;
                }
            }
            return changed ? o.toString() : raw;
        } catch (Throwable t) {
            return raw;
        }
    }
}
