package Zeze.Net;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.Handshake.KeepAlive;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Action1;
import Zeze.Util.Factory;
import Zeze.Util.GlobalTimer;
import Zeze.Util.KV;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.LongHashMap;
import Zeze.Util.OutLong;
import Zeze.Util.OutObject;
import Zeze.Util.PerfCounter;
import Zeze.Util.Random;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Service extends ReentrantLock {
	protected static final Logger logger = LogManager.getLogger(Service.class);
	private static final AtomicLong staticSessionIdAtomicLong = new AtomicLong(1);
	private static final @NotNull VarHandle closedRecvCountHandle, closedRecvSizeHandle;
	private static final @NotNull VarHandle closedSendCountHandle, closedSendSizeHandle, closedSendRawSizeHandle;
	protected static final @NotNull VarHandle overflowSizeHandle, overflowCountHandle;
	protected static final IOException serviceStoppedException = new IOException("serviceStopped");
	protected static final IOException inputClosedException = new IOException("inputClosed");
	protected static final IOException throttleException = new IOException("checkThrottle failed");
	public static final IOException keepAliveException = new IOException("checkKeepAlive failed");

	static {
		var lookup = MethodHandles.lookup();
		try {
			closedRecvCountHandle = lookup.findVarHandle(Service.class, "closedRecvCount", long.class);
			closedRecvSizeHandle = lookup.findVarHandle(Service.class, "closedRecvSize", long.class);
			closedSendCountHandle = lookup.findVarHandle(Service.class, "closedSendCount", long.class);
			closedSendSizeHandle = lookup.findVarHandle(Service.class, "closedSendSize", long.class);
			closedSendRawSizeHandle = lookup.findVarHandle(Service.class, "closedSendRawSize", long.class);
			overflowSizeHandle = lookup.findVarHandle(Service.class, "overflowSize", long.class);
			overflowCountHandle = lookup.findVarHandle(Service.class, "overflowCount", int.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private final @NotNull String name;
	private @NotNull String instanceName = ""; // 用来区分多实例的Service，用于日志，不影响逻辑。
	private final Application zeze;
	private @NotNull SocketOptions socketOptions; // 同一个 Service 下的所有连接都是用相同配置。
	private @NotNull ServiceConf config;
	private @NotNull LongSupplier sessionIdGenerator = staticSessionIdAtomicLong::getAndIncrement;
	protected final LongConcurrentHashMap<AsyncSocket> socketMap = new LongConcurrentHashMap<>();
	private final LongConcurrentHashMap<ProtocolFactoryHandle<? extends Protocol<?>>> factorys = new LongConcurrentHashMap<>();
	private final LongConcurrentHashMap<Protocol<?>> rpcContexts = new LongConcurrentHashMap<>();
	private final LongConcurrentHashMap<ManualContext> manualContexts = new LongConcurrentHashMap<>();
	@SuppressWarnings("unused")
	private volatile long closedRecvCount, closedRecvSize, closedSendCount, closedSendSize, closedSendRawSize; // 已关闭连接的从socket接收/已发送数据/准备发送数据的次数和总字节数
	private volatile long recvCount, recvSize, sendCount, sendSize, sendRawSize; // 当前已统计的从socket接收/已发送数据/准备发送数据的次数和总字节数
	@SuppressWarnings("unused")
	protected volatile long overflowSize;
	@SuppressWarnings("unused")
	protected volatile int overflowCount;

	private @Nullable Selectors selectors;
	private @Nullable ScheduledFuture<?> statisticLogFuture;
	private boolean noProcedure = false;
	protected Future<?> keepCheckTimer;

	public @NotNull String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(@NotNull String instanceName) {
		this.instanceName = instanceName;
	}

	public Service(@NotNull String name) {
		this(name, (Config)null);
	}

	public Service(@NotNull String name, @Nullable Config config) {
		this.name = name;
		zeze = null;
		this.config = initConfig(config);
		socketOptions = this.config.getSocketOptions();
		logger.info("start: {}", name);
		tryStartStatisticLog();
	}

	public Service(@NotNull String name, @Nullable Application app) {
		this.name = name;
		zeze = app;
		config = initConfig(app != null ? app.getConfig() : null);
		socketOptions = config.getSocketOptions();
		logger.info("start: {}", name);
		tryStartStatisticLog();
	}

	public boolean isNoProcedure() {
		return noProcedure;
	}

	public void setNoProcedure(boolean value) {
		noProcedure = value;
	}

	private void tryStartStatisticLog() {
		var stat = System.getProperty(name + ".stat");
		if (stat != null && !stat.isBlank()) {
			int periodSec = Integer.parseInt(stat);
			if (periodSec > 0)
				startStatisticLog(periodSec);
		}
	}

	private @NotNull ServiceConf initConfig(@Nullable Config config) {
		var sc = config != null ? config.getServiceConf(name) : null;
		if (sc == null) {
			// setup program default
			sc = new ServiceConf();
			if (config != null) {
				// reference to config default
				sc.setSocketOptions(config.getDefaultServiceConf().getSocketOptions());
				sc.setHandshakeOptions(config.getDefaultServiceConf().getHandshakeOptions());
			}
		}
		sc.setService(this);
		return sc;
	}

	public void setSelectors(@Nullable Selectors selectors) {
		this.selectors = selectors;
	}

	public @NotNull Selectors getSelectors() {
		return null != selectors ? selectors : Selectors.getInstance();
	}

	public final @NotNull String getName() {
		return name;
	}

	public final Application getZeze() {
		return zeze;
	}

	public @NotNull SocketOptions getSocketOptions() {
		return socketOptions;
	}

	public void setSocketOptions(@NotNull SocketOptions ops) {
		//noinspection ConstantValue
		if (ops != null)
			socketOptions = ops;
	}

	public @NotNull ServiceConf getConfig() {
		return config;
	}

	public void setConfig(@NotNull ServiceConf conf) {
		//noinspection ConstantValue
		if (conf != null)
			config = conf;
	}

	public final @NotNull LongSupplier getSessionIdGenerator() {
		return sessionIdGenerator;
	}

	public final void setSessionIdGenerator(@Nullable LongSupplier value) {
		sessionIdGenerator = value != null ? value : staticSessionIdAtomicLong::getAndIncrement;
	}

	public final long nextSessionId() {
		return sessionIdGenerator.getAsLong();
	}

	public final int getSocketCount() {
		return socketMap.size();
	}

	protected final boolean addSocket(@NotNull AsyncSocket so) {
		return socketMap.putIfAbsent(so.getSessionId(), so) == null;
	}

	public final void updateRecvSendSize() {
		long rc = 0, rs = 0, sc = 0, ss = 0, sr = 0;
		for (var socket : socketMap) {
			rc += socket.getRecvCount();
			rs += socket.getRecvSize();
			sc += socket.getSendCount();
			ss += socket.getSendSize();
			sr += socket.getSendRawSize();
		}
		recvCount = closedRecvCount + rc;
		recvSize = closedRecvSize + rs;
		sendCount = closedSendCount + sc;
		sendSize = closedSendSize + ss;
		sendRawSize = closedSendRawSize + sr;
	}

	public final long getRecvCount() {
		return recvCount;
	}

	public final long getRecvSize() {
		return recvSize;
	}

	public final long getSendCount() {
		return sendCount;
	}

	public final long getSendSize() {
		return sendSize;
	}

	public final long getSendRawSize() {
		return sendRawSize;
	}

	/**
	 * 只包含成功建立的连接：服务器Accept和客户端Connected的连接。
	 *
	 * @param sessionId session id
	 * @return Socket Instance.
	 */
	public @Nullable AsyncSocket GetSocket(long sessionId) {
		return socketMap.get(sessionId);
	}

	public @Nullable AsyncSocket GetSocket() {
		var sockets = socketMap.iterator();
		return sockets.hasNext() ? sockets.next() : null;
	}

	public void start() throws Exception {
		config.start();
	}

	public void Start() throws Exception {
		start();
	}

	public void Stop() throws Exception {
		stop();
	}

	public void stop() throws Exception {
		lock();
		try {
			config.stop();

			for (AsyncSocket as : socketMap)
				as.close(serviceStoppedException); // remove in callback OnSocketClose

			// 先不清除，让Rpc的TimerTask仍然在超时以后触发回调。
			// 【考虑一下】也许在服务停止时马上触发回调并且清除上下文比较好。
			// 【注意】直接清除会导致同步等待的操作无法继续。异步只会没有回调，没问题。
			// _RpcContexts.Clear();

			if (keepCheckTimer != null) {
				keepCheckTimer.cancel(true);
				keepCheckTimer = null;
			}
		} finally {
			unlock();
		}
	}

	public final @NotNull AsyncSocket newServerSocket(@Nullable String ipaddress, int port,
													  @Nullable Acceptor acceptor) {
		try {
			return newServerSocket(InetAddress.getByName(ipaddress), port, acceptor);
		} catch (UnknownHostException e) {
			Task.forceThrow(e);
			return null; // never run here
		}
	}

	public final @NotNull AsyncSocket newServerSocket(@Nullable InetAddress ipaddress, int port,
													  @Nullable Acceptor acceptor) {
		return newServerSocket(new InetSocketAddress(ipaddress, port), acceptor);
	}

	public final @NotNull AsyncSocket newServerSocket(@Nullable InetSocketAddress localEP, @Nullable Acceptor acceptor) {
		return new AsyncSocket(this, localEP, acceptor);
	}

	public final @NotNull AsyncSocket newClientSocket(@Nullable String hostNameOrAddress, int port,
													  @Nullable Object userState, @Nullable Connector connector) {
		return new AsyncSocket(this, hostNameOrAddress, port, userState, connector);
	}

	/**
	 * ASocket 关闭的时候总是回调。
	 *
	 * @param so closing socket
	 * @param e  caught exception, null for none.
	 */
	public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
		if (socketMap.remove(so.getSessionId(), so)) {
			closedRecvCountHandle.getAndAdd(this, so.getRecvCount());
			closedRecvSizeHandle.getAndAdd(this, so.getRecvSize());
			closedSendCountHandle.getAndAdd(this, so.getSendCount());
			closedSendSizeHandle.getAndAdd(this, so.getSendSize());
			closedSendRawSizeHandle.getAndAdd(this, so.getSendRawSize());
		}
	}

	/**
	 * 可靠rpc调用：一般用于重新发送没有返回结果的rpc。
	 * 在 OnSocketClose 之后调用，此时外面【必须】拿不到此 AsyncSocket 了。
	 * 当 OnSocketDisposed 调用发生时，AsyncSocket.Socket已经设为 null。
	 * 对于那些在 AsyncSocket.Dispose 时已经得到的 AsyncSocket 引用，
	 * 使用时判断返回值：主要是 Send 返回 false。
	 *
	 * @param so after socket closed. last callback.
	 */
	@SuppressWarnings("RedundantThrows")
	public void OnSocketDisposed(@SuppressWarnings("unused") @NotNull AsyncSocket so) throws Exception {
		// 一般实现：遍历RpcContexts，
		/*
		var ctxSends = GetRpcContextsToSender(so);
		var ctxPending = RemoveRpcContexts(ctxSends.Keys);
		foreach (var ctx in ctxRemoved)
		{
		    // process
		}
		*/
	}

	public final @NotNull Collection<Protocol<?>> removeRpcContexts(@NotNull Collection<Long> sids) {
		var result = new ArrayList<Protocol<?>>(sids.size());
		for (var sid : sids) {
			var ctx = removeRpcContext(sid);
			if (ctx != null)
				result.add(ctx);
		}
		return result;
	}

	/**
	 * 服务器接受到新连接回调。
	 *
	 * @param so new socket accepted.
	 */
	public void OnSocketAccept(@NotNull AsyncSocket so) throws Exception {
		if (socketMap.size() > config.getMaxConnections())
			throw new IllegalStateException("too many connections");
		addSocket(so);
		OnHandshakeDone(so);
	}

	@SuppressWarnings({"RedundantThrows", "MethodMayBeStatic"})
	public void OnSocketAcceptError(@NotNull AsyncSocket listener, @NotNull Throwable e) throws Exception {
		logger.error("OnSocketAcceptError: {} exception:", listener, e);
	}

	/**
	 * 连接完成建立调用。
	 * 未加密压缩的连接在 OnSocketAccept OnSocketConnected 里面调用这个方法。
	 * 加密压缩的连接在相应的方法中调用（see Services\Handshake.cs）。
	 * 注意：修改OnHandshakeDone的时机，需要重载OnSocketAccept OnSocketConnected，并且不再调用Service的默认实现。
	 */
	public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
		so.setHandshakeDone(true);
		if (so.getConnector() != null)
			so.getConnector().OnSocketHandshakeDone(so);
	}

	/**
	 * 连接失败回调。同时也会回调OnSocketClose。
	 *
	 * @param so socket that connect error.
	 * @param e  exception caught
	 */
	@SuppressWarnings("RedundantThrows")
	public void OnSocketConnectError(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
		socketMap.remove(so.getSessionId(), so);
	}

	/**
	 * 连接成功回调。
	 *
	 * @param so connect succeed
	 */
	public void OnSocketConnected(@NotNull AsyncSocket so) throws Exception {
		addSocket(so);
		OnHandshakeDone(so);
	}

	/**
	 * 处理数据。
	 * 在异步线程中回调，要注意线程安全。
	 *
	 * @param so    current socket
	 * @param input 方法外绝对不能持有input及其Bytes的引用! 也就是只能在方法内读input.
	 *              处理了多少要体现在input.ReadIndex上,剩下的等下次收到数据后会继续在此处理.
	 * @return 是否可以立即再次从socket接收数据(如果缓冲区还有数据的话), 否则会等下次select循环再处理
	 */
	public boolean OnSocketProcessInputBuffer(@NotNull AsyncSocket so, @NotNull ByteBuffer input) throws Exception {
		Protocol.decode(this, so, input);
		return true;
	}

	/**
	 * 对方正常关闭连接或者shutdownOutput时的处理, 大多数情况直接关闭连接来应对, 少数情况可以继续发送数据直到主动关闭.
	 * 理论上无法得知对方是否还可以接收数据, 只能靠上层协商行为规范.
	 */
	@SuppressWarnings("MethodMayBeStatic")
	public void OnSocketInputClosed(@NotNull AsyncSocket so) throws Exception {
		so.close(inputClosedException);
	}

	// 用来派发异步rpc回调。
	@SuppressWarnings("RedundantThrows")
	public <P extends Protocol<?>> void dispatchRpcResponse(@NotNull P rpc, @NotNull ProtocolHandle<P> responseHandle,
															@NotNull ProtocolFactoryHandle<?> factoryHandle)
			throws Exception {
		// 一般来说到达这个函数，肯定执行非事务分支了，事务分支在下面的dispatchProtocol中就被拦截。
		// 但为了更具适应性，就是有人重载了下面的dispatchProtocol，然后没有处理事务，直接派发到这里，
		// 这里还是处理了存储过程的创建。但这里处理的存储过程没有redo时重置协议参数的能力。
		Application zeze;
		if (!noProcedure && (zeze = this.zeze) != null && factoryHandle.Level != TransactionLevel.None) {
			Task.executeRpcResponseUnsafe(zeze.newProcedure(() -> responseHandle.handle(rpc),
					rpc.getClass().getName() + ":Response", factoryHandle.Level,
					null != rpc.getSender() ? rpc.getSender().getUserState() : null), factoryHandle.Mode);
		} else
			Task.executeRpcResponseUnsafe(() -> responseHandle.handle(rpc), rpc, factoryHandle.Mode);
	}

	public boolean isHandshakeProtocol(long typeId) {
		return false;
	}

	public final @NotNull Protocol<?> decodeProtocol(long typeId, @NotNull ByteBuffer bb,
													 @NotNull ProtocolFactoryHandle<?> factoryHandle,
													 @Nullable AsyncSocket so) {
		return decodeProtocol(typeId, bb, factoryHandle, so, true);
	}

	@SuppressWarnings("MethodMayBeStatic")
	public @NotNull Protocol<?> decodeProtocol(long typeId, @NotNull ByteBuffer bb,
											   @NotNull ProtocolFactoryHandle<?> factoryHandle,
											   @Nullable AsyncSocket so, boolean needLog) {
		var p = factoryHandle.Factory.create();
		p.decode(bb);
		// 协议必须完整的解码，为了方便应用某些时候设计出兼容的协议。去掉这个检查。
		/*
		if (bb.ReadIndex != endReadIndex)
			throw new IllegalStateException(
					String.format("protocol '%s' in '%s' module=%d protocol=%d size=%d!=%d decode error!",
							p.getClass().getName(), service.getName(), moduleId, protocolId,
							bb.ReadIndex - beginReadIndex, size));
		*/
		p.setSender(so);
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId) && needLog)
			AsyncSocket.log("RECV", so == null ? 0 : so.getSessionId(), p);
		return p;
	}

	// 用来派发已经decode的协议，不支持事务重做时重置协议参数。
	public void dispatchProtocol(@NotNull Protocol<?> p) throws Exception {
		var factoryHandle = findProtocolFactoryHandle(p.getTypeId());
		if (factoryHandle != null)
			dispatchProtocol(p, factoryHandle);
		else
			logger.warn("dispatchProtocol: not found protocol factory: {}", p);
	}

	public void dispatchProtocol(@NotNull Protocol<?> p, @NotNull ProtocolFactoryHandle<?> factoryHandle)
			throws Exception {
		Application zeze;
		// 一般来说到达这个函数，肯定执行非事务分支了，事务分支在下面的dispatchProtocol中就被拦截。
		// 但为了更具适应性，就是有人重载了下面的dispatchProtocol，然后没有处理事务，直接派发到这里，
		// 这里还是处理了存储过程的创建。但这里处理的存储过程没有redo时重置协议参数的能力。
		if (!noProcedure && factoryHandle.Level != TransactionLevel.None && (zeze = this.zeze) != null) {
			var protocolClassName = p.getClass().getName();
			var proc = zeze.newProcedure(() -> p.handle(this, factoryHandle), protocolClassName,
					factoryHandle.Level, null != p.getSender() ? p.getSender().getUserState() : null);
			Task.executeUnsafe(proc, p, Protocol::trySendResultCode, factoryHandle.Mode);
		} else {
			Task.executeUnsafe(() -> p.handle(this, factoryHandle),
					p, Protocol::trySendResultCode, null, factoryHandle.Mode);
		}
	}

	public void dispatchProtocol(long typeId, @NotNull ByteBuffer bb, @NotNull ProtocolFactoryHandle<?> factoryHandle,
								 @Nullable AsyncSocket so) throws Exception {
		if (isHandshakeProtocol(typeId)) {
			// handshake protocol call direct in io-thread.
			var p = decodeProtocol(typeId, bb, factoryHandle, so);
			Task.call(() -> p.handle(this, factoryHandle), "Service.handleHandshakeProtocol");
			return;
		}
		Application zeze;
		if (!noProcedure && factoryHandle.Level != TransactionLevel.None && (zeze = this.zeze) != null) {
			// 事务模式，需要从decode重启。
			// 传给事务的buffer可能重做需要重新decode，不能直接引用网络层的buffer，需要copy一次。
			var protocolRawArgument = new Binary(bb.Copy());
			var bbCopy = ByteBuffer.Wrap(protocolRawArgument);
			var outProtocol = new OutObject<Protocol<?>>();
			var protocolClassName = factoryHandle.Class.getName();
			var proc = zeze.newProcedure(() -> {
				var needLog = bbCopy.ReadIndex == 0;
				bbCopy.ReadIndex = 0; // 考虑redo,要重置读指针
				var p = decodeProtocol(typeId, bbCopy, factoryHandle, so, needLog);
				outProtocol.value = p;
				return p.handle(this, factoryHandle);
			}, protocolClassName, factoryHandle.Level, so != null ? so.getUserState() : null);
			proc.setProtocolClassName(protocolClassName);
			proc.setProtocolRawArgument(protocolRawArgument);
			Task.executeUnsafe(proc, outProtocol, Protocol::trySendResultCode, factoryHandle.Mode);
		} else {
			var p = decodeProtocol(typeId, bb, factoryHandle, so);
			// 其他协议或者rpc，马上在io线程继续派发。
			// 对于rpc.response还会继续调用dispatchRpcResponse继续派发。
			p.dispatch(this, factoryHandle);
		}
	}

	/**
	 * @param data 方法外绝对不能持有data及其Bytes的引用! 也就是只能在方法内读data, 只能处理data.ReadIndex到data.WriteIndex范围内
	 */
	@SuppressWarnings("RedundantThrows")
	public void dispatchUnknownProtocol(@NotNull AsyncSocket so, int moduleId, int protocolId, @NotNull ByteBuffer data)
			throws Exception {
		throw new UnsupportedOperationException(getName() + " Unknown Protocol (" + moduleId + ", " + protocolId
				+ ") size=" + data.size() + " so=" + so);
	}

	@SuppressWarnings("RedundantThrows")
	public boolean checkOverflow(@NotNull AsyncSocket so, long newSize, byte @NotNull [] bytes, int offset, int length)
			throws Exception {
		var maxSize = getSocketOptions().getOutputBufferMaxSize();
		if (newSize <= maxSize)
			return true;
		overflowSizeHandle.getAndAdd(this, (long)length);
		if ((int)overflowCountHandle.getAndAdd(this, 1) == 0) {
			Task.scheduleUnsafe(1000, () -> logger.error("Send overflow(>{}): {} dropped {}/{}",
					maxSize, this, overflowSizeHandle.getAndSet(this, 0L), overflowCountHandle.getAndSet(this, 0)));
		}
		return false;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 协议工厂
	 */
	public static class ProtocolFactoryHandle<P extends Protocol<?>> {
		public final @NotNull Class<P> Class;
		public final long TypeId;
		public Factory<P> Factory;
		public @Nullable ProtocolHandle<P> Handle;
		public TransactionLevel Level;
		public DispatchMode Mode;
		public int CriticalLevel = Protocol.eCriticalPlus;

		public ProtocolFactoryHandle(@NotNull Class<P> protocolClass, long typeId) {
			Class = protocolClass;
			TypeId = typeId;
			Level = TransactionLevel.Serializable;
			Mode = DispatchMode.Normal;
		}

		public ProtocolFactoryHandle(@NotNull Factory<P> factory) {
			this(factory, null, TransactionLevel.Serializable, DispatchMode.Normal);
		}

		public ProtocolFactoryHandle(@NotNull Factory<P> factory, @Nullable ProtocolHandle<P> handle) {
			this(factory, handle, TransactionLevel.Serializable, DispatchMode.Normal);
		}

		public ProtocolFactoryHandle(@NotNull Factory<P> factory, @Nullable ProtocolHandle<P> handle,
									 @NotNull TransactionLevel level) {
			this(factory, handle, level, DispatchMode.Normal);
		}

		@SuppressWarnings("unchecked")
		public ProtocolFactoryHandle(@NotNull Factory<P> factory, @Nullable ProtocolHandle<P> handle,
									 @NotNull TransactionLevel level, @NotNull DispatchMode mode) {
			P p = factory.create();
			Class = (Class<P>)p.getClass();
			TypeId = p.getTypeId();
			Factory = factory;
			Handle = handle;
			Level = level;
			Mode = mode;
			CriticalLevel = p.getCriticalLevel();
		}
	}

	public final @NotNull LongConcurrentHashMap<ProtocolFactoryHandle<? extends Protocol<?>>> getFactorys() {
		return factorys;
	}

	public final void AddFactoryHandle(long type, @NotNull ProtocolFactoryHandle<? extends Protocol<?>> factory) {
		if (factorys.putIfAbsent(type, factory) != null)
			throw new IllegalStateException(String.format("duplicate factory type=%d moduleId=%d id=%d",
					type, type >>> 32, type & 0xffff_ffffL));
	}

	public final @Nullable ProtocolFactoryHandle<? extends Protocol<?>> findProtocolFactoryHandle(long type) {
		return factorys.get(type);
	}

	/**
	 * Rpc Context. 模板不好放进去，使用基类 Protocol
	 */
	public final long addRpcContext(@NotNull Protocol<?> p) {
		while (true) {
			long sessionId = nextSessionId();
			if (rpcContexts.putIfAbsent(sessionId, p) == null)
				return sessionId;
		}
	}

	void addRpcContext(long sessionId, @NotNull Protocol<?> p) {
		rpcContexts.putIfAbsent(sessionId, p);
	}

	@SuppressWarnings("unchecked")
	public final <T extends Protocol<?>> @Nullable T removeRpcContext(long sid) {
		return (T)rpcContexts.remove(sid);
	}

	public final boolean removeRpcContext(long sid, @NotNull Protocol<?> ctx) {
		return rpcContexts.remove(sid, ctx);
	}

	// Not Need Now
	public final @NotNull LongHashMap<Protocol<?>> getRpcContextsToSender(@NotNull AsyncSocket sender) {
		return getRpcContexts(p -> p.getSender() == sender);
	}

	public final @NotNull LongHashMap<Protocol<?>> getRpcContexts(@NotNull Predicate<Protocol<?>> filter) {
		var result = new LongHashMap<Protocol<?>>(Math.max(rpcContexts.size(), 1024)); // 初始容量先别定太大,可能只过滤出一小部分
		for (var it = rpcContexts.entryIterator(); it.moveToNext(); ) {
			if (filter.test(it.value()))
				result.put(it.key(), it.value());
		}
		return result;
	}

	public static abstract class ManualContext {
		private long sessionId;
		private @Nullable Object userState;
		private boolean isTimeout;
		private Service service;

		public final long getSessionId() {
			return sessionId;
		}

		public final void setSessionId(long value) {
			sessionId = value;
		}

		public final @Nullable Object getUserState() {
			return userState;
		}

		public final void setUserState(@Nullable Object value) {
			userState = value;
		}

		public boolean isTimeout() {
			return isTimeout;
		}

		void setIsTimeout(boolean value) {
			isTimeout = value;
		}

		public Service getService() {
			return service;
		}

		public void setService(Service service) {
			this.service = service;
		}

		@SuppressWarnings("RedundantThrows")
		public void onRemoved() throws Exception {
		}
	}

	public final long addManualContextWithTimeout(@NotNull ManualContext context) {
		return addManualContextWithTimeout(context, 10 * 1000);
	}

	public final long addManualContextWithTimeout(@NotNull ManualContext context, long timeout) { // 毫秒
		while (true) {
			long sessionId = nextSessionId();
			if (manualContexts.putIfAbsent(sessionId, context) == null) {
				context.setSessionId(sessionId);
				context.setService(this);
				Task.schedule(timeout, () -> tryRemoveManualContext(sessionId, true));
				return sessionId;
			}
		}
	}

	@SuppressWarnings("unchecked")
	public final <T extends ManualContext> @Nullable T tryGetManualContext(long sessionId) {
		return (T)manualContexts.get(sessionId);
	}

	public final <T extends ManualContext> @Nullable T tryRemoveManualContext(long sessionId) {
		return tryRemoveManualContext(sessionId, false);
	}

	private <T extends ManualContext> @Nullable T tryRemoveManualContext(long sessionId, boolean isTimeout) {
		@SuppressWarnings("unchecked")
		var r = (T)manualContexts.remove(sessionId);
		if (r != null) {
			try {
				r.setIsTimeout(isTimeout);
				r.onRemoved();
			} catch (Throwable e) { // run handle. 必须捕捉所有异常。
				logger.error("ManualContext.onRemoved exception:", e);
			}
		}
		return r;
	}

	// 还是不直接暴露内部的容器。提供这个方法给外面用。以后如果有问题，可以改这里。

	public final void foreach(@NotNull Action1<@NotNull AsyncSocket> action) throws Exception {
		for (var socket : socketMap)
			action.run(socket);
	}

	public @NotNull KV<@NotNull String, @NotNull Integer> getOneAcceptorAddress() {
		var ipPort = KV.create("", 0);

		config.forEachAcceptor2(a -> {
			if (!a.getIp().isEmpty() && a.getPort() != 0) {
				// 找到ip，port都配置成明确地址的。
				ipPort.setKey(a.getIp());
				ipPort.setValue(a.getPort());
				return false;
			}
			// 获得最后一个配置的port。允许返回(null, port)。
			ipPort.setValue(a.getPort());
			return true;
		});

		return ipPort;
	}

	public @NotNull KV<@NotNull String, @NotNull Integer> getOnePassiveAddress() {
		var ipPort = getOneAcceptorAddress();
		// 允许系统来选择端口。
		//if (ipPort.getValue() == 0)
		//	throw new IllegalStateException("Acceptor: No Config.");

		if (ipPort.getKey().equals("@internal") || ipPort.getKey().isBlank())
			ipPort.setKey(Helper.selectOneIpAddress(true));
		else if (ipPort.getKey().equals("@external"))
			ipPort.setKey(Helper.selectOneIpAddress(false));

		if (ipPort.getKey().isEmpty()) {
			// 实在找不到ip地址，就设置成loopback。
			logger.warn("PassiveAddress No Config. set ip to 127.0.0.1");
			ipPort.setKey("127.0.0.1");
		}
		return ipPort;
	}

	public void onServerSocketBind(@NotNull ServerSocket port) {
	}

	/**
	 * 接收一个协议(尚未处理)时,判断单个socket是否接收超限
	 *
	 * @return 是否检查通过, false则丢弃该协议(也可以同时关闭连接)
	 */
	@SuppressWarnings("MethodMayBeStatic")
	public boolean checkThrottle(@NotNull AsyncSocket sender, int moduleId, int protocolId, int size) {
		var throttle = sender.getTimeThrottle();
		if (null != throttle && !throttle.checkNow(size)) {
			// trySendResultCode(Procedure.Busy); // 超过速度限制，不报告错误。因为可能是一种攻击。
			sender.close(throttleException); // 默认关闭连接。
			return false; // 超过速度控制，丢弃这条协议。
		}
		return true;
	}

	public boolean discard(@NotNull AsyncSocket sender, int moduleId, int protocolId, int size) throws Exception {
		return false;
	}

	public @NotNull ScheduledFuture<?> startStatisticLog(int periodSec) {
		lock();
		try {
			var f = statisticLogFuture;
			if (f != null && !f.isCancelled())
				return f;
			var lastSizes = new long[6];
			lastSizes[0] = -1;
			f = Task.scheduleUnsafe(Random.getInstance().nextLong(periodSec * 1000L), periodSec * 1000L, () -> {
				updateRecvSendSize();
				var selectors = getSelectors();
				long selectCount = selectors.getSelectCount();
				long recvCount = this.recvCount;
				long recvSize = this.recvSize;
				long sendCount = this.sendCount;
				long sendSize = this.sendSize;
				long sendRawSize = this.sendRawSize;
				if (lastSizes[0] != -1) {
					long sn = (selectCount - lastSizes[0]) / periodSec;
					long rc = (recvCount - lastSizes[1]) / periodSec;
					long rs = (recvSize - lastSizes[2]) / periodSec;
					long sc = (sendCount - lastSizes[3]) / periodSec;
					long ss = (sendSize - lastSizes[4]) / periodSec;
					long sr = (sendRawSize - lastSizes[5]) / periodSec;
					var operates = new OutLong();
					var outBufSize = new OutLong();
					foreach(socket -> {
						operates.value += socket.getOperateSize();
						outBufSize.value += socket.getOutputBufferSize();
					});
					operates.value /= periodSec;
					outBufSize.value /= periodSec;
					PerfCounter.logger.info(
							"{}.{}.stat: select={}/{}, recv={}/{}, send={}/{}, sendRaw={}, sockets={}, ops={}, outBuf={}",
							name, instanceName, sn, selectors.getCount(), rs, rc, ss, sc, sr, getSocketCount(),
							operates.value, outBufSize.value);
				}
				lastSizes[0] = selectCount;
				lastSizes[1] = recvCount;
				lastSizes[2] = recvSize;
				lastSizes[3] = sendCount;
				lastSizes[4] = sendSize;
				lastSizes[5] = sendRawSize;
			});
			statisticLogFuture = f;
			return f;
		} finally {
			unlock();
		}
	}

	public boolean cancelStartStatisticLog() {
		lock();
		try {
			var f = statisticLogFuture;
			statisticLogFuture = null;
			return f != null && f.cancel(false);
		} finally {
			unlock();
		}
	}

	@SuppressWarnings("MethodMayBeStatic")
	public void onRpcLostContext(@NotNull Rpc<?, ?> rpc) {
		logger.warn("rpc response: lost context, maybe timeout. {}", rpc);
	}

	public void tryStartKeepAliveCheckTimer() {
		lock();
		try {
			if (keepCheckTimer == null) {
				var period = getConfig().getHandshakeOptions().getKeepCheckPeriod() * 1000L;
				if (period > 0) {
					keepCheckTimer = Task.scheduleUnsafe(
							Random.getInstance().nextLong(period) + 1, period, this::checkKeepAlive);
				}
			}
		} finally {
			unlock();
		}
	}

	private void checkKeepAlive() throws Exception {
		var conf = getConfig().getHandshakeOptions();
		var keepRecvTimeout = conf.getKeepRecvTimeout() > 0 ? conf.getKeepRecvTimeout() : Integer.MAX_VALUE;
		var keepSendTimeout = conf.getKeepSendTimeout() > 0 ? conf.getKeepSendTimeout() : Integer.MAX_VALUE;
		int now = (int)GlobalTimer.getCurrentSeconds();
		foreach(socket -> {
			if (now - socket.getActiveRecvTime() > keepRecvTimeout) {
				try {
					onKeepAliveTimeout(socket);
				} catch (Exception e) {
					logger.error("onKeepAliveTimeout exception:", e);
				}
			}
			if (socket.getType() == AsyncSocket.Type.eClient && now - socket.getActiveSendTime() > keepSendTimeout) {
				try {
					onSendKeepAlive(socket);
				} catch (Exception e) {
					logger.error("onSendKeepAlive exception:", e);
				}
			}
		});
	}

	@SuppressWarnings("MethodMayBeStatic")
	protected void onKeepAliveTimeout(@NotNull AsyncSocket socket) throws Exception {
		socket.close(keepAliveException);
	}

	/**
	 * 1. 如果你是handshake的service，重载这个方法，按注释发送KeepAlive即可【已改成默认发送，不需要操作】；
	 * 2. 如果你是其他service子类，重载这个方法，按注释发送KeepAlive，并且服务器端需要注册这条协议并写一个不需要处理代码的handler；
	 * 3. 如果不发送, 会导致KeepTimerClient时间后再次触发, 也可以调用socket.setActiveSendTime()避免频繁触发。
	 *
	 * @param socket 当前连接
	 */
	@SuppressWarnings("MethodMayBeStatic")
	protected void onSendKeepAlive(@NotNull AsyncSocket socket) {
		KeepAlive.instance.Send(socket); // skip result
	}
}
