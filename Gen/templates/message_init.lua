local message_core = require('common.message_core')

local message_init = {}

---@class msg.EmptyBean : msg.Protocol
local EmptyBean = {
    __type_name__ = 'EmptyBean',
    __type_id__ = '0',
    new = message_core.bean_new,
}

EmptyBean.__index = EmptyBean
message_init.EmptyBean = EmptyBean

function message_init.init(message)
    {{- for solution in solutions }}
    message_init.{{solution.name}} = message.{{solution.name}}
    {{- end }}
end

return message_init