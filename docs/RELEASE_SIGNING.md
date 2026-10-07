# Release 签名与发布

官方 1.0.7 APK 使用独立的 RSA 3072 签名密钥，APK Signature Scheme v2 验证通过。

公开证书 SHA-256 指纹：

```text
9B2E06E55FF569A936313E95E827FCE97D5AB1BD0EA741AC366EE4FB473C364A
```

密钥不包含在仓库、源码压缩包或 GitHub Release 中。维护者工作目录本地保存：

- `.signing/jidiu-philosophy-release.jks`
- `release-signing.properties`

请将这两个文件备份到自己的安全位置，后续正式升级沿用相同 keystore、别名和密码。不要把真实配置提交 Git，不要重新生成密钥替换现有版本。

其他开发者复制 `release-signing.properties.example` 为 `release-signing.properties`，用自己的 keystore 配置签名；或使用 README 中列出的四个环境变量。无签名配置仍可构建 Debug 和未签名 Release。

本地 Debug 包的签名与官方 Release 不同。Android 拒绝跨签名覆盖安装；本项目尚无跨签名的数据迁移。不要为了安装而无提示删除旧数据。

上传的 APK SHA-256 校验值随 Release 的 `SHA256SUMS.txt` 提供。证书指纹与 APK 文件哈希用途不同，不能互相替代。
