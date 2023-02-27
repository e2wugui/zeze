package Zeze.Net;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Action1;
import Zeze.Util.Factory;
import Zeze.Util.KV;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.LongHashMap;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Service {
	protected static final Logger logger = LogManager.getLogger(Service.class);
	private static final AtomicLong staticSessionIdAtomicLong = new AtomicLong(1);
	private static final VarHandle closedRecvSizeHandle, closedSendSizeHandle, closedSendRawSizeHandle;
	protected static final VarHandle overflowSizeHandle, overflowCountHandle;

	static {
		var l = MethodHandles.lookup();
		try {
			closedRecvSizeHandle = l.findVarHandle(Service.class, "closedRecvSize", long.class);
			closedSendSizeHandle = l.findVarHandle(Service.class, "closedSendSize", long.class);
			closedSendRawSizeHandle = l.findVarHandle(Service.class, "closedSendRawSize", long.class);
			overflowSizeHandle = l.findVarHandle(Service.class, "overflowSize", long.class);
			overflowCountHandle = l.findVarHandle(Service.class, "overflowCount", int.class);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private final String name;
	private final Application zeze;
	private SocketOptions socketOptions; // 同一个 Service 下的所有连接都是用相同配置。
	private ServiceConf config;
	private LongSupplier sessionIdGenerator;
	protected final LongConcurrentHashMap<AsyncSocket> socketMap = new LongConcurrentHashMap<>();
	private final LongConcurrentHashMap<ProtocolFactoryHandle<? extends Protocol<?>>> factorys = new LongConcurrentHashMap<>();
	private final LongConcurrentHashMap<Protocol<?>> rpcContexts = new LongConcurrentHashMap<>();
	private final LongConcurrentHashMap<ManualContext> manualContexts = new LongConcurrentHashMap<>();
	@SuppressWarnings("unused")
	private volatile long closedRecvSize, closedSendSize, closedSendRawSize; // 已关闭连接的从socket接收/已发送数据/准备发送数据的总字节数
	private volatile long recvSize, sendSize, sendRawSize; // 当前已统计的从socket接收/已发送数据/准备发送数据的总字节数
	@SuppressWarnings("unused")
	protected volatile long overflowSize;
	@SuppressWarnings("unused")
	protected volatile int overflowCount;

	private Selectors selectors;

	public Service(String name) {
		this.name = name;
		zeze = null;
		socketOptions = new SocketOptions();
	}

	public Service(String name, Config config) {
		this.name = name;
		zeze = null;
		initConfig(config);
	}

	public Service(String name, Application app) {
		this.name = name;
		zeze = app;
		initConfig(app != null ? app.getConfig() : null);
	}

	private void initConfig(Config config) {
		this.config = config != null ? config.getServiceConf(name) : null;
		if (this.config == null) {
			// setup program default
			this.config = new ServiceConf();
			if (config != null) {
				// reference to config default
				this.config.setSocketOptions(config.getDefaultServiceConf().getSocketOptions());
				this.config.setHandshakeOptions(config.getDefaultServiceConf().getHandshakeOptions());
			}
		}
		this.config.setService(this);
		socketOptions = this.config.getSocketOptions();
	}

	public void setSelectors(Selectors selectors) {
		this.selectors = selectors;
	}

	public Selectors getSelectors() {
		return null != selectors ? selectors : Selectors.getInstance();
	}

	public final String getName() {
		return name;
	}

	public final Application getZeze() {
		return zeze;
	}

	public SocketOptions getSocketOptions() {
		return socketOptions;
	}

	public void setSocketOptions(SocketOptions ops) {
		if (ops != null)
			socketOptions = ops;
	}

	public ServiceConf getConfig() {
		return config;
	}

	public void setConfig(ServiceConf conf) {
		config = conf;
	}

	public final LongSupplier getSessionIdGenerator() {
		return sessionIdGenerator;
	}

	public final void setSessionIdGenerator(LongSupplier value) {
		sessionIdGenerator = value;
	}

	public final long nextSessionId() {
		var gen = sessionIdGenerator;
		return gen != null ? gen.getAsLong() : staticSessionIdAtomicLong.getAndIncrement();
	}

	public final int getSocketCount() {
		return socketMap.size();
	}

	protected final boolean addSocket(AsyncSocket so) {
		return socketMap.putIfAbsent(so.getSessionId(), so) == null;
	}

	final void changeSocketSessionId(AsyncSocket so, long newSessionId) {
		var oldSessionId = so.getSessionId();
		if (socketMap.remove(oldSessionId, so)) {
			if (socketMap.putIfAbsent(newSessionId, so) == null)
				return;
			if (socketMap.putIfAbsent(oldSessionId, so) != null) { // rollback
				closedRecvSizeHandle.getAndAdd(this, so.getRecvSize());
				closedSendSizeHandle.getAndAdd(this, so.getSendSize());
				closedSendRawSizeHandle.getAndAdd(this, so.getSendRawSize());
			}
			throw new IllegalStateException("duplicate sessionId: " + so);
		}
		// 为了简化并发问题，只能加入Service以后的Socket的SessionId。
		throw new IllegalStateException("Not Exist In Service: " + so);
	}

	public final void updateRecvSendSize() {
		long r = 0, s = 0, sr = 0;
		for (var socket : socketMap) {
			r += socket.getRecvSize();
			s += socket.getSendSize();
			sr += socket.getSendRawSize();
		}
		recvSize = closedRecvSize + r;
		sendSize = closedSendSize + s;
		sendRawSize = closedSendRawSize + sr;
	}

	public final long getRecvSize() {
		return recvSize;
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
	public AsyncSocket GetSocket(long sessionId) {
		return socketMap.get(sessionId);
	}

	public AsyncSocket GetSocket() {
		var sockets = socketMap.iterator();
		return sockets.hasNext() ? sockets.next() : null;
	}

	public void start() throws Exception {
		if (config != null)
			config.start();
	}

	public void Start() throws Exception {
		start();
	}

	public void Stop() throws Exception {
		stop();
	}

	public void stop() throws Exception {
		if (config != null)
			config.stop();

		for (AsyncSocket as : socketMap)
			as.close(); // remove in callback OnSocketClose

		// 先不清除，让Rpc的TimerTask仍然在超时以后触发回调。
		// 【考虑一下】也许在服务停止时马上触发回调并且清除上下文比较好。
		// 【注意】直接清除会导致同步等待的操作无法继续。异步只会没有回调，没问题。
		// _RpcContexts.Clear();
	}

	public final AsyncSocket newServerSocket(String ipaddress, int port, Acceptor acceptor) {
		try {
			return newServerSocket(InetAddress.getByName(ipaddress), port, acceptor);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}

	public final AsyncSocket newServerSocket(InetAddress ipaddress, int port, Acceptor acceptor) {
		return newServerSocket(new InetSocketAddress(ipaddress, port), acceptor);
	}

	public final AsyncSocket newServerSocket(InetSocketAddress localEP, Acceptor acceptor) {
		return new AsyncSocket(this, localEP, acceptor);
	}

	public final AsyncSocket newClientSocket(String hostNameOrAddress, int port, Object userState, Connector connector) {
		return new AsyncSocket(this, hostNameOrAddress, port, userState, connector);
	}

	/**
	 * ASocket 关闭的时候总是回调。
	 *
	 * @param so closing socket
	 * @param e  caught exception, null for none.
	 */
	public void OnSocketClose(AsyncSocket so, Throwable e) throws Exception {
		if (socketMap.remove(so.getSessionId(), so)) {
			closedRecvSizeHandle.getAndAdd(this, so.getRecvSize());
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
	public void OnSocketDisposed(@SuppressWarnings("unused") AsyncSocket so) throws Exception {
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

	public final Collection<Protocol<?>> removeRpcContexts(Collection<Long> sids) {
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
	public void OnSocketAccept(AsyncSocket so) throws Exception {
		if (null != config && socketMap.size() > config.getMaxConnections()) {
			throw new RuntimeException("too many connections");
		}
		addSocket(so);
		OnHandshakeDone(so);
	}

	@SuppressWarnings({"RedundantThrows", "MethodMayBeStatic"})
	public void OnSocketAcceptError(AsyncSocket listener, Throwable e) throws Exception {
		logger.error("OnSocketAcceptError: {}", listener, e);
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
	public void OnSocketConnectError(AsyncSocket so, Throwable e) throws Exception {
		socketMap.remove(so.getSessionId(), so);
	}

	/**
	 * 连接成功回调。
	 *
	 * @param so connect succeed
	 */
	public void OnSocketConnected(AsyncSocket so) throws Exception {
		addSocket(so);
		OnHandshakeDone(so);
	}

	/**
	 * 处理数据。
	 * 在异步线程中回调，要注意线程安全。
	 *
	 * @param so    current socket
	 * @param input 方法外绝对不能持有input.Bytes的引用! 也就是只能在方法内读input.
	 *              处理了多少要体现在input.ReadIndex上,剩下的等下次收到数据后会继续在此处理.
	 */
	public void OnSocketProcessInputBuffer(AsyncSocket so, ByteBuffer input) throws Exception {
		Protocol.decode(this, so, input);
	}

	// 用来派发异步rpc回调。
	@SuppressWarnings("RedundantThrows")
	public <P extends Protocol<?>> void dispatchRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
															ProtocolFactoryHandle<?> factoryHandle) throws Exception {
		Task.runRpcResponseUnsafe(() -> responseHandle.handle(rpc), rpc, factoryHandle.Mode);
	}

	public boolean isHandshakeProtocol(long typeId) {
		return false;
	}

	@SuppressWarnings("MethodMayBeStatic")
	public Protocol<?> decodeProtocol(long typeId, ByteBuffer bb, ProtocolFactoryHandle<?> factoryHandle, AsyncSocket so) {
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
		if (null != so)
			p.setUserState(so.getUserState());
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId)) {
			var log = AsyncSocket.logger;
			var level = AsyncSocket.PROTOCOL_LOG_LEVEL;
			var sessionId = null == so ? 0 : so.getSessionId();
			var className = p.getClass().getSimpleName();
			if (p instanceof Rpc) {
				var rpc = ((Rpc<?, ?>)p);
				var rpcSessionId = rpc.getSessionId();
				if (rpc.isRequest())
					log.log(level, "RECV:{} {}:{} {}", sessionId, className, rpcSessionId, p.Argument);
				else {
					log.log(level, "RECV:{} {}:{}>{} {}", sessionId, className, rpcSessionId,
							p.resultCode, rpc.Result);
				}
			} else if (p.resultCode == 0)
				log.log(level, "RECV:{} {} {}", sessionId, className, p.Argument);
			else
				log.log(level, "RECV:{} {}>{} {}", sessionId, className, p.resultCode, p.Argument);
		}
		return p;
	}

	// 用来派发已经decode的协议，不支持事务重做时重置协议参数。
	public void dispatchProtocol(Protocol<?> p) throws Exception {
		var factoryHandle = findProtocolFactoryHandle(p.getTypeId());
		dispatchProtocol(p, factoryHandle);
	}

	public void dispatchProtocol(Protocol<?> p, ProtocolFactoryHandle<?> factoryHandle) throws Exception {
		TransactionLevel level = factoryHandle.Level;
		Application zeze = this.zeze;
		// 一般来说到达这个函数，肯定执行这个分支了，事务分支在下面的dispatchProtocol中就被拦截。
		// 但为了更具适应性，这里还是处理了存储过程的创建。
		// 为了避免redirect时死锁,这里一律不在whileCommit时执行
		if (zeze != null && level != TransactionLevel.None) {
			Task.runUnsafe(
					zeze.newProcedure(() -> p.handle(this, factoryHandle),
							p.getClass().getName(), level, p.getUserState()),
					p, Protocol::trySendResultCode, factoryHandle.Mode);
		} else {
			Task.runUnsafe(
					() -> p.handle(this, factoryHandle),
					p, Protocol::trySendResultCode,
					null, factoryHandle.Mode);
		}
	}

	public void dispatchProtocol(long typeId, ByteBuffer bb, ProtocolFactoryHandle<?> factoryHandle, AsyncSocket so)
			throws Exception {
		if (isHandshakeProtocol(typeId)) {
			// handshake protocol call direct in io-thread.
			var p = decodeProtocol(typeId, bb, factoryHandle, so);
			Task.call(() -> p.handle(this, factoryHandle), "handle handshake protocol");
			return;
		}
		var level = factoryHandle.Level;
		if (zeze != null && level != TransactionLevel.None) {
			// 事务模式，需要从decode重启。
			// 传给事务的buffer可能重做需要重新decode，不能直接引用网络层的buffer，需要copy一次。
			var bbCopy = ByteBuffer.Wrap(bb.Copy());
			var outProtocol = new OutObject<Protocol<?>>();
			Task.runUnsafe(zeze.newProcedure(() -> {
						bbCopy.ReadIndex = 0; // 考虑redo,要重置读指针
						var p = decodeProtocol(typeId, bbCopy, factoryHandle, so);
						return p.handle(this, factoryHandle);
					}, factoryHandle.Class.getName(), level, so.getUserState()),
					outProtocol, Protocol::trySendResultCode, factoryHandle.Mode);
		} else {
			var p = decodeProtocol(typeId, bb, factoryHandle, so);
			// 其他协议或者rpc，马上在io线程继续派发。
			// 对于rpc.response还会继续调用dispatchRpcResponse继续派发。
			p.dispatch(this, factoryHandle);
		}
	}

	/*
	public <P extends Protocol<?>> void DispatchProtocol(P p, ProtocolFactoryHandle<P> factoryHandle) throws Exception {
		ProtocolHandle<P> handle = factoryHandle.Handle;
		if (handle != null) {
			if (isHandshakeProtocol(p.getTypeId())) {
				// handshake protocol call direct in io-thread.
				Task.call(() -> handle.handle(p), p, Protocol::trySendResultCode);
				return;
			}
			TransactionLevel level = factoryHandle.Level;
			Application zeze = this.zeze;
			// 为了避免redirect时死锁,这里一律不在whileCommit时执行
			if (zeze != null && level != TransactionLevel.None) {
				Task.runUnsafe(zeze.newProcedure(() -> handle.handle(p), p.getClass().getName(), level,
						p.getUserState()), p, Protocol::trySendResultCode, factoryHandle.Mode);
			} else
				Task.runUnsafe(() -> handle.handle(p), p, Protocol::trySendResultCode, null, factoryHandle.Mode);
		} else
			logger.warn("DispatchProtocol: Protocol Handle Not Found: {}", p);
	}
	*/

	/**
	 * @param data 方法外绝对不能持有data.Bytes的引用! 也就是只能在方法内读data, 只能处理data.ReadIndex到data.WriteIndex范围内
	 */
	@SuppressWarnings("RedundantThrows")
	public void dispatchUnknownProtocol(AsyncSocket so, int moduleId, int protocolId, ByteBuffer data) throws Exception {
		throw new UnsupportedOperationException("Unknown Protocol (" + moduleId + ", " + protocolId + ") size=" + data.Size());
	}

	@SuppressWarnings("RedundantThrows")
	public boolean checkOverflow(AsyncSocket so, long newSize, byte[] bytes, int offset, int length) throws Exception {
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
		public final Class<P> Class;
		public final long TypeId;
		public Factory<P> Factory;
		public ProtocolHandle<P> Handle;
		public TransactionLevel Level;
		public DispatchMode Mode;

		public ProtocolFactoryHandle(Class<P> protocolClass, long typeId) {
			Class = protocolClass;
			TypeId = typeId;
			Level = TransactionLevel.Serializable;
			Mode = DispatchMode.Normal;
		}

		public ProtocolFactoryHandle(Factory<P> factory) {
			this(factory, null, TransactionLevel.Serializable, DispatchMode.Normal);
		}

		public ProtocolFactoryHandle(Factory<P> factory, ProtocolHandle<P> handle) {
			this(factory, handle, TransactionLevel.Serializable, DispatchMode.Normal);
		}

		public ProtocolFactoryHandle(Factory<P> factory, ProtocolHandle<P> handle, TransactionLevel level) {
			this(factory, handle, level, DispatchMode.Normal);
		}

		@SuppressWarnings("unchecked")
		public ProtocolFactoryHandle(Factory<P> factory, ProtocolHandle<P> handle, TransactionLevel level,
									 DispatchMode mode) {
			P p = factory.create();
			Class = (Class<P>)p.getClass();
			TypeId = p.getTypeId();
			Factory = factory;
			Handle = handle;
			Level = level;
			Mode = mode;
		}
	}

	public final LongConcurrentHashMap<ProtocolFactoryHandle<? extends Protocol<?>>> getFactorys() {
		return factorys;
	}

	public final void AddFactoryHandle(long type, ProtocolFactoryHandle<? extends Protocol<?>> factory) {
		if (factorys.putIfAbsent(type, factory) != null)
			throw new IllegalStateException(String.format("duplicate factory type=%d moduleId=%d id=%d",
					type, type >>> 32, type & 0xffff_ffffL));
	}

	public final ProtocolFactoryHandle<? extends Protocol<?>> findProtocolFactoryHandle(long type) {
		return factorys.get(type);
	}

	/**
	 * Rpc Context. 模板不好放进去，使用基类 Protocol
	 */
	public final long addRpcContext(Protocol<?> p) {
		while (true) {
			long sessionId = nextSessionId();
			if (rpcContexts.putIfAbsent(sessionId, p) == null)
				return sessionId;
		}
	}

	@SuppressWarnings("unchecked")
	public final <T extends Protocol<?>> T removeRpcContext(long sid) {
		return (T)rpcContexts.remove(sid);
	}

	public final <T extends Protocol<?>> boolean removeRpcContext(long sid, T ctx) {
		return rpcContexts.remove(sid, ctx);
	}

	// Not Need Now
	public final LongHashMap<Protocol<?>> getRpcContextsToSender(AsyncSocket sender) {
		return getRpcContexts(p -> p.getSender() == sender);
	}

	public final LongHashMap<Protocol<?>> getRpcContexts(Predicate<Protocol<?>> filter) {
		var result = new LongHashMap<Protocol<?>>(Math.max(rpcContexts.size(), 1024)); // 初始容量先别定太大,可能只过滤出一小部分
		for (var it = rpcContexts.entryIterator(); it.moveToNext(); ) {
			if (filter.test(it.value()))
				result.put(it.key(), it.value());
		}
		return result;
	}

	public static abstract class ManualContext {
		private long sessionId;
		private Object userState;
		private boolean isTimeout;
		private Service service;

		public final long getSessionId() {
			return sessionId;
		}

		public final void setSessionId(long value) {
			sessionId = value;
		}

		public final Object getUserState() {
			return userState;
		}

		public final void setUserState(Object value) {
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

	public final long addManualContextWithTimeout(ManualContext context) {
		return addManualContextWithTimeout(context, 10 * 1000);
	}

	public final long addManualContextWithTimeout(ManualContext context, long timeout) { // 毫秒
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
	public final <T extends ManualContext> T tryGetManualContext(long sessionId) {
		return (T)manualContexts.get(sessionId);
	}

	public final <T extends ManualContext> T tryRemoveManualContext(long sessionId) {
		return tryRemoveManualContext(sessionId, false);
	}

	private <T extends ManualContext> T tryRemoveManualContext(long sessionId, boolean isTimeout) {
		@SuppressWarnings("unchecked")
		var r = (T)manualContexts.remove(sessionId);
		if (r != null) {
			try {
				r.setIsTimeout(isTimeout);
				r.onRemoved();
			} catch (Throwable e) { // run handle. 必须捕捉所有异常。
				logger.error("ManualContext.OnRemoved", e);
			}
		}
		return r;
	}

	// 还是不直接暴露内部的容器。提供这个方法给外面用。以后如果有问题，可以改这里。

	public final void foreach(Action1<AsyncSocket> action) throws Exception {
		for (var socket : socketMap)
			action.run(socket);
	}

	public KV<String, Integer> getOneAcceptorAddress() {
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

	public KV<String, Integer> getOnePassiveAddress() {
		var ipPort = getOneAcceptorAddress();
		// 允许系统来选择端口。
		//if (ipPort.getValue() == 0)
		//	throw new IllegalStateException("Acceptor: No Config.");

		if (ipPort.getKey().equals("@internal")) {
			ipPort.setKey(Helper.getOnePrivateNetworkInterfaceIpAddress());
		} else if (ipPort.getKey().equals("@external")) {
			ipPort.setKey(Helper.getOnePublicNetworkInterfaceIpAddress());
		}

		if (ipPort.getKey().isEmpty()) {
			// 可能绑定在任意地址上。尝试获得网卡的地址。
			ipPort.setKey(Helper.getOneNetworkInterfaceIpAddress());
			if (ipPort.getKey().isEmpty()) {
				// 实在找不到ip地址，就设置成loopback。
				logger.warn("PassiveAddress No Config. set ip to 127.0.0.1");
				ipPort.setKey("127.0.0.1");
			}
		}
		return ipPort;
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
			sender.close(); // 默认关闭连接。
			return false; // 超过速度控制，丢弃这条协议。
		}
		return true;
	}

	public boolean discard(AsyncSocket sender, int moduleId, int protocolId, int size) throws Exception {
		return false;
	}
}
