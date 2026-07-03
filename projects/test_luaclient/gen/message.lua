local message_init = require('msg.message_init')
local message = {}

message.EmptyBean = message_init.EmptyBean
message.vector2 = message_init.vector2
message.vector2int = message_init.vector2int
message.vector3 = message_init.vector3
message.vector3int = message_init.vector3int
message.vector4 = message_init.vector4

message.luatest = require "msg.luatest"
message.luatest.Module1 = require "msg.luatest.Module1"
message.luatest.Module1.Module11 = require "msg.luatest.Module1.Module11"

message_init.init(message)

message.luatest.Module1.__reg__()
message.luatest.Module1.Module11.__reg__()

return message
