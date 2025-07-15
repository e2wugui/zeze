# 登录排队
实现登录排队。gs统计在线用户数量，当都达到上限时，用户排队登录。

## 登录排队服务器
整个服务集群有一台专用的登录排队服务器，他监控gs的在线用户数量，他负责排队让用户进来登录。
这个取名：LoginQueueServer

## 系统各部分介绍
* Gs
		游戏服务器，通过LoginQueueAgent连接LoginQueueServer，并通过这个连接向LoginQueueServer报告自己的用户在线量。
* LoginQueueServer
		独立的Tcp服务，维护用户登录队列。
* LoginQueueAgent
		登录排队服务内部客户端接口，由Gs使用。
* LoginQueueClient
		登录排队服务客户端接口服务，由客户端使用。
* Linkd
		游戏连接服务，用户登录游戏需要通过这个服务连接，并由他转发请求给Gs。Linkd和LoginQueueServer之间不需要连接。

## 开启登录排队配置和步骤
1. Gs的zezexml增加到LoginQueueServer连接的LoginQueueAgent的配置。
2. Linkd 增加LoginQueueServer ras-public-key 配置。

## 登录排队服务各部分算法描述
1. Gs连接LoginQueueServer，当Gs上的用户上线或者下线时，向LoginQueueServer报告自己当前的在线量。
    报告在线数量也可以采用定时报告。这里选择哪种方式待定。
2. LoginQueueServer维护一个LoginQueueClient的所有用户连接的队列。当有Gs空闲时，向用户连接发放
    一个rsa算法签字的token。token包含具体gs的信息和过期时间信息。
3. LoginQueueClient连接LoginQueueServer，并得到当前队列的数量。当前队列数量定时更新。当到达队头时
    会收到token。
4. 客户端选择任意一台Linkd，发送从LoginQueueServer得到的token，然后进行正常的auth和login。
5. Linkd收到token，并进行签字校验（通过上面配置的rsa-public-key），把token信息记录下来。
    当客户端进行auth认证时，判断已经得到正确有效的token。当客户端开始login流程时，选择Gs进行登录。
    这里选择Gs是否利用token里面的信息待定。
