package Zeze.Net;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.DispatchMode;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.LongHashSet;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 简单可靠udp实现。
 * 多对多模式：绑定一个udp，可以发送和接收多个其他机器的数据。没有使用Udp.connect。
 * 1 数据重发。
 * 2 处理乱序。
 * 3 没有流量控制。
 */
public class ReliableUdp extends ReentrantLock implements SelectorHandle, Closeable {
	private static final Logger logger = LogManager.getLogger(AsyncSocket.class);

	public static final int TypePacket = 0;
	public static final int TypeControl = 1;

	private final DatagramChannel datagramChannel;
	private SelectionKey selectionKey;
	private final Selector selector;
	private final InetSocketAddress local;
	private final ConcurrentHashMap<SocketAddress, Session> sessions = new ConcurrentHashMap<>();
	private final ReliableUdpHandle defaultHandle;
	private int MaxPacketLength = 2048;

	// 应用应该需要这个，特别是Server端，免得外面又需要建立一个Map来管理。
	// 【注意】应用直接删除这个Map时需要注意是否会出现问题。
	public ConcurrentHashMap<SocketAddress, Session> getSessions() {
		return sessions;
	}

	public int getMaxPacketLength() {
		return MaxPacketLength;
	}

	public void setMaxPacketLength(int max) {
		MaxPacketLength = max;
	}

	public final Selector getSelector() {
		return selector;
	}

	public final InetSocketAddress getLocalInetAddress() {
		return local;
	}

	// bind to (address, port)
	public ReliableUdp(String address, int port, ReliableUdpHandle defaultHandle) {
		try {
			this.defaultHandle = defaultHandle;
			local = new InetSocketAddress(InetAddress.getByName(address), port);
			datagramChannel = DatagramChannel.open();
			datagramChannel.configureBlocking(false);
			datagramChannel.bind(local);
			selector = Selectors.getInstance().choice();
			selector.register(datagramChannel, SelectionKey.OP_READ, this);
		} catch (IOException e) {
			Task.forceThrow(e);
			throw new AssertionError(); // never run here
		}
	}

	// 打开一个连接用来发送数据。
	public Session open(String peer, int port, ReliableUdpHandle handle) {
		try {
			var ep = new InetSocketAddress(InetAddress.getByName(peer), port);
			return new Session(ep, handle);
		} catch (UnknownHostException e) {
			Task.forceThrow(e);
			return null; // never run here
		}
	}

	public static class Packet implements Serializable {
		public long serialId;
		public byte[] bytes;
		public int offset;
		public int length;

		// 发送端用来记录这个包的重发 TimerTask。每个包一个Timer很浪费，先这样。
		public transient Future<?> resendTimerTask;

		public Packet() {
		}

		public Packet(long serialId, byte[] bytes, int offset, int length) {
			this.serialId = serialId;
			this.bytes = bytes;
			this.offset = offset;
			this.length = length;
		}

		@Override
		public void decode(IByteBuffer bb) {
			// TypePacket 外面解析。
			serialId = bb.ReadLong();
			bytes = bb.ReadBytes();
			offset = 0;
			length = bytes.length;
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteInt(TypePacket);
			bb.WriteLong(serialId);
			bb.WriteBytes(bytes, offset, length);
		}
	}

	public static class Control implements Serializable {
		public static final int Ack = 1;
		public static final int Resend = 2;
		public static final int NoSession = 3;

		public int command;
		public final LongHashSet serialIds = new LongHashSet();

		@Override
		public void decode(IByteBuffer bb) {
			// TypePacket 外面解析。
			command = bb.ReadInt();
			for (int count = bb.ReadInt(); count > 0; --count) {
				serialIds.add(bb.ReadLong());
			}
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteInt(TypeControl);
			bb.WriteInt(command);
			bb.WriteInt(serialIds.size());
			serialIds.foreach(bb::WriteLong);
		}
	}

	public class Session {
		private final ReliableUdpHandle handle;
		private final SocketAddress peer;
		private final LongConcurrentHashMap<Packet> sendWindow = new LongConcurrentHashMap<>();
		private final AtomicLong serialIdGenerator = new AtomicLong(1);
		private final LongConcurrentHashMap<Packet> recvWindow = new LongConcurrentHashMap<>();

		private long lastDispatchedSerialId;
		private long maxRecvPacketSerialId;

		public Session(SocketAddress peer, ReliableUdpHandle handle) {
			this.handle = handle;
			this.peer = peer;
			sessions.put(peer, this);
		}

		public boolean send(byte[] bytes, int offset, int length) {
			if (length > MaxPacketLength)
				throw new IllegalArgumentException("length > MaxPacketLength: " + MaxPacketLength);

			var serialId = serialIdGenerator.getAndIncrement();
			var packet = new Packet(serialId, bytes, offset, length);
			sendWindow.put(packet.serialId, packet);

			// start auto resend timer.
			packet.resendTimerTask = Task.scheduleUnsafe(3000, 3000, () -> sendTo(peer, packet));
			sendTo(peer, packet);
			return true;
		}
	}

	private void sendTo(SocketAddress peer, Serializable p) {
		try {
			var bb = ByteBuffer.Allocate(512);
			p.encode(bb);
			datagramChannel.send(java.nio.ByteBuffer.wrap(bb.Bytes, bb.ReadIndex, bb.WriteIndex), peer);
		} catch (IOException e) {
			Task.forceThrow(e);
		}
	}

	// 重载这个决定是否动态创建Session。
	// 一般Server模式需要接收任何地方来的包，此时需要动态创建。
	// 一般Client模式仅接收来自一个地方的包，此时可以限制一下，重载这个方法并且返回null即可。
	public Session dynamicCreateSession(SocketAddress source) {
		return new Session(source, defaultHandle);
	}

	// 派发收到的包，决定执行方式。默认多线程执行。
	// 如果执行的操作没有阻塞，可以直接在网络线程中执行。
	// 重载当然也可以实现其他模式，加到自己的队列什么的。
	public void dispatch(Session session, Packet packet) {
		Task.executeUnsafe(() -> session.handle.handle(session, packet), "ReliableUdp.dispatch", DispatchMode.Normal);
		// session.Handle.handle(session, packet); // 直接在网络线程中执行。
	}

	private void tryDispatchRecvWindow(Session session) {
		for (long serialId = session.lastDispatchedSerialId + 1;
			 serialId <= session.maxRecvPacketSerialId;
			 ++serialId) {
			var p = session.recvWindow.get(serialId);
			if (p == null)
				break; // 仍然有乱序的包没有到达，等待。
			dispatch(session, p);
			session.lastDispatchedSerialId = serialId;
			session.recvWindow.remove(serialId);
		}
	}

	private void processPacket(SocketAddress source, ByteBuffer bb) {
		var packet = new Packet();
		packet.decode(bb);

		// 只要收到包就发送ack，不需要判断其他条件，这样让发送者能更好的的清除SendWindow。
		var ack = new Control();
		ack.command = Control.Ack;
		ack.serialIds.add(packet.serialId);
		sendTo(source, ack);

		var session = sessions.get(source);
		if (session == null)
			session = dynamicCreateSession(source);

		if (session != null) {
			if (packet.serialId <= session.lastDispatchedSerialId) {
				return; // skip duplicate packet.
			}

			if (packet.serialId > session.maxRecvPacketSerialId)
				session.maxRecvPacketSerialId = packet.serialId;

			if (packet.serialId == session.lastDispatchedSerialId + 1) {
				// 顺序到达，马上派发。
				dispatch(session, packet);
				session.lastDispatchedSerialId = packet.serialId;
				session.recvWindow.remove(packet.serialId);

				// 派发可能收到的原来乱序的包。
				tryDispatchRecvWindow(session);
				return;
			}

			// 发现乱序。

			// 记住当前包。
			session.recvWindow.put(packet.serialId, packet);

			// 请求重发。
			var resend = new Control();
			resend.command = Control.Resend;
			for (var serialId = session.lastDispatchedSerialId + 1; serialId < session.maxRecvPacketSerialId; ++serialId) {
				resend.serialIds.add(serialId);
			}
			sendTo(source, resend);
			return;
		}

		// report error to source;
		var noss = new Control();
		noss.command = Control.NoSession;
		sendTo(source, noss);
	}

	private void processPacketControl(SocketAddress source, ByteBuffer bb) {
		var control = new Control();
		control.decode(bb);

		var session = sessions.get(source);

		// 需要确定控制协议是否需要动态创建Session。
		if (session == null)
			session = dynamicCreateSession(source);

		// 没有会话，此时忽略Control。控制协议不允许再次报告错误，避免形成两端互相发送错误的死循环。
		if (session == null)
			return;

		switch (control.command) {
		case Control.Ack:
			for (var it = control.serialIds.iterator(); it.moveToNext(); ) {
				var p = session.sendWindow.remove(it.value());
				p.resendTimerTask.cancel(false);
			}
			break;

		case Control.Resend:
			for (var it = control.serialIds.iterator(); it.moveToNext(); ) {
				var packet = session.sendWindow.get(it.value());
				if (null != packet)
					sendTo(session.peer, packet);
				// else skip 重发请求的包已经不在Window中，此时可能是重复的Resend请求。
			}
			break;

		case Control.NoSession:
			break;
		}
	}

	@Override
	public void doHandle(SelectionKey key) throws Exception {
		if (key.isReadable()) {
			var buffer = java.nio.ByteBuffer.allocate(MaxPacketLength);
			var source = datagramChannel.receive(buffer);
			if (source != null) {
				var bb = ByteBuffer.Wrap(buffer.array(), buffer.position(), buffer.limit());
				var type = bb.ReadInt();
				switch (type) {
				case TypePacket:
					processPacket(source, bb);
					break;
				case TypeControl:
					processPacketControl(source, bb);
					break;
				}
			}
			return;
		}

		throw new IllegalStateException();
	}

	@Override
	public void doException(SelectionKey key, Throwable e) {
		logger.error("doException", e);
	}

	@Override
	public void close() {
		lock();
		try {
			if (selectionKey == null)
				return;
			try {
				selectionKey.channel().close();
			} catch (IOException skip) {
				logger.error("", skip);
			}
			selectionKey = null;
		} finally {
			unlock();
		}
	}
}
