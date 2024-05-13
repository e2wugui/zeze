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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rocksdb.RocksDBException;

/**
 * AllocateId128 server
 */
public class Id128UdpServer {
	private static final @NotNull Logger logger = LogManager.getLogger();

	private static class Id128WithLock extends Id128 {
		final FastLock lock = new FastLock();

		@Override
		public Id128WithLock clone() {
			throw new AssertionError(new CloneNotSupportedException());
		}
	}

	private final @Nullable RocksDatabase.Table table;
	private final ConcurrentHashMap<Binary, Id128WithLock> cache = new ConcurrentHashMap<>();
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
		var bytes16 = new byte[16];
		for (; ; ) {
			try {
				dbb.clear();
				var addr = udpChannel.receive(dbb);
				dbb.flip();
				bbSend.Reset();
				try {
					while (!bbRecv.isEmpty()) {
						rpc.decode(bbRecv);
						process(rpc, bytes16);
						rpc.setRequest(false);
						rpc.encode(bbSend);
					}
				} catch (Exception e) {
					logger.error("process exception:", e);
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

	private void process(@NotNull AllocateId128 rpc, byte @NotNull [] bytes16) throws RocksDBException {
		var arg = rpc.Argument;
		var res = rpc.Result;
		var name = arg.getBinaryName();
		var count = arg.getCount();
		var id128 = cache.computeIfAbsent(name, k -> {
			var id = new Id128WithLock();
			try {
				var v = table != null ? table.get(k.bytesUnsafe()) : null;
				if (v != null)
					id.decode16(v);
				else
					id.increment(1); // 从1开始分配
			} catch (RocksDBException e) {
				Task.forceThrow(e);
			}
			return id;
		});
		id128.lock.lock();
		try {
			res.getStartId().assign(id128);
			id128.increment(count);
			if (table != null) { // 以后再考虑能不能移到锁外
				id128.encode16(bytes16);
				table.put(name.bytesUnsafe(), bytes16);
			}
		} finally {
			id128.lock.unlock();
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
