package Zeze.Net;

import Zeze.Serialize.*;
import Zeze.Transaction.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import Zeze.*;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class Service {
	private static final Logger logger = LogManager.getLogger(Service.class);

	/** 
	 同一个 Service 下的所有连接都是用相同配置。
	*/
	public SocketOptions SocketOptions = new SocketOptions();
	public ServiceConf Config;
	private Application Zeze;
	public final Application getZeze() {
		return Zeze;
	}
	private String Name;
	public final String getName() {
		return Name;
	}

	private java.util.concurrent.ConcurrentHashMap<Long, AsyncSocket> SocketMap = new java.util.concurrent.ConcurrentHashMap<Long, AsyncSocket> ();
	protected final java.util.concurrent.ConcurrentHashMap<Long, AsyncSocket> getSocketMap() {
		return SocketMap;
	}

	public final java.util.concurrent.ConcurrentHashMap<Long, AsyncSocket> getSocketMapInternal() {
		return getSocketMap();
	}

	private void InitConfig(Config config) {
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

	public Service(String name, Config config) {
		Name = name;
		InitConfig(config);
	}

	public Service(String name, Application app) {
		Name = name;
		Zeze = app;
		InitConfig(app == null ? null : app.getConfig());
	}

	public Service(String name) {
		Name = name;
	}

	/** 
	 只包含成功建立的连接：服务器Accept和客户端Connected的连接。
	 
	 @param serialNo
	 @return 
	*/
	public AsyncSocket GetSocket(long serialNo) {
		return getSocketMap().get(serialNo);
	}

	public AsyncSocket GetSocket() {
		for (var e : getSocketMap().entrySet()) {
			return e.getValue();
		}
		return null;
	}

	public void Start() {
		if (Config != null) {
			Config.Start();
		}
	}

	public void Stop() {
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

	public final AsyncSocket NewServerSocket(String ipaddress, int port) {
		return NewServerSocket(InetAddress.getByName(ipaddress), port);
	}

	public final AsyncSocket NewServerSocket(InetAddress ipaddress, int port) {
		return NewServerSocket(new InetSocketAddress(ipaddress, port));
	}

	public final AsyncSocket NewServerSocket(InetSocketAddress localEP) {
		return new AsyncSocket(this, localEP);
	}


	public final AsyncSocket NewClientSocket(String hostNameOrAddress, int port) {
		return NewClientSocket(hostNameOrAddress, port, null);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public AsyncSocket NewClientSocket(string hostNameOrAddress, int port, object userState = null)
	public final AsyncSocket NewClientSocket(String hostNameOrAddress, int port, Object userState) {
		return new AsyncSocket(this, hostNameOrAddress, port, userState);
	}

	/** 
	 ASocket 关闭的时候总是回调。
	 
	 @param so
	 @param e
	*/
	public void OnSocketClose(AsyncSocket so, RuntimeException e) {
		TValue _;
		tangible.OutObject<AsyncSocket> tempOut__ = new tangible.OutObject<AsyncSocket>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getSocketMap().TryRemove(so.getSessionId(), tempOut__);
	_ = tempOut__.outArgValue;

		if (null != e) {
			logger.Log(getSocketOptions().getSocketLogLevel(), e, "OnSocketClose");
		}
	}

	/** 
	 可靠rpc调用：一般用于重新发送没有返回结果的rpc。
	 在 OnSocketClose 之后调用，此时外面【必须】拿不到此 AsyncSocket 了。
	 当 OnSocketDisposed 调用发生时，AsyncSocket.Socket已经设为 null。
	 对于那些在 AsyncSocket.Dispose 时已经得到的 AsyncSocket 引用，
	 使用时判断返回值：主要是 Send 返回 false。
	 
	 @param so
	*/
	public void OnSocketDisposed(AsyncSocket so) {
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
		return GetRpcContexts((p) -> p.Sender == sender);
	}

	public final HashMap<Long, Protocol> GetRpcContexts(tangible.Func1Param<Protocol, Boolean> filter) {
		var result = new HashMap<Long, Protocol>(getRpcContexts().Count);
		for (var ctx : getRpcContexts()) {
			if (filter.invoke(ctx.Value)) {
				result.put(ctx.Key, ctx.Value);
			}
		}
		return result;
	}

	public final Collection<Protocol> RemoveRpcContets(Collection<Long> sids) {
		var result = new ArrayList<Protocol>(sids.size());
		for (var sid : sids) {
			var ctx = this.<Protocol>RemoveRpcContext(sid);
			if (null != ctx) {
				result.add(ctx);
			}
		}
		return result;
	}

	/** 
	 服务器接受到新连接回调。
	 
	 @param so
	*/
	public void OnSocketAccept(AsyncSocket so) {
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getSocketMap().TryAdd(so.getSessionId(), so);
		OnHandshakeDone(so);
	}

	/** 
	 连接完成建立调用。
	 未加密压缩的连接在 OnSocketAccept OnSocketConnected 里面调用这个方法。
	 加密压缩的连接在相应的方法中调用（see Services\Handshake.cs）。
	 注意：修改OnHandshakeDone的时机，需要重载OnSocketAccept OnSocketConnected，并且不再调用Service的默认实现。
	*/
	public void OnHandshakeDone(AsyncSocket sender) {
		sender.setHandshakeDone(true);
		if (sender.getConnector() != null) {
			sender.getConnector().OnSocketHandshakeDone(sender);
		}
	}

	/** 
	 连接失败回调。同时也会回调OnSocketClose。
	 
	 @param so
	 @param e
	*/
	public void OnSocketConnectError(AsyncSocket so, RuntimeException e) {
		TValue _;
		tangible.OutObject<AsyncSocket> tempOut__ = new tangible.OutObject<AsyncSocket>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getSocketMap().TryRemove(so.getSessionId(), tempOut__);
	_ = tempOut__.outArgValue;
		logger.Log(getSocketOptions().getSocketLogLevel(), e, "OnSocketConnectError");
	}

	/** 
	 连接成功回调。
	 
	 @param so
	*/
	public void OnSocketConnected(AsyncSocket so) {
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getSocketMap().TryAdd(so.getSessionId(), so);
		OnHandshakeDone(so);
	}

	/** 
	 处理数据。
	 在异步线程中回调，要注意线程安全。
	 
	 @param so
	 @param input
	*/
	public void OnSocketProcessInputBuffer(AsyncSocket so, ByteBuffer input) {
		Protocol.Decode(this, so, input);
	}

	// 用来派发异步rpc回调。
	public void DispatchRpcResponse(Protocol rpc, tangible.Func1Param<Protocol, Integer> responseHandle, ProtocolFactoryHandle factoryHandle) {
		if (null != getZeze() && false == factoryHandle.getNoProcedure()) {
			Zeze.Util.Task.Run(getZeze().NewProcedure(() -> responseHandle.invoke(rpc), rpc.getClass().getName() + ":Response", rpc.UserState));
		}
		else {
			Zeze.Util.Task.Run(() -> responseHandle.invoke(rpc), rpc);
		}
	}

	public final void DispatchProtocol2(Object key, Protocol p, ProtocolFactoryHandle factoryHandle) {
		if (null != factoryHandle.getHandle()) {
			if (null != getZeze() && false == factoryHandle.getNoProcedure()) {
				getZeze().getTaskOneByOneByKey().Execute(key, () -> Zeze.Util.Task.Call(getZeze().NewProcedure(() -> factoryHandle.Handle(p), p.getClass().getName(), p.UserState), p, (p, code) -> p.SendResultCode(code)));
			}
			else {
				getZeze().getTaskOneByOneByKey().Execute(key, () -> Zeze.Util.Task.Call(() -> factoryHandle.Handle(p), p, (p, code) -> p.SendResultCode(code)));
			}
		}
		else {
			logger.Log(getSocketOptions().getSocketLogLevel(), "Protocol Handle Not Found. {0}", p);
		}
	}

	public void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle) {
		if (null != factoryHandle.getHandle()) {
			if (null != getZeze() && false == factoryHandle.getNoProcedure()) {
				Zeze.Util.Task.Run(getZeze().NewProcedure(() -> factoryHandle.Handle(p), p.getClass().getName(), p.UserState), p);
			}
			else {
				Zeze.Util.Task.Run(() -> factoryHandle.Handle(p), p);
			}
		}
		else {
			logger.Log(getSocketOptions().getSocketLogLevel(), "Protocol Handle Not Found. {0}", p);
		}
	}

	public void DispatchUnknownProtocol(AsyncSocket so, int type, ByteBuffer data) {
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
		throw new RuntimeException("Unknown Protocol (" + (type >> 16 & 0xffff) + ", " + (type & 0xffff) + ") size=" + data.getSize());
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	/** 协议工厂
	*/
	@FunctionalInterface
	public static interface IProtocolFactory {
		Protocol create();
	}

	@FunctionalInterface
	public static interface IProtocolHandle {
		int handle(Protocol p);
	}
	
	public static class ProtocolFactoryHandle {
		public IProtocolFactory Factory;
		public IProtocolHandle Handle;
		public boolean NoProcedure = false;
	}

	private java.util.concurrent.ConcurrentHashMap<Integer, ProtocolFactoryHandle> Factorys = new java.util.concurrent.ConcurrentHashMap<Integer, ProtocolFactoryHandle> ();
	public final java.util.concurrent.ConcurrentHashMap<Integer, ProtocolFactoryHandle> getFactorys() {
		return Factorys;
	}

	public final void AddFactoryHandle(int type, ProtocolFactoryHandle factory) {
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (false == getFactorys().TryAdd(type, factory)) {
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
			throw new RuntimeException(String.format("duplicate factory type=%1$s moduleid=%2$s id=%3$s", type, (type >> 16) & 0x7fff, type & 0x7fff));
		}
	}

	public static <T extends Protocol> tangible.Func1Param<Protocol, Integer> MakeHandle(Object target, java.lang.reflect.Method method) {
		return (Protocol p) -> {
				if (method.IsStatic) {
					var handler = Delegate.CreateDelegate(tangible.Func1Param<T, Integer>.class, method);
					return ((tangible.Func1Param<T, Integer>)handler)((T)p);
				}
				else {
					var handler = Delegate.CreateDelegate(tangible.Func1Param<T, Integer>.class, target, method);
					return ((tangible.Func1Param<T, Integer>)handler)((T)p);
				}
		};
	}

	public final ProtocolFactoryHandle FindProtocolFactoryHandle(int type) {
		ProtocolFactoryHandle factory;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut_factory = new tangible.OutObject < getZeze().Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (getFactorys().TryGetValue(type, tempOut_factory)) {
		factory = tempOut_factory.outArgValue;
			return factory;
		}
	else {
		factory = tempOut_factory.outArgValue;
	}

		return null;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	/** Rpc Context. 模板不好放进去，使用基类 Protocol
	*/
	private static Util.AtomicLong StaticSessionIdAtomicLong = new Util.AtomicLong();
	private static Util.AtomicLong getStaticSessionIdAtomicLong() {
		return StaticSessionIdAtomicLong;
	}
	private tangible.Func0Param<Long> SessionIdGenerator;
	public final tangible.Func0Param<Long> getSessionIdGenerator() {
		return SessionIdGenerator;
	}
	public final void setSessionIdGenerator(tangible.Func0Param<Long> value) {
		SessionIdGenerator = value;
	}

	private final java.util.concurrent.ConcurrentHashMap<Long, Protocol> _RpcContexts = new java.util.concurrent.ConcurrentHashMap<Long, Protocol>();
	public final IReadOnlyDictionary<Long, Protocol> getRpcContexts() {
		return _RpcContexts;
	}

	public final long NextSessionId() {
		if (null != getSessionIdGenerator()) {
			return SessionIdGenerator();
		}
		return getStaticSessionIdAtomicLong().IncrementAndGet();
	}

	public final long AddRpcContext(Protocol p) {
		while (true) {
			long sessionId = NextSessionId();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
			if (_RpcContexts.TryAdd(sessionId, p)) {
				return sessionId;
			}
		}
	}

	public final <T extends Protocol> T RemoveRpcContext(long sid) {
		TValue p;
		tangible.OutObject<Protocol> tempOut_p = new tangible.OutObject<Protocol>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (_RpcContexts.TryRemove(sid, tempOut_p)) {
		p = tempOut_p.outArgValue;
			return (T)p;
		}
	else {
		p = tempOut_p.outArgValue;
	}
		return null;
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

		public void OnRemoved() {
		}

		// after OnRemoved if Timeout
		public void OnTimeout() {
		}

	}

	private final java.util.concurrent.ConcurrentHashMap<Long, ManualContext> ManualContexts = new java.util.concurrent.ConcurrentHashMap<Long, ManualContext>();


	public final long AddManualContextWithTimeout(ManualContext context) {
		return AddManualContextWithTimeout(context, 10 * 1000);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public long AddManualContextWithTimeout(ManualContext context, long timeout = 10*1000)
	public final long AddManualContextWithTimeout(ManualContext context, long timeout) {
		while (true) {
			long sessionId = NextSessionId();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
			if (ManualContexts.TryAdd(sessionId, context)) {
				context.setSessionId(sessionId);
				Util.Scheduler.getInstance().Schedule((ThisTask) -> this.<ManualContext>TryRemoveManualContext(sessionId) == null ? null : this.<ManualContext>TryRemoveManualContext(sessionId).OnTimeout(), timeout, -1);
				return sessionId;
			}
		}
	}

	public final <T extends ManualContext> T TryGetManualContext(long sessionId) {
		TValue c;
		tangible.OutObject<TValue> tempOut_c = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (ManualContexts.TryGetValue(sessionId, tempOut_c)) {
		c = tempOut_c.outArgValue;
			return (T)c;
		}
	else {
		c = tempOut_c.outArgValue;
	}
		return null;
	}

	public final <T extends ManualContext> T TryRemoveManualContext(long sessionId) {
		TValue c;
		tangible.OutObject<ManualContext> tempOut_c = new tangible.OutObject<ManualContext>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (ManualContexts.TryRemove(sessionId, tempOut_c)) {
		c = tempOut_c.outArgValue;
			c.OnRemoved();
			return (T)c;
		}
	else {
		c = tempOut_c.outArgValue;
	}
		return null;
	}

	// 还是不直接暴露内部的容器。提供这个方法给外面用。以后如果有问题，可以改这里。
	public final void Foreach(tangible.Action1Param<AsyncSocket> action) {
		for (var socket : getSocketMap().values()) {
			action.invoke(socket);
		}
	}


	public final String GetOneNetworkInterfaceIpAddress() {
		return GetOneNetworkInterfaceIpAddress(AddressFamily.Unspecified);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public string GetOneNetworkInterfaceIpAddress(AddressFamily family = AddressFamily.Unspecified)
	public final String GetOneNetworkInterfaceIpAddress(AddressFamily family) {
		for (NetworkInterface neti : NetworkInterface.GetAllNetworkInterfaces()) {
			if (neti.NetworkInterfaceType == NetworkInterfaceType.Loopback) {
				continue;
			}

			IPInterfaceProperties property = neti.GetIPProperties();
			for (UnicastIPAddressInformation ip : property.UnicastAddresses) {
				switch (ip.Address.AddressFamily) {
					case InterNetworkV6:
					case InterNetwork:
						if (family == AddressFamily.Unspecified || family == ip.Address.AddressFamily) {
							return ip.Address.toString();
						}
						continue;
				}
			}
		}
		return null;
	}

//C# TO JAVA CONVERTER TODO TASK: Methods returning tuples are not converted by C# to Java Converter:
//	public (string, int) GetOneAcceptorAddress()
//		{
//			string ip = string.Empty;
//			int port = 0;
//
//			Config.ForEachAcceptor((a) =>
//				{
//					if (false == string.IsNullOrEmpty(a.Ip) && a.Port != 0)
//					{
//						// 找到ip，port都配置成明确地址的。
//						ip = a.Ip;
//						port = a.Port;
//						return false;
//					}
//					// 获得最后一个配置的port。允许返回(null, port)。
//					port = a.Port;
//					return true;
//				}
//				);
//
//			return (ip, port);
//		}

//C# TO JAVA CONVERTER TODO TASK: Methods returning tuples are not converted by C# to Java Converter:
//	public (string, int) GetOnePassiveAddress()
//		{
//			var(ip, port) = GetOneAcceptorAddress();
//			if (port == 0)
//				throw new Exception("Acceptor: No Config.");
//
//			if (string.IsNullOrEmpty(ip))
//			{
//				// 可能绑定在任意地址上。尝试获得网卡的地址。
//				ip = GetOneNetworkInterfaceIpAddress();
//				if (string.IsNullOrEmpty(ip))
//				{
//					// 实在找不到ip地址，就设置成loopback。
//					logger.Warn("PassiveAddress No Config. set ip to 127.0.0.1");
//					ip = "127.0.0.1";
//				}
//			}
//			return (ip, port);
//		}
}