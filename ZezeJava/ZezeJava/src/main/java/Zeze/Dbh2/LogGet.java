
package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.Get;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;
import Zeze.Builtin.Dbh2.BGetArgumentData;

public class LogGet extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogGet.class.getName());

	private BGetArgumentData argument;

	public LogGet() {
		this(null);
	}

	public LogGet(Get req) {
		super(req);
		if (null != req)
			this.argument = req.Argument;
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
		argument = new BGetArgumentData();
		argument.decode(bb);
	}
}
