package Zeze.MQ.Master;

import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MasterService extends Service {
	private final Main main;

	public MasterService(Main main, Config config) {
		super("Zeze.MQ.Master", config);
		this.main = main;
	}

	@Override
	public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
		super.OnSocketClose(so, e);
		main.getMaster().tryRemoveManager(so);
	}
}
