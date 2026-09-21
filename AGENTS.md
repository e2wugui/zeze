# 生成器（Gen）工作流

## 构建 Gen.exe

修改 `Gen/` 下的生成器源码后，用仓库根目录的 `PublishGen.bat` 重新发布 `publish/Gen.exe`。

**不要提交 Gen.exe**：它是发布产物，按需用 PublishGen.bat 重建。

## 生成代码

用仓库根目录的 `gen_use_publish.bat` 全量重生成（confcs、ZezeJava Builtin、ZezeJavaTest demo、ZezexJava、python）。生成产物（Gen 目录、Builtin 等）随源码一起提交；Gen.exe 不提交。
