package Zeze.Net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
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
	private final AtomicLong serialIdGen = new AtomicLong();
	private final Encrypt2 encrypt;
	private final Decrypt2 decrypt;
	private final ReplayAttack replayAttack;

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

	public DatagramSession(DatagramSocket socket, InetSocketAddress remote, long sessionId, byte[] securityKey,
						   ReplayAttackPolicy policy) {
		this.socket = socket;
		this.remote = remote;
		this.sessionId = sessionId;
		if (securityKey != null) {
			var key = Digest.md5(securityKey);
			encrypt = new Encrypt2(null, key, null);
			decrypt = new Decrypt2(null, key, null);
		} else {
			encrypt = null;
			decrypt = null;
		}
		switch (policy) {
		case IncreasingOnly:
			replayAttack = new ReplayAttackMax();
			break;
		case AllowDisorder:
			replayAttack = new ReplayAttackGrowRange();
			break;
		default:
			throw new UnsupportedOperationException("unknown policy: " + policy);
		}
	}

	// [8]sessionId | [8]serialId | packet
	// [8]sessionId | [8]serialId | encrypt{ packet | [8]sessionId | [8]serialId }
	public void send(byte[] packet, int offset, int size) throws IOException {
		var serialId = serialIdGen.incrementAndGet();
		ByteBuffer bb;
		if (encrypt == null) {
			bb = ByteBuffer.Allocate(8 + 8 + size);
			bb.WriteLong8(sessionId);
			bb.WriteLong8(serialId);
			bb.Append(packet, offset, size);
		} else {
			var bc = new BufferCodec(8 + 8 + 8 + size);
			bc.WriteLong8(sessionId);
			bc.WriteLong8(serialId);
			// 下面的数据需要加密
			encrypt.reset(bc, bc.Bytes);
			encrypt.update(packet, offset, size);
			encrypt.update(bc.Bytes, 0, 16); // [8]sessionId | [8]serialId
			encrypt.flush();
			bb = bc;
		}
		// serialId 和 sendTo 之间有窗口，可能大的 serialId 后发送。这是udp，不解决这个问题了。
		socket.sendTo(remote, bb.Bytes, 0, bb.WriteIndex);
	}

	// [8]sessionId | [8]serialId | [4]moduleId | [4]protocolId | [4]size | protocolData
	// [8]sessionId | [8]serialId | encrypt{ [4]moduleId | [4]protocolId | [4]size | protocolData | [8]sessionId | [8]serialId }
	public void send(Protocol<?> p) throws IOException {
		int preAllocSize = p.preAllocSize();
		var serialId = serialIdGen.incrementAndGet();
		ByteBuffer bb;
		if (encrypt == null) {
			bb = ByteBuffer.Allocate(Math.min(8 + 8 + Protocol.HEADER_SIZE + preAllocSize, 65536));
			bb.WriteLong8(sessionId);
			bb.WriteLong8(serialId);
			p.encodeWithHead(bb);
		} else {
			var bc = new BufferCodec(Math.min(8 + 8 + 8 + Protocol.HEADER_SIZE + preAllocSize, 65536));
			bc.WriteLong8(sessionId);
			bc.WriteLong8(serialId);
			// 下面的数据需要加密
			encrypt.reset(bc, bc.Bytes);
			var tmp = ByteBuffer.Allocate(Math.min(Protocol.HEADER_SIZE + preAllocSize, 65536));
			p.encodeWithHead(tmp);
			encrypt.update(tmp.Bytes, 0, tmp.WriteIndex);
			encrypt.update(bc.Bytes, 0, 16); // [8]sessionId | [8]serialId
			encrypt.flush();
			bb = bc;
		}
		// serialId 和 sendTo 之间有窗口，可能大的 serialId 后发送。这是udp，不解决这个问题了。
		socket.sendTo(remote, bb.Bytes, 0, bb.WriteIndex);
	}

	/**
	 * @param bb 有效数据范围:[0,WriteIndex]. 方法外绝对不能持有bb.Bytes的引用! 也就是只能在方法内访问bb.
	 */
	public void onProcessDatagram(InetSocketAddress remote, ByteBuffer bb) throws Exception {
		int endPos = bb.WriteIndex;
		if (decrypt != null) {
			if (endPos < 32) // minimal packet size ([8]sessionId + [8]serialId + [8]sessionId + [8]serialId)
				return;
			var bc = new BufferCodec(bb);
			bc.WriteIndex = 16; // 重置到加密数据的起始位置,准备覆写解密数据
			decrypt.reset(bc, bb.Bytes); // Decrypt2支持原地解密
			decrypt.update(bb.Bytes, 16, endPos - 16);
			decrypt.flush();
			if (!Arrays.equals(bb.Bytes, 0, 16, bb.Bytes, endPos - 16, endPos)) // check decrypted data (sessionId)
				return;
			bb.WriteIndex = endPos - 16; // 数据尾部位置向前跳过验证过的"[8]sessionId + [8]serialId"
		} else if (endPos < 16) // minimal packet size([8]sessionId + [8]serialId)
			return;
		bb.ReadIndex = 16; // 跳过头部的sessionId和serialId
		var serialId = ByteBuffer.ToLong(bb.Bytes, 8);
		replayAttack.lock();
		try {
			if (replayAttack.replay(serialId))
				return;
		} finally {
			replayAttack.unlock();
		}
		this.remote = remote;
		socket.getService().onProcessDatagram(this, bb, serialId);
	}

	public void close() {
		socket.removeSession(this);
	}

	@Override
	public String toString() {
		return socket.toString() + '-' + (remote != null ? remote : "") + '[' + sessionId + ']';
	}
}
