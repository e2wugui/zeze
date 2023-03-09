package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.BBucketMetaData;
import Zeze.Builtin.Dbh2.SetBucketMeta;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;

public class LogSetBucketMeta extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogSetBucketMeta.class.getName());

	private BBucketMetaData argument;

	public LogSetBucketMeta() {
		this(null);
	}

	public LogSetBucketMeta(SetBucketMeta req) {
		super(req);
		if (null != req)
			argument = req.Argument;
	}

	@Override
	public int typeId() {
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
	public void decode(ByteBuffer bb) {
		super.decode(bb);
		argument = new BBucketMetaData();
		argument.decode(bb);
	}
}
