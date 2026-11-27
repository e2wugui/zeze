package Zeze.Services.ServiceManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.NioByteBuffer;
import Zeze.Util.FastLock;
import Zeze.Util.Id128;
import Zeze.Util.Task;
import Zeze.Util.TimeAdaptedFund;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import org.rocksdb.RocksDBException;
import Zeze.Util.RocksDatabase.Table;

/**
 * Id128 UDP 发号服务端：接收批量 AllocateId128 请求并按 name 递增分配号段，可选经 RocksDB 持久化水位。
 */
public class Id128UdpServer {
	private static final Logger logger = LogManager.getLogger(Id128UdpServer.class);

	/** 唯一 name 缓存上限：纯内存模式超限拒绝，持久化模式逐出闲置条目后按需从 RocksDB 恢复。 */
	public static final int MAX_UNIQUE_NAMES = 1024;

	private static class Id128Context extends FastLock {
		final Id128 current = new Id128();
		final Id128 max = new Id128();
	}

	private final @Nullable Table table;
	private final ConcurrentHashMap<Binary, Id128Context> cache = new ConcurrentHashMap<>();
	private final TimeAdaptedFund fund = TimeAdaptedFund.getDefaultFund();
	private final DatagramChannel udpChannel;
	private final Thread worker; // 工作线程，根据性能测试情况，以后可能多个

	public Id128UdpServer() throws IOException {
		this(null, null, 0); // 任意地址与自动分配端口
	}

	public Id128UdpServer(@Nullable Table table) throws IOException {
		this(table, null, 0); // 任意地址与自动分配端口
	}

	public Id128UdpServer(@Nullable Table table, @Nullable String host, int port) throws IOException {
		this.table = table;
		var address = host == null || host.isBlank()
				? new InetSocketAddress(port)
				: new InetSocketAddress(InetAddress.getByName(host), port);
		udpChannel = DatagramChannel.open();
		try {
			udpChannel.configureBlocking(true);
			udpChannel.bind(address);
			worker = new Thread(this::run, "Id128UdpServer");
			worker.setDaemon(true);
			worker.setPriority(Thread.NORM_PRIORITY + 2);
			worker.setUncaughtExceptionHandler((__, e) -> logger.error("uncaught exception:", e));
		} catch (IOException | RuntimeException | Error e) {
			try {
				udpChannel.close();
			} catch (IOException closeError) {
				e.addSuppressed(closeError);
			}
			throw e;
		}
	}

	public int getLocalPort() throws IOException {
		return getLocalSocketAddress().getPort();
	}

	public InetSocketAddress getLocalSocketAddress() throws IOException {
		return (InetSocketAddress)udpChannel.getLocalAddress();
	}

	public void start() throws IOException {
		logger.info("start worker: {}", udpChannel.getLocalAddress());
		worker.start();
	}

	public void stop() throws Exception {
		logger.info("stop begin");
		udpChannel.close();
		worker.join();
		logger.info("stop end");
	}

	private void run() {
		logger.info("worker begin");
		var bbRecv = NioByteBuffer.allocate(2048);
		var dbb = bbRecv.getNioByteBuffer();
		var bbSend = ByteBuffer.Allocate(2048);
		var bbReply = ByteBuffer.Allocate(64);
		var rpc = new AllocateId128();
		var bbTemp = ByteBuffer.Allocate(32);
		for (; ; ) {
			try {
				dbb.clear();
				var addr = udpChannel.receive(dbb);
				dbb.flip();
				bbSend.Reset();
				try {
					while (!bbRecv.isEmpty()) {
						// 解码异常归为非法输入限频告警并丢弃当前报文的剩余帧，避免与内部处理故障混淆。
						try {
							rpc.decode(bbRecv);
						} catch (RuntimeException e) {
							warnRejected(e);
							break;
						}
						if (!rpc.isRequest()) {
							warnRejected(new IllegalArgumentException("AllocateId128 expected request"));
							break;
						}
						process(rpc, bbTemp);
						rpc.setRequest(false);
						bbReply.Reset();
						rpc.encode(bbReply);
						// 老客户端按1472字节接收；回复比请求更长，按完整RPC边界拆包，不能使用接收缓冲容量发送。
						if (bbSend.size() + bbReply.size() > 1472) {
							sendReplies(bbSend, addr);
							bbSend.Reset();
						}
						bbSend.Append(bbReply.Bytes, bbReply.ReadIndex, bbReply.size());
					}
				} catch (Exception e) {
					if (e instanceof IllegalArgumentException) {
						// 入口校验拒绝限频告警不打栈，防高频非法包刷爆日志。
						warnRejected(e);
					} else {
						logger.error("process exception:", e);
					}
				}
				sendReplies(bbSend, addr);
			} catch (Throwable e) {
				if (!udpChannel.isOpen()) {
					logger.info("{}: {}", e.getClass().getName(), e.getMessage());
					break;
				}
				logger.error("worker exception:", e);
				if (e instanceof Error)
					break;
			}
		}
		logger.info("worker end");
	}

	private void sendReplies(ByteBuffer replies, java.net.SocketAddress address) throws IOException {
		int size = replies.size();
		if (size > 0) {
			int sent = udpChannel.send(java.nio.ByteBuffer.wrap(replies.Bytes, replies.ReadIndex, size), address);
			if (sent != size)
				logger.error("send failed: r={} != {}", sent, size);
		}
	}

	private volatile long lastRejectLogMs; // 拒绝告警限频（60秒一次）

	private void warnRejected(Throwable e) {
		var now = System.currentTimeMillis();
		var last = lastRejectLogMs;
		if (now - last < 60_000)
			return; // 限频窗口内静默跳过
		lastRejectLogMs = now;
		logger.error("AllocateId128 rejected (possible attack or misbehaving client), cached names={}: {}",
				cache.size(), e.toString());
	}

	/** 尝试逐出一个未持锁的缓存条目以腾出槽位；全员持锁时返回 false。 */
	private boolean evictIdleContext() {
		for (var e : cache.entrySet()) {
			var context = e.getValue();
			if (context.tryLock()) {
				try {
					if (cache.remove(e.getKey(), context))
						return true;
				} finally {
					context.unlock();
				}
			}
		}
		return false;
	}

	private void process(AllocateId128 rpc, ByteBuffer bbTemp) throws RocksDBException {
		var arg = rpc.Argument;
		var res = rpc.Result;
		var name = arg.getBinaryName();
		var count = arg.getCount();
		// 入口校验（无认证 UDP 端口）：限制非法 count、过长 name 及无界唯一 name。
		if (count < 1 || count > Tid128Cache.ALLOCATE_COUNT_MAX)
			throw new IllegalArgumentException("AllocateId128 invalid count=" + count + " name.size=" + name.size());
		if (name.size() > 128)
			throw new IllegalArgumentException("AllocateId128 name too long: size=" + name.size());
		// 持久化模式满员时尝试逐出闲置条目自愈，纯内存模式直接拒绝。
		if (!cache.containsKey(name) && cache.size() >= MAX_UNIQUE_NAMES
				&& (table == null || !evictIdleContext()))
			throw new IllegalArgumentException("AllocateId128 unique names exceeded " + MAX_UNIQUE_NAMES
					+ " name.size=" + name.size());
		final Id128Context context;
		for (; ; ) {
			var c = cache.computeIfAbsent(name, k -> {
				var ctx = new Id128Context();
				try {
					var v = table != null ? table.get(k.bytesUnsafe()) : null;
					if (v != null) {
						ctx.max.assign(Id128.readRawFrom(ByteBuffer.Wrap(v)));
						ctx.current.assign(ctx.max);
					}
				} catch (RocksDBException e) {
					throw Task.forceThrow(e);
				}
				return ctx;
			});
			c.lock();
			if (cache.get(name) == c) {
				context = c;
				break; // 持锁且在册：逐出需 tryLock 本上下文，临界区内不会被逐出。
			}
			// 若加锁前已被逐出并重建，释放孤儿锁并重试，防止双上下文发放重叠号段。
			c.unlock();
		}
		try {
			var current = context.current;
			res.getStartId().assign(current);
			current.increment(count);
			var max = context.max;
			if (current.compareTo(max) > 0) {
				// 先持久化新水位再推进内存 max，确保已交付号段必被持久化水位覆盖。
				var newMax = max.clone();
				newMax.increment(Math.max(fund.next(), count));
				if (newMax.compareTo(current) < 0)
					newMax.assign(current);
				bbTemp.Reset();
				newMax.encodeRaw(bbTemp);
				if (table != null)
					table.put(name.bytesUnsafe(), 0, name.size(), bbTemp.Bytes, 0, bbTemp.WriteIndex);
				max.assign(newMax);
			}
		} finally {
			context.unlock();
		}
		res.setCount(count);
	}

	public static void main(String[] args) throws Exception {
		var server = new Id128UdpServer();
		server.start();
		var serverSocketAddress = new InetSocketAddress("127.0.0.1", server.getLocalPort());

		try (var client = new DatagramSocket()) {
			var r = new AllocateId128();
			r.Argument.setName("test");
			r.Argument.setCount(100);
			var bbr = ByteBuffer.Allocate();
			for (int j = 0; j < 2; j++)
				r.encode(bbr);
			for (int i = 0; i < 2; i++) {
				var req = new DatagramPacket(bbr.Bytes, bbr.ReadIndex, bbr.size(), serverSocketAddress);
				client.send(req);

				var buf = new byte[2048];
				var p = new DatagramPacket(buf, buf.length);
				client.receive(p);

				var bb = ByteBuffer.Wrap(p.getData(), p.getOffset(), p.getLength());
				var rr = new AllocateId128();
				for (int j = 0; j < 2; j++) {
					rr.decode(bb);
					logger.info("{}", rr.Result);
				}
			}
		} catch (Exception ex) {
			logger.error("main exception:", ex);
		} finally {
			logger.info("main stop");
			server.stop();
			logger.info("main end");
		}
	}
}
