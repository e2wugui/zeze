package Zeze.Onz;

import Zeze.Builtin.Onz.FlushReady;
import Zeze.Builtin.Onz.FuncProcedure;
import Zeze.Builtin.Onz.FuncSaga;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Data;
import Zeze.Transaction.Procedure;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.TaskCompletionSource;

public class OnzAgent extends AbstractOnzAgent {
	private final LongConcurrentHashMap<OnzTransaction<?, ?>> transactions = new LongConcurrentHashMap<>();
	private final AgentService service = new AgentService();

	public static class AgentService extends Service {
		public static final String eServiceName = "OnzAgent";

		public AgentService() {
			super(eServiceName);
		}
	}

	public OnzAgent() {
		RegisterProtocols(service);
	}

	public void start() throws Exception {
		service.start();
	}

	public void stop() throws Exception {
		service.stop();
	}

	public AgentService getService() {
		return service;
	}

	void addTransaction(OnzTransaction<?, ?> t) {
		if (null != transactions.putIfAbsent(t.getOnzTid(), t))
			throw new RuntimeException("duplication onzTransactionTid=" + t.getOnzTid());
	}

	void removeTransaction(OnzTransaction<?, ?> t) {
		transactions.remove(t.getOnzTid());
	}

	@Override
	protected long ProcessFlushReadyRequest(FlushReady r) throws Exception {
		var pending = transactions.get(r.Argument.getOnzTid());
		if (null == pending) {
			// 迟到的FlushReady：参与方能走到flush阶段，说明它已收到Commit决策
			// （sendReadyAndWait已返回）。这里事务不存在说明协调者侧perform已结束
			// （finally已removeTransaction，含waitFlushDone超时降级路径——降级本来就会
			// 放行已收到的FlushReady并对参与方主动checkpoint）。
			// 如果回错误码，参与方sendFlushReady会判定失败，异常上抛进finalCommit
			// 最终halt(543543)整个进程。幂等放行，允许参与方继续落库。
			r.SendResult();
			return 0;
		}

		pending.trySetFlushReady(r);
		return 0;
	}

	static <A extends Data, R extends Data> TaskCompletionSource<R>
	callProcedureAsync(OnzTransaction<?, ?> pending,
					   AsyncSocket zezeOnzInstance,
					   String onzProcedureName, A argument, R result, int flushMode) {

		var future  = new TaskCompletionSource<R>();
		var r = new FuncProcedure();
		r.Argument.setOnzTid(pending.getOnzTid());
		r.Argument.setFuncName(onzProcedureName);
		r.Argument.setFlushMode(flushMode);
		r.Argument.setFlushTimeout(pending.getFlushTimeout());
		var bbArgument = ByteBuffer.Allocate();
		argument.encode(bbArgument);
		r.Argument.setFuncArgument(new Binary(bbArgument));

		// Send false（socket断开窗口）时回调不注册、无超时调度，future必须完成，否则perform的future.get()永久挂起
		if (!r.Send(zezeOnzInstance, (p) ->{
			if (r.getResultCode() == 0) {
				var bbResult = ByteBuffer.Wrap(r.Result.getFuncResult());
				result.decode(bbResult);
				future.setResult(result);
			} else {
				future.setException(new RuntimeException(
						"call error: " + onzProcedureName
						+ " code=" + r.getResultCode()));
			}
			return 0;
		})) {
			future.setException(new RuntimeException(
					"call error: " + onzProcedureName + " code=" + Procedure.ErrorSendFail));
		}
		return future;
	}

	static <A extends Data, R extends Data> TaskCompletionSource<R>
	callSagaAsync(OnzTransaction<?, ?> pending,
				  AsyncSocket zezeOnzInstance,
				  String onzProcedureName, A argument, R result, int flushMode) {

		var future  = new TaskCompletionSource<R>();
		var r = new FuncSaga();
		r.Argument.setOnzTid(pending.getOnzTid());
		r.Argument.setFuncName(onzProcedureName);
		r.Argument.setFlushMode(flushMode);
		r.Argument.setFlushTimeout(pending.getFlushTimeout());
		var bbArgument = ByteBuffer.Allocate();
		argument.encode(bbArgument);
		r.Argument.setFuncArgument(new Binary(bbArgument));

		// 同上：Send false必须完成future
		if (!r.Send(zezeOnzInstance, (p) ->{
			if (r.getResultCode() == 0) {
				var bbResult = ByteBuffer.Wrap(r.Result.getFuncResult());
				result.decode(bbResult);
				future.setResult(result);
			} else {
				future.setException(new RuntimeException(
						"call error: " + onzProcedureName
								+ " code=" + r.getResultCode()));
			}
			return 0;
		})) {
			future.setException(new RuntimeException(
					"call error: " + onzProcedureName + " code=" + Procedure.ErrorSendFail));
		}
		return future;
	}
}
