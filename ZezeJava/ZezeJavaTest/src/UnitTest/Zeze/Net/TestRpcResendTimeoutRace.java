package UnitTest.Zeze.Net;

import java.lang.reflect.Method;

import Zeze.Net.Rpc;
import Zeze.Net.Service;
import Zeze.Util.TaskCompletionSource;
import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND6-11：同实例重发在add(新sid)/remove(旧sid)间隙被旧超时定时器命中——原定时器用
 * 单参removeRpcContext(sid)按键移除，旧sid短暂仍映射本实例，取到后置isTimeout=true、
 * 毒化future（SendForWait刚换上的新future被置超时异常，真实应答再派发一次→双派发）。
 * 修复：超时动作抽为onTimeout，先判字段sessionId与定时器捕获的sid失配（重发即失配，
 * 新定时器已接管）再做键+值双参移除（条目被应答消费时移除失败正确跳过）。
 * 直接调onTimeout（反射，包私有）按三个时序断言：重发窗口旧定时器跳过、归属定时器
 * 正常超时、条目被消费后定时器跳过。
 */
@Fast
public class TestRpcResendTimeoutRace {

	private static void invokeOnTimeout(Service service, Rpc<BValue, BValue> rpc, long timerSid) throws Exception {
		Method onTimeout = Rpc.class.getDeclaredMethod("onTimeout", Service.class, long.class);
		onTimeout.setAccessible(true);
		onTimeout.invoke(rpc, service, timerSid);
	}

	@Test
	public void testResendWindowStaleTimerSkips() throws Exception {
		Zeze.Util.Task.tryInitThreadPool();
		var service = new Service("TestRpcResendTimeoutRace");
		var rpc = new TestRpc.FirstRpc();

		// 首次发送注册S1。
		long s1 = service.addRpcContext(rpc);
		rpc.setSessionId(s1);
		var future = new TaskCompletionSource<BValue>();
		rpc.setFuture(future);

		// 重发窗口：注册S2并更新字段sessionId（旧条目S1尚未移除——ns级竞态窗口正是此处）。
		long s2 = service.addRpcContext(rpc);
		rpc.setSessionId(s2);

		// 旧定时器（捕获S1）此刻触发：必须跳过，不得毒化新请求。
		invokeOnTimeout(service, rpc, s1);
		Assertions.assertFalse(rpc.isTimeout(), "陈旧定时器不得置假超时");
		Assertions.assertFalse(future.isDone(), "陈旧定时器不得毒化新请求的future");
		Assertions.assertNotNull(service.removeRpcContext(s1, rpc) ? rpc : null, "S1条目应仍存在");

		// 归属定时器（S2）无应答触发：正常超时路径。
		invokeOnTimeout(service, rpc, s2);
		Assertions.assertTrue(rpc.isTimeout(), "归属定时器必须置超时");
		Assertions.assertTrue(future.isCompletedExceptionally(), "future须以异常完成");
		Assertions.assertNull(service.removeRpcContext(s2), "超时处置后条目应已移除");

		// 条目已被应答消费后定时器再触发：跳过（原单参路径会取到null，双参路径移除失败）。
		var rpc2 = new TestRpc.FirstRpc();
		long s3 = service.addRpcContext(rpc2);
		rpc2.setSessionId(s3);
		Assertions.assertTrue(service.removeRpcContext(s3, rpc2), "模拟应答消费条目");
		var future2 = new TaskCompletionSource<BValue>();
		rpc2.setFuture(future2);
		invokeOnTimeout(service, rpc2, s3);
		Assertions.assertFalse(rpc2.isTimeout(), "条目已消费的定时器必须跳过");
		Assertions.assertFalse(future2.isDone());
	}

	@Test
	public void testMidResendInstructionWindowDocumented() throws Exception {
		Zeze.Util.Task.tryInitThreadPool();
		var service = new Service("TestRpcMidResendWindow");
		var rpc = new TestRpc.FirstRpc();

		// 交错(ii)中态（FND6-11补文档化）：重发的addRpcContext已落地（S2在map）但字段
		// sessionId仍是旧值S1——指令级间隙（putfield未执行）。此刻旧定时器：守卫通过
		// （S1==字段S1）、双参移除成功（S1仍映射this）→ 毒化发生。这是复核后仍存在的
		// TOCTOU残余（守卫读→移除→复核读之间字段未变），非缺陷回归——窗口已从「一次
		// 字段读」收窄到「守卫读→移除→复核读」，完全消除需实例互斥（热路径不值）。
		// 本用例钉住该残余的当前语义，防未来无意识变更。
		long s1 = service.addRpcContext(rpc);
		rpc.setSessionId(s1);
		long s2 = service.addRpcContext(rpc); // 新条目落地，字段不动
		var future = new TaskCompletionSource<BValue>();
		rpc.setFuture(future);

		invokeOnTimeout(service, rpc, s1);
		Assertions.assertTrue(rpc.isTimeout(),
				"交错(ii)中态下旧定时器仍毒化（文档化TOCTOU残余，见onTimeout注释）");
		Assertions.assertTrue(future.isCompletedExceptionally());
		// S2条目未被旧定时器触碰：仍由新上下文流程/真实应答路径处置。
		Assertions.assertTrue(service.removeRpcContext(s2, rpc), "S2条目应仍由新定时器路径处置，未被旧定时器误删");
	}
}
