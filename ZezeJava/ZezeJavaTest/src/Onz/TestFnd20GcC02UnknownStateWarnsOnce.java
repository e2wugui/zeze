package Onz;

import java.nio.file.Path;
import Zeze.Onz.AbstractOnz;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import static Onz.Fnd20GcOnzFastSupport.*;

/**
 * FND20 GC-C02 回归：redoTimer 的 state switch 无 default 分支——值可解但
 * state∉{ePreparing,eCommitting}的条目（损坏但未截断的垃圾值/未来版本前向写入降级
 * 运行）每轮静默跳过，记录永不清算且零信号（同函数hang/超龄NotFound/毒值均有告警，
 * 唯此支没有；redo轮固定遍历负担与两表条目单调增长）。修复：补 default 分支，按tid
 * 只error一次（对齐agedNotFoundWarnedTids形态）后跳过，留库人工排查。
 * 形态：@Fast自包含（进程内SM+OnzServer，serverId 851）；state=9 的索引条目直注 +
 * 空参与方有效孤儿（redo无网络即收敛）验证迭代继续推进。
 */
@Fast
public class TestFnd20GcC02UnknownStateWarnsOnce {
	// 851段：本组测试类各自的RocksDB目录/SM端口错开（851/51851）。
	private static final int ServerId = 851;
	private static final int SmPort = 51851;

	// 迭代序：未知state(0601) < 有效eCommitting孤儿(0602)——有效记录在未知条目之后，
	// 迭代必须越过未知条目继续推进它才收敛。
	private static final long UnknownStateTid = 0x5CA1BEEF00000601L;
	private static final long ValidTid = 0x5CA1BEEF00000602L;

	private Fnd20GcOnzFastSupport.FastFixture fixture;

	@BeforeEach
	public void before(@TempDir Path tempDir) throws Exception {
		fixture = startOnzServer(ServerId, SmPort, tempDir, 0, null);
	}

	@AfterEach
	public void after() {
		fixture.close();
	}

	/**
	 * 核心红测：未知state条目必须跳过不redo（语义未知，盲目补发决策可能制造分歧），
	 * 且按tid只error一次（修复前：静默跳过零信号，条目滞留不可观测）。对照：排序在后的
	 * 有效决策照常redo收敛（default分支不中止迭代），未知条目永不清算（去重集合随之常驻）。
	 */
	@Test
	@Timeout(60)
	public void testUnknownStateSkipsWithSignalWarnsOnceAndKeepsIteration() throws Exception {
		RocksDatabase.Table commitIndex = tableOf(fixture.onzServer, "commitIndex");
		RocksDatabase.Table commitPoint = tableOf(fixture.onzServer, "commitPoint");

		// 未知state=9：值长度足够的"完好"垃圾（单字节varint可解，不触发GC-C01的截断抛出）。
		writeIndexEntry(fixture.onzServer, keyOf(UnknownStateTid), indexValue(9, 121_000));
		// 有效条目：eCommitting孤儿、空参与方。
		writeOrphanRecords(fixture.onzServer, ValidTid, AbstractOnz.eCommitting, null);
		Assertions.assertEquals(2, count(commitIndex), "前置：两条索引条目就位");

		var appender = attachToOnzServerLogger();
		try {
			invokeRedoTimer(fixture.onzServer);

			Assertions.assertEquals(1, count(commitIndex),
					"未知state条目留库人工排查（不盲目redo）、有效记录被redo清理"
							+ "（修复前default缺失是静默跳过：本断言同样过，红在零信号）");
			Assertions.assertEquals(0, count(commitPoint), "有效记录的点表随索引清理，未知条目无点条目");
			Assertions.assertTrue(dedupSet(fixture.onzServer, "unknownStateWarnedTids").contains(UnknownStateTid),
					"未知state必须触发error告警（修复前：静默跳过零信号，且集合字段不存在）");
			Assertions.assertEquals(1, appender.countErrorContaining("条目state=9 未知"),
					"第一轮恰好一条error（带tid与state值）");

			// 第二轮redo：条目仍在（永不清算），按tid去重不再重复告警。
			invokeRedoTimer(fixture.onzServer);
			Assertions.assertEquals(1, count(commitIndex), "未知state条目维持留库");
			Assertions.assertEquals(1, appender.countErrorContaining("条目state=9 未知"),
					"告警按tid去重：第二轮不再重复error（修复前每轮静默，修复漏去重则每轮一条）");
			Assertions.assertEquals(1, dedupSet(fixture.onzServer, "unknownStateWarnedTids").size(),
					"集合有界：每未知条目恰一项");
		} finally {
			detachFromOnzServerLogger(appender);
		}
	}
}
