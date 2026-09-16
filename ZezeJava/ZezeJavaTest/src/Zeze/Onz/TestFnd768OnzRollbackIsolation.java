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
 * FND7-68 回归（无网络桩）：rollback()对每个参与方的Rollback发送await无try/catch。
 * 该方法运行在OnzServer.perform的rc!=0路径或catch块内：第一个参与方的发送异常
 * 中断循环（后续参与方收不到Rollback），外传会被perform的catch二次rollback从头
 * 重试，再抛则替换原始错误（业务rc丢失，最终只报Procedure.Exception）。
 * 修复：逐参与方捕获记fatal后继续（对齐commit()的FND4-86模式），保证全部参与方
 * 都收到Rollback。假OnzServer经Unsafe.allocateInstance构造（真构造需SM+RocksDB）。
 */
@Fast
public class TestFnd768OnzRollbackIsolation {

	/** 仅记录getZezeInstance调用；allocateInstance跳过构造，字段由测试反射注入。 */
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
			return 0; // 不进入：直接调用rollback()
		}
	}

	@Test
	public void testRollbackContinuesAfterParticipantFail() throws Exception {
		var txn = new FakeTxn();
		procMap(txn).put("z1", failedFuture());
		procMap(txn).put("z2", failedFuture());
		var fake = newFake(List.of("z1")); // z1: getZezeInstance抛异常；z2: null socket发送失败
		txnField("onzServer").set(txn, fake);

		txn.rollback(); // 修复前：z1的异常逃逸rollback()，z2从未被通知

		Assertions.assertEquals(2, fake.asked.size(), "第一个参与方失败后必须继续通知其余参与方（FND7-68）");
		Assertions.assertTrue(fake.asked.contains("z1"), "失败的参与方z1必须被尝试过");
		Assertions.assertTrue(fake.asked.contains("z2"), "后续参与方z2必须收到Rollback通知");
	}

	private static TaskCompletionSource<?> failedFuture() {
		var future = new TaskCompletionSource<EmptyBean.Data>();
		future.setException(new RuntimeException("fake procedure fail"));
		return future;
	}

	private static FakeOnzServer newFake(List<String> throwFor) throws Exception {
		var fake = allocate(FakeOnzServer.class);
		inject(fake, "asked", new CopyOnWriteArrayList<String>());
		inject(fake, "throwFor", throwFor);
		return fake;
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<String, TaskCompletionSource<?>> procMap(OnzTransaction<?, ?> txn) throws Exception {
		return (ConcurrentHashMap<String, TaskCompletionSource<?>>)txnField("zezeProcedures").get(txn);
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
