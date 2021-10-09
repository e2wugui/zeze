package Zeze.Raft;

import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.*;
import java.util.*;

public final class InstallSnapshot extends Rpc<InstallSnapshotArgument, InstallSnapshotResult> {
	public final static int ProtocolId_ = Bean.Hash16(InstallSnapshot.class.FullName);

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