package Zeze.Dbh2.Master;

import Zeze.Config;
import Zeze.Net.AsyncSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MasterService extends Zeze.Net.Service {
	private final Main main;

	public MasterService(Main main, Config config) {
		super("Zeze.Dbh2.Master", config);
		setNoProcedure(true);
		this.main = main;
	}

	@Override
	public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
		super.OnSocketClose(so, e);
		main.getMaster().tryRemoveManager(so);
	}
}
