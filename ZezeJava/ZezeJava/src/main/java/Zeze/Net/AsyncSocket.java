package Zeze.Net;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.function.LongPredicate;
import java.util.function.Supplier;
import java.util.function.LongSupplier;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.GlobalTimer;
import Zeze.Util.JsonWriter;
import Zeze.Util.LongHashSet;
import Zeze.Util.ShutdownHook;
import Zeze.Util.TimeThrottle;
import Zeze.Util.ZezeCounter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * 异步连接抽象基类：定义生命周期状态机（置死/登记互斥）、解码准入与收发统计。
 * @since 1.7.0 起不实现 java.io.Closeable：socket 引用为共享借用，所有权归连接管理层（Service 等），
 * 		不满足 Closeable"获取即拥有"的契约；关闭请使用显式的 {@link #close(Throwable, boolean)}
 * 		或 {@link #closeGracefully()}。
 */
public abstract class AsyncSocket {
	private static final Logger logger = LogManager.getLogger(AsyncSocket.class);
	public static final Level PROTOCOL_LOG_LEVEL = Level.toLevel(System.getProperty("protocolLog"), Level.OFF);
	public static final boolean ENABLE_PROTOCOL_LOG = PROTOCOL_LOG_LEVEL != Level.OFF && logger.isEnabled(PROTOCOL_LOG_LEVEL);
	public static final boolean ENABLE_DEBUG_LOG = logger.isDebugEnabled();
	public static final boolean ENABLE_PROTOCOL_LOG_OLD = "true".equalsIgnoreCase(System.getProperty("protocolLogOld"));
	private static final LongHashSet protocolLogExcept = new LongHashSet();

	protected volatile @Nullable Object userState;

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

	private final @Nullable Service service;

	// 置死与登记互斥锁；锁内仅做状态转移与登记动作，不调用外部回调。
	private final Object lifecycleLock = new Object();
	private volatile boolean lifecycleClosed;

	// service 为 null 时（如测试桩）的保底发号流。
	private static final java.util.concurrent.atomic.AtomicLong nullServiceSessionIdGen =
			new java.util.concurrent.atomic.AtomicLong(
					(System.nanoTime() ^ new java.security.SecureRandom().nextLong()) & Long.MAX_VALUE);

	protected AsyncSocket(@Nullable Service service) {
		this.service = service;
		this.sessionId = service != null ? service.nextSessionId() : nullServiceSessionIdGen.getAndIncrement();
	}

	/** 置死转移（恰好一次），已关闭时返回 false。 */
	private boolean markClosed() {
		synchronized (lifecycleLock) {
			if (lifecycleClosed)
				return false;
			lifecycleClosed = true;
			return true;
		}
	}

	/** 登记临界区：与连接置死互斥，已关闭时返回 null；锁内仅执行登记动作。 */
	final <T> @Nullable T runIfOpen(Supplier<T> action) {
		synchronized (lifecycleLock) {
			if (lifecycleClosed)
				return null;
			return action.get();
		}
	}

	public boolean isClosed() {
		return lifecycleClosed;
	}

	public @Nullable Service getService() {
		return service;
	}

	/** 连接级解码准入谓词（仅 selector 线程访问）：未完成安全 codec 装配前限制可解码协议，null 表示不限制。 */
	private @Nullable LongPredicate decodeAdmission;

	public @Nullable LongPredicate getDecodeAdmission() {
		return decodeAdmission;
	}

	void setDecodeAdmission(@Nullable LongPredicate admission) {
		this.decodeAdmission = admission;
	}

	// close 收尾通知服务销毁会话；回调异常仅记日志不外抛。
	protected final void fireOnSocketDisposed() {
		try {
			//noinspection DataFlowIssue
			getService().OnSocketDisposed(this);
		} catch (Exception e) {
			logger.error("Service.OnSocketDisposed exception:", e);
		}
	}

	public static boolean canLogProtocol(long protocolTypeId) {
		return !protocolLogExcept.contains(protocolTypeId);
	}

	public @Nullable Object getUserState() {
		return userState;
	}

	public void setUserState(@Nullable Object value) {
		userState = value;
	}

	/**
	 * @deprecated 全局共享发号在同 JVM 多 Service 拓扑下会撞号，请改用实例级 {@link Service#setSessionIdGenerator}。
	 * 		本方法保留兼容，等价于 {@link Service#setDefaultSessionIdGenFunc}。
	 */
	@Deprecated
	public static void setSessionIdGenFunc(@Nullable LongSupplier seed) {
		Service.setDefaultSessionIdGenFunc(seed);
	}

	public enum Type {
		eServer,
		eClient,
		eServerSocket,
	}

	// selector 线程写、KeepAlive 定时线程读；用 long 秒避免 int 回绕。
	private volatile long activeRecvTime; // 上次接收的时间戳(秒)
	private volatile long activeSendTime; // 上次发送的时间戳(秒)

	public abstract Type getType();

	public long getActiveRecvTime() {
		return activeRecvTime;
	}

	public long getActiveSendTime() {
		return activeSendTime;
	}

	public void setActiveRecvTime() {
		activeRecvTime = GlobalTimer.getCurrentSeconds();
	}

	public void setActiveSendTime() {
		activeSendTime = GlobalTimer.getCurrentSeconds();
	}

	public void resetActiveSendRecvTime() {
		activeSendTime = activeRecvTime = GlobalTimer.getCurrentSeconds();
	}

	private final long sessionId;

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

	public void close() {
		close(null);
	}

	/** 置死恰好一次后执行子类关闭流程；final 保证置死不可绕过。 */
	public final boolean close(@Nullable Throwable ex, boolean gracefully) {
		if (!markClosed())
			return false;
		doClose(ex, gracefully);
		return true;
	}

	/** 子类关闭与资源释放；重入已由 {@link #close(Throwable, boolean)} 拦截。 */
	protected abstract void doClose(@Nullable Throwable ex, boolean gracefully);

	public abstract boolean Send(byte [] bytes, int offset, int length);

	public boolean Send(Protocol<?> p) {
		if (ENABLE_PROTOCOL_LOG && canLogProtocol(p.getTypeId()))
			log("SEND", getSessionId(), p);
		var bb = p.encode();
		var size = bb.size(); // Send 交接后 bb 不可再访问，先取计数所需值
		var r = Send(bb);
		if (r)
			ZezeCounter.instance.addSendSize(p.getTypeId(), size);
		return r;
	}

	public boolean Send(ByteBuffer bb) { // 返回true则bb的Bytes不能再修改了
		return Send(bb.Bytes, bb.ReadIndex, bb.size());
	}

	public boolean Send(Binary binary) {
		return Send(binary.bytesUnsafe(), binary.getOffset(), binary.size());
	}

	public boolean Send(String str) {
		return Send(str.getBytes(StandardCharsets.UTF_8));
	}

	public boolean Send(byte [] bytes) { // 返回true则bytes不能再修改了
		return Send(bytes, 0, bytes.length);
	}

	public static void log(String action, long id, Protocol<?> p) {
		var sb = new StringBuilder(64);
		sb.append(action).append(':').append(id).append(' ').append(p.getClass().getSimpleName());
		boolean logResultCode;
		Object bean;
		if (p instanceof Rpc<?, ?> rpc) {
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

	public static void log(String action, long id, @Nullable String platform, Protocol<?> p) {
		var sb = new StringBuilder(64);
		sb.append(action).append(':').append(id);
		if (platform != null && !platform.isEmpty())
			sb.append('@').append(platform);
		sb.append(' ').append(p.getClass().getSimpleName());
		boolean logResultCode;
		Object bean;
		if (p instanceof Rpc<?, ?> rpc) {
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

	public static void log(String action, String id, Protocol<?> p) {
		var sb = new StringBuilder(64);
		sb.append(action).append(':').append(id).append(' ').append(p.getClass().getSimpleName());
		boolean logResultCode;
		Object bean;
		if (p instanceof Rpc<?, ?> rpc) {
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

	public static void log(String action, long sessionId, int moduleId, int protocolId, ByteBuffer bb) {
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

	public static void log(String action, Object idStr, long typeId, ByteBuffer bb) {
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

	public static String toStr(@Nullable Object obj) {
		if (ENABLE_PROTOCOL_LOG_OLD)
			return String.valueOf(obj);
		var jw = JsonWriter.local();
		try {
			return jw.clear().setFlagsAndDepthLimit(JsonWriter.FLAG_NO_QUOTE_KEY, 16).write(obj).toString();
		} finally {
			jw.clear();
		}
	}

	public abstract @Nullable TimeThrottle getTimeThrottle();

	public @Nullable Connector getConnector() {
		return null; // 只有tcp需要重载
	}

	// 连接成功前返回null, 成功后即使close也不会返回null
	public abstract @Nullable SocketAddress getRemoteAddress();

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

	private volatile boolean isHandshakeDone;

	public boolean isHandshakeDone() {
		return isHandshakeDone;
	}

	public void setHandshakeDone(boolean value) {
		isHandshakeDone = value;
	}
}
