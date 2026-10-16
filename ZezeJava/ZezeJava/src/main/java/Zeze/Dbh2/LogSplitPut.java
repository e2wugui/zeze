package Zeze.Dbh2;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import Zeze.Builtin.Dbh2.BSplitPut;
import Zeze.Builtin.Dbh2.SplitPut;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

/**
 * 分裂数据搬迁写入的 Raft 日志。
 */
public class LogSplitPut extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogSplitPut.class.getName());

	private BSplitPut.@Nullable Data puts;

	public LogSplitPut() {
		this(null);
	}

	public LogSplitPut(@Nullable SplitPut req) {
		super(req);
		if (null != req)
			puts = req.Argument;
	}

	@Override
	public long typeId() {
		return TypeId_;
	}

	@Override
	public void apply(RaftLog holder, StateMachine stateMachine) throws Exception {
		var sm = (Dbh2StateMachine)stateMachine;
		sm.applySplitPut(Objects.requireNonNull(puts));
	}

	@Override
	public void encode(ByteBuffer bb) {
		super.encode(bb);
		Objects.requireNonNull(puts).encode(bb);
	}

	@Override
	public void decode(IByteBuffer bb) {
		super.decode(bb);
		puts = new BSplitPut.Data();
		puts.decode(bb);
	}
}
