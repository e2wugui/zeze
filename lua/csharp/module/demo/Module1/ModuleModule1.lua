local msg = require('msg.message')
local Module1 = {}

function Module1.Init()
    Module1.RegisterHandlers()
end

--- [[ AUTO GENERATE START ]] ---
function Module1.RegisterHandlers()
    msg.demo.Module1.Protocol1.Handle = Module1.OnMsg_Protocol1
    msg.demo.Module1.Protocol3.Handle = Module1.OnMsg_Protocol3
    msg.demo.Module1.Rpc2.Handle = Module1.OnMsg_Rpc2
end
--- [[ AUTO GENERATE END ]] ---

---@param p msg.demo.Module1.Protocol1
function Module1.OnMsg_Protocol1(p)
end

---@param p msg.demo.Module1.Protocol3
function Module1.OnMsg_Protocol3(p)
end

---@param p msg.demo.Module1.Rpc2
function Module1.OnMsg_Rpc2(p)
end

return Module1
