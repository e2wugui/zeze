package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jetbrains.annotations.NotNull;

/**
 * pending-settle标志清除日志（GA-D01 A1，additive手写Log类，同LogEndSplit形态）。
 * settle重试链终局（rc==0，或eSplittingBucketNotFound=已结算证据的既有终局语义）时由
 * 源桶追加，apply内身份匹配清除标志（Bucket.clearPendingSettle）。不清除则标志永存，
 * 数个选举周期后可能把陈旧迁移重放进恰好同四元组在途的新世代条目上（跨世代倒灌）。
 * 滚动升级顺序约束：新日志类型对旧follower不可decode——升级窗口避免在途分桶或先升
 * follower（与LogEndMove入仓时同款约束，非新增负担）。
 */
public class LogClearPendingSettle extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogClearPendingSettle.class.getName());

	private BBucketMeta.Data to;

	public LogClearPendingSettle() {
		super(null);
	}

	public LogClearPendingSettle(BBucketMeta.Data to) {
		super(null);
		this.to = to;
	}

	@Override
	public long typeId() {
		return TypeId_;
	}

	@Override
	public void apply(RaftLog holder, StateMachine stateMachine) {
		var sm = (Dbh2StateMachine)stateMachine;
		sm.clearPendingSettle(to);
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		super.encode(bb);
		to.encode(bb);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		super.decode(bb);
		to = new BBucketMeta.Data();
		to.decode(bb);
	}
}
