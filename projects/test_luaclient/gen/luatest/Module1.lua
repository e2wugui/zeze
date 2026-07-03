
local message_core = require 'common.message_core'
local message_init = require 'msg.message_init'

local Module1 = {}

---@class msg.luatest.Module1.AutoKey : msg.Bean
---@field name string
---@field localId long
---@field new fun(t: table):msg.luatest.Module1.AutoKey
Module1.AutoKey = {
    __type_name__ = 'luatest.Module1.AutoKey',
    __type_id__ = '-2380498687461193834',
    new = message_core.bean_new,
    name = '',
    localId = 0,
}
Module1.AutoKey.__index = Module1.AutoKey

---@class msg.luatest.Module1.Key : msg.Bean
---@field Enum1 integer
---@field s integer
---@field new fun(t: table):msg.luatest.Module1.Key
Module1.Key = {
    __type_name__ = 'luatest.Module1.Key',
    __type_id__ = '3613565487130963831',
    new = message_core.bean_new,
    Enum1 = 4,
    s = 1,
}
Module1.Key.__index = Module1.Key

---@class msg.luatest.Module1.Simple : msg.Bean
---@field int1 integer
---@field long2 long
---@field string3 string
---@field new fun(t: table):msg.luatest.Module1.Simple
Module1.Simple = {
    __type_name__ = 'luatest.Module1.Simple',
    __type_id__ = '1',
    new = message_core.bean_new,
    int1 = 0,
    long2 = 0,
    string3 = '',
}
Module1.Simple.__index = Module1.Simple

---@class msg.luatest.Module1.Value : msg.Bean
---@field Enum1 integer
---@field int1 integer
---@field long2 long
---@field string3 string
---@field bool4 boolean
---@field short5 integer
---@field float6 number
---@field double7 number
---@field list9 table<msg.luatest.Bean1>
---@field set10 table<integer>
---@field map11 table<long, msg.luatest.Module1.Simple>
---@field dynamic14 msg.luatest.Bean1|msg.luatest.Module1.Simple
---@field vector2 msg.vector2
---@field vector3 msg.vector3
---@field new fun(t: table):msg.luatest.Module1.Value
Module1.Value = {
    __type_name__ = 'luatest.Module1.Value',
    __type_id__ = '-6473288029264554187',
    new = message_core.bean_new,
    Enum1 = 4,
    int1 = 0,
    long2 = 0,
    string3 = '',
    bool4 = false,
    short5 = 0,
    float6 = 0.0,
    double7 = 0.0,
    list9 = {},
    set10 = {},
    map11 = {},
    dynamic14 = message_init._default_empty_bean,
}
Module1.Value.__index = message_core.build_index(Module1.Value)
Module1.Value.__newindex = message_core.build_newindex(Module1.Value)

---@class msg.luatest.Module1.Protocol1 : msg.Protocol
---@field argument msg.luatest.Module1.Value
---@field new fun(self: msg.luatest.Module1.Protocol1, argument : table | nil):msg.luatest.Module1.Protocol1
---@field send fun(self: msg.luatest.Module1.Protocol1)
Module1.Protocol1 = {
    __type_name__ = 'luatest.Module1.Protocol1',
    __type_id__ = '7571189254',
    protocolId = -1018745338,
    moduleId = 1,
    resultCode = 0,
    send = message_core.send,
    new = message_core.protocol_new,
}

Module1.Protocol1.__index = message_core.build_index(Module1.Protocol1)
Module1.Protocol1.__newindex = message_core.build_newindex(Module1.Protocol1)
---@class msg.luatest.Module1.Rpc1 : msg.Protocol
---@field argument msg.luatest.Module1.Value
---@field result msg.luatest.Module1.Value
---@field new fun(self: msg.luatest.Module1.Rpc1, argument : table | nil):msg.luatest.Module1.Rpc1
---@field send fun(self: msg.luatest.Module1.Rpc1, handler: fun(response: msg.luatest.Module1.Value))
Module1.Rpc1 = {
    __type_name__ = 'luatest.Module1.Rpc1',
    __type_id__ = '6744810510',
    protocolId = -1845124082,
    moduleId = 1,
    resultCode = 0,
    send = message_core.send_rpc,
    resultCode = 0,
    isRequest = false,
    new = message_core.rpc_new,
}

Module1.Rpc1.__index = message_core.build_index(Module1.Rpc1)
Module1.Rpc1.__newindex = message_core.build_newindex(Module1.Rpc1)
Module1.eTestEnum = 1

function Module1.__reg__()
    Module1.Protocol1.__reg_beans = {}
    Module1.Protocol1.__reg_beans.argument = message_init.luatest.Module1.Value
    Module1.Rpc1.__reg_beans = {}
    Module1.Rpc1.__reg_beans.argument = message_init.luatest.Module1.Value
    Module1.Rpc1.__reg_beans.result = message_init.luatest.Module1.Value
end

return Module1