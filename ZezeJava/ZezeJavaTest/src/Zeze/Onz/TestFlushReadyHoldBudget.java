package Zeze.Onz;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Onz.BSavedCommits;
import Zeze.Builtin.Onz.FlushReady;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * FlushReady持有预算回归（协调者对被扣ready的放行上限 vs 参与方rpc预算2×flushTimeout）。
 * 参与方在Checkpoint提交路径无超时等待FlushReady应答，超时经Table检查点模式重抛最终
 * halt(543543)整进程——协调者必须保证放行不超过预算。原先放行点只有"计数满足开闸"与
 * "waitFlushDone的flushTimeout降级（计时起点在commit循环尾部之后）"：首个已提交参与方的
 * ready在commit循环中途到达即被扣住，循环尾部对死/慢参与方的串行等待（每参与方≈建连5s
 * 或Commit rpc默认超时5s，数量与时长均无上界）把放行推迟到T_tail+flushTimeout，
 * T_tail≥flushTimeout时必然超出参与方2×flushTimeout预算，健康参与方被halt。
 * 修复：首条被扣ready起1.5×flushTimeout持有期限强制开闸（&lt;2F，余0.5F给rpc往返）；
 * Commit投递失败的参与方即时出闸门分母（永不发ready者不占等待预算）。
 * 直构形态（@Fast，对齐TestGcD03FlushReadyDedup）：反射填充zezeProcedures的参与方名，
 * 直接驱动包内trySetFlushReady/markParticipantFlushImpossible；rpc无真实socket，应答路径
 * SendResult(null sender)仅记warn不抛。
 */
@Fast
public class TestFlushReadyHoldBudget {

	@BeforeAll
	static void initTaskPools() {
		// 持有期限定时走TaskSpec.schedule（调度线程池）——生产由Application初始化，
		// 直构测试自行初始化。
		Task.tryInitThreadPool();
	}

	/** 死/慢参与方形态（其他参与方永不发ready）：被扣ready必须在参与方预算（2F）内放行，
	 * 由持有期限（1.5F）兜底——修复前无任何开闸通道（计数永不满足、waitFlushDone未运行），
	 * ready被无限扣住，参与方FlushReady超时→Table模式重抛→halt(543543)。 */
	@Test
	@Timeout(30)
	public void testHeldReadyReleasedWithinParticipantBudget() throws Exception {
		var txn = newTransaction(800, "zeze1", "zeze2", "zeze3");
		var r = ready("zeze1");
		var begin = System.nanoTime();
		txn.trySetFlushReady(r);
		// 轮询放行时刻：预算2F=1.6s内必须观察到SendResult（持有期限1.5F兜底）。
		var released = false;
		var elapsedMs = 0L;
		for (var i = 0; i < 400 && !released; ++i) { // 25ms步进，上限10s
			Thread.sleep(25);
			elapsedMs = (System.nanoTime() - begin) / 1_000_000;
			released = r.isSendResultDone();
		}
		Assertions.assertTrue(released,
				"被扣ready必须在参与方预算(2*flushTimeout)内放行——否则健康参与方FlushReady超时halt(543543)");
		Assertions.assertTrue(elapsedMs >= 800,
				"计数未满足不得提前放行（同时性尽力语义；放行只能由持有期限触发）");
		Assertions.assertTrue(elapsedMs < 2 * 800 + 400,
				"放行不得超过参与方预算2*flushTimeout+调度抖动宽限（本次实测" + elapsedMs + "ms）");
		Assertions.assertTrue(gateOpen(txn), "持有期限到期必须开闸");
		Assertions.assertTrue(flushDone(txn), "开闸必须置位flushDone（waitFlushDone随即快速返回）");
	}

	/** Commit投递失败的参与方（死地址建连5s超时/Commit rpc超时/非0应答）永不发本事务的
	 * FlushReady，必须即时出分母：唯一可flush参与方的ready到达即开闸，不被失败者扣住。 */
	@Test
	@Timeout(30)
	public void testFailedParticipantLeavesGateDenominator() throws Exception {
		var txn = newTransaction(10_000, "zeze1", "zeze2", "zeze3");
		txn.markParticipantFlushImpossible("zeze2");
		Assertions.assertFalse(gateOpen(txn), "无ready不得开闸（分母=2、计数=0）");
		txn.markParticipantFlushImpossible("zeze3");
		Assertions.assertFalse(gateOpen(txn), "无ready不得开闸（分母=1、计数=0）");
		var r = ready("zeze1");
		txn.trySetFlushReady(r);
		Assertions.assertTrue(gateOpen(txn), "Commit失败者出分母后，唯一可flush参与方ready即满足计数开闸");
		Assertions.assertTrue(r.isSendResultDone(), "开闸必须放行被扣ready");
		Assertions.assertTrue(flushDone(txn));
	}

	/** 分母排除不得过度：仍有可flush参与方未ready时不开闸（排除只针对确认不会发ready者）。 */
	@Test
	@Timeout(30)
	public void testExclusionStillWaitsForReachableParticipants() throws Exception {
		var txn = newTransaction(60_000, "zeze1", "zeze2", "zeze3");
		txn.markParticipantFlushImpossible("zeze3");
		var r1 = ready("zeze1");
		txn.trySetFlushReady(r1);
		Assertions.assertFalse(gateOpen(txn), "分母=2、计数=1：仍需等待可flush参与方zeze2");
		var r2 = ready("zeze2");
		txn.trySetFlushReady(r2);
		Assertions.assertTrue(gateOpen(txn), "可flush参与方全部ready必须开闸（失败者已出分母）");
		Assertions.assertTrue(r1.isSendResultDone() && r2.isSendResultDone(), "开闸必须放行全部被扣ready");
	}

	// ------------------------------------------------------------------ 直构辅助

	private static final class LocalTransaction extends OnzTransaction<BSavedCommits.Data, BSavedCommits.Data> {
		@Override
		protected long perform() {
			return 0; // 闸门路径不触达perform
		}
	}

	private static OnzTransaction<BSavedCommits.Data, BSavedCommits.Data> newTransaction(
			int flushTimeoutMs, String... participants) throws Exception {
		var txn = new LocalTransaction();
		// setFlushTimeout在OnzTransaction上是public（协调者侧配置），无需反射。
		txn.setFlushTimeout(flushTimeoutMs);
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
}
