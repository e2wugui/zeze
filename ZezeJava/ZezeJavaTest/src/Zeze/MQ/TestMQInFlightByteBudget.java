package Zeze.MQ;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import javax.xml.parsers.DocumentBuilderFactory;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.PushMessage;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * 内存队列在飞字节预算回归：装载治理此前只按条数（4096）封顶，消息字节数无上界——大消息
 * 积压/重启装载按"协议上限 100MB × 条数"承诺内存，Manager OOM；backlog 存在时重启即
 * 崩溃循环。修复后双预算（分区 MaxInFlightBytesPerPartition 默认 64MB + 全局
 * MaxTotalInFlightBytes 默认 256MB）约束直入与装载，判据统一"队列为空恒放行队头（保队头
 * 活性），否则双预算均达标才装"。
 * <p>
 * ① 单分区截断+活性：盘上 5 条 30MB 消息 + 默认 64MB 预算 → 装载截断为 2 条、highLoad
 * 精确、ack 释放后续填，全部消息最终按序消费；
 * ② 预算小于单条：队头仍装载推送（无死锁），逐条推进；
 * ③ 重启装载受全局预算截断（构造期 pullMessage 同受约束——崩溃循环根治点）。
 * <p>
 * 注：文件放 src/MQ/ 但声明 package Zeze.MQ——访问 MQSingle/MQManager 包内缝
 * （注入构造器、pullMessage、getFileForTest、getQueueForTest），与 TestMQSingleDirectEnqueue
 * 先例一致。ack 经 setPending+handlePushResult 直驱（TestMQSingleAckCallbackStall 先例）；
 * 驱动前以未完成 future 占位 messageFillFuture 排除异步回填竞争（TestMQSingleDirectEnqueue 先例）。
 */
@ResourceLock("mq-file-statics") // 旁观者READ：与改写trunkFileSize/makeIndexPeriod的类互斥——静态被并行改小期间本类append会滚出无索引段，fillMessage seekForPrev落空即messageIndexNotFound假红（2026-10-04 test40批r5实证）；旁观者彼此READ可并行
@Fast
public class TestMQInFlightByteBudget {

	private static BMessage.Data messageOf(long id, int bodySize) {
		var message = new BMessage.Data();
		message.setTimestamp(id);
		message.setBody(new Binary(new byte[bodySize]));
		return message;
	}

	// 与 MQSingle.messageBytes 同尺（编码尺寸；与段记录体一致）。
	private static long encodedBytes(BMessage.Data message) {
		var bb = ByteBuffer.Allocate(64 + message.getBody().size());
		message.encode(bb);
		return bb.size();
	}

	private static Object getField(Object obj, String name) throws Exception {
		var f = obj.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(obj);
	}

	@SuppressWarnings("unchecked")
	private static List<String> queueIds(MQSingle single) throws Exception {
		var ids = new ArrayList<String>();
		for (var message : (Queue<BMessage.Data>)getField(single, "messageQueue"))
			ids.add(String.valueOf(message.getTimestamp()));
		return ids;
	}

	private static void setPending(MQSingle single, PushMessage push) throws Exception {
		var f = MQSingle.class.getDeclaredField("pendingPushMessage");
		f.setAccessible(true);
		f.set(single, push);
	}

	// 占位 messageFillFuture：阻断 ack 内 tryStartBackgroundFill 的异步提交，测试手动驱动
	// pullMessage 保持确定性（先例：TestMQSingleDirectEnqueue）。
	private static void blockAsyncFill(MQSingle single) throws Exception {
		var f = MQSingle.class.getDeclaredField("messageFillFuture");
		f.setAccessible(true);
		f.set(single, new java.util.concurrent.CompletableFuture<Void>());
	}

	private static void ackSuccess(MQSingle single) throws Exception {
		var ack = new PushMessage();
		ack.setResultCode(0);
		setPending(single, ack);
		blockAsyncFill(single);
		single.handlePushResult();
	}

	// 等待构造期排位的后台回填任务静默：小消息下构造尾部 tryStartBackgroundFill 的字节滞回
	//（queueBytes < per/2）不拦（字节远小于 64MB 默认），会排一个异步任务；预算绑定下它零进度
	// 完成、无续排（progressed=false），静默即终态。不等待则断言与任务交错可读到瞬态
	//（calculateFill 已扣减、尾部重算未达的 highLoad=0 窗口）。
	private static void awaitFillQuiescent(MQSingle single) throws Exception {
		var deadline = System.currentTimeMillis() + 10_000;
		while (System.currentTimeMillis() < deadline) {
			if (null == getField(single, "messageFillFuture"))
				return;
			Thread.sleep(20);
		}
		Assertions.fail("background fill not quiescent in time");
	}

	/**
	 * ① 单分区预算截断 + highLoad 精确 + ack 续填（默认预算行为）：
	 * 5 条 30MB（默认 64MB 预算装 2 条）；旧代码（仅条数）5 条全装（150MB 驻留）。
	 */
	@Test
	public void testPerPartitionBudgetTruncatesAndAckContinues(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var home = tempDir.resolve("db").toString();
		var database = new RocksDatabase(home);
		var file = new MQFileWithIndex(home, database, "topic", 0);
		try {
			var bodySize = 30 * 1024 * 1024;
			var w = encodedBytes(messageOf(1, bodySize)); // id 从 1 起：timestamp 在场，各条权重一致
			// 盘上积压 5 条 30MB（绕开内存队列直接落盘；模拟重启装载/大消息积压形态）。
			for (long id = 1; id <= 5; ++id)
				file.appendMessage(messageOf(id, bodySize));

			// 构造期 pullMessage(true)：默认 per=64MB → 装载 [1,2]（2×30MB ≤ 64MB；第 3 条超）。
			var single = new MQSingle(new MQPartition(null), "topic", 0, file);
			Assertions.assertEquals(List.of("1", "2"), queueIds(single),
					"字节预算须截断装载（旧代码仅按条数，5 条全装 → 150MB 驻留）");
			Assertions.assertEquals(3L, getField(single, "highLoad"),
					"highLoad 须按盘上真相精确（未装载积压 3 条）");

			// 队列非空且预算满：再驱动回填不得装载（零进度，不重复、不越预算）。
			blockAsyncFill(single);
			single.pullMessage();
			Assertions.assertEquals(List.of("1", "2"), queueIds(single), "预算不达标不得装载");

			// ack 续填（活性）：每轮 ack 出队释放字节 → 手动驱动回填装载下一条至预算。
			ackSuccess(single); // 消费 id1 → 释放 30MB
			single.pullMessage(); // 装载 id3（30+30 ≤ 64）
			Assertions.assertEquals(List.of("2", "3"), queueIds(single));
			Assertions.assertEquals(2 * w, single.queueBytes(), "在飞字节=2 条编码尺寸");

			ackSuccess(single); // 消费 id2
			single.pullMessage(); // 装载 id4
			Assertions.assertEquals(List.of("3", "4"), queueIds(single));

			ackSuccess(single); // 消费 id3
			single.pullMessage(); // 装载 id5
			Assertions.assertEquals(List.of("4", "5"), queueIds(single));

			ackSuccess(single); // 消费 id4
			single.pullMessage(); // 无积压（highLoad=0）
			Assertions.assertEquals(List.of("5"), queueIds(single));

			ackSuccess(single); // 消费 id5
			Assertions.assertEquals(List.of(), queueIds(single));
			Assertions.assertEquals(0L, single.queueBytes(), "全部出队后字节记账归零");
			Assertions.assertEquals(5L, file.getFirstMessageId(), "位点推进到 5");
			Assertions.assertEquals(0L, getField(single, "highLoad"));
		} finally {
			database.close();
			file.close();
		}
	}

	/**
	 * ② 预算小于单条消息：队列空时仍装载队头（保队头活性，无死锁），逐条 ack 推进；
	 * 队列非空时新装载被拒（有界），ack 腾空后队头恒可达。
	 */
	@Test
	public void testBudgetBelowSingleMessageKeepsHeadAlive(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var home = tempDir.resolve("db").toString();
		var config = managerConfigWith("MaxInFlightBytesPerPartition", "1"); // 1 字节预算：恒小于单条
		var manager = new MQManager(home, config);
		try {
			manager.createPartition("topic", new HashSet<>(List.of(0)));
			var file = manager.getQueueForTest("topic").get(0).getFileForTest();
			var bodySize = 1024;
			var w = encodedBytes(messageOf(1, bodySize));
			for (long id = 1; id <= 3; ++id)
				file.appendMessage(messageOf(id, bodySize));

			// 模拟重启装载：摘除重建（构造期装载受预算约束）。
			manager.getQueueForTest("topic").removePartition(0);
			manager.createPartition("topic", new HashSet<>(List.of(0)));
			var single = manager.getQueueForTest("topic").get(0);
			awaitFillQuiescent(single);

			// 队列空：队头恒装载（预算 1 字节也放行）——否则推送无源、ack 无事件，死锁。
			Assertions.assertEquals(List.of("1"), queueIds(single), "队列空时装队头（活性优先于预算）");
			Assertions.assertEquals(w, single.queueBytes(), "队头入账（可超预算）");

			// 队列非空：新装载被拒（预算 1 字节恒不达标），有界驻留。
			blockAsyncFill(single);
			single.pullMessage();
			Assertions.assertEquals(List.of("1"), queueIds(single), "队列非空时预算不达标不得装载");

			ackSuccess(single); // 消费 id1 → 队列空、字节归零
			single.pullMessage(); // 队头 id2 装载（空队列恒放行）
			Assertions.assertEquals(List.of("2"), queueIds(single), "ack 腾空后队头恒可达（逐条推进）");

			ackSuccess(single);
			single.pullMessage();
			Assertions.assertEquals(List.of("3"), queueIds(single));

			ackSuccess(single);
			Assertions.assertEquals(List.of(), queueIds(single));
			// 位点断言取重建后的新 file 实例（removePartition 已 close 旧实例）。
			Assertions.assertEquals(3L, manager.getQueueForTest("topic").get(0).getFileForTest().getFirstMessageId());
		} finally {
			manager.stop();
		}
	}

	/**
	 * ③ 重启装载受全局预算截断（字节断言）：全局预算=3 条编码尺寸，盘上 8 条 → 构造期装载
	 * 恰好 3 条（崩溃循环根治点：重启装载不再无界承诺内存）。
	 */
	@Test
	public void testRestartLoadTruncatedByTotalBudget(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var home = tempDir.resolve("db").toString();
		var bodySize = 1024;
		var w = encodedBytes(messageOf(1, bodySize));
		var config = managerConfigWith("MaxTotalInFlightBytes", String.valueOf(3 * w));
		var manager = new MQManager(home, config);
		try {
			manager.createPartition("topic", new HashSet<>(List.of(0)));
			var file = manager.getQueueForTest("topic").get(0).getFileForTest();
			for (long id = 1; id <= 8; ++id)
				file.appendMessage(messageOf(id, bodySize));

			// 模拟重启装载：摘除（close 释放全局记账）→ 重建（构造期 pullMessage 受全局预算约束）。
			manager.getQueueForTest("topic").removePartition(0);
			manager.createPartition("topic", new HashSet<>(List.of(0)));
			var reloaded = manager.getQueueForTest("topic").get(0);
			awaitFillQuiescent(reloaded);

			Assertions.assertEquals(List.of("1", "2", "3"), queueIds(reloaded),
					"重启装载受全局字节预算截断（旧代码 8 条全装）");
			Assertions.assertEquals(3 * w, reloaded.queueBytes(), "字节断言：恰装 3 条编码尺寸");
			Assertions.assertEquals(3 * w, manager.totalInFlightBytes.get(),
					"全局记账=已装载字节（跨分区共享的预算执行载体）");
			Assertions.assertEquals(5L, getField(reloaded, "highLoad"), "未装载积压精确为 5");
		} finally {
			manager.stop();
		}
	}

	// MQConfig 定制段注入（真实 MQManager 形态，不触碰共享的 DEFAULT_CONFIG）。
	private static Config managerConfigWith(String attributeName, String value) throws Exception {
		var config = new Config();
		var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		var mqConfElem = doc.createElement("MQConfig");
		mqConfElem.setAttribute(attributeName, value);
		config.getCustomizes().put("MQConfig", mqConfElem);
		return config;
	}
}
