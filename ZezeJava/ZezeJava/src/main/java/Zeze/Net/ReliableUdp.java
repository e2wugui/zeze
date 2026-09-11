package Zeze.Net;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.LongHashSet;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * 简单可靠udp实现。
 * 多对多模式：绑定一个udp，可以发送和接收多个其他机器的数据。没有使用Udp.connect。
 * 1 数据重发。
 * 2 处理乱序。
 * 3 没有流量控制。
 * 4 会话代际（FND3-26）：serialId 只在进程内单调，任一端重启后对端无法把新序号空间与旧会话
 *   的去重状态对齐（发送端重启→旧会话按重复包丢弃但照常回Ack，数据静默丢失；接收端重启→
 *   发送端续高序号全部滞留接收窗口，整条流永久停摆）。每个会话创建时随机分配64位代际
 *   （generation），随每个 Packet/Control 携带：
 *   - 接收端收到全新代际 → 对端重启（发送方向），重置接收状态，按新空间从 1 接收；
 *   - 发送端从 Ack/Resend 发现对端代际换代 → 对端接收状态已归零，丢弃在途包（回调
 *     onSessionReset）、序号归 1 并更换自身代际，让对端同步清掉旧空间滞留包；
 *   - 退役代际记忆：旧代迟到残包整体忽略——否则会被当成全新代际再次触发重置，重新制造停摆；
 *   - Ack/Resend 回显所针对的对端代际，跨代际的确认/重发请求一律忽略。
 *   代际与时钟无关。不可检测：VM快照恢复（同代际回退）；无认证（伪造包可重置会话，与整体水位一致）。
 */
public class ReliableUdp extends ReentrantLock implements SelectorHandle, Closeable {
	private static final Logger logger = LogManager.getLogger(ReliableUdp.class);

	public static final int TypePacket = 0;
	public static final int TypeControl = 1;

	// 接收乱序窗口上界：packet.serialId 与 lastDispatchedSerialId 的差值超过这个值的包直接丢弃（不回 Ack）。
	// serialId 来自网络输入，不校验的话，伪造的超大 serialId 会抬高 maxRecvPacketSerialId，导致构造海量 Resend 条目（单包DoS）。
	// 取 1<<14：覆盖约 5k 包/秒 × 3 秒兜底重发窗口的合法在途差值；伪造单包的残余成本被压到亚毫秒级
	// （Resend 请求按 MaxPacketLength 截断，见 processPacket；recvWindow 滞留量以实际收到的包数为限）。
	private static final long MaxRecvSerialIdWindow = 1L << 14;

	// Packet 线上编码最坏开销：type(1)+代际(9)+序号(9)+长度前缀(5)=24B。
	// 接收端以 MaxPacketLength 为接收缓冲，数据报超出即被静默截断成畸形包，入口预留余量。
	private static final int MaxWireOverhead = 32;

	// 会话代际随机源：SecureRandom 64 位，与时钟无关；0 保留表示“未知代际”。
	private static final SecureRandom GenerationRandom = new SecureRandom();

	private static long nextGeneration() {
		var gen = GenerationRandom.nextLong();
		return gen != 0 ? gen : 1;
	}

	private final DatagramChannel datagramChannel;
	private SelectionKey selectionKey;
	private final Selector selector;
	private final InetSocketAddress local;
	private final ConcurrentHashMap<SocketAddress, Session> sessions = new ConcurrentHashMap<>();
	private final ReliableUdpHandle defaultHandle;
	private int MaxPacketLength = 2048;
	private final LongAdder malformedPackets = new LongAdder();
	private volatile long lastMalformedWarnTime;

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
			var bindAddr = new InetSocketAddress(InetAddress.getByName(address), port);
			datagramChannel = DatagramChannel.open();
			datagramChannel.configureBlocking(false);
			datagramChannel.bind(bindAddr);
			// 必须回读实际绑定地址：port 传 0（临时端口）时请求地址的端口仍是 0。
			local = (InetSocketAddress)datagramChannel.getLocalAddress();
			selector = Selectors.getInstance().choice();
			selectionKey = selector.register(datagramChannel, SelectionKey.OP_READ, this);
		} catch (IOException e) {
			throw Task.forceThrow(e);
		}
	}

	// 打开一个连接用来发送数据。
	public Session open(String peer, int port, ReliableUdpHandle handle) {
		try {
			var ep = new InetSocketAddress(InetAddress.getByName(peer), port);
			return new Session(ep, handle);
		} catch (UnknownHostException e) {
			throw Task.forceThrow(e);
		}
	}

	public static class Packet implements Serializable {
		public long generation; // 发送方会话代际
		public long serialId;
		public byte[] bytes;
		public int offset;
		public int length;

		// 发送端用来记录这个包的重发 TimerTask。每个包一个Timer很浪费，先这样。
		public transient Future<?> resendTimerTask;

		public Packet() {
		}

		public Packet(long generation, long serialId, byte[] bytes, int offset, int length) {
			this.generation = generation;
			this.serialId = serialId;
			this.bytes = bytes;
			this.offset = offset;
			this.length = length;
		}

		@Override
		public void decode(IByteBuffer bb) {
			// TypePacket 外面解析。
			generation = bb.ReadLong();
			serialId = bb.ReadLong();
			bytes = bb.ReadBytes();
			offset = 0;
			length = bytes.length;
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteUInt(TypePacket);
			bb.WriteLong(generation);
			bb.WriteLong(serialId);
			bb.WriteBytes(bytes, offset, length);
		}
	}

	public static class Control implements Serializable {
		public static final int Ack = 1;
		public static final int Resend = 2;
		public static final int NoSession = 3;

		public long generation;     // 发送方会话代际；NoSession 无会话上下文，恒 0
		public long peerGeneration; // Ack/Resend 回显所针对的对端代际；NoSession 恒 0
		public int command;
		public final LongHashSet serialIds = new LongHashSet();

		@Override
		public void decode(IByteBuffer bb) {
			// TypePacket 外面解析。
			generation = bb.ReadLong();
			peerGeneration = bb.ReadLong();
			command = bb.ReadInt();
			for (int count = bb.ReadUInt(); count > 0; count--)
				serialIds.add(bb.ReadLong());
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteUInt(TypeControl);
			bb.WriteLong(generation);
			bb.WriteLong(peerGeneration);
			bb.WriteInt(command);
			bb.WriteUInt(serialIds.size());
			serialIds.foreach(bb::WriteLong);
		}
	}

	/**
	 * 对端代际跟踪器（仅 selector 线程访问）。
	 * 数据包与控制包各用一个实例：换代信号必须两个方向各观察到一次——控制方向先学到新代际
	 * 触发发送端重整后，数据方向仍保持旧值，对端换代后的第一批数据包才能再次触发接收重置。
	 */
	private static final class GenerationTracker {
		static final int RetiredCapacity = 8;
		long current; // 0=尚未从对端学到
		final long[] retired = new long[RetiredCapacity];
		int head; // 下一个写入位置
		int size;

		/** adopt 结果：Ignore=不采纳（generation 为 0 或旧代残包）；Learn=首次学习；Change=检测到对端换代。 */
		enum Adopt { Ignore, Learn, Change }

		Adopt adopt(long generation) {
			if (generation == 0 || generation == current)
				return Adopt.Ignore;
			for (int i = 0; i < size; i++) {
				int idx = head - 1 - i;
				if (idx < 0)
					idx += retired.length;
				if (retired[idx] == generation)
					return Adopt.Ignore; // 旧代残包
			}
			var first = current == 0;
			if (!first)
				push(current);
			current = generation;
			return first ? Adopt.Learn : Adopt.Change;
		}

		private void push(long gen) {
			retired[head] = gen;
			head = (head + 1) % retired.length;
			if (size < retired.length)
				size++;
		}
	}

	public class Session {
		private final ReliableUdpHandle handle;
		private final SocketAddress peer;
		private final LongConcurrentHashMap<Packet> sendWindow = new LongConcurrentHashMap<>();
		private final AtomicLong serialIdGenerator = new AtomicLong(1);
		private final LongConcurrentHashMap<Packet> recvWindow = new LongConcurrentHashMap<>();

		// 本端会话代际：创建时随机；发送重整时更换（让对端清掉旧空间滞留包）。
		// volatile：send()（应用线程，持本会话锁）读，rebaseSendState（selector 线程，持本会话锁）写。
		private volatile long selfGeneration;

		// ===== 以下仅 selector 线程访问（processPacket/processPacketControl 单线程）=====
		private final GenerationTracker peerPacketGeneration = new GenerationTracker();
		private final GenerationTracker peerControlGeneration = new GenerationTracker();
		private long lastDispatchedSerialId;
		private long maxRecvPacketSerialId;

		public Session(SocketAddress peer, ReliableUdpHandle handle) {
			this.handle = handle;
			this.peer = peer;
			this.selfGeneration = nextGeneration();
			var old = sessions.put(peer, this);
			if (old != null) // 覆盖旧会话时取消其重发定时器，避免泄漏
				old.cancelResendTimers();
		}

		// 取消发送窗口内所有包的重发定时器
		private void cancelResendTimers() {
			for (var it = sendWindow.entryIterator(); it.moveToNext(); ) {
				var p = it.value();
				if (p != null && p.resendTimerTask != null)
					p.resendTimerTask.cancel(false);
			}
		}

		public boolean send(byte[] bytes, int offset, int length) {
			if (length > MaxPacketLength - MaxWireOverhead)
				throw new IllegalArgumentException(
						"length > MaxPacketLength - " + MaxWireOverhead + ": " + (MaxPacketLength - MaxWireOverhead));

			Packet packet;
			synchronized (this) { // 与 rebaseSendState 互斥：取号、入窗、挂定时器必须原子，防止重整归零后出现逆序号
				packet = new Packet(selfGeneration, serialIdGenerator.getAndIncrement(), bytes, offset, length);
				sendWindow.put(packet.serialId, packet);

				// start auto resend timer.
				packet.resendTimerTask = TaskSpec.ofAction(() -> sendTo(peer, packet)).schedulePeriodNow(3000, 3000);
			}
			return sendTo(peer, packet);
		}

		// 对端（发送方向）换代：重置接收状态，后续按新代际的序号空间从 1 接收。
		private void resetRecvState() {
			lastDispatchedSerialId = 0;
			maxRecvPacketSerialId = 0;
			recvWindow.clear();
			handle.onSessionReset(this); // 网络线程内联执行：保证先于新代际任何数据回调
		}

		// 对端换代（其对本端的接收状态已归零）或 NoSession：在途包不会再被对端按当前序号接收，
		// 丢弃并从 1 重新编号；同时更换自身代际，让对端把旧空间滞留在接收窗口的包一并清掉。
		private void rebaseSendState() {
			boolean dropped;
			synchronized (this) { // 与 send() 互斥
				dropped = !sendWindow.isEmpty();
				if (!dropped && serialIdGenerator.get() == 1)
					return; // 已是全新状态（无在途、序号未分配过），跳过；避免对端反复 NoSession 造成代际churn
				cancelResendTimers();
				sendWindow.clear();
				serialIdGenerator.set(1);
				selfGeneration = nextGeneration();
			}
			if (dropped)
				handle.onSessionReset(this); // 网络线程内联执行；需要可靠投递的业务数据请重发
		}
	}

	private boolean sendTo(SocketAddress peer, Serializable p) {
		try {
			var bb = ByteBuffer.Allocate(512);
			p.encode(bb);
			return datagramChannel.send(java.nio.ByteBuffer.wrap(bb.Bytes, bb.ReadIndex, bb.size()), peer)
					== bb.size();
		} catch (IOException e) {
			throw Task.forceThrow(e);
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
		TaskSpec.ofAction(() -> session.handle.handle(session, packet)).name("ReliableUdp.dispatch").runNow();
		// session.Handle.handle(session, packet); // 直接在网络线程中执行。
	}

	private void tryDispatchRecvWindow(Session session) {
		for (long serialId = session.lastDispatchedSerialId; ++serialId <= session.maxRecvPacketSerialId; ) {
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

		var session = sessions.get(source);
		if (session == null)
			session = dynamicCreateSession(source);

		if (session != null) {
			// 代际门：全新代际=对端重启（发送方向），重置接收状态按新空间从 1 接收；
			// 退役代际=旧代迟到残包，整体忽略——回 Ack 无意义（其代际已退役），重置则重新制造停摆。
			if (packet.generation != session.peerPacketGeneration.current) {
				switch (session.peerPacketGeneration.adopt(packet.generation)) {
				case Ignore -> { return; } // 旧代残包
				case Change -> session.resetRecvState();
				case Learn -> {} // 首次学习：会话接收状态本就为空
				}
			}

			// 窗口校验必须放在 Ack 之前：超窗包丢弃时不能回 Ack，否则发送方会把它从 SendWindow
			// 清除并停止重发，合法但暂时超窗的包（丢包+高速发送把差值顶过窗口）就永久丢了；
			// 不回 Ack，发送方 3 秒兜底重发会在缺口填上、lastDispatchedSerialId 追平后正常接收。
			// 重复包（serialId - lastDispatchedSerialId <= 0）不会被此检查误伤，会走到下面正常回 Ack。
			if (packet.serialId - session.lastDispatchedSerialId > MaxRecvSerialIdWindow)
				return; // skip packet beyond recv window. 超出乱序窗口上界，直接丢弃，防止抬高 maxRecvPacketSerialId。

			// 剩下的包（含重复包）只要收到就发送ack，不需要判断其他条件，这样让发送者能更好的的清除SendWindow。
			var ack = new Control();
			ack.command = Control.Ack;
			ack.generation = session.selfGeneration;
			ack.peerGeneration = packet.generation; // 回显被确认包的代际，发送端只认针对自己当前代际的确认
			ack.serialIds.add(packet.serialId);
			sendTo(source, ack);

			if (packet.serialId <= session.lastDispatchedSerialId)
				return; // skip duplicate packet.

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

			// 请求重发。只需索要缺口头部即可推进；截断防止重新同步时（对端重启后缺口可达
			// MaxRecvSerialIdWindow）构造超过 MaxPacketLength 的巨型控制包——编码/发送失败，
			// 缺口永远补不上。long 变长编码最多 9 字节，留 64 字节头部余量。
			var resend = new Control();
			resend.command = Control.Resend;
			resend.generation = session.selfGeneration;
			resend.peerGeneration = session.peerPacketGeneration.current;
			var maxResendIds = Math.max(1, (MaxPacketLength - 64) / 9);
			for (var serialId = session.lastDispatchedSerialId;
					++serialId < session.maxRecvPacketSerialId && resend.serialIds.size() < maxResendIds; )
				resend.serialIds.add(serialId);
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

		// 代际门（NoSession 无会话上下文 generation 恒 0，不经过此门）：
		// 全新代际=对端重启（其对本端的接收状态已归零），重整发送状态；
		// 旧代残包忽略；首次学习不重整。
		if (control.generation != 0 && control.generation != session.peerControlGeneration.current) {
			switch (session.peerControlGeneration.adopt(control.generation)) {
			case Ignore -> { return; } // 旧代残包
			case Change -> session.rebaseSendState();
			case Learn -> {} // 首次学习，不重整
			}
		}

		switch (control.command) {
		case Control.Ack:
			// 只认针对本端当前代际的确认：重整换代后，对端按旧代际序号空间发的确认不得清除新空间。
			if (control.peerGeneration == session.selfGeneration) {
				for (var it = control.serialIds.iterator(); it.moveToNext(); ) {
					var p = session.sendWindow.remove(it.value());
					if (p != null) // 可能收到重复的Ack：超时重发后对端会再次确认。
						p.resendTimerTask.cancel(false);
				}
			}
			break;

		case Control.Resend:
			// 只认针对本端当前代际的重发请求，理由同 Ack。
			if (control.peerGeneration == session.selfGeneration) {
				for (var it = control.serialIds.iterator(); it.moveToNext(); ) {
					var packet = session.sendWindow.get(it.value());
					if (packet != null)
						sendTo(session.peer, packet);
					// else skip 重发请求的包已经不在Window中，此时可能是重复的Resend请求。
				}
			}
			break;

		case Control.NoSession:
			// 对端已没有本端的会话（如对端重启或 client 模式拒建），不会再按当前序号 Ack；
			// 丢弃在途包并从 1 重新编号——对端将来接受会话时其接收状态必然是全新的，可直接按序接收。
			session.rebaseSendState();
			break;
		}
	}

	@Override
	public void doHandle(SelectionKey key) throws Exception {
		if (key.isReadable()) {
			var buffer = java.nio.ByteBuffer.allocate(MaxPacketLength);
			var source = datagramChannel.receive(buffer);
			if (source != null) {
				// 两参Wrap=（数组,长度）：position是收到的字节数。三参误用（offset=position,length=limit=capacity）
				// 使VerifyArrayIndex恒抛IllegalArgumentException，收包从未工作过（对照DatagramSocket.java:124同型）
				var bb = ByteBuffer.Wrap(buffer.array(), buffer.position());
				// UDP源地址可伪造，包内字节任意。type本身及Packet/Control的ReadLong/ReadBytes/
				// ReadUInt(count)都是变长编码，畸形包必抛异常；异常抛到Selector会无条件关闭
				// 整个channel（Selector对doHandle异常兜底key.channel().close()），单个畸形包
				// 杀掉全部会话。按包捕获，限频warn+计数（每包一条会被打成日志洪水）丢弃。
				try {
					var type = bb.ReadUInt();
					switch (type) {
					case TypePacket:
						processPacket(source, bb);
						break;
					case TypeControl:
						processPacketControl(source, bb);
						break;
					}
				} catch (Exception e) {
					malformedPackets.increment();
					var now = System.currentTimeMillis();
					if (now - lastMalformedWarnTime >= 1000) {
						lastMalformedWarnTime = now;
						logger.warn("malformed udp packets: count={} lastSource={}",
								malformedPackets.sumThenReset(), source, e);
					}
				}
			}
			return;
		}

		throw new IllegalStateException();
	}

	@Override
	public void doException(@NotNull SelectionKey key, @NotNull Throwable e) {
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
			// 取消所有会话的重发定时器，避免泄漏
			for (var session : sessions.values())
				session.cancelResendTimers();
			selectionKey = null;
		} finally {
			unlock();
		}
	}
}
