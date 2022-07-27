local message_core = require('common.message_core')

local message_init = {}

---@class msg.EmptyBean : msg.Bean
local EmptyBean = {
    __type_name__ = 'EmptyBean',
    __type_id__ = '0',
    new = message_core.bean_new,
}

EmptyBean.__index = EmptyBean
message_init.EmptyBean = EmptyBean

---@class msg.vector2 : msg.Bean
local vector2 = {
    __type_name__ = 'vector2',
    __type_id__ = '0',
    new = message_core.bean_new,
}

vector2.__index = vector2
message_init.vector2 = vector2

---@class msg.vector2int : msg.Bean
local vector2int = {
    __type_name__ = 'vector2int',
    __type_id__ = '0',
    new = message_core.bean_new,
}

vector2int.__index = vector2int
message_init.vector2int = vector2int

---@class msg.vector3 : msg.Bean
local vector3 = {
    __type_name__ = 'vector3',
    __type_id__ = '0',
    new = message_core.bean_new,
}

vector3.__index = vector3
message_init.vector3 = vector3

---@class msg.vector3int : msg.Bean
local vector3int = {
    __type_name__ = 'vector3int',
    __type_id__ = '0',
    new = message_core.bean_new,
}

vector3int.__index = vector3int
message_init.vector3int = vector3int

---@class msg.vector4 : msg.Bean
local vector4 = {
    __type_name__ = 'vector4',
    __type_id__ = '0',
    new = message_core.bean_new,
}

vector4.__index = vector4
message_init.vector4 = vector4

function message_init.init(message)
    {{- for solution in solutions }}
    message_init.{{solution.name}} = message.{{solution.name}}
    {{- end }}
end

return message_init