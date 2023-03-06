package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.Put;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;
import Zeze.Builtin.Dbh2.BPutArgumentData;

public class LogPut extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogPut.class.getName());

	private BPutArgumentData argument;

	public LogPut() {
		this(null);
	}

	public LogPut(Put req) {
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

	}

	@Override
	public void encode(ByteBuffer bb) {
		super.encode(bb);
		argument.encode(bb);
	}

	@Override
	public void decode(ByteBuffer bb) {
		super.decode(bb);
		argument = new BPutArgumentData();
		argument.decode(bb);
	}
}
