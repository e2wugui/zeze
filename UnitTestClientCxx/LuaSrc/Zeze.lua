
local Zeze = {}
Zeze.ProtocolHandles = {}

function ZezeDispatchProtocol(p)
    local handle = Zeze.ProtocolHandles[p.TypeId]
    if (nil == handle) then
        return 0
    end
    handle(p)
    return 1 -- 1 if found. not result of handle 
end

function ZezeHandshakeDone(service, sessionId)
    Zeze.CurrentService = service
    Zeze.CurrentSessionId = sessionId
    -- connection ready. write you code here.
end

return Zeze
