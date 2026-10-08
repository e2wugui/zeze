package Zeze.Onz;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import Zeze.Builtin.Onz.BSavedCommits;
import Zeze.Builtin.Onz.FlushReady;
import Zeze.Util.TaskCompletionSource;
import harness.Fast;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * FND19 GC-D03 回归：FlushReady 计数契约闭合（防闸门提前打开）。
 * trySetFlushReady 原先按 rpc 对象身份聚合计数（flushReadies.size()==zezeProcedures.size()，
 * 自注"简单的用数量判断"）：参与方 flush 失败重试每次发出新 rpc（FND8-18 的正确性机制），
 * 同一参与方贡献≥2条时计数可在其余参与方尚未 flush 时满足——闸门提前打开，flushDone 以
 * "全部落盘"收场而实际有参与方未 flush，且无任何日志。
 * 修复（协议批，BFlushReady 新增 Participant）：参与方填本集群身份，协调者按身份去重计数；
 * 同一 Participant 的第二条 ready 记 warn；空 Participant（旧版本参与方）按 rpc 对象身份
 * 兜底计数并每事务 warn 一次。
 * <p>
 * 直构形态（@Fast）：反射填充 zezeProcedures 的 N 个参与方名，直接驱动包内
 * trySetFlushReady；rpc 无真实 socket，应答路径 SendResult(null sender) 仅记 warn 不抛。
 */
@Fast
public class TestFlushReadyDedup {
	private Logger txnLogger;
	private CapturingAppender appender;

	@BeforeEach
	public void before() {
		// trySetFlushReady对首条被扣ready排持有期限定时（TaskSpec.schedule，生产由
		// Application初始化调度池）——直构测试自行初始化。
		Zeze.Util.Task.tryInitThreadPool();
		txnLogger = (Logger)LogManager.getLogger(OnzTransaction.class);
		appender = new CapturingAppender();
		appender.start();
		txnLogger.addAppender(appender);
	}

	@AfterEach
	public void after() {
		txnLogger.removeAppender(appender);
		appender.stop();
	}

	/** N 个不同参与方各一条 → 第 N 条开闸，全部 ready 得到应答，无 warn。 */
	@Test
	@Timeout(30)
	public void testDistinctParticipantsOpenGate() throws Exception {
		var txn = newTransaction("zeze1", "zeze2", "zeze3");
		var r1 = ready("zeze1");
		var r2 = ready("zeze2");
		var r3 = ready("zeze3");
		txn.trySetFlushReady(r1);
		txn.trySetFlushReady(r2);
		Assertions.assertFalse(gateOpen(txn), "两条不同参与方ready（N=3）不得开闸");
		Assertions.assertFalse(flushDone(txn), "计数未满足不得置位flushDone");
		txn.trySetFlushReady(r3);
		Assertions.assertTrue(gateOpen(txn), "N个不同参与方各一条必须开闸");
		Assertions.assertTrue(flushDone(txn), "开闸必须置位flushDone");
		Assertions.assertTrue(r1.isSendResultDone() && r2.isSendResultDone() && r3.isSendResultDone(),
				"开闸时必须应答全部ready（每条ready背后是一个等待的参与方事务）");
		// 开闸后到达的（含同参与方重发）立即应答且不再计数告警。
		var late = ready("zeze1");
		txn.trySetFlushReady(late);
		Assertions.assertTrue(late.isSendResultDone(), "开闸后到达的ready必须立即应答");
		Assertions.assertTrue(warnMessages().isEmpty(), "正常路径（N个不同参与方各一条）不得有warn");
	}

	/** 某参与方重发两条 + N-1 其他：rpc 条数==N（旧判据在此开闸）但不同身份只有 N-1 → 不开闸 + warn。 */
	@Test
	@Timeout(30)
	public void testDuplicateRetryDoesNotOpenGate() throws Exception {
		var txn = newTransaction("zeze1", "zeze2", "zeze3");
		// zeze1 flush失败重试：两条ready是不同的rpc对象（每次new FlushReady），
		// 加上 zeze2 一条，rpc条数=3==参与方数——修复前判据在此提前开闸（本案核心）。
		txn.trySetFlushReady(ready("zeze1"));
		txn.trySetFlushReady(ready("zeze1"));
		txn.trySetFlushReady(ready("zeze2"));
		Assertions.assertFalse(gateOpen(txn), "同参与方重发不得虚增计数提前开闸（修复前：3条rpc==3参与方即开闸）");
		Assertions.assertFalse(flushDone(txn), "计数未满足不得置位flushDone");
		var dups = warnMessages().stream().filter(m -> m.contains("duplicate FlushReady")).toList();
		Assertions.assertEquals(1, dups.size(), "同一参与方第二条ready必须恰好warn一次（防闸门提前打开的直接信号）");
		Assertions.assertTrue(dups.get(0).contains("zeze1"), "warn必须指向重发的参与方");
		// zeze3 补齐第三个不同身份 → 开闸，此前滞留的重复ready也一并应答。
		txn.trySetFlushReady(ready("zeze3"));
		Assertions.assertTrue(gateOpen(txn), "第三身份到达必须开闸");
		Assertions.assertTrue(flushDone(txn));
		Assertions.assertEquals(1, warnMessages().stream().filter(m -> m.contains("duplicate FlushReady")).count(),
				"开闸后的补齐ready不得再计warn");
	}

	/** 空 Participant（旧版本参与方）兜底：按 rpc 对象身份计数，条数够即开闸（=修复前语义），每事务 warn 一次。 */
	@Test
	@Timeout(30)
	public void testEmptyParticipantLegacyFallback() throws Exception {
		var txn = newTransaction("zeze1", "zeze2");
		// 两个旧版本参与方（协议无Participant，decode缺省空串），不同rpc对象兜底计数。
		txn.trySetFlushReady(ready(""));
		Assertions.assertFalse(gateOpen(txn), "一条legacy ready（N=2）不得开闸");
		txn.trySetFlushReady(ready(""));
		Assertions.assertTrue(gateOpen(txn), "legacy条数==N必须开闸（兼容窗口保持修复前语义，等待不能挂满超时）");
		Assertions.assertTrue(flushDone(txn));
		var legacy = warnMessages().stream().filter(m -> m.contains("without Participant")).toList();
		Assertions.assertEquals(1, legacy.size(), "兼容窗口每事务warn恰好一次（不是每条一次）");
		Assertions.assertTrue(legacy.get(0).contains("old client"), "warn必须写明旧版本兼容语义");
	}

	/** 混部窗口：新版本参与方带身份 + 旧版本参与方空身份，两类计数合并判据。 */
	@Test
	@Timeout(30)
	public void testMixedNamedAndLegacyCounting() throws Exception {
		var txn = newTransaction("zeze1", "zeze2");
		txn.trySetFlushReady(ready("zeze1"));
		Assertions.assertFalse(gateOpen(txn));
		txn.trySetFlushReady(ready(""));
		Assertions.assertTrue(gateOpen(txn), "一个带身份+一个legacy（N=2）必须开闸");
		Assertions.assertTrue(flushDone(txn));
		Assertions.assertEquals(1, warnMessages().stream().filter(m -> m.contains("without Participant")).count());
		Assertions.assertTrue(warnMessages().stream().noneMatch(m -> m.contains("duplicate FlushReady")),
				"不同参与方（一新一旧）不得计入duplicate告警");
	}

	// ------------------------------------------------------------------ 直构辅助

	private static final class LocalTransaction extends OnzTransaction<BSavedCommits.Data, BSavedCommits.Data> {
		@Override
		protected long perform() {
			return 0; // 计数路径不触达perform
		}
	}

	private static OnzTransaction<BSavedCommits.Data, BSavedCommits.Data> newTransaction(String... participants)
			throws Exception {
		var txn = new LocalTransaction();
		@SuppressWarnings("unchecked")
		var procedures = (ConcurrentHashMap<String, TaskCompletionSource<?>>)
				field("zezeProcedures").get(txn);
		for (var participant : participants)
			procedures.put(participant, new TaskCompletionSource<>());
		return txn;
	}

	private static FlushReady ready(String participant) {
		var r = new FlushReady();
		r.Argument.setOnzTid(1);
		r.Argument.setParticipant(participant);
		return r;
	}

	private static boolean gateOpen(Object txn) throws Exception {
		return field("flushGateOpen").getBoolean(txn);
	}

	private static boolean flushDone(Object txn) throws Exception {
		return ((TaskCompletionSource<?>)field("flushDone").get(txn)).isDone();
	}

	private static Field field(String name) throws NoSuchFieldException {
		var f = OnzTransaction.class.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	private List<String> warnMessages() {
		return appender.events.stream()
				.filter(m -> m.startsWith(Level.WARN + ":"))
				.map(m -> m.substring(m.indexOf(':') + 1))
				.toList();
	}

	/**
	 * append时立即快照为字符串：log4j2垃圾-free模式下LogEvent对象按线程复用，
	 * 按引用收集会在事后读到被复用改写的状态（实测level漂移为OFF、消息串扰）。
	 */
	private static final class CapturingAppender extends AbstractAppender {
		private final List<String> events = new java.util.concurrent.CopyOnWriteArrayList<>();

		private CapturingAppender() {
			super("flushready-capture", null, null, true, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(LogEvent event) {
			events.add(event.getLevel() + ":" + event.getMessage().getFormattedMessage());
		}
	}
}
