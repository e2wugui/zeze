package Zeze.Onz;

import Zeze.Builtin.Onz.BFuncSagaEnd;
import Zeze.Builtin.Onz.FlushReady;
import Zeze.Builtin.Onz.FuncProcedure;
import Zeze.Builtin.Onz.FuncSaga;
import Zeze.Builtin.Onz.FuncSagaEnd;
import Zeze.Builtin.Onz.Ready;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.TaskCompletionSource;

public class OnzAgent extends AbstractOnzAgent {
	private final LongConcurrentHashMap<OnzTransaction> transactions = new LongConcurrentHashMap<>();

	@Override
	protected long ProcessReadyRequest(Ready r) throws Exception {
		var pending = transactions.get(r.Argument.getOnzTid());
		if (null == pending)
			return errorCode(eOnzTidNotFound);

		pending.trySetReady(r);
		return 0;
	}

	@Override
	protected long ProcessFlushReadyRequest(FlushReady r) throws Exception {
		var pending = transactions.get(r.Argument.getOnzTid());
		if (null == pending)
			return errorCode(eOnzTidNotFound);

		pending.trySetFlushReady(r);
		return 0;
	}

	<A extends Bean, R extends Bean> TaskCompletionSource<R>
	callProcedureAsync(OnzTransaction pending,
					   AsyncSocket zezeOnzInstance,
					   String onzProcedureName, A argument, R result, int flushMode) {

		var future  = new TaskCompletionSource<R>();
		var r = new FuncProcedure();
		r.Argument.setOnzTid(pending.getOnzTid());
		r.Argument.setFuncName(onzProcedureName);
		r.Argument.setFlushMode(flushMode);
		var bbArgument = ByteBuffer.Allocate();
		argument.encode(bbArgument);
		r.Argument.setFuncArgument(new Binary(bbArgument));

		r.Send(zezeOnzInstance, (p) ->{
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
		});
		return future;
	}

	<A extends Bean, R extends Bean> TaskCompletionSource<R>
	callSagaAsync(OnzTransaction pending,
				  AsyncSocket zezeOnzInstance,
				  String onzProcedureName, A argument, R result, int flushMode) {

		var future  = new TaskCompletionSource<R>();
		var r = new FuncSaga();
		r.Argument.setOnzTid(pending.getOnzTid());
		r.Argument.setFuncName(onzProcedureName);
		r.Argument.setFlushMode(flushMode);
		var bbArgument = ByteBuffer.Allocate();
		argument.encode(bbArgument);
		r.Argument.setFuncArgument(new Binary(bbArgument));

		r.Send(zezeOnzInstance, (p) ->{
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
		});
		return future;
	}

	TaskCompletionSource<BFuncSagaEnd.Data> callSagaEndAsync(OnzTransaction pending, AsyncSocket zezeOnzInstance) {

		var future  = new TaskCompletionSource<BFuncSagaEnd.Data>();
		var r = new FuncSagaEnd();
		r.Argument.setOnzTid(pending.getOnzTid());

		r.Send(zezeOnzInstance, (p) ->{
			if (r.getResultCode() == 0) {
				future.setResult(new BFuncSagaEnd.Data());
			} else {
				future.setException(new RuntimeException("call error: " + " code=" + r.getResultCode()));
			}
			return 0;
		});
		return future;
	}
}
