using System;
using System.Runtime.CompilerServices;
using Zeze.Serialize;

namespace Zeze.Net
{
    public abstract class Protocol : Serializable
    {
		public interface IDecodeAndDispatch
		{
			bool DecodeAndDispatch(Service service, long sessionId, long typeId, ByteBuffer _os_);
        }

#if HAS_NLOG
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
#elif HAS_MYLOG
        private static readonly Zeze.MyLog logger = Zeze.MyLog.GetLogger(typeof(Protocol));
#endif

        public abstract int ModuleId { get; }
        public abstract int ProtocolId { get; }

		public long TypeId => (long)ModuleId << 32 | (uint)ProtocolId;
		public virtual int FamilyClass => Zeze.Net.FamilyClass.Protocol;

#if USE_CONFCS
		public virtual Zeze.Util.ConfBean ResultBean { get; }
		public virtual Zeze.Util.ConfBean ArgumentBean { get; }
#else
        public virtual Zeze.Transaction.Bean ResultBean { get; }
		public virtual Zeze.Transaction.Bean ArgumentBean { get; }
#endif
		public static int GetModuleId(long typeId)
        {
			return (int)(typeId >> 32);
        }

		public static int GetProtocolId(long typeId)
        {
			return (int)typeId;
        }

		public static long MakeTypeId(int moduleId, int protocolId)
        {
			return (long)moduleId << 32 | (uint)protocolId;
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

		public static T Decode<T>(ByteBuffer bb, T p)
			where T : Protocol
		{
			var mid = bb.ReadInt4();
			var pid = bb.ReadInt4();
			if (MakeTypeId(mid, pid) != p.TypeId)
				throw new Exception($"mid:pid={mid}:{pid} mismatch {p.ModuleId}:{p.ProtocolId}");
			var size = bb.ReadInt4();
			if (size > bb.Size)
				throw new Exception($"protocol data not enough.");
			p.Decode(bb);
			return p;
		}

		public ByteBuffer Encode()
        {
			ByteBuffer bb = ByteBuffer.Allocate(1024);

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
			Service = Sender.Service;
#if HAS_NLOG || HAS_MYLOG
			logger.Debug($"Send {this}");
#endif
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
		public virtual void SendResult(Binary result = null)
		{
		}

		public void SendResultCode(long code, Binary result = null)
        {
			ResultCode = code;
			SendResult(result);
		}

		// 用于Rpc发送结果。
		// Rpc会重载实现。
		public virtual bool TrySendResultCode(long code)
		{
			return false;
		}

		// always true for Protocol, Rpc Will setup
		public bool IsRequest { get; set; } = true;
		public long ResultCode { get; set; }

		/// <summary>
        /// 单个协议解码。输入是一个完整的协议包，返回解出的协议。如果没有找到解码存根，返回null。
        /// </summary>
        /// <param name="service">服务，用来查找协议存根。</param>
        /// <param name="singleEncodedProtocol">单个完整的协议包</param>
        /// <returns>decoded protocol instance. if decode fail return null.</returns>
        public static Protocol Decode(Service service, ByteBuffer singleEncodedProtocol)
        {
            int moduleId = singleEncodedProtocol.ReadInt4();
            int protocolId = singleEncodedProtocol.ReadInt4();
            int size = singleEncodedProtocol.ReadInt4();
            int beginReadIndex = singleEncodedProtocol.ReadIndex;
            int endReadIndex = beginReadIndex + size;
            int savedWriteIndex = singleEncodedProtocol.WriteIndex;
            singleEncodedProtocol.WriteIndex = endReadIndex;

            Protocol p = null;
            var factoryHandle = service.FindProtocolFactoryHandle(MakeTypeId(moduleId, protocolId));
            if (factoryHandle != null && factoryHandle.Factory != null)
            {
                p = factoryHandle.Factory();
                p.Decode(singleEncodedProtocol);
            }
            singleEncodedProtocol.ReadIndex = endReadIndex;
            singleEncodedProtocol.WriteIndex = savedWriteIndex;
            return p;
        }

		/// <summary>
        /// Id + size + protocol.bytes
        /// </summary>
        /// <param name="bb"></param>
        /// <returns></returns>
        internal static void Decode(Service service, AsyncSocket so, ByteBuffer bb, IDecodeAndDispatch toLua = null)
        {
			ByteBuffer os = ByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.Size); // 创建一个新的ByteBuffer，解码确认了才修改bb索引。
			while (os.Size > 0)
			{
				// 尝试读取协议类型和大小
				int moduleId;
				int protocolId;
				int size;
				int readIndexSaved = os.ReadIndex;

				if (os.Size >= 12) // protocl header size.
				{
					moduleId = os.ReadInt4();
					protocolId = os.ReadInt4();
					size = os.ReadInt4();
				}
				else
				{
					bb.ReadIndex = readIndexSaved;
					return;
				}

				// 以前写过的实现在数据不够之前会根据type检查size是否太大。
				// 现在去掉协议的最大大小的配置了.由总的参数 SocketOptions.InputBufferMaxProtocolSize 限制。
				// 参考 AsyncSocket
				long type = MakeTypeId(moduleId, protocolId);
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

				var pBuffer = ByteBuffer.Wrap(os.Bytes, os.ReadIndex, size);
				os.ReadIndex += size;
				if (service.CheckThrottle(so, moduleId, protocolId, size) && false == service.Discard(so, moduleId, protocolId, size))
				{
                    var factoryHandle = service.FindProtocolFactoryHandle(type);
                    if (null != factoryHandle)
                    {
                        Protocol p = factoryHandle.Factory();
                        p.Service = service;
                        p.Decode(pBuffer);
                        // 协议必须完整的解码，为了方便应用某些时候设计出兼容的协议。去掉这个检查。
                        /*
                        if (pBuffer.ReadIndex != pBuffer.WriteIndex)
                        {
                            throw new Exception($"p=({moduleId},{protocolId}) size={size} too many data");
                        }
                        */
                        p.Sender = so;
                        p.UserState = so.UserState;
                        p.Dispatch(service, factoryHandle);
                        continue;
                    }

                    // 优先派发c#实现，然后尝试lua实现，最后UnknownProtocol。
                    if (null != toLua)
                    {
                        if (toLua.DecodeAndDispatch(service, so.SessionId, type, pBuffer))
                        {
                            // 协议必须完整的解码，为了方便应用某些时候设计出兼容的协议。去掉这个检查。
                            /*
                            if (pBuffer.ReadIndex != pBuffer.WriteIndex)
                            {
                                throw new Exception($"toLua p=({moduleId},{protocolId}) size={size} too many data");
                            }
                            */
                            continue;
                        }
                    }
                    service.DispatchUnknownProtocol(so, moduleId, protocolId, pBuffer);
                }
            }
			bb.ReadIndex = os.ReadIndex;
		}

		public override string ToString()
        {
			return $"{GetType().FullName}({ModuleId},{ProtocolId})";
        }
    }

#if USE_CONFCS
    public abstract class Protocol<TArgument> : Protocol where TArgument : Zeze.Util.ConfBean, new()
    {
        public TArgument Argument { get; set; } = new TArgument();
        public override Zeze.Util.ConfBean ArgumentBean => Argument;
#else
    public abstract class Protocol<TArgument> : Protocol where TArgument : Transaction.Bean, new()
    {
        public TArgument Argument { get; set; } = new TArgument();
        public override Zeze.Transaction.Bean ArgumentBean => Argument;
#endif
        public override void Decode(ByteBuffer bb)
        {
            var compress = bb.ReadInt();
            //FamilyClass = compress & Zeze.Net.FamilyClass.FamilyClassMask;
            ResultCode = ((compress & Zeze.Net.FamilyClass.BitResultCode) != 0) ? bb.ReadLong() : 0;
            Argument.Decode(bb);
		}

		public override void Encode(ByteBuffer bb)
        {
            var compress = FamilyClass; // is Protocol(2)
            if (ResultCode != 0)
                compress |= Zeze.Net.FamilyClass.BitResultCode;
            bb.WriteInt(compress);
            if (ResultCode != 0)
                bb.WriteLong(ResultCode);
            Argument.Encode(bb);
        }

        public override string ToString()
        {
            return $"{GetType().FullName} ResultCode={ResultCode}{Environment.NewLine}  Argument={Argument}";
        }
    }
}
