local message_core = require('{{lua_util_dir}}.message_core')

local message_init = {}

---@class {{message_namespace}}.EmptyBean : {{message_namespace}}.Bean
local EmptyBean = {
    __type_name__ = 'EmptyBean',
    __type_id__ = '0',
    new = message_core.bean_new,
}

EmptyBean.__index = EmptyBean
message_init.EmptyBean = EmptyBean
message_init._default_empty_bean = EmptyBean:new()

---@class {{message_namespace}}.vector2 : {{message_namespace}}.Bean
local vector2 = {
    __type_name__ = 'vector2',
    __type_id__ = '0',
    new = message_core.bean_new,
}

vector2.__index = vector2
message_init.vector2 = vector2

---@class {{message_namespace}}.vector2int : {{message_namespace}}.Bean
local vector2int = {
    __type_name__ = 'vector2int',
    __type_id__ = '0',
    new = message_core.bean_new,
}

vector2int.__index = vector2int
message_init.vector2int = vector2int

---@class {{message_namespace}}.vector3 : {{message_namespace}}.Bean
local vector3 = {
    __type_name__ = 'vector3',
    __type_id__ = '0',
    new = message_core.bean_new,
}

vector3.__index = vector3
message_init.vector3 = vector3

---@class {{message_namespace}}.vector3int : {{message_namespace}}.Bean
local vector3int = {
    __type_name__ = 'vector3int',
    __type_id__ = '0',
    new = message_core.bean_new,
}

vector3int.__index = vector3int
message_init.vector3int = vector3int

---@class {{message_namespace}}.vector4 : {{message_namespace}}.Bean
local vector4 = {
    __type_name__ = 'vector4',
    __type_id__ = '0',
    new = message_core.bean_new,
}

vector4.__index = vector4
message_init.vector4 = vector4

function message_init.init(message)
    {{- for solution_name in solution_names }}
    message_init.{{solution_name}} = message.{{solution_name}}
    {{- end }}
end

return message_init