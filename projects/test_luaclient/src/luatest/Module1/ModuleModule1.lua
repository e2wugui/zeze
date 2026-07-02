local msg = require('msg.message')
local Module1 = {}

function Module1.Init()
    Module1.RegisterHandlers()
end

--- [[ AUTO GENERATE START ]] ---
function Module1.RegisterHandlers()
    msg.luatest.Module1.Protocol1.Handle = Module1.OnMsg_Protocol1
    msg.luatest.Module1.Rpc1.Handle = Module1.OnMsg_Rpc1
end
--- [[ AUTO GENERATE END ]] ---

---@param p msg.luatest.Module1.Protocol1
function Module1.OnMsg_Protocol1(p)
end

---@param p msg.luatest.Module1.Rpc1
function Module1.OnMsg_Rpc1(p)
end

return Module1
