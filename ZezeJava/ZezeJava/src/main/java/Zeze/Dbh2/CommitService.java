package Zeze.Dbh2;

import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Transaction.DispatchMode;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CommitService extends Zeze.Net.Service {
	public CommitService(Config config) {
		super("Zeze.Dbh2.Commit", config);
		setNoProcedure(true);
	}

	@Override
	public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
		super.OnSocketClose(so, e);
	}
}