#pragma once

#include "ByteBuffer.h"
#include "Net.h"
#include "IDecodeAndDispatcher.h"

namespace Zeze
{
	namespace Net
	{
		class FamilyClass
		{
		public:
			static const int Protocol = 2;
			static const int Request = 1;
			static const int Response = 0;
			static const int RaftRequest = 4;
			static const int RaftResponse = 3;

			static const int BitResultCode = 1 << 5;
			static const int FamilyClassMask = BitResultCode - 1;

			constexpr static bool IsRpc(int familyClass)
			{
				return familyClass <= Request;
			}

			constexpr static bool IsRaftRpc(int familyClass)
			{
				return familyClass >= RaftResponse;
			}
		};

		class Protocol : public Serializable
		{
		public:
			long long ResultCode = 0;
			std::shared_ptr<Socket> Sender;
			void* UserState;

			Protocol() { }
			virtual int ModuleId() const = 0;
			virtual int ProtocolId() const = 0;

			long long TypeId()
			{
				return ((long long)ModuleId() << 32) | (unsigned int)ProtocolId();
			}

			constexpr static long long MakeTypeId(int mid, int pid)
			{
				return ((long long)mid << 32) | (unsigned int)pid;
			}

			virtual int GetFamilyClass() const
			{
				return FamilyClass::Protocol;
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

			virtual bool Send(Socket* socket);

			virtual bool TrySendResultCode(long code)
			{
				return false;
			}

			virtual void SendResult()
			{
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
			std::unique_ptr<ArgumentType> Argument;

			ProtocolWithArgument()
				: Argument(new ArgumentType())
			{
			}

			virtual void Decode(ByteBuffer& bb) override
			{
				auto header = bb.ReadInt();
				if ((header & FamilyClass::FamilyClassMask) != FamilyClass::Protocol)
					throw new invalid_argument(std::string("invalid header(") + header + ") for decoding protocol ");
				ResultCode = (header & FamilyClass::BitResultCode) != 0 ? bb.ReadLong() : 0;
				Argument->Decode(bb);
			}

			virtual void Encode(ByteBuffer& bb) const override
			{
				if (ResultCode == 0)
					bb.WriteInt(FamilyClass::Protocol);
				else {
					bb.WriteInt(FamilyClass::Protocol | FamilyClass::BitResultCode);
					bb.WriteLong(ResultCode);
				}
				Argument->Encode(bb);
			}
		};
	} // namespace Net
} // namespace Zeze