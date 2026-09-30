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

/**
 * 同一 topic 的分区集合：维护分区表与订阅的消费者，按 sessionId 将分区绑定到消费者连接。
 */
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

	// loadMonitorTimer 周期驱动：逐分区尝试水位线整段回收（条件自判，见MQFileWithIndex.tryRecycle）。
	public void tryRecycleSegments(long delayMs) {
		for (var partition : partitions.values())
			partition.tryRecycleSegments(delayMs);
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

	// 删除活分区：先摘除（此后 SendMessage/Subscribe 走 eTopicNotExist/ePartition 拒绝，
	// 新的 fill/推送无从发起），再 bind(null) 静默在途推送、close 有界排空在飞回填（MQSingle.close 契约）。
	// 之后由 MQManager.deletePartition 清理段文件/索引列族/meta。分区删除不触发 arrangeConsumer：
	// 订阅集合未变，其余分区的 sessionId 取模绑定不受影响。
	// @return 被摘除的分区（mq-02：其 fileWithIndex 的在飞 fill 计数供 deletePartitionStorage
	// 的终结原语排空；不在活集合返回 null）。
	public MQSingle removePartition(int index) throws IOException {
		// 全程持本锁，与 arrangeConsumer 串行化：锁外摘除时其 CHM 弱一致迭代可在 remove 落地前
		// 读到本分区，随后的 bind 阻塞在 MQSingle 锁上待下方 close 排空让锁，之后在已 close 分区上
		// 重设 bindSocket 并重推（closed 拒绝见 MQSingle.bind/tryPushMessage）。锁序沿既有方向
		// managementLock→本锁→MQSingle 锁（arrangeConsumer 同向），无反向取锁路径。
		lock();
		try {
			var partition = partitions.remove(index);
			if (null != partition) {
				partition.bind(0, null);
				partition.close();
			}
			return partition;
		} finally {
			unlock();
		}
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
		// 条件删（CHM.remove(key,value)，按值 equals 匹配——AsyncSocket 未覆写 equals，即引用
		// 相等）：按 (sessionId, socket) 双身份匹配才移除。协议不鉴权，无条件按 sessionId 删除会
		// 使携他人 sessionId 的退订请求移除对方订阅（其分区被重排、消息停投直到对端重连重订阅）。
		// ghost 清理（MQSingle.handlePushResult）传 pendingPushMessage.getSender() 即推送时记录的
		// 原 socket：双身份匹配语义保持，且重订阅换 socket 后旧 socket 的 ghost 清理不再误删新订阅。
		if (subscribes.remove(sessionId, sender)) {
			// 订阅发生变更
			arrangeConsumer();
		}
	}

	// 消费者socket关闭：按socket身份清理其全部sessionId订阅（socket死亡使其上所有订阅失效）。
	// 不清理则死socket永久占槽，绑到它的分区消息永久积压。
	public void onSocketClose(AsyncSocket sender) {
		boolean changed = false;
		for (var entry : subscribes.entrySet()) {
			// 迭代器读到旧 socket 后，同 sessionId 可能已重订阅换绑新 socket。
			// 条件删除与 unsubscribe 同义，不能用按 key 无条件删的 iterator.remove。
			if (entry.getValue() == sender && subscribes.remove(entry.getKey(), sender)) {
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
			if (subs.length == 0) {
				// TOCTOU 收口：subscribes 的变更（subscribe/unsubscribe/onSocketClose）都在本锁外，
				// isEmpty 判真之后、toArray 之前可被并发清空——空数组使下方 % subs.length 除零
				// （ArithmeticException 从订阅应答/关闭回调炸出，重排中断且分区残留死绑定）。
				// 复查为空则与 isEmpty 分支同构收口：全部分区 bind(0,null)。
				for (var partition : partitions.values())
					partition.bind(0, null);
				return;
			}
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
		close(Long.MAX_VALUE);
	}

	public void close(long drainDeadlineMs) throws IOException { // 总额包络下传
		for (var partition : partitions.values())
			partition.close(drainDeadlineMs);
	}
}
