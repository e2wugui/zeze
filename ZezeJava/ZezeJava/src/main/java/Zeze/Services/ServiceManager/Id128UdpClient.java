package Zeze.Services.ServiceManager;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.IdentityHashSet;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.OutInt;
import Zeze.Util.OutObject;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Id128UdpClient {
	private static final Logger logger = LogManager.getLogger();
	private static final int eSoTimeoutTick = 15;

	private final DatagramSocket udp;
	private final Thread worker;
	private boolean running = true;
	private final ConcurrentHashMap<String, TaskCompletionSource<Tid128Cache>> currentFuture
			= new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, IdentityHashSet<TaskCompletionSource<Tid128Cache>>> pending
			= new ConcurrentHashMap<>();

	public Id128UdpClient(int port, Service service) throws Exception {
		// 查找smAgent的Service，使用其中第一个Connector的信息。
		var outIp = new OutObject<String>();
		var outPort = new OutInt();
		service.getConfig().forEachConnector2((connector -> {
			outIp.value = connector.getHostNameOrAddress();
			outPort.value = connector.getPort();
			return false;
		}));
		udp = new DatagramSocket(port == 0 ? outPort.value : port, InetAddress.getByName(outIp.value));
		udp.setSoTimeout(eSoTimeoutTick);

		worker = new Thread(this::run, "Id128UdpServer");
		worker.setDaemon(true);
		worker.setPriority(Thread.NORM_PRIORITY + 2);
		worker.setUncaughtExceptionHandler((__, e) -> logger.error("uncaught exception", e));
	}

	public void start() {
		worker.start();
	}

	public TaskCompletionSource<Tid128Cache> allocateFuture(String globalName) {
		return currentFuture.computeIfAbsent(globalName, __ -> new TaskCompletionSource<>());
	}

	public void stop() throws Exception {
		running = false;
		udp.close();
		worker.interrupt();
		worker.join();
	}

	private void run() {
		var buf = new byte[1472];
		var p = new DatagramPacket(buf, buf.length);

		while (running) {
			try {
				udp.receive(p);
				var bb = ByteBuffer.Wrap(p.getData(), p.getOffset(), p.getLength());
				while (!bb.isEmpty()) {
					var rpc = new AllocateId128();
					rpc.decode(bb); // rpc result.
					processResult(rpc);
				}
			} catch (SocketTimeoutException | SocketException ignored) {
			} catch (Exception ex) {
				logger.warn("", ex);
			}
			processTick();
		}
	}

	private void processResult(AllocateId128 r) {

	}

	private void processTick() {

	}

}
