
local message_core = require 'common.message_core'
local message_init = require 'msg.message_init'

local Module11 = {}

---@class msg.luatest.Module1.Module11.Base : msg.Bean
---@field baseInt integer
---@field new fun(t: table):msg.luatest.Module1.Module11.Base
Module11.Base = {
    __type_name__ = 'luatest.Module1.Module11.Base',
    __type_id__ = '9101036892867031875',
    new = message_core.bean_new,
    baseInt = 0,
}
Module11.Base.__index = Module11.Base

---@class msg.luatest.Module1.Module11.Dynamic : msg.Bean
---@field dyn msg.luatest.Module1.Module11.Base
---@field new fun(t: table):msg.luatest.Module1.Module11.Dynamic
Module11.Dynamic = {
    __type_name__ = 'luatest.Module1.Module11.Dynamic',
    __type_id__ = '2144297238039226637',
    new = message_core.bean_new,
    dyn = message_init._default_empty_bean,
}
Module11.Dynamic.__index = Module11.Dynamic


function Module11.__reg__()
end

return Module11