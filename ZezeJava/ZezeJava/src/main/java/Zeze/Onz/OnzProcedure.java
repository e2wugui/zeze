package Zeze.Onz;

import java.util.Objects;

import java.util.ArrayList;
import java.util.Set;
import Zeze.Builtin.Onz.BFuncProcedure.Data;
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
import org.jspecify.annotations.Nullable;

/** 参与方procedure上下文，负责业务执行、两阶段提交与flush协调。 */
public class OnzProcedure implements FuncLong {
	private static final Logger logger = LogManager.getLogger(OnzProcedure.class);
	private final @Nullable Data funcArgument;
	private final @Nullable OnzProcedureStub<?, ?> stub;
	private final @Nullable Bean argument;
	private final @Nullable Bean result;
	private final @Nullable Rpc<?, ?> rpc;
	private volatile @Nullable TaskCompletionSource<Boolean> commitFuture;

	public @Nullable Rpc<?, ?> getRpc() {
		return rpc;
	}

	public OnzProcedure(@Nullable Rpc<?, ?> rpc,
						@Nullable Data funcArgument,
						@Nullable OnzProcedureStub<?, ?> stub, @Nullable Bean argument, @Nullable Bean result) {
		this.rpc = rpc;
		this.funcArgument = funcArgument;
		this.stub = stub;
		this.argument = argument;
		this.result = result;
	}

	private Data getFuncArgument() {
		return Objects.requireNonNull(funcArgument);
	}

	public int getFlushMode() {
		return getFuncArgument().getFlushMode();
	}

	public long getOnzTid() {
		return getFuncArgument().getOnzTid();
	}

	public @Nullable OnzProcedureStub<?, ?> getStub() {
		return stub;
	}

	public @Nullable Bean getArgument() {
		return argument;
	}

	public @Nullable Bean getResult() {
		return result;
	}

	public boolean isEnd() {
		return true;
	}

	@Override
	public long call() throws Exception {
		// Transaction完成加锁和时戳检查后发送ready，并等待协调者决策。
		var txn = Transaction.getCurrent();
		if (null == txn)
			throw new RuntimeException("no transaction.");
		txn.setOnzProcedure(this);
		return Objects.requireNonNull(getStub()).call(this, Objects.requireNonNull(getArgument()), Objects.requireNonNull(getResult()));
	}

	public String getName() {
		//noinspection DataFlowIssue
		return getStub().getName();
	}

	void commit() {
		Objects.requireNonNull(commitFuture).setResult(true);
	}

	void rollback() {
		Objects.requireNonNull(commitFuture).setException(new RuntimeException("rollback"));
	}

	public void sendReadyAndWait() {
		commitFuture = new TaskCompletionSource<>();
		//noinspection DataFlowIssue
		getStub().getOnz().markReadyProcedure(this);

		var req = (FuncProcedure)Objects.requireNonNull(rpc);
		var bbResult = ByteBuffer.Allocate();
		Objects.requireNonNull(getResult()).encode(bbResult);
		req.Result.setFuncResult(new Binary(bbResult));
		req.SendResult();

		// 超时或中断时先置回滚哨兵；条目已被决策线程取走则等待其完成，防止迟到的Commit误报成功。
		try {
			if (!awaitDecision(getFuncArgument().getFlushTimeout())) {
				if (getStub().getOnz().markTimeoutRolledBack(this)) {
					Objects.requireNonNull(commitFuture).setException(new RuntimeException(
							"onz wait commit/rollback timeout. tid=" + getOnzTid() + " name=" + getName()));
				}
			}
			awaitDecision();
		} catch (InterruptedException e) {
			if (getStub().getOnz().markTimeoutRolledBack(this)) {
				var cause = new RuntimeException(
						"onz wait commit/rollback interrupted. tid=" + getOnzTid() + " name=" + getName(), e);
				Objects.requireNonNull(commitFuture).setException(cause);
				Thread.currentThread().interrupt();
				throw cause;
			}
			// CAS失败说明决策在途，或上面的超时分支已完成future；不得用中断覆盖其结果。
			//noinspection ResultOfMethodCallIgnored
			Thread.interrupted(); // 清除残余中断，等待结束后恢复。
			try {
				for (;;) {
					try {
						awaitDecision();
						break;
					} catch (InterruptedException again) {
						// 决策已在途，再次中断也继续等待。
					}
				}
			} finally {
				Thread.currentThread().interrupt();
			}
		}
	}

	/** await会抛出未声明的InterruptedException；显式声明以便调用方捕获。 */
	private void awaitDecision() throws InterruptedException {
		//noinspection DataFlowIssue
		commitFuture.await();
	}

	/** 同无参重载；取消或超时返回false。 */
	private boolean awaitDecision(long timeoutMs) throws InterruptedException {
		return Objects.requireNonNull(commitFuture).await(timeoutMs);
	}

	@SuppressWarnings("PointlessNullCheck") // 保留发送前显式检查解析到的目标连接。
	protected TaskCompletionSource<Long> sendFlushReady() {
		var future = new TaskCompletionSource<Long>();
		var r = new FlushReady();
		r.Argument.setOnzTid(getOnzTid());
		// 协调者按参与方身份去重，避免重试重复计数。
		//noinspection DataFlowIssue
		r.Argument.setParticipant(getStub().getOnz().getParticipantName());
		// 2倍超时覆盖协调者1.5倍持有期限及传输余量；保留非正值并防止int溢出。
		var flushTimeout = getFuncArgument().getFlushTimeout();
		var flushReadyTimeoutMs = Math.clamp(2L * flushTimeout, flushTimeout, Integer.MAX_VALUE);
		// 原连接断开时尝试同IP的存活连接；找不到则留待下轮checkpoint重试。
		//noinspection DataFlowIssue
		var target = getStub().getOnz().resolveFlushSocket(rpc.getSender());
		if (null == target || !r.Send(target, (p) -> {
			if (r.getResultCode() == 0) {
				future.setResult(0L);
				return 0;
			}
			// 失败也必须完成future，避免sendFlushAndWait永久等待。
			logger.warn("waitFlushReady timeout, {}", funcArgument);
			future.setException(new RuntimeException("waitFlushReady timeout"));
			return 0;
		}, flushReadyTimeoutMs)) {
			logger.warn("sendFlushReady fail, {}", funcArgument);
			future.setException(new RuntimeException("sendFlushReady fail"));
		}
		return future;
	}

	/** 标记ready后发生的本地回滚，使迟到的协调者决策能识别分歧。 */
	public void markRolledBackAfterReady() {
		//noinspection DataFlowIssue
		getStub().getOnz().markRolledBackAfterReady(this);
	}

	public static void sendFlushAndWait(@Nullable Set<? extends @Nullable OnzProcedure> onzProcedures) {
		if (onzProcedures != null) {
			var futures = new ArrayList<TaskCompletionSource<Long>>();
			for (var onz : onzProcedures) {
				if (onz != null && onz.isEnd())
					futures.add(onz.sendFlushReady());
			}
			for (var future : futures)
				future.await();
		}
	}
}
