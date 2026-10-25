
请使用第一性原理思考。你不能总是假设我非常清楚自己想要什么和该怎么得到。请保持审慎，从原始需求和问题出发，如果动机和目标不清晰，停下来和我讨论。如果目标清晰但是路径不是最短，告诉我，并且建议更好的办法。 

## 构建 Gen.exe

修改 `Gen/` 下的生成器源码后，用 `Gen/PublishGen.bat` 重新发布 `publish/Gen.exe`。

**不要提交 Gen.exe**：它是发布产物，按需用 `Gen/PublishGen.bat` 重建。

## 生成代码

用仓库根目录的 `gen_use_publish.bat` 全量重生成（confcs、ZezeJava Builtin、ZezeJavaTest demo、ZezexJava、python）。生成产物（Gen 目录、Builtin 等）随源码一起提交；Gen.exe 不提交。
