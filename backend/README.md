# 圆桌会议大模型接口

这是可选的“自有服务”模式。手机自行填写 Key 的直接接入方式见 [手机接入说明](../docs/AI_CLIENT.md)。

Python **3.11+**，只使用标准库。Android → 自己的圆桌 Backend → 兼容 Chat Completions 的模型服务。供应商密钥只通过服务端环境变量读取，不进入 APK 或客户端 DataStore。

## 启动

在项目根目录运行，PowerShell 示例：

```powershell
$env:AI_UPSTREAM_BASE_URL = 'https://api.deepseek.com'
$env:AI_MODEL = '填写供应商当前支持且有权限使用的模型 ID'
$env:AI_API_KEY = '填写您自己的供应商密钥'
python backend/server.py
```

不要把真实密钥写进仓库。可用部署平台的 secret manager 设置这些环境变量。服务不自动读取 `.env` 文件。

兼容服务可以更换 `AI_UPSTREAM_BASE_URL` 与 `AI_MODEL`。基础地址须包含供应商要求的路径前缀，如 `/v1`；程序追加 `/chat/completions`。默认发送 `response_format: {"type":"json_object"}`。不支持 JSON mode 的兼容服务可设 `AI_JSON_MODE=false`，但仍必须返回可校验的 JSON，格式错误不会假装成功。

协议参考：[DeepSeek Chat Completions](https://api-docs.deepseek.com/api/create-chat-completion/)、[JSON Output](https://api-docs.deepseek.com/guides/json_mode/)。这说明协议兼容方式，不保证任意服务或任意模型都支持全部参数。

服务默认监听 **127.0.0.1:8080**。未填模型配置时可以启动，`health.configured=false`；讨论请求返回 503，不使用演示冒充模型输出。

## 千问 / 阿里云百炼

支持百炼的 Chat Completions 兼容接口与 JSON 输出。先在百炼控制台复制您所属地域／业务空间的 API Host，并创建该地域的 API Key。按 [官方 Base URL 文档](https://help.aliyun.com/zh/model-studio/base-url) 选择地址，不能混用地域的密钥和 Host。

```powershell
# 把 YOUR_WORKSPACE_ID 替换为自己的业务空间 ID，或直接复制控制台提供的兼容 Base URL。
$env:AI_UPSTREAM_BASE_URL = 'https://YOUR_WORKSPACE_ID.cn-beijing.maas.aliyuncs.com/compatible-mode/v1'
$env:AI_MODEL = 'qwen3.8-max'
$env:AI_API_KEY = '填写您自己的百炼 API Key'
$env:AI_JSON_MODE = 'true'
$env:AI_ENABLE_THINKING = 'false'
python backend/server.py
```

模型必须是当前业务空间有调用权限、支持 JSON 输出的型号，可按控制台更换。当前网关使用非流式响应，千问配置显式关闭思考模式，避免要求流式的思考模型路径。`AI_ENABLE_THINKING` 默认不发送，兼容其他供应商；配置千问时设为 false。参数依据 [千问结构化输出文档](https://help.aliyun.com/zh/model-studio/qwen-structured-output)。

Android 仍填写自己 Backend 的地址，不直接填写百炼地址或 API Key。尚未使用真实百炼凭据完成付费调用；新增测试使用模拟供应商验证千问请求参数。

## Android 本地调试

Debug APK：设置 → 圆桌会议 · 大模型服务，填写 `http://10.0.2.2:8080/`，保存，然后“检查连接”。Android 模拟器的 10.0.2.2 指向主机回环地址。

也可以通过 USB / adb 转发给测试设备：

```powershell
adb reverse tcp:8080 tcp:8080
```

此时在 Debug APK 填写 `http://127.0.0.1:8080/`。手机自身的 localhost 并不会自动指向电脑。Debug 仅允许这几个本机地址使用 HTTP；Release 一律 HTTPS。

填写地址后新会议使用真实服务；清空或点“使用演示模式”恢复演示。已有演示会议仍是演示，已有真实会议不会回退为演示。应用重启后配置保留。检查连接调用 `/health`，不触发收费模型请求；模型认证、配额或生成质量须由实际会议调用验证。

## 对外部署边界

这是可运行的开发／自托管网关，不是已部署的公共服务。默认仅绑定本机，不带公网用户认证、账户或计费系统。对外部署必须置于自己的 HTTPS 反向代理或受控私有网络后；公开商用之前需补充用户认证、配额、审计和滥用防护。不要将此标准库开发服务器直接开放到公网。身份认证应由部署方案实现；当前 App 尚未提供登录／会话令牌流程。

反向代理可把 `/api/` 前缀剥离后转发到本服务，App 地址则设 `https://your-service.example/api/`。需要 HTTPS 证书、上游出站访问权限；真实模型可能产生供应商费用。部署请同时带上 `server.py` 与 `philosophers.json`，目录保持相邻。人物内容与 Android seed 一致，更新时应同步。

实现包含：请求体 128KiB 上限、上下文最多40条／40000字符、每客户端8次/分钟、最多4个并发模型请求、上游50秒超时、服务端／客户端双重结构校验、规范人物名称、仅返回新增回复、无请求正文日志。限制是开发网关的基础保护，并不替代生产身份认证。

## HTTP 协议

- `GET /health` → `{"status":"ok","configured":true,"model":"your-model"}`。不调用上游。
- `POST /round-table` → 结构化新增发言。请求／响应字段见 [Backend 协议](../docs/BACKEND.md)。
- 400 请求不合法；413 超出请求大小；415 不支持的 Content-Type；429 频率或并发超限；503 未配置模型；502 上游异常或非法结构。错误不会透出供应商响应正文或凭据。

## 测试

```powershell
python -m unittest discover -s backend -v
```

14 项测试通过本地 HTTP 模拟供应商验证网关：首次请求、追问上下文、名称规范化、health 不调用模型、非法请求、非法模型返回、未配置、请求大小、限流、重定向拒绝、千问非思考参数和密钥不出现在配置打印中。测试不使用真实密钥、不发生真实付费调用。



## 1.0.6 圆桌扩展协议

支持本轮发言名单 `speakerIds`、定向回应 `replyTo`、独立小结 `mode=summary`，以及普通讨论同时返回小结 `includeSummary=true`。协议与响应示例见 [圆桌体验说明](../docs/MEETING_EXPERIENCE.md)。请求保留全体成员与上下文；模型本轮只从名单内发言，每位至多一条。小结每项需要有效的发言 ID；独立小结不补写发言，不支持或格式不正确时返回明确错误。自定义服务应同步升级，旧字段保持兼容。
