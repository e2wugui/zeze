package UnitTest.Zeze.Transaction;

import java.util.concurrent.atomic.AtomicInteger;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.Checkpoint;
import Zeze.Transaction.CheckpointMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.FuncLong;

/**
 * resolveOnce 在一次 perform（含全部 redo 重试）生命周期内，每个 (owner, key) 只向 resolver 求解一次。
 * 这是 Rpc 会合消费依赖的语义：首轮 removeRpcContext 一次，重试复用缓存，不再触碰 rpcContexts。
 */
@Fast
public class TestResolveOnce {
	private static final Object OWNER = new Object();

	private static Application app;

	@BeforeAll
	public static void setUp() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setNoDatabase(true);
		app = new Application("TestResolveOnce", config);
		// perform 要求 getCheckpoint() 非 null；NoDatabase 轻量模式不创建，注入一个未 start 的。
		var field = Application.class.getDeclaredField("checkpoint");
		field.setAccessible(true);
		field.set(app, new Checkpoint(app, CheckpointMode.Table, 0));
	}

	@Test
	public final void testResolvedOnceAcrossRedoRetries() {
		var calls = new AtomicInteger();
		var resolves = new AtomicInteger();
		var result = new Procedure(app, (FuncLong)() -> {
			calls.incrementAndGet();
			var v = Transaction.getCurrent().resolveOnce(OWNER, 1, k -> {
				resolves.incrementAndGet();
				return "ctx-" + k;
			});
			Assertions.assertEquals("ctx-1", v);
			if (calls.get() < 4)
				Transaction.getCurrent().throwRedo(0, "force redo");
			return Procedure.Success;
		}, "TestResolveOnce.RetryThenSuccess", null).call();

		Assertions.assertEquals(Procedure.Success, result);
		Assertions.assertEquals(4, calls.get()); // 4次尝试
		Assertions.assertEquals(1, resolves.get()); // 只在首轮解析
	}

	@Test
	public final void testResolvedOnceAcrossTooManyTry() {
		var calls = new AtomicInteger();
		var resolves = new AtomicInteger();
		var result = new Procedure(app, (FuncLong)() -> {
			calls.incrementAndGet();
			Transaction.getCurrent().resolveOnce(OWNER, 2, k -> {
				resolves.incrementAndGet();
				return "ctx";
			});
			Transaction.getCurrent().throwRedo(0, "force redo");
			return Procedure.Success;
		}, "TestResolveOnce.TooManyTry", null).call();

		Assertions.assertEquals(Procedure.TooManyTry, result);
		Assertions.assertEquals(256, calls.get()); // 满256次尝试
		// 终局失败同样只解析一次：缓存生命周期覆盖整个 perform，包括最后一次尝试之后。
		Assertions.assertEquals(1, resolves.get());
	}

	@Test
	public final void testNullVerdictCached() {
		var calls = new AtomicInteger();
		var resolves = new AtomicInteger();
		var result = new Procedure(app, (FuncLong)() -> {
			calls.incrementAndGet();
			var v = Transaction.getCurrent().resolveOnce(OWNER, 3, k -> {
				resolves.incrementAndGet();
				return null;
			});
			Assertions.assertNull(v);
			if (calls.get() < 3)
				Transaction.getCurrent().throwRedo(0, "force redo");
			return Procedure.Success;
		}, "TestResolveOnce.NullCached", null).call();

		Assertions.assertEquals(Procedure.Success, result);
		// null 判定同样缓存：输给并发消费者一次就永久成立，重试不再重复探测。
		Assertions.assertEquals(1, resolves.get());
	}

	@Test
	public final void testCacheNotCarriedOverToNextPerform() {
		var resolves = new AtomicInteger();
		var action = (FuncLong)() -> {
			var v = Transaction.getCurrent().resolveOnce(OWNER, 4, k -> {
				resolves.incrementAndGet();
				return "ctx";
			});
			Assertions.assertEquals("ctx", v);
			return Procedure.Success;
		};
		new Procedure(app, action, "TestResolveOnce.First", null).call();
		new Procedure(app, action, "TestResolveOnce.Second", null).call();

		// 同线程复用 Transaction 对象（ThreadLocal），缓存不跨事务泄漏
		Assertions.assertEquals(2, resolves.get());
	}

	@Test
	public final void testDifferentOwnersResolvedSeparately() {
		// 不同属主（如不同 Service 各自安装了值域重叠的自定义 sessionId 生成器）
		// 即使 key 相同也是不同资源，各自独立解析，不会串。
		var ownerA = new Object();
		var ownerB = new Object();
		var resolves = new AtomicInteger();
		var result = new Procedure(app, (FuncLong)() -> {
			var va = Transaction.getCurrent().resolveOnce(ownerA, 5, k -> {
				resolves.incrementAndGet();
				return "A";
			});
			var vb = Transaction.getCurrent().resolveOnce(ownerB, 5, k -> {
				resolves.incrementAndGet();
				return "B";
			});
			Assertions.assertEquals("A", va);
			Assertions.assertEquals("B", vb);
			// 同一属主同 key 命中缓存
			var va2 = Transaction.getCurrent().resolveOnce(ownerA, 5, k -> {
				resolves.incrementAndGet();
				return "A2";
			});
			Assertions.assertEquals("A", va2);
			return Procedure.Success;
		}, "TestResolveOnce.OwnerScope", null).call();

		Assertions.assertEquals(Procedure.Success, result);
		Assertions.assertEquals(2, resolves.get()); // A、B 各一次；A 的重复请求命中缓存
	}
}
