# 游戏中的哲学

入口：探索 → 游戏中的哲学。首批作品《素晴日》《樱之诗》各自使用独立卡片与详情页面，每部四个思想入口。支持作品名、日文名、英文别名与主题搜索，全部笔记离线可读。

## 内容范围

- 素晴日：语言与世界、幸福与事实、表达的限度、认识与不同视角。
- 樱之诗：日常幸福、审美分歧、艺术与用途、哲学梯子与生活。
- 每条包含作品联系、哲学解释、常见误解、带回自己生活的问题、相关哲学家／选择问题和可点击来源。
- 标注“官方主题”“作品关联解读”“延伸对照”。比较康德、亚里士多德、王尔德不等于宣称角色明确引用或代表这些思想。不捏造游戏台词与作者意图。
- 涉及后段主题的条目默认折叠，用户主动展开；重建页面保留展开状态，可以再次收起。当前笔记不揭示人物身份、具体结局或详细伤害情节。
- 图片为原创 Canvas 天空窗口／樱花画框插画，未复制游戏封面、角色图或游戏截图。外部官方网页可能含成人原作内容，页面说明外链性质。

## 来源与依据

[枕《樱之诗》官方故事介绍](https://www.makura-soft.com/sakuranouta/story/story.html)直接说明两作幸福主题的联系及日常、梯子意象。素晴日的作品概念参考[发行方介绍](https://store.steampowered.com/app/658620/Wonderful_Everyday_Down_the_RabbitHole/)。

维特根斯坦关联辅助核对[官方视觉档案中作者解说的日文转录](https://amenowoto.blog/blog/2026-02-03/subahibi-archive-translation/)；此为第三方转录，App 明示来源属性，不将转录当作已核对的游戏逐句台词。

哲学解释参考[《逻辑哲学论》原典](https://www.gutenberg.org/files/5740/5740-pdf.pdf)、[《尼各马可伦理学》](https://www.gutenberg.org/files/8438/8438-h/8438-h.htm)、[《道林·格雷的画像》序言](https://www.gutenberg.org/files/174/174-h/174-h.htm)以及[康德美学学术解说](https://plato.stanford.edu/entries/kant-aesthetics/)。条目采用独立短篇概括与比较，不搬运原作长段台词。

## 工程与验证

内容位于 `app/src/main/assets/seed/games.json`，独立内容版本 1。GameRepository 在 IO 线程解析，AppViewModel 用 StateFlow 提供 UI 数据；只读编辑笔记无需新建 Room 表或迁移历史数据。可通过新增 GameWork 与 GameTopic 扩充作品。

GameContentTest 校验 JSON、作品与条目 ID、来源 HTTPS、内容非空以及题目／哲学家引用的完整性。GamePhilosophyJourneyTest 在设备验证别名搜索、独立作品页面、剧透默认隐藏与重建恢复、哲学家和问题导航。最终结果见 [VALIDATION.md](VALIDATION.md)。

## 内容边界

首批仅两部作品、八篇短笔记，属于哲学主题阅读入口，不是逐章文本考证或完整剧情解析。未逐句核对所有原作版本，未标为明确引用的观点都按解读或延伸处理；未来扩大书目与原作审校后再添加更细的章节定位。作品版权属于原权利人，本 App 与其无官方关联。
