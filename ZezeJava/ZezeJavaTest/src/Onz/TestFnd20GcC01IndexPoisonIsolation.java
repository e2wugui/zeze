package Onz;

import java.nio.file.Path;
import Zeze.Onz.AbstractOnz;
import Zeze.Onz.OnzServer;
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
 * FND20 GC-C01 回归：redoTimer 循环体的裸解码（226行state的ReadUInt、227行tid的
 * ToLongBE）处于所有单条容错之外——索引自身的值空/截断（bit rot/半写，commitIndex
 * 与commitPoint同batch写入、受损暴露面相同）或key短于8字节（人工修库）时异常冲出
 * 循环中止整个commitIndex遍历，周期路径被DaemonTimer.runBody吞掉后每轮从头再撞同
 * 一条，排序在后的未决决策redo永久停滞；启动路径start()首轮同样只处理毒条目之前
 * 的部分。GC-C02(FND19)只闭合了redo内层的点表侧隔离（requireNonNull/decode），索引
 * 侧同型洞仍在——属fix-the-fix。修复：单条处理整体包try/catch(Throwable)，毒条目
 * 记error（带key/tid）后跳过留库人工排查，其后记录照常收敛（对齐ApplyHelper逐记录
 * 隔离形态，与redo内层构成两层防御）。
 * 形态：@Fast自包含（进程内SM+OnzServer，serverId 850段）；索引侧直注毒条目
 * （FND19 writeIndexOnly示范的合法操作）；有效孤儿的参与方列表为空——redo无网络
 * 即完成收敛（removeCommitRecord），迭代推进与否完全由毒条目隔离决定。
 */
@Fast
public class TestFnd20GcC01IndexPoisonIsolation {
	// 850段：@Fast类并行时本地RocksDB目录与其他测试类（800/810/820段）互不冲突。
	private static final int ServerId = 850;
	private static final int SmPort = 51850;

	// 迭代序（key字节序）：毒短key(0x01..) < 毒空值(tid 0502) < 有效(tid 0503)——
	// 两条毒都在有效记录之前，修复前迭代根本轮不到有效记录。
	private static final long EmptyValueTid = 0x5CA1BEEF00000502L; // 索引值空 → ReadUInt抛
	private static final long ValidTid = 0x5CA1BEEF00000503L; // 有效eCommitting孤儿 → redo必须收敛清理

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
	 * 核心红测：索引侧毒条目（短key/空值）必须被单条隔离跳过，不得中止迭代——排序在后的
	 * 有效eCommitting决策照常redo收敛。修复前：毒条目的解码异常冲出redoTimer（裸解码在
	 * 所有容错之外），反射驱动直接抛InvocationTargetException，三表记录全部滞留。
	 */
	@Test
	@Timeout(60)
	public void testIndexPoisonIsolatedNotBlockingIteration() throws Exception {
		RocksDatabase.Table commitIndex = tableOf(fixture.onzServer, "commitIndex");
		RocksDatabase.Table commitPoint = tableOf(fixture.onzServer, "commitPoint");

		// 毒条目1：3字节key（修库写短key形态）——值完好，ReadUInt通过，ToLongBE越界抛。
		writeIndexEntry(fixture.onzServer, new byte[]{0x01, 0x02, 0x03},
				indexValue(AbstractOnz.eCommitting, 121_000));
		// 毒条目2：8字节key、值空（bit rot/半写形态）→ ReadUInt抛。
		writeIndexEntry(fixture.onzServer, keyOf(EmptyValueTid), new byte[0]);
		// 有效条目：eCommitting孤儿、空参与方 → redo迭代到达即收敛删除。
		writeOrphanRecords(fixture.onzServer, ValidTid, AbstractOnz.eCommitting, null);
		Assertions.assertEquals(3, count(commitIndex), "前置：三条索引条目就位");

		var appender = attachToOnzServerLogger();
		try {
			// 修复前：invokeRedoTimer在毒条目1上抛InvocationTargetException（异常冲出
			// redoTimer），有效记录永轮不到——本行即红。
			invokeRedoTimer(fixture.onzServer);

			Assertions.assertEquals(2, count(commitIndex),
					"毒条目留库人工排查、有效记录被redo清理（修复前：3条全滞留）");
			Assertions.assertEquals(0, count(commitPoint),
					"有效记录的点表随索引一起清理（两表同生命周期），毒条目无点条目");
			Assertions.assertEquals(2, appender.countErrorContaining("毒条目解码失败"),
					"两条毒条目各记一条error（带key/tid定位）——留库人工排查的可观测性");

			// 第二轮：毒条目仍被隔离（每轮各再记一条，对齐redo内层"redo fail"形态），
			// redoTimer仍正常完成，无累积副作用。
			invokeRedoTimer(fixture.onzServer);
			Assertions.assertEquals(2, count(commitIndex), "毒条目留库，第二轮维持");
			Assertions.assertEquals(4, appender.countErrorContaining("毒条目解码失败"),
					"每轮每毒条目一条：第二轮再各记一条（无去重——对齐内层形态）");
		} finally {
			detachFromOnzServerLogger(appender);
		}
	}
}
