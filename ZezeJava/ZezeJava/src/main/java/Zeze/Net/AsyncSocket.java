package Zeze.Net;

import java.io.Closeable;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import javax.validation.constraints.Null;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.Handshake.Constant;
import Zeze.Util.Action0;
import Zeze.Util.GlobalTimer;
import Zeze.Util.JsonWriter;
import Zeze.Util.LongHashSet;
import Zeze.Util.PerfCounter;
import Zeze.Util.ShutdownHook;
import Zeze.Util.Task;
import Zeze.Util.TimeThrottle;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AsyncSocket implements SelectorHandle, Closeable {
	public static final Logger logger = LogManager.getLogger(AsyncSocket.class);
	public static final Level PROTOCOL_LOG_LEVEL = Level.toLevel(System.getProperty("protocolLog"), Level.OFF);
	public static final boolean ENABLE_PROTOCOL_LOG = PROTOCOL_LOG_LEVEL != Level.OFF && logger.isEnabled(PROTOCOL_LOG_LEVEL);
	public static final boolean ENABLE_DEBUG_LOG = logger.isDebugEnabled();
	public static final boolean ENABLE_PROTOCOL_LOG_OLD = "true".equalsIgnoreCase(System.getProperty("protocolLogOld"));
	private static final LongHashSet protocolLogExcept = new LongHashSet();
	private static final @NotNull VarHandle closedHandle, outputBufferSizeHandle;
	private static final byte SEND_CLOSE_DETAIL_MAX = 20; // 必须小于REAL_CLOSED
	private static final byte REAL_CLOSED = Byte.MAX_VALUE;
	private static final AtomicLong sessionIdGen = new AtomicLong(1);
	private static @NotNull LongSupplier sessionIdGenFunc = sessionIdGen::getAndIncrement;

	static {
		try {
			var lookup = MethodHandles.lookup();
			closedHandle = lookup.findVarHandle(AsyncSocket.class, "closed", byte.class);
			outputBufferSizeHandle = lookup.findVarHandle(AsyncSocket.class, "outputBufferSize", long.class);
		} catch (ReflectiveOperationException e) {
			Task.forceThrow(e);
			throw new AssertionError(); // never run here
		}

		var str = System.getProperty("protocolLogExcept");
		if (str != null) {
			for (var numStr : str.split("[^\\d\\-]")) {
				if (!numStr.isBlank())
					protocolLogExcept.add(Long.parseLong(numStr));
			}
		}

		ShutdownHook.init();
	}

	public static boolean canLogProtocol(long protocolTypeId) {
		return !protocolLogExcept.contains(protocolTypeId);
	}

	public static void setSessionIdGenFunc(@Nullable LongSupplier seed) {
		sessionIdGenFunc = seed != null ? seed : sessionIdGen::getAndIncrement;
	}

	private final long sessionId = sessionIdGenFunc.getAsLong(); // 只在setSessionId里修改
	private final @NotNull Service service;
	private final @Nullable Object acceptorOrConnector;
	private final @NotNull Selector selector;
	private final @NotNull SelectionKey selectionKey;
	private volatile @Nullable SocketAddress remoteAddress; // 连接成功时设置
	private volatile Object userState;

	@SuppressWarnings("unused")
	private volatile long outputBufferSize;
	private final ConcurrentLinkedQueue<Action0> operates;

	private final BufferCodec inputBuffer; // 记录这个变量用来操作buffer. 只在selector线程访问
	private final OutputBuffer outputBuffer;
	private @Nullable Codec inputCodecChain; // 只在selector线程访问
	private @Nullable Codec outputCodecChain; // 只在selector线程访问
	private volatile byte security; // 1:Input; 2:Output; 1|2:Input+Output

	private volatile boolean isHandshakeDone;
	@SuppressWarnings("unused")
	private volatile byte closed;
	private volatile boolean closePending;
	private long recvCount, recvSize; // 已处理接收的次数, 已从socket接收数据的统计总字节数
	private long sendCount, sendSize; // 已处理发送的次数, 已向socket发送数据的统计总字节数
	private long sendRawSize; // 准备发送数据的统计总字节数(只在SetOutputSecurityCodec后统计,压缩加密之前的大小)
	private final TimeThrottle timeThrottle;

	public enum Type {
		eServer,
		eClient,
		eServerSocket,
	}

	private final Type type;
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

	public TimeThrottle getTimeThrottle() {
		return timeThrottle;
	}

	public long getSessionId() {
		return sessionId;
	}

	public @NotNull Service getService() {
		return service;
	}

	public @Nullable Acceptor getAcceptor() {
		return acceptorOrConnector instanceof Acceptor ? (Acceptor)acceptorOrConnector : null;
	}

	public @Nullable Connector getConnector() {
		return acceptorOrConnector instanceof Connector ? (Connector)acceptorOrConnector : null;
	}

	public @NotNull SelectableChannel getChannel() { // SocketChannel or ServerSocketChannel, 一定不为null
		return selectionKey.channel();
	}

	public @Nullable Socket getSocket() {
		SelectableChannel sc = getChannel();
		return sc instanceof SocketChannel ? ((SocketChannel)sc).socket() : null;
	}

	public @Nullable SocketAddress getLocalAddress() { // 已经close的情况下返回null
		try {
			SelectableChannel sc = getChannel();
			if (sc instanceof SocketChannel)
				return ((SocketChannel)sc).getLocalAddress();
			if (sc instanceof ServerSocketChannel)
				return ((ServerSocketChannel)sc).getLocalAddress();
		} catch (IOException ignored) {
		}
		return null;
	}

	public @Nullable InetAddress getLocalInetAddress() { // 已经close的情况下返回null
		SocketAddress sa = getLocalAddress();
		return sa instanceof InetSocketAddress ? ((InetSocketAddress)sa).getAddress() : null;
	}

	public @Nullable SocketAddress getRemoteAddress() { // 连接成功前返回null, 成功后即使close也不会返回null
		return remoteAddress;
	}

	public @Nullable InetSocketAddress getRemoteInet() {
		SocketAddress sa = remoteAddress;
		return sa instanceof InetSocketAddress ? ((InetSocketAddress)sa) : null;
	}

	public @Nullable InetAddress getRemoteInetAddress() { // 连接成功前返回null, 成功后即使close也不会返回null
		SocketAddress sa = remoteAddress;
		return sa instanceof InetSocketAddress ? ((InetSocketAddress)sa).getAddress() : null;
	}

	/**
	 * 保存需要存储在Socket中的状态。
	 * 简单变量，没有考虑线程安全问题。
	 * 内部不使用。
	 */
	public Object getUserState() {
		return userState;
	}

	public void setUserState(Object value) {
		userState = value;
	}

	public boolean isHandshakeDone() {
		return isHandshakeDone;
	}

	public void setHandshakeDone(boolean value) {
		isHandshakeDone = value;
	}

	public boolean isClosed() {
		return closed != 0;
	}

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

	public int getOperateSize() {
		return operates.size();
	}

	public long getOutputBufferSize() {
		return outputBufferSize;
	}

	/**
	 * for server socket
	 */
	public AsyncSocket(@NotNull Service service, @Nullable InetSocketAddress localEP, @Nullable Acceptor acceptor) {
		this.service = service;
		this.acceptorOrConnector = acceptor;
		this.type = Type.eServerSocket;
		service.tryStartKeepAliveCheckTimer();

		ServerSocketChannel ssc = null;
		try {
			ssc = ServerSocketChannel.open();
			ssc.configureBlocking(false);
			ServerSocket ss = ssc.socket();
			ss.setReuseAddress(true);
			// xxx 只能设置到 ServerSocket 中，以后 Accept 的连接通过继承机制得到这个配置。
			Integer recvBufSize = service.getSocketOptions().getReceiveBuffer();
			if (recvBufSize != null)
				ss.setReceiveBufferSize(recvBufSize);
			ss.bind(localEP, service.getSocketOptions().getBacklog());
			service.onServerSocketBind(ss);
			logger.info("Listen: [{}] {} for {}:{}", sessionId, localEP, service.getClass().getName(), service.getName());

			timeThrottle = null;
			selector = service.getSelectors().choice();
			operates = null;
			inputBuffer = null;
			outputBuffer = null;
			selectionKey = selector.register(ssc, 0, this); // 先获取key,因为有小概率出现事件处理比赋值更先执行
			interestOps(0, SelectionKey.OP_ACCEPT);
			selector.wakeup();
		} catch (IOException e) {
			if (ssc != null) {
				try {
					ssc.close();
				} catch (Exception ex) {
					logger.error("ServerSocketChannel.close", ex);
				}
			}
			throw new IllegalStateException("bind " + localEP, e);
		}
	}

	@Override
	public void doHandle(@NotNull SelectionKey key) throws Exception {
		SelectableChannel channel = key.channel();
		int ops = key.readyOps();
		if ((ops & SelectionKey.OP_READ) != 0)
			processReceive((SocketChannel)channel);
		if ((ops & SelectionKey.OP_WRITE) != 0)
			doWrite((SocketChannel)channel);

		if ((ops & SelectionKey.OP_ACCEPT) != 0) {
			SocketChannel sc = null;
			try {
				sc = ((ServerSocketChannel)channel).accept();
				if (sc != null)
					new AsyncSocket(service, sc, (Acceptor)acceptorOrConnector);
			} catch (Exception e) {
				if (sc != null)
					sc.close();
				service.OnSocketAcceptError(this, e);
			}
		} else if ((ops & SelectionKey.OP_CONNECT) != 0) {
			Throwable e = null;
			try {
				SocketChannel sc = (SocketChannel)channel;
				if (sc.finishConnect()) {
					// 先修改事件，防止doConnectSuccess发送数据注册了新的事件导致OP_CONNECT重新触发。
					// 虽然实际上在回调中应该不会唤醒Selector重入。
					doConnectSuccess(sc);
					return;
				}
			} catch (Exception ex) {
				e = ex;
			}
			close(e); // if OnSocketConnectError throw Exception, this will close in doException
			service.OnSocketConnectError(this, e);
		}
	}

	@Override
	public void doException(@NotNull SelectionKey key, @NotNull Throwable e) {
		close(e);
	}

	/**
	 * use inner. create when accepted;
	 */
	private AsyncSocket(@NotNull Service service, @NotNull SocketChannel sc, @Nullable Acceptor acceptor)
			throws Exception {
		this.service = service;
		this.acceptorOrConnector = acceptor;
		this.type = Type.eServer;
		resetActiveSendRecvTime();

		// 据说连接接受以后设置无效，应该从 ServerSocket 继承
		sc.configureBlocking(false);
		Socket so = sc.socket();
		remoteAddress = so.getRemoteSocketAddress();
		Integer recvBufSize = this.service.getSocketOptions().getReceiveBuffer();
		if (recvBufSize != null)
			so.setReceiveBufferSize(recvBufSize);
		Integer sendBufSize = this.service.getSocketOptions().getSendBuffer();
		if (sendBufSize != null)
			so.setSendBufferSize(sendBufSize);
		Boolean noDelay = this.service.getSocketOptions().getNoDelay();
		if (noDelay != null)
			so.setTcpNoDelay(noDelay);

		timeThrottle = TimeThrottle.create(this.service.getSocketOptions());
		selector = service.getSelectors().choice();
		operates = new ConcurrentLinkedQueue<>();
		inputBuffer = new BufferCodec();
		outputBuffer = new OutputBuffer(selector);
		selectionKey = selector.register(sc, 0, this); // 先获取key,因为有小概率出现事件处理比赋值selectionKey和OnSocketAccept更先执行
		logger.info("Accepted: {} for {}:{} recvBuf={}, sendBuf={}", this, service.getClass().getName(),
				service.getName(), so.getReceiveBufferSize(), so.getSendBufferSize());
		service.OnSocketAccept(this);
		interestOps(0, SelectionKey.OP_READ);
		selector.wakeup();
	}

	/**
	 * for client socket. connect
	 */
	private void doConnectSuccess(@NotNull SocketChannel sc) throws Exception {
		var socket = sc.socket();
		remoteAddress = socket.getRemoteSocketAddress();
		logger.info("Connected: {} for {}:{} recvBuf={}, sendBuf={}", this, service.getClass().getName(),
				service.getName(), socket.getReceiveBufferSize(), socket.getSendBufferSize());
		if (acceptorOrConnector instanceof Connector)
			((Connector)acceptorOrConnector).OnSocketConnected(this);
		service.OnSocketConnected(this);
		interestOps(SelectionKey.OP_CONNECT, SelectionKey.OP_READ);
	}

	public AsyncSocket(@NotNull Service service, @Nullable String hostNameOrAddress, int port,
					   @Nullable Object userState, @Nullable Connector connector) {
		this.service = service;
		this.acceptorOrConnector = connector;
		this.userState = userState;
		this.type = Type.eClient;
		resetActiveSendRecvTime();
		service.tryStartKeepAliveCheckTimer();

		SocketChannel sc = null;
		try {
			sc = SocketChannel.open();
			sc.configureBlocking(false);
			Socket so = sc.socket();
			Integer recvBufSize = this.service.getSocketOptions().getReceiveBuffer();
			if (recvBufSize != null)
				so.setReceiveBufferSize(recvBufSize);
			Integer sendBufSize = this.service.getSocketOptions().getSendBuffer();
			if (sendBufSize != null)
				so.setSendBufferSize(sendBufSize);
			Boolean noDelay = this.service.getSocketOptions().getNoDelay();
			if (noDelay != null)
				so.setTcpNoDelay(noDelay);
			logger.info("Connect: [{}] {}:{} for {}:{}", sessionId,
					hostNameOrAddress, port, service.getClass().getName(), service.getName());

			timeThrottle = TimeThrottle.create(this.service.getSocketOptions());
			selector = service.getSelectors().choice();
			operates = new ConcurrentLinkedQueue<>();
			inputBuffer = new BufferCodec();
			outputBuffer = new OutputBuffer(selector);
			InetAddress address = InetAddress.getByName(hostNameOrAddress); // TODO async dns lookup
			selectionKey = selector.register(sc, 0, this); // 先获取key,因为有小概率出现事件处理比赋值更先执行
			// 必须在connect前设置，否则selectionKey没初始化，有可能事件丢失？（现象好像是doHandle触发了）。
			if (sc.connect(new InetSocketAddress(address, port))) // 马上成功时，还没有注册到Selector中。
				doConnectSuccess(sc);
			else
				interestOps(0, SelectionKey.OP_CONNECT);
			selector.wakeup();
		} catch (Exception e) {
			if (sc != null) {
				try {
					sc.close();
				} catch (Exception ex) {
					logger.error("SocketChannel.close", ex);
				}
			}
			Task.forceThrow(e);
			throw new AssertionError(); // neven run here
		}
	}

	public boolean isInputSecurity() {
		return (security & 1) != 0;
	}

	public boolean isOutputSecurity() {
		return (security & 2) != 0;
	}

	public boolean isSecurity() {
		return security == (1 | 2);
	}

	public void verifySecurity() {
		if (service.getConfig().getHandshakeOptions().getEncryptType() != 0 && !isSecurity())
			throw new IllegalStateException(service.getName() + " !isSecurity");
	}

	public void setInputSecurityCodec(int encryptType, byte @Nullable [] encryptParam, int compressType) {
		submitAction(() -> { // 进selector线程调用
			Codec chain = inputBuffer;
			switch (compressType) {
			case Constant.eCompressTypeDisable:
				break;
			case Constant.eCompressTypeMppc:
				chain = new Decompress(chain);
				break;
			case Constant.eCompressTypeZstd:
				chain = new DecompressZstd(chain, 128 * 1024, 128 * 1024);
				break;
			// TODO: 新增压缩算法支持这里加case
			default:
				throw new UnsupportedOperationException("SetInputSecurityCodec: unknown compressType=" + compressType);
			}
			switch (encryptType) {
			case Constant.eEncryptTypeDisable:
				break;
			case Constant.eEncryptTypeAes:
				var keyMd5 = Digest.md5(encryptParam);
				chain = new Decrypt2(chain, keyMd5, keyMd5);
				break;
			//TODO: 新增加密算法支持这里加case
			default:
				throw new UnsupportedOperationException("SetInputSecurityCodec: unknown encryptType=" + encryptType);
			}
			inputCodecChain = chain;
			//noinspection NonAtomicOperationOnVolatileField
			security |= 1;
			logger.info("setInputSecurityCodec: {} decrypt={} decompress={}", this, encryptType, compressType);
		});
	}

	public void setOutputSecurityCodec(int encryptType, byte @Nullable [] encryptParam, int compressType) {
		submitAction(() -> { // 进selector线程调用
			Codec chain = outputBuffer;
			switch (encryptType) {
			case Constant.eEncryptTypeDisable:
				break;
			case Constant.eEncryptTypeAes:
				var keyMd5 = Digest.md5(encryptParam);
				chain = new Encrypt2(chain, keyMd5, keyMd5);
				break;
			//TODO: 新增加密算法支持这里加case
			default:
				throw new UnsupportedOperationException("SetOutputSecurityCodec: unknown encryptType=" + encryptType);
			}
			switch (compressType) {
			case Constant.eCompressTypeDisable:
				break;
			case Constant.eCompressTypeMppc:
				chain = new Compress(chain);
				break;
			case Constant.eCompressTypeZstd:
				chain = new CompressZstd(chain);
				break;
			//TODO: 新增压缩算法支持这里加case
			default:
				throw new UnsupportedOperationException("SetOutputSecurityCodec: unknown compress=" + compressType);
			}
			outputCodecChain = chain;
			//noinspection NonAtomicOperationOnVolatileField
			security |= 2;
			logger.info("setOutputSecurityCodec: {} compress={} encrypt={}", this, compressType, encryptType);
		});
	}

	public boolean submitAction(@NotNull Action0 callback) {
		var c = closed;
		if (c != 0) {
			if (c < SEND_CLOSE_DETAIL_MAX) {
				closedHandle.compareAndSet(this, (byte)c, (byte)(c + 1));
				logger.error("submitAction to closed socket: {}", this, new Exception());
			} else
				logger.error("submitAction to closed socket: {}", this);
			return false;
		}
		operates.offer(callback);
		if (interestOps(0, SelectionKey.OP_WRITE))
			selector.wakeup();
		return true;
	}

	// 返回是否实际修改过,需要后续wakeup
	private boolean interestOps(int remove, int add) {
		int ops = selectionKey.interestOps();
		int opsNew = (ops & ~remove) | add;
		if (ops == opsNew)
			return false;
		selectionKey.interestOps(opsNew);
		return true;
	}

	/**
	 * 可能直接加到发送缓冲区，返回true则bytes不能再修改了。
	 */
	public boolean Send(byte @NotNull [] bytes, int offset, int length) {
		ByteBuffer.VerifyArrayIndex(bytes, offset, length);

		var newSize = (long)outputBufferSizeHandle.getAndAdd(this, (long)length) + length;
		try {
			if (!service.checkOverflow(this, newSize, bytes, offset, length)) {
				outputBufferSizeHandle.getAndAdd(this, (long)-length);
				return false;
			}
			if (submitAction(() -> { // 进selector线程调用
				var codec = outputCodecChain;
				if (codec != null) {
					sendRawSize += length;
					// 压缩加密等 codec 链操作。
					int oldSize = outputBuffer.size();
					codec.update(bytes, offset, length);
					int deltaLen = outputBuffer.size() - oldSize - length;
					if (deltaLen != 0)
						outputBufferSizeHandle.getAndAdd(this, (long)deltaLen);
				} else
					outputBuffer.put(bytes, offset, length);
				if (PerfCounter.ENABLE_PERF)
					PerfCounter.instance.addSendInfo(bytes, offset, length);
			})) {
				setActiveSendTime();
				return true;
			}
		} catch (Exception ex) {
			outputBufferSizeHandle.getAndAdd(this, (long)-length);
			close(ex);
		}
		return false;
	}

	public static @NotNull String toStr(@NotNull Object obj) {
		return ENABLE_PROTOCOL_LOG_OLD
				? String.valueOf(obj)
				: JsonWriter.local().clear().setFlagsAndDepthLimit(JsonWriter.FLAG_NO_QUOTE_KEY, 16)
				.write(obj).toString();
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
			header = bb.ReadInt();
			familyClass = header & FamilyClass.FamilyClassMask;
			if ((header & FamilyClass.BitResultCode) != 0)
				resultCode = bb.ReadLong();
			if (FamilyClass.isRpc(familyClass))
				rpcSessionId = bb.ReadLong();
		} catch (Exception e) {
			logger.error("decode protocol failed: moduleId={}, protocolId={}, size={}",
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
			header = bb.ReadInt();
			familyClass = header & FamilyClass.FamilyClassMask;
			if ((header & FamilyClass.BitResultCode) != 0)
				resultCode = bb.ReadLong();
			if (FamilyClass.isRpc(familyClass))
				rpcSessionId = bb.ReadLong();
		} catch (Exception e) {
			logger.error("decode protocol failed: moduleId={}, protocolId={}, size={}",
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

	public boolean SendShared(@NotNull Protocol<?> p) {
		if (ENABLE_PROTOCOL_LOG && canLogProtocol(p.getTypeId()))
			log("SEND", sessionId, p);

		var result = submitAction(() -> { // 进selector线程调用
			var bb = p.encodeShared();
			var bytes = bb.Bytes;
			var offset = bb.ReadIndex;
			var length = bb.size();

			var newSize = (long)outputBufferSizeHandle.getAndAdd(this, (long)length) + length;
			if (!service.checkOverflow(this, newSize, bytes, offset, length)) {
				outputBufferSizeHandle.getAndAdd(this, (long)-length);
				return;
			}
			var codec = outputCodecChain;
			if (codec != null) {
				sendRawSize += length;
				// 压缩加密等 codec 链操作。
				int oldSize = outputBuffer.size();
				codec.update(bytes, offset, length);
				int deltaLen = outputBuffer.size() - oldSize - length;
				if (deltaLen != 0)
					outputBufferSizeHandle.getAndAdd(this, (long)deltaLen);
			} else
				outputBuffer.put(bytes, offset, length);
			if (PerfCounter.ENABLE_PERF)
				PerfCounter.instance.addSendInfo(bytes, offset, length);
		});
		if (result)
			setActiveSendTime();
		return result;
	}

	public boolean Send(@NotNull Protocol<?> p) {
		if (ENABLE_PROTOCOL_LOG && canLogProtocol(p.getTypeId()))
			log("SEND", sessionId, p);
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

	private void processReceive(@NotNull SocketChannel sc) throws Exception { // 只在selector线程调用
		recvCount++;
		java.nio.ByteBuffer buffer = selector.getReadBuffer(); // 线程共享的buffer,只能本方法内临时使用
		boolean readAgain = false;
		do {
			buffer.clear();
			int bytesTransferred = sc.read(buffer); // 对方正常关闭或shutdownOutput会返回-1; 而连接被对方RESET会抛异常
			if (bytesTransferred > 0) {
				setActiveRecvTime();
				recvSize += bytesTransferred;
				readAgain = bytesTransferred == buffer.limit();
				ByteBuffer codecBuf = inputBuffer.getBuffer(); // codec对buffer的引用一定是不可变的
				Codec codec = inputCodecChain;
				if (codec != null) {
					// 解密解压处理，处理结果直接加入 inputCodecBuffer。
					codecBuf.EnsureWrite(bytesTransferred);
					codec.update(buffer.array(), 0, bytesTransferred);
					codec.flush();
					readAgain &= service.OnSocketProcessInputBuffer(this, codecBuf);
				} else if (!codecBuf.isEmpty()) {
					// 上次解析有剩余数据（不完整的协议），把新数据加入。
					codecBuf.Append(buffer.array(), 0, bytesTransferred);
					readAgain &= service.OnSocketProcessInputBuffer(this, codecBuf);
				} else {
					ByteBuffer avoidCopy = ByteBuffer.Wrap(buffer.array(), bytesTransferred);
					readAgain &= service.OnSocketProcessInputBuffer(this, avoidCopy);
					if (!avoidCopy.isEmpty()) // 有剩余数据（不完整的协议），加入 inputCodecBuffer 等待新的数据。
						codecBuf.Append(avoidCopy.Bytes, avoidCopy.ReadIndex, avoidCopy.size());
				}

				// 1 检测 buffer 是否满，2 剩余数据 Compact，3 需要的话，释放buffer内存。
				int remain = codecBuf.size();
				if (remain <= 0) {
					if (codecBuf.capacity() <= 32 * 1024)
						codecBuf.Reset();
					else
						codecBuf.FreeInternalBuffer(); // 只在过大的缓冲区时释放内部bytes[], 避免频繁分配
				} else {
					int max = service.getSocketOptions().getInputBufferMaxProtocolSize();
					if (remain >= max)
						throw new IllegalStateException("InputBufferMaxProtocolSize " + remain + " >= " + max);
					codecBuf.Compact();
				}
			} else if (!readAgain)
				service.OnSocketInputClosed(this);
			else
				readAgain = false;
		} while (readAgain);
	}

	/*
	 * OutputBuffer 流式写入。
	 * 核心原则
	 * 保持它满载。由于OutputBuffer每次写2个Buffer，所以每次从operates导入时，使得operates为空或者
	 * OutputBuffer.getBufferSize() > 2；这样既能保持OutputBuffer满载writeTo，又能使得
	 * interestOps remove write保持一样的逻辑。
	 * 根据上面原则进行如下修改：
	 * 1. operates 重新改成 ConcurrentLinkedQueue
	 * 2. 锁外执行
	 *		int blockSize = selector.getSelectors().getBufferSize();
	 *		for (Action0 op; bufSize < blockSize * 2 && (op = operates.poll()) != null; ) {
	 *			op.run();
	 *			bufSize = outputBuffer.size();
	 *		}
	 * 3. interestOps 保持不变。
	 * 4. 并发性
	 *    高并发完全由ConcurrentLinkedQueue决定，在忙碌的情况下，完全不需要lock(Submit的锁)。
	 * 问题：
	 * 1. 现在Socket.SendBufferSize是按Service配置的，但是OutputBuffer的Buffer.BlockSize是固定的，
	 *    对于大的SendBufferSize，无法发挥最佳性能。
	 *    解决方法？
	 *    Service.start的时候把自己的SendBufferSize配置设置Max到相应的Selectors中，其中所有的Selector都采用这个Max。
	 *    这样的话基本上整个系统还是一个OutputBuffer.BlockSize。如果需要对特别的Service设置特别的BlockSize，让这个
	 *    Service使用独立的Selectors。
	 * 2. doWrite while (true)
	 *    outputBuffer全部刷出后，马上重复检查一次operates是否必要，或者等到下一次doWrite更好。
	 *    因为刚写完，如果此时operates也是繁忙的，有数据，导致一次导入，但是马上write(socket)可能是失败的，
	 *    存在浪费一次write(socket)的调用，当然这个比较罕见，因为对于原来的逻辑，outputBuffer全部刷完
	 *    对于繁忙连接是比较罕见的，但是存在抖动的可能。
	 *    考虑清楚以后去掉while(true)？
	 */
	private void doWrite(@NotNull SocketChannel sc) throws Exception { // 只在selector线程调用
		sendCount++;
		int blockSize = selector.getSelectors().getBbPoolBlockSize();
		int bufSize = outputBuffer.size();
		while (true) {
			for (Action0 op; /*bufSize < blockSize * 2 &&*/ (op = operates.poll()) != null; ) {
				op.run();
				bufSize = outputBuffer.size();
			}
			var flushed = true;
			var codec = outputCodecChain;
			if (codec != null) {
				// 减慢flush频率，
				// 在保持底层outputBuffer.writeTo能满载的情况下，尽量缓冲住Chain里面的数据。
				// 这使得某些Chain算法比如Zstd能大块的工作，具有更高的效率。
				if (bufSize < blockSize) {
					codec.flush();
					int newBufSize = outputBuffer.size();
					int deltaLen = newBufSize - bufSize;
					if (deltaLen != 0) {
						bufSize = newBufSize;
						outputBufferSizeHandle.getAndAdd(this, (long)deltaLen);
					}
				} else
					flushed = false;
			}

			if (bufSize > 0) {
				var rc = outputBuffer.writeTo(sc);
				if (rc < 0) {
					close(); // 很罕见的正常关闭, 不设置异常, 其实write抛异常的可能性更大
					return;
				}
				sendSize += rc;
				outputBufferSizeHandle.getAndAdd(this, -rc);
				bufSize = outputBuffer.size();
				if (bufSize > 0) {
					// 有数据正在发送，此时可以安全退出执行，写完以后Selector会再次触发doWrite。
					// add write event，里面判断了事件没有变化时不做操作，严格来说，再次注册事件是不需要的。
					return;
				}
				// 全部都写出去了，继续尝试看看有没有新的操作。
			}
			// 时间窗口
			// 必须和把Operate加入队列同步！否则可能会出现，刚加入操作没有被处理，但是OP_WRITE又被Remove的问题。
			if (operates.isEmpty() && flushed) { // 此时bufSize=0,下次循环会触发flush
				// 真的没有等待处理的操作了，去掉事件，返回。以后新的操作在下一次doWrite时处理。
				interestOps(SelectionKey.OP_WRITE, 0);
				if (operates.isEmpty()) { // 再判断一次,避免跟submitAction的并发竞争问题
					if (closePending)
						realClose();
					return;
				}
				interestOps(0, SelectionKey.OP_WRITE);
			}
			// 发现数据，继续尝试处理。
		}
	}

	private void realClose() {
		if ((byte)closedHandle.getAndSet(this, (byte)REAL_CLOSED) == REAL_CLOSED) // 阻止递归关闭
			return;
		try {
			selectionKey.channel().close();
		} catch (Exception e) {
			logger.error("SocketChannel.close", e);
		}
		selector.addTask(() -> { // 进selector线程调用
			if (outputBuffer != null)
				outputBuffer.close();
			try {
				service.OnSocketDisposed(this);
			} catch (Exception e) {
				logger.error("Service.OnSocketDisposed", e);
			}
		});
		selector.wakeup();
		if (timeThrottle != null)
			timeThrottle.close();
	}

	private boolean close(@Nullable Throwable ex, boolean gracefully) {
		if (!closedHandle.compareAndSet(this, (byte)0, (byte)1)) // 阻止递归关闭
			return false;

		if (ex != null) {
			if (ex instanceof IOException)
				logger.info("close: {} {}", this, ex);
			else
				logger.warn("close: {}", this, ex);
		} else
			logger.info("close: {} {}", this, gracefully ? " gracefully" : "");

		if (acceptorOrConnector instanceof Connector) {
			try {
				((Connector)acceptorOrConnector).OnSocketClose(this, ex);
			} catch (Exception e) {
				logger.error("Connector.OnSocketClose", e);
			}
		}
		try {
			service.OnSocketClose(this, ex);
		} catch (Exception e) {
			logger.error("Service.OnSocketClose", e);
		}

		if (gracefully) {
			closePending = true;
			if (interestOps(0, SelectionKey.OP_WRITE))
				selector.wakeup();
			Task.schedule(120 * 1000, this::realClose); // 最多给2分钟清空输出队列。
		} else
			realClose();
		return true;
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

	@Override
	public @NotNull String toString() {
		SocketAddress localAddress = getLocalAddress();
		SocketAddress remoteAddress = this.remoteAddress;
		return "[" + sessionId + ']' +
				(localAddress != null ? localAddress : (acceptorOrConnector instanceof Acceptor ?
						((Acceptor)acceptorOrConnector).getName() : "")) + "-" + // 如果有localAddress则表示还没close
				(remoteAddress != null ? remoteAddress : (acceptorOrConnector instanceof Connector ?
						((Connector)acceptorOrConnector).getName() : "")); // 如果有RemoteAddress则表示曾经连接成功过
	}
}
