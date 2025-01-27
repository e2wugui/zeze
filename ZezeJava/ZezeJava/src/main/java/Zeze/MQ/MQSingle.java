package Zeze.MQ;

import java.io.File;
import java.nio.file.Paths;
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
	private final File file;

	private final Queue<BMessage.Data> messages = new ArrayDeque<>(); // 需要写入文件，临时放内存用来测试。

	public MQPartition getMQPartition() {
		return mqPartition;
	}

	public MQSingle(MQPartition partition, String topic, int partitionId) {
		this.mqPartition = partition;
		file = Paths.get(partition.getManager().getHome(), topic, String.valueOf(partitionId)).toFile();
		try {
			//noinspection ResultOfMethodCallIgnored
			file.createNewFile();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		this.topic = topic;
		this.partitionIndex = partitionId;
	}

	public double load() {
		return 0.0;
	}

	public void sendMessage(BSendMessage.Data message) {
		lock();
		try {
			messages.offer(message.getMessage());
			tryPushMessage();
		} finally {
			unlock();
		}
	}

	private void tryPushMessage() {
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
					if (pendingPushMessage.getResultCode() == 0)
						messages.poll();

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
}
