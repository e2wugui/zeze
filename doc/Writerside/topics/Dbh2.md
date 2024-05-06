# Dbh2

## 什么是Dbh2
Dbh2是一个专用于Zeze的基于RocksDB和Raft的分布式KV数据库。它不支持冲突记录的并发写，
这个由Zeze提供保护。它支持并发读取，但是读取记录进一步互斥逻辑也有Zeze提供保护。
所以，它的目的是提供一个低成本的高可靠的高分布的存储系统。

## 主要特性
* 一个Dbh2集群支持多个Database
* 每个Database包含多张表
* 每张表是一个KV表
* 每个表按Key的顺序自动分段到多个桶内
* 每个桶由raft节点组成（高可靠来源）
* 分桶管理自动
* 还具有整个桶自动迁移功能
* 具有动态扩容能力

## 主要部分
* Master 知道所有数据库，所有表，所有桶的分布情况。提供表创建，桶信息查询。
* Dbh2Manager，桶运行容器，包含多个桶。主要负载实现点。
* Dbh2Agent，嵌入到dbh2的客户端内执行。
* CommitServer，Dbh2Agent的提交功能移到这个服务进程，仅用来提交事务，可选的。

## 接口
* Zeze.Dbh2.Database 实现了Zeze.Transaction.Database接口。
* zeze.xml 数据库配置
```
<DatabaseConf Name="" DatabaseType="Dbh2"
    DatabaseUrl="dbh2://127.0.0.1:10999/dbh2_database"/>

127.0.0.1:10999是Master的地址端口；
dbh2_database是数据库名；
```
## 启动脚本
```
nohup java -Dlogname=master   -Xmx4g -cp .:lib/* Zeze.Dbh2.Master.Main zeze.xml&
sleep 2
nohup java -Dlogname=manager0 -Xmx4g -cp .:lib/* Zeze.Dbh2.Dbh2Manager manager0 zeze0.xml &
nohup java -Dlogname=manager1 -Xmx4g -cp .:lib/* Zeze.Dbh2.Dbh2Manager manager1 zeze1.xml &
nohup java -Dlogname=manager2 -Xmx4g -cp .:lib/* Zeze.Dbh2.Dbh2Manager manager2 zeze2.xml &
# 需要启动3个manager组成raft集群。
```

## 配置
1. Master配置
```
<?xml version="1.0" encoding="utf-8"?>

<zeze
	GlobalCacheManagerHostNameOrAddress="" GlobalCacheManagerPort="5002"
	CheckpointPeriod="60000" ServerId="0" CheckpointMode="Table" CheckpointFlushMode="SingleThread" CheckpointModeTableFlushConcurrent="4"
	ServiceManager=""
	>
	<DatabaseConf Name="" DatabaseType="Memory" DatabaseUrl=""/>
	<ServiceConf Name="Zeze.Services.ServiceManager.Agent">
		<Connector HostNameOrAddress="127.0.0.1" Port="5001"/>
	</ServiceConf>
	<ServiceConf Name="Zeze.Dbh2.Master">
		<Acceptor Ip="127.0.0.1" Port="10999"/>
	</ServiceConf>
	<ServiceConf Name="Zeze.Dbh2.Master.Agent">
		<Connector HostNameOrAddress="127.0.0.1" Port="10999"/>
	</ServiceConf>
	<ServiceConf Name="Zeze.Raft.ProxyServer">
		<Acceptor Ip="127.0.0.1" Port="0"/>
	</ServiceConf>
	<ServiceConf Name="Zeze.Dbh2.Commit">
		<Acceptor Ip="127.0.0.1" Port="7788"/>
	</ServiceConf>
</zeze>
```
2. Manager配置（一个）
```
<?xml version="1.0" encoding="utf-8"?>

<zeze
	GlobalCacheManagerHostNameOrAddress="" GlobalCacheManagerPort="5002"
	CheckpointPeriod="60000" ServerId="0" CheckpointMode="Table" CheckpointFlushMode="SingleThread" CheckpointModeTableFlushConcurrent="4"
	ServiceManager="disable"
	>
	<DatabaseConf Name="" DatabaseType="Memory" DatabaseUrl=""/>
	<ServiceConf Name="Zeze.Dbh2.Master">
		<Acceptor Ip="127.0.0.1" Port="10999"/>
	</ServiceConf>
	<ServiceConf Name="Zeze.Dbh2.Master.Agent">
		<Connector HostNameOrAddress="127.0.0.1" Port="10999"/>
	</ServiceConf>
	<ServiceConf Name="Zeze.Raft.ProxyServer">
		<Acceptor Ip="127.0.0.1" Port="7780"/>
	</ServiceConf>
	<ServiceConf Name="Zeze.Dbh2.Commit">
		<Acceptor Ip="127.0.0.1" Port="7788"/>
	</ServiceConf>
</zeze>
```