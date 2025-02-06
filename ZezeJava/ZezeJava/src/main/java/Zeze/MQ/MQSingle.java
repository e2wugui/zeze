package Zeze.MQ;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.BSendMessage;
import Zeze.Builtin.MQ.PushMessage;
import Zeze.Net.AsyncSocket;
import org.jetbrains.annotations.Nullable;

public class MQSingle extends ReentrantLock {
	private final String topic;
	private final int partitionIndex;
	private long bindSessionId;
	private @Nullable AsyncSocket bindSocket;
	private @Nullable PushMessage pendingPushMessage;
	private final MQPartition mqPartition;
	private final MQFileWithIndex fileWithIndex;
	private long highLoad = 0;

	public static final int maxFillMessageCount = 4 * 1024;

	private final Queue<BMessage.Data> messages = new ArrayDeque<>(); // 需要写入文件，临时放内存用来测试。

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
			pullMessage();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public double load() {
		return 0.0;
	}

	public void sendMessage(BSendMessage.Data message) {
		lock();
		try {
			fileWithIndex.appendMessage(message.getMessage());
			if (highLoad == 0 && messages.size() < maxFillMessageCount) {
				// 低负载，缓冲足够大，直接进入缓冲，保持highLoad为0.
				messages.offer(message.getMessage());
			} else {
				highLoad++;
			}
			tryPushMessage();
		} finally {
			unlock();
		}
	}

	private void pullMessage() {
		if (messages.isEmpty() && highLoad > 0) {
			var fillCount = Math.min(highLoad, maxFillMessageCount);
			fileWithIndex.fillMessage(messages, (int)fillCount);
			highLoad -= fillCount;
		}
	}

	private void tryPushMessage() {
		pullMessage();
		if (null == pendingPushMessage && !messages.isEmpty() && bindSocket != null) {
			pendingPushMessage = new PushMessage();
			pendingPushMessage.Argument.setTopic(topic);
			pendingPushMessage.Argument.setPartitionIndex(partitionIndex);
			pendingPushMessage.Argument.setSessionId(bindSessionId);
			var message = messages.peek();
			pendingPushMessage.Argument.setMessage(message);
			pendingPushMessage.Send(bindSocket, (p) -> {
				lock();
				try {
					if (pendingPushMessage.getResultCode() == 0) {
						messages.poll();
						fileWithIndex.increaseFirstMessageId();
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
		fileWithIndex.close();
	}
}
