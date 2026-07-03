local message = require 'msg.message'
local Module11 = { }

Module11.Base = {
    name = 'luatest.Module1.Module11.Base',
    type_id = '9101036892867031875',
    metatable = message.luatest.Module1.Module11.Base,
    variables = {
        baseInt = { id = 1, type = 'int', },
    },
}

Module11.Dynamic = {
    name = 'luatest.Module1.Module11.Dynamic',
    type_id = '2144297238039226637',
    metatable = message.luatest.Module1.Module11.Dynamic,
    variables = {
        dyn = { id = 1, type = 'dynamic', dynamcic_meta = { ['1'] = 'luatest.Module1.Module11.Base', }, },
    },
}

function Module11.__reg__(meta)
    meta.beans['luatest.Module1.Module11.Base'] = Module11.Base
    meta.beans['luatest.Module1.Module11.Dynamic'] = Module11.Dynamic
end

return Module11
