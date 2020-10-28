#pragma once

#include "ByteBuffer.h"
#include "Net.h"

namespace Zeze
{
	namespace Net
	{
		class Protocol : public Zeze::Serialize::Serializable
		{
		public:
			int ResultCode = 0;
			std::shared_ptr<Socket> Sender;

			Protocol() : Sender(NULL) { }
			virtual int ModuleId() = 0;
			virtual int ProtocolId() = 0;

			int TypeId() { return ModuleId() << 16 | ProtocolId(); }

			void EncodeProtcocol(Zeze::Serialize::ByteBuffer& bb)
			{
				bb.WriteInt4(TypeId());
				int outstate;
				bb.BeginWriteWithSize4(outstate);
				this->Encode(bb);
				bb.EndWriteWithSize4(outstate);
			}

			void Send(Socket* socket)
			{
				Zeze::Serialize::ByteBuffer bb(1024);
				EncodeProtcocol(bb);
				socket->Send(bb.Bytes, bb.ReadIndex, bb.Size());
			}
			
			virtual void Dispatch(Service * service)
			{
				service->DispatchProtocol(this);
			}

			static void DecodeProtocol(Service * service, const std::shared_ptr<Socket> & sender, Zeze::Serialize::ByteBuffer& bb, ToLua* toLua = NULL);
		};

		template <typename ArgumentType>
		class ProtocolWithArgument : public Protocol
		{
		public:
			ArgumentType Argument;

			virtual void Decode(Zeze::Serialize::ByteBuffer& bb)
			{
				ResultCode = bb.ReadInt();
				Argument.Decode(bb);
			}

			virtual void Encode(Zeze::Serialize::ByteBuffer& bb)
			{
				bb.WriteInt(ResultCode);
				Argument.Encode(bb);
			}
		};
	} // namespace Net
} // namespace Zeze