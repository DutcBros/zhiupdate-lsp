# 止更

LSPosed / Xposed 模块：拦截 **迅雷、夸克、古文岛、古诗文网** 的强制更新墙，并挡住因「拒绝更新」触发的 `System.exit` / `Process.killProcess`，避免闪退。

不解锁会员、不绕过登录、不改下载逻辑。

## 作用域

| 应用 | 包名 |
| --- | --- |
| 迅雷 | `com.xunlei.downloadprovider` |
| 迅雷浏览器 | `com.xunlei.browser` |
| 夸克 | `com.quark.browser` |
| 古文岛 | `com.guwendao.gwd` |
| 古诗文网 | `org.gushiwen.gushiwen` |

克隆包只要前缀相同也会命中。不要把系统框架勾进作用域。

## 为什么不闪退

参考了 [ForceBlockAllCrashesLSPosed](https://github.com/daniel-penn/ForceBlockAllCrashesLSPosed) 的自杀 API hook，但**不做全局吞异常**：

1. 每个 hook 单独 try/catch，类或方法不存在就跳过。
2. 强制更新弹窗是「不 show」，而不是点取消（取消回调经常就是退出）。
3. 更新 Activity 让 `onCreate` 跑完再 `finish`，避免半初始化 NPE。
4. 只在拦截后约 12 秒内挡住 `System.exit` / `killProcess`，正常退出不受影响。
5. 同一窗口期内，主线程 `Handler.dispatchMessage` 的残留异常会被吞掉。

## 编译

用 Android Studio 打开本目录，JDK 17，Sync 后 `Build > Build APK(s)`。

命令行（需 Android SDK）：

```
./gradlew :app:assembleRelease
```

产物：`app/build/outputs/apk/release/app-release-unsigned.apk`。用自己的密钥签名后安装。

## 安装

1. 设备已 Root，并装有 [LSPosed](https://github.com/JingMatrix/LSPosed) 或 LSPosed_mod。
2. 安装本模块 APK。
3. 在 LSPosed 里启用「止更」，作用域只勾上表应用。
4. 强行停止目标 App 后重新打开。

日志过滤 tag：`止更`。
