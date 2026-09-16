package Zeze.Onz;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import sun.misc.Unsafe;
import Zeze.Builtin.Onz.BSavedCommits;
import Zeze.Net.AsyncSocket;
import Zeze.Transaction.EmptyBean;
import Zeze.Util.TaskCompletionSource;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * R2-M③ 回归（无网络桩）：OnzTransaction.endSaga的发送循环无逐参与方容错——
 * 第一个参与方的getZezeInstance/SendForWait异常中断整个循环，后续参与方收不到
 * FuncSagaEnd(cancel=false)：上下文与setEnd滞留，只能等参与方cleanupTimeoutSagas
 * （默认1小时）回收。commit()虽有兜底catch（endSaga失败不转rollback），但通知
 * 依旧缺失。修复：逐参与方try/catch记error后继续（对齐cancelSaga/commit()的
 * FND4-86模式），await循环同理。假OnzServer经Unsafe.allocateInstance构造。
 */
@Fast
public class TestEndSagaNotifiesAllParticipants {

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
			return null; // null socket -> SendForWait future "Send Fail"（不触网络）
		}

		@Override
		void saveCommitPoint(byte[] tidBytes, BSavedCommits.Data bState, int state) {
			// 假服务器：跳过持久化（commit决策点），直接进入endSaga通知阶段。
		}

		@Override
		void removeCommitRecord(byte[] tidBytes) {
			// 假服务器：无持久化可清理。
		}
	}

	static class FakeTxn extends OnzTransaction<EmptyBean.Data, EmptyBean.Data> {
		@Override
		protected long perform() throws Exception {
			return 0; // 不进入：直接调用commit()（其内部endSaga）
		}
	}

	@Test
	public void testEndSagaContinuesAfterParticipantFail() throws Exception {
		var txn = new FakeTxn();
		var ok = new TaskCompletionSource<EmptyBean.Data>();
		ok.setResult(EmptyBean.Data.instance); // 无状态bean可安全共享
		sagaMap(txn).put("z1", ok);
		sagaMap(txn).put("z2", ok);
		var fake = newFake(List.of("z1")); // z1: getZezeInstance抛异常；z2: null socket发送失败
		txnField("onzServer").set(txn, fake);

		txn.commit(new byte[16], null); // zezeProcedures为空：saveCommitPoint后直接endSaga

		Assertions.assertEquals(2, fake.asked.size(), "第一个参与方失败后必须继续通知其余参与方（R2-M③）");
		Assertions.assertTrue(fake.asked.contains("z1"), "失败的参与方z1必须被尝试过");
		Assertions.assertTrue(fake.asked.contains("z2"), "后续参与方z2必须收到FuncSagaEnd(cancel=false)通知");
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

	private static FakeOnzServer newFake(List<String> throwFor) throws Exception {
		var fake = allocate(FakeOnzServer.class);
		inject(fake, "asked", new CopyOnWriteArrayList<String>());
		inject(fake, "throwFor", throwFor);
		return fake;
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
