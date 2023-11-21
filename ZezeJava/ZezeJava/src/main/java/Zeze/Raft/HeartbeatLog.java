package Zeze.Raft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;

final class HeartbeatLog extends Log {
	public static final int SetLeaderReadyEvent = 1;
	public static final int TypeId_ = Bean.hash32(HeartbeatLog.class.getName());

	@Override
	public long typeId() {
		return TypeId_;
	}

	private int operate;

	public HeartbeatLog() {
		this(0);
	}

	public HeartbeatLog(int operate) {
		super(null);
		this.operate = operate;
	}

	@Override
	public void apply(RaftLog holder, StateMachine stateMachine) throws Exception {
		//noinspection SwitchStatementWithTooFewBranches
		switch (operate) {
		case SetLeaderReadyEvent:
			// 由于这个log会被存储到日志队列中，所以在leader选出来，apply历史日志时，就会触发这个调用。
			// 但是这个调用真正产生效果，需要是最后一个 HearbeatLog。
			// 这个条件在setLeaderReady内部检测 leaderWaitReadyTerm，leaderWaitReadyIndex。
			stateMachine.getRaft().setLeaderReady(holder);
			break;
		}
	}

	@Override
	public void encode(ByteBuffer bb) {
		super.encode(bb);
		bb.WriteInt(operate);
	}

	@Override
	public void decode(IByteBuffer bb) {
		super.decode(bb);
		operate = bb.ReadInt();
	}

	@Override
	public String toString() {
		return String.format("(%s Operate=%d)", super.toString(), operate);
	}
}
