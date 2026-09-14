package Zeze.Onz;

import java.util.ArrayList;
import java.util.Set;
import Zeze.Builtin.Onz.BFuncProcedure;
import Zeze.Builtin.Onz.FlushReady;
import Zeze.Builtin.Onz.FuncProcedure;
import Zeze.Net.Binary;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Transaction;
import Zeze.Util.FuncLong;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OnzProcedure implements FuncLong {
	private static final @NotNull Logger logger = LogManager.getLogger(OnzProcedure.class);
	private final BFuncProcedure.Data funcArgument;
	private final OnzProcedureStub<?, ?> stub;
	private final Bean argument;
	private final Bean result;
	private final Rpc<?, ?> rpc;
	private volatile TaskCompletionSource<Boolean> commitFuture;

	public Rpc<?, ?> getRpc() {
		return rpc;
	}

	public OnzProcedure(Rpc<?, ?> rpc,
						BFuncProcedure.Data funcArgument,
						OnzProcedureStub<?, ?> stub, Bean argument, Bean result) {
		this.rpc = rpc;
		this.funcArgument = funcArgument;
		this.stub = stub;
		this.argument = argument;
		this.result = result;
	}

	public int getFlushMode() {
		return funcArgument.getFlushMode();
	}

	public long getOnzTid() {
		return funcArgument.getOnzTid();
	}

	public OnzProcedureStub<?, ?> getStub() {
		return stub;
	}

	public Bean getArgument() {
		return argument;
	}

	public Bean getResult() {
		return result;
	}

	public boolean isEnd() {
		return true;
	}

	@Override
	public long call() throws Exception {
		// 这里实际上需要侵入Zeze.Transaction，在锁定，时戳检查完成后，
		// 发送result给调用者，完成ready状态，
		// Zeze.Transaction 需要同步进行等待。

		var txn = Transaction.getCurrent();
		if (null == txn)
			throw new RuntimeException("no transaction.");
		txn.setOnzProcedure(this);
		return stub.call(this, argument, result);
	}

	public String getName() {
		return stub.getName();
	}

	void commit() {
		// throw if null
		commitFuture.setResult(true);
	}

	void rollback() {
		commitFuture.setException(new RuntimeException("rollback"));
	}

	public void sendReadyAndWait() {
		commitFuture = new TaskCompletionSource<>();
		stub.getOnz().markReadyProcedure(this);

		// 发送rpc结果
		var req = (FuncProcedure)rpc;
		var bbResult = ByteBuffer.Allocate();
		getResult().encode(bbResult);
		req.Result.setFuncResult(new Binary(bbResult));
		req.SendResult();

		// 发送事务执行阶段的两段式提交的准备完成，同时等待一起提交的信号。
		// FND5-45：协调者在perform阶段崩溃（决策未持久化：commitIndex尚无记录，重启后的
		// redoTimer不会重发Rollback）时，无超时等待使参与方事务线程永久挂起并持有行锁。
		// 超时按Rollback自愈：清理登记后以异常结束等待，本地事务回滚、锁释放。
		// flushTimeout为协调者随请求下发的既有参数，等待语义与flush路径（sendFlushReady）一致。
		if (!commitFuture.await(funcArgument.getFlushTimeout())) {
			if (stub.getOnz().removeReadyProcedure(this)) {
				// 条目仍是自己的：无并发决策，安全以超时异常结束（抛出→本地事务回滚）。
				// 登记tid：迟到的Commit命中即真实不一致（协调者提交了已回滚的参与方），
				// 由ProcessCommitRequest记error暴露。
				stub.getOnz().markTimeoutRolledBack(getOnzTid());
				commitFuture.setException(new RuntimeException(
						"onz wait commit/rollback timeout. tid=" + getOnzTid() + " name=" + getName()));
			}
			// else：迟到的Commit/Rollback已并发取走条目，其线程即将完成future——
			// 等待既成决策，不得覆盖（覆盖可能把已到达的commit翻成rollback）。
		}
		commitFuture.await();
	}

	protected TaskCompletionSource<Long> sendFlushReady() {
		// 发送事务保存阶段的两段式提交的准备完成，同时等待一起提交的信号。
		var future = new TaskCompletionSource<Long>();
		var r = new FlushReady();
		r.Argument.setOnzTid(getOnzTid());
		if (!r.Send(rpc.getSender(), (p) -> {
			if (r.getResultCode() == 0) {
				future.setResult(0L);
				return 0;
			}
			// 两条失败路径都必须完成future（setException）：sendFlushAndWait用无超时await且被Checkpoint.flush
			// 在提交路径调用，不完成future会永久卡死zeze事务线程。异常让该事务提交失败走redo，
			// 对端未确认flush时静默继续会破坏两段式提交（Onz saga补偿兜底）。
			logger.warn("waitFlushReady timeout, {}", funcArgument);
			future.setException(new RuntimeException("waitFlushReady timeout"));
			return 0;
		}, funcArgument.getFlushTimeout())) {
			logger.warn("sendFlushReady fail, {}", funcArgument);
			future.setException(new RuntimeException("sendFlushReady fail"));
		}
		return future;
	}

	// helper
	public static void sendFlushAndWait(@Nullable Set<OnzProcedure> onzProcedures) {
		if (onzProcedures != null) {
			// send all
			var futures = new ArrayList<TaskCompletionSource<Long>>();
			for (var onz : onzProcedures) {
				if (onz != null && onz.isEnd())
					futures.add(onz.sendFlushReady());
			}
			// wait all
			for (var future : futures)
				future.await();
		}
	}
}
