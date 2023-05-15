package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;

public class LogSetBucketAndDeleteSplittingMeta extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogSetBucketAndDeleteSplittingMeta.class.getName());

	private BBucketMeta.Data argument;

	public LogSetBucketAndDeleteSplittingMeta() {
		super(null);
	}

	public LogSetBucketAndDeleteSplittingMeta(BBucketMeta.Data meta) {
		super(null);
		argument = meta;
	}

	@Override
	public int typeId() {
		return TypeId_;
	}

	@Override
	public void apply(RaftLog holder, StateMachine stateMachine) throws Exception {
		var sm = (Dbh2StateMachine)stateMachine;
		sm.setBucketAndDeleteSplittingMeta(argument);
	}

	@Override
	public void encode(ByteBuffer bb) {
		super.encode(bb);
		argument.encode(bb);
	}

	@Override
	public void decode(ByteBuffer bb) {
		super.decode(bb);
		argument = new BBucketMeta.Data();
		argument.decode(bb);
	}
}
