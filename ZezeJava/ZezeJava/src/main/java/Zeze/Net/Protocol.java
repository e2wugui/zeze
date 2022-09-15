package Zeze.Net;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;

public abstract class Protocol<TArgument extends Bean> implements Serializable {
	private static final int HEADER_SIZE = 12; // moduleId[4] + protocolId[4] + size[4]

	private AsyncSocket sender;
	private Object userState;
	private long resultCode;
	public TArgument Argument;

	public AsyncSocket getSender() {
		return sender;
	}

	public void setSender(AsyncSocket sender) {
		this.sender = sender;
	}

	public Service getService() {
		return sender != null ? sender.getService() : null;
	}

	public Object getUserState() {
		return userState;
	}

	public void setUserState(Object userState) {
		this.userState = userState;
	}

	public final long getResultCode() {
		return resultCode;
	}

	public final void setResultCode(long value) {
		resultCode = value;
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

	public static int getModuleId(long typeId) {
		return (int)(typeId >> 32);
	}

	public static int getProtocolId(long typeId) {
		return (int)typeId;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteLong(getResultCode());
		Argument.encode(bb);
	}

	@Override
	public void decode(ByteBuffer bb) {
		setResultCode(bb.ReadLong());
		Argument.decode(bb);
	}

	@Override
	public int preAllocSize() {
		return 9 + Argument.preAllocSize();
	}

	@Override
	public void preAllocSize(int size) {
		Argument.preAllocSize(size - 1);
	}

	public final ByteBuffer encode() {
		int preAllocSize = preAllocSize();

		ByteBuffer bb = ByteBuffer.Allocate(Math.min(HEADER_SIZE + preAllocSize, 65536));
		bb.WriteInt4(getModuleId());
		bb.WriteInt4(getProtocolId());
		int saveSize = bb.BeginWriteWithSize4();
		this.encode(bb);
		bb.EndWriteWithSize4(saveSize);

		int size = bb.Size() - saveSize - 4;
		if (size > preAllocSize)
			preAllocSize(size);
		return bb;
	}

	public boolean Send(AsyncSocket so) {
		if (so == null)
			return false;
		sender = so;
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
	public <P extends Protocol<?>> void dispatch(Service service, Service.ProtocolFactoryHandle<P> factoryHandle)
			throws Throwable {
		service.DispatchProtocol((P)this, factoryHandle);
	}

	/**
	 * moduleId[4] + protocolId[4] + size[4] + protocol.bytes[size]
	 */
	public static void decode(Service service, AsyncSocket so, ByteBuffer bb) throws Throwable {
		while (bb.Size() >= HEADER_SIZE) { // 只有协议发送被分成很小的包，协议头都不够的时候才会发生这个异常。几乎不可能发生。
			// 读取协议类型和大小
			var bytes = bb.Bytes;
			int beginReadIndex = bb.ReadIndex;
			int moduleId = ByteBuffer.ToInt(bytes, beginReadIndex);
			int protocolId = ByteBuffer.ToInt(bytes, beginReadIndex + 4);
			int size = ByteBuffer.ToInt(bytes, beginReadIndex + 8);

			// 以前写过的实现在数据不够之前会根据type检查size是否太大。
			// 现在去掉协议的最大大小的配置了.由总的参数 SocketOptions.InputBufferMaxProtocolSize 限制。
			// 参考 AsyncSocket
			long longSize = size & 0xffff_ffffL;
			if (HEADER_SIZE + longSize > bb.Size()) {
				// 数据不够时检查。这个检测不需要严格的。如果数据够，那就优先处理。
				int maxSize = service.getSocketOptions().getInputBufferMaxProtocolSize();
				if (longSize > maxSize) {
					var factoryHandle = service.findProtocolFactoryHandle(MakeTypeId(moduleId, protocolId));
					var pName = factoryHandle != null && factoryHandle.Factory != null ?
							factoryHandle.Factory.create().getClass().getName() : "?";
					throw new IllegalStateException(
							String.format("protocol '%s' in '%s' module=%d protocol=%d size=%d>%d too large!",
									pName, service.getName(), moduleId, protocolId, longSize, maxSize));
				}
				// not enough data. try next time.
				return;
			}
			bb.ReadIndex = beginReadIndex += HEADER_SIZE;
			int endReadIndex = beginReadIndex + size;

			var factoryHandle = service.findProtocolFactoryHandle(MakeTypeId(moduleId, protocolId));
			if (factoryHandle != null && factoryHandle.Factory != null) {
				var p = factoryHandle.Factory.create();
				p.decode(bb);
				if (bb.ReadIndex != endReadIndex)
					throw new IllegalStateException(
							String.format("protocol '%s' in '%s' module=%d protocol=%d size=%d!=%d decode error!",
									p.getClass().getName(), service.getName(), moduleId, protocolId,
									bb.ReadIndex - beginReadIndex, size));
				p.sender = so;
				p.userState = so.getUserState();
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
								p.getClass().getSimpleName(), ((Rpc<?, ?>)p).getSessionId(), p.resultCode, p.getResultBean());
				}
				p.dispatch(service, factoryHandle);
			} else {
				if (AsyncSocket.ENABLE_PROTOCOL_LOG) {
					AsyncSocket.logger.log(AsyncSocket.LEVEL_PROTOCOL_LOG, "RECV[{}] {}:{} [{}]",
							so.getSessionId(), moduleId, protocolId, bb.Size());
				}
				int savedWriteIndex = bb.WriteIndex;
				bb.WriteIndex = endReadIndex;
				service.dispatchUnknownProtocol(so, moduleId, protocolId, bb); // 这里只能临时读bb,不能持有Bytes引用
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
