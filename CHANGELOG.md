# Zeze 更新日志

所有对Zeze框架的显著变更都会记录在这个文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
并且这个项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [1.6.0] - 2025-10-23

### 新增
- **LoginQueue系统**: 完整的登录队列解决方案，支持流量控制和负载均衡
- **WebSocket支持**: 原生WebSocket客户端和服务器端实现
- **GTable数据结构**: 新的通用表数据结构，支持JSON序列化
- **MQ消息队列**: 消息队列系统，支持文件索引存储
- **RSA+AES加密**: 新的混合加密方案，增强安全性
- **HaProxyHeader支持**: HAProxy协议头解析支持
- **Http文件上传**: 支持multipart和content两种文件上传方式
- **Timer扩展**: TimerRole和TimerAccount支持transmit和Class参数
- **RawBean**: 不可变Bean类型支持
- **AccountOnline模块**: 账号在线状态管理
- **RunClassServer**: 热更新和动态类加载支持
- **Prometheus监控**: 增加procedure_redo、procedure_redo_and_release_lock、procedure_many_locks等指标
- **ReadOnly Bean接口**: 为所有Bean类型生成只读接口，提升类型安全
- **noSchema表属性**: 支持定义noSchema="true"属性表示不参与表结构兼容性检查

### 变更
- **Table缓存配置**: CacheCapacity=0表示尽量不留缓存，<0表示不限缓存量
- **Redirect改进**: 支持Bean类型参数，Loop Back事务名带上实际方法名
- **Timer优化**: 取消不存在的timerId返回成功，补充异常日志
- **AsyncSocket重构**: 改进网络层架构
- **Json序列化**: 支持record类型序列化
- **BeanFactory优化**: 减少锁竞争，提升性能
- **Service配置**: ServiceManager支持Announce方式的离线通知
- **Netty依赖**: 改为可选依赖
- **内存表优化**: 允许有dirty状态，避免软引用失效
- **并发级别调整**: 默认值优化，减少锁竞争
- **代码生成改进**: 按xml定义顺序生成java协议方法，提升代码可读性
- **动态容器类型优化**: 为dynamic容器类型生成meta缓存，提升构造性能

### 修复
- **并发问题**: 修复TestTaskOneByOne cond.signal丢失风险
- **内存泄漏**: 修复Agent reload死循环问题
- **事务处理**: 修复reduce异常时SendResult发送问题
- **网络连接**: 修复WebSocket连接和关闭问题
- **数据库**: 修复Dbh2配置毁坏后重新load的bug
- **类型错误**: 修复lua list类型错误
- **数组越界**: 修复长度为0时的数组越界问题
- **文件上传**: 修复HttpMultipartHandle处理逻辑
- **热更新**: 修正热更新机制
- **Timer处理**: 修复Timer CustomData失败问题，需要忽略createBean失败
- **检查点处理**: 修复checkpoint.flush异常时tableName信息显示问题

### 移除
- **过时方法**: 移除TimerRole中的Deprecated方法
- **不必要依赖**: 移除guava依赖，改为可选
- **旧版功能**: 移除AutoKeyOld定义
- **冗余代码**: 清理不必要代码和配置

### 依赖更新
- 更新fastjson2库
- 更新rocksdb版本
- 更新mysql连接器版本

### 文档
- 新增LoginQueue.md、ReloadClass&RunClass.md、Log.md等文档
- 更新README和配置说明
- 改进代码注释和警告信息
- 新增METRICS.md监控指标文档
- 更新CUE配置文档

## 版本历史

[1.6.0]: https://gitee.com/e2wugui/zeze/compare/v1.5.10...v1.6.0