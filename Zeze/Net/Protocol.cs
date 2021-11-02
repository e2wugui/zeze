using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;

namespace Zeze.Net
{
    public abstract class Protocol : Serializable
    {
        public abstract int ModuleId { get; }
        public abstract int ProtocolId { get; }

		public long TypeId => (long)ModuleId << 32 | (ProtocolId & 0xffff_ffff);

		public static int GetModuleId(long typeId)
        {
			return (int)((ulong)typeId >> 32);
        }

		public static int GetProtocolId(long typeId)
        {
			return (int)(typeId & 0xffff_ffff);
        }

		public Service Service { get; set; }

		public AsyncSocket Sender { get; set; }

		public object UserState { get; set; }

		internal virtual void Dispatch(Service service, Service.ProtocolFactoryHandle factoryHandle)
		{
			service.DispatchProtocol(this, factoryHandle);
		}

		public abstract void Decode(ByteBuffer bb);

		public abstract void Encode(ByteBuffer bb);

		public ByteBuffer Encode()
        {
			ByteBuffer bb = ByteBuffer.Allocate();

			bb.WriteInt4(ModuleId);
			bb.WriteInt4(ProtocolId);

			bb.BeginWriteWithSize4(out var state);
			this.Encode(bb);
			bb.EndWriteWithSize4(state);
			return bb;
		}

		public virtual bool Send(AsyncSocket so)
		{
			if (null == so)
				return false;
			Sender = so;
			if (UniqueRequestId == 0)
				UniqueRequestId = so.Service.NextSessionId();
			return so.Send(Encode());
		}

		public virtual bool Send(Service service)
		{
			AsyncSocket so = service.GetSocket();
			if (null != so)
				return Send(so);
			return false;
		}

		// 用于Rpc自动发送结果。
		// Rpc会重载实现。
		public virtual void SendResultCode(int code)
        {
			ResultCode = code;
		}

		// always true for Protocol, Rpc Will override
		public bool IsRequest { get; set; } = true;

		/// <summary>
		/// 唯一的请求编号，重发时保持不变。
		/// 第一次发送的时候用Service.SessionIdGenerator生成。
		/// Rpc才会实际使用这个。
		/// </summary>
		public long UniqueRequestId { get; protected set; }

		public int ResultCode { get; set; }

		/// <summary>
		/// Id + size + protocol.bytes
		/// </summary>
		/// <param name="bb"></param>
		/// <returns></returns>
		internal static void Decode(Service service, AsyncSocket so, ByteBuffer bb, Zeze.Services.ToLuaService.ToLua toLua = null)
        {
			ByteBuffer os = ByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.Size); // 创建一个新的ByteBuffer，解码确认了才修改bb索引。
			while (os.Size > 0)
			{
				// 尝试读取协议类型和大小
				int moduleId;
				int protocoId;
				int size;
				int readIndexSaved = os.ReadIndex;

				if (os.Size >= 12) // protocl header size.
				{
					moduleId = os.ReadInt4();
					protocoId = os.ReadInt4();
					size = os.ReadInt4();
				}
				else
				{
					// SKIP! 只有协议发送被分成很小的包，协议头都不够的时候才会发生这个异常。几乎不可能发生。
					//bb.ReadIndex = readIndexSaved;
					return;
				}

				// 以前写过的实现在数据不够之前会根据type检查size是否太大。
				// 现在去掉协议的最大大小的配置了.由总的参数 SocketOptions.InputBufferMaxProtocolSize 限制。
				// 参考 AsyncSocket
				long type = (long)moduleId << 32 | (protocoId & 0xffff_ffff);
				if (size < 0 || size > os.Size)
                {
					// 数据不够时检查。这个检测不需要严格的。如果数据够，那就优先处理。
					if (size < 0 || size > service.SocketOptions.InputBufferMaxProtocolSize)
                    {
						var pName = service.FindProtocolFactoryHandle(type)?.Factory().GetType().FullName;
						throw new Exception($"Decode InputBufferMaxProtocolSize '{service.Name}' p='{pName}' type={type} size={size}");
					}

					// not enough data. try next time.
					bb.ReadIndex = readIndexSaved;
					return;
				}

				var factoryHandle = service.FindProtocolFactoryHandle(type);
				if (null != factoryHandle)
				{
					var pBuffer = ByteBuffer.Wrap(os.Bytes, os.ReadIndex, size);
					os.ReadIndex += size;

					Protocol p = factoryHandle.Factory();
					p.Service = service;
					p.Decode(pBuffer);
					if (pBuffer.ReadIndex != pBuffer.WriteIndex)
                    {
						throw new Exception($"p=({moduleId},{protocoId}) size={size} too many data");
                    }
					p.Sender = so;
					p.UserState = so.UserState;
					p.Dispatch(service, factoryHandle);
					continue;
				}
				// 优先派发c#实现，然后尝试lua实现，最后UnknownProtocol。
				if (null != toLua)
				{
					if (toLua.DecodeAndDispatch(service, so.SessionId, type, os))
						continue;
				}
				service.DispatchUnknownProtocol(so, moduleId, protocoId, ByteBuffer.Wrap(os.Bytes, os.ReadIndex, size));
				os.ReadIndex += size;
			}
			bb.ReadIndex = os.ReadIndex;
		}

		public override string ToString()
        {
			return $"{GetType().FullName}({ModuleId},{ProtocolId},{UniqueRequestId})";
        }
    }

    public abstract class Protocol<TArgument> : Protocol where TArgument : global::Zeze.Transaction.Bean, new()
    {
        public TArgument Argument { get; set; } = new TArgument();

		public override void Decode(ByteBuffer bb)
        {
			ResultCode = bb.ReadInt();
			UniqueRequestId = bb.ReadLong();
			Argument.Decode(bb);
		}

		public override void Encode(ByteBuffer bb)
        {
			bb.WriteInt(ResultCode);
			bb.WriteLong(UniqueRequestId);
			Argument.Encode(bb);
		}

        public override string ToString()
        {
            return $"{GetType().FullName} UniqueRequestId={UniqueRequestId} ResultCode={ResultCode}{Environment.NewLine}\tArgument={Argument}";
        }
    }
}
