#pragma once

#include "ByteBuffer.h"
#include "Net.h"
#include "IDecodeAndDispatcher.h"

namespace Zeze
{
	namespace Net
	{
		class Protocol : public Zeze::Serialize::Serializable
		{
		public:
			long long ResultCode = 0;
			std::shared_ptr<Socket> Sender;

			Protocol() : Sender(NULL) { }
			virtual int ModuleId() = 0;
			virtual int ProtocolId() = 0;

			long long TypeId() { return ((long long)ModuleId() << 32) | (unsigned int)ProtocolId(); }

			void EncodeProtcocol(Zeze::Serialize::ByteBuffer& bb)
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
				Zeze::Serialize::ByteBuffer bb(1024);
				EncodeProtcocol(bb);
				socket->Send((const char *)bb.Bytes, bb.ReadIndex, bb.Size());
			}
			
			virtual void Dispatch(Service * service, Service::ProtocolFactoryHandle & factoryHandle)
			{
				service->DispatchProtocol(this, factoryHandle);
			}

			static void DecodeProtocol(Service * service, const std::shared_ptr<Socket> & sender, Zeze::Serialize::ByteBuffer& bb, IDecodeAndDispatcher* toLua = NULL);
		};

		template <typename ArgumentType>
		class ProtocolWithArgument : public Protocol
		{
		public:
			ArgumentType Argument;

			virtual void Decode(Zeze::Serialize::ByteBuffer& bb) override
			{
				ResultCode = bb.ReadLong();
				Argument.Decode(bb);
			}

			virtual void Encode(Zeze::Serialize::ByteBuffer& bb) const override
			{
				bb.WriteLong(ResultCode);
				Argument.Encode(bb);
			}
		};
	} // namespace Net
} // namespace Zeze