# Release 0.1 验证记录

日期：2026-10-04。版本名 `0.1` / 版本码 `2`。最终发布前的设备压力测试与结果仍在补充，正式标签以完成后的本文件为准。

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

## 本次响应与功耗改进

- 按下即修改逻辑位置；旋转 / 落底 / 撤回不会因为动画而禁止输入。默认 DAS 140 ms、ARR 40 ms；灵敏操控 100 / 28 ms，软降 35 ms。
- 帧时钟在 Canvas 绘制阶段读取，避免仅因时钟更新而重新执行棋盘组合。
- 失败后的最后动画播放完便停止持续刷新；从失败状态撤回时重置计时，避免将结束画面的等待时间计入下落。
- 只在实际游戏进行中保持屏幕常亮，首页、暂停和结束画面允许系统休眠。
- 失焦 / 暂停清空长按输入，触控取消也释放按键。

## 验证范围

尚未在实体手机、最低 API 26、90 / 120 Hz 屏幕或 16 KB 内存页系统上实测。模拟器的帧数据受宿主负载和显示链路影响，不能据此保证所有手机零掉帧。本次发布渠道为 GitHub Release，不包含应用商店审核。

适配依据：[Android 16 SDK 配置](https://developer.android.com/about/versions/16/setup-sdk)、[Android 16 行为变化与游戏分类例外](https://developer.android.com/about/versions/16/behavior-changes-16)、[AGP 8.10 的 API 36 支持](https://developer.android.com/build/releases/agp-8-10-0-release-notes)、[16 KB 页兼容检查](https://developer.android.com/guide/practices/page-sizes)。
