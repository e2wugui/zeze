package Benchmark;

import java.util.Objects;
import harness.Bench;
import harness.MacroBench;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import demo.App;
import org.junit.jupiter.api.Assertions;

// MMO-lite 负载画像：背包/房间战斗/排行/邮件/会话五件套按固定权重混合（30/20/20/20/10），
// 进程内纯核心 API。框架基准口径——画像承载的是访问模式（容器churn / 同key冲突 /
// 热key有序容器 / 写多insert-delete churn / 单字段高频更新），不是游戏 schema 仿真；
// 完整游戏 demo（Socket 驱动全栈）为二期，基于 BenchSocket 标准化成果另立车道。
// 表映射（复用 demo 既有表，免 Gen 重生成链路）：
//   背包=demo_web.tMap2Bean1（map2 容器 put+remove churn，1万角色×16槽位）
//   房间=Table1（500 key 同 key 冲突，单事务双字段写）
//   排行=Table5（Module2.BValue.psortedmap 单热key，put+remove 有序容器churn）
//   邮件=Table3（写多 insert/delete churn，1万 id 轮转窗口）
//   会话=TableImportant（单字段高频更新，1万角色）
// 混合走确定性分派（i%10），非随机——同轮内各操作数量精确可验；每轮开头全量重置
// （对齐 G 的 roundBody 自包含契约），校验集中在末尾测量轮外。
@SuppressWarnings("NewClassNamingConvention")
@Bench
@Tag("core")
public class IMmoLiteWorkload {
	public static final int TxnCount = 200_000;
	public static final int Roles = 10_000;
	public static final int Rooms = 500;
	public static final int RankCap = 1_000;     // 排行榜容量（分数槽位模转）
	public static final int MailWindow = 10_000; // 邮件 id 轮转窗口
	public static final int InvSlots = 16;       // 背包槽位
	public static final int Warmups = 2;
	public static final int Rounds = 5;

	// 操作计数（过程内累加、末轮外校验；返回 0 避免 defaultLogAction）
	private static final java.util.concurrent.atomic.AtomicLong invOps = new java.util.concurrent.atomic.AtomicLong();
	private static final java.util.concurrent.atomic.AtomicLong roomOps = new java.util.concurrent.atomic.AtomicLong();
	private static final java.util.concurrent.atomic.AtomicLong rankOps = new java.util.concurrent.atomic.AtomicLong();
	private static final java.util.concurrent.atomic.AtomicLong mailOps = new java.util.concurrent.atomic.AtomicLong();
	private static final java.util.concurrent.atomic.AtomicLong sessOps = new java.util.concurrent.atomic.AtomicLong();

	@Test
	public void testBenchmark() throws Exception {
		App.Instance.Stop();
		App.Instance.Start();
		try {
			MacroBench.run("I_MmoLiteWorkload", Warmups, Rounds, TxnCount, () -> {
				clearAll();
				for (int i = 0; i < TxnCount; ++i) {
					final int seq = i;
					final int mix = i % 10; // 0-2背包 3-4房间 5-6排行 7-8邮件 9会话
					App.Instance.getZeze().newProcedure(() -> op(mix, seq), "mmo").call();
				}
			});
			App.Instance.getZeze().newProcedure(IMmoLiteWorkload::Check, "check").call();
			clearAll();
		} finally {
			//App.Instance.Stop();
		}
	}

	private static long op(int mix, int seq) {
		final int i = seq;
		// 角色/房间取 (i/10)：与 mix（i%10）解耦——否则会话类操作只摸到 ≡9 (mod 10) 的角色，
		// i/10 在一轮内覆盖 0..TxnCount/10，全体角色/房间均被触达
		final long role = (i / 10) % Roles;
		switch (mix) {
			case 0, 1, 2 -> { // 背包：加一件+删一件（容器churn，槽位 i 与 i+8 恒不同）
				var bean = Objects.requireNonNull(App.Instance.demo_web).getMap2Bean1().getOrAdd(role);
				var item = new demo.Bean1();
				item.setV1(i);
				item.getV2().put(0, i);
				bean.getMap2().put(i % InvSlots, item);
				bean.getMap2().remove((i + InvSlots / 2) % InvSlots);
				invOps.incrementAndGet();
			}
			case 3, 4 -> { // 房间战斗：同key冲突 + 单事务双字段写
				var room = Objects.requireNonNull(App.Instance.demo_Module1).getTable1().getOrAdd((long)((i / 10) % Rooms));
				room.setLong2(room.getLong2() + 1);
				room.setInt_1(i);
				roomOps.incrementAndGet();
			}
			case 5, 6 -> { // 排行榜：热key有序容器 put+remove
				var board = Objects.requireNonNull(App.Instance.demo_Module1).getTable5().getOrAdd(1L);
				board.getPsortedmap().put(i % RankCap, (int)(role % Roles));
				board.getPsortedmap().remove((i + RankCap / 2) % RankCap);
				rankOps.incrementAndGet();
			}
			case 7, 8 -> { // 邮件：写多 insert/delete churn
				Objects.requireNonNull(App.Instance.demo_Module1).getTable3().getOrAdd((long)(i % MailWindow)).setLong2(i);
				App.Instance.demo_Module1.getTable3().remove((long)((i + 1) % MailWindow));
				mailOps.incrementAndGet();
			}
			default -> { // 会话心跳：单字段高频更新
				Objects.requireNonNull(App.Instance.demo_Module1).getTableImportant().getOrAdd(role).setLong2(i);
				sessOps.incrementAndGet();
			}
		}
		return 0;
	}

	private static long Check() {
		long totalRounds = Warmups + Rounds;
		// 房间总场次 = roomOps（每 op 恰 +1，且每轮重置——校验的是末轮状态）
		long roomSum = 0;
		for (long room = 0; room < Rooms; ++room)
			roomSum += Objects.requireNonNull(App.Instance.demo_Module1).getTable1().getOrAdd(room).getLong2();
		Assertions.assertEquals(roomOps.get() - (long)(totalRounds - 1) * TxnCount / 10 * 2, roomSum,
				"room hits: only last round survives clearAll");

		var board = App.Instance.demo_Module1.getTable5().getOrAdd(1L);
		Assertions.assertTrue(board.getPsortedmap().size() > 0 && board.getPsortedmap().size() <= RankCap,
				"rank board size in (0, RankCap]");

		long mails = 0;
		for (long m = 0; m < MailWindow; ++m)
			if (App.Instance.demo_Module1.getTable3().getOrAdd(m).getLong2() >= 0)
				++mails;
		Assertions.assertTrue(mails > 0 && mails <= MailWindow, "mail window populated");

		long sessRoles = 0;
		for (long r = 0; r < Roles; ++r)
			if (App.Instance.demo_Module1.getTableImportant().getOrAdd(r).getLong2() > 0)
				++sessRoles;
		Assertions.assertEquals(Roles, sessRoles, "all roles heartbeated (last round touches all)");

		// 背包槽位有界
		long invRoles = 0;
		for (long r = 0; r < Roles; ++r) {
			var m2 = Objects.requireNonNull(App.Instance.demo_web).getMap2Bean1().getOrAdd(r).getMap2();
			Assertions.assertTrue(m2.size() <= InvSlots, "inventory bounded");
			if (m2.size() > 0)
				++invRoles;
		}
		Assertions.assertEquals(invOps.get(), (long)totalRounds * TxnCount / 10 * 3, "inv op count");
		Assertions.assertEquals(rankOps.get(), (long)totalRounds * TxnCount / 10 * 2, "rank op count");
		Assertions.assertEquals(mailOps.get(), (long)totalRounds * TxnCount / 10 * 2, "mail op count");
		Assertions.assertEquals(sessOps.get(), (long)totalRounds * TxnCount / 10, "session op count");
		return 0;
	}

	private static void clearAll() throws Exception {
		App.Instance.getZeze().newProcedure(() -> {
			for (long r = 0; r < Rooms; ++r)
				Objects.requireNonNull(App.Instance.demo_Module1).getTable1().remove(r);
			App.Instance.demo_Module1.getTable5().remove(1L);
			for (long m = 0; m < MailWindow; ++m)
				App.Instance.demo_Module1.getTable3().remove(m);
			for (long r = 0; r < Roles; ++r)
				App.Instance.demo_Module1.getTableImportant().remove(r);
			for (long r = 0; r < Roles; ++r)
				Objects.requireNonNull(App.Instance.demo_web).getMap2Bean1().remove(r);
			return 0L;
		}, "clear").call();
		App.Instance.getZeze().checkpointRun();
	}
}
