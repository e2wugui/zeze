package Zeze.MQ;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Net.AsyncSocket;

public class MQPartition extends ReentrantLock {
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
		if (null == subscribes.putIfAbsent(sessionId, sender)) {
			// 新订阅
			arrangeConsumer();
		}
	}

	public void unsubscribe(AsyncSocket sender, long sessionId) {
		if (subscribes.remove(sessionId) != null) {
			// 订阅发生变更
			arrangeConsumer();
		}
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
