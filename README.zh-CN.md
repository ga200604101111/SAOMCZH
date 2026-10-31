<p align="center">
  <a href="./README.md">English</a> | <a href="./README.zh-TW.md">繁體中文</a> | <strong>簡體中文</strong>
</p>

# SAOMCZH

**面向 Minecraft《刀剑神域》（Sword Art Online）的社区繁体中文本地化项目。**

SAOMCZH 维护经过审核的翻译词库、限定来源的客户端文字拦截规则、中文字形与排版修复，以及经过签名验证的 Java Agent 更新架构。项目希望在**不修改服务器原始数据、玩家消息或第三方模组 JAR** 的前提下，让游戏内容逐步实现繁体中文化。

> **语言说明：** 本 README 提供 English、繁體中文、简体中文三种阅读语言；目前实际维护的**游戏翻译语言只有 `zh_tw`**。提供简体中文说明文件，**不代表**已经提供 `zh_cn` 游戏词库。

## 当前开发版本

以下为截至 **2026 年 10 月 31 日**，已经测试的源码及本地集成状态；**不代表全部对应的二进制文件都已作为公开 GitHub Release 发布**。

| 组件 | 状态 |
| --- | --- |
| 固定 Java Agent | `SAO_ZH_Agent_v17.jar` |
| 外部 Runtime | 1.8.0 — 已完成签名并准备在下次启动时加载 |
| 拦截规则 | 1.12.0 — 130 个限定来源 Hook |
| 繁体中文词库 | 1.0.19 — 8,836 条 |
| Compatibility 兼容模块 | 1.1.0 |
| 开发用游戏环境 | Minecraft 1.21.4／Fabric；针对特定已安装 SAO 模组进行测试 |

Runtime 与规则已经通过字节码检查及隔离 Java Agent 启动测试。**这不代表所有界面、字体或 MCEF 语音功能都已在真实 Minecraft 游戏画面中完成验收。**

## 功能特性

- **跨模组共享词库：** `names`、`items`、`lines`、`messages` 能在受支持的界面之间复用审核过的译文，同时保留分类优先级及安全限制，减少重复翻译。
- **限定来源的动态翻译：** 涵盖部分物品名称与描述、生物和 NPC 名称、任务、技能、Actionbar／系统通知、地点、悬浮文字及 HUD。
- **中文字形与排版：** 尽量保留文字颜色、图标、原生物品数量、`Lv.` 等级标记与数值；在支持的入口先翻译，再测量中文字宽。
- **漏译收集：** 记录允许收集的未知游戏文字供后续审核，而不是无差别收集玩家聊天。
- **模块化更新：** 通过验证的词库可以与 Runtime、拦截规则分开更新；部分代码或资源变更仍需重新启动。
- **Sound of Aincrad 网页界面：** 通过 **Java Agent 的限定拦截入口**翻译 MCEF 内嵌网页，**不需要安装额外的 Fabric 模组**，也不修改音频、WebRTC、设备名称或按键操作。

目前仅处理已经确认的文字来源，不保证所有界面都已翻译；无法识别或暂不支持的内容会保留原文。

## 仓库结构

| 路径 | 用途 |
| --- | --- |
| [`languages/zh_tw/translations.json`](languages/zh_tw/translations.json) | 分为四类的繁体中文审核词条 |
| [`languages/zh_tw/zh-tw-version.json`](languages/zh_tw/zh-tw-version.json) | 词库版本、文件大小及 SHA-256 |
| [`languages/zh_tw/hook_text_rules.json`](languages/zh_tw/hook_text_rules.json) | 与语言相关、受执行限制的文本规则 |
| [`languages/locales.json`](languages/locales.json) | 已注册的游戏翻译语言 |
| [`extensions/modules/`](extensions/modules/README.md) | 模块化拦截规则与依赖关系 |
| [`client-framework/`](client-framework/README.md) | Agent、Runtime、Compatibility 源码及技术说明 |
| [`tools/validate.py`](tools/validate.py) | 版本描述文件、哈希及变量占位符验证 |
| [`tools/github_sync.py`](tools/github_sync.py) | 可选的 GitHub 词库缓存同步工具 |
| [`tools/export_missing.py`](tools/export_missing.py) | 导出本机收集的漏译候选内容 |

根目录的 [`manifest.json`](manifest.json) 记录项目及语言版本信息。**资源包单独维护在 [SAOMCZHPACK](https://github.com/ga200604101111/SAOMCZHPACK)**，不与翻译词库混在一起。

## 翻译如何更新？

1. 保留**原始文本匹配键**，添加或修改翻译；不要破坏变量占位符、颜色代码、图标或必要的空格。
2. 更新词库版本，以及版本文件中的 `size_bytes`、`sha256`。
3. 执行下列验证命令，检查结果后再发布。
4. 客户端更新器可以下载并验证新版词库；已经打开的界面或缓存文字可能需要重新打开或刷新。
5. Runtime 与 Compatibility 使用独立的签名验证流程，通常在下次游戏启动时生效。**单独更新词库不需要重新构建固定 Java Agent。**

更新信息使用数字签名和 SHA-256 验证。没有签名的开发版 Runtime 不应被当作可信任的正式更新使用。

## 开发者验证

在仓库根目录运行：

```bash
python tools/validate.py
python -m unittest discover -s client-framework/tests -p "test_*.py"
python -m unittest discover -s tests -p "test_*.py"
```

如果需要从源码构建，请参考 [`client-framework/tools/compile_sources.py`](client-framework/tools/compile_sources.py)。源码编译只会生成**未签名**的开发文件，部署前仍需要单独的信任密钥、签名与验证流程。

可通过以下命令查看漏译数据的导出方法：

```bash
python tools/export_missing.py --help
```

共享之前请再次检查导出内容；本地诊断记录可能包含上下文信息，不适合直接全部公开上传。

## 安装与运行限制

本仓库**不是一键模组安装包**。在受支持的环境中，已有的固定 Java Agent 通过 JVM 启动参数 `-javaagent:...` 加载，外部模块和规则则通过各自的签名更新机制管理。**不要把 Java Agent 放进 Minecraft 的 `mods` 文件夹，也不需要为了 Sound of Aincrad 翻译另外安装 Fabric 模组。**

请保留原版 SAO、Sound of Aincrad 与 MCEF 模组，并保存上一次可以正常运行的备份。更新 Runtime 或资源包时，可能需要完全重启原有游戏实例。

## 更多技术文档

- [Agent 与 Runtime 架构](client-framework/README.md)
- [功能模块目录](extensions/modules/README.md)
- [悬浮文字翻译流程](client-framework/docs/world-text-pipeline-20261029.md)
- [中文字体、排版和地点 HUD](client-framework/docs/typography-and-discovery-20261030.md)
- [仅通过 Java Agent 实现的 MCEF 网页界面翻译](client-framework/docs/zapaudio-agent-webui-20261031.md)
- [独立资源包仓库](https://github.com/ga200604101111/SAOMCZHPACK)
- [GitHub Releases](https://github.com/ga200604101111/SAOMCZH/releases)

## 社区项目与第三方权利

SAOMCZH 是非官方社区翻译项目，并非《刀剑神域》或相关 Minecraft 模组的官方发行版本。第三方内容及资源仍受各自的许可和再分发条件约束。请勿将服务器凭据、私人签名密钥或未经许可分发的原版模组资源提交到本仓库。
