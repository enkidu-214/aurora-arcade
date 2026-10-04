# 构建与发布

Release 0.1 的版本名称为 `0.1`，版本码为 `2`，标签为 `v0.1`。发布 APK 使用独立 RSA 发布签名，和早期本地测试签名不同。

## 签名

当前发布密钥和密码仅保存在本机项目的 `.signing/` 目录。请将整个目录另行备份到安全位置；未来覆盖升级必须继续使用同一密钥。不要提交、公开或放进 Release 附件。

新检出的仓库不包含私钥，默认生成 `app-release-unsigned.apk`。开发运行可直接使用 `assembleDebug`。要生成兼容现有用户升级的发布包，先恢复原来的密钥及 `.signing/release.properties`：

```properties
storeFile=.signing/aurora-release.jks
keyAlias=aurora-release
storePassword=<原来的密码>
keyPassword=<原来的密码>
```

其他开发者若要分发自己的派生版本，应使用自己的包名和签名。

## 检查与打包

安装 JDK 17 或 21、Android SDK 36、Build Tools 36.0.0，然后运行：

```sh
./gradlew :core:test :app:lintRelease :app:assembleRelease :app:assembleDebug
```

发布前必须安装优化后的 release 包，在目标系统检查移动 / 长按 / 双向旋转 / 软降 / 暂存 / 消行 / 落底 / 单步撤回 / 失败后撤回 / 暂停恢复，以及屏幕边缘系统手势。记录设备、系统、帧数据和实测范围。

```sh
python3 scripts/check-apk-alignment.py app/build/outputs/apk/release/app-release.apk
zipalign -c -P 16 4 app/build/outputs/apk/release/app-release.apk
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
cp app/build/outputs/apk/release/app-release.apk releases/aurora-arcade-0.1.apk
cd releases
shasum -a 256 aurora-arcade-0.1.apk > SHA256SUMS
shasum -a 256 -c SHA256SUMS
```

Git 中保留源码、校验和验证记录；APK 上传至对应标签的 GitHub Release。CI 只验证构建，既不持有发布私钥，也不自动发布。

本项目为独立开发的离线方块游戏，与 Tetris 官方没有关联；未打包其图片、商标、音乐或代码。
