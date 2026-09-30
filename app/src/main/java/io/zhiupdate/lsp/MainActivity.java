package io.zhiupdate.lsp;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#0E0F0C"));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(22);
        root.setPadding(pad, pad, pad, pad);

        add(root, "止更", 34, Color.parseColor("#E8E6DC"), true);
        add(root, "LSPosed 模块 · 拦截强制更新 · 阻断拒更闪退", 14, Color.parseColor("#8AA38A"), false);
        add(root, "在 LSPosed 中启用本模块，作用域只勾选目标 App，不要勾系统框架。改完后强行停止目标应用再打开。", 15, Color.parseColor("#C4C2B6"), false);

        add(root, "作用域", 18, Color.parseColor("#E8E6DC"), true);
        add(root, "迅雷    com.xunlei.downloadprovider\n夸克    com.quark.browser\n古文岛  com.guwendao.gwd\n古诗文网 org.gushiwen.gushiwen",
                13, Color.parseColor("#B8C5B6"), false);

        add(root, "四层防护", 18, Color.parseColor("#E8E6DC"), true);
        add(root, "1  JSON 把 forceUpdate / updateType≥2 改成非强制\n2  强制更新弹窗直接不 show，绝不点「取消」\n3  更新页 Activity 在 onCreate 之后 finish\n4  仅在拦截后的短窗口内挡住 System.exit / killProcess",
                14, Color.parseColor("#C4C2B6"), false);

        add(root, "本模块不解锁会员、不绕过鉴权、不修改下载逻辑。仅去掉强制更新墙。", 13, Color.parseColor("#7A7A70"), false);
        scroll.addView(root);
        setContentView(scroll);
    }

    private void add(LinearLayout root, String text, int sp, int color, boolean serif) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        tv.setLineSpacing(dp(4), 1.15f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(serif ? 10 : 18);
        tv.setLayoutParams(lp);
        if (serif) tv.setGravity(Gravity.START);
        root.addView(tv);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
