#pragma once

#include "ByteBuffer.h"
#include "Net.h"
#include "IDecodeAndDispatcher.h"

namespace Zeze
{
	namespace Net
	{
		class Protocol : public Serializable
		{
		public:
			long long ResultCode = 0;
			std::shared_ptr<Socket> Sender;

			Protocol() : Sender(NULL) { }
			virtual int ModuleId() const = 0;
			virtual int ProtocolId() const = 0;

			long long TypeId() { return ((long long)ModuleId() << 32) | (unsigned int)ProtocolId(); }

			constexpr static long long MakeTypeId(int mid, int pid)
			{
				return ((long long)mid << 32) | (unsigned int)pid;
			}

			void EncodeProtcocol(ByteBuffer& bb)
			{
				bb.WriteInt4(ModuleId());
				bb.WriteInt4(ProtocolId());
				int outstate;
				bb.BeginWriteWithSize4(outstate);
				this->Encode(bb);
				bb.EndWriteWithSize4(outstate);
			}

			void Send(Socket* socket)
			{
				ByteBuffer bb(1024);
				EncodeProtcocol(bb);
				socket->Send((const char *)bb.Bytes, bb.ReadIndex, bb.Size());
			}
			
			virtual void Dispatch(Service * service, Service::ProtocolFactoryHandle & factoryHandle)
			{
				service->DispatchProtocol(this, factoryHandle);
			}

			static void DecodeProtocol(Service * service, const std::shared_ptr<Socket> & sender, ByteBuffer& bb, IDecodeAndDispatcher* toLua = NULL);
		};

		template <typename ArgumentType>
		class ProtocolWithArgument : public Protocol
		{
		public:
			ArgumentType Argument;

			virtual void Decode(ByteBuffer& bb) override
			{
				ResultCode = bb.ReadLong();
				Argument.Decode(bb);
			}

			virtual void Encode(ByteBuffer& bb) const override
			{
				bb.WriteLong(ResultCode);
				Argument.Encode(bb);
			}
		};
	} // namespace Net
} // namespace Zeze