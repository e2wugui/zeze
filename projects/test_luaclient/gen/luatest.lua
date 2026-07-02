
local message_core = require 'common.message_core'
local message_init = require 'msg.message_init'

local luatest = {}

---@class msg.luatest.Bean1 : msg.Bean
---@field Enum1 integer
---@field v1 integer
---@field v2 table<integer, integer>
---@field new fun(t: table):msg.luatest.Bean1
luatest.Bean1 = {
    __type_name__ = 'luatest.Bean1',
    __type_id__ = '-5664905809240227220',
    new = message_core.bean_new,
    Enum1 = 4,
    v1 = 1,
    v2 = {},
}
luatest.Bean1.__index = message_core.build_index(luatest.Bean1)
luatest.Bean1.__newindex = message_core.build_newindex(luatest.Bean1)


function luatest.__reg__()
end

return luatest