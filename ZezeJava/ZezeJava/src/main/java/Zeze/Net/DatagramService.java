package Zeze.Net;

import java.io.IOException;
import java.net.InetSocketAddress;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.OutObject;
import Zeze.Util.ReplayAttackPolicy;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatagramService {
	protected static final Logger logger = LogManager.getLogger(DatagramService.class);
	private final String name;
	private final Application zeze;
	private SocketOptions socketOptions; // 同一个 Service 下的所有连接都是用相同配置。
	private ServiceConf config;
	protected final LongConcurrentHashMap<DatagramSocket> socketMap = new LongConcurrentHashMap<>(); // key: DatagramSocket的sessionId
	private final LongConcurrentHashMap<Service.ProtocolFactoryHandle<? extends Protocol<?>>> factorys = new LongConcurrentHashMap<>(); // key: 协议type

	private Selectors selectors;

	public DatagramService(String name) {
		this(name, (Config)null);
	}

	public DatagramService(String name, Config config) {
		this.name = name;
		zeze = null;
		initConfig(config);
	}

	public DatagramService(String name, Application app) {
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
		this.config.setService(null);
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

	public DatagramSocket bind(InetSocketAddress inetAddress) throws IOException {
		return new DatagramSocket(this, inetAddress);
	}

	public DatagramSession createSession(InetSocketAddress local, InetSocketAddress remote, long sessionId,
										 byte[] securityKey, ReplayAttackPolicy policy) throws IOException {
		return bind(local).createSession(remote, sessionId, securityKey, policy);
	}

	public void start() {
	}

	public void stop() {
		socketMap.forEach(socket -> {
			try {
				socket.close();
			} catch (Exception e) {
				logger.error("socket.close({}) exception:", socket, e);
			}
		});
	}

	public final int getSocketCount() {
		return socketMap.size();
	}

	protected final boolean addSocket(DatagramSocket so) {
		return socketMap.putIfAbsent(so.getSessionId(), so) == null;
	}

	public final DatagramSocket getSocket(long sessionId) {
		return socketMap.get(sessionId);
	}

	/**
	 * @param input 方法外绝对不能持有bb.Bytes的引用! 也就是只能在方法内读input.
	 */
	public void onProcessDatagram(DatagramSession sender, ByteBuffer input, long serialId) throws Exception {
		// 由于参数不同，需要重新实现一个。
		var moduleId = input.ReadInt4();
		var protocolId = input.ReadInt4();
		input.ReadInt4(); // size, 对UDP帧来说没什么用
		var typeId = Protocol.makeTypeId(moduleId, protocolId);
		dispatchProtocol(input, findProtocolFactoryHandle(typeId), sender, serialId);
	}

	@SuppressWarnings("MethodMayBeStatic")
	public void onSocketException(DatagramSocket sender, Throwable e) {
		logger.error("onSocketException({})", sender, e);
		sender.close();
	}

	@SuppressWarnings("RedundantThrows")
	public void onSocketClose(DatagramSocket sender) throws Exception {
		socketMap.remove(sender.getSessionId());
	}

	public final LongConcurrentHashMap<Service.ProtocolFactoryHandle<? extends Protocol<?>>> getFactorys() {
		return factorys;
	}

	public final void addFactoryHandle(long type, Service.ProtocolFactoryHandle<? extends Protocol<?>> factory) {
		if (factorys.putIfAbsent(type, factory) != null) {
			throw new IllegalStateException(String.format("duplicate factory type=%d moduleId=%d id=%d",
					type, type >>> 32, type & 0xffff_ffffL));
		}
	}

	public final Service.ProtocolFactoryHandle<? extends Protocol<?>> findProtocolFactoryHandle(long type) {
		return factorys.get(type);
	}

	@SuppressWarnings("MethodMayBeStatic")
	public Protocol<?> decodeProtocol(ByteBuffer bb, Service.ProtocolFactoryHandle<?> factoryHandle,
									  DatagramSession sender, long serialId) {
		var p = factoryHandle.Factory.create();
		p.decode(bb);
		p.setDatagramSession(sender);
		p.setUserState(serialId);
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(p.getTypeId()))
			AsyncSocket.log("RECV", sender != null ? sender.getSessionId() : 0, p);
		return p;
	}

	public void dispatchProtocol(ByteBuffer bb, Service.ProtocolFactoryHandle<?> factoryHandle,
								 DatagramSession sender, long serialId) throws Exception {
		var level = factoryHandle.Level;
		var zeze = this.zeze;
		// 为了避免redirect时死锁,这里一律不在whileCommit时执行
		if (zeze != null && level != TransactionLevel.None) {
			var bbCopy = ByteBuffer.Wrap(bb.Copy()); // 传给事务的buffer可能重做需要重新decode，不能直接引用网络层的buffer，需要copy一次。
			var outProtocol = new OutObject<Protocol<?>>();
			Task.executeUnsafe(zeze.newProcedure(() -> {
						bbCopy.ReadIndex = 0; // 考虑redo,要重置读指针
						var p = decodeProtocol(bbCopy, factoryHandle, sender, serialId);
						outProtocol.value = p;
						return p.handle(this, factoryHandle);
					}, factoryHandle.Class.getName(), level),
					outProtocol, Protocol::trySendResultCode, factoryHandle.Mode);
		} else {
			var p = decodeProtocol(bb, factoryHandle, sender, serialId);
			Task.executeUnsafe(() -> p.handle(this, factoryHandle), p,
					Protocol::trySendResultCode, null, factoryHandle.Mode);
		}
	}
}
