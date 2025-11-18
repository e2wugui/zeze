#pragma once

#include "CoreMinimal.h"
#include "ExtensionMethods.h"
#include "GameFramework/Actor.h"
#include "Net.h"
#include "Protocol.h"
#include <unordered_map>
#include <unordered_set>
#include <string>
#include <mutex>
#include "ToTypeScriptService.generated.h"

class ToTypeScriptService : public Zeze::Net::Service
{
public:
	virtual void OnSocketClose(const std::shared_ptr<Zeze::Net::Socket>& sender, const std::exception* e) override
	{
		SetSocketClose(sender->GetSessionId());
		Service::OnSocketClose(sender, e);
	}

	virtual void OnHandshakeDone(const std::shared_ptr<Zeze::Net::Socket>& sender) override
	{
		Service::OnHandshakeDone(sender);
		SetHandshakeDone(sender->GetSessionId());
	}

	virtual void OnSocketProcessInputBuffer(const std::shared_ptr<Zeze::Net::Socket>& sender, Zeze::ByteBuffer& input) override
	{
		if (sender->IsHandshakeDone())
		{
			AppendInputBuffer(sender->GetSessionId(), input);
			input.ReadIndex = input.WriteIndex;
		}
		else
		{
			Zeze::Net::Protocol::DecodeProtocol(this, sender, input);
		}
	}
	std::unordered_map<int64_t, std::string> ToBuffer;
	std::unordered_set<int64_t> ToHandshakeDone;
	std::unordered_set<int64_t> ToSocketClose;
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
			ToBuffer[socketSessionId].append((const char*)buffer.Bytes + buffer.ReadIndex, buffer.Size());
		}
	}

};

DECLARE_DYNAMIC_DELEGATE_OneParam(FCallbackOnSocketHandshakeDone, int64, sessionId);
DECLARE_DYNAMIC_DELEGATE_OneParam(FCallbackOnSocketClose, int64, sessionId);
DECLARE_DYNAMIC_DELEGATE_FourParams(FCallbackOnSocketProcessInputBuffer, int64, sessionId
, FArrayBuffer&, buffer, int32, offset, int32, size);

UCLASS()
class ZEZEUNREAL_API UToTypeScriptService : public UObject
{
	GENERATED_BODY()

		ToTypeScriptService RealService;
public:
	UPROPERTY()
		FCallbackOnSocketHandshakeDone CallbackWhenSocketHandshakeDone;

	UPROPERTY()
		FCallbackOnSocketClose CallbackWhenSocketClose;

	UPROPERTY()
		FCallbackOnSocketProcessInputBuffer CallbackWhenSocketProcessInputBuffer;

	UToTypeScriptService()
	{
	}

	UFUNCTION(BlueprintCallable, meta = (DisplayName = "Connect", ScriptName = "Connect", Keywords = "Zeze"), Category = "Zeze")
		void Connect(const FString& host, int port, bool autoReconnect)
	{
		RealService.SetAutoConnect(autoReconnect);
		RealService.Connect(std::string(TCHAR_TO_UTF8(*host)), port);
	}

	UFUNCTION(BlueprintCallable, meta = (DisplayName = "Send", ScriptName = "Send", Keywords = "Zeze"), Category = "Zeze")
		void Send(int64 sessionId, const FArrayBuffer& buffer, int offset, int len)
	{
		if (Zeze::Net::Socket* so = RealService.socket.get())
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
		if (Zeze::Net::Socket* so = RealService.socket.get())
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
		std::unordered_map<long long, std::string> inputTmp;
		std::lock_guard<std::mutex> lock(RealService.mutex);
		{
			handshakeTmp.swap(RealService.ToHandshakeDone);
			socketCloseTmp.swap(RealService.ToSocketClose);
			inputTmp.swap(RealService.ToBuffer);
		}

		for (auto& e : socketCloseTmp)
		{
			CallbackWhenSocketClose.ExecuteIfBound(e);
		}

		for (auto& e : handshakeTmp)
		{
			CallbackWhenSocketHandshakeDone.ExecuteIfBound(e);
		}

		for (auto& e : inputTmp)
		{
			FArrayBuffer ab;
			ab.Data = (void*)e.second.data();
			ab.Length = e.second.size();
			CallbackWhenSocketProcessInputBuffer.ExecuteIfBound(e.first, ab, 0, e.second.size());
		}
	}
};
