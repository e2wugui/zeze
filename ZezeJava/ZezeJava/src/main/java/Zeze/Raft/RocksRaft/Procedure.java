package Zeze.Raft.RocksRaft;

import Zeze.Net.Protocol;
import Zeze.Raft.RaftRetryException;
import Zeze.Util.Func0;
import Zeze.Util.TaskCanceledException;
import Zeze.Util.ThrowAgainException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Procedure {
	private static final Logger logger = LogManager.getLogger(Table.class);

	private Rocks Rocks;
	private Func0<Long> Func;

	public Protocol<?> UniqueRequest;
	public Protocol<?> AutoResponse;
	public long ResultCode;

	public Procedure() {
	}

	public Procedure(Rocks rocks, Func0<Long> func) {
		Rocks = rocks;
		Func = func;
	}

	public final Rocks getRocks() {
		return Rocks;
	}

	public final void setRocks(Rocks value) {
		Rocks = value;
	}

	public final Func0<Long> getFunc() {
		return Func;
	}

	public final void setFunc(Func0<Long> value) {
		Func = value;
	}

	protected long Process() throws Throwable {
		Func0<Long> func = Func;
		if (func != null)
			return func.call();
		return Zeze.Transaction.Procedure.NotImplement;
	}

	public final long Call() throws Throwable {
		Transaction currentT = Transaction.getCurrent();
		if (currentT == null) {
			try {
				return Transaction.Create().Perform(this);
			} finally {
				Transaction.Destroy();
			}
		}
		currentT.Begin();
		try {
			var result = Process();
			if (result == 0) {
				currentT.Commit();
				return 0;
			}
			currentT.Rollback();
			return result;
		} catch (ThrowAgainException | RaftRetryException e) {
			currentT.Rollback();
			throw e;
		} catch (Throwable e) {
			currentT.Rollback();
			if (e instanceof AssertionError)
				throw e;
			logger.error("RocksRaft Process Exception", e);
			return e instanceof TaskCanceledException
					? Zeze.Transaction.Procedure.CancelException
					: Zeze.Transaction.Procedure.Exception;
		}
	}
}
