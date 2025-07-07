---@alias integer number
---@alias long integer


---@class msg.Bean
---@field __type_name__ string
---@field __type_id__ long


---@class msg.Protocol
---@field __type_name__ string
---@field __type_id__ long
---@field protocolId integer
---@field moduleId integer
---@field resultCode integer
---@field argument table

---@class msg.RPC : msg.Protocol
---@field sessionId long
---@field result msg.Bean
---@field resultCode long
---@field responseHandle function
---@field future future
---@field isRequest boolean
---@field isTimeout boolean


local Future = require('zeze.future')
local timer = require('zeze.timer')
local message_core = {}
function message_core.send(message)
    --local module = require("module.module")
    --local net = require "module.net"
    --if module.login.isLogin or net.notLoginCanSendProtocol[message.__type_name__] then
        message_core.service.SendProtocol(message)
    --else
    --    logger.Warn("not login attemp send message. message typename{0}", message.__type_name__)
    --end
end

local isEditor = UnityEngine.Application.isEditor
if not isEditor then
    local function rpc_time_out_call_back(sessionId, service)        
        local message = service.RemoveRpcContext(sessionId)
        if not message then
            return
        end
        --message.remainSendTimes = message.remainSendTimes - 1
        --if message.remainSendTimes > 0 then
        --    message.sessionId = message_core.service.AddRpcContext(message)
        --    message_core.service.SendProtocol(message)
        --    if message.timer_id then
        --        timer.unschedule(message.timer_id)
        --    end
        --    message.timer_id = timer.schedule(message.timeOut, rpc_time_out_call_back, message.sessionId, message_core.service)
        --else
            message.timer_id = nil
            message.future:set_result(false)
            message.isTimeout = true;
            message.resultCode = -10; --- Timeout
            if message.responseHandle then
                message.responseHandle(message)
            end
        --end
    end

    function message_core.send_rpc(message, responseHandle)
        --local module = require("module.module")
        --local net = require "module.network.net"
        --if not module.login.isLogin and not net.notLoginCanSendProtocol[message.__type_name__] then
        --    logger.Warn("not login attemp send message. message typename{0}", message.__type_name__)
        --    return
        --end
        --if (responseHandle == nil) then
        --    logger.Warn("rpc must has call back !!! {0}", message.__type_name__)
        --end
        local future = Future:new()
        message.responseHandle = responseHandle
        message.future = future
        message.isRequest = true
        message.timeOut = message.timeOut and message.timeOut / 1000 or 120
        message.sessionId = message_core.service.AddRpcContext(message)
        message_core.service.SendProtocol(message)
        --message.remainSendTimes = net.IsInResendRpcWhenTimeoutWhiteList(message) and 3 or 1
        message.timer_id = timer.Schedule(message.timeOut, rpc_time_out_call_back, message.sessionId, message_core.service)
        return future
    end
else
    local function rpc_time_out_call_back(sessionId, service)
        local message = service.RemoveRpcContext(sessionId)
        if not message then
            return
        end
        message.timer_id = nil
        message.future:set_result(false)
        message.isTimeout = true;
        message.resultCode = -10; --- Timeout
        --message.responseHandle(message)
    end

    function message_core.send_rpc(message, responseHandle)
        --local module = require("module.module")
        --local net = require "module.net"
        --if not module.login.isLogin and not net.notLoginCanSendProtocol[message.__type_name__] then
        --    logger.Warn("not login attemp send message. message typename{0}", message.__type_name__)
        --    return
        --end
        local future = Future:new()
        message.responseHandle = responseHandle
        message.future = future
        message.isRequest = true
        local time_out = message.timeOut and message.timeOut / 1000 or 3600
        message.sessionId = message_core.service.AddRpcContext(message)
        message_core.service.SendProtocol(message)
        message.timer_id = timer.Schedule(time_out, rpc_time_out_call_back, message.sessionId, message_core.service)
        return future
    end
end

function message_core.build_index(type_class)
    return function(t, k)
        local reg_beans = type_class.__reg_beans
        local c = reg_beans and reg_beans[k]
        if c then
            local instance = c:new()
            rawset(t, k, instance)
            return instance
        else
            local v = type_class[k]
            if type(v) == 'table' and not v.__type_id__ then
                v = {}
                rawset(t, k, v)
                return v
            else
                return v
            end
        end
    end
end

function message_core.build_newindex(type)
    return function(t, k, v)
        local reg_beans = type.__reg_beans
        local c = reg_beans and reg_beans[k]
        if c then
            rawset(t, k, c:new(v))
        else
            rawset(t, k, v)
        end
    end
end

function message_core.bean_new(metatable, o)
    local t = o or {}
    setmetatable(t, metatable)
    return t
end

function message_core.build_bean_new(table_create_template)
    --- 这么写是为了 pre alloc table
    return function(metatable, o)
        local t = o or { table.unpack(table_create_template) }
        setmetatable(t, metatable)
        return t
    end

end

function message_core.protocol_new(metatable, argument)
    --- 这么写是为了 pre alloc table
    local t = { argument = nil }
    setmetatable(t, metatable)
    if argument then
        t.argument = argument
    end
    return t
end

function message_core.rpc_new(metatable, argument)
    --- 这么写是为了 pre alloc table
    local t = { argument = nil, result = nil }
    setmetatable(t, metatable)
    if argument then
        t.argument = argument
    end
    return t
end

function message_core.set_default_service(service)
    message_core.service = service
end

return message_core
