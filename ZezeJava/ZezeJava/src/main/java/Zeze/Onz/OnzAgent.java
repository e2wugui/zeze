package Zeze.Onz;

import java.io.Serial;
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
import org.jspecify.annotations.Nullable;

/** 协调者侧代理：维护在途OnzTransaction登记供redo过滤真残留，并提供远程procedure/saga调用的发送辅助。 */
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

	// redoTimer 以在途登记过滤，redo 只处理真残留（perform 线程已死亡的）。
	boolean hasTransaction(long onzTid) {
		return transactions.containsKey(onzTid);
	}

	/**
	 * 参与方已应答的调用失败（非 0 结果码或应答载荷解码失败），用于与超时/发送失败区分：
	 * 已应答说明 FuncSaga 已先于 FuncSagaEnd 完成注册，cancelSaga 无需对 NotFound 做乱序重试。
	 */
	static final class CallAnsweredException extends RuntimeException {
		@Serial
		private static final long serialVersionUID = 1L;
		final boolean participantSucceeded;

		CallAnsweredException(String message) {
			this(message, null, false);
		}

		CallAnsweredException(String message, @Nullable Throwable cause,
							  boolean participantSucceeded) {
			super(message, cause);
			this.participantSucceeded = participantSucceeded;
		}
	}

	@Override
	protected long ProcessFlushReadyRequest(FlushReady r) {
		var pending = transactions.get(r.Argument.getOnzTid());
		if (null == pending) {
			// 事务已结束或已降级时幂等放行迟到的 FlushReady，避免参与方提交阶段异常停机。
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
		// 复用事务 flushTimeout 作为 RPC 超时，避免长步骤被固定 5s 误判失败。
		r.setTimeout(pending.getFlushTimeout());
		var bbArgument = ByteBuffer.Allocate();
		argument.encode(bbArgument);
		r.Argument.setFuncArgument(new Binary(bbArgument));

		// Send 返回 false 时不会触发回调，必须显式完成 future 防止调用方永久等待。
		if (!r.Send(zezeOnzInstance, (p) ->{
			if (r.getResultCode() == 0) {
				// 回调是 future 的唯一完成者，解码异常必须以异常完成 future 防止调用方永久挂起。
				try {
					var bbResult = ByteBuffer.Wrap(r.Result.getFuncResult());
					result.decode(bbResult);
					future.setResult(result);
				} catch (Throwable ex) {
					future.setException(new RuntimeException(
							"call result decode fail: " + onzProcedureName, ex));
				}
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
		sendSaga(pending, zezeOnzInstance, onzProcedureName, argument, result, flushMode, future);
		return future;
	}

	static <A extends Data, R extends Data> void sendSaga(OnzTransaction<?, ?> pending,
			AsyncSocket zezeOnzInstance, String onzProcedureName, A argument, R result, int flushMode,
			TaskCompletionSource<R> future) {
		var r = new FuncSaga();
		r.Argument.setOnzTid(pending.getOnzTid());
		r.Argument.setFuncName(onzProcedureName);
		r.Argument.setFlushMode(flushMode);
		r.Argument.setFlushTimeout(pending.getFlushTimeout());
		// 复用事务 flushTimeout 作为 RPC 超时，避免已本地提交的慢步骤被过早判为超时。
		r.setTimeout(pending.getFlushTimeout());
		var bbArgument = ByteBuffer.Allocate();
		argument.encode(bbArgument);
		r.Argument.setFuncArgument(new Binary(bbArgument));

		if (!r.Send(zezeOnzInstance, (p) ->{
			if (r.getResultCode() == 0) {
				// 解码异常同样须完成 future，防止 cancelSaga 等待结果时永久挂起。
				try {
					var bbResult = ByteBuffer.Wrap(r.Result.getFuncResult());
					result.decode(bbResult);
					future.setResult(result);
				} catch (Throwable ex) {
					// 成功应答仍证明步骤已提交，本地解码失败不改变补偿分诊。
					future.setException(new CallAnsweredException(
							"call result decode fail: " + onzProcedureName, ex, true));
				}
			} else if (r.isTimeout() || r.getResultCode() == Procedure.ErrorSendFail) {
				// 未应答失败（超时或发送失败）保持普通异常，供 cancelSaga 按乱序窗口处理。
				future.setException(new RuntimeException(
						"call error: " + onzProcedureName
								+ " code=" + r.getResultCode()));
			} else {
				future.setException(new CallAnsweredException(
						"call error: " + onzProcedureName
								+ " code=" + r.getResultCode()));
			}
			return 0;
		})) {
			future.setException(new RuntimeException(
					"call error: " + onzProcedureName + " code=" + Procedure.ErrorSendFail));
		}
	}
}
