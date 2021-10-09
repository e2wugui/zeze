package Zeze.Net;

import Zeze.Serialize.*;
import Zeze.*;

public abstract class Protocol implements Serializable {
	public abstract int getModuleId();
	public abstract int getProtocolId();
	public final int getTypeId() {
		return getModuleId() << 16 | getProtocolId();
	}
	private Service Service;
	public final Service getService() {
		return Service;
	}
	public final void setService(Service value) {
		Service = value;
	}

	public static int GetModuleId(int type) {
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
		return type >> 16 & 0xffff;
	}

	public static int GetProtocolId(int type) {
		return type & 0xffff;
	}

	private AsyncSocket Sender;
	public final AsyncSocket getSender() {
		return Sender;
	}
	public final void setSender(AsyncSocket value) {
		Sender = value;
	}

	private Object UserState;
	public final Object getUserState() {
		return UserState;
	}
	public final void setUserState(Object value) {
		UserState = value;
	}

	public void Dispatch(Service service, Service.ProtocolFactoryHandle factoryHandle) {
		service.DispatchProtocol(this, factoryHandle);
	}

	public abstract void Decode(ByteBuffer bb);

	public abstract void Encode(ByteBuffer bb);

	public final ByteBuffer Encode() {
		ByteBuffer bb = ByteBuffer.Allocate();
		bb.WriteInt4(getTypeId());
		int state;
		tangible.OutObject<Integer> tempOut_state = new tangible.OutObject<Integer>();
		bb.BeginWriteWithSize4(tempOut_state);
	state = tempOut_state.outArgValue;
		this.Encode(bb);
		bb.EndWriteWithSize4(state);
		return bb;
	}

	public boolean Send(AsyncSocket so) {
		if (null == so) {
			return false;
		}
		setSender(so);
		if (getUniqueRequestId() == 0) {
			setUniqueRequestId(so.getService().NextSessionId());
		}
		return so.Send(Encode());
	}

	public boolean Send(Service service) {
		AsyncSocket so = service.GetSocket();
		if (null != so) {
			return Send(so);
		}
		return false;
	}

	// 用于Rpc自动发送结果。
	// Rpc会重载实现。
	public void SendResultCode(int code) {
		setResultCode(code);
	}

	// always true for Protocol, Rpc Will override
	private boolean IsRequest = true;
	public final boolean isRequest() {
		return IsRequest;
	}
	public final void setRequest(boolean value) {
		IsRequest = value;
	}

	/** 
	 唯一的请求编号，重发时保持不变。
	 第一次发送的时候用Service.SessionIdGenerator生成。
	 Rpc才会实际使用这个。
	*/
	private long UniqueRequestId;
	public final long getUniqueRequestId() {
		return UniqueRequestId;
	}
	protected final void setUniqueRequestId(long value) {
		UniqueRequestId = value;
	}

	private int ResultCode;
	public final int getResultCode() {
		return ResultCode;
	}
	public final void setResultCode(int value) {
		ResultCode = value;
	}

	/** 
	 Id + size + protocol.bytes
	 
	 @param bb
	 @return 
	*/

	public static void Decode(Service service, AsyncSocket so, ByteBuffer bb) {
		Decode(service, so, bb, null);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: internal static void Decode(Service service, AsyncSocket so, ByteBuffer bb, Zeze.Services.ToLuaService.ToLua toLua = null)
	public static void Decode(Service service, AsyncSocket so, ByteBuffer bb, Zeze.Services.ToLuaService.ToLua toLua) {
		ByteBuffer os = ByteBuffer.Wrap(bb.getBytes(), bb.getReadIndex(), bb.getSize()); // 创建一个新的ByteBuffer，解码确认了才修改bb索引。
		while (os.getSize() > 0) {
			// 尝试读取协议类型和大小
			int type;
			int size;
			int readIndexSaved = os.getReadIndex();

			if (os.getSize() >= 8) { // protocl header size.
				type = os.ReadInt4();
				size = os.ReadInt4();
			}
			else {
				// SKIP! 只有协议发送被分成很小的包，协议头都不够的时候才会发生这个异常。几乎不可能发生。
				//bb.ReadIndex = readIndexSaved;
				return;
			}

			// 以前写过的实现在数据不够之前会根据type检查size是否太大。
			// 现在去掉协议的最大大小的配置了.由总的参数 SocketOptions.InputBufferMaxProtocolSize 限制。
			// 参考 AsyncSocket
			if (size < 0 || size > os.getSize()) {
				// 数据不够时检查。这个检测不需要严格的。如果数据够，那就优先处理。
				if (size < 0 || size > service.SocketOptions.InputBufferMaxProtocolSize) {
					var pName = service.FindProtocolFactoryHandle(type) == null ? null : service.FindProtocolFactoryHandle(type).Factory().getClass().getName();
					throw new RuntimeException(String.format("Decode InputBufferMaxProtocolSize '%1$s' p='%2$s' type=%3$s size=%4$s", service.getName(), pName, type, size));
				}

				// not enough data. try next time.
				bb.setReadIndex(readIndexSaved);
				return;
			}

			Service.ProtocolFactoryHandle factoryHandle = service.FindProtocolFactoryHandle(type);
			if (null != factoryHandle) {
				var pBuffer = ByteBuffer.Wrap(os.getBytes(), os.getReadIndex(), size);
				os.setReadIndex(os.getReadIndex() + size);

				Protocol p = factoryHandle.Factory();
				p.Service = service;
				p.Decode(pBuffer);
				if (pBuffer.getReadIndex() != pBuffer.getWriteIndex()) {
					throw new RuntimeException(String.format("type=%1$s size=%2$s too many data", type, size));
				}
				p.Sender = so;
				p.UserState = so.getUserState();
				p.Dispatch(service, factoryHandle);
				continue;
			}
			// 优先派发c#实现，然后尝试lua实现，最后UnknownProtocol。
			if (null != toLua) {
				if (toLua.DecodeAndDispatch(service, so.getSessionId(), type, os)) {
					continue;
				}
			}
			service.DispatchUnknownProtocol(so, type, ByteBuffer.Wrap(os.getBytes(), os.getReadIndex(), size));
			os.setReadIndex(os.getReadIndex() + size);
		}
		bb.setReadIndex(os.getReadIndex());
	}

	@Override
	public String toString() {
		return String.format("%1$s(%2$s,%3$s,%4$s)", this.getClass().getName(), getModuleId(), getProtocolId(), getUniqueRequestId());
	}
}