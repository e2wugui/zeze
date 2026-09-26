package Zeze.MQ;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.BSendMessage;
import Zeze.Builtin.MQ.PushMessage;
import Zeze.Net.AsyncSocket;
import Zeze.Util.OutLong;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import static Zeze.MQ.Master.AbstractMaster.eConsumerNotFound;

public class MQSingle extends ReentrantLock {
	private static final Logger logger = LogManager.getLogger();

	private final String topic;
	private final int partitionIndex;
	private long bindSessionId;
	private @Nullable AsyncSocket bindSocket;
	private @Nullable PushMessage pendingPushMessage;
	private final MQPartition mqPartition;
	private final MQFileWithIndex fileWithIndex;
	private long highLoad;
	private final AtomicLong loadCounter = new AtomicLong();
	private long lastLoadCounter;
	private long lastReportTime = System.currentTimeMillis();

	public static final int maxFillMessageCount = 4 * 1024;

	private final Queue<BMessage.Data> messageQueue = new ConcurrentLinkedQueue<>();
	private volatile Future<?> messageFillFuture;
	//private final Future<?> fillGuardTimer;

	public MQPartition getMQPartition() {
		return mqPartition;
	}

	public MQSingle(MQPartition partition, String topic, int partitionId) {
		this(partition, topic, partitionId, createFileWithIndex(partition, topic, partitionId));
	}

	private static MQFileWithIndex createFileWithIndex(MQPartition partition, String topic, int partitionId) {
		try {
			return new MQFileWithIndex(
					partition.getManager().getHome(),
					partition.getManager().getRocksDatabase(),
					topic, partitionId);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	// 包内可见：测试注入MQFileWithIndex（见ZezeJavaTest的TestMQSingleDirectEnqueue）；行为与公有构造一致。
	MQSingle(MQPartition partition, String topic, int partitionId, MQFileWithIndex fileWithIndex) {
		this.mqPartition = partition;
		this.topic = topic;
		this.partitionIndex = partitionId;
		try {
			this.fileWithIndex = fileWithIndex;
			this.highLoad = fileWithIndex.getNextMessageId() - fileWithIndex.getFirstMessageId();
			pullMessage(); // 构造的时候还没有绑定网络，所以只装载进来，不需要tryPushMessage.
			//fillGuardTimer = Task.scheduleNow(5_000, 5_000, this::tryStartBackgroundFill);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public double load() {
		var now = System.currentTimeMillis();
		var elapse = (now - lastReportTime) / 1000.0f;
		lastReportTime = now;
		var load = loadCounter.get();
		var diffLoad = load - lastLoadCounter;
		if (diffLoad > 0) {
			lastLoadCounter = load;
			return diffLoad / elapse;
		}
		return 0.0;
	}

	public void sendMessage(BSendMessage.Data message) {
		lock();
		try {
			// 停机窗口的竞态收口：handler 入口的 stopped 检查通过后 stop() 仍可能先完成（worker
			// 池任务滞后执行），这里在锁内复查——close 持同锁关文件流后，晚到任务必经此处拒绝。
			if (managerStopped())
				throw new IllegalStateException("mq manager stopped, reject sendMessage. topic=" + topic);
			// 【不变量】内存队列必须恰好是盘上积压[firstMessageId,nextMessageId)的连续前缀
			// （队头id==firstMessageId）。仅当队列已装载全部积压时才允许直入：此时盘上没有
			// 待回填消息，也不存在还会向队尾追加的后台回填（回填一旦还有消息未装载完，
			// 队列大小必小于积压数，条件不成立），直入不会破坏顺序。判断必须在appendMessage
			// 之前做（append会推进nextMessageId）。
			// 旧条件highLoad==0在"pullMessage已用calculateFill把highLoad减到0、但锁外
			// fillMessage还没装载完盘上积压"的窗口内也成立，此时直入会插到积压之前：
			// 分区内乱序，且ack后increaseFirstMessageId盲目+1越过未投递消息，重启后消息永久丢失。
			// nextMessageId/firstMessageId的所有写点（appendMessage/increaseFirstMessageId）
			// 都在本锁内执行，这里锁内读取是精确的。
			var directEnqueue = messageQueue.size() < maxFillMessageCount
					&& messageQueue.size() == fileWithIndex.getNextMessageId() - fileWithIndex.getFirstMessageId();
			fileWithIndex.appendMessage(message.getMessage());
			if (directEnqueue) {
				// 低负载，缓冲足够大，直接进入缓冲。
				messageQueue.offer(message.getMessage());
			} else {
				highLoad++;
				// 盘上出现未装载积压：尝试启动回填。fill 失败复位后的重试事件源除了 ack 回调，
				// 还需要这里——队列耗尽后 ack 链不再产生事件，只有新消息能重新驱动回填。
				tryStartBackgroundFill();
			}
			tryPushMessage();
		} finally {
			unlock();
		}
	}

	private void tryStartBackgroundFill() {
		lock();
		try {
			// stopped 后不得再提交新 fill：close 的有界排空只等待已存在的 future，之后新提交的
			// 任务会与 rocksDatabase.close 并发（native use-after-free）。
			if (!managerStopped() && highLoad > 0 && messageFillFuture == null && messageQueue.size() < maxFillMessageCount / 2)
				messageFillFuture = TaskSpec.ofAction(this::pullMessage).name("pullMessage").submitNow();
		} finally {
			unlock();
		}
	}

	// 包内可见：测试在确定位置同步驱动一次后台回填（见ZezeJavaTest的TestMQSingleDirectEnqueue）。
	void pullMessage() {
		// 在另一个线程中调用，但只有一个线程任务。
		var first = new OutLong();
		var last = new OutLong();
		try {
			lock();
			try {
				if (highLoad > 0) {
					// calculateFill 里面还会加fileWithIndex的锁. 两把锁得到一个快照。
					highLoad -= fileWithIndex.calculateFill(messageQueue, first, last, maxFillMessageCount);
				}
			} finally {
				unlock();
			}
			fileWithIndex.fillMessage(messageQueue, first.value, last.value);
			// 这里有一个时间窗口：刚刚fill的消息全部都消费完毕，下面才置空，导致fill停止。
			messageFillFuture = null; // 这个清除没加锁
			tryStartBackgroundFill(); // 这个调用是为了解决上面的时间窗口的。
		} catch (RuntimeException e) {
			// fill 失败必须复位 messageFillFuture 并重算 highLoad，否则 tryStartBackgroundFill 永远
			// 看到非null而跳过，该分区回填永久停摆。calculateFill 已按快照扣减 highLoad 但装载
			// 未完成，按盘上真实积压重算；next/first 的所有写点（appendMessage/increaseFirstMessageId）
			// 都在本锁内执行，锁内读取是精确的。不在此立即重启 fill（确定性数据损坏时避免紧密
			// 循环），由后续 sendMessage/ack 事件驱动重试。
			lock();
			try {
				highLoad = fileWithIndex.getNextMessageId() - fileWithIndex.getFirstMessageId() - messageQueue.size();
				messageFillFuture = null;
			} finally {
				unlock();
			}
			logger.error("pullMessage fill failed. topic={} partition={}", topic, partitionIndex, e);
			throw e; // 后台任务路径异常由 TaskBody 记日志后吞掉；构造路径保持创建失败的响亮语义。
		}
		// fill 装载完成后，消息队列从空变为非空时（例如ack回调触发fill时队列已空），无人驱动推送，
		// 这里主动尝试推送；构造函数路径 bindSocket==null 时自然短路。
		lock();
		try {
			tryPushMessage();
		} finally {
			unlock();
		}
	}

	private void tryPushMessage() {
		if (null == pendingPushMessage && !messageQueue.isEmpty() && bindSocket != null) {
			pendingPushMessage = new PushMessage();
			pendingPushMessage.Argument.setTopic(topic);
			pendingPushMessage.Argument.setPartitionIndex(partitionIndex);
			pendingPushMessage.Argument.setSessionId(bindSessionId);
			var message = messageQueue.peek();
			pendingPushMessage.Argument.setMessage(message);
			// 推送超时必须走 MQConfig.RpcTimeout（默认 20s）：此前不传超时固定按 Rpc 字段默认
			// 5000ms，慢消费者场景每 5 秒被重推一次，配置的 RpcTimeout 只流入 Raft ProxyServer
			// 的代理路径、从不作用于 MQ 数据面。
			if (!pendingPushMessage.Send(bindSocket, (p) -> {
				handlePushResult();
				return 0;
			}, mqPartition.getManager().getMqConfig().getRpcTimeout())) {
				// Send失败（连接失效）时回调不会被调用，必须在这里清理，
				// 否则pendingPushMessage永久悬挂，该分区消息投递永久停止，bind()也无法恢复。
				pendingPushMessage = null;
			}
		}
	}

	// 包内可见：推送ack回调体（测试同步模拟回调到达，见ZezeJavaTest的TestMQSingleAckCallbackStall）。
	// 持本锁执行，锁内再进fileWithIndex锁，与calculateFill的锁序一致。
	void handlePushResult() {
		lock();
		try {
			// 停机窗口：socket 关闭会使在飞 rpc 的超时回调照常触发（Service.stop 不清 _RpcContexts），
			// 此处不触碰 rocksdb 位点（close 已持锁排空在飞写，pending 随分区关闭一并丢弃）。
			if (managerStopped())
				return;
			loadCounter.incrementAndGet(); // 处理失败也进行计数。

			if (pendingPushMessage.getResultCode() == 0) {
				// 先持久化推进位点再出队：increaseFirstMessageId 抛异常（如rocksdb写失败）时消息
				// 留在队首、位点未推进（内存位点在持久化成功后才前移），下面finally重推的就是
				// 同一条（at-least-once），位点不会跳过它。
				fileWithIndex.increaseFirstMessageId();
				messageQueue.poll();
				tryStartBackgroundFill();
			} else if (pendingPushMessage.getResultCode() == eConsumerNotFound) {
				// 幽灵订阅自愈：消费端条目已删（退订 best-effort 失败/超时遗留），继续重推只会
				// 永远收到 eConsumerNotFound 空转，该分区位点永不推进——清掉 Manager 侧订阅并重排。
				// 必须异步执行：arrangeConsumer 持 MQPartition 锁后经 partition.bind() 进各 MQSingle
				// 锁，这里正持本 MQSingle 锁，同步反向取 MQPartition 锁构成 AB-BA 死锁。
				// 会话标识取 pending 里发送时记录的值而非当前 bind 字段：推送在途期间分区可能已
				// 重绑到新会话，按当前字段清理会误杀新订阅。
				var ghostSessionId = pendingPushMessage.Argument.getSessionId();
				var ghostSocket = pendingPushMessage.getSender();
				TaskSpec.ofAction(() -> mqPartition.unsubscribe(ghostSocket, ghostSessionId))
						.name("MQSingle.unsubscribeGhost")
						.submitNow();
			}
		} finally {
			// 不管推送成功失败，都复位pending并尝试重新pushMessage（出错时是否随机延迟再重试？）。
			// 必须必达：本次rpc上下文已消费、不会再有第二次回调，上面任何异常跳过这里都会使
			// pending永久悬挂，该分区推送永久停止。
			try {
				pendingPushMessage = null;
				tryPushMessage();
			} finally {
				unlock();
			}
		}
	}

	public void bind(long sessionId, AsyncSocket socket) {
		lock();
		try {
			this.bindSessionId = sessionId;
			this.bindSocket = socket;
			if (null != bindSocket)
				tryPushMessage();
		} finally {
			unlock();
		}
	}

	public String getTopic() {
		return topic;
	}

	// 【GB-D02】包内可见：loadMonitorTimer 周期驱动的段回收透传（fileWithIndex 私有；
	// 回收自判条件与锁序见 MQFileWithIndex.tryRecycle，无需本类锁——其全部写点在fileWithIndex锁内）。
	void tryRecycleSegments(long delayMs) {
		fileWithIndex.tryRecycle(delayMs);
	}

	public int getPartitionIndex() {
		return partitionIndex;
	}

	// 测试可用 null manager 构造，这里 null 容忍。
	private boolean managerStopped() {
		var manager = mqPartition.getManager();
		return null != manager && manager.isStopped();
	}

	public void close() throws IOException {
		//fillGuardTimer.cancel(true);
		// 停机排空在飞回填（预算式，对齐 loadMonitorTimer 的 RpcTimeout 量级+余量）：fill 任务在
		// 锁外持索引迭代器与文件读，与 rocksDatabase.close 并发属 native use-after-free
		// （RocksDatabase.close 契约）。超预算仅告警继续（与 Application 停机的有界等待口径一致，
		// 不引入无限等待）。
		// 读取必须在分区锁内（增量审R1-01）：tryStartBackgroundFill 的 stopped检查+future赋值
		// 持同锁原子——锁内读要么看到已提交的future（排空它），要么读到null且此后新提交必被
		// stopped拒绝（stop先于queue.close置位），消除锁外读漏掉临界提交的窗口。
		Future<?> fill;
		lock();
		try {
			fill = messageFillFuture;
		} finally {
			unlock();
		}
		if (null != fill) {
			var manager = mqPartition.getManager();
			var budgetMs = (null != manager ? manager.getMqConfig().getRpcTimeout() : 20_000) + 5_000L;
			try {
				fill.get(budgetMs, TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				logger.warn("mq fill task not drained in {}ms, continue close. topic={} partition={}",
						budgetMs, topic, partitionIndex);
			} catch (ExecutionException e) {
				// fill 自身失败已在 pullMessage 的 catch 记录日志。
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				logger.warn("mq fill drain interrupted, continue close. topic={} partition={}", topic, partitionIndex);
			}
		}
		// 持锁关文件流：与在飞 sendMessage（appendMessage 同锁）串行；close 过后晚到的任务在
		// 锁内复查 stopped 拒绝，不再触碰文件与 rocksdb。
		lock();
		try {
			fileWithIndex.close();
		} finally {
			unlock();
		}
	}
}
