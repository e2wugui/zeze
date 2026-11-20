package Zeze.Raft.RocksRaft;

import org.jspecify.annotations.Nullable;

import Zeze.Net.Protocol;
import Zeze.Raft.RaftRetryException;
import Zeze.Raft.RaftRpc;
import Zeze.Util.FuncLong;
import Zeze.Util.TaskCanceledException;
import Zeze.Util.ThrowAgainException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * RocksRaft 存储过程：在（新建或嵌套的）事务内执行回调，按返回值提交/回滚并驱动唯一请求应答。
 */
public class Procedure {
	private static final Logger logger = LogManager.getLogger(Procedure.class);

	private @Nullable Rocks rocks;
	private @Nullable FuncLong func;
	private long expectedTerm = -1;

	/**
	 * 唯一请求去重的应用侧接线点（API）：应用在raft存储过程内把携带Unique（requestId&gt;0）
	 * 的RaftRpc赋给本字段，事务提交时其unique与Result随日志持久化——存根有效期内同号重发
	 * 由服务端去重命中并以RaftApplied附原始结果应答，不重执行（存根写入门禁=requestId&gt;0，
	 * 见LogSequence在途存根与Rocks终态存根）。
	 * 不设置（默认null）时重发将作为新日志重新执行——走此模式的处理器必须幂等（框架内建
	 * 服务如SM/GCM-raft均不设置，重发=重执行；Dbh2经appendLog(log, result)等效携带unique）。
	 */
	public @Nullable RaftRpc<?, ?> uniqueRequest;
	public @Nullable Protocol<?> autoResponse;

	public final void setAutoResponseResultCode(long code) {
		if (null != autoResponse)
			autoResponse.setResultCode(code);
	}

	public Procedure() {
	}

	public Procedure(Rocks rocks, FuncLong func) {
		this.rocks = rocks;
		this.func = func;
	}

	public final @Nullable Rocks getRocks() {
		return rocks;
	}

	public final void setRocks(Rocks value) {
		rocks = value;
	}

	public final @Nullable FuncLong getFunc() {
		return func;
	}

	public final void setFunc(@Nullable FuncLong value) {
		func = value;
	}

	public final long getExpectedTerm() {
		return expectedTerm;
	}

	/**
	 * 限定本顶层过程产生的日志只能在指定任期追加；负值保留通常的追加语义。
	 * 校验在Raft锁内进行，任期变化按RaftRetry回滚。嵌套过程由顶层过程控制提交。
	 */
	public final void setExpectedTerm(long value) {
		expectedTerm = value;
	}

	protected long process() throws Exception {
		var func = this.func;
		if (func != null)
			return func.call();
		return Zeze.Transaction.Procedure.NotImplement;
	}

	public final long call() throws Exception {
		var currentT = Transaction.getCurrent();
		if (currentT == null) {
			try {
				return Transaction.create().perform(this);
			} finally {
				Transaction.destroy();
			}
		}
		currentT.bindRocks(rocks);
		currentT.begin();
		try {
			var result = process();
			if (result == 0) {
				currentT.commit();
				return 0;
			}
			currentT.rollback();
			return result;
		} catch (ThrowAgainException | RaftRetryException e) {
			currentT.rollback();
			throw e;
		} catch (Throwable e) { // procedure . exception to return. rethrow AssertionError, logger.error
			currentT.rollback();
			if (e instanceof AssertionError)
				throw e;
			logger.error("RocksRaft Process Exception", e);
			return e instanceof TaskCanceledException
					? Zeze.Transaction.Procedure.CancelException
					: Zeze.Transaction.Procedure.Exception;
		}
	}
}
