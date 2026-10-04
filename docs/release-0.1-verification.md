# Release 0.1 验证记录

日期：2026-10-04。版本名 `0.1` / 版本码 `2`。发布 APK 大小 924454 字节，SHA-256 见 [v0.1 对应的 SHA256SUMS](https://github.com/enkidu-214/aurora-arcade/releases/download/v0.1/SHA256SUMS)。

## 构建与包检查

- Gradle 8.11.1 / AGP 8.10.1 / Kotlin 2.1.20 / Compose BOM 2025.04.01。
- compileSdk / targetSdk 36，Build Tools 36.0.0；minSdk 26。
- 24 项核心 JUnit 测试重新执行，0 失败 / 0 错误。
- `:app:assembleRelease :app:assembleDebug :app:lintRelease` 成功。Lint 0 错误 / 9 警告：4 项构建工具或依赖版本提示、2 项竖屏提示、1 项备份规则建议、2 项 KTX 风格建议。
- Release 启用 R8、资源压缩和独立 RSA 3072 位发布签名。`apksigner verify` 通过 APK v2 签名验证。
- `zipalign -c -P 16 4` 通过；64 位原生图形库的 PT_LOAD 对齐为 16384，RELRO 页保护范围不覆盖其他可写段，`scripts/check-apk-alignment.py` 检查通过。此为静态检查，不代替 16 KB 系统实测。
- APK 实际清单确认：包名 `com.aurora.arcade`、版本名 `0.1`、版本码 `2`、目标 API 36，无网络权限。
- 签名证书 SHA-256：`c67dd093001c935f61403c263e9ad0bf6cc5a4b5214a83d5b8612aaa0c3b08da`。

## Android 16

项目专用 AOSP ARM64 模拟器：Android 16 / API 36，构建 `BE2A.250530.026.D1`，1080 × 2160 / 420 dpi，60 Hz，宿主 GPU，Apple M4 Pro。

已通过安装、启动、顶部双预览、系统返回触发暂停、恢复游戏、左右长按到墙。四行消除场景得到 1327 分，撤回恢复 500 分，第二次撤回禁用，顶部暂存可用。

另使用游戏引擎生成的失败后存档检查结束画面：动画结束后 1.2 秒内新增绘制帧为 0；撤回后移动可用，原有两个固定格保持不变，当前 O 块继续正常下落。展开并收起通知栏会暂停，返回首页可用，崩溃缓冲区为空。首次全屏系统教程已正常确认。

测试镜像为 4 KB 内存页系统。正常屏幕实际截图：[Android 16 游戏界面](screenshots/api36-game.png)。

## 连续操作与帧时间

最终签名 release APK，Android 16、60 Hz、宿主 GPU；包含左右移动、双向旋转、快速落底、撤回，操作间隔 70 ms。先做 25 轮短测，再做 120 轮约一分钟连续采样。

| 指标 | 120 轮采样 |
| --- | --- |
| 连续输入时长 | 60.35 秒 |
| 渲染帧数 | 3628 |
| Android janky frames（新口径） | 1（0.03%） |
| Frame deadline missed | 1 |
| gfxinfo 总帧时间 P50 / P95 / P99 | 19 / 21 / 23 ms |
| 最近 120 帧 UI 阶段 P50 / P95 | 0.455 / 1.098 ms |
| 结束分数 / 固定方格 | 500 / 36，恢复正确 |
| 崩溃 | 0 |

UI 阶段按 `HandleInputStart → SyncQueued` 计算，不是手指触屏到显示的端到端延迟。模拟显示链路包含等待，旧版 jank 计数为 598（16.48%），与新口径指标不同，不能隐藏或混用。实际屏幕触感和高刷新率表现仍需实体手机验证。[原始 gfxinfo 记录](metrics/android16-0.1-gfxinfo.txt)。

## 源码独立构建

从已提交的 Git 源码导出干净目录，不包含 `.signing`、`local.properties` 或本地构建输出，使用已安装 SDK 和依赖 / 任务缓存离线执行 `:core:test :app:lintRelease :app:assembleRelease`，17 秒完成，生成无签名 release APK。其 DEX、AndroidManifest 和资源表与最终签名发布包逐字节一致。此前核心 24 项测试已使用 `--rerun-tasks` 重新执行。

GitHub OAuth 授权不含 `workflow`，未启用 GitHub Actions。可审阅的自动构建模板位于 `docs/ci/android.yml`；并未将本机检查描述为云端 CI 通过。

## Android 11 小屏回归

同一最终签名 APK 在 Android 11 / API 30 ARM64、720 × 1280 / 360 dpi、系统字体 1.3 倍下安装启动正常。棋盘 `[150,108]–[571,937]`，顶部两个预览及全部七个按键完整可见。实际执行左右移动、长按、双向旋转、软降、四行消除（1321 分）、撤回（恢复 500 分且第二次禁用）和暂存；崩溃日志为空。

## 本次响应与功耗改进

- 按下即修改逻辑位置；旋转 / 落底 / 撤回不会因为动画而禁止输入。默认 DAS 140 ms、ARR 40 ms；灵敏操控 100 / 28 ms，软降 35 ms。
- 帧时钟在 Canvas 绘制阶段读取，避免仅因时钟更新而重新执行棋盘组合。
- 失败后的最后动画播放完便停止持续刷新；从失败状态撤回时重置计时，避免将结束画面的等待时间计入下落。
- 只在实际游戏进行中保持屏幕常亮，首页、暂停和结束画面允许系统休眠。
- 失焦 / 暂停清空长按输入，触控取消也释放按键。

## 验证范围

尚未在实体手机、最低 API 26、90 / 120 Hz 屏幕或 16 KB 内存页系统上实测。模拟器的帧数据受宿主负载和显示链路影响，不能据此保证所有手机零掉帧。本次发布渠道为 GitHub Release，不包含应用商店审核。

适配依据：[Android 16 SDK 配置](https://developer.android.com/about/versions/16/setup-sdk)、[Android 16 行为变化与游戏分类例外](https://developer.android.com/about/versions/16/behavior-changes-16)、[AGP 8.10 的 API 36 支持](https://developer.android.com/build/releases/agp-8-10-0-release-notes)、[16 KB 页兼容检查](https://developer.android.com/guide/practices/page-sizes)。
