package Zeze.Onz;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import sun.misc.Unsafe;

import Zeze.Builtin.Onz.FuncSagaEnd;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Util.TaskCompletionSource;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * R3-C D①（M②）回归：cancelSaga 对 rpc 层失败（超时）步骤的 eSagaNotFound 应答单次延迟重试
 * （延迟=flushTimeout）。窗口：FuncSaga 与 FuncSagaEnd 同为 Normal 派发，参与方可先处理
 * FuncSagaEnd——上下文未注册应答 eSagaNotFound，迟到 FuncSaga 注册执行业务后无人再补偿。
 * 正常完成（成功/业务失败）的步骤 NotFound 是终态，不重试；重试仍 NotFound 即放弃（单次）。
 * <p>
 * 回环双 Service（端口0，进程内真实 rpc 往返）：假 OnzServer（Unsafe 构造）把 getZezeInstance
 * 指向回环客户端 socket；服务端按脚本应答结果码并计数 cancel 请求。
 */
@Fast
public class TestR3cSagaCancelNotFoundRetry {
	private static final int NotFound = AbstractOnz.eSagaNotFound;

	/** 应答脚本用尽的请求一律应答0（成功）。 */
	private static final class ScriptedServer extends Service {
		final ConcurrentLinkedQueue<Long> script = new ConcurrentLinkedQueue<>();
		final List<Long> cancelRequests = new CopyOnWriteArrayList<>();

		ScriptedServer() {
			super("TestR3cSagaCancelNotFoundRetry.Server");
		}

		long processFuncSagaEnd(Protocol<?> p) {
			var rpc = (FuncSagaEnd)p;
			if (rpc.Argument.isCancel())
				cancelRequests.add(rpc.Argument.getOnzTid());
			var code = script.poll();
			// 与真实参与方一致：非零结果码经 errorCode() 组合 moduleId 上线（协调者侧解码比较）。
			rpc.SendResultCode(code == null ? 0 : Zeze.IModule.errorCode(AbstractOnz.ModuleId, code.intValue()));
			return Procedure.Success;
		}
	}

	private static final class Client extends Service {
		final TaskCompletionSource<AsyncSocket> connected = new TaskCompletionSource<>();

		Client() {
			super("TestR3cSagaCancelNotFoundRetry.Client");
		}

		@Override
		public void OnSocketConnected(@NotNull AsyncSocket so) throws Exception {
			super.OnSocketConnected(so);
			connected.setResult(so);
		}
	}

	/** 仅返回预置 socket；allocateInstance 跳过构造，字段由测试反射注入。 */
	static class FakeOnzServer extends OnzServer {
		AsyncSocket socket;

		@SuppressWarnings("DataFlowIssue")
		FakeOnzServer() throws Exception {
			super("x=x", null); // 编译需要；永不可达
			throw new IllegalStateException("must allocate by Unsafe");
		}

		@Override
		public AsyncSocket getZezeInstance(String zezeName) {
			return socket;
		}
	}

	static class FakeTxn extends OnzTransaction<EmptyBean.Data, EmptyBean.Data> {
		@Override
		protected long perform() throws Exception {
			return 0; // 不进入：直接调用 rollback()（其内部 cancelSaga）
		}
	}

	/** 超时步骤（rpc 异常收场）：NotFound 后必须单次延迟重试，重试到达即补偿完成。 */
	@Test
	public void testRetryOnceForTimedOutStep() throws Exception {
		var env = startEnv((long)NotFound, 0L);
		var txn = env.newTxnWithFailedStep();
		txn.rollback();
		awaitCancelCount(env, 2);
		Assertions.assertEquals(2, env.server.cancelRequests.size(),
				"超时步骤的eSagaNotFound必须触发恰好一次延迟重试（初始+重试=2）");
		env.close();
	}

	/** 正常完成步骤（业务失败自清理后的NotFound）：终态，不得重试。 */
	@Test
	public void testNoRetryForCompletedStep() throws Exception {
		var env = startEnv((long)NotFound);
		var txn = new FakeTxn();
		var ok = new TaskCompletionSource<EmptyBean.Data>();
		ok.setResult(EmptyBean.Data.instance);
		bindServer(txn, env.fake);
		txn.setFlushTimeout(150);
		sagaMap(txn).put("z1", ok);
		txn.rollback();
		awaitCancelCount(env, 1);
		// rollback()返回≠服务端已计数：cancel的future.get()只有flushTimeout预算，回环rpc全链路
		//（编码→TCP→服务端EventLoop→业务→应答）压测饥饿下可超预算，断言时请求在途未到达
		//（30轮压测轮6实证expected1was0）。到达后留足潜在重试（延迟=flushTimeout）的观察窗再判终态。
		Thread.sleep(600);
		Assertions.assertEquals(1, env.server.cancelRequests.size(),
				"正常完成步骤的NotFound是终态（FuncSaga已被处理，注册先于任何FuncSagaEnd）");
		env.close();
	}

	/** 重试仍 NotFound：单次即放弃，不得无限重试。 */
	@Test
	public void testGiveUpAfterSingleRetry() throws Exception {
		var env = startEnv((long)NotFound, (long)NotFound);
		var txn = env.newTxnWithFailedStep();
		txn.rollback();
		awaitCancelCount(env, 2);
		Thread.sleep(600);
		Assertions.assertEquals(2, env.server.cancelRequests.size(),
				"重试仍NotFound必须放弃：单次重试，不得第三次发送");
		env.close();
	}

	// ///////////////////////////////////////////////////////////
	// 回环环境

	/** 轮询等待服务端cancel计数达到期望（10s上限）。rollback()返回时初始cancel可能仍在途
	 * （future.get()仅flushTimeout预算），立即断言在压测线程饥饿下假红。 */
	private static void awaitCancelCount(Env env, int expected) throws InterruptedException {
		var deadline = System.currentTimeMillis() + 10_000;
		while (env.server.cancelRequests.size() < expected && System.currentTimeMillis() < deadline)
			Thread.sleep(20);
	}

	private static final class Env implements AutoCloseable {
		final ScriptedServer server = new ScriptedServer();
		final Client client = new Client();
		final FakeOnzServer fake;
		final AtomicInteger nextId = new AtomicInteger();

		Env(long... scriptCodes) throws Exception {
			var typeId = new FuncSagaEnd().getTypeId();
			server.AddFactoryHandle(typeId,
					new Service.ProtocolFactoryHandle<>(FuncSagaEnd::new, server::processFuncSagaEnd));
			client.AddFactoryHandle(typeId, new Service.ProtocolFactoryHandle<>(FuncSagaEnd::new));
			Zeze.Util.Task.tryInitThreadPool();

			var listener = (Zeze.Net.TcpSocket)server.newServerSocket("127.0.0.1", 0, null);
			int port = listener.getLocalInet().getPort();
			client.newClientSocket("127.0.0.1", port, null, null);
			var socket = client.connected.get();
			for (var code : scriptCodes)
				server.script.add(code);

			fake = allocate(FakeOnzServer.class);
			inject(fake, "socket", socket);
		}

		FakeTxn newTxnWithFailedStep() throws Exception {
			var txn = new FakeTxn();
			bindServer(txn, fake);
			txn.setFlushTimeout(150); // 重试延迟=flushTimeout，测试里压到150ms
			var failed = new TaskCompletionSource<EmptyBean.Data>();
			failed.setException(new RuntimeException("fake saga timeout"));
			sagaMap(txn).put("z1", failed);
			return txn;
		}

		@Override
		public void close() throws Exception {
			server.stop();
			client.stop();
		}
	}

	private static Env startEnv(long... scriptCodes) throws Exception {
		return new Env(scriptCodes);
	}

	private static void bindServer(OnzTransaction<?, ?> txn, OnzServer server) throws Exception {
		txnField("onzServer").set(txn, server);
	}

	@SuppressWarnings("unchecked")
	private static java.util.concurrent.ConcurrentHashMap<String, TaskCompletionSource<?>> sagaMap(
			OnzTransaction<?, ?> txn) throws Exception {
		return (java.util.concurrent.ConcurrentHashMap<String, TaskCompletionSource<?>>)txnField("zezeSagas").get(txn);
	}

	private static Field txnField(String name) throws Exception {
		var field = OnzTransaction.class.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}

	private static void inject(Object obj, String name, Object value) throws Exception {
		var field = obj.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(obj, value);
	}

	@SuppressWarnings("unchecked")
	private static <T> T allocate(Class<T> cls) throws Exception {
		var theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
		theUnsafeField.setAccessible(true);
		var theUnsafe = (Unsafe)theUnsafeField.get(null);
		return (T)theUnsafe.allocateInstance(cls);
	}
}
