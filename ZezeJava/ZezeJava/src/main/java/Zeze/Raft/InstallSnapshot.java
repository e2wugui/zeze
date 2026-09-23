package Zeze.Raft;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;

final class InstallSnapshot extends Rpc<BInstallSnapshotArgument, BInstallSnapshotResult> {
	public static final int ProtocolId_ = Bean.hash32(InstallSnapshot.class.getName());
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

	static {
		register(TypeId_, InstallSnapshot.class);
	}

	public InstallSnapshot() {
		Argument = new BInstallSnapshotArgument();
		Result = new BInstallSnapshotResult();
	}

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public static final int ResultCodeTermError = 1;
	public static final int ResultCodeOldInstall = 2;
	public static final int ResultCodeNewOffset = 3;
	public static final int ResultCodeSnapshottingConflict = 4;
	// 同边界安装正在收尾（done 已处理、finalizing 条目未摘除）时对新块的拒绝码；
	// leader 侧与其他非 Success 码相同：SendSnapshotting.end，下个心跳重试。
	public static final int ResultCodeFinalizingConflict = 5;
}
