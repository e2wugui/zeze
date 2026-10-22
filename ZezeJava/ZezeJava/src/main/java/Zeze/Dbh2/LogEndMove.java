package Zeze.Dbh2;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

/**
 * 结束桶迁移的 Raft 日志。
 */
public class LogEndMove extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogEndMove.class.getName());

	private BBucketMeta.@Nullable Data to;

	public LogEndMove() {
		super(null);
	}

	public LogEndMove(BBucketMeta.Data to) {
		super(null);
		this.to = to;
	}

	public BBucketMeta.@Nullable Data getTo() {
		return to;
	}

	@Override
	public long typeId() {
		return TypeId_;
	}

	@Override
	public void apply(RaftLog holder, StateMachine stateMachine) throws Exception {
		var sm = (Dbh2StateMachine)stateMachine;
		sm.endMove(Objects.requireNonNull(to));
	}

	@Override
	public void encode(ByteBuffer bb) {
		super.encode(bb);
		//noinspection DataFlowIssue
		to.encode(bb);
	}

	@Override
	public void decode(IByteBuffer bb) {
		super.decode(bb);
		to = new BBucketMeta.Data();
		to.decode(bb);
	}
}
