package Zeze.Net;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.Handshake.Constant;
import Zeze.Util.Action0;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import Zeze.Util.TimeThrottle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * TCP 连接与监听实现：基于 Selector 的事件驱动收发、codec 链（加密/压缩）装配与优雅关闭。
 */
public final class TcpSocket extends AsyncSocket implements SelectorHandle {
	@Override
	public Service getService() {
		var service = super.getService();
		assert service != null;
		return service;
	}

	private static final Logger logger = LogManager.getLogger(TcpSocket.class);
	private static final VarHandle closeDetailHandle, outputBufferSizeHandle;
	private static final byte SEND_CLOSE_DETAIL_MAX = 20; // 必须小于REAL_CLOSED
	private static final byte REAL_CLOSED = Byte.MAX_VALUE;
	private static final int MAX_OPERATES_PER_WRITE = 1024;
	private static final IOException connectFailedException = new IOException("connect failed");
	// DNS 解析专用 daemon 池：同步解析不可中断，不得占用 selector 或 Task 线程池。
	private static final ExecutorService dnsResolver = Executors.newCachedThreadPool(r -> {
		var t = new Thread(r, "Zeze-Dns-Resolver");
		t.setDaemon(true);
		return t;
	});

	static {
		try {
			var lookup = MethodHandles.lookup();
			closeDetailHandle = lookup.findVarHandle(TcpSocket.class, "closeDetail", byte.class);
			outputBufferSizeHandle = lookup.findVarHandle(TcpSocket.class, "outputBufferSize", long.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private final @Nullable Object acceptorOrConnector;
	private final Selector selector;
	private final SelectionKey selectionKey;

	@SuppressWarnings("unused")
	private volatile long outputBufferSize;
	private final @Nullable ConcurrentLinkedQueue<Action0> operates;

	private final @Nullable BufferCodec inputBuffer; // 记录这个变量用来操作buffer. 只在selector线程访问
	private final @Nullable OutputBuffer outputBuffer;
	private @Nullable Codec inputCodecChain; // 只在selector线程访问
	private @Nullable Codec outputCodecChain; // 只在selector线程访问
	private volatile byte security; // 1:Input; 2:Output; 1|2:Input+Output

	// 关闭后 submitAction 误用日志计数；realClose 置 REAL_CLOSED 防递归。
	@SuppressWarnings("unused")
	private volatile byte closeDetail;
	private volatile boolean closePending;
	private @Nullable HaProxyHeader haProxyHeader;
	private volatile @Nullable SocketAddress remoteAddress; // 连接成功时设置
	private final @Nullable TimeThrottle timeThrottle;
	private final Type type;

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public @Nullable SocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	public @Nullable HaProxyHeader getHaProxyHeader() {
		return haProxyHeader;
	}

	public void setHaProxyHeader(HaProxyHeader haProxyHeader) {
		this.haProxyHeader = haProxyHeader;
	}

	public @Nullable Acceptor getAcceptor() {
		return acceptorOrConnector instanceof Acceptor ? (Acceptor)acceptorOrConnector : null;
	}

	@Override
	public @Nullable Connector getConnector() {
		return acceptorOrConnector instanceof Connector ? (Connector)acceptorOrConnector : null;
	}

	public SelectableChannel getChannel() { // SocketChannel or ServerSocketChannel, 一定不为null
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

	public @Nullable InetSocketAddress getLocalInet() { // 已经close的情况下返回null
		SocketAddress sa = getLocalAddress();
		return sa instanceof InetSocketAddress ? (InetSocketAddress)sa : null;
	}

	public int getOperateSize() {
		//noinspection DataFlowIssue
		return operates.size();
	}

	public long getOutputBufferSize() {
		return outputBufferSize;
	}

	@Override
	public @Nullable TimeThrottle getTimeThrottle() {
		return this.timeThrottle;
	}

	/** 服务端监听 socket 构造。 */
	public TcpSocket(Service service, @Nullable InetSocketAddress localEP, @Nullable Acceptor acceptor) {
		super(service);

		this.acceptorOrConnector = acceptor;
		this.type = Type.eServerSocket;
		service.tryStartKeepAliveCheckTimer();

		ServerSocketChannel ssc = null;
		try {
			ssc = ServerSocketChannel.open();
			ssc.configureBlocking(false);
			ServerSocket ss = ssc.socket();
			ss.setReuseAddress(true);
			// 只能设置到 ServerSocket 中，以后 Accept 的连接通过继承机制得到这个配置。
			Integer recvBufSize = service.getSocketOptions().getReceiveBuffer();
			if (recvBufSize != null)
				ss.setReceiveBufferSize(recvBufSize);
			ss.bind(localEP, service.getSocketOptions().getBacklog());
			service.onServerSocketBind(ss);
			logger.info("Listen: [{}] {} for {}:{}", getSessionId(), localEP, service.getClass().getName(), service.getName());

			timeThrottle = null;
			selector = service.getSelectors().choice();
			operates = null;
			inputBuffer = null;
			outputBuffer = null;
			selectionKey = selector.register(ssc, 0, this); // 先获取key,因为有小概率出现事件处理比赋值更先执行
			addInterestOps(SelectionKey.OP_ACCEPT);
			selector.wakeup();
		} catch (Exception e) {
			if (ssc != null) {
				try {
					ssc.close();
				} catch (Exception ex) {
					logger.error("ServerSocketChannel.close exception:", ex);
				}
			}
			throw new IllegalStateException("bind " + localEP, e);
		}
	}

	@Override
	public void doHandle(SelectionKey key) throws Exception {
		SelectableChannel channel = key.channel();
		int ops = key.readyOps();
		if ((ops & SelectionKey.OP_ACCEPT) != 0) {
			SocketChannel sc = null;
			try {
				sc = ((ServerSocketChannel)channel).accept();
				if (sc != null)
					new TcpSocket(getService(), sc, (Acceptor)acceptorOrConnector);
			} catch (Exception e) {
				if (sc != null)
					sc.close();
				getService().OnSocketAcceptError(this, e);
				return;
			}
		} else if ((ops & SelectionKey.OP_CONNECT) != 0) {
			try {
				SocketChannel sc = (SocketChannel)channel;
				if (!sc.finishConnect())
					throw connectFailedException;
				// 先修改事件，防止doConnectSuccess发送数据注册了新的事件导致OP_CONNECT重新触发。
				// 虽然实际上在回调中应该不会唤醒Selector重入。
				removeInterestOps(SelectionKey.OP_CONNECT);
				doConnectSuccess(sc);
			} catch (Exception e) {
				close(e); // if OnSocketConnectError throw Exception, this will close in doException
				getService().OnSocketConnectError(this, e);
				return;
			}
		}

		if ((ops & SelectionKey.OP_READ) != 0)
			processReceive((SocketChannel)channel);
		if ((ops & SelectionKey.OP_WRITE) != 0)
			doWrite((SocketChannel)channel);
	}

	@Override
	public void doException(SelectionKey key, Throwable e) {
		close(e);
	}

	/** 服务端接受新连接时内部构造。 */
	private TcpSocket(Service service, SocketChannel sc, @Nullable Acceptor acceptor)
			throws Exception {
		super(service);

		setDecodeAdmission(service.getConnectionDecodeAdmission());
		this.acceptorOrConnector = acceptor;
		this.type = Type.eServer;
		resetActiveSendRecvTime();

		// 连接接受以后设置无效，应该从 ServerSocket 继承
		sc.configureBlocking(false);
		Socket so = sc.socket();
		remoteAddress = so.getRemoteSocketAddress();
		Integer recvBufSize = getService().getSocketOptions().getReceiveBuffer();
		if (recvBufSize != null)
			so.setReceiveBufferSize(recvBufSize);
		Integer sendBufSize = getService().getSocketOptions().getSendBuffer();
		if (sendBufSize != null)
			so.setSendBufferSize(sendBufSize);
		Boolean noDelay = getService().getSocketOptions().getNoDelay();
		if (noDelay != null)
			so.setTcpNoDelay(noDelay);

		timeThrottle = TimeThrottle.create(getService().getSocketOptions());
		selector = service.getSelectors().choice();
		operates = new ConcurrentLinkedQueue<>();
		inputBuffer = new BufferCodec();
		outputBuffer = new OutputBuffer(selector);
		selectionKey = selector.register(sc, 0, this); // 先获取key,因为有小概率出现事件处理比赋值selectionKey和OnSocketAccept更先执行
		logger.info("Accepted: {} for {}:{} recvBuf={}, sendBuf={}", this, service.getClass().getName(),
				service.getName(), so.getReceiveBufferSize(), so.getSendBufferSize());
		try {
			service.OnSocketAccept(this);
		} catch (Exception e) {
			// OnSocketAccept 抛异常时显式 close 触发 OnSocketClose 清理已登记状态，再重抛记录 accept 错误。
			close(e);
			throw e;
		}
		if (!isClosed()) {
			try {
				addInterestOps(SelectionKey.OP_READ);
			} catch (CancelledKeyException e) {
				if (!isClosed())
					throw e;
				return;
			}
			selector.wakeup();
		}
	}

	/** 客户端连接建立成功后的回调处理。 */
	private void doConnectSuccess(SocketChannel sc) throws Exception {
		var socket = sc.socket();
		remoteAddress = socket.getRemoteSocketAddress();
		logger.info("Connected: {} for {}:{} recvBuf={}, sendBuf={}", this, getService().getClass().getName(),
				getService().getName(), socket.getReceiveBufferSize(), socket.getSendBufferSize());
		if (acceptorOrConnector instanceof Connector)
			((Connector)acceptorOrConnector).OnSocketConnected(this);
		getService().OnSocketConnected(this);
		if (!isClosed()) {
			try {
				addInterestOps(SelectionKey.OP_READ);
			} catch (CancelledKeyException e) {
				if (!isClosed())
					throw e;
			}
		}
	}

	public TcpSocket(Service service, @Nullable String hostNameOrAddress, int port,
					 @Nullable Object userState, @Nullable Connector connector) {
		super(service);

		setDecodeAdmission(service.getConnectionDecodeAdmission());
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
			Integer recvBufSize = getService().getSocketOptions().getReceiveBuffer();
			if (recvBufSize != null)
				so.setReceiveBufferSize(recvBufSize);
			Integer sendBufSize = getService().getSocketOptions().getSendBuffer();
			if (sendBufSize != null)
				so.setSendBufferSize(sendBufSize);
			Boolean noDelay = getService().getSocketOptions().getNoDelay();
			if (noDelay != null)
				so.setTcpNoDelay(noDelay);
			logger.info("Connect: [{}] {}:{} for {}:{}", getSessionId(),
					hostNameOrAddress, port, service.getClass().getName(), service.getName());

			timeThrottle = TimeThrottle.create(getService().getSocketOptions());
			selector = service.getSelectors().choice();
			operates = new ConcurrentLinkedQueue<>();
			inputBuffer = new BufferCodec();
			outputBuffer = new OutputBuffer(selector);
			// 先获取key,因为有小概率出现事件处理比赋值更先执行；connect前必须完成注册，否则事件丢失。
			selectionKey = selector.register(sc, 0, this);
		} catch (Exception e) {
			if (sc != null) {
				try {
					sc.close();
				} catch (Exception ex) {
					logger.error("SocketChannel.close exception:", ex);
				}
			}
			throw Task.forceThrow(e);
		}
		connectAsync(hostNameOrAddress, port);
	}

	// 专用线程异步解析 DNS 并发起非阻塞 connect，失败走 close + OnSocketConnectError。
	private void connectAsync(@Nullable String hostNameOrAddress, int port) {
		if (isClosed())
			return;
		dnsResolver.execute(() -> {
			try {
				InetAddress address = getService().resolveAddress(hostNameOrAddress);
				if (isClosed())
					return;
				SocketChannel sc = (SocketChannel)getChannel();
				if (sc.connect(new InetSocketAddress(address, port))) // 马上成功时（回环），直接完成
					doConnectSuccess(sc);
				else if (!isClosed()) {
					try {
						addInterestOps(SelectionKey.OP_CONNECT);
					} catch (CancelledKeyException e) {
						if (!isClosed())
							throw e;
						return;
					}
				}
				selector.wakeup();
			} catch (Exception e) {
				if (close(e)) {
					try {
						getService().OnSocketConnectError(this, e);
					} catch (Exception ex) {
						logger.error("Service.OnSocketConnectError exception:", ex);
					}
				}
			}
		});
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

	// 同方向 codec 链只允许装配一次（仅 selector 线程调用），避免覆盖旧链导致 native 资源泄漏。
	private boolean securityCodecReinstalled(byte bit) {
		if ((security & bit) == 0)
			return false;
		close(new IllegalStateException("security codec chain re-install, bit=" + bit));
		return true;
	}

	// 仅在 selector 线程的 submitAction 中串行调用；双向 codec 装齐时撤销解码准入限制。
	private void securityCodecInstalled(byte bit) {
		//noinspection NonAtomicOperationOnVolatileField
		security |= bit;
		if (security == (1 | 2))
			setDecodeAdmission(null);
	}

	public void verifySecurity() {
		if (getService().getConfig().getHandshakeOptions().getEncryptType() != Constant.eEncryptTypeDisable && !isSecurity())
			throw new IllegalStateException(getService().getName() + " !isSecurity");
	}

	public void setInputSecurityCodec(int encryptType, byte @Nullable [] encryptParam, int compressType) {
		submitAction(() -> { // 进selector线程调用
			if (securityCodecReinstalled((byte)1))
				return;
			// 开启压缩时挂载 InputLimitCodec 流式限制解压输出，防压缩放大耗尽内存。
			Codec chain = compressType == Constant.eCompressTypeDisable
					? Objects.requireNonNull(inputBuffer) : new InputLimitCodec(this, Objects.requireNonNull(inputBuffer));
			try {
				switch (compressType) {
				case Constant.eCompressTypeDisable:
					break;
				case Constant.eCompressTypeMppc:
					chain = new Decompress(chain);
					break;
				case Constant.eCompressTypeZstd:
					chain = new DecompressZstd(chain, 128 * 1024, 128 * 1024);
					break;
				// 新增压缩算法支持这里加case
				default:
					throw new UnsupportedOperationException("SetInputSecurityCodec: unknown compressType=" + compressType);
				}
				switch (encryptType) {
				case Constant.eEncryptTypeDisable:
					break;
				case Constant.eEncryptTypeAesNoSecureIp:
				case Constant.eEncryptTypeAes:
					//noinspection DataFlowIssue
					var keyMd5 = Digest.md5(encryptParam);
					chain = new Decrypt2(chain, keyMd5, keyMd5);
					break;
				case Constant.eEncryptTypeRsaAes:
					//noinspection DataFlowIssue
					chain = new Decrypt2(chain, Arrays.copyOfRange(encryptParam, 16, 32), encryptParam);
					break;
				// 新增加密算法支持这里加case
				default:
					throw new UnsupportedOperationException("SetInputSecurityCodec: unknown encryptType=" + encryptType);
				}
			} catch (Throwable e) {
				if (chain != inputBuffer) {
					try {
						chain.close();
					} catch (Throwable closeEx) {
						e.addSuppressed(closeEx);
					}
				}
				throw e;
			}
			inputCodecChain = chain;
			securityCodecInstalled((byte)1);
			logger.info("setInputSecurityCodec: {} decrypt={} decompress={}", this, encryptType, compressType);
		});
	}

	/**
	 * 自定义输入 codec 链扩展点；若链中包含解压器，调用方需自行约束解压输出上限以防压缩放大。
	 */
	public void setInputSecurityCodec(BiFunction<AsyncSocket, BufferCodec, Codec> creator) {
		submitAction(() -> { // 进selector线程调用
			if (securityCodecReinstalled((byte)1))
				return;
			inputCodecChain = creator.apply(this, Objects.requireNonNull(inputBuffer));
			securityCodecInstalled((byte)1);
			logger.info("setInputSecurityCodec: {} class={}", this, inputCodecChain.getClass().getName());
		});
	}

	public void setOutputSecurityCodec(int encryptType, byte @Nullable [] encryptParam, int compressType) {
		submitAction(() -> { // 进selector线程调用
			if (securityCodecReinstalled((byte)2))
				return;
			Codec chain = outputBuffer;
			switch (encryptType) {
			case Constant.eEncryptTypeDisable:
				break;
			case Constant.eEncryptTypeAesNoSecureIp:
			case Constant.eEncryptTypeAes:
				//noinspection DataFlowIssue
				var keyMd5 = Digest.md5(encryptParam);
				chain = new Encrypt2(chain, keyMd5, keyMd5);
				break;
			case Constant.eEncryptTypeRsaAes:
				//noinspection DataFlowIssue
				chain = new Encrypt2(chain, Arrays.copyOfRange(encryptParam, 16, 32), encryptParam);
				break;
			// 新增加密算法支持这里加case
			default:
				throw new UnsupportedOperationException("SetOutputSecurityCodec: unknown encryptType=" + encryptType);
			}
			switch (compressType) {
			case Constant.eCompressTypeDisable:
				break;
			case Constant.eCompressTypeMppc:
				chain = new Compress(Objects.requireNonNull(chain));
				break;
			case Constant.eCompressTypeZstd:
				chain = new CompressZstd(Objects.requireNonNull(chain));
				break;
			// 新增压缩算法支持这里加case
			default:
				throw new UnsupportedOperationException("SetOutputSecurityCodec: unknown compress=" + compressType);
			}
			outputCodecChain = chain;
			securityCodecInstalled((byte)2);
			logger.info("setOutputSecurityCodec: {} compress={} encrypt={}", this, compressType, encryptType);
		});
	}

	public void setOutputSecurityCodec(BiFunction<AsyncSocket, OutputBuffer, Codec> creator) {
		submitAction(() -> { // 进selector线程调用
			if (securityCodecReinstalled((byte)2))
				return;
			outputCodecChain = creator.apply(this, Objects.requireNonNull(outputBuffer));
			securityCodecInstalled((byte)2);
			logger.info("setOutputSecurityCodec: {} class={}", this, outputCodecChain.getClass().getName());
		});
	}

	public boolean submitAction(Action0 callback) {
		if (isClosed()) {
			var c = closeDetail;
			if (c < SEND_CLOSE_DETAIL_MAX) {
				closeDetailHandle.compareAndSet(this, (byte)c, (byte)(c + 1));
				logger.error("submitAction to closed socket: {}", this, new Exception("only for stack trace"));
			} else
				logger.error("submitAction to closed socket: {}", this);
			return false;
		}
		//noinspection DataFlowIssue
		operates.offer(callback);
		if (addInterestOps(SelectionKey.OP_WRITE))
			selector.wakeup();
		return true;
	}

	// 返回是否实际修改过,需要后续wakeup
	private boolean addInterestOps(int ops) {
		return (selectionKey.interestOpsOr(ops) & ops) != ops;
	}

	// 返回是否实际修改过,需要后续wakeup
	private boolean removeInterestOps(int ops) {
		return (selectionKey.interestOpsAnd(~ops) & ops) != 0;
	}

	public void pauseReceive() {
		if (removeInterestOps(SelectionKey.OP_READ))
			selector.wakeup();
	}

	public void resumeReceive() {
		if (addInterestOps(SelectionKey.OP_READ))
			selector.wakeup();
	}

	/**
	 * 可能直接加到发送缓冲区，返回true则bytes不能再修改了。
	 */
	@Override
	public boolean Send(byte [] bytes, int offset, int length) {
		ByteBuffer.VerifyArrayIndex(bytes, offset, length);

		var newSize = (long)outputBufferSizeHandle.getAndAdd(this, (long)length) + length;
		try {
			if (!getService().checkOverflow(this, newSize, bytes, offset, length)) {
				outputBufferSizeHandle.getAndAdd(this, (long)-length);
				return false;
			}
			if (submitAction(() -> { // 进selector线程调用
				var codec = outputCodecChain;
				if (codec != null) {
					sendRawSize += length;
					// 压缩加密等 codec 链操作。
					//noinspection DataFlowIssue
					int oldSize = outputBuffer.size();
					codec.update(bytes, offset, length);
					int deltaLen = outputBuffer.size() - oldSize - length;
					if (deltaLen != 0)
						outputBufferSizeHandle.getAndAdd(this, (long)deltaLen);
				} else
					//noinspection DataFlowIssue
					outputBuffer.put(bytes, offset, length);
			})) {
				setActiveSendTime();
				return true;
			}
			outputBufferSizeHandle.getAndAdd(this, (long)-length);
		} catch (Exception ex) {
			outputBufferSizeHandle.getAndAdd(this, (long)-length);
			close(ex);
		}
		return false;
	}

	private void processReceive(SocketChannel sc) throws Exception { // 只在selector线程调用
		recvCount++;
		java.nio.ByteBuffer buffer = selector.getReadBuffer(); // 线程共享的buffer,只能本方法内临时使用
		boolean readAgain;
		do {
			buffer.clear();
			int bytesTransferred = sc.read(buffer); // 对方正常关闭或shutdownOutput会返回-1; 而连接被对方RESET会抛异常
			if (bytesTransferred > 0) {
				setActiveRecvTime();
				recvSize += bytesTransferred;
				readAgain = bytesTransferred == buffer.limit();
				//noinspection DataFlowIssue
				ByteBuffer codecBuf = inputBuffer.getBuffer(); // codec对buffer的引用一定是不可变的
				Codec codec = inputCodecChain;
				if (codec != null) {
					// 解密解压处理，处理结果直接加入 inputCodecBuffer。
					codecBuf.EnsureWrite(bytesTransferred);
					codec.update(buffer.array(), 0, bytesTransferred);
					codec.flush();
					readAgain &= getService().OnSocketProcessInputBuffer(this, codecBuf);
				} else if (!codecBuf.isEmpty()) {
					// 上次解析有剩余数据（不完整的协议），把新数据加入。
					codecBuf.Append(buffer.array(), 0, bytesTransferred);
					readAgain &= getService().OnSocketProcessInputBuffer(this, codecBuf);
				} else {
					ByteBuffer avoidCopy = ByteBuffer.Wrap(buffer.array(), bytesTransferred);
					readAgain &= getService().OnSocketProcessInputBuffer(this, avoidCopy);
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
					int max = getService().getSocketOptions().getInputBufferMaxProtocolSize();
					// max 为最大协议体大小（不含帧头），此处残留含帧头，故允许至 HEADER_SIZE + max。
					if (remain > Protocol.HEADER_SIZE + max)
						throw new IllegalStateException(
								"InputBufferMaxProtocolSize " + remain + " > " + (Protocol.HEADER_SIZE + max));
					codecBuf.Compact();
				}
			} else if (bytesTransferred < 0) {
				readAgain = false; // 避免保留上轮满读标记导致非关闭式 OnSocketInputClosed 忙循环
				getService().OnSocketInputClosed(this);
			} else
				readAgain = false;
		} while (readAgain);
	}

	/** 解压期间先消费完整协议，限值只约束尚未消费的单帧，避免把合法批量协议当作压缩放大。 */
	private record InputLimitCodec(TcpSocket socket, BufferCodec sink) implements Codec {
		private int room() {
			long limit = (long)Protocol.HEADER_SIZE + socket.getService().getSocketOptions().getInputBufferMaxProtocolSize();
			if (sink.size() >= limit) {
				var buffer = sink.getBuffer();
				if (buffer.size() >= Protocol.HEADER_SIZE
						&& (ByteBuffer.ToInt(buffer.Bytes, buffer.ReadIndex + 8) & 0xffff_ffffL)
						> socket.getService().getSocketOptions().getInputBufferMaxProtocolSize())
					throw new IllegalStateException("InputBufferMaxProtocolSize: declared frame exceeds limit");
				try {
					socket.getService().OnSocketProcessInputBuffer(socket, sink.getBuffer());
				} catch (Exception e) {
					throw Task.forceThrow(e);
				}
				if (sink.size() >= limit)
					throw new IllegalStateException("InputBufferMaxProtocolSize: unconsumed frame >= " + limit);
				sink.getBuffer().Compact();
			}
			return (int)Math.min(limit - sink.size(), Integer.MAX_VALUE);
		}

		@Override
		public void update(byte c) {
			room();
			sink.update(c);
		}

		@Override
		public void update(byte [] data, int off, int len) {
			while (len > 0) {
				int count = Math.min(len, room());
				sink.update(data, off, count);
				off += count;
				len -= count;
			}
		}

		@Override
		public void flush() {
			sink.flush();
		}

		@Override
		public void close() {
			sink.close();
		}
	}

	/*
	 * OutputBuffer 流式写入。
	 * 核心原则
	 * 保持它满载。由于OutputBuffer每次写2个Buffer，所以每次从operates导入时，使得operates为空或者
	 * OutputBuffer.getBufferSize() > 2；这样既能保持OutputBuffer满载writeTo，又能使得
	 * interestOps remove write保持一样的逻辑。
	 * 实现要点：operates用ConcurrentLinkedQueue，锁外批量导入（每轮至多导入blockSize*2的字节量）；
	 * interestOps逻辑不变；高并发完全由ConcurrentLinkedQueue承担，忙碌时不需要锁。
	 * 已知问题：
	 * 1. Socket.SendBufferSize是按Service配置的，但是OutputBuffer的Buffer.BlockSize是固定的，
	 *    对于大的SendBufferSize，无法发挥最佳性能。
	 *    可能的解决方法：
	 *    Service.start的时候把自己的SendBufferSize配置设置Max到相应的Selectors中，其中所有的Selector都采用这个Max。
	 *    这样的话基本上整个系统还是一个OutputBuffer.BlockSize。如果需要对特别的Service设置特别的BlockSize，让这个
	 *    Service使用独立的Selectors。
	 * 每次doWrite限制操作数与写出字节数，持续生产操作或codec续作也必须让出selector。
	 */
	private void doWrite(SocketChannel sc) throws Exception { // 只在selector线程调用
		sendCount++;
		int blockSize = selector.getSelectors().getBbPoolBlockSize();
		long byteBudget = (long)blockSize * 2;
		//noinspection DataFlowIssue
		int bufSize = outputBuffer.size();
		int operateCount = 0;
		long written = 0;
		while (operateCount < MAX_OPERATES_PER_WRITE && written < byteBudget) {
			//noinspection DataFlowIssue
			for (Action0 op; operateCount < MAX_OPERATES_PER_WRITE && bufSize < byteBudget
					&& (op = operates.poll()) != null; ) {
				operateCount++;
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
				written += rc;
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
			//noinspection DataFlowIssue
			if (operates.isEmpty() && flushed) { // 此时bufSize=0,下次循环会触发flush
				// 真的没有等待处理的操作了，去掉事件，返回。以后新的操作在下一次doWrite时处理。
				removeInterestOps(SelectionKey.OP_WRITE);
				if (operates.isEmpty()) { // 再判断一次,避免跟submitAction的并发竞争问题
					if (closePending)
						realClose();
					return;
				}
				addInterestOps(SelectionKey.OP_WRITE);
			}
			// 发现数据，继续尝试处理。
		}
	}

	private void realClose() {
		if ((byte)closeDetailHandle.getAndSet(this, (byte)REAL_CLOSED) == REAL_CLOSED) // 阻止递归关闭
			return;
		try {
			selectionKey.channel().close();
		} catch (Exception e) {
			logger.error("SocketChannel.close exception:", e);
		}
		selector.addTask(() -> { // 进selector线程调用
			if (outputBuffer != null)
				outputBuffer.close();
			try {
				getService().OnSocketDisposed(this);
			} catch (Exception e) {
				logger.error("service.OnSocketDisposed exception:", e);
			} finally {
				// 独立关闭双向 codec，避免一侧释放失败阻断另一侧 native 资源回收。
				Codec codec = inputCodecChain;
				if (codec != null) {
					inputCodecChain = null;
					try {
						codec.close();
					} catch (Throwable e) {
						logger.error("inputCodec.close exception:", e);
					}
				}
				codec = outputCodecChain;
				if (codec != null) {
					outputCodecChain = null;
					try {
						codec.close();
					} catch (Throwable e) {
						logger.error("outputCodec.close exception:", e);
					}
				}
			}
		});
		// 绕过合并门控直接唤醒，避免空闲 selector 上 dispose 任务无限期延迟。
		selector.wakeupDirect();
		if (timeThrottle != null)
			timeThrottle.close();
	}

	@Override
	protected void doClose(@Nullable Throwable ex, boolean gracefully) {
		if (ex != null) {
			if (ex instanceof IOException)
				logger.info("close: {} {}", this, ex);
			else
				logger.warn("close: {} exception:", this, ex);
		} else
			logger.info("close: {}{}", this, gracefully ? " gracefully" : "");

		if (acceptorOrConnector instanceof Connector) {
			try {
				((Connector)acceptorOrConnector).OnSocketClose(this, ex);
			} catch (Exception e) {
				logger.error("Connector.OnSocketClose exception:", e);
			}
		}
		try {
			getService().OnSocketClose(this, ex);
		} catch (Exception e) {
			logger.error("Service.OnSocketClose exception:", e);
		}

		// 监听 socket 无输出缓冲且不支持 OP_WRITE，直接 realClose。
		if (gracefully && type != Type.eServerSocket) {
			closePending = true;
			try {
				if (addInterestOps(SelectionKey.OP_WRITE))
					selector.wakeup();
			} catch (CancelledKeyException e) {
				// key 已取消不会再触发 OP_WRITE，直接 realClose 兜底回收。
				realClose();
				return;
			}
			// scheduleNow 不随事务回滚取消，确保超时兜底关闭。
			TaskSpec.ofAction(this::realClose).scheduleNow(120 * 1000); // 最多给2分钟清空输出队列。
		} else
			realClose();
	}

	@Override
	public String toString() {
		SocketAddress localAddress = getLocalAddress();
		SocketAddress remoteAddress = this.remoteAddress;
		return "[" + getSessionId() + ']' +
				(localAddress != null ? localAddress : (acceptorOrConnector instanceof Acceptor ?
						((Acceptor)acceptorOrConnector).getName() : "")) + "-" + // 如果有localAddress则表示还没close
				(remoteAddress != null ? remoteAddress : (acceptorOrConnector instanceof Connector ?
						((Connector)acceptorOrConnector).getName() : "")); // 如果有RemoteAddress则表示曾经连接成功过
	}
}
