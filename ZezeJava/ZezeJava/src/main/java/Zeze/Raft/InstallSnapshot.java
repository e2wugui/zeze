package Zeze.Raft;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;

final class InstallSnapshot extends Rpc<BInstallSnapshotArgument, BInstallSnapshotResult> {
	public static final int ProtocolId_ = Bean.hash32(InstallSnapshot.class.getName());
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

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
}
