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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongSupplier;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Action0;
import Zeze.Util.ShutdownHook;
import Zeze.Util.Task;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class AsyncSocket implements SelectorHandle, Closeable {
	public static final Logger logger = LogManager.getLogger(AsyncSocket.class);
	public static final Level LEVEL_PROTOCOL_LOG = Level.toLevel(System.getProperty("protocolLog"), Level.OFF);
	public static final boolean ENABLE_PROTOCOL_LOG = LEVEL_PROTOCOL_LOG != Level.OFF;
	private static final VarHandle closedHandle;
	private static final byte SEND_CLOSE_DETAIL_MAX = 100; // 必须小于REAL_CLOSED
	private static final byte REAL_CLOSED = Byte.MAX_VALUE;
	private static final AtomicLong sessionIdGen = new AtomicLong(1);
	private static LongSupplier sessionIdGenFunc;

	static {
		try {
			closedHandle = MethodHandles.lookup().findVarHandle(AsyncSocket.class, "closed", byte.class);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
		ShutdownHook.init();
	}

	public static void setSessionIdGenFunc(LongSupplier seed) {
		sessionIdGenFunc = seed;
	}

	private static long nextSessionId() {
		var genFunc = sessionIdGenFunc;
		return genFunc != null ? genFunc.getAsLong() : sessionIdGen.getAndIncrement();
	}

	private final ReentrantLock lock = new ReentrantLock();
	private long sessionId = nextSessionId(); // 只在setSessionId里修改
	private final Service service;
	private final Acceptor acceptor;
	private final Connector connector;

	private ArrayList<Action0> operates0 = new ArrayList<>();
	private ArrayList<Action0> operates1 = new ArrayList<>();
	private final AtomicLong outputBufferListCountSum = new AtomicLong();
	private final ArrayDeque<java.nio.ByteBuffer> outputBufferListSending = new ArrayDeque<>(); // 正在发送的 buffers. 只在selector线程访问

	private final BufferCodec inputCodecBuffer = new BufferCodec(); // 记录这个变量用来操作buffer. 只在selector线程访问
	private final BufferCodec outputCodecBuffer = new BufferCodec(); // 记录这个变量用来操作buffer. 只在selector线程访问
	private Codec inputCodecChain; // 只在selector线程访问
	private Codec outputCodecChain; // 只在selector线程访问
	private volatile byte security; // 1:Input; 2:Output; 1|2:Input+Output

	private final Selector selector;
	private final SelectionKey selectionKey;
	private volatile SocketAddress remoteAddress; // 连接成功时设置
	private volatile Object userState;
	private volatile boolean isHandshakeDone;
	@SuppressWarnings("unused")
	private volatile byte closed;
	private boolean closePending;
	private long recvSize; // 从socket接收数据的统计总字节数
	private long sendSize; // 向socket发送数据的统计总字节数

	private int sendBufferSize; // 缓存下来为了避免每次去查询。是一种优化。

	public long getSessionId() {
		return sessionId;
	}

	public void setSessionId(long newSessionId) {
		service.changeSocketSessionId(this, newSessionId);
		sessionId = newSessionId;
	}

	public Service getService() {
		return service;
	}

	public Acceptor getAcceptor() {
		return acceptor;
	}

	public Connector getConnector() {
		return connector;
	}

	public SelectableChannel getChannel() { // SocketChannel or ServerSocketChannel, 一定不为null
		return selectionKey.channel();
	}

	public Socket getSocket() {
		SelectableChannel sc = getChannel();
		return sc instanceof SocketChannel ? ((SocketChannel)sc).socket() : null;
	}

	public SocketAddress getLocalAddress() { // 已经close的情况下返回null
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

	public InetAddress getLocalInetAddress() { // 已经close的情况下返回null
		SocketAddress sa = getLocalAddress();
		return sa instanceof InetSocketAddress ? ((InetSocketAddress)sa).getAddress() : null;
	}

	public SocketAddress getRemoteAddress() { // 连接成功前返回null, 成功后即使close也不会返回null
		return remoteAddress;
	}

	public InetAddress getRemoteInetAddress() { // 连接成功前返回null, 成功后即使close也不会返回null
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

	public long getRecvSize() {
		return recvSize;
	}

	public long getSendSize() {
		return sendSize;
	}

	/**
	 * for server socket
	 */
	public AsyncSocket(Service service, InetSocketAddress localEP, Acceptor acceptor) {
		this.service = service;
		this.acceptor = acceptor;
		connector = null;

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
			logger.info("Listen: {} for {}:{}", localEP, service.getClass().getName(), service.getName());

			selector = service.getSelectors().choice();
			selectionKey = selector.register(ssc, 0, this); // 先获取key,因为有小概率出现事件处理比赋值更先执行
			selector.register(ssc, SelectionKey.OP_ACCEPT, this);
		} catch (IOException e) {
			if (ssc != null) {
				try {
					ssc.close();
				} catch (Throwable ex) {
					logger.error("ServerSocketChannel.close", ex);
				}
			}
			throw new RuntimeException("bind " + localEP, e);
		}
	}

	@Override
	public void doHandle(SelectionKey key) throws Throwable {
		SelectableChannel channel = key.channel();
		int ops = key.readyOps();
		if ((ops & SelectionKey.OP_READ) != 0)
			ProcessReceive((SocketChannel)channel);
		if ((ops & SelectionKey.OP_WRITE) != 0)
			doWrite((SocketChannel)channel);

		if ((ops & SelectionKey.OP_ACCEPT) != 0) {
			SocketChannel sc = null;
			try {
				sc = ((ServerSocketChannel)channel).accept();
				if (sc != null)
					service.OnSocketAccept(new AsyncSocket(service, sc, acceptor));
			} catch (Throwable e) {
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
			} catch (Throwable ex) {
				e = ex;
			}
			service.OnSocketConnectError(this, e);
			close(e); // if OnSocketConnectError throw Exception, this will close in doException
		}
	}

	@Override
	public void doException(SelectionKey key, Throwable e) {
		close(e);
	}

	/**
	 * use inner. create when accepted;
	 */
	private AsyncSocket(Service service, SocketChannel sc, Acceptor acceptor) throws IOException {
		this.service = service;
		this.acceptor = acceptor;
		connector = null;

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
		sendBufSize = so.getSendBufferSize();
		Boolean noDelay = this.service.getSocketOptions().getNoDelay();
		if (noDelay != null)
			so.setTcpNoDelay(noDelay);

		selector = service.getSelectors().choice();
		selectionKey = selector.register(sc, 0, this); // 先获取key,因为有小概率出现事件处理比赋值更先执行
		selector.register(sc, SelectionKey.OP_READ, this);
		logger.info("Accept: {} for {}:{}", this, service.getClass().getName(), service.getName());
	}

	/**
	 * for client socket. connect
	 */
	private void doConnectSuccess(SocketChannel sc) throws Throwable {
		remoteAddress = sc.socket().getRemoteSocketAddress();
		logger.info("Connect: {} for {}:{}", this, service.getClass().getName(), service.getName());
		if (connector != null)
			connector.OnSocketConnected(this);
		service.OnSocketConnected(this);
		interestOps(SelectionKey.OP_CONNECT, SelectionKey.OP_READ);
	}

	public AsyncSocket(Service service, String hostNameOrAddress, int port, Object userState, Connector connector) {
		this.service = service;
		this.connector = connector;
		acceptor = null;
		this.userState = userState;

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

			selector = service.getSelectors().choice();
			InetAddress address = InetAddress.getByName(hostNameOrAddress); // TODO async dns lookup
			selectionKey = selector.register(sc, 0, this); // 先获取key,因为有小概率出现事件处理比赋值更先执行
			// 必须在connect前设置，否则selectionKey没初始化，有可能事件丢失？（现象好像是doHandle触发了）。
			if (sc.connect(new InetSocketAddress(address, port))) { // 马上成功时，还没有注册到Selector中。
				selector.register(sc, SelectionKey.OP_READ, this);
				doConnectSuccess(sc);
			} else
				selector.register(sc, SelectionKey.OP_CONNECT, this);
		} catch (Throwable e) {
			if (sc != null) {
				try {
					sc.close();
				} catch (Throwable ex) {
					logger.error("SocketChannel.close", ex);
				}
			}
			throw new RuntimeException(e);
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

	public void VerifySecurity() {
		if (service.getConfig().getHandshakeOptions().getEnableEncrypt() && !isSecurity())
			throw new IllegalStateException(service.getName() + " !isSecurity");
	}

	public void SetInputSecurityCodec(byte[] key, boolean compress) {
		SubmitAction(() -> { // 进selector线程调用
			Codec chain = inputCodecBuffer;
			if (compress)
				chain = new Decompress(chain);
			if (key != null)
				chain = new Decrypt(chain, key);
			inputCodecChain = chain;
			//noinspection NonAtomicOperationOnVolatileField
			security |= 1;
		});
	}

	public void SetOutputSecurityCodec(byte[] key, boolean compress) {
		SubmitAction(() -> { // 进selector线程调用
			Codec chain = outputCodecBuffer;
			if (key != null)
				chain = new Encrypt(chain, key);
			if (compress)
				chain = new Compress(chain);
			outputCodecChain = chain;
			//noinspection NonAtomicOperationOnVolatileField
			security |= 2;
		});
	}

	public boolean SubmitAction(Action0 callback) {
		lock.lock();
		try {
			if (closed != 0)
				return false;
			var operates = operates0;
			operates.add(callback);
			if (operates.size() == 1)
				interestOps(0, SelectionKey.OP_WRITE); // try
			return true;
		} finally {
			lock.unlock();
		}
	}

	private void interestOps(int remove, int add) {
		int ops = selectionKey.interestOps();
		int opsNew = (ops & ~remove) | add;
		if (ops != opsNew) {
			selectionKey.interestOps(opsNew);
			selector.wakeup();
		}
	}

	/**
	 * 可能直接加到发送缓冲区，返回true则bytes不能再修改了。
	 */
	public boolean Send(byte[] bytes, int offset, int length) {
		ByteBuffer.VerifyArrayIndex(bytes, offset, length);

		byte c = closed;
		if (c != 0) {
			if (c < SEND_CLOSE_DETAIL_MAX) {
				closedHandle.compareAndSet(this, (byte)c, (byte)(c + 1));
				logger.error("Send to closed socket: {} len={}", this, length, new Exception());
			} else
				logger.error("Send to closed socket: {} len={}", this, length);
			return false;
		}
		try {
			if (!service.checkOverflow(this, outputBufferListCountSum.addAndGet(length), bytes, offset, length)) {
				outputBufferListCountSum.addAndGet(-length);
				return false;
			}
			if (SubmitAction(() -> { // 进selector线程调用
				Codec codec = outputCodecChain;
				if (codec != null) {
					// 压缩加密等 codec 链操作。
					ByteBuffer codecBuf = outputCodecBuffer.getBuffer(); // codec对buffer的引用一定是不可变的
					codecBuf.EnsureWrite(length); // reserve
					codec.update(bytes, offset, length);
					codec.flush();
					int newLen = codecBuf.Size();
					int deltaLen = newLen - length;
					if (deltaLen != 0)
						outputBufferListCountSum.getAndAdd(deltaLen);
					outputBufferListSending.addLast(java.nio.ByteBuffer.wrap(codecBuf.Bytes, codecBuf.ReadIndex, newLen));
					codecBuf.FreeInternalBuffer();
				} else
					outputBufferListSending.addLast(java.nio.ByteBuffer.wrap(bytes, offset, length));
			}))
				return true;
			logger.error("Send to closed socket: {} len={}", this, length, new Exception());
		} catch (Throwable ex) {
			outputBufferListCountSum.addAndGet(-length);
			close(ex);
		}
		return false;
	}

	public boolean Send(Protocol<?> protocol) {
		if (ENABLE_PROTOCOL_LOG) {
			if (protocol.isRequest()) {
				if (protocol instanceof Rpc)
					logger.log(LEVEL_PROTOCOL_LOG, "SEND[{}] {}({}): {}", sessionId, protocol.getClass().getSimpleName(),
							((Rpc<?, ?>)protocol).getSessionId(), protocol.Argument);
				else
					logger.log(LEVEL_PROTOCOL_LOG, "SEND[{}] {}: {}", sessionId, protocol.getClass().getSimpleName(), protocol.Argument);
			} else
				logger.log(LEVEL_PROTOCOL_LOG, "SEND[{}] {}({})> {}", sessionId, protocol.getClass().getSimpleName(),
						((Rpc<?, ?>)protocol).getSessionId(), protocol.getResultBean());
		}
		return Send(protocol.encode());
	}

	public boolean Send(ByteBuffer bb) { // 返回true则bb不能再修改了
		return Send(bb.Bytes, bb.ReadIndex, bb.Size());
	}

	public boolean Send(Binary binary) {
		return Send(binary.bytesUnsafe(), binary.getOffset(), binary.size());
	}

	public boolean Send(String str) {
		return Send(str.getBytes(StandardCharsets.UTF_8));
	}

	public boolean Send(byte[] bytes) { // 返回true则bytes不能再修改了
		return Send(bytes, 0, bytes.length);
	}

	private void ProcessReceive(SocketChannel sc) throws Throwable { // 只在selector线程调用
		java.nio.ByteBuffer buffer = selector.getReadBuffer(); // 线程共享的buffer,只能本方法内临时使用
		buffer.clear();
		int bytesTransferred = sc.read(buffer);
		if (bytesTransferred > 0) {
			recvSize += bytesTransferred;
			ByteBuffer codecBuf = inputCodecBuffer.getBuffer(); // codec对buffer的引用一定是不可变的
			Codec codec = inputCodecChain;
			if (codec != null) {
				// 解密解压处理，处理结果直接加入 inputCodecBuffer。
				codecBuf.EnsureWrite(bytesTransferred);
				codec.update(buffer.array(), 0, bytesTransferred);
				codec.flush();
				service.OnSocketProcessInputBuffer(this, codecBuf);
			} else if (codecBuf.Size() > 0) {
				// 上次解析有剩余数据（不完整的协议），把新数据加入。
				codecBuf.Append(buffer.array(), 0, bytesTransferred);

				service.OnSocketProcessInputBuffer(this, codecBuf);
			} else {
				ByteBuffer avoidCopy = ByteBuffer.Wrap(buffer.array(), 0, bytesTransferred);

				service.OnSocketProcessInputBuffer(this, avoidCopy);

				if (avoidCopy.Size() > 0) // 有剩余数据（不完整的协议），加入 inputCodecBuffer 等待新的数据。
					codecBuf.Append(avoidCopy.Bytes, avoidCopy.ReadIndex, avoidCopy.Size());
			}

			// 1 检测 buffer 是否满，2 剩余数据 Compact，3 需要的话，释放buffer内存。
			int remain = codecBuf.Size();
			if (remain <= 0) {
				if (codecBuf.Capacity() <= 32 * 1024)
					codecBuf.Reset();
				else
					codecBuf.FreeInternalBuffer(); // 只在过大的缓冲区时释放内部bytes[], 避免频繁分配
			} else {
				int max = service.getSocketOptions().getInputBufferMaxProtocolSize();
				if (remain >= max)
					throw new IllegalStateException("InputBufferMaxProtocolSize " + remain + " >= " + max);
				codecBuf.Compact();
			}
		} else
			close(); // 对方正常关闭连接时的处理, 不设置异常; 连接被对方RESET的话read会抛异常
	}

	private void doWrite(SocketChannel sc) throws Throwable { // 只在selector线程调用
		while (true) {
			var operates = operates0;
			lock.lock();
			operates0 = operates1; // swap
			operates1 = operates;
			lock.unlock();
			//noinspection ForLoopReplaceableByForEach
			for (int i = 0, n = operates.size(); i < n; i++)
				operates.get(i).run();
			operates.clear();

			int bufSize = outputBufferListSending.size();
			if (bufSize <= 0) {
				// 时间窗口
				// 必须和把Operate加入队列同步！否则可能会出现，刚加入操作没有被处理，但是OP_WRITE又被Remove的问题。
				lock.lock();
				try {
					if (operates0.isEmpty()) {
						// 真的没有等待处理的操作了，去掉事件，返回。以后新的操作在下一次doWrite时处理。
						interestOps(SelectionKey.OP_WRITE, 0);
						if (closePending)
							realClose();
						return;
					}
				} finally {
					lock.unlock();
				}
				// 发现数据，继续尝试处理。
			} else {
				var willSend = this.sendBufferSize;
				var sendBuffers = new java.nio.ByteBuffer[1000];
				var i = 0;
				for (var it = outputBufferListSending.iterator(); i < 2000 && it.hasNext(); /*nothing*/) {
					var buffer = it.next();
					sendBuffers[i++] = buffer;
					willSend -= buffer.limit();
					if (willSend <= 0)
						break;
				}
				long rc = sc.write(sendBuffers, 0, i);
//				long rc = bufSize == 1 ? sc.write(outputBufferListSending.peekFirst()) :
//						sc.write(outputBufferListSending.toArray(new java.nio.ByteBuffer[bufSize]));
				if (rc < 0) {
					close(); // 很罕见的正常关闭, 不设置异常, 其实write抛异常的可能性更大
					return;
				}
				sendSize += rc;
				outputBufferListCountSum.getAndAdd(-rc);
				for (java.nio.ByteBuffer bb; (bb = outputBufferListSending.pollFirst()) != null; ) {
					if (bb.remaining() > 0) {
						outputBufferListSending.addFirst(bb);
						// 有数据正在发送，此时可以安全退出执行，写完以后Selector会再次触发doWrite。
						// add write event，里面判断了事件没有变化时不做操作，严格来说，再次注册事件是不需要的。
						interestOps(0, SelectionKey.OP_WRITE);
						return;
					}
				}
				// 全部都写出去了，继续尝试看看有没有新的操作。
			}
		}
	}

	private void realClose() {
		if ((byte)closedHandle.getAndSet(this, REAL_CLOSED) == REAL_CLOSED) // 阻止递归关闭
			return;
		try {
			selectionKey.channel().close();
		} catch (Throwable e) {
			logger.error("SocketChannel.close", e);
		}
		try {
			service.OnSocketDisposed(this);
		} catch (Throwable e) {
			logger.error("Service.OnSocketDisposed", e);
		}
		if (acceptor != null)
			selector.wakeup(); // Acceptor的socket需要selector在select开始时执行,所以wakeup一下尽早触发下次select
	}

	private boolean close(Throwable ex, boolean gracefully) {
		if (!closedHandle.compareAndSet(this, (byte)0, (byte)1)) // 阻止递归关闭
			return false;

		if (ex != null) {
			if (ex instanceof IOException)
				logger.info("close: {} {}", this, ex);
			else
				logger.warn("close: {}", this, ex);
		} else
			logger.debug("close{}: {}", gracefully ? " gracefully" : "", this);

		if (connector != null) {
			try {
				connector.OnSocketClose(this, ex);
			} catch (Throwable e) {
				logger.error("Connector.OnSocketClose", e);
			}
		}
		try {
			service.OnSocketClose(this, ex);
		} catch (Throwable e) {
			logger.error("Service.OnSocketClose", e);
		}

		if (gracefully) {
			lock.lock();
			try {
				closePending = true;
				interestOps(0, SelectionKey.OP_WRITE); // try
			} finally {
				lock.unlock();
			}
			Task.schedule(120 * 1000, this::realClose); // 最多给2分钟清空输出队列。
		} else
			realClose();
		return true;
	}

	// 优雅的关闭一般用于正常流程，不提供异常参数。
	public boolean closeGracefully() {
		return close(null, true);
	}

	public boolean close(Throwable ex) {
		return close(ex, false);
	}

	@Override
	public void close() {
		close(null);
	}

	@Override
	public String toString() {
		SocketAddress localAddress = getLocalAddress();
		SocketAddress remoteAddress = this.remoteAddress;
		return "[" + sessionId + ']' +
				(localAddress != null ? localAddress : (acceptor != null ? acceptor.getName() : "")) + "-" + // 如果有localAddress则表示还没close
				(remoteAddress != null ? remoteAddress : (connector != null ? connector.getName() : "")); // 如果有RemoteAddress则表示曾经连接成功过
	}
}
