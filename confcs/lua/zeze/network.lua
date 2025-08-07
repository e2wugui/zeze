local message_init = require('zeze.message_core')
local timer = require('zeze.Timer')

local network = {}

local lua_client = zeze.LuaClient
network.RpcContexts = {}
network._current_session_id = 0

local connectCallback = nil

function network.Init()
    network.LoadProtocolMeta()
    message_init.set_default_service(network)
    lua_client.SetOnSocketConnected(network.on_connected)
    lua_client.SetOnReceiveProtocol(network.on_receiveProtocol)
    lua_client.SetOnSocketClosed(network.on_net_close)
    lua_client.SetKeepAliveTimeout(network.on_keepalive_timeout)

    network._isConnected = false
    network._isConnecting = false
end

------------------------------------------network service 接口---------------------------------------------
function network.LoadProtocolMeta()
    local meta_data = require('msg.ZezeMeta')
    lua_client.LoadMeta(meta_data)
end

function network.SendProtocol(protocol)
    lua_client.SendProtocol(protocol)
end

---@param rpc msg.RPC
function network.AddRpcContext(rpc)
    local current_session_id = network._current_session_id + 1
    network.RpcContexts[current_session_id] = rpc
    network._current_session_id = current_session_id
    return current_session_id
end

function network.RemoveRpcContext(session_id)
    local rpc = network.RpcContexts[session_id]
    network.RpcContexts[session_id] = nil
    return rpc
end

local LoginQueueToken;

function network.Connect(ipOrHost, port, callback)
	if IsWeChatMiniGame then
		local wsUrl = string.format("ws://%s:%s/websocket", ipOrHost, port)
		lua_client.ConnectWebsocket(wsUrl, false)
	else
		lua_client.Connect(ipOrHost, port, false)
	end
	connectCallback  = callback
end

function network.ConnectWebsocket(wsUrl)
    lua_client.ConnectWebsocket(wsUrl, false)
end

function network.Close()
    lua_client.Close()
end

function network.Update()
    lua_client.Update()
end

local msg = require("msg.message")
------------------------------------------lua_client 回调接口---------------------------------------------
function network.on_connected()
	--local hasLocalAccount = GameSave.String.LocalAccount and GameSave.String.LocalAccount ~= ''
	----优先debug面板指定的账号，没有再用服务器发来的token
	--local account = hasLocalAccount and GameSave.String.LocalAccount or PlayerDataManager.basic:GetUserId()
    --local token = ""
    --local appVersion = ""
    --local linkd = require("module.syx.Linkd.ModuleLinkd")
    --linkd.auth(account, token, appVersion, LoginQueueToken)
	network._isConnected = true
	if connectCallback then
		connectCallback(true)
		connectCallback = nil
	end
	ServerConnector:OnZezeConnected()
end

function network.on_keepalive_timeout()
    print("网络保活超时，需要的话，自己手动关闭连接");
end

function network.on_net_close()
	network._isConnected = false
	ServerConnector:OnZezeDisConnected()
    print("网络关闭");
end

function network.on_receiveProtocol(protocol)
    if (protocol.isRpc and not protocol.isRequest) then
        network.on_receiveRPC(protocol)
        return
    end
    --logger.Protocol("=== recv {0} {1}", protocol.__type_name__, protocol)

    if protocol.Handle then
        protocol:Handle()
    end
end

---@param rpc msg.RPC
function network.on_receiveRPC(rpc)
    --logger.Protocol("=== recv {0} {1}", rpc.__type_name__, rpc)
    local context = network.RpcContexts[rpc.sessionId]
    if not context then
        --logger.Error('rpc response: lost context, maybe timeout. {0}', rpc.__type_name__)
        return
    end
    if context.timer_id then
        timer.Unschedule(context.timer_id)
    end
    network.RpcContexts[rpc.sessionId] = nil
    context.isRequest = false
    context.result = rpc.result
    context.sender = rpc.sender
    context.resultCode = rpc.resultCode
    context.future:set_result(true)
    if context.responseHandle then
        context.responseHandle(context)
    end
end

local function OnQueueFull()
end

local function OnQueuePosition(queuePosition)
end

local function OnLoginToken(LinkIp, LinkPort, Token)
	LoginQueueToken = Token
	Connect(LinkIp, LinkPort)
end

-- 约定注册给 c# 使用
_G.zezeActions = {
    OnQueueFull = OnQueueFull
    OnQueuePosition = OnQueuePosition
    OnLoginToken = OnLoginToken
}

return network
