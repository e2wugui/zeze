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
---@field result table
---@field resultCode long
---@field responseHandle function
---@field future future
---@field isRequest boolean


local Future = require('common.future')
local message_init = {}

function message_init.send(message)
    message_init.service.SendProtocol(message)
end

function message_init.send_rpc(message, responseHandle)
    local future = Future:new()
    message.future = future
    message.responseHandle = responseHandle
    message.sessionId = message_init.service.AddRpcContext(message)
    message_init.service.SendProtocol(message)
    return future
end

function message_init.new(metatable)
    local t = {}
    setmetatable(t, metatable)
    return t
end

function message_init.set_default_service(service)
    message_init.service = service
end

local EmptyBean = {
    __type_name__ = 'EmptyBean',
    __type_id__ = '0',
    new = message_init.new,
}

EmptyBean.__index = EmptyBean
message_init.EmptyBean = EmptyBean

function message_init.init(message)
    message_init.{{solution.name}} = message.{{solution.name}}
end

return message_init