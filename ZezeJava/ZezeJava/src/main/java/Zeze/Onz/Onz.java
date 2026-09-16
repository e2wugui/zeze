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
	// FND5-45联动/FND6-38：ready等待超时自愈回滚的tid记账（值=回滚时刻，有界），仅供
	// 过期清理循迹；决定性状态在readyProcedures的槽位哨兵（TimeoutRolledBackMarker）——
	// 「取走登记」（Commit/Rollback的remove）与「自愈标记」（replace CAS置哨兵）在同一
	// map的CAS原子域内互斥可见，消除原remove→mark两语句间隙被并发Commit穿过（两表皆null
	// 按已提交幂等重发假应答成功，分歧暴露日志漏报）的窗口；先mark后remove则产生假阳性。
	// 条目随saga清理周期过期（过期时连同槽位哨兵一起回收）。
	private static final long TimeoutRolledBackTtlMs = 3600_000;
	private final LongConcurrentHashMap<Long> timeoutRolledBack = new LongConcurrentHashMap<>();
	static final OnzProcedure TimeoutRolledBackMarker = new OnzProcedure(null, null, null, null, null);

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

	/** FND5-45/FND6-38：参与方ready等待超时自愈——CAS把槽位原子置换为超时哨兵。
	 * 仅当条目仍是自己时成功：迟到的Commit/Rollback可能已并发取走，由调用方等待
	 * 既成决策（不得覆盖）。成功即已标记，另记时间戳供TTL清理。 */
	boolean markTimeoutRolledBack(OnzProcedure procedure) {
		var tid = procedure.getOnzTid();
		if (!readyProcedures.replace(tid, procedure, TimeoutRolledBackMarker))
			return false;
		timeoutRolledBack.put(tid, System.currentTimeMillis());
		return true;
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
		for (var saga : sagas) {
			if (!saga.isEnd() && now - saga.getStartTime() >= sagaContextTimeoutMs) {
				// 协调者已不可能再发FuncSagaEnd：正常流程成功后数秒内到达；
				// 协调者崩溃时saga无持久化事务状态（buildSavedCommits为空），
				// 重启后的redoTimer不会重发FuncSagaEnd。滞留条目持有rpc
				// （sender socket引用）与业务bean，且end=false会扭曲flush语义判断。
				if (sagas.remove(saga.getOnzTid(), saga))
					logger.warn("cleanup timeout saga context. tid={}, name={}", saga.getOnzTid(), saga.getName());
			}
		}
		// FND5-45联动的超时回滚登记过期：Rollback最迟在崩溃协调者重启+redo一轮内到达，
		// 1小时足够（对齐sagaContextTimeoutMs默认）。
		for (var it = timeoutRolledBack.keyIterator(); it.hasNext(); ) {
			var tid = it.next();
			var stamp = timeoutRolledBack.get(tid);
			if (stamp != null && now - stamp >= TimeoutRolledBackTtlMs) {
				timeoutRolledBack.remove(tid);
				readyProcedures.remove(tid, TimeoutRolledBackMarker); // FND6-38：连同槽位哨兵一起过期
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
		var tid = r.Argument.getOnzTid();
		var procedure = readyProcedures.remove(tid);
		if (null != procedure) {
			if (procedure == TimeoutRolledBackMarker) {
				// FND5-44/45联动（FND6-38原子化）：本参与方已超时自愈回滚，协调者却持久化了
				// commit决策——真实不一致（静默部分提交），error暴露。仍应答成功：改错误码会让
				// redo无限重发（条目已不存在，永无应答成功的可能），且无法与已提交后的重复发送区分。
				logger.error("Commit for timeout-rolled-back onz tid={}"
						+ " (participant rolled back before decision arrived; coordinator committed)"
						+ " -- data divergence exposed.", tid);
				timeoutRolledBack.remove(tid); // 记账清理（漏删亦由TTL回收）
			} else
				procedure.commit();
		}
		// else：已提交后的重复发送（redo重发/应答丢失），正常幂等路径。
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessRollbackRequest(Rollback r) throws Exception {
		var tid = r.Argument.getOnzTid();
		var procedure = readyProcedures.remove(tid);
		if (null != procedure) {
			if (procedure == TimeoutRolledBackMarker)
				// 超时自愈后到达的Rollback：与本地已回滚一致，静默回收哨兵（无重复发送告警价值）。
				timeoutRolledBack.remove(tid);
			else
				procedure.rollback();
		}
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
		// FND7-34：业务执行期间持有businessLock，与并发的FuncSagaEnd(cancel/end)互斥——
		// 协调者超时补偿会在业务仍执行时到达，不互斥则补偿与业务并发/抢先，业务随后
		// 失败回滚时补偿就成了过补偿。
		var rc = Procedure.Exception;
		((OnzSaga)procedure).lockBusiness();
		try {
			rc = TaskSpec.ofProcedure(zeze.newProcedure(procedure, procedure.getName())).call();
		} finally {
			// 失败清理必须在businessLock之内、解锁之前（R3-C复审）：解锁与remove的间隙里，
			// 等锁的FuncSagaEnd(cancel)会抢先 acquire 并在sagas.remove(tid,context)成功后
			// 对已回滚（写从未发生）的业务执行补偿——过补偿（反向分歧）。锁内先remove再
			// unlock，等锁方醒来必然观察到条目已消失（锁的happens-before），应答eSagaNotFound。
			if (rc != 0)
				sagas.remove(r.Argument.getOnzTid(), procedure); // 两参remove防御tid条目被替换
			((OnzSaga)procedure).unlockBusiness();
		}
		return rc;
	}

	@Override
	protected long ProcessFuncSagaEndRequest(Zeze.Builtin.Onz.FuncSagaEnd r) throws Exception {
		var tid = r.Argument.getOnzTid();
		var context = sagas.get(tid);
		if (context == null)
			return errorCode(eSagaNotFound);

		// FND7-34：等业务完成再决策（FuncSagaEnd可能在慢业务执行期间到达）。
		// 业务失败已在finally中自清理条目：锁到手后remove失败即eSagaNotFound，
		// 失败步骤不会被补偿（无过补偿）；业务成功则条目仍在，补偿/结束串行执行。
		context.lockBusiness();
		try {
			if (!sagas.remove(tid, context))
				return errorCode(eSagaNotFound);

			// 没有设置cancel标志时，表示事务正常结束，用来删除sagas上下文。
			if (r.Argument.isCancel()) {
				var stub = (OnzSagaStub<?, ?, ?>)context.getStub();
				var cancelArgument = stub.decodeCancelArgument(r.Argument.getFuncArgument());
				var rc = TaskSpec.ofProcedure(zeze.newProcedure(() -> stub.end(context, cancelArgument), context.getName())).call();
				if (rc != 0) {
					// 补偿失败：上下文必须放回sagas，否则协调者（cancelSaga只记错误日志不重试）
					// 或人工重发FuncSagaEnd时只能得到eSagaNotFound，补偿永久丢失且不可重试。
					// 放回后由cleanupTimeoutSagas超时兜底（默认1小时，可配置）。
					if (null != sagas.putIfAbsent(tid, context))
						logger.error("saga context re-insert conflict. tid={}", tid);
					return rc;
				}
			}
			context.setEnd();
		} finally {
			context.unlockBusiness();
		}

		r.SendResult();
		return 0;
	}
}
