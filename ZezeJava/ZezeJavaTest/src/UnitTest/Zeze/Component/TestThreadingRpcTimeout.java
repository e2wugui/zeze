package UnitTest.Zeze.Component;

import Zeze.Component.Threading;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND5-22 回归：Threading客户端rpc超时上限 Math.max(timeoutMs + 1000, 5000)
 * 在timeoutMs接近Integer.MAX_VALUE时int回绕为负，max取下限5000——客户端
 * 5秒即抛RpcTimeout，服务端仍会最终获锁并占用（KeepAlive断链30分钟才强制
 * 释放），期间同名锁对其他用户不可得。修复：long运算+钳制int上限
 * （SendForWait超时参数为int）。
 */
@Fast
public class TestThreadingRpcTimeout {

	@Test
	public void testRpcTimeoutNoOverflow() {
		int computed;
		try {
			var m = Threading.class.getDeclaredMethod("rpcTimeoutMs", int.class);
			m.setAccessible(true);
			computed = (int)m.invoke(null, Integer.MAX_VALUE);
		} catch (NoSuchMethodException e) {
			Assertions.fail("超时计算必须独立为rpcTimeoutMs（FND5-22），实际不存在: " + e);
			return;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Assertions.assertEquals(Integer.MAX_VALUE, computed,
				"timeoutMs=Integer.MAX_VALUE不得回绕为5000下限（FND5-22）");
	}

	@Test
	public void testRpcTimeoutNormalValues() throws Exception {
		var m = Threading.class.getDeclaredMethod("rpcTimeoutMs", int.class);
		m.setAccessible(true);
		Assertions.assertEquals(5000, (int)m.invoke(null, 0), "下限5000不变");
		Assertions.assertEquals(5000, (int)m.invoke(null, Integer.MIN_VALUE / 2), "异常小值钳到下限");
		Assertions.assertEquals(6000, (int)m.invoke(null, 5000), "常规值+1000不变");
		Assertions.assertEquals(Integer.MAX_VALUE, (int)m.invoke(null, Integer.MAX_VALUE - 1),
				"接近上限钳制不回绕");
	}
}
