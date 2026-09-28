package Zeze.Onz;

import java.nio.file.Path;

import Zeze.Builtin.Onz.BSavedCommits;
import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Onz.AbstractOnz;
import Zeze.Onz.OnzServer;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.BServiceInfo;
import harness.Fast;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import static Zeze.Onz.Fnd20GcOnzFastSupport.*;
import static Zeze.Onz.Fnd21GcOnzFastSupport.startNonSharedOnzServer;

/**
 * FND21 GC-D01 回归：redo 参与者循环内的异常（按名解析/建连/点表/await——"第五类"滞留，
 * 异常源全部直落 redo 唯一 catch）原先走不到任何告警集合的 add 点——settleStuckRecord
 * 守卫对此不可见（FND20 三件套的清算通道对第五类不闭合），确定性滞留（死地址/除名集群/
 * 毒点表）每 60s 重放一条带栈 error（每 tid 每天 1440 条）且永不可清算；拒绝文案断言
 * "需等下一轮 redo≤60s 重新分诊"，该指引对此类永不兑现（误导性全称断言）。
 * 修复（拍板A：第四集合+年龄门槛）：redo catch 按记录年龄（既有 stamp 入参，零新状态）
 * ≥ RedoFailAgedBudgetMs（=1h，对齐 SagaNotFoundAgedBudgetMs 形态）分诊登记第四告警集合
 * redoFailWarnedTids 并按 tid 去重 error 一次（成因片段=异常类名+消息）；未达门槛保持
 * 既有 "redo fail" 全量日志（瞬态/年轻失败完全可见，行数有界：门槛/60s 轮后转去重形态）。
 * settleStuckRecord 守卫并入第四集合；removeOk 与 settle 双点同步回收；拒绝文案撤
 * "≤60s 重新分诊"全称断言、按实际分型如实指引。
 * 形态：@Fast 自包含（进程内 SM + 无通告 zeze1 集群——getZezeInstance 真实抛出路径，
 * 即立案第五类成因"集群除名/无通告"），serverId 894 段（890-893 已占），端口 51897+；
 * 自愈用例的 Commit 桩晚于协调者注册上线（TestFnd20GcD01 的晚注册形态）。
 */
@Fast
@ResourceLock("onz-server-logger") // 共享log4j2 OnzServer logger操纵的测试类互斥（addAppender/setLevel竞态，FND22门禁插曲）
public class TestGcD01RedoFailAgedSettle {
	// 894段：FND21 Gc系已占890-893，本类错开（894/51897）；Commit桩51898（仅自愈用例单方法
	// 使用，无跨方法重绑端口问题）、集群xml ServerId=895（C02的"894"只是桩identity字符串）。
	private static final int ServerId = 894;
	private static final int SmPort = 51897;
	private static final int CommitStubPort = 51898;
	private static final int ClusterConfigServerId = 895;

	private static final long AgedTid = 0x5CA1BEEF00000901L; // 超龄redo失败（第五类主路径）
	private static final long YoungTid = 0x5CA1BEEF00000902L; // 未达龄redo失败（门槛防误登记）
	private static final long SelfHealTid = 0x5CA1BEEF00000903L; // 瞬态自愈（removeOk回收）

	@TempDir
	Path tempDir;

	/**
	 * 主路径红测：超龄 redo 失败分诊登记第四集合（error 一次含成因片段，后续轮次静默防
	 * 刷屏），记录保留等人工清算；settleStuckRecord 守卫放行（清算通道对第五类闭合）、
	 * 两表原子删除、第四集合回收；反向：分诊前拒绝且文案按分型如实指引（撤"≤60s 重新
	 * 分诊"全称断言——它对此类永不兑现：其 redo 路径到分诊年龄前走不到集合 add 点）。
	 */
	@Test
	@Timeout(90)
	public void testAgedRedoFailDiagnosedDedupThenSettled() throws Exception {
		// 无通告zeze1（零桩）：getZezeInstance真实抛出（除名集群/无通告=第五类稳定成因）
		try (var f = startNonSharedOnzServer(ServerId, SmPort, tempDir, "zeze1", ClusterConfigServerId)) {
			var commitIndex = tableOf(f.onzServer, "commitIndex");
			var commitPoint = tableOf(f.onzServer, "commitPoint");
			// 超龄eCommitting孤儿+procedure参与方"zeze1"：redo直入参与者解析即抛，年龄超1h门槛。
			writeAgedOrphan(f.onzServer, AgedTid, AbstractOnz.eCommitting, "zeze1",
					Zeze.Onz.Onz.eDefaultSagaContextTimeoutMs + 100_000);
			Assertions.assertEquals(1, count(commitIndex), "前置：索引条目就位");

			var originLevel = onzServerLogger().getLevel();
			var appender = attachAuditCapture();
			try {
				// 反向：redo未跑过（未分诊）——守卫拒绝，文案按分型如实指引（撤全称断言）。
				Assertions.assertFalse(f.onzServer.settleStuckRecord(AgedTid), "未被报告滞留的tid必须拒绝");
				var rejected = firstErrorMessageContaining(appender, "不是协调者已报告滞留");
				Assertions.assertTrue(rejected.contains("redo失败未达龄"),
						"拒绝文案必须按实际分型指引（进行中/未分诊/redo失败未达龄）: " + rejected);
				Assertions.assertFalse(rejected.contains("60s"),
						"撤掉\"≤60s重新分诊\"全称断言——它对redo失败类永不兑现: " + rejected);
				Assertions.assertEquals(1, count(commitIndex), "拒绝不得动索引表");
				Assertions.assertEquals(1, count(commitPoint), "拒绝不得动点表（两表同生命周期）");

				// 分诊：超龄异常登记第四集合+error一次（成因片段），不走"redo fail"全量形态。
				invokeRedoTimer(f.onzServer);
				Assertions.assertTrue(dedupSet(f.onzServer, "redoFailWarnedTids").contains(AgedTid),
						"超龄redo失败分诊登记第四告警集合");
				Assertions.assertEquals(1, count(commitIndex), "记录保留等人工清算（守卫可达终点的前提）");
				Assertions.assertEquals(1, count(commitPoint), "两表同生命周期：一起保留");
				var dedupError = firstErrorMessageContaining(appender, "redo fail aged");
				Assertions.assertTrue(dedupError.contains("tid=" + AgedTid) && dedupError.contains("age=")
								&& dedupError.contains("cause=java.lang.RuntimeException") && dedupError.contains("zeze1"),
						"分诊error含成因片段（异常类名+消息——诊断\"先修参与方还是改库\"的依据）: " + dedupError);

				// 去重：第二轮同型异常不再刷error（重放无新信息，现状每tid每天1440条→首条）。
				invokeRedoTimer(f.onzServer);
				Assertions.assertEquals(1, appender.countErrorContaining("redo fail aged"), "按tid去重：每tid只error一次");
				Assertions.assertEquals(0, appender.countErrorContaining("redo fail. tid=" + AgedTid),
						"超龄后不再走年轻形态的全量带栈日志");
				Assertions.assertEquals(1, count(commitIndex), "去重不影响记录保留");

				// 清算：守卫放行（第五类闭合）——审计留痕+原子双删+第四集合回收。
				Assertions.assertTrue(f.onzServer.settleStuckRecord(AgedTid), "守卫放行已分诊的redo失败滞留tid");
				Assertions.assertEquals(0, count(commitIndex), "清算删除索引条目（removeCommitRecord单batch原子双删）");
				Assertions.assertEquals(0, count(commitPoint), "两表同batch一起删除（FND4-88）");
				Assertions.assertFalse(dedupSet(f.onzServer, "redoFailWarnedTids").contains(AgedTid),
						"记录关闭后回收第四集合去重项（对齐removeOk分支）");
				var audit = firstWarnMessageContaining(appender, "清算滞留决策记录");
				Assertions.assertTrue(audit.contains("tid=" + AgedTid) && audit.contains("onzs=[zeze1]"),
						"删前审计留痕tid/参与方清单: " + audit);
				Assertions.assertFalse(f.onzServer.settleStuckRecord(AgedTid), "集合已回收：重复清算被拒（一次性）");
			} finally {
				restoreAuditCapture(originLevel, appender);
			}
		}
	}

	/**
	 * 门槛反向（未达龄）：年轻失败保持既有 "redo fail" 全量日志（瞬态/年轻失败完全可见，
	 * 每轮一条），不登记第四集合——年龄即持续失败时长的下界证据，未超预算说明还可能是
	 * 瞬态失败，无"协调者已报告滞留"证据不可清算（守卫下限不动，误删进行中事务唯一
	 * 收敛通道仍不可达）。
	 */
	@Test
	@Timeout(90)
	public void testYoungRedoFailKeepsFullLogUnregistered() throws Exception {
		try (var f = startNonSharedOnzServer(ServerId, SmPort, tempDir, "zeze1", ClusterConfigServerId)) {
			var commitIndex = tableOf(f.onzServer, "commitIndex");
			// 年龄60s（<1h门槛；eCommitting无ePreparing的120s年龄闸，redo立即执行）
			writeAgedOrphan(f.onzServer, YoungTid, AbstractOnz.eCommitting, "zeze1", 60_000);
			var appender = attachToOnzServerLogger();
			try {
				invokeRedoTimer(f.onzServer);
				Assertions.assertEquals(1, appender.countErrorContaining("redo fail. tid=" + YoungTid),
						"未达门槛保持既有全量\"redo fail\"日志（年轻失败完全可见）");
				Assertions.assertEquals(0, appender.countErrorContaining("redo fail aged"),
						"未达门槛不得转分诊形态（持续失败时长证据还不够）");
				Assertions.assertFalse(dedupSet(f.onzServer, "redoFailWarnedTids").contains(YoungTid),
						"第四集合不登记年轻失败（防瞬态误登记）");
				Assertions.assertEquals(1, count(commitIndex), "失败轮保留记录（既有行为不动）");

				// 门槛内每轮重放全量（无去重）——与超龄后的去重形态对照。
				invokeRedoTimer(f.onzServer);
				Assertions.assertEquals(2, appender.countErrorContaining("redo fail. tid=" + YoungTid),
						"门槛内每轮全量可见（去重只在超龄分诊后生效）");
				Assertions.assertFalse(f.onzServer.settleStuckRecord(YoungTid), "未达龄不可清算（守卫下限不动）");
			} finally {
				detachFromOnzServerLogger(appender);
			}
		}
	}

	/**
	 * 自愈回收（removeOk双回收点之一）：超龄失败已登记第四集合后参与方恢复（无通告→
	 * Commit桩注册上线），下一轮 redo 全 0 → removeOk 删记录并同步回收第四集合——瞬态
	 * 失败不永久占据集合（集合有界于在库的异常滞留决策数，自愈语义与设计"年龄门槛的
	 * 已知不精确"论证闭环：登记不阻塞收敛）。
	 */
	@Test
	@Timeout(90)
	public void testTransientSelfHealRecyclesRedoFailTid() throws Exception {
		try (var f = startNonSharedOnzServer(ServerId, SmPort, tempDir, "zeze1", ClusterConfigServerId)) {
			var commitIndex = tableOf(f.onzServer, "commitIndex");
			var commitPoint = tableOf(f.onzServer, "commitPoint");
			writeAgedOrphan(f.onzServer, SelfHealTid, AbstractOnz.eCommitting, "zeze1",
					Zeze.Onz.Onz.eDefaultSagaContextTimeoutMs + 100_000);
			invokeRedoTimer(f.onzServer); // 无通告→分诊登记
			Assertions.assertTrue(dedupSet(f.onzServer, "redoFailWarnedTids").contains(SelfHealTid), "前置：超龄失败已分诊");

			// 参与方恢复：Commit桩晚注册上线（传播窗口由waitZezeInstanceReady轮询兜底）。
			var stub = startCommitStub();
			Agent agent = null;
			try {
				agent = registerStubToSm("895", CommitStubPort);
				waitZezeInstanceReady(f.onzServer);
				invokeRedoTimer(f.onzServer); // Commit应答0→removeOk
				Assertions.assertEquals(0, count(commitIndex), "参与方恢复后redo收敛删除记录");
				Assertions.assertEquals(0, count(commitPoint), "两表同batch删除");
				Assertions.assertFalse(dedupSet(f.onzServer, "redoFailWarnedTids").contains(SelfHealTid),
						"removeOk双回收点之一：自愈后tid离开第四集合");
			} finally {
				if (agent != null)
					agent.stop();
				stub.stop();
			}
		}
	}

	/** Commit桩参与方（自愈用例专用）：eCommitting的redo发Commit（sendRedoDecision按参与方类型分流），显式应答0。 */
	private static Service startCommitStub() throws Exception {
		var stub = new Service("Fnd21GcD01CommitStub", new Config());
		stub.AddFactoryHandle(Zeze.Builtin.Onz.Commit.TypeId_,
				new Service.ProtocolFactoryHandle<>(Zeze.Builtin.Onz.Commit::new, r -> {
					r.SendResult(); // 框架仅在非0时回发错误码（TaskSpec契约），0需显式应答
					return 0;
				}, Zeze.Transaction.TransactionLevel.None, Zeze.Transaction.DispatchMode.Direct));
		stub.newServerSocket("127.0.0.1", CommitStubPort, null);
		stub.start();
		return stub;
	}

	/** 桩服务向fixture的进程内SM注册"Onz"服务（参与方地址发现走getZezeInstance真实路径）。 */
	private static Agent registerStubToSm(String identity, int stubPort) throws Exception {
		var agent = new Agent(new Config());
		agent.getClient().getConfig().addConnector(new Connector("127.0.0.1", SmPort));
		agent.start();
		agent.waitReady();
		agent.registerService(new BServiceInfo("Onz", identity, 0, "127.0.0.1", stubPort));
		return agent;
	}

	/** 参与方地址发现就绪（晚注册传播窗口的兜底轮询，对齐TestFnd20GcD01同型助手）。 */
	private static void waitZezeInstanceReady(OnzServer onzServer) throws Exception {
		var deadline = System.currentTimeMillis() + 30_000;
		for (;;) {
			try {
				onzServer.getZezeInstance("zeze1");
				return;
			} catch (RuntimeException e) {
				Assertions.assertTrue(System.currentTimeMillis() < deadline, "zeze1参与方地址未就绪");
				Thread.sleep(100);
			}
		}
	}

	/** 手写超龄孤儿决策两表（TestFnd20GcD01的writeAgedOrphan同型）：ageMs=索引时戳回拨量。 */
	private static void writeAgedOrphan(OnzServer onzServer, long tid, int state, String onzs, long ageMs) throws Exception {
		var key = keyOf(tid);
		var saved = new BSavedCommits.Data();
		if (onzs != null)
			saved.getOnzs().add(onzs);
		var bbState = ByteBuffer.Allocate();
		saved.encode(bbState);
		tableOf(onzServer, "commitPoint").put(key, java.util.Arrays.copyOf(bbState.Bytes, bbState.WriteIndex));
		tableOf(onzServer, "commitIndex").put(key, indexValue(state, ageMs));
	}

	/** 首条含子串的ERROR消息全文；找不到返回空串令contains断言失败。 */
	private static String firstErrorMessageContaining(CaptureAppender appender, String substring) {
		return appender.events.stream()
				.filter(e -> e.getLevel() == Level.ERROR)
				.map(e -> e.getMessage().getFormattedMessage())
				.filter(m -> m.contains(substring))
				.findFirst().orElse("");
	}

	/** 首条含子串的WARN消息全文（审计留痕内容断言）；找不到返回空串令contains断言失败。 */
	private static String firstWarnMessageContaining(CaptureAppender appender, String substring) {
		return appender.events.stream()
				.filter(e -> e.getLevel() == Level.WARN)
				.map(e -> e.getMessage().getFormattedMessage())
				.filter(m -> m.contains(substring))
				.findFirst().orElse("");
	}

	private static org.apache.logging.log4j.core.Logger onzServerLogger() {
		return (org.apache.logging.log4j.core.Logger)org.apache.logging.log4j.LogManager.getLogger(OnzServer.class);
	}

	/**
	 * 挂载捕获并把OnzServer的logger级别压到WARN：隔离车道classpath无log4j2配置时根级别
	 * 为ERROR，WARN级审计事件在到达appender前即被过滤（ERROR断言不受影响）。必须先挂
	 * appender再压级——addAppender首次调用会为该logger名新建LoggerConfig并拷贝父级级别，
	 * 先setLevel会被这次拷贝覆盖回ERROR。finally配restore还原（原级别可能是null=继承）。
	 * （对齐TestFnd20GcD01同名助手形态。）
	 */
	private static CaptureAppender attachAuditCapture() {
		var appender = attachToOnzServerLogger();
		onzServerLogger().setLevel(Level.WARN);
		return appender;
	}

	private static void restoreAuditCapture(org.apache.logging.log4j.Level origin, CaptureAppender appender) {
		detachFromOnzServerLogger(appender);
		onzServerLogger().setLevel(origin);
	}
}
