package Zeze.Services.ServiceManager;

import java.util.Objects;
import java.io.IOException;
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
import org.jspecify.annotations.Nullable;

/**
 * Id128 UDP发号客户端：按name合并请求批量向Id128UdpServer申请号段，FutureNode链表
 * 等待应答或超时，供Tid128Cache发放。
 */
public class Id128UdpClient {
	private static final Logger logger = LogManager.getLogger(Id128UdpClient.class);
	private static final int eSoTimeoutTick = 15;
	private static final int eMaxUdpPacketSize = 256;
	private static final int eSendImmediatelyGuard = 1; // >1会导致Simulate测试跑得太慢,还需要分析原因
	private static final int eRpcTimeoutChecker = 1500;
	private static final int eRpcTimeout = 5000;

	private final @Nullable AbstractAgent agent;
	private final DatagramSocket udp;
	private final Thread worker = new Thread(this::run, "Id128UdpClient");
	private final ConcurrentHashMap<String, FutureNode> currentFuture = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, FutureNode> tailFuture = new ConcurrentHashMap<>();
	private final LongConcurrentHashMap<AllocateId128> pendingRpc = new LongConcurrentHashMap<>();
	private long lastProcessTickTime = System.currentTimeMillis();
	private long lastRpcTimeoutCheckTime = System.currentTimeMillis();
	private final FuncLong nextSessionIdFunc;

	public Id128UdpClient(@Nullable AbstractAgent agent, String ip, int port, FuncLong nextSessionIdFunc)
			throws IOException {
		this.agent = agent;
		this.nextSessionIdFunc = nextSessionIdFunc;

		var address = InetAddress.getByName(ip);
		udp = new DatagramSocket();
		try {
			udp.connect(address, port);
			udp.setSoTimeout(eSoTimeoutTick);
			worker.setDaemon(true);
			worker.setPriority(Thread.NORM_PRIORITY + 2);
			worker.setUncaughtExceptionHandler((__, e) -> logger.error("uncaught exception", e));
		} catch (IOException | RuntimeException | Error e) {
			udp.close();
			throw e;
		}
	}

	public void start() {
		worker.start();
	}

	public static class FutureNode extends TaskCompletionSource<Tid128Cache> {
		private final int count;
		private final AtomicInteger pending = new AtomicInteger();
		private @Nullable FutureNode prev; // 单向链表,指向前一个

		public FutureNode(int count) {
			this.count = count;
		}
	}

	public synchronized FutureNode allocateFuture(String globalName, int allocateCount) {
		if (udp.isClosed()) {
			var stopped = new FutureNode(allocateCount);
			stopped.setException(new IllegalStateException("client stopped"));
			return stopped;
		}
		// 多次请求的allocateCount不一样,只记住第一次的.
		var outFuture = new OutObject<FutureNode>();
		currentFuture.compute(globalName, (key, value) -> {
			var current = value;
			if (current == null)
				current = new FutureNode(allocateCount);
			outFuture.value = current; // 后面可能删除,所以需要用这个返回.
			if (current.pending.incrementAndGet() < eSendImmediatelyGuard)
				return current;

			// 下面 1.入队, 2.发送rpc请求, 3.删除.

			// 1.入队
			var finalCurrent = current;
			tailFuture.compute(globalName, (key2, value2) -> {
				finalCurrent.prev = value2; // value2 为 null 表示首节点
				return finalCurrent;
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
				// udp 已 connect，无需指定目标地址
				var udpPacket = new DatagramPacket(bb.Bytes, bb.ReadIndex, bb.size());
				udp.send(udpPacket);
				} catch (Exception e) {
					current.setException(e);
					// 标记已完成，避免超时检查器重复处理。
					current.pending.set(0);
				}

			// 3.删除
			return null;
		});
		return Objects.requireNonNull(outFuture.value);
	}

	public void stop() throws Exception {
		// 先关入队，再等worker和清理。关闭后allocateFuture不能再发布待处理节点。
		synchronized (this) {
			udp.close();
		}
		worker.interrupt();
		worker.join();
		// worker 已停止，为所有在途节点补置异常以防调用方永久阻塞。
		var stopped = new IllegalStateException("client stopped");
		for (var futureNode : currentFuture.values())
			completeIfPending(futureNode, stopped);
		currentFuture.clear();
		for (var futureNode : tailFuture.values())
			completeIfPending(futureNode, stopped);
		tailFuture.clear();
		for (var eIt = pendingRpc.entryIterator(); eIt.moveToNext(); )
			completeIfPending(Objects.requireNonNull(eIt.value().getFutureNode()), stopped);
		pendingRpc.clear();
	}

	private static void completeIfPending(FutureNode futureNode, Exception ex) {
		if (futureNode.pending.getAndSet(0) > 0)
			futureNode.setException(ex);
	}

	private void run() {
		var buf = new byte[1472];
		var p = new DatagramPacket(buf, buf.length);

		while (!udp.isClosed()) {
			try {
				udp.receive(p);
				var bb = ByteBuffer.Wrap(p.getData(), p.getOffset(), p.getLength());
				while (!bb.isEmpty()) {
					var rpc = new AllocateId128();
					rpc.decode(bb);
					if (rpc.isRequest() || rpc.Result.getCount() <= 0)
						continue;
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
		if (context != null) {
			var futureNode = context.getFutureNode();
			// 判断是否已经设置过结果.迟到或乱序的rpc.
			//noinspection DataFlowIssue
			if (futureNode.pending.get() > 0) {
				var futureContext = futureNode;
				// 响应rpc只解码Result,Argument保持默认空值,所以这里只能用请求对象的name.
				var globalName = context.Argument.getName();
				var tid128Cache = new Tid128Cache(globalName, agent,
						r.Result.getStartId(), r.Result.getCount());
				do {
					var current = futureNode;
					// 循环中再次检查以避开超时检查器已完成的节点。
					if (current.pending.get() > 0) {
						current.setResult(tid128Cache);
						current.pending.set(0); // 标记已设置结果
					}
					futureNode = current.prev;
					current.prev = null; // 断开引用便于 GC
				} while (futureNode != null);
				// 设置完 result 后再摘除尾节点，确保异常时队列一致。
				tailFuture.compute(globalName, (key, value) -> value == futureContext ? null : value);
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
			// 记录已成功发出的节点边界，send 异常时仅对未发出的 [sentCount, size) 节点报错。
			int sentCount = 0;
			try {
				for (var key : currentFuture.keySet()) {
					var futureNode = currentFuture.remove(key);
					if (futureNode != null) { // 可能被 allocateFuture 并发删除
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
							var udpPacket = new DatagramPacket(bb.Bytes, bb.ReadIndex, bb.size());
							udp.send(udpPacket);
							sentCount = futureNodesGuard.size(); // 推进已发送边界
							bb.ReadIndex = 0;
							bb.WriteIndex = 0;
						}
					}
				}
				if (!bb.isEmpty()) {
					var udpPacket = new DatagramPacket(bb.Bytes, bb.ReadIndex, bb.size());
					udp.send(udpPacket);
					sentCount = futureNodesGuard.size();
				}
				futureNodesGuard.clear();
			} catch (Exception ex) {
				for (var futureNode : futureNodesGuard.subList(sentCount, futureNodesGuard.size())) {
					futureNode.setException(ex);
					futureNode.pending.set(0);
				}
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

				// 仅使本请求超时；其前驱仍可能由各自响应完成。扫描结束后统一摘除已完成节点。
				var futureNode = r.getFutureNode();
				//noinspection DataFlowIssue
				if (futureNode.pending.get() > 0) {
					futureNode.setException(new TimeoutException());
					futureNode.pending.set(0); // 标记已设置结果
				}
				pendingRpc.remove(eIt.key());
			}
			// 清理已超时/失败节点以防 tailFuture 无限累积，同时保留仍在途的节点。
			for (var name : tailFuture.keySet()) {
				tailFuture.computeIfPresent(name, (__, tail) -> {
					while (tail != null && tail.pending.get() <= 0) {
						var previous = tail.prev;
						tail.prev = null;
						tail = previous;
					}
					for (var node = tail; node != null; node = node.prev) {
						while (node.prev != null && node.prev.pending.get() <= 0) {
							var completed = node.prev;
							node.prev = completed.prev;
							completed.prev = null;
						}
					}
					return tail;
				});
			}
		}
	}

	public static void main(String[] args) throws Exception {
		var nextSessionId = new AtomicLong();
		var server = new Id128UdpServer();
		server.start(); // 需先启动以绑定端口
		var client = new Id128UdpClient(null, "127.0.0.1", server.getLocalPort(), nextSessionId::incrementAndGet);
		client.start();
		try {
			System.out.println(client.allocateFuture("testGlobal1", 128).getNonNull().next());
			System.out.println(client.allocateFuture("testGlobal1", 128).getNonNull().next());
			var futures = new ArrayList<TaskCompletionSource<Tid128Cache>>();
			futures.add(client.allocateFuture("testGlobal1", 128));
			futures.add(client.allocateFuture("testGlobal1", 128));
			futures.add(client.allocateFuture("testGlobal1", 128));
			futures.add(client.allocateFuture("testGlobal1", 128));
			System.out.println("concurrent-->");
			for (var future : futures)
				System.out.println(future.getNonNull().next());
		} finally {
			client.stop();
			server.stop();
		}
	}
}
