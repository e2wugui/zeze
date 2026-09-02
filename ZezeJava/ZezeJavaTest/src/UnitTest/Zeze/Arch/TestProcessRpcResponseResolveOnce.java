package UnitTest.Zeze.Arch;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import Zeze.Application;
import Zeze.Arch.LoadBase;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderImplement;
import Zeze.Builtin.Provider.LinkBroken;
import Zeze.Config;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Transaction.Checkpoint;
import Zeze.Transaction.CheckpointMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.FuncLong;
import Zeze.Util.Task;
import demo.Module1.BValue;

/**
 * linkd 转发路径的 Rpc 响应会合（ProviderImplement.processRpcResponse）与直连路径
 * （Rpc.handle，见 TestRpcHandleResolveOnce）同一约定：
 * 会合消费（removeRpcContext）只允许发生一次（首轮，含null判定），重试复用事务上的
 * resolveOnce 缓存；成功与重试耗尽终局 rpcContexts 均不残留，处理期间条目不在表，
 * 一次性超时任务抢不走已到达的响应；丢上下文立即以 Unknown 终止。
 */
@Fast
public class TestProcessRpcResponseResolveOnce {
	private static Application app;
	private static ProviderApp providerApp;
	private static ProviderImplement provider;
	private static Method processRpcResponse;

	@BeforeAll
	public static void setUp() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setNoDatabase(true);
		app = new Application("TestProcessRpcResponseResolveOnce", config);
		// perform 要求 getCheckpoint() 非 null；NoDatabase 轻量模式不创建，注入一个未 start 的。
		var field = Application.class.getDeclaredField("checkpoint");
		field.setAccessible(true);
		field.set(app, new Checkpoint(app, CheckpointMode.Table, 0));

		// 打包用哑构造：只创建一个未启动的 ProviderService（rpcContexts 所在），其余成员为 null。
		providerApp = new ProviderApp(app);
		provider = new TestProviderImplement(providerApp);
		processRpcResponse = ProviderImplement.class.getDeclaredMethod("processRpcResponse", Protocol.class);
		processRpcResponse.setAccessible(true);
	}

	public static class TestProviderImplement extends ProviderImplement {
		public TestProviderImplement(@NotNull ProviderApp app) {
			providerApp = app;
		}

		@Override
		public @Nullable LoadBase getLoad() {
			return null;
		}

		@Override
		public void stop() {
		}

		@Override
		protected long ProcessLinkBroken(@NotNull LinkBroken p) {
			return Procedure.Success;
		}
	}

	public static class TestRpc extends Rpc<BValue, BValue> {
		public TestRpc() {
			Argument = new BValue();
			Result = new BValue();
		}

		@Override
		public int getModuleId() {
			return 1;
		}

		@Override
		public int getProtocolId() {
			return -4;
		}
	}

	// 反射调用私有方法。redo 信号 GoBackZeze 是 Error 子类，会被 InvocationTargetException
	// 包装，解开以原类型重抛，perform 才能识别为 redo 而不是普通异常。
	private static long callProcessRpcResponse(@NotNull Protocol<?> response) throws Exception {
		try {
			return (Long)processRpcResponse.invoke(provider, response);
		} catch (InvocationTargetException e) {
			throw Task.forceThrow(e.getCause());
		}
	}

	// 模拟 ProcessDispatch 事务分支的 action：每轮重试重新执行 processRpcResponse。
	private static long dispatchResponseInTransaction(@NotNull TestRpc response, @NotNull AtomicInteger calls)
			throws Exception {
		var action = (FuncLong)() -> {
			calls.incrementAndGet();
			return callProcessRpcResponse(response);
		};
		return new Procedure(app, action, "TestProcessRpcResponseResolveOnce.Dispatch", null).call();
	}

	@Test
	public final void testContextResolvedOnceOnRetryThenSuccess() throws Exception {
		var service = providerApp.providerService;
		var context = new TestRpc();
		var responseHandleCalls = new AtomicInteger();
		context.setResponseHandle(r -> {
			if (responseHandleCalls.incrementAndGet() < 3)
				Transaction.getCurrent().throwRedo(0, "force redo");
			return Procedure.Success;
		});
		var sid = service.addRpcContext(context);
		context.setSessionId(sid);

		var response = new TestRpc();
		response.setRequest(false);
		response.setSessionId(sid);

		var calls = new AtomicInteger();
		var result = dispatchResponseInTransaction(response, calls);

		Assertions.assertEquals(Procedure.Success, result);
		Assertions.assertEquals(3, calls.get());
		Assertions.assertEquals(3, responseHandleCalls.get());
		// 成功终局 rpcContexts 不残留
		Assertions.assertNull(service.removeRpcContext(sid));
	}

	@Test
	public final void testContextNotLeakOnTooManyTry() throws Exception {
		var service = providerApp.providerService;
		var context = new TestRpc();
		var responseHandleCalls = new AtomicInteger();
		context.setResponseHandle(r -> {
			responseHandleCalls.incrementAndGet();
			Transaction.getCurrent().throwRedo(0, "force redo");
			return Procedure.Success;
		});
		var sid = service.addRpcContext(context);
		context.setSessionId(sid);

		var response = new TestRpc();
		response.setRequest(false);
		response.setSessionId(sid);

		var calls = new AtomicInteger();
		var result = dispatchResponseInTransaction(response, calls);

		Assertions.assertEquals(Procedure.TooManyTry, result);
		Assertions.assertEquals(256, calls.get());
		Assertions.assertEquals(256, responseHandleCalls.get());
		// 重试耗尽终局失败后，rpcContexts 不残留条目
		Assertions.assertNull(service.removeRpcContext(sid));
	}

	@Test
	public final void testEntryAbsentDuringProcessing() throws Exception {
		// 响应到达（首轮消费）之后、事务终局之前，条目必须已从表中移除：
		// 一次性超时任务在处理期间触发时 removeRpcContext 只能得到 null。
		var service = providerApp.providerService;
		var context = new TestRpc();
		var responseHandleCalls = new AtomicInteger();
		var stolen = new AtomicReference<Protocol<?>>();
		context.setResponseHandle(r -> {
			if (responseHandleCalls.incrementAndGet() < 3) {
				if (responseHandleCalls.get() == 2)
					stolen.set(service.removeRpcContext(((TestRpc)r).getSessionId())); // 模拟超时任务抢占
				Transaction.getCurrent().throwRedo(0, "force redo");
			}
			return Procedure.Success;
		});
		var sid = service.addRpcContext(context);
		context.setSessionId(sid);

		var response = new TestRpc();
		response.setRequest(false);
		response.setSessionId(sid);

		var calls = new AtomicInteger();
		var result = dispatchResponseInTransaction(response, calls);

		Assertions.assertEquals(Procedure.Success, result);
		Assertions.assertEquals(3, responseHandleCalls.get());
		Assertions.assertNull(stolen.get()); // 抢占失败：条目在首轮已被消费
	}

	@Test
	public final void testLostContextTerminateImmediately() throws Exception {
		var response = new TestRpc();
		response.setRequest(false);
		response.setSessionId(987654321); // 从未注册的会合

		var calls = new AtomicInteger();
		var result = dispatchResponseInTransaction(response, calls);

		// 上下文丢失（一般已被超时消费）：立即以 Unknown 终止，不空转剩余重试
		Assertions.assertEquals(Procedure.Unknown, result);
		Assertions.assertEquals(1, calls.get());
	}

	@Test
	public final void testNoTransactionDirectConsume() throws Exception {
		// 无事务分支（应用框架不支持事务或协议配置 NoProcedure）：直接消费，单次执行
		var service = providerApp.providerService;
		var context = new TestRpc();
		var responseHandleCalls = new AtomicInteger();
		context.setResponseHandle(r -> {
			responseHandleCalls.incrementAndGet();
			return Procedure.Success;
		});
		var sid = service.addRpcContext(context);
		context.setSessionId(sid);

		var response = new TestRpc();
		response.setRequest(false);
		response.setSessionId(sid);

		Assertions.assertEquals(Procedure.Success, callProcessRpcResponse(response));
		Assertions.assertEquals(1, responseHandleCalls.get());
		Assertions.assertNull(service.removeRpcContext(sid));
	}
}
