package Zeze.Net;

import Zeze.Serialize.*;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Str;
import Zeze.Util.Task;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import Zeze.Util.KV;
import java.util.concurrent.ConcurrentHashMap;

public class Service {
	private static final Logger logger = LogManager.getLogger(Service.class);

	/** 
	 同一个 Service 下的所有连接都是用相同配置。
	*/
	private SocketOptions SocketOptions = new SocketOptions();
	public SocketOptions getSocketOptions() {
		return SocketOptions;
	}
	public void setSocketOptions(SocketOptions ops) {
		SocketOptions = ops;
	}

	private ServiceConf Config;
	public ServiceConf getConfig() {
		return Config;
	}
	public void setConfig(ServiceConf conf) {
		Config = conf;
	}

	private Application Zeze;
	public final Application getZeze() {
		return Zeze;
	}
	private final String Name;
	public final String getName() {
		return Name;
	}

	protected ConcurrentHashMap<Long, AsyncSocket> SocketMap = new ConcurrentHashMap<> ();
	protected final ConcurrentHashMap<Long, AsyncSocket> getSocketMap() {
		return SocketMap;
	}

	public final ConcurrentHashMap<Long, AsyncSocket> getSocketMapInternal() {
		return getSocketMap();
	}

	private void InitConfig(Zeze.Config config) throws Throwable {
		Config = config == null ? null : config.GetServiceConf(getName());
		if (null == Config) {
			// setup program default
			Config = new ServiceConf();
			if (null != config) {
				// reference to config default
				Config.setSocketOptions(config.getDefaultServiceConf().getSocketOptions());
				Config.setHandshakeOptions(config.getDefaultServiceConf().getHandshakeOptions());
			}
		}
		Config.SetService(this);
		SocketOptions= Config.getSocketOptions();
	}

	public Service(String name, Zeze.Config config) throws Throwable {
		Name = name;
		InitConfig(config);
	}

	public Service(String name, Application app) throws Throwable {
		Name = name;
		Zeze = app;
		InitConfig(app == null ? null : app.getConfig());
	}

	public Service(String name) {
		Name = name;
	}

	/** 
	 只包含成功建立的连接：服务器Accept和客户端Connected的连接。
	 
	 @param sessionId
	 session id
	 @return
	 Socket Instance.
	*/
	public AsyncSocket GetSocket(long sessionId) {
		return getSocketMap().get(sessionId);
	}

	public AsyncSocket GetSocket() {
		for (var e : getSocketMap().entrySet()) {
			return e.getValue();
		}
		return null;
	}

	public void Start() throws Throwable {
		if (Config != null) {
			Config.Start();
		}
	}

	public void Stop() throws Throwable {
		if (Config != null) {
			Config.Stop();
		}

		for (var e : getSocketMap().entrySet()) {
			e.getValue().close(); // remove in callback OnSocketClose
		}

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
	 ASocket 关闭的时候总是回调。
	 
	 @param so
	 closing socket
	 @param e
	 catched exception, null for none.
	*/
	public void OnSocketClose(AsyncSocket so, Throwable e) throws Throwable {
		SocketMap.remove(so.getSessionId(), so);
		if (null != e) {
			logger.log(getSocketOptions().getSocketLogLevel(), "OnSocketClose", e);
		}
	}

	/** 
	 可靠rpc调用：一般用于重新发送没有返回结果的rpc。
	 在 OnSocketClose 之后调用，此时外面【必须】拿不到此 AsyncSocket 了。
	 当 OnSocketDisposed 调用发生时，AsyncSocket.Socket已经设为 null。
	 对于那些在 AsyncSocket.Dispose 时已经得到的 AsyncSocket 引用，
	 使用时判断返回值：主要是 Send 返回 false。
	 
	 @param so
	 after socket closed. last callback.
	*/
	public void OnSocketDisposed(AsyncSocket so) throws Throwable {
		// 一般实现：遍历RpcContexts，
		/*
		var ctxSends = GetRpcContextsToSender(so);
		var ctxPending = RemoveRpcContets(ctxSends.Keys);
		foreach (var ctx in ctxRemoved)
		{
		    // process
		}
		*/
	}

	// Not Need Now
	public final HashMap<Long, Protocol> GetRpcContextsToSender(AsyncSocket sender) {
		return GetRpcContexts((p) -> p.getSender() == sender);
	}

	public final HashMap<Long, Protocol> GetRpcContexts(RpcContextFilter filter) {
		var result = new HashMap<Long, Protocol>(_RpcContexts.size());
		for (var ctx : _RpcContexts.entrySet()) {
			if (filter.invoke(ctx.getValue())) {
				result.put(ctx.getKey(), ctx.getValue());
			}
		}
		return result;
	}

	public final Collection<Protocol> RemoveRpcContets(Collection<Long> sids) {
		var result = new ArrayList<Protocol>(sids.size());
		for (var sid : sids) {
			var ctx = this.RemoveRpcContext(sid);
			if (null != ctx) {
				result.add(ctx);
			}
		}
		return result;
	}

	/** 
	 服务器接受到新连接回调。
	 
	 @param so
	 new socket accepted.
	*/
	public void OnSocketAccept(AsyncSocket so) throws Throwable {
		SocketMap.putIfAbsent(so.getSessionId(), so);
		OnHandshakeDone(so);
	}

	public void OnSocketAcceptError(AsyncSocket listener, Throwable e) throws Throwable {
		logger.log(getSocketOptions().getSocketLogLevel(), () -> "OnSocketAcceptError" + listener, e);
	}
	/** 
	 连接完成建立调用。
	 未加密压缩的连接在 OnSocketAccept OnSocketConnected 里面调用这个方法。
	 加密压缩的连接在相应的方法中调用（see Services\Handshake.cs）。
	 注意：修改OnHandshakeDone的时机，需要重载OnSocketAccept OnSocketConnected，并且不再调用Service的默认实现。
	*/
	public void OnHandshakeDone(AsyncSocket sender) throws Throwable {
		sender.setHandshakeDone(true);
		if (sender.getConnector() != null) {
			sender.getConnector().OnSocketHandshakeDone(sender);
		}
	}

	/** 
	 连接失败回调。同时也会回调OnSocketClose。
	 
	 @param so
	 socket that connect error.
	 @param e
	 excepton catched
	*/
	public void OnSocketConnectError(AsyncSocket so, Throwable e) throws Throwable {
		SocketMap.remove(so.getSessionId(), so);
		logger.log(getSocketOptions().getSocketLogLevel(), "OnSocketConnectError", e);
	}

	/** 
	 连接成功回调。
	 
	 @param so
	 connect successed
	*/
	public void OnSocketConnected(AsyncSocket so) throws Throwable {
		SocketMap.putIfAbsent(so.getSessionId(), so);
		OnHandshakeDone(so);
	}

	/** 
	 处理数据。
	 在异步线程中回调，要注意线程安全。
	 
	 @param so
	 current socket
	 @param input
	 data
	*/
	public void OnSocketProcessInputBuffer(AsyncSocket so, ByteBuffer input) throws Throwable {
		Protocol.Decode(this, so, input);
	}

	// 用来派发异步rpc回调。
	public void DispatchRpcResponse(Protocol rpc, ProtocolHandle responseHandle, ProtocolFactoryHandle factoryHandle) throws Throwable {
		if (null != getZeze() && TransactionLevel.None != factoryHandle.Level) {
			Task.Run(getZeze().NewProcedure(
					() -> responseHandle.handle(rpc),
					rpc.getClass().getName() + ":Response",
					factoryHandle.Level,
					rpc.getUserState()));
		}
		else {
			Task.Run(() -> responseHandle.handle(rpc), rpc);
		}
	}

	public final void DispatchProtocol2(Object key, Protocol p, ProtocolFactoryHandle factoryHandle) throws Throwable {
		if (null != factoryHandle.Handle) {
			if (TransactionLevel.None != factoryHandle.Level) {
				getZeze().getTaskOneByOneByKey().Execute(key, () ->
					Task.Call(getZeze().NewProcedure(
							() -> factoryHandle.Handle.handle(p),
							p.getClass().getName(),
							factoryHandle.Level,
							p.getUserState()),
							p,
							Protocol::SendResultCode
							)
					);
			}
			else {
				getZeze().getTaskOneByOneByKey().Execute(key,
						() -> Task.Call(() -> factoryHandle.Handle.handle(p),
								p, Protocol::SendResultCode));
			}
		}
		else {
			logger.log(SocketOptions.getSocketLogLevel(), "Protocol Handle Not Found. {}", p);
		}
	}

	public void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle) throws Throwable {
		if (null != factoryHandle.Handle) {
			if (null != getZeze() && TransactionLevel.None != factoryHandle.Level) {
				Task.Run(getZeze().NewProcedure(
						() -> factoryHandle.Handle.handle(p),
						p.getClass().getName(),
						factoryHandle.Level,
						p.getUserState()),
						p);
			}
			else {
				Task.Run(() -> factoryHandle.Handle.handle(p), p);
			}
		}
		else {
			logger.log(SocketOptions.getSocketLogLevel(), "Protocol Handle Not Found. {0}", p);
		}
	}

	public void DispatchUnknownProtocol(AsyncSocket so, int moduleId, int protocolId, ByteBuffer data) throws Throwable {
		throw new RuntimeException("Unknown Protocol (" + moduleId + ", " + protocolId + ") size=" + data.Size());
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	/** 协议工厂
	*/	
	public static class ProtocolFactoryHandle {
		public Zeze.Util.Factory<Protocol> Factory;
		public ProtocolHandle Handle;
		public TransactionLevel Level = TransactionLevel.Serializable;
		public ProtocolFactoryHandle() { }

		public ProtocolFactoryHandle(Zeze.Util.Factory<Protocol> factory) {
			this.Factory = factory;
		}

		public ProtocolFactoryHandle(Zeze.Util.Factory<Protocol> factory, ProtocolHandle handle) {
			this.Factory = factory;
			this.Handle = handle;
		}

		public ProtocolFactoryHandle(Zeze.Util.Factory<Protocol> factory, ProtocolHandle handle, TransactionLevel level) {
			this.Factory = factory;
			this.Handle = handle;
			this.Level = level;
		}
	}

	private final ConcurrentHashMap<Long, ProtocolFactoryHandle> Factorys = new ConcurrentHashMap<> ();
	public final ConcurrentHashMap<Long, ProtocolFactoryHandle> getFactorys() {
		return Factorys;
	}

	public final void AddFactoryHandle(long type, ProtocolFactoryHandle factory) {
		if (null != getFactorys().putIfAbsent(type, factory)) {
			throw new RuntimeException(Str.format("duplicate factory type={} moduleid={} id={}", type, (type >>> 16) & 0x7fff, type & 0x7fff));
		}
	}

	public final ProtocolFactoryHandle FindProtocolFactoryHandle(long type) {
		return Factorys.get(type);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	/** Rpc Context. 模板不好放进去，使用基类 Protocol
	*/
	private static final AtomicLong StaticSessionIdAtomicLong = new AtomicLong();
	private static AtomicLong getStaticSessionIdAtomicLong() {
		return StaticSessionIdAtomicLong;
	}
	private SessionIdGenerator SessionIdGenerator;
	public final SessionIdGenerator getSessionIdGenerator() {
		return SessionIdGenerator;
	}
	public final void setSessionIdGenerator(SessionIdGenerator value) {
		SessionIdGenerator = value;
	}

	private final ConcurrentHashMap<Long, Protocol> _RpcContexts = new ConcurrentHashMap<>();
	public final long NextSessionId() {
		if (null != SessionIdGenerator) {
			return SessionIdGenerator.next();
		}
		return getStaticSessionIdAtomicLong().incrementAndGet();
	}

	public final long AddRpcContext(Protocol p) {
		while (true) {
			long sessionId = NextSessionId();
			if (null == _RpcContexts.putIfAbsent(sessionId, p)) {
				return sessionId;
			}
		}
	}

	public final <T extends Protocol> T RemoveRpcContext(long sid) {
		@SuppressWarnings("unchecked")
		var t = (T)_RpcContexts.remove(sid);
		return t;
	}

	public abstract static class ManualContext {
		private long SessionId;
		public final long getSessionId() {
			return SessionId;
		}
		public final void setSessionId(long value) {
			SessionId = value;
		}
		private Object UserState;
		public final Object getUserState() {
			return UserState;
		}
		public final void setUserState(Object value) {
			UserState = value;
		}

		public void OnRemoved() throws Throwable {
		}

		// after OnRemoved if Timeout
		public void OnTimeout() throws Throwable {
	}

	}

	private final java.util.concurrent.ConcurrentHashMap<Long, ManualContext> ManualContexts = new java.util.concurrent.ConcurrentHashMap<>();


	public final long AddManualContextWithTimeout(ManualContext context) {
		return AddManualContextWithTimeout(context, 10 * 1000);
	}

	public final long AddManualContextWithTimeout(ManualContext context, long timeout) {
		while (true) {
			long sessionId = NextSessionId();
			if (null == ManualContexts.putIfAbsent(sessionId, context)) {
				context.setSessionId(sessionId);
				Task.schedule((ThisTask) -> {
					ManualContext ctx = this.TryRemoveManualContext(sessionId);
						if (null != ctx) {
							ctx.OnTimeout();
						}
					}, timeout, -1);
				return sessionId;
			}
		}
	}

	public final <T extends ManualContext> T TryGetManualContext(long sessionId) {
		@SuppressWarnings("unchecked")
		var r = (T)ManualContexts.get(sessionId);
		return r;
	}

	public final <T extends ManualContext> T TryRemoveManualContext(long sessionId) {
		@SuppressWarnings("unchecked")
		var r = (T)ManualContexts.remove(sessionId);
		if (null != r) {
			try {
				r.OnRemoved();
			} catch (Throwable skip) {
				logger.error(skip);
			}
		}
		return r;
	}

	// 还是不直接暴露内部的容器。提供这个方法给外面用。以后如果有问题，可以改这里。

	public final void Foreach(Zeze.Util.Action1<AsyncSocket> action) throws Throwable {
		for (var socket : getSocketMap().values()) {
			action.run(socket);
		}
	}


	public final String GetOneNetworkInterfaceIpAddress() {
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface networkInterface = interfaces.nextElement();
				Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
				if (inetAddresses.hasMoreElements()) {
					var inetaddr = inetAddresses.nextElement();
					return inetaddr.getHostAddress();
				}
			}
			return "";
		} catch (SocketException ex) {
			throw new RuntimeException(ex);
		}
	}

	public Zeze.Util.KV<String, Integer> GetOneAcceptorAddress() {
		final var ipport = KV.Create("", 0);

		Config.ForEachAcceptor2((a) -> {
			if (!a.getIp().isEmpty() && a.getPort() != 0) {
				// 找到ip，port都配置成明确地址的。
				ipport.setKey(a.getIp());
				ipport.setValue(a.getPort());
				return false;
			}
			// 获得最后一个配置的port。允许返回(null, port)。
			ipport.setValue(a.getPort());
			return true;
		});

		return ipport;
	}

	public Zeze.Util.KV<String, Integer> GetOnePassiveAddress() {
		var ipport = GetOneAcceptorAddress();
		if (ipport.getValue() == 0)
			throw new RuntimeException("Acceptor: No Config.");

		if (ipport.getKey().isEmpty()) {
			// 可能绑定在任意地址上。尝试获得网卡的地址。
			ipport.setKey(GetOneNetworkInterfaceIpAddress());
			if (ipport.getKey().isEmpty()) {
				// 实在找不到ip地址，就设置成loopback。
				logger.warn("PassiveAddress No Config. set ip to 127.0.0.1");
				ipport.setKey("127.0.0.1");
			}
		}
		return ipport;
	}
}