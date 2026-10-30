package Zeze.Onz;

import Zeze.Application;
import Zeze.Builtin.Onz.BFuncProcedure;
import Zeze.Builtin.Onz.FuncSaga;
import Zeze.Config;
import Zeze.Transaction.EmptyBean;
import Zeze.Util.LongConcurrentHashMap;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

@Fast
public class TestSagaCleanupRefresh {
	private static Onz participant() throws Exception {
		var config = new Config();
		config.setServerId(-1); // 未启动、无数据库，不登记服务身份或占用网络资源。
		config.setNoDatabase(true);
		config.setServiceManager("disable");
		return new Onz(new Application("SagaCleanupRefresh", config));
	}

	@SuppressWarnings("unchecked")
	private static LongConcurrentHashMap<OnzSaga> sagas(Onz onz) throws Exception {
		var field = Onz.class.getDeclaredField("sagas");
		field.setAccessible(true);
		return (LongConcurrentHashMap<OnzSaga>)field.get(onz);
	}

	private static OnzSaga expiredSaga(Onz onz) throws Exception {
		var stub = new OnzSagaStub<>(onz, "retrying", (p, a, r) -> 0,
				EmptyBean.class, EmptyBean.class, (p, a) -> 0, EmptyBean.class);
		var argument = new BFuncProcedure.Data();
		argument.setOnzTid(17);
		var saga = new OnzSaga(new FuncSaga(), argument, stub, new EmptyBean(), new EmptyBean());
		var field = OnzSaga.class.getDeclaredField("lastActiveTime");
		field.setAccessible(true);
		field.setLong(saga, System.currentTimeMillis() - 2 * Onz.eDefaultSagaContextTimeoutMs);
		sagas(onz).put(saga.getOnzTid(), saga);
		return saga;
	}

	@Test
	public void refreshBeforeCleanupLocksKeepsCompensationContext() throws Exception {
		var onz = participant();
		var saga = spy(expiredSaga(onz));
		sagas(onz).put(saga.getOnzTid(), saga);
		// 清理已取到旧时戳，但还未加锁时，插入补偿失败的真实锁内刷新动作。
		doAnswer(invocation -> {
			var observed = invocation.callRealMethod();
			saga.lockBusiness();
			try {
				saga.refreshLastActive();
			} finally {
				saga.unlockBusiness();
			}
			return observed;
		}).doCallRealMethod().when(saga).getLastActiveTime();

		onz.cleanupTimeoutSagas();

		assertSame(saga, sagas(onz).get(saga.getOnzTid()), "刷新后的补偿上下文必须留待重试");
	}

	@Test
	public void inactiveExpiredContextIsStillRemoved() throws Exception {
		var onz = participant();
		var saga = expiredSaga(onz);

		onz.cleanupTimeoutSagas();

		assertNull(sagas(onz).get(saga.getOnzTid()));
	}
}
