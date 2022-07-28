package Zeze.Net;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
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
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Service {
	protected static final Logger logger = LogManager.getLogger(Service.class);
	private static final AtomicLong StaticSessionIdAtomicLong = new AtomicLong(1);

	private final String Name;
	private final Application Zeze;
	private SocketOptions SocketOptions; // 同一个 Service 下的所有连接都是用相同配置。
	private ServiceConf Config;
	private LongSupplier SessionIdGenerator;
	protected final LongConcurrentHashMap<AsyncSocket> SocketMap = new LongConcurrentHashMap<>();
	private final LongConcurrentHashMap<ProtocolFactoryHandle<? extends Protocol<?>>> Factorys = new LongConcurrentHashMap<>();
	private final LongConcurrentHashMap<Protocol<?>> _RpcContexts = new LongConcurrentHashMap<>();
	private final LongConcurrentHashMap<ManualContext> ManualContexts = new LongConcurrentHashMap<>();

	public Service(String name) {
		Name = name;
		Zeze = null;
		SocketOptions = new SocketOptions();
	}

	public Service(String name, Config config) throws Throwable {
		Name = name;
		Zeze = null;
		InitConfig(config);
	}

	public Service(String name, Application app) throws Throwable {
		Name = name;
		Zeze = app;
		InitConfig(app != null ? app.getConfig() : null);
	}

	private void InitConfig(Config config) throws Throwable {
		Config = config != null ? config.GetServiceConf(Name) : null;
		if (Config == null) {
			// setup program default
			Config = new ServiceConf();
			if (config != null) {
				// reference to config default
				Config.setSocketOptions(config.getDefaultServiceConf().getSocketOptions());
				Config.setHandshakeOptions(config.getDefaultServiceConf().getHandshakeOptions());
			}
		}
		Config.SetService(this);
		SocketOptions = Config.getSocketOptions();
	}

	public final String getName() {
		return Name;
	}

	public final Application getZeze() {
		return Zeze;
	}

	public SocketOptions getSocketOptions() {
		return SocketOptions;
	}

	public void setSocketOptions(SocketOptions ops) {
		if (ops != null)
			SocketOptions = ops;
	}

	public ServiceConf getConfig() {
		return Config;
	}

	public void setConfig(ServiceConf conf) {
		Config = conf;
	}

	public final LongSupplier getSessionIdGenerator() {
		return SessionIdGenerator;
	}

	public final void setSessionIdGenerator(LongSupplier value) {
		SessionIdGenerator = value;
	}

	public final long NextSessionId() {
		LongSupplier gen = SessionIdGenerator;
		return gen != null ? gen.getAsLong() : StaticSessionIdAtomicLong.getAndIncrement();
	}

	public final int getSocketCount() {
		return SocketMap.size();
	}

	protected final LongConcurrentHashMap<AsyncSocket> getSocketMap() {
		return SocketMap;
	}

	/**
	 * 只包含成功建立的连接：服务器Accept和客户端Connected的连接。
	 *
	 * @param sessionId session id
	 * @return Socket Instance.
	 */
	public AsyncSocket GetSocket(long sessionId) {
		return SocketMap.get(sessionId);
	}

	public AsyncSocket GetSocket() {
		Iterator<AsyncSocket> sockets = SocketMap.iterator();
		return sockets.hasNext() ? sockets.next() : null;
	}

	public void Start() throws Throwable {
		if (Config != null)
			Config.Start();
	}

	public void Stop() throws Throwable {
		if (Config != null)
			Config.Stop();

		for (AsyncSocket as : SocketMap)
			as.close(); // remove in callback OnSocketClose

		// 先不清除，让Rpc的TimerTask仍然在超时以后触发回调。
		// 【考虑一下】也许在服务停止时马上触发回调并且清除上下文比较好。
		// 【注意】直接清除会导致同步等待的操作无法继续。异步只会没有回调，没问题。
		// _RpcContexts.Clear();
	}

	public final AsyncSocket NewServerSocket(String ipaddress, int port, Acceptor acceptor) {
		try {
			return NewServerSocket(InetAddress.getByName(ipaddress), port, acceptor);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}

	public final AsyncSocket NewServerSocket(InetAddress ipaddress, int port, Acceptor acceptor) {
		return NewServerSocket(new InetSocketAddress(ipaddress, port), acceptor);
	}

	public final AsyncSocket NewServerSocket(InetSocketAddress localEP, Acceptor acceptor) {
		return new AsyncSocket(this, localEP, acceptor);
	}

	public final AsyncSocket NewClientSocket(String hostNameOrAddress, int port, Object userState, Connector connector) {
		return new AsyncSocket(this, hostNameOrAddress, port, userState, connector);
	}

	/**
	 * ASocket 关闭的时候总是回调。
	 *
	 * @param so closing socket
	 * @param e  caught exception, null for none.
	 */
	public void OnSocketClose(AsyncSocket so, Throwable e) throws Throwable {
		SocketMap.remove(so.getSessionId(), so);
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
	public void OnSocketDisposed(@SuppressWarnings("unused") AsyncSocket so) throws Throwable {
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

	public final Collection<Protocol<?>> RemoveRpcContexts(Collection<Long> sids) {
		var result = new ArrayList<Protocol<?>>(sids.size());
		for (var sid : sids) {
			var ctx = RemoveRpcContext(sid);
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
	public void OnSocketAccept(AsyncSocket so) throws Throwable {
		SocketMap.putIfAbsent(so.getSessionId(), so);
		OnHandshakeDone(so);
	}

	@SuppressWarnings({"RedundantThrows", "MethodMayBeStatic"})
	public void OnSocketAcceptError(AsyncSocket listener, Throwable e) throws Throwable {
		logger.error("OnSocketAcceptError: {}", listener, e);
	}

	/**
	 * 连接完成建立调用。
	 * 未加密压缩的连接在 OnSocketAccept OnSocketConnected 里面调用这个方法。
	 * 加密压缩的连接在相应的方法中调用（see Services\Handshake.cs）。
	 * 注意：修改OnHandshakeDone的时机，需要重载OnSocketAccept OnSocketConnected，并且不再调用Service的默认实现。
	 */
	public void OnHandshakeDone(AsyncSocket sender) throws Throwable {
		sender.setHandshakeDone(true);
		if (sender.getConnector() != null)
			sender.getConnector().OnSocketHandshakeDone(sender);
	}

	/**
	 * 连接失败回调。同时也会回调OnSocketClose。
	 *
	 * @param so socket that connect error.
	 * @param e  exception caught
	 */
	@SuppressWarnings("RedundantThrows")
	public void OnSocketConnectError(AsyncSocket so, Throwable e) throws Throwable {
		SocketMap.remove(so.getSessionId(), so);
	}

	/**
	 * 连接成功回调。
	 *
	 * @param so connect succeed
	 */
	public void OnSocketConnected(AsyncSocket so) throws Throwable {
		SocketMap.putIfAbsent(so.getSessionId(), so);
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
	public void OnSocketProcessInputBuffer(AsyncSocket so, ByteBuffer input) throws Throwable {
		Protocol.Decode(this, so, input);
	}

	// 用来派发异步rpc回调。
	@SuppressWarnings("RedundantThrows")
	public <P extends Protocol<?>> void DispatchRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
															ProtocolFactoryHandle<?> factoryHandle) throws Throwable {
		if (Zeze != null && factoryHandle.Level != TransactionLevel.None) {
			Task.runRpcResponse(Zeze.NewProcedure(() -> responseHandle.handle(rpc), rpc.getClass().getName() + ":Response",
					factoryHandle.Level, rpc.getUserState()), factoryHandle.Mode);
		} else
			Task.runRpcResponse(() -> responseHandle.handle(rpc), rpc, factoryHandle.Mode);
	}

	public <P extends Protocol<?>> void DispatchProtocol2(Object key, P p, ProtocolFactoryHandle<P> factoryHandle) {
		if (factoryHandle.Handle != null) {
			if (factoryHandle.Level != TransactionLevel.None) {
				Zeze.getTaskOneByOneByKey().Execute(key, () ->
						Task.Call(Zeze.NewProcedure(() -> factoryHandle.Handle.handle(p), p.getClass().getName(),
								factoryHandle.Level, p.getUserState()), p, Protocol::trySendResultCode),
						factoryHandle.Mode);
			} else {
				Zeze.getTaskOneByOneByKey().Execute(key,
						() -> Task.Call(() -> factoryHandle.Handle.handle(p), p, Protocol::trySendResultCode),
						factoryHandle.Mode);
			}
		} else
			logger.warn("DispatchProtocol2: Protocol Handle Not Found: {}", p);
	}

	public boolean IsHandshakeProtocol(long typeId) {
		return false;
	}

	public <P extends Protocol<?>> void DispatchProtocol(P p, ProtocolFactoryHandle<P> factoryHandle) throws Throwable {
		ProtocolHandle<P> handle = factoryHandle.Handle;
		if (handle != null) {
			if (IsHandshakeProtocol(p.getTypeId())) {
				// handshake protocol call direct in io-thread.
				Task.Call(() -> handle.handle(p), p);
				return;
			}
			TransactionLevel level = factoryHandle.Level;
			Application zeze = Zeze;
			if (zeze != null && level != TransactionLevel.None)
				Task.run(zeze.NewProcedure(() -> handle.handle(p), p.getClass().getName(), level, p.getUserState()), p, factoryHandle.Mode);
			else
				Task.run(() -> handle.handle(p), p, factoryHandle.Mode);
		} else
			logger.warn("DispatchProtocol: Protocol Handle Not Found: {}", p);
	}

	/**
	 * @param data 方法外绝对不能持有data.Bytes的引用! 也就是只能在方法内读data, 只能处理data.ReadIndex到data.WriteIndex范围内
	 */
	@SuppressWarnings("RedundantThrows")
	public void DispatchUnknownProtocol(AsyncSocket so, int moduleId, int protocolId, ByteBuffer data) throws Throwable {
		throw new UnsupportedOperationException("Unknown Protocol (" + moduleId + ", " + protocolId + ") size=" + data.Size());
	}

	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 协议工厂
	 */
	public static class ProtocolFactoryHandle<P extends Protocol<?>> {
		public Factory<P> Factory;
		public ProtocolHandle<P> Handle;
		public TransactionLevel Level = TransactionLevel.Serializable;
		public DispatchMode Mode = DispatchMode.Normal;

		public ProtocolFactoryHandle() {
		}

		public ProtocolFactoryHandle(Factory<P> factory) {
			Factory = factory;
		}

		public ProtocolFactoryHandle(Factory<P> factory, ProtocolHandle<P> handle) {
			Factory = factory;
			Handle = handle;
		}

		public ProtocolFactoryHandle(Factory<P> factory, ProtocolHandle<P> handle, TransactionLevel level) {
			Factory = factory;
			Handle = handle;
			Level = level;
		}

		public ProtocolFactoryHandle(Factory<P> factory, ProtocolHandle<P> handle, TransactionLevel level, DispatchMode mode) {
			Factory = factory;
			Handle = handle;
			Level = level;
			Mode = mode;
		}
	}

	public final LongConcurrentHashMap<ProtocolFactoryHandle<? extends Protocol<?>>> getFactorys() {
		return Factorys;
	}

	public final void AddFactoryHandle(long type, ProtocolFactoryHandle<? extends Protocol<?>> factory) {
		if (Factorys.putIfAbsent(type, factory) != null)
			throw new IllegalStateException(String.format("duplicate factory type=%d moduleId=%d id=%d",
					type, type >>> 32, type & 0xffff_ffffL));
	}

	public final ProtocolFactoryHandle<? extends Protocol<?>> FindProtocolFactoryHandle(long type) {
		return Factorys.get(type);
	}

	/**
	 * Rpc Context. 模板不好放进去，使用基类 Protocol
	 */
	public final long AddRpcContext(Protocol<?> p) {
		while (true) {
			long sessionId = NextSessionId();
			if (_RpcContexts.putIfAbsent(sessionId, p) == null)
				return sessionId;
		}
	}

	@SuppressWarnings("unchecked")
	public final <T extends Protocol<?>> T RemoveRpcContext(long sid) {
		return (T)_RpcContexts.remove(sid);
	}

	public final <T extends Protocol<?>> boolean RemoveRpcContext(long sid, T ctx) {
		return _RpcContexts.remove(sid, ctx);
	}

	// Not Need Now
	public final LongHashMap<Protocol<?>> GetRpcContextsToSender(AsyncSocket sender) {
		return GetRpcContexts(p -> p.getSender() == sender);
	}

	public final LongHashMap<Protocol<?>> GetRpcContexts(Predicate<Protocol<?>> filter) {
		var result = new LongHashMap<Protocol<?>>(Math.max(_RpcContexts.size(), 1024)); // 初始容量先别定太大,可能只过滤出一小部分
		for (var it = _RpcContexts.entryIterator(); it.moveToNext(); ) {
			if (filter.test(it.value()))
				result.put(it.key(), it.value());
		}
		return result;
	}

	public abstract static class ManualContext {
		private long SessionId;
		private Object UserState;
		private boolean IsTimeout;

		public final long getSessionId() {
			return SessionId;
		}

		public final void setSessionId(long value) {
			SessionId = value;
		}

		public final Object getUserState() {
			return UserState;
		}

		public final void setUserState(Object value) {
			UserState = value;
		}

		public void OnRemoved() throws Throwable {
		}

		public boolean isTimeout() {
			return IsTimeout;
		}

		void setIsTimeout(boolean value) {
			IsTimeout = value;
		}

		private Service service;
		public Service getService() {
			return service;
		}
		public void setService(Service service) {
			this.service = service;
		}
	}

	public final long AddManualContextWithTimeout(ManualContext context) {
		return AddManualContextWithTimeout(context, 10 * 1000);
	}

	public final long AddManualContextWithTimeout(ManualContext context, long timeout) { // 毫秒
		while (true) {
			long sessionId = NextSessionId();
			if (ManualContexts.putIfAbsent(sessionId, context) == null) {
				context.setSessionId(sessionId);
				context.setService(this);
				Task.schedule(timeout, () -> TryRemoveManualContext(sessionId, true));
				return sessionId;
			}
		}
	}

	@SuppressWarnings("unchecked")
	public final <T extends ManualContext> T TryGetManualContext(long sessionId) {
		return (T)ManualContexts.get(sessionId);
	}

	public final <T extends ManualContext> T TryRemoveManualContext(long sessionId) {
		return TryRemoveManualContext(sessionId, false);
	}

	private <T extends ManualContext> T TryRemoveManualContext(long sessionId, boolean isTimeout) {
		@SuppressWarnings("unchecked")
		var r = (T)ManualContexts.remove(sessionId);
		if (r != null) {
			try {
				r.setIsTimeout(isTimeout);
				r.OnRemoved();
			} catch (Throwable skip) {
				logger.error("ManualContext.OnRemoved", skip);
			}
		}
		return r;
	}

	// 还是不直接暴露内部的容器。提供这个方法给外面用。以后如果有问题，可以改这里。

	public final void Foreach(Action1<AsyncSocket> action) throws Throwable {
		for (AsyncSocket socket : SocketMap)
			action.run(socket);
	}

	public static String GetOneNetworkInterfaceIpAddress() {
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				Enumeration<InetAddress> inetAddresses = interfaces.nextElement().getInetAddresses();
				if (inetAddresses.hasMoreElements())
					return inetAddresses.nextElement().getHostAddress();
			}
			return "";
		} catch (SocketException ex) {
			throw new RuntimeException(ex);
		}
	}

	public KV<String, Integer> GetOneAcceptorAddress() {
		var ipPort = KV.Create("", 0);

		Config.ForEachAcceptor2(a -> {
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

	public KV<String, Integer> GetOnePassiveAddress() {
		var ipPort = GetOneAcceptorAddress();
		if (ipPort.getValue() == 0)
			throw new IllegalStateException("Acceptor: No Config.");

		if (ipPort.getKey().isEmpty()) {
			// 可能绑定在任意地址上。尝试获得网卡的地址。
			ipPort.setKey(GetOneNetworkInterfaceIpAddress());
			if (ipPort.getKey().isEmpty()) {
				// 实在找不到ip地址，就设置成loopback。
				logger.warn("PassiveAddress No Config. set ip to 127.0.0.1");
				ipPort.setKey("127.0.0.1");
			}
		}
		return ipPort;
	}
}
