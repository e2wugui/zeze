package Zeze.Net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Serialize.ByteBuffer;

public class DatagramSession {
	private final DatagramSocket socket;
	InetSocketAddress remote;
	private final long sessionId;
	private AtomicLong serialId = new AtomicLong();
	private final byte[] securityKey;
	private ReplayAttackPolicy replayAttackPolicy = ReplayAttackPolicy.IncreasingOnly;

	public enum ReplayAttackPolicy {
		IncreasingOnly,
		AllowDisorder,
	}

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
						   ReplayAttackPolicy replayAttackPolicy) {
		this.socket = socket;
		this.remote = remote;
		this.sessionId = sessionId;
		this.securityKey = securityKey;
		this.replayAttackPolicy = replayAttackPolicy;
	}

	public void send(byte[] packet, int offset, int size) throws IOException {
		var bb = ByteBuffer.Allocate();
		bb.WriteLong(sessionId);

		// 下面这块数据加密
		if (null == securityKey) {
			bb.WriteLong(serialId.incrementAndGet());
			bb.Append(packet, offset, size);
		} else {
			var securityBb = ByteBuffer.Allocate();
			securityBb.WriteLong(serialId.incrementAndGet());
			securityBb.Append(packet, offset, size);

			var encryptedBuffer = new BufferCodec();
			var encrypt = new Encrypt2(encryptedBuffer, securityKey);
			encrypt.update(securityBb.Bytes, securityBb.ReadIndex, securityBb.size());
			encrypt.flush();
			bb.Append(encryptedBuffer.Bytes, encryptedBuffer.ReadIndex, encryptedBuffer.size());
		}
		// serialId 和 sendTo 之间有窗口，可能大的 serialId 后发送。这是udp，不解决这个问题了。
		socket.sendTo(remote, bb.Bytes, bb.ReadIndex, bb.size());
	}

	public void send(Protocol<?> p) throws IOException {
		var bb = ByteBuffer.Allocate();
		bb.WriteLong(sessionId);

		// 下面这块数据加密
		if (null == securityKey) {
			bb.WriteLong(serialId.incrementAndGet());
			p.encodeWithHead(bb);
		} else {
			var securityBb = ByteBuffer.Allocate();
			securityBb.WriteLong(serialId.incrementAndGet());
			p.encodeWithHead(securityBb);
			var encryptedBuffer = new BufferCodec();
			var encrypt = new Encrypt2(encryptedBuffer, securityKey);
			encrypt.update(securityBb.Bytes, securityBb.ReadIndex, securityBb.size());
			encrypt.flush();
			bb.Append(encryptedBuffer.Bytes, encryptedBuffer.ReadIndex, encryptedBuffer.size());
		}

		// serialId 和 sendTo 之间有窗口，可能大的 serialId 后发送。这是udp，不解决这个问题了。
		socket.sendTo(remote, bb.Bytes, bb.ReadIndex, bb.size());
	}

	public void onProcessDatagram(InetSocketAddress remote, ByteBuffer bb) throws Exception {
		this.remote = remote;

		// 解密
		if (null != securityKey) {
			var decryptBuffer = new BufferCodec();
			var decrypt = new Decrypt2(decryptBuffer, securityKey);
			decrypt.update(bb.Bytes, bb.ReadIndex, bb.size());
			decrypt.flush();
			bb = decryptBuffer.getBuffer();

			// 防重放处理。只有加密才支持防重放。
			var serialId = bb.ReadLong();
			switch (replayAttackPolicy) {
			case IncreasingOnly:
				synchronized (this) {
					if (serialId <= maxReceiveSerialId)
						return;
					maxReceiveSerialId = serialId;
				}
				break;

			case AllowDisorder:
				if (replayAttack(serialId))
					return;
				break;
			}
		} else {
			bb.ReadLong(); // discard serialId;
		}
		socket.getService().onProcessDatagram(this, bb);
	}

	private long maxReceiveSerialId;
	private final byte[] replayAttack = new byte[128];
	private int maxBitPosition;

	private synchronized boolean replayAttack(long serialId) {
		long grow = serialId - maxReceiveSerialId;
		if (grow > replayAttack.length * 8L)
			return true; // 跳的太远，拒绝掉。

		int increase = (int)grow;
		if (increase > 0) { // grow clear
			for (var i = 0; i < increase; ++i) {
				// clear bit
				var pos = (maxBitPosition + i) % (replayAttack.length * 8);
				var index = pos / 8;
				var bit = 1 << pos % 8;
				replayAttack[index] &= ~bit;
			}
			maxBitPosition += increase;
			if (maxBitPosition > replayAttack.length * 8)
				maxBitPosition %= replayAttack.length * 8;

			return false; // allow
		}
		if (increase <= -replayAttack.length)
			return true; // 过期的，拒绝掉。

		var pos = maxBitPosition + increase;
		if (pos < 0) // 有范围检查，只需要加一次，否则用while
			pos += replayAttack.length;

		var index = pos / 8;
		var bit = 1 << pos % 8;
		if ((replayAttack[index] & bit) != 0)
			return true; // duplicate
		replayAttack[index] |= bit;
		return false;
	}
}
