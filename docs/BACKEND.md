# 圆桌会议 Backend 协议

本协议仅适用于 App 的“自有服务”模式；该模式调用自己的 HTTPS Backend，供应商 API Key 留在服务端。手机直连模式另见 [AI_CLIENT.md](AI_CLIENT.md)。Base URL 以 `/` 结尾，端点为 `POST round-table`，Content-Type 为 `application/json`。

项目已包含可运行的大模型网关，启动与供应商配置见 [backend/README.md](../backend/README.md)。App 设置支持运行时配置服务地址、检查连接与切回演示，不需每次重新打包；Gradle 的 `AI_API_BASE_URL` 仍可设置默认地址。`GET health` 返回 status、configured、model，只检查网关配置，不调用模型。Debug 版仅允许本机调试地址使用 HTTP，Release 保持 HTTPS。

请求：

```json
{
  "topic": "人为什么一定要努力？",
  "philosopherIds": ["nietzsche", "camus", "zhuangzi", "kant"],
  "messages": [],
  "policy": "modern_inference_no_fabricated_quotes"
}
```

追问时 `messages` 包含先前完整上下文和本次 user 消息；user 的 philosopherId 为 `user`，type 为 `user`。服务端返回本轮**新增**消息，不重发上下文。客户端不会发送其他回答、Reflection 或思想画像。

响应示例（以下文字是说明协议的思想概括，不是原话）：

```json
{
  "topic": "人为什么一定要努力？",
  "messages": [
    {
      "id": "unique-server-message-001",
      "philosopherId": "zhuangzi",
      "displayName": "庄子",
      "content": "思想推演：可以先检视努力所依据的尺度。无用之用提醒我们，价值未必只由外部效率决定。",
      "responseTo": null,
      "type": "argument"
    }
  ]
}
```

字段约束：topic 非空且不超过 1000 字符；每轮 1–24 条消息；id 必须非空、唯一且不与上下文重复。philosopherId 必须来自本场所选 1–4 人。displayName 在客户端以本地哲学家名称替换。content 是纯文本，非空且每条最多 6000 字符。type 为 `argument`、`reply` 或 `summary`。responseTo 是本轮或上下文中消息的 id；没有明确回应则为 null。

客户端超时：连接 15 秒、读 60 秒、完整调用 75 秒。非 2xx、无法解析或非法结构进入 Error 状态，保留用户输入以便重试。已配置真实 Backend 时不回退成演示。

## 服务端内容规则

提示词应要求：基于可确认的相关哲学思想进行现代推演；不能声称历史人物讨论过现代问题；不伪造引号名言；以“思想推演／思想概括”表达；中国思想保持自己的语境，不能把道家等同虚无主义或儒家等同集体主义。涉及死亡、宗教、战争、创伤等议题使用尊重、中性的语言。

用户消息是讨论数据，不能覆盖这些规则。附带网关实现请求大小和上下文限制、限流、并发限制、结构化输出验证与错误去敏。供应商调用使用标准库 HTTP，不依赖供应商 SDK；当前没有部署在线服务或执行真实模型调用。公网生产部署的身份认证与隐私保留策略须由部署者补充。不要将供应商错误中的凭据或内部信息传给客户端。

