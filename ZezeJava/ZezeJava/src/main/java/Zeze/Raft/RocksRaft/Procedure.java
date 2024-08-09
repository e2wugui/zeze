package Zeze.Raft.RocksRaft;

import Zeze.Net.Protocol;
import Zeze.Raft.RaftRetryException;
import Zeze.Raft.RaftRpc;
import Zeze.Util.FuncLong;
import Zeze.Util.TaskCanceledException;
import Zeze.Util.ThrowAgainException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Procedure {
	private static final Logger logger = LogManager.getLogger(Procedure.class);

	private Rocks rocks;
	private FuncLong func;

	public RaftRpc<?, ?> uniqueRequest;
	public Protocol<?> autoResponse;

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

	public final Rocks getRocks() {
		return rocks;
	}

	public final void setRocks(Rocks value) {
		rocks = value;
	}

	public final FuncLong getFunc() {
		return func;
	}

	public final void setFunc(FuncLong value) {
		func = value;
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
