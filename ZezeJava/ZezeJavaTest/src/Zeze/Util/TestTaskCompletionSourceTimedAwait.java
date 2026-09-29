package Zeze.Util;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * TaskCompletionSource 带超时await重载（FND29 dbh2-02，模式B）的语义：
 * rpc等待点的future完成路径可能整体失效（发送容器被并发close后的注册竞态，无任何路径
 * 触发该future），等待方必须用带超时版本兜底，不得无参await无界悬挂。
 * 超时异常形态对齐既有get(timeout,unit)（TimeoutException，经forceThrow未检查传播）；
 * 纯增量API，不改动无参版语义。
 */
@Fast
public class TestTaskCompletionSourceTimedAwait {

	@Test
	public void testCompletedBeforeTimeoutReturnsThis() {
		var tcs = new TaskCompletionSource<String>();
		tcs.setResult("ok");
		var returned = tcs.await(1000, TimeUnit.MILLISECONDS);
		Assertions.assertSame(tcs, returned, "await(timeout,unit)保持await()的链式返回this");
		Assertions.assertEquals("ok", returned.getNow());
	}

	@Test
	public void testTimeoutThrowsTimeoutException() {
		var tcs = new TaskCompletionSource<String>();
		Assertions.assertThrows(TimeoutException.class,
				() -> tcs.await(50, TimeUnit.MILLISECONDS),
				"超时必须抛TimeoutException（对齐get(timeout,unit)既有形态），不得悬挂也不得返回false");
		Assertions.assertFalse(tcs.isDone(), "超时不得完成future（等待方可按需重试/放弃）");
	}

	// get(0)!=get(0)：零超时是立即超时，不得退化为无参语义（get(timeout,unit)同款契约）。
	@Test
	public void testZeroTimeoutIsImmediateTimeout() {
		var tcs = new TaskCompletionSource<String>();
		Assertions.assertThrows(TimeoutException.class,
				() -> tcs.await(0, TimeUnit.MILLISECONDS),
				"零超时必须立即超时");
	}

	@Test
	public void testExceptionalCompletionPropagates() {
		var tcs = new TaskCompletionSource<String>();
		tcs.setException(new IllegalStateException("boom"));
		Assertions.assertThrows(CompletionException.class,
				() -> tcs.await(1000, TimeUnit.MILLISECONDS),
				"已异常完成的future：超时重载与无参版语义一致（CompletionException包裹）");
	}

	// 等待方超时放弃后，补完成方（容器close路径）的setResult仍能正常落槽——
	// "注册future的容器close时必须补完成"契约与超时兜底互不侵占。
	@Test
	public void testLateSetResultAfterTimeoutStillWins() {
		var tcs = new TaskCompletionSource<String>();
		Assertions.assertThrows(TimeoutException.class, () -> tcs.await(20, TimeUnit.MILLISECONDS));
		Assertions.assertTrue(tcs.setResult("late"), "超时放弃后setResult必须成功");
		Assertions.assertEquals("late", tcs.getNow());
	}
}
