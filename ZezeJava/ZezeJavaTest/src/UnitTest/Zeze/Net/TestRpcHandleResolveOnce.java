package UnitTest.Zeze.Net;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import Zeze.Application;
import Zeze.Config;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Net.Service;
import Zeze.Transaction.Checkpoint;
import Zeze.Transaction.CheckpointMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.FuncLong;
import demo.Module1.BValue;

/**
 * 事务模式下 Rpc.handle 随 action 在 redo 时整体重跑。
 * rpcContexts 的会合消费只允许发生一次（首轮），重试复用事务上的 resolveOnce 缓存：
 * 无论终局是提交还是重试耗尽，条目都不会被"放回后无人再消费"而泄漏；
 * 处理期间条目不在表里，一次性超时任务也抢不走已到达的响应。
 */
@Fast
public class TestRpcHandleResolveOnce {
	private static Application app;

	@BeforeAll
	public static void setUp() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setNoDatabase(true);
		app = new Application("TestRpcHandleResolveOnce", config);
		// perform 要求 getCheckpoint() 非 null；NoDatabase 轻量模式不创建，注入一个未 start 的。
		var field = Application.class.getDeclaredField("checkpoint");
		field.setAccessible(true);
		field.set(app, new Checkpoint(app, CheckpointMode.Table, 0));
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
			return -2;
		}
	}

	// 模拟 Service.dispatchProtocol 事务分支的 action：每轮重试重新执行 handle。
	private static long dispatchResponseInTransaction(Service service, TestRpc response,
													  AtomicInteger handleCalls) throws Exception {
		var action = (FuncLong)() -> {
			handleCalls.incrementAndGet();
			var factoryHandle = new Service.ProtocolFactoryHandle<>(TestRpc.class, response.getTypeId());
			return response.handle(service, factoryHandle);
		};
		return new Procedure(app, action, "TestRpcHandleResolveOnce.Dispatch", null).call();
	}

	@Test
	public final void testContextNotLeakOnTooManyTry() throws Exception {
		var service = new Service("TestRpcHandleResolveOnce.TooManyTry");
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

		var handleCalls = new AtomicInteger();
		var result = dispatchResponseInTransaction(service, response, handleCalls);

		Assertions.assertEquals(Procedure.TooManyTry, result);
		Assertions.assertEquals(256, handleCalls.get());
		Assertions.assertEquals(256, responseHandleCalls.get());
		// 回归断言：重试耗尽终局失败后，rpcContexts 不残留条目
		Assertions.assertNull(service.removeRpcContext(sid));
	}

	@Test
	public final void testContextResolvedOnceOnRetryThenSuccess() throws Exception {
		var service = new Service("TestRpcHandleResolveOnce.RetryThenSuccess");
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

		var handleCalls = new AtomicInteger();
		var result = dispatchResponseInTransaction(service, response, handleCalls);

		Assertions.assertEquals(Procedure.Success, result);
		Assertions.assertEquals(3, handleCalls.get());
		Assertions.assertEquals(3, responseHandleCalls.get());
		// 成功终局同样不残留
		Assertions.assertNull(service.removeRpcContext(sid));
	}

	@Test
	public final void testEntryAbsentDuringProcessing() throws Exception {
		// 响应到达（首轮handle）之后、事务终局之前，条目必须已从表中消费：
		// 一次性超时任务在处理期间触发时 removeRpcContext 只能得到 null，
		// 不可能抢走已到达的响应把处理替换成超时。
		var service = new Service("TestRpcHandleResolveOnce.Absent");
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

		var handleCalls = new AtomicInteger();
		var result = dispatchResponseInTransaction(service, response, handleCalls);

		Assertions.assertEquals(Procedure.Success, result);
		Assertions.assertEquals(3, responseHandleCalls.get());
		Assertions.assertNull(stolen.get()); // 抢占失败：条目在首轮已被消费
	}

	@Test
	public final void testLostContextTerminateImmediately() throws Exception {
		var service = new Service("TestRpcHandleResolveOnce.Lost");
		var response = new TestRpc();
		response.setRequest(false);
		response.setSessionId(123456789); // 从未注册的会合

		var handleCalls = new AtomicInteger();
		var result = dispatchResponseInTransaction(service, response, handleCalls);

		// 上下文丢失（一般已被超时消费）：立即以 Unknown 终止，不空转剩余重试、不以Success提交空事务
		Assertions.assertEquals(Procedure.Unknown, result);
		Assertions.assertEquals(1, handleCalls.get());
	}
}
