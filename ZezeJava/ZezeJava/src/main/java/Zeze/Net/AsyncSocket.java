package Zeze.Net;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.GlobalTimer;
import Zeze.Util.JsonWriter;
import Zeze.Util.LongHashSet;
import Zeze.Util.ShutdownHook;
import Zeze.Util.TimeThrottle;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AsyncSocket implements Closeable {
	public static final @NotNull Logger logger = LogManager.getLogger();
	public static final @NotNull Level PROTOCOL_LOG_LEVEL = Level.toLevel(System.getProperty("protocolLog"), Level.OFF);
	public static final boolean ENABLE_PROTOCOL_LOG = PROTOCOL_LOG_LEVEL != Level.OFF && logger.isEnabled(PROTOCOL_LOG_LEVEL);
	public static final boolean ENABLE_DEBUG_LOG = logger.isDebugEnabled();
	public static final boolean ENABLE_PROTOCOL_LOG_OLD = "true".equalsIgnoreCase(System.getProperty("protocolLogOld"));
	private static final LongHashSet protocolLogExcept = new LongHashSet();

	protected Object userState;

	private static final AtomicLong sessionIdGen = new AtomicLong(1);
	private static @NotNull LongSupplier sessionIdGenFunc = sessionIdGen::getAndIncrement;

	static {
		var str = System.getProperty("protocolLogExcept");
		if (str != null) {
			//noinspection DynamicRegexReplaceableByCompiledPattern
			for (var numStr : str.split("[^\\d\\-]")) {
				if (!numStr.isBlank())
					protocolLogExcept.add(Long.parseLong(numStr));
			}
		}

		ShutdownHook.init();
	}

	private final @NotNull Service service;

	protected AsyncSocket(@NotNull Service service) {
		this.service = service;
	}

	public @NotNull Service getService() {
		return service;
	}

	public static boolean canLogProtocol(long protocolTypeId) {
		return !protocolLogExcept.contains(protocolTypeId);
	}

	public Object getUserState() {
		return userState;
	}

	public void setUserState(Object value) {
		userState = value;
	}

	public static void setSessionIdGenFunc(@Nullable LongSupplier seed) {
		sessionIdGenFunc = seed != null ? seed : sessionIdGen::getAndIncrement;
	}

	public enum Type {
		eServer,
		eClient,
		eServerSocket,
	}

	protected Type type;
	private int activeRecvTime; // 上次接收的时间戳(秒)
	private int activeSendTime; // 上次发送的时间戳(秒)

	public Type getType() {
		return type;
	}

	public int getActiveRecvTime() {
		return activeRecvTime;
	}

	public int getActiveSendTime() {
		return activeSendTime;
	}

	public void setActiveRecvTime() {
		activeRecvTime = (int)GlobalTimer.getCurrentSeconds();
	}

	public void setActiveSendTime() {
		activeSendTime = (int)GlobalTimer.getCurrentSeconds();
	}

	public void resetActiveSendRecvTime() {
		activeSendTime = activeRecvTime = (int)GlobalTimer.getCurrentSeconds();
	}

	private final long sessionId = sessionIdGenFunc.getAsLong(); // 只在setSessionId里修改

	public long getSessionId() {
		return sessionId;
	}

	// 优雅的关闭一般用于正常流程，不提供异常参数。
	public boolean closeGracefully() {
		return close(null, true);
	}

	public boolean close(@Nullable Throwable ex) {
		return close(ex, false);
	}

	@Override
	public void close() {
		close(null);
	}

	protected abstract boolean close(@Nullable Throwable ex, boolean gracefully);

	public abstract boolean Send(byte @NotNull [] bytes, int offset, int length);

	public boolean Send(@NotNull Protocol<?> p) {
		if (ENABLE_PROTOCOL_LOG && canLogProtocol(p.getTypeId()))
			log("SEND", getSessionId(), p);
		return Send(p.encode());
	}

	public boolean Send(@NotNull ByteBuffer bb) { // 返回true则bb的Bytes不能再修改了
		return Send(bb.Bytes, bb.ReadIndex, bb.size());
	}

	public boolean Send(@NotNull Binary binary) {
		return Send(binary.bytesUnsafe(), binary.getOffset(), binary.size());
	}

	public boolean Send(@NotNull String str) {
		return Send(str.getBytes(StandardCharsets.UTF_8));
	}

	public boolean Send(byte @NotNull [] bytes) { // 返回true则bytes不能再修改了
		return Send(bytes, 0, bytes.length);
	}

	public static void log(@NotNull String action, long id, @NotNull Protocol<?> p) {
		var sb = new StringBuilder(64);
		sb.append(action).append(':').append(id).append(' ').append(p.getClass().getSimpleName());
		boolean logResultCode;
		Object bean;
		if (p instanceof Rpc) {
			var rpc = ((Rpc<?, ?>)p);
			sb.append(':').append(rpc.getSessionId());
			logResultCode = !rpc.isRequest();
			bean = logResultCode ? rpc.Result : rpc.Argument;
		} else {
			logResultCode = p.resultCode != 0;
			bean = p.Argument;
		}
		if (logResultCode)
			sb.append('>').append(p.resultCode);
		sb.append(' ').append(toStr(bean));
		logger.log(PROTOCOL_LOG_LEVEL, sb);
	}

	public static void log(@NotNull String action, long id, String platform, @NotNull Protocol<?> p) {
		var sb = new StringBuilder(64);
		sb.append(action).append(':').append(id);
		if (platform != null && !platform.isEmpty())
			sb.append('@').append(platform);
		sb.append(' ').append(p.getClass().getSimpleName());
		boolean logResultCode;
		Object bean;
		if (p instanceof Rpc) {
			var rpc = ((Rpc<?, ?>)p);
			sb.append(':').append(rpc.getSessionId());
			logResultCode = !rpc.isRequest();
			bean = logResultCode ? rpc.Result : rpc.Argument;
		} else {
			logResultCode = p.resultCode != 0;
			bean = p.Argument;
		}
		if (logResultCode)
			sb.append('>').append(p.resultCode);
		sb.append(' ').append(toStr(bean));
		logger.log(PROTOCOL_LOG_LEVEL, sb);
	}

	public static void log(@NotNull String action, @NotNull String id, @NotNull Protocol<?> p) {
		var sb = new StringBuilder(64);
		sb.append(action).append(':').append(id).append(' ').append(p.getClass().getSimpleName());
		boolean logResultCode;
		Object bean;
		if (p instanceof Rpc) {
			var rpc = ((Rpc<?, ?>)p);
			sb.append(':').append(rpc.getSessionId());
			logResultCode = !rpc.isRequest();
			bean = logResultCode ? rpc.Result : rpc.Argument;
		} else {
			logResultCode = p.resultCode != 0;
			bean = p.Argument;
		}
		if (logResultCode)
			sb.append('>').append(p.resultCode);
		sb.append(' ').append(toStr(bean));
		logger.log(PROTOCOL_LOG_LEVEL, sb);
	}

	public static void log(@NotNull String action, long sessionId, int moduleId, int protocolId, @NotNull ByteBuffer bb) {
		int beginReadIndex = bb.ReadIndex;
		int header = -1;
		int familyClass = 0;
		var resultCode = 0L;
		var rpcSessionId = 0L;
		try {
			header = bb.ReadUInt();
			familyClass = header & FamilyClass.FamilyClassMask;
			if ((header & FamilyClass.BitResultCode) != 0)
				resultCode = bb.ReadLong();
			if (FamilyClass.isRpc(familyClass))
				rpcSessionId = bb.ReadLong();
		} catch (Exception e) {
			logger.error("decode protocol(moduleId={}, protocolId={}, size={}) exception:",
					moduleId, protocolId, bb.WriteIndex - beginReadIndex, e);
		}
		var sb = new StringBuilder();
		sb.append(action).append(':').append(sessionId).append(' ').append(moduleId).append(':').append(protocolId);
		if (FamilyClass.isRpc(familyClass))
			sb.append(':').append(rpcSessionId);
		if (familyClass == FamilyClass.Response || resultCode != 0)
			sb.append('>').append(resultCode);
		sb.append(' ').append(header).append('[').append(bb.size()).append(']');
		bb.ReadIndex = beginReadIndex;
		logger.log(PROTOCOL_LOG_LEVEL, sb);
	}

	public static void log(@NotNull String action, @NotNull Object idStr, long typeId, @NotNull ByteBuffer bb) {
		int moduleId = Protocol.getModuleId(typeId);
		int protocolId = Protocol.getProtocolId(typeId);
		int beginReadIndex = bb.ReadIndex;
		int header = -1;
		int familyClass = 0;
		var resultCode = 0L;
		var rpcSessionId = 0L;
		try {
			header = bb.ReadUInt();
			familyClass = header & FamilyClass.FamilyClassMask;
			if ((header & FamilyClass.BitResultCode) != 0)
				resultCode = bb.ReadLong();
			if (FamilyClass.isRpc(familyClass))
				rpcSessionId = bb.ReadLong();
		} catch (Exception e) {
			logger.error("decode protocol(moduleId={}, protocolId={}, size={}) exception:",
					moduleId, protocolId, bb.WriteIndex - beginReadIndex, e);
		}
		var sb = new StringBuilder();
		sb.append(action).append(':').append(idStr).append(' ').append(moduleId).append(':').append(protocolId);
		if (FamilyClass.isRpc(familyClass))
			sb.append(':').append(rpcSessionId);
		if (familyClass == FamilyClass.Response || resultCode != 0)
			sb.append('>').append(resultCode);
		sb.append(' ').append(header).append('[').append(bb.size()).append(']');
		bb.ReadIndex = beginReadIndex;
		logger.log(PROTOCOL_LOG_LEVEL, sb);
	}

	public static @NotNull String toStr(@Nullable Object obj) {
		if (ENABLE_PROTOCOL_LOG_OLD)
			return String.valueOf(obj);
		var jw = JsonWriter.local();
		try {
			return jw.clear().setFlagsAndDepthLimit(JsonWriter.FLAG_NO_QUOTE_KEY, 16).write(obj).toString();
		} finally {
			jw.clear();
		}
	}

	protected @Nullable TimeThrottle timeThrottle;

	public @Nullable TimeThrottle getTimeThrottle() {
		return timeThrottle;
	}

	public @Nullable Connector getConnector() {
		return null; // 只有tcp需要重载
	}

	protected volatile @Nullable SocketAddress remoteAddress; // 连接成功时设置

	public @Nullable SocketAddress getRemoteAddress() { // 连接成功前返回null, 成功后即使close也不会返回null
		return remoteAddress;
	}

	public @Nullable InetSocketAddress getRemoteInet() {
		SocketAddress sa = getRemoteAddress();
		return sa instanceof InetSocketAddress ? ((InetSocketAddress)sa) : null;
	}

	protected long recvCount, recvSize; // 已处理接收的次数, 已从socket接收数据的统计总字节数
	protected long sendCount, sendSize; // 已处理发送的次数, 已向socket发送数据的统计总字节数
	protected long sendRawSize; // 准备发送数据的统计总字节数(只在SetOutputSecurityCodec后统计,压缩加密之前的大小)

	public long getRecvCount() {
		return recvCount;
	}

	public long getRecvSize() {
		return recvSize;
	}

	public long getSendCount() {
		return sendCount;
	}

	public long getSendSize() {
		return sendSize;
	}

	public long getSendRawSize() {
		return sendRawSize;
	}

	public abstract boolean isClosed();
}
