package Zeze.Raft;

import Zeze.Serialize.ByteBuffer;

final class HeartbeatLog extends Log {
	public static final int SetLeaderReadyEvent = 1;

	private int Operate;
	private String Info;

	public HeartbeatLog() {
		this(0, null);
	}

	public HeartbeatLog(int operate, String info) {
		super(null);
		Operate = operate;
		Info = info != null ? info : "";
	}

	public String getInfo() {
		return Info;
	}

	@Override
	public void Apply(RaftLog holder, StateMachine stateMachine) throws Throwable {
		//noinspection SwitchStatementWithTooFewBranches
		switch (Operate) {
		case SetLeaderReadyEvent:
			stateMachine.getRaft().SetLeaderReady(holder);
			break;
		}
	}

	@Override
	public void Encode(ByteBuffer bb) {
		super.Encode(bb);
		bb.WriteInt(Operate);
		bb.WriteString(Info);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		super.Decode(bb);
		Operate = bb.ReadInt();
		Info = bb.ReadString();
	}

	@Override
	public String toString() {
		return String.format("(%s Operate=%d Info=%s)", super.toString(), Operate, Info);
	}
}
