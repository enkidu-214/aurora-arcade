# 安装包

下载 [Release 0.4](https://github.com/enkidu-214/aurora-arcade/releases/tag/v0.4) 中的 `aurora-arcade-0.4.apk`，版本 0.4（versionCode 5），包含十款游戏，新增超新星弹球。沿用发布签名，可覆盖旧版保留存档；校验文件为 `SHA256SUMS` 或 `aurora-arcade-0.4.apk.sha256`，执行 `shasum -a 256 -c aurora-arcade-0.4.apk.sha256`。验证记录见 [弹球版本验证](../docs/pinball-0.4-verification.md)。

本地九游戏开发版：`aurora-arcade-0.3.apk`，版本 0.3（versionCode 4），沿用发布签名，可以覆盖安装 0.1 / 0.2。校验文件为 `aurora-arcade-0.3.apk.sha256`，执行 `shasum -a 256 -c aurora-arcade-0.3.apk.sha256`。此包已在本机生成，尚未创建对应 GitHub Release。

下载 [Release 0.2](https://github.com/enkidu-214/aurora-arcade/releases/tag/v0.2) 中的 `aurora-arcade-0.2.apk` 和 `SHA256SUMS`。此版本包含新增音效，版本号为 0.2（versionCode 3），沿用 0.1 的发布签名，可直接覆盖升级。

APK 不进入 Git 历史，本目录中的 `SHA256SUMS` 对应公开发布的版本化 APK。在两文件所在目录执行 `shasum -a 256 -c SHA256SUMS`（或 Linux 的 `sha256sum -c SHA256SUMS`）即可校验。

历史版本请使用对应 Release 页面的安装包及校验文件。安装时以带版本号的 APK 为准；本机的 `aurora-arcade-debug.apk` 是开发调试包，签名不同，不能覆盖正式发布包。
