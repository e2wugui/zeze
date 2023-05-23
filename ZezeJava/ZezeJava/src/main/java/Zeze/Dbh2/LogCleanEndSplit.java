package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Net.Binary;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;

public class LogCleanEndSplit extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogCleanEndSplit.class.getName());

	private byte[] key;

	public LogCleanEndSplit() {
		super(null);
	}

	public LogCleanEndSplit(byte[] key) {
		super(null);
		this.key = key;
	}

	@Override
	public int typeId() {
		return TypeId_;
	}

	@Override
	public void apply(RaftLog holder, StateMachine stateMachine) throws Exception {
		var sm = (Dbh2StateMachine)stateMachine;
		sm.cleanEndSplit(key);
	}

	@Override
	public void encode(ByteBuffer bb) {
		super.encode(bb);
		bb.WriteBytes(key);
	}

	@Override
	public void decode(ByteBuffer bb) {
		super.decode(bb);
		key = bb.ReadBytes();
	}
}
