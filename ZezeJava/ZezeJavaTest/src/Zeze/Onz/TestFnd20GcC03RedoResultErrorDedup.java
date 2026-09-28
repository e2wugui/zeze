package Zeze.Onz;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Onz.AbstractOnz;
import Zeze.Onz.OnzServer;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import static Zeze.Onz.Fnd20GcOnzFastSupport.*;

/**
 * FND20 GC-C03 回归：redo 对非0非eSagaNotFound应答的error无按tid去重——确定性
 * 补偿失败（参与方按契约放回上下文，协调者保留记录等重试，redoDaemon每60s重发）
 * 每轮一条error永续刷屏（每tid每天1440条），而同方法同分支语义的agedNotFound
 * WarnedTids（GC-D04-C）与hangWarnedTids都有按tid去重——GC-C01(FND19)处置建议的
 * "防刷屏"句未落地。修复：通用错误分支按tid只error一次（对齐agedNotFound
 * WarnedTids形态，新增redoResultWarnedTids集合），记录收敛删除时回收tid。
 * 形态：@Fast自包含（进程内SM+OnzServer，serverId 852）+桩参与方（Net.Service
 * 监听并向SM注册"Onz"服务——redo经getZezeInstance的真实地址发现路径建连），
 * Commit应答码由volatile开关控制（100=确定性失败，0=恢复收敛）。
 */
@Fast
@ResourceLock("onz-server-logger") // 共享log4j2 OnzServer logger操纵的测试类互斥（addAppender/setLevel竞态，FND22门禁插曲）
public class TestFnd20GcC03RedoResultErrorDedup {
	// 852段：本组测试类各自的RocksDB目录/SM端口错开（852/51852），桩参与方51862。
	private static final int ServerId = 852;
	private static final int SmPort = 51852;
	private static final int StubPort = 51862;

	private static final long FailTid = 0x5CA1BEEF00000701L;
	private static final long FailTid2 = 0x5CA1BEEF00000702L;

	// Commit应答码开关（桩在IO/派发线程执行，volatile AtomicInteger供跨线程读取）
	static final AtomicInteger CommitRc = new AtomicInteger();

	private Fnd20GcOnzFastSupport.FastFixture fixture;

	@BeforeEach
	public void before(@TempDir Path tempDir) throws Exception {
		CommitRc.set(100);
		fixture = startOnzServer(ServerId, SmPort, tempDir, StubPort, CommitRc::get);
		// 参与方地址发现就绪（注册先于OnzServer构造，通常零等待；慢机兜底轮询）。
		var deadline = System.currentTimeMillis() + 30_000;
		for (;;) {
			try {
				fixture.onzServer.getZezeInstance("zeze1");
				break;
			} catch (RuntimeException e) {
				Assertions.assertTrue(System.currentTimeMillis() < deadline, "zeze1参与方地址未就绪");
				Thread.sleep(100);
			}
		}
	}

	@AfterEach
	public void after() {
		if (fixture != null) // before()半途失败时fixture尚未创建（stop幂等口径）
			fixture.close();
	}

	/**
	 * 核心红测：确定性结果错误（非0非eSagaNotFound）保留决策记录等重试，error按tid
	 * 只记一次——第二轮重发重失败不得刷屏（修复前：每轮一条，集合字段不存在）；
	 * 故障恢复（应答0）后记录收敛删除，去重项一并回收。
	 */
	@Test
	@Timeout(90)
	public void testResultErrorWarnsOncePerTidAndRecyclesOnConvergence() throws Exception {
		RocksDatabase.Table commitIndex = tableOf(fixture.onzServer, "commitIndex");
		RocksDatabase.Table commitPoint = tableOf(fixture.onzServer, "commitPoint");

		// eCommitting孤儿、procedure参与方"zeze1"（无saga=前缀）→ redo发Commit到桩。
		writeOrphanRecords(fixture.onzServer, FailTid, AbstractOnz.eCommitting, "zeze1");

		var appender = attachToOnzServerLogger();
		try {
			// 第一轮：补偿失败（rc=100）→ 保留记录等重试 + error（带tid与resultCode）。
			invokeRedoTimer(fixture.onzServer);
			Assertions.assertEquals(1, count(commitIndex),
					"非0应答必须保留决策记录等下一轮redo重试（GC-C01(FND19)行为不动）");
			Assertions.assertEquals(1, count(commitPoint), "两表同生命周期（FND4-88）：一起保留");
			Assertions.assertTrue(dedupSet(fixture.onzServer, "redoResultWarnedTids").contains(FailTid),
					"结果错误必须触发error告警并按tid登记（修复前：无去重集合，裸刷屏）");
			Assertions.assertEquals(1, appender.countErrorContaining("redo result error"),
					"第一轮恰好一条error");

			// 第二轮：记录保留驱动重发→再次失败——按tid去重，不重复error。
			invokeRedoTimer(fixture.onzServer);
			Assertions.assertEquals(1, count(commitIndex), "确定性失败：记录继续保留");
			Assertions.assertEquals(1, appender.countErrorContaining("redo result error"),
					"告警按tid去重：第二轮不再重复error（修复前每轮一条——每tid每天1440条的刷屏源）");

			// 故障恢复：应答0 → 记录收敛删除，去重项回收（集合不泄漏）。
			CommitRc.set(0);
			invokeRedoTimer(fixture.onzServer);
			Assertions.assertEquals(0, count(commitIndex), "恢复后redo完成收敛清理");
			Assertions.assertEquals(0, count(commitPoint), "两表同生命周期：一起清理");
			Assertions.assertFalse(dedupSet(fixture.onzServer, "redoResultWarnedTids").contains(FailTid),
					"记录收敛后回收告警去重项（对齐agedNotFoundWarnedTids的removeOk分支）");
			// 另一参与方决策的新故障期（新tid）：去重按tid独立——各告警一次。
			CommitRc.set(100);
			writeOrphanRecords(fixture.onzServer, FailTid2, AbstractOnz.eCommitting, "zeze1");
			invokeRedoTimer(fixture.onzServer);
			Assertions.assertEquals(1, count(commitIndex), "新故障期：新记录保留等重试");
			Assertions.assertEquals(2, appender.countErrorContaining("redo result error"),
					"不同tid各告警一次（去重粒度是tid，不压制新故障）");
			Assertions.assertEquals(1, dedupSet(fixture.onzServer, "redoResultWarnedTids").size(),
					"集合有界于在库的失败重试决策数：FailTid已回收，仅剩FailTid2一项");
		} finally {
			detachFromOnzServerLogger(appender);
		}
	}
}
