package Zeze.Component;

import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.Builtin.DelayRemove.BTableKey;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Transaction.TableX;
import Zeze.Util.FuncLong;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND-C1-2/FND-C1-3回归：DelayRemove.onTimer 的节点消费与失败处理。
 * <ul>
 * <li>FND-C1-2：onTimer 原来先 pollNode（出队删行）再判头节点是否到期——未到期时整个节点
 * （≤30条登记）已随事务提交被摘除，既不物理删除也不在队列里，GC追踪永久丢失；
 * 低流量服每天GC碰到的头节点永远"未到期"，每天净丢一个节点。修复：先peekNode判期，
 * 未到期直接结束本轮（零写事务），到期才pollNode消费。</li>
 * <li>FND-C1-3：onTimer 原来丢弃 call() 返回值——登记的EncodedKey损坏时removeEncodedKey
 * 解码抛异常，事务回滚返回非0，返回值被忽略后立即重试同一节点=确定性无限忙循环，
 * 钉死调度线程且GC停摆。修复：非0告警并退出本轮，由固定延迟调度下个周期重试。</li>
 * </ul>
 * 注：文件放在 src/Zeze/Component/ 但声明 package Zeze.Component——需要访问 onTimer 的
 * 包内测试缝（与 src/MQ 的TestMQSingleDirectEnqueue、src/Zeze/Dbh2/Master 下同包测试先例一致）。
 */
@Fast
public class TestDelayRemoveOnTimer {

	// 与TestQueueCompatible的500+、MQSingle测试错开：@Fast类并行时Application的本地缓存按serverId一份。
	private static final AtomicInteger NextServerId = new AtomicInteger(600);

	/** GC目标表用真实的tQueues（String键）：getTable命中，decodeKey=ReadString。 */
	private static final String VictimTable = "Zeze_Builtin_Collections_Queue_tQueues";

	private static Config newConf() {
		var conf = new Config();
		conf.setServiceManager("disable");
		int serverId = NextServerId.getAndIncrement();
		conf.setServerId(serverId);
		conf.setDefaultTableConf(new Config.TableConf());
		// Memory库按url分桶，独占url=独占存储（同TestQueueCompatible）。
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("delay_remove_on_timer_test_" + serverId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return conf;
	}

	/** 与DelayRemove构造器打开的是同一个Queue实例（Module.queues按名字缓存）。 */
	private static Zeze.Collections.Queue<BTableKey> gcQueue(Application app) {
		return app.getQueueModule().open("__GCTableQueue#" + app.getConfig().getServerId(), BTableKey.class);
	}

	private static void run(Application app, String name, FuncLong action) {
		var rc = TaskSpec.ofProcedure(app.newProcedure(action, name)).call();
		Assertions.assertEquals(0L, rc);
	}

	// FND-C1-3：到期但损坏的登记（EncodedKey对String键表声明8字节字符串却只有1字节，
	// ReadString的ensureRead抛IllegalStateException）→ onTimer必须有限时间返回（不忙循环），
	// 且失败事务回滚后坏登记仍在队列头（不丢失、等下个周期重试）。
	@Test
	public void test1_CorruptedExpiredKeyTerminatesAndKeepsQueue() throws Exception {
		var app = new Application("TestDelayRemoveOnTimer1", newConf());
		try {
			app.start();
			var gcQueue = gcQueue(app);
			run(app, "forgeBadRegistration", () -> {
				var bad = new BTableKey();
				bad.setTableName(VictimTable);
				bad.setEncodedKey(new Binary(new byte[]{0x08})); // ReadUInt=8后ensureRead(8)必抛
				bad.setEnqueueTime(System.currentTimeMillis() - 100L * 24 * 3600 * 1000); // 100天前，已过保留期
				gcQueue.add(bad);
				return 0L;
			});

			// 守护线程驱动：修复前这里是无限忙循环，用join超时判定而不是挂死测试进程。
			var worker = new Thread(() -> app.getDelayRemove().onTimer(), "TestDelayRemoveOnTimer.corrupted");
			worker.setDaemon(true);
			worker.start();
			worker.join(30_000);
			Assertions.assertFalse(worker.isAlive(), "onTimer必须在有限时间返回，不得忙循环钉死线程");

			// 失败节点的事务已回滚：坏登记仍在队列中（GC未丢追踪，也未误删）
			run(app, "assertKeptAfterFailure", () -> {
				Assertions.assertNotNull(gcQueue.peek(), "失败事务回滚后登记必须仍在队列中");
				Assertions.assertEquals(1L, gcQueue.size());
				return 0L;
			});
		} finally {
			app.stop();
		}
	}

	// FND-C1-2：头节点未到期（刚登记）→ onTimer后登记必须仍可从队列取到。
	// 修复前：pollNode先删行、判期后return 0提交，登记永久脱离GC追踪（红）。
	@Test
	public void test2_UnexpiredHeadRegistrationKept() throws Exception {
		var app = new Application("TestDelayRemoveOnTimer2", newConf());
		try {
			app.start();
			var gcQueue = gcQueue(app);
			run(app, "addFreshRegistration", () -> {
				var fresh = new BTableKey();
				fresh.setTableName(VictimTable);
				fresh.setEncodedKey(new Binary(new byte[]{0x00})); // 合法的空字符串key编码
				fresh.setEnqueueTime(System.currentTimeMillis()); // 刚登记，远未到保留期
				gcQueue.add(fresh);
				return 0L;
			});

			app.getDelayRemove().onTimer();

			run(app, "assertUnexpiredKept", () -> {
				var head = gcQueue.peek();
				Assertions.assertNotNull(head, "未到期的头节点登记必须仍在队列中（不得脱离GC追踪）");
				Assertions.assertTrue(System.currentTimeMillis() - head.getEnqueueTime() < 60_000,
						"留下的应是刚登记的条目");
				Assertions.assertEquals(1L, gcQueue.size());
				return 0L;
			});
		} finally {
			app.stop();
		}
	}

	// FND-C1-2语义保持：已到期的登记仍被真实消费——目标表行被物理删除、GC队列排空。
	@Test
	public void test3_ExpiredRegistrationReallyRemoved() throws Exception {
		var app = new Application("TestDelayRemoveOnTimer3", newConf());
		try {
			app.start();
			@SuppressWarnings("unchecked")
			var victimTable = (TableX<String, ?>)app.getTable(VictimTable);
			Assertions.assertNotNull(victimTable);
			run(app, "createVictimRow", () -> {
				victimTable.getOrAdd("TestDelayRemoveOnTimer.victim");
				return 0L;
			});
			var gcQueue = gcQueue(app);
			run(app, "addExpiredRegistration", () -> {
				var old = new BTableKey();
				old.setTableName(VictimTable);
				old.setEncodedKey(new Binary(victimTable.encodeKey("TestDelayRemoveOnTimer.victim")));
				old.setEnqueueTime(System.currentTimeMillis() - 100L * 24 * 3600 * 1000); // 已过保留期
				gcQueue.add(old);
				return 0L;
			});

			app.getDelayRemove().onTimer();

			run(app, "assertVictimRemoved", () -> {
				Assertions.assertNull(victimTable.get("TestDelayRemoveOnTimer.victim"),
						"到期登记的目标行必须被物理删除");
				Assertions.assertTrue(gcQueue.isEmpty(), "消费完后GC队列必须排空");
				return 0L;
			});
		} finally {
			app.stop();
		}
	}
}
