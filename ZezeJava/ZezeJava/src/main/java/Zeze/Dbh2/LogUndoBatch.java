package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.UndoBatch;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jetbrains.annotations.NotNull;

/**
 * 回滚事务批量的 Raft 日志。
 */
public class LogUndoBatch extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogUndoBatch.class.getName());

	private long tid;
	// undo 来源（FND29 dbh2-03 断根）：true=协调者驱动的 UndoBatch（决策已终局，apply 即确认）；
	// false=桶侧 onTimer 自主超时 undo（协调者决策未知，apply 走未确认墓碑——迟到
	// LogCommitBatch 可复活，协调者 UndoBatch 或墓碑窗超时才物理删除）。
	private boolean fromCoordinator;

	public LogUndoBatch() {
		this(0L);
	}

	public LogUndoBatch(UndoBatch req) {
		super(req);
		if (null != req) {
			this.tid = req.Argument.getTid();
			this.fromCoordinator = true;
		}
	}

	public LogUndoBatch(long tid) {
		super(null);
		this.tid = tid;
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
	public void encode(@NotNull ByteBuffer bb) {
		super.encode(bb);
		bb.WriteLong(tid);
		bb.WriteBool(fromCoordinator);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		super.decode(bb);
		tid = bb.ReadLong();
		// 旧版本日志条目无来源位：剩余字节守卫（升级期 replay 的旧条目按自主 undo 处理；
		// replay 时事务表为空，两条路径都落 not-found 分支，误分类无行为差异）。
		fromCoordinator = bb.getWriteIndex() - bb.getReadIndex() >= 1 && bb.ReadBool();
	}
}
