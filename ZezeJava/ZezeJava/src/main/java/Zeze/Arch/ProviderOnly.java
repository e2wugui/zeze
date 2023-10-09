package Zeze.Arch;

import Zeze.AppBase;
import Zeze.Builtin.Provider.LinkBroken;
import Zeze.Util.Task;

public class ProviderOnly extends ProviderImplement {
	private ProviderLoadOnly load;

	public ProviderLoadOnly getLoad() {
		return load;
	}

	@Override
	protected long ProcessLinkBroken(LinkBroken p) throws Exception {
		return 0;
	}

	public void create(AppBase app) throws Exception {
		load = new ProviderLoadOnly(app.getZeze());
		var config = app.getZeze().getConfig();
		load.getOverload().register(Task.getThreadPool(), config);
	}

	public void start() {
		load.start();
	}

	public void stop() {
		if (load != null)
			load.stop();
	}
}
