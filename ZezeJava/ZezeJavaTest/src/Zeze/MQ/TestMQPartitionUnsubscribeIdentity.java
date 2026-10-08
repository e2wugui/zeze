package Zeze.MQ;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND24 mq-04 回归：unsubscribe 必须按 (sessionId, socket) 双身份条件删
 * （ConcurrentHashMap.remove(key, value)）——协议不鉴权，旧代码无条件按 sessionId 删除，
 * 任一对端携他人 sessionId 的 Unsubscribe 即可移除对方订阅（分区被重排、消息停投直到
 * 对端重连重订阅）。
 * <p>
 * 同时锁定 ghost 清理语义的改进：重订阅换 socket 后，旧 socket 身份的 ghost 清理
 * （MQSingle.handlePushResult 传 pendingPushMessage.getSender()）不再误删新订阅
 * （旧代码会按 sessionId 误删换绑后的新条目）。
 * <p>
 * 注：需要 MQSingle 的包内测试缝与 MQPartition.subscribes 的反射观察点
 * （布局约定见 MqTestSupport / TestMQPartitionResubscribeReplace）。
 */
@Fast
public class TestMQPartitionUnsubscribeIdentity {

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<Long, AsyncSocket> subscribesOf(MQPartition partition) throws Exception {
		var f = MQPartition.class.getDeclaredField("subscribes");
		f.setAccessible(true);
		return (ConcurrentHashMap<Long, AsyncSocket>)f.get(partition);
	}

	private static AsyncSocket bindSocketOf(MQSingle single) throws Exception {
		var f = MQSingle.class.getDeclaredField("bindSocket");
		f.setAccessible(true);
		return (AsyncSocket)f.get(single);
	}

	@SuppressWarnings("unchecked")
	private static void injectPartition(MQPartition partition, int index, MQSingle single) throws Exception {
		var f = MQPartition.class.getDeclaredField("partitions");
		f.setAccessible(true);
		((ConcurrentHashMap<Integer, MQSingle>)f.get(partition)).put(index, single);
	}

	@Test
	public void testUnsubscribeRequiresMatchingSocketIdentity(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var manager = new MQManager(tempDir.resolve("manager").toString(), new Config());
		try {
			var home = tempDir.resolve("db").toString();
			var database = new RocksDatabase(home);
			var file = new MQFileWithIndex(home, database, "topic", 0);
			var partition = new MQPartition(manager);
			var single = new MQSingle(partition, "topic", 0, file);
			injectPartition(partition, 0, single);
			try {
				var service = new Service("TestMQPartitionUnsubscribeIdentity");
				var ownerSocket = new MqTestSupport.FakeSocket(service);
				var foreignSocket = new MqTestSupport.FakeSocket(service);

				// 基线：sessionId=1 经 socket A 订阅成功，分区 0 绑定 A。
				partition.subscribe(ownerSocket, 1L);
				Assertions.assertSame(ownerSocket, subscribesOf(partition).get(1L));
				Assertions.assertSame(ownerSocket, bindSocketOf(single));

				// 攻击面：另一 socket B 携他人 sessionId=1 发 Unsubscribe——不得生效。
				// 旧代码（无条件 remove(sessionId)）此处移除 A 的订阅并 bind(0,null) 重排。
				partition.unsubscribe(foreignSocket, 1L);
				Assertions.assertSame(ownerSocket, subscribesOf(partition).get(1L),
						"非订阅本人的 socket 携他人 sessionId 退订不得移除对方订阅");
				Assertions.assertSame(ownerSocket, bindSocketOf(single),
						"无效退订不得触发重排解除绑定");

				// 本人退订：双身份匹配，正常移除并重排解绑。
				partition.unsubscribe(ownerSocket, 1L);
				Assertions.assertNull(subscribesOf(partition).get(1L),
						"双身份匹配的退订必须移除订阅");
				Assertions.assertNull(bindSocketOf(single),
						"订阅移除后分区必须重排解绑");
			} finally {
				single.close();
				database.close();
			}
		} finally {
			manager.stop();
		}
	}

	@Test
	public void testStaleSocketGhostCleanupKeepsResubscribedEntry(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var manager = new MQManager(tempDir.resolve("manager2").toString(), new Config());
		try {
			var home = tempDir.resolve("db2").toString();
			var database = new RocksDatabase(home);
			var file = new MQFileWithIndex(home, database, "topic", 0);
			var partition = new MQPartition(manager);
			var single = new MQSingle(partition, "topic", 0, file);
			injectPartition(partition, 0, single);
			try {
				var service = new Service("TestMQPartitionUnsubscribeIdentity");
				var staleSocket = new MqTestSupport.FakeSocket(service);
				var freshSocket = new MqTestSupport.FakeSocket(service);

				// 订阅经旧 socket 建立，随后重订阅换绑新 socket（TestMQPartitionResubscribeReplace 锁定的替换语义）。
				partition.subscribe(staleSocket, 1L);
				partition.subscribe(freshSocket, 1L);
				Assertions.assertSame(freshSocket, subscribesOf(partition).get(1L));
				Assertions.assertSame(freshSocket, bindSocketOf(single));

				// 旧 socket 推送超时/eConsumerNotFound 触发的 ghost 清理：以推送时记录的旧 socket
				// 身份调 unsubscribe——不得误删换绑后的新订阅。旧代码按 sessionId 无条件删除，此处
				// 把新订阅一并移除（分区解绑，消费者已重订阅却停投）。
				partition.unsubscribe(staleSocket, 1L);
				Assertions.assertSame(freshSocket, subscribesOf(partition).get(1L),
						"旧socket身份的ghost清理不得误删换绑后的新订阅");
				Assertions.assertSame(freshSocket, bindSocketOf(single));

				// 新 socket 身份的清理正常生效（ghost 链收敛点）。
				partition.unsubscribe(freshSocket, 1L);
				Assertions.assertNull(subscribesOf(partition).get(1L));
				Assertions.assertNull(bindSocketOf(single));
			} finally {
				single.close();
				database.close();
			}
		} finally {
			manager.stop();
		}
	}
}
