package Zeze.Net;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatagramSocket implements SelectorHandle, Closeable {
	private static final Logger logger = LogManager.getLogger(DatagramSocket.class);
	private final DatagramChannel datagramChannel;
	private final Selector selector;
	private SelectionKey selectionKey;
	private final InetSocketAddress local;
	private final DatagramService service;
	private final static AtomicLong sessionIdGen = new AtomicLong();
	private final long sessionId = sessionIdGen.incrementAndGet();
	private final ConcurrentHashMap<Long, DatagramSession> sessions = new ConcurrentHashMap<>();

	public DatagramService getService() {
		return service;
	}

	DatagramSocket(DatagramService service, InetSocketAddress local) throws IOException {
		this.service = service;
		datagramChannel = DatagramChannel.open();
		datagramChannel.configureBlocking(false);
		datagramChannel.bind(local);
		this.local = (InetSocketAddress)datagramChannel.getLocalAddress();
		selector = service.getSelectors().choice();
		selectionKey = selector.register(datagramChannel, SelectionKey.OP_READ, this);
		service.addSocket(this); // ??? 最后加入 ???
	}

	public long getSessionId() {
		return sessionId;
	}

	public InetSocketAddress getLocal() {
		return local;
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

	public DatagramSession createSession(InetSocketAddress remote,
										 long sessionId, byte[] securityKey,
										 DatagramSession.ReplayAttackPolicy replayAttackPolicy) {
		var session = new DatagramSession(this, remote, sessionId, securityKey, replayAttackPolicy);
		if (null == sessions.putIfAbsent(sessionId, session))
			return session;
		return null;
	}

	@Override
	public void doHandle(SelectionKey key) throws Exception {
		if (key.isReadable()) {
			var buffer = selector.getReadBuffer(); // 线程共享的buffer,只能本方法内临时使用
			buffer.clear();
			var source = datagramChannel.receive(buffer);
			if (source != null) {
				var bb = ByteBuffer.Wrap(buffer.array(), 0, buffer.position());
				var ssid = bb.ReadLong8();
				var ss = sessions.get(ssid);
				if (null != ss)
					ss.onProcessDatagram((InetSocketAddress)source, bb);
			}
			return;
		}

		throw new IllegalStateException();
	}

	@Override
	public void doException(SelectionKey key, Throwable e) throws Exception {
		service.onSocketException(this, e);
		close();
	}

	@Override
	public void close() throws IOException {
		SelectionKey key;
		synchronized (this) {
			if (selectionKey == null)
				return;
			key = selectionKey;
			selectionKey = null;
		}
		try {
			service.onSocketClose(this);
		} catch (Throwable e) {
			logger.error("", e);
		}
		try {
			key.channel().close();
		} catch (IOException skip) {
			logger.error("", skip);
		}
	}
}
