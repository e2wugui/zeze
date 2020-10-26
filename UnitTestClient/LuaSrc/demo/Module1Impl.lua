local Module1Impl = {}

local Zeze = require 'Zeze'

function Module1:Init()
    Zeze.ProtocolHandles[74770] = Module1.ProcessProtocol1
    Zeze.ProtocolHandles[82178] = Module1.ProcessProtocol3
end

function Module1Impl:ProcessProtocol1(p)
    -- write handle here
end

function Module1Impl:ProcessProtocol3(p)
    -- write handle here
end


return Module1Impl
