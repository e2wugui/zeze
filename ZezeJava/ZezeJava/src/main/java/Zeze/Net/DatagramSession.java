package Zeze.Net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Serialize.ByteBuffer;

public class DatagramSession {
	private final DatagramSocket socket;
	private final InetSocketAddress remote;
	private final long sessionId;
	private AtomicLong serialId = new AtomicLong();

	public DatagramSocket getSocket() {
		return socket;
	}

	public InetSocketAddress getRemote() {
		return remote;
	}

	public InetSocketAddress getLocal() {
		return socket.getLocal();
	}

	public long getSessionId() {
		return sessionId;
	}

	public DatagramSession(DatagramSocket socket, InetSocketAddress remote, long sessionId) {
		this.socket = socket;
		this.remote = remote;
		this.sessionId = sessionId;
	}

	public void send(byte[] packet, int offset, int size) throws IOException {
		send(remote, packet, offset, size);
	}

	public void send(InetSocketAddress remote, byte[] packet, int offset, int size) throws IOException {
		var bb = ByteBuffer.Allocate();
		bb.WriteLong(sessionId);

		// 下面这块数据加密
		bb.WriteLong(serialId.incrementAndGet());
		bb.WriteBytes(packet, offset, size);

		// serialId 和 sendTo 之间有窗口，可能大的 serialId 后发送。这是udp，不解决这个问题了。
		socket.sendTo(remote, bb.Bytes, bb.ReadIndex, bb.size());
	}

	public void send(Protocol<?> p) throws IOException {
		send(remote, p);
	}

	public void send(InetSocketAddress remote, Protocol<?> p) throws IOException {
		var bb = ByteBuffer.Allocate();
		bb.WriteLong(sessionId);

		// 下面这块数据加密
		bb.WriteLong(serialId.incrementAndGet());
		bb.WriteByteBuffer(p.encode());

		// serialId 和 sendTo 之间有窗口，可能大的 serialId 后发送。这是udp，不解决这个问题了。
		socket.sendTo(remote, bb.Bytes, bb.ReadIndex, bb.size());
	}

	public void onProcessDatagram(InetSocketAddress remote, ByteBuffer bb) throws Exception {
		var serialId = bb.ReadLong();
		// todo 其他处理。
		socket.getService().onProcessDatagram(this, remote, bb);
	}
}
