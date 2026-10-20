# 02 JDBC 三后端 saveDataWithSameVersion SP 返回码 4 丢映射

- **严重级**：P2（窗口窄=并发首插、重启自愈、fail-fast 不损数据；但破坏跨后端 CAS 重试契约）
- **类别**：Storage/数据库后端边界·跨后端契约不一致
- **子系统**：Transaction（Database 族）
- **位置**：`DatabaseMySql.java:622-628`（switch）、`DatabasePostgreSQL.java:485-487`、`DatabaseSqlServer.java:250-252`；SP 侧 `DatabaseMySql.java:311-322`、`DatabasePostgreSQL.java:272`、`DatabaseSqlServer.java:317`
- **状态**：✅ **已修复（2026-10-20）**——三后端各补 `case 4 -> KV.create(0L, false)`（对齐
  case 3 注释口径，注明对齐 Tikv/MongoDb 契约）；javac -Xlint:deprecation 三文件编译零告警。
  回归测试说明：ret=4 窗口在 SP 内部（SELECT 与 INSERT 之间被他进程抢先），Java 侧无法
  确定性触发（TestHistoryOwnerConcurrentClaim 的暂停注入在 Operates 层、覆盖 CAS 协议
  不覆盖 SP 内部路径），确定性用例需 CI 真 DB 桶 + 会话内 SAVEPOINT 注入，留待 DB 测试
  基建就绪后补。原初审/定谳记录如下。

## 缺陷

三个 JDBC 后端的存储过程都显式设计了 `ret=4` = 插入路径撞并发重复键（`INSERT IGNORE` 后
`ROW_COUNT()=0`），但 Java 侧 switch 只映射 `case 0/2/3`，4 落入
`default -> throw new IllegalStateException("Procedure SaveDataWithSameVersion Exec Error")`。
"零成功、非零失败、失败可重试"的接口契约（IGlobalAgent 同款口径）在 JDBC 三后端被破坏：
败者本应拿到 `KV.create(0L,false)` 后回读胜者继续，实际以误导性异常中断。

## 代码证据

```sql
-- DatabaseMySql.java:311-318（_ZezeSaveDataWithSameVersion_ 过程体）
INSERT IGNORE INTO _ZezeDataWithVersion_ VALUES(in_id,in_data,inout_version);
SELECT ROW_COUNT() INTO row_count;
IF row_count = 1 THEN SET ret_value=0; COMMIT; LEAVE return_label; END IF;
SET ret_value=4;   -- 并发首插认领失败
```

```java
// DatabaseMySql.java:622-628
return switch (ps.getInt(4)) {
    case 0 -> KV.create(ps.getLong(3), true);
    case 2 -> KV.create(0L, false);
    case 3 -> KV.create(0L, false);
    default -> throw new IllegalStateException("Procedure SaveDataWithSameVersion Exec Error");
};  // 4 未映射
```

对照组：Tikv（`DatabaseTikv.java:145/157/177/200`）、MongoDb、DynamoDb 同场景均返回
`KV.create(version, false)` 走重试。

## 触发场景

同一 key 首行尚不存在、两进程并发认领：
- `History/OwnerCheck.java:53` 的 tHistory 归属认领（`saveDataWithSameVersion(key, name, 0)`，
  注释明说设计语义是"失败时回读真正胜者"，同库多 app 同名并发首启是合法部署）；
- `Application.java:639` publishSchemas 首次发布（CAS 循环以 `false` 为重试信号，慢 DDL 下
  atomicOpenDatabase 的 600s 租约可开窗）。

败者直接异常中断启动；重启后行已存在、走 UPDATE 路径自愈。

## 修法方向

三后端各补一行 `case 4 -> KV.create(0L, false);`（对齐 case 3 的注释口径），回归测试覆盖
并发首插双进程场景。
