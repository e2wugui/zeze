package Zeze.Services.ServiceManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.OutInt;
import Zeze.Util.OutObject;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Id128UdpClient {
	private static final Logger logger = LogManager.getLogger();
	private static final int eSoTimeoutTick = 15;
	private static final int eMaxUdpPacketSize = 256;

	private final AbstractAgent agent;
	private final DatagramSocket udp;
	private final Thread worker;
	private boolean running = true;
	private final ConcurrentHashMap<String, FutureNode> currentFuture = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, FutureNode> tailFuture = new ConcurrentHashMap<>();
	private final LongConcurrentHashMap<AllocateId128> pendingRpc = new LongConcurrentHashMap<>();
	private long lastProcessTickTime = System.currentTimeMillis();
	private final Service service;

	public Id128UdpClient(AbstractAgent agent, int port, Service service) throws Exception {
		this.agent = agent;
		this.service = service;

		// 查找smAgent的Service，使用其中第一个Connector的信息。
		var outIp = new OutObject<String>();
		var outPort = new OutInt();
		service.getConfig().forEachConnector2((connector -> {
			outIp.value = connector.getHostNameOrAddress();
			outPort.value = connector.getPort();
			return false;
		}));
		udp = new DatagramSocket();
		udp.connect(InetAddress.getByName(outIp.value), port == 0 ? outPort.value : port);
		udp.setSoTimeout(eSoTimeoutTick);

		worker = new Thread(this::run, "Id128UdpServer");
		worker.setDaemon(true);
		worker.setPriority(Thread.NORM_PRIORITY + 2);
		worker.setUncaughtExceptionHandler((__, e) -> logger.error("uncaught exception", e));
	}

	public void start() {
		worker.start();
	}

	public static class FutureNode extends TaskCompletionSource<Tid128Cache> {
		private final int count;
		private final AtomicInteger pending = new AtomicInteger();
		private FutureNode prev; // 单向链表,指向前一个.

		public FutureNode(int count) {
			this.count = count;
		}
	}

	public TaskCompletionSource<Tid128Cache> allocateFuture(String globalName, int allocateCount) {
		// 多次请求的allocateCount不一样,只记住第一次的.
		var current = currentFuture.computeIfAbsent(globalName, __ -> new FutureNode(allocateCount));
		if (current.pending.incrementAndGet() >= 5) {
			var futureNodeGet = currentFuture.get(globalName);
			tailFuture.compute(globalName, (key, value) -> {
				if (value == futureNodeGet)
					return value; // 并发allocate,自己已经是tail,保持不变.
				futureNodeGet.prev = value; // value maybe null. that is first node.
				return futureNodeGet;
			});
			// todo 并发确认! currentFuture.remove,tailFuture.compute应该是一个原子操作,这样写不需要加锁吧?
			//  或者currentFuture一开始就compute,代码全部写在里面?
			var futureNode = currentFuture.remove(globalName);
			if (null != futureNode) { // concurrent remove. maybe null.
				var r = new AllocateId128(futureNode);
				r.setSessionId(service.nextSessionId());
				r.Argument.setName(globalName);
				r.Argument.setCount(futureNode.count);
				if (null != pendingRpc.putIfAbsent(r.getSessionId(), r))
					throw new RuntimeException("impossible!");
				var bb = ByteBuffer.Allocate();
				r.encode(bb);
				// udp is connected.
				var udpPacket = new DatagramPacket(bb.Bytes, bb.ReadIndex, bb.size());
				try {
					udp.send(udpPacket);
				} catch (IOException e) {
					futureNode.setException(e);
					throw new RuntimeException(e);
				}
			}
		}
		return current;
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
			try {
				processTick();
			} catch (Exception ex) {
				logger.error("", ex);
			}
		}
	}

	// 单线程
	private void processResult(AllocateId128 r) {
		var context = pendingRpc.remove(r.getSessionId());
		if (null != context) {
			var futureNode = context.getFutureNode();
			// 判断是否已经设置过结果.迟到或乱序的rpc.
			if (futureNode.pending.get() > 0) {
				var ffn = futureNode;
				// todo [确认] 如果tail是当前futureNode,则删除(置null也行).
				// 只需要执行一次,不用到循环里面判断,因为tail总是最后一个.
				tailFuture.compute(r.Argument.getName(), (key, value) -> {
					return (value == ffn ? null : value);
				});
				var tid128Cache = new Tid128Cache(context.Argument.getName(), agent, r.Result.getStartId(), r.Result.getCount());
				do {
					futureNode.pending.set(0); // 用来标记futureNode已经设置过结果.
					futureNode.setResult(tid128Cache);
					var current = futureNode;
					futureNode = futureNode.prev;
					current.prev = null; // help gc.
				} while (futureNode != null);
			}
		}
	}

	// 单线程
	private void processTick() throws Exception {
		var now = System.currentTimeMillis();
		var period = now - lastProcessTickTime;
		if (period >= eSoTimeoutTick) {
			lastProcessTickTime = now;
			var bb = ByteBuffer.Allocate();
			for (var key : currentFuture.keySet()) {
				var futureNode = currentFuture.remove(key);
				if (futureNode != null) { // 实际上不可能为null.只有这里会删除,单线程.
					var r = new AllocateId128(futureNode);
					r.setSessionId(service.nextSessionId());
					r.Argument.setName(key);
					r.Argument.setCount(futureNode.count);
					if (null != pendingRpc.putIfAbsent(r.getSessionId(), r))
						throw new RuntimeException("impossible!");
					r.encode(bb);
					if (bb.size() > eMaxUdpPacketSize) {
						// udp is connected.
						var udpPacket = new DatagramPacket(bb.Bytes, bb.ReadIndex, bb.size());
						udp.send(udpPacket);
						// clear
						bb.ReadIndex = 0;
						bb.WriteIndex = 0;
					}
				}
			}
			if (!bb.isEmpty()) {
				// udp is connected.
				var udpPacket = new DatagramPacket(bb.Bytes, bb.ReadIndex, bb.size());
				udp.send(udpPacket);
			}
		}
	}

}
