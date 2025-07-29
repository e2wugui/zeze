# 登录排队
实现登录排队。gs统计负载，当都达到上限时，用户排队登录。

## 登录排队服务器
整个服务集群有一台专用的登录排队服务器，他监控gs的负载，负责排队让用户进来登录。
这个取名：LoginQueueServer

## 系统各部分介绍
* Gs
游戏服务器，通过LoginQueueAgent连接LoginQueueServer，并通过这个连接向LoginQueueServer报告自己的负载。
* LoginQueueServer
独立的Tcp服务，维护用户登录队列。
* LoginQueueAgent
登录排队服务内部客户端接口，由Gs使用。
* LoginQueueClient
登录排队服务客户端接口服务，由客户端使用。
* Linkd
游戏连接服务，用户登录游戏需要通过这个服务连接，并由他转发请求给Gs。Linkd也链接LoginQueueServer并报告自己的负载。

## 开启登录排队配置和步骤
1. Gs的zeze.xml增加到LoginQueueServer连接的LoginQueueAgent的配置。
2. Linkd的zeze.xml增加到LoginQueueServer连接的LoginQueueAgent的配置。

## 登录排队服务各部分算法描述
1. Gs连接LoginQueueServer，ProviderLoadBase.java 实现报告自己的负载。
2. LoginQueueServer维护一个LoginQueueClient的所有用户连接的队列。当有Gs空闲时，向用户连接发放
    一个算法加密的token。token包含具体gs的信息和过期时间信息。
3. LoginQueueClient连接LoginQueueServer，并得到当前队列的数量。当前队列数量定时更新。当到达队头时
    会收到token。
4. 客户端从token中得到需要连接的Linkd，连接linkd发送auth，需要带上token，并且服务器的代码需要
    手动调用一下verifyAndParseLoginToken(token)，这个方法需要第一个调用，由内部提供。
5. Linkd收到token，并进行token校验，把token信息记录下来。
    当客户端进行auth认证时，判断已经得到正确有效的token。
    当客户端开始login流程时，选择Gs进行登录。
    需要新增根据serverId选择Gs的算法。

## 启动和配置LoginQueue
* 启动LoginQueue服务

* 配置两个地方：linkd.xml和gs.xml
增加LoginQueueAgentService配置。
	<ServiceConf Name="LoginQueueAgentService">
		<Connector HostNameOrAddress="127.0.0.1" Port="9999"/> LoginQueue服务器内部地址
	</ServiceConf>

* 客户端（lua）
'''
ConnectLoginQueue(outIp, outPort)
实现network.lua里面的三个回调。
local function OnQueueFull()
	-- 报告错误
end

local function OnQueuePosition(queuePosition)
	-- 报告队列位置
end

local function OnQueuePosition(LinkIp, LinkPort, Token)
	-- 排队成功
	loginToken = Token;  -- 保存token
	Connect(LinkIp, LinkPort)
end

function network.on_connected()
	local p = Auth::new()
	p.LoginToken = loginToken;
	... 其他参数
	p.send();
end

'''
