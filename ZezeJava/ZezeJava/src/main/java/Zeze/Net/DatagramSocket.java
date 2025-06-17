package Zeze.Net;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Random;
import Zeze.Util.ReplayAttackPolicy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DatagramSocket extends ReentrantLock implements SelectorHandle, Closeable {
	private static final @NotNull Logger logger = LogManager.getLogger(DatagramSocket.class);
	private final @NotNull Service service;
	private final @NotNull DatagramChannel datagramChannel;
	private final @NotNull Selector selector;
	private @Nullable SelectionKey selectionKey;
	private final LongConcurrentHashMap<DatagramSession> tokens = new LongConcurrentHashMap<>(); // key: DatagramSession的tokenId

	DatagramSocket(@NotNull Service service, @NotNull InetSocketAddress local) throws IOException {
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

	public @NotNull Service getService() {
		return service;
	}

	public InetSocketAddress getLocal() { // 已经close的情况下返回null
		try {
			if (datagramChannel.isOpen())
				return (InetSocketAddress)datagramChannel.getLocalAddress();
		} catch (IOException ignored) {
		}
		return null;
	}

	public void sendTo(@NotNull SocketAddress peer, @NotNull java.nio.ByteBuffer bb) throws IOException {
		datagramChannel.send(bb, peer);
	}

	public void sendTo(@NotNull SocketAddress peer, byte @NotNull [] packet, int offset, int size) throws IOException {
		datagramChannel.send(java.nio.ByteBuffer.wrap(packet, offset, size), peer);
	}

	public void sendTo(@NotNull SocketAddress peer, @NotNull Serializable p) throws IOException {
		int preAllocSize = p.preAllocSize();
		var bb = ByteBuffer.Allocate(Math.min(preAllocSize, 65536));
		p.encode(bb);
		int size = bb.WriteIndex;
		if (size > preAllocSize)
			p.preAllocSize(size);
		datagramChannel.send(java.nio.ByteBuffer.wrap(bb.Bytes, 0, size), peer);
	}

	public @Nullable DatagramSession createSession(@NotNull InetSocketAddress remote, long tokenId,
												   byte @Nullable [] securityKey, @NotNull ReplayAttackPolicy policy) {
		var session = new DatagramSession(this, remote, tokenId, securityKey, policy);
		if (null == tokens.putIfAbsent(tokenId, session))
			return session;
		return null;
	}

	public @NotNull DatagramSession createSessionServer(@NotNull InetSocketAddress remote,
														byte @Nullable [] securityKey,
														@NotNull ReplayAttackPolicy policy) {
		while (true) {
			var tokenId = Random.getInstance().nextLong();
			var session = new DatagramSession(this, remote, tokenId, securityKey, policy);
			if (null == tokens.putIfAbsent(tokenId, session))
				return session;
		}
	}

	public void removeSession(@NotNull DatagramSession session) {
		tokens.remove(session.getTokenId());
	}

	public boolean containsSession(@NotNull DatagramSession session) {
		return tokens.containsKey(session.getTokenId());
	}

	@Override
	public void doHandle(@NotNull SelectionKey key) throws Exception {
		if (key.isReadable()) {
			var buffer = selector.getReadBuffer(); // 线程共享的buffer,只能本方法内临时使用
			buffer.clear();
			var source = datagramChannel.receive(buffer);
			if (source != null) {
				var bb = ByteBuffer.Wrap(buffer.array(), buffer.position());
				var ssid = ByteBuffer.ToLong(bb.Bytes, 0);
				var ss = tokens.get(ssid);
				if (null != ss)
					ss.onProcessDatagram((InetSocketAddress)source, bb);
			}
		} else
			throw new IllegalStateException();
	}

	@Override
	public void doException(@NotNull SelectionKey key, @NotNull Throwable e) {
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
	}

	@Override
	public @NotNull String toString() {
		var localAddress = getLocal();
		return (localAddress != null ? localAddress.toString() : "closed"); // 如果有localAddress则表示还没close
	}
}
