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
 * 清除 pending-settle 标志的 Raft 日志：在迁移结算终局后由源桶追加，按身份匹配清除对应标志。
 */
public class LogClearPendingSettle extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogClearPendingSettle.class.getName());

	private BBucketMeta.@Nullable Data to;

	public LogClearPendingSettle() {
		super(null);
	}

	public LogClearPendingSettle(BBucketMeta.Data to) {
		super(null);
		this.to = to;
	}

	@Override
	public long typeId() {
		return TypeId_;
	}

	@Override
	public void apply(RaftLog holder, StateMachine stateMachine) {
		var sm = (Dbh2StateMachine)stateMachine;
		sm.clearPendingSettle(Objects.requireNonNull(to));
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
