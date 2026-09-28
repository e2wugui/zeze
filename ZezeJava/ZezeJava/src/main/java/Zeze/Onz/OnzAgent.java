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

	/** 参与方已应答的调用失败（业务非0结果码，或应答载荷decode失败）——应答到达即
	 * FuncSaga已被参与方处理过，注册必然先于任何FuncSagaEnd。与"未应答"的失败
	 * （超时/发送失败，可能处于FuncSagaEnd先于FuncSaga注册被处理的乱序窗口）区分：
	 * cancelSaga仅对未应答失败的步骤做eSagaNotFound的单次延迟重试（见OnzTransaction.cancelSaga）。 */
	static final class CallAnsweredException extends RuntimeException {
		@Serial
		private static final long serialVersionUID = 1L;

		CallAnsweredException(String message) {
			super(message);
		}

		CallAnsweredException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	@Override
	protected long ProcessFlushReadyRequest(FlushReady r) {
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
		// rpc超时不使用字段默认5s——复用flushTimeout（默认10s，随事务可配）。
		// 步骤业务+网络往返超过5s时固定超时把仍会成功的调用判为失败，触发不必要的回滚。
		r.setTimeout(pending.getFlushTimeout());
		var bbArgument = ByteBuffer.Allocate();
		argument.encode(bbArgument);
		r.Argument.setFuncArgument(new Binary(bbArgument));

		// Send false（socket断开窗口）时回调不注册、无超时调度，future必须完成，否则perform的future.get()永久挂起
		if (!r.Send(zezeOnzInstance, (p) ->{
			if (r.getResultCode() == 0) {
				// 回调式发送没有框架future，本局部TCS的唯一完成者是这条回调；
				// 真实应答消费rpc上下文后，超时兜底的双参remove必失败直接return（Rpc.schedule），
				// 不会重放回调——decode抛出（载荷结构性损坏/空载荷decode不足1字节）若发生在
				// setResult之前，异常冲出回调被派发框架吞掉，future永pending，业务的future.get()
				// 无超时永久挂起且零可观测性。对齐sendFlushReady"失败路径必须完成future"与Rpc.handle
				// "resultCode先于future"的先立结果形态：decode失败同样以异常完成future，业务走正常
				// rollback/补偿链而不是无声挂死。
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
		var r = new FuncSaga();
		r.Argument.setOnzTid(pending.getOnzTid());
		r.Argument.setFuncName(onzProcedureName);
		r.Argument.setFlushMode(flushMode);
		r.Argument.setFlushTimeout(pending.getFlushTimeout());
		// rpc超时不使用字段默认5s——复用flushTimeout（默认10s，随事务可配）。
		// saga参与方"发结果即本地提交"（OnzSaga.sendReadyAndWait），固定5s超时把实际会提交
		// 的步骤判为失败，若该步骤未被cancel补偿则写入永久残留（部分提交
		// 的静默分歧）。
		r.setTimeout(pending.getFlushTimeout());
		var bbArgument = ByteBuffer.Allocate();
		argument.encode(bbArgument);
		r.Argument.setFuncArgument(new Binary(bbArgument));

		// 同上：Send false必须完成future
		if (!r.Send(zezeOnzInstance, (p) ->{
			if (r.getResultCode() == 0) {
				// 同callProcedureAsync：回调是TCS唯一完成者，超时兜底被真实
				// 应答短路不重放——decode抛出必须以异常完成future，否则cancelSaga的无超时
				// saga.get()（await saga result）在协调者线程上永久挂起，补偿链无人触发。
				try {
					var bbResult = ByteBuffer.Wrap(r.Result.getFuncResult());
					result.decode(bbResult);
					future.setResult(result);
				} catch (Throwable ex) {
					// 应答已到达（decode失败不影响"已应答"的判别），以CallAnsweredException完成。
					future.setException(new CallAnsweredException(
							"call result decode fail: " + onzProcedureName, ex));
				}
			} else if (r.isTimeout()) {
				// 未应答失败（超时）：保持泛型异常形态，cancelSaga归入乱序窗口类。
				future.setException(new RuntimeException(
						"call error: " + onzProcedureName
								+ " code=" + r.getResultCode()));
			} else {
				// 已应答的业务失败（非0结果码），以CallAnsweredException完成。
				future.setException(new CallAnsweredException(
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
