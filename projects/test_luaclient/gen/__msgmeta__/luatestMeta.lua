local message = require 'msg.message'
local luatest = { }

luatest.Bean1 = {
    name = 'luatest.Bean1',
    type_id = '-5664905809240227220',
    metatable = message.luatest.Bean1,
    variables = {
        v1 = { id = 1, type = '{name: "int", is_bean: false, is_collection: false}', default = 1, },
        v2 = { id = 2, type = '{name: "map", is_bean: false, is_collection: true, key_type: {name: "int", is_bean: false, is_collection: false}, value_type: {name: "int", is_bean: false, is_collection: false}}', key = '{name: "int", is_bean: false, is_collection: false}', value = '{name: "int", is_bean: false, is_collection: false}', },
    },
}

function luatest.__reg__(meta)
    meta.beans['luatest.Bean1'] = luatest.Bean1
end

return luatest
