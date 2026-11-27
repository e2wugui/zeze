package Zeze.Dbh2;

import org.jspecify.annotations.Nullable;

import Zeze.Builtin.Dbh2.UndoBatch;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

/**
 * 回滚事务批量的 Raft 日志。
 */
public class LogUndoBatch extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogUndoBatch.class.getName());

	private long tid;
	// true=终局确认回滚（物理删除），false=桶侧自主超时回滚（记未确认墓碑，迟到 commit 可复活）
	private boolean fromCoordinator;

	public LogUndoBatch() {
		this(0L);
	}

	public LogUndoBatch(@Nullable UndoBatch req) {
		super(req);
		if (null != req) {
			this.tid = req.Argument.getTid();
			this.fromCoordinator = true;
		}
	}

	public LogUndoBatch(long tid) {
		this(tid, false);
	}

	public LogUndoBatch(long tid, boolean fromCoordinator) {
		super(null);
		this.tid = tid;
		this.fromCoordinator = fromCoordinator;
	}

	@Override
	public long typeId() {
		return TypeId_;
	}

	@Override
	public void apply(RaftLog holder, StateMachine stateMachine) throws Exception {
		var sm = (Dbh2StateMachine)stateMachine;
		sm.undoBatch(tid, fromCoordinator);
	}

	@Override
	public void encode(ByteBuffer bb) {
		super.encode(bb);
		bb.WriteLong(tid);
		bb.WriteBool(fromCoordinator);
	}

	@Override
	public void decode(IByteBuffer bb) {
		super.decode(bb);
		tid = bb.ReadLong();
		// 兼容无来源位的旧格式日志，按自主 undo 处理
		fromCoordinator = bb.getWriteIndex() - bb.getReadIndex() >= 1 && bb.ReadBool();
	}
}
