# Services

Zeze内建服务。都是独立进程。

## ServiceManager

* 管理服务器信息的注册和订阅。
* 当新增服务器或者服务器关闭时，会给订阅者发送新的服务器列表。
* 服务用名字区分，每个服务可能有多个提供者，也可能有多个订阅者。
* 主要数据结构：Map&lt;ServiceName, List&lt;ServiceInfo&gt;&gt;
* 服务器列表List按 ServiceInfo.ServiceIdentity排序。

| 名词              | 说明                                                       |
|-----------------|----------------------------------------------------------|
| ServiceName     | 服务名字，用来表示一个服务。                                           |
| ServiceInfo     | 服务包含的信息，包含ServiceName, ServiceIdentity,Ip,Port,ExtraInfo |
| ServiceIdentity | 服务器编号，用来区分不同的服务器，具体含义由应用自己决定。                            |
| Ip，Port         | 服务器地址和端口，当服务作为Acceptor时一般需要提供。可选。                        |
| ExtraInfo       | 应用自定义数据，使用Binary存储。可选。                                   |

### 订阅模式
* Simple

某个ServiceName下的服务器信息列表发生改变的时候，给所有订阅者发送通知。
当第一次订阅的时候会得到当前已经存在的服务器列表信息。
通知和第一次获得订阅列表有多线程保护，保证相关操作系列化，不会出现信息丢失。

* ReadyCommit
订阅者收到的服务器列表的时间有先后，还需要进行准备，如建立连接并登录等。这个过程
不会很快，造成每个订阅者真正可使用的服务器列表不会一致。当订阅者使用hash方式选
择服务器时，这种不一致会导致选中的服务器不一样。为了降低这种不一致，引入这种订阅
模式。步骤如下：
```
	某个ServiceName下的服务器信息列表发生改变的时候，给所有订阅者发送通知
	开始收集订阅者的Ready回复。
	所有订阅者都Ready了以后，再通知一次准备好的服务器列表。
```
* ServiceManager.Agent

ServiceManager客户端，主要接口如下：

| 接口                 | 说明                                   |
|--------------------|--------------------------------------|
| RegisterService    | 注册服务信息                               |
| UnRegisterService  | 撤销注册服务信息                             |
| UpdateService      | 更新服务信息                               |
| SubscribeService   | 订阅                                   |
| UnSubscribeService | 撤销订阅                                 |
| SetLoad            | 设置负载信息，信息键值为”Ip:Port”。订阅服务信息时，如果信    |
|                    | 息中包含Ip,port，那么就自动订阅该服务器的负载。任何服务器     |
|                    | 都可以发送SetLoad。负载会被广播给关注这个ip:port的订阅者。 |
|                    | 目前没有撤销订阅的接口，只有当ServiceManager广播发现网   |
|                    |  络断开，才自动撤销负载订阅。                      |

### Arch是怎么使用ServiceManager的
* Server内的每个模块是一个服务；Linkd以模块为单位派发客户端请求。
* Server的模块服务名字编码方式是：ServerServiceNamePrefix#ModuleId。
* ServerServiceNamePrefix是应用名字，作为这个应用的标识。构造Arch.ProviderApp时由用
户传入，当多个ProviderApp共享一个ServiceManager时，每个ProviderApp的
* ServerServiceNamePrefix必须能区分开，否则服务订阅会错乱。
* Server服务的ServiceIdentity编码：String.valueOf(ServerId)。
* Linkd服务的ServiceIdentity编码："@" + ProviderIp + ":" + ProviderPort。
这里ProviderIp,ProviderPort是Linkd的Tcp.Acceptor，Server会发现Linkd时主动连接。
“@“开头的ServiceIdentity目前由Zeze.Arch保留，其他第三方的ServiceManager使用者不
能使用。

## GlobalCacheManager
一致性缓存锁管理服务器。

## GlobalCacheManagerWithRaft
Raft版本的一致性缓存锁管理服务器。

