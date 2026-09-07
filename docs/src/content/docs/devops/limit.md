---
title: "Limit"
sidebar:
  order: 4
---


## Database
* Zeze.Transaction.Database.eMaxKeyLength

Table.Key的最大长度。默认为900，取全后端最严格的真实限制：SqlServer 聚集索引
键上限 900 字节（行内 PRIMARY KEY 默认聚集）。MySQL 8 为 3072、PostgreSQL 约 2704、
MongoDB 为 1024，均宽于此值。这个预算在所有 KV 后端统一入口检查执法
（replace/remove/find 及带游标的 walk）。修改这个参数需要重新编译Zeze。
已经创建的表的Key类型需要自己手动修改(Alter)。
