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

	public DatagramSession(DatagramSocket socket, InetSocketAddress remote,
						   long sessionId, byte[] securityKey,
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
			throw new RuntimeException("unknown policy.");
		}
	}

	// [8]sessionId | [8]serialId | encrypt{ [8]sessionId | packet }
	public void send(byte[] packet, int offset, int size) throws IOException {
		var serialId = serialIdGen.incrementAndGet();
		ByteBuffer bb;
		if (encrypt == null) {
			bb = ByteBuffer.Allocate(8 + 8 + size);
			bb.WriteLong8(sessionId);
			bb.WriteLong8(serialId);
			bb.Append(packet, offset, size);
		} else {
			var bc = new BufferCodec(ByteBuffer.Allocate(8 + 8 + 8 + size));
			bc.WriteLong8(sessionId);
			bc.WriteLong8(serialId);
			// 下面的数据需要加密
			encrypt.reset(bc, bc.Bytes);
			encrypt.update(bc.Bytes, 0, 8); // sessionId
			encrypt.update(packet, offset, size);
			encrypt.flush();
			bb = bc;
		}
		// serialId 和 sendTo 之间有窗口，可能大的 serialId 后发送。这是udp，不解决这个问题了。
		socket.sendTo(remote, bb.Bytes, 0, bb.WriteIndex);
	}

	// [8]sessionId | [8]serialId | encrypt{ [8]sessionId | [4]moduleId | [4]protocolId | [4]size | protocolData }
	public void send(Protocol<?> p) throws IOException {
		int preAllocSize = p.preAllocSize();
		var serialId = serialIdGen.incrementAndGet();
		ByteBuffer bb;
		if (encrypt == null) {
			bb = ByteBuffer.Allocate(8 + 8 + Protocol.HEADER_SIZE + preAllocSize);
			bb.WriteLong8(sessionId);
			bb.WriteLong8(serialId);
			p.encodeWithHead(bb);
		} else {
			var bc = new BufferCodec(ByteBuffer.Allocate(8 + 8 + 8 + Protocol.HEADER_SIZE + preAllocSize));
			bc.WriteLong8(sessionId);
			bc.WriteLong8(serialId);
			// 下面的数据需要加密
			encrypt.reset(bc, bc.Bytes);
			encrypt.update(bc.Bytes, 0, 8); // sessionId
			var tmp = ByteBuffer.Allocate(Protocol.HEADER_SIZE + preAllocSize);
			p.encodeWithHead(tmp);
			encrypt.update(tmp.Bytes, 0, tmp.WriteIndex);
			encrypt.flush();
			bb = bc;
		}
		// serialId 和 sendTo 之间有窗口，可能大的 serialId 后发送。这是udp，不解决这个问题了。
		socket.sendTo(remote, bb.Bytes, 0, bb.WriteIndex);
	}

	/**
	 * @param bb 方法外绝对不能持有bb.Bytes的引用! 也就是只能在方法内访问bb.
	 */
	public void onProcessDatagram(InetSocketAddress remote, ByteBuffer bb) throws Exception {
		if (bb.WriteIndex < 24) // minimal packet size
			return;
		var serialId = ByteBuffer.ToLong(bb.Bytes, 8);
		if (decrypt != null) {
			int size = bb.WriteIndex - 16;
			bb.WriteIndex = 16;
			var bc = new BufferCodec(bb);
			decrypt.reset(bc, bb.Bytes); // Decrypt2支持原地解密
			decrypt.update(bb.Bytes, 16, size);
			decrypt.flush();
			if (!Arrays.equals(bb.Bytes, 0, 8, bb.Bytes, 16, 24)) // check decrypted data (sessionId)
				return;
			bb.ReadIndex = 24;
			bb.WriteIndex = bc.WriteIndex;
		} else
			bb.ReadIndex = 16;
		synchronized (replayAttack) {
			if (replayAttack.replay(serialId))
				return;
		}
		this.remote = remote;
		socket.getService().onProcessDatagram(this, bb, serialId);
	}

	public void close() {
		socket.removeSession(this);
	}
}
