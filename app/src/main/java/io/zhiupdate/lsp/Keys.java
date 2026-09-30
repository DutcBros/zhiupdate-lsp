package io.zhiupdate.lsp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Specific update-force keys and copy. Generic names like "force" or "type" are excluded. */
public final class Keys {
    private Keys() {}

    public static final Set<String> FORCE_BOOL = set(
            "forceupdate", "force_update", "isforceupdate", "is_force_update",
            "mustupdate", "must_update", "needforceupdate", "need_force_update",
            "isforceupgrade", "is_force_upgrade", "forceupgrade", "force_upgrade",
            "isforcedupdate", "is_forced_update", "forcedupdate", "forced_update",
            "needforce", "need_force", "ismustupdate", "is_must_update",
            "updateforce", "update_force", "forceinstall", "force_install",
            "hasforceupdate", "has_force_update", "isforced", "is_forced",
            "mandatoryupdate", "mandatory_update", "compulsoryupdate",
            "forceupgradeflag", "force_upgrade_flag"
    );

    public static final Set<String> FORCE_INT = set(
            "forceupdate", "force_update", "isforceupdate", "is_force_update",
            "forceflag", "force_flag", "forceupdatetype", "force_update_type",
            "forcetype", "force_type", "updatemode", "update_mode"
    );

    /** 0 = none / optional. Hosts use 1=soft, 2=hard. */
    public static final Set<String> TYPE_INT = set(
            "updatetype", "update_type", "upgradetype", "upgrade_type",
            "updateflag", "update_flag", "upgradestrategy", "upgrade_strategy"
    );

    public static final Set<String> FORCE_STRING = set(
            "updatetype", "update_type", "upgradetype", "upgrade_type",
            "updatestrategy", "update_strategy", "forcetype", "force_type",
            "updatestatus", "update_status"
    );

    public static String normalize(String key) {
        if (key == null) return "";
        return key.toLowerCase(Locale.ROOT).replace("-", "_");
    }

    public static boolean isForceBool(String key) {
        return FORCE_BOOL.contains(normalize(key));
    }

    public static boolean isForceInt(String key) {
        return FORCE_INT.contains(normalize(key));
    }

    public static boolean isTypeInt(String key) {
        return TYPE_INT.contains(normalize(key));
    }

    public static boolean isForceString(String key) {
        return FORCE_STRING.contains(normalize(key));
    }

    public static boolean looksLikeForcePayload(String raw) {
        if (raw == null) return false;
        int n = raw.length();
        if (n < 24 || n > 400_000) return false;
        String s = raw.toLowerCase(Locale.ROOT);
        boolean update = s.contains("update") || s.contains("upgrade")
                || raw.contains("更新") || raw.contains("升级") || raw.contains("新版本");
        if (!update) return false;
        return s.contains("force") || s.contains("must_update") || s.contains("mustupdate")
                || s.contains("mandatory") || raw.contains("强制")
                || s.contains("need_force") || s.contains("is_force");
    }

    public static boolean isForceUpdateCopy(String text) {
        if (text == null || text.isEmpty()) return false;
        String t = text.replaceAll("\\s+", "");
        if (t.length() < 2) return false;
        boolean mentions = contains(t,
                "更新", "升级", "新版本", "版本过低", "latest version", "new version",
                "Update", "Upgrade", "update", "upgrade");
        if (!mentions) return false;
        return contains(t,
                "强制", "必须更新", "必须升级", "请更新后", "更新后才能", "更新后才可",
                "否则无法", "无法继续", "无法使用", "将退出", "将会退出", "退出应用",
                "关闭应用", "不再提供", "已停止服务", "版本过低", "过低无法",
                "不更新将", "拒绝将", "取消将退出", "must update", "force update",
                "Force Update", "required to continue", "won't run", "will exit",
                "请升级后", "升级后才能", "升级后才可");
    }

    public static boolean contains(String hay, String... needles) {
        if (hay == null) return false;
        for (String n : needles) {
            if (n != null && hay.contains(n)) return true;
        }
        return false;
    }

    private static Set<String> set(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }
}
