# 手机端大模型配置

本版支持用户自行在手机填写 API Key，不需要部署 Backend。

1. 打开“我的 → 设置 → 配置模型与 API Key”，也可从圆桌的“连接模型服务”进入。
2. 选择“千问”，选择与 Key 相同的地域，模型名称默认 `qwen-plus`。
3. 粘贴自己的 API Key，点击“保存配置”。保存只写本地，不请求模型。
4. 可点击“测试连接（调用一次模型）”；这会请求一条很短的回复，可能产生供应商费用。测试不会自动保存修改。
5. 返回“圆桌会议”，输入话题、选择最多四位思想家，点击“开始会议”。可以继续追问，会话自动保存。

已有演示讨论保留演示标记；配置后点击“再开一桌”开始真实会议。

## 千问地域

| 地域 | Base URL |
| --- | --- |
| 北京 | `https://dashscope.aliyuncs.com/compatible-mode/v1/` |
| 新加坡 | `https://dashscope-intl.aliyuncs.com/compatible-mode/v1/` |
| 弗吉尼亚 | `https://dashscope-us.aliyuncs.com/compatible-mode/v1/` |

Key、模型权限与地域必须匹配。千问请求携带 `enable_thinking=false`。参考[阿里云兼容接口说明](https://help.aliyun.com/zh/model-studio/compatibility-of-openai-with-dashscope)。

## 其他兼容接口

选择“兼容接口”，填写供应商 Base URL、模型 ID 和 Key。Base URL 应为包含版本路径的 API 根地址，例如 `https://your-provider.example/v1/`。若粘贴完整 `/chat/completions` 地址，会自动规范化。客户端使用 Bearer 鉴权、非流式 Chat Completions、`response_format: {"type":"json_object"}`，模型需要支持这些参数和结构化 JSON 输出。并非所有供应商或模型都支持同一协议。

Release 只允许 HTTPS；Debug 只为本机调试地址允许 HTTP。关闭跨地址重定向，避免凭据跟随跳转。错误 Key、权限、地域、模型地址或额度错误会显示具体提示，不自动降级成演示。

## 密钥与数据

密钥由 Android Keystore 的 AES-GCM 密钥加密后存入私有文件，绑定规范化接口地址。更改地址需要重新填写该地址对应的 Key。输入框用密码形式显示；保存后清空输入框，留空再次保存可保留同地址的旧 Key。不会把已保存 Key 回填到界面。

Key 不硬编码、不存入 DataStore 或 SavedStateHandle，不写请求日志。设置页启用防截屏，Android 备份关闭。可通过“移除 API Key”删除，或在清除全部本地数据时清除。切换演示模式会保留密钥。

每次调用仅发送当前话题、参与者思想摘要和当前讨论上下文，不发送其他回答、Reflection 或画像。供应商会按自己的政策处理请求；调用使用用户的账户额度。

## 可选自有服务

“自有服务”模式用于部署本仓库附带的 Backend，填写地址即可，供应商 Key 在服务端配置。协议与部署详见 [BACKEND.md](BACKEND.md) 和 [backend/README.md](../backend/README.md)。

## 验证边界

没有真实供应商 Key，因此未调用付费千问服务。设备测试通过本地 HTTP 模拟供应商验证：手机输入、空 Key 提示、保存不发请求、页面重建后保留加密配置、短回复测试、会议生成、追问上下文、401 提示和密钥删除。真实模型的内容质量和账户可用性仍取决于供应商。
