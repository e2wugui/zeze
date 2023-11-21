package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.SetBucketMeta;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

public class LogSetBucketMeta extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogSetBucketMeta.class.getName());

	private BBucketMeta.Data argument;

	public LogSetBucketMeta() {
		this((SetBucketMeta)null);
	}

	public LogSetBucketMeta(SetBucketMeta req) {
		super(req);
		if (null != req)
			argument = req.Argument;
	}

	public LogSetBucketMeta(BBucketMeta.Data meta) {
		super(null);
		argument = meta;
	}

	@Override
	public long typeId() {
		return TypeId_;
	}

	@Override
	public void apply(RaftLog holder, StateMachine stateMachine) throws Exception {
		var sm = (Dbh2StateMachine)stateMachine;
		sm.setBucketMeta(argument);
	}

	@Override
	public void encode(ByteBuffer bb) {
		super.encode(bb);
		argument.encode(bb);
	}

	@Override
	public void decode(IByteBuffer bb) {
		super.decode(bb);
		argument = new BBucketMeta.Data();
		argument.decode(bb);
	}
}
