# 构建与交付验证

## 当前版本：1.0.8 / versionCode 9

验证日期：2026 年 10 月 9 日（Asia/Shanghai）。

- `:app:build :app:connectedDebugAndroidTest` **BUILD SUCCESSFUL**，耗时 5 分 11 秒。
- Debug / Release 各 **54 项 JVM 测试**，0 失败、0 错误、0 跳过。
- **32 项 Android 测试**，0 失败、0 跳过；Pixel 8 / Android 16 / API 36 模拟器。
- 服务端 **14 项 Python 测试**通过，不调用真实付费模型。
- lint **0 errors、19 warnings**，为依赖更新和 targetSdk 版本提示，没有关闭检查或加入 baseline。
- 新增 TIPS 内容、笔记 DAO 与真实页面测试，覆盖搜索、输入、旋转、保存重开、关联词语／人物／问题、单条删除确认、清理边界，以及 2→3 和 1→2→3 迁移。
- 实际检查浅色、深色、个人理解与正式包截图。测试期间发现的相关词语复用旧页面状态问题已修复并回归通过。

### 正式包与升级

文件：`LittleThoughts-1.0.8-release.apk`，12,709,626 字节。

SHA-256：

```text
1762FFB76502EE33F543688FE928D924A48082E2A9115D7E50DC3B6EA623E6AC
```

`apksigner verify` 通过，沿用 1.0.7 的正式 RSA 3072 密钥和 v2 签名；公开证书指纹见 [签名说明](RELEASE_SIGNING.md)。包名 `com.xiaojidiu.philosophy`，minSdk 26、targetSdk 35。

实际升级验收：在模拟器安装官方 1.0.7 正式包，完成引导并回答 q30，然后用 `adb install -r` 覆盖安装 1.0.8。dumpsys 确认 versionName 1.0.8、versionCode 9；不重复引导，个人页显示原有一次回答，历史仍包含原问题和选项。正式包中的 TIPS 列表与形而上学详情实际打开成功。

截图：`tips-list.png`、`tips-detail.png`、`tips-understanding.png`、`tips-dark.png`、`tips-release-upgrade-history.png`、`tips-release.png`，均位于 `docs/screenshots/`。

TIPS 的内容与数据边界见 [TIPS.md](TIPS.md)。个人理解只保留每词最新文字，没有笔记版本时间线、云同步或导出；不评分、不影响思想画像、不随圆桌请求上传。

### 验证边界

正式包已在模拟器安装、运行并覆盖升级验证；没有实体设备、API 26 真机、厂商系统、真实付费模型或系统低内存杀进程新增实测。音乐、提醒和内容审校的既有边界仍成立。

---

## 历史记录：1.0.7 / versionCode 8

以下为 2026 年 10 月 7 日首次公开发布的验证记录。

## 构建

- 播放列表完成后执行 `:app:build :app:connectedDebugAndroidTest`，Build 成功（3 分 5 秒）。
- Debug / Release 各 **51 项 JVM 测试**，均无失败、错误或跳过。
- **25 项 Android 测试**，均无失败或跳过；环境为 Pixel 8 / Android 16 / API 36 模拟器。
- 独立 Release 签名配置加入后，再执行 `:app:build`，成功（42 秒）；Android 页面代码未变，此次没有重复运行设备测试。
- 服务端 **14 项 Python 测试**通过，不调用真实付费模型。
- lint **0 errors、19 warnings**，为依赖更新和 targetSdk 版本提示，没有关闭检查或加入 baseline。

## 正式安装包

GitHub Release：`v1.0.7`。文件：`LittleThoughts-1.0.7-release.apk`。

包名 `com.xiaojidiu.philosophy`，minSdk 26、targetSdk 35，versionName 1.0.7、versionCode 8。Release 不含 debuggable 标记。独立正式签名通过 `apksigner verify`，签名说明见 [RELEASE_SIGNING.md](RELEASE_SIGNING.md)。

文件大小：12654983 字节。

SHA-256：

```text
62CCCD2F88EDE0095CC1D7A534CEDDD057FF0E87AC8E53820FF806B4C9FBE7E4
```

此前 Debug 交付包曾在模拟器安装并用 dumpsys 确认版本。正式签名包完成构建、签名和包元数据校验，未覆盖安装已有 Debug 包，以保留其本地数据。正式包与 Debug 签名不同，不能直接升级覆盖。

## 覆盖的产品流程

首次引导 → 今日问题 → 选择 → 背景与多方观点 → Reflection → 思想画像 → 历史 → 重新回答与过去留言 → 探索 → 哲学家／流派／思想关系 → 收藏 → 游戏作品与剧透折叠 → 圆桌演示／真实 HTTP 模拟模型 → 用户头像／昵称 → 定向追问、小结与来源回看 → 会后念头与历史 → 设置、主题和删除确认。

另覆盖本地多曲导入、部分无效文件、音频元数据和原生解码、播放进度、旋转、音量持久化、前后台与音频焦点、三种模式、手动切歌、单首移除保留其他文件与原音频，以及旧单曲记录升级。

源截图在 `docs/screenshots/`；本机编译／安装日志和重复拉取副本不提交仓库。

## 验证边界

- 自动化以模拟器为主，没有 API 26 真机、厂商省电、实体耳机／来电及完整人工 TalkBack 回归。
- 音乐测试模拟系统文件选择器返回结果，实际执行文件复制、解析、MediaPlayer 播放；测试提供器不进入 Release。音乐仅在 App 前台播放。
- 真实 HTTP 模型使用本地模拟供应商；未使用用户的真实 Key 调用付费模型。AI 思想推演可能有偏差，小结来源校验只验证消息关联。
- 没有等待通知的 24 小时周期、验证 Doze 准点送达或主动触发操作系统低内存杀进程。
- 本地数据不支持导出／云同步或 Debug → 正式签名包迁移；卸载会删除本地记录。
- 游戏笔记两部作品、八篇主题，直觉选择只恢复页面状态。内容与画像需要持续审校。

进一步说明见 [音乐与配色](BGM_AND_PALETTE.md)、[游戏哲学](GAME_PHILOSOPHY.md)、[手机 AI](AI_CLIENT.md) 和 [圆桌协议](MEETING_EXPERIENCE.md)。
