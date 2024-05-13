package Zeze.Services.ServiceManager;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.FuncLong;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.OutObject;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Id128UdpClient {
	private static final Logger logger = LogManager.getLogger();
	private static final int eSoTimeoutTick = 15;
	private static final int eMaxUdpPacketSize = 256;
	private static final int eSendImmediatelyGuard = 1; // >1会导致Simulate测试跑得太慢,还需要分析原因
	private static final int eRpcTimeoutChecker = 1500;
	private static final int eRpcTimeout = 5000;

	private final AbstractAgent agent;
	private final DatagramSocket udp;
	private final Thread worker;
	private boolean running = true;
	private final ConcurrentHashMap<String, FutureNode> currentFuture = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, FutureNode> tailFuture = new ConcurrentHashMap<>();
	private final LongConcurrentHashMap<AllocateId128> pendingRpc = new LongConcurrentHashMap<>();
	private long lastProcessTickTime = System.currentTimeMillis();
	private long lastRpcTimeoutCheckTime = System.currentTimeMillis();
	private final FuncLong nextSessionIdFunc;

	public Id128UdpClient(AbstractAgent agent, String ip, int port, FuncLong nextSessionIdFunc) throws Exception {
		this.agent = agent;
		this.nextSessionIdFunc = nextSessionIdFunc;

		udp = new DatagramSocket();
		udp.connect(InetAddress.getByName(ip), port);
		udp.setSoTimeout(eSoTimeoutTick);

		worker = new Thread(this::run, "Id128UdpClient");
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
		var outFuture = new OutObject<TaskCompletionSource<Tid128Cache>>();
		currentFuture.compute(globalName, (key, value) -> {
			var current = value;
			if (current == null)
				current = new FutureNode(allocateCount);
			outFuture.value = current; // 后面可能删除,所以需要用这个返回.
			if (current.pending.incrementAndGet() < eSendImmediatelyGuard)
				return current;

			// 下面 1.入队, 2.发送rpc请求, 3.删除.

			// 1.入队
			var funalCurrent = current;
			tailFuture.compute(globalName, (key2, value2) -> {
				funalCurrent.prev = value2; // value maybe null. that is first node.
				return funalCurrent;
			});

			// 2.发送rpc
			try {
				var r = new AllocateId128(current);
				r.setSessionId(nextSessionIdFunc.call());
				r.Argument.setName(globalName);
				r.Argument.setCount(current.count);
				r.setTimeout(eRpcTimeout);
				if (null != pendingRpc.putIfAbsent(r.getSessionId(), r))
					throw new RuntimeException("impossible!");
				var bb = ByteBuffer.Allocate();
				r.encode(bb);
				// udp is connected.
				var udpPacket = new DatagramPacket(bb.Bytes, bb.ReadIndex, bb.size());
				udp.send(udpPacket);
			} catch (Exception e) {
				current.setException(e);
			}

			// 3.删除
			return null;
		});
		return outFuture.value;
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
			} catch (Throwable ex) {
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
				var futureContext = futureNode;
				var tid128Cache = new Tid128Cache(context.Argument.getName(), agent, r.Result.getStartId(), r.Result.getCount());
				do {
					var current = futureNode;
					// 循环中需要再次判断, see eRpcTimeoutChecker, 但是不判断也是可以的,下面代码刚好可以工作.
					if (current.pending.get() > 0) {
						current.setResult(tid128Cache);
						current.pending.set(0); // 用来标记futureNode已经设置过结果.
					}
					futureNode = current.prev;
					current.prev = null; // help gc.
				} while (futureNode != null);
				// 设置完result以后才删除.却表中间异常,队列不会乱.
				// 只需要执行一次,不用到循环里面判断,因为tail总是最后一个.
				tailFuture.compute(r.Argument.getName(), (key, value) -> {
					return (value == futureContext ? null : value);
				});
			}
		}
	}

	// 单线程
	private void processTick() {
		var now = System.currentTimeMillis();
		if (now - lastProcessTickTime >= eSoTimeoutTick) {
			lastProcessTickTime = now;
			var bb = ByteBuffer.Allocate();
			var futureNodesGuard = new ArrayList<FutureNode>();
			try {
				for (var key : currentFuture.keySet()) {
					var futureNode = currentFuture.remove(key);
					if (futureNode != null) { // 并发删除, see allocateFuture
						futureNodesGuard.add(futureNode);
						var r = new AllocateId128(futureNode);
						r.setSessionId(nextSessionIdFunc.call());
						r.Argument.setName(key);
						r.Argument.setCount(futureNode.count);
						r.setTimeout(eRpcTimeout);
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
				futureNodesGuard.clear();
			} catch (Exception ex) {
				for (var futureNode : futureNodesGuard)
					futureNode.setException(ex);
				logger.error("", ex);
			}
		}
		var period = (int)(now - lastRpcTimeoutCheckTime);
		if (period > eRpcTimeoutChecker) {
			lastRpcTimeoutCheckTime = now;
			for (var eIt = pendingRpc.entryIterator(); eIt.moveToNext(); ) {
				var r = eIt.value();
				var remain = r.getTimeout() - period;
				if (remain > 0) {
					r.setTimeout(remain);
					continue;
				}

				// 1. 仅给当前的future报告错误,链表保持不变(残留),以后的设置结果和报错由future保证不会重复.
				// 循环中需要再次判断, see eRpcTimeoutChecker, 但是不判断也是可以的,下面代码刚好可以工作.
				var futureNode = r.getFutureNode();
				if (futureNode.pending.get() > 0) {
					futureNode.setException(new TimeoutException());
					futureNode.pending.set(0); // 用来标记futureNode已经设置过结果.
				}
				pendingRpc.remove(eIt.key());
				/*
				// 2. 这里仅仅为了回收rpc-context,不对futureNode队列进行处理.
				//    如果后面的请求有结果,仍然会得到setResult.
				pendingRpc.remove(eIt.key());

				// 3. 给当前以及之前的报错.之前的从链表中去掉,当前由于没有实现双向链表残留着.
				var futureNode = r.getFutureNode();
				do {
					var current = futureNode;
					// 循环中需要再次判断, see eRpcTimeoutChecker, 但是不判断也是可以的,下面码刚好可以工作.
					if (current.pending.get() > 0) {
						current.setException(new TimeoutException());
						current.pending.set(0);
					}
					futureNode = current.prev;
					current.prev = null;
				} while (futureNode != null);
				pendingRpc.remove(eIt.key());
				*/
			}
		}
	}

	public static void main(String [] args) throws Exception {
		var nextSessionId = new AtomicLong();
		var server = new Id128UdpServer();
		server.start(); // must start first. server.getLocalPort() need.
		var client = new Id128UdpClient(null, "127.0.0.1", server.getLocalPort(), nextSessionId::incrementAndGet);
		client.start();
		try {
			System.out.println(client.allocateFuture("testGlobal1", 128).get().next());
			System.out.println(client.allocateFuture("testGlobal1", 128).get().next());
			var futures = new ArrayList<TaskCompletionSource<Tid128Cache>>();
			futures.add(client.allocateFuture("testGlobal1", 128));
			futures.add(client.allocateFuture("testGlobal1", 128));
			futures.add(client.allocateFuture("testGlobal1", 128));
			futures.add(client.allocateFuture("testGlobal1", 128));
			System.out.println("concurrent-->");
			for (var future : futures)
				System.out.println(future.get().next());
		} finally {
			client.stop();
			server.stop();
		}
	}
}
