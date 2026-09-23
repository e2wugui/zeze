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
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TimeAdaptedFund;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rocksdb.RocksDBException;

/**
 * AllocateId128 server
 */
public class Id128UdpServer {
	private static final @NotNull Logger logger = LogManager.getLogger(Id128UdpServer.class);

	/**
	 * FND6-28：唯一name数量上限（防御注释曾宣称但实现缺失）。该UDP端口无认证，攻击者以
	 * 合法count+海量不同name可无界撑爆cache/RocksDB。合法部署的name数量=History集群名
	 * 数量，量级极小，1024远高于任何合理用量；纯内存部署（table==null）超限拒绝整包，
	 * 持久化部署（table!=null）逐出闲置条目自愈（见evictIdleContext）。
	 */
	public static final int MAX_UNIQUE_NAMES = 1024;

	private static class Id128Context extends FastLock {
		final Id128 current = new Id128();
		final Id128 max = new Id128();
	}

	private final @Nullable RocksDatabase.Table table;
	private final ConcurrentHashMap<Binary, Id128Context> cache = new ConcurrentHashMap<>();
	private final @NotNull TimeAdaptedFund fund = TimeAdaptedFund.getDefaultFund();
	private final @NotNull DatagramChannel udpChannel;
	private final @NotNull Thread worker; // 工作线程，根据性能测试情况，以后可能多个

	public Id128UdpServer() throws IOException {
		this(null, null, 0); // any ip & auto port.
	}

	public Id128UdpServer(@Nullable RocksDatabase.Table table) throws IOException {
		this(table, null, 0); // any ip & auto port.
	}

	public Id128UdpServer(@Nullable RocksDatabase.Table table, @Nullable String host, int port) throws IOException {
		this.table = table;
		udpChannel = DatagramChannel.open();
		udpChannel.configureBlocking(true);
		udpChannel.bind(host == null || host.isBlank()
				? new InetSocketAddress(port)
				: new InetSocketAddress(InetAddress.getByName(host), port));

		worker = new Thread(this::run, "Id128UdpServer");
		worker.setDaemon(true);
		worker.setPriority(Thread.NORM_PRIORITY + 2);
		worker.setUncaughtExceptionHandler((__, e) -> logger.error("uncaught exception:", e));
	}

	public int getLocalPort() throws IOException {
		return getLocalSocketAddress().getPort();
	}

	public @NotNull InetSocketAddress getLocalSocketAddress() throws IOException {
		return (InetSocketAddress)udpChannel.getLocalAddress();
	}

	public void start() throws IOException {
		logger.info("start worker: {}", udpChannel.getLocalAddress());
		worker.start();
	}

	public void stop() throws Exception {
		logger.info("stop begin");
		udpChannel.close();
		// worker.interrupt();
		worker.join();
		logger.info("stop end");
	}

	private void run() {
		logger.info("worker begin");
		var bbRecv = NioByteBuffer.allocate(2048);
		var dbb = bbRecv.getNioByteBuffer();
		var bbSend = ByteBuffer.Allocate(2048);
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
						// SM1-F2：解码异常单独隔离限频并丢弃剩余报文。纯垃圾报文走
						// Rpc.decode的IllegalStateException（非IllegalArgumentException），
						// 原落入共享catch的全栈error按包记录，FND6-28防护在最易构造的攻击
						// 向量上失效。不按异常类型放宽共享catch——那会把encode段内部故障
						// （如RocksDB异常路径上的RuntimeException）也降级为限频单行，违背
						// FND6-28"全栈与告警留给内部故障"的意图；decode输入完全对端可控，
						// 天然只覆盖攻击面。剩余报文不可信（帧边界已错乱），丢弃。
						try {
							rpc.decode(bbRecv);
						} catch (RuntimeException e) {
							warnRejected(e);
							break;
						}
						process(rpc, bbTemp);
						rpc.setRequest(false);
						rpc.encode(bbSend);
					}
				} catch (Exception e) {
					if (e instanceof IllegalArgumentException) {
						// FND6-28补：入口校验拒绝（对端可控输入）限频记一条、不打栈——无认证
						// 端口上高频非法包按包全栈error可耗尽日志盘/CPU（日志刷屏DoS），全栈与
						// 告警留给内部故障（RocksDB异常等）。
						warnRejected(e);
					} else {
						logger.error("process exception:", e);
					}
				}
				int sendSize = bbSend.WriteIndex;
				if (sendSize > 0) {
					dbb.clear();
					dbb.put(bbSend.Bytes, 0, sendSize); // NioByteBuffer还不支持写,这里只能多一次复制
					dbb.flip();
					int r = udpChannel.send(dbb, addr);
					if (r != sendSize)
						logger.error("send failed: r={} != {}", r, sendSize);
				}
			} catch (Throwable e) { // logger.error
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

	private volatile long lastRejectLogMs; // 拒绝告警限频（60秒一次）

	private void warnRejected(@NotNull Throwable e) {
		var now = System.currentTimeMillis();
		var last = lastRejectLogMs;
		if (now - last < 60_000)
			return; // 限频窗口内静默拒绝（race下至多多记几条）
		lastRejectLogMs = now;
		logger.error("AllocateId128 rejected (possible attack or misbehaving client), cached names={}: {}",
				cache.size(), e.toString());
	}

	/**
	 * 逐出一个未持锁的cache条目腾出槽位（FND6-28补：满员自愈）。在途分配持锁不可逐出；
	 * max推进即在锁内table.put持久化，逐出后按需经computeIfAbsent从RocksDB恢复，
	 * 语义等同进程重启加载（put失败即进程重启同样丢失，不引入新损失类别）。
	 * 全部条目持锁（病态并发）时失败。
	 */
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

	private void process(@NotNull AllocateId128 rpc, @NotNull ByteBuffer bbTemp) throws RocksDBException {
		var arg = rpc.Argument;
		var res = rpc.Result;
		var name = arg.getBinaryName();
		var count = arg.getCount();
		// 入口校验（count与name均为对端可控，该UDP端口无认证）：count<=0使号段回退→跨客户端重复tid；
		// 巨量count或无界唯一name撑爆cache/RocksDB。非法抛出，由run()内层catch记日志丢弃整包
		// （诚实客户端报文不与攻击报文共用报文段，不受影响）。
		if (count < 1 || count > Tid128Cache.ALLOCATE_COUNT_MAX)
			throw new IllegalArgumentException("AllocateId128 invalid count=" + count + " name.size=" + name.size());
		if (name.size() > 128)
			throw new IllegalArgumentException("AllocateId128 name too long: size=" + name.size());
		// FND6-28补：新name（cache未命中）满员时不再一律拒绝——持久化部署逐出一个闲置条目
		// 自愈（重启冷却后cache为空，谁先请求谁占槽，一律拒绝会把合法History name锁死在
		// rocks外，rpc超时直击finalCommit主路径）；纯内存部署无恢复手段（逐出即丢状态），
		// 维持拒绝。竞态窗口内可能略超上限（多线程同时computeIfAbsent），有界即可。
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
						ctx.max.decodeRaw(ByteBuffer.Wrap(v));
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
				break; // 持锁且在册：逐出需tryLock本上下文，临界区内不会被逐出。
			}
			// FND6-28补（多worker前瞻）：computeIfAbsent插入后未首次上锁的窗口内，条目可被
			// evictIdleContext逐出、并发同name请求从rocks重建新上下文——孤儿上下文放锁重试。
			// 不闭环则双上下文各持独立current/max分配重叠号段，孤儿的table.put还可能把max
			// 写回旧值导致重载重发已交付区间。当前单worker不可达（worker注释「以后可能多个」）。
			c.unlock();
		}
		try {
			var current = context.current;
			res.getStartId().assign(current);
			current.increment(count);
			var max = context.max;
			if (current.compareTo(max) > 0) {
				// 【FND11 svc-01】先落库再推进内存max（对齐ServiceManagerServer.AutoKey判例
				// "确保数据库记下了再更新max"）：put失败时max原地不动，异常传播丢弃本次响应
				//（current已推进形成号洞，无害）；后续请求仍见current>max，重算含本次终点的新
				// 水位再put——任何成功响应的号段必被已持久化的max覆盖，重启重载不会重复发号。
				// 新水位至少覆盖本次交付终点（此前put失败的请求已把current推到旧max之前）。
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
