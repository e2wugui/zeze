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
import Zeze.Util.Task;
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
	private final Future<?> fillGuardTimer;

	public MQPartition getMQPartition() {
		return mqPartition;
	}

	public MQSingle(MQPartition partition, String topic, int partitionId) {
		this.mqPartition = partition;
		this.topic = topic;
		this.partitionIndex = partitionId;
		try {
			this.fileWithIndex = new MQFileWithIndex(
					partition.getManager().getHome(),
					partition.getManager().getRocksDatabase(),
					topic, partitionId);
			this.highLoad = fileWithIndex.getNextMessageId() - fileWithIndex.getFirstMessageId();
			pullMessage(); // 构造的时候还没有绑定网络，所以只装载进来，不需要tryPushMessage.
			fillGuardTimer = Task.scheduleUnsafe(5_000, 5_000, this::tryStartBackgroundFill);
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
			fileWithIndex.appendMessage(message.getMessage());
			if (highLoad == 0 && messageQueue.size() < maxFillMessageCount) {
				// 低负载，缓冲足够大，直接进入缓冲，保持highLoad为0.
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
				messageFillFuture = Task.runUnsafe(this::pullMessage, "pullMessage");
		} finally {
			unlock();
		}
	}

	private void pullMessage() {
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
		// 这里有一个时间窗口：刚刚fill的消息全部都消费完毕，下面才置空，导致fill停止。目前解决方法是启动一个Timer。
		messageFillFuture = null; // 这个清除没加锁
	}

	private void tryPushMessage() {
		if (null == pendingPushMessage && !messageQueue.isEmpty() && bindSocket != null) {
			pendingPushMessage = new PushMessage();
			pendingPushMessage.Argument.setTopic(topic);
			pendingPushMessage.Argument.setPartitionIndex(partitionIndex);
			pendingPushMessage.Argument.setSessionId(bindSessionId);
			var message = messageQueue.peek();
			pendingPushMessage.Argument.setMessage(message);
			pendingPushMessage.Send(bindSocket, (p) -> {
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
			});
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
		fillGuardTimer.cancel(true);
		fileWithIndex.close();
	}
}
