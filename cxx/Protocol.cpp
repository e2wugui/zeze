
#include "Protocol.h"

namespace Zeze
{
	namespace Net
	{
		void Protocol::DecodeProtocol(Service * service, const std::shared_ptr<Socket>& sender, Zeze::Serialize::ByteBuffer& bb, ToLua* toLua /*= NULL*/)
		{
			Zeze::Serialize::ByteBuffer os(bb.Bytes, bb.ReadIndex, bb.Size()); // 创建一个新的ByteBuffer，解码确认了才修改bb索引。
			while (os.Size() > 0)
			{
				// 尝试读取协议类型和大小
				int type;
				int size;
				int readIndexSaved = os.ReadIndex;

				if (os.Size() >= 8) // protocl header size.
				{
					type = os.ReadInt4();
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

				Zeze::Serialize::ByteBuffer pbb(os.Bytes, os.ReadIndex, size);
				os.ReadIndex += size;
				std::auto_ptr<Protocol> p(service->CreateProtocol(type, pbb));
				if (NULL == p.get())
				{
					// 优先派发c++实现，然后尝试lua实现，最后UnknownProtocol。
					if (NULL != toLua)
					{
						if (toLua->DecodeAndDispatch(service, sender->SessionId, type, os))
							continue;
					}
					service->DispatchUnknownProtocol(sender, type, pbb);
				}
				else
				{
					p->Sender = sender;
					p.release()->Dispatch(service);
				}
			}
			bb.ReadIndex = os.ReadIndex;
		}
	} // namespace Net
} // namespace Zeze