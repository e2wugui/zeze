package Zeze.MQ;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import Zeze.Net.AsyncSocket;

public class MQPartition extends ReentrantLock {
	private static final Logger logger = LogManager.getLogger();
	private final ConcurrentHashMap<Integer, MQSingle> partitions = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Long, AsyncSocket> subscribes = new ConcurrentHashMap<>();
	private final MQManager manager;

	public MQPartition(MQManager manager) {
		this.manager = manager;
	}

	public int size() {
		return partitions.size();
	}

	public double load() {
		var load = 0.0;
		for (var partition : partitions.values())
			load += partition.load();
		return load;
	}

	public MQSingle get(int partitionIndex) {
		return partitions.get(partitionIndex);
	}

	public MQManager getManager() {
		return manager;
	}

	public void createPartitions(String topic, Set<Integer> partitionIndexes) {
		for (var index : partitionIndexes)
			partitions.computeIfAbsent(index, (key) -> new MQSingle(this, topic, index));
	}

	public void subscribe(AsyncSocket sender, long sessionId) {
		// 同 sessionId 换 socket 必须替换旧条目：网络静默死亡后消费者重连重订阅，新连接的
		// Subscribe 先于旧 socket 的 OnSocketClose 到达是常态序（KeepCheckPeriod 默认禁用）。
		// putIfAbsent 会静默吞掉新 socket 的订阅并回成功，分区继续绑在死 socket 上空转重推，
		// 消费者"订阅成功、连接健康、永不收消息"直到进程重启。对齐 Master.ProcessRegisterRequest
		// 的"同身份替换旧条目"写法。同 socket 重复订阅（重连重订阅与首订阅重叠）幂等无害。
		var old = subscribes.put(sessionId, sender);
		if (null == old || old != sender) {
			// 新订阅或 socket 更换（订阅变更）
			if (null != old)
				logger.info("mq subscribe socket replaced (stale connection superseded). sessionId={}", sessionId);
			arrangeConsumer();
		}
	}

	public void unsubscribe(AsyncSocket sender, long sessionId) {
		if (subscribes.remove(sessionId) != null) {
			// 订阅发生变更
			arrangeConsumer();
		}
	}

	// 消费者socket关闭：按socket身份清理其全部sessionId订阅（socket死亡使其上所有订阅失效）。
	// 不清理则死socket永久占槽，绑到它的分区消息永久积压。
	public void onSocketClose(AsyncSocket sender) {
		boolean changed = false;
		for (var it = subscribes.entrySet().iterator(); it.hasNext(); ) {
			if (it.next().getValue() == sender) {
				it.remove();
				changed = true;
			}
		}
		if (changed)
			arrangeConsumer();
	}

	private void arrangeConsumer() {
		lock();
		try {
			if (subscribes.isEmpty()) {
				for (var partition : partitions.values())
					partition.bind(0, null);
				return;
			}
			var subs = subscribes.entrySet().toArray();
			Arrays.sort(subs, new SessionIdComparator());
			for (var partition : partitions.values()) {
				var subIndex = partition.getPartitionIndex() % subs.length;
				@SuppressWarnings("unchecked")
				var sub = (java.util.Map.Entry<Long, AsyncSocket>)subs[subIndex];
				partition.bind(sub.getKey(), sub.getValue());
			}
		} finally {
			unlock();
		}
	}

	static class SessionIdComparator implements Comparator<Object> {

		@SuppressWarnings("unchecked")
		@Override
		public int compare(Object _o1, Object _o2) {
			var o1 = (java.util.Map.Entry<Long, AsyncSocket>)_o1;
			var o2 = (java.util.Map.Entry<Long, AsyncSocket>)_o2;
			return Long.compare(o1.getKey(), o2.getKey());
		}
	}

	public void close() throws IOException {
		for (var partition : partitions.values())
			partition.close();
	}
}
