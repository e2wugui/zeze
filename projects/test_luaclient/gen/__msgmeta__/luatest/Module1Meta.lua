local message = require 'msg.message'
local Module1 = { }

Module1.AutoKey = {
    name = 'luatest.Module1.AutoKey',
    type_id = '-2380498687461193834',
    metatable = message.luatest.Module1.AutoKey,
    variables = {
        name = { id = 1, type = '{name: "string", is_bean: false, is_collection: false}', },
        localId = { id = 2, type = '{name: "long", is_bean: false, is_collection: false}', },
    },
}

Module1.Key = {
    name = 'luatest.Module1.Key',
    type_id = '3613565487130963831',
    metatable = message.luatest.Module1.Key,
    variables = {
        s = { id = 1, type = '{name: "short", is_bean: false, is_collection: false}', default = 1, },
    },
}

Module1.Simple = {
    name = 'luatest.Module1.Simple',
    type_id = '1',
    metatable = message.luatest.Module1.Simple,
    variables = {
        int1 = { id = 1, type = '{name: "int", is_bean: false, is_collection: false}', },
        long2 = { id = 2, type = '{name: "long", is_bean: false, is_collection: false}', },
        string3 = { id = 3, type = '{name: "string", is_bean: false, is_collection: false}', },
    },
}

Module1.Value = {
    name = 'luatest.Module1.Value',
    type_id = '-6473288029264554187',
    metatable = message.luatest.Module1.Value,
    variables = {
        int1 = { id = 1, type = '{name: "int", is_bean: false, is_collection: false}', },
        long2 = { id = 2, type = '{name: "long", is_bean: false, is_collection: false}', },
        string3 = { id = 3, type = '{name: "string", is_bean: false, is_collection: false}', },
        bool4 = { id = 4, type = '{name: "bool", is_bean: false, is_collection: false}', },
        short5 = { id = 5, type = '{name: "short", is_bean: false, is_collection: false}', },
        float6 = { id = 6, type = '{name: "float", is_bean: false, is_collection: false}', },
        double7 = { id = 7, type = '{name: "double", is_bean: false, is_collection: false}', },
        list9 = { id = 9, type = '{name: "list", is_bean: false, is_collection: true, value_type: {name: "Bean1", full_name: "luatest.Bean1", type_id: -5664905809240227220, variables: [{name: "v1", id: 1, type: "int", variable_type: {name: "int", is_bean: false, is_collection: false}, initial: "1"}, {name: "v2", id: 2, type: "map", variable_type: {name: "map", is_bean: false, is_collection: true, key_type: {name: "int", is_bean: false, is_collection: false}, value_type: {name: "int", is_bean: false, is_collection: false}}, initial: ""}], enums: [{name: "Enum1", value: "4"}]}}', value = 'luatest.Bean1', },
        set10 = { id = 10, type = '{name: "set", is_bean: false, is_collection: true, value_type: {name: "int", is_bean: false, is_collection: false}}', value = '{name: "int", is_bean: false, is_collection: false}', },
        map11 = { id = 11, type = '{name: "map", is_bean: false, is_collection: true, key_type: {name: "long", is_bean: false, is_collection: false}, value_type: {name: "Simple", full_name: "luatest.Module1.Simple", type_id: 1, variables: [{name: "int1", id: 1, type: "int", variable_type: {name: "int", is_bean: false, is_collection: false}, initial: ""}, {name: "long2", id: 2, type: "long", variable_type: {name: "long", is_bean: false, is_collection: false}, initial: ""}, {name: "string3", id: 3, type: "string", variable_type: {name: "string", is_bean: false, is_collection: false}, initial: ""}], enums: []}}', key = '{name: "long", is_bean: false, is_collection: false}', value = 'luatest.Module1.Simple', },
        dynamic14 = { id = 14, type = '{name: "dynamic", is_bean: false, is_collection: false, real_beans: [{key: 1, value: {name: "Bean1", full_name: "luatest.Bean1", type_id: -5664905809240227220, variables: [{name: "v1", id: 1, type: "int", variable_type: {name: "int", is_bean: false, is_collection: false}, initial: "1"}, {name: "v2", id: 2, type: "map", variable_type: {name: "map", is_bean: false, is_collection: true, key_type: {name: "int", is_bean: false, is_collection: false}, value_type: {name: "int", is_bean: false, is_collection: false}}, initial: ""}], enums: [{name: "Enum1", value: "4"}]}}, {key: 2, value: {name: "Simple", full_name: "luatest.Module1.Simple", type_id: 1, variables: [{name: "int1", id: 1, type: "int", variable_type: {name: "int", is_bean: false, is_collection: false}, initial: ""}, {name: "long2", id: 2, type: "long", variable_type: {name: "long", is_bean: false, is_collection: false}, initial: ""}, {name: "string3", id: 3, type: "string", variable_type: {name: "string", is_bean: false, is_collection: false}, initial: ""}], enums: []}}]}', dynamcic_meta = { ['1'] = 'luatest.Bean1', ['2'] = 'luatest.Module1.Simple', }, },
        vector2 = { id = 17, type = '{name: "vector2", is_bean: false, is_collection: false}', },
        vector3 = { id = 19, type = '{name: "vector3", is_bean: false, is_collection: false}', },
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
    meta.beans['luatest.Module1.Simple'] = Module1.Simple
    meta.beans['luatest.Module1.Value'] = Module1.Value
    meta.protocols['luatest.Module1.Protocol1'] = Module1.Protocol1
    meta.protocols['luatest.Module1.Rpc1'] = Module1.Rpc1
end

return Module1
