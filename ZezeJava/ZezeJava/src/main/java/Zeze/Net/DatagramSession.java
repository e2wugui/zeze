package Zeze.Net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.ReplayAttack;
import Zeze.Util.ReplayAttackGrowRange;
import Zeze.Util.ReplayAttackMax;
import Zeze.Util.ReplayAttackPolicy;

public class DatagramSession {
	private final DatagramSocket socket;
	private InetSocketAddress remote;
	private final long sessionId;
	private final AtomicLong serialId = new AtomicLong();
	private final byte[] securityKey;
	private ReplayAttack replayAttack;

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

	public DatagramSession(DatagramSocket socket, InetSocketAddress remote,
						   long sessionId, byte[] securityKey,
						   ReplayAttackPolicy policy) {
		this.socket = socket;
		this.remote = remote;
		this.sessionId = sessionId;
		this.securityKey = securityKey;
		switch (policy) {
		case IncreasingOnly:
				this.replayAttack = new ReplayAttackMax();
				break;
		case AllowDisorder:
				this.replayAttack = new ReplayAttackGrowRange();
				break;
		default:
			throw new RuntimeException("unknown policy.");
		}
	}

	public void send(byte[] packet, int offset, int size) throws IOException {
		var bb = new BufferCodec(ByteBuffer.Allocate(8 + 9 + size));
		bb.WriteLong8(sessionId);

		// 下面这块数据加密
		if (null == securityKey) {
			bb.WriteLong(serialId.incrementAndGet());
			bb.Append(packet, offset, size);
		} else {
			var securityBb = ByteBuffer.Allocate(9);
			securityBb.WriteLong(serialId.incrementAndGet());
			var encrypt = new Encrypt2(bb, securityKey);
			encrypt.update(securityBb.Bytes, 0, securityBb.WriteIndex);
			encrypt.update(packet, offset, size);
			encrypt.flush();
		}
		// serialId 和 sendTo 之间有窗口，可能大的 serialId 后发送。这是udp，不解决这个问题了。
		socket.sendTo(remote, bb.Bytes, 0, bb.WriteIndex);
	}

	public void send(Protocol<?> p) throws IOException {
		int preAllocSize = p.preAllocSize();
		var bb = new BufferCodec(ByteBuffer.Allocate(8 + 9 + Protocol.HEADER_SIZE + preAllocSize));
		bb.WriteLong8(sessionId);

		// 下面这块数据加密
		if (null == securityKey) {
			bb.WriteLong(serialId.incrementAndGet());
			p.encodeWithHead(bb);
		} else {
			var securityBb = ByteBuffer.Allocate(9 + Protocol.HEADER_SIZE + preAllocSize);
			securityBb.WriteLong(serialId.incrementAndGet());
			p.encodeWithHead(securityBb);
			var encrypt = new Encrypt2(bb, securityKey);
			encrypt.update(securityBb.Bytes, 0, securityBb.WriteIndex);
			encrypt.flush();
		}

		// serialId 和 sendTo 之间有窗口，可能大的 serialId 后发送。这是udp，不解决这个问题了。
		socket.sendTo(remote, bb.Bytes, 0, bb.WriteIndex);
	}

	/**
	 * @param bb 方法外绝对不能持有bb.Bytes的引用! 也就是只能在方法内读bb.
	 */
	public void onProcessDatagram(InetSocketAddress remote, ByteBuffer bb) throws Exception {
		this.remote = remote;

		// 解密
		if (null != securityKey) {
			var decryptBuffer = new BufferCodec(ByteBuffer.Allocate(bb.size()));
			var decrypt = new Decrypt2(decryptBuffer, securityKey);
			decrypt.update(bb.Bytes, bb.ReadIndex, bb.size());
			decrypt.flush();
			bb = decryptBuffer.getBuffer();

			// 防重放处理。只有加密才支持防重放。
			var serialId = bb.ReadLong();
			synchronized (this) {
				if (replayAttack.replay(serialId))
					return;
			}
		} else {
			bb.ReadLong(); // discard serialId;
		}
		socket.getService().onProcessDatagram(this, bb);
	}

	public void close() {
		socket.removeSession(this);
	}
}
