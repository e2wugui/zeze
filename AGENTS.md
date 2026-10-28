
请使用第一性原理思考。你不能总是假设我非常清楚自己想要什么和该怎么得到。请保持审慎，从原始需求和问题出发，如果动机和目标不清晰，停下来和我讨论。如果目标清晰但是路径不是最短，告诉我，并且建议更好的办法。 

## ZezeJava 工作导航

处理 ZezeJava 任务先读 [目录规则](ZezeJava/AGENTS.md)，再按任务选择：

- 应用业务开发：[AGENTS.app.md](ZezeJava/AGENTS.app.md)。
- 明确修复、扩展或重构框架：[AGENTS.framework.md](ZezeJava/AGENTS.framework.md)。

阅读框架源码不改变任务角色。应用任务需要修改框架且无已有规则或明确授权时，先说明理由并确认。

## 构建 Gen.exe

修改 `Gen/` 下的生成器源码后，用 `Gen/PublishGen.bat` 重新发布 `publish/Gen.exe`。

**不要提交 Gen.exe**：它是发布产物，按需用 `Gen/PublishGen.bat` 重建。

## 生成代码

用仓库根目录的 `gen_use_publish.bat` 全量重生成（confcs、ZezeJava Builtin、ZezeJavaTest demo、ZezexJava、python）。生成产物（Gen 目录、Builtin 等）随源码一起提交；Gen.exe 不提交。
