local Module1Impl = {}

local Zeze = require 'Zeze'

function Module1Impl:Init()
    -- ZEZE_FILE_CHUNK {{{ REGISTER PROTOCOL
    Zeze.ProtocolHandles[74770] = Module1Impl.ProcessProtocol1
    Zeze.ProtocolHandles[82178] = Module1Impl.ProcessProtocol3
    Zeze.ProtocolHandles[116383] = Module1Impl.ProcessRpc1Request
    -- ZEZE_FILE_CHUNK }}} REGISTER PROTOCOL
end

function Module1Impl.ProcessProtocol1(p)
    -- write handle here
    print('------------- recv protocol shut down ------------------')
    print(require ('serpent').block(p))
    --IsMainRunning = false
end

function Module1Impl.ProcessProtocol3(p)
    -- write handle here
end

function Module1Impl.ProcessRpc1Request(rpc)
    -- write rpc request handle here
end


return Module1Impl
