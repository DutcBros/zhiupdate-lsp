package io.zhiupdate.lsp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class Targets {
    private Targets() {}

    public static final String XUNLEI = "com.xunlei.downloadprovider";
    public static final String XUNLEI_BROWSER = "com.xunlei.browser";
    public static final String QUARK = "com.quark.browser";
    public static final String GUWENDAO = "com.guwendao.gwd";
    public static final String GUSHIWEN = "org.gushiwen.gushiwen";

    private static final Set<String> PACKAGES = new HashSet<>(Arrays.asList(
            XUNLEI,
            XUNLEI_BROWSER,
            "com.xunlei.download",
            QUARK,
            "com.quark.browser.lite",
            GUWENDAO,
            GUSHIWEN
    ));

    private static final Set<String> NEVER = new HashSet<>(Arrays.asList(
            "android",
            "system",
            "com.android.systemui",
            "org.lsposed.manager",
            "io.github.lsposed.manager",
            "com.android.phone",
            "com.android.providers.settings"
    ));

    public static boolean shouldHook(String pkg, String process) {
        if (pkg == null) return false;
        if (NEVER.contains(pkg)) return false;
        if (process != null && ("system".equals(process) || process.startsWith("android."))) {
            return false;
        }
        if (PACKAGES.contains(pkg)) return true;
        // Coexistence / clone apps: com.xunlei.downloadprovider.xxx
        for (String p : PACKAGES) {
            if (pkg.startsWith(p + ".") || pkg.startsWith(p + ":")) return true;
        }
        return false;
    }

    public static boolean isForceUpdateClass(String className) {
        if (className == null) return false;
        String s = className.toLowerCase();
        if (s.contains("forceupdate") || s.contains("mustupdate")
                || s.contains("forcedupdate") || s.contains("appmustupdate")
                || s.contains("updateintercept") || s.contains("versionforbid")
                || s.contains("forcenewversion")) {
            return true;
        }
        String simple = className;
        int dot = className.lastIndexOf('.');
        if (dot >= 0) simple = className.substring(dot + 1);
        String simpleL = simple.toLowerCase();
        if (simpleL.equals("updateactivity") || simpleL.equals("upgradeactivity")
                || simpleL.equals("appupdateactivity") || simpleL.equals("versionupdateactivity")
                || simpleL.equals("checkupdateactivity") || simpleL.equals("updatedialogactivity")) {
            return true;
        }
        if ((s.contains(".update.") || s.contains(".upgrade."))
                && (simpleL.contains("activity") || simpleL.contains("dialog")
                || simpleL.contains("fragment"))) {
            return true;
        }
        return false;
    }
}
