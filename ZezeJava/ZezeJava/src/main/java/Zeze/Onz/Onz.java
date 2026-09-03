package Zeze.Onz;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import Zeze.Application;
import Zeze.Builtin.Onz.Checkpoint;
import Zeze.Builtin.Onz.Commit;
import Zeze.Builtin.Onz.Rollback;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Onz extends AbstractOnz {
	public static final String eServiceName = "Onz";

	private static final Logger logger = LogManager.getLogger(Onz.class);

	private final ConcurrentHashMap<String, OnzProcedureStub<?, ?>> procedureStubs = new ConcurrentHashMap<>();
	private final LongConcurrentHashMap<OnzProcedure> readyProcedures = new LongConcurrentHashMap<>();
	private final LongConcurrentHashMap<OnzSaga> sagas = new LongConcurrentHashMap<>();
	private final OnzService service;
	private final Application zeze;
	// saga上下文兜底清理：正常流程FuncSagaEnd在步骤成功后数秒内到达；
	// 协调者崩溃（saga无持久化状态，重启后不会重发FuncSagaEnd）或FuncSagaEnd
	// 发送失败时，超时清理是参与方唯一的回收路径（FND-G1-6）。
	private long sagaContextTimeoutMs = 3600_000;
	private Future<?> sagaCleanupTimer;

	public long getSagaContextTimeoutMs() {
		return sagaContextTimeoutMs;
	}

	public void setSagaContextTimeoutMs(long sagaContextTimeoutMs) {
		this.sagaContextTimeoutMs = sagaContextTimeoutMs;
	}

	void markReadyProcedure(OnzProcedure procedure) {
		if (null != readyProcedures.putIfAbsent(procedure.getOnzTid(), procedure))
			throw new RuntimeException("ready procedure exist. " + procedure.getOnzTid());
	}

	public Application getZeze() {
		return zeze;
	}

	public static class OnzService extends Service {
		public static final String eName = "Zeze.Onz.Server";

		public OnzService(Application zeze) {
			super(eName, zeze);
		}
	}

	public Onz(Application zeze) {
		this.zeze = zeze;
		var config = zeze.getConfig();
		if (null != config.getServiceConf(OnzService.eName)) {
			service = new OnzService(zeze);
			RegisterProtocols(service);
		} else {
			service = null;
		}
	}

	public void start() throws Exception {
		if (null != service) {
			service.start();
			var kv = service.getOneAcceptorAddress();
			var ip = kv.getKey();
			var port = kv.getValue();
			var zeze = service.getZeze();
			var config = zeze.getConfig();
			var identity = String.valueOf(config.getServerId());
			zeze.getServiceManager().registerService(new BServiceInfo(eServiceName, identity, 0, ip, port));
		}
		sagaCleanupTimer = TaskSpec.ofAction(this::cleanupTimeoutSagas).schedulePeriodNow(60_000, 60_000);
	}

	public void stop() throws Exception {
		if (null != sagaCleanupTimer) {
			sagaCleanupTimer.cancel(false);
			sagaCleanupTimer = null;
		}
		if (null != service)
			service.stop();
	}

	/**
	 * 清理超时仍未收到FuncSagaEnd的saga上下文。定时器周期调用，测试可直接调用。
	 */
	public void cleanupTimeoutSagas() {
		var now = System.currentTimeMillis();
		for (var it = sagas.iterator(); it.hasNext(); ) {
			var saga = it.next();
			if (!saga.isEnd() && now - saga.getStartTime() >= sagaContextTimeoutMs) {
				// 协调者已不可能再发FuncSagaEnd：正常流程成功后数秒内到达；
				// 协调者崩溃时saga无持久化事务状态（buildSavedCommits为空），
				// 重启后的redoTimer不会重发FuncSagaEnd。滞留条目持有rpc
				// （sender socket引用）与业务bean，且end=false会扭曲flush语义判断。
				if (sagas.remove(saga.getOnzTid(), saga))
					logger.warn("cleanup timeout saga context. tid={}, name={}", saga.getOnzTid(), saga.getName());
			}
		}
	}

	public <A extends Bean, R extends Bean> void register(
			String name, OnzFuncProcedure<A, R> func,
			Class<A> argumentClass, Class<R> resultClass) {

		if (null != procedureStubs.putIfAbsent(name,
				new OnzProcedureStub<>(this, name, func, argumentClass, resultClass)))
			throw new RuntimeException("duplicate Onz Procedure Name=" + name);
	}

	public <A extends Bean, R extends Bean, T extends Bean> void registerSaga(
			String name, OnzFuncSaga<A, R> func, OnzFuncSagaEnd<T> funcCancel,
			Class<A> argumentClass, Class<R> resultClass, Class<T> cancelClass) {

		if (null != procedureStubs.putIfAbsent(name,
				new OnzSagaStub<>(this, name, func, argumentClass, resultClass, funcCancel, cancelClass)))
			throw new RuntimeException("duplicate Onz Procedure Name=" + name);
	}

	@Override
	protected long ProcessCheckpointRequest(Checkpoint r) throws Exception {
		service.getZeze().checkpointRun();
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessCommitRequest(Commit r) throws Exception {
		var procedure = readyProcedures.remove(r.Argument.getOnzTid());
		if (null != procedure)
			procedure.commit();
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessRollbackRequest(Rollback r) throws Exception {
		var procedure = readyProcedures.remove(r.Argument.getOnzTid());
		if (null != procedure)
			procedure.rollback();
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessFuncProcedureRequest(Zeze.Builtin.Onz.FuncProcedure r) throws Exception {
		var stub = procedureStubs.get(r.Argument.getFuncName());
		if (stub == null)
			return errorCode(eProcedureNotFound);
		var buffer = ByteBuffer.Wrap(r.Argument.getFuncArgument().bytesUnsafe());
		var procedure = stub.newProcedure(r, r.Argument, buffer);
		return TaskSpec.ofProcedure(zeze.newProcedure(procedure, procedure.getName())).call();
	}

	@Override
	protected long ProcessFuncSagaRequest(Zeze.Builtin.Onz.FuncSaga r) throws Exception {
		var stub = procedureStubs.get(r.Argument.getFuncName());
		if (stub == null)
			return errorCode(eProcedureNotFound);

		var buffer = ByteBuffer.Wrap(r.Argument.getFuncArgument().bytesUnsafe());
		var procedure = stub.newProcedure(r, r.Argument, buffer);
		if (null != sagas.putIfAbsent(r.Argument.getOnzTid(), (OnzSaga)procedure))
			return errorCode(eSagaTidExist);

		// 步骤失败（业务返回非0或异常）时本地事务已回滚：协调者cancelSaga只对成功的
		// 步骤发FuncSagaEnd（失败步骤被跳过），正常结束路径endSaga也只在成功时到达，
		// 这里不清理则条目永久滞留（持有rpc与业务bean，FND-G1-6）。
		var rc = Procedure.Exception;
		try {
			rc = TaskSpec.ofProcedure(zeze.newProcedure(procedure, procedure.getName())).call();
		} finally {
			if (rc != 0)
				sagas.remove(r.Argument.getOnzTid(), procedure); // 两参remove防御tid条目被替换
		}
		return rc;
	}

	@Override
	protected long ProcessFuncSagaEndRequest(Zeze.Builtin.Onz.FuncSagaEnd r) throws Exception {
		var context = sagas.remove(r.Argument.getOnzTid());
		if (context == null)
			return errorCode(eSagaNotFound);

		// 没有设置cancel标志时，表示事务正常结束，用来删除sagas上下文。
		if (r.Argument.isCancel()) {
			var stub = (OnzSagaStub<?, ?, ?>)context.getStub();
			var cancelArgument = stub.decodeCancelArgument(r.Argument.getFuncArgument());
			var rc = TaskSpec.ofProcedure(zeze.newProcedure(() -> stub.end(context, cancelArgument), context.getName())).call();
			if (rc != 0)
				return rc;
		}
		context.setEnd();

		r.SendResult();
		return 0;
	}
}
