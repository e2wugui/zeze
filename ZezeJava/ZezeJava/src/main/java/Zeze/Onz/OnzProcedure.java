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

/** 参与方侧procedure执行上下文：承载一次FuncProcedure调用的业务执行、ready上报与Commit/Rollback/flush两阶段应答。 */
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

		var req = (FuncProcedure)rpc;
		var bbResult = ByteBuffer.Allocate();
		getResult().encode(bbResult);
		req.Result.setFuncResult(new Binary(bbResult));
		req.SendResult();

		// 发送事务执行阶段的两段式提交的准备完成，同时等待一起提交的信号。
		// 协调者在perform阶段崩溃（决策未持久化：commitIndex尚无记录，重启后的
		// redoTimer不会重发Rollback）时，无超时等待使参与方事务线程永久挂起并持有行锁。
		// 超时按Rollback自愈：清理登记后以异常结束等待，本地事务回滚、锁释放。
		// flushTimeout为协调者随请求下发的既有参数，等待语义与flush路径（sendFlushReady）一致。
		// 目标不变量：任何从等待中异常退出的路径要么留下哨兵（markTimeoutRolledBack成功）、
		// 要么等到既成决策（CAS失败即决策线程已取走条目并即将完成future），绝不把活条目
		// 留给迟到的Commit干净应答（那会静默部分提交：协调者按成功收场而本地已回滚）。
		// 覆盖两个park点：限时await与末行无超时await（后者窗口=限时超时且CAS失败后决策未完成）。
		try {
			if (!awaitDecision(funcArgument.getFlushTimeout())) {
				if (stub.getOnz().markTimeoutRolledBack(this)) {
					// CAS占用成功（条目仍是自己的）：无并发决策，安全以超时异常结束（抛出→本地事务回滚）。
					// 槽位哨兵即超时标记：迟到的Commit取走哨兵即真实不一致（协调者提交了已回滚的
					// 参与方），由ProcessCommitRequest记error暴露——取走与标记在同一map的CAS原子域
					// 内互斥可见，无漏报窗口。
					commitFuture.setException(new RuntimeException(
							"onz wait commit/rollback timeout. tid=" + getOnzTid() + " name=" + getName()));
				}
				// else：迟到的Commit/Rollback已并发取走条目，其线程即将完成future——
				// 等待既成决策，不得覆盖（覆盖可能把已到达的commit翻成rollback）。
			}
			awaitDecision();
		} catch (InterruptedException e) {
			// 任务看门狗（Task.defaultTimeout，ThreadDiagnosable对Normal优先级线程一次性中断）
			// 打断等待park：awaitDecision以受检声明上抛该中断（实际抛点TaskCompletionSource
			// .get，经Task.forceThrow不声明上抛）。此时既不走上面的超时分支（无哨兵），也未必
			// 有决策线程取走条目——若让中断直接逃逸，本地回滚后readyProcedures留活条目，迟到
			// 的Commit取活procedure干净应答成功，分歧error仅在取到哨兵时触发，本路径零标记
			// =静默部分提交。故中断视同超时自愈。park醒来的中断标志已被Thread.interrupted()
			// 消费，catch入口为清除态（残余窗口见下方防御性清除）。
			if (stub.getOnz().markTimeoutRolledBack(this)) {
				// CAS占用成功（条目仍是自己的）：无并发决策，槽位已置换为哨兵，安全以异常
				// 结束（抛出→Transaction.perform catch(Throwable)→finalRollback本地回滚），
				// 迟到的Commit取走哨兵由ProcessCommitRequest记error暴露——与超时分支同收敛。
				var cause = new RuntimeException(
						"onz wait commit/rollback interrupted. tid=" + getOnzTid() + " name=" + getName(), e);
				commitFuture.setException(cause);
				Thread.currentThread().interrupt(); // 恢复中断标志：任务被中断的事实不丢失
				throw cause;
			}
			// CAS失败两源，均不抛出（抛出会把已到达的commit翻成本地回滚）、不覆盖future：
			// 1) 迟到的Commit/Rollback已并发取走条目——决策线程持本procedure引用，且remove与
			//    commit()/rollback()之间无阻塞点，future必将完成，等待即得既成决策；
			// 2) 上面的超时分支已CAS成功置哨兵并setException——future已完成，await立即
			//    以CompletionException返回（同样走本地回滚，哨兵已在位）。
			//noinspection ResultOfMethodCallIgnored
			Thread.interrupted(); // 防御性清除残余标志（中断落在消费点与本catch之间的窗口），等待park不再立即再抛
			try {
				for (;;) {
					try {
						awaitDecision();
						break;
					} catch (InterruptedException again) {
						// 容忍再次中断（看门狗仅中断一次，纯防御）：决策已在途，继续等既成决策。
					}
				}
			} finally {
				Thread.currentThread().interrupt(); // 恢复中断标志（commit正常返回与rollback异常退出两路）
			}
			// 既成决策由future结果表达：commit→await正常返回（perform继续finalCommit本地提交）；
			// rollback→await抛CompletionException（perform catch(Throwable)→finalRollback本地回滚）。
		}
	}

	/** {@link TaskCompletionSource#await()}的窄型中断声明包装：await不声明受检异常，但park被
	 * 中断时经Task.forceThrow以InterruptedException上抛（实际抛点TaskCompletionSource.get；
	 * 同型先例见OnzTransaction.commit"await不声明受检异常（中断在内部分理），统一捕获"的宽捕获
	 * 分诊）。本包装的throws声明使sendReadyAndWait能按窄型捕获中断并视同超时自愈（onz-1：
	 * 中断逃逸会绕过哨兵留下活条目→静默部分提交），无需宽捕获Exception后分诊。 */
	private void awaitDecision() throws InterruptedException {
		commitFuture.await();
	}

	/** {@link TaskCompletionSource#await(long)}的同型包装（语义见无参重载）。
	 * @return 是否得到结果, 取消或超时会返回false */
	private boolean awaitDecision(long timeoutMs) throws InterruptedException {
		return commitFuture.await(timeoutMs);
	}

	protected TaskCompletionSource<Long> sendFlushReady() {
		// 发送事务保存阶段的两段式提交的准备完成，同时等待一起提交的信号。
		var future = new TaskCompletionSource<Long>();
		var r = new FlushReady();
		r.Argument.setOnzTid(getOnzTid());
		// 携带本集群身份（Onz.getParticipantName）。flush失败重试会重走本方法发出
		// 新的rpc对象，协调者按Participant去重计数——重发不虚增
		// 计数提前打开"已全部flush"闸门。身份不填（旧版本参与方）时协调者按rpc对象兜底计数。
		r.Argument.setParticipant(stub.getOnz().getParticipantName());
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

	/**
	 * ready后本地回滚（perform停机拒绝等"结果已发、决策已到而本地落库失败"）——
	 * 由Transaction.perform在RejectWhileStopping分支调用：记分歧error的补充动作，
	 * 回填超时哨兵覆盖迟到redo Commit的二次确认（见Onz.markRolledBackAfterReady）。
	 */
	public void markRolledBackAfterReady() {
		stub.getOnz().markRolledBackAfterReady(this);
	}

	public static void sendFlushAndWait(@Nullable Set<OnzProcedure> onzProcedures) {
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
