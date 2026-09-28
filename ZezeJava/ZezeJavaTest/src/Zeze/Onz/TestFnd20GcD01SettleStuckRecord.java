package Zeze.Onz;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import Zeze.Builtin.Onz.BSavedCommits;
import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Onz.AbstractOnz;
import Zeze.Onz.OnzServer;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import static Zeze.Onz.Fnd20GcOnzFastSupport.*;

/**
 * FND20 GC-D01 回归：GC-D04-C"保留 + 曝光"（超龄NotFound分诊）及同形的 GC-C02（未知
 * state）/GC-C03（确定性补偿失败）滞留决策记录没有可达的终点——removeCommitRecord 无
 * 外部调用方，人工清算只剩停进程直改 RocksDB 双表（无 batch 原子性），保留记录除外部
 * 干预不可收敛。修复（拍板A：守卫式本地清算）：OnzServer.settleStuckRecord(tid)——
 * 守卫只放行三个滞留告警集合（agedNotFound/redoResult/unknownStateWarnedTids）中的
 * tid（协调者自己已报告滞留的证据；误删进行中事务唯一收敛通道不可达），dbLock 域内
 * stopped 双检，删前读两表原值记审计日志（被放弃的补偿对象的最后留痕），复用
 * removeCommitRecord 单 batch 原子双删，三集合同步回收（unknownState 补上既有回收缺口）。
 * 形态：@Fast自包含（进程内SM+OnzServer，serverId 856）；agedNotFound 路径用自建
 * Rollback 桩（ePreparing 的 redo 发 Rollback，按 eSagaNotFound 组合码应答，真实分诊
 * 路径），redoResult 路径复用 support 的 Commit 桩（rc=100），unknownState/stopped 为
 * 纯本地路径；反向验证守卫拒绝（未分诊 tid 不放行、终态服务器拒绝）。
 */
@Fast
@ResourceLock("onz-server-logger") // 共享log4j2 OnzServer logger操纵的测试类互斥（addAppender/setLevel竞态，FND22门禁插曲）
public class TestFnd20GcD01SettleStuckRecord {
	// 856段：FND20 Gc系已占850-852，本类错开（856/51856）；Commit桩51866、Rollback桩51867。
	private static final int ServerId = 856;
	private static final int SmPort = 51856;
	private static final int CommitStubPort = 51866;
	private static final int RollbackStubPort = 51867;

	// eSagaNotFound的线上组合码形态（moduleId<<32|code，与Onz参与方侧errorCode(eSagaNotFound)
	// 同构）：redo用IModule.getErrorCode(resultCode)解码比较（OnzServer.redo的NotFound分支）。
	private static final long SagaNotFoundRc = Zeze.IModule.errorCode(AbstractOnz.ModuleId, AbstractOnz.eSagaNotFound);

	private static final long AgedTid = 0x5CA1BEEF00000801L; // 超龄NotFound滞留（GC-D04-C主路径）
	private static final long UnknownTid = 0x5CA1BEEF00000802L; // 未知state滞留（GC-C02）
	private static final long FailTid = 0x5CA1BEEF00000803L; // 确定性补偿失败滞留（GC-C03）
	private static final long StoppedTid = 0x5CA1BEEF00000804L; // 终态服务器拒绝

	// 桩应答码开关（桩在IO/派发线程执行，跨线程读取用原子量）。
	static final AtomicInteger CommitRc = new AtomicInteger(); // support桩（Commit）
	static final AtomicLong RollbackRc = new AtomicLong(); // 本类自建桩（Rollback），组合码是long

	@TempDir
	Path tempDir;

	/**
	 * 主路径红测（超龄NotFound）：守卫先拒绝未分诊的tid（记录不动、error说明成因），
	 * redo真实分诊（Rollback→eSagaNotFound组合码+记录超龄）登记滞留后，清算放行——
	 * 删前审计留痕（tid/原state/参与方清单）、两表原子双删、告警集合回收；集合回收后
	 * 重复清算再次被守卫拒绝（清算按分诊一次性）。
	 */
	@Test
	@Timeout(90)
	public void testGuardRejectsUndiagnosedThenSettlesAgedNotFound() throws Exception {
		try (var f = startOnzServer(ServerId, SmPort, tempDir, 0, null)) {
			var stub = startRollbackStub();
			Agent agent = null;
			try {
				agent = registerStubToSm("858", RollbackStubPort);
				waitZezeInstanceReady(f.onzServer);

				var commitIndex = tableOf(f.onzServer, "commitIndex");
				var commitPoint = tableOf(f.onzServer, "commitPoint");
				// 超龄rollback孤儿：ePreparing（rollback决策）+procedure参与方"zeze1"（无saga=前缀
				// → redo发Rollback），年龄超SagaNotFoundAgedBudgetMs（=eDefaultSagaContextTimeoutMs）。
				writeAgedOrphan(f.onzServer, AgedTid, AbstractOnz.ePreparing, "zeze1",
						Zeze.Onz.Onz.eDefaultSagaContextTimeoutMs + 100_000);
				Assertions.assertEquals(1, count(commitIndex), "前置：索引条目就位");

				var originLevel = onzServerLogger().getLevel();
				var appender = attachAuditCapture();
				try {
					// 反向：记录滞留但redo未跑过（未分诊）——不在任何告警集合，守卫必须拒绝。
					Assertions.assertFalse(f.onzServer.settleStuckRecord(AgedTid),
							"未被报告滞留的tid必须拒绝（进行中/未分诊：误删活事务唯一收敛通道不可达）");
					Assertions.assertEquals(1, count(commitIndex), "拒绝不得动索引表");
					Assertions.assertEquals(1, count(commitPoint), "拒绝不得动点表（两表同生命周期）");
					Assertions.assertEquals(1, appender.countErrorContaining("不是协调者已报告滞留"),
							"拒绝必须error说明成因（含等下一轮redo重新分诊的指引）");

					// 分诊（GC-D04-C真实路径不动）：Rollback应答eSagaNotFound组合码+记录超龄
					// → 保留决策记录+登记agedNotFoundWarnedTids。
					RollbackRc.set(SagaNotFoundRc);
					invokeRedoTimer(f.onzServer);
					Assertions.assertEquals(1, count(commitIndex), "超龄NotFound保留决策记录（GC-D04-C行为不动）");
					Assertions.assertEquals(1, count(commitPoint), "两表同生命周期：一起保留");
					Assertions.assertTrue(dedupSet(f.onzServer, "agedNotFoundWarnedTids").contains(AgedTid),
							"分诊登记滞留告警集合");

				// 清算（本案新增）：守卫放行已分诊tid——审计留痕+原子双删+集合回收。
				Assertions.assertTrue(f.onzServer.settleStuckRecord(AgedTid), "守卫放行已分诊的滞留tid");
					Assertions.assertEquals(0, count(commitIndex), "清算删除索引条目（removeCommitRecord单batch原子双删）");
					Assertions.assertEquals(0, count(commitPoint), "两表同batch一起删除（FND4-88）");
					Assertions.assertFalse(dedupSet(f.onzServer, "agedNotFoundWarnedTids").contains(AgedTid),
							"记录关闭后回收告警去重项（对齐removeOk分支）");
					var audit = firstWarnMessageContaining(appender, "清算滞留决策记录");
					Assertions.assertTrue(audit.contains("tid=" + AgedTid) && audit.contains("state=1")
									&& audit.contains("onzs=[zeze1]") && audit.contains("age="),
							"删前审计留痕tid/原state/年龄/参与方清单（被放弃的补偿对象的最后留痕）: " + audit);
					Assertions.assertFalse(f.onzServer.settleStuckRecord(AgedTid),
							"集合已回收：重复清算被守卫拒绝（清算按分诊一次性）");
				} finally {
					restoreAuditCapture(originLevel, appender);
				}
			} finally {
				if (agent != null)
					agent.stop();
				stub.stop();
			}
		}
	}

	/**
	 * 汇流路径（GC-C02）：未知state滞留条目（可无点条目）清算放行，审计如实记录缺失的
	 * 点条目，unknownStateWarnedTids 随清算回收——该集合此前无回收点（条目永不redo收敛），
	 * 本方法是唯一回收通道，集合不再单调增长。
	 */
	@Test
	@Timeout(60)
	public void testSettleUnknownStateRecyclesDedupSet() throws Exception {
		try (var f = startOnzServer(ServerId, SmPort, tempDir, 0, null)) {
			var commitIndex = tableOf(f.onzServer, "commitIndex");
			// 未知state=9（GC-C02形态）：仅索引条目、无点条目。
			writeIndexEntry(f.onzServer, keyOf(UnknownTid), indexValue(9, 121_000));
			invokeRedoTimer(f.onzServer); // 分诊：error一次+登记集合，条目留库
			Assertions.assertEquals(1, count(commitIndex), "未知state条目留库人工排查（GC-C02行为不动）");
			Assertions.assertTrue(dedupSet(f.onzServer, "unknownStateWarnedTids").contains(UnknownTid), "前置：已分诊");

			var originLevel = onzServerLogger().getLevel();
			var appender = attachAuditCapture();
			try {
				Assertions.assertTrue(f.onzServer.settleStuckRecord(UnknownTid), "守卫放行unknownState滞留tid");
				Assertions.assertEquals(0, count(commitIndex), "清算删除索引条目");
				Assertions.assertFalse(dedupSet(f.onzServer, "unknownStateWarnedTids").contains(UnknownTid),
						"GC-C02的tid回收缺口闭合：集合成员随清算回收（不再\"按设计永不回收\"）");
				var audit = firstWarnMessageContaining(appender, "清算滞留决策记录");
				Assertions.assertTrue(audit.contains("state=9") && audit.contains("无点条目"),
						"审计如实留痕原state与缺失的点条目: " + audit);
				Assertions.assertFalse(f.onzServer.settleStuckRecord(UnknownTid), "集合回收后再次拒绝");
			} finally {
				restoreAuditCapture(originLevel, appender);
			}
		}
	}

	/**
	 * 汇流路径（GC-C03）：确定性补偿失败（Commit应答rc=100）滞留记录清算放行，两表删除、
	 * redoResultWarnedTids 回收——守卫对三个滞留证据集合的放行一致（同一套集合的读写）。
	 */
	@Test
	@Timeout(90)
	public void testSettleRedoResultStuckRecord() throws Exception {
		CommitRc.set(100);
		try (var f = startOnzServer(ServerId, SmPort, tempDir, CommitStubPort, CommitRc::get)) {
			waitZezeInstanceReady(f.onzServer);
			var commitIndex = tableOf(f.onzServer, "commitIndex");
			var commitPoint = tableOf(f.onzServer, "commitPoint");
			// eCommitting孤儿+procedure参与方"zeze1" → redo发Commit到桩，rc=100确定性失败。
			writeAgedOrphan(f.onzServer, FailTid, AbstractOnz.eCommitting, "zeze1", 121_000);
			invokeRedoTimer(f.onzServer); // 分诊：保留记录等重试+登记redoResultWarnedTids
			Assertions.assertEquals(1, count(commitIndex), "确定性失败保留决策记录等重试（GC-C03行为不动）");
			Assertions.assertTrue(dedupSet(f.onzServer, "redoResultWarnedTids").contains(FailTid), "前置：已分诊");

			Assertions.assertTrue(f.onzServer.settleStuckRecord(FailTid), "守卫放行redoResult滞留tid");
			Assertions.assertEquals(0, count(commitIndex), "清算原子双删（索引）");
			Assertions.assertEquals(0, count(commitPoint), "清算原子双删（点表）");
			Assertions.assertFalse(dedupSet(f.onzServer, "redoResultWarnedTids").contains(FailTid), "集合回收");
		}
	}

	/**
	 * 反向（终态）：stop() 后库已关，清算必须拒绝且不得触碰库（不抛异常）——stopped
	 * 检查先于任何库访问（对齐redoTimer头部守卫，与stop的关库互斥）。
	 */
	@Test
	@Timeout(60)
	public void testSettleRejectedAfterStop() throws Exception {
		try (var f = startOnzServer(ServerId, SmPort, tempDir, 0, null)) {
			writeIndexEntry(f.onzServer, keyOf(StoppedTid), indexValue(9, 121_000));
			invokeRedoTimer(f.onzServer); // 分诊登记（纯本地路径，守卫可通过）
			Assertions.assertTrue(dedupSet(f.onzServer, "unknownStateWarnedTids").contains(StoppedTid), "前置：已分诊");

			f.onzServer.stop(); // 终态；fixture.close()的stop幂等
			var appender = attachToOnzServerLogger();
			try {
				Assertions.assertFalse(f.onzServer.settleStuckRecord(StoppedTid),
						"终态服务器必须拒绝（守卫通过也不行：库已关，stopped双检先于任何库访问）");
				Assertions.assertEquals(1, appender.countErrorContaining("OnzServer stopped"), "拒绝原因可观测");
			} finally {
				detachFromOnzServerLogger(appender);
			}
		}
	}

	/**
	 * Rollback桩参与方（agedNotFound路径专用）：ePreparing的redo发Rollback（sendRedoDecision
	 * 按参与方类型分流），应答码由RollbackRc控制（0=显式SendResult，非0=框架回发该码）。
	 * support桩只应答Commit，本桩补Rollback；晚于OnzServer构造注册到SM，订阅传播由
	 * waitZezeInstanceReady轮询等待。
	 */
	private static Service startRollbackStub() throws Exception {
		var stub = new Service("Fnd20GcD01RollbackStub", new Config());
		stub.AddFactoryHandle(Zeze.Builtin.Onz.Rollback.TypeId_,
				new Service.ProtocolFactoryHandle<>(Zeze.Builtin.Onz.Rollback::new, r -> {
					var rc = RollbackRc.get();
					if (rc == 0)
						r.SendResult(); // 框架仅在非0时回发错误码（TaskSpec契约），0需显式应答
					return rc;
				}, Zeze.Transaction.TransactionLevel.None, Zeze.Transaction.DispatchMode.Direct));
		stub.newServerSocket("127.0.0.1", RollbackStubPort, null);
		stub.start();
		return stub;
	}

	/** 桩服务向fixture的进程内SM注册"Onz"服务（参与方地址发现走getZezeInstance真实路径）。 */
	private Agent registerStubToSm(String identity, int stubPort) throws Exception {
		var agent = new Agent(new Config());
		agent.getClient().getConfig().addConnector(new Connector("127.0.0.1", SmPort));
		agent.start();
		agent.waitReady();
		agent.registerService(new BServiceInfo("Onz", identity, 0, "127.0.0.1", stubPort));
		return agent;
	}

	/** 参与方地址发现就绪（晚注册传播窗口的兜底轮询，对齐C03的before轮询形态）。 */
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

	/** 手写超龄孤儿决策两表（TestFnd19GcD04的ageMs版）：ageMs=索引时戳回拨量（support版定龄121s不够超龄分诊）。 */
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

	/** 首条含子串的WARN消息全文（审计留痕内容断言）；找不到返回空串令contains断言失败。 */
	private static String firstWarnMessageContaining(Fnd20GcOnzFastSupport.CaptureAppender appender, String substring) {
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
	 * 先setLevel会被这次拷贝覆盖回ERROR（首次之后则保留）。finally配
	 * {@code restoreAuditCapture(原级别, appender)}还原（原级别可能是null=继承）。
	 */
	private static Fnd20GcOnzFastSupport.CaptureAppender attachAuditCapture() {
		var appender = attachToOnzServerLogger();
		onzServerLogger().setLevel(Level.WARN);
		return appender;
	}

	private static void restoreAuditCapture(org.apache.logging.log4j.Level origin,
											Fnd20GcOnzFastSupport.CaptureAppender appender) {
		detachFromOnzServerLogger(appender);
		onzServerLogger().setLevel(origin);
	}
}
