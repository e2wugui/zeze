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
 * FND19 GB-C01 回归：同 sessionId 换 socket 的重订阅不得被 putIfAbsent 静默吞掉。
 * <p>
 * 网络静默死亡（无 FIN/RST 到达 Manager）后，消费者侧重连出新 socket B 并在 OnHandshakeDone
 * 重发 Subscribe；Manager 对旧 socket A 的死亡毫不知情（KeepCheckPeriod 默认禁用，半开连接
 * 只能等 TCP 重传超时），B 的 Subscribe 几乎必然先于 A 的 OnSocketClose 到达。旧代码
 * putIfAbsent 吞掉 B 的订阅并照常回 code=0——分区继续绑在死 socket A 上 RpcTimeout 空转
 * 重推，消费者"订阅成功、连接健康、永不收消息"；A 最终关闭时按 socket 身份清掉的是 A 的
 * 条目（B 的从未进表），分区 bind(0,null)，此后无任何自动恢复路径。
 * <p>
 * 修复：对齐 Master.ProcessRegisterRequest 的"同身份替换旧条目"写法——subscribes 按
 * sessionId put 替换旧 socket 并 arrangeConsumer；同 socket 重复订阅幂等无害。
 * <p>
 * 注：需要 MQSingle 的包内测试缝与 MQPartition.subscribes 的反射观察点（布局约定见
 * Fnd19MqTestSupport）。
 */
@Fast
public class TestFnd19MQPartitionResubscribeReplace {

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

	private static long bindSessionIdOf(MQSingle single) throws Exception {
		var f = MQSingle.class.getDeclaredField("bindSessionId");
		f.setAccessible(true);
		return f.getLong(single);
	}

	@SuppressWarnings("unchecked")
	private static void injectPartition(MQPartition partition, int index, MQSingle single) throws Exception {
		var f = MQPartition.class.getDeclaredField("partitions");
		f.setAccessible(true);
		((ConcurrentHashMap<Integer, MQSingle>)f.get(partition)).put(index, single);
	}

	@Test
	public void testResubscribeWithNewSocketReplacesStaleOne(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		// 真实（未 start 的）manager：subscribe→arrangeConsumer→bind 链路与生产一致；
		// 不占端口、不连 Master。
		var manager = new MQManager(tempDir.resolve("manager").toString(), new Config());
		try {
			var home = tempDir.resolve("db").toString();
			var database = new RocksDatabase(home);
			var file = new MQFileWithIndex(home, database, "topic", 0);
			var partition = new MQPartition(manager);
			var single = new MQSingle(partition, "topic", 0, file);
			injectPartition(partition, 0, single);
			try {
				var service = new Service("TestFnd19MQPartitionResubscribeReplace");
				var staleSocket = new Fnd19MqTestSupport.FakeSocket(service);
				var freshSocket = new Fnd19MqTestSupport.FakeSocket(service);

				// 基线：sessionId=1 经 socket A 订阅成功，分区 0 绑定 A。
				partition.subscribe(staleSocket, 1L);
				Assertions.assertSame(staleSocket, subscribesOf(partition).get(1L));
				Assertions.assertSame(staleSocket, bindSocketOf(single));
				Assertions.assertEquals(1L, bindSessionIdOf(single));

				// 网络静默死亡后重连重订阅：新 socket B 的 Subscribe 先于 A 的 OnSocketClose 到达。
				// 旧代码（putIfAbsent）此处静默吞掉 B：subscribes/绑定仍是死 socket A。
				partition.subscribe(freshSocket, 1L);
				Assertions.assertSame(freshSocket, subscribesOf(partition).get(1L),
						"同sessionId的新socket订阅必须替换旧条目（旧代码静默吞掉，消费者永久饿死）");
				Assertions.assertSame(freshSocket, bindSocketOf(single),
						"socket更换即订阅变更，分区必须重排绑定到新socket");
				Assertions.assertEquals(1L, bindSessionIdOf(single));

				// 半开旧连接 A 最终被 Manager 察觉关闭：按 socket 身份清理不得误伤 B 的订阅
				// （旧代码：B 的条目从未进表，A 关闭后 subscribes 清空、分区 bind(0,null)，投递停止）。
				partition.onSocketClose(staleSocket);
				Assertions.assertSame(freshSocket, subscribesOf(partition).get(1L),
						"旧socket关闭不得移除替换后的新socket订阅");
				Assertions.assertSame(freshSocket, bindSocketOf(single),
						"旧socket关闭不得解除新socket的分区绑定");

				// 同 socket 重复订阅（重连重订阅与首订阅重叠）：幂等无害。
				partition.subscribe(freshSocket, 1L);
				Assertions.assertSame(freshSocket, subscribesOf(partition).get(1L));
				Assertions.assertSame(freshSocket, bindSocketOf(single));
			} finally {
				single.close();
				database.close();
			}
		} finally {
			// 未 start 的 MQManager.stop() 安全（DaemonTimer.stop 未启动即 return、Acceptor/Service.stop
			// 对未启动组件为 no-op）：关掉构造期打开的 rocksdb（TestMQSinglePushRpcTimeout 同款依据）。
			manager.stop();
		}
	}
}
