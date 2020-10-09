using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;
using Zeze.Serialize;

namespace Zeze.Net
{
    public abstract class Protocol : Serializable
    {
        public abstract int ModuleId { get; }
        public abstract int ProtocolId { get; }
        public int TypeId => ModuleId << 16 | ProtocolId;

		public AsyncSocket Sender { get; protected set; }
		public object UserState { get; set; }

		internal virtual void Dispatch(Service service)
		{
			service.DispatchProtocol(this, Service.DispatchType.Request);
		}

		public abstract void Decode(ByteBuffer bb);

		public abstract void Encode(ByteBuffer bb);

		public virtual void Send(AsyncSocket so)
		{
			Sender = so;

			ByteBuffer bb = ByteBuffer.Allocate();
			bb.WriteInt4(TypeId);
			int savedWriteIndex = bb.WriteIndex;
			bb.Append(Helper.Bytes4);
			this.Encode(bb);
			bb.Replace(savedWriteIndex, BitConverter.GetBytes(bb.Size - 8));
			so.Send(bb);
		}

		public void Send(Service service)
		{
			Send(service.GetSocket());
		}

		/// <summary>
		/// Id + size + protocol.bytes
		/// </summary>
		/// <param name="bb"></param>
		/// <returns></returns>
		internal static void Decode(Service service, AsyncSocket so, ByteBuffer bb)
        {
			ByteBuffer os = ByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.Size); // 创建一个新的ByteBuffer，解码确认了才修改bb索引。
			while (os.Size > 0)
			{
				// 尝试读取协议类型和大小
				int type;
				int size;
				int readIndexSaved = os.ReadIndex;

				if (os.Size >= 8) // protocl header size.
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
				if (size > os.Size)
                {
					// 数据不够时检查。这个检测不需要严格的。如果数据够，那就优先处理。
					if (size > service.SocketOptions.InputBufferMaxProtocolSize)
						throw new Exception("Decode InputBufferMaxProtocolSize" + service.SocketOptions.InputBufferMaxProtocolSize);

					// not enough data. try next time.
					bb.ReadIndex = readIndexSaved;
					return;
				}

				// 直接使用os，可以少创建对象，否则 Wrap 一个更安全：
				// ByteBuffer.Wrap(os.Bytes, os.ReadIndex, size)
				// 使用Wrap的话，记得手动增加: os.ReadIndex += size;
				Protocol p = service.CreateProtocol(type, os);
				if (null == p)
				{
					service.DispatchUnknownProtocol(so, type, ByteBuffer.Wrap(os.Bytes, os.ReadIndex, size));
					os.ReadIndex += size;
				}
				else
                {
					p.Sender = so;
					p.UserState = so.UserState;
					p.Dispatch(service);
				}
			}
			bb.ReadIndex = os.ReadIndex;
		}

		public override string ToString()
        {
			return $"{GetType().FullName}({ModuleId},{ProtocolId})";
        }
    }

    public abstract class Protocol<TArgument> : Protocol where TArgument : global::Zeze.Transaction.Bean, new()
    {
        public TArgument Argument { get; set; } = new TArgument();
		public int ResultCode { get; set; }

		public override void Decode(ByteBuffer bb)
        {
			ResultCode = bb.ReadInt();
			Argument.Decode(bb);
		}

		public override void Encode(ByteBuffer bb)
        {
			bb.WriteInt(ResultCode);
			Argument.Encode(bb);
		}
    }
}
