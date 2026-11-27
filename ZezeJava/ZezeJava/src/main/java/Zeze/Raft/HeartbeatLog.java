package Zeze.Raft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;

/**
 * Raft 内部心跳日志：不携带用户数据；SetLeaderReadyEvent 用作 leader-ready 的提交见证。
 */
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
			// 重放历史日志时也会触发，setLeaderReady 内部校验 leaderWaitReadyTerm/Index 过滤旧心跳。
			//noinspection DataFlowIssue
			stateMachine.getRaft().setLeaderReady(holder);
			break;
		}
	}

	@Override
	public void encode(ByteBuffer bb) {
		super.encode(bb);
		bb.WriteUInt(operate);
	}

	@Override
	public void decode(IByteBuffer bb) {
		super.decode(bb);
		operate = bb.ReadUInt();
	}

	@Override
	public String toString() {
		return String.format("(%s Operate=%d)", super.toString(), operate);
	}
}
