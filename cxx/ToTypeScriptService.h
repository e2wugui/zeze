#pragma once

#include "CoreMinimal.h"
#include "GameFramework/Actor.h"
#include "Net.h"
#include "Protocol.h"
#include <unordered_map>
#include <unordered_set>
#include <mutex>
#include "ToTypeScriptService.generated.h"

class ToTypeScriptService : public Zeze::Net::Service
{
public:
    virtual void OnSocketClose(const std::shared_ptr<Socket>& sender, const std::exception* e) override
    {
        if (sender.get() == socket.get())
        {
            SetSocketClose(sender->SessionId);
            socket.reset();
        }
        Service::OnSocketClose(sender, e);
    }

    virtual void OnHandshakeDone(const std::shared_ptr<Socket>& sender) override
    {
        Service::OnHandshakeDone(sender);
        SetHandshakeDone(sender->SessionId);
    }

    virtual void OnSocketProcessInputBuffer(const std::shared_ptr<Socket>& sender, Zeze::Serialize::ByteBuffer& input) override
    {
        if (sender->IsHandshakeDone)
        {
            AppendInputBuffer(sender->SessionId, input);
            input.ReadIndex = input.WriteIndex;
        }
        else
        {
            Protocol::DecodeProtocol(this, sender, input);
        }
    }
    std::unordered_map<int64, Zeze::Serialize::ByteBuffer> ToBuffer;
    std::unordered_set<int64> ToHandshakeDone;
    std::unordered_set<int64> ToSocketClose;
    std::mutex mutex;

    void SetHandshakeDone(long long socketSessionId)
    {
        std::lock_guard<std::mutex> lock(mutex);
        {
            ToHandshakeDone.insert(socketSessionId);
        }
    }

    void SetSocketClose(long long socketSessionId)
    {
        std::lock_guard<std::mutex> lock(mutex);
        {
            ToSocketClose.insert(socketSessionId);
        }
    }

    void AppendInputBuffer(long long socketSessionId, Zeze::Serialize::ByteBuffer& buffer)
    {
        std::lock_guard<std::mutex> lock(mutex);
        {
            ToBuffer[socketSessionId].Append((const char*)buffer.Bytes, buffer.ReadIndex, buffer.Size());
        }
    }

};

DECLARE_DELEGATE_OneParam(FCallbackOnSocketHandshakeDone, int64, A);
DECLARE_DELEGATE_OneParam(FCallbackOnSocketClose, int64, A);
DECLARE_DELEGATE_FourParams(FCallbackOnSocketProcessInputBuffer, int64, FArrayBuffer&, int32, int32, A);

UCLASS()
class ZEZEUNREAL_API UToTypeScriptService : public UObject
{
    GENERATED_BODY()

    FCallbackOnSocketHandshakeDone CallbackSocketHandshakeDone;
    FCallbackOnSocketClose CallbackSocketClose;
    FCallbackOnSocketProcessInputBuffer CallbackSocketProcessInputBuffer;
    ToTypeScriptService RealService;

public:
    UToTypeScriptService(const std::string& name,
        const FCallbackOnSocketHandshakeDone& cb1,
        const FCallbackOnSocketClose& cb2,
        const FCallbackOnSocketProcessInputBuffer& cb3)
        : RealService(name)
    {
        CallbackSocketHandshakeDone = cb1;
        CallbackSocketClose = cb2;
        CallbackSocketProcessInputBuffer = cb3;
    }

    UFUNCTION(BlueprintCallable, meta = (DisplayName = "Connect", ScriptName = "Connect", Keywords = "Zeze"), Category = "Zeze")
    void Connect(const FString& host, int port, bool autoReconnect)
    {
        RealService.SetAutoConnect(autoReconnect);
        RealService.Connect(host, port); // TODO cast FString to std::string
    }

    UFUNCTION(BlueprintCallable, meta = (DisplayName = "Send", ScriptName = "Send", Keywords = "Zeze"), Category = "Zeze")
    void Send(int64 sessionId, const FArrayBuffer& buffer, int offset, int len)
    {
        if (Socket* so = RealService.socket().get())
        {
            if (so->SessionId == sessionId)
            {
                so->Send((const char*)buffer.Data, offset, len);
            }
        }
    }

    UFUNCTION(BlueprintCallable, meta = (DisplayName = "Close", ScriptName = "Close", Keywords = "Zeze"), Category = "Zeze")
    void Close(int64 sessionId)
    {
        if (Socket* so = RealService.socket().get())
        {
            if (so->SessionId == sessionId)
            {
                so->Close(NULL);
            }
        }
    }

    UFUNCTION(BlueprintCallable, meta = (DisplayName = "TickUpdate", ScriptName = "TickUpdate", Keywords = "Zeze"), Category = "Zeze")
    void TickUpdate()
    {
        std::unordered_set<long long> handshakeTmp;
        std::unordered_set<long long> socketCloseTmp;
        std::unordered_map<long long, Zeze::Serialize::ByteBuffer> inputTmp;
        std::lock_guard<std::mutex> lock(mutex);
        {
            handshakeTmp.swap(RealService.ToHandshakeDone);
            socketCloseTmp.swap(RealService.ToSocketClose);
            inputTmp.swap(RealService.ToBuffer);
        }

        for (auto& e : socketCloseTmp)
        {
            CallbackSocketClose(e);
        }

        for (auto& e : handshakeTmp)
        {
            CallbackSocketHandshakeDone(e);
        }

        for (auto& e : inputTmp)
        {
            CallbackSocketProcessInputBuffer(e.first, FArrayBuffer((char*)(e.second.Bytes + e.second.ReadIndex), e.second.Size()), 0, e.second.Size());
        }
    }
};
