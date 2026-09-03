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
import org.jetbrains.annotations.Nullable;

public class MQSingle extends ReentrantLock {
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
				lock();
				try {
					loadCounter.incrementAndGet(); // 处理失败也进行计数。

					if (pendingPushMessage.getResultCode() == 0) {
						messageQueue.poll();
						fileWithIndex.increaseFirstMessageId();
						tryStartBackgroundFill();
					}

					// 不管是否失败，都尝试重新pushMessage。出错的时候要不要随机延迟一下再重试？
					pendingPushMessage = null;
					tryPushMessage();
				} finally {
					unlock();
				}
				return 0;
			})) {
				// Send失败（连接失效）时回调不会被调用，必须在这里清理，
				// 否则pendingPushMessage永久悬挂，该分区消息投递永久停止，bind()也无法恢复。
				pendingPushMessage = null;
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
