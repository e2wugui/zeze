package Zeze.Net;

import Zeze.Serialize.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public final class AsyncSocket implements SelectorHandle, Closeable {
	private static final Logger logger = LogManager.getLogger(AsyncSocket.class);

	final class OperateSend implements Runnable {
		private byte[] bytes;
		private int offset;
		private int length;

		public OperateSend(byte[] bytes, int offset, int length) {
			this.bytes = bytes;
			this.offset = offset;
			this.length = length;
			_outputBufferListCountSum.addAndGet(length);
		}

		@Override
		public void run() {
			_prepareSending(bytes, offset, length);
			_outputBufferListCountSum.addAndGet(-length);
		}
	}

	final class OperateSetOutputSecurityCodec implements Runnable {
		private byte[] key;
		private boolean compress;
		private Runnable callback;

		public OperateSetOutputSecurityCodec(byte[] key, boolean compress, Runnable callback) {
			this.key = key;
			this.compress = compress;
			this.callback = callback;
		}

		@Override
		public void run() {
			_SetOutputSecurityCodec(key, compress);
			if (null != callback)
				callback.run();
		}
	}

	final class OperateSetInputSecurityCodec implements Runnable {
		private byte[] key;
		private boolean compress;
		private Runnable callback;

		public OperateSetInputSecurityCodec(byte[] key, boolean compress, Runnable callback) {
			this.key = key;
			this.compress = compress;
			this.callback = callback;
		}

		@Override
		public void run() {
			_SetInputSecurityCodec(key, compress);
			if (null != callback)
				callback.run();
		}
	}

	private LinkedBlockingQueue<Runnable> _operates = new LinkedBlockingQueue<>();
	private AtomicInteger _outputBufferListCountSum = new AtomicInteger();
	private ArrayList<java.nio.ByteBuffer> _outputBufferListSending = null; // 正在发送的 buffers.

	private Service Service;
	public Service getService() {
		return Service;
	}
	private void setService(Service value) {
		Service = value;
	}

	private Connector Connector;
	public Connector getConnector() {
		return Connector;
	}

	private Acceptor Acceptor;
	public Acceptor getAcceptor() {
		return Acceptor;
	}

	private RuntimeException LastException;
	public RuntimeException getLastException() {
		return LastException;
	}
	private void setLastException(RuntimeException value) {
		LastException = value;
	}

	private long SessionId;
	public long getSessionId() {
		return SessionId;
	}

	private SelectionKey selectionKey;
	private final Selector selector;

	public SocketChannel getSocketChannel() {
		return (SocketChannel)selectionKey.channel();
	}

	public Socket getSocket() {
		return ((SocketChannel)selectionKey.channel()).socket();
	}

	/** 
	 保存需要存储在Socket中的状态。
	 简单变量，没有考虑线程安全问题。
	 内部不使用。
	*/
	private Object UserState;
	public Object getUserState() {
		return UserState;
	}
	public void setUserState(Object value) {
		UserState = value;
	}
	private boolean IsHandshakeDone;
	public boolean isHandshakeDone() {
		return IsHandshakeDone;
	}
	public void setHandshakeDone(boolean value) {
		IsHandshakeDone = value;
	}

	private static final AtomicLong SessionIdGen = new AtomicLong();

	private final BufferCodec inputCodecBuffer = new BufferCodec(); // 记录这个变量用来操作buffer
	private final BufferCodec outputCodecBuffer = new BufferCodec(); // 记录这个变量用来操作buffer

	private Codec inputCodecChain;
	private Codec outputCodecChain;

	private String RemoteAddress;
	public String getRemoteAddress() {
		return RemoteAddress;
	}

	/** 
	 for server socket
	*/
	public AsyncSocket(Service service, InetSocketAddress localEP, Acceptor acceptor) {
		this.setService(service);
		this.Acceptor = acceptor;

		try {
			ServerSocketChannel ssc = ServerSocketChannel.open();
			ServerSocket ss = ssc.socket();
			ssc.configureBlocking(false);
			ss.setReuseAddress(true);
			// xxx 只能设置到 ServerSocket 中，以后 Accept 的连接通过继承机制得到这个配置。
			if (null != service.getSocketOptions().getReceiveBuffer())
				ss.setReceiveBufferSize(service.getSocketOptions().getReceiveBuffer());
			ss.bind(localEP, service.getSocketOptions().getBacklog());

			SessionId = SessionIdGen.incrementAndGet();

			selector = Selectors.getInstance().choice();
			selectionKey = selector.register(ssc, SelectionKey.OP_ACCEPT, this);	
		}
		catch (Throwable ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void doHandle(SelectionKey key) throws Throwable {
		if (key.isAcceptable()) {
			SocketChannel sc = null;
			try {
				ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
				sc = ssc.accept();
				if (null != sc)
					Service.OnSocketAccept(new AsyncSocket(Service, sc, this.Acceptor));
			} catch (Throwable e) {
				if (null != sc)
					sc.close();
				Service.OnSocketAcceptError(this, e);
				// skip all error
			}
			return;
		}

		if (key.isConnectable()) {
			try {
				SocketChannel sc = (SocketChannel) key.channel();
				if (sc.finishConnect()) {
					// 先修改事件，防止doConnectSuccess发送数据注册了新的事件导致OP_CONNECT重新触发。
					// 虽然实际上在回调中应该不会唤醒Selector重入。
					interestOps(SelectionKey.OP_CONNECT, SelectionKey.OP_READ);
					doConnectSuccess(sc);
					return;
				}
				Service.OnSocketConnectError(this, null);
			}
			catch (RuntimeException e) {
				Service.OnSocketConnectError(this, e);
				close();
			}
			return;
		}

		if (key.isWritable()) {
			doWrite(key);
		}

		if (key.isReadable()) {
			ProcessReceive((SocketChannel) key.channel());
		}
	}

	@Override
	public void doException(SelectionKey key, Throwable e) throws Throwable {
		var This = (AsyncSocket)key.attachment();
		logger.error("doException {}", This.RemoteAddress, e);
	}

	/** 
	 use inner. create when accept;
	*/
	private AsyncSocket(Service service, SocketChannel sc, Acceptor acceptor) throws IOException {
		this.setService(service);
		this.Acceptor = acceptor;

		// 据说连接接受以后设置无效，应该从 ServerSocket 继承
		Socket so = sc.socket();
		if (null != Service.getSocketOptions().getReceiveBuffer())
			so.setReceiveBufferSize(Service.getSocketOptions().getReceiveBuffer());
		if (null != Service.getSocketOptions().getSendBuffer())
			so.setSendBufferSize(Service.getSocketOptions().getSendBuffer());
		if (null != Service.getSocketOptions().getNoDelay())
			so.setTcpNoDelay(Service.getSocketOptions().getNoDelay());
		sc.configureBlocking(false);

		this.SessionId = SessionIdGen.incrementAndGet();
		RemoteAddress = so.getRemoteSocketAddress().toString();

		selector = Selectors.getInstance().choice();
		selectionKey = selector.register(sc, SelectionKey.OP_READ, this);
	}

	/** 
	 for client socket. connect
	*/
	private void doConnectSuccess(SocketChannel sc) {
		if (Connector != null) {
			Connector.OnSocketConnected(this);
		}
		RemoteAddress = sc.socket().getInetAddress().getAddress().toString();
		Service.OnSocketConnected(this);
	}

	public AsyncSocket(Service service, String hostNameOrAddress, int port, Object userState, Connector connector) {
		this.setService(service);
		this.Connector = connector;

		UserState = userState;
		this.SessionId = SessionIdGen.incrementAndGet();

		SocketChannel sc = null;
		try {
			sc = SocketChannel.open();
			sc.configureBlocking(false);
			Socket so = sc.socket();			
			if (null != Service.getSocketOptions().getNoDelay())
				so.setTcpNoDelay(Service.getSocketOptions().getNoDelay());
			if (null != Service.getSocketOptions().getReceiveBuffer())
				so.setReceiveBufferSize(Service.getSocketOptions().getReceiveBuffer());
			if (null != Service.getSocketOptions().getSendBuffer())
				so.setSendBufferSize(Service.getSocketOptions().getSendBuffer());

			selector = Selectors.getInstance().choice();
			var address = InetAddress.getByName(hostNameOrAddress); // TODO async dns lookup
			if (sc.connect(new InetSocketAddress(address, port))) {
				// 马上成功时，还没有注册到Selector中。
				selectionKey = selector.register(sc, SelectionKey.OP_READ, this);
				doConnectSuccess(sc);
			} else {
				selectionKey = selector.register(sc, SelectionKey.OP_CONNECT, this);
			}
		} catch (Throwable e) {
			if (null != sc)
				try {
					sc.close();
				} catch (Throwable e2) {
					// skip
				}
			throw new RuntimeException(e);
		}
	}

	public void SetOutputSecurityCodec(byte[] key, boolean compress, Runnable callback) {
		synchronized (this) {
			_operates.add(new OperateSetOutputSecurityCodec(key, compress, callback));
			this.interestOps(0, SelectionKey.OP_WRITE); // try
		}
	}

	private void _SetOutputSecurityCodec(byte[] key, boolean compress) {
		synchronized (this) {
			Codec chain = outputCodecBuffer;
			if (null != key) {
				chain = new Encrypt(chain, key);
			}
			if (compress) {
				chain = new Compress(chain);
			}
			/*
			if (outputCodecChain != null) {
				outputCodecChain.close();
			}
			*/
			outputCodecChain = chain;
			setOutputSecurity(true);
		}
	}

	private boolean IsInputSecurity;
	public boolean isInputSecurity() {
		return IsInputSecurity;
	}
	private void setInputSecurity(boolean value) {
		IsInputSecurity = value;
	}
	private boolean IsOutputSecurity;
	public boolean isOutputSecurity() {
		return IsOutputSecurity;
	}
	private void setOutputSecurity(boolean value) {
		IsOutputSecurity = value;
	}
	public boolean isSecurity() {
		return isInputSecurity() && isOutputSecurity();
	}

	public void VerifySecurity() {
		if (!isSecurity()) {
			throw new RuntimeException(getService().getName() + " !IsSecurity");
		}
	}

	public void SetInputSecurityCodec(byte[] key, boolean compress, Runnable callback) {
		synchronized (this) {
			_operates.add(new OperateSetInputSecurityCodec(key, compress, callback));
			this.interestOps(0, SelectionKey.OP_WRITE); // try
		}
	}

	private void _SetInputSecurityCodec(byte[] key, boolean compress) {
		synchronized (this) {
			Codec chain = inputCodecBuffer;
			if (compress) {
				chain = new Decompress(chain);
			}
			if (null != key) {
				chain = new Decrypt(chain, key);
			}
			/*
			if (inputCodecChain != null) {
				inputCodecChain.close();
			}
			*/
			inputCodecChain = chain;
			setInputSecurity(true);
		}
	}

	public boolean Send(Protocol protocol) {
		return Send(protocol.Encode());
	}

	public boolean Send(Zeze.Serialize.ByteBuffer bb) {
		return Send(bb.Bytes, bb.ReadIndex, bb.Size());
	}

	public boolean Send(Binary binary) {
		return Send(binary.InternalGetBytesUnsafe(), binary.getOffset(), binary.size());
	}

	public boolean Send(byte[] bytes) {
		return Send(bytes, 0, bytes.length);
	}

	/** 
	 可能直接加到发送缓冲区，不能再修改bytes了。
	 */
	public boolean Send(byte[] bytes, int offset, int length) {
		Zeze.Serialize.ByteBuffer.VerifyArrayIndex(bytes, offset, length);

		if (null == selectionKey) {
			return false;
		}
		if (_outputBufferListCountSum.get() + length > Service.getSocketOptions().getOutputBufferMaxSize())
			return false;
		synchronized (this) {
			_operates.add(new OperateSend(bytes, offset, length));
			this.interestOps(0, SelectionKey.OP_WRITE); // try
		}
		return true;
	}

	public boolean Send(String str) {
		return Send(str.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	private void ProcessReceive(SocketChannel sc) throws IOException {
		var buffer = java.nio.ByteBuffer.allocate(32 * 1024);
		int BytesTransferred = sc.read(buffer);
		if (BytesTransferred > 0) {
			if (null != inputCodecChain) {
				// 解密解压处理，处理结果直接加入 inputCodecBuffer。
				inputCodecBuffer.getBuffer().EnsureWrite(BytesTransferred);
				inputCodecChain.update(buffer.array(), 0, BytesTransferred);
				inputCodecChain.flush();

				this.getService().OnSocketProcessInputBuffer(this, inputCodecBuffer.getBuffer());
			}
			else if (inputCodecBuffer.getBuffer().Size() > 0) {
				// 上次解析有剩余数据（不完整的协议），把新数据加入。
				inputCodecBuffer.getBuffer().Append(buffer.array(), 0, BytesTransferred);

				this.getService().OnSocketProcessInputBuffer(this, inputCodecBuffer.getBuffer());
			}
			else {
				ByteBuffer avoidCopy = ByteBuffer.Wrap(buffer.array(), 0, BytesTransferred);

				this.getService().OnSocketProcessInputBuffer(this, avoidCopy);

				if (avoidCopy.Size() > 0) { // 有剩余数据（不完整的协议），加入 inputCodecBuffer 等待新的数据。
					inputCodecBuffer.getBuffer().Append(avoidCopy.Bytes, avoidCopy.ReadIndex, avoidCopy.Size());
				}
			}

			// 1 检测 buffer 是否满，2 剩余数据 Campact，3 需要的话，释放buffer内存。
			int remain = inputCodecBuffer.getBuffer().Size();
			if (remain > 0) {
				var max = getService().getSocketOptions().getInputBufferMaxProtocolSize();
				if (remain >= max) {
					throw new RuntimeException("InputBufferMaxProtocolSize " + max);
				}
				inputCodecBuffer.getBuffer().Campact();
			}
			else {
				inputCodecBuffer.getBuffer().FreeInternalBuffer(); // 解析缓冲如果为空，马上释放内部bytes[]。
			}
		}
		else {
			close(); // 正常关闭，不设置异常
		}
	}

	private void interestOps(int remove, int add) {
		int ops = selectionKey.interestOps();
		int opsNew = (ops & ~remove) | add;
		if (ops != opsNew) {
			selectionKey.interestOps(opsNew);
			if (Thread.currentThread().getId() != selector.getId())
				selectionKey.selector().wakeup();
		}
	}

	private void _prepareSending(byte[] bytes, int offset, int length) {
		if (null == _outputBufferListSending)
			_outputBufferListSending = new ArrayList<>();

		if (null != outputCodecChain) {
			// 压缩加密等 codec 链操作。
			outputCodecBuffer.getBuffer().EnsureWrite(length); // reserve
			outputCodecChain.update(bytes, offset, length);
			outputCodecChain.flush();
			var codec = outputCodecBuffer.getBuffer();
			_outputBufferListSending.add(java.nio.ByteBuffer.wrap(codec.Bytes, codec.ReadIndex, codec.Size()));
			// 加入Sending后outputBufferCodec需要释放对byte[]的引用。
			codec.FreeInternalBuffer();
		}
		else {
			_outputBufferListSending.add(java.nio.ByteBuffer.wrap(bytes, offset, length));
		}
	}

	private void doWrite(SelectionKey key) {
		while (true) {
			for (var op = _operates.poll(); op != null; op = _operates.poll()) {
				op.run();
			}

			if (null == _outputBufferListSending) {
				// 时间窗口
				// 必须和把Operate加入队列同步！否则可能会出现，刚加入操作没有被处理，但是OP_WRITE又被Remove的问题。
				synchronized (this) {
					if (_operates.isEmpty()) {
						// 真的没有等待处理的操作了，去掉事件，返回。以后新的操作在下一次doWrite时处理。
						this.interestOps(SelectionKey.OP_WRITE, 0);
						return;
					}
					continue; // 发现数据，继续尝试处理。
				}
			}

			try {
				var sc = (SocketChannel) key.channel();
				var rc = sc.write(_outputBufferListSending.toArray(new java.nio.ByteBuffer[_outputBufferListSending.size()]));
				if (rc >= 0) {
					int i = 0;
					for (; i < _outputBufferListSending.size() && _outputBufferListSending.get(i).remaining() == 0; ++i) {
						// nothing
					}
					_outputBufferListSending.subList(0, i).clear();
					if (_outputBufferListSending.isEmpty()) {
						_outputBufferListSending = null; // free output buffer
						continue; // 全部都写出去了，继续尝试看看有没有新的操作。
					}
					// 有数据正在发送，此时可以安全退出执行，写完以后Selector会再次触发doWrite。
					// add write event，里面判断了事件没有变化时不做操作，严格来说，再次注册事件是不需要的。
					this.interestOps(0, SelectionKey.OP_WRITE);
					return;
				}
				// error
				close();
			} catch (IOException e) {
				close();
				throw new RuntimeException(e);
			}
		}
	}

	public void Close(RuntimeException e) {
		this.setLastException(e);
		close();
	}

	public void close() {
		try {
			synchronized (this) {
				if (selectionKey == null) {
					return;
				}

				try {
					if (Connector != null) {
						Connector.OnSocketClose(this);
					}
					Service.OnSocketClose(this, this.getLastException());
					selectionKey.channel().close();
					selectionKey = null;
				}
				catch (RuntimeException e) {
					// skip Dispose error
				}
			}

			synchronized (this) {
				try {
					Service.OnSocketDisposed(this);
				}
				catch (RuntimeException e2) {
					// skip Dispose error
				}
			}
		}
		catch (Throwable ex) {
			throw new RuntimeException(ex); // TODO skip close exception?
		}
	}

	public void setSessionId(long newSessionId) {
		if (getService().getSocketMapInternal().remove(SessionId, this)) {
			if (null != getService().getSocketMapInternal().putIfAbsent(newSessionId, this)) {
				getService().getSocketMapInternal().putIfAbsent(SessionId, this); // rollback
				throw new RuntimeException("duplicate sessionid " + this);
			}
			SessionId = newSessionId;
		}
		else {
			// 为了简化并发问题，只能加入Service以后的Socket的SessionId。
			throw new RuntimeException("Not Exist In Service " + this);
		}
	}

	@Override
	public String toString() {
		var sc =  this.getSocketChannel();
		if (null != sc) {
			try {
				return sc.getLocalAddress() + "-" + sc.getRemoteAddress();
			} catch (IOException e) {
				return "-" + e;
			}
		}
		return "-";
	}
}