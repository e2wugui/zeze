package Zeze.Net;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Random;
import Zeze.Util.ReplayAttackPolicy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * UDP socket：绑定本地端口，按 tokenId 将数据报分发到会话。
 */
public class DatagramSocket extends ReentrantLock implements SelectorHandle, Closeable {
	private static final Logger logger = LogManager.getLogger(DatagramSocket.class);
	private final Service service;
	private final DatagramChannel datagramChannel;
	private final Selector selector;
	private @Nullable SelectionKey selectionKey;
	private final LongConcurrentHashMap<DatagramSession> tokens = new LongConcurrentHashMap<>(); // key: DatagramSession的tokenId

	DatagramSocket(Service service, InetSocketAddress local) throws IOException {
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

	public Service getService() {
		return service;
	}

	public @Nullable InetSocketAddress getLocal() { // 已经close的情况下返回null
		try {
			if (datagramChannel.isOpen())
				return (InetSocketAddress)datagramChannel.getLocalAddress();
		} catch (IOException ignored) {
		}
		return null;
	}

	/** 非阻塞发送：返回 true 表示内核已接受数据报，false 表示发送缓冲满未发出。 */
	public boolean sendTo(SocketAddress peer, java.nio.ByteBuffer bb) throws IOException {
		return datagramChannel.send(bb, peer) > 0;
	}

	public boolean sendTo(SocketAddress peer, byte [] packet, int offset, int size) throws IOException {
		return sendTo(peer, java.nio.ByteBuffer.wrap(packet, offset, size));
	}

	public boolean sendTo(SocketAddress peer, Serializable p) throws IOException {
		int preAllocSize = p.preAllocSize();
		var bb = ByteBuffer.Allocate(Math.min(preAllocSize, 65536));
		p.encode(bb);
		int size = bb.WriteIndex;
		if (size > preAllocSize)
			p.preAllocSize(size);
		return sendTo(peer, java.nio.ByteBuffer.wrap(bb.Bytes, 0, size));
	}

	// 须在锁内调用：与 close() 互斥，已关闭时拒绝新建会话。
	private void checkNotClosed() {
		if (selectionKey == null)
			throw new IllegalStateException("DatagramSocket closed: " + this);
	}

	public @Nullable DatagramSession createSession(InetSocketAddress remote, long tokenId,
												   byte @Nullable [] securityKey, ReplayAttackPolicy policy) {
		var session = new DatagramSession(this, remote, tokenId, securityKey, policy);
		lock();
		try {
			checkNotClosed();
			if (null == tokens.putIfAbsent(tokenId, session))
				return session;
		} finally {
			unlock();
		}
		return null;
	}

	public DatagramSession createSessionServer(InetSocketAddress remote,
														byte @Nullable [] securityKey,
														ReplayAttackPolicy policy) {
		while (true) {
			var tokenId = Random.getInstance().nextLong();
			var session = new DatagramSession(this, remote, tokenId, securityKey, policy);
			lock();
			try {
				checkNotClosed();
				if (null == tokens.putIfAbsent(tokenId, session))
					return session;
			} finally {
				unlock();
			}
		}
	}

	public void removeSession(DatagramSession session) {
		tokens.remove(session.getTokenId(), session);
	}

	public boolean containsSession(DatagramSession session) {
		return tokens.get(session.getTokenId()) == session;
	}

	@Override
	public void doHandle(SelectionKey key) throws Exception {
		if (key.isReadable()) {
			var buffer = selector.getReadBuffer(); // 线程共享的buffer,只能本方法内临时使用
			buffer.clear();
			var source = datagramChannel.receive(buffer);
			if (source != null) {
				var bb = ByteBuffer.Wrap(buffer.array(), buffer.position());
				if (bb.WriteIndex < 8)
					return; // 不足 tokenId 长度直接丢弃，避免读到共享缓冲残留字节
				var ssid = ByteBuffer.ToLong(bb.Bytes, 0);
				var ss = tokens.get(ssid);
				if (null != ss)
					ss.onProcessDatagram((InetSocketAddress)source, bb);
			}
		} else
			throw new IllegalStateException();
	}

	@Override
	public void doException(SelectionKey key, Throwable e) {
		close();
		logger.error("", e);
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
			key.channel().close();
		} catch (IOException e) {
			logger.error("close channel({}) exception:", this, e);
		}
		// 先快照再逐个关闭，避免回调修改 tokens 集合。
		var sessions = new ArrayList<DatagramSession>();
		for (DatagramSession token : tokens)
			sessions.add(token);
		for (var session : sessions)
			session.close(null);
	}

	@Override
	public String toString() {
		var localAddress = getLocal();
		return (localAddress != null ? localAddress.toString() : "closed"); // 如果有localAddress则表示还没close
	}
}
