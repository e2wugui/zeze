#include "Protocol.h"

namespace Zeze
{
	namespace Net
	{
		bool Protocol::Send(Socket* socket)
		{
			if (nullptr == socket)
				return false;
			ByteBuffer bb(1024);
			EncodeProtcocol(bb);
			socket->Send((const char*)bb.Bytes, bb.ReadIndex, bb.Size());
			return true;
		}

		void Protocol::DecodeProtocol(Service* service, const std::shared_ptr<Socket>& sender, ByteBuffer& bb, IDecodeAndDispatcher* toLua /*= nullptr*/)
		{
			ByteBuffer os(bb.Bytes, bb.ReadIndex, bb.Size()); // 创建一个新的ByteBuffer，解码确认了才修改bb索引。
			while (os.Size() > 0)
			{
				// 尝试读取协议类型和大小
				int moduleId;
				int protocolId;
				int size;
				int readIndexSaved = os.ReadIndex;

				if (os.Size() >= 12) // protocl header size.
				{
					moduleId = os.ReadInt4();
					protocolId = os.ReadInt4();
					size = os.ReadInt4();
				}
				else
				{
					// SKIP! 只有协议发送被分成很小的包，协议头都不够的时候才会发生这个异常。几乎不可能发生。
					bb.ReadIndex = readIndexSaved;
					return;
				}

				// 以前写过的实现在数据不够之前会根据type检查size是否太大。
				// 现在去掉协议的最大大小的配置了.由总的参数 SocketOptions.InputBufferMaxProtocolSize 限制。
				// 参考 AsyncSocket
				if (size > os.Size())
				{
					// not enough data. try next time.
					bb.ReadIndex = readIndexSaved;
					return;
				}

				ByteBuffer pbb(os.Bytes, os.ReadIndex, size);
				os.ReadIndex += size;
				Service::ProtocolFactoryHandle factoryHandle;
				int64_t type = ((int64_t)moduleId << 32) | (unsigned int)protocolId;
				if (service->FindProtocolFactoryHandle(type, factoryHandle))
				{
					std::auto_ptr<Protocol> p(factoryHandle.Factory());
					p->Decode(pbb);
					p->Sender = sender;

					if (service->IsHandshakeProtocol(type))
					{
						factoryHandle.Handle(p.get());
					}
					else
					{
						p.release()->Dispatch(service, factoryHandle);
					}
					continue;
				}
				// 优先派发c++实现，然后尝试lua实现，最后UnknownProtocol。
				if (nullptr != toLua && toLua->DecodeAndDispatch(service, sender->GetSessionId(), moduleId, protocolId, pbb))
					continue;
				service->DispatchUnknownProtocol(sender, moduleId, protocolId, pbb);
			}
			bb.ReadIndex = os.ReadIndex;
		}
	} // namespace Net
} // namespace Zeze