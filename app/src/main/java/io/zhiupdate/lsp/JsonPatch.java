package io.zhiupdate.lsp;

import org.json.JSONObject;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Primary, lowest-risk layer: rewrite force-update flags in JSON.
 * Hosts keep running because they believe there is nothing to force.
 */
public final class JsonPatch {
    private static final ThreadLocal<Boolean> REENTRY = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private JsonPatch() {}

    public static void install(XC_LoadPackage.LoadPackageParam lpparam) {
        hookOrgJson();
        hookFastjson(lpparam.classLoader);
        hookGson(lpparam.classLoader);
    }

    private static void hookOrgJson() {
        XC_MethodHook forceBool = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (Keys.isForceBool((String) param.args[0])) {
                    param.setResult(false);
                    Guard.mark("json-bool " + param.args[0]);
                }
            }
        };
        Safe.hook(JSONObject.class, "optBoolean", String.class, boolean.class, forceBool);
        Safe.hook(JSONObject.class, "optBoolean", String.class, forceBool);
        Safe.hook(JSONObject.class, "getBoolean", String.class, forceBool);

        XC_MethodHook forceIntBefore = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (Keys.isForceInt((String) param.args[0])) {
                    param.setResult(0);
                    Guard.mark("json-int " + param.args[0]);
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (Keys.isTypeInt((String) param.args[0])) {
                    Object r = param.getResult();
                    if (r instanceof Number && ((Number) r).intValue() >= 2) {
                        param.setResult(0);
                        Guard.mark("json-type " + param.args[0]);
                    }
                }
            }
        };
        Safe.hook(JSONObject.class, "optInt", String.class, int.class, forceIntBefore);
        Safe.hook(JSONObject.class, "optInt", String.class, forceIntBefore);
        Safe.hook(JSONObject.class, "getInt", String.class, forceIntBefore);

        XC_MethodHook forceString = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (!Keys.isForceString((String) param.args[0])) return;
                Object r = param.getResult();
                if (!(r instanceof String)) return;
                String v = ((String) r).toLowerCase();
                if (v.contains("force") || v.contains("must") || v.contains("mandatory")
                        || v.contains("强制") || "2".equals(v) || "hard".equals(v)) {
                    param.setResult("none");
                    Guard.mark("json-string");
                }
            }
        };
        Safe.hook(JSONObject.class, "optString", String.class, String.class, forceString);
        Safe.hook(JSONObject.class, "optString", String.class, forceString);

        Safe.run("JSONObject.<init>(String)", () -> {
            XposedHelpers.findAndHookConstructor(JSONObject.class, String.class,
                    Safe.neverThrow("JSONObject.<init>", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (Boolean.TRUE.equals(REENTRY.get())) return;
                            String raw = (String) param.args[0];
                            if (!Keys.looksLikeForcePayload(raw)) return;
                            String patched = patchRaw(raw);
                            if (patched != null && !patched.equals(raw)) {
                                param.args[0] = patched;
                                Guard.mark("json-ctor");
                            }
                        }
                    }));
        });
    }

    private static String patchRaw(String raw) {
        try {
            REENTRY.set(Boolean.TRUE);
            JSONObject o = new JSONObject(raw);
            patchObject(o);
            return o.toString();
        } catch (Throwable t) {
            return raw;
        } finally {
            REENTRY.set(Boolean.FALSE);
        }
    }

    private static void patchObject(JSONObject o) {
        try {
            java.util.Iterator<String> it = o.keys();
            java.util.ArrayList<String> keys = new java.util.ArrayList<>();
            while (it.hasNext()) keys.add(it.next());
            for (String key : keys) {
                if (Keys.isForceBool(key)) {
                    o.put(key, false);
                } else if (Keys.isForceInt(key)) {
                    o.put(key, 0);
                } else if (Keys.isTypeInt(key)) {
                    int v = o.optInt(key, 0);
                    if (v >= 2) o.put(key, 0);
                } else if (Keys.isForceString(key)) {
                    String v = o.optString(key, "");
                    if (Keys.contains(v.toLowerCase(), "force", "must", "mandatory", "强制", "hard")) {
                        o.put(key, "none");
                    }
                } else {
                    Object child = o.opt(key);
                    if (child instanceof JSONObject) patchObject((JSONObject) child);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void hookFastjson(ClassLoader cl) {
        Class<?> json = Safe.find(cl, "com.alibaba.fastjson.JSONObject");
        if (json == null) json = Safe.find(cl, "com.alibaba.fastjson2.JSONObject");
        if (json == null) return;
        Safe.hook(json, "getBooleanValue", String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (Keys.isForceBool((String) param.args[0])) param.setResult(false);
            }
        });
        Safe.hook(json, "getBoolean", String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (Keys.isForceBool((String) param.args[0])) param.setResult(false);
            }
        });
        Safe.hook(json, "getIntValue", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                String key = (String) param.args[0];
                if (Keys.isForceInt(key)) {
                    param.setResult(0);
                    return;
                }
                if (Keys.isTypeInt(key)) {
                    Object r = param.getResult();
                    if (r instanceof Number && ((Number) r).intValue() >= 2) param.setResult(0);
                }
            }
        });
    }

    private static void hookGson(ClassLoader cl) {
        Class<?> json = Safe.find(cl, "com.google.gson.JsonObject");
        if (json == null) return;
        Safe.hook(json, "get", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (!Keys.isForceBool((String) param.args[0])) return;
                try {
                    Class<?> prim = Safe.find(cl, "com.google.gson.JsonPrimitive");
                    if (prim != null) {
                        param.setResult(prim.getConstructor(Boolean.class).newInstance(Boolean.FALSE));
                    }
                } catch (Throwable ignored) {
                }
            }
        });
    }
}
