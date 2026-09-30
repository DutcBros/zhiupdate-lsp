package io.zhiupdate.lsp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * If a dialog is clearly a force-update wall, skip showing it.
 * Never click "取消" — that is what hosts bind to System.exit.
 */
public final class DialogPatch {
    private DialogPatch() {}

    public static void install(XC_LoadPackage.LoadPackageParam lpparam) {
        XC_MethodHook beforeShow = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Object dialog = param.thisObject;
                if (!(dialog instanceof Dialog)) return;
                String copy = readDialogCopy((Dialog) dialog);
                if (!Keys.isForceUpdateCopy(copy)) return;
                Guard.mark("dialog " + dialog.getClass().getName());
                param.setResult(null);
                Safe.run("dismiss", () -> {
                    try {
                        ((Dialog) dialog).setCancelable(true);
                        ((Dialog) dialog).setCanceledOnTouchOutside(true);
                    } catch (Throwable ignored) {
                    }
                });
            }
        };
        Safe.hook(Dialog.class, "show", beforeShow);
        Safe.hook(AlertDialog.class, "show", beforeShow);

        Safe.run("Builder.setMessage", () -> {
            XposedHelpers.findAndHookMethod(AlertDialog.Builder.class, "setMessage",
                    CharSequence.class, Safe.neverThrow("setMessage", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            CharSequence msg = (CharSequence) param.args[0];
                            if (msg != null && Keys.isForceUpdateCopy(msg.toString())) {
                                Guard.mark("builder-message");
                            }
                        }
                    }));
        });
    }

    private static String readDialogCopy(Dialog dialog) {
        StringBuilder sb = new StringBuilder();
        try {
            Object alert = null;
            try {
                alert = XposedHelpers.getObjectField(dialog, "mAlert");
            } catch (Throwable ignored) {
            }
            if (alert != null) {
                append(sb, fieldText(alert, "mTitle"));
                append(sb, fieldText(alert, "mMessage"));
                append(sb, fieldText(alert, "mPositiveButtonText"));
                append(sb, fieldText(alert, "mNegativeButtonText"));
                append(sb, fieldText(alert, "mNeutralButtonText"));
            }
        } catch (Throwable ignored) {
        }
        try {
            if (dialog.getWindow() != null && dialog.getWindow().getDecorView() != null) {
                scrape(dialog.getWindow().getDecorView(), sb, 0);
            }
        } catch (Throwable ignored) {
        }
        return sb.toString();
    }

    private static String fieldText(Object obj, String name) {
        try {
            Object v = XposedHelpers.getObjectField(obj, name);
            return v == null ? null : String.valueOf(v);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void append(StringBuilder sb, String v) {
        if (v == null || v.isEmpty()) return;
        if (sb.length() > 0) sb.append(' ');
        sb.append(v);
    }

    private static void scrape(View view, StringBuilder sb, int depth) {
        if (view == null || depth > 8) return;
        try {
            if (view instanceof TextView) {
                CharSequence t = ((TextView) view).getText();
                if (t != null && t.length() > 0) append(sb, t.toString());
            }
            if (view instanceof ViewGroup) {
                ViewGroup g = (ViewGroup) view;
                int n = Math.min(g.getChildCount(), 24);
                for (int i = 0; i < n; i++) scrape(g.getChildAt(i), sb, depth + 1);
            }
        } catch (Throwable ignored) {
        }
    }
}
