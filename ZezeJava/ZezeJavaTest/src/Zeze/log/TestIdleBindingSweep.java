package Zeze.log;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND24 zoker-06 守卫：闲置绑定清扫（FileSessionManager.sweepIdleBindings）直驱。
 * 修复前 map 无淘汰无上限：每源 IP 最后一个会话滞留到进程结束（9980 无认证端口下伪造源 IP
 * 线性放大）。修复=lastActiveNanos+TTL 惰性清扫（驱逐走既有 closeExecutor）。
 * 直驱面（sweepIdleBindings 私有静态，反射先例 TestE02CommitLockCaseFolding；resolve 全链
 * 依赖 LogAgent 网络，见 TestD06LogSessionBinding 的直测边界声明）：
 * <ul>
 * <li>闲置超 TTL 的绑定被逐（条目消失）且其会话经 closeExecutor 异步关闭；</li>
 * <li>未闲置绑定不受波及（误逐=每次查询重建，游标抖动）；</li>
 * <li>TTL 语义钉：闲置判定以 lastActiveNanos 为准（touched 前移后不逐）。</li>
 * </ul>
 * 修复前红点：NoSuchMethodException（清扫不存在=条目只增不减的病灶本体）。
 */
@Fast
public class TestIdleBindingSweep {

	/** 最小可关闭会话桩：记录 close 被调用（closeExecutor 异步，闩同步）。 */
	private static final class StubSession implements AutoCloseable {
		final CountDownLatch closed = new CountDownLatch(1);

		@Override
		public void close() {
			closed.countDown();
		}
	}

	private static SocketAddress addr(String ip) {
		return new InetSocketAddress(ip, 80);
	}

	private static void sweepNow() throws Exception {
		Method method = FileSessionManager.class.getDeclaredMethod("sweepIdleBindings", long.class);
		method.setAccessible(true);
		method.invoke(null, System.nanoTime());
	}

	@Test
	public void testIdleBindingEvictedAndFreshKept() throws Exception {
		var staleSession = new StubSession();
		// 闲置超过 TTL（2h）：以 touched 回拨 3h 摆出闲置形态（touched 只前移时间戳，三元组/会话不变）
		var staleBinding = LogSessionBinding.server("server1", "app.log", "search|-1|-1|1|[error]|", staleSession)
				.touched(System.nanoTime() - Duration.ofHours(3).toNanos());
		var freshSession = new StubSession();
		var freshBinding = LogSessionBinding.server("server1", "app.log", "search|-1|-1|1|[error]|", freshSession);

		var staleAddr = addr("10.9.9.9");
		var freshAddr = addr("10.9.9.8");
		assertNull(FileSessionManager.put(staleAddr, staleBinding), "摆盘：stale 首次入库无替换");
		assertNull(FileSessionManager.put(freshAddr, freshBinding), "摆盘：fresh 首次入库无替换");

		sweepNow();

		assertNull(FileSessionManager.get(staleAddr), "闲置超 TTL 的绑定被逐（条目不再只增不减）");
		assertNotNull(FileSessionManager.get(freshAddr), "未闲置绑定不得误逐（误逐=每次查询重建抖动）");
		assertTrue(staleSession.closed.await(5, TimeUnit.SECONDS),
				"被逐绑定的会话经 closeExecutor 异步关闭（服务端句柄释放）");
		// fresh 条目留在静态 map（惰性清扫域自管理；其时间戳新鲜，不波及并行车道的其他测试）
	}
}
