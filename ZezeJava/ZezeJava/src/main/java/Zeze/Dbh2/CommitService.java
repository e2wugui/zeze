package Zeze.Dbh2;

import Zeze.Config;
import Zeze.Net.AsyncSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CommitService extends Zeze.Net.Service {
	public CommitService(Config config) {
		super("Zeze.Dbh2.Commit", config);
	}

	@Override
	public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
		super.OnSocketClose(so, e);
	}
}
