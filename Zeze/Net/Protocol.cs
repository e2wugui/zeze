using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;
using Zeze.Serialize;
using System.Collections.Concurrent;

namespace Zeze.Net
{
    public abstract class Protocol : Serializable
    {
        public abstract int ModuleId { get; }
        public abstract int ProtocolId { get; }
        public int Id => ModuleId << 16 | ProtocolId;

		public AsyncSocket Sender { get; private set; }

		public static ConcurrentDictionary<int, Func<Protocol>> Factorys { get; } = new ConcurrentDictionary<int, Func<Protocol>>();

		public static Protocol Create(int type, ByteBuffer bb)
		{
			Func<Protocol> factory;
			if (false == Factorys.TryGetValue(type, out factory))
			{
				return null;
			}

			Protocol p = factory();
			p.Decode(bb);
			return p;
		}

		internal virtual void Dispatch(Manager manager)
		{
			manager.DispatchProtocol(this);
		}

		public abstract void Run();

		public abstract void Decode(ByteBuffer bb);

		public abstract void Encode(ByteBuffer bb);

		public virtual void Send(AsyncSocket so)
		{
			ByteBuffer bb = ByteBuffer.Allocate();
			bb.WriteInt4(Id);
			int savedWriteIndex = bb.WriteIndex;
			bb.Append(Helper.Bytes4);
			this.Encode(bb);
			bb.Replace(savedWriteIndex, BitConverter.GetBytes(bb.Size - 4));
			so.Send(bb);
		}

		public void Send(Manager manager)
		{
			Send(manager.GetSocket());
		}

		/// <summary>
		/// Id + size + protocol.bytes
		/// </summary>
		/// <param name="bb"></param>
		/// <returns></returns>
		internal static void Decode(Manager manager, AsyncSocket so, ByteBuffer bb)
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
				// 现在去掉协议的最大大小的配置了.由总的参数 InputBufferMaxCapacity 限制。
				// 参考 AsyncSocket
				if (size > os.Size)
                {
					// not enough data. try next time.
					bb.ReadIndex = readIndexSaved;
					return;
				}

				Protocol p = Create(type, os);
				if (null == p)
				{
					manager.DispatchUnknownProtocol(so, type, ByteBuffer.Wrap(os.Bytes, os.ReadIndex, size));
					os.ReadIndex += size;
				}
				else
                {
					p.Sender = so;
					p.Dispatch(manager);
				}
			}
			bb.ReadIndex = os.ReadIndex;
		}

		public override string ToString()
        {
            StringBuilder sb = new StringBuilder();
            sb.Append(this.GetType().FullName);
            sb.Append("(").Append(ModuleId).Append(",").Append(ProtocolId).Append(")");
            return sb.ToString();
        }
    }

    public abstract class Protocol<TArgument> : Protocol where TArgument : Zeze.Transaction.Bean, new()
    {
        public TArgument Argument { get; set; } = new TArgument();

        public override void Decode(ByteBuffer bb)
        {
			Argument.Decode(bb);
		}

		public override void Encode(ByteBuffer bb)
        {
			Argument.Encode(bb);
		}
    }
}
