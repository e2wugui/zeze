local Module1 = {}

local Zeze = require 'Zeze'

function Module1:Init()
    Zeze.ProtocolHandles[74770] = Module1.ProcessProtocol1
    Zeze.ProtocolHandles[82178] = Module1.ProcessProtocol3
end

function Module1:ProcessProtocol1(p)
    -- write handle here
    local serpent = require 'serpent'
    print('------------- recv protocol ------------------')
    print(serpert.block(p))
    IsMainRunning = false
end

function Module1:ProcessProtocol3(p)
    -- write handle here
end

