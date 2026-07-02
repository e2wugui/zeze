local message = require 'msg.message'
local meta = { beans = {}, protocols = {}, structs = {} }

meta.structs['vector2'] = {
    name = 'vector2',
    type_id = '8',
    metatable = message.vector2,
    variables = {
        x = { id = 1, type = 'float'},
        y = { id = 2, type = 'float'},
    },
}

meta.structs['vector2int'] = {
    name = 'vector2int',
    type_id = '9',
    metatable = message.vector2int,
    variables = {
        x = { id = 1, type = 'int'},
        y = { id = 2, type = 'int'},
    },
}

meta.structs['vector3'] = {
    name = 'vector3',
    type_id = '10',
    metatable = message.vector3,
    variables = {
        x = { id = 1, type = 'float'},
        y = { id = 2, type = 'float'},
        z = { id = 3, type = 'float'},
    },
}

meta.structs['vector3int'] = {
    name = 'vector3int',
    type_id = '11',
    metatable = message.vector3int,
    variables = {
        x = { id = 1, type = 'int'},
        y = { id = 2, type = 'int'},
        z = { id = 3, type = 'int'},
    },
}

meta.structs['vector4'] = {
    name = 'vector4',
    type_id = '12',
    metatable = message.vector4,
    variables = {
        x = { id = 1, type = 'float'},
        y = { id = 2, type = 'float'},
        z = { id = 3, type = 'float'},
        w = { id = 4, type = 'float'},
    },
}

require('msg.__msgmeta__.luatest.Module1Meta').__reg__(meta)
require('msg.__msgmeta__.luatest.Module1.Module11Meta').__reg__(meta)
require('msg.__msgmeta__.luatestMeta').__reg__(meta)
package.loaded['msg.__msgmeta__.luatest.Module1Meta'] = nil
package.loaded['msg.__msgmeta__.luatest.Module1.Module11Meta'] = nil
package.loaded['msg.__msgmeta__.luatestMeta'] = nil

return meta
