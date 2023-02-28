package Zeze.Net;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.ProtocolFactoryFinder;

public abstract class Protocol<TArgument extends Serializable> implements Serializable {
	public static final int HEADER_SIZE = 12; // moduleId[4] + protocolId[4] + size[4]

	private Object sender; // AsyncSocket or DatagramSession
	private Object userState;
	public TArgument Argument;
	protected long resultCode;

	public int getFamilyClass() {
		return FamilyClass.Protocol;
	}

	public AsyncSocket getSender() {
		return (AsyncSocket)sender;
	}

	public void setSender(AsyncSocket sender) {
		this.sender = sender;
	}

	public Service getService() {
		return sender instanceof AsyncSocket ? ((AsyncSocket)sender).getService() : null;
	}

	public Object getUserState() {
		return userState;
	}

	public void setUserState(Object userState) {
		this.userState = userState;
	}

	public DatagramSession getDatagramSession() {
		return (DatagramSession)sender;
	}

	public void setDatagramSession(DatagramSession datagramSession) {
		sender = datagramSession;
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

	public Serializable getResultBean() {
		return null;
	}

	public abstract int getModuleId();

	public abstract int getProtocolId();

	public long getTypeId() {
		return makeTypeId(getModuleId(), getProtocolId());
	}

	public static long makeTypeId(int moduleId, int protocolId) {
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
		if (resultCode == 0)
			bb.WriteInt(FamilyClass.Protocol);
		else {
			bb.WriteInt(FamilyClass.Protocol | FamilyClass.BitResultCode);
			bb.WriteLong(resultCode);
		}
		Argument.encode(bb);
	}

	@Override
	public void decode(ByteBuffer bb) {
		var header = bb.ReadInt();
		if ((header & FamilyClass.FamilyClassMask) != FamilyClass.Protocol)
			throw new IllegalStateException("invalid header(" + header + ") for decoding protocol " + getClass());
		resultCode = (header & FamilyClass.BitResultCode) != 0 ? bb.ReadLong() : 0;
		Argument.decode(bb);
	}

	@Override
	public int preAllocSize() {
		return 10 + Argument.preAllocSize(); // [1]familyClass + [9]resultCode
	}

	@Override
	public void preAllocSize(int size) {
		Argument.preAllocSize(size - 1); // [1]familyClass
	}

	public final ByteBuffer encode() {
		int preAllocSize = preAllocSize();
		var bb = ByteBuffer.Allocate(Math.min(HEADER_SIZE + preAllocSize, 65536));
		encodeWithHead(bb);
		return bb;
	}

	public final void encodeWithHead(ByteBuffer bb) {
		bb.WriteInt4(getModuleId());
		bb.WriteInt4(getProtocolId());
		int saveSize = bb.BeginWriteWithSize4();
		encode(bb);
		bb.EndWriteWithSize4(saveSize);

		int size = bb.size() - saveSize - 4;
		if (size > preAllocSize())
			preAllocSize(size);
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
		resultCode = code;
		SendResult(result);
	}

	public <P extends Protocol<?>> void dispatch(Service service, Service.ProtocolFactoryHandle<P> factoryHandle)
			throws Exception {
		service.dispatchProtocol(this, factoryHandle);
	}

	public <P extends Protocol<?>> long handle(Service service, Service.ProtocolFactoryHandle<P> factoryHandle)
			throws Exception {
		var handle = factoryHandle.Handle;
		if (handle != null)
			return handle.handleProtocol(this);

		Service.logger.warn("Protocol.handle({}): Protocol Handle Not Found: {}", service.getName(), this);
		if (service.getSocketOptions().isCloseWhenMissHandle() && sender != null)
			((AsyncSocket)sender).close();

		return 0;
	}

	public <P extends Protocol<?>> long handle(DatagramService service, Service.ProtocolFactoryHandle<P> factoryHandle)
			throws Exception {
		var handle = factoryHandle.Handle;
		if (handle != null)
			return handle.handleProtocol(this);

		Service.logger.warn("Protocol.handle({}): Protocol Handle Not Found: {}", service.getName(), this);
		if (service.getSocketOptions().isCloseWhenMissHandle() && sender != null)
			((DatagramSession)sender).close();

		return 0;
	}

	/**
	 * 单个协议解码。输入是一个完整的协议包，返回解出的协议。如果没有找到解码存根，返回null。
	 *
	 * @param service               服务，用来查找协议存根。
	 * @param singleEncodedProtocol 单个完整的协议包。
	 * @return decoded protocol instance. if decode fail return null.
	 */
	public static Protocol<?> decode(ProtocolFactoryFinder service, ByteBuffer singleEncodedProtocol) {
		int moduleId = singleEncodedProtocol.ReadInt4();
		int protocolId = singleEncodedProtocol.ReadInt4();
		int size = singleEncodedProtocol.ReadInt4();
		int beginReadIndex = singleEncodedProtocol.ReadIndex;
		int endReadIndex = beginReadIndex + size;
		int savedWriteIndex = singleEncodedProtocol.WriteIndex;
		singleEncodedProtocol.WriteIndex = endReadIndex;

		Protocol<?> p = null;
		var factoryHandle = service.find(makeTypeId(moduleId, protocolId));
		if (factoryHandle != null && factoryHandle.Factory != null) {
			p = factoryHandle.Factory.create();
			p.decode(singleEncodedProtocol);
		}
		singleEncodedProtocol.ReadIndex = endReadIndex;
		singleEncodedProtocol.WriteIndex = savedWriteIndex;
		return p;
	}

	public static Protocol<?> decode(Service service, ByteBuffer singleEncodedProtocol) {
		return decode(service::findProtocolFactoryHandle, singleEncodedProtocol);
	}

	public static Protocol<?> decode(DatagramService service, ByteBuffer singleEncodedProtocol) {
		return decode(service::findProtocolFactoryHandle, singleEncodedProtocol);
	}

	/**
	 * moduleId[4] + protocolId[4] + size[4] + protocol.bytes[size]
	 */
	public static void decode(Service service, AsyncSocket so, ByteBuffer bb) throws Exception {
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
					var factoryHandle = service.findProtocolFactoryHandle(makeTypeId(moduleId, protocolId));
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
			int savedWriteIndex = bb.WriteIndex;
			bb.WriteIndex = endReadIndex;

			if (service.checkThrottle(so, moduleId, protocolId, size)
					&& !service.discard(so, moduleId, protocolId, size)) { // 默认超速是丢弃请求，
				var typeId = makeTypeId(moduleId, protocolId);
				var factoryHandle = service.findProtocolFactoryHandle(typeId);
				if (factoryHandle != null && factoryHandle.Factory != null) {
					service.dispatchProtocol(typeId, bb, factoryHandle, so);
				} else {
					if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId))
						AsyncSocket.log("RECV", so.getSessionId(), moduleId, protocolId, bb);
					service.dispatchUnknownProtocol(so, moduleId, protocolId, bb); // 这里只能临时读bb,不能持有Bytes引用
				}
			}
			bb.ReadIndex = endReadIndex;
			bb.WriteIndex = savedWriteIndex;
		}
	}

	@Override
	public String toString() {
		return String.format("%s ResultCode=%d%n\tArgument=%s", getClass().getName(), resultCode, Argument);
	}
}
