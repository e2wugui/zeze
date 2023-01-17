package Zeze.Raft;

import Zeze.Serialize.ByteBuffer;

final class HeartbeatLog extends Log {
	public static final int SetLeaderReadyEvent = 1;

	private int operate;
	private String info;

	public HeartbeatLog() {
		this(0, null);
	}

	public HeartbeatLog(int operate, String info) {
		super(null);
		this.operate = operate;
		this.info = info != null ? info : "";
	}

	public String getInfo() {
		return info;
	}

	@Override
	public void apply(RaftLog holder, StateMachine stateMachine) throws Exception {
		//noinspection SwitchStatementWithTooFewBranches
		switch (operate) {
		case SetLeaderReadyEvent:
			stateMachine.getRaft().setLeaderReady(holder);
			break;
		}
	}

	@Override
	public void encode(ByteBuffer bb) {
		super.encode(bb);
		bb.WriteInt(operate);
		bb.WriteString(info);
	}

	@Override
	public void decode(ByteBuffer bb) {
		super.decode(bb);
		operate = bb.ReadInt();
		info = bb.ReadString();
	}

	@Override
	public String toString() {
		return String.format("(%s Operate=%d Info=%s)", super.toString(), operate, info);
	}
}
