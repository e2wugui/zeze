package Zeze.Onz;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Builtin.Onz.Checkpoint;
import Zeze.Builtin.Onz.Commit;
import Zeze.Builtin.Onz.Rollback;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Transaction.Bean;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Task;

public class Onz extends AbstractOnz {
	public static final String eServiceName = "Onz";

	private final ConcurrentHashMap<String, OnzProcedureStub<?, ?>> procedureStubs = new ConcurrentHashMap<>();
	private final LongConcurrentHashMap<OnzProcedure> readyProcedures = new LongConcurrentHashMap<>();
	private final LongConcurrentHashMap<OnzSaga> sagas = new LongConcurrentHashMap<>();
	private final OnzService service;
	private final Application zeze;

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
	}

	public void stop() throws Exception {
		if (null != service)
			service.stop();
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
		return Task.call(zeze.newProcedure(procedure, procedure.getName()));
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

		return Task.call(zeze.newProcedure(procedure, procedure.getName()));
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
			var rc = Task.call(zeze.newProcedure(() -> stub.end(context, cancelArgument), context.getName()));
			if (rc != 0)
				return rc;
		}
		context.setEnd();

		r.SendResult();
		return 0;
	}
}
