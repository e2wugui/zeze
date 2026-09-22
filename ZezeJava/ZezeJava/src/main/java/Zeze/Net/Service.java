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
import Zeze.Util.KV;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Service extends ReentrantLock {
	protected static final @NotNull Logger logger = LogManager.getLogger(Service.class);
	// FND7-19/R3：默认共享发号流随机63位基址——同JVM内全Service共享一条流保证不撞号，
	// 随机基址使跨JVM/leader代碰撞概率2^-63量级（ServiceManagerWithRaft按sessionId判活，
	// 固定从1起号跨代必撞）。
	private static final AtomicLong staticSessionIdAtomicLong = new AtomicLong(
			(System.nanoTime() ^ new java.security.SecureRandom().nextLong()) & Long.MAX_VALUE);
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
	private boolean noProcedure;
	// volatile：tryStartKeepAliveCheckTimer的无锁快路径在Service锁外读它。
	protected volatile Future<?> keepCheckTimer;
	// keepalive定时器随服务生命周期管理：start启动（keepCheckPeriod默认0禁用时无开销）、
	// stop熔断。同时保留TcpSocket构造的兜底启动：Token/GlobalAgent/OnzServer等
	// 手工connector路径不经过Service.start()，仍依赖懒启动。
	private volatile boolean keepAliveCheckStopped;
	private @Nullable ServiceStatisticLog servicePerf;
	// 门禁白名单：非空即布防——未完成密钥交换的连接仅放行白名单内的协议。
	private final LongHashSet decodeAdmissionWhitelist = new LongHashSet();

	public @NotNull String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(@NotNull String instanceName) {
		this.instanceName = instanceName;
	}

	// 停机屏障（XA1-F1/N2-F3）：stop最前置位、start复位；addSocket在登记成功后复查，
	// 命中即自查自关——与stop的关闭循环两序either-way必被一方关闭，迟到连接不再泄漏。
	private volatile boolean stopped;

	public Service(@NotNull String name) {
		this(name, null, null);
	}

	public Service(@NotNull String name, @Nullable Config config) {
		this(name, null, config);
	}

	public Service(@NotNull String name, @Nullable Application app) {
		this(name, app, app != null ? app.getConfig() : null);
	}

	public Service(@NotNull String name, @Nullable Application app, @Nullable Config config) {
		this.name = name;
		zeze = app;
		this.config = initConfig(config);
		socketOptions = this.config.getSocketOptions();
		noProcedure = app == null || app.isNoDatabase();
		// FND7-19/R3：模板仅在构造期取用一次（多Service共享同一supplier会撞号，模板语义
		// 仅为兼容旧全局安装的单Service用法）。
		var template = defaultSessionIdGenFunc;
		if (template != null)
			sessionIdGenerator = template;
		logger.info("start: {}", name);
		tryStartStatisticLog();
	}

	// FND7-19/R3：兼容旧全局安装AsyncSocket.setSessionIdGenFunc的模板入口——仅对之后
	// 构造的Service生效。多Service共享同一supplier仍会撞号（正是被取代的旧缺陷形态，
	// Zezex linkd/Game.Server拓扑即此），自定义发号请用实例级setSessionIdGenerator
	// （须在创建任何socket之前调用，并保证进程内全局值域不重叠）。
	private static volatile @Nullable LongSupplier defaultSessionIdGenFunc;

	/** 兼容旧全局安装的模板入口：仅对之后构造的Service生效；多Service共享同一supplier仍会撞号。 */
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

	/**
	 * 会话按sessionId登记入表（putIfAbsent，先注册者胜）。
	 * 返回false=同号互撞或socket已close：撞号时打error日志并关闭新连接（保留先注册者），
	 * 已close的迟到登记被拒；调用方不得再对该连接回调OnHandshakeDone。
	 */
	protected final boolean addSocket(@NotNull AsyncSocket so) {
		var existing = so.runIfOpen(() -> {
			var e = socketMap.putIfAbsent(so.getSessionId(), so);
			return e != null ? e : so; // 插入成功以so自身为哨兵，与runIfOpen的closed-null区分
		});
		if (existing == null)
			return false; // 已closed：迟到登记拒绝
		if (existing != so) {
			logger.error("addSocket: duplicate sessionId {} in service '{}': existing socket {} kept, "
							+ "colliding socket {} closed. Usually caused by multiple apps in one JVM overriding "
							+ "the global static AsyncSocket.setSessionIdGenFunc, making new ids collide "
							+ "with existing sessions.",
					so.getSessionId(), name, existing, so);
			so.close(new IllegalStateException("duplicate session id: " + so.getSessionId()));
			return false;
		}
		// 登记成功后复查停机屏障（XA1-F1/N2-F3）：stop最前置位后仍在飞的登记在此自查自关。
		// 复查读到stopped==false时，put必然先于stop的关闭循环开始——循环遍历live的socketMap
		// 必然看到已完成的put并关闭它；两序either-way必被一方关闭，无锁封死迟到连接泄漏。
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
		stopped = false; // 复位停机屏障（XA1-F1/N2-F3）：支持stop后再start
		// keepalive定时器随服务启动（先于config.start()创建任何socket）；KeepCheckPeriod
		// 默认0禁用时tryStartKeepAliveCheckTimer内部不创建任务，无开销。
		keepAliveCheckStopped = false;
		tryStartKeepAliveCheckTimer();
		config.start();
		ZezeCounter.instance.serviceStart(this);
	}

	/**
	 * 停止服务：关闭全部连接并熔断keepalive定时器与懒启动重试。
	 * <p>
	 * 【锁序契约】本方法持本Service锁逐个close连接，而 {@link AsyncSocket#close} 在关闭
	 * 发起线程同步回调 {@link #OnSocketClose}（重入本Service锁安全）；回调同样发生在
	 * selector线程与keepCheck的tick线程。因此OnSocketClose覆写可以重入本Service锁，但
	 * 锁序恒为Service锁→回调内业务锁单向，不得反向阻塞；本方法锁内段不得等待"由
	 * OnSocketClose（任意线程）持有的资源"——keepCheckTimer.cancel须先取TimerFuture锁
	 * 并join在飞tick，而飞tick可能正同步停在子类OnSocketClose里等本Service锁，故锁内
	 * 只捕获句柄置null，cancel移到锁外。
	 */
	public void stop() throws Exception {
		stopped = true; // 停机屏障最前置位（XA1-F1/N2-F3）：先于关闭循环，addSocket登记后复查据此拒绝迟到连接
		config.stop();
		Future<?> keepTimer;
		lock();
		try {
			for (AsyncSocket as : socketMap)
				as.close(serviceStoppedException); // remove in callback OnSocketClose

			// 不清除_RpcContexts：让Rpc的TimerTask超时后照常触发回调；直接清除会卡死同步等待。
			// 在飞Rpc的去留可由应用层OnSocketDisposed覆写决定。
			// _RpcContexts.Clear();

			keepTimer = keepCheckTimer;
			keepCheckTimer = null;
			keepAliveCheckStopped = true; // 熔断tryStartKeepAliveCheckTimer的挂起重试

			ZezeCounter.instance.serviceStop(this);
		} finally {
			unlock();
		}
		// cancel须在Service锁外（见上锁序契约）：锁内只捕获句柄置null（保住与
		// tryStartKeepAliveCheckTimer的互斥）；锁外cancel最多等一轮在飞tick，无死锁。
		if (keepTimer != null)
			keepTimer.cancel(true);
	}

	public final @NotNull AsyncSocket newServerSocket(@Nullable String ipaddress, int port,
													  @Nullable Acceptor acceptor) {
		try {
			return newServerSocket(InetAddress.getByName(ipaddress), port, acceptor);
		} catch (UnknownHostException e) {
			throw Task.forceThrow(e);
		}
	}

	public final @NotNull AsyncSocket newServerSocket(@Nullable InetAddress ipaddress, int port,
													  @Nullable Acceptor acceptor) {
		return newServerSocket(new InetSocketAddress(ipaddress, port), acceptor);
	}

	public final @NotNull AsyncSocket newServerSocket(@Nullable InetSocketAddress localEP, @Nullable Acceptor acceptor) {
		return new TcpSocket(this, localEP, acceptor);
	}

	public final @NotNull AsyncSocket newClientSocket(@Nullable String hostNameOrAddress, int port,
													  @Nullable Object userState, @Nullable Connector connector) {
		return new TcpSocket(this, hostNameOrAddress, port, userState, connector);
	}

	/** 客户端连接DNS解析点：TcpSocket在专用resolver线程上异步调用，允许阻塞；可覆写（测试门控/自定义resolver）。 */
	protected @NotNull InetAddress resolveAddress(@Nullable String hostNameOrAddress) throws IOException {
		return InetAddress.getByName(hostNameOrAddress);
	}

	// 契约：客户端socket构造微秒级非阻塞（可在Connector锁内调用），覆写不得阻塞
	public @NotNull AsyncSocket newWebsocketClient(@NotNull String url,
												   @Nullable Object userState, @Nullable Connector connector) {
		return new WebsocketClient(this, url, userState, connector);
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
	 * 连接销毁回调：默认实现对仍挂在本连接上的在飞Rpc上下文立即失败处置。
	 * 在 OnSocketClose 之后调用，此时外面【必须】拿不到此 AsyncSocket 了（Socket已设
	 * null）；对已持有的引用，使用时判断返回值，主要是 Send 返回 false。
	 *
	 * <p>
	 * 默认实现：在飞Rpc上下文立即失败——future以 {@link RpcSocketDisposedException}
	 * 失败（可诊断，区别于超时），handle以 {@link Procedure#ErrorSendFail} 立即派发
	 * （上下文已移除，超时定时器不再触发，不派发则回调方永远等不到）。
	 * 框架层没有"在飞Rpc随重连重发"机制（重连只重建socket；Rpc实例一次性，重发须新建
	 * 实例并配合幂等/去重），故立即失败不丢失语义；跨连接迟到应答即使按sid命中，上下文
	 * 已移除只走到 {@link #onRpcLostContext}。
	 *
	 * @param so after socket closed. last callback.
	 */
	public void OnSocketDisposed(@NotNull AsyncSocket so) throws Exception {
		var ctxSends = getRpcContextsToSender(so);
		if (ctxSends.isEmpty())
			return;
		for (var it = ctxSends.iterator(); it.moveToNext(); ) {
			var sid = it.key();
			var ctx = removeRpcContext(sid);
			if (ctx == null)
				continue; // 已被应答/超时消费
			if (!(ctx instanceof Rpc<?, ?> rpc))
				continue; // 当前addRpcContext只有Rpc，防御未来扩展
			rpc.setResultCode(Procedure.ErrorSendFail);
			var future = rpc.getFuture();
			if (future != null)
				future.setException(RpcSocketDisposedException.getInstance());
			else {
				//noinspection unchecked
				var handle = (ProtocolHandle<Rpc<?, ?>>)(ProtocolHandle<?>)rpc.getResponseHandle();
				if (handle != null) {
					var factoryHandle = findProtocolFactoryHandle(ctx.getTypeId());
					if (factoryHandle != null)
						dispatchRpcResponse(rpc, handle, factoryHandle);
					else // N2-F4：协议工厂缺失时静默丢弃responseHandle无线索，对齐onRpcLostContext补warn
						logger.warn("rpc disposed: protocol factory not found, response handle skipped: {}", rpc);
				}
			}
		}
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
	 * 接受新连接的公共步骤：限流→haProxy→注册→OnHandshakeDone（TCP与websocket接受路径统一入口）。
	 * 超限抛IllegalStateException，调用方负责捕获并关闭连接；addSocket返回false时直接返回，
	 * 不回调OnHandshakeDone。推迟OnHandshakeDone的接受路径（Handshake家族/Token覆写）不走本方法，
	 * 自行checkMaxConnections+setupHaProxyHeader+按addSocket返回值短路。
	 */
	protected final void tryAccept(@NotNull AsyncSocket so) throws Exception {
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
	public void OnSocketAccept(@NotNull AsyncSocket so) throws Exception {
		tryAccept(so); // 超限抛出由accept流程catch关闭（既有契约）；撞号静默返回
	}

	/**
	 * 给新接受的连接安装 HaProxy 头解析器（ServiceConf 配置了 HaProxyKey 时）。
	 * 覆写 OnSocketAccept 的服务子类（HandshakeServer/HandshakeBoth/TokenServer 等）不再走
	 * Service.OnSocketAccept 的默认实现，会丢失这里的安装（FND7-24）：LB 的 "PROXY ..." 头
	 * 会被当作协议帧头解码成未知协议，所有连接被拒且零告警。覆写点必须在收到任何数据前
	 * 调用本方法恢复（对齐 HandshakeBase.checkMaxConnections 的 FND-S3-2 判例）。
	 */
	protected final void setupHaProxyHeader(@NotNull AsyncSocket so) {
		if (config.getHaProxyKey() != null && so instanceof TcpSocket tcp)
			tcp.setHaProxyHeader(new HaProxyHeader(config.getHaProxyKey()));
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
	public boolean OnSocketProcessInputBuffer(@NotNull AsyncSocket so, @NotNull ByteBuffer input) throws Exception {
		if (so instanceof TcpSocket tcp) {
			var haProxyHeader = tcp.getHaProxyHeader();
			if (haProxyHeader != null && !haProxyHeader.decodeHeader(input))
				return true; // 没有解析完header，看作成功。
		}
		Protocol.decode(this, so, input);
		return true;
	}

	/**
	 * 登记门禁白名单协议：白名单非空即布防——未完成密钥交换的连接仅放行白名单内的协议，
	 * 其余明文帧解码即断连。须在建立连接前调用：既有连接不回溯装配，且集合非线程安全，
	 * 连接存在期间不得追加。
	 */
	public void armDecodeAdmission(long typeId) {
		decodeAdmissionWhitelist.add(typeId);
	}

	/**
	 * 连接级解码准入：由 TcpSocket 连接构造器调用（唯一调用方），返回的谓词在
	 * {@link Protocol#decode} 逐帧生效（帧头解析后、完整性检查前），返回 false 即断连；
	 * 白名单非空时以其为准入，双向 codec 装齐撤销。null 表示不设防。
	 */
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
		if (!noProcedure && factoryHandle.Level != TransactionLevel.None) {
			TaskSpec.ofProcedure(zeze.newProcedure(() -> responseHandle.handle(rpc),
					rpc.getClass().getName() + ":Response", factoryHandle.Level))
					.dispatchMode(factoryHandle.Mode).runNow();
		} else
			TaskSpec.ofFunc(() -> responseHandle.handle(rpc), rpc).dispatchMode(factoryHandle.Mode).runNow();
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
		// 一般来说到达这个函数，肯定执行非事务分支了，事务分支在下面的dispatchProtocol中就被拦截。
		// 但为了更具适应性，就是有人重载了下面的dispatchProtocol，然后没有处理事务，直接派发到这里，
		// 这里还是处理了存储过程的创建。但这里处理的存储过程没有redo时重置协议参数的能力。
		if (!noProcedure && factoryHandle.Level != TransactionLevel.None) {
			var protocolClassName = p.getClass().getName();
			var proc = zeze.newProcedure(() -> p.handle(this, factoryHandle), protocolClassName, factoryHandle.Level);
			TaskSpec.ofProcedure(proc, p, Protocol::trySendResultCode)
					.dispatchMode(factoryHandle.Mode).runNow();
		} else {
			TaskSpec.ofFunc(() -> p.handle(this, factoryHandle), p, Protocol::trySendResultCode)
					.dispatchMode(factoryHandle.Mode).runNow();
		}
	}

	public void dispatchProtocol(long typeId, @NotNull ByteBuffer bb, @NotNull ProtocolFactoryHandle<?> factoryHandle,
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
			Procedure proc;
			if (zeze.getConfig().isHistory()) {
				proc = zeze.newProcedure(action, protocolClassName, factoryHandle.Level,
						protocolClassName, new Binary(bytesCopy));
			} else
				proc = zeze.newProcedure(action, protocolClassName, factoryHandle.Level);
			// N2-F1：p==null当且仅当action内decodeProtocol抛出（outProtocol.value未赋值），
			// 方法引用Protocol::trySendResultCode不容忍null会NPE吞掉错误处置。此时对齐非事务
			// 分支（decode异常上抛即断连）的语义close连接——Rpc.Send经连接关闭路径感知
			// （OnSocketDisposed对在飞上下文立即失败），不再干等超时。
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
	public void dispatchUnknownProtocol(@NotNull AsyncSocket so, int moduleId, int protocolId, @NotNull ByteBuffer data)
			throws Exception {
		throw new UnsupportedOperationException(getName() + " Unknown Protocol (" + moduleId + ", " + protocolId
				+ ") size=" + data.size() + " so=" + so);
	}

	@SuppressWarnings({"RedundantThrows", "BooleanMethodIsAlwaysInverted"})
	public boolean checkOverflow(@NotNull AsyncSocket so, long newSize, byte @NotNull [] bytes, int offset, int length)
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

	public void AddFactoryHandle(long type, @NotNull ProtocolFactoryHandle<? extends Protocol<?>> factory) {
		if (factorys.putIfAbsent(type, factory) != null)
			throw new IllegalStateException(String.format("duplicate factory type=%d moduleId=%d id=%d",
					type, type >>> 32, type & 0xffff_ffffL));
	}

	public @Nullable ProtocolFactoryHandle<? extends Protocol<?>> findProtocolFactoryHandle(long type) {
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
				// 超时清理必须立即注册（scheduleNow）：上面的 putIfAbsent 注册不随事务回滚撤销，
				// 回滚后条目仍需超时兜底移除并回调 onRemoved；事务感知的 schedule 会随回滚丢弃注册，导致 manualContexts 条目永驻。
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

	// 还是不直接暴露内部的容器。提供这个方法给外面用。以后如果有问题，可以改这里。

	public final void foreach(@NotNull Action1<@NotNull AsyncSocket> action) throws Exception {
		for (var socket : socketMap)
			action.run(socket);
	}

	public @NotNull KV<@NotNull String, @NotNull Integer> getOneAcceptorAddress() {
		// KV.key构造后不变（FND5-12复审）：累积后create，不再setKey。
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

		return KV.create(ip[0], port[0]);
	}

	public @NotNull KV<@NotNull String, @NotNull Integer> getOnePassiveAddress() {
		var ipPort = getOneAcceptorAddress();
		// 允许系统来选择端口。
		//if (ipPort.getValue() == 0)
		//	throw new IllegalStateException("Acceptor: No Config.");

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
		return KV.create(ip, ipPort.getValue());
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
	public void onRpcLostContext(@NotNull Rpc<?, ?> rpc) {
		logger.warn("rpc response: lost context, maybe timeout. {}", rpc);
	}

	public void tryStartKeepAliveCheckTimer() {
		// 无锁快路径：定时器已启动（服务start时eager启动，主路径）或服务已停时，
		// 每条TcpSocket构造只花一次volatile读，不碰Service锁。
		if (keepCheckTimer != null || keepAliveCheckStopped)
			return;
		// 兜底路径（手工connector等服务未经start()的懒启动）：tryLock+延迟补偿——
		// 本方法可能与Service锁的长期持有者（如SMServer.closeSession持锁提交raft事务）并发；
		// 阻塞等锁会与 Raft锁→Service锁 的调用路径互喂成ABBA死锁。定时器启动可以推迟：
		// 抢锁失败挂1s后重试（链式，同时刻至多一个）。
		if (!tryLock()) {
			TaskSpec.ofAction(this::tryStartKeepAliveCheckTimer).scheduleNow(1000);
			return;
		}
		try {
			if (keepCheckTimer == null) {
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
		// long秒：与AsyncSocket.activeRecvTime/activeSendTime的long存储配套，int截断会在
		// 时间源越过2^31秒后使差值变大负数，超时判定恒false（静默死连接永不回收）。
		long now = GlobalTimer.getCurrentSeconds();
		foreach(socket -> {
			// FND7-63：覆盖判据从instanceof TcpSocket放宽为"活跃时间曾被更新"：
			// Websocket/WebsocketClient建立时reset、收发路径更新，同样被回收/探测，
			// 静默死链不再泄漏；从不更新活跃时间的连接类型（如未适配的自定义AsyncSocket）
			// 保持豁免，避免activeRecvTime==0被当作超时立即误杀。
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
		new KeepAlive().Send(socket); // skip result
	}

	public @NotNull DatagramSocket bindUdp(@NotNull InetSocketAddress local) throws IOException {
		return new DatagramSocket(this, local);
	}

	public final void connect(@NotNull String hostNameOrAddress, int port) {
		connect(hostNameOrAddress, port, true);
	}

	public void connect(String hostNameOrAddress, int port, boolean autoReconnect) {
		var out = new OutObject<Connector>();
		config.tryGetOrAddConnector(hostNameOrAddress, port, autoReconnect, out);
		out.value.start();
	}
}
