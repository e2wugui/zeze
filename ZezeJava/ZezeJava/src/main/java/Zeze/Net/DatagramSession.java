package Zeze.Net;

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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DatagramSession extends AsyncSocket {
	private static final @NotNull Logger logger = LogManager.getLogger(DatagramSession.class);
	private final @NotNull DatagramSocket socket;
	// N1-F1：跨线程可见性——selector线程在processDatagram写（NAT重绑），Send可在任意业务线程读；
	// 无happens-before边时应答持续发往失效地址。单引用读写无复合操作，volatile即可。
	private volatile @NotNull InetSocketAddress remote;
	private final long tokenId;
	private final AtomicLong serialIdGen = new AtomicLong();
	private final @Nullable Encrypt2 encrypt;
	private final @Nullable Decrypt2 decrypt;
	private final @NotNull ReplayAttack replayAttack;
	private final LongAdder malformedPackets = new LongAdder();
	private volatile long lastMalformedWarnTime;

	public @NotNull DatagramSocket getSocket() {
		return socket;
	}

	public @Nullable SocketAddress getLocalAddress() {
		return socket.getLocal();
	}

	@Override
	public @NotNull SocketAddress getRemoteAddress() {
		return remote;
	}

	public long getTokenId() {
		return tokenId;
	}

	DatagramSession(@NotNull DatagramSocket socket, @NotNull InetSocketAddress remote, long tokenId,
					byte @Nullable [] securityKey, @NotNull ReplayAttackPolicy policy) {
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
	public boolean Send(byte @NotNull [] packet, int offset, int size) {
		if (isClosed()) // 已关闭的会话不得真实发出数据报并谎报成功
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
			synchronized (encrypt) { // Encrypt2有可变状态且非线程安全，而Send允许并发调用
				encrypt.reset(bc, bc.Bytes);
				encrypt.update(packet, offset, size);
				encrypt.update(bc.Bytes, 0, 16); // [8]tokenId | [8]serialId
				encrypt.flush();
			}
			bb = bc;
		}
		// serialId 和 sendTo 之间有窗口，可能大的 serialId 后发送。这是udp，不解决这个问题了。
		try {
			socket.sendTo(remote, bb.Bytes, 0, bb.WriteIndex);
		} catch (IOException e) {
			// FND8-50：履行AsyncSocket布尔契约（对齐TcpSocket）：IO失败close并返回false，
			// 不抛RuntimeException——否则上层Rpc.Send在addRpcContext之后、清理之前被异常
			// 穿透，rpcContexts条目永久泄漏（无超时定时器兜底）。UDP非阻塞send的现实IOException
			// 基本只有ClosedChannelException（会话随channel生死），close不过激；丢包/缓冲满
			// 表现为send返回0而非异常，属UDP允许丢包语义。
			close(e);
			return false;
		}
		return true;
	}

	// [8]tokenId | [8]serialId | [4]moduleId | [4]protocolId | [4]size | protocolData
	// [8]tokenId | [8]serialId | encrypt{ [4]moduleId | [4]protocolId | [4]size | protocolData | [8]tokenId | [8]serialId }
	@Override
	public boolean Send(@NotNull Protocol<?> p) {
		if (isClosed()) // 已关闭的会话不得真实发出数据报并谎报成功
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
			synchronized (encrypt) { // Encrypt2有可变状态且非线程安全，而Send允许并发调用
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
		// serialId 和 sendTo 之间有窗口，可能大的 serialId 后发送。这是udp，不解决这个问题了。
		try {
			socket.sendTo(remote, bb.Bytes, 0, bb.WriteIndex);
		} catch (IOException e) {
			close(e); // 同Send(byte[],int,int)：布尔契约，不抛RuntimeException（FND8-50）
			return false;
		}
		return true;
	}

	/**
	 * @param bb 有效数据范围:[0,WriteIndex]. 方法外绝对不能持有bb.Bytes的引用! 也就是只能在方法内访问bb.
	 */
	public void onProcessDatagram(@NotNull InetSocketAddress remote, @NotNull ByteBuffer bb) {
		// 畸形包（解密CodecException/协议decode异常等）按包捕获：数据报独立成帧、无流失步，
		// 丢包即可；异常抛到Selector会强制关闭整个channel（全部会话陪葬）。限频warn+计数防日志洪水。
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

	private void processDatagram(@NotNull InetSocketAddress remote, @NotNull ByteBuffer bb) throws Exception {
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

	@Override
	public @NotNull Type getType() {
		return Type.eClient;
	}

	@Override
	protected void doClose(@Nullable Throwable ex, boolean gracefully) {
		socket.removeSession(this);
		try {
			getService().OnSocketClose(this, ex); // 对齐TcpSocket/WebsocketClient家族的关闭契约
		} catch (Exception e) {
			logger.error("OnSocketClose exception:", e);
		}
		// 对齐TcpSocket.realClose：会话销毁后将在飞Rpc上下文立即失败处置（FND8-50补充，
		// 与FND8-44同点）——否则等待方只能干等Rpc超时，且OnSocketDisposed覆写永不触发。
		fireOnSocketDisposed();
	}

	@Override
	public @Nullable TimeThrottle getTimeThrottle() {
		return null;
	}


	@Override
	public @NotNull String toString() {
		return socket.toString() + '-' + remote + '[' + tokenId + ']';
	}
}
