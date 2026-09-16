package Zeze.Onz;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import sun.misc.Unsafe;
import Zeze.Net.AsyncSocket;
import Zeze.Transaction.EmptyBean;
import Zeze.Util.TaskCompletionSource;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-34 回归（无网络桩）：cancelSaga 只对 future 成功的 saga 步骤发送 FuncSagaEnd(cancel)。
 * saga 参与方"发结果即本地提交"（OnzSaga.sendReadyAndWait），协调者 rpc 超时（原固定 5s，
 * 修复后复用 flushTimeout）不代表参与方未提交：超时步骤被跳过补偿的话，其写入已持久化而
 * 协调者按失败处理——部分提交的静默分歧。修复后失败/超时的步骤也发送 cancel：
 * 参与方上下文在则真正补偿，不在则应答 eSagaNotFound 可辨识忽略。
 * 假 OnzServer 经 Unsafe.allocateInstance 构造（真构造需 ServiceManager+RocksDB），
 * getZezeInstance 返回 null 走 SendForWait 失败路径（不触网络）。
 */
@Fast
public class TestFnd734OnzSagaCancelFailedStep {

	/** 仅记录 getZezeInstance 调用；allocateInstance 跳过构造，字段由测试反射注入。 */
	static class FakeOnzServer extends OnzServer {
		List<String> asked;
		List<String> throwFor;

		@SuppressWarnings("DataFlowIssue")
		FakeOnzServer() throws Exception {
			super("x=x", null); // 编译需要；永不可达
			throw new IllegalStateException("must allocate by Unsafe");
		}

		@Override
		public AsyncSocket getZezeInstance(String zezeName) {
			asked.add(zezeName);
			if (throwFor.contains(zezeName))
				throw new RuntimeException("fake fail. " + zezeName);
			return null; // null socket -> SendForWait future "Send Fail" -> await 抛异常
		}
	}

	static class FakeTxn extends OnzTransaction<EmptyBean.Data, EmptyBean.Data> {
		@Override
		protected long perform() throws Exception {
			return 0; // 不进入：直接调用 rollback()（其内部 cancelSaga）
		}
	}

	@Test
	public void testCancelSentForFailedStep() throws Exception {
		var txn = new FakeTxn();
		sagaMap(txn).put("z1", failedFuture());
		var fake = newFake(List.of());
		bindServer(txn, fake);

		txn.rollback(); // zezeProcedures 为空，直接走 cancelSaga

		Assertions.assertTrue(fake.asked.contains("z1"),
				"失败/超时的saga步骤也必须发送FuncSagaEnd(cancel)（FND7-34：超时但实际已提交的步骤要补偿）");
	}

	// 控制组：成功步骤的cancel是既有语义，修复前后都必须发送。
	@Test
	public void testCancelStillSentForSuccessStep() throws Exception {
		var txn = new FakeTxn();
		var ok = new TaskCompletionSource<EmptyBean.Data>();
		ok.setResult(EmptyBean.Data.instance); // 无状态bean可安全共享
		sagaMap(txn).put("z1", ok);
		var fake = newFake(List.of());
		bindServer(txn, fake);

		txn.rollback();

		Assertions.assertTrue(fake.asked.contains("z1"), "成功步骤的cancel必须保持发送");
	}

	private static TaskCompletionSource<?> failedFuture() {
		var future = new TaskCompletionSource<EmptyBean.Data>();
		future.setException(new RuntimeException("fake saga timeout"));
		return future;
	}

	private static FakeOnzServer newFake(List<String> throwFor) throws Exception {
		var fake = allocate(FakeOnzServer.class);
		inject(fake, "asked", new CopyOnWriteArrayList<String>());
		inject(fake, "throwFor", throwFor);
		return fake;
	}

	private static void bindServer(OnzTransaction<?, ?> txn, OnzServer server) throws Exception {
		txnField("onzServer").set(txn, server);
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<String, TaskCompletionSource<?>> sagaMap(OnzTransaction<?, ?> txn) throws Exception {
		return (ConcurrentHashMap<String, TaskCompletionSource<?>>)txnField("zezeSagas").get(txn);
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
