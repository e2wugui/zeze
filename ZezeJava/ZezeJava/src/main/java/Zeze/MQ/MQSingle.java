package Zeze.MQ;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
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
			if (highLoad > 0 && messageFillFuture == null && messageQueue.size() < maxFillMessageCount / 2)
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
			if (!pendingPushMessage.Send(bindSocket, (p) -> {
				handlePushResult();
				return 0;
			})) {
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
			loadCounter.incrementAndGet(); // 处理失败也进行计数。

			if (pendingPushMessage.getResultCode() == 0) {
				// 先持久化推进位点再出队：increaseFirstMessageId 抛异常（如rocksdb写失败）时消息
				// 留在队首、位点未推进（内存位点在持久化成功后才前移），下面finally重推的就是
				// 同一条（at-least-once），位点不会跳过它。
				fileWithIndex.increaseFirstMessageId();
				messageQueue.poll();
				tryStartBackgroundFill();
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

	public int getPartitionIndex() {
		return partitionIndex;
	}

	public void close() throws IOException {
		//fillGuardTimer.cancel(true);
		fileWithIndex.close();
	}
}
