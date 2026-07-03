local message = require 'msg.message'
local luatest = { }

luatest.Bean1 = {
    name = 'luatest.Bean1',
    type_id = '-5664905809240227220',
    metatable = message.luatest.Bean1,
    variables = {
        v1 = { id = 1, type = 'int', default = 1, },
        v2 = { id = 2, type = 'map', key = 'int', value = 'int', },
    },
}

function luatest.__reg__(meta)
    meta.beans['luatest.Bean1'] = luatest.Bean1
end

return luatest
