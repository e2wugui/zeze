local message = require 'msg.message'
local Module1 = { }

Module1.AutoKey = {
    name = 'luatest.Module1.AutoKey',
    type_id = '-2380498687461193834',
    metatable = message.luatest.Module1.AutoKey,
    variables = {
        name = { id = 1, type = 'string', },
        localId = { id = 2, type = 'long', },
    },
}

Module1.Key = {
    name = 'luatest.Module1.Key',
    type_id = '3613565487130963831',
    metatable = message.luatest.Module1.Key,
    variables = {
        s = { id = 1, type = 'short', default = 1, },
    },
}

Module1.BeanVar = {
    name = 'luatest.Module1.BeanVar',
    type_id = '4048650238212899759',
    metatable = message.luatest.Module1.BeanVar,
    variables = {
        ref = { id = 1, type = 'luatest.Module1.Simple', },
    },
}

Module1.Simple = {
    name = 'luatest.Module1.Simple',
    type_id = '1',
    metatable = message.luatest.Module1.Simple,
    variables = {
        int1 = { id = 1, type = 'int', },
        long2 = { id = 2, type = 'long', },
        string3 = { id = 3, type = 'string', },
    },
}

Module1.Value = {
    name = 'luatest.Module1.Value',
    type_id = '-6473288029264554187',
    metatable = message.luatest.Module1.Value,
    variables = {
        int1 = { id = 1, type = 'int', },
        long2 = { id = 2, type = 'long', },
        string3 = { id = 3, type = 'string', },
        bool4 = { id = 4, type = 'bool', },
        short5 = { id = 5, type = 'short', },
        float6 = { id = 6, type = 'float', },
        double7 = { id = 7, type = 'double', },
        list9 = { id = 9, type = 'list', value = 'luatest.Bean1', },
        set10 = { id = 10, type = 'set', value = 'int', },
        map11 = { id = 11, type = 'map', key = 'long', value = 'luatest.Module1.Simple', },
        dynamic14 = { id = 14, type = 'dynamic', dynamcic_meta = { ['1'] = 'luatest.Bean1', ['2'] = 'luatest.Module1.Simple', }, },
        vector2 = { id = 17, type = 'vector2', },
        vector3 = { id = 19, type = 'vector3', },
    },
}

Module1.Protocol1 = {
    name = 'luatest.Module1.Protocol1',
    id = '-1018745338',
    type_id = '7571189254',
    metatable = message.luatest.Module1.Protocol1,
    argument = 'luatest.Module1.Value',
}

Module1.Rpc1 = {
    name = 'luatest.Module1.Rpc1',
    id = '-1845124082',
    type_id = '6744810510',
    metatable = message.luatest.Module1.Rpc1,
    argument = 'luatest.Module1.Value',
    result = 'luatest.Module1.Value',
}

function Module1.__reg__(meta)
    meta.beans['luatest.Module1.AutoKey'] = Module1.AutoKey
    meta.beans['luatest.Module1.Key'] = Module1.Key
    meta.beans['luatest.Module1.BeanVar'] = Module1.BeanVar
    meta.beans['luatest.Module1.Simple'] = Module1.Simple
    meta.beans['luatest.Module1.Value'] = Module1.Value
    meta.protocols['luatest.Module1.Protocol1'] = Module1.Protocol1
    meta.protocols['luatest.Module1.Rpc1'] = Module1.Rpc1
end

return Module1
