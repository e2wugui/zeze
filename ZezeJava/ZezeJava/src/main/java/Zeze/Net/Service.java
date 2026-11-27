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
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.Handshake.KeepAlive;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Action1;
import Zeze.Util.Factory;
import Zeze.Util.FuncLong;
import Zeze.Util.GlobalTimer;
import Zeze.Util.KVNotNull;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.LongHashMap;
import Zeze.Util.LongHashSet;
import Zeze.Util.OutObject;
import Zeze.Util.Random;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import Zeze.Util.ZezeCounter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * 网络服务基类：管理连接（socket）生命周期、协议工厂注册、协议解码与事务派发。
 */
public class Service extends ReentrantLock {
	protected static final Logger logger = LogManager.getLogger(Service.class);
	// 默认共享发号流使用随机 63 位基址，避免同 JVM 多 Service 或跨代重启撞号。
	private static final AtomicLong staticSessionIdAtomicLong = new AtomicLong(
			(System.nanoTime() ^ new java.security.SecureRandom().nextLong()) & Long.MAX_VALUE);
	private static final VarHandle closedRecvCountHandle, closedRecvSizeHandle;
	private static final VarHandle closedSendCountHandle, closedSendSizeHandle, closedSendRawSizeHandle;
	protected static final VarHandle overflowSizeHandle, overflowCountHandle;
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

	private final String name;
	private String instanceName = ""; // 用来区分多实例的Service，用于日志，不影响逻辑。
	private final @Nullable Application zeze;
	private SocketOptions socketOptions; // 同一个 Service 下的所有连接都是用相同配置。
	private ServiceConf config;
	private LongSupplier sessionIdGenerator = staticSessionIdAtomicLong::getAndIncrement;
	protected final LongConcurrentHashMap<AsyncSocket> socketMap = new LongConcurrentHashMap<>();
	private final LongConcurrentHashMap<ProtocolFactoryHandle<? extends Protocol<?>>> factorys = new LongConcurrentHashMap<>();
	private final LongConcurrentHashMap<Rpc<?, ?>> rpcContexts = new LongConcurrentHashMap<>();
	private final LongConcurrentHashMap<ManualContext> manualContexts = new LongConcurrentHashMap<>();
	private volatile long closedRecvCount, closedRecvSize, closedSendCount, closedSendSize, closedSendRawSize; // 已关闭连接的从socket接收/已发送数据/准备发送数据的次数和总字节数
	private volatile long recvCount, recvSize, sendCount, sendSize, sendRawSize; // 当前已统计的从socket接收/已发送数据/准备发送数据的次数和总字节数
	protected volatile long overflowSize;
	protected volatile int overflowCount;

	private @Nullable Selectors selectors;
	private boolean noProcedure;
	protected volatile @Nullable Future<?> keepCheckTimer; // 无锁快路径在 Service 锁外读
	private volatile boolean keepAliveCheckStopped; // stop 时置位以熔断保活定时器懒启动重试
	private @Nullable ServiceStatisticLog servicePerf;
	// 门禁白名单：非空时未完成密钥交换的连接仅放行白名单内的协议。
	private final LongHashSet decodeAdmissionWhitelist = new LongHashSet();

	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	// 停机屏障：stop 最前置位、start 复位，addSocket 登记后复查以防迟到连接泄漏。
	private volatile boolean stopped;

	public Service(String name) {
		this(name, null, null);
	}

	public Service(String name, @Nullable Config config) {
		this(name, null, config);
	}

	public Service(String name, @Nullable Application app) {
		this(name, app, app != null ? app.getConfig() : null);
	}

	public Service(String name, @Nullable Application app, @Nullable Config config) {
		this.name = name;
		zeze = app;
		this.config = initConfig(config);
		socketOptions = this.config.getSocketOptions();
		noProcedure = app == null || app.isNoDatabase();
		var template = defaultSessionIdGenFunc;
		if (template != null)
			sessionIdGenerator = template;
		logger.info("start: {}", name);
		tryStartStatisticLog();
	}

	private static volatile @Nullable LongSupplier defaultSessionIdGenFunc;

	/** 兼容全局发号安装的模板入口：仅对之后构造的 Service 生效，自定义发号请用 {@link #setSessionIdGenerator}。 */
	public static void setDefaultSessionIdGenFunc(@Nullable LongSupplier seed) {
		defaultSessionIdGenFunc = seed;
	}

	public boolean isNoProcedure() {
		return noProcedure;
	}

	public void setNoProcedure(boolean value) {
		noProcedure = value || zeze == null || zeze.isNoDatabase();
	}

	private void tryStartStatisticLog() {
		var stat = System.getProperty(name + ".stat");
		if (stat != null && !stat.isBlank()) {
			int periodSec = Integer.parseInt(stat);
			if (periodSec > 0)
				startStatisticLog(periodSec);
		}
	}

	private ServiceConf initConfig(@Nullable Config config) {
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

	public Selectors getSelectors() {
		return null != selectors ? selectors : Selectors.getInstance();
	}

	public final String getName() {
		return name;
	}

	public final @Nullable Application getZeze() {
		return zeze;
	}

	public SocketOptions getSocketOptions() {
		return socketOptions;
	}

	public void setSocketOptions(SocketOptions ops) {
		//noinspection ConstantValue
		if (ops != null)
			socketOptions = ops;
	}

	public ServiceConf getConfig() {
		return config;
	}

	public void setConfig(ServiceConf conf) {
		//noinspection ConstantValue
		if (conf != null)
			config = conf;
	}

	public final LongSupplier getSessionIdGenerator() {
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

	/**
	 * 会话按 sessionId 登记入表（先注册者胜）；已关闭或撞号时返回 false（撞号关闭新连接），
	 * 返回 false 时调用方不得继续回调 OnHandshakeDone。
	 */
	protected final boolean addSocket(AsyncSocket so) {
		var existing = so.runIfOpen(() -> {
			var e = socketMap.putIfAbsent(so.getSessionId(), so);
			return e != null ? e : so; // 插入成功以 so 自身区分 runIfOpen 的 closed-null
		});
		if (existing == null)
			return false;
		if (existing != so) {
			logger.error("addSocket: duplicate sessionId {} in service '{}': existing socket {} kept, "
							+ "colliding socket {} closed. Usually caused by multiple apps in one JVM overriding "
							+ "the global static AsyncSocket.setSessionIdGenFunc, making new ids collide "
							+ "with existing sessions.",
					so.getSessionId(), name, existing, so);
			so.close(new IllegalStateException("duplicate session id: " + so.getSessionId()));
			return false;
		}
		// 登记后复查停机屏障：与 stop() 的置位+遍历形成双向兜底，避免迟到连接泄漏。
		if (stopped) {
			so.close(serviceStoppedException);
			return false;
		}
		return true;
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
		stopped = false;
		keepAliveCheckStopped = false;
		tryStartKeepAliveCheckTimer();
		config.start();
		ZezeCounter.instance.serviceStart(this);
	}

	/**
	 * 停止服务：关闭全部连接并停止保活定时器。
	 * 锁序：本方法持 Service 锁同步触发 {@link #OnSocketClose}，故锁序恒为 Service 锁 → 回调业务锁；
	 * keepCheckTimer 的 cancel 在锁外执行，避免与在飞心跳回调死锁。
	 */
	public void stop() throws Exception {
		stopped = true;
		config.stop();
		Future<?> keepTimer;
		lock();
		try {
			for (AsyncSocket as : socketMap)
				as.close(serviceStoppedException); // remove in callback OnSocketClose

			// 不清除 rpcContexts：让 Rpc 超时或 OnSocketDisposed 正常收尾，避免卡死同步等待。

			keepTimer = keepCheckTimer;
			keepCheckTimer = null;
			keepAliveCheckStopped = true;

			ZezeCounter.instance.serviceStop(this);
		} finally {
			unlock();
		}
		// 锁外 cancel 定时器，避免与正停在子类 OnSocketClose 的在飞心跳任务死锁。
		if (keepTimer != null)
			keepTimer.cancel(true);
	}

	public final AsyncSocket newServerSocket(@Nullable String ipaddress, int port,
													  @Nullable Acceptor acceptor) {
		try {
			return newServerSocket(InetAddress.getByName(ipaddress), port, acceptor);
		} catch (UnknownHostException e) {
			throw Task.forceThrow(e);
		}
	}

	public final AsyncSocket newServerSocket(@Nullable InetAddress ipaddress, int port,
													  @Nullable Acceptor acceptor) {
		return newServerSocket(new InetSocketAddress(ipaddress, port), acceptor);
	}

	public final AsyncSocket newServerSocket(@Nullable InetSocketAddress localEP, @Nullable Acceptor acceptor) {
		return new TcpSocket(this, localEP, acceptor);
	}

	public final AsyncSocket newClientSocket(@Nullable String hostNameOrAddress, int port,
													  @Nullable Object userState, @Nullable Connector connector) {
		return new TcpSocket(this, hostNameOrAddress, port, userState, connector);
	}

	/** 客户端连接 DNS 解析扩展点（由 TcpSocket 在专用解析线程上异步调用，允许阻塞）。 */
	protected InetAddress resolveAddress(@Nullable String hostNameOrAddress) throws IOException {
		return InetAddress.getByName(hostNameOrAddress);
	}

	// 构造须保持非阻塞（可能在 Connector 锁内调用）。
	public AsyncSocket newWebsocketClient(String url,
												   @Nullable Object userState, @Nullable Connector connector) {
		return new WebsocketClient(this, url, userState, connector);
	}

	/**
	 * ASocket 关闭的时候总是回调。
	 *
	 * @param so closing socket
	 * @param e  caught exception, null for none.
	 */
	public void OnSocketClose(AsyncSocket so, @Nullable Throwable e) throws Exception {
		if (socketMap.remove(so.getSessionId(), so)) {
			closedRecvCountHandle.getAndAdd(this, so.getRecvCount());
			closedRecvSizeHandle.getAndAdd(this, so.getRecvSize());
			closedSendCountHandle.getAndAdd(this, so.getSendCount());
			closedSendSizeHandle.getAndAdd(this, so.getSendSize());
			closedSendRawSizeHandle.getAndAdd(this, so.getSendRawSize());
		}
	}

	/**
	 * 连接销毁回调（在 {@link #OnSocketClose} 之后触发）：默认使仍挂在本连接上的在飞 Rpc 立即失败
	 * （future 置 {@link RpcSocketDisposedException}，回调以 {@link Procedure#ErrorSendFail} 派发）。
	 *
	 * @param so after socket closed. last callback.
	 */
	public void OnSocketDisposed(AsyncSocket so) throws Exception {
		var ctxSends = getRpcContextsToSender(so);
		if (ctxSends.isEmpty())
			return;
		for (var it = ctxSends.iterator(); it.moveToNext(); ) {
			try {
				var rpc = it.value();
				if (!removeRpcContext(it.key(), rpc))
					continue;
				rpc.setResultCode(Procedure.ErrorSendFail);
				var future = rpc.getFuture();
				if (future != null)
					future.setException(RpcSocketDisposedException.getInstance());
				else {
					@SuppressWarnings("unchecked")
					var handle = (ProtocolHandle<Rpc<?, ?>>)(ProtocolHandle<?>)rpc.getResponseHandle();
					if (handle != null) {
						var factoryHandle = findProtocolFactoryHandle(rpc.getTypeId());
						if (factoryHandle != null)
							dispatchRpcResponse(rpc, handle, factoryHandle);
						else
							logger.warn("rpc disposed: protocol factory not found, response handle skipped: {}", rpc);
					}
				}
			} catch (Throwable e) {
				logger.error("rpc disposed: context {} dispatch failed:", it.key(), e);
			}
		}
	}

	public final Collection<Rpc<?, ?>> removeRpcContexts(Collection<Long> sids) {
		var result = new ArrayList<Rpc<?, ?>>(sids.size());
		for (var sid : sids) {
			var ctx = removeRpcContext(sid);
			if (ctx != null)
				result.add(ctx);
		}
		return result;
	}

	/**
	 * 接受新连接的公共流程：限流、安装 HaProxy 头解析器、登记入表并回调 {@link #OnHandshakeDone}。
	 * 连接数超限时抛 {@link IllegalStateException}；登记失败时静默返回。
	 */
	protected final void tryAccept(AsyncSocket so) throws Exception {
		if (socketMap.size() >= config.getMaxConnections()) // 这里可能有并发原子性问题,不能保证限制在max以内
			throw new IllegalStateException("too many connections");
		setupHaProxyHeader(so);
		if (!addSocket(so))
			return;
		OnHandshakeDone(so);
	}

	/**
	 * 服务器接受到新连接回调。
	 *
	 * @param so new socket accepted.
	 */
	public void OnSocketAccept(AsyncSocket so) throws Exception {
		tryAccept(so);
	}

	/**
	 * 按配置给新接受的连接安装 HaProxy 头解析器；覆写 {@link #OnSocketAccept} 的子类须在收包前调用。
	 */
	protected final void setupHaProxyHeader(AsyncSocket so) {
		if (config.getHaProxyKey() != null && so instanceof TcpSocket tcp)
			tcp.setHaProxyHeader(new HaProxyHeader(config.getHaProxyKey()));
	}

	@SuppressWarnings({"RedundantThrows", "MethodMayBeStatic"})
	public void OnSocketAcceptError(AsyncSocket listener, Throwable e) throws Exception {
		logger.error("OnSocketAcceptError: {} exception:", listener, e);
	}

	/**
	 * 连接完成建立调用。
	 * 未加密压缩的连接在 OnSocketAccept OnSocketConnected 里面调用这个方法。
	 * 加密压缩的连接在相应的方法中调用（see Services\Handshake.cs）。
	 * 注意：修改OnHandshakeDone的时机，需要重载OnSocketAccept OnSocketConnected，并且不再调用Service的默认实现。
	 */
	public void OnHandshakeDone(AsyncSocket so) throws Exception {
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
	public void OnSocketConnectError(AsyncSocket so, @Nullable Throwable e) throws Exception {
		socketMap.remove(so.getSessionId(), so);
	}

	/**
	 * 连接成功回调。
	 *
	 * @param so connect succeed
	 */
	public void OnSocketConnected(AsyncSocket so) throws Exception {
		if (!addSocket(so))
			return;
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
	public boolean OnSocketProcessInputBuffer(AsyncSocket so, ByteBuffer input) throws Exception {
		if (so instanceof TcpSocket tcp) {
			var haProxyHeader = tcp.getHaProxyHeader();
			if (haProxyHeader != null && !haProxyHeader.decodeHeader(input))
				return true; // 没有解析完header，看作成功。
		}
		Protocol.decode(this, so, input);
		return true;
	}

	/** 登记门禁白名单协议：须在建立连接前调用（非线程安全），非空时未完成密钥交换的连接仅放行白名单协议。 */
	public void armDecodeAdmission(long typeId) {
		decodeAdmissionWhitelist.add(typeId);
	}

	/** 返回连接级解码准入谓词（供 TcpSocket 构造装配，双向安全 codec 装齐时撤销）；null 表示不限制。 */
	public @Nullable LongPredicate getConnectionDecodeAdmission() {
		if (!decodeAdmissionWhitelist.isEmpty())
			return decodeAdmissionWhitelist::contains;
		return null;
	}

	/**
	 * 对方正常关闭连接或者shutdownOutput时的处理, 大多数情况直接关闭连接来应对, 少数情况可以继续发送数据直到主动关闭.
	 * 理论上无法得知对方是否还可以接收数据, 只能靠上层协商行为规范.
	 */
	@SuppressWarnings({"RedundantThrows"})
	public void OnSocketInputClosed(AsyncSocket so) throws Exception {
		so.close(inputClosedException);
	}

	// 用来派发异步rpc回调。
	@SuppressWarnings("RedundantThrows")
	public <P extends Protocol<?>> void dispatchRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
															ProtocolFactoryHandle<?> factoryHandle)
			throws Exception {
		// 一般来说到达这个函数，肯定执行非事务分支了，事务分支在下面的dispatchProtocol中就被拦截。
		// 但为了更具适应性，就是有人重载了下面的dispatchProtocol，然后没有处理事务，直接派发到这里，
		// 这里还是处理了存储过程的创建。但这里处理的存储过程没有redo时重置协议参数的能力。
		if (!noProcedure && factoryHandle.Level != TransactionLevel.None) {
			TaskSpec.ofProcedure(Objects.requireNonNull(zeze, "service has no application").newProcedure(() -> responseHandle.handle(rpc),
					rpc.getClass().getName() + ":Response", factoryHandle.Level))
					.dispatchMode(factoryHandle.Mode).runNow();
		} else
			TaskSpec.ofFunc(() -> responseHandle.handle(rpc), rpc).dispatchMode(factoryHandle.Mode).runNow();
	}

	public boolean isHandshakeProtocol(long typeId) {
		return false;
	}

	public final Protocol<?> decodeProtocol(long typeId, ByteBuffer bb,
													 ProtocolFactoryHandle<?> factoryHandle,
													 @Nullable AsyncSocket so) {
		return decodeProtocol(typeId, bb, factoryHandle, so, true);
	}

	public Protocol<?> decodeProtocol(long typeId, ByteBuffer bb,
											   ProtocolFactoryHandle<?> factoryHandle,
											   @Nullable AsyncSocket so, boolean needLog) {
		//noinspection DataFlowIssue
		var p = factoryHandle.Factory.create();
		p.decode(bb);
		// 不强制协议完整解码（不检查bb是否读完），方便应用设计兼容协议。
		p.setSender(so);
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId) && needLog)
			AsyncSocket.log("RECV", so == null ? 0 : so.getSessionId(), p);
		return p;
	}

	// 用来派发已经decode的协议，不支持事务重做时重置协议参数。
	public void dispatchProtocol(Protocol<?> p) throws Exception {
		var factoryHandle = findProtocolFactoryHandle(p.getTypeId());
		if (factoryHandle != null)
			dispatchProtocol(p, factoryHandle);
		else
			logger.warn("dispatchProtocol: not found protocol factory: {}", p);
	}

	public void dispatchProtocol(Protocol<?> p, ProtocolFactoryHandle<?> factoryHandle)
			throws Exception {
		// 一般来说到达这个函数，肯定执行非事务分支了，事务分支在下面的dispatchProtocol中就被拦截。
		// 但为了更具适应性，就是有人重载了下面的dispatchProtocol，然后没有处理事务，直接派发到这里，
		// 这里还是处理了存储过程的创建。但这里处理的存储过程没有redo时重置协议参数的能力。
		if (!noProcedure && factoryHandle.Level != TransactionLevel.None) {
			var protocolClassName = p.getClass().getName();
			var proc = Objects.requireNonNull(zeze, "service has no application").newProcedure(() -> p.handle(this, factoryHandle), protocolClassName, factoryHandle.Level);
			TaskSpec.ofProcedure(proc, p, (__, code) -> p.trySendResultCode(code))
					.dispatchMode(factoryHandle.Mode).runNow();
		} else {
			TaskSpec.ofFunc(() -> p.handle(this, factoryHandle), p, (__, code) -> p.trySendResultCode(code))
					.dispatchMode(factoryHandle.Mode).runNow();
		}
	}

	public void dispatchProtocol(long typeId, ByteBuffer bb, ProtocolFactoryHandle<?> factoryHandle,
								 @Nullable AsyncSocket so) throws Exception {
		if (isHandshakeProtocol(typeId)) {
			// handshake protocol call direct in io-thread.
			var p = decodeProtocol(typeId, bb, factoryHandle, so);
			TaskSpec.ofFunc(() -> p.handle(this, factoryHandle)).name("Service.handleHandshakeProtocol").call();
			return;
		}
		if (!noProcedure && factoryHandle.Level != TransactionLevel.None) {
			// 事务模式，需要从decode重启。
			// 传给事务的buffer可能重做需要重新decode，不能直接引用网络层的buffer，需要copy一次。
			var bytesCopy = bb.Copy();
			var bbCopy = ByteBuffer.Wrap(bytesCopy);
			var outProtocol = new OutObject<Protocol<?>>();
			var protocolClassName = factoryHandle.Class.getName();
			var dispatchTime = ZezeCounter.ENABLE ? System.nanoTime() : 0L; // 入队时刻
			FuncLong action = () -> {
				var needLog = bbCopy.ReadIndex == 0;
				if (needLog && dispatchTime != 0) // 首次执行才度量，redo不重复计入
					ZezeCounter.instance.addRecvDispatchTime(typeId, System.nanoTime() - dispatchTime);
				bbCopy.ReadIndex = 0; // 考虑redo,要重置读指针
				var p = decodeProtocol(typeId, bbCopy, factoryHandle, so, needLog);
				outProtocol.value = p;
				return p.handle(this, factoryHandle);
			};
			var app = Objects.requireNonNull(zeze, "service has no application");
			Procedure proc;
			if (app.getConfig().isHistory()) {
				proc = app.newProcedure(action, protocolClassName, factoryHandle.Level,
						protocolClassName, new Binary(bytesCopy));
			} else
				proc = app.newProcedure(action, protocolClassName, factoryHandle.Level);
			// p == null 说明 decodeProtocol 抛异常，直接关闭连接以对齐非事务分支的断连语义。
			TaskSpec.ofProcedureOut(proc, outProtocol, (p, code) -> {
				if (p != null)
					p.trySendResultCode(code);
				else if (so != null)
					so.close(new IOException("protocol decode failed"));
			}).dispatchMode(factoryHandle.Mode).runNow();
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
	public void dispatchUnknownProtocol(AsyncSocket so, int moduleId, int protocolId, ByteBuffer data)
			throws Exception {
		throw new UnsupportedOperationException(getName() + " Unknown Protocol (" + moduleId + ", " + protocolId
				+ ") size=" + data.size() + " so=" + so);
	}

	@SuppressWarnings({"RedundantThrows", "BooleanMethodIsAlwaysInverted"})
	public boolean checkOverflow(AsyncSocket so, long newSize, byte [] bytes, int offset, int length)
			throws Exception {
		var maxSize = getSocketOptions().getOutputBufferMaxSize();
		if (newSize <= maxSize)
			return true;
		overflowSizeHandle.getAndAdd(this, (long)length);
		if ((int)overflowCountHandle.getAndAdd(this, 1) == 0) {
			TaskSpec.ofAction(() -> logger.error("Send overflow(>{}): {} dropped {}/{}",
					maxSize, this, overflowSizeHandle.getAndSet(this, 0L), overflowCountHandle.getAndSet(this, 0)))
					.scheduleNow(1000);
		}
		return false;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 协议工厂
	 */
	public static class ProtocolFactoryHandle<P extends Protocol<?>> {
		public final Class<P> Class;
		public final long TypeId;
		public @Nullable Factory<P> Factory;
		public @Nullable ProtocolHandle<P> Handle;
		public TransactionLevel Level;
		public DispatchMode Mode;
		public int CriticalLevel = Protocol.eCriticalPlus;

		public ProtocolFactoryHandle(Class<P> protocolClass, long typeId) {
			Class = protocolClass;
			TypeId = typeId;
			Level = TransactionLevel.Serializable;
			Mode = DispatchMode.Normal;
		}

		public ProtocolFactoryHandle(Factory<P> factory) {
			this(factory, null, TransactionLevel.Serializable, DispatchMode.Normal);
		}

		public ProtocolFactoryHandle(Factory<P> factory, @Nullable ProtocolHandle<P> handle) {
			this(factory, handle, TransactionLevel.Serializable, DispatchMode.Normal);
		}

		public ProtocolFactoryHandle(Factory<P> factory, @Nullable ProtocolHandle<P> handle,
									 TransactionLevel level) {
			this(factory, handle, level, DispatchMode.Normal);
		}

		@SuppressWarnings("unchecked")
		public ProtocolFactoryHandle(Factory<P> factory, @Nullable ProtocolHandle<P> handle,
									 TransactionLevel level, DispatchMode mode) {
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

	public final LongConcurrentHashMap<ProtocolFactoryHandle<? extends Protocol<?>>> getFactorys() {
		return factorys;
	}

	public void AddFactoryHandle(long type, ProtocolFactoryHandle<? extends Protocol<?>> factory) {
		if (factorys.putIfAbsent(type, factory) != null)
			throw new IllegalStateException(String.format("duplicate factory type=%d moduleId=%d id=%d",
					type, type >>> 32, type & 0xffff_ffffL));
	}

	public @Nullable ProtocolFactoryHandle<? extends Protocol<?>> findProtocolFactoryHandle(long type) {
		return factorys.get(type);
	}

	/**
	 * Rpc Context. 使用通配符容纳不同参数和结果类型的 Rpc。
	 */
	public final long addRpcContext(Rpc<?, ?> rpc) {
		while (true) {
			long sessionId = nextSessionId();
			if (tryAddRpcContext(sessionId, rpc))
				return sessionId;
		}
	}

	final boolean tryAddRpcContext(long sessionId, Rpc<?, ?> rpc) {
		return rpcContexts.putIfAbsent(sessionId, rpc) == null;
	}

	public final @Nullable Rpc<?, ?> removeRpcContext(long sid) {
		var rpc = rpcContexts.remove(sid);
		if (rpc != null)
			rpc.cancelTimeout();
		return rpc;
	}

	// 应答会合校验用的非消费读（先校验再移除）。
	public final @Nullable Rpc<?, ?> getRpcContext(long sid) {
		return rpcContexts.get(sid);
	}

	public final boolean removeRpcContext(long sid, Rpc<?, ?> ctx) {
		if (!rpcContexts.remove(sid, ctx))
			return false;
		ctx.cancelTimeout();
		return true;
	}

	public final LongHashMap<Rpc<?, ?>> getRpcContextsToSender(AsyncSocket sender) {
		return getRpcContexts(p -> p.getSender() == sender);
	}

	public final LongHashMap<Rpc<?, ?>> getRpcContexts(Predicate<Rpc<?, ?>> filter) {
		var result = new LongHashMap<Rpc<?, ?>>(Math.max(rpcContexts.size(), 1024)); // 初始容量先别定太大,可能只过滤出一小部分
		for (var it = rpcContexts.entryIterator(); it.moveToNext(); ) {
			//noinspection DataFlowIssue
			var rpc = it.value();
			if (filter.test(rpc))
				result.put(it.key(), rpc);
		}
		return result;
	}

	public static abstract class ManualContext {
		private long sessionId;
		private @Nullable Object userState;
		private volatile boolean isTimeout;
		private @Nullable Service service;

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

		public @Nullable Service getService() {
			return service;
		}

		public void setService(Service service) {
			this.service = service;
		}

		@SuppressWarnings("RedundantThrows")
		public void onRemoved() throws Exception {
		}
	}

	public final long addManualContextWithTimeout(ManualContext context) {
		return addManualContextWithTimeout(context, 10 * 1000);
	}

	public final long addManualContextWithTimeout(ManualContext context, long timeout) { // 毫秒
		while (true) {
			long sessionId = nextSessionId();
			if (manualContexts.putIfAbsent(sessionId, context) == null) {
				context.setSessionId(sessionId);
				context.setService(this);
				// putIfAbsent 不随事务回滚撤销，须用 scheduleNow 注册超时兜底。
				TaskSpec.ofAction(() -> tryRemoveManualContext(sessionId, true)).scheduleNow(timeout);
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

	// 不直接暴露内部的容器，提供这个方法给外面用。以后如果有问题，可以改这里。

	public final void foreach(Action1<AsyncSocket> action) throws Exception {
		for (var socket : socketMap)
			action.run(socket);
	}

	/**
	 * 取一个 Acceptor 地址用于对外暴露。
	 * 优先返回第一个 ip、port 均显式配置的 Acceptor；若整份配置都没有显式地址，
	 * 则回退为最后一个遍历到的 Acceptor 条目（ip 可能为空串、port 可能为 0），
	 * 调用方需自行处理这种"未显式配置"的回退值（可参考 getOnePassiveAddress）。
	 */
	public KVNotNull<String, Integer> getOneAcceptorAddress() {
		// KV.key构造后不变：先累积再create。
		var ip = new String[]{""};
		var port = new int[]{0};
		config.forEachAcceptor2(a -> {
			if (!a.getIp().isEmpty() && a.getPort() != 0) {
				// 找到ip，port都配置成明确地址的。
				ip[0] = a.getIp();
				port[0] = a.getPort();
				return false;
			}
			// 获得最后一个配置的ip,port。
			ip[0] = a.getIp();
			port[0] = a.getPort();
			return true;
		});

		return KVNotNull.create(ip[0], port[0]);
	}

	public KVNotNull<String, Integer> getOnePassiveAddress() {
		var ipPort = getOneAcceptorAddress();
		// 允许系统来选择端口。

		var ip = ipPort.getKey();
		if (ip.equals("@internal") || ip.isBlank())
			ip = Helper.selectOneIpAddress(true);
		else if (ip.equals("@external"))
			ip = Helper.selectOneIpAddress(false);

		if (ip.isEmpty()) {
			// 实在找不到ip地址的话，就设置成loopback。
			logger.warn("PassiveAddress No Config. set ip to 127.0.0.1");
			ip = "127.0.0.1";
		}
		return KVNotNull.create(ip, ipPort.getValue());
	}

	public void onServerSocketBind(ServerSocket port) {
	}

	/**
	 * 接收一个协议(尚未处理)时,判断单个socket是否接收超限
	 *
	 * @return 是否检查通过, false则丢弃该协议(也可以同时关闭连接)
	 */
	@SuppressWarnings("MethodMayBeStatic")
	public boolean checkThrottle(AsyncSocket sender, int moduleId, int protocolId, int size) {
		var throttle = sender.getTimeThrottle();
		if (null != throttle && !throttle.checkNow(size)) {
			// trySendResultCode(Procedure.Busy); // 超过速度限制，不报告错误。因为可能是一种攻击。
			sender.close(throttleException); // 默认关闭连接。
			return false; // 超过速度控制，丢弃这条协议。
		}
		return true;
	}

	public boolean discard(AsyncSocket sender, int moduleId, int protocolId, int size) throws Exception {
		return false;
	}

	public ScheduledFuture<?> startStatisticLog(int periodSec) {
		lock();
		try {
			if (servicePerf == null)
				servicePerf = new ServiceStatisticLog(this);
			return servicePerf.startStatisticLog(periodSec);
		} finally {
			unlock();
		}
	}

	public boolean cancelStartStatisticLog() {
		lock();
		try {
			return servicePerf == null || servicePerf.cancelStartStatisticLog();
		} finally {
			unlock();
		}
	}

	@SuppressWarnings("MethodMayBeStatic")
	public void onRpcLostContext(Rpc<?, ?> rpc) {
		logger.warn("rpc response: lost context, maybe timeout. {}", rpc);
	}

	public void tryStartKeepAliveCheckTimer() {
		if (keepCheckTimer != null || keepAliveCheckStopped)
			return;
		// 使用 tryLock + 延迟重试，避免与持 Service 锁的长操作形成锁序死锁。
		if (!tryLock()) {
			TaskSpec.ofAction(this::tryStartKeepAliveCheckTimer).scheduleNow(1000);
			return;
		}
		try {
			if (keepCheckTimer == null && !keepAliveCheckStopped) {
				var period = getConfig().getHandshakeOptions().getKeepCheckPeriod() * 1000L;
				if (period > 0) {
					keepCheckTimer = TaskSpec.ofAction(this::checkKeepAlive).schedulePeriodNow(
							Random.getInstance().nextLong(period) + 1, period);
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
		long now = GlobalTimer.getCurrentSeconds();
		foreach(socket -> {
			// 仅检查曾维护活跃时间的连接，未适配活跃时间的自定义连接保持豁免。
			if (socket.getActiveRecvTime() > 0 || socket.getActiveSendTime() > 0) {
				long recvTime = now - socket.getActiveRecvTime();
				if (recvTime > keepRecvTimeout) {
					try {
						onKeepAliveTimeout(socket);
					} catch (Exception e) {
						logger.error("onKeepAliveTimeout exception:", e);
					}
				}
				if (socket.getType() == AsyncSocket.Type.eClient && (now - socket.getActiveSendTime() > keepSendTimeout ||
						recvTime > keepSendTimeout)) { // 上次接收时间超过SendTimeout也要发起KeepAlive,通过RPC回复更新上次接收时间
					try {
						onSendKeepAlive(socket);
					} catch (Exception e) {
						logger.error("onSendKeepAlive exception:", e);
					}
				}
			}
		});
	}

	@SuppressWarnings({"MethodMayBeStatic", "RedundantThrows"})
	protected void onKeepAliveTimeout(AsyncSocket socket) throws Exception {
		socket.close(keepAliveException);
	}

	/**
	 * 1. 如果你是handshake的service，默认实现已发送KeepAlive，无需重载；
	 * 2. 如果你是其他service子类，重载这个方法，按注释发送KeepAlive，并且服务器端需要注册这条协议并写一个不需要处理代码的handler；
	 * 3. 如果不发送, 会导致KeepTimerClient时间后再次触发, 也可以调用socket.setActiveSendTime()避免频繁触发。
	 *
	 * @param socket 当前连接
	 */
	@SuppressWarnings("MethodMayBeStatic")
	protected void onSendKeepAlive(AsyncSocket socket) {
		new KeepAlive().Send(socket); // skip result
	}

	public DatagramSocket bindUdp(InetSocketAddress local) throws IOException {
		return new DatagramSocket(this, local);
	}

	public final void connect(String hostNameOrAddress, int port) {
		connect(hostNameOrAddress, port, true);
	}

	public void connect(String hostNameOrAddress, int port, boolean autoReconnect) {
		var out = new OutObject<Connector>();
		config.tryGetOrAddConnector(hostNameOrAddress, port, autoReconnect, out);
		//noinspection DataFlowIssue
		out.value.start();
	}
}
