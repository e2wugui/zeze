local Module1 = {}

function Module1:Init()
    ZezeProtocolHandles[74770] = Module1.ProcessProtocol1
    ZezeProtocolHandles[82178] = Module1.ProcessProtocol3
end

function Module1:ProcessProtocol1(p)
    -- write handle here
end

function Module1:ProcessProtocol3(p)
    -- write handle here
end

