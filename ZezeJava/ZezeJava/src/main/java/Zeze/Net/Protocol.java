package Zeze.Net;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;

public abstract class Protocol<TArgument extends Bean> implements Serializable {
	private static final int HEADER_SIZE = 12; // moduleId[4] + protocolId[4] + size[4]

	private AsyncSocket Sender;
	private Object UserState;
	private long ResultCode;
	public TArgument Argument;

	public AsyncSocket getSender() {
		return Sender;
	}

	public void setSender(AsyncSocket sender) {
		Sender = sender;
	}

	public Service getService() {
		return Sender != null ? Sender.getService() : null;
	}

	public Object getUserState() {
		return UserState;
	}

	public void setUserState(Object userState) {
		UserState = userState;
	}

	public final long getResultCode() {
		return ResultCode;
	}

	public final void setResultCode(long value) {
		ResultCode = value;
	}

	public boolean isRequest() {
		return true;
	}

	public void setRequest(boolean request) {
	}

	public TArgument getArgumentBean() {
		return Argument;
	}

	public Bean getResultBean() {
		return null;
	}

	public abstract int getModuleId();

	public abstract int getProtocolId();

	public final long getTypeId() {
		return MakeTypeId(getModuleId(), getProtocolId());
	}

	public static long MakeTypeId(int moduleId, int protocolId) {
		return (long)moduleId << 32 | (protocolId & 0xffff_ffffL);
	}

	public static int GetModuleId(long typeId) {
		return (int)(typeId >> 32);
	}

	public static int GetProtocolId(long typeId) {
		return (int)typeId;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteLong(getResultCode());
		Argument.Encode(bb);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		setResultCode(bb.ReadLong());
		Argument.Decode(bb);
	}

	@Override
	public int getPreAllocSize() {
		return 9 + Argument.getPreAllocSize();
	}

	@Override
	public void setPreAllocSize(int size) {
		Argument.setPreAllocSize(size - 1);
	}

	public final ByteBuffer Encode() {
		int preAllocSize = getPreAllocSize();

		ByteBuffer bb = ByteBuffer.Allocate(Math.min(HEADER_SIZE + preAllocSize, 65536));
		bb.WriteInt4(getModuleId());
		bb.WriteInt4(getProtocolId());
		int saveSize = bb.BeginWriteWithSize4();
		Encode(bb);
		bb.EndWriteWithSize4(saveSize);

		int size = bb.Size() - saveSize - 4;
		if (size > preAllocSize)
			setPreAllocSize(size);
		return bb;
	}

	public boolean Send(AsyncSocket so) {
		if (so == null)
			return false;
		Sender = so;
		return so.Send(this);
	}

	public boolean Send(Service service) {
		return Send(service.GetSocket());
	}

	// 用于Rpc发送结果。
	// Rpc会重载实现。
	public void SendResult(Binary result) {
	}

	// Rpc 发送结果辅助函数。
	public final void SendResult() {
		SendResult(null);
	}

	// 用于Rpc发送结果。
	// Rpc会重载实现。
	public boolean trySendResultCode(long code) {
		return false;
	}

	public final void SendResultCode(long code) {
		SendResultCode(code, null);
	}

	public final void SendResultCode(long code, @SuppressWarnings("unused") Binary result) {
		setResultCode(code);
		SendResult(result);
	}

	@SuppressWarnings("unchecked")
	public <P extends Protocol<?>> void Dispatch(Service service, Service.ProtocolFactoryHandle<P> factoryHandle)
			throws Throwable {
		service.DispatchProtocol((P)this, factoryHandle);
	}

	/**
	 * moduleId[4] + protocolId[4] + size[4] + protocol.bytes[size]
	 */
	public static void Decode(Service service, AsyncSocket so, ByteBuffer bb) throws Throwable {
		while (bb.Size() >= HEADER_SIZE) { // 只有协议发送被分成很小的包，协议头都不够的时候才会发生这个异常。几乎不可能发生。
			// 读取协议类型和大小
			int beginReadIndex = bb.ReadIndex;
			int moduleId = bb.ReadInt4();
			int protocolId = bb.ReadInt4();
			int size = bb.ReadInt4();

			// 以前写过的实现在数据不够之前会根据type检查size是否太大。
			// 现在去掉协议的最大大小的配置了.由总的参数 SocketOptions.InputBufferMaxProtocolSize 限制。
			// 参考 AsyncSocket
			long longSize = size & 0xffff_ffffL;
			if (longSize > bb.Size()) {
				// 数据不够时检查。这个检测不需要严格的。如果数据够，那就优先处理。
				int maxSize = service.getSocketOptions().getInputBufferMaxProtocolSize();
				if (longSize > maxSize) {
					var factoryHandle = service.FindProtocolFactoryHandle(MakeTypeId(moduleId, protocolId));
					String pName = factoryHandle != null && factoryHandle.Factory != null ?
							factoryHandle.Factory.create().getClass().getName() : "?";
					throw new IllegalStateException(
							String.format("protocol '%s' in '%s' module=%d protocol=%d size=%d>%d too large!",
									pName, service.getName(), moduleId, protocolId, longSize, maxSize));
				}
				// not enough data. try next time.
				bb.ReadIndex = beginReadIndex;
				return;
			}
			int endReadIndex = beginReadIndex + HEADER_SIZE + size;

			var factoryHandle = service.FindProtocolFactoryHandle(MakeTypeId(moduleId, protocolId));
			if (factoryHandle != null && factoryHandle.Factory != null) {
				Protocol<?> p = factoryHandle.Factory.create();
				p.Decode(bb);
				if (bb.ReadIndex != endReadIndex)
					throw new IllegalStateException(
							String.format("protocol '%s' in '%s' module=%d protocol=%d size=%d!=%d decode error!",
									p.getClass().getName(), service.getName(), moduleId, protocolId,
									bb.ReadIndex - beginReadIndex - HEADER_SIZE, size));
				p.Sender = so;
				p.UserState = so.getUserState();
				if (AsyncSocket.ENABLE_PROTOCOL_LOG) {
					if (p.isRequest()) {
						if (p instanceof Rpc)
							AsyncSocket.logger.log(AsyncSocket.LEVEL_PROTOCOL_LOG, "RECV[{}] {}({}): {}", so.getSessionId(),
									p.getClass().getSimpleName(), ((Rpc<?, ?>)p).getSessionId(), p.Argument);
						else
							AsyncSocket.logger.log(AsyncSocket.LEVEL_PROTOCOL_LOG, "RECV[{}] {}: {}", so.getSessionId(),
									p.getClass().getSimpleName(), p.Argument);
					} else
						AsyncSocket.logger.log(AsyncSocket.LEVEL_PROTOCOL_LOG, "RECV[{}] {}({})>{} {}", so.getSessionId(),
								p.getClass().getSimpleName(), ((Rpc<?, ?>)p).getSessionId(), p.ResultCode, p.getResultBean());
				}
				p.Dispatch(service, factoryHandle);
			} else {
				if (AsyncSocket.ENABLE_PROTOCOL_LOG) {
					AsyncSocket.logger.log(AsyncSocket.LEVEL_PROTOCOL_LOG, "RECV[{}] {}:{} [{}]",
							so.getSessionId(), moduleId, protocolId, bb.Size());
				}
				int savedWriteIndex = bb.WriteIndex;
				bb.WriteIndex = endReadIndex;
				service.DispatchUnknownProtocol(so, moduleId, protocolId, bb); // 这里只能临时读bb,不能持有Bytes引用
				bb.ReadIndex = endReadIndex;
				bb.WriteIndex = savedWriteIndex;
			}
		}
	}

	@Override
	public String toString() {
		return String.format("%s ResultCode=%d%n\tArgument=%s", getClass().getName(), getResultCode(), Argument);
	}
}
