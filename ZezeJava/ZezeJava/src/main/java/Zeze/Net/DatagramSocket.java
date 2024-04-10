package Zeze.Net;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.ReplayAttackPolicy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatagramSocket extends ReentrantLock implements SelectorHandle, Closeable {
	private static final Logger logger = LogManager.getLogger(DatagramSocket.class);
	private static final AtomicLong sessionIdGen = new AtomicLong(); // 只用于分配DatagramSocket的sessionId

	private final DatagramChannel datagramChannel;
	private final Selector selector;
	private SelectionKey selectionKey;
	private final DatagramService service;
	private final long sessionId = sessionIdGen.incrementAndGet(); // 注意这是DatagramSocket的sessionId, 跟DatagramSession的sessionId的意义不同
	private final LongConcurrentHashMap<DatagramSession> sessions = new LongConcurrentHashMap<>(); // key: DatagramSession的sessionId

	DatagramSocket(DatagramService service, InetSocketAddress local) throws IOException {
		this.service = service;
		datagramChannel = DatagramChannel.open();
		try {
			datagramChannel.configureBlocking(false);
			var so = datagramChannel.socket();
			Integer recvBufSize = service.getSocketOptions().getReceiveBuffer();
			if (recvBufSize != null)
				so.setReceiveBufferSize(recvBufSize);
			Integer sendBufSize = service.getSocketOptions().getSendBuffer();
			if (sendBufSize != null)
				so.setSendBufferSize(sendBufSize);
			datagramChannel.bind(local);
			selector = service.getSelectors().choice();
			selectionKey = selector.register(datagramChannel, 0, this); // 先获取key,因为有小概率出现事件处理比赋值selectionKey和addSocket更先执行
			service.addSocket(this);
			selectionKey.interestOps(SelectionKey.OP_READ);
			selector.wakeup();
		} catch (Throwable e) { // rethrow
			try {
				if (selectionKey != null)
					close();
				else
					datagramChannel.close();
			} catch (Exception ex) {
				logger.error("close channel({}) exception:", this, ex);
			}
			throw e;
		}
	}

	public DatagramService getService() {
		return service;
	}

	public long getSessionId() {
		return sessionId;
	}

	public InetSocketAddress getLocal() { // 已经close的情况下返回null
		try {
			return (InetSocketAddress)datagramChannel.getLocalAddress();
		} catch (IOException ignored) {
			return null;
		}
	}

	public void sendTo(SocketAddress peer, java.nio.ByteBuffer bb) throws IOException {
		datagramChannel.send(bb, peer);
	}

	public void sendTo(SocketAddress peer, byte[] packet, int offset, int size) throws IOException {
		datagramChannel.send(java.nio.ByteBuffer.wrap(packet, offset, size), peer);
	}

	public void sendTo(SocketAddress peer, Serializable p) throws IOException {
		int preAllocSize = p.preAllocSize();
		var bb = ByteBuffer.Allocate(Math.min(preAllocSize, 65536));
		p.encode(bb);
		int size = bb.WriteIndex;
		if (size > preAllocSize)
			p.preAllocSize(size);
		datagramChannel.send(java.nio.ByteBuffer.wrap(bb.Bytes, 0, size), peer);
	}

	public DatagramSession createSession(InetSocketAddress remote, long sessionId, byte[] securityKey,
										 ReplayAttackPolicy policy) {
		var session = new DatagramSession(this, remote, sessionId, securityKey, policy);
		if (null == sessions.putIfAbsent(sessionId, session))
			return session;
		return null;
	}

	public void removeSession(DatagramSession session) {
		sessions.remove(session.getSessionId());
	}

	@Override
	public void doHandle(SelectionKey key) throws Exception {
		if (key.isReadable()) {
			var buffer = selector.getReadBuffer(); // 线程共享的buffer,只能本方法内临时使用
			buffer.clear();
			var source = datagramChannel.receive(buffer);
			if (source != null) {
				var bb = ByteBuffer.Wrap(buffer.array(), buffer.position());
				var ssid = ByteBuffer.ToLong(bb.Bytes, 0);
				var ss = sessions.get(ssid);
				if (null != ss)
					ss.onProcessDatagram((InetSocketAddress)source, bb);
			}
		} else
			throw new IllegalStateException();
	}

	@Override
	public void doException(SelectionKey key, Throwable e) {
		service.onSocketException(this, e);
	}

	@Override
	public void close() {
		SelectionKey key;
		lock();
		try {
			if (selectionKey == null)
				return;
			key = selectionKey;
			selectionKey = null;
		} finally {
			unlock();
		}
		try {
			service.onSocketClose(this);
		} catch (Exception e) {
			logger.error("onSocketClose({}) exception:", this, e);
		}
		try {
			key.channel().close();
		} catch (IOException e) {
			logger.error("close channel({}) exception:", this, e);
		}
	}

	@Override
	public String toString() {
		var localAddress = getLocal();
		return "[" + sessionId + ']' + (localAddress != null ? localAddress : ""); // 如果有localAddress则表示还没close
	}
}
