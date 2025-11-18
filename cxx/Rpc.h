#pragma once

#include "Protocol.h"
#include <cmath>
#include "TaskCompletionSource.h"

namespace Zeze
{
	namespace Net
	{
		template<class ArgumentType, class ResultType>
		class Rpc : public Protocol
		{
			void Schedule(Service* service, int64_t sessionId, int millisecondsTimeout) {
				int timeout = (int)std::ceil(millisecondsTimeout / 1000.0f);
				SetTimeout(
					[service, sessionId]()
					{
						auto context = (Rpc<ArgumentType, ResultType>*)service->RemoveRpcContext(sessionId);
				if (context == nullptr) // 一般来说，此时结果已经返回。
					return;

				context->IsTimeout = true;
				context->ResultCode = ResultCode::Timeout;

				if (context->Future.get() != nullptr)
					context->Future->TrySetException(std::exception("RpcTimeout"));
				else if (context->ResponseHandle) {
					// 本来Schedule已经在Task中执行了，这里又派发一次。
					// 主要是为了让应用能拦截修改Response的处理方式。
					// Timeout 应该是少的，先这样了。
					Service::ProtocolFactoryHandle factoryHandle;
					if (service->FindProtocolFactoryHandle(context->TypeId(), factoryHandle))
						service->DispatchRpcResponse(context, context->ResponseHandle, factoryHandle);
				}
					}, timeout);
			}

			bool Send(Socket* so, const std::function<int(Protocol*)>& responseHandle, int millisecondsTimeout = 5000)
			{
				if (so == nullptr)
					return false;
				auto service = so->GetService();
				if (service == nullptr)
					return false;

				// try remove. 只维护一个上下文。
				service->RemoveRpcContext(SessionId, this);
				ResponseHandle = responseHandle;
				SessionId = service->AddRpcContext(this);
				Timeout = millisecondsTimeout;
				IsTimeout = false;
				IsRequest = true;

				if (Protocol::Send(so)) {
					Schedule(service, SessionId, millisecondsTimeout);
					return true;
				}

				// 发送失败，一般是连接失效，此时删除上下文。
				// 其中rpc-trigger-result的原子性由RemoveRpcContext保证。
				// 恢复最初的语义吧：如果ctx已经被并发的Remove，也就是被处理了，这里返回true。
				return !service->RemoveRpcContext(SessionId, this);
			}

		public:
			std::unique_ptr<ArgumentType> Argument;
			std::unique_ptr<ResultType> Result;
			int64_t SessionId;
			bool IsRequest;
			bool IsTimeout;
			std::function<int(Protocol*)> ResponseHandle;
			bool SendResultDone;
			int Timeout;
			std::unique_ptr<TaskCompletionSource<bool>> Future;

			Rpc()
				: Argument(new ArgumentType()), Result(new ResultType())
			{
				SessionId = 0;
				IsRequest = true;
				IsTimeout = false;
				SendResultDone = false;
				Timeout = 0;
			}

			virtual int GetFamilyClass() const override
			{
				return IsRequest ? FamilyClass::Request : FamilyClass::Response;
			}

			void SendAsync(Socket* so, const std::function<int(Protocol*)>& responseHandle, int millisecondsTimeout = 5000)
			{
				std::auto_ptr<Rpc<ArgumentType, ResultType>> guard(this);
				if (Send(so, responseHandle, millisecondsTimeout))
					guard.release();
			}
			
			TaskCompletionSource<bool>* SendForWait(Socket* so, int millisecondsTimeout = 5000)
			{
				Future.reset(new TaskCompletionSource<bool>());
				if (!Send(so, nullptr, millisecondsTimeout))
					Future->TrySetException(std::exception("Send Failed."));
				return Future.get();
			}

			virtual void SendResult() override
			{
				if (SendResultDone) {
					return;
				}
				SendResultDone = true;
				IsRequest = false;
				if (!Protocol::Send(Sender.get()))
					std::cout << "Rpc.SendResult Failed: " << std::endl;
			}

			virtual bool TrySendResultCode(int64_t code) override
			{
				if (SendResultDone)
					return false;
				ResultCode = code;
				SendResult();
				return true;
			}

			virtual void Encode(ByteBuffer& bb) const
			{
				auto header = GetFamilyClass();
				if (ResultCode == 0)
				{
					bb.WriteInt(header);
				}
				else
				{
					bb.WriteInt(header | FamilyClass::BitResultCode);
					bb.WriteLong(ResultCode);
				}
				bb.WriteLong(SessionId);
				if (IsRequest)
					Argument->Encode(bb);
				else
					Result->Encode(bb);
			}

			virtual void Decode(ByteBuffer& bb)
			{
				auto header = bb.ReadInt();
				auto familyClass = header & FamilyClass::FamilyClassMask;
				if (!FamilyClass::IsRpc(familyClass))
					throw new std::invalid_argument("invalid header for decoding rpc ");
				IsRequest = familyClass == FamilyClass::Request;
				ResultCode = (header & FamilyClass::BitResultCode) != 0 ? bb.ReadLong() : 0;
				SessionId = bb.ReadLong();
				if (IsRequest)
					Argument->Decode(bb);
				else
					Result->Decode(bb);
			}

			virtual void Dispatch(Service* service, Service::ProtocolFactoryHandle& factoryHandle) override
			{
				if (IsRequest)
				{
					Protocol::Dispatch(service, factoryHandle);
					return;
				}
				std::auto_ptr<Rpc<ArgumentType, ResultType>> guard(this);

				// response, 从上下文中查找原来发送的rpc对象，并派发该对象。
				auto context = (Rpc<ArgumentType, ResultType>*)service->RemoveRpcContext(SessionId);
				if (context == nullptr)
					return;

				context->Sender = Sender;
				context->UserState = UserState;
				context->ResultCode = ResultCode;
				context->Result.reset(Result.release());
				context->IsTimeout = false; // not need
				context->IsRequest = false;

				if (context->Future != nullptr)
					context->Future->SetResult(true); // SendForWait，设置结果唤醒等待者。
				else if (context->ResponseHandle)
					service->DispatchRpcResponse(context, context->ResponseHandle, factoryHandle);
				else
					delete context; // 没有结果处理流程的context可以删除了。
			}
		};
	}
}