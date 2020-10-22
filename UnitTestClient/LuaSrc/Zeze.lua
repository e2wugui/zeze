
ZezeProtocolHandles = {}

local demo = require 'demo'

function ZezeDispatchProtocol(p)
    local handle = ZezeProtocolHandles[p.TypeId]
    if (nil == handle) then
        return 0
    end
    handle(p)
    return 1 -- 1 if found. not result of handle 
end

function ZezeHandshakeDone(service, sessionId)
    ZezeCurrentService = service
    ZezeCurrentSessionId = sessionId
    -- connection ready. write you code here.

    local p = demo.Module1.Protocol1

    p.Argument={} -- reset

    p.Argument[demo.Mudule1.Value.int1] = 123
    p.Argument[demo.Mudule1.Value.long2] = 123
    p.Argument[demo.Mudule1.Value.string3] = '123'
    p.Argument[demo.Mudule1.Value.bool] = true
    p.Argument[demo.Mudule1.Value.short5] = 123
    p.Argument[demo.Mudule1.Value.float6] = 123.1
    p.Argument[demo.Mudule1.Value.double7] = 123.1
    p.Argument[demo.Mudule1.Value.bytes8] = '123'

    p.Argument[demo.Mudule1.Value.list9] = { 123, 124, 125 }

    p.Argument[demo.Mudule1.Value.set10] = { [123]=0, [124]=0, [125]=0 } -- set use key

    p.Argument[demo.Mudule1.Value.map11] = { [123]={ [demo.Mudule1.Value.int1]=123 } } -- map11 init part var for Value
    p.Argument[demo.Mudule1.Value.map11][124] = { [demo.Mudule1.Value.int1]=124 }

    p.Argument[demo.Mudule1.Value.bean12] = { [demo.Bean1.V1]=123 }
    p.Argument[demo.Mudule1.Value.bean12][demo.Bean1.V2] = { [123]=123, [124]=124 } -- another init

    p.Argument[demo.Mudule1.Value.byte13] = 123

    p.Argument[demo.Mudule1.Value.dynamic14] = {}
    p.Argument[demo.Mudule1.Value.dynamic14]._TypeId_ = demo.Module1.Simple._TypeId_
    p.Argument[demo.Mudule1.Value.dynamic14][demo.Module1.Simple.int1] = 123

    p.Argument[demo.Mudule1.Value.map15] = { [123]=123, [124]=124 }

    local serpent = require 'serpent'
    print('------------- send protocol ------------------')
    print(serpert.block(p))

    SendProtocolCurrent(p)
end

