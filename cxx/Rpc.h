#pragma once

#include "Protocol.h"
#include <cmath>

namespace Zeze
{
	namespace Net
	{
		template<class ArgumentType, class ResultType>
		class Rpc : public Protocol
		{
		public:
			std::unique_ptr<ArgumentType> Argument;
			std::unique_ptr<ResultType> Result;
			long long SessionId;
			bool IsRequest;
			bool IsTimeout;
			std::function<int(Protocol*)> ResponseHandle;
			bool SendResultDone;
			int Timeout;

			Rpc()
				: Argument(new ArgumentType()), Result(new ResultType())
			{
				IsRequest = true;
				IsTimeout = false;
				SendResultDone = false;
			}

			virtual int GetFamilyClass() const override
			{
				return IsRequest ? FamilyClass::Request : FamilyClass::Response;
			}

			void Schedule(Service* service, long long sessionId, int millisecondsTimeout) {
				long timeout = std::ceil(millisecondsTimeout / 1000.0f);
				if (Reflect.inDebugMode)
					timeout += 10 * 60 * 1000; // 调试状态下RPC超时放宽到至少10分钟,方便调试时不容易超时

				Task.schedule(timeout, () -> {
					auto context = (Rpc<ArgumentType, ResultType>*)service->RemoveRpcContext(sessionId);
					if (context == NULL) // 一般来说，此时结果已经返回。
						return;

					context.IsTimeout = true;
					context.ResultCode = ErrorCode::Timeout;

					if (context->Future.get() != NULL)
						context->Future.TrySetException(RpcTimeoutException.getInstance());
					else if (context->ResponseHandle) {
						// 本来Schedule已经在Task中执行了，这里又派发一次。
						// 主要是为了让应用能拦截修改Response的处理方式。
						// Timeout 应该是少的，先这样了。
						Service::ProtocolFactoryHandle factoryHandle;
						if (service->FindProtocolFactoryHandle(context->TypeId(), factoryHandle))
							service.DispatchRpcResponse(context, context->ResponseHandle, factoryHandle);
					}
				});
			}

			bool Send(Socket* so, const std::function<int(Protocol*)>& responseHandle, int millisecondsTimeout = 5000)
			{
				if (so == NULL)
					return false;
				auto service = so->Service;
				if (service == NULL)
					return false;

				// try remove. 只维护一个上下文。
				service.RemoveRpcContext(SessionId, this);
				this.ResponseHandle = responseHandle;
				SessionId = service.AddRpcContext(this);
				Timeout = millisecondsTimeout;
				IsTimeout = false;
				IsRequest = true;

				if (Protocol::Send(so)) {
					schedule(service, SessionId, millisecondsTimeout);
					return true;
				}

				// 发送失败，一般是连接失效，此时删除上下文。
				// 其中rpc-trigger-result的原子性由RemoveRpcContext保证。
				// 恢复最初的语义吧：如果ctx已经被并发的Remove，也就是被处理了，这里返回true。
				return !service.RemoveRpcContext(SessionId, this);
			}

			TaskCompletionSource<TResult> SendForWait(Socket *, int millisecondsTimeout = 5000)
			{
				Future = new TaskCompletionSource<>();
				if (!Send(so, null, millisecondsTimeout))
					Future->TrySetException(new exception("Send Failed."));
				return Future;
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

			virtual bool TrySendResultCode(long long code) override
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
					throw new invalid_argument(std::string("invalid header(") + header + ") for decoding rpc ");
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

				// response, 从上下文中查找原来发送的rpc对象，并派发该对象。
				auto context = (Rpc<ArgumentType, ResultType>*)service->RemoveRpcContext(SessionId);
				if (context == NULL)
					return;

				context->Sender = Sender;
				context->UserState = UserState;
				context->ResultCode = ResultCode;
				context->Result.reset = Result;
				context->IsTimeout = false; // not need
				context->IsRequest = false;

				if (context->Future.get() != NULL)
					context->Future->SetResult(context->Result.release()); // SendForWait，设置结果唤醒等待者。
				else if (context->ResponseHandle)
					service->DispatchRpcResponse(context, context->ResponseHandle, factoryHandle);
			}
		};
	}
}