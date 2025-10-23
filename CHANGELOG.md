# Zeze 更新日志

所有对Zeze框架的显著变更都会记录在这个文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
并且这个项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [1.6.0] - 2025-10-23

### 新增
- **METRICS监控配置**
- **RSA+AES加密**: 基于非对称的加密方案，增强安全性
- **RawBean**: Timer对dynamic的未知类型Bean类型用RawBean接收,解决CustomData类型找不到导致事务失败的问题
- **Prometheus监控**: 增加procedure_redo、procedure_redo_and_release_lock、procedure_many_locks等指标
- **ReadOnly Bean接口**: 增加一些接口方法
- **noSchema表属性**: table支持定义noSchema="true"属性表示不参与表结构兼容性检查

### 变更
- **Netty HTTP**: 改进和规范Netty的初始化实现
- **Table缓存配置**: CacheCapacity=0表示尽量不留缓存，<0表示不限缓存量
- **Redirect改进**: Redirect Loop Back的事务名带上实际的方法名
- **Redirect改进**: 改进Redirect方法返回Long类型的处理,不再特殊处理回复类型中的resultCode字段,RPC回复错误码一律走RedirectFuture.onFail
- **Online**: VerifyLocal改进事务名和batch记录数
- **Component**: 降低容器节点的记录数上限,避免事务内锁过多
- **日志**: checkpoint.flush异常的时候，带上所有当前flush.tableName并继续抛出，记录到日志
- **日志**: GlobalCacheManagerAsyncServer对reduce超时输出警告日志
- **日志**: 改进AsyncSocket及子类的logger
- **网络握手**: 改进选择保底加密模式的策略
- **事务**: 事务actionName为空字符串时取action类名

### 修复
- **事务处理**: 修复reduce异常时SendResult发送问题
- **并发问题**: 修复TestTaskOneByOne cond.signal丢失风险
- **C# Services**: 修正GlobalKeyState的Encode/Decode

### 依赖更新
- 更新log4j2版本到2.25.2
- 更新rocksdb版本到10.4.2
- 更新netty版本到4.1.127.Final
- 更新fastjson2版本到2.0.59

### 文档
- 新增ReloadClass&RunClass.md文档
- 新增METRICS.md监控指标文档
- README补充相关的几个链接

## [1.5.10] - 2025-09-04

### 新增
- **Database关系表查询**: 增加关系表自定义查询方法 relationalSql(...)
- **Linkd错误码改进**: LinkdProvider.choiceProvider改成返回错误码
- **Online日志优化**: Online.sendDirect当目标没有登录时，日志级别调成debug

### 变更
- **Linkd选择方法**: choice方法返回整数错误码代替boolean，方便追踪失败原因
- **ThreadHelper日志**: await日志降成trace级

### 修复
- **Linkd.Auth**: 调整错误码返回方式
- **ZezeJavaTest警告**: 修正ZezeJavaTest里的一些警告

### 依赖更新
- 更新依赖库

## [1.5.9] - 2025-08-06

### 新增
- **LoginQueue系统**: 完整的登录队列解决方案，支持流量控制和负载均衡
- **TimeThrottle**: 线程安全的流量控制机制
- **LoginQueue服务**: 登录队列服务启动脚本和配置

### 变更
- **连接数配置**: maxConnections="2000000"
- **onAccept改进**: onAccept -> tryOnAccept，关闭的时候不再调用super
- **LoginToken过期时间**: LoginToken.expireTime改成常量，并改大

### 修复
- **LoginQueue算法**: 修复onAccept没有流量控制，不能提前分配的问题
- **LoginQueue测试**: LoginQueue全部联调成功，测试通过

## [1.5.8] - 2025-06-23

### 新增
- **RunClassServer**: 热更新和动态类加载支持
- **Http文件上传**: 支持multipart和content两种文件上传方式
- **HttpMultipartHandle**: Netty增加HttpMultipartHandle
- **UDP支持**: UDP相关的一些实现改进

### 变更
- **Redirect超时**: Redirect默认改成30秒超时(主要用于内部环境,容忍长时间处理)
- **Netty依赖**: gradle配置里的Netty依赖改成可选的
- **Task错误处理**: actionWhenError统一用ProtocolErrorHandle类型

### 修复
- **热更新**: 修正热更新机制
- **Token处理**: 修正Token一处bug
- **HttpFileUploadHandle**: 改进 HttpFileUploadHandle

## [1.5.7] - 2025-06-04

### 新增
- **WebSocket支持**: 原生WebSocket客户端和服务器端实现
- **WebSocketClient**: C# WebSocket客户端实现
- **AsyncSocket重构**: 改进网络层架构

### 变更
- **Service配置**: Service的AddFactoryHandle和findProtocolFactoryHandle允许重载实现
- **Config改进**: Config里的serviceManager字段增加修改方法
- **代码生成**: 生成代码中对未知Bean/Data类型的异常信息加上类型ID

### 修复
- **lua类型错误**: 修复lua list类型错误
- **AsyncSocket**: 重构AsyncSocket，改进网络处理

## [1.5.6] - 2025-05-20

### 新增
- **ToLuaService2**: lua service client重构，不依赖第三方库
- **新配置方式**: GenDir,SrcDir,DisableDeleteGen配置支持
- **CommonDir**: 专门用来存放公共的总是覆盖的Bean,Protocol等

### 变更
- **confcs兼容性**: confcs工程改成兼容.net 4.7.1, .net standard 2.0, C# 8.0
- **默认模块配置**: provider.module.binds.xml增加<defaultModule>配置
- **WebSocket帧大小**: WebSocket支持设置帧大小限制

### 修复
- **lua生成代码**: 修复lua生成代码注解错误
- **JsonReader**: 少量改进JsonReader

## [1.5.5] - 2025-04-10

### 新增
- **AccountOnline模块**: 账号在线状态管理
- **Json序列化record**: 内置Json支持序列化record
- **Netty WebSocket**: Netty补充ContinuationWebSocketFrame的处理

### 变更
- **Json改进**: 少量改进Json; 更新fastjson库
- **Rank并发级别**: Rank.getConcurrentLevel默认改成128
- **confcs构建目标**: 为编译通过, 构建目标从 .Net Framework 4.7.1/C# 7.3 改成 netstandard2.1/C# 8.0

### 修复
- **TimerRole**: 修正TimerRole在scheduleOnline时的本地在线判断
- **JsonReader**: JsonReader支持用换行符代替逗号的写法

## [1.5.4] - 2025-03-20

### 新增
- **MathEx.unsignedMod**: 更加严格的MathEx.unsignedMod实现
- **Rank功能**: Rank删除redirectAll，修改ZezexJava的Rank

### 变更
- **AutoKey**: TableX里的AutoKey改用Application里的
- **无符号求余**: 使用remainderUnsigned方法实现无符号求余
- **日志改进**: logger.log(level, ..., ex)根据ex是否null决定是否提供额外参数

### 移除
- **AutoKeyOld**: 删除AutoKeyOld的定义
- **ZezexJava Rank**: 删除ZezexJava的Rank相关定义和功能

## [1.5.3] - 2025-03-13

### 新增
- **Variable节点**: 增加"var"node-name for Variable
- **mergeRank功能**: 增加mergeRank，允许重复mergeRank

### 变更
- **RankTotal**: 增加 keyHint
- **服务停止**: service.stop(): config.stop()移到锁外

## [1.5.2] - 2025-03-03

### 新增
- **HttpResponseWithBodyStream**: 增加HttpResponseWithBodyStream
- **Prometheus监控**: 增加Prometheus metric支持
- **ZezeCounter**: 从PerfCounter抽象出ZezeCounter接口

### 变更
- **redirect支持Bean**: redirect支持Bean类型参数
- **Rank.Value**: Rank.Value改成dynamic，可以自定义
- **Service统计**: Service统计日志移到PerfCounter里

### 修复
- **HttpResponseWithBodyStream**: fix bug, use Unpooled.copiedBuffer
- **Service停止**: fix serviceStop

## 版本历史

[1.6.0]: https://gitee.com/e2wugui/zeze/compare/v1.5.10...v1.6.0
[1.5.10]: https://gitee.com/e2wugui/zeze/compare/v1.5.9...v1.5.10
[1.5.9]: https://gitee.com/e2wugui/zeze/compare/v1.5.8...v1.5.9
[1.5.8]: https://gitee.com/e2wugui/zeze/compare/v1.5.7...v1.5.8
[1.5.7]: https://gitee.com/e2wugui/zeze/compare/v1.5.6...v1.5.7
[1.5.6]: https://gitee.com/e2wugui/zeze/compare/v1.5.5...v1.5.6
[1.5.5]: https://gitee.com/e2wugui/zeze/compare/v1.5.4...v1.5.5
[1.5.4]: https://gitee.com/e2wugui/zeze/compare/v1.5.3...v1.5.4
[1.5.3]: https://gitee.com/e2wugui/zeze/compare/v1.5.2...v1.5.3
[1.5.2]: https://gitee.com/e2wugui/zeze/compare/v1.5.1...v1.5.2
