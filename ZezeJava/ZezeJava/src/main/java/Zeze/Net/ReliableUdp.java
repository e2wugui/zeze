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
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.LongHashSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 简单可靠udp实现。
 * 多对多模式：绑定一个udp，可以发送和接收多个其他机器的数据。没有使用Udp.connect。
 * 1 数据重发。
 * 2 处理乱序。
 * 3 没有流量控制。
 */
public class ReliableUdp implements SelectorHandle, Closeable {
	private static final Logger logger = LogManager.getLogger(AsyncSocket.class);

	private final DatagramChannel datagramChannel;
	private SelectionKey selectionKey;
	private final Selector selector;
	private final InetSocketAddress local;
	private final ConcurrentHashMap<SocketAddress, Session> Sessions = new ConcurrentHashMap<>();
	private final ReliableUdpHandle DefaultHandle;

	// 应用应该需要这个，特别是Server端，免得外面又需要建立一个Map来管理。
	// 【注意】应用直接删除这个Map时需要注意是否会出现问题。
	public ConcurrentHashMap<SocketAddress, Session> getSessions() {
		return Sessions;
	}

	private int MaxPacketLength = 2048;

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
			DefaultHandle = defaultHandle;
			local = new InetSocketAddress(InetAddress.getByName(address), port);
			datagramChannel = DatagramChannel.open();
			datagramChannel.configureBlocking(false);
			datagramChannel.bind(local);
			selector = Selectors.getInstance().choice();
			selector.register(datagramChannel, SelectionKey.OP_READ, this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// 打开一个连接用来发送数据。
	public Session open(String peer, int port, ReliableUdpHandle handle) {
		try {
			var ep = new InetSocketAddress(InetAddress.getByName(peer), port);
			return new Session(ep, handle);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}

	public final static int TypePacket = 0;
	public final static int TypeControl = 1;

	public static class Packet implements Serializable {
		public long SerialId;
		public byte[] Bytes;
		public int Offset;
		public int Length;

		// 发送端用来记录这个包的重发 TimerTask。TODO 每个包一个Timer很浪费，先这样。
		public Future<?> ResendTimerTask;

		public Packet() {

		}

		public Packet(long serialId, byte[] bytes, int offset, int length) {
			SerialId = serialId;
			Bytes = bytes;
			Offset = offset;
			Length = length;
		}

		@Override
		public void Decode(ByteBuffer bb) {
			// TypePacket 外面解析。
			SerialId = bb.ReadLong();
			Bytes = bb.ReadBytes();
			Offset = 0;
			Length = Bytes.length;
		}

		@Override
		public void Encode(ByteBuffer bb) {
			bb.WriteInt(TypePacket);
			bb.WriteLong(SerialId);
			bb.WriteBytes(Bytes, Offset, Length);
		}
	}

	public static class Control implements Serializable {
		public final static int Ack = 1;
		public final static int Resend = 2;
		public final static int NoSession = 3;

		public int Command;
		public final LongHashSet SerialIds = new LongHashSet();

		@Override
		public void Decode(ByteBuffer bb) {
			// TypePacket 外面解析。
			Command = bb.ReadInt();
			for (int count = bb.ReadInt(); count > 0; --count) {
				SerialIds.add(bb.ReadLong());
			}
		}

		@Override
		public void Encode(ByteBuffer bb) {
			bb.WriteInt(TypeControl);
			bb.WriteInt(Command);
			bb.WriteInt(SerialIds.size());
			SerialIds.foreach(bb::WriteLong);
		}
	}

	public class Session {
		private final ReliableUdpHandle Handle;
		private final SocketAddress Peer;
		private final LongConcurrentHashMap<Packet> SendWindow = new LongConcurrentHashMap<>();
		private final AtomicLong SerialIdGenerator = new AtomicLong(1);
		private final LongConcurrentHashMap<Packet> RecvWindow = new LongConcurrentHashMap<>();

		private long LastDispatchedSerialId;
		private long MaxRecvPacketSerialId;

		public Session(SocketAddress peer, ReliableUdpHandle handle) {
			Handle = handle;
			Peer = peer;
			Sessions.put(peer, this);
		}

		public boolean send(byte[] bytes, int offset, int length) {
			if (length > MaxPacketLength)
				throw new IllegalArgumentException("length > MaxPacketLength: " + MaxPacketLength);

			var serialId = SerialIdGenerator.getAndIncrement();
			var packet = new Packet(serialId, bytes, offset, length);
			SendWindow.put(packet.SerialId, packet);

			// start auto resend timer.
			packet.ResendTimerTask = Zeze.Util.Task.schedule(3000, 3000, () -> sendTo(Peer, packet));
			sendTo(Peer, packet);
			return true;
		}
	}

	private void sendTo(SocketAddress peer, Serializable p) {
		try {
			var bb = ByteBuffer.Allocate(512);
			p.Encode(bb);
			datagramChannel.send(java.nio.ByteBuffer.wrap(bb.Bytes, bb.ReadIndex, bb.WriteIndex), peer);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// 重载这个决定是否动态创建Session。
	// 一般Server模式需要接收任何地方来的包，此时需要动态创建。
	// 一般Client模式仅接收来自一个地方的包，此时可以限制一下，重载这个方法并且返回null即可。
	public Session DynamicCreateSession(SocketAddress source) {
		return new Session(source, DefaultHandle);
	}

	// 派发收到的包，决定执行方式。默认多线程执行。
	// 如果执行的操作没有阻塞，可以直接在网络线程中执行。
	// 重载当然也可以实现其他模式，加到自己的队列什么的。
	public void Dispatch(Session session, Packet packet) {
		Zeze.Util.Task.run(() -> session.Handle.handle(session, packet), "ReliableUdp.DefaultDispatch");
		// session.Handle.handle(session, packet); // 直接在网络线程中执行。
	}

	private void tryDispatchRecvWindow(Session session) {
		for (long serialId = session.LastDispatchedSerialId + 1;
			 serialId <= session.MaxRecvPacketSerialId;
			 ++serialId) {
			var p = session.RecvWindow.get(serialId);
			if (null == p)
				break; // 仍然有乱序的包没有到达，等待。
			Dispatch(session, p);
			session.LastDispatchedSerialId = serialId;
			session.RecvWindow.remove(serialId);
		}
	}

	private void ProcessPacket(SocketAddress source, ByteBuffer bb) {
		var packet = new Packet();
		packet.Decode(bb);

		// 只要收到包就发送ack，不需要判断其他条件，这样让发送者能更好的的清除SendWindow。
		var ack = new Control();
		ack.Command = Control.Ack;
		ack.SerialIds.add(packet.SerialId);
		sendTo(source, ack);

		var session = Sessions.get(source);
		if (null == session)
			session = DynamicCreateSession(source);

		if (null != session) {
			if (packet.SerialId <= session.LastDispatchedSerialId) {
				return; // skip duplicate packet.
			}

			if (packet.SerialId > session.MaxRecvPacketSerialId)
				session.MaxRecvPacketSerialId = packet.SerialId;

			if (packet.SerialId == session.LastDispatchedSerialId + 1) {
				// 顺序到达，马上派发。
				Dispatch(session, packet);
				session.LastDispatchedSerialId = packet.SerialId;
				session.RecvWindow.remove(packet.SerialId);

				// 派发可能收到的原来乱序的包。
				tryDispatchRecvWindow(session);
				return;
			}

			// 发现乱序。

			// 记住当前包。
			session.RecvWindow.put(packet.SerialId, packet);

			// 请求重发。
			// TODO 这里应该需要延迟一下，因为即使发生了乱序，可能在短时间内，包会继续到达。
			var resend = new Control();
			resend.Command = Control.Resend;
			for (var serialId = session.LastDispatchedSerialId + 1; serialId < session.MaxRecvPacketSerialId; ++serialId) {
				resend.SerialIds.add(serialId);
			}
			sendTo(source, resend);
			return;
		}

		// report error to source;
		var noss = new Control();
		noss.Command = Control.NoSession;
		sendTo(source, noss);
	}

	private void ProcessPacketControl(SocketAddress source, ByteBuffer bb) {
		var control = new Control();
		control.Decode(bb);

		var session = Sessions.get(source);

		// TODO 需要确定控制协议是否需要动态创建Session。
		if (null == session)
			session = DynamicCreateSession(source);

		// 没有会话，此时忽略Control。控制协议不允许再次报告错误，避免形成两端互相发送错误的死循环。
		if (null == session)
			return;

		switch (control.Command) {
		case Control.Ack:
			for (var it = control.SerialIds.iterator(); it.moveToNext(); ) {
				var p = session.SendWindow.remove(it.value());
				p.ResendTimerTask.cancel(false);
			}
			break;

		case Control.Resend:
			for (var it = control.SerialIds.iterator(); it.moveToNext(); ) {
				var packet = session.SendWindow.get(it.value());
				if (null != packet)
					sendTo(session.Peer, packet);
				// else skip 重发请求的包已经不在Window中，此时可能是重复的Resend请求。
			}
			break;

		case Control.NoSession:
			break;
		}
	}

	@Override
	public void doHandle(SelectionKey key) throws Throwable {
		if (key.isReadable()) {
			var buffer = java.nio.ByteBuffer.allocate(MaxPacketLength);
			var source = datagramChannel.receive(buffer);
			if (null != source) {
				var bb = ByteBuffer.Wrap(buffer.array(), buffer.position(), buffer.limit());
				var type = bb.ReadInt();
				switch (type) {
				case TypePacket:
					ProcessPacket(source, bb);
					break;
				case TypeControl:
					ProcessPacketControl(source, bb);
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
	public synchronized void close() {
		if (selectionKey == null)
			return;
		try {
			selectionKey.channel().close();
		} catch (IOException skip) {
			logger.error("", skip);
		}
		selectionKey = null;
	}
}
