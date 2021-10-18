package Zeze.Net;

import Zeze.Serialize.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public final class AsyncSocket implements SelectorHandle, Closeable {
	private static Logger logger = LogManager.getLogger(AsyncSocket.class);
	
	private ArrayList<java.nio.ByteBuffer> _outputBufferList = null;
	private int _outputBufferListCountSum = 0;
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
	public void setConnector(Connector value) {
		Connector = value;
	}

	private Acceptor Acceptor;
	public Acceptor getAcceptor() {
		return Acceptor;
	}
	public void setAcceptor(Acceptor value) {
		Acceptor = value;
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
	private void setSessionId(long value) {
		SessionId = value;
	}

	private SelectionKey selectionKey;
	private Selector selector;

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

	private static AtomicLong SessionIdGen = new AtomicLong();

	private BufferCodec inputCodecBuffer = new BufferCodec(); // 记录这个变量用来操作buffer
	private BufferCodec outputCodecBuffer = new BufferCodec(); // 记录这个变量用来操作buffer

	private Codec inputCodecChain;
	private Codec outputCodecChain;

	private String RemoteAddress;
	public String getRemoteAddress() {
		return RemoteAddress;
	}

	/** 
	 for server socket
	*/
	public AsyncSocket(Service service, InetSocketAddress localEP) {
		this.setService(service);

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
					Service.OnSocketAccept(new AsyncSocket(Service, sc));

			} catch (Throwable e) {
				if (null != sc)
					sc.close();
				// skip all error
			}
			return;
		}

		if (key.isConnectable()) {
			try {
				SocketChannel sc = (SocketChannel) key.channel();
				if (sc.finishConnect()) {
					doConnectSuccess(sc);
					selectionKey.interestOps(SelectionKey.OP_READ);
					return;
				}
				Service.OnSocketConnectError(this, null);
			}
			catch (RuntimeException e) {
				Service.OnSocketConnectError(this, e);
				close();
			}
		}

		if (key.isWritable()) {
			doWrite(key);
			return;
		}

		if (key.isReadable()) {
			ProcessReceive((SocketChannel) key.channel());
			return;
		}
	}

	@Override
	public void doException(SelectionKey key, Throwable e) throws Throwable {
		var This = (AsyncSocket)key.attachment();
		logger.error("doException {}", This.RemoteAddress, e);
	}

	/** 
	 use inner. create when accept;
	 
	 @param accepted
	*/
	private AsyncSocket(Service service, SocketChannel sc) throws SocketException, IOException {
		this.setService(service);


		// 据说连接接受以后设置无效，应该从 ServerSocket 继承
		Socket so = sc.socket();
		if (null != Service.getSocketOptions().getReceiveBuffer())
			so.setReceiveBufferSize(Service.getSocketOptions().getReceiveBuffer());
		if (null != Service.getSocketOptions().getSendBuffer())
			so.setSendBufferSize(Service.getSocketOptions().getSendBuffer());
		if (null != Service.getSocketOptions().getNoDelay())
			so.setTcpNoDelay(Service.getSocketOptions().getNoDelay());
		sc.configureBlocking(false);

		this.setSessionId(SessionIdGen.incrementAndGet());
		RemoteAddress = so.getRemoteSocketAddress().toString();

		selector = Selectors.getInstance().choice();
		selectionKey = selector.register(sc, SelectionKey.OP_READ, this);
	}

	/** 
	 for client socket. connect
	 
	 @param hostNameOrAddress
	 @param port
	*/

	public AsyncSocket(Service service, String hostNameOrAddress, int port) {
		this(service, hostNameOrAddress, port, null);
	}

	private void doConnectSuccess(SocketChannel sc) {
		if (Connector != null) {
			Connector.OnSocketConnected(this);
		}
		RemoteAddress = sc.socket().getInetAddress().getAddress().toString();
		Service.OnSocketConnected(this);
	}

	public AsyncSocket(Service service, String hostNameOrAddress, int port, Object userState) {
		this.setService(service);

		UserState = userState;
		this.setSessionId(SessionIdGen.incrementAndGet());

		SocketChannel sc = null;
		try {
			sc = SocketChannel.open();
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
				doConnectSuccess(sc);
				// 马上成功时，还没有注册到Selector中。
				selectionKey = selector.register(sc, SelectionKey.OP_READ, this);
			} else {
				selectionKey = selector.register(sc, SelectionKey.OP_CONNECT, this);
			}
		} catch (Throwable e) {
			if (null != sc)
				try {
					sc.close();
				} catch (Throwable e2) {
				}
			throw new RuntimeException(e);
		}
	}

	public void SetOutputSecurityCodec(byte[] key, boolean compress) {
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
			throw new RuntimeException(String.format("%1$s !IsSecurity", getService().getName()));
		}
	}

	public void SetInputSecurityCodec(byte[] key, boolean compress) {
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
	 
	 @param bytes
	 @param offset
	 @param length
	*/
	public boolean Send(byte[] bytes, int offset, int length) {
		Zeze.Serialize.ByteBuffer.VerifyArrayIndex(bytes, offset, length);

		synchronized (this) {
			if (null == selectionKey) {
				return false;
			}

			if (null == _outputBufferList) {
				_outputBufferList = new ArrayList<>();
			}

			if (_outputBufferListCountSum + length > Service.getSocketOptions().getOutputBufferMaxSize())
				return false;

			_outputBufferList.add(java.nio.ByteBuffer.wrap(bytes, offset, length));
			_outputBufferListCountSum += length;

			this.interestOps(0, SelectionKey.OP_WRITE); // try
			return true;
		}
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

	private void doWrite(SelectionKey key) {
		ArrayList<java.nio.ByteBuffer> current;
		synchronized (this) {
			current = _outputBufferList;
			_outputBufferList = null;
			_outputBufferListCountSum = 0;
		}

		if (null != outputCodecChain) {
			if (null != current) {
				// 压缩加密等 codec 链操作。
				if (null == _outputBufferListSending)
					_outputBufferListSending = new ArrayList<>();
				for (var buffer : current) {
					int length = buffer.remaining();
					outputCodecBuffer.getBuffer().EnsureWrite(length); // reserve
					outputCodecChain.update(buffer.array(), buffer.position(), length);
					outputCodecChain.flush();
	
					var codec = outputCodecBuffer.getBuffer();
					_outputBufferListSending.add(java.nio.ByteBuffer.wrap(codec.Bytes, codec.ReadIndex, codec.Size()));
					// 加入Sending后outputBufferCodec需要释放对byte[]的引用。
					codec.FreeInternalBuffer();
				}
			}
		}
		else if (null != current) {
			if (null == _outputBufferListSending)
				_outputBufferListSending = current;
			else
				_outputBufferListSending.addAll(current);
		}

		if (null == _outputBufferListSending)
			return;

		try {
			var sc = (SocketChannel)key.channel();
			var rc = sc.write(_outputBufferListSending.toArray(new java.nio.ByteBuffer[_outputBufferListSending.size()]));
			if (rc >= 0) {
				int i = 0;
				for ( ; i < _outputBufferListSending.size() && _outputBufferListSending.get(i).remaining() == 0; ++i) {
				}
				_outputBufferListSending.subList(0, i).clear();
				if (_outputBufferListSending.isEmpty()) {
					_outputBufferListSending = null; // free output buffer
					// remove write event
					this.interestOps(SelectionKey.OP_WRITE, 0);
				}
				else {
					// add write event
					this.interestOps(0, SelectionKey.OP_WRITE);
				}
				return;
			}
			// error
			close();
		} catch (IOException e) {
			close();
			throw new RuntimeException(e);
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

	public void SetSessionId(long newSessionId) {
		if (getService().getSocketMapInternal().remove(getSessionId(), this)) {
			if (null != getService().getSocketMapInternal().putIfAbsent(newSessionId, this)) {
				getService().getSocketMapInternal().putIfAbsent(getSessionId(), this); // rollback
				throw new RuntimeException(String.format("duplicate sessionid %1$s", this));
			}
			setSessionId(newSessionId);
		}
		else {
			// 为了简化并发问题，只能加入Service以后的Socket的SessionId。
			throw new RuntimeException(String.format("Not Exist In Service %1$s", this));
		}
	}
}