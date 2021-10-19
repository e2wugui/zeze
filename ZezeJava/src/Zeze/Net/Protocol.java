package Zeze.Net;

import Zeze.Serialize.*;

public abstract class Protocol implements Serializable {
	public abstract int getModuleId();
	public abstract int getProtocolId();

	public final int getTypeId() {
		return getModuleId() << 16 | getProtocolId();
	}

	public Service Service;

	public static int GetModuleId(int type) {
		return type >>> 16 & 0xffff;
	}

	public static int GetProtocolId(int type) {
		return type & 0xffff;
	}

	public AsyncSocket Sender;
	public Object UserState;

	public void Dispatch(Service service, Service.ProtocolFactoryHandle factoryHandle) {
		service.DispatchProtocol(this, factoryHandle);
	}

	public abstract void Decode(ByteBuffer bb);

	public abstract void Encode(ByteBuffer bb);

	public final ByteBuffer Encode() {
		ByteBuffer bb = ByteBuffer.Allocate();
		bb.WriteInt4(getTypeId());
		int state = bb.BeginWriteWithSize4();
		this.Encode(bb);
		bb.EndWriteWithSize4(state);
		return bb;
	}

	public boolean Send(AsyncSocket so) {
		if (null == so) {
			return false;
		}
		Sender = so;
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
	*/

	public static void Decode(Service service, AsyncSocket so, ByteBuffer bb) {
		ByteBuffer os = ByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.Size());
		// 创建一个新的ByteBuffer，解码确认了才修改bb索引。
		while (os.Size() > 0) {
			// 尝试读取协议类型和大小
			int type;
			int size;
			int readIndexSaved = os.ReadIndex;

			if (os.Size() >= 8) { // protocl header size.
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
			if (size < 0 || size > os.Size()) {
				// 数据不够时检查。这个检测不需要严格的。如果数据够，那就优先处理。
				if (size < 0 || size > service.getSocketOptions().getInputBufferMaxProtocolSize()) {
					var factoryHandle = service.FindProtocolFactoryHandle(type);
					var pName = null == factoryHandle || null == factoryHandle.Factory
							? "" : factoryHandle.Factory.create().getClass().getName();
					throw new RuntimeException(String.format("Decode InputBufferMaxProtocolSize '%1$s' p='%2$s' type=%3$s size=%4$s", service.getName(), pName, type, size));
				}

				// not enough data. try next time.
				bb.ReadIndex = readIndexSaved;
				return;
			}

			Service.ProtocolFactoryHandle factoryHandle = service.FindProtocolFactoryHandle(type);
			if (null != factoryHandle) {
				var pBuffer = ByteBuffer.Wrap(os.Bytes, os.ReadIndex, size);
				os.ReadIndex += size;

				Protocol p = factoryHandle.Factory.create();
				p.Service = service;
				p.Decode(pBuffer);
				if (pBuffer.ReadIndex != pBuffer.WriteIndex) {
					throw new RuntimeException(String.format("type=%1$s size=%2$s too many data", type, size));
				}
				p.Sender = so;
				p.UserState = so.getUserState();
				p.Dispatch(service, factoryHandle);
				continue;
			}
			service.DispatchUnknownProtocol(so, type, ByteBuffer.Wrap(os.Bytes, os.ReadIndex, size));
			os.ReadIndex += size;
		}
		bb.ReadIndex = os.ReadIndex;
	}

	@Override
	public String toString() {
		return String.format("%1$s(%2$s,%3$s,%4$s)", this.getClass().getName(), getModuleId(), getProtocolId(), getUniqueRequestId());
	}
}