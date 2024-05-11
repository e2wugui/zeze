package Zeze.Services.ServiceManager;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Id128;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * AllocateId128 server
 */
public class Id128UdpServer {
	private static final Logger logger = LogManager.getLogger();

	private final DatagramSocket udp;
	private final Thread worker; // 工作线程，根据性能测试情况，以后可能多个
	private boolean running = true;

	public Id128UdpServer() throws Exception {
		this(null, 0); // any ip & auto port.
	}

	public Id128UdpServer(String ip, int port) throws Exception {
		udp = null == ip || ip.isBlank()
				? new DatagramSocket(port)
				: new DatagramSocket(port, InetAddress.getByName(ip));

		worker = new Thread(this::run, "Id128UdpServer");
		worker.setDaemon(true);
		worker.setPriority(Thread.NORM_PRIORITY + 2);
		worker.setUncaughtExceptionHandler((__, e) -> logger.error("uncaught exception", e));
	}

	public int getLocalPort() {
		return udp.getLocalPort();
	}

	public SocketAddress getLocalSocketAddress() {
		return udp.getLocalSocketAddress();
	}

	public void start() {
		worker.start();
	}

	public void stop() throws Exception {
		running = false;
		udp.close();
		worker.interrupt();
		worker.join();
	}

	private void run() {
		var buf = new byte[256];
		var p = new DatagramPacket(buf, buf.length);
		while (running) {
			try {
				udp.receive(p);
				var bb = ByteBuffer.Wrap(p.getData(), p.getOffset(), p.getLength());
				while (!bb.isEmpty()) {
					var rpc = new AllocateId128();
					rpc.decode(bb);
					process(rpc);
					var bbr = ByteBuffer.Allocate();
					rpc.setRequest(false);
					rpc.encode(bbr);
					var result = new DatagramPacket(bbr.Bytes, bbr.ReadIndex, bbr.size(), p.getSocketAddress());
					udp.send(result);
				}
			} catch (SocketException ignored) {
			} catch (Exception ex) {
				logger.warn("", ex);
			}
		}
	}

	Id128 current = new Id128();
	private void process(AllocateId128 r) {
		// 单线程，临时写来测试。
		current.increment(r.Argument.getCount());
		r.Result.setStartId(current); // 直接引用了，多线程的话需要一个拷贝!!!
		r.Result.setName(r.Argument.getName());
		r.Result.setCount(r.Argument.getCount());
	}

	public static void main(String [] args) throws Exception {
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

				System.out.println(rr.Result);
			}
			{
				var req = new DatagramPacket(bbr.Bytes, bbr.ReadIndex, bbr.size(), serverSocketAddress);
				client.send(req);

				var buf = new byte[256];
				var p = new DatagramPacket(buf, buf.length);
				client.receive(p);
				var rr = new AllocateId128();
				rr.decode(ByteBuffer.Wrap(p.getData(), p.getOffset(), p.getLength()));

				System.out.println(rr.Result);
			}
		} catch (Exception ex) {
			logger.error("", ex);
		} finally {
			server.stop();
		}
	}
}
