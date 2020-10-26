local Module1Impl = {}

local Zeze = require 'Zeze'

function Module1Impl:Init()
    Zeze.ProtocolHandles[74770] = Module1Impl.ProcessProtocol1
    Zeze.ProtocolHandles[82178] = Module1Impl.ProcessProtocol3
end

function Module1Impl.ProcessProtocol1(p)
    -- write handle here
    print(require ('serpent').block(p))
    print('------------- recv protocol shut down ------------------')
    IsMainRunning = false
end

function Module1Impl.ProcessProtocol3(p)
    -- write handle here
end


return Module1Impl
