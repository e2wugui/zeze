package Zeze.Services.ServiceManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
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
	private final @NotNull DatagramSocket udp;
	private final @NotNull Thread worker; // 工作线程，根据性能测试情况，以后可能多个

	public Id128UdpServer() throws IOException {
		this(null, null, 0); // any ip & auto port.
	}

	public Id128UdpServer(@Nullable RocksDatabase.Table table) throws IOException {
		this(table, null, 0); // any ip & auto port.
	}

	public Id128UdpServer(@Nullable RocksDatabase.Table table, @Nullable String ip, int port) throws IOException {
		this.table = table;
		udp = null == ip || ip.isBlank()
				? new DatagramSocket(port)
				: new DatagramSocket(port, InetAddress.getByName(ip));

		worker = new Thread(this::run, "Id128UdpServer");
		worker.setDaemon(true);
		worker.setPriority(Thread.NORM_PRIORITY + 2);
		worker.setUncaughtExceptionHandler((__, e) -> logger.error("uncaught exception:", e));
	}

	public int getLocalPort() {
		return udp.getLocalPort();
	}

	public SocketAddress getLocalSocketAddress() {
		return udp.getLocalSocketAddress();
	}

	public void start() {
		logger.info("start worker: {}:{}", udp.getLocalAddress(), udp.getLocalPort());
		worker.start();
	}

	public void stop() throws InterruptedException {
		logger.info("stop begin");
		udp.close();
		// worker.interrupt();
		worker.join();
		logger.info("stop end");
	}

	private void run() {
		logger.info("worker begin");
		var bbRecv = ByteBuffer.Allocate(2048);
		var bbSend = ByteBuffer.Allocate(2048);
		var packet = new DatagramPacket(bbRecv.Bytes, bbRecv.capacity());
		var rpc = new AllocateId128();
		var bytes16 = new byte[16];
		for (; ; ) {
			try {
				packet.setData(bbRecv.Bytes);
				udp.receive(packet);
				bbRecv.ReadIndex = packet.getOffset();
				bbRecv.WriteIndex = bbRecv.ReadIndex + packet.getLength();
				try {
					bbSend.Reset();
					while (!bbRecv.isEmpty()) {
						rpc.decode(bbRecv);
						process(rpc, bytes16);
						rpc.setRequest(false);
						rpc.encode(bbSend);
					}
					packet.setData(bbSend.Bytes, 0, bbSend.WriteIndex);
					udp.send(packet);
				} catch (Exception e) {
					logger.error("process exception:", e);
				}
			} catch (Throwable e) { // logger.error
				if (udp.isClosed()) {
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

	private void process(@NotNull AllocateId128 rpc, byte @NotNull [] bytes16) throws Exception {
		var arg = rpc.Argument;
		var res = rpc.Result;
		var name = arg.getBinaryName();
		var count = arg.getCount();
		var id128 = cache.computeIfAbsent(name, k -> {
			var id = new Id128WithLock();
			try {
				var v = table != null ? table.get(k.bytesUnsafe()) : null;
				if (v != null && v.length >= 16)
					id.decode(v);
			} catch (RocksDBException e) {
				Task.forceThrow(e);
			}
			return id;
		});
		id128.lock.lock();
		try {
			id128.increment(count);
			res.getStartId().assign(id128);
			id128.encode(bytes16);
			if (table != null)
				table.put(name.bytesUnsafe(), bytes16); // 以后再考虑能不能移到锁外
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
			r.Argument.setCount(128);
			var bbr = ByteBuffer.Allocate();
			r.encode(bbr);
			{
				var req = new DatagramPacket(bbr.Bytes, bbr.ReadIndex, bbr.size(), serverSocketAddress);
				client.send(req);

				var buf = new byte[256];
				var p = new DatagramPacket(buf, buf.length);
				client.receive(p);
				var rr = new AllocateId128();
				rr.decode(ByteBuffer.Wrap(p.getData(), p.getOffset(), p.getLength()));

				logger.info("{}", rr.Result);
			}
			{
				var req = new DatagramPacket(bbr.Bytes, bbr.ReadIndex, bbr.size(), serverSocketAddress);
				client.send(req);

				var buf = new byte[256];
				var p = new DatagramPacket(buf, buf.length);
				client.receive(p);
				var rr = new AllocateId128();
				rr.decode(ByteBuffer.Wrap(p.getData(), p.getOffset(), p.getLength()));

				logger.info("{}", rr.Result);
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
