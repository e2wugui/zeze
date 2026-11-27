package Zeze.Net;

import java.util.Objects;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.ReplayAttack;
import Zeze.Util.ReplayAttackGrowRange;
import Zeze.Util.ReplayAttackMax;
import Zeze.Util.ReplayAttackPolicy;
import Zeze.Util.TimeThrottle;
import Zeze.Util.ZezeCounter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * UDP 会话：以 tokenId 标识的单条 UDP 虚拟连接（AsyncSocket 的数据报适配）。
 */
public class DatagramSession extends AsyncSocket {
	@Override
	public Service getService() {
		return Objects.requireNonNull(super.getService());
	}

	private static final Logger logger = LogManager.getLogger(DatagramSession.class);
	private final DatagramSocket socket;
	private volatile InetSocketAddress remote; // selector 线程更新（NAT 重绑），业务线程并发读
	private final long tokenId;
	private final AtomicLong serialIdGen = new AtomicLong();
	private final @Nullable Encrypt2 encrypt;
	private final @Nullable Decrypt2 decrypt;
	private final ReplayAttack replayAttack;
	private final LongAdder malformedPackets = new LongAdder();
	private volatile long lastMalformedWarnTime;

	public DatagramSocket getSocket() {
		return socket;
	}

	public @Nullable SocketAddress getLocalAddress() {
		return socket.getLocal();
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return remote;
	}

	public long getTokenId() {
		return tokenId;
	}

	DatagramSession(DatagramSocket socket, InetSocketAddress remote, long tokenId,
					byte @Nullable [] securityKey, ReplayAttackPolicy policy) {
		super(socket.getService());
		this.socket = socket;
		this.remote = remote;
		this.tokenId = tokenId;
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

	// [8]tokenId | [8]serialId | packet
	// [8]tokenId | [8]serialId | encrypt{ packet | [8]tokenId | [8]serialId }
	@Override
	public boolean Send(byte [] packet, int offset, int size) {
		if (isClosed())
			return false;
		var serialId = serialIdGen.incrementAndGet();
		ByteBuffer bb;
		if (encrypt == null) {
			bb = ByteBuffer.Allocate(8 + 8 + size);
			bb.WriteLong8s(tokenId, serialId);
			bb.Append(packet, offset, size);
		} else {
			var bc = new BufferCodec(8 + 8 + 8 + size);
			bc.WriteLong8s(tokenId, serialId);
			// 下面的数据需要加密
			synchronized (encrypt) { // Encrypt2 非线程安全，Send 允许并发调用
				encrypt.reset(bc, bc.Bytes);
				encrypt.update(packet, offset, size);
				encrypt.update(bc.Bytes, 0, 16); // [8]tokenId | [8]serialId
				encrypt.flush();
			}
			bb = bc;
		}
		// serialId 和 sendTo 之间有窗口，可能大的 serialId 后发送；UDP 语义下不保证发送顺序。
		try {
			if (!socket.sendTo(remote, bb.Bytes, 0, bb.WriteIndex))
				return false; // 发送缓冲满，通道仍健康，不 close
		} catch (IOException e) {
			close(e);
			return false;
		}
		return true;
	}

	// [8]tokenId | [8]serialId | [4]moduleId | [4]protocolId | [4]size | protocolData
	// [8]tokenId | [8]serialId | encrypt{ [4]moduleId | [4]protocolId | [4]size | protocolData | [8]tokenId | [8]serialId }
	@Override
	public boolean Send(Protocol<?> p) {
		if (isClosed())
			return false;
		int preAllocSize = p.preAllocSize();
		var serialId = serialIdGen.incrementAndGet();
		ByteBuffer bb;
		if (encrypt == null) {
			bb = ByteBuffer.Allocate(Math.min(8 + 8 + Protocol.HEADER_SIZE + preAllocSize, 65536));
			bb.WriteLong8s(tokenId, serialId);
			p.encodeWithHead(bb);
			ZezeCounter.instance.addSendSize(p.getTypeId(), bb.WriteIndex - 16); // 减去tokenId/serialId组帧
		} else {
			var bc = new BufferCodec(Math.min(8 + 8 + 8 + Protocol.HEADER_SIZE + preAllocSize, 65536));
			bc.WriteLong8s(tokenId, serialId);
			// 下面的数据需要加密
			synchronized (encrypt) { // Encrypt2 非线程安全，Send 允许并发调用
				encrypt.reset(bc, bc.Bytes);
				var tmp = ByteBuffer.Allocate(Math.min(Protocol.HEADER_SIZE + preAllocSize, 65536));
				p.encodeWithHead(tmp);
				ZezeCounter.instance.addSendSize(p.getTypeId(), tmp.WriteIndex);
				encrypt.update(tmp.Bytes, 0, tmp.WriteIndex);
				encrypt.update(bc.Bytes, 0, 16); // [8]tokenId | [8]serialId
				encrypt.flush();
			}
			bb = bc;
		}
		// serialId 和 sendTo 之间有窗口，可能大的 serialId 后发送；UDP 语义下不保证发送顺序。
		try {
			if (!socket.sendTo(remote, bb.Bytes, 0, bb.WriteIndex))
				return false;
		} catch (IOException e) {
			close(e);
			return false;
		}
		return true;
	}

	/**
	 * @param bb 有效数据范围:[0,WriteIndex]. 方法外绝对不能持有bb.Bytes的引用! 也就是只能在方法内访问bb.
	 */
	public void onProcessDatagram(InetSocketAddress remote, ByteBuffer bb) {
		// 畸形包按包捕获丢弃并限频记日志，避免异常抛到 Selector 关闭共享的 UDP channel。
		try {
			processDatagram(remote, bb);
		} catch (Exception e) {
			malformedPackets.increment();
			var now = System.currentTimeMillis();
			if (now - lastMalformedWarnTime >= 1000) {
				lastMalformedWarnTime = now;
				logger.warn("malformed datagram: count={} token={} source={}",
						malformedPackets.sumThenReset(), tokenId, remote, e);
			}
		}
	}

	private void processDatagram(InetSocketAddress remote, ByteBuffer bb) throws Exception {
		int endPos = bb.WriteIndex;
		if (decrypt != null) {
			if (endPos < 32) // minimal packet size ([8]tokenId + [8]serialId + [8]tokenId + [8]serialId)
				return;
			var bc = new BufferCodec(bb);
			bc.WriteIndex = 16; // 重置到加密数据的起始位置,准备覆写解密数据
			decrypt.reset(bc, bb.Bytes); // Decrypt2支持原地解密
			decrypt.update(bb.Bytes, 16, endPos - 16);
			decrypt.flush();
			if (!Arrays.equals(bb.Bytes, 0, 16, bb.Bytes, endPos - 16, endPos)) // check decrypted data (tokenId)
				return;
			bb.WriteIndex = endPos - 16; // 数据尾部位置向前跳过验证过的"[8]tokenId + [8]serialId"
		} else if (endPos < 16) // minimal packet size([8]tokenId + [8]serialId)
			return;
		bb.ReadIndex = 16; // 跳过头部的tokenId和serialId
		validateDatagram(bb);
		var serialId = ByteBuffer.ToLong(bb.Bytes, 8);
		replayAttack.lock();
		try {
			if (replayAttack.replay(serialId))
				return;
		} finally {
			replayAttack.unlock();
		}
		this.remote = remote;
		socket.getService().OnSocketProcessInputBuffer(this, bb);
	}

	// UDP 不保留半包：更新接收状态或派发前先整包校验所有协议帧头与长度。
	private void validateDatagram(ByteBuffer input) {
		var data = ByteBuffer.Wrap(input.Bytes, input.ReadIndex, input.size());
		while (!data.isEmpty()) {
			if (data.size() < Protocol.HEADER_SIZE)
				throw new IllegalStateException("incomplete datagram protocol header");
			Protocol.decode(getService(), data);
		}
	}

	@Override
	public Type getType() {
		return Type.eClient;
	}

	@Override
	protected void doClose(@Nullable Throwable ex, boolean gracefully) {
		socket.removeSession(this);
		try {
			getService().OnSocketClose(this, ex);
		} catch (Exception e) {
			logger.error("OnSocketClose exception:", e);
		}
		fireOnSocketDisposed();
	}

	@Override
	public @Nullable TimeThrottle getTimeThrottle() {
		return null;
	}

	@Override
	public String toString() {
		return socket.toString() + '-' + remote + '[' + tokenId + ']';
	}
}
