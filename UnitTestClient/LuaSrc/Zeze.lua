
ZezeProtocolHandles = {}

function ZezeDispatchProtocol(p)
    local handle = ZezeProtocolHandles[p.TypeId]
    if nil == handle then
        return 0
    handle(p)
    return 1 -- 1 if found. not result of handle 
end

function ZezeHandshakeDone(service, sessionId)
    ZezeCurrentService = service
    ZezeCurrentSessionId = sessionId
    -- connection ready. write you code here.
end

